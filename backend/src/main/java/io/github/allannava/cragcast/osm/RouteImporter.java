package io.github.allannava.cragcast.osm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * At startup, after the crags and their sectors, fills the route table (CG-105) from the single
 * routes of classpath:data/osm-climbing.json — the objects tools/osm_crags.py gave a "linkedBy"
 * key — each linked to the crag it names. Replaced whole, since the file is the source. A route
 * whose crag is not in the crag table is kept unlinked; without the file the table is left as it is.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "cragcast.import-crags", havingValue = "true", matchIfMissing = true)
class RouteImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RouteImporter.class);

    private final CragRepository crags;
    private final RouteWriter writer;
    private final JsonMapper json;

    RouteImporter(CragRepository crags, RouteWriter writer, JsonMapper json) {
        this.crags = crags;
        this.writer = writer;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource file = new ClassPathResource(OsmObjectImporter.FILE);
        if (!file.exists()) {
            log.warn("{} not on the classpath: route left as it is", OsmObjectImporter.FILE);
            return;
        }
        Map<String, Long> ids = crags.findAll().stream().collect(Collectors.toMap(Crag::getSlug, Crag::getId));
        List<Route> rows = read(file).stream()
                .map(e -> new Route(e.crag() == null ? null : ids.get(e.crag()), e,
                        json.writeValueAsString(e.details() == null ? Map.of() : e.details())))
                .toList();
        long linked = rows.stream().filter(r -> r.getCragId() != null).count();
        log.info("routes imported from {}: {}, {} linked to a crag", OsmObjectImporter.FILE, writer.replaceAll(rows), linked);
    }

    /** The single routes of the file: the objects tools/osm_crags.py linked, or tried to. */
    List<RouteEntry> read(ClassPathResource file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            List<RouteEntry> all = json.readValue(in, RoutesFile.class).objects();
            return all == null ? List.of() : all.stream().filter(RouteEntry::isRoute).toList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoutesFile(List<RouteEntry> objects) {
    }

    /** An object of data/osm-climbing.json with the keys a single route carries (CG-105). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RouteEntry(String id, String category, String climbing, String name, double latitude, double longitude,
                      Map<String, Object> details, String crag, String sector, String linkedBy) {

        boolean isRoute() {
            return "route".equals(category) && ("route".equals(climbing) || "route_bottom".equals(climbing)
                    || "route_top".equals(climbing));
        }
    }
}
