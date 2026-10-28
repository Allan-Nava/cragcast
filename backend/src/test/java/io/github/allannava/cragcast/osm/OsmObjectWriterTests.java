package io.github.allannava.cragcast.osm;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.DockerAvailable;
import io.github.allannava.cragcast.TestcontainersConfigurationAccess;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

/** Against PostgreSQL and the V5 migration (CG-103). */
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false", "cragcast.avalanche.archive.enabled=false"})
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class OsmObjectWriterTests {

    @Autowired
    OsmObjectRepository objects;

    @Autowired
    OsmObjectWriter writer;

    @Autowired
    OsmObjectImporter importer;

    @Test
    void theImportFillsTheTableFromTheFile() throws Exception {
        // Run as at startup: the other test replaces the table, and test order is not fixed.
        importer.run(new DefaultApplicationArguments());
        int inFile = importer.read(new ClassPathResource(OsmObjectImporter.FILE)).size();
        assertThat(objects.count()).isEqualTo(inFile);
        assertThat(objects.countByCategory("route")).isPositive();
        assertThat(objects.findByCategoryOrderById("crag")).isNotEmpty();
    }

    @Test
    void aNewImportReplacesTheTableWhole() {
        var a = new OsmObjectsFile.Entry("osm-node-1", "node", 1, "cliff", "Test A", null, 45.1, 9.1, null, Map.of());
        var b = new OsmObjectsFile.Entry("osm-node-2", "node", 2, "route", "Test B", "route", 45.2, 9.2, null, Map.of("grade", "5c"));
        writer.replaceAll(List.of(new OsmObject(a, "{}"), new OsmObject(b, "{\"grade\":\"5c\"}")));
        writer.replaceAll(List.of(new OsmObject(b, "{\"grade\":\"5c\"}")));          // a left OSM
        assertThat(objects.findAll()).extracting(OsmObject::getId).containsExactly("osm-node-2");
        assertThat(objects.findById("osm-node-2").orElseThrow().getGrade()).isEqualTo("5c");
    }
}
