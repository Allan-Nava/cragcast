import AxeBuilder from "@axe-core/playwright";
import { AFTER, CRAGS, FERRATE, MANY, TOMORROW, conditions, expect, listNames, openFilters, test } from "./fixtures";

test.describe("the page", () => {
  test("loads tomorrow's verdicts, ordered by verdict then name", async ({ page }) => {
    await page.goto("./");
    await expect(page.locator("#count")).toHaveText("6 falesie");
    await expect(page.locator(".summary")).toContainText("2 si va");
    await expect(page.locator(".summary")).toContainText("2 forse");
    await expect.poll(() => listNames(page)).toEqual(["Còrna Test — Si va", "Sasso Finto — Si va", "Placca Prova — Forse",
      "Torre Inventata — Forse", "Muro Esempio — No", "Parete Immaginaria — No"]);
    await expect(page.locator("#updated")).toContainText("Aggiornato adesso");
  });

  test("search ignores accents; province and verdicts filter; the URL keeps the view and back restores it", async ({ page }) => {
    await page.goto("./");
    await page.getByPlaceholder("Cerca una falesia…").fill("corna");
    await expect.poll(() => listNames(page)).toEqual(["Còrna Test — Si va"]);
    await expect(page).toHaveURL(/q=corna/);
    await page.getByPlaceholder("Cerca una falesia…").fill("");
    await openFilters(page);
    await page.getByLabel("Provincia").selectOption("SO");
    await expect.poll(() => listNames(page)).toEqual(["Torre Inventata — Forse", "Muro Esempio — No"]);
    await page.getByRole("button", { name: /^N?\s*No$/ }).click();
    await expect.poll(() => listNames(page)).toEqual(["Torre Inventata — Forse"]);
    await expect(page).toHaveURL(/p=SO/);
    await expect(page).toHaveURL(/v=GO%2CMAYBE/);
    await page.goBack();
    await expect.poll(() => listNames(page)).toEqual(["Torre Inventata — Forse", "Muro Esempio — No"]);
    await page.reload();
    await expect.poll(() => listNames(page)).toEqual(["Torre Inventata — Forse", "Muro Esempio — No"]);
  });

  test("the day buttons switch every verdict", async ({ page }) => {
    await page.goto("./");
    await page.getByRole("navigation", { name: "Giorno" }).getByRole("button", { name: "Dopodomani" }).click();
    await expect(page).toHaveURL(new RegExp(`day=${AFTER}`));
    await expect(page.locator(".summary")).toContainText("3 si va");
    await expect.poll(async () => (await listNames(page))[0]).toBe("Muro Esempio — Si va");
  });

  test("a link to a crag opens its panel; closing takes it out of the URL", async ({ page }) => {
    await page.goto(`./?crag=${CRAGS[2].id}`);
    const panel = page.locator("dialog#detail");
    await expect(panel).toBeVisible();
    await expect(panel.locator("#d-title")).toContainText("Muro Esempio — No");
    await expect(panel.locator(".hchart")).toHaveCount(2);
    await panel.getByLabel("Chiudi").click();
    await expect(panel).toBeHidden();
    await expect(page).not.toHaveURL(/crag=/);
  });

  test("favourites survive a reload and filter the list", async ({ page }) => {
    await page.goto("./");
    await page.getByRole("button", { name: "Aggiungi Sasso Finto alle preferite" }).click();
    await page.reload();
    await openFilters(page);
    await page.getByRole("button", { name: "Preferite (1)" }).click();
    await expect.poll(() => listNames(page)).toEqual(["Sasso Finto — Si va"]);
    await expect(page).not.toHaveURL(/osm-node-4/);                      // favourites never in the URL
  });

  test("near me orders by distance", async ({ page, context }) => {
    await context.grantPermissions(["geolocation"]);
    await context.setGeolocation({ latitude: 46.2, longitude: 9.9 });
    await page.goto("./");
    await openFilters(page);
    await page.getByRole("button", { name: "Vicino a me" }).click();
    await expect.poll(async () => (await listNames(page))[0]).toBe("Muro Esempio — No");
    await expect(page.locator(".me")).toBeAttached();               // on a phone the map is behind "Mappa"
  });

  // CG-60: both themes, each with a crag's panel open so its charts and table are scanned too.
  for (const colorScheme of ["dark", "light"] as const) {
    test(`no serious or critical accessibility violations, ${colorScheme} theme`, async ({ page }) => {
      await page.emulateMedia({ colorScheme });
      await page.goto("./");
      await expect(page.locator("#list li h2").first()).toBeVisible();
      const scan = async () => {
        const results = await new AxeBuilder({ page }).exclude(".leaflet-control-attribution").analyze();
        const bad = results.violations.filter(v => v.impact === "serious" || v.impact === "critical");
        expect(bad.map(v => `${v.id}: ${v.nodes.map(n => n.target.join(" ")).join(", ")}`)).toEqual([]);
      };
      await scan();
      await page.locator("#list li .more").first().click();
      await expect(page.locator("dialog#detail")).toBeVisible();
      await scan();
    });
  }

  test("tomorrow is the default day", async ({ page }) => {
    await page.goto("./");
    const tomorrow = page.getByRole("navigation", { name: "Giorno" }).getByRole("button", { name: "Domani", exact: true });
    await expect(tomorrow).toHaveAttribute("aria-pressed", "true");
    await tomorrow.click();
    await expect(page).toHaveURL(new RegExp(`day=${TOMORROW}`));
  });
});

test("CG-57: a long list keeps the summary in sight and offers 'Torna su'", async ({ page, isMobile }) => {
  await page.route("**/data/conditions.json*", r => r.fulfill({ json: conditions(MANY) }));
  await page.goto("./");
  await expect(page.locator("#count")).toHaveText("46 falesie");
  await expect(page.getByRole("button", { name: "Torna su" })).toHaveCount(0);
  // A phone scrolls the page; a desktop scrolls the list's own column.
  if (isMobile) await page.mouse.wheel(0, 3000);
  else await page.locator("#list").evaluate(l => { l.scrollTop = 2000; l.dispatchEvent(new Event("scroll")); });
  await expect(page.getByRole("button", { name: "Torna su" })).toBeVisible();
  const summary = (await page.locator(".summary").boundingBox())!;
  expect(summary.y).toBeGreaterThanOrEqual(-1);
  expect(summary.y).toBeLessThan(isMobile ? 10 : 260);                     // stuck at the top while scrolling
  await page.getByRole("button", { name: "Torna su" }).click();
  if (isMobile) await expect.poll(() => page.evaluate(() => scrollY)).toBe(0);
  else await expect.poll(() => page.locator("#list").evaluate(l => l.scrollTop)).toBe(0);
});

test("CG-62: every control shows the same focus ring under the keyboard", async ({ page }) => {
  await page.goto("./");
  await expect(page.locator("#list li h2").first()).toBeVisible();
  const rings = new Set<string>();
  for (let i = 0; i < 12; i++) {
    await page.keyboard.press("Tab");
    const ring = await page.evaluate(() => {
      const el = document.activeElement as HTMLElement | null;
      if (!el || el === document.body) return null;
      const s = getComputedStyle(el);
      return `${s.outlineStyle} ${s.outlineWidth} ${s.outlineOffset}`;
    });
    if (ring) rings.add(ring);
  }
  expect([...rings]).toEqual(["solid 2px 2px"]);
});

test("CG-117: a crag's sectors and routes open in its panel, without overflowing it", async ({ page }) => {
  // Made-up sectors and routes on the first test crag.
  const withRoutes = CRAGS.map((c, i) => i ? c : {
    ...c,
    sectors: [{ id: "osm-way-11", name: "Còrna Test - Settore Alto", aspect: "SW", gradeMin: "5b", gradeMax: "7a" }],
    routes: [
      { id: "osm-node-21", name: "Diedro Lungo del Test", grade: "5c", length: 25, bolts: 11, pitches: 1, sector: "osm-way-11" },
      { id: "osm-node-22", name: "Spigolo", grade: "6a+", length: null, bolts: null, pitches: 3, sector: null },
    ],
  });
  await page.route("**/data/conditions.json*", r => r.fulfill({ json: conditions(withRoutes) }));
  await page.goto(`./?crag=${CRAGS[0].id}`);
  const panel = page.locator("dialog#detail");
  await expect(panel).toBeVisible();
  await panel.locator("summary", { hasText: "2 vie su OpenStreetMap" }).click();
  await panel.locator("summary", { hasText: "1 settore su OpenStreetMap" }).click();
  const table = panel.locator("table.routes");
  await expect(table.locator("tbody tr:not(.level)")).toHaveCount(2);
  await expect(table.locator("tbody tr:not(.level)").first()).toContainText("Settore Alto");
  await expect(table.locator("tbody tr:not(.level)").first()).not.toContainText("Còrna Test");    // the title says it
  // CG-118: one level at a time.
  await panel.getByRole("button", { name: "6 impegnativo · 1" }).click();
  await expect(table.locator("tbody tr:not(.level)")).toHaveCount(1);
  await expect(table.locator("tbody tr:not(.level)")).toContainText("Spigolo");
  const results = await new AxeBuilder({ page }).include("dialog#detail").analyze();
  expect(results.violations.filter(v => v.impact === "serious" || v.impact === "critical").map(v => v.id)).toEqual([]);
  // The table may scroll inside its box, never the page sideways.
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test("CG-114: the via ferratas behind their own switch, with their own mark, in both themes without axe findings", async ({ page }) => {
  await page.route("**/data/conditions.json*", r => r.fulfill({ json: { ...conditions(), ferrate: FERRATE } }));
  await page.goto("./");
  await expect(page.locator("#count")).toHaveText("6 falesie");
  await page.getByRole("button", { name: "Vie ferrate" }).click();
  await expect(page).toHaveURL(/k=ferrate/);
  await expect(page.locator("#count")).toHaveText("2 vie ferrate");
  await expect.poll(() => listNames(page)).toEqual(["Ferrata Finta — Si va", "Ferrata Inventata — No"]);
  await expect(page.locator("p.note")).toContainText("si verificano sempre sul posto");
  await expect(page.locator("#list .peak")).toHaveCount(2);
  for (const colorScheme of ["dark", "light"] as const) {
    await page.emulateMedia({ colorScheme });
    const results = await new AxeBuilder({ page }).exclude(".leaflet-control-attribution").analyze();
    expect(results.violations.filter(v => v.impact === "serious" || v.impact === "critical").map(v => v.id)).toEqual([]);
  }
  await page.locator("#list li .more").last().click();
  const panel = page.locator("dialog#detail");
  await expect(panel).toBeVisible();
  await expect(panel.locator("h2")).toContainText("Via ferrata Ferrata Inventata — No");
  await expect(panel).toContainText("difficoltà non mappata su OpenStreetMap");
  await expect(panel).toContainText("«2+»");
  await expect(panel).toContainText("temporale previsto");
  const results = await new AxeBuilder({ page }).include("dialog#detail").analyze();
  expect(results.violations.filter(v => v.impact === "serious" || v.impact === "critical").map(v => v.id)).toEqual([]);
  await panel.getByRole("button", { name: "Chiudi" }).click();
  await page.getByRole("button", { name: "Falesie" }).click();
  await expect(page.locator("#count")).toHaveText("6 falesie");
});
