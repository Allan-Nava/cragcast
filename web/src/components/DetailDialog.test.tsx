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
    expect(writeText).toHaveBeenCalledWith(`${location.origin}${location.pathname}c/node%2F1.html`);
  });
  it("uses the share sheet where there is one", async () => {
    const share = vi.fn(() => Promise.resolve());
    vi.stubGlobal("navigator", { ...navigator, share });
    setup(false);
    fireEvent.click(screen.getByRole("button", { name: "Condividi" }));
    await act(async () => {});
    expect(share).toHaveBeenCalledWith({ title: "Corna di Medale — cragcast", url: expect.stringContaining("c/node%2F1.html") });
  });
});

group("panel edges (CG-50)", () => {
  it("shows the link when copying fails", async () => {
    vi.stubGlobal("navigator", { ...navigator, share: undefined, clipboard: { writeText: () => Promise.reject(new Error("denied")) } });
    setup(true);
    fireEvent.click(screen.getByRole("button", { name: "Copia link" }));
    expect((await screen.findByRole("link", { name: /c\/node%2F1\.html$/ })).getAttribute("href")).toContain("c/node%2F1.html");
  });

  it("Esc on the sheet closes it through the slide-out", () => {
    const { dialog, onClose } = setup(false);
    fireEvent(dialog, new Event("cancel", { cancelable: true }));
    expect(dialog.open).toBe(false);
    expect(onClose).toHaveBeenCalled();
  });

  it("uses the browser's own dialog methods where they exist", () => {
    const show = vi.fn(function (this: HTMLDialogElement) { this.setAttribute("open", ""); });
    const showModal = vi.fn(function (this: HTMLDialogElement) { this.setAttribute("open", ""); });
    const close = vi.fn(function (this: HTMLDialogElement) { this.removeAttribute("open"); this.dispatchEvent(new Event("close")); });
    Object.assign(HTMLDialogElement.prototype, { show, showModal, close });
    try {
      setup(true);
      expect(show).toHaveBeenCalled();
      cleanup();
      const { dialog } = setup(false);
      expect(showModal).toHaveBeenCalled();
      fireEvent.click(screen.getByLabelText("Chiudi"));
      expect(close).toHaveBeenCalled();
      expect(dialog.open).toBe(false);
    } finally {
      for (const k of ["show", "showModal", "close"]) delete (HTMLDialogElement.prototype as unknown as Record<string, unknown>)[k];
    }
  });
});

group("tidy panel (CG-59)", () => {
  it("lists every OSM object a crag was folded from, folded away", () => {
    vi.stubGlobal("matchMedia", () => ({ matches: true }));
    const c = crag({ alsoMapped: ["https://www.openstreetmap.org/way/2", "https://www.openstreetmap.org/node/3"] });
    render(<DetailDialog crag={c} day="2026-10-16" onClose={() => {}} />);
    const box = document.querySelector("details.mapped")!;
    expect(box.querySelector("summary")!.textContent).toBe("Mappata 3 volte su OpenStreetMap");
    expect([...box.querySelectorAll("a")].map(a => a.getAttribute("href"))).toEqual(c.alsoMapped);
  });
  it("shows nothing of the kind for a crag mapped once", () => {
    vi.stubGlobal("matchMedia", () => ({ matches: true }));
    render(<DetailDialog crag={crag()} day="2026-10-16" onClose={() => {}} />);
    expect(document.querySelector("details.mapped")).toBeNull();
  });
});
