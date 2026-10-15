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
 * today and tomorrow, and writes the JSON the static page on GitHub Pages reads. Run hourly by
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
        if (args.length != 2) {
            System.err.println("usage: ConditionsExport <crags.json> <conditions.json>");
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

        LocalDate today = LocalDate.now(ZONE);
        Output out = build(crags, forecasts, List.of(today, today.plusDays(1)), Instant.now());
        Path target = Path.of(args[1]);
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.writeString(target, json.writeValueAsString(out));
        long go = out.crags().stream().filter(c -> c.days().get(today.plusDays(1).toString()).verdict() == Verdict.GO).count();
        System.out.printf("%d crags scored for %s and %s; %d go tomorrow%n",
                out.crags().size(), today, today.plusDays(1), go);
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
                byDay.put(day.toString(), new DayOut(r.verdict(), r.reasons(), w));
            }
            out.add(new CragOut(c.id(), c.name(), c.latitude(), c.longitude(), c.aspect(), c.sourceUrl(),
                    c.details() == null ? Map.of() : c.details(), c.elevation(), byDay));
        }
        return new Output(now.toString(), ZONE.getId(), days.stream().map(LocalDate::toString).toList(),
                crags.licence(), "Weather: Open-Meteo.com (CC BY 4.0)", crags.elevationSource(), out);
    }

    record CragsFile(String region, String source, String licence, String elevationSource, List<Crag> crags) {
    }

    /** {@code details}: the climbing tags OSM has for the crag (grades, length, rock…), passed through. */
    record Crag(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl,
                Map<String, Object> details, Integer elevation) {
    }

    record DayOut(Verdict verdict, List<String> reasons, Weather weather) {
    }

    record CragOut(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl,
                   Map<String, Object> details, Integer elevation, Map<String, DayOut> days) {
    }

    record Output(String generatedAt, String timezone, List<String> days, String cragData, String weatherData,
                  String elevationData, List<CragOut> crags) {
    }
}
