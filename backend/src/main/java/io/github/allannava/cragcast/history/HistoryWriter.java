package io.github.allannava.cragcast.history;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one month's rows, replacing each day whole: history.yml's rerun replaces a day's rows,
 * so a crag missing from the new run must not survive from the old one. A bean of its own so
 * that the transaction applies when HistorySync calls it.
 */
@Component
class HistoryWriter {

    private final CragHistoryRepository history;

    HistoryWriter(CragHistoryRepository history) {
        this.history = history;
    }

    @Transactional
    public int replaceDays(List<HistoryRow> rows) {
        Set<LocalDate> days = rows.stream().map(HistoryRow::day).collect(Collectors.toSet());
        if (days.isEmpty()) {
            return 0;
        }
        history.deleteByDayIn(days);
        history.flush();
        history.saveAll(rows.stream().map(CragHistory::new).toList());
        return rows.size();
    }
}
