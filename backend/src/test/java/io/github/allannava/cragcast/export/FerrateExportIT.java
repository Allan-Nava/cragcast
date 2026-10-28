package io.github.allannava.cragcast.export;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.testing.FakeHttp;
import io.github.allannava.cragcast.testing.FakeHttp.Answer;
import java.net.URI;
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
 * CG-114: the hourly export with data/ferrate.json next to the crags — the via ferratas scored at
 * their tops in their own Open-Meteo request, written under {@code ferrate}, kept out of the API v1;
 * and the crags still written when the via ferratas' forecast fails. Made-up places.
 */
class FerrateExportIT {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final LocalDate today = LocalDate.now(ConditionsExport.ZONE);

    @TempDir
    Path dir;

    /**
     * Five days of hours per place, dry and mild; with {@code ferrata}, the top's extra lists too,
     * and place 2 has a thunderstorm at 15:00 tomorrow.
     */
    private String forecasts(int places, boolean ferrata) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int p = 0; p < places; p++) {
            List<String> time = new ArrayList<>();
            List<Double> zero = new ArrayList<>(), temp = new ArrayList<>(), wind = new ArrayList<>();
            List<Integer> code = new ArrayList<>(), day = new ArrayList<>();
            for (int h = 0; h < 5 * 24; h++) {
                var t = today.minusDays(2).atStartOfDay().plusHours(h);
                time.add(t.toString());
                zero.add(0.0);
                temp.add(14.0);
                wind.add(8.0);
                code.add(p == 1 && t.toLocalDate().equals(today.plusDays(1)) && t.getHour() == 15 ? 95 : 1);
                day.add(t.getHour() >= 7 && t.getHour() < 18 ? 1 : 0);
            }
            Map<String, Object> hourly = ferrata
                    ? Map.of("time", time, "precipitation", zero, "temperature_2m", temp, "wind_speed_10m", wind,
                             "wind_gusts_10m", wind, "weather_code", code, "cape", zero, "snowfall", zero, "is_day", day)
                    : Map.of("time", time, "precipitation", zero, "temperature_2m", temp, "wind_speed_10m", wind);
            out.add(Map.of("timezone", "Europe/Rome", "hourly", hourly));
        }
        return JSON.writeValueAsString(out);
    }

    private static boolean forTops(URI uri) {
        return FakeHttp.query(uri).containsKey("elevation");
    }

    private void files() throws Exception {
        Files.writeString(dir.resolve("crags.json"), """
                {"licence":"l","elevationSource":"e","crags":[{"id":"osm-node-1","name":"Test Wall","latitude":45.9,
                 "longitude":9.4,"aspect":"S","sourceUrl":"https://example.org/1","details":{}}]}""");
        Files.writeString(dir.resolve("ferrate.json"), """
                {"licence":"l","elevationSource":"e","ferrate":[
                 {"id":"osm-way-10","name":"Ferrata Test","sourceUrl":"https://example.org/w10",
                  "pieces":["https://example.org/w10"],"start":{"latitude":45.9,"longitude":9.4,"elevation":900},
                  "top":{"latitude":45.91,"longitude":9.41,"elevation":1300},"lengthM":400,"scaleMin":2,"scaleMax":3,
                  "scaleDropped":[]},
                 {"id":"osm-relation-20","name":"Ferrata Temporale","sourceUrl":"https://example.org/r20",
                  "pieces":[],"start":{"latitude":46.0,"longitude":9.5,"elevation":1500},
                  "top":{"latitude":46.01,"longitude":9.51,"elevation":2100},"lengthM":800,"scaleMin":null,
                  "scaleMax":null,"scaleDropped":["2+"]}]}""");
    }

    @Test
    void theViaFerratasAreScoredAtTheirTopsAndKeptOutOfTheApi() throws Exception {
        files();
        try (var meteo = new FakeHttp(uri -> Answer.ok(forecasts(FakeHttp.query(uri).get("latitude").split(",").length, forTops(uri))));
             var avalanche = new FakeHttp(uri -> Answer.NOT_FOUND)) {
            int code = ConditionsExport.run(new String[] {dir.resolve("crags.json").toString(),
                    dir.resolve("site/data/conditions.json").toString(), dir.resolve("site/api/v1").toString()},
                    Map.of("OPEN_METEO_URL", meteo.url(), "AVALANCHE_URL", avalanche.url()));
            assertThat(code).isZero();
            assertThat(meteo.asked()).hasSize(2);                 // the crags, then the tops
            Map<String, String> tops = FakeHttp.query(meteo.asked().get(1));
            assertThat(tops).containsEntry("latitude", "45.91,46.01").containsEntry("elevation", "1300.0,2100.0");
            assertThat(tops.get("hourly")).contains("weather_code", "cape", "is_day");
        }
        JsonNode data = JSON.readTree(Files.readString(dir.resolve("site/data/conditions.json")));
        String tomorrow = today.plusDays(1).toString();
        JsonNode fine = data.path("ferrate").get(0), storm = data.path("ferrate").get(1);
        assertThat(data.path("ferrate").size()).isEqualTo(2);
        assertThat(fine.path("latitude").asDouble()).isEqualTo(45.9);           // placed at the start
        assertThat(fine.path("top").path("elevation").asInt()).isEqualTo(1300);
        assertThat(fine.path("scaleMax").asInt()).isEqualTo(3);
        assertThat(fine.path("days").path(tomorrow).path("verdict").asString()).isEqualTo("GO");
        assertThat(fine.path("days").path(tomorrow).path("hours").size()).isEqualTo(16);
        assertThat(storm.path("days").path(tomorrow).path("verdict").asString()).isEqualTo("NO_GO");
        assertThat(storm.path("days").path(tomorrow).path("reasons").get(0).asString()).isEqualTo("thunderstorm forecast");
        assertThat(storm.path("scaleDropped").get(0).asString()).isEqualTo("2+");
        assertThat(Files.readString(dir.resolve("site/api/v1/crags.json"))).doesNotContain("Ferrata");
    }

    @Test
    void aFailedForecastForTheTopsLeavesThemOutAndTheCragsIn() throws Exception {
        files();
        try (var meteo = new FakeHttp(uri -> forTops(uri) ? new Answer(500, "busy") : Answer.ok(forecasts(1, false)));
             var avalanche = new FakeHttp(uri -> Answer.NOT_FOUND)) {
            int code = ConditionsExport.run(new String[] {dir.resolve("crags.json").toString(), dir.resolve("out.json").toString()},
                    Map.of("OPEN_METEO_URL", meteo.url(), "AVALANCHE_URL", avalanche.url()));
            assertThat(code).isZero();
        }
        JsonNode data = JSON.readTree(Files.readString(dir.resolve("out.json")));
        assertThat(data.path("crags").size()).isEqualTo(1);
        assertThat(data.has("ferrate")).isFalse();
    }
}
