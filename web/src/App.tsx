// Reads data/conditions.json, written every hour by the GitHub Action (ConditionsExport), and
// shows one mark per crag for the chosen day. Search, verdict filter, province and day live in
// the URL (?q=&v=&p=&day=), so a link opens the same view and back works.
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { Conditions, Crag, Verdict } from "./types";
import { ORDER, fold, place } from "./lib/text";
import { km } from "./lib/geo";
import { inArea, readState, stateQuery, type Area, type ViewState } from "./lib/state";
import { Controls } from "./components/Controls";
import { CragMap, type MapApi } from "./components/CragMap";
import { CragList } from "./components/CragList";
import { DetailDialog } from "./components/DetailDialog";
import { Help } from "./components/Help";
import { Footer } from "./components/Footer";

/** The crags the view shows, in the order of the list. */
export function visible(crags: Crag[], s: ViewState, distance: ((c: Crag) => number) | null): Crag[] {
  const q = fold(s.q.trim());
  return crags
    .filter(c => s.verdicts.has(c.days[s.day].verdict))
    .filter(c => !q || fold(c.name).includes(q) || fold(place(c)).includes(q))
    .filter(c => !s.province || c.provinceCode === s.province)
    .filter(c => !s.area || inArea(s.area, c.latitude, c.longitude))
    .sort(distance
      ? (a, b) => distance(a) - distance(b)
      : (a, b) => ORDER[a.days[s.day].verdict] - ORDER[b.days[s.day].verdict] || a.name.localeCompare(b.name, "it"));
}

export function App() {
  const [data, setData] = useState<Conditions | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [view, setView] = useState<ViewState | null>(null);
  // CG-22: the reader's position lives only in this state — never in the URL, never sent.
  const [here, setHere] = useState<{ lat: number; lon: number } | null>(null);
  const [asking, setAsking] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [detail, setDetail] = useState<Crag | null>(null);
  // CG-38: the crag under the pointer or keyboard focus in the list, and the one last chosen.
  const [hot, setHot] = useState<string | null>(null);
  const [chosen, setChosen] = useState<string | null>(null);
  const mapApi = useRef<MapApi | null>(null);

  useEffect(() => {
    fetch("data/conditions.json", { cache: "no-cache" })
      .then(r => { if (!r.ok) throw new Error(String(r.status)); return r.json() as Promise<Conditions>; })
      .then(d => { setData(d); setView(readState(location.search, d.days)); })
      .catch((e: Error) => setError(e.message));
  }, []);

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
  const shown = useMemo(() => data && view ? visible(data.crags, view, distance) : [], [data, view, distance]);
  const openDetail = useCallback((c: Crag) => setDetail(c), []);
  const onChosen = useCallback((id: string) => setChosen(id), []);
  const onArea = (area: Area | null) => { if (view) go({ ...view, area }, true); };

  const near = () => {
    if (here) { setHere(null); setNotice(null); return; }       // second click: back to the verdict order
    if (!navigator.geolocation) { setNotice("Il browser non dà la posizione."); return; }
    setAsking(true);
    navigator.geolocation.getCurrentPosition(pos => {
      setAsking(false);
      setNotice(null);
      setHere({ lat: pos.coords.latitude, lon: pos.coords.longitude });
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
  const count = notice ?? (!data ? "" : shown.length === total ? `${shown.length} falesie` : `${shown.length} di ${total} falesie`);

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
                state={view ?? { day: "", q: "", verdicts: new Set(["GO", "MAYBE", "NO_GO"]), province: "", area: null }}
                near={asking ? "asking" : here ? "on" : "off"} count={count}
                onDay={day => view && go({ ...view, day }, true)}
                onQuery={q => view && go({ ...view, q }, false)}
                onToggle={toggle}
                onProvince={province => view && go({ ...view, province }, true)}
                onNear={near} />
      <main>
        <CragMap crags={shown} day={view?.day ?? ""} here={here} onDetail={openDetail} apiRef={mapApi}
                 hot={hot} area={view?.area ?? null} onArea={onArea} onChosen={onChosen} />
        {data && view
          ? <CragList crags={shown} day={view.day} distance={distance} chosen={chosen}
                      onOpen={c => { setChosen(c.id); mapApi.current?.open(c.id); }} onDetail={openDetail} onHot={setHot} />
          : <ul id="list" />}
      </main>
      <DetailDialog crag={detail} day={view?.day ?? ""} onClose={() => setDetail(null)} />
      <Help />
      <Footer generatedAt={data?.generatedAt ?? null} error={error} />
    </>
  );
}
