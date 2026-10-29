package io.github.allannava.cragcast.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.allannava.cragcast.avalanche.Avalanche;
import io.github.allannava.cragcast.avalanche.AvalancheBulletins;
import io.github.allannava.cragcast.avalanche.AvalancheRegions;
import io.github.allannava.cragcast.conditions.ConditionsScorer;
import io.github.allannava.cragcast.conditions.Verdict;
import io.github.allannava.cragcast.conditions.Weather;
import io.github.allannava.cragcast.crag.Aspect;
import io.github.allannava.cragcast.ferrata.FerrataClient;
import io.github.allannava.cragcast.weather.HourlyForecast;
import io.github.allannava.cragcast.weather.OpenMeteoClient;
import io.github.allannava.cragcast.weather.WeatherReducer;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;

/**
 * The serverless version: reads data/crags.json, fetches the forecast for every crag, scores
 * today, tomorrow and the day after, adds the avalanche bulletin where it covers the crag (CG-11),
 * and writes the JSON the static page on GitHub Pages reads. Run hourly by
 * .github/workflows/pages.yml; no database, no Spring context.
 *
 * <pre>java -cp ... io.github.allannava.cragcast.export.ConditionsExport data/crags.json site/data/conditions.json</pre>
 */
public final class ConditionsExport {

    static final ZoneId ZONE = ZoneId.of("Europe/Rome");
    /**
     * Places per Open-Meteo request: few requests an hour (CG-43 brought about a thousand crags),
     * and a URL still well under the server's limit.
     */
    static final int BATCH = 100;
    /**
     * CG-43: Open-Meteo's free tier counts every place, 600 a minute: a pause between requests of
     * {@link #BATCH} keeps a run near 400 a minute. OPEN_METEO_PAUSE_MS overrides it (tests: 0).
     */
    static final long PAUSE_MS = 15_000;

    private ConditionsExport() {
    }

    public static void main(String[] args) throws Exception {
        int code = run(args, System.getenv());
        if (code != 0) {
            System.exit(code);
        }
    }

    /**
     * The whole export, with the environment passed in (OPEN_METEO_URL, AVALANCHE_URL, SITE_URL)
     * so the integration test can point it at fake servers. Returns the exit code.
     */
    static int run(String[] args, Map<String, String> env) throws Exception {
        if (args.length < 2 || args.length > 4) {
            System.err.println("usage: ConditionsExport <crags.json> <conditions.json> [<api dir> [<site dir>]]");
            return 2;
        }
        JsonMapper json = JsonMapper.builder().build();
        CragsFile crags = json.readValue(Files.readString(Path.of(args[0])), CragsFile.class);

        OpenMeteoClient weather = OpenMeteoClient.create(
                env.getOrDefault("OPEN_METEO_URL", "https://api.open-meteo.com"));
        long pause = Long.parseLong(env.getOrDefault("OPEN_METEO_PAUSE_MS", Long.toString(PAUSE_MS)));
        List<HourlyForecast> forecasts = new ArrayList<>();
        for (int i = 0; i < crags.crags().size(); i += BATCH) {
            if (i > 0) {
                Thread.sleep(pause);
            }
            List<double[]> places = crags.crags().subList(i, Math.min(i + BATCH, crags.crags().size()))
                    .stream().map(c -> new double[] {c.latitude(), c.longitude()}).toList();
            forecasts.addAll(weather.hourly(places));
        }

        // CG-33: today, tomorrow and the day after — all three inside the 3 forecast days fetched.
        LocalDate today = LocalDate.now(ZONE);
        List<LocalDate> days = List.of(today, today.plusDays(1), today.plusDays(2));

        // CG-11: the avalanche bulletin by micro-region, for the crags inside one. The regions
        // sit next to the crags (data/avalanche-regions.geojson); without the file, no bulletin.
        Path regionsFile = Path.of(args[0]).toAbsolutePath().resolveSibling("avalanche-regions.geojson");
        AvalancheRegions regions = Files.exists(regionsFile)
                ? AvalancheRegions.parse(Files.readString(regionsFile), json) : AvalancheRegions.none();
        Map<LocalDate, Map<String, Avalanche>> bulletins = new LinkedHashMap<>();
        if (regions.size() > 0) {
            HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            String base = env.getOrDefault("AVALANCHE_URL", AvalancheBulletins.BASE_URL);
            for (LocalDate day : days) {
                bulletins.put(day, AvalancheBulletins.fetch(http, base, day, json));
            }
        }

        // CG-117: the sectors and single routes, from the OSM objects file next to the crags.
        Path climbingFile = Path.of(args[0]).toAbsolutePath().resolveSibling("osm-climbing.json");
        OsmClimbing climbing = Files.exists(climbingFile)
                ? OsmClimbing.parse(Files.readString(climbingFile), json) : OsmClimbing.none();

        // CG-114: the via ferratas next to the crags, scored at their tops. They never hold the
        // crags back: without the file, or when their forecast fails, the page has none.
        List<FerrateExport.FerrataOut> ferrate = List.of();
        Path ferrateFile = Path.of(args[0]).toAbsolutePath().resolveSibling("ferrate.json");
        if (Files.exists(ferrateFile)) {
            try {
                FerrateExport.FerrateFile file = json.readValue(Files.readString(ferrateFile), FerrateExport.FerrateFile.class);
                FerrataClient client = FerrataClient.create(env.getOrDefault("OPEN_METEO_URL", "https://api.open-meteo.com"));
                Thread.sleep(pause);                       // the tops count against the same minute
                ferrate = FerrateExport.build(file, FerrateExport.fetch(client, file, BATCH), days, regions, bulletins);
            } catch (RuntimeException e) {
                System.err.println("via ferratas skipped: " + e);
            }
        }

        Output out = build(crags, forecasts, days, Instant.now(), regions, bulletins, climbing, ferrate);
        Path target = Path.of(args[1]);
        Files.createDirectories(target.toAbsolutePath().getParent());
        // CG-43: the page's data by region; the API and the share pages below take every crag.
        for (Map.Entry<String, Output> f : split(out).entrySet()) {
            Path file = f.getKey().equals("conditions.json") ? target : target.resolveSibling(f.getKey());
            Files.writeString(file, json.writeValueAsString(f.getValue()));
        }
        if (args.length >= 3) {
            // CG-32: the static API, one index and one file per crag, next to the page.
            Path api = Path.of(args[2]);
            for (Map.Entry<String, Object> f : api(out).entrySet()) {
                Path file = api.resolve(f.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, json.writeValueAsString(f.getValue()));
            }
        }
        if (args.length == 4) {
            // CG-45: a page per crag with the link preview, next to the page.
            String site = env.getOrDefault("SITE_URL", "https://allan-nava.github.io/cragcast/");
            for (Map.Entry<String, String> f : SharePages.pages(out, site.endsWith("/") ? site : site + "/").entrySet()) {
                Path file = Path.of(args[3]).resolve(f.getKey());
                Files.createDirectories(file.getParent());
                Files.writeString(file, f.getValue());
            }
        }
        long go = out.crags().stream().filter(c -> c.days().get(today.plusDays(1).toString()).verdict() == Verdict.GO).count();
        long avalanche = out.crags().stream().filter(c -> c.days().values().stream().anyMatch(d -> d.avalanche() != null)).count();
        System.out.printf("%d crags scored for %s to %s; %d go tomorrow; %d with an avalanche bulletin; %d via ferratas%n",
                out.crags().size(), today, today.plusDays(2), go, avalanche, out.ferrate().size());
        return 0;
    }

    static Output build(CragsFile crags, List<HourlyForecast> forecasts, List<LocalDate> days, Instant now) {
        return build(crags, forecasts, days, now, AvalancheRegions.none(), Map.of());
    }

    static Output build(CragsFile crags, List<HourlyForecast> forecasts, List<LocalDate> days, Instant now,
                        AvalancheRegions regions, Map<LocalDate, Map<String, Avalanche>> bulletins) {
        return build(crags, forecasts, days, now, regions, bulletins, OsmClimbing.none());
    }

    static Output build(CragsFile crags, List<HourlyForecast> forecasts, List<LocalDate> days, Instant now,
                        AvalancheRegions regions, Map<LocalDate, Map<String, Avalanche>> bulletins,
                        OsmClimbing climbing) {
        return build(crags, forecasts, days, now, regions, bulletins, climbing, List.of());
    }

    /**
     * The pure part: crags, their forecasts, the day's bulletins and the OSM objects in, the page's
     * JSON out; {@code ferrate} (CG-114) already scored by {@link FerrateExport}.
     */
    static Output build(CragsFile crags, List<HourlyForecast> forecasts, List<LocalDate> days, Instant now,
                        AvalancheRegions regions, Map<LocalDate, Map<String, Avalanche>> bulletins,
                        OsmClimbing climbing, List<FerrateExport.FerrataOut> ferrate) {
        if (forecasts.size() != crags.crags().size()) {
            throw new IllegalArgumentException(forecasts.size() + " forecasts for " + crags.crags().size() + " crags");
        }
        ConditionsScorer scorer = new ConditionsScorer();
        List<CragOut> out = new ArrayList<>();
        for (int i = 0; i < forecasts.size(); i++) {
            Crag c = crags.crags().get(i);
            Optional<String> region = regions.regionOf(c.latitude(), c.longitude());
            Map<String, DayOut> byDay = new LinkedHashMap<>();
            for (LocalDate day : days) {
                Weather w = WeatherReducer.reduce(forecasts.get(i), day);
                ConditionsScorer.Result r = scorer.score(c.aspect(), w);
                Avalanche avalanche = region.map(id -> bulletins.getOrDefault(day, Map.of()).get(id)).orElse(null);
                byDay.put(day.toString(), new DayOut(r.verdict(), r.reasons(), w, hours(forecasts.get(i), day), avalanche));
            }
            out.add(new CragOut(c.id(), c.name(), c.latitude(), c.longitude(), c.aspect(), c.sourceUrl(),
                    c.details() == null ? Map.of() : c.details(), c.elevation(),
                    c.alsoMapped() == null ? List.of() : c.alsoMapped(),
                    c.municipality(), c.province(), c.provinceCode(), byDay,
                    climbing.sectors(c.sectors()), climbing.routes(c.id()), c.region()));
        }
        return new Output(now.toString(), ZONE.getId(), days.stream().map(LocalDate::toString).toList(),
                crags.licence(), "Weather: Open-Meteo.com (CC BY 4.0)", crags.elevationSource(), AVALANCHE_DATA, out, ferrate);
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
                out.elevationData(), "https://github.com/Allan-Nava/cragcast#api", out.avalancheData());
        List<IndexEntry> index = new ArrayList<>();
        for (CragOut c : out.crags()) {
            Map<String, Verdict> verdicts = new LinkedHashMap<>();
            c.days().forEach((day, d) -> verdicts.put(day, d.verdict()));
            index.add(new IndexEntry(c.id(), c.name(), c.municipality(), c.provinceCode(), c.latitude(), c.longitude(),
                    c.aspect(), c.elevation(), verdicts, "crags/" + c.id() + ".json", c.region()));
            files.put("crags/" + c.id() + ".json", new CragDocument(meta, c));
        }
        files.put("crags.json", new Index(meta, index));
        // CG-115: the via ferratas, as their own files — new files, so v1 holds. Absent when there are none.
        if (!out.ferrate().isEmpty()) {
            List<FerrataIndexEntry> ferrate = new ArrayList<>();
            for (FerrateExport.FerrataOut f : out.ferrate()) {
                Map<String, Verdict> verdicts = new LinkedHashMap<>();
                f.days().forEach((day, d) -> verdicts.put(day, d.verdict()));
                ferrate.add(new FerrataIndexEntry(f.id(), f.name(), f.latitude(), f.longitude(), f.top().elevation(),
                        f.scaleMin(), f.scaleMax(), verdicts, "ferrate/" + f.id() + ".json",
                        f.municipality(), f.provinceCode(), f.region()));
                files.put("ferrate/" + f.id() + ".json", new FerrataDocument(meta, f));
            }
            files.put("ferrate.json", new FerrateIndex(meta, ferrate));
        }
        return files;
    }

    /** CG-115: one via ferrata in api/v1/ferrate.json — at its start; {@code topElevation} from the DEM. */
    /** {@code municipality}, {@code provinceCode}, {@code region}: CG-121, added at the end. */
    record FerrataIndexEntry(String id, String name, double latitude, double longitude, Integer topElevation,
                             Integer scaleMin, Integer scaleMax, Map<String, Verdict> verdicts, String url,
                             String municipality, String provinceCode, String region) {
    }

    record FerrateIndex(Meta meta, List<FerrataIndexEntry> ferrate) {
    }

    record FerrataDocument(Meta meta, FerrateExport.FerrataOut ferrata) {
    }

    static final String AVALANCHE_DATA = "Avalanche danger: bulletins of ARPA Lombardia (" + AvalancheBulletins.BULLETIN_PAGE
            + "), AINEVA Piemonte (https://bollettini.aineva.it) and the Euregio for Trentino and Alto Adige "
            + "(https://avalanche.report), through the EAWS (static.avalanche.report); micro-regions: EAWS (CC0)";

    record Meta(int version, String generatedAt, String timezone, List<String> days, String cragData,
                String weatherData, String elevationData, String docs, String avalancheData) {
    }

    record IndexEntry(String id, String name, String municipality, String provinceCode, double latitude,
                      double longitude, Aspect aspect, Integer elevation, Map<String, Verdict> verdicts, String url,
                      String region) {
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
    /** {@code sectors}: CG-104, the OSM ids of the sectors folded into this crag. */
    /** {@code region}: CG-43, the crag's Italian region from OSM's boundaries; null in older files. */
    record Crag(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl,
                Map<String, Object> details, Integer elevation, List<String> alsoMapped,
                String municipality, String province, String provinceCode, List<String> sectors, String region) {

        Crag(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl,
             Map<String, Object> details, Integer elevation, List<String> alsoMapped,
             String municipality, String province, String provinceCode, List<String> sectors) {
            this(id, name, latitude, longitude, aspect, sourceUrl, details, elevation, alsoMapped, municipality, province,
                    provinceCode, sectors, null);
        }
    }

    /** CG-117: one sector of the crag, with its own aspect and grades. */
    record SectorOut(String id, String name, Aspect aspect, String gradeMin, String gradeMax) {
    }

    /** CG-117: one single route of the crag; {@code sector} is the id of its sector, or null. */
    record RouteOut(String id, String name, String grade, Double length, Integer bolts, Integer pitches,
                    String sector) {
    }

    /** {@code avalanche}: CG-11, present only when the day's bulletin covers the crag. */
    record DayOut(Verdict verdict, List<String> reasons, Weather weather, List<HourOut> hours,
                  @JsonInclude(JsonInclude.Include.NON_NULL) Avalanche avalanche) {
    }

    /** {@code region}: CG-43, added at the end as the API v1 allows. */
    record CragOut(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl,
                   Map<String, Object> details, Integer elevation, List<String> alsoMapped,
                   String municipality, String province, String provinceCode, Map<String, DayOut> days,
                   List<SectorOut> sectors, List<RouteOut> routes, String region) {

        CragOut(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl,
                Map<String, Object> details, Integer elevation, List<String> alsoMapped,
                String municipality, String province, String provinceCode, Map<String, DayOut> days,
                List<SectorOut> sectors, List<RouteOut> routes) {
            this(id, name, latitude, longitude, aspect, sourceUrl, details, elevation, alsoMapped, municipality, province,
                    provinceCode, days, sectors, routes, null);
        }
    }

    /**
     * {@code ferrate}: CG-114, left out when there are none, so data without via ferratas reads as before.
     * {@code regions}: CG-43, the other regions' files next to this one, left out when there are none.
     */
    record Output(String generatedAt, String timezone, List<String> days, String cragData, String weatherData,
                  String elevationData, String avalancheData, List<CragOut> crags,
                  @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FerrateExport.FerrataOut> ferrate,
                  @JsonInclude(JsonInclude.Include.NON_EMPTY) List<RegionFile> regions) {

        /** Data without via ferratas, as before CG-114. */
        Output(String generatedAt, String timezone, List<String> days, String cragData, String weatherData,
               String elevationData, String avalancheData, List<CragOut> crags) {
            this(generatedAt, timezone, days, cragData, weatherData, elevationData, avalancheData, crags, List.of());
        }

        Output(String generatedAt, String timezone, List<String> days, String cragData, String weatherData,
               String elevationData, String avalancheData, List<CragOut> crags, List<FerrateExport.FerrataOut> ferrate) {
            this(generatedAt, timezone, days, cragData, weatherData, elevationData, avalancheData, crags, ferrate, List.of());
        }

        Output with(List<CragOut> crags, List<FerrateExport.FerrataOut> ferrate, List<RegionFile> regions) {
            return new Output(generatedAt, timezone, days, cragData, weatherData, elevationData, avalancheData, crags,
                    ferrate, regions);
        }
    }

    /** CG-43: another region's crags, in {@code file} next to conditions.json. */
    record RegionFile(String name, int count, String file) {
    }

    /** CG-43: the region the page shows first, whose crags (and the via ferratas) stay in conditions.json. */
    static final String MAIN_REGION = "Lombardia";

    /**
     * CG-43: the page's data split by region, as file name → document. conditions.json keeps
     * Lombardy's crags (and those without a region), the via ferratas and the list of the other
     * files; each other region gets conditions-&lt;slug&gt;.json with its crags alone, fetched by the
     * page when that region is chosen. One file for every crag was too heavy (Lighthouse 0.73).
     */
    static Map<String, Output> split(Output out) {
        Map<String, List<CragOut>> byRegion = new LinkedHashMap<>();
        List<CragOut> main = new ArrayList<>();
        for (CragOut c : out.crags()) {
            if (c.region() == null || c.region().equals(MAIN_REGION)) {
                main.add(c);
            } else {
                byRegion.computeIfAbsent(c.region(), r -> new ArrayList<>()).add(c);
            }
        }
        // CG-121: the via ferratas go with their region's crags, the same way.
        Map<String, List<FerrateExport.FerrataOut>> ferrateByRegion = new LinkedHashMap<>();
        List<FerrateExport.FerrataOut> mainFerrate = new ArrayList<>();
        for (FerrateExport.FerrataOut f : out.ferrate()) {
            if (f.region() == null || f.region().equals(MAIN_REGION)) {
                mainFerrate.add(f);
            } else {
                ferrateByRegion.computeIfAbsent(f.region(), r -> new ArrayList<>()).add(f);
                byRegion.computeIfAbsent(f.region(), r -> new ArrayList<>());
            }
        }
        Map<String, Output> files = new LinkedHashMap<>();
        List<RegionFile> regions = new ArrayList<>();
        byRegion.forEach((region, crags) -> {
            String file = "conditions-" + slug(region) + ".json";
            regions.add(new RegionFile(region, crags.size(), file));
            files.put(file, out.with(crags, ferrateByRegion.getOrDefault(region, List.of()), List.of()));
        });
        Map<String, Output> all = new LinkedHashMap<>();
        all.put("conditions.json", out.with(main, mainFerrate, regions));
        all.putAll(files);
        return all;
    }

    /** "Trentino-Alto Adige" → "trentino-alto-adige". */
    static String slug(String region) {
        String folded = java.text.Normalizer.normalize(region, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return folded.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
