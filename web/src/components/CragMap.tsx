import { useEffect, useRef } from "react";
import L from "../lib/leaflet";
import "leaflet.markercluster";
import type { Crag, Verdict } from "../types";
import { LABEL, LETTER, allDetails, dangerLabel, describe, osmEditUrl, place, zoneName } from "../lib/text";

export interface MapApi {
  /** Zoom into the cluster a crag sits in and open its popup. */
  open(id: string): void;
}

interface Props {
  crags: Crag[];
  day: string;
  here: { lat: number; lon: number } | null;
  onDetail(crag: Crag): void;
  apiRef: React.RefObject<MapApi | null>;
}

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
  crag.alsoMapped?.forEach((u, i) => links.push(i === 0 ? " · anche " : ", ", a(u, `OSM ${i + 2}`)));
  line(...links);
  const edit = osmEditUrl(crag.sourceUrl);
  if (edit) line(a(edit, "modifica su OpenStreetMap"));
  const more = Object.assign(document.createElement("button"), { type: "button", className: "more", textContent: "Dettagli" });
  more.addEventListener("click", () => onDetail(crag));
  box.append(more);
  return box;
}

export function CragMap({ crags, day, here, onDetail, apiRef }: Props) {
  const div = useRef<HTMLDivElement>(null);
  const map = useRef<L.Map | null>(null);
  const layer = useRef<L.MarkerClusterGroup | null>(null);
  const markers = useRef(new Map<string, L.Marker>());
  const me = useRef<L.Marker | null>(null);

  useEffect(() => {                                   // the map, once
    if (!div.current || map.current) return;
    const m = L.map(div.current, { scrollWheelZoom: false }).setView([45.95, 9.7], 8);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 17,
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(m);
    // CG-23: crags close together gather into one mark with the count and the split written out.
    layer.current = L.markerClusterGroup({
      maxClusterRadius: 40,
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
        span.append(Object.assign(document.createElement("b"), { textContent: String(total) }),
          Object.assign(document.createElement("small"), { textContent: `${n.GO}S·${n.MAYBE}F·${n.NO_GO}N` }));
        return L.divIcon({ className: "", html: span, iconSize: [46, 46] });
      },
    }).addTo(m);
    map.current = m;
    apiRef.current = {
      open(id) {
        const mk = markers.current.get(id);
        if (mk && layer.current) layer.current.zoomToShowLayer(mk, () => mk.openPopup());
      },
    };
    return () => { m.remove(); map.current = null; };
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
    map.current.setView([here.lat, here.lon], 9);
  }, [here]);

  return <div id="map" ref={div} role="region" aria-label="Mappa delle falesie" />;
}
