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
}
