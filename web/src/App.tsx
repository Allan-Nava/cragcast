// Reads data/conditions.json, written every hour by the GitHub Action (ConditionsExport), and
// shows one mark per crag for the chosen day. Search, verdict filter, province, grades, order
// and day live in the URL (?q=&v=&p=&g=&o=&day=), so a link opens the same view and back works.
import { Suspense, lazy, useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { Conditions, Crag, Verdict } from "./types";
import { ORDER, fold, place } from "./lib/text";
import { km } from "./lib/geo";
import { inArea, readState, stateQuery, type Area, type ViewState } from "./lib/state";
import { Controls } from "./components/Controls";
import { CragMap, type MapApi } from "./components/CragMap";
import { CragList } from "./components/CragList";
import { Help } from "./components/Help";
import { Footer } from "./components/Footer";
import { Summary } from "./components/Summary";
import { BackToTop } from "./components/BackToTop";
import { loadFavourites, loadOnly, saveFavourites, saveOnly } from "./lib/favourites";
import { hasGrades, inGrade } from "./lib/grades";

/** The crags matching search, province, area, (CG-46) favourites and (CG-61) grades — every
 * verdict (CG-39 counts them). */
export function matching(crags: Crag[], s: ViewState, favourites: Set<string> | null = null): Crag[] {
  const q = fold(s.q.trim());
  return crags
    .filter(c => !favourites || favourites.has(c.id))
    .filter(c => !q || fold(c.name).includes(q) || fold(place(c)).includes(q))
    .filter(c => !s.province || c.provinceCode === s.province)
    .filter(c => !s.area || inArea(s.area, c.latitude, c.longitude))
    .filter(c => !s.grade || inGrade(c.details, s.grade));
}

/** CG-61: how many crags the grade filter leaves out only because OSM has no grade for them. */
export const ungraded = (crags: Crag[], s: ViewState, favourites: Set<string> | null = null): number =>
  s.grade ? matching(crags, { ...s, grade: null }, favourites).filter(c => !hasGrades(c.details)).length : 0;

const byName = (a: Crag, b: Crag) => a.name.localeCompare(b.name, "it");
const byVerdict = (day: string) => (a: Crag, b: Crag) => ORDER[a.days[day].verdict] - ORDER[b.days[day].verdict] || byName(a, b);
/** Elevation, the unknown ones last whichever way. */
const byElevation = (sign: 1 | -1) => (a: Crag, b: Crag) =>
  (a.elevation == null ? 1 : 0) - (b.elevation == null ? 1 : 0)
  || (a.elevation != null && b.elevation != null ? sign * (a.elevation - b.elevation) : 0) || byName(a, b);

/** The crags the view shows, in the order of the list: by distance when given, else the view's. */
export function visible(crags: Crag[], s: ViewState, distance: ((c: Crag) => number) | null,
                        favourites: Set<string> | null = null): Crag[] {
  const order = distance ? (a: Crag, b: Crag) => distance(a) - distance(b)
    : s.order === "name" ? byName
    : s.order === "low" ? byElevation(1)
    : s.order === "high" ? byElevation(-1)
    : byVerdict(s.day);
  return matching(crags, s, favourites).filter(c => s.verdicts.has(c.days[s.day].verdict)).sort(order);
}

/** CG-54: the width below which the page shows the map or the list, not both (style.css). */
const narrow = () => typeof matchMedia === "function" && matchMedia("(max-width: 599px)").matches;

// CG-64: the crag's panel and its charts are not needed for the first paint: they come in their
// own chunk, fetched when the browser is idle or at the first "Dettagli", whichever is first.
const loadPanel = () => import("./components/DetailDialog");
const DetailDialog = lazy(() => loadPanel().then(m => ({ default: m.DetailDialog })));

/** How often the page looks for new data; the data itself changes hourly, at about :20. */
export const REFRESH_MS = 10 * 60 * 1000;

export function App() {
  const [data, setData] = useState<Conditions | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [view, setView] = useState<ViewState | null>(null);
  // CG-22: the reader's position lives only in this state — never in the URL, never sent.
  const [here, setHere] = useState<{ lat: number; lon: number } | null>(null);
  const [asking, setAsking] = useState(false);
  // CG-61: whether the list follows the distance — on with "Vicino a me", off when the reader
  // picks another order; in memory only, as the position is.
  const [byDistance, setByDistance] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  // CG-38: the crag under the pointer or keyboard focus in the list, and the one last chosen.
  const [hot, setHot] = useState<string | null>(null);
  const [chosen, setChosen] = useState<string | null>(null);
  // CG-46: favourites live in this browser only.
  const [favourites, setFavourites] = useState(loadFavourites);
  const [onlyFavourites, setOnlyFavourites] = useState(loadOnly);
  const toggleFavourite = useCallback((id: string) => setFavourites(prev => {
    const next = new Set(prev);
    if (next.has(id)) next.delete(id); else next.add(id);
    saveFavourites(next);
    return next;
  }), []);
  const scope = onlyFavourites ? favourites : null;
  const mapApi = useRef<MapApi | null>(null);

  useEffect(() => {
    fetch("data/conditions.json", { cache: "no-cache" })
      .then(r => { if (!r.ok) throw new Error(String(r.status)); return r.json() as Promise<Conditions>; })
      .then(d => { setData(d); setView(readState(location.search, d.days)); })
      .catch((e: Error) => setError(e.message));
  }, []);

  // CG-41: new data without a reload — every REFRESH_MS while the page is visible, and when the
  // reader comes back to it. The view, the open panel and the position stay; a failed fetch
  // keeps the data shown (the footer then says how old it is).
  useEffect(() => {
    if (!data) return;
    const shown = data.generatedAt;
    let busy = false;
    const refresh = () => {
      if (busy || document.visibilityState === "hidden") return;
      busy = true;
      fetch("data/conditions.json", { cache: "no-cache" })
        .then(r => (r.ok ? (r.json() as Promise<Conditions>) : null))
        .then(d => {
          if (!d || d.generatedAt === shown) return;
          setData(d);
          // Past midnight a day drops out of the data: fall back as a fresh visit would.
          setView(v => (v && !d.days.includes(v.day) ? { ...v, day: readState("", d.days).day } : v));
        })
        .catch(() => {})
        .finally(() => { busy = false; });
    };
    const timer = setInterval(refresh, REFRESH_MS);
    document.addEventListener("visibilitychange", refresh);
    return () => { clearInterval(timer); document.removeEventListener("visibilitychange", refresh); };
  }, [data]);

  useEffect(() => {
    if (!data) return;
    const back = () => setView(readState(location.search, data.days));
    addEventListener("popstate", back);
    return () => removeEventListener("popstate", back);
  }, [data]);

  /** A new view: pushed to history for a click, replaced while typing. */
  const go = (next: ViewState, push: boolean) => {
    const url = location.pathname + stateQuery(next);
    if (push) history.pushState(null, "", url); else history.replaceState(null, "", url);
    setNotice(null);
    setView(next);
  };

  const provinces = useMemo(() => {
    if (!data) return [];
    const names = new Map<string, string>();
    data.crags.forEach(c => { if (c.provinceCode) names.set(c.provinceCode, c.province ?? c.provinceCode); });
    return [...names].sort((a, b) => a[1].localeCompare(b[1], "it"))
      .map(([code, name]) => ({ code, name, count: data.crags.filter(c => c.provinceCode === code).length }));
  }, [data]);

  const distance = useMemo(() => here ? (c: Crag) => km(here.lat, here.lon, c.latitude, c.longitude) : null, [here]);
  const sortDistance = byDistance ? distance : null;
  const shown = useMemo(() => data && view ? visible(data.crags, view, sortDistance, scope) : [], [data, view, sortDistance, scope]);
  // CG-44: the open panel lives in the URL, so a link opens on it and back closes it.
  const detail = data && view?.crag ? data.crags.find(c => c.id === view.crag) ?? null : null;
  const viewRef = useRef(view);
  viewRef.current = view;
  const openDetail = useCallback((c: Crag) => {
    const v = viewRef.current;
    if (v && v.crag !== c.id) go({ ...v, crag: c.id }, true);
  }, []);
  // CG-64: fetch the panel's chunk once the page is idle, so "Dettagli" opens at once. Once a
  // panel has been wanted it stays mounted, so its slide-out on close still runs.
  const [panelWanted, setPanelWanted] = useState(false);
  useEffect(() => { if (detail) setPanelWanted(true); }, [detail]);
  useEffect(() => {
    const idle = (window as { requestIdleCallback?: (f: () => void) => number }).requestIdleCallback;
    const id = idle ? idle(() => { void loadPanel(); }) : setTimeout(() => { void loadPanel(); }, 2000);
    return () => { if (!idle) clearTimeout(id); };
  }, []);

  // CG-54: a map that was hidden has no size; measure it when it comes back.
  useEffect(() => { if (view?.show === "map") mapApi.current?.refresh(); }, [view?.show]);

  // A link straight to a crag: once the map has its marks, fly to it.
  const landed = useRef(false);
  useEffect(() => {
    if (landed.current || !detail) return;
    landed.current = true;
    setChosen(detail.id);
    const t = setTimeout(() => mapApi.current?.open(detail.id), 0);
    return () => clearTimeout(t);
  }, [detail]);
  const onChosen = useCallback((id: string) => setChosen(id), []);
  const onArea = (area: Area | null) => { if (view) go({ ...view, area }, true); };

  const near = () => {
    if (here) { setHere(null); setByDistance(false); setNotice(null); return; }   // second click: back to the view's order
    if (!navigator.geolocation) { setNotice("Il browser non dà la posizione."); return; }
    setAsking(true);
    navigator.geolocation.getCurrentPosition(pos => {
      setAsking(false);
      setNotice(null);
      setHere({ lat: pos.coords.latitude, lon: pos.coords.longitude });
      setByDistance(true);
    }, err => {
      setAsking(false);
      setNotice(err.code === err.PERMISSION_DENIED
        ? "Posizione non concessa: l'ordine resta per verdetto."
        : "Posizione non disponibile al momento.");
    }, { enableHighAccuracy: false, timeout: 10000, maximumAge: 600000 });
  };

  const toggle = (v: Verdict) => {
    if (!view) return;
    const verdicts = new Set(view.verdicts);
    if (verdicts.has(v)) verdicts.delete(v); else verdicts.add(v);
    go({ ...view, verdicts }, true);
  };

  const total = data?.crags.length ?? 0;
  const active = view ? [view.verdicts.size < 3, !!view.province, !!view.grade, !!view.area, !!here, onlyFavourites].filter(Boolean).length : 0;
  const unknown = data && view ? ungraded(data.crags, view, scope) : 0;
  const count = notice ?? (!data ? (error ? "" : "Carico le falesie…") : (shown.length === total ? `${shown.length} falesie` : `${shown.length} di ${total} falesie`)
    + (unknown ? ` · ${unknown} senza gradi su OpenStreetMap, escluse` : ""));

  return (
    <>
      <header>
        <img src="logo.svg" alt="" width={40} height={40} />
        <div>
          <h1>cragcast</h1>
          <p>Si arrampica domani? Le falesie della Lombardia, dal meteo e dall'esposizione della parete.</p>
        </div>
      </header>
      <Controls days={data?.days ?? []} provinces={provinces}
                state={view ?? { day: "", q: "", verdicts: new Set(["GO", "MAYBE", "NO_GO"]), province: "", area: null, crag: null, show: "list", grade: null, order: "verdict" }}
                byDistance={byDistance && !!here}
                near={asking ? "asking" : here ? "on" : "off"} active={active}
                             onDay={day => view && go({ ...view, day }, true)}
                onQuery={q => view && go({ ...view, q }, false)}
                onToggle={toggle}
                onProvince={province => view && go({ ...view, province }, true)}
                onGrade={grade => view && go({ ...view, grade }, true)}
                onOrder={order => {
                  if (order === "distance") { setByDistance(true); return; }
                  setByDistance(false);
                  if (view) go({ ...view, order }, true);
                }}
                onNear={near}
                favourites={favourites.size} onlyFavourites={onlyFavourites}
                onOnlyFavourites={() => setOnlyFavourites(on => { saveOnly(!on); return !on; })} />
      {data && view
        ? <Summary crags={matching(data.crags, view, scope)} day={view.day} index={data.days.indexOf(view.day)} count={count} />
        : <p className={error ? "summary" : "summary ghost"}><span id="count" aria-live="polite">{count}</span></p>}
      {/* CG-54: on a phone, the map or the list; hidden from 600 px, where both show. */}
      <div className="show" role="group" aria-label="Vista">
        {(["list", "map"] as const).map(s => (
          <button key={s} type="button" aria-pressed={(view?.show ?? "list") === s}
                  onClick={() => view && view.show !== s && go({ ...view, show: s }, true)}>
            {s === "list" ? "Lista" : "Mappa"}
          </button>
        ))}
      </div>
      <main className={"show-" + (view?.show ?? "list") + (detail ? " panel-open" : "")}>
        <CragMap crags={shown} day={view?.day ?? ""} here={here} onDetail={openDetail} apiRef={mapApi}
                 hot={hot} area={view?.area ?? null} onArea={onArea} onChosen={onChosen} />
        {data && view
          ? <CragList crags={shown} day={view.day} days={data.days} onDay={day => go({ ...view, day }, true)}
                      distance={distance} chosen={chosen} favourites={favourites} onFavourite={toggleFavourite}
                      empty={onlyFavourites && favourites.size === 0
                        ? "Nessuna preferita: tocca la stella di una falesia per aggiungerla." : undefined}
                      onOpen={c => {
                        setChosen(c.id);
                        // On a phone the list hides the map: show it, then fly (CG-54).
                        if (narrow() && view.show !== "map") go({ ...view, show: "map" }, true);
                        setTimeout(() => mapApi.current?.open(c.id), 0);
                      }} onDetail={openDetail} onHot={setHot} />
          : <ul id="list" className={error ? undefined : "loading"} aria-busy={!error}>
              {!error && [0, 1, 2, 3, 4].map(i => <li key={i} className="ghost" aria-hidden="true" />)}
            </ul>}
        {/* CG-56: inside the grid, so on a desktop the panel takes the list's column. */}
        {panelWanted && (
          <Suspense fallback={null}>
            <DetailDialog crag={detail} day={view?.day ?? ""} days={data?.days ?? []}
                         favourite={detail ? favourites.has(detail.id) : false} onFavourite={toggleFavourite}
            onDay={day => view && go({ ...view, day }, true)}
                         onClose={() => { if (viewRef.current?.crag) go({ ...viewRef.current, crag: null }, true); }} />
          </Suspense>
        )}
      </main>
      <BackToTop />
      <Help />
      <Footer generatedAt={data?.generatedAt ?? null} error={error} />
    </>
  );
}
