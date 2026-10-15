// The data every end-to-end test sees: six made-up crags over three days from today, served in
// place of data/conditions.json; map tiles answered with a blank image.
import { test as base, expect, type Page } from "@playwright/test";

const iso = (d: Date) => d.toISOString().slice(0, 10);
export const DAYS = [0, 1, 2].map(n => iso(new Date(Date.now() + n * 864e5)));
export const [TODAY, TOMORROW, AFTER] = DAYS;

type V = "GO" | "MAYBE" | "NO_GO";
function crag(id: string, name: string, lat: number, lon: number, province: [string, string, string], verdicts: V[], extra = {}) {
  const days: Record<string, unknown> = {};
  DAYS.forEach((d, i) => {
    const rain = verdicts[i] === "NO_GO" ? 4 : 0;
    days[d] = {
      verdict: verdicts[i],
      reasons: verdicts[i] === "NO_GO" ? ["rain forecast during the day"] : verdicts[i] === "MAYBE" ? ["cold"] : [],
      weather: { rainLast48hMm: 0, rainDuringDayMm: rain, maxTemperatureC: 14 + i, maxWindKmh: 8 },
      hours: Array.from({ length: 16 }, (_, h) => ({ hour: 6 + h, rainMm: rain && h > 3 && h < 8 ? 1 : 0, temperatureC: 8 + h / 2, windKmh: 6 })),
    };
  });
  return { id, name, latitude: lat, longitude: lon, aspect: "S", sourceUrl: `https://www.openstreetmap.org/node/${id.split("-").pop()}`,
           details: {}, elevation: 600, alsoMapped: [], municipality: province[0], province: province[1], provinceCode: province[2],
           days, ...extra };
}

export const CRAGS = [
  crag("osm-node-1", "Còrna Test", 45.86, 9.40, ["Lecco", "Lecco", "LC"], ["GO", "GO", "MAYBE"], { details: { gradeMax: "7a" } }),
  crag("osm-node-2", "Placca Prova", 45.87, 9.42, ["Lecco", "Lecco", "LC"], ["NO_GO", "MAYBE", "GO"]),
  crag("osm-node-3", "Muro Esempio", 46.17, 9.87, ["Sondrio", "Sondrio", "SO"], ["MAYBE", "NO_GO", "GO"]),
  crag("osm-node-4", "Sasso Finto", 45.70, 9.67, ["Bergamo", "Bergamo", "BG"], ["GO", "GO", "GO"]),
  crag("osm-node-5", "Parete Immaginaria", 45.55, 10.22, ["Brescia", "Brescia", "BS"], ["GO", "NO_GO", "NO_GO"]),
  crag("osm-node-6", "Torre Inventata", 46.30, 10.30, ["Bormio", "Sondrio", "SO"], ["MAYBE", "MAYBE", "MAYBE"]),
];

export const conditions = (crags = CRAGS) => ({
  generatedAt: new Date().toISOString(), timezone: "Europe/Rome", days: DAYS,
  cragData: "test", weatherData: "test", elevationData: "test", crags,
});

/** CG-57: forty more made-up crags, for a list long enough to scroll. */
export const MANY = [...CRAGS, ...Array.from({ length: 40 }, (_, i) =>
  crag(`osm-way-${100 + i}`, `Parete Lunga ${String(i).padStart(2, "0")}`, 45.6 + i * 0.01, 9.5, ["Lecco", "Lecco", "LC"], ["GO", "GO", "GO"]))];

const BLANK_PNG = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=", "base64");

export const test = base.extend<{ page: Page }>({
  page: async ({ page }, use) => {
    await page.route("**/data/conditions.json*", r => r.fulfill({ json: conditions() }));
    await page.route("https://tile.openstreetmap.org/**", r => r.fulfill({ body: BLANK_PNG, contentType: "image/png" }));
    await use(page);
  },
});
export { expect };

/** The list's crag names, in order. */
export const listNames = (page: Page) =>
  page.locator("#list li h2").evaluateAll(hs => hs.map(h => (h.textContent ?? "").replace(/^[SFN]\s*/, "").replace(/\d+ km$/, "").trim()));

/** CG-53: on a phone the filters fold behind "Filtri"; open them when they are folded. */
export async function openFilters(page: Page) {
  const toggle = page.getByRole("button", { name: /^Filtri/ });
  if (await toggle.isVisible() && (await toggle.getAttribute("aria-expanded")) === "false") await toggle.click();
}
