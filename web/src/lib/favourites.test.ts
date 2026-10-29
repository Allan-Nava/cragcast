import { afterEach, describe as group, expect, it, vi } from "vitest";
import { loadFavourites, loadOnly, loadRegion, saveFavourites, saveOnly, saveRegion } from "./favourites";

afterEach(() => { vi.unstubAllGlobals(); localStorage.clear(); });

group("favourites (CG-46)", () => {
  it("are kept in the browser", () => {
    saveFavourites(new Set(["osm-node-1", "osm-way-2"]));
    expect([...loadFavourites()]).toEqual(["osm-node-1", "osm-way-2"]);
    saveOnly(true);
    expect(loadOnly()).toBe(true);
  });
  it("survive storage that is broken or refuses", () => {
    localStorage.setItem("cragcast:favourites", "{not json");
    expect(loadFavourites().size).toBe(0);
    localStorage.setItem("cragcast:favourites", JSON.stringify(["a", 3, null]));
    expect([...loadFavourites()]).toEqual(["a"]);
    vi.stubGlobal("localStorage", { getItem: () => { throw new Error("blocked"); }, setItem: () => { throw new Error("blocked"); } });
    expect(loadFavourites().size).toBe(0);
    expect(() => saveFavourites(new Set(["x"]))).not.toThrow();
    expect(loadOnly()).toBe(false);
  });
});

group("the region remembered (CG-129)", () => {
  const known = ["Lombardia", "Piemonte", "all"];
  it("is kept in the browser, and only a region cragcast knows comes back", () => {
    expect(loadRegion(known)).toBeNull();
    saveRegion("Piemonte");
    expect(loadRegion(known)).toBe("Piemonte");
    localStorage.setItem("cragcast:region", "Atlantide");
    expect(loadRegion(known)).toBeNull();
  });
  it("survives storage that refuses", () => {
    vi.stubGlobal("localStorage", { getItem: () => { throw new Error("blocked"); }, setItem: () => { throw new Error("blocked"); } });
    expect(loadRegion(known)).toBeNull();
    expect(() => saveRegion("Piemonte")).not.toThrow();
  });
});

