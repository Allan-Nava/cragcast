-- CG-103: every object of tools/osm_crags.py's OpenStreetMap query — crags, cliffs, loose points,
-- single routes, gyms — as data/osm-climbing.json holds it. The file stays the source; this is
-- its copy, replaced whole at startup by OsmObjectImporter. ODbL: credit OpenStreetMap where
-- shown. A column for what is worth filtering on; the rest of the validated tags as JSON text.
CREATE TABLE osm_object (
    id         VARCHAR(40)      PRIMARY KEY,          -- osm-node-123, as in data/crags.json
    osm_type   VARCHAR(10)      NOT NULL,             -- node, way, relation
    osm_id     BIGINT           NOT NULL,
    category   VARCHAR(10)      NOT NULL,             -- crag, cliff, point, route, venue, other
    name       VARCHAR(300),
    climbing   VARCHAR(40),                           -- OSM's raw climbing=* value
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL,
    aspect     VARCHAR(2),
    grade      VARCHAR(4),                            -- a route's own French grade
    grade_min  VARCHAR(4),
    grade_max  VARCHAR(4),
    details    TEXT             NOT NULL DEFAULT '{}'
);
CREATE INDEX osm_object_category ON osm_object (category);
