import type { Crag, Hour } from "../types";

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
