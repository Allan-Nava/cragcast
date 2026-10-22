package io.github.allannava.cragcast.osm;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Replaces the sector table whole, in one transaction, as OsmObjectWriter does for osm_object. */
@Component
class SectorWriter {

    private final SectorRepository sectors;

    SectorWriter(SectorRepository sectors) {
        this.sectors = sectors;
    }

    @Transactional
    public int replaceAll(List<Sector> rows) {
        sectors.deleteAllInBatch();
        sectors.saveAll(rows);
        return rows.size();
    }
}
