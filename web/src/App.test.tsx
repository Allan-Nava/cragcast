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
