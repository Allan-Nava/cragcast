package io.github.allannava.cragcast.avalanche;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The bulletin of 2026-03-01 for Lombardy (IT-25), trimmed to its levels and problem types, and
 * the real micro-regions in data/. The test points are made up, not crags.
 */
class AvalancheTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static String fixture() throws IOException {
        try (var in = AvalancheTest.class.getResourceAsStream("/avalanche/IT-25-2026-03-01.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void readsTheLevelsAndProblemsByRegion() throws IOException {
        Map<String, Avalanche> byRegion = AvalancheBulletins.parse(fixture(), JSON);

        assertThat(byRegion).hasSize(12);
        Avalanche rhaetian = byRegion.get("IT-25-SO-03");
        assertThat(rhaetian.level()).isEqualTo(3);                       // considerable above the treeline
        assertThat(rhaetian.name()).isEqualTo("RETICHE ORIENTALI");
        assertThat(rhaetian.ratings()).contains(new Avalanche.Rating(3, "2000", null, "later"));
        assertThat(rhaetian.problems()).containsExactly("wind_slab", "persistent_weak_layers", "wet_snow");
        assertThat(rhaetian.bulletin()).isEqualTo(AvalancheBulletins.BULLETIN_PAGE);
        Avalanche varese = byRegion.get("IT-25-VA-01");
        assertThat(varese.level()).isEqualTo(1);
        assertThat(varese.ratings()).containsExactly(new Avalanche.Rating(1, null, null, null));   // all day, all heights
    }

    @Test
    void placesPointsInTheMicroRegions() throws IOException {
        AvalancheRegions regions = AvalancheRegions.parse(
                Files.readString(Path.of("../data/avalanche-regions.geojson")), JSON);

        assertThat(regions.size()).isEqualTo(76);                             // CG-120: four providers
        assertThat(regions.regionOf(46.47, 10.37)).contains("IT-25-SO-03");   // Bormio, upper Valtellina
        assertThat(regions.regionOf(45.4642, 9.19)).isEmpty();                // Milan: no bulletin
        // CG-120: Piedmont, Trentino and Alto Adige have their own zones, and their providers.
        assertThat(regions.regionOf(45.97, 7.97)).hasValueSatisfying(id -> assertThat(id).startsWith("IT-21-"));  // Macugnaga
        assertThat(regions.regionOf(46.10, 11.00)).hasValueSatisfying(id -> assertThat(id).startsWith("IT-32-TN-")); // Paganella
        assertThat(regions.regionOf(46.57, 11.67)).hasValueSatisfying(id -> assertThat(id).startsWith("IT-32-BZ-")); // Val Gardena
    }

    @Test
    void eachZoneHasItsProviderAndItsOfficialBulletin() {
        assertThat(AvalancheBulletins.providerOf("IT-25-SO-03").name()).isEqualTo("ARPA Lombardia");
        assertThat(AvalancheBulletins.providerOf("IT-21-TO-01").name()).isEqualTo("AINEVA Piemonte");
        assertThat(AvalancheBulletins.providerOf("IT-32-TN-09").name()).isEqualTo("Euregio — Trentino");
        assertThat(AvalancheBulletins.providerOf("IT-32-BZ-01").page()).isEqualTo("https://avalanche.report/bulletin/latest");
        assertThat(AvalancheBulletins.providerOf("XX-1").name()).isEqualTo("ARPA Lombardia");
        assertThat(AvalancheBulletins.providerOf(null).name()).isEqualTo("ARPA Lombardia");
        var a = new Avalanche("IT-21-TO-01", "Test", 2, java.util.List.of(), java.util.List.of(), "x");
        assertThat(a.provider()).isEqualTo("AINEVA Piemonte");
    }

    @Test
    void ringContainment() {
        double[][] square = {{0, 0}, {10, 0}, {10, 10}, {0, 10}, {0, 0}};
        assertThat(AvalancheRegions.inside(5, 5, square)).isTrue();
        assertThat(AvalancheRegions.inside(15, 5, square)).isFalse();
    }

    @Test
    void aMissingBulletinIsNoBulletin() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] body = fixture().getBytes(StandardCharsets.UTF_8);
        server.createContext("/", ex -> {
            boolean known = ex.getRequestURI().getPath().equals("/2026-03-01/2026-03-01-IT-25.json");
            ex.sendResponseHeaders(known ? 200 : 404, known ? body.length : -1);
            if (known) {
                ex.getResponseBody().write(body);
            }
            ex.close();
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            HttpClient http = HttpClient.newHttpClient();
            assertThat(AvalancheBulletins.fetch(http, base, LocalDate.of(2026, 3, 1), JSON)).hasSize(12);
            assertThat(AvalancheBulletins.fetch(http, base, LocalDate.of(2026, 7, 1), JSON)).isEmpty();
            assertThat(AvalancheBulletins.fetch(http, "http://127.0.0.1:1", LocalDate.of(2026, 3, 1), JSON)).isEmpty();
        } finally {
            server.stop(0);
        }
    }
}
