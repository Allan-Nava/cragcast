import { CRAGS, expect, test } from "./fixtures";

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
