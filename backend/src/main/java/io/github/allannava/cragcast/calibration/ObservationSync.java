package io.github.allannava.cragcast.calibration;

import io.github.allannava.cragcast.weather.OpenMeteoClient;
import java.util.List;
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
import org.springframework.web.client.RestClient;

/**
 * CG-108: copies data/observations.csv from main into {@code observation}, as HistorySync does
 * for the history branch — at startup and each night, the table replaced whole in one
 * transaction. The CSV stays the source and changes only by a reviewed PR; nothing else writes
 * the table. A failed read or a malformed file keeps the rows that are there.
 */
@Component
@ConditionalOnProperty(name = "cragcast.observations.enabled", havingValue = "true", matchIfMissing = true)
public class ObservationSync {

    static final String PATH = "/data/observations.csv";
    static final String HEADER = "date,crag,rock,observer,note";
    private static final Logger log = LoggerFactory.getLogger(ObservationSync.class);
    private static final String INSERT = """
            INSERT INTO observation (day, crag, rock, observer, note) VALUES (?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING""";

    private final RestClient rest;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    @Autowired
    ObservationSync(@Value("${cragcast.observations.base-url}") String baseUrl, JdbcTemplate jdbc,
                    TransactionTemplate tx) {
        this(RestClient.builder().baseUrl(baseUrl), jdbc, tx);
    }

    ObservationSync(RestClient.Builder builder, JdbcTemplate jdbc, TransactionTemplate tx) {
        this.rest = builder.build();
        this.jdbc = jdbc;
        this.tx = tx;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        syncSafely();
    }

    @Scheduled(cron = "${cragcast.observations.cron:0 40 4 * * *}", zone = OpenMeteoClient.TIMEZONE)
    public void nightly() {
        syncSafely();
    }

    private void syncSafely() {
        try {
            log.info("observations synced: {} rows", sync());
        } catch (RuntimeException e) {
            log.warn("observations not synced, keeping the previous rows: {}", e.toString());
        }
    }

    /** @return how many observations the table holds now */
    int sync() {
        String csv = rest.get().uri(PATH).retrieve().body(String.class);
        List<String> lines = csv == null ? List.of() : csv.lines().toList();
        // An empty or cut answer must not empty the table: the header proves it is the file.
        if (lines.isEmpty() || !lines.getFirst().strip().equals(HEADER)) {
            throw new IllegalStateException(PATH + ": no header " + HEADER);
        }
        return replace(Calibrate.parse(lines));
    }

    /** The table becomes {@code rows}, in one transaction. */
    int replace(List<Observation> rows) {
        tx.executeWithoutResult(s -> {
            jdbc.update("DELETE FROM observation");
            jdbc.batchUpdate(INSERT, rows, 100, (ps, o) -> {
                ps.setObject(1, o.date());
                ps.setString(2, o.crag());
                ps.setString(3, o.rock().name());
                ps.setString(4, o.observer());
                ps.setString(5, o.note());
            });
        });
        return rows.size();
    }
}
