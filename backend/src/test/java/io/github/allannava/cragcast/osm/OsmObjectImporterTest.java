package io.github.allannava.cragcast.osm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/** Reads the real data/osm-climbing.json that the build puts on the classpath (CG-103). */
class OsmObjectImporterTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void readsEveryObjectOfTheShippedFile() throws Exception {
        var entries = new OsmObjectImporter(mock(OsmObjectWriter.class), json).read(new ClassPathResource(OsmObjectImporter.FILE));
        assertThat(entries).hasSizeGreaterThan(100);
        assertThat(entries).extracting(OsmObjectsFile.Entry::category)
                .containsOnly("crag", "cliff", "point", "route", "venue", "other").contains("crag", "route");
        assertThat(entries).allSatisfy(e -> {
            assertThat(e.id()).isEqualTo("osm-" + e.osmType() + "-" + e.osmId());
            assertThat(e.latitude()).isBetween(44.0, 47.1);      // Lombardy, Piedmont, Trentino-Alto Adige (CG-43)
        });
    }

    @Test
    void writesEveryObjectWithItsFilterColumns() throws Exception {
        OsmObjectWriter writer = mock(OsmObjectWriter.class);
        when(writer.replaceAll(anyList())).thenAnswer(a -> ((List<?>) a.getArgument(0)).size());
        new OsmObjectImporter(writer, json).run(new DefaultApplicationArguments());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OsmObject>> rows = ArgumentCaptor.forClass(List.class);
        verify(writer).replaceAll(rows.capture());
        assertThat(rows.getValue()).hasSizeGreaterThan(100);
        assertThat(rows.getValue()).allSatisfy(o -> assertThat(o.getDetailsJson()).startsWith("{"));
    }

    @Test
    void aRouteKeepsItsGradeAndTheRestOfItsTagsAsJson() {
        var e = new OsmObjectsFile.Entry("osm-node-7", "node", 7, "route", "Spigolo Test", "route_bottom", 45.1, 9.9, "SW",
                Map.of("grade", "6a+", "gradeMax", "7a", "length", 25));
        var o = new OsmObject(e, json.writeValueAsString(e.details()));
        assertThat(o.getGrade()).isEqualTo("6a+");
        assertThat(o.getGradeMax()).isEqualTo("7a");
        assertThat(o.getAspect()).isEqualTo("SW");
        assertThat(o.getDetailsJson()).contains("\"length\":25");
    }
}
