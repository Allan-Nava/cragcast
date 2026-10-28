package io.github.allannava.cragcast.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.allannava.cragcast.avalanche.Avalanche;
import io.github.allannava.cragcast.avalanche.AvalancheRegions;
import io.github.allannava.cragcast.ferrata.FerrataForecast;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The pure part of CG-114's export; a made-up via ferrata and forecast. */
class FerrateExportTest {

    private static final LocalDate JUNE_2 = LocalDate.of(2026, 6, 2);

    private static FerrataForecast fine() {
        int n = 120;
        List<LocalDateTime> t = new ArrayList<>();
        List<Integer> day = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            t.add(LocalDateTime.of(2026, 5, 31, 0, 0).plusHours(i));
            day.add(t.get(i).getHour() >= 6 && t.get(i).getHour() < 20 ? 1 : 0);
        }
        List<Double> zero = Collections.nCopies(n, 0.0);
        return new FerrataForecast(t, zero, Collections.nCopies(n, 15.0), Collections.nCopies(n, 10.0),
                Collections.nCopies(n, 20.0), Collections.nCopies(n, 1), zero, zero, day);
    }

    private static FerrateExport.FerrateFile file(Integer scaleMax) {
        return new FerrateExport.FerrateFile("Test", "test", "l", "e", List.of(new FerrateExport.Ferrata(
                "osm-way-1", "Ferrata Test", "https://example.org/w1", null,
                new FerrateExport.Point(45.9, 9.4, 900), new FerrateExport.Point(45.91, 9.41, 1300), 400,
                scaleMax == null ? null : 1, scaleMax, null)));
    }

    @Test
    void placedAtTheStartWithTheTopsWeatherAndHours() {
        var out = FerrateExport.build(file(3), List.of(fine()), List.of(JUNE_2), AvalancheRegions.none(), Map.of());

        var f = out.getFirst();
        assertThat(f.latitude()).isEqualTo(45.9);
        assertThat(f.pieces()).isEmpty();
        assertThat(f.scaleDropped()).isEmpty();
        var day = f.days().get("2026-06-02");
        assertThat(day.verdict().name()).isEqualTo("GO");
        assertThat(day.weather().maxTemperatureC()).isEqualTo(15.0);
        assertThat(day.hours()).hasSize(16).first().extracting(ConditionsExport.HourOut::hour).isEqualTo(6);
        assertThat(day.avalanche()).isNull();
    }

    @Test
    void aForecastMissingIsRefused() {
        assertThatThrownBy(() -> FerrateExport.build(file(3), List.of(), List.of(JUNE_2), AvalancheRegions.none(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void thePagesDataLeavesFerrateOutWhenThereAreNone() {
        JsonMapper json = JsonMapper.builder().build();
        var empty = new ConditionsExport.Output("t", "Europe/Rome", List.of(), "c", "w", "e", "a", List.of(), List.of());
        assertThat(json.writeValueAsString(empty)).doesNotContain("ferrate");
        var one = new ConditionsExport.Output("t", "Europe/Rome", List.of(), "c", "w", "e", "a", List.of(),
                FerrateExport.build(file(null), List.of(fine()), List.of(JUNE_2), AvalancheRegions.none(),
                        Map.<LocalDate, Map<String, Avalanche>>of()));
        assertThat(json.writeValueAsString(one)).contains("\"ferrate\":[{\"id\":\"osm-way-1\"").contains("\"scaleMax\":null");
    }
}
