import AxeBuilder from "@axe-core/playwright";
import { AFTER, CRAGS, TOMORROW, expect, listNames, test } from "./fixtures";

test.describe("the page", () => {
  test("loads tomorrow's verdicts, ordered by verdict then name", async ({ page }) => {
    await page.goto("./");
    await expect(page.locator("#count")).toHaveText("6 falesie");
    await expect(page.locator(".summary")).toContainText("2 si va");
    await expect(page.locator(".summary")).toContainText("2 forse");
    expect(await listNames(page)).toEqual(["Còrna Test — Si va", "Sasso Finto — Si va", "Placca Prova — Forse",
      "Torre Inventata — Forse", "Muro Esempio — No", "Parete Immaginaria — No"]);
    await expect(page.locator("#updated")).toContainText("Aggiornato adesso");
  });

  test("search ignores accents; province and verdicts filter; the URL keeps the view and back restores it", async ({ page }) => {
    await page.goto("./");
    await page.getByPlaceholder("Cerca una falesia…").fill("corna");
    expect(await listNames(page)).toEqual(["Còrna Test — Si va"]);
    await expect(page).toHaveURL(/q=corna/);
    await page.getByPlaceholder("Cerca una falesia…").fill("");
    await page.getByLabel("Provincia").selectOption("SO");
    expect(await listNames(page)).toEqual(["Torre Inventata — Forse", "Muro Esempio — No"]);
    await page.getByRole("button", { name: /^N?\s*No$/ }).click();
    expect(await listNames(page)).toEqual(["Torre Inventata — Forse"]);
    await expect(page).toHaveURL(/p=SO/);
    await expect(page).toHaveURL(/v=GO%2CMAYBE/);
    await page.goBack();
    expect(await listNames(page)).toEqual(["Torre Inventata — Forse", "Muro Esempio — No"]);
    await page.reload();
    expect(await listNames(page)).toEqual(["Torre Inventata — Forse", "Muro Esempio — No"]);
  });

  test("the day buttons switch every verdict", async ({ page }) => {
    await page.goto("./");
    await page.getByRole("navigation", { name: "Giorno" }).getByRole("button", { name: "Dopodomani" }).click();
    await expect(page).toHaveURL(new RegExp(`day=${AFTER}`));
    await expect(page.locator(".summary")).toContainText("3 si va");
    expect((await listNames(page))[0]).toBe("Muro Esempio — Si va");
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
    await page.getByRole("button", { name: "Preferite (1)" }).click();
    expect(await listNames(page)).toEqual(["Sasso Finto — Si va"]);
    await expect(page).not.toHaveURL(/osm-node-4/);                      // favourites never in the URL
  });

  test("near me orders by distance", async ({ page, context }) => {
    await context.grantPermissions(["geolocation"]);
    await context.setGeolocation({ latitude: 46.2, longitude: 9.9 });
    await page.goto("./");
    await page.getByRole("button", { name: "Vicino a me" }).click();
    expect((await listNames(page))[0]).toBe("Muro Esempio — No");
    await expect(page.locator(".me")).toBeVisible();
  });

  test("no serious or critical accessibility violations", async ({ page }) => {
    await page.goto("./");
    await expect(page.locator("#list li h2").first()).toBeVisible();
    const results = await new AxeBuilder({ page }).exclude(".leaflet-control-attribution").analyze();
    const bad = results.violations.filter(v => v.impact === "serious" || v.impact === "critical");
    expect(bad.map(v => `${v.id}: ${v.nodes.map(n => n.target.join(" ")).join(", ")}`)).toEqual([]);
  });

  test("tomorrow is the default day", async ({ page }) => {
    await page.goto("./");
    const tomorrow = page.getByRole("navigation", { name: "Giorno" }).getByRole("button", { name: "Domani", exact: true });
    await expect(tomorrow).toHaveAttribute("aria-pressed", "true");
    await tomorrow.click();
    await expect(page).toHaveURL(new RegExp(`day=${TOMORROW}`));
  });
});
