-- CG-107: Lombardy's avalanche bulletin, one row per day and EAWS micro-region, kept as
-- AvalancheArchive last read it. Only its facts — the levels, the bands, the problem types —
-- and the link to the official bulletin; never its text (ARPA Lombardia states no licence,
-- CG-11). A later issue of the same day replaces that day whole. It informs, it never scores.
CREATE TABLE avalanche_bulletin (
    day        DATE         NOT NULL,
    region     VARCHAR(20)  NOT NULL,   -- IT-25-LC-01
    name       VARCHAR(200),            -- the region's name in the bulletin
    level      SMALLINT     NOT NULL,   -- the day's highest, 1 (low) to 5 (very high)
    ratings    TEXT         NOT NULL,   -- JSON: the levels by elevation band and part of the day
    problems   TEXT         NOT NULL,   -- JSON array of CAAML problem types
    bulletin   VARCHAR(300) NOT NULL,   -- the official bulletin's page
    fetched_at TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (day, region)
);
