package io.github.allannava.cragcast.crag;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * At startup, brings the crag table in line with classpath:data/crags.json (OpenStreetMap):
 * inserts new crags and updates known ones by slug (the OSM id). Never deletes: a crag that
 * leaves OSM keeps its history until someone removes it on purpose.
 */
@Component
@ConditionalOnProperty(name = "cragcast.import-crags", havingValue = "true", matchIfMissing = true)
class CragImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CragImporter.class);

    private final CragRepository crags;
    private final JsonMapper json;

    CragImporter(CragRepository crags, JsonMapper json) {
        this.crags = crags;
        this.json = json;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        int inserted = 0;
        int updated = 0;
        for (Entry e : read()) {
            var existing = crags.findBySlug(e.id());
            if (existing.isPresent()) {
                existing.get().update(e.name(), null, e.latitude(), e.longitude(), e.aspect(), e.sourceUrl());
                updated++;
            } else {
                crags.save(new Crag(e.id(), e.name(), null, e.latitude(), e.longitude(), e.aspect(), e.sourceUrl()));
                inserted++;
            }
        }
        log.info("crags imported from data/crags.json: {} new, {} updated", inserted, updated);
    }

    List<Entry> read() throws IOException {
        try (InputStream in = new ClassPathResource("data/crags.json").getInputStream()) {
            return json.readValue(in, File.class).crags();
        }
    }

    record File(String region, String source, String licence, List<Entry> crags) {
    }

    record Entry(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl) {
    }
}
