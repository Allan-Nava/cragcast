package io.github.allannava.cragcast.avalanche;

import io.github.allannava.cragcast.weather.OpenMeteoClient;
import java.net.http.HttpClient;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-107: keeps each day's avalanche bulletin in {@code avalanche_bulletin}, one row per
 * micro-region — the facts {@link AvalancheBulletins} reads and the link, never the text. Read
 * at startup and a few times a day; the latest issue of a day replaces that day whole. No
 * bulletin (out of season, before the morning issue, a failed request) writes nothing and
 * keeps what is there.
 */
@Component
@ConditionalOnProperty(name = "cragcast.avalanche.archive.enabled", havingValue = "true", matchIfMissing = true)
public class AvalancheArchive {

    private static final Logger log = LoggerFactory.getLogger(AvalancheArchive.class);
    private static final String INSERT = """
            INSERT INTO avalanche_bulletin (day, region, name, level, ratings, problems, bulletin, fetched_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""";

    private final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private final String baseUrl;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Autowired
    AvalancheArchive(@Value("${cragcast.avalanche.base-url}") String baseUrl, JdbcTemplate jdbc,
                     TransactionTemplate tx, Clock clock) {
        this.baseUrl = baseUrl;
        this.jdbc = jdbc;
        this.tx = tx;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        keepSafely();
    }

    @Scheduled(cron = "${cragcast.avalanche.archive.cron:0 30 9,13,18 * * *}", zone = OpenMeteoClient.TIMEZONE)
    public void daily() {
        keepSafely();
    }

    private void keepSafely() {
        LocalDate today = LocalDate.now(clock);
        try {
            int n = keep(today, AvalancheBulletins.fetch(http, baseUrl, today, json));
            log.info("avalanche bulletin of {}: {} regions kept", today, n);
        } catch (RuntimeException e) {
            log.warn("avalanche bulletin of {} not kept, keeping the previous rows: {}", today, e.toString());
        }
    }

    /**
     * Replaces the day with {@code byRegion}, in one transaction; an empty map touches nothing.
     *
     * @return how many regions were written
     */
    int keep(LocalDate day, Map<String, Avalanche> byRegion) {
        if (byRegion.isEmpty()) {
            return 0;
        }
        Instant now = clock.instant();
        List<Avalanche> rows = byRegion.values().stream()
                .sorted(Comparator.comparing(Avalanche::region)).toList();
        tx.executeWithoutResult(s -> {
            jdbc.update("DELETE FROM avalanche_bulletin WHERE day = ?", day);
            jdbc.batchUpdate(INSERT, rows, 100, (ps, a) -> {
                ps.setObject(1, day);
                ps.setString(2, a.region());
                ps.setString(3, a.name());
                ps.setInt(4, a.level());
                ps.setString(5, json.writeValueAsString(a.ratings()));
                ps.setString(6, json.writeValueAsString(a.problems()));
                ps.setString(7, a.bulletin());
                ps.setTimestamp(8, Timestamp.from(now));
            });
        });
        return rows.size();
    }
}
