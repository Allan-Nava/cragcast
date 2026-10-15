package io.github.allannava.cragcast.crag;

import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/crags")
class CragController {

    private final CragRepository crags;
    private final JsonMapper json;

    CragController(CragRepository crags, JsonMapper json) {
        this.crags = crags;
        this.json = json;
    }

    private CragView view(Crag c) {
        Map<String, Object> details = json.readValue(c.getDetailsJson(), new TypeReference<Map<String, Object>>() { });
        return new CragView(c.getSlug(), c.getName(), c.getRegion(), c.getLatitude(), c.getLongitude(),
                c.getAspect(), c.getElevation(), details, c.getSourceUrl());
    }

    @GetMapping
    List<CragView> list() {
        return crags.findAllByOrderByNameAsc().stream().map(this::view).toList();
    }

    @GetMapping("/{slug}")
    CragView get(@PathVariable String slug) {
        return crags.findBySlug(slug)
                .map(this::view)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no crag " + slug));
    }

    /** What the API exposes: the entity minus its database id, with the details as JSON. */
    record CragView(String slug, String name, String region, double latitude, double longitude,
                    Aspect aspect, Integer elevation, Map<String, Object> details, String sourceUrl) {
    }
}
