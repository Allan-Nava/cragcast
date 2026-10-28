-- CG-106: what every hourly refresh said, kept. crag_weather holds only the latest hour; this
-- table appends one row per crag, day and refresh, so "what did cragcast say at 18:00 the day
-- before" stays answerable. Written by ForecastArchive after each successful refresh, never
-- updated. It exists nowhere else: the nightly dump (CG-109) is its only copy.
-- No ON DELETE CASCADE: CragImporter never deletes a crag, and nothing should take the archive
-- with it if one ever is.
CREATE TABLE crag_forecast (
    crag_id            BIGINT           NOT NULL REFERENCES crag (id),
    day                DATE             NOT NULL,
    fetched_at         TIMESTAMPTZ      NOT NULL,   -- the refresh's time, as in crag_weather
    verdict            VARCHAR(16)      NOT NULL,   -- GO, MAYBE, NO_GO
    reasons            TEXT             NOT NULL,   -- JSON array of the scorer's reasons
    rain_last48h_mm    DOUBLE PRECISION NOT NULL,
    rain_during_day_mm DOUBLE PRECISION NOT NULL,
    max_temperature_c  DOUBLE PRECISION NOT NULL,
    max_wind_kmh       DOUBLE PRECISION NOT NULL,
    hours              TEXT             NOT NULL,   -- JSON: the day's hours as parallel lists
    PRIMARY KEY (crag_id, day, fetched_at)
);
