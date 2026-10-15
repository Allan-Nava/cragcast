// Reads data/conditions.json, written every hour by the GitHub Action (ConditionsExport),
// and draws one dot per crag, coloured by the verdict for the chosen day. Search, verdict
// filter and day live in the URL (?q=&v=&day=), so a link opens the same view.
const LABEL = { GO: "Si va", MAYBE: "Forse", NO_GO: "No" };
// CG-21: the verdict is never told by colour alone — each has a shape and a letter too
// (circle S, diamond F, square N), on the map and in the legend.
const LETTER = { GO: "S", MAYBE: "F", NO_GO: "N" };

function verdictIcon(verdict, name) {
  const cls = verdict.toLowerCase();
  return L.divIcon({
    className: "",
    html: `<span class="mark ${cls}" role="img" aria-label="${escapeHtml(name)}: ${LABEL[verdict]}"><b>${LETTER[verdict]}</b></span>`,
    iconSize: [24, 24],
    iconAnchor: [12, 12],
    popupAnchor: [0, -12],
  });
}
const ORDER = { GO: 0, MAYBE: 1, NO_GO: 2 };
const REASON_IT = {
  "rain forecast during the day": "pioggia prevista durante il giorno",
  "strong wind": "vento forte",
  "rock may still be wet from recent rain": "roccia forse ancora bagnata dalla pioggia recente",
  "cold": "freddo",
  "hot on a sunny wall": "caldo su parete al sole",
};
const ASPECT_IT = { N: "N", NE: "NE", E: "E", SE: "SE", S: "S", SW: "SO", W: "O", NW: "NO" };
const STYLE_IT = { sport: "sportiva", trad: "trad", multipitch: "vie lunghe" };

/** The OSM climbing details that exist for a crag, as short Italian phrases; [] when none. */
function detailParts(d) {
  if (!d) return [];
  const parts = [];
  if (d.gradeMin || d.gradeMax) {
    const range = d.gradeMin && d.gradeMax ? `${d.gradeMin}–${d.gradeMax}` : d.gradeMin ? `da ${d.gradeMin}` : `fino a ${d.gradeMax}`;
    parts.push(`gradi ${range}` + (d.gradeMean ? ` (media ${d.gradeMean})` : ""));
  } else if (d.gradeMean) {
    parts.push(`grado medio ${d.gradeMean}`);
  }
  if (d.lengthMin && d.lengthMax) parts.push(`vie da ${d.lengthMin} a ${d.lengthMax} m`);
  else if (d.lengthMax) parts.push(`vie fino a ${d.lengthMax} m`);
  else if (d.length) parts.push(`vie di ${d.length} m`);
  if (d.routes) parts.push(`${d.routes} vie`);
  if (d.rock) parts.push(d.rockIt || d.rock);
  if (d.styles && d.styles.length) parts.push(d.styles.map(x => STYLE_IT[x] || x).join(", "));
  if (d.bolted) parts.push("chiodata");
  return parts;
}

const map = L.map("map", { scrollWheelZoom: false }).setView([45.95, 9.7], 8);
L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 17,
  attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
}).addTo(map);
// CG-23: crags close together (Lecco, Val Masino) gather into one mark until you zoom in. The
// mark says how many crags and how they split — in text, not colour (CG-21).
const layer = L.markerClusterGroup({
  maxClusterRadius: 40,
  disableClusteringAtZoom: 12,
  showCoverageOnHover: false,
  iconCreateFunction(cluster) {
    const n = { GO: 0, MAYBE: 0, NO_GO: 0 };
    cluster.getAllChildMarkers().forEach(m => n[m.options.verdict]++);
    const label = `${cluster.getChildCount()} falesie: ${n.GO} si va, ${n.MAYBE} forse, ${n.NO_GO} no`;
    return L.divIcon({
      className: "",
      html: `<span class="cluster" role="img" aria-label="${label}" title="${label}"><b>${cluster.getChildCount()}</b>` +
            `<small>${n.GO}S·${n.MAYBE}F·${n.NO_GO}N</small></span>`,
      iconSize: [46, 46],
    });
  },
}).addTo(map);

/** Lower case, accents removed: "Còrna" and "CORNA" both become "corna". */
const fold = s => s.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase();

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function describe(crag, day) {
  const d = crag.days[day];
  const w = d.weather;
  // The unmapped-aspect note goes in the aspect line, not again among the reasons.
  const reasons = d.reasons.filter(r => !r.startsWith("wall aspect not mapped")).map(r => REASON_IT[r] || r);
  const facts = `pioggia ultime 48 h ${w.rainLast48hMm.toFixed(1)} mm · in giornata ${w.rainDuringDayMm.toFixed(1)} mm · ` +
                `max ${Math.round(w.maxTemperatureC)} °C · vento ${Math.round(w.maxWindKmh)} km/h`;
  const aspect = crag.aspect ? `parete esposta a ${ASPECT_IT[crag.aspect]}`
                             : "esposizione non mappata: valutata come parete in ombra";
  return { verdict: d.verdict, reasons, facts, aspect };
}

/** Great-circle distance in km (haversine). */
function km(aLat, aLon, bLat, bLon) {
  const r = Math.PI / 180, R = 6371;
  const dLat = (bLat - aLat) * r, dLon = (bLon - aLon) * r;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(aLat * r) * Math.cos(bLat * r) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

// CG-22: the reader's position lives only in this variable — never in the URL, never sent.
let here = null;
let hereMarker = null;

// --- State, read from and written to the URL ------------------------------------------------

const state = { day: null, q: "", verdicts: new Set(["GO", "MAYBE", "NO_GO"]) };

function readUrl(days) {
  const p = new URLSearchParams(location.search);
  state.day = days.includes(p.get("day")) ? p.get("day") : days[Math.min(1, days.length - 1)];
  state.q = p.get("q") || "";
  const v = (p.get("v") || "").split(",").filter(x => x in ORDER);
  state.verdicts = new Set(v.length ? v : Object.keys(ORDER));
}

function writeUrl(push) {
  const p = new URLSearchParams();
  if (state.q) p.set("q", state.q);
  if (state.verdicts.size < 3) p.set("v", [...state.verdicts].join(","));
  p.set("day", state.day);
  const url = location.pathname + "?" + p.toString();
  push ? history.pushState(null, "", url) : history.replaceState(null, "", url);
}

// --- Drawing ---------------------------------------------------------------------------------

function render(data) {
  const day = state.day;
  const q = fold(state.q.trim());
  layer.clearLayers();
  const list = document.getElementById("list");
  list.replaceChildren();

  const dist = c => here ? km(here.lat, here.lon, c.latitude, c.longitude) : null;
  const shown = data.crags
    .filter(c => state.verdicts.has(c.days[day].verdict))
    .filter(c => !q || fold(c.name).includes(q))
    .sort(here
      ? (a, b) => dist(a) - dist(b)
      : (a, b) => ORDER[a.days[day].verdict] - ORDER[b.days[day].verdict] || a.name.localeCompare(b.name, "it"));

  for (const crag of shown) {
    const info = describe(crag, day);
    const cls = info.verdict.toLowerCase();
    const details = (crag.elevation != null ? [`quota circa ${crag.elevation} m`] : []).concat(detailParts(crag.details));
    const site = crag.details && crag.details.website;
    // https://www.openstreetmap.org/node/123 -> https://www.openstreetmap.org/edit?node=123
    const m = crag.sourceUrl.match(/openstreetmap\.org\/(node|way|relation)\/(\d+)/);
    const edit = m ? `https://www.openstreetmap.org/edit?${m[1]}=${m[2]}` : null;
    const popup = `<strong>${escapeHtml(crag.name)}</strong><br>${LABEL[info.verdict]} — ${escapeHtml(info.aspect)}<br>` +
                  (info.reasons.length ? escapeHtml(info.reasons.join("; ")) + "<br>" : "") +
                  (details.length ? escapeHtml(details.join(" · ")) + "<br>" : "") +
                  `<small>${escapeHtml(info.facts)}</small><br><a href="${crag.sourceUrl}">fonte OpenStreetMap</a>` +
                  (site ? ` · <a href="${encodeURI(site)}" rel="noopener">sito</a>` : "") +
                  (edit ? `<br><a href="${edit}" rel="noopener">modifica su OpenStreetMap</a>` : "");
    const marker = L.marker([crag.latitude, crag.longitude], {
      verdict: info.verdict,             // read by the cluster mark
      icon: verdictIcon(info.verdict, crag.name),
      title: `${crag.name} — ${LABEL[info.verdict]}`,
      keyboard: true,
    }).bindPopup(popup).addTo(layer);

    const li = document.createElement("li");
    li.tabIndex = 0;
    const h = document.createElement("h2");
    const dot = document.createElement("span");
    dot.className = "mark small " + cls;
    dot.setAttribute("aria-hidden", "true");   // the verdict is in the text next to it
    dot.innerHTML = `<b>${LETTER[info.verdict]}</b>`;
    h.append(dot, `${crag.name} — ${LABEL[info.verdict]}`);
    if (here) {
      const k = document.createElement("span");
      k.className = "km";
      k.textContent = `${Math.round(dist(crag))} km`;
      h.append(k);
    }
    const p = document.createElement("p");
    p.textContent = [info.aspect, ...info.reasons].join(" · ");
    li.append(h, p);
    if (details.length) {
      const extra = document.createElement("p");
      extra.className = "details";
      extra.textContent = details.join(" · ");
      li.append(extra);
    }
    // zoomToShowLayer opens the cluster the crag sits in before showing its popup.
    const open = () => layer.zoomToShowLayer(marker, () => marker.openPopup());
    li.addEventListener("click", open);
    li.addEventListener("keydown", e => { if (e.key === "Enter") open(); });
    list.append(li);
  }

  if (!shown.length) {
    const li = document.createElement("li");
    li.className = "empty";
    li.textContent = "Nessuna falesia corrisponde alla ricerca.";
    list.append(li);
  }
  document.getElementById("count").textContent =
    shown.length === data.crags.length ? `${shown.length} falesie` : `${shown.length} di ${data.crags.length} falesie`;
}

function syncControls(data) {
  document.getElementById("q").value = state.q;
  document.querySelectorAll(".days button").forEach(b => b.setAttribute("aria-pressed", String(b.dataset.day === state.day)));
  document.querySelectorAll(".legend button").forEach(b =>
    b.setAttribute("aria-pressed", String(state.verdicts.has(b.dataset.verdict))));
  render(data);
}

fetch("data/conditions.json", { cache: "no-cache" })
  .then(r => { if (!r.ok) throw new Error(r.status); return r.json(); })
  .then(data => {
    readUrl(data.days);

    const nav = document.querySelector(".days");
    data.days.forEach((day, i) => {
      const b = document.createElement("button");
      b.type = "button";
      b.dataset.day = day;
      b.textContent = i === 0 ? "Oggi" : i === 1 ? "Domani" : day;
      b.title = new Date(day + "T12:00:00").toLocaleDateString("it-IT", { weekday: "long", day: "numeric", month: "long" });
      b.addEventListener("click", () => { state.day = day; writeUrl(true); syncControls(data); });
      nav.append(b);
    });

    document.querySelectorAll(".legend button").forEach(b => b.addEventListener("click", () => {
      const v = b.dataset.verdict;
      state.verdicts.has(v) ? state.verdicts.delete(v) : state.verdicts.add(v);
      writeUrl(true);
      syncControls(data);
    }));

    const input = document.getElementById("q");
    input.addEventListener("input", () => { state.q = input.value; writeUrl(false); render(data); });

    window.addEventListener("popstate", () => { readUrl(data.days); syncControls(data); });

    const near = document.getElementById("near");
    near.addEventListener("click", () => {
      if (here) {                       // second click: back to the verdict order
        here = null;
        if (hereMarker) { hereMarker.remove(); hereMarker = null; }
        near.setAttribute("aria-pressed", "false");
        render(data);
        return;
      }
      if (!navigator.geolocation) {
        document.getElementById("count").textContent = "Il browser non dà la posizione.";
        return;
      }
      near.textContent = "Cerco la posizione…";
      navigator.geolocation.getCurrentPosition(pos => {
        here = { lat: pos.coords.latitude, lon: pos.coords.longitude };
        near.textContent = "Vicino a me";
        near.setAttribute("aria-pressed", "true");
        hereMarker = L.marker([here.lat, here.lon], {
          icon: L.divIcon({ className: "", html: '<span class="me" aria-label="La tua posizione"></span>', iconSize: [14, 14] }),
          title: "La tua posizione", keyboard: false,
        }).addTo(map);
        map.setView([here.lat, here.lon], 9);
        render(data);
      }, err => {
        near.textContent = "Vicino a me";
        document.getElementById("count").textContent = err.code === err.PERMISSION_DENIED
          ? "Posizione non concessa: l'ordine resta per verdetto."
          : "Posizione non disponibile al momento.";
      }, { enableHighAccuracy: false, timeout: 10000, maximumAge: 600000 });
    });

    syncControls(data);
    const t = new Date(data.generatedAt);
    document.getElementById("updated").textContent =
      "Aggiornato " + t.toLocaleString("it-IT", { dateStyle: "medium", timeStyle: "short" });
  })
  .catch(e => { document.getElementById("updated").textContent = "Dati non disponibili (" + e.message + ")."; });
