// Reads data/conditions.json, written every hour by the GitHub Action (ConditionsExport),
// and draws one dot per crag, coloured by the verdict for the chosen day.
const LABEL = { GO: "Si va", MAYBE: "Forse", NO_GO: "No" };
const REASON_IT = {
  "rain forecast during the day": "pioggia prevista durante il giorno",
  "strong wind": "vento forte",
  "rock may still be wet from recent rain": "roccia forse ancora bagnata dalla pioggia recente",
  "cold": "freddo",
  "hot on a sunny wall": "caldo su parete al sole",
  "wall aspect not mapped: judged as a shaded wall": "esposizione non mappata: valutata come parete in ombra",
};
const ASPECT_IT = { N: "N", NE: "NE", E: "E", SE: "SE", S: "S", SW: "SO", W: "O", NW: "NO" };

const map = L.map("map", { scrollWheelZoom: false }).setView([45.95, 9.7], 8);
L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
  maxZoom: 17,
  attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
}).addTo(map);
const layer = L.layerGroup().addTo(map);

function dayLabel(iso, i) {
  const d = new Date(iso + "T12:00:00");
  const name = d.toLocaleDateString("it-IT", { weekday: "long", day: "numeric", month: "long" });
  return (i === 0 ? "Oggi" : i === 1 ? "Domani" : name) + " · " + name;
}

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

function render(data, day) {
  layer.clearLayers();
  const list = document.getElementById("list");
  list.replaceChildren();
  const order = { GO: 0, MAYBE: 1, NO_GO: 2 };
  const crags = [...data.crags].sort((a, b) =>
    order[a.days[day].verdict] - order[b.days[day].verdict] || a.name.localeCompare(b.name, "it"));

  for (const crag of crags) {
    const info = describe(crag, day);
    const cls = info.verdict.toLowerCase();
    const popup = `<strong>${escapeHtml(crag.name)}</strong><br>${LABEL[info.verdict]} — ${escapeHtml(info.aspect)}<br>` +
                  (info.reasons.length ? escapeHtml(info.reasons.join("; ")) + "<br>" : "") +
                  `<small>${escapeHtml(info.facts)}</small><br><a href="${crag.sourceUrl}">fonte OpenStreetMap</a>`;
    const marker = L.circleMarker([crag.latitude, crag.longitude], {
      radius: 8, weight: 2, color: "#050505",
      fillColor: getComputedStyle(document.documentElement).getPropertyValue("--" + cls).trim(), fillOpacity: 0.95,
    }).bindPopup(popup).addTo(layer);

    const li = document.createElement("li");
    const h = document.createElement("h2");
    const dot = document.createElement("i");
    dot.className = "dot " + cls;
    h.append(dot, `${crag.name} — ${LABEL[info.verdict]}`);
    const p = document.createElement("p");
    p.textContent = [info.aspect, ...info.reasons].join(" · ");
    li.append(h, p);
    li.addEventListener("click", () => { map.setView([crag.latitude, crag.longitude], 12); marker.openPopup(); });
    list.append(li);
  }
}

fetch("data/conditions.json", { cache: "no-cache" })
  .then(r => { if (!r.ok) throw new Error(r.status); return r.json(); })
  .then(data => {
    const nav = document.querySelector(".days");
    data.days.forEach((day, i) => {
      const b = document.createElement("button");
      b.textContent = i === 0 ? "Oggi" : i === 1 ? "Domani" : day;
      b.title = dayLabel(day, i);
      b.setAttribute("aria-pressed", String(i === 1));
      b.addEventListener("click", () => {
        nav.querySelectorAll("button").forEach(x => x.setAttribute("aria-pressed", "false"));
        b.setAttribute("aria-pressed", "true");
        render(data, day);
      });
      nav.append(b);
    });
    render(data, data.days[Math.min(1, data.days.length - 1)]);
    const t = new Date(data.generatedAt);
    document.getElementById("updated").textContent =
      "Aggiornato " + t.toLocaleString("it-IT", { dateStyle: "medium", timeStyle: "short" }) +
      ` · ${data.crags.length} falesie`;
  })
  .catch(e => { document.getElementById("updated").textContent = "Dati non disponibili (" + e.message + ")."; });
