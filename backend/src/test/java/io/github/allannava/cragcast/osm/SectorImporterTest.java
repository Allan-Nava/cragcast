package io.github.allannava.cragcast.osm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** Over the real data/crags.json and data/osm-climbing.json the build puts on the classpath (CG-104). */
class SectorImporterTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void everySectorOfTheShippedCragsIsInTheShippedObjects() throws Exception {
        var withSectors = new SectorImporter(mock(CragRepository.class), mock(SectorWriter.class), json).read().stream()
                .filter(c -> c.sectors() != null).toList();
        assertThat(withSectors).isNotEmpty();
        Set<String> objects = new OsmObjectImporter(mock(OsmObjectWriter.class), json)
                .read(new ClassPathResource(OsmObjectImporter.FILE)).stream()
                .map(OsmObjectsFile.Entry::id).collect(Collectors.toSet());
        assertThat(withSectors).allSatisfy(c -> {
            assertThat(c.sectors()).doesNotHaveDuplicates();
            assertThat(objects).containsAll(c.sectors());
        });
    }

    @Test
    void eachSectorIsLinkedToItsCragInTheCragsOrder() throws Exception {
        CragRepository crags = mock(CragRepository.class);
        SectorWriter writer = mock(SectorWriter.class);
        when(writer.replaceAll(anyList())).thenAnswer(a -> ((List<?>) a.getArgument(0)).size());
        var importer = new SectorImporter(crags, writer, json);
        var first = importer.read().stream().filter(c -> c.sectors() != null).findFirst().orElseThrow();
        when(crags.findAll()).thenReturn(List.of(crag(first.id(), 42L), crag("osm-node-0", 7L)));

        importer.run(new DefaultApplicationArguments());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Sector>> rows = ArgumentCaptor.forClass(List.class);
        verify(writer).replaceAll(rows.capture());
        assertThat(rows.getValue()).extracting(Sector::getId).containsExactlyElementsOf(first.sectors());
        assertThat(rows.getValue()).allSatisfy(s -> {
            assertThat(s.getCragId()).isEqualTo(42L);
            assertThat(s.getCategory()).isIn("crag", "cliff", "point");
            assertThat(s.getLatitude()).isBetween(44.0, 47.1);   // the three regions (CG-43)
            assertThat(s.getDetailsJson()).startsWith("{");
        });
        assertThat(rows.getValue()).extracting(Sector::getPosition).startsWith(0, 1);
    }

    @Test
    void aSectorKeepsItsOwnPositionAspectAndGrades() {
        var e = new OsmObjectsFile.Entry("osm-way-5", "way", 5, "cliff", "Falesia Test - Settore B", null, 45.2, 9.4, "S",
                java.util.Map.of("gradeMin", "5b", "gradeMax", "7a"));
        var s = new Sector(3L, 1, e, "{\"gradeMin\":\"5b\",\"gradeMax\":\"7a\"}");
        assertThat(List.of(s.getName(), s.getAspect(), s.getGradeMin(), s.getGradeMax()))
                .containsExactly("Falesia Test - Settore B", "S", "5b", "7a");
        assertThat(s.getLongitude()).isEqualTo(9.4);
    }

    private static Crag crag(String slug, long id) {
        Crag c = new Crag(slug, "Test", null, 45, 9, null, "https://www.openstreetmap.org/node/0");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }
}
