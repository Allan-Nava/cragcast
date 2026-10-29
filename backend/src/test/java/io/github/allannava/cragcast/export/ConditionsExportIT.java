package io.github.allannava.cragcast.export;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.testing.FakeHttp;
import io.github.allannava.cragcast.testing.FakeHttp.Answer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-49: the hourly export as one — ConditionsExport's own entry point, the real Open-Meteo
 * client and avalanche fetch over HTTP to fake servers on localhost, and every file it writes
 * read back: the page's data, the static API, the share pages. Made-up crags, not real places.
 */
class ConditionsExportIT {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final LocalDate today = LocalDate.now(ConditionsExport.ZONE);

    @TempDir
    Path dir;

    /** Five days of hours (two past, three ahead) per place; place 2 rains from 08 to 18 tomorrow. */
    private String forecasts(int places) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int p = 0; p < places; p++) {
            List<String> time = new ArrayList<>();
            List<Double> rain = new ArrayList<>(), temp = new ArrayList<>(), wind = new ArrayList<>();
            for (int h = 0; h < 5 * 24; h++) {
                var t = today.minusDays(2).atStartOfDay().plusHours(h);
                time.add(t.toString());
                boolean wet = p == 1 && t.toLocalDate().equals(today.plusDays(1)) && t.getHour() >= 8 && t.getHour() <= 18;
                rain.add(wet ? 1.0 : 0.0);
                temp.add(18.0);
                wind.add(8.0);
            }
            out.add(Map.of("timezone", "Europe/Rome", "hourly",
                    Map.of("time", time, "precipitation", rain, "temperature_2m", temp, "wind_speed_10m", wind)));
        }
        return JSON.writeValueAsString(out);
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = ConditionsExportIT.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void writesThePagesDataTheApiAndTheSharePages() throws Exception {
        Files.writeString(dir.resolve("crags.json"), """
                {"region":"Test","source":"test","licence":"test licence","elevationSource":"test dem","crags":[
                 {"id":"osm-node-1","name":"Dry Test Wall","latitude":46.47,"longitude":10.37,"aspect":"S",
                  "sourceUrl":"https://example.org/1","details":{"gradeMax":"7a"},"elevation":1400,
                  "municipality":"Testville","province":"Testshire","provinceCode":"TT","region":"Lombardia"},
                 {"id":"osm-way-2","name":"Wet Test Wall","latitude":45.5,"longitude":9.2,"aspect":null,
                  "sourceUrl":"https://example.org/2","details":{}},
                 {"id":"osm-node-3","name":"Third <Test> Wall","latitude":45.6,"longitude":9.3,"aspect":"N",
                  "sourceUrl":"https://example.org/3","details":{},"region":"Piemonte"}]}""");
        // One avalanche micro-region, around the first crag only; its id is one the bulletin names.
        Files.writeString(dir.resolve("avalanche-regions.geojson"), """
                {"features":[{"properties":{"id":"IT-25-SO-03"},"geometry":{"type":"MultiPolygon",
                 "coordinates":[[[[10.3,46.4],[10.5,46.4],[10.5,46.6],[10.3,46.6],[10.3,46.4]]]]}}]}""");
        String bulletin = fixture("/avalanche/IT-25-2026-03-01.json");

        try (var meteo = new FakeHttp(uri -> Answer.ok(forecasts(FakeHttp.query(uri).get("latitude").split(",").length)));
             var avalanche = new FakeHttp(uri -> uri.getPath().equals("/" + today + "/" + today + "-IT-25.json")
                     ? Answer.ok(bulletin) : Answer.NOT_FOUND)) {
            int code = ConditionsExport.run(new String[] {dir.resolve("crags.json").toString(),
                    dir.resolve("site/data/conditions.json").toString(), dir.resolve("site/api/v1").toString(),
                    dir.resolve("site").toString()}, Map.of("OPEN_METEO_URL", meteo.url(),
                    "AVALANCHE_URL", avalanche.url(), "SITE_URL", "https://example.org/cc"));
            assertThat(code).isZero();
            assertThat(meteo.asked()).hasSize(1);                                  // three crags, one batch
            assertThat(FakeHttp.query(meteo.asked().getFirst())).containsEntry("past_days", "2").containsEntry("forecast_days", "3");
            assertThat(avalanche.asked()).hasSize(3);                              // one bulletin per day
        }

        JsonNode data = JSON.readTree(Files.readString(dir.resolve("site/data/conditions.json")));
        String tomorrow = today.plusDays(1).toString();
        assertThat(data.path("days").size()).isEqualTo(3);
        // CG-43: Lombardy's crags (and one without a region) here, Piedmont's in its own file.
        assertThat(data.path("crags").size()).isEqualTo(2);
        assertThat(data.path("regions").get(0).path("file").asString()).isEqualTo("conditions-piemonte.json");
        assertThat(data.path("regions").get(0).path("count").asInt()).isEqualTo(1);
        JsonNode piemonte = JSON.readTree(Files.readString(dir.resolve("site/data/conditions-piemonte.json")));
        assertThat(piemonte.path("crags").get(0).path("id").asString()).isEqualTo("osm-node-3");
        assertThat(piemonte.has("regions")).isFalse();
        JsonNode dry = data.path("crags").get(0), wet = data.path("crags").get(1);
        assertThat(dry.path("days").path(tomorrow).path("verdict").asString()).isEqualTo("GO");
        assertThat(wet.path("days").path(tomorrow).path("verdict").asString()).isEqualTo("NO_GO");
        // Rain is stamped at the end of its hour, so 08:00-18:00 takes the values of 09..18: 10 of the 11 wet hours.
        assertThat(wet.path("days").path(tomorrow).path("weather").path("rainDuringDayMm").asDouble()).isEqualTo(10.0);
        assertThat(dry.path("days").path(tomorrow).path("hours").size()).isEqualTo(16);
        assertThat(dry.path("days").path(today.toString()).path("avalanche").path("region").asString()).isEqualTo("IT-25-SO-03");
        assertThat(dry.path("days").path(tomorrow).has("avalanche")).isFalse();     // no bulletin for tomorrow
        assertThat(wet.path("days").path(today.toString()).has("avalanche")).isFalse();  // outside every region
        assertThat(data.path("avalancheData").asString()).contains("ARPA Lombardia");

        JsonNode index = JSON.readTree(Files.readString(dir.resolve("site/api/v1/crags.json")));
        assertThat(index.path("meta").path("version").asInt()).isEqualTo(1);
        assertThat(index.path("crags").get(1).path("verdicts").path(tomorrow).asString()).isEqualTo("NO_GO");
        assertThat(index.path("crags").get(0).path("url").asString()).isEqualTo("crags/osm-node-1.json");
        // CG-43: the region, added at the end; null where the file has none. The API keeps every crag.
        assertThat(index.path("crags").get(0).path("region").asString()).isEqualTo("Lombardia");
        assertThat(index.path("crags").get(1).path("region").isNull()).isTrue();
        assertThat(index.path("crags").get(2).path("region").asString()).isEqualTo("Piemonte");
        JsonNode one = JSON.readTree(Files.readString(dir.resolve("site/api/v1/crags/osm-node-1.json")));
        assertThat(one.path("crag").path("details").path("gradeMax").asString()).isEqualTo("7a");

        String share = Files.readString(dir.resolve("site/c/osm-way-2.html"));
        assertThat(share).contains("<meta property=\"og:url\" content=\"https://example.org/cc/c/osm-way-2.html\">");
        assertThat(share).contains(": no (pioggia prevista durante il giorno)");
        assertThat(Files.readString(dir.resolve("site/c/osm-node-3.html"))).contains("Third &lt;Test&gt; Wall").doesNotContain("<Test>");
    }

    @Test
    void refusesWrongArguments() throws Exception {
        assertThat(ConditionsExport.run(new String[] {"only-one"}, Map.of())).isEqualTo(2);
    }

    @Test
    void stillWritesWhenTheBulletinServerIsDown() throws Exception {
        Files.writeString(dir.resolve("crags.json"), """
                {"licence":"l","elevationSource":"e","crags":[{"id":"osm-node-1","name":"Test Wall","latitude":46.47,
                 "longitude":10.37,"aspect":"S","sourceUrl":"https://example.org/1","details":{}}]}""");
        Files.writeString(dir.resolve("avalanche-regions.geojson"), """
                {"features":[{"properties":{"id":"IT-25-SO-03"},"geometry":{"type":"MultiPolygon",
                 "coordinates":[[[[10.3,46.4],[10.5,46.4],[10.5,46.6],[10.3,46.6],[10.3,46.4]]]]}}]}""");
        try (var meteo = new FakeHttp(uri -> Answer.ok(forecasts(1)));
             var avalanche = new FakeHttp(uri -> new Answer(500, "boom"))) {
            int code = ConditionsExport.run(new String[] {dir.resolve("crags.json").toString(),
                    dir.resolve("out.json").toString()}, Map.of("OPEN_METEO_URL", meteo.url(), "AVALANCHE_URL", avalanche.url()));
            assertThat(code).isZero();
        }
        assertThat(Files.readString(dir.resolve("out.json"))).doesNotContain("\"avalanche\"");
    }
}
