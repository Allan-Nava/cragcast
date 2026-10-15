package io.github.allannava.cragcast.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.allannava.cragcast.conditions.Verdict;
import io.github.allannava.cragcast.crag.Aspect;
import io.github.allannava.cragcast.weather.HourlyForecast;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Synthetic crags and forecasts: made-up names and numbers, not real places. */
class ConditionsExportTest {

    private static HourlyForecast dryDays() {
        List<LocalDateTime> t = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            t.add(LocalDateTime.of(2026, 6, 1, 0, 0).plusHours(i));
        }
        return new HourlyForecast(t, new ArrayList<>(Collections.nCopies(120, 0.0)),
                new ArrayList<>(Collections.nCopies(120, 18.0)), new ArrayList<>(Collections.nCopies(120, 8.0)));
    }

    private static ConditionsExport.CragsFile twoCrags() {
        return new ConditionsExport.CragsFile("Test", "test", "test licence", "test elevation", List.of(
                new ConditionsExport.Crag("t-1", "Sunny Test Wall", 45.0, 9.0, Aspect.S, "https://example.org/1",
                        java.util.Map.of("gradeMin", "5a", "gradeMax", "7b"), 820,
                        List.of("https://example.org/1-twin")),
                new ConditionsExport.Crag("t-2", "Unmapped Test Wall", 45.1, 9.1, null, "https://example.org/2", null, null, null)));
    }

    @Test
    void scoresEveryCragForEveryDay() {
        var days = List.of(LocalDate.of(2026, 6, 3), LocalDate.of(2026, 6, 4));
        var out = ConditionsExport.build(twoCrags(), List.of(dryDays(), dryDays()), days, Instant.EPOCH);

        assertThat(out.days()).containsExactly("2026-06-03", "2026-06-04");
        assertThat(out.crags()).hasSize(2);
        var sunny = out.crags().get(0).days().get("2026-06-04");
        assertThat(sunny.verdict()).isEqualTo(Verdict.GO);
        assertThat(sunny.weather().maxTemperatureC()).isEqualTo(18.0);
        // 06:00 to 21:00 of the day: 16 hours, in order.
        assertThat(sunny.hours()).hasSize(16);
        assertThat(sunny.hours().getFirst().hour()).isEqualTo(6);
        assertThat(sunny.hours().getLast().hour()).isEqualTo(21);
        assertThat(out.cragData()).isEqualTo("test licence");
        assertThat(out.crags().get(0).details()).containsEntry("gradeMax", "7b");
        assertThat(out.crags().get(1).details()).isEmpty();
        assertThat(out.crags().get(0).elevation()).isEqualTo(820);
        assertThat(out.crags().get(0).alsoMapped()).containsExactly("https://example.org/1-twin");
        assertThat(out.crags().get(1).alsoMapped()).isEmpty();
        assertThat(out.elevationData()).isEqualTo("test elevation");
    }

    @Test
    void anUnmappedAspectSaysSo() {
        var out = ConditionsExport.build(twoCrags(), List.of(dryDays(), dryDays()),
                List.of(LocalDate.of(2026, 6, 3)), Instant.EPOCH);
        assertThat(out.crags().get(1).days().get("2026-06-03").reasons())
                .contains("wall aspect not mapped: judged as a shaded wall");
    }

    @Test
    void refusesMismatchedForecasts() {
        assertThatThrownBy(() -> ConditionsExport.build(twoCrags(), List.of(dryDays()),
                List.of(LocalDate.of(2026, 6, 3)), Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
