package io.github.allannava.cragcast.osm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.allannava.cragcast.crag.Crag;
import io.github.allannava.cragcast.crag.CragRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
 * At startup, after CragImporter, fills the sector table (CG-104): for every crag of
 * classpath:data/crags.json, the OSM objects listed in its "sectors", with their values from
 * classpath:data/osm-climbing.json. Replaced whole, since both files are the source. A sector
 * missing from the objects file (the two written by different runs) is skipped and counted;
 * without the objects file the table is left as it is.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "cragcast.import-crags", havingValue = "true", matchIfMissing = true)
class SectorImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SectorImporter.class);
    static final String CRAGS = "data/crags.json";

    private final CragRepository crags;
    private final SectorWriter writer;
    private final JsonMapper json;

    SectorImporter(CragRepository crags, SectorWriter writer, JsonMapper json) {
        this.crags = crags;
        this.writer = writer;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource objectsFile = new ClassPathResource(OsmObjectImporter.FILE);
        if (!objectsFile.exists()) {
            log.warn("{} not on the classpath: sector left as it is", OsmObjectImporter.FILE);
            return;
        }
        Map<String, OsmObjectsFile.Entry> objects;
        try (InputStream in = objectsFile.getInputStream()) {
            List<OsmObjectsFile.Entry> list = json.readValue(in, OsmObjectsFile.class).objects();
            objects = (list == null ? List.<OsmObjectsFile.Entry>of() : list).stream()
                    .collect(Collectors.toMap(OsmObjectsFile.Entry::id, Function.identity(), (a, b) -> a));
        }
        Map<String, Long> ids = crags.findAll().stream().collect(Collectors.toMap(Crag::getSlug, Crag::getId));
        List<Sector> rows = new ArrayList<>();
        int missing = 0;
        for (CragSectors c : read()) {
            Long cragId = ids.get(c.id());
            if (cragId == null || c.sectors() == null) {
                continue;
            }
            for (int i = 0; i < c.sectors().size(); i++) {
                OsmObjectsFile.Entry e = objects.get(c.sectors().get(i));
                if (e == null) {
                    missing++;
                    continue;
                }
                rows.add(new Sector(cragId, i, e, json.writeValueAsString(e.details() == null ? Map.of() : e.details())));
            }
        }
        log.info("sectors imported from {}: {}, {} not in {}", CRAGS, writer.replaceAll(rows), missing,
                OsmObjectImporter.FILE);
    }

    List<CragSectors> read() throws IOException {
        try (InputStream in = new ClassPathResource(CRAGS).getInputStream()) {
            return json.readValue(in, CragsFile.class).crags();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CragsFile(List<CragSectors> crags) {
    }

    /** Only what this import needs of a crag: its id and the OSM ids of its sectors. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CragSectors(String id, List<String> sectors) {
    }
}
