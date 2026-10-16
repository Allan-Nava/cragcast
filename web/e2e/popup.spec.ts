import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "./fixtures";

test.skip(({ isMobile }) => isMobile, "the popup is checked on the desktop layout");

test("CG-59: an open popup passes the accessibility scan, its button included", async ({ page }) => {
  await page.goto("./");
  await page.locator("#crag-osm-node-5 h2").click();
  await expect(page.locator(".leaflet-popup-content .more")).toBeVisible();
  const results = await new AxeBuilder({ page }).include(".leaflet-popup").analyze();
  const bad = results.violations.filter(v => v.impact === "serious" || v.impact === "critical");
  expect(bad.map(v => `${v.id}: ${v.nodes.map(n => n.target.join(" ")).join(", ")}`)).toEqual([]);
});

/** WCAG 2 contrast ratio of two "rgb(r, g, b)" colours. */
function contrast(a: string, b: string) {
  const lum = (c: string) => {
    const [r, g, bl] = c.match(/\d+/g)!.slice(0, 3).map(Number).map(v => v / 255)
      .map(v => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
    return 0.2126 * r + 0.7152 * g + 0.0722 * bl;
  };
  const [hi, lo] = [lum(a), lum(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

test("CG-59: the popup's button is readable on the popup's white (4.5:1)", async ({ page }) => {
  await page.goto("./");
  await page.locator("#crag-osm-node-5 h2").click();
  const b = page.locator(".leaflet-popup-content .more");
  await b.waitFor();
  const { fg, bg } = await b.evaluate(el => ({
    fg: getComputedStyle(el).color,
    bg: getComputedStyle(el.closest(".leaflet-popup-content-wrapper")!).backgroundColor,
  }));
  expect(contrast(fg, bg)).toBeGreaterThanOrEqual(4.5);
});

test("CG-59: an open popup sits wholly inside the map, its close button with a margin", async ({ page }) => {
  await page.goto("./");
  await page.locator("#crag-osm-node-5 h2").click();
  const popup = page.locator(".leaflet-popup-content-wrapper");
  await expect(popup).toBeVisible();
  await expect.poll(async () => {
    const m = (await page.locator("#map").boundingBox())!, p = (await popup.boundingBox())!;
    return p.y - m.y;
  }).toBeGreaterThanOrEqual(10);
});
