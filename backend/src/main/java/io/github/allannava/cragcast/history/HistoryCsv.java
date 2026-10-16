package io.github.allannava.cragcast.history;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads one month of the history branch (history/README.md): columns found by their header
 * name, so a column added at the end changes nothing here; quoted fields as Python's csv module
 * writes them. A row with a missing or malformed field is skipped, never guessed.
 */
final class HistoryCsv {

    private HistoryCsv() {
    }

    record Parsed(List<HistoryRow> rows, int skipped) {
    }

    static Parsed parse(String text) {
        List<List<String>> records = records(text);
        if (records.isEmpty()) {
            return new Parsed(List.of(), 0);
        }
        Map<String, Integer> col = new HashMap<>();
        List<String> header = records.getFirst();
        for (int i = 0; i < header.size(); i++) {
            col.put(header.get(i).strip(), i);
        }
        List<HistoryRow> rows = new ArrayList<>();
        int skipped = 0;
        for (List<String> r : records.subList(1, records.size())) {
            if (r.size() == 1 && r.getFirst().isBlank()) {
                continue;
            }
            try {
                rows.add(new HistoryRow(
                        LocalDate.parse(field(r, col, "date")),
                        required(field(r, col, "crag_id")),
                        required(field(r, col, "crag_name")),
                        blankToNull(field(r, col, "province")),
                        required(field(r, col, "verdict")),
                        Double.parseDouble(field(r, col, "rain_last48h_mm")),
                        Double.parseDouble(field(r, col, "rain_day_mm")),
                        Double.parseDouble(field(r, col, "max_temp_c")),
                        Double.parseDouble(field(r, col, "max_wind_kmh")),
                        field(r, col, "reasons"),
                        Instant.parse(field(r, col, "generated_at"))));
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        return new Parsed(rows, skipped);
    }

    private static String field(List<String> r, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        if (i == null) {
            throw new IllegalArgumentException("no column " + name);
        }
        return i < r.size() ? r.get(i) : "";
    }

    private static String required(String s) {
        if (s.isBlank()) {
            throw new IllegalArgumentException("empty field");
        }
        return s;
    }

    private static String blankToNull(String s) {
        return s.isBlank() ? null : s;
    }

    /** RFC 4180: commas, quoted fields with doubled quotes inside, CRLF or LF line ends. */
    static List<List<String>> records(String text) {
        List<List<String>> out = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder f = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    f.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    f.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                record.add(f.toString());
                f.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                record.add(f.toString());
                f.setLength(0);
                out.add(record);
                record = new ArrayList<>();
            } else {
                f.append(c);
            }
        }
        if (f.length() > 0 || !record.isEmpty()) {
            record.add(f.toString());
            out.add(record);
        }
        return out;
    }
}
