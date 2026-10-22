package io.github.allannava.cragcast.osm;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Replaces the route table whole, in one transaction, as OsmObjectWriter does for osm_object. */
@Component
class RouteWriter {

    private final RouteRepository routes;

    RouteWriter(RouteRepository routes) {
        this.routes = routes;
    }

    @Transactional
    public int replaceAll(List<Route> rows) {
        routes.deleteAllInBatch();
        routes.saveAll(rows);
        return rows.size();
    }
}
