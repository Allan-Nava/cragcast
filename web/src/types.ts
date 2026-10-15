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

export interface Day {
  verdict: Verdict;
  reasons: string[];
  weather: Weather;
  hours: Hour[];
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
}

export interface Conditions {
  generatedAt: string;
  timezone: string;
  days: string[];
  cragData: string;
  weatherData: string;
  elevationData: string;
  crags: Crag[];
}
