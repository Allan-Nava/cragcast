// CG-63: visual snapshots of the main views, on fixed data at a fixed moment, compared with the
// images in e2e/__snapshots__/. Fonts differ between machines, so the images are CI's own: the
// comparison runs in CI (or with SNAPSHOTS=1 on a machine that made them); a change to the
// layout shows up as a failed comparison with the diff in the run's artifact. To accept a change,
// download CI's new images (the "snapshots" artifact) into e2e/__snapshots__/:
//   gh run download <run> -n snapshots -D web/e2e/__snapshots__
import { FIXED_DAYS, conditions, expect, fixedCrags, test } from "./fixtures";

test.skip(!process.env.CI && !process.env.SNAPSHOTS, "visual snapshots compare CI's own rendering");

// Fixed days, so weekdays never move. The clock is not frozen: Leaflet times its pans by the
// clock, and a frozen one stopped the pan that fits a popup. The one text that follows the
// clock, the footer's "Aggiornato …", is masked instead.
test.beforeEach(async ({ page }) => {
  await page.route("**/data/conditions.json*", r => r.fulfill({ json: conditions(fixedCrags(), FIXED_DAYS) }));
});
const mask = (page: import("@playwright/test").Page) => ({ mask: [page.locator("#updated")] });

const ready = async (page: import("@playwright/test").Page) => {
  await expect(page.locator("#count")).toHaveText("6 falesie");
  await page.evaluate(() => document.fonts.ready);
};

test("the page", async ({ page }) => {
  await page.goto("./?day=2026-10-17");
  await ready(page);
  await expect(page).toHaveScreenshot("page.png", { fullPage: true, ...mask(page) });
});

test("a crag's panel", async ({ page }) => {
  await page.goto("./?day=2026-10-17&crag=osm-node-2");
  await ready(page);
  await expect(page.locator("dialog#detail")).toBeVisible();
  await expect(page).toHaveScreenshot("panel.png", mask(page));
});

test("the map", async ({ page, isMobile }) => {
  await page.goto(`./?day=2026-10-17${isMobile ? "&show=map" : ""}`);
  await ready(page);
  await expect(page.locator("#map")).toHaveScreenshot("map.png");
});

test("the filters open", async ({ page, isMobile }) => {
  test.skip(!isMobile, "folded only on a phone");
  await page.goto("./?day=2026-10-17");
  await ready(page);
  await page.getByRole("button", { name: /^Filtri/ }).click();
  await expect(page.locator(".controls")).toHaveScreenshot("filters.png");
});
