package io.github.allannava.cragcast.osm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** Over the real data/osm-climbing.json and data/crags.json the build puts on the classpath (CG-105). */
class RouteImporterTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final ClassPathResource file = new ClassPathResource(OsmObjectImporter.FILE);

    @Test
    void theShippedRoutesNameCragsAndSectorsThatExist() throws Exception {
        var routes = new RouteImporter(mock(CragRepository.class), mock(RouteWriter.class), json).read(file);
        assertThat(routes).hasSizeGreaterThan(100)
                .allSatisfy(r -> assertThat(r.climbing()).isIn("route", "route_bottom", "route_top"));
        var cragFile = new SectorImporter(mock(CragRepository.class), mock(SectorWriter.class), json).read();
        Set<String> cragIds = cragFile.stream().map(SectorImporter.CragSectors::id).collect(Collectors.toSet());
        Map<String, List<String>> sectors = cragFile.stream().filter(c -> c.sectors() != null)
                .collect(Collectors.toMap(SectorImporter.CragSectors::id, SectorImporter.CragSectors::sectors));
        var linked = routes.stream().filter(r -> r.crag() != null).toList();
        assertThat(linked).hasSizeGreaterThan(routes.size() / 2).allSatisfy(r -> {
            assertThat(cragIds).contains(r.crag());
            assertThat(r.linkedBy()).isIn("relation", "distance");
            if (r.sector() != null) {
                assertThat(sectors.get(r.crag())).contains(r.sector());
            }
        });
        assertThat(routes).filteredOn(r -> r.crag() == null)
                .allSatisfy(r -> assertThat(r.linkedBy()).isNull());
    }

    @Test
    void aRouteIsLinkedOnlyToACragTheTableHas() throws Exception {
        CragRepository crags = mock(CragRepository.class);
        RouteWriter writer = mock(RouteWriter.class);
        when(writer.replaceAll(anyList())).thenAnswer(a -> ((List<?>) a.getArgument(0)).size());
        var importer = new RouteImporter(crags, writer, json);
        var known = importer.read(file).stream().filter(r -> r.crag() != null).findFirst().orElseThrow();
        Crag c = new Crag(known.crag(), "Test", null, 45, 9, null, "https://www.openstreetmap.org/node/0");
        ReflectionTestUtils.setField(c, "id", 5L);
        when(crags.findAll()).thenReturn(List.of(c));

        importer.run(new DefaultApplicationArguments());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Route>> rows = ArgumentCaptor.forClass(List.class);
        verify(writer).replaceAll(rows.capture());
        assertThat(rows.getValue()).hasSize(importer.read(file).size());
        assertThat(rows.getValue()).filteredOn(r -> r.getCragId() != null)
                .isNotEmpty().allSatisfy(r -> assertThat(r.getCragId()).isEqualTo(5L));
        assertThat(rows.getValue()).filteredOn(r -> r.getCragId() == null)
                .allSatisfy(r -> assertThat(r.getLinkedBy()).isNull());
    }

    @Test
    void aRouteKeepsItsGradeLengthBoltsAndPitches() {
        var e = new RouteImporter.RouteEntry("osm-node-3", "route", "route_bottom", "Diedro Test", 45.1, 9.1,
                Map.of("grade", "6b+", "length", 25.5, "bolts", 11, "pitches", 1), "osm-node-1", "osm-way-2", "distance");
        var r = new Route(9L, e, "{}");
        assertThat(List.of(r.getGrade(), r.getGradeRank(), r.getLengthM(), r.getBolts(), r.getPitches()))
                .containsExactly("6b+", 39, 25.5, 11, 1);
        assertThat(List.of(r.getSectorId(), r.getLinkedBy(), r.getClimbing())).containsExactly("osm-way-2", "distance", "route_bottom");
        var unlinked = new Route(null, e, "{}");
        assertThat(unlinked.getSectorId()).isNull();
        var bare = new Route(null, new RouteImporter.RouteEntry("osm-node-4", "route", "route", null, 45, 9, null, null, null, null), "{}");
        assertThat(bare.getGradeRank()).isNull();
        assertThat(bare.getBolts()).isNull();
    }

    @Test
    void anchorsAndOtherObjectsAreNoRoutes() {
        assertThat(new RouteImporter.RouteEntry("a", "route", "anchor", null, 0, 0, null, null, null, null).isRoute()).isFalse();
        assertThat(new RouteImporter.RouteEntry("b", "cliff", null, null, 0, 0, null, null, null, null).isRoute()).isFalse();
        assertThat(new RouteImporter.RouteEntry("c", "route", "route_top", null, 0, 0, null, null, null, null).isRoute()).isTrue();
    }
}
