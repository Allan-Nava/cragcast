// CG-63: visual snapshots of the main views, on fixed data at a fixed moment, compared with the
// images in e2e/__snapshots__/. Fonts differ between machines, so the images are CI's own: the
// comparison runs in CI (or with SNAPSHOTS=1 on a machine that made them); a change to the
// layout shows up as a failed comparison with the diff in the run's artifact. To accept a change,
// download CI's new images (the "snapshots" artifact) into e2e/__snapshots__/.
import { FIXED_NOW, expect, fixedConditions, test } from "./fixtures";

test.skip(!process.env.CI && !process.env.SNAPSHOTS, "visual snapshots compare CI's own rendering");

test.beforeEach(async ({ page }) => {
  await page.clock.setFixedTime(FIXED_NOW);
  await page.route("**/data/conditions.json*", r => r.fulfill({ json: fixedConditions() }));
});

const ready = async (page: import("@playwright/test").Page) => {
  await expect(page.locator("#count")).toHaveText("6 falesie");
  await page.evaluate(() => document.fonts.ready);
};

test("the page", async ({ page }) => {
  await page.goto("./?day=2026-10-17");
  await ready(page);
  await expect(page).toHaveScreenshot("page.png", { fullPage: true });
});

test("a crag's panel", async ({ page }) => {
  await page.goto("./?day=2026-10-17&crag=osm-node-2");
  await ready(page);
  await expect(page.locator("dialog#detail")).toBeVisible();
  await expect(page).toHaveScreenshot("panel.png");
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
