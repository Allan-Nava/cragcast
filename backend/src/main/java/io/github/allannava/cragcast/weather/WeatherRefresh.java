package io.github.allannava.cragcast.weather;

import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every hour (and once at startup), fetches the forecast for every crag — one Open-Meteo
 * request per 50 crags — and stores the reduced weather of today, tomorrow and the day after.
 * The conditions endpoint reads only the table, so a slow or failing Open-Meteo never makes a
 * user wait: the API serves the last good hour.
 */
@Component
@ConditionalOnProperty(name = "cragcast.refresh.enabled", havingValue = "true", matchIfMissing = true)
public class WeatherRefresh {

    static final int BATCH = 50;
    static final int DAYS = 3;
    private static final Logger log = LoggerFactory.getLogger(WeatherRefresh.class);

    private final CragRepository crags;
    private final CragWeatherRepository weather;
    private final OpenMeteoClient openMeteo;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    /** The forecasts of the last refresh, by crag slug, for {@link ForecastsRefreshed}. */
    private Map<String, HourlyForecast> last = Map.of();
    /** When the last refresh ran: crag_weather's fetched_at, and the archive's (CG-106). */
    private Instant lastAt;

    public WeatherRefresh(CragRepository crags, CragWeatherRepository weather, OpenMeteoClient openMeteo, Clock clock,
                          ApplicationEventPublisher events) {
        this.crags = crags;
        this.weather = weather;
        this.openMeteo = openMeteo;
        this.clock = clock;
        this.events = events;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        refreshSafely();
    }

    @Scheduled(cron = "${cragcast.refresh.cron:0 7 * * * *}", zone = OpenMeteoClient.TIMEZONE)
    public void hourly() {
        refreshSafely();
    }

    private void refreshSafely() {
        try {
            int rows = refresh();
            log.info("weather refreshed: {} crag-days", rows);
        } catch (RuntimeException e) {
            // Keep the last good hour; the next run tries again.
            log.warn("weather refresh failed, keeping the previous data: {}", e.toString());
            return;
        }
        try {
            // CG-110: outside the refresh's transaction; a failure there leaves the table as written.
            events.publishEvent(new ForecastsRefreshed(last, lastAt));
        } catch (RuntimeException e) {
            log.warn("page data not rebuilt, keeping the previous one: {}", e.toString());
        }
    }

    /** @return how many crag-day rows were written */
    @Transactional
    public int refresh() {
        List<Crag> all = crags.findAll();
        Instant now = clock.instant();
        LocalDate today = LocalDate.now(clock);
        int rows = 0;
        Map<String, HourlyForecast> bySlug = new LinkedHashMap<>();
        for (int i = 0; i < all.size(); i += BATCH) {
            List<Crag> batch = all.subList(i, Math.min(i + BATCH, all.size()));
            List<HourlyForecast> forecasts = openMeteo.hourly(
                    batch.stream().map(c -> new double[] {c.getLatitude(), c.getLongitude()}).toList());
            for (int j = 0; j < batch.size(); j++) {
                bySlug.put(batch.get(j).getSlug(), forecasts.get(j));
                for (int d = 0; d < DAYS; d++) {
                    LocalDate day = today.plusDays(d);
                    var w = WeatherReducer.reduce(forecasts.get(j), day);
                    var key = new CragWeather.Key(batch.get(j).getId(), day);
                    // Saved explicitly: refresh() is also called from inside this class, where
                    // @Transactional does not apply and a changed entity would not be flushed.
                    var row = weather.findById(key).orElse(null);
                    if (row == null) {
                        row = new CragWeather(batch.get(j).getId(), day, w, now);
                    } else {
                        row.set(w, now);
                    }
                    weather.save(row);
                    rows++;
                }
            }
        }
        last = bySlug;
        lastAt = now;
        return rows;
    }
}
