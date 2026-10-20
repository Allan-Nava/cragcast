package io.github.allannava.cragcast.osm;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replaces the table whole: the file is the source, so an object OSM no longer has must not
 * survive from the last import. A bean of its own so the transaction applies when the importer
 * calls it — the table is never seen half filled.
 */
@Component
class OsmObjectWriter {

    private final OsmObjectRepository objects;

    OsmObjectWriter(OsmObjectRepository objects) {
        this.objects = objects;
    }

    @Transactional
    public int replaceAll(List<OsmObject> rows) {
        objects.deleteAllInBatch();
        objects.saveAll(rows);
        return rows.size();
    }
}
