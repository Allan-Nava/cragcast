// CG-117: the sectors and routes in the panel. Made-up crag, sectors and routes.
import { afterEach, describe as group, expect, it, vi } from "vitest";
import { cleanup, render, within } from "@testing-library/react";
import { Climbing } from "./Climbing";
import { DetailDialog } from "./DetailDialog";
import { crag } from "../test/fixtures";
import type { Route, Sector } from "../types";

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

const sectors: Sector[] = [
  { id: "osm-way-5", name: "Settore Alto", aspect: "SW", gradeMin: "5b", gradeMax: "7a" },
  { id: "osm-node-6", name: null, aspect: null, gradeMin: null, gradeMax: "6a" },
  { id: "osm-node-7", name: "Settore Basso", aspect: null, gradeMin: "6a", gradeMax: "6a" },
  { id: "osm-node-8", name: "Placchetta", aspect: null, gradeMin: "4", gradeMax: null },
];
const routes: Route[] = [
  { id: "osm-node-1", name: "Diedro", grade: "5c", length: 25.5, bolts: 11, pitches: 1, sector: "osm-way-5" },
  { id: "osm-node-2", name: null, grade: null, length: null, bolts: null, pitches: 3, sector: null },
];

group("sectors and routes (CG-117)", () => {
  it("lists the sectors folded away, with aspect and grades", () => {
    render(<Climbing crag={crag({ sectors, routes: [] })} />);
    const box = document.querySelector("details.climbing")!;
    expect(box.querySelector("summary")!.textContent).toBe("4 settori su OpenStreetMap");
    const items = [...box.querySelectorAll("li")].map(li => li.textContent);
    expect(items).toEqual(["Settore Alto · esposto a SO · 5b–7a", "Settore senza nome · fino a 6a",
                           "Settore Basso · 6a", "Placchetta · da 4"]);
    expect(box.querySelector("a")!.getAttribute("href")).toBe("https://www.openstreetmap.org/way/5");
    expect(document.querySelectorAll("details.climbing")).toHaveLength(1);       // no routes, no table
  });

  it("shows the routes as a table, with a sector column only when one is known", () => {
    render(<Climbing crag={crag({ sectors, routes })} />);
    const table = document.querySelector("table.routes") as HTMLTableElement;
    expect(document.querySelectorAll("details.climbing")[1].querySelector("summary")!.textContent).toBe("2 vie su OpenStreetMap");
    expect([...table.querySelectorAll("thead th")].map(th => th.textContent)).toEqual(["Via", "Grado", "Lunghezza", "Spit", "Settore"]);
    const [first, second] = [...table.querySelectorAll("tbody tr")];
    expect([...first.children].map(c => c.textContent)).toEqual(["Diedro", "5c", "25,5 m", "11", "Settore Alto"]);
    expect([...second.children].map(c => c.textContent)).toEqual(["Senza nome · 3 tiri", "—", "—", "—", "—"]);
    expect(within(first as HTMLElement).getByRole("link").getAttribute("href")).toBe("https://www.openstreetmap.org/node/1");
  });

  it("has no sector column and no sector list for routes alone", () => {
    render(<Climbing crag={crag({ routes: [{ ...routes[0], sector: null }] })} />);
    expect(document.querySelectorAll("details.climbing")).toHaveLength(1);
    expect([...document.querySelectorAll("table.routes thead th")].map(th => th.textContent)).not.toContain("Settore");
    expect(document.querySelector("table.routes .muted")).toBeNull();               // one pitch says nothing
  });

  it("shows nothing for a crag without either, or from data before them", () => {
    const { container } = render(<Climbing crag={crag()} />);
    expect(container.innerHTML).toBe("");
    render(<Climbing crag={crag({ sectors: [], routes: [] })} />);
    expect(document.querySelector("details.climbing")).toBeNull();
  });

  it("sits in the crag's panel", () => {
    vi.stubGlobal("matchMedia", () => ({ matches: true }));
    render(<DetailDialog crag={crag({ routes })} day="2026-10-16" onClose={() => {}} />);
    expect(document.querySelector("#detail table.routes")).not.toBeNull();
  });
});
