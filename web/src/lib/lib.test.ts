import { describe as group, expect, it } from "vitest";
import { allDetails, dangerLabel, describe, detailParts, fold, osmEditUrl, place, problemParts, ratingParts, weatherRows, zoneName } from "./text";
import type { Avalanche } from "../types";
import { km } from "./geo";
import { ticks } from "./ticks";
import { cragLink, inArea, readState, stateQuery } from "./state";
import { crag } from "../test/fixtures";
import { GRADE_CHOICES, gradeLabel, gradeRank, hasGrades, inGrade, readGrade } from "./grades";

const DAYS = ["2026-10-15", "2026-10-16", "2026-10-17"];

group("text", () => {
  it("folds case and accents", () => expect(fold("Còrna DI Médale")).toBe("corna di medale"));
  it("writes the place with the province code", () => {
    expect(place({ municipality: "Canzo", provinceCode: "CO" })).toBe("Canzo (CO)");
    expect(place({ municipality: null, provinceCode: "CO" })).toBe("");
  });
  it("turns OSM details into Italian phrases", () => {
    expect(detailParts({ gradeMin: "5a", gradeMax: "7b", lengthMax: 30, rock: "limestone", rockIt: "calcare", styles: ["sport"], bolted: true }))
      .toEqual(["gradi 5a–7b", "vie fino a 30\u00a0m", "calcare", "sportiva", "chiodata"]);
    expect(detailParts({ gradeMax: "6c" })).toEqual(["gradi fino a 6c"]);
    expect(detailParts(undefined)).toEqual([]);
  });
  it("puts the elevation first", () => {
    expect(allDetails(crag({ elevation: 820, details: { routes: 40 } }))).toEqual(["quota circa 820\u00a0m", "40 vie"]);
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

group("area (CG-38)", () => {
  it("round-trips through the URL, rounded to about 100 m", () => {
    const s = readState("?a=45.81234,9.21,46.1,9.6&day=2026-10-16", DAYS);
    expect(s.area).toEqual([45.81234, 9.21, 46.1, 9.6]);
    expect(stateQuery(s)).toBe("?a=45.812%2C9.210%2C46.100%2C9.600&day=2026-10-16");
  });
  it("ignores a malformed or inverted area", () => {
    expect(readState("?a=1,2,3", DAYS).area).toBeNull();
    expect(readState("?a=46,9,45,10", DAYS).area).toBeNull();
    expect(readState("?a=x,9,46,10", DAYS).area).toBeNull();
  });
  it("tells whether a point is inside", () => {
    expect(inArea([45, 9, 46, 10], 45.5, 9.5)).toBe(true);
    expect(inArea([45, 9, 46, 10], 46.5, 9.5)).toBe(false);
  });
});

group("crag link (CG-44)", () => {
  it("keeps the open crag in the URL", () => {
    const s = readState("?crag=osm-node-12&day=2026-10-16", DAYS);
    expect(s.crag).toBe("osm-node-12");
    expect(stateQuery(s)).toBe("?crag=osm-node-12&day=2026-10-16");
    expect(readState("?crag=", DAYS).crag).toBeNull();
  });
  it("shares the page and the crag only, so the day is the reader's tomorrow", () => {
    expect(cragLink("https://x.test/cragcast/", "osm-node-12")).toBe("https://x.test/cragcast/c/osm-node-12.html");
  });
});

group("map or list (CG-54)", () => {
  it("the list by default, the map only when asked, kept in the URL", () => {
    expect(readState("", DAYS).show).toBe("list");
    const s = readState("?show=map&day=2026-10-16", DAYS);
    expect(s.show).toBe("map");
    expect(stateQuery(s)).toBe("?show=map&day=2026-10-16");
    expect(readState("?show=nonsense", DAYS).show).toBe("list");
    expect(stateQuery(readState("?day=2026-10-16", DAYS))).toBe("?day=2026-10-16");
  });
});

group("weather table (CG-59)", () => {
  it("labels and Italian numbers, units tied to them", () => {
    expect(weatherRows({ rainLast48hMm: 12.34, rainDuringDayMm: 0.04, maxTemperatureC: 18.6, maxWindKmh: 7.4 })).toEqual([
      ["Pioggia 48 h prima", "12,3\u00a0mm"], ["Pioggia in giornata", "0,0\u00a0mm"],
      ["Massima", "19\u00a0°C"], ["Vento massimo", "7\u00a0km/h"]]);
  });
});

group("grades (CG-61)", () => {
  it("ranks French grades as osm_crags.py's grade_order does", () => {
    const g = ["8a", "5c", "6a+", "6a", "4", "7c+", "6b"];
    expect([...g].sort((a, b) => gradeRank(a)! - gradeRank(b)!)).toEqual(["4", "5c", "6a", "6a+", "6b", "7c+", "8a"]);
    expect(gradeRank("V+")).toBeNull();
    expect(gradeRank("")).toBeNull();
  });
  it("keeps a crag only when OSM's grades settle it, never by guess", () => {
    const le6a = { dir: "le", grade: "6a" } as const, ge7a = { dir: "ge", grade: "7a" } as const;
    expect(inGrade({ gradeMin: "5a", gradeMax: "8a" }, le6a)).toBe(true);
    expect(inGrade({ gradeMin: "5a", gradeMax: "8a" }, ge7a)).toBe(true);
    expect(inGrade({ gradeMin: "6b", gradeMax: "8a" }, le6a)).toBe(false);
    expect(inGrade({ gradeMin: "4c", gradeMax: "6c" }, ge7a)).toBe(false);
    expect(inGrade({ gradeMax: "5c" }, le6a)).toBe(true);             // up to 5c: surely routes up to 6a
    expect(inGrade({ gradeMax: "7b" }, le6a)).toBe(false);            // easiest unknown: left out
    expect(inGrade({ gradeMin: "7a+" }, ge7a)).toBe(true);
    expect(inGrade({ gradeMin: "5a" }, ge7a)).toBe(false);
    expect(inGrade({}, le6a)).toBe(false);
    expect(inGrade(undefined, ge7a)).toBe(false);
    expect(hasGrades({ gradeMax: "6a" })).toBe(true);
    expect(hasGrades({ rock: "limestone" })).toBe(false);
  });
  it("reads only what it offers, and says it in Italian", () => {
    expect(readGrade("le6a")).toEqual({ dir: "le", grade: "6a" });
    expect(readGrade("ge8a")).toEqual({ dir: "ge", grade: "8a" });
    expect(readGrade("lt6a")).toBeNull();
    expect(readGrade("geX")).toBeNull();
    expect(readGrade(null)).toBeNull();
    expect(GRADE_CHOICES.map(gradeLabel)).toEqual(["fino al 5c", "fino al 6a", "fino al 6b", "fino al 6c",
      "dal 7a", "dal 7b", "dal 7c", "dall'8a"]);
    expect(gradeLabel({ dir: "le", grade: "8a" })).toBe("fino all'8a");
  });
  it("the grade and the order live in the URL, the defaults left out", () => {
    const s = readState("?g=ge7a&o=high&day=2026-10-16", DAYS);
    expect(s.grade).toEqual({ dir: "ge", grade: "7a" });
    expect(s.order).toBe("high");
    expect(stateQuery(s)).toBe("?g=ge7a&o=high&day=2026-10-16");
    const d = readState("?g=zz&o=distance", DAYS);              // distance never comes from a URL
    expect(d.grade).toBeNull();
    expect(d.order).toBe("verdict");
    expect(stateQuery(d)).toBe("?day=2026-10-16");
  });
});
