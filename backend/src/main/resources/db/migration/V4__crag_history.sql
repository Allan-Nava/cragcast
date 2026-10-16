-- CG-101: the history branch's verdicts, one row per crag and day, as history.yml wrote them
-- (CSV columns, history/README.md). The CSV stays the source; this is its copy, rebuilt by
-- HistorySync. crag_slug has no foreign key: a crag that left OSM keeps its past days.
CREATE TABLE crag_history (
    day             DATE         NOT NULL,
    crag_slug       VARCHAR(80)  NOT NULL,
    crag_name       VARCHAR(200) NOT NULL,
    province        VARCHAR(8),
    verdict         VARCHAR(16)  NOT NULL,
    rain_last48h_mm DOUBLE PRECISION NOT NULL,
    rain_day_mm     DOUBLE PRECISION NOT NULL,
    max_temp_c      DOUBLE PRECISION NOT NULL,
    max_wind_kmh    DOUBLE PRECISION NOT NULL,
    reasons         TEXT         NOT NULL DEFAULT '',
    generated_at    TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (day, crag_slug)
);
