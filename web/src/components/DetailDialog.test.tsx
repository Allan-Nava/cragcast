// CG-40: the panel as a side panel (wide) or a bottom sheet (narrow).
import { afterEach, describe as group, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { DetailDialog, SWIPE } from "./DetailDialog";
import { crag } from "../test/fixtures";

const D = ["2026-10-15", "2026-10-16", "2026-10-17"];
afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.useRealTimers(); });

/** A crag with the same day on all three dates; matchMedia answers as a wide or narrow screen. */
function setup(wide: boolean, reduced = true) {
  vi.stubGlobal("matchMedia", (q: string) => ({
    matches: q.includes("min-width") ? wide : q.includes("reduce") ? reduced : false,
  }));
  const c = crag();
  const one = c.days[D[1]];
  c.days = { [D[0]]: { ...one, verdict: "NO_GO" }, [D[1]]: one, [D[2]]: { ...one, verdict: "MAYBE" } };
  const onDay = vi.fn(), onClose = vi.fn();
  render(<DetailDialog crag={c} day={D[1]} days={D} onDay={onDay} onClose={onClose} />);
  const dialog = document.getElementById("detail") as HTMLDialogElement;
  return { onDay, onClose, dialog };
}

const swipe = (el: Element, dx: number, dy: number, from: Element = el) => {
  fireEvent.pointerDown(from, { clientX: 200, clientY: 200 });
  fireEvent.pointerUp(el, { clientX: 200 + dx, clientY: 200 + dy });
};

group("panel (CG-40)", () => {
  it("is a side panel on a wide screen and a sheet on a narrow one", () => {
    expect(setup(true).dialog.className).toContain("side");
    cleanup();
    const { dialog } = setup(false);
    expect(dialog.className).toContain("sheet");
    expect(dialog.querySelector(".handle")).not.toBeNull();
  });

  it("switches the day with its buttons, which carry the verdicts", () => {
    const { onDay } = setup(true);
    const tomorrow = screen.getByRole("button", { name: "Domani: si va" });
    expect(tomorrow.getAttribute("aria-pressed")).toBe("true");
    fireEvent.click(screen.getByRole("button", { name: "Dopodomani: forse" }));
    expect(onDay).toHaveBeenCalledWith(D[2]);
  });

  it("switches the day with a sideways swipe, but not one that starts on a chart", () => {
    const { onDay, dialog } = setup(false);
    swipe(dialog, -(SWIPE + 10), 5);
    expect(onDay).toHaveBeenLastCalledWith(D[2]);
    swipe(dialog, SWIPE + 10, 5);
    expect(onDay).toHaveBeenLastCalledWith(D[0]);
    onDay.mockClear();
    swipe(dialog, -(SWIPE + 10), 5, dialog.querySelector(".hchart svg")!);
    expect(onDay).not.toHaveBeenCalled();
    swipe(dialog, -20, 0);
    expect(onDay).not.toHaveBeenCalled();                          // too short
  });

  it("closes the sheet with a downward swipe, and the side panel with Esc", () => {
    const sheet = setup(false);
    swipe(sheet.dialog, 0, SWIPE * 2);
    expect(sheet.dialog.open).toBe(false);
    cleanup();
    const side = setup(true);
    expect(side.dialog.open).toBe(true);
    fireEvent.keyDown(document, { key: "Escape" });
    expect(side.dialog.open).toBe(false);
  });

  it("slides out before closing when motion is allowed", () => {
    vi.useFakeTimers();
    const { dialog } = setup(true, false);
    fireEvent.click(screen.getByLabelText("Chiudi"));
    expect(dialog.className).toContain("leaving");
    expect(dialog.open).toBe(true);
    act(() => { vi.advanceTimersByTime(200); });
    expect(dialog.open).toBe(false);
  });
});

group("share (CG-44)", () => {
  it("copies the link where the browser has no share sheet", async () => {
    const writeText = vi.fn(() => Promise.resolve());
    vi.stubGlobal("navigator", { ...navigator, share: undefined, clipboard: { writeText } });
    setup(true);
    fireEvent.click(screen.getByRole("button", { name: "Copia link" }));
    expect(await screen.findByText("Link copiato.", { exact: false })).toBeTruthy();
    expect(writeText).toHaveBeenCalledWith(`${location.origin}${location.pathname}?crag=node%2F1`);
  });
  it("uses the share sheet where there is one", async () => {
    const share = vi.fn(() => Promise.resolve());
    vi.stubGlobal("navigator", { ...navigator, share });
    setup(false);
    fireEvent.click(screen.getByRole("button", { name: "Condividi" }));
    await act(async () => {});
    expect(share).toHaveBeenCalledWith({ title: "Corna di Medale — cragcast", url: expect.stringContaining("?crag=node%2F1") });
  });
});
