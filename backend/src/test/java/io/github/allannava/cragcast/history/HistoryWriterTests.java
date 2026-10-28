package io.github.allannava.cragcast.history;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.DockerAvailable;
import io.github.allannava.cragcast.TestcontainersConfigurationAccess;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Against PostgreSQL and the V4 migration; made-up crags. */
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false", "cragcast.avalanche.archive.enabled=false",
        "cragcast.observations.enabled=false"})
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class HistoryWriterTests {

    private static final LocalDate OCT_15 = LocalDate.of(2026, 10, 15);
    private static final LocalDate OCT_16 = LocalDate.of(2026, 10, 16);

    @Autowired
    HistoryWriter writer;

    @Autowired
    CragHistoryRepository history;

    @BeforeEach
    void empty() {
        history.deleteAll();
    }

    private static HistoryRow row(LocalDate day, String slug, String verdict) {
        return new HistoryRow(day, slug, "Test " + slug, null, verdict, 0, 0, 15, 10, "",
                Instant.parse("2026-10-15T10:00:00Z"));
    }

    @Test
    void aRerunReplacesTheDayWholeAndLeavesTheOthers() {
        writer.replaceDays(List.of(row(OCT_15, "wall-a", "GO"), row(OCT_15, "wall-b", "GO"),
                row(OCT_16, "wall-a", "MAYBE")));

        int written = writer.replaceDays(List.of(row(OCT_15, "wall-a", "NO_GO")));

        assertThat(written).isEqualTo(1);
        assertThat(history.findAll()).extracting(h -> h.getDay() + " " + h.getCragSlug() + " " + h.getVerdict())
                .containsExactlyInAnyOrder("2026-10-15 wall-a NO_GO", "2026-10-16 wall-a MAYBE");
    }

    @Test
    void nothingToWriteTouchesNothing() {
        writer.replaceDays(List.of(row(OCT_15, "wall-a", "GO")));

        assertThat(writer.replaceDays(List.of())).isZero();
        assertThat(history.count()).isEqualTo(1);
    }
}
