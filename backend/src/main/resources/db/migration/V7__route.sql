-- CG-105: OpenStreetMap's single routes (climbing=route, route_bottom, route_top) from
-- data/osm-climbing.json, each linked by tools/osm_crags.py to the crag it belongs to — by an
-- OSM relation, or as the nearest crag or sector within 400 m — and to its sector when there is
-- one. A route never becomes a crag (CG-42): it is stored here and served under its crag by the
-- API. Replaced whole at startup by RouteImporter, after the sectors; ODbL like osm_object.
CREATE TABLE route (
    id          VARCHAR(40)      PRIMARY KEY,          -- osm-node-123
    crag_id     BIGINT           REFERENCES crag (id) ON DELETE CASCADE,   -- null: no crag near
    sector_id   VARCHAR(40),                           -- a sector's id; no key, both replaced whole
    linked_by   VARCHAR(10),                           -- relation, distance; null when unlinked
    climbing    VARCHAR(20)      NOT NULL,             -- OSM's route, route_bottom or route_top
    name        VARCHAR(300),
    grade       VARCHAR(4),                            -- French, as OSM gives it
    grade_rank  INTEGER,                               -- the grade in order: FrenchGrade.rank()
    length_m    DOUBLE PRECISION,
    bolts       INTEGER,
    pitches     INTEGER,
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    details     TEXT             NOT NULL DEFAULT '{}'
);
CREATE INDEX route_crag ON route (crag_id, grade_rank);
