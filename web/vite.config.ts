import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { readFileSync } from "node:fs";

// Builds the page into ../site, which GitHub Pages serves under /cragcast/: relative paths
// ("./") so it works at any base. tools/build_site.sh then adds data/, api/ and the logo.
export default defineConfig({
  base: "./",
  plugins: [react(), {
    // The logo's one copy is assets/logo.svg (also the README's); the build emits it into site/.
    name: "cragcast-logo",
    generateBundle() {
      this.emitFile({ type: "asset", fileName: "logo.svg", source: readFileSync(new URL("../assets/logo.svg", import.meta.url)) });
    },
  }],
  build: { outDir: "../site", emptyOutDir: true },
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.{ts,tsx}"],          // e2e/ belongs to Playwright
    // CG-47: the floors are the coverage measured when they were set, rounded down; raise them
    // as tests are added, never lower them.
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      exclude: ["src/**/*.test.{ts,tsx}", "src/test/**", "src/main.tsx", "src/types.ts"],
      reporter: ["text-summary", "json-summary"],
      thresholds: { lines: 99, statements: 97, functions: 96, branches: 91 },
    },
  },
});
