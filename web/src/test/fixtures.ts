import type { Crag, Ferrata, Hour } from "../types";

export const hours: Hour[] = Array.from({ length: 16 }, (_, i) => ({
  hour: 6 + i, rainMm: i === 8 ? 1.2 : 0, temperatureC: 10 + i, windKmh: 5,
}));

export function crag(over: Partial<Crag> & { verdict?: "GO" | "MAYBE" | "NO_GO" } = {}): Crag {
  const { verdict = "GO", ...rest } = over;
  return {
    id: "node/1", name: "Corna di Medale", latitude: 45.86, longitude: 9.4, aspect: "S",
    sourceUrl: "https://www.openstreetmap.org/node/1", details: {}, elevation: null, alsoMapped: [],
    municipality: "Lecco", province: "Lecco", provinceCode: "LC",
    days: {
      "2026-10-16": {
        verdict, reasons: verdict === "GO" ? [] : ["rain forecast during the day"],
        weather: { rainLast48hMm: 0, rainDuringDayMm: 1.2, maxTemperatureC: 25, maxWindKmh: 5 }, hours,
      },
    },
    ...rest,
  };
}

/** CG-114: a made-up via ferrata, as data/conditions.json lists it. */
export function ferrata(over: Partial<Ferrata> & { verdict?: "GO" | "MAYBE" | "NO_GO" } = {}): Ferrata {
  const { verdict = "GO", ...rest } = over;
  return {
    id: "osm-way-10", name: "Ferrata Prova", latitude: 45.9, longitude: 9.45, sourceUrl: "https://www.openstreetmap.org/way/10",
    pieces: ["https://www.openstreetmap.org/way/10", "https://www.openstreetmap.org/way/11"],
    start: { latitude: 45.9, longitude: 9.45, elevation: 900 }, top: { latitude: 45.91, longitude: 9.46, elevation: 1300 },
    lengthM: 450, scaleMin: 2, scaleMax: 3, scaleDropped: [],
    days: {
      "2026-10-16": {
        verdict, reasons: verdict === "GO" ? [] : ["thunderstorm forecast"],
        weather: { rainLast48hMm: 0, rainDuringDayMm: 0, maxTemperatureC: 12, maxWindKmh: 10 }, hours,
      },
    },
    ...rest,
  };
}
