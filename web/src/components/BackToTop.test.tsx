import { afterEach, describe as group, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { AFTER_PX, BackToTop } from "./BackToTop";

afterEach(() => { cleanup(); vi.unstubAllGlobals(); vi.restoreAllMocks(); document.body.innerHTML = ""; });

group("back to top (CG-57)", () => {
  it("shows after the page is scrolled, and takes the page back up", () => {
    const scrollTo = vi.fn();
    vi.stubGlobal("scrollTo", scrollTo);
    render(<BackToTop />);
    expect(screen.queryByText("Torna su")).toBeNull();
    vi.spyOn(window, "scrollY", "get").mockReturnValue(AFTER_PX + 1);
    act(() => { fireEvent.scroll(window); });
    fireEvent.click(screen.getByText("Torna su"));
    expect(scrollTo).toHaveBeenCalledWith({ top: 0, behavior: "smooth" });
  });

  it("shows when the list's own column is scrolled, and scrolls that up, without motion when reduced", () => {
    vi.stubGlobal("matchMedia", (q: string) => ({ matches: q.includes("reduce") }));
    const list = document.createElement("ul");
    list.id = "list";
    list.scrollTo = vi.fn();
    Object.defineProperty(list, "scrollTop", { value: AFTER_PX + 50, configurable: true });
    document.body.append(list);
    render(<BackToTop />);
    act(() => { fireEvent.scroll(list); });
    fireEvent.click(screen.getByText("Torna su"));
    expect(list.scrollTo).toHaveBeenCalledWith({ top: 0, behavior: "auto" });
  });
});
