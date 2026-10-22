package io.github.allannava.cragcast.osm;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.DockerAvailable;
import io.github.allannava.cragcast.TestcontainersConfigurationAccess;
import io.github.allannava.cragcast.crag.CragRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Against PostgreSQL and the V6 migration (CG-104). */
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false"})
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class SectorWriterTests {

    @Autowired
    CragRepository crags;

    @Autowired
    SectorRepository sectors;

    @Autowired
    SectorWriter writer;

    @Autowired
    SectorImporter importer;

    @Test
    void theImportLinksTheFilesSectorsToTheirCrags() throws Exception {
        // Run as at startup: the other test replaces the table, and test order is not fixed.
        importer.run(new DefaultApplicationArguments());
        var first = importer.read().stream().filter(c -> c.sectors() != null).findFirst().orElseThrow();
        long cragId = crags.findBySlug(first.id()).orElseThrow().getId();
        assertThat(sectors.findByCragIdOrderByPosition(cragId)).extracting(Sector::getId)
                .containsExactlyElementsOf(first.sectors());
        long inFile = importer.read().stream().filter(c -> c.sectors() != null).mapToLong(c -> c.sectors().size()).sum();
        assertThat(sectors.count()).isEqualTo(inFile);
    }

    @Test
    void aNewImportReplacesTheTableWhole() {
        long cragId = crags.findAll().getFirst().getId();
        var a = new OsmObjectsFile.Entry("osm-way-1", "way", 1, "cliff", "Test - A", null, 45.1, 9.1, "S", Map.of("gradeMax", "7a"));
        var b = new OsmObjectsFile.Entry("osm-node-2", "node", 2, "point", "Test - B", null, 45.2, 9.2, null, Map.of());
        writer.replaceAll(List.of(new Sector(cragId, 0, a, "{\"gradeMax\":\"7a\"}"), new Sector(cragId, 1, b, "{}")));
        writer.replaceAll(List.of(new Sector(cragId, 0, b, "{}")));             // a folded no more
        assertThat(sectors.findAll()).extracting(Sector::getId).containsExactly("osm-node-2");
    }
}
