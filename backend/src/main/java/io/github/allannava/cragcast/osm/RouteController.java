package io.github.allannava.cragcast.osm;

import io.github.allannava.cragcast.crag.CragRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** CG-105: a crag's single routes, under the crag — never in the crag list (CG-42). */
@RestController
@RequestMapping("/api/crags")
class RouteController {

    private final CragRepository crags;
    private final RouteRepository routes;
    private final JsonMapper json;

    RouteController(CragRepository crags, RouteRepository routes, JsonMapper json) {
        this.crags = crags;
        this.routes = routes;
        this.json = json;
    }

    @GetMapping("/{slug}/routes")
    List<RouteView> list(@PathVariable String slug) {
        long cragId = crags.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no crag " + slug))
                .getId();
        return routes.findByCragIdOrderByGradeRankAscNameAscIdAsc(cragId).stream().map(this::view).toList();
    }

    private RouteView view(Route r) {
        Map<String, Object> details = json.readValue(r.getDetailsJson(), new TypeReference<Map<String, Object>>() { });
        String[] osm = r.getId().split("-", 3);                       // osm-node-123
        return new RouteView(r.getId(), r.getName(), r.getGrade(), r.getGradeRank(), r.getLengthM(), r.getBolts(),
                r.getPitches(), r.getSectorId(), r.getLinkedBy(), r.getLatitude(), r.getLongitude(), details,
                "https://www.openstreetmap.org/" + osm[1] + "/" + osm[2]);
    }

    /** What the API exposes of a route; the OSM link for the ODbL credit and the source. */
    record RouteView(String id, String name, String grade, Integer gradeRank, Double length, Integer bolts,
                     Integer pitches, String sector, String linkedBy, double latitude, double longitude,
                     Map<String, Object> details, String sourceUrl) {
    }
}
