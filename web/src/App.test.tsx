// CG-41: the page picks up new data on its own, keeping the view.
import { afterEach, describe as group, expect, it, vi } from "vitest";
import { act, cleanup, render, screen } from "@testing-library/react";
import { App, REFRESH_MS } from "./App";
import { crag } from "./test/fixtures";
import type { Conditions } from "./types";

afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.useRealTimers(); });

const DAY = "2026-10-16";
const conditions = (generatedAt: string, verdict: "GO" | "NO_GO"): Conditions => ({
  generatedAt, timezone: "Europe/Rome", days: [DAY], cragData: "", weatherData: "", elevationData: "",
  crags: [crag({ verdict })],
});

group("live data (CG-41)", () => {
  it("shows ghosts while loading, then refreshes the verdicts without a reload", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const answers = [conditions("2026-10-16T08:20:00Z", "GO"), conditions("2026-10-16T09:20:00Z", "NO_GO")];
    let calls = 0;
    vi.stubGlobal("fetch", vi.fn(() => {
      const body = answers[Math.min(calls++, answers.length - 1)];
      return Promise.resolve({ ok: true, json: () => Promise.resolve(body) });
    }));
    history.replaceState(null, "", `/?day=${DAY}`);

    render(<App />);
    expect(document.querySelectorAll("#list li.ghost")).toHaveLength(5);
    expect(await screen.findByText("Corna di Medale — Si va")).toBeTruthy();

    await act(async () => { await vi.advanceTimersByTimeAsync(REFRESH_MS); });
    expect(await screen.findByText("Corna di Medale — No")).toBeTruthy();
    expect(location.search).toBe(`?day=${DAY}`);                    // the view stayed
    expect(calls).toBe(2);
  });
});

group("a link to a crag (CG-44)", () => {
  it("opens on the crag's panel, and closing it takes the crag out of the URL", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve(conditions("2026-10-16T08:20:00Z", "GO")) })));
    history.replaceState(null, "", `/?crag=${encodeURIComponent("node/1")}&day=${DAY}`);
    render(<App />);
    await screen.findAllByText("Corna di Medale — Si va");          // the list's row and the panel's title
    expect(document.getElementById("d-title")?.textContent).toContain("Corna di Medale — Si va");
    expect((document.getElementById("detail") as HTMLDialogElement).hasAttribute("open")).toBe(true);
    await act(async () => { screen.getByLabelText("Chiudi").click(); await new Promise(r => setTimeout(r, 250)); });
    expect(new URLSearchParams(location.search).get("crag")).toBeNull();
  });

  it("back closes the panel", async () => {
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve(conditions("2026-10-16T08:20:00Z", "GO")) })));
    history.replaceState(null, "", `/?day=${DAY}`);
    render(<App />);
    await screen.findByText("Corna di Medale — Si va");
    await act(async () => { screen.getAllByText("Dettagli")[0].click(); });
    expect(new URLSearchParams(location.search).get("crag")).toBe("node/1");
    await act(async () => {
      history.replaceState(null, "", `/?day=${DAY}`);
      dispatchEvent(new PopStateEvent("popstate"));
    });
    expect((document.getElementById("detail") as HTMLDialogElement).hasAttribute("open")).toBe(false);
  });
});

group("favourites (CG-46)", () => {
  it("stars a crag, filters on favourites, and remembers both", async () => {
    localStorage.clear();
    const two: Conditions = { ...conditions("2026-10-16T08:20:00Z", "GO"),
      crags: [crag({ id: "a", name: "Parete Alfa" }), crag({ id: "b", name: "Parete Beta" })] };
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve({ ok: true, json: () => Promise.resolve(two) })));
    history.replaceState(null, "", `/?day=${DAY}`);
    const { unmount } = render(<App />);
    await screen.findByText("Parete Alfa — Si va");

    await act(async () => { screen.getByRole("button", { name: "Preferite (0)" }).click(); });
    expect(screen.getByText("Nessuna preferita: tocca la stella di una falesia per aggiungerla.")).toBeTruthy();
    await act(async () => { screen.getByRole("button", { name: "Preferite (0)" }).click(); });

    await act(async () => { screen.getByRole("button", { name: "Aggiungi Parete Beta alle preferite" }).click(); });
    await act(async () => { screen.getByRole("button", { name: "Preferite (1)" }).click(); });
    expect(screen.queryByText("Parete Alfa — Si va")).toBeNull();
    expect(screen.getByText("Parete Beta — Si va")).toBeTruthy();
    expect(location.search).toBe(`?day=${DAY}`);                    // never in the URL

    unmount();
    render(<App />);                                                 // a later visit
    await screen.findByText("Parete Beta — Si va");
    expect(screen.queryByText("Parete Alfa — Si va")).toBeNull();
    expect(screen.getByRole("button", { name: "Togli Parete Beta dalle preferite" }).getAttribute("aria-pressed")).toBe("true");
    localStorage.clear();
  });
});
