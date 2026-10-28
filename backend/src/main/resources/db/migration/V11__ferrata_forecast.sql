-- CG-115: what every hourly refresh said about each via ferrata, kept as crag_forecast keeps the
-- crags' (CG-106, ADR 0021): one row per via ferrata, day and refresh, never updated. The via
-- ferratas are not in the database (data/ferrate.json in the jar, ADR 0023), so ferrata_id is the
-- file's id (osm-way-…, osm-relation-…) with no foreign key. Written by ForecastArchive.
CREATE TABLE ferrata_forecast (
    ferrata_id VARCHAR(40)  NOT NULL,
    day        DATE         NOT NULL,
    fetched_at TIMESTAMPTZ  NOT NULL,   -- the refresh's time, as in crag_forecast
    verdict    VARCHAR(16)  NOT NULL,   -- GO, MAYBE, NO_GO
    reasons    TEXT         NOT NULL,   -- JSON array of FerrataScorer's reasons
    weather    TEXT         NOT NULL,   -- JSON: the FerrataWeather it scored, at the top
    hours      TEXT         NOT NULL,   -- JSON: the day's hours at the top as parallel lists
    PRIMARY KEY (ferrata_id, day, fetched_at)
);
