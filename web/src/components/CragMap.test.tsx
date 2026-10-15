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
