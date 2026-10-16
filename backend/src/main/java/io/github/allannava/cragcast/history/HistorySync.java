package io.github.allannava.cragcast.history;

import io.github.allannava.cragcast.weather.OpenMeteoClient;
import java.time.Clock;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Copies the history branch into crag_history (CG-101, ADR 0016): every month since the first
 * at startup, the last two each night after history.yml has run. It reads GitHub, nothing
 * writes to it — the CSVs stay the source, and a lost database is rebuilt by a restart.
 */
@Component
@ConditionalOnProperty(name = "cragcast.history.enabled", havingValue = "true", matchIfMissing = true)
public class HistorySync {

    private static final Logger log = LoggerFactory.getLogger(HistorySync.class);

    private final RestClient rest;
    private final HistoryWriter writer;
    private final Clock clock;
    private final YearMonth since;

    @Autowired
    HistorySync(@Value("${cragcast.history.base-url}") String baseUrl,
                @Value("${cragcast.history.since}") String since,
                HistoryWriter writer, Clock clock) {
        this(RestClient.builder().baseUrl(baseUrl), YearMonth.parse(since), writer, clock);
    }

    HistorySync(RestClient.Builder builder, YearMonth since, HistoryWriter writer, Clock clock) {
        this.rest = builder.build();
        this.since = since;
        this.writer = writer;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        syncSafely(since);
    }

    // history.yml runs at 20:30 UTC and GitHub may start it late: 23:50, and again at 05:50.
    @Scheduled(cron = "${cragcast.history.cron:0 50 23,5 * * *}", zone = OpenMeteoClient.TIMEZONE)
    public void nightly() {
        syncSafely(YearMonth.now(clock).minusMonths(1));
    }

    private void syncSafely(YearMonth from) {
        try {
            int rows = sync(from);
            log.info("history synced from {}: {} crag-days", from, rows);
        } catch (RuntimeException e) {
            // Keep what is there; the next run tries again.
            log.warn("history sync failed, keeping the previous rows: {}", e.toString());
        }
    }

    /** @return how many crag-day rows were written, from the month {@code from} to this one */
    int sync(YearMonth from) {
        int rows = 0;
        for (YearMonth m = from.isBefore(since) ? since : from; !m.isAfter(YearMonth.now(clock)); m = m.plusMonths(1)) {
            String csv = month(m);
            if (csv == null) {
                continue;
            }
            HistoryCsv.Parsed parsed = HistoryCsv.parse(csv);
            if (parsed.skipped() > 0) {
                log.warn("history {}: {} malformed rows skipped", m, parsed.skipped());
            }
            rows += writer.replaceDays(parsed.rows());
        }
        return rows;
    }

    /** The month's CSV, or null when there is none yet (a month history.yml has not reached). */
    private String month(YearMonth m) {
        try {
            return rest.get()
                    .uri("/{year}/{month}.csv", m.getYear(), String.format("%02d", m.getMonthValue()))
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }
}
