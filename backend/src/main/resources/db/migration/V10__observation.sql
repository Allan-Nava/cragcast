-- CG-108: data/observations.csv copied — the days someone saw the rock first-hand (CG-27). The
-- CSV on main stays the source and changes only by a reviewed PR; ObservationSync replaces this
-- table whole from it at startup and each night. crag is as the CSV writes it: an OSM id from
-- data/crags.json or a crag's exact name, so no foreign key.
CREATE TABLE observation (
    day      DATE         NOT NULL,
    crag     VARCHAR(200) NOT NULL,
    rock     VARCHAR(8)   NOT NULL,   -- DRY, DAMP, WET
    observer VARCHAR(200) NOT NULL,   -- who saw it, so a doubtful row can be asked about
    note     TEXT         NOT NULL DEFAULT '',
    PRIMARY KEY (day, crag, observer)
);
