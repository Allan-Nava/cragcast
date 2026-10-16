// CG-51: the page end to end in Chromium — the built site from `vite preview`, the data from a
// fixture (e2e/fixtures.ts), map tiles stubbed so no test reaches OpenStreetMap.
import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  // CG-63: one folder of images per project. Repeated runs on one machine are identical to the
  // pixel, so the tolerance is a few pixels: a ratio of 1% let a broken logo pass.
  snapshotPathTemplate: "{testDir}/__snapshots__/{projectName}/{arg}{ext}",
  expect: { toHaveScreenshot: { maxDiffPixels: 25, animations: "disabled", caret: "hide" } },
  use: {
    baseURL: "http://localhost:4173/",
    serviceWorkers: "block",
    locale: "it-IT",
    timezoneId: "Europe/Rome",
    trace: "retain-on-failure",
    contextOptions: { reducedMotion: "reduce" },
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"], viewport: { width: 1280, height: 900 } } },
    { name: "phone", use: { ...devices["Pixel 7"] } },
  ],
  webServer: {
    command: "npm run build && npx vite preview --port 4173 --strictPort",
    url: "http://localhost:4173/",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
