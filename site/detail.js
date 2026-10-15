// CG-24: the crag's panel — verdict, reasons, details, the day's hours as two small charts
// (temperature, rain: two charts, never one with two y-axes), a table of the same hours, links.
// Colours validated on the panel surface #111418 (dataviz validator: CVD ΔE 26.8, ≥3:1):
// rain #3987e5, temperature #d95926 — neither is a verdict colour.
const SVG = "http://www.w3.org/2000/svg";
const RAIN = "#3987e5", TEMP = "#d95926";

function el(tag, attrs = {}, text) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v);
  if (text != null) e.textContent = text;
  return e;
}
function svgEl(tag, attrs = {}) {
  const e = document.createElementNS(SVG, tag);
  for (const [k, v] of Object.entries(attrs)) e.setAttribute(k, v);
  return e;
}
const fmt = (n, d = 0) => n.toLocaleString("it-IT", { minimumFractionDigits: d, maximumFractionDigits: d });

/** Clean ticks: 0/5/10… for temperature, 0 and a round top for rain. */
function ticks(min, max, isRain) {
  if (isRain) { const top = Math.max(1, Math.ceil(max)); return { lo: 0, hi: top, ticks: [0, top] }; }
  const step = max - min > 12 ? 10 : 5;
  const lo = Math.floor(min / step) * step, hi = Math.ceil(max / step) * step || lo + step;
  const t = []; for (let v = lo; v <= hi; v += step) t.push(v);
  return { lo, hi: hi === lo ? lo + step : hi, ticks: t };
}

function chart(hours, key, title, unit) {
  const W = 320, H = 116, L = 30, R = 8, T = 14, B = 18, isRain = key === "rainMm";
  const vals = hours.map(h => h[key]);
  const { lo, hi, ticks: tk } = ticks(Math.min(...vals), Math.max(...vals), isRain);
  const band = (W - L - R) / hours.length;
  const x = i => L + band * (i + 0.5);
  const y = v => T + (H - T - B) * (1 - (v - lo) / (hi - lo));

  const wrap = el("figure", { class: "hchart" });
  wrap.append(el("figcaption", {}, title));
  const svg = svgEl("svg", { viewBox: `0 0 ${W} ${H}`, role: "img", "aria-label": `${title}, ora per ora: vedi la tabella` });
  for (const v of tk) {                       // hairline grid + tick labels in muted ink
    svg.append(svgEl("line", { x1: L, x2: W - R, y1: y(v), y2: y(v), class: "grid" }));
    const t = svgEl("text", { x: L - 4, y: y(v) + 3, "text-anchor": "end", class: "tick" }); t.textContent = fmt(v); svg.append(t);
  }
  hours.forEach((h, i) => {                    // hour labels every 3 h
    if (h.hour % 3 === 0) { const t = svgEl("text", { x: x(i), y: H - 4, "text-anchor": "middle", class: "tick" }); t.textContent = h.hour; svg.append(t); }
  });
  if (isRain) {
    const bw = Math.min(24, band - 2);         // ≤24 px, 2 px surface gap
    hours.forEach((h, i) => {
      if (h.rainMm <= 0) return;
      const top = y(h.rainMm), base = y(0), r = Math.min(4, (base - top) / 2), x0 = x(i) - bw / 2;
      svg.append(svgEl("path", { fill: RAIN, d: `M${x0},${base}V${top + r}Q${x0},${top} ${x0 + r},${top}H${x0 + bw - r}Q${x0 + bw},${top} ${x0 + bw},${top + r}V${base}Z` }));
    });
    if (vals.every(v => v <= 0)) {
      const t = svgEl("text", { x: (L + W - R) / 2, y: T + (H - T - B) / 2, "text-anchor": "middle", class: "note" });
      t.textContent = "nessuna pioggia prevista"; svg.append(t);
    }
  } else {
    svg.append(svgEl("polyline", { points: hours.map((h, i) => `${x(i)},${y(h.temperatureC)}`).join(" "),
      fill: "none", stroke: TEMP, "stroke-width": 2, "stroke-linejoin": "round", "stroke-linecap": "round" }));
    const iMax = vals.indexOf(Math.max(...vals));           // label only the maximum
    svg.append(svgEl("circle", { cx: x(iMax), cy: y(vals[iMax]), r: 4, fill: TEMP, stroke: "#111418", "stroke-width": 2 }));
    const t = svgEl("text", { x: x(iMax), y: y(vals[iMax]) - 7, "text-anchor": "middle", class: "label" });
    t.textContent = `max ${fmt(vals[iMax])}${unit}`; svg.append(t);
  }
  const cross = svgEl("line", { y1: T, y2: H - B, class: "cross", visibility: "hidden" });
  svg.append(cross);
  wrap.append(svg);
  return { wrap, svg, place: i => { cross.setAttribute("x1", x(i)); cross.setAttribute("x2", x(i)); cross.setAttribute("visibility", "visible"); },
           index: clientX => { const r = svg.getBoundingClientRect(); const vx = (clientX - r.left) / r.width * W;
                               return Math.max(0, Math.min(hours.length - 1, Math.floor((vx - L) / band))); } };
}

function openDetail(crag, day, info, details) {
  const d = crag.days[day];
  const body = document.getElementById("d-body");
  body.replaceChildren();
  const h = el("h2", { id: "d-title" });
  h.append(Object.assign(el("span", { class: "mark " + d.verdict.toLowerCase(), "aria-hidden": "true" }), { innerHTML: `<b>${LETTER[d.verdict]}</b>` }),
           ` ${crag.name} — ${LABEL[d.verdict]}`);
  body.append(h, el("p", { class: "muted" }, [info.aspect, ...info.reasons].join(" · ")));
  if (details.length) body.append(el("p", {}, details.join(" · ")));

  const hours = d.hours || [];
  if (hours.length) {
    const readout = el("p", { class: "readout", "aria-live": "polite" }, "Passa sul grafico o usa le frecce per leggere un'ora.");
    const temp = chart(hours, "temperatureC", "Temperatura (°C)", "°");
    const rain = chart(hours, "rainMm", "Pioggia (mm per ora)", " mm");
    const show = i => {
      const hr = hours[i]; temp.place(i); rain.place(i);
      readout.textContent = `ore ${hr.hour}: ${fmt(hr.temperatureC)} °C · pioggia ${fmt(hr.rainMm, 1)} mm · vento ${fmt(hr.windKmh)} km/h`;
    };
    let cur = hours.findIndex(x => x.hour === 12); if (cur < 0) cur = 0;
    for (const c of [temp, rain]) {
      c.svg.setAttribute("tabindex", "0");
      c.svg.addEventListener("pointermove", e => { cur = c.index(e.clientX); show(cur); });
      c.svg.addEventListener("focus", () => show(cur));
      c.svg.addEventListener("keydown", e => {
        if (e.key === "ArrowRight") { cur = Math.min(hours.length - 1, cur + 1); show(cur); e.preventDefault(); }
        if (e.key === "ArrowLeft") { cur = Math.max(0, cur - 1); show(cur); e.preventDefault(); }
      });
    }
    const table = el("table");
    const head = el("tr"); ["Ora", "°C", "Pioggia mm", "Vento km/h"].forEach(t => head.append(el("th", { scope: "col" }, t)));
    table.append(el("thead")); table.tHead.append(head);
    const tb = el("tbody");
    hours.forEach(hr => { const r = el("tr"); [hr.hour, fmt(hr.temperatureC), fmt(hr.rainMm, 1), fmt(hr.windKmh)].forEach(v => r.append(el("td", {}, String(v)))); tb.append(r); });
    table.append(tb);
    const more = el("details", { class: "hours" }); more.append(el("summary", {}, "Tabella ora per ora"), table);
    body.append(readout, temp.wrap, rain.wrap, more);
  }

  const links = el("p", { class: "links" });
  const m = crag.sourceUrl.match(/openstreetmap\.org\/(node|way|relation)\/(\d+)/);
  const add = (href, text) => { const a = el("a", { href, rel: "noopener" }, text); links.append(a, " "); };
  add(`https://www.google.com/maps/dir/?api=1&destination=${crag.latitude},${crag.longitude}`, "Indicazioni (Google Maps)");
  add(`https://www.openstreetmap.org/directions?route=%3B${crag.latitude}%2C${crag.longitude}`, "Indicazioni (OSM)");
  add(crag.sourceUrl, "Su OpenStreetMap");
  if (m) add(`https://www.openstreetmap.org/edit?${m[1]}=${m[2]}`, "Modifica su OSM");
  body.append(links);
  document.getElementById("detail").showModal();
}
