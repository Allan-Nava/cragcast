package io.github.allannava.cragcast.calibration;

import io.github.allannava.cragcast.conditions.ConditionsScorer;
import io.github.allannava.cragcast.conditions.Weather;
import io.github.allannava.cragcast.crag.Aspect;
import io.github.allannava.cragcast.weather.OpenMeteoClient;
import io.github.allannava.cragcast.weather.WeatherReducer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-27: replays observed days. For each row of data/observations.csv, fetches that day's weather
 * from Open-Meteo's Historical Forecast API, scores it as the live page would, and prints a
 * Markdown report: each day, the verdict against the rock, and a summary. The thresholds change
 * only when this report shows why, with a test for the day that moved them.
 *
 * <pre>java -cp ... io.github.allannava.cragcast.calibration.Calibrate data/observations.csv data/crags.json</pre>
 */
public final class Calibrate {

    static final String HISTORICAL = "https://historical-forecast-api.open-meteo.com";

    private Calibrate() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("usage: Calibrate <observations.csv> <crags.json>");
            System.exit(2);
        }
        List<Observation> observations = parse(Files.readAllLines(Path.of(args[0])));
        Crags crags = JsonMapper.builder().build().readValue(Files.readString(Path.of(args[1])), Crags.class);
        OpenMeteoClient weather = OpenMeteoClient.create(HISTORICAL);
        ConditionsScorer scorer = new ConditionsScorer();

        List<Row> rows = new ArrayList<>();
        for (Observation o : observations) {
            Crag c = crags.find(o.crag());
            var forecast = weather.hourlyBetween(c.latitude(), c.longitude(), o.date().minusDays(3), o.date());
            Weather w = WeatherReducer.reduce(forecast, o.date());
            var result = scorer.score(c.aspect(), w);
            rows.add(new Row(o, c, w, result, Agreement.of(o.rock(), result.verdict())));
        }
        System.out.print(report(rows));
    }

    /** CSV with a header: date,crag,rock,observer,note — rock is dry, damp or wet. */
    static List<Observation> parse(List<String> lines) {
        List<Observation> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split(",", 5);
            if (f.length < 4) {
                throw new IllegalArgumentException("line " + (i + 1) + ": expected date,crag,rock,observer[,note]");
            }
            out.add(new Observation(LocalDate.parse(f[0].strip()), f[1].strip(),
                    Observation.Rock.valueOf(f[2].strip().toUpperCase(Locale.ROOT)), f[3].strip(),
                    f.length == 5 ? f[4].strip() : ""));
        }
        return out;
    }

    static String report(List<Row> rows) {
        StringBuilder md = new StringBuilder("| Date | Crag | Aspect | Rock seen | Verdict | Rain 48 h | Rain day | Agreement |\n")
                .append("|---|---|---|---|---|---|---|---|\n");
        Map<Agreement, Integer> counts = new EnumMap<>(Agreement.class);
        for (Row r : rows) {
            counts.merge(r.agreement(), 1, Integer::sum);
            md.append(String.format(Locale.ROOT, "| %s | %s | %s | %s | %s | %.1f mm | %.1f mm | %s |%n",
                    r.o().date(), r.c().name(), r.c().aspect() == null ? "not mapped" : r.c().aspect(),
                    r.o().rock().name().toLowerCase(Locale.ROOT), r.result().verdict(),
                    r.w().rainLast48hMm(), r.w().rainDuringDayMm(), r.agreement()));
        }
        md.append("\n").append(rows.size()).append(" observed days: ");
        List<String> parts = new ArrayList<>();
        for (Agreement a : Agreement.values()) {
            parts.add(a + " " + counts.getOrDefault(a, 0));
        }
        return md.append(String.join(", ", parts)).append("\n").toString();
    }

    record Row(Observation o, Crag c, Weather w, ConditionsScorer.Result result, Agreement agreement) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Crag(String id, String name, double latitude, double longitude, Aspect aspect) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Crags(List<Crag> crags) {
        Crag find(String key) {
            List<Crag> hits = crags.stream().filter(c -> c.id().equals(key) || c.name().equalsIgnoreCase(key)).toList();
            if (hits.size() != 1) {
                throw new IllegalArgumentException(hits.isEmpty() ? "no crag " + key
                        : key + " names " + hits.size() + " crags: use the id (osm-…) instead");
            }
            return hits.getFirst();
        }
    }
}
