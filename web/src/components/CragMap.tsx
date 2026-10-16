import { useEffect, useRef, useState } from "react";
import L from "../lib/leaflet";
import "leaflet.markercluster";
import type { Crag, Verdict } from "../types";
import type { Area } from "../lib/state";
import { LABEL, LETTER, allDetails, dangerLabel, describe, mappedTimes, osmEditUrl, place, zoneName } from "../lib/text";

export interface MapApi {
  /** Zoom into the cluster a crag sits in and open its popup. */
  open(id: string): void;
  /** CG-54: the map was hidden and is shown again: measure it afresh. */
  refresh(): void;
}

interface Props {
  crags: Crag[];
  day: string;
  here: { lat: number; lon: number } | null;
  onDetail(crag: Crag): void;
  apiRef: React.RefObject<MapApi | null>;
  /** CG-38: the crag to light up (hovered or focused in the list). */
  hot?: string | null;
  area?: Area | null;
  onArea?(area: Area | null): void;
  onChosen?(id: string): void;
}

const reducedMotion = () => typeof matchMedia === "function" && matchMedia("(prefers-reduced-motion: reduce)").matches;

function verdictIcon(verdict: Verdict, name: string): L.DivIcon {
  const span = document.createElement("span");
  span.className = "mark " + verdict.toLowerCase();
  span.setAttribute("role", "img");
  span.setAttribute("aria-label", `${name}: ${LABEL[verdict]}`);
  span.append(Object.assign(document.createElement("b"), { textContent: LETTER[verdict] }));
  return L.divIcon({ className: "", html: span, iconSize: [24, 24], iconAnchor: [12, 12], popupAnchor: [0, -12] });
}

/** The popup, built from DOM nodes: names and texts from OSM never become markup. */
function popupFor(crag: Crag, day: string, onDetail: (c: Crag) => void): HTMLElement {
  const info = describe(crag, day);
  const details = allDetails(crag);
  const box = document.createElement("div");
  const line = (...nodes: (Node | string)[]) => { box.append(...nodes, document.createElement("br")); };
  const strong = Object.assign(document.createElement("strong"), { textContent: crag.name });
  line(strong, place(crag) ? ` · ${place(crag)}` : "");
  line(`${LABEL[info.verdict]} — ${info.aspect}`);
  if (info.reasons.length) line(info.reasons.join("; "));
  if (details.length) line(details.join(" · "));
  line(Object.assign(document.createElement("small"), { textContent: info.facts }));
  const av = crag.days[day].avalanche;                // CG-11
  if (av) line(Object.assign(document.createElement("strong"), { textContent: `Valanghe: ${dangerLabel(av.level)}` }),
    ` — zona ${zoneName(av)}`);
  const a = (href: string, text: string) => Object.assign(document.createElement("a"), { href, textContent: text, rel: "noopener" });
  const links: (Node | string)[] = [a(crag.sourceUrl, "fonte OpenStreetMap")];
  if (crag.details?.website) links.push(" · ", a(crag.details.website, "sito"));
  // CG-59: a crag folded from many objects says how many, not a row of "OSM n" links; the
  // panel lists them.
  if (mappedTimes(crag) > 1) links.push(` · mappata ${mappedTimes(crag)} volte`);
  line(...links);
  const edit = osmEditUrl(crag.sourceUrl);
  if (edit) line(a(edit, "modifica su OpenStreetMap"));
  const more = Object.assign(document.createElement("button"), { type: "button", className: "more", textContent: "Dettagli" });
  more.addEventListener("click", () => onDetail(crag));
  box.append(more);
  return box;
}

export function CragMap({ crags, day, here, onDetail, apiRef, hot = null, area = null, onArea, onChosen }: Props) {
  const div = useRef<HTMLDivElement>(null);
  const map = useRef<L.Map | null>(null);
  const layer = useRef<L.MarkerClusterGroup | null>(null);
  const markers = useRef(new Map<string, L.Marker>());
  const me = useRef<L.Marker | null>(null);
  // Moves the page makes (fly to a crag, centre on the reader) are not the reader exploring.
  const ours = useRef(false);
  const programmatic = useRef<(move: () => void) => void>(move => move());
  const [moved, setMoved] = useState(false);
  const chosenRef = useRef(onChosen);
  chosenRef.current = onChosen;

  useEffect(() => {                                   // the map, once
    if (!div.current || map.current) return;
    const m = L.map(div.current, { scrollWheelZoom: false }).setView([45.95, 9.7], 8);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 17,
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(m);
    // CG-23: crags close together gather into one mark with the count and the split written out.
    layer.current = L.markerClusterGroup({
      maxClusterRadius: 50,                       // CG-55: wider marks, so gather a little wider
      disableClusteringAtZoom: 12,
      showCoverageOnHover: false,
      iconCreateFunction(cluster) {
        const n: Record<Verdict, number> = { GO: 0, MAYBE: 0, NO_GO: 0 };
        cluster.getAllChildMarkers().forEach(mk => n[(mk.options as { verdict: Verdict }).verdict]++);
        const total = cluster.getChildCount();
        const label = `${total} falesie: ${n.GO} si va, ${n.MAYBE} forse, ${n.NO_GO} no`;
        const span = document.createElement("span");
        span.className = "cluster";
        span.setAttribute("role", "img");
        span.setAttribute("aria-label", label);
        span.title = label;
        // CG-55: the split at a readable size; parts at zero are left out of the mark ("9S", not
        // "9S·0F·0N") and kept in the label read aloud.
        const split = (["GO", "MAYBE", "NO_GO"] as Verdict[]).filter(v => n[v]).map(v => `${n[v]}${LETTER[v]}`).join("·");
        span.append(Object.assign(document.createElement("b"), { textContent: String(total) }),
          Object.assign(document.createElement("small"), { textContent: split }));
        return L.divIcon({ className: "", html: span, iconSize: [64, 50], iconAnchor: [32, 25] });
      },
    }).addTo(m);
    map.current = m;
    // The reader explored when they dragged or zoomed; a pan to fit a popup is neither.
    m.on("dragend", () => setMoved(true));
    m.on("zoomend", () => { if (!ours.current) setMoved(true); });
    programmatic.current = (move: () => void) => {
      ours.current = true;
      m.once("moveend", () => setTimeout(() => { ours.current = false; }, 0));
      move();
    };
    apiRef.current = {
      refresh() { m.invalidateSize(); },
      // CG-38: fly to the crag, out of its cluster (clusters break up from zoom 12), then open it.
      open(id) {
        const mk = markers.current.get(id);
        if (!mk) return;
        m.invalidateSize();                       // the map may have just been shown (CG-54)
        const to = mk.getLatLng(), zoom = Math.max(m.getZoom(), 13);
        // After the flight the mark may still sit in a cluster being rebuilt: zoomToShowLayer
        // waits for it (or opens the cluster) before the popup.
        m.once("moveend", () => layer.current?.zoomToShowLayer(mk, () => mk.openPopup()));
        programmatic.current(() => { if (reducedMotion()) m.setView(to, zoom); else m.flyTo(to, zoom, { duration: 0.8 }); });
      },
    };
    return () => { apiRef.current = null; m.remove(); map.current = null; };
  }, [apiRef]);

  useEffect(() => {                                   // the markers, whenever the view changes
    const group = layer.current;
    if (!group) return;
    group.clearLayers();
    markers.current.clear();
    for (const crag of crags) {
      const verdict = crag.days[day].verdict;
      const mk = L.marker([crag.latitude, crag.longitude], {
        icon: verdictIcon(verdict, crag.name),
        title: `${crag.name} — ${LABEL[verdict]}`,
        keyboard: true,
      });
      (mk.options as { verdict?: Verdict }).verdict = verdict;     // read by the cluster mark
      mk.bindPopup(() => popupFor(crag, day, onDetail));
      mk.on("popupopen", () => chosenRef.current?.(crag.id));
      markers.current.set(crag.id, mk);
      group.addLayer(mk);
    }
  }, [crags, day, onDetail]);

  useEffect(() => {                                   // the reader's position (CG-22)
    me.current?.remove();
    me.current = null;
    if (!here || !map.current) return;
    const dot = document.createElement("span");
    dot.className = "me";
    dot.setAttribute("aria-label", "La tua posizione");
    me.current = L.marker([here.lat, here.lon], {
      icon: L.divIcon({ className: "", html: dot, iconSize: [14, 14] }), title: "La tua posizione", keyboard: false,
    }).addTo(map.current);
    const m = map.current, at = here;
    programmatic.current(() => m.setView([at.lat, at.lon], 9));
  }, [here]);

  useEffect(() => {                                   // CG-38: light the hovered crag's mark
    const group = layer.current, mk = hot ? markers.current.get(hot) : undefined;
    if (!group || !mk) return;
    const shown = group.getVisibleParent(mk) as L.Marker | null;   // the mark itself, or its cluster
    const el = shown?.getElement();
    if (!shown || !el) return;
    el.classList.add("hot");
    shown.setZIndexOffset(1000);
    return () => { el.classList.remove("hot"); shown.setZIndexOffset(0); };
  }, [hot, crags, day]);

  const searchHere = () => {
    const b = map.current?.getBounds();
    if (!b) return;
    setMoved(false);
    onArea?.([b.getSouth(), b.getWest(), b.getNorth(), b.getEast()]);
  };

  return (
    <div className="mapwrap">
      <div id="map" ref={div} role="region" aria-label="Mappa delle falesie" />
      <div className="mapbar">
        {moved && <button type="button" onClick={searchHere}>Cerca in quest'area</button>}
        {area && <button type="button" className="clear" onClick={() => { setMoved(false); onArea?.(null); }}
                         aria-label="Togli il filtro dell'area">Solo quest'area ×</button>}
      </div>
    </div>
  );
}
