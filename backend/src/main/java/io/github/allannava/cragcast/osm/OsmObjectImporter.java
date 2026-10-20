package io.github.allannava.cragcast.osm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * At startup, copies classpath:data/osm-climbing.json into osm_object (CG-103), replacing what
 * was there: every object of the OpenStreetMap climbing query, whole, as CragImporter does for
 * the crags. Without the file (a build that predates it) the table is left as it is.
 */
@Component
@ConditionalOnProperty(name = "cragcast.import-osm-objects", havingValue = "true", matchIfMissing = true)
class OsmObjectImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OsmObjectImporter.class);
    static final String FILE = "data/osm-climbing.json";

    private final OsmObjectWriter writer;
    private final JsonMapper json;

    OsmObjectImporter(OsmObjectWriter writer, JsonMapper json) {
        this.writer = writer;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource file = new ClassPathResource(FILE);
        if (!file.exists()) {
            log.warn("{} not on the classpath: osm_object left as it is", FILE);
            return;
        }
        List<OsmObject> rows = read(file).stream()
                .map(e -> new OsmObject(e, json.writeValueAsString(e.details() == null ? Map.of() : e.details())))
                .toList();
        log.info("OSM objects imported from {}: {}", FILE, writer.replaceAll(rows));
    }

    List<OsmObjectsFile.Entry> read(ClassPathResource file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            OsmObjectsFile f = json.readValue(in, OsmObjectsFile.class);
            return f.objects() == null ? List.of() : f.objects();
        }
    }
}
