// CG-46: favourite crags, kept in this browser only — never in the URL, never sent, like the
// reader's position (CG-22). Storage can be missing or refuse (private windows, blocked site
// data): then favourites last for the visit and nothing breaks.
const KEY = "cragcast:favourites";
const ONLY = "cragcast:onlyFavourites";

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
