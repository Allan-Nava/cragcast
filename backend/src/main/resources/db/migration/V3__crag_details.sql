-- CG-31: what the page already shows, in the API too. The elevation of the area (Copernicus DEM,
-- see data/crags.json) and the OSM climbing details as JSON text — grades, route length, rock,
-- style, website — read and written whole, never queried by field.
ALTER TABLE crag ADD COLUMN elevation INTEGER;
ALTER TABLE crag ADD COLUMN details TEXT NOT NULL DEFAULT '{}';
