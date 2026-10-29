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
 * CG-11: the avalanche bulletins, as the EAWS publish them in CAAML v6 JSON on
 * static.avalanche.report — one file per day and provider, {@code <day>/<day>-<file>.json},
 * issued the morning of that day: Lombardy's (ARPA Lombardia) and, since CG-120, Piedmont's
 * (AINEVA Piemonte) and Trentino-Alto Adige's (the Euregio, one file per province). None states
 * a licence: only the facts are kept and shown, never the text, each with its provider's link
 * (ADR 0007, ADR 0027). Out of season, or before the morning issue, there is no file (404); a
 * provider's missing file never holds back the others'.
 */
public final class AvalancheBulletins {

    public static final String BASE_URL = "https://static.avalanche.report/eaws_bulletins";
    /** The official bulletin, linked next to every level shown. */
    public static final String BULLETIN_PAGE =
            "https://www.arpalombardia.it/temi-ambientali/neve-e-ghiacciai/bollettino-neve-e-valanghe/";
    static final String REGION = "IT-25";

    /** CG-120: one EAWS file of the day, who issues it, and where its official bulletin is read. */
    public record Provider(String file, String name, String page) {
    }

    public static final List<Provider> PROVIDERS = List.of(
            new Provider(REGION, "ARPA Lombardia", BULLETIN_PAGE),
            new Provider("IT-21", "AINEVA Piemonte", "https://bollettini.aineva.it/bulletin/latest"),
            new Provider("IT-32-TN", "Euregio — Trentino", "https://avalanche.report/bulletin/latest"),
            new Provider("IT-32-BZ", "Euregio — Alto Adige", "https://avalanche.report/bulletin/latest"));

    /** The provider of a micro-region, by the longest file prefix of its id; Lombardy's otherwise. */
    public static Provider providerOf(String regionId) {
        Provider best = null;
        for (Provider p : PROVIDERS) {
            if (regionId != null && regionId.startsWith(p.file() + "-")
                    && (best == null || p.file().length() > best.file().length())) {
                best = p;
            }
        }
        return best == null ? PROVIDERS.getFirst() : best;
    }
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
                Provider provider = providerOf(id);
                byRegion.put(id, new Avalanche(id, text(region.path("name")), max, List.copyOf(ratings),
                        List.copyOf(problems), provider.page(), provider.name()));
            }
        }
        return byRegion;
    }

    /**
     * The day's bulletins from {@code baseUrl}, every provider's file, by micro-region. A missing
     * file is the normal case out of season; any other failure is reported on stderr and treated
     * the same way, so the page is never held back by a bulletin.
     */
    public static Map<String, Avalanche> fetch(HttpClient http, String baseUrl, LocalDate day, JsonMapper json) {
        Map<String, Avalanche> all = new HashMap<>();
        for (Provider p : PROVIDERS) {
            all.putAll(fetch(http, baseUrl, day, p.file(), json));
        }
        return all;
    }

    private static Map<String, Avalanche> fetch(HttpClient http, String baseUrl, LocalDate day, String file, JsonMapper json) {
        URI uri = URI.create(baseUrl + "/" + day + "/" + day + "-" + file + ".json");
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
