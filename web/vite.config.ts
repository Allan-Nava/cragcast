import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

// Builds the page into ../site, which GitHub Pages serves under /cragcast/: relative paths
// ("./") so it works at any base. tools/build_site.sh then adds data/, api/ and the logo.
export default defineConfig({
  base: "./",
  plugins: [react()],
  build: { outDir: "../site", emptyOutDir: true },
  test: { environment: "jsdom" },
});
