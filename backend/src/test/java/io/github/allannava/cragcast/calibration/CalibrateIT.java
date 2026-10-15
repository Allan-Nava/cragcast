package io.github.allannava.cragcast.calibration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.allannava.cragcast.testing.FakeHttp;
import io.github.allannava.cragcast.testing.FakeHttp.Answer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/**
 * CG-49: the calibration replay as one — observations and crags from files, the real client
 * against a fake Historical Forecast API on localhost, the Markdown report. Made-up days and
 * crags, not observations.
 */
class CalibrateIT {

    @TempDir
    Path dir;

    /** The hours between start_date and end_date; it rains 2 mm an hour on {@code wetDay}. */
    private static String history(LocalDate start, LocalDate end, LocalDate wetDay) {
        List<String> time = new ArrayList<>();
        List<Double> rain = new ArrayList<>(), temp = new ArrayList<>(), wind = new ArrayList<>();
        for (var t = start.atStartOfDay(); !t.toLocalDate().isAfter(end); t = t.plusHours(1)) {
            time.add(t.toString());
            rain.add(t.toLocalDate().equals(wetDay) ? 2.0 : 0.0);
            temp.add(16.0);
            wind.add(5.0);
        }
        return JsonMapper.builder().build().writeValueAsString(Map.of("timezone", "Europe/Rome",
                "hourly", Map.of("time", time, "precipitation", rain, "temperature_2m", temp, "wind_speed_10m", wind)));
    }

    @Test
    void replaysObservedDaysIntoAReport() throws Exception {
        Files.writeString(dir.resolve("crags.json"), """
                {"crags":[{"id":"osm-node-1","name":"Test Wall","latitude":45.8,"longitude":9.4,"aspect":"S"},
                          {"id":"osm-node-2","name":"Shady Test Wall","latitude":45.9,"longitude":9.5,"aspect":null}]}""");
        Files.writeString(dir.resolve("obs.csv"), """
                date,crag,rock,observer,note
                # a comment line
                2026-05-10,osm-node-1,dry,tester,
                2026-05-11,shady test wall,wet,tester,rained the day before

                """);
        LocalDate wet = LocalDate.parse("2026-05-10");
        try (var api = new FakeHttp(uri -> {
            var q = FakeHttp.query(uri);
            return Answer.ok(history(LocalDate.parse(q.get("start_date")), LocalDate.parse(q.get("end_date")), wet));
        })) {
            String report = Calibrate.run(dir.resolve("obs.csv"), dir.resolve("crags.json"), api.url());
            assertThat(api.asked()).hasSize(2);
            assertThat(FakeHttp.query(api.asked().getFirst())).containsEntry("start_date", "2026-05-07").containsEntry("end_date", "2026-05-10");
            assertThat(report).startsWith("| Date | Crag | Aspect | Rock seen | Verdict |");
            assertThat(report).contains("| 2026-05-10 | Test Wall | S | dry | NO_GO |");      // it rained that day
            assertThat(report).contains("| 2026-05-11 | Shady Test Wall | not mapped | wet | MAYBE |");  // and so the next
            assertThat(report).contains("2 observed days: ");
        }
    }

    @Test
    void refusesAnAmbiguousOrUnknownCrag() throws Exception {
        Files.writeString(dir.resolve("crags.json"), """
                {"crags":[{"id":"osm-node-1","name":"Twin","latitude":45,"longitude":9},
                          {"id":"osm-node-2","name":"Twin","latitude":46,"longitude":9}]}""");
        Files.writeString(dir.resolve("obs.csv"), "date,crag,rock,observer\n2026-05-10,twin,dry,tester\n");
        assertThatThrownBy(() -> Calibrate.run(dir.resolve("obs.csv"), dir.resolve("crags.json"), "http://127.0.0.1:1"))
                .hasMessageContaining("names 2 crags");
        Files.writeString(dir.resolve("obs.csv"), "date,crag,rock,observer\n2026-05-10,nowhere,dry,tester\n");
        assertThatThrownBy(() -> Calibrate.run(dir.resolve("obs.csv"), dir.resolve("crags.json"), "http://127.0.0.1:1"))
                .hasMessageContaining("no crag nowhere");
    }
}
