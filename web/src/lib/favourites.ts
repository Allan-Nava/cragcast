// CG-46: favourite crags, kept in this browser only — never in the URL, never sent, like the
// reader's position (CG-22). Storage can be missing or refuse (private windows, blocked site
// data): then favourites last for the visit and nothing breaks.
const KEY = "cragcast:favourites";
const ONLY = "cragcast:onlyFavourites";
/** CG-129: the last region chosen, the same way — this browser only, never sent. */
const REGION = "cragcast:region";

export function loadFavourites(): Set<string> {
  try {
    const v: unknown = JSON.parse(localStorage.getItem(KEY) ?? "[]");
    return new Set(Array.isArray(v) ? v.filter((x): x is string => typeof x === "string") : []);
  } catch {
    return new Set();
  }
}

export function saveFavourites(ids: Set<string>): void {
  try { localStorage.setItem(KEY, JSON.stringify([...ids])); } catch { /* kept for this visit only */ }
}

export function loadOnly(): boolean {
  try { return localStorage.getItem(ONLY) === "1"; } catch { return false; }
}

export function saveOnly(on: boolean): void {
  try { localStorage.setItem(ONLY, on ? "1" : "0"); } catch { /* this visit only */ }
}

/** CG-129: the region chosen last in this browser, if it is one cragcast knows; else null. */
export function loadRegion(known: string[]): string | null {
  try {
    const v = localStorage.getItem(REGION);
    return v && known.includes(v) ? v : null;
  } catch {
    return null;
  }
}

export function saveRegion(region: string): void {
  try { localStorage.setItem(REGION, region); } catch { /* this visit only */ }
}
