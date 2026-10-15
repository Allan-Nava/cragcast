package io.github.allannava.cragcast.avalanche;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-11: Lombardy's avalanche bulletin (ARPA Lombardia), as the EAWS publish it in CAAML v6
 * JSON on static.avalanche.report — one file per day, {@code <day>/<day>-IT-25.json}, issued
 * the morning of that day. Out of season, or before the morning issue, there is no file (404)
 * and no crag gets an avalanche line.
 */
public final class AvalancheBulletins {

    public static final String BASE_URL = "https://static.avalanche.report/eaws_bulletins";
    /** The official bulletin, linked next to every level shown. */
    public static final String BULLETIN_PAGE =
            "https://www.arpalombardia.it/temi-ambientali/neve-e-ghiacciai/bollettino-neve-e-valanghe/";
    static final String REGION = "IT-25";
    private static final Map<String, Integer> LEVEL =
            Map.of("low", 1, "moderate", 2, "considerable", 3, "high", 4, "very_high", 5);

    private AvalancheBulletins() {
    }

    /** One day's bulletin by micro-region id; empty when there is none. */
    public static Map<String, Avalanche> parse(String body, JsonMapper json) {
        Map<String, Avalanche> byRegion = new HashMap<>();
        for (JsonNode b : json.readTree(body).path("bulletins")) {
            List<Avalanche.Rating> ratings = new ArrayList<>();
            for (JsonNode r : b.path("dangerRatings")) {
                Integer level = LEVEL.get(r.path("mainValue").asString());
                if (level == null) {
                    continue;                       // no_rating, no_snow
                }
                String period = r.path("validTimePeriod").asString(null);
                ratings.add(new Avalanche.Rating(level, text(r.path("elevation").path("lowerBound")),
                        text(r.path("elevation").path("upperBound")), "all_day".equals(period) ? null : period));
            }
            if (ratings.isEmpty()) {
                continue;
            }
            int max = ratings.stream().mapToInt(Avalanche.Rating::level).max().orElseThrow();
            Set<String> problems = new LinkedHashSet<>();
            for (JsonNode p : b.path("avalancheProblems")) {
                problems.add(p.path("problemType").asString());
            }
            for (JsonNode region : b.path("regions")) {
                String id = region.path("regionID").asString();
                byRegion.put(id, new Avalanche(id, text(region.path("name")), max, List.copyOf(ratings),
                        List.copyOf(problems), BULLETIN_PAGE));
            }
        }
        return byRegion;
    }

    /**
     * The day's bulletin from {@code baseUrl}. A missing file is the normal case out of season;
     * any other failure is reported on stderr and treated the same way, so the hourly page is
     * never held back by the bulletin.
     */
    public static Map<String, Avalanche> fetch(HttpClient http, String baseUrl, LocalDate day, JsonMapper json) {
        URI uri = URI.create(baseUrl + "/" + day + "/" + day + "-" + REGION + ".json");
        try {
            HttpResponse<String> res = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404 || res.statusCode() == 403) {
                return Map.of();
            }
            if (res.statusCode() != 200) {
                System.err.println("avalanche bulletin " + uri + ": HTTP " + res.statusCode() + ", skipped");
                return Map.of();
            }
            return parse(res.body(), json);
        } catch (IOException | RuntimeException e) {
            System.err.println("avalanche bulletin " + uri + ": " + e + ", skipped");
            return Map.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of();
        }
    }

    private static String text(JsonNode n) {
        return n.isMissingNode() || n.isNull() ? null : n.asString();
    }
}
