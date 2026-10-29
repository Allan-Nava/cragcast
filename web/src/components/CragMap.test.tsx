// CG-38 with the real Leaflet in jsdom (no layout, no animation): the reader's moves offer
// "Cerca in quest'area", the page's own moves do not.
import { afterAll, afterEach, beforeAll, describe as group, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { createRef } from "react";
import { CragMap, type MapApi } from "./CragMap";
import { crag, ferrata } from "../test/fixtures";
import { asPlace } from "../lib/ferrate";

const DAY = "2026-10-16";
afterEach(cleanup);

// jsdom has no layout: give every element a size, so Leaflet sees an 800×600 map and
// markercluster puts the marks in view on it.
const size = { clientWidth: 800, clientHeight: 600 };
const saved = Object.fromEntries(Object.keys(size).map(k => [k, Object.getOwnPropertyDescriptor(HTMLElement.prototype, k)]));
beforeAll(() => { for (const [k, v] of Object.entries(size)) Object.defineProperty(HTMLElement.prototype, k, { configurable: true, get: () => v }); });
afterAll(() => { for (const [k, d] of Object.entries(saved)) if (d) Object.defineProperty(HTMLElement.prototype, k, d); });

function setup(area: [number, number, number, number] | null = null) {
  const onArea = vi.fn(), onChosen = vi.fn();
  const api = createRef<MapApi | null>() as React.RefObject<MapApi | null>;
  const view = render(<CragMap crags={[crag()]} day={DAY} here={null} onDetail={() => {}} apiRef={api}
                               area={area} onArea={onArea} onChosen={onChosen} />);
  return { onArea, onChosen, api, ...view };
}

group("map (CG-38)", () => {
  it("offers the area search after the reader zooms, and sends the map's bounds", () => {
    const { onArea, container } = setup();
    expect(screen.queryByText("Cerca in quest'area")).toBeNull();
    act(() => { fireEvent.click(container.querySelector(".leaflet-control-zoom-in")!); });
    fireEvent.click(screen.getByText("Cerca in quest'area"));
    expect(onArea).toHaveBeenCalledOnce();
    const [s, w, n, e] = onArea.mock.calls[0][0];
    expect(s).toBeLessThanOrEqual(n);
    expect(w).toBeLessThanOrEqual(e);
    expect(screen.queryByText("Cerca in quest'area")).toBeNull();
  });

  it("does not offer it after the page flies to a crag, and reports the crag chosen", async () => {
    const { api, onChosen } = setup();
    await act(async () => { api.current!.open("node/1"); await new Promise(r => setTimeout(r, 50)); });
    expect(screen.queryByText("Cerca in quest'area")).toBeNull();
    expect(onChosen).toHaveBeenCalledWith("node/1");
  });

  it("measures itself again when asked (a map shown after being hidden)", () => {
    const { api } = setup();
    expect(() => api.current!.refresh()).not.toThrow();
  });

  it("shows the active area and clears it", () => {
    const { onArea } = setup([45, 9, 46, 10]);
    fireEvent.click(screen.getByLabelText("Togli il filtro dell'area"));
    expect(onArea).toHaveBeenCalledWith(null);
  });
});

group("map marks and popups (CG-50)", () => {
  const many = () => Array.from({ length: 4 }, (_, i) => crag({
    id: `n${i}`, name: `Parete ${i}`, verdict: i === 3 ? "NO_GO" : "GO", latitude: 45.9 + i * 0.001, longitude: 9.4 }));

  it("gathers close crags into one mark that writes the split out", () => {
    const api = createRef<MapApi | null>() as React.RefObject<MapApi | null>;
    render(<CragMap crags={many()} day={DAY} here={null} onDetail={() => {}} apiRef={api} />);
    const cluster = document.querySelector(".cluster")!;
    expect(cluster.getAttribute("aria-label")).toBe("4 falesie: 3 si va, 0 forse, 1 no");
    expect(cluster.textContent).toBe("43S·1N");                       // CG-55: no "0F" in the mark
  });

  it("opens a popup with everything known about the crag, and Dettagli opens the panel", async () => {
    const onDetail = vi.fn();
    const c = crag({ id: "p", elevation: 700, details: { gradeMax: "7a", website: "https://example.org/site" },
                     alsoMapped: ["https://www.openstreetmap.org/way/9"], verdict: "MAYBE" });
    c.days[DAY].avalanche = { region: "R", name: "ZONA TEST", level: 2, ratings: [], problems: [], bulletin: "https://example.org/b" };
    const api = createRef<MapApi | null>() as React.RefObject<MapApi | null>;
    render(<CragMap crags={[c]} day={DAY} here={null} onDetail={onDetail} apiRef={api} />);
    await act(async () => { api.current!.open("p"); await new Promise(r => setTimeout(r, 50)); });
    const popup = document.querySelector(".leaflet-popup-content")!;
    expect(popup.textContent).toContain("Corna di Medale · Lecco (LC)");
    expect(popup.textContent).toContain("Forse — parete esposta a S");
    expect(popup.textContent).toContain("pioggia prevista durante il giorno");
    expect(popup.textContent).toContain("quota circa 700\u00a0m · gradi fino a 7a");
    expect(popup.textContent).toContain("Valanghe: pericolo 2 (moderato) — zona Zona test");
    const links = [...popup.querySelectorAll("a")].map(a => [a.textContent, a.getAttribute("href")]);
    expect(links).toEqual([["fonte OpenStreetMap", "https://www.openstreetmap.org/node/1"], ["sito", "https://example.org/site"],
      ["modifica su OpenStreetMap", "https://www.openstreetmap.org/edit?node=1"]]);
    expect(popup.textContent).toContain("sito · mappata 2 volte");
    expect([...popup.querySelectorAll("table.facts th")].map(th => th.textContent))
      .toEqual(["Pioggia 48 h prima", "Pioggia in giornata", "Massima", "Vento massimo"]);             // CG-59: counted, listed in the panel
    fireEvent.click(popup.querySelector("button.more")!);
    expect(onDetail).toHaveBeenCalledWith(c);
  });

  it("rings the mark of the crag pointed at in the list", () => {
    const api = createRef<MapApi | null>() as React.RefObject<MapApi | null>;
    const one = [crag({ id: "solo", latitude: 46.5, longitude: 10.5 })];
    const { rerender } = render(<CragMap crags={one} day={DAY} here={null} onDetail={() => {}} apiRef={api} hot={null} />);
    rerender(<CragMap crags={one} day={DAY} here={null} onDetail={() => {}} apiRef={api} hot="solo" />);
    expect(document.querySelector(".hot .mark")).not.toBeNull();
    rerender(<CragMap crags={one} day={DAY} here={null} onDetail={() => {}} apiRef={api} hot={null} />);
    expect(document.querySelector(".hot")).toBeNull();
  });

  it("shows the reader's position without offering the area search", () => {
    const api = createRef<MapApi | null>() as React.RefObject<MapApi | null>;
    render(<CragMap crags={[crag()]} day={DAY} here={{ lat: 45.5, lon: 9.2 }} onDetail={() => {}} apiRef={api} onArea={() => {}} />);
    expect(document.querySelector(".me")?.getAttribute("aria-label")).toBe("La tua posizione");
    expect(screen.queryByText("Cerca in quest'area")).toBeNull();
  });

  // A real drag needs a browser: jsdom lacks the SVG and layout Leaflet's dragging reads.
  // The end-to-end tests (CG-51) drag the map in Chromium.
});

group("a via ferrata on the map (CG-114)", () => {
  it("has its own mark beside the verdict's, says so, and its popup says to check on the spot", async () => {
    const api = createRef<MapApi | null>() as React.RefObject<MapApi | null>;
    const { container } = render(<CragMap crags={[asPlace(ferrata({ verdict: "NO_GO" }))]} day={DAY} here={null}
                                          onDetail={() => {}} apiRef={api} />);
    const mark = container.querySelector(".vf .mark.no_go");
    expect(mark?.getAttribute("aria-label")).toBe("Via ferrata Ferrata Prova: No");
    expect(container.querySelector(".vf .peak")).not.toBeNull();
    await act(async () => { api.current!.open("osm-way-10"); await new Promise(r => setTimeout(r, 50)); });
    const popup = container.querySelector(".leaflet-popup-content");
    expect(popup?.textContent).toContain("No — via ferrata, meteo in cima");
    expect(popup?.textContent).toContain("Condizioni da verificare sempre sul posto.");
  });
});

group("the frame (CG-123)", () => {
  const calm = () => vi.stubGlobal("matchMedia", (q: string) => ({ matches: q.includes("reduce"), media: q }) as MediaQueryList);
  afterEach(() => vi.unstubAllGlobals());

  it("fits another region's crags once they arrive, comes back home, and never offers the area search", async () => {
    calm();
    const api = createRef<MapApi | null>() as React.RefObject<MapApi | null>;
    const turin = [crag({ id: "p1", latitude: 45.1, longitude: 7.4 }), crag({ id: "p2", latitude: 45.3, longitude: 7.6 })];
    const view = (crags: ReturnType<typeof crag>[], frame: string | null) =>
      <CragMap crags={crags} day={DAY} here={null} onDetail={() => {}} apiRef={api} frame={frame} />;
    const { rerender } = render(view([crag()], null));
    expect(api.current!.view()).toMatchObject({ lat: 45.95, lon: 9.7, zoom: 8 });

    rerender(view([], "crags|Piemonte"));                   // chosen, its file not here yet: no move
    expect(api.current!.view().lon).toBe(9.7);
    await act(async () => { rerender(view(turin, "crags|Piemonte")); });
    const there = api.current!.view();
    expect(there.lon).toBeGreaterThan(7.3);
    expect(there.lon).toBeLessThan(7.7);
    expect(there.zoom).toBeGreaterThan(8);
    expect(screen.queryByText("Cerca in quest'area")).toBeNull();     // a move by the page

    await act(async () => { rerender(view([crag()], null)); });
    expect(api.current!.view()).toMatchObject({ lat: 45.95, lon: 9.7, zoom: 8 });
  });
});

