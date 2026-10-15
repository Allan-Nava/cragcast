import { CRAGS, expect, listNames, openFilters, test } from "./fixtures";

test.skip(({ isMobile }) => !isMobile, "phone layout");

async function swipe(page: import("@playwright/test").Page, from: { x: number; y: number }, dx: number, dy: number) {
  const el = page.locator("dialog#detail");
  const opts = (x: number, y: number) => ({ clientX: x, clientY: y, pointerType: "touch", isPrimary: true, bubbles: true });
  await el.dispatchEvent("pointerdown", opts(from.x, from.y));
  await el.dispatchEvent("pointerup", opts(from.x + dx, from.y + dy));
}

test("the panel is a sheet: a sideways swipe switches day, a downward one closes it", async ({ page }) => {
  await page.goto(`./?crag=${CRAGS[1].id}`);
  const sheet = page.locator("dialog#detail");
  await expect(sheet).toHaveClass(/sheet/);
  await expect(sheet.locator("#d-title")).toContainText("Placca Prova — Forse");
  const title = (await sheet.locator("#d-title").boundingBox())!;
  await swipe(page, { x: title.x + 200, y: title.y }, -120, 5);
  await expect(sheet.locator("#d-title")).toContainText("Placca Prova — Si va");
  await swipe(page, { x: title.x + 200, y: title.y }, 0, 200);
  await expect(sheet).toBeHidden();
  await expect(page).not.toHaveURL(/crag=/);
});

test("the controls fit the screen without sideways scrolling", async ({ page }) => {
  await page.goto("./");
  await expect(page.locator("#list li h2").first()).toBeVisible();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(0);
});

test("CG-53: the controls take two rows and the filters fold behind 'Filtri'", async ({ page }) => {
  await page.goto("./");
  await expect(page.locator("#list li h2").first()).toBeAttached();
  const toggle = page.getByRole("button", { name: "Filtri" });
  await expect(toggle).toHaveAttribute("aria-expanded", "false");
  await expect(page.getByLabel("Provincia")).toBeHidden();
  // Header, days, search row and summary end well before the old ~600 px.
  const view = (await page.getByRole("group", { name: "Vista" }).boundingBox())!;
  expect(view.y).toBeLessThan(300);
  await expect(page.locator(".summary")).toContainText("6 falesie");
  await expect(page.locator(".summary .date")).toBeHidden();

  await toggle.click();
  await page.getByLabel("Provincia").selectOption("LC");
  await page.getByRole("button", { name: /^N?\s*No$/ }).click();
  await expect(page.getByRole("button", { name: "Filtri (2)" })).toBeVisible();
  expect(await listNames(page)).toEqual(["Còrna Test — Si va", "Placca Prova — Forse"]);
  await page.getByRole("button", { name: "Filtri (2)" }).click();
  await expect(page.getByLabel("Provincia")).toBeHidden();
  await openFilters(page);
  await expect(page.getByLabel("Provincia")).toHaveValue("LC");          // folding keeps the filters on
});

test("CG-54: the list first; 'Mappa' shows the map; a crag chosen in the list opens on the map", async ({ page }) => {
  await page.goto("./");
  const group = page.getByRole("group", { name: "Vista" });
  await expect(group.getByRole("button", { name: "Lista" })).toHaveAttribute("aria-pressed", "true");
  await expect(page.locator("#map")).toBeHidden();
  const first = (await page.locator("#list li").first().boundingBox())!;
  expect(first.y).toBeLessThan(400);                                     // the first crag is on the first screen

  await group.getByRole("button", { name: "Mappa" }).click();
  await expect(page.locator("#map")).toBeVisible();
  await expect(page.locator("#list")).toBeHidden();
  await expect(page).toHaveURL(/show=map/);
  const map = (await page.locator("#map").boundingBox())!;
  expect(map.height).toBeGreaterThan(400);
  await expect(page.locator(".leaflet-tile-loaded").first()).toBeVisible();   // measured, so tiles fill it

  await page.goBack();
  await expect(page.locator("#list")).toBeVisible();
  await page.locator("#crag-osm-node-3 h2").click();
  await expect(page.locator("#map")).toBeVisible();
  await expect(page.locator(".leaflet-popup-content")).toContainText("Muro Esempio");
});
