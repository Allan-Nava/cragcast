// CG-114: via ferratas as places, their words, and the view's kind in the URL. Made-up data.
import { describe as group, expect, it } from "vitest";
import { ON_THE_SPOT, asPlace, ferrataParts, scaleText } from "./ferrate";
import { allDetails, aspectShort, describe } from "./text";
import { readState, regionOf, stateQuery } from "./state";
import { crag, ferrata } from "../test/fixtures";

group("a via ferrata as a place", () => {
  it("sits at its start, takes its top's elevation, has no aspect and no climbing details", () => {
    const p = asPlace(ferrata());
    expect([p.latitude, p.longitude, p.elevation, p.aspect]).toEqual([45.9, 9.45, 1300, null]);
    expect(p.details).toEqual({});
    expect(p.ferrata?.pieces).toHaveLength(2);
    expect(asPlace({ ...ferrata(), scaleDropped: undefined as unknown as string[], pieces: undefined as unknown as string[] })
      .ferrata).toMatchObject({ scaleDropped: [], pieces: [] });
  });

  it("says its difficulty as OSM gives it, or that it is not mapped", () => {
    expect(scaleText({ scaleMin: 3, scaleMax: 3 })).toBe("difficoltà 3 (scala 0–6)");
    expect(scaleText({ scaleMin: 2, scaleMax: 4 })).toBe("difficoltà 2–4 (scala 0–6)");
    expect(scaleText({ scaleMin: null, scaleMax: null })).toBe("difficoltà non mappata su OpenStreetMap");
  });

  it("lists difficulty, length, start and top instead of a crag's details", () => {
    const p = asPlace(ferrata());
    expect(ferrataParts(p)).toEqual(["difficoltà 2–3 (scala 0–6)", "attrezzata per circa 450 m",
      "attacco circa 900 m, cima circa 1300 m"]);
    expect(allDetails(p)).toEqual(ferrataParts(p));
    expect(ferrataParts(asPlace(ferrata({ lengthM: null, top: { latitude: 1, longitude: 1, elevation: null } }))))
      .toEqual(["difficoltà 2–3 (scala 0–6)"]);
    expect(ferrataParts(crag())).toEqual([]);
  });

  it("speaks of the top, not of a wall, and in Italian", () => {
    const d = describe(asPlace(ferrata({ verdict: "NO_GO" })), "2026-10-16");
    expect(d.aspect).toBe("via ferrata, meteo in cima");
    expect(d.reasons).toEqual(["temporale previsto"]);
    expect(aspectShort(asPlace(ferrata()))).toBeNull();
    expect(ON_THE_SPOT).toContain("sul posto");
  });
});

group("the kind in the URL", () => {
  const DAYS = ["2026-10-16"];
  it("reads and writes ?k=ferrate, crags by default", () => {
    expect(readState("", DAYS).kind).toBe("crags");
    expect(readState("?k=other", DAYS).kind).toBe("crags");
    const s = readState("?k=ferrate", DAYS);
    expect(s.kind).toBe("ferrate");
    expect(stateQuery(s)).toBe("?k=ferrate&day=2026-10-16");
    expect(stateQuery({ ...s, kind: "crags" })).toBe("?day=2026-10-16");
  });

  it("CG-43: the region, Lombardy unless ?r= says otherwise; data without one is Lombardy", () => {
    expect(readState("", DAYS).region).toBe("Lombardia");
    const s = readState("?r=Piemonte", DAYS);
    expect(s.region).toBe("Piemonte");
    expect(stateQuery(s)).toBe("?r=Piemonte&day=2026-10-16");
    expect(stateQuery({ ...s, region: "Lombardia" })).toBe("?day=2026-10-16");
    expect(regionOf({})).toBe("Lombardia");
    expect(regionOf({ region: "Piemonte" })).toBe("Piemonte");
  });
});
