package io.github.allannava.cragcast.export;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.allannava.cragcast.crag.Aspect;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** CG-117: the sectors and routes the page and the API show. Made-up objects, not real places. */
class OsmClimbingTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private static final String FILE = """
            {"objects": [
             {"id": "osm-way-5", "category": "cliff", "name": "Test - Settore Alto", "aspect": "S",
              "details": {"gradeMin": "5b", "gradeMax": "7a"}},
             {"id": "osm-node-1", "category": "route", "name": "Zeta", "details": {"grade": "6a+", "bolts": 9},
              "crag": "t-1", "sector": "osm-way-5", "linkedBy": "relation"},
             {"id": "osm-node-2", "category": "route", "name": "Senza grado", "details": {},
              "crag": "t-1", "sector": null, "linkedBy": "distance"},
             {"id": "osm-node-3", "category": "route", "name": "Alfa", "details": {"grade": "5c", "length": 20, "pitches": 1},
              "crag": "t-1", "sector": null, "linkedBy": "distance"},
             {"id": "osm-node-4", "category": "route", "name": "Lontana", "details": {"grade": "4"},
              "crag": null, "sector": null, "linkedBy": null},
             {"id": "osm-node-6", "category": "route", "name": "Ancora", "details": {"grade": "6a+"},
              "crag": "t-1", "sector": null, "linkedBy": "distance"}
            ]}""";

    @Test
    void aCragGetsItsSectorsInOrderAndItsRoutesEasiestFirst() {
        var c = OsmClimbing.parse(FILE, json);
        assertThat(c.sectors(List.of("osm-way-5", "osm-way-404")))
                .containsExactly(new ConditionsExport.SectorOut("osm-way-5", "Test - Settore Alto", Aspect.S, "5b", "7a"));
        assertThat(c.routes("t-1")).extracting(ConditionsExport.RouteOut::name)
                .containsExactly("Alfa", "Ancora", "Zeta", "Senza grado");
        var alfa = c.routes("t-1").getFirst();
        assertThat(List.of(alfa.grade(), alfa.length(), alfa.pitches())).containsExactly("5c", 20.0, 1);
        assertThat(c.routes("t-1").get(2).sector()).isEqualTo("osm-way-5");
        assertThat(c.routes("t-2")).isEmpty();
        assertThat(c.sectors(null)).isEmpty();
    }

    @Test
    void withoutTheFileNoCragHasSectorsOrRoutes() {
        assertThat(OsmClimbing.none().sectors(List.of("osm-way-5"))).isEmpty();
        assertThat(OsmClimbing.none().routes("t-1")).isEmpty();
    }

    @Test
    void anAspectOutsideTheEightIsNone() {
        var c = OsmClimbing.parse("""
                {"objects": [{"id": "osm-way-7", "category": "cliff", "name": "X", "aspect": "SSW", "details": null}]}""", json);
        assertThat(c.sectors(List.of("osm-way-7")).getFirst().aspect()).isNull();
    }

    @Test
    void theExportPutsThemOnTheCragAndInItsApiFile() {
        var crags = new ConditionsExport.CragsFile("Test", "test", "test licence", "test elevation", List.of(
                new ConditionsExport.Crag("t-1", "Test Wall", 45.0, 9.0, Aspect.S, "https://example.org/1",
                        Map.of(), 820, List.of(), null, null, null, List.of("osm-way-5"))));
        var days = List.of(LocalDate.of(2026, 6, 3));
        var out = ConditionsExport.build(crags, List.of(ConditionsExportTest.dryDays()), days, Instant.EPOCH,
                io.github.allannava.cragcast.avalanche.AvalancheRegions.none(), Map.of(), OsmClimbing.parse(FILE, json));
        assertThat(out.crags().getFirst().sectors()).hasSize(1);
        assertThat(out.crags().getFirst().routes()).hasSize(4);
        var doc = (ConditionsExport.CragDocument) ConditionsExport.api(out).get("crags/t-1.json");
        assertThat(doc.crag().routes().getFirst().name()).isEqualTo("Alfa");
    }

    @Test
    void theShippedFileGivesSomeCragsTheirRoutes() throws Exception {
        var c = OsmClimbing.parse(new String(new org.springframework.core.io.ClassPathResource("data/osm-climbing.json")
                .getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), json);
        assertThat(c.routes("osm-relation-11740994")).hasSizeGreaterThan(20)               // Falesia del Picuz
                .allSatisfy(r -> assertThat(r.id()).startsWith("osm-"));
    }
}
