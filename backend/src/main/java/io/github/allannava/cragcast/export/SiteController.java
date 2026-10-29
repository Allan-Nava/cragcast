package io.github.allannava.cragcast.export;

import java.util.function.Function;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * CG-110: the files GitHub Pages serves next to the page, served live from {@link SiteData} at
 * the same paths — so the same page, behind the Pi's nginx, reads them without a change. Before
 * the first refresh has finished there is nothing yet: 503, and the page says the data is not
 * available, as it does when Pages fails.
 */
@RestController
class SiteController {

    private static final MediaType HTML = new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8);

    private final SiteData site;

    SiteController(SiteData site) {
        this.site = site;
    }

    @GetMapping("/data/conditions.json")
    ResponseEntity<String> conditions() {
        return serve(SiteData.Snapshot::conditions, MediaType.APPLICATION_JSON);
    }

    /** CG-43: another region's crags, as on GitHub Pages. */
    @GetMapping("/data/{file:conditions-.+\\.json}")
    ResponseEntity<String> region(@PathVariable String file) {
        return serve(s -> s.regions().get(file), MediaType.APPLICATION_JSON);
    }

    @GetMapping("/api/v1/crags.json")
    ResponseEntity<String> index() {
        return serve(s -> s.api().get("crags.json"), MediaType.APPLICATION_JSON);
    }

    @GetMapping("/api/v1/crags/{file:.+}")
    ResponseEntity<String> crag(@PathVariable String file) {
        return serve(s -> s.api().get("crags/" + file), MediaType.APPLICATION_JSON);
    }

    @GetMapping("/api/v1/ferrate.json")
    ResponseEntity<String> ferrate() {
        return serve(s -> s.api().get("ferrate.json"), MediaType.APPLICATION_JSON);
    }

    @GetMapping("/api/v1/ferrate/{file:.+}")
    ResponseEntity<String> ferrata(@PathVariable String file) {
        return serve(s -> s.api().get("ferrate/" + file), MediaType.APPLICATION_JSON);
    }

    @GetMapping("/c/{file:.+}")
    ResponseEntity<String> share(@PathVariable String file) {
        return serve(s -> s.pages().get("c/" + file), HTML);
    }

    private ResponseEntity<String> serve(Function<SiteData.Snapshot, String> pick, MediaType type) {
        SiteData.Snapshot s = site.latest();
        if (s == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("");
        }
        String body = pick.apply(s);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        // Network first, as on Pages: the page asks with no-cache and the data changes hourly.
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.noCache()).body(body);
    }
}
