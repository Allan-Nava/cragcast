// Words of the page: verdict labels and letters, the reasons and details in Italian.
import type { Aspect, Avalanche, Crag, Details, Verdict } from "../types";

export const LABEL: Record<Verdict, string> = { GO: "Si va", MAYBE: "Forse", NO_GO: "No" };
// CG-21: the verdict is never told by colour alone — circle S, diamond F, square N.
export const LETTER: Record<Verdict, string> = { GO: "S", MAYBE: "F", NO_GO: "N" };
export const ORDER: Record<Verdict, number> = { GO: 0, MAYBE: 1, NO_GO: 2 };
export const VERDICTS: Verdict[] = ["GO", "MAYBE", "NO_GO"];

const REASON_IT: Record<string, string> = {
  "rain forecast during the day": "pioggia prevista durante il giorno",
  "strong wind": "vento forte",
  "rock may still be wet from recent rain": "roccia forse ancora bagnata dalla pioggia recente",
  "cold": "freddo",
  "hot on a sunny wall": "caldo su parete al sole",
};
const ASPECT_IT: Record<Aspect, string> = { N: "N", NE: "NE", E: "E", SE: "SE", S: "S", SW: "SO", W: "O", NW: "NO" };
const STYLE_IT: Record<string, string> = { sport: "sportiva", trad: "trad", multipitch: "vie lunghe" };

/** Lower case, accents removed: "Còrna" and "CORNA" both become "corna". */
export const fold = (s: string): string => s.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase();

/** "Canzo (CO)": where the crag is, from OSM's administrative boundaries (CG-25). */
export const place = (c: Pick<Crag, "municipality" | "provinceCode">): string =>
  c.municipality ? c.municipality + (c.provinceCode ? ` (${c.provinceCode})` : "") : "";

export interface Described {
  verdict: Verdict;
  reasons: string[];
  facts: string;
  aspect: string;
}

export function describe(crag: Crag, day: string): Described {
  const d = crag.days[day];
  const w = d.weather;
  // The unmapped-aspect note goes in the aspect line, not again among the reasons.
  const reasons = d.reasons.filter(r => !r.startsWith("wall aspect not mapped")).map(r => REASON_IT[r] ?? r);
  const facts = `pioggia ultime 48 h ${w.rainLast48hMm.toFixed(1)} mm · in giornata ${w.rainDuringDayMm.toFixed(1)} mm · ` +
    `max ${Math.round(w.maxTemperatureC)} °C · vento ${Math.round(w.maxWindKmh)} km/h`;
  const aspect = crag.aspect ? `parete esposta a ${ASPECT_IT[crag.aspect]}`
    : "esposizione non mappata: valutata come parete in ombra";
  return { verdict: d.verdict, reasons, facts, aspect };
}

/** The OSM climbing details that exist for a crag, as short Italian phrases; [] when none. */
export function detailParts(d: Details | undefined): string[] {
  if (!d) return [];
  const parts: string[] = [];
  if (d.gradeMin || d.gradeMax) {
    const range = d.gradeMin && d.gradeMax ? `${d.gradeMin}–${d.gradeMax}` : d.gradeMin ? `da ${d.gradeMin}` : `fino a ${d.gradeMax}`;
    parts.push(`gradi ${range}` + (d.gradeMean ? ` (media ${d.gradeMean})` : ""));
  } else if (d.gradeMean) {
    parts.push(`grado medio ${d.gradeMean}`);
  }
  // A no-break space ties each number to its unit, so "m" never wraps alone (CG-58).
  if (d.lengthMin && d.lengthMax) parts.push(`vie da ${d.lengthMin} a ${d.lengthMax}\u00a0m`);
  else if (d.lengthMax) parts.push(`vie fino a ${d.lengthMax}\u00a0m`);
  else if (d.length) parts.push(`vie di ${d.length}\u00a0m`);
  if (d.routes) parts.push(`${d.routes} vie`);
  if (d.rock) parts.push(d.rockIt || d.rock);
  if (d.styles && d.styles.length) parts.push(d.styles.map(x => STYLE_IT[x] ?? x).join(", "));
  if (d.bolted) parts.push("chiodata");
  return parts;
}

/** Elevation first, then the OSM details: the line under each crag. */
export const allDetails = (c: Crag): string[] =>
  (c.elevation != null ? [`quota circa ${c.elevation}\u00a0m`] : []).concat(detailParts(c.details));

/** https://www.openstreetmap.org/node/123 → https://www.openstreetmap.org/edit?node=123 */
export function osmEditUrl(sourceUrl: string): string | null {
  const m = sourceUrl.match(/openstreetmap\.org\/(node|way|relation)\/(\d+)/);
  return m ? `https://www.openstreetmap.org/edit?${m[1]}=${m[2]}` : null;
}

export const dayLabel = (i: number, day: string): string => ["Oggi", "Domani", "Dopodomani"][i] ?? day;

// CG-11: the avalanche bulletin in words. The European scale's own Italian names.
const DANGER_IT = ["", "debole", "moderato", "marcato", "forte", "molto forte"];
const PROBLEM_IT: Record<string, string> = {
  new_snow: "neve fresca", wind_slab: "neve ventata", persistent_weak_layers: "strati deboli persistenti",
  wet_snow: "neve bagnata", gliding_snow: "valanghe di slittamento", cornices: "cornici",
  favourable_situation: "situazione favorevole", no_distinct_avalanche_problem: "nessun problema evidente",
};
const bound = (b: string): string => (b === "treeline" ? "il limite del bosco" : `${b} m`);

/** "pericolo 3 (marcato)": the day's highest level. */
export const dangerLabel = (level: number): string => `pericolo ${level} (${DANGER_IT[level] ?? "?"})`;

/** One phrase per published rating: "3 marcato sopra 2000 m, pomeriggio". */
export function ratingParts(a: Avalanche): string[] {
  return a.ratings.map(r => {
    const where = r.above && r.below ? ` tra ${bound(r.above)} e ${bound(r.below)}`
      : r.above ? ` sopra ${bound(r.above)}` : r.below ? ` sotto ${bound(r.below)}` : "";
    const when = r.period === "earlier" ? ", mattino" : r.period === "later" ? ", pomeriggio" : "";
    return `${r.level} ${DANGER_IT[r.level] ?? ""}${where}${when}`;
  });
}

export const problemParts = (a: Avalanche): string[] => a.problems.map(p => PROBLEM_IT[p] ?? p.replace(/_/g, " "));

/** "Retiche orientali": the bulletin's zone name, which comes in capitals. */
export const zoneName = (a: Avalanche): string =>
  a.name ? a.name.charAt(0) + a.name.slice(1).toLowerCase() : a.region;

/** CG-58: the aspect for a list card — "esposta a SO", or null when not mapped (the card then
 * shows the short "esposizione ?" tag; the panel and the popup keep the full sentence). */
export const aspectShort = (c: Pick<Crag, "aspect">): string | null =>
  c.aspect ? `esposta a ${ASPECT_IT[c.aspect]}` : null;

/** CG-59: how many OSM objects a crag was folded from (itself and alsoMapped). */
export const mappedTimes = (c: Pick<Crag, "alsoMapped">): number => 1 + (c.alsoMapped?.length ?? 0);
