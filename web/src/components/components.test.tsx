import { afterEach, describe as group, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { CragList } from "./CragList";
import { Controls } from "./Controls";
import { DetailDialog } from "./DetailDialog";
import { HourChart, hourReadout } from "./HourChart";
import { ago, updatedText } from "./Footer";
import { matching, visible } from "../App";
import { Summary } from "./Summary";
import { crag, hours } from "../test/fixtures";
import { readState } from "../lib/state";

const DAY = "2026-10-16";
afterEach(cleanup);

group("list", () => {
  it("shows verdict, place and details, and opens the panel without opening the popup", () => {
    const onOpen = vi.fn(), onDetail = vi.fn();
    render(<CragList crags={[crag({ elevation: 500 })]} day={DAY} distance={null} onOpen={onOpen} onDetail={onDetail} />);
    expect(screen.getByRole("heading").textContent).toContain("Corna di Medale — Si va");
    expect(screen.getByText(/Lecco \(LC\) · esposta a S/)).toBeTruthy();
    expect(screen.getByText("quota circa 500 m")).toBeTruthy();
    fireEvent.click(screen.getByText("Dettagli"));
    expect(onDetail).toHaveBeenCalledOnce();
    expect(onOpen).not.toHaveBeenCalled();
  });
  it("says so when nothing matches", () => {
    render(<CragList crags={[]} day={DAY} distance={null} onOpen={() => {}} onDetail={() => {}} />);
    expect(screen.getByText("Nessuna falesia corrisponde alla ricerca.")).toBeTruthy();
  });
});

group("filter", () => {
  const all = [crag({ id: "a", name: "Zucco", verdict: "NO_GO" }), crag({ id: "b", name: "Bàrro", verdict: "GO" }),
               crag({ id: "c", name: "Arco", verdict: "GO", provinceCode: "BG", municipality: "Bergamo", latitude: 45.7, longitude: 9.67 })];
  const s = (q: string) => readState(q + `&day=${DAY}`, [DAY]);
  it("orders by verdict, then name", () => expect(visible(all, s("?"), null).map(c => c.id)).toEqual(["c", "b", "a"]));
  it("searches without accents and by place", () => {
    expect(visible(all, s("?q=barro"), null).map(c => c.id)).toEqual(["b"]);
    expect(visible(all, s("?q=bergamo"), null).map(c => c.id)).toEqual(["c"]);
  });
  it("filters by verdict and province", () => {
    expect(visible(all, s("?v=NO_GO"), null).map(c => c.id)).toEqual(["a"]);
    expect(visible(all, s("?p=BG"), null).map(c => c.id)).toEqual(["c"]);
  });
  it("keeps only the crags inside the chosen area", () => {
    expect(visible(all, s("?a=45.5,9.39,46,9.41"), null).map(c => c.id).sort()).toEqual(["a", "b"]);
    expect(visible(all, s("?a=40,1,41,2"), null)).toEqual([]);
  });
  it("orders by distance when given one", () => {
    const d = { a: 1, b: 3, c: 2 } as Record<string, number>;
    expect(visible(all, s("?"), c => d[c.id]).map(c => c.id)).toEqual(["a", "c", "b"]);
  });
});

group("charts", () => {
  it("labels only the maximum temperature and draws one bar per rainy hour", () => {
    const { container } = render(<>
      <HourChart hours={hours} kind="temperature" cursor={null} onCursor={() => {}} />
      <HourChart hours={hours} kind="rain" cursor={null} onCursor={() => {}} />
    </>);
    expect(container.querySelectorAll("text.label")).toHaveLength(1);
    expect(container.querySelector("text.label")!.textContent).toBe("max 25°");
    expect(container.querySelectorAll("path")).toHaveLength(1);
  });
  it("says there is no rain when there is none", () => {
    const dry = hours.map(h => ({ ...h, rainMm: 0 }));
    render(<HourChart hours={dry} kind="rain" cursor={null} onCursor={() => {}} />);
    expect(screen.getByText("nessuna pioggia prevista")).toBeTruthy();
  });
  it("moves the cursor with the arrow keys, from midday", () => {
    const onCursor = vi.fn();
    render(<HourChart hours={hours} kind="rain" cursor={null} onCursor={onCursor} />);
    fireEvent.keyDown(screen.getByRole("img"), { key: "ArrowRight" });
    expect(onCursor).toHaveBeenLastCalledWith(7);                   // 12:00 is index 6
  });
  it("reads an hour out in Italian", () => {
    expect(hourReadout({ hour: 14, temperatureC: 18.4, rainMm: 0.25, windKmh: 12 }))
      .toBe("ore 14: 18 °C · pioggia 0,3 mm · vento 12 km/h");
  });
});

group("footer", () => {
  it("warns when the data is older than 3 h", () => {
    const t = "2026-10-15T08:00:00Z";
    expect(updatedText(t, Date.parse(t) + 2 * 3.6e6).stale).toBe(false);
    const old = updatedText(t, Date.parse(t) + 5 * 3.6e6);
    expect(old.stale).toBe(true);
    expect(old.text).toMatch(/^Aggiornato 5 ore fa \(.*\) — sei offline o l'aggiornamento non è riuscito\.$/);
  });
  it("says how long ago, in words", () => {
    expect(ago(20_000)).toBe("adesso");
    expect(ago(12 * 60_000)).toBe("12 min fa");
    expect(ago(61 * 60_000)).toBe("1 ora fa");
    expect(ago(5 * 3.6e6)).toBe("5 ore fa");
  });
});

group("panel", () => {
  it("shows the crag, the hours and the directions", () => {
    render(<DetailDialog crag={crag({ elevation: 900 })} day={DAY} onClose={() => {}} />);
    expect(document.getElementById("d-title")!.textContent).toContain("Corna di Medale — Si va");
    expect(screen.getByText("Passa sul grafico o usa le frecce per leggere un'ora.")).toBeTruthy();
    expect(screen.getByText("Tabella ora per ora")).toBeTruthy();
    expect(screen.getByText("Indicazioni (Google Maps)").getAttribute("href"))
      .toBe("https://www.google.com/maps/dir/?api=1&destination=45.86,9.4");
    expect(screen.getByText("Modifica su OSM").getAttribute("href")).toBe("https://www.openstreetmap.org/edit?node=1");
  });
});

group("avalanche in the panel", () => {
  it("shows the bulletin when there is one, and nothing when there is none", () => {
    const c = crag();
    c.days[DAY].avalanche = { region: "IT-25-LC-01", name: "PREALPI CENTRALI", level: 2, bulletin: "https://example.org/b",
      ratings: [{ level: 2, above: "1500", below: null, period: null }], problems: ["wet_snow"] };
    const { container, unmount } = render(<DetailDialog crag={c} day={DAY} onClose={() => {}} />);
    expect(container.querySelector(".avalanche")!.textContent)
      .toBe("Valanghe: pericolo 2 (moderato) — zona Prealpi centrali: 2 moderato sopra 1500 m. Problemi: neve bagnata. Bollettino ARPA Lombardia");
    unmount();
    const { container: none } = render(<DetailDialog crag={crag()} day={DAY} onClose={() => {}} />);
    expect(none.querySelector(".avalanche")).toBeNull();
  });
});

group("list and map (CG-38)", () => {
  it("reports the crag under the pointer and keyboard focus, and marks the chosen one", () => {
    const onHot = vi.fn();
    render(<CragList crags={[crag()]} day={DAY} distance={null} chosen="node/1"
                     onOpen={() => {}} onDetail={() => {}} onHot={onHot} />);
    const li = screen.getByRole("listitem");
    expect(li.getAttribute("aria-current")).toBe("true");
    fireEvent.mouseEnter(li);
    expect(onHot).toHaveBeenLastCalledWith("node/1");
    fireEvent.mouseLeave(li);
    expect(onHot).toHaveBeenLastCalledWith(null);
    fireEvent.focus(li);
    expect(onHot).toHaveBeenLastCalledWith("node/1");
  });
});

group("three days at a glance (CG-39)", () => {
  const D = ["2026-10-15", "2026-10-16", "2026-10-17"];
  function threeDays() {
    const c = crag();
    const base = c.days[DAY];
    c.days = {
      [D[0]]: { ...base, verdict: "NO_GO", weather: { ...base.weather, rainDuringDayMm: 6.2, maxTemperatureC: 14 } },
      [D[1]]: { ...base, verdict: "MAYBE", weather: { ...base.weather, rainDuringDayMm: 0.2, maxTemperatureC: 16.6 } },
      [D[2]]: { ...base, verdict: "GO", weather: { ...base.weather, rainDuringDayMm: 0, maxTemperatureC: 19 } },
    };
    return c;
  }

  it("shows each day's verdict, max and rain, and switches day without opening the crag", () => {
    const onDay = vi.fn(), onOpen = vi.fn();
    render(<CragList crags={[threeDays()]} day={D[1]} days={D} onDay={onDay} distance={null} onOpen={onOpen} onDetail={() => {}} />);
    const buttons = screen.getAllByRole("button").filter(b => b.classList.contains("day"));
    expect(buttons).toHaveLength(3);
    // The accessible name starts with what is seen, then the full reading (WCAG 2.5.3).
    expect(buttons[0].textContent).toBe("Ngio14°6 — giovedì 15: no, massima 14 °C, pioggia 6,2 mm");
    expect(buttons[1].textContent).toMatch(/: forse, massima 17 °C$/);                      // 0.2 mm is not shown
    expect(buttons[1].getAttribute("aria-pressed")).toBe("true");
    expect(buttons[0].querySelector(".rain")!.textContent).toBe("6");
    expect(buttons[1].querySelector(".rain")).toBeNull();
    fireEvent.click(buttons[2]);
    expect(onDay).toHaveBeenCalledWith(D[2]);
    expect(onOpen).not.toHaveBeenCalled();
  });

  it("counts the day's verdicts in words", () => {
    const c = [threeDays(), threeDays(), crag({ id: "x" })];
    render(<Summary crags={c} day={D[1]} index={1} />);
    expect(document.querySelector(".summary")!.textContent).toMatch(/^Domani, venerdì 16 ottobre:.*1 si va.*2 forse.*0 no$/);
  });

  it("matching keeps every verdict, so the summary ignores the verdict toggles", () => {
    const s = readState(`?v=GO&day=${DAY}`, [DAY]);
    const all = [crag({ id: "a", verdict: "NO_GO" }), crag({ id: "b" })];
    expect(matching(all, s)).toHaveLength(2);
    expect(visible(all, s, null)).toHaveLength(1);
  });
});

group("chart pointer (CG-50)", () => {
  it("reads the hour under the pointer, and steps back with the left arrow", () => {
    const onCursor = vi.fn();
    render(<HourChart hours={hours} kind="temperature" cursor={9} onCursor={onCursor} />);
    const svg = screen.getByRole("img");
    svg.getBoundingClientRect = () => ({ left: 0, top: 0, width: 320, height: 116, right: 320, bottom: 116, x: 0, y: 0, toJSON() {} });
    fireEvent.pointerMove(svg, { clientX: 31 });                        // first band
    expect(onCursor).toHaveBeenLastCalledWith(0);
    fireEvent.pointerMove(svg, { clientX: 400 });                       // past the end: the last hour
    expect(onCursor).toHaveBeenLastCalledWith(15);
    fireEvent.keyDown(svg, { key: "ArrowLeft" });
    expect(onCursor).toHaveBeenLastCalledWith(8);
    fireEvent.focus(svg);
    expect(onCursor).toHaveBeenLastCalledWith(9);                       // focus keeps the hour shown
  });
});

group("list details (CG-50)", () => {
  it("scrolls the chosen crag into view side by side, not on a phone", () => {
    const scroll = vi.fn();
    Element.prototype.scrollIntoView = scroll;
    vi.stubGlobal("matchMedia", (q: string) => ({ matches: q.includes("900px") }));
    render(<CragList crags={[crag()]} day={DAY} distance={null} chosen="node/1" onOpen={() => {}} onDetail={() => {}} />);
    expect(scroll).toHaveBeenCalledWith({ block: "nearest" });
    cleanup();
    scroll.mockClear();
    vi.stubGlobal("matchMedia", () => ({ matches: false }));
    render(<CragList crags={[crag()]} day={DAY} distance={null} chosen="node/1" onOpen={() => {}} onDetail={() => {}} />);
    expect(scroll).not.toHaveBeenCalled();
    vi.unstubAllGlobals();
  });

  it("focus on a button inside a row does not light the map", () => {
    const onHot = vi.fn();
    render(<CragList crags={[crag()]} day={DAY} distance={null} onOpen={() => {}} onDetail={() => {}} onHot={onHot} />);
    fireEvent.focus(screen.getByText("Dettagli"));
    expect(onHot).not.toHaveBeenCalledWith("node/1");
  });
});

group("compact controls (CG-53)", () => {
  const state = readState(`?day=${DAY}`, [DAY]);
  const noop = () => {};
  it("'Filtri' opens and folds the filters, and counts those on", () => {
    const { rerender } = render(<Controls days={[DAY]} provinces={[]} state={state} near="off" onDay={noop} onQuery={noop}
                                          onToggle={noop} onProvince={noop} onNear={noop} />);
    const toggle = screen.getByRole("button", { name: "Filtri" });
    const panel = document.getElementById("filters")!;
    expect(toggle.getAttribute("aria-controls")).toBe("filters");
    expect(panel.dataset.open).toBe("false");
    fireEvent.click(toggle);
    expect(toggle.getAttribute("aria-expanded")).toBe("true");
    expect(panel.dataset.open).toBe("true");
    rerender(<Controls days={[DAY]} provinces={[]} state={state} near="off" active={2} onDay={noop} onQuery={noop}
                       onToggle={noop} onProvince={noop} onNear={noop} />);
    expect(screen.getByRole("button", { name: "Filtri (2)" })).toBeTruthy();
  });

  it("the summary ends with the number of crags shown", () => {
    render(<Summary crags={[crag()]} day={DAY} index={1} count="1 di 220 falesie" />);
    expect(document.getElementById("count")!.textContent).toBe("1 di 220 falesie");
    expect(document.querySelector(".summary .date")!.textContent).toBe(", venerdì 16 ottobre");
  });
});

group("compact cards (CG-58)", () => {
  it("an unmapped aspect is a short tag that still says what it means", () => {
    render(<CragList crags={[crag({ aspect: null, verdict: "NO_GO" })]} day={DAY} distance={null} onOpen={() => {}} onDetail={() => {}}
                     onFavourite={() => {}} favourites={new Set()} />);
    const meta = document.querySelector("#list li p.meta")!;
    expect(meta.textContent).toBe("Lecco (LC) · esposizione ? (non mappata: valutata come parete in ombra) · pioggia prevista durante il giorno");
    expect(meta.querySelector(".tag")!.getAttribute("title")).toContain("valutata come parete in ombra");
    // The heading reads as the crag alone; the star sits beside it.
    expect(screen.getByRole("heading", { level: 2 }).textContent).toBe("NCorna di Medale — No");
    expect(document.querySelector("#list li .head > .star")).not.toBeNull();
    expect(document.querySelector("#list li .row > .more")).not.toBeNull();
  });
});

group("chart label at the edge (CG-63)", () => {
  it("leans inwards when the maximum is at the first or last hour", () => {
    const rising = hours.map((h, i) => ({ ...h, temperatureC: 5 + i }));
    const { container, unmount } = render(<HourChart hours={rising} kind="temperature" cursor={null} onCursor={() => {}} />);
    expect(container.querySelector("text.label")!.getAttribute("text-anchor")).toBe("end");
    unmount();
    const falling = hours.map((h, i) => ({ ...h, temperatureC: 25 - i }));
    const { container: c2 } = render(<HourChart hours={falling} kind="temperature" cursor={null} onCursor={() => {}} />);
    expect(c2.querySelector("text.label")!.getAttribute("text-anchor")).toBe("start");
  });
});
