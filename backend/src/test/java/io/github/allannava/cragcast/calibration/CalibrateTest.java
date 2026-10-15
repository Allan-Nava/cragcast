package io.github.allannava.cragcast.calibration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.allannava.cragcast.calibration.Observation.Rock;
import io.github.allannava.cragcast.conditions.Verdict;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalibrateTest {

    @Test
    void agreementTable() {
        assertThat(Agreement.of(Rock.DRY, Verdict.GO)).isEqualTo(Agreement.AGREES);
        assertThat(Agreement.of(Rock.DRY, Verdict.MAYBE)).isEqualTo(Agreement.TOO_CAUTIOUS);
        assertThat(Agreement.of(Rock.DAMP, Verdict.GO)).isEqualTo(Agreement.MISSED_WET_ROCK);
        assertThat(Agreement.of(Rock.DAMP, Verdict.MAYBE)).isEqualTo(Agreement.AGREES);
        assertThat(Agreement.of(Rock.DAMP, Verdict.NO_GO)).isEqualTo(Agreement.TOO_HARSH);
        assertThat(Agreement.of(Rock.WET, Verdict.MAYBE)).isEqualTo(Agreement.AGREES);
        assertThat(Agreement.of(Rock.WET, Verdict.NO_GO)).isEqualTo(Agreement.AGREES);
        assertThat(Agreement.of(Rock.WET, Verdict.GO)).isEqualTo(Agreement.MISSED_WET_ROCK);
    }

    @Test
    void parsesTheCsvSkippingCommentsAndKeepingCommasInTheNote() {
        var rows = Calibrate.parse(List.of(
                "date,crag,rock,observer,note",
                "# made-up rows for the test",
                "2026-06-03,Test Wall,dry,Allan,sunny, windy",
                "2026-06-04,osm-node-1,WET,Allan"));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).rock()).isEqualTo(Rock.DRY);
        assertThat(rows.get(0).note()).isEqualTo("sunny, windy");
        assertThat(rows.get(1).date()).isEqualTo(LocalDate.of(2026, 6, 4));
    }

    @Test
    void refusesAnAmbiguousCragName() {
        var crags = new Calibrate.Crags(List.of(
                new Calibrate.Crag("osm-node-1", "Twin Wall", 45, 9, null),
                new Calibrate.Crag("osm-node-2", "Twin Wall", 45.001, 9, null)));
        assertThatThrownBy(() -> crags.find("Twin Wall")).hasMessageContaining("use the id");
        assertThat(crags.find("osm-node-2").latitude()).isEqualTo(45.001);
    }
}
