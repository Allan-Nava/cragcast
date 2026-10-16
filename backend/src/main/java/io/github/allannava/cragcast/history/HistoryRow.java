package io.github.allannava.cragcast.history;

import java.time.Instant;
import java.time.LocalDate;

/** One line of the history CSV: a crag's verdict on one day and the weather it came from. */
record HistoryRow(LocalDate day, String cragSlug, String cragName, String province, String verdict,
                  double rainLast48hMm, double rainDayMm, double maxTempC, double maxWindKmh,
                  String reasons, Instant generatedAt) {
}
