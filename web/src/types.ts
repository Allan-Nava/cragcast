// The shape of data/conditions.json, written by ConditionsExport (backend/.../export).
export type Verdict = "GO" | "MAYBE" | "NO_GO";
export type Aspect = "N" | "NE" | "E" | "SE" | "S" | "SW" | "W" | "NW";

export interface Weather {
  rainLast48hMm: number;
  rainDuringDayMm: number;
  maxTemperatureC: number;
  maxWindKmh: number;
}

export interface Hour {
  hour: number;
  rainMm: number;
  temperatureC: number;
  windKmh: number;
}

/** CG-11: the avalanche bulletin for the crag's micro-region, only when one covers it. */
export interface Avalanche {
  region: string;
  name: string | null;
  level: number;
  ratings: { level: number; above: string | null; below: string | null; period: "earlier" | "later" | null }[];
  problems: string[];
  bulletin: string;
}

export interface Day {
  verdict: Verdict;
  reasons: string[];
  weather: Weather;
  hours: Hour[];
  avalanche?: Avalanche;
}

export interface Details {
  gradeMin?: string;
  gradeMax?: string;
  gradeMean?: string;
  length?: number;
  lengthMin?: number;
  lengthMax?: number;
  routes?: number;
  rock?: string;
  rockIt?: string;
  styles?: string[];
  bolted?: boolean;
  website?: string;
}

/** CG-104/CG-117: one sector folded into the crag, with its own aspect and grades. */
export interface Sector {
  id: string;
  name: string | null;
  aspect: Aspect | null;
  gradeMin: string | null;
  gradeMax: string | null;
}

/** CG-105/CG-117: one single route of the crag from OSM; sector is a Sector's id, or null. */
export interface Route {
  id: string;
  name: string | null;
  grade: string | null;
  length: number | null;
  bolts: number | null;
  pitches: number | null;
  sector: string | null;
}

export interface Crag {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  aspect: Aspect | null;
  sourceUrl: string;
  details: Details;
  elevation: number | null;
  alsoMapped: string[];
  municipality: string | null;
  province: string | null;
  provinceCode: string | null;
  days: Record<string, Day>;
  /** CG-117: absent in data written before it (a page cached by the service worker). */
  sectors?: Sector[];
  routes?: Route[];
  /** CG-43: the Italian region, from OSM's boundaries; absent in data before it (all Lombardy). */
  region?: string | null;
  /** CG-114: set on a via ferrata turned into a place for the list, the map and the panel. */
  ferrata?: FerrataFacts;
}

/** CG-114: a via ferrata's start or top, with its elevation from the DEM. */
export interface Point {
  latitude: number;
  longitude: number;
  elevation: number | null;
}

/** CG-114: what the page says about a via ferrata beyond its verdict. */
export interface FerrataFacts {
  start: Point;
  top: Point;
  lengthM: number | null;
  /** via_ferrata_scale 0–6 from OSM, the range over its pieces; null when not mapped. */
  scaleMin: number | null;
  scaleMax: number | null;
  /** Values found on OSM that are not on the 0–6 scale ("2+"), dropped. */
  scaleDropped: string[];
  pieces: string[];
}

/** CG-114: one via ferrata in data/conditions.json, placed at its start, scored at its top. */
export interface Ferrata extends FerrataFacts {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  sourceUrl: string;
  days: Record<string, Day>;
}

export interface Conditions {
  generatedAt: string;
  timezone: string;
  days: string[];
  cragData: string;
  weatherData: string;
  elevationData: string;
  avalancheData?: string;
  crags: Crag[];
  /** CG-114: absent when there are none (the Pi's data before CG-115, older data). */
  ferrate?: Ferrata[];
}
