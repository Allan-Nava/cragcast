// CG-50: the page's flows through App — near me, a failed load, the controls and history.
import { afterEach, beforeEach, describe as group, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { App, REFRESH_MS } from "./App";
import { crag, ferrata } from "./test/fixtures";
import type { Conditions } from "./types";

const DAY = "2026-10-16", NEXT = "2026-10-17";
const data = (generatedAt = "2026-10-16T08:20:00Z"): Conditions => {
  const a = crag({ id: "a", name: "Parete Alfa", latitude: 45.85, longitude: 9.39, provinceCode: "LC", province: "Lecco" });
  const b = crag({ id: "b", name: "Parete Beta", verdict: "NO_GO", latitude: 46.2, longitude: 9.9, provinceCode: "SO",
                   province: "Sondrio", municipality: "Sondrio" });
  for (const c of [a, b]) c.days[NEXT] = { ...c.days[DAY], verdict: "MAYBE" };
  return { generatedAt, timezone: "Europe/Rome", days: [DAY, NEXT], cragData: "", weatherData: "", elevationData: "", crags: [a, b] };
};
const serve = (...bodies: (Conditions | number)[]) => {
  let i = 0;
  const f = vi.fn(() => {
    const b = bodies[Math.min(i++, bodies.length - 1)];
    return Promise.resolve(typeof b === "number" ? { ok: false, status: b } : { ok: true, json: () => Promise.resolve(b) });
  });
  vi.stubGlobal("fetch", f);
  return f;
};
// The heading's own text: after the verdict mark and, for a via ferrata (CG-114), its peak.
const names = () => [...document.querySelectorAll("#list li h2")].map(h => [...h.childNodes].find(n => n.nodeType === 3)?.textContent ?? "");

beforeEach(() => { history.replaceState(null, "", `/?day=${DAY}`); localStorage.clear(); });
afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.useRealTimers(); });

group("near me (CG-22)", () => {
  function geo(answer: (ok: PositionCallback, fail: PositionErrorCallback) => void) {
    vi.stubGlobal("navigator", { ...navigator, geolocation: { getCurrentPosition: vi.fn(answer) } });
  }

  it("orders by distance with the km, and a second press goes back", async () => {
    serve(data());
    geo(ok => ok({ coords: { latitude: 46.17, longitude: 9.87 } } as GeolocationPosition));
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    expect(names()).toEqual(["Parete Alfa — Si va", "Parete Beta — No"]);
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Vicino a me" })); });
    expect(names()).toEqual(["Parete Beta — No", "Parete Alfa — Si va"]);
    expect(document.querySelectorAll(".km")).toHaveLength(2);
    expect(screen.getByRole("button", { name: "Vicino a me" }).getAttribute("aria-pressed")).toBe("true");
    expect(location.search).toBe(`?day=${DAY}`);                       // the position is never in the URL
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Vicino a me" })); });
    expect(document.querySelectorAll(".km")).toHaveLength(0);
  });

  it("says so when the position is refused, unavailable, or not offered", async () => {
    serve(data());
    geo((_ok, fail) => fail({ code: 1, PERMISSION_DENIED: 1 } as GeolocationPositionError));
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Vicino a me" })); });
    expect(screen.getByText("Posizione non concessa: l'ordine resta per verdetto.")).toBeTruthy();
    cleanup();

    geo((_ok, fail) => fail({ code: 3, PERMISSION_DENIED: 1 } as GeolocationPositionError));
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Vicino a me" })); });
    expect(screen.getByText("Posizione non disponibile al momento.")).toBeTruthy();
    // Any change to the view clears the notice.
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: /Forse/ })); });
    expect(screen.queryByText("Posizione non disponibile al momento.")).toBeNull();
    cleanup();

    vi.stubGlobal("navigator", { ...navigator, geolocation: undefined });
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Vicino a me" })); });
    expect(screen.getByText("Il browser non dà la posizione.")).toBeTruthy();
  });
});

group("loading", () => {
  it("says the data is not available when the load fails", async () => {
    serve(503);
    render(<App />);
    expect(await screen.findByText("Dati non disponibili (503).")).toBeTruthy();
    expect(document.querySelectorAll("#list li.ghost")).toHaveLength(0);
    expect(screen.getByText("", { selector: "#count" })).toBeTruthy();
  });

  it("does not refresh a hidden page, nor swap data that has not changed", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const f = serve(data(), data(), data("2026-10-16T09:20:00Z"));
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
    await act(async () => { await vi.advanceTimersByTimeAsync(REFRESH_MS); });
    expect(f).toHaveBeenCalledTimes(1);
    vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
    await act(async () => { document.dispatchEvent(new Event("visibilitychange")); });
    expect(f).toHaveBeenCalledTimes(2);                                  // same generatedAt: nothing changes
    expect(screen.getByText("Parete Alfa — Si va")).toBeTruthy();
  });

  it("past midnight, a day that left the data falls back as a fresh visit would", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const later = data("2026-10-17T00:20:00Z");
    later.days = [NEXT];
    later.crags.forEach(c => { delete c.days[DAY]; });
    serve(data(), later);
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    await act(async () => { await vi.advanceTimersByTimeAsync(REFRESH_MS); });
    expect(await screen.findByText("Parete Alfa — Forse")).toBeTruthy();
  });
});

group("controls and history (CG-20)", () => {
  it("day, search, verdicts and province change the URL; typing replaces, clicks push; back restores", async () => {
    serve(data());
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    const length = history.length;

    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Domani" })); });
    expect(new URLSearchParams(location.search).get("day")).toBe(NEXT);
    expect(screen.getByText("Parete Alfa — Forse")).toBeTruthy();

    await act(async () => { fireEvent.change(screen.getByPlaceholderText("Cerca una falesia…"), { target: { value: "beta" } }); });
    expect(names()).toEqual(["Parete Beta — Forse"]);
    expect(history.length).toBe(length + 1);                             // typing replaced, the day click pushed

    await act(async () => { fireEvent.change(screen.getByPlaceholderText("Cerca una falesia…"), { target: { value: "" } }); });
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: /Forse/ })); });
    expect(new URLSearchParams(location.search).get("v")).toBe("GO,NO_GO");
    expect(screen.getByText("Nessuna falesia corrisponde alla ricerca.")).toBeTruthy();
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: /Forse/ })); });

    const select = screen.getByLabelText("Provincia") as HTMLSelectElement;
    expect([...select.options].map(o => o.textContent)).toEqual(["Tutte le province", "Lecco (1)", "Sondrio (1)"]);
    await act(async () => { fireEvent.change(select, { target: { value: "SO" } }); });
    expect(names()).toEqual(["Parete Beta — Forse"]);
    expect(screen.getByText("1 di 2 falesie")).toBeTruthy();

    await act(async () => {
      history.replaceState(null, "", `/?day=${DAY}`);
      dispatchEvent(new PopStateEvent("popstate"));
    });
    expect(names()).toEqual(["Parete Alfa — Si va", "Parete Beta — No"]);
    expect((screen.getByLabelText("Provincia") as HTMLSelectElement).value).toBe("");
  });

  it("a day button in a row switches the page's day, and Enter on a row opens it", async () => {
    serve(data());
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    const strip = screen.getByRole("group", { name: "Parete Alfa: i giorni" });
    await act(async () => { fireEvent.click(strip.querySelectorAll("button")[1]); });
    expect(new URLSearchParams(location.search).get("day")).toBe(NEXT);
    const row = document.getElementById("crag-a")!;
    await act(async () => { fireEvent.keyDown(row, { key: "Enter" }); });
    expect(row.classList.contains("chosen")).toBe(true);
  });

  it("a favourite can be taken off again, from the panel too", async () => {
    serve(data());
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Aggiungi Parete Alfa alle preferite" })); });
    await act(async () => { fireEvent.click(screen.getAllByText("Dettagli")[0]); });
    const inPanel = document.querySelector("#detail .star")!;
    expect(inPanel.getAttribute("aria-pressed")).toBe("true");
    await act(async () => { fireEvent.click(inPanel); });
    expect(screen.getByRole("button", { name: "Preferite (0)" })).toBeTruthy();
  });
});

group("map or list (CG-54)", () => {
  it("the switch puts the view in the URL; on a phone a crag chosen in the list shows the map", async () => {
    serve(data());
    vi.stubGlobal("matchMedia", (q: string) => ({ matches: q.includes("max-width: 599px"), addEventListener() {}, removeEventListener() {} }));
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    const view = screen.getByRole("group", { name: "Vista" });
    expect(document.querySelector("main")!.className).toBe("show-list");
    await act(async () => { fireEvent.click(view.querySelectorAll("button")[1]); });
    expect(new URLSearchParams(location.search).get("show")).toBe("map");
    expect(document.querySelector("main")!.className).toBe("show-map");
    await act(async () => { fireEvent.click(view.querySelectorAll("button")[1]); });   // already on: nothing pushed
    await act(async () => { fireEvent.click(view.querySelectorAll("button")[0]); });
    expect(new URLSearchParams(location.search).get("show")).toBeNull();

    await act(async () => { fireEvent.click(document.querySelector("#crag-b h2")!); await new Promise(r => setTimeout(r, 20)); });
    expect(new URLSearchParams(location.search).get("show")).toBe("map");
    expect(document.getElementById("crag-b")!.classList.contains("chosen")).toBe(true);
  });

  it("on a desktop choosing a crag leaves the view alone", async () => {
    serve(data());
    vi.stubGlobal("matchMedia", () => ({ matches: false, addEventListener() {}, removeEventListener() {} }));
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    await act(async () => { fireEvent.click(document.querySelector("#crag-a h2")!); await new Promise(r => setTimeout(r, 20)); });
    expect(new URLSearchParams(location.search).get("show")).toBeNull();
  });
});

group("panel beside (CG-56)", () => {
  it("marks the grid while a panel is open, so the list steps aside", async () => {
    serve(data());
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    const main = document.querySelector("main")!;
    expect(main.classList.contains("panel-open")).toBe(false);
    await act(async () => { fireEvent.click(screen.getAllByText("Dettagli")[0]); });
    expect(main.classList.contains("panel-open")).toBe(true);
    expect(main.querySelector("dialog#detail")).not.toBeNull();            // the panel lives in the grid
  });
});

group("order and grades (CG-61)", () => {
  // Made-up crags: Alfa low and easy, Beta high and hard, Gamma with no grades on OSM.
  const graded = (): Conditions => {
    const d = data();
    const [a, b] = d.crags;
    a.elevation = 300; a.details = { gradeMin: "4c", gradeMax: "6b" };
    b.elevation = 1400; b.details = { gradeMin: "6c", gradeMax: "8a" };
    const g = crag({ id: "g", name: "Parete Gamma", verdict: "MAYBE", latitude: 45.9, longitude: 9.5, elevation: 800 });
    g.days[NEXT] = { ...g.days[DAY] };
    return { ...d, crags: [a, b, g] };
  };

  it("orders by name and elevation, in the URL; going back restores the verdict order", async () => {
    serve(graded());
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    expect(names()).toEqual(["Parete Alfa — Si va", "Parete Gamma — Forse", "Parete Beta — No"]);
    const order = screen.getByLabelText("Ordine") as HTMLSelectElement;
    expect([...order.options].map(o => o.textContent)).toEqual(["Per verdetto", "Per nome", "Dalle più basse", "Dalle più alte"]);

    await act(async () => { fireEvent.change(order, { target: { value: "high" } }); });
    expect(names()).toEqual(["Parete Beta — No", "Parete Gamma — Forse", "Parete Alfa — Si va"]);
    expect(new URLSearchParams(location.search).get("o")).toBe("high");
    await act(async () => { fireEvent.change(order, { target: { value: "low" } }); });
    expect(names()).toEqual(["Parete Alfa — Si va", "Parete Gamma — Forse", "Parete Beta — No"]);
    await act(async () => { fireEvent.change(order, { target: { value: "name" } }); });
    expect(names()).toEqual(["Parete Alfa — Si va", "Parete Beta — No", "Parete Gamma — Forse"]);

    await act(async () => {
      history.replaceState(null, "", `/?day=${DAY}`);
      dispatchEvent(new PopStateEvent("popstate"));
    });
    expect(names()).toEqual(["Parete Alfa — Si va", "Parete Gamma — Forse", "Parete Beta — No"]);
    expect(order.value).toBe("verdict");
  });

  it("filters by grade, says how many have none, and counts as a filter", async () => {
    serve(graded());
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    const grade = screen.getByLabelText("Gradi") as HTMLSelectElement;

    await act(async () => { fireEvent.change(grade, { target: { value: "le6a" } }); });
    expect(names()).toEqual(["Parete Alfa — Si va"]);
    expect(new URLSearchParams(location.search).get("g")).toBe("le6a");
    expect(document.getElementById("count")?.textContent).toBe("1 di 3 falesie · 1 senza gradi su OpenStreetMap, escluse");
    expect(screen.getByRole("button", { name: "Filtri (1)" })).toBeTruthy();

    await act(async () => { fireEvent.change(grade, { target: { value: "ge7a" } }); });
    expect(names()).toEqual(["Parete Beta — No"]);
    await act(async () => { fireEvent.change(grade, { target: { value: "" } }); });
    expect(names()).toHaveLength(3);
    expect(document.getElementById("count")?.textContent).toBe("3 falesie");
  });

  it("near me adds distance to the orders; another order keeps the km, a second press drops both", async () => {
    serve(graded());
    vi.stubGlobal("navigator", { ...navigator, geolocation: {
      getCurrentPosition: vi.fn((ok: PositionCallback) => ok({ coords: { latitude: 46.17, longitude: 9.87 } } as GeolocationPosition)),
    } });
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Vicino a me" })); });
    const order = screen.getByLabelText("Ordine") as HTMLSelectElement;
    expect(order.value).toBe("distance");
    expect(names()[0]).toBe("Parete Beta — No");

    await act(async () => { fireEvent.change(order, { target: { value: "name" } }); });
    expect(names()).toEqual(["Parete Alfa — Si va", "Parete Beta — No", "Parete Gamma — Forse"]);
    expect(document.querySelectorAll(".km")).toHaveLength(3);
    expect(location.search).not.toMatch(/distance|46\.17/);              // the position is never in the URL

    await act(async () => { fireEvent.change(order, { target: { value: "distance" } }); });
    expect(names()[0]).toBe("Parete Beta — No");
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Vicino a me" })); });
    expect([...order.options].map(o => o.value)).not.toContain("distance");
    expect(order.value).toBe("name");
  });
});

group("panel on demand (CG-64)", () => {
  it("fetches the panel's chunk when the browser is idle", async () => {
    serve(data());
    const idle = vi.fn((f: () => void) => { f(); return 1; });
    vi.stubGlobal("requestIdleCallback", idle);
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    expect(idle).toHaveBeenCalledOnce();
    await act(async () => { fireEvent.click(screen.getAllByText("Dettagli")[0]); });
    await waitFor(() => expect(document.getElementById("d-title")?.textContent).toContain("Parete Alfa — Si va"));
  });
});

group("via ferratas (CG-114)", () => {
  const withFerrate = (): Conditions => ({ ...data(), ferrate: [ferrata({ verdict: "NO_GO" }), ferrata({ id: "osm-way-20", name: "Ferrata Seconda" })] });
  for (const f of withFerrate().ferrate!) f.days[NEXT] = { ...f.days[DAY] };

  it("no switch when the data has none", async () => {
    serve(data());
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    expect(screen.queryByRole("group", { name: "Cosa mostrare" })).toBeNull();
  });

  it("the switch shows the via ferratas alone, says so, and keeps it in the URL", async () => {
    const d = withFerrate();
    for (const f of d.ferrate!) f.days[NEXT] = { ...f.days[DAY] };
    serve(d);
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    history.replaceState(null, "", `/?p=LC&day=${DAY}`);
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Vie ferrate" })); });
    expect(location.search).toBe(`?k=ferrate&day=${DAY}`);           // the crags' province left behind
    expect(names()).toEqual(["Ferrata Seconda — Si va", "Ferrata Prova — No"]);
    expect(screen.getByText("2 vie ferrate")).toBeTruthy();
    expect(screen.getByText(/si verificano sempre sul posto/)).toBeTruthy();
    expect(document.querySelectorAll("#list .peak")).toHaveLength(2);
    expect(screen.queryByLabelText("Provincia")).toBeNull();
    expect(screen.getByPlaceholderText("Cerca una via ferrata…")).toBeTruthy();
    await act(async () => { fireEvent.click(screen.getByRole("button", { name: "Falesie" })); });
    expect(names()).toEqual(["Parete Alfa — Si va", "Parete Beta — No"]);
  });

  it("a link to a via ferrata opens its panel, with the line on checking on the spot and its share link", async () => {
    const d = withFerrate();
    for (const f of d.ferrate!) f.days[NEXT] = { ...f.days[DAY] };
    serve(d);
    history.replaceState(null, "", `/?k=ferrate&crag=osm-way-10&day=${DAY}`);
    render(<App />);
    const title = await screen.findByRole("heading", { name: /Via ferrata Ferrata Prova — No/ });
    const panel = title.closest("dialog")!;
    expect(panel.textContent).toContain("difficoltà 2–3 (scala 0–6)");
    expect(panel.textContent).toContain("Il giudizio è una stima dal meteo previsto in cima");
    expect(panel.textContent).toContain("Disegnata in 2 tratti su OpenStreetMap");
    expect(panel.querySelector(".share")).not.toBeNull();                // CG-119: c/<id>.html exists for it
  });

  it("a province or grade in a shared link never hides the via ferratas", async () => {
    const d = withFerrate();
    for (const f of d.ferrate!) f.days[NEXT] = { ...f.days[DAY] };
    serve(d);
    history.replaceState(null, "", `/?k=ferrate&p=LC&g=le6a&day=${DAY}`);
    render(<App />);
    await screen.findByText("Ferrata Prova — No");
    expect(names()).toHaveLength(2);
  });
});

group("regions (CG-43)", () => {
  const withPiemonte = (): Conditions => {
    const d = data();
    const c = crag({ id: "p", name: "Parete Gamma", latitude: 45.5, longitude: 7.5, provinceCode: "TO", province: "Torino",
                     municipality: "Torino", region: "Piemonte" });
    c.days[NEXT] = { ...c.days[DAY] };
    return { ...d, crags: [...d.crags, c] };
  };

  it("the same choice from the first paint as after the data, so the row keeps its width", async () => {
    serve(data());
    render(<App />);
    const options = () => [...(screen.getByLabelText("Regione") as HTMLSelectElement).options].map(o => o.text);
    const before = options();
    expect(before).toEqual(["Lombardia", "Piemonte", "Trentino-Alto Adige", "Tutte le regioni"]);
    await screen.findByText("Parete Alfa — Si va");
    expect(options()).toEqual(before);
  });

  it("Lombardy by default; another region in the URL with its own provinces; all of them on request", async () => {
    serve(withPiemonte());
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    expect(names()).toEqual(["Parete Alfa — Si va", "Parete Beta — No"]);
    const region = screen.getByLabelText("Regione") as HTMLSelectElement;
    await act(async () => { fireEvent.change(region, { target: { value: "Piemonte" } }); });
    expect(location.search).toBe(`?r=Piemonte&day=${DAY}`);
    expect(names()).toEqual(["Parete Gamma — Si va"]);
    expect([...(screen.getByLabelText("Provincia") as HTMLSelectElement).options].map(o => o.text))
      .toEqual(["Tutte le province", "Torino (1)"]);
    expect(screen.getByRole("button", { name: /^Filtri/ }).textContent).toBe("Filtri (1)");
    await act(async () => { fireEvent.change(region, { target: { value: "all" } }); });
    expect(names()).toHaveLength(3);
  });
});

group("a region's own file (CG-43)", () => {
  const split = () => {
    const main: Conditions = { ...data(), regions: [{ name: "Piemonte", count: 1, file: "conditions-piemonte.json" }] };
    const c = crag({ id: "p", name: "Parete Gamma", latitude: 45.5, longitude: 7.5, provinceCode: "TO", province: "Torino",
                     municipality: "Torino", region: "Piemonte" });
    c.days[NEXT] = { ...c.days[DAY] };
    return { main, other: { ...data(), crags: [c] } as Conditions };
  };
  const byUrl = (answers: Record<string, unknown | number>) => {
    const f = vi.fn((url: string) => {
      const key = Object.keys(answers).find(k => url.startsWith(k))!;
      const b = answers[key];
      return Promise.resolve(typeof b === "number" ? { ok: false, status: b } : { ok: true, json: () => Promise.resolve(b) });
    });
    vi.stubGlobal("fetch", f);
    return f;
  };

  it("is fetched only when its region is chosen, and counted from the list before", async () => {
    const { main, other } = split();
    const f = byUrl({ "data/conditions.json": main, "data/conditions-piemonte.json": other });
    render(<App />);
    await screen.findByText("Parete Alfa — Si va");
    expect(f.mock.calls.map(c => c[0])).toEqual(["data/conditions.json"]);
    const region = screen.getByLabelText("Regione") as HTMLSelectElement;
    await act(async () => { fireEvent.change(region, { target: { value: "Piemonte" } }); });
    await screen.findByText("Parete Gamma — Si va");
    expect(f.mock.calls.map(c => c[0])).toEqual(["data/conditions.json", "data/conditions-piemonte.json"]);
    expect(screen.getByText("1 falesia")).toBeTruthy();                 // CG-125: the singular
    await act(async () => { fireEvent.change(region, { target: { value: "all" } }); });
    expect(names()).toHaveLength(3);
    expect(f).toHaveBeenCalledTimes(2);                                   // kept, not fetched again
  });

  it("says so when the region's file cannot be fetched", async () => {
    const { main } = split();
    byUrl({ "data/conditions.json": main, "data/conditions-piemonte.json": 503 });
    history.replaceState(null, "", `/?r=Piemonte&day=${DAY}`);
    render(<App />);
    expect(await screen.findByText("Non riesco a caricare le falesie di questa regione: riprova più tardi.")).toBeTruthy();
  });
});
