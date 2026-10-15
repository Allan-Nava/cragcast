// CG-50: the page's flows through App — near me, a failed load, the controls and history.
import { afterEach, beforeEach, describe as group, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { App, REFRESH_MS } from "./App";
import { crag } from "./test/fixtures";
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
const names = () => [...document.querySelectorAll("#list li h2")].map(h => h.firstChild?.nextSibling?.textContent ?? "");

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
