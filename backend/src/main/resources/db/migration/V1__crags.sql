-- A crag is a climbing area with one dominant wall aspect; the aspect decides how much sun
-- dries the rock. Every row carries the public source its position was taken from (CG-2):
-- no coordinates typed from memory.
CREATE TABLE crag (
    id          BIGSERIAL PRIMARY KEY,
    slug        VARCHAR(80)  NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    region      VARCHAR(80)  NOT NULL,
    latitude    DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude   DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    aspect      VARCHAR(2)   NOT NULL CHECK (aspect IN ('N','NE','E','SE','S','SW','W','NW')),
    source_url  VARCHAR(500) NOT NULL
);
