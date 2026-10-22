-- CG-104: the sectors tools/osm_crags.py's group() folds into a crag — separate cliffs or loose
-- points on OpenStreetMap, listed by id in data/crags.json's "sectors" — each with its own
-- position, aspect and grades from data/osm-climbing.json. The crag's own row keeps the folded
-- values. Replaced whole at startup by SectorImporter, after the crags; ODbL like osm_object.
CREATE TABLE sector (
    id         VARCHAR(40)      PRIMARY KEY,          -- osm-way-123, as in data/osm-climbing.json
    crag_id    BIGINT           NOT NULL REFERENCES crag (id) ON DELETE CASCADE,
    position   INTEGER          NOT NULL,             -- order in the crag's list, from 0
    category   VARCHAR(10)      NOT NULL,             -- crag, cliff, point: what OSM maps it as
    name       VARCHAR(300),
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL,
    aspect     VARCHAR(2),
    grade_min  VARCHAR(4),
    grade_max  VARCHAR(4),
    details    TEXT             NOT NULL DEFAULT '{}'
);
CREATE INDEX sector_crag ON sector (crag_id, position);
