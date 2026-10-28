package io.github.allannava.cragcast.crag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Reads the real data/crags.json that the build puts on the classpath. */
class CragImporterTest {

    @Test
    void readsTheCragsShippedInTheJar() throws IOException {
        var entries = new CragImporter(mock(CragRepository.class), JsonMapper.builder().build()).read();
        assertThat(entries).hasSizeGreaterThan(50);
        assertThat(entries).allSatisfy(e -> {
            assertThat(e.id()).startsWith("osm-");
            assertThat(e.sourceUrl()).startsWith("https://www.openstreetmap.org/");
            assertThat(e.latitude()).isBetween(44.0, 47.1);      // the three regions (CG-43)
        });
        assertThat(entries).anyMatch(e -> e.aspect() == null);
        assertThat(entries).allSatisfy(e -> assertThat(e.elevation()).isNotNull());
        assertThat(entries).anyMatch(e -> e.details() != null && e.details().containsKey("gradeMax"));
    }
}
