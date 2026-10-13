package io.github.allannava.cragcast.crag;

import java.util.List;
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

    CragController(CragRepository crags) {
        this.crags = crags;
    }

    @GetMapping
    List<CragView> list() {
        return crags.findAllByOrderByNameAsc().stream().map(CragView::of).toList();
    }

    @GetMapping("/{slug}")
    CragView get(@PathVariable String slug) {
        return crags.findBySlug(slug)
                .map(CragView::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no crag " + slug));
    }

    /** What the API exposes: the entity minus its database id. */
    record CragView(String slug, String name, String region, double latitude, double longitude,
                    Aspect aspect, String sourceUrl) {
        static CragView of(Crag c) {
            return new CragView(c.getSlug(), c.getName(), c.getRegion(), c.getLatitude(),
                    c.getLongitude(), c.getAspect(), c.getSourceUrl());
        }
    }
}
