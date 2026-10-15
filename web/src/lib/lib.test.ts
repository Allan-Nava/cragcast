import { describe as group, expect, it } from "vitest";
import { allDetails, dangerLabel, describe, detailParts, fold, osmEditUrl, place, problemParts, ratingParts, zoneName } from "./text";
import type { Avalanche } from "../types";
import { km } from "./geo";
import { ticks } from "./ticks";
import { readState, stateQuery } from "./state";
import { crag } from "../test/fixtures";

const DAYS = ["2026-10-15", "2026-10-16", "2026-10-17"];

group("text", () => {
  it("folds case and accents", () => expect(fold("Còrna DI Médale")).toBe("corna di medale"));
  it("writes the place with the province code", () => {
    expect(place({ municipality: "Canzo", provinceCode: "CO" })).toBe("Canzo (CO)");
    expect(place({ municipality: null, provinceCode: "CO" })).toBe("");
  });
  it("turns OSM details into Italian phrases", () => {
    expect(detailParts({ gradeMin: "5a", gradeMax: "7b", lengthMax: 30, rock: "limestone", rockIt: "calcare", styles: ["sport"], bolted: true }))
      .toEqual(["gradi 5a–7b", "vie fino a 30 m", "calcare", "sportiva", "chiodata"]);
    expect(detailParts({ gradeMax: "6c" })).toEqual(["gradi fino a 6c"]);
    expect(detailParts(undefined)).toEqual([]);
  });
  it("puts the elevation first", () => {
    expect(allDetails(crag({ elevation: 820, details: { routes: 40 } }))).toEqual(["quota circa 820 m", "40 vie"]);
  });
  it("keeps the unmapped-aspect note out of the reasons", () => {
    const c = crag({ aspect: null, verdict: "MAYBE" });
    c.days["2026-10-16"].reasons.push("wall aspect not mapped in OSM");
    const d = describe(c, "2026-10-16");
    expect(d.reasons).toEqual(["pioggia prevista durante il giorno"]);
    expect(d.aspect).toBe("esposizione non mappata: valutata come parete in ombra");
  });
  it("links to the OSM editor", () => {
    expect(osmEditUrl("https://www.openstreetmap.org/way/42")).toBe("https://www.openstreetmap.org/edit?way=42");
    expect(osmEditUrl("https://example.org")).toBeNull();
  });
});

group("geo", () => {
  it("measures Milan to Lecco as about 46 km", () => {
    expect(km(45.4642, 9.19, 45.8566, 9.3977)).toBeCloseTo(46.5, 0);
  });
});

group("ticks", () => {
  it("rain starts at 0 and tops at a whole mm, at least 1", () => {
    expect(ticks(0, 0, true)).toEqual({ lo: 0, hi: 1, ticks: [0, 1] });
    expect(ticks(0, 2.3, true)).toEqual({ lo: 0, hi: 3, ticks: [0, 3] });
  });
  it("temperature steps by 5, or by 10 over a wide range", () => {
    expect(ticks(8, 17, false).ticks).toEqual([5, 10, 15, 20]);
    expect(ticks(-2, 21, false).ticks).toEqual([-10, 0, 10, 20, 30]);
    expect(ticks(10, 10, false)).toEqual({ lo: 10, hi: 15, ticks: [10, 15] });
  });
});

group("state", () => {
  it("defaults to tomorrow and every verdict", () => {
    const s = readState("", DAYS);
    expect(s.day).toBe("2026-10-16");
    expect([...s.verdicts]).toEqual(["GO", "MAYBE", "NO_GO"]);
  });
  it("reads and writes the same view", () => {
    const s = readState("?q=medale&v=GO,MAYBE&p=LC&day=2026-10-17", DAYS);
    expect(stateQuery(s)).toBe("?q=medale&v=GO%2CMAYBE&p=LC&day=2026-10-17");
  });
  it("ignores an unknown day and unknown verdicts", () => {
    const s = readState("?day=1999-01-01&v=XX", DAYS);
    expect(s.day).toBe("2026-10-16");
    expect(s.verdicts.size).toBe(3);
  });
});

group("avalanche", () => {
  const a: Avalanche = {
    region: "IT-25-SO-03", name: "RETICHE ORIENTALI", level: 3, bulletin: "https://example.org/b",
    ratings: [{ level: 1, above: null, below: "treeline", period: "earlier" },
              { level: 3, above: "2000", below: null, period: "later" },
              { level: 2, above: "1500", below: "2000", period: null }],
    problems: ["wind_slab", "persistent_weak_layers", "something_new"],
  };
  it("names the level on the European scale", () => expect(dangerLabel(3)).toBe("pericolo 3 (marcato)"));
  it("writes each band and part of the day", () => {
    expect(ratingParts(a)).toEqual(["1 debole sotto il limite del bosco, mattino", "3 marcato sopra 2000 m, pomeriggio",
      "2 moderato tra 1500 m e 2000 m"]);
  });
  it("translates the problems, and passes an unknown one through readably", () => {
    expect(problemParts(a)).toEqual(["neve ventata", "strati deboli persistenti", "something new"]);
  });
  it("writes the zone in sentence case", () => expect(zoneName(a)).toBe("Retiche orientali"));
});
