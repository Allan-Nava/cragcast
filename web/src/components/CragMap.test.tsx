// CG-38 with the real Leaflet in jsdom (no layout, no animation): the reader's moves offer
// "Cerca in quest'area", the page's own moves do not.
import { afterAll, afterEach, beforeAll, describe as group, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { createRef } from "react";
import { CragMap, type MapApi } from "./CragMap";
import { crag } from "../test/fixtures";

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
    expect(cluster.textContent).toBe("43S·0F·1N");
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
    expect(popup.textContent).toContain("quota circa 700 m · gradi fino a 7a");
    expect(popup.textContent).toContain("Valanghe: pericolo 2 (moderato) — zona Zona test");
    const links = [...popup.querySelectorAll("a")].map(a => [a.textContent, a.getAttribute("href")]);
    expect(links).toEqual([["fonte OpenStreetMap", "https://www.openstreetmap.org/node/1"], ["sito", "https://example.org/site"],
      ["OSM 2", "https://www.openstreetmap.org/way/9"], ["modifica su OpenStreetMap", "https://www.openstreetmap.org/edit?node=1"]]);
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
