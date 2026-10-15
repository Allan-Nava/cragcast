import { expect, listNames, test } from "./fixtures";

test.skip(({ isMobile }) => isMobile, "desktop layout");

test("the panel sits beside the map, which stays usable; its days switch; Esc closes it", async ({ page }) => {
  await page.goto("./");
  await page.locator("#crag-osm-node-2 .more").click();
  const panel = page.locator("dialog#detail");
  await expect(panel).toHaveClass(/side/);
  await page.locator(".leaflet-control-zoom-in").click();                 // the map still takes clicks
  await panel.getByRole("button", { name: /Dopodomani/ }).click();
  await expect(panel.locator("#d-title")).toContainText("Placca Prova — Si va");
  await page.keyboard.press("Escape");
  await expect(panel).toBeHidden();
});

test("pointing at a crag in the list rings its mark", async ({ page }) => {
  await page.goto("./");
  await page.locator("#crag-osm-node-4").hover();
  await expect(page.locator(".leaflet-marker-icon.hot, .hot")).toHaveCount(1);
});

test("after a real drag, 'Cerca in quest'area' limits the list to the map", async ({ page }) => {
  await page.goto("./?day=" + new Date(Date.now() + 864e5).toISOString().slice(0, 10));
  await expect(page.getByRole("button", { name: "Cerca in quest'area" })).toHaveCount(0);
  const map = page.locator("#map");
  // Zoom in three steps on Lombardy's centre; Leaflet ignores a zoom asked while one animates.
  for (let i = 0; i < 3; i++) {
    await page.locator(".leaflet-control-zoom-in").click();
    await expect(page.locator(".leaflet-zoom-anim")).toHaveCount(0);
    await page.waitForTimeout(300);
  }
  const box = (await map.boundingBox())!;
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width / 2 + 120, box.y + box.height / 2 + 40, { steps: 8 });
  await page.mouse.up();
  await page.getByRole("button", { name: "Cerca in quest'area" }).click();
  await expect(page).toHaveURL(/a=/);
  const shown = await listNames(page);
  expect(shown.length).toBeLessThan(6);
  await page.getByRole("button", { name: "Togli il filtro dell'area" }).click();
  await expect(page.locator("#count")).toHaveText("6 falesie");
});

test("choosing a crag in the list flies the map to it and opens its popup", async ({ page }) => {
  await page.goto("./");
  await page.locator("#crag-osm-node-5 h2").click();
  await expect(page.locator(".leaflet-popup-content")).toContainText("Parete Immaginaria");
  await expect(page.locator("#crag-osm-node-5")).toHaveClass(/chosen/);
  expect((await listNames(page)).length).toBe(6);
});

test("CG-53: on a desktop the filters stay in the row, without the 'Filtri' button", async ({ page }) => {
  await page.goto("./");
  await expect(page.getByRole("button", { name: /^Filtri/ })).toBeHidden();
  await expect(page.getByLabel("Provincia")).toBeVisible();
  await expect(page.locator(".summary .date")).toBeVisible();
});

test("CG-54: on a desktop the map and the list show together, without the switch", async ({ page }) => {
  await page.goto("./?show=map");
  await expect(page.getByRole("group", { name: "Vista" })).toBeHidden();
  await expect(page.locator("#map")).toBeVisible();
  await expect(page.locator("#list")).toBeVisible();
});

test("CG-56: the panel takes the list's column, below the controls; the list comes back on close", async ({ page }) => {
  await page.goto("./");
  const controls = (await page.locator(".controls").boundingBox())!;
  const map = (await page.locator("#map").boundingBox())!;
  await page.locator("#crag-osm-node-2 .more").click();
  const panel = page.locator("dialog#detail");
  await expect(panel).toBeVisible();
  const box = (await panel.boundingBox())!;
  expect(box.y).toBeGreaterThanOrEqual(controls.y + controls.height);     // the filters stay in sight
  expect(box.x).toBeGreaterThanOrEqual(map.x + map.width);                // beside the map, not over it
  await expect(page.getByLabel("Provincia")).toBeVisible();
  await expect(page.locator("#list")).toBeHidden();
  expect((await page.locator("#map").boundingBox())!.width).toBe(map.width);   // the map keeps its place
  await panel.getByLabel("Chiudi").click();
  await expect(page.locator("#list")).toBeVisible();
});
