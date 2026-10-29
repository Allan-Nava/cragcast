// Words of the page: verdict labels and letters, the reasons and details in Italian.
import type { Aspect, Avalanche, Crag, Details, Verdict, Weather } from "../types";
import { ferrataParts } from "./ferrate";

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
  // CG-114: the via ferratas' own reasons (FerrataScorer).
  "thunderstorm forecast": "temporale previsto",
  "unstable air: storms can build": "aria instabile: si possono formare temporali",
  "some instability: storms possible": "un po' di instabilità: temporali possibili",
  "fresh snow at the top": "neve fresca in cima",
  "ice likely: rain, then frost at the top": "probabile ghiaccio: pioggia e poi gelo in cima",
  "strong gusts at the top": "raffiche forti in cima",
  "windy at the top": "vento in cima",
  "rock and cables may still be wet from recent rain": "roccia e cavi forse ancora bagnati dalla pioggia recente",
  "cold at the top": "freddo in cima",
  "hot: the cables burn in the sun": "caldo: i cavi scottano al sole",
  "short day: little light for the route": "giornata corta: poca luce per l'itinerario",
};
export const ASPECT_IT: Record<Aspect, string> = { N: "N", NE: "NE", E: "E", SE: "SE", S: "S", SW: "SO", W: "O", NW: "NO" };
const STYLE_IT: Record<string, string> = { sport: "sportiva", trad: "trad", multipitch: "vie lunghe" };

/** Lower case, accents removed: "Còrna" and "CORNA" both become "corna". */
export const fold = (s: string): string => s.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase();

/** "Canzo (CO)": where the crag is, from OSM's administrative boundaries (CG-25). */
export const place = (c: Pick<Crag, "municipality" | "provinceCode">): string =>
  c.municipality ? c.municipality + (c.provinceCode ? ` (${c.provinceCode})` : "") : "";

export interface Described {
  verdict: Verdict;
  reasons: string[];
  aspect: string;
}

export function describe(crag: Crag, day: string): Described {
  const d = crag.days[day];
  // The unmapped-aspect note goes in the aspect line, not again among the reasons.
  const reasons = d.reasons.filter(r => !r.startsWith("wall aspect not mapped")).map(r => REASON_IT[r] ?? r);
  // CG-114: a via ferrata has no wall aspect; the line says what kind of place it is instead.
  const aspect = crag.ferrata ? "via ferrata, meteo in cima"
    : crag.aspect ? `parete esposta a ${ASPECT_IT[crag.aspect]}`
    : "esposizione non mappata: valutata come parete in ombra";
  return { verdict: d.verdict, reasons, aspect };
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
  c.ferrata ? ferrataParts(c) : (c.elevation != null ? [`quota circa ${c.elevation}\u00a0m`] : []).concat(detailParts(c.details));

/** https://www.openstreetmap.org/node/123 → https://www.openstreetmap.org/edit?node=123 */
export function osmEditUrl(sourceUrl: string): string | null {
  const m = sourceUrl.match(/openstreetmap\.org\/(node|way|relation)\/(\d+)/);
  return m ? `https://www.openstreetmap.org/edit?${m[1]}=${m[2]}` : null;
}

/** CG-117: an object's page on OSM from its id: "osm-node-123" -> …/node/123. */
export const osmUrl = (id: string): string => {
  const [, kind, n] = id.split("-");
  return `https://www.openstreetmap.org/${kind}/${n}`;
};

export const dayLabel = (i: number, day: string): string => ["Oggi", "Domani", "Dopodomani"][i] ?? day;

/** CG-125: "1 falesia", "6 falesie", "1 via ferrata", "0 vie ferrate". */
export function places(n: number, kind: "crags" | "ferrate"): string {
  const [one, many] = kind === "ferrate" ? ["via ferrata", "vie ferrate"] : ["falesia", "falesie"];
  return `${n} ${n === 1 ? one : many}`;
}

/** CG-125: the list's count — "6 falesie", or "1 di 6 falesie" when filters leave some out. */
export const shownOf = (shown: number, total: number, kind: "crags" | "ferrate"): string =>
  shown === total ? places(total, kind) : `${shown} di ${places(total, kind)}`;

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
export const aspectShort = (c: Pick<Crag, "aspect" | "ferrata">): string | null =>
  c.ferrata ? null : c.aspect ? `esposta a ${ASPECT_IT[c.aspect]}` : null;

/** CG-59: how many OSM objects a crag was folded from (itself and alsoMapped). */
export const mappedTimes = (c: Pick<Crag, "alsoMapped">): number => 1 + (c.alsoMapped?.length ?? 0);

const num = (n: number, d: number) => n.toLocaleString("it-IT", { minimumFractionDigits: d, maximumFractionDigits: d });

/** CG-59: the weather the verdict was made from, as label and value pairs for a small table,
 * numbers in Italian ("0,4 mm", not "0.4 mm"). */
export function weatherRows(w: Weather): [string, string][] {
  return [
    ["Pioggia 48 h prima", `${num(w.rainLast48hMm, 1)}\u00a0mm`],
    ["Pioggia in giornata", `${num(w.rainDuringDayMm, 1)}\u00a0mm`],
    ["Massima", `${num(w.maxTemperatureC, 0)}\u00a0°C`],
    ["Vento massimo", `${num(w.maxWindKmh, 0)}\u00a0km/h`],
  ];
}
