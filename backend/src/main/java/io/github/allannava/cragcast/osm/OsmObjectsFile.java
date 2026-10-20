package io.github.allannava.cragcast.osm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/** The shape of data/osm-climbing.json, written by tools/osm_crags.py (CG-103). */
@JsonIgnoreProperties(ignoreUnknown = true)
record OsmObjectsFile(String licence, List<Entry> objects) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(String id, String osmType, long osmId, String category, String name, String climbing,
                 double latitude, double longitude, String aspect, Map<String, Object> details) {
    }
}
