package io.github.allannava.cragcast.osm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.allannava.cragcast.DockerAvailable;
import io.github.allannava.cragcast.TestcontainersConfigurationAccess;
import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

/** Against PostgreSQL and the V7 migration (CG-105). */
@SpringBootTest(properties = {"cragcast.refresh.enabled=false", "cragcast.history.enabled=false"})
@AutoConfigureMockMvc
@Import(TestcontainersConfigurationAccess.Config.class)
@EnabledIf(DockerAvailable.CHECK)
class RouteApiTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    CragRepository crags;

    @Autowired
    RouteRepository routes;

    @Autowired
    RouteWriter writer;

    @Autowired
    RouteImporter importer;

    @Test
    void theImportKeepsEveryRouteOfTheFile() throws Exception {
        // Run as at startup: the other tests replace the table, and test order is not fixed.
        importer.run(new DefaultApplicationArguments());
        var inFile = importer.read(new ClassPathResource(OsmObjectImporter.FILE));
        assertThat(routes.count()).isEqualTo(inFile.size());
        // Linked only to the crags the table holds: CragApiTests empties it, and test order is not fixed.
        var slugs = crags.findAll().stream().map(Crag::getSlug).collect(java.util.stream.Collectors.toSet());
        assertThat(routes.countByCragIdIsNull()).isEqualTo(inFile.stream().filter(r -> !slugs.contains(r.crag())).count());
    }

    @Test
    void aCragsRoutesComeEasiestFirstTheUngradedLast() throws Exception {
        // Test data only: a made-up crag and routes, not real places.
        Crag c = crags.save(new Crag("osm-node-999999999", "Test Wall", null, 45.0, 9.0, null, "https://example.org/t"));
        writer.replaceAll(List.of(
                route(c.getId(), "osm-node-1", "Placca", Map.of("grade", "6a+", "bolts", 9)),
                route(c.getId(), "osm-node-2", "Senza grado", Map.of()),
                route(c.getId(), "osm-node-3", "Diedro", Map.of("grade", "5c", "length", 20)),
                route(null, "osm-node-4", "Lontana", Map.of("grade", "4"))));

        mvc.perform(get("/api/crags/osm-node-999999999/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Diedro"))
                .andExpect(jsonPath("$[0].length").value(20.0))
                .andExpect(jsonPath("$[1].grade").value("6a+"))
                .andExpect(jsonPath("$[1].bolts").value(9))
                .andExpect(jsonPath("$[1].sourceUrl").value("https://www.openstreetmap.org/node/1"))
                .andExpect(jsonPath("$[2].name").value("Senza grado"));
        mvc.perform(get("/api/crags")).andExpect(jsonPath("$[?(@.name == 'Diedro')]").isEmpty());
        mvc.perform(get("/api/crags/nowhere/routes")).andExpect(status().isNotFound());
        crags.delete(c);
    }

    private static Route route(Long cragId, String id, String name, Map<String, Object> details) {
        return new Route(cragId, new RouteImporter.RouteEntry(id, "route", "route_bottom", name, 45.0, 9.0, details,
                cragId == null ? null : "osm-node-999999999", null, cragId == null ? null : "distance"), "{}");
    }
}
