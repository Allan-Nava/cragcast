// CG-114: via ferratas on the page. Each becomes a place of the same shape as a crag, so the
// list, the map, the panel, the favourites and the charts read both; `ferrata` says which it is.
import type { Crag, Ferrata } from "../types";

/** A via ferrata as a place: at its start, its elevation the top's; no aspect, no climbing details. */
export const asPlace = (f: Ferrata): Crag => ({
  id: f.id, name: f.name, latitude: f.latitude, longitude: f.longitude, aspect: null, sourceUrl: f.sourceUrl,
  details: {}, elevation: f.top.elevation, alsoMapped: [], municipality: null, province: null, provinceCode: null,
  days: f.days,
  ferrata: { start: f.start, top: f.top, lengthM: f.lengthM, scaleMin: f.scaleMin, scaleMax: f.scaleMax,
             scaleDropped: f.scaleDropped ?? [], pieces: f.pieces ?? [] },
});

/** The difficulty as OSM gives it: "difficoltà 3", "difficoltà 2–4", or that it is not mapped. */
export function scaleText(f: Pick<Ferrata, "scaleMin" | "scaleMax">): string {
  if (f.scaleMin == null || f.scaleMax == null) return "difficoltà non mappata su OpenStreetMap";
  return f.scaleMin === f.scaleMax ? `difficoltà ${f.scaleMin} (scala 0–6)` : `difficoltà ${f.scaleMin}–${f.scaleMax} (scala 0–6)`;
}

/** Short Italian phrases for a via ferrata: difficulty, length, start and top. */
export function ferrataParts(c: Crag): string[] {
  const f = c.ferrata;
  if (!f) return [];
  const parts = [scaleText(f)];
  if (f.lengthM) parts.push(`attrezzata per circa ${f.lengthM} m`);
  if (f.start.elevation != null && f.top.elevation != null) {
    parts.push(`attacco circa ${f.start.elevation} m, cima circa ${f.top.elevation} m`);
  }
  return parts;
}

/** CG-114: said wherever a via ferrata's verdict is shown in full. */
export const ON_THE_SPOT =
  "Il giudizio è una stima dal meteo previsto in cima: le condizioni di una via ferrata si verificano sempre sul posto.";
