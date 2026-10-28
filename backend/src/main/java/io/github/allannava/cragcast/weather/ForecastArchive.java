package io.github.allannava.cragcast.weather;

import io.github.allannava.cragcast.conditions.ConditionsScorer;
import io.github.allannava.cragcast.conditions.Verdict;
import io.github.allannava.cragcast.conditions.Weather;
import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-106: after each successful refresh, appends what it computed to {@code crag_forecast} —
 * per crag and day, the verdict, its reasons, the four numbers and the day's hours — keyed by
 * the refresh's time, so a past hour's answer stays readable after crag_weather moves on.
 * Scored from the forecasts the refresh already fetched; never a second Open-Meteo request.
 * A failure here is logged and loses that hour's rows only: the refresh and the page's data
 * are already done.
 */
@Component
public class ForecastArchive {

    private static final Logger log = LoggerFactory.getLogger(ForecastArchive.class);
    private static final String INSERT = """
            INSERT INTO crag_forecast (crag_id, day, fetched_at, verdict, reasons, rain_last48h_mm,
                rain_during_day_mm, max_temperature_c, max_wind_kmh, hours)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING""";

    /** One crag-day as the refresh at {@code fetchedAt} saw it; reasons and hours as JSON text. */
    record Row(long cragId, LocalDate day, Instant fetchedAt, Verdict verdict, String reasons, Weather weather,
               String hours) {
    }

    private final JsonMapper json = JsonMapper.builder().build();
    private final ConditionsScorer scorer = new ConditionsScorer();
    private final CragRepository crags;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    ForecastArchive(CragRepository crags, JdbcTemplate jdbc, TransactionTemplate tx, Clock clock) {
        this.crags = crags;
        this.jdbc = jdbc;
        this.tx = tx;
        this.clock = clock;
    }

    @EventListener
    void on(ForecastsRefreshed e) {
        try {
            int n = append(rows(crags.findAll(), e.forecasts(), e.fetchedAt()));
            log.info("forecasts archived: {} crag-days", n);
        } catch (RuntimeException ex) {
            // Never thrown on: the page's data listens to the same event.
            log.warn("forecasts not archived for {}: {}", e.fetchedAt(), ex.toString());
        }
    }

    /** The same days the refresh stores, today to the day after tomorrow, in the refresh's zone. */
    List<Row> rows(List<Crag> all, Map<String, HourlyForecast> forecasts, Instant fetchedAt) {
        LocalDate today = LocalDate.ofInstant(fetchedAt, clock.getZone());
        List<Row> out = new ArrayList<>();
        for (Crag c : all) {
            HourlyForecast f = forecasts.get(c.getSlug());
            if (f == null) {
                continue;
            }
            for (int d = 0; d < WeatherRefresh.DAYS; d++) {
                LocalDate day = today.plusDays(d);
                Weather w = WeatherReducer.reduce(f, day);
                ConditionsScorer.Result r = scorer.score(c.getAspect(), w);
                out.add(new Row(c.getId(), day, fetchedAt, r.verdict(), json.writeValueAsString(r.reasons()), w,
                        json.writeValueAsString(hours(f, day))));
            }
        }
        return out;
    }

    /**
     * The whole day, hour by hour, as parallel lists like Open-Meteo's: a third the size of one
     * object per hour, and 16 000 rows a day add up. A precipitation value is the rain of the
     * hour ending at that hour.
     */
    static Map<String, List<Number>> hours(HourlyForecast f, LocalDate day) {
        Map<String, List<Number>> out = new LinkedHashMap<>();
        List<Number> hour = new ArrayList<>(), rain = new ArrayList<>(), temp = new ArrayList<>(), wind = new ArrayList<>();
        for (int k = 0; k < f.time().size(); k++) {
            LocalDateTime t = f.time().get(k);
            if (t.toLocalDate().equals(day)) {
                hour.add(t.getHour());
                rain.add(f.precipitation().get(k));
                temp.add(f.temperature().get(k));
                wind.add(f.windSpeed().get(k));
            }
        }
        out.put("hour", hour);
        out.put("rainMm", rain);
        out.put("temperatureC", temp);
        out.put("windKmh", wind);
        return out;
    }

    /**
     * All rows or none, in one transaction. A row already there (the same refresh appended
     * twice) is left as it was: the archive is never rewritten.
     *
     * @return how many rows were offered
     */
    int append(List<Row> rows) {
        tx.executeWithoutResult(s -> jdbc.batchUpdate(INSERT, rows, 500, (ps, r) -> {
            ps.setLong(1, r.cragId());
            ps.setObject(2, r.day());
            ps.setTimestamp(3, Timestamp.from(r.fetchedAt()));
            ps.setString(4, r.verdict().name());
            ps.setString(5, r.reasons());
            ps.setDouble(6, r.weather().rainLast48hMm());
            ps.setDouble(7, r.weather().rainDuringDayMm());
            ps.setDouble(8, r.weather().maxTemperatureC());
            ps.setDouble(9, r.weather().maxWindKmh());
            ps.setString(10, r.hours());
        }));
        return rows.size();
    }
}
