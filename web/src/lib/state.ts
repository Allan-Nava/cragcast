// The view in the URL (?q=&v=&p=&a=&crag=&day=), so a link opens the same view and back works (CG-20).
// The reader's position is never part of it (CG-22).
import type { Verdict } from "../types";
import { ORDER, VERDICTS } from "./text";

export interface ViewState {
  day: string;
  q: string;
  verdicts: Set<Verdict>;
  province: string;
  /** CG-38: "Cerca in quest'area" — south, west, north, east; null for everywhere. */
  area: Area | null;
  /** CG-44: the crag whose panel is open; a link with ?crag= opens on it. */
  crag: string | null;
}

export type Area = [number, number, number, number];

/** The area from ?a=s,w,n,e; null when absent or malformed. */
function readArea(a: string | null): Area | null {
  if (!a) return null;
  const n = a.split(",").map(Number);
  if (n.length !== 4 || n.some(x => !Number.isFinite(x)) || n[0] >= n[2] || n[1] >= n[3]) return null;
  return n as Area;
}

export const inArea = (a: Area, lat: number, lon: number): boolean =>
  lat >= a[0] && lat <= a[2] && lon >= a[1] && lon <= a[3];

export function readState(search: string, days: string[]): ViewState {
  const p = new URLSearchParams(search);
  const day = p.get("day");
  const v = (p.get("v") ?? "").split(",").filter((x): x is Verdict => x in ORDER);
  return {
    day: day && days.includes(day) ? day : days[Math.min(1, days.length - 1)],
    q: p.get("q") ?? "",
    verdicts: new Set(v.length ? v : VERDICTS),
    province: p.get("p") ?? "",
    area: readArea(p.get("a")),
    crag: p.get("crag") || null,
  };
}

export function stateQuery(s: ViewState): string {
  const p = new URLSearchParams();
  if (s.q) p.set("q", s.q);
  if (s.verdicts.size < 3) p.set("v", VERDICTS.filter(v => s.verdicts.has(v)).join(","));
  if (s.province) p.set("p", s.province);
  if (s.area) p.set("a", s.area.map(x => x.toFixed(3)).join(","));
  if (s.crag) p.set("crag", s.crag);
  p.set("day", s.day);
  return "?" + p.toString();
}

/** CG-44: the link to share for one crag — the page and the crag only, so it opens on the
 * day that is "tomorrow" for whoever follows it. */
export const cragLink = (base: string, id: string): string => `${base}?crag=${encodeURIComponent(id)}`;
