// CG-117: the sectors and routes in the panel. Made-up crag, sectors and routes.
import { afterEach, describe as group, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, within } from "@testing-library/react";
import { Climbing, shortName } from "./Climbing";
import { DetailDialog } from "./DetailDialog";
import { crag } from "../test/fixtures";
import type { Route, Sector } from "../types";

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

const sectors: Sector[] = [
  { id: "osm-way-5", name: "Corna di Medale - Settore Alto", aspect: "SW", gradeMin: "5b", gradeMax: "7a" },
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

  it("shows the routes as a table under their levels, with a sector column only when one is known", () => {
    render(<Climbing crag={crag({ sectors, routes })} />);
    const table = document.querySelector("table.routes") as HTMLTableElement;
    expect(document.querySelectorAll("details.climbing")[1].querySelector("summary")!.textContent)
      .toBe("2 vie su OpenStreetMap · solo 5c");
    expect([...table.querySelectorAll("thead th")].map(th => th.textContent)).toEqual(["Via", "Grado", "Lunghezza", "Spit", "Settore"]);
    expect([...table.querySelectorAll("tr.level th")].map(th => th.textContent))
      .toEqual(["Livello 5 · medio — 1 via", "Senza grado — 1 via"]);
    const [first, second] = [...table.querySelectorAll("tbody tr:not(.level)")];
    expect([...first.children].map(c => c.textContent)).toEqual(["Diedro", "5c", "25,5\u00a0m", "11", "Settore Alto"]);
    expect([...second.children].map(c => c.textContent)).toEqual(["Senza nome · 3 tiri", "—", "—", "—", "—"]);
    expect(within(first as HTMLElement).getByRole("link").getAttribute("href")).toBe("https://www.openstreetmap.org/node/1");
    expect(document.querySelector("p.scale")!.textContent).toContain("5a, 5b, 5c, 5c+, poi 6a");
  });

  it("filters the table by level, each button with its count (CG-118)", () => {
    const mixed: Route[] = ["3b", "4", "5b", "5c+", "6a", "6a+", "6c", "7a", "8a"].map((grade, i) =>
      ({ id: `osm-node-${i + 10}`, name: `Via ${grade}`, grade, length: null, bolts: null, pitches: null, sector: null }));
    render(<Climbing crag={crag({ routes: mixed })} />);
    const buttons = () => [...document.querySelectorAll(".levels button")];
    expect(buttons().map(b => b.textContent)).toEqual(["Tutte · 9", "4 e meno facile · 2", "5 medio · 2",
      "6 impegnativo · 3", "7 difficile · 1", "8 e oltre estremo · 1"]);
    expect(document.querySelector("summary")!.textContent).toBe("9 vie su OpenStreetMap · dal 3b all'8a");
    expect(buttons()[0].getAttribute("aria-pressed")).toBe("true");
    fireEvent.click(buttons()[3]);
    expect(buttons()[3].getAttribute("aria-pressed")).toBe("true");
    expect(buttons()[0].getAttribute("aria-pressed")).toBe("false");
    expect([...document.querySelectorAll("table.routes tbody tr:not(.level) a")].map(a => a.textContent))
      .toEqual(["Via 6a", "Via 6a+", "Via 6c"]);
    fireEvent.click(buttons()[0]);
    expect(document.querySelectorAll("table.routes tbody tr:not(.level)")).toHaveLength(9);
  });

  it("offers no level buttons when every route is of one level", () => {
    render(<Climbing crag={crag({ routes: [routes[0]] })} />);
    expect(document.querySelector(".levels")).toBeNull();
  });

  it("has no sector column and no sector list for routes alone", () => {
    render(<Climbing crag={crag({ routes: [{ ...routes[0], sector: null }] })} />);
    expect(document.querySelectorAll("details.climbing")).toHaveLength(1);
    expect([...document.querySelectorAll("table.routes thead th")].map(th => th.textContent)).not.toContain("Settore");
    expect(document.querySelector("table.routes .muted")).toBeNull();               // one pitch says nothing
  });

  it("drops the crag's own name from a sector's, and only that", () => {
    expect(shortName("Corna di Medale - Settore Alto", "Corna di Medale")).toBe("Settore Alto");
    expect(shortName("corna di medale, Placche", "Corna di Medale")).toBe("Placche");
    expect(shortName("Sasso Altro - Settore Alto", "Corna di Medale")).toBe("Sasso Altro - Settore Alto");
    expect(shortName("Muro Giallo", "Corna di Medale")).toBe("Muro Giallo");
    expect(shortName(null, "Corna di Medale")).toBeNull();
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
    expect(document.querySelectorAll("#detail table.routes")).toHaveLength(1);         // once, whatever the keys beside it
  });
});
