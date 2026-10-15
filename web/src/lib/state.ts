// The view in the URL (?q=&v=&p=&day=), so a link opens the same view and back works (CG-20).
// The reader's position is never part of it (CG-22).
import type { Verdict } from "../types";
import { ORDER, VERDICTS } from "./text";

export interface ViewState {
  day: string;
  q: string;
  verdicts: Set<Verdict>;
  province: string;
}

export function readState(search: string, days: string[]): ViewState {
  const p = new URLSearchParams(search);
  const day = p.get("day");
  const v = (p.get("v") ?? "").split(",").filter((x): x is Verdict => x in ORDER);
  return {
    day: day && days.includes(day) ? day : days[Math.min(1, days.length - 1)],
    q: p.get("q") ?? "",
    verdicts: new Set(v.length ? v : VERDICTS),
    province: p.get("p") ?? "",
  };
}

export function stateQuery(s: ViewState): string {
  const p = new URLSearchParams();
  if (s.q) p.set("q", s.q);
  if (s.verdicts.size < 3) p.set("v", VERDICTS.filter(v => s.verdicts.has(v)).join(","));
  if (s.province) p.set("p", s.province);
  p.set("day", s.day);
  return "?" + p.toString();
}
