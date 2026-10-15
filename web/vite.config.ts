import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

// Builds the page into ../site, which GitHub Pages serves under /cragcast/: relative paths
// ("./") so it works at any base. tools/build_site.sh then adds data/, api/ and the logo.
export default defineConfig({
  base: "./",
  plugins: [react()],
  build: { outDir: "../site", emptyOutDir: true },
  test: {
    environment: "jsdom",
    // CG-47: the floors are the coverage measured when they were set, rounded down; raise them
    // as tests are added, never lower them.
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      exclude: ["src/**/*.test.{ts,tsx}", "src/test/**", "src/main.tsx", "src/types.ts"],
      reporter: ["text-summary", "json-summary"],
      thresholds: { lines: 88, statements: 82, functions: 81, branches: 80 },
    },
  },
});
