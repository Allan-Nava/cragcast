-- Crags now come from OpenStreetMap (data/crags.json), where most have no mapped aspect: the
-- aspect becomes optional. The CHECK from V1 still holds, since a NULL passes it.
ALTER TABLE crag ALTER COLUMN aspect DROP NOT NULL;
-- V1 had a region column; OSM gives none per crag (CG-25 adds the area later).
ALTER TABLE crag ALTER COLUMN region DROP NOT NULL;

-- One row per crag and day: the four numbers the scorer reads, reduced from the hourly
-- forecast by the hourly refresh (CG-5), so the API never waits on Open-Meteo.
CREATE TABLE crag_weather (
    crag_id            BIGINT NOT NULL REFERENCES crag (id) ON DELETE CASCADE,
    day                DATE   NOT NULL,
    rain_last48h_mm    DOUBLE PRECISION NOT NULL,
    rain_during_day_mm DOUBLE PRECISION NOT NULL,
    max_temperature_c  DOUBLE PRECISION NOT NULL,
    max_wind_kmh       DOUBLE PRECISION NOT NULL,
    fetched_at         TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (crag_id, day)
);
