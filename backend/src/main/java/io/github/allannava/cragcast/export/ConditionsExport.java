package io.github.allannava.cragcast.export;

import io.github.allannava.cragcast.conditions.ConditionsScorer;
import io.github.allannava.cragcast.conditions.Verdict;
import io.github.allannava.cragcast.conditions.Weather;
import io.github.allannava.cragcast.crag.Aspect;
import io.github.allannava.cragcast.weather.HourlyForecast;
import io.github.allannava.cragcast.weather.OpenMeteoClient;
import io.github.allannava.cragcast.weather.WeatherReducer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/**
 * The serverless version: reads data/crags.json, fetches the forecast for every crag, scores
 * today, tomorrow and the day after, and writes the JSON the static page on GitHub Pages reads. Run hourly by
 * .github/workflows/pages.yml; no database, no Spring context.
 *
 * <pre>java -cp ... io.github.allannava.cragcast.export.ConditionsExport data/crags.json site/data/conditions.json</pre>
 */
public final class ConditionsExport {

    static final ZoneId ZONE = ZoneId.of("Europe/Rome");
    /** Places per Open-Meteo request, to keep the URL short. */
    static final int BATCH = 50;

    private ConditionsExport() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 && args.length != 3) {
            System.err.println("usage: ConditionsExport <crags.json> <conditions.json> [<api dir>]");
            System.exit(2);
        }
        JsonMapper json = JsonMapper.builder().build();
        CragsFile crags = json.readValue(Files.readString(Path.of(args[0])), CragsFile.class);

        OpenMeteoClient weather = OpenMeteoClient.create(
                System.getenv().getOrDefault("OPEN_METEO_URL", "https://api.open-meteo.com"));
        List<HourlyForecast> forecasts = new ArrayList<>();
        for (int i = 0; i < crags.crags().size(); i += BATCH) {
            List<double[]> places = crags.crags().subList(i, Math.min(i + BATCH, crags.crags().size()))
                    .stream().map(c -> new double[] {c.latitude(), c.longitude()}).toList();
            forecasts.addAll(weather.hourly(places));
        }

        // CG-33: today, tomorrow and the day after — all three inside the 3 forecast days fetched.
        LocalDate today = LocalDate.now(ZONE);
        Output out = build(crags, forecasts, List.of(today, today.plusDays(1), today.plusDays(2)), Instant.now());
        Path target = Path.of(args[1]);
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.writeString(target, json.writeValueAsString(out));
        if (args.length == 3) {
            // CG-32: the static API, one index and one file per crag, next to the page.
            Path api = Path.of(args[2]);
            for (Map.Entry<String, Object> f : api(out).entrySet()) {
                Path file = api.resolve(f.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, json.writeValueAsString(f.getValue()));
            }
        }
        long go = out.crags().stream().filter(c -> c.days().get(today.plusDays(1).toString()).verdict() == Verdict.GO).count();
        System.out.printf("%d crags scored for %s to %s; %d go tomorrow%n",
                out.crags().size(), today, today.plusDays(2), go);
    }

    /** The pure part: crags and their forecasts in, the page's JSON out. */
    static Output build(CragsFile crags, List<HourlyForecast> forecasts, List<LocalDate> days, Instant now) {
        if (forecasts.size() != crags.crags().size()) {
            throw new IllegalArgumentException(forecasts.size() + " forecasts for " + crags.crags().size() + " crags");
        }
        ConditionsScorer scorer = new ConditionsScorer();
        List<CragOut> out = new ArrayList<>();
        for (int i = 0; i < forecasts.size(); i++) {
            Crag c = crags.crags().get(i);
            Map<String, DayOut> byDay = new LinkedHashMap<>();
            for (LocalDate day : days) {
                Weather w = WeatherReducer.reduce(forecasts.get(i), day);
                ConditionsScorer.Result r = scorer.score(c.aspect(), w);
                byDay.put(day.toString(), new DayOut(r.verdict(), r.reasons(), w, hours(forecasts.get(i), day)));
            }
            out.add(new CragOut(c.id(), c.name(), c.latitude(), c.longitude(), c.aspect(), c.sourceUrl(),
                    c.details() == null ? Map.of() : c.details(), c.elevation(),
                    c.alsoMapped() == null ? List.of() : c.alsoMapped(),
                    c.municipality(), c.province(), c.provinceCode(), byDay));
        }
        return new Output(now.toString(), ZONE.getId(), days.stream().map(LocalDate::toString).toList(),
                crags.licence(), "Weather: Open-Meteo.com (CC BY 4.0)", crags.elevationSource(), out);
    }

    /**
     * CG-32: the static API, as relative path → document. {@code crags.json} lists every crag with
     * its verdict per day and the path of its own file; {@code crags/<id>.json} holds one crag
     * whole (details, weather, hours). Version 1: fields are only ever added, never renamed or
     * removed — a breaking change goes to /api/v2.
     */
    static Map<String, Object> api(Output out) {
        Map<String, Object> files = new LinkedHashMap<>();
        Meta meta = new Meta(1, out.generatedAt(), out.timezone(), out.days(), out.cragData(), out.weatherData(),
                out.elevationData(), "https://github.com/Allan-Nava/cragcast#api");
        List<IndexEntry> index = new ArrayList<>();
        for (CragOut c : out.crags()) {
            Map<String, Verdict> verdicts = new LinkedHashMap<>();
            c.days().forEach((day, d) -> verdicts.put(day, d.verdict()));
            index.add(new IndexEntry(c.id(), c.name(), c.municipality(), c.provinceCode(), c.latitude(), c.longitude(),
                    c.aspect(), c.elevation(), verdicts, "crags/" + c.id() + ".json"));
            files.put("crags/" + c.id() + ".json", new CragDocument(meta, c));
        }
        files.put("crags.json", new Index(meta, index));
        return files;
    }

    record Meta(int version, String generatedAt, String timezone, List<String> days, String cragData,
                String weatherData, String elevationData, String docs) {
    }

    record IndexEntry(String id, String name, String municipality, String provinceCode, double latitude,
                      double longitude, Aspect aspect, Integer elevation, Map<String, Verdict> verdicts, String url) {
    }

    record Index(Meta meta, List<IndexEntry> crags) {
    }

    record CragDocument(Meta meta, CragOut crag) {
    }

    /** CG-24: the day's hours from 06:00 to 21:00, for the chart in the crag's panel. */
    static List<HourOut> hours(HourlyForecast f, LocalDate day) {
        List<HourOut> out = new ArrayList<>();
        for (int k = 0; k < f.time().size(); k++) {
            var t = f.time().get(k);
            if (t.toLocalDate().equals(day) && t.getHour() >= FIRST_HOUR && t.getHour() <= LAST_HOUR) {
                out.add(new HourOut(t.getHour(), f.precipitation().get(k), f.temperature().get(k), f.windSpeed().get(k)));
            }
        }
        return out;
    }

    static final int FIRST_HOUR = 6;
    static final int LAST_HOUR = 21;

    record HourOut(int hour, double rainMm, double temperatureC, double windKmh) {
    }

    record CragsFile(String region, String source, String licence, String elevationSource, List<Crag> crags) {
    }

    /** {@code details}: the climbing tags OSM has for the crag (grades, length, rock…), passed through. */
    /** {@code alsoMapped}: OSM links of duplicates folded into this crag by tools/osm_crags.py. */
    record Crag(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl,
                Map<String, Object> details, Integer elevation, List<String> alsoMapped,
                String municipality, String province, String provinceCode) {
    }

    record DayOut(Verdict verdict, List<String> reasons, Weather weather, List<HourOut> hours) {
    }

    record CragOut(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl,
                   Map<String, Object> details, Integer elevation, List<String> alsoMapped,
                   String municipality, String province, String provinceCode, Map<String, DayOut> days) {
    }

    record Output(String generatedAt, String timezone, List<String> days, String cragData, String weatherData,
                  String elevationData, List<CragOut> crags) {
    }
}
