package io.github.allannava.cragcast.crag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
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
@Order(1)               // before SectorImporter, which links sectors to these rows
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
            String details = json.writeValueAsString(e.details() == null ? Map.of() : e.details());
            var existing = crags.findBySlug(e.id());
            if (existing.isPresent()) {
                existing.get().update(e.name(), null, e.latitude(), e.longitude(), e.aspect(), e.sourceUrl());
                existing.get().setExtras(e.elevation(), details);
                updated++;
            } else {
                Crag c = new Crag(e.id(), e.name(), null, e.latitude(), e.longitude(), e.aspect(), e.sourceUrl());
                c.setExtras(e.elevation(), details);
                crags.save(c);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record File(String region, String source, String licence, List<Entry> crags) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(String id, String name, double latitude, double longitude, Aspect aspect, String sourceUrl,
                 Map<String, Object> details, Integer elevation) {
    }
}
