#!/usr/bin/env bash
# Build site/ as GitHub Pages serves it: build the React page (web/, Vite empties site/ first),
# compile the scorer, score every crag, write site/data/conditions.json and the static API in
# site/api/v1/. The logo comes with the page's build (vite.config.ts).
# Used by pages.yml (publish) and lighthouse.yml (measure); runnable locally with Java 21 and Node 22+.
set -euo pipefail
cd "$(dirname "$0")/.."
(
  cd web
  npm ci --no-audit --no-fund
  npm run build
)
(
  cd backend
  ./mvnw -q -B -DskipTests package
  ./mvnw -q -B dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=runtime
  java -cp "target/classes:$(cat target/cp.txt)" io.github.allannava.cragcast.export.ConditionsExport \
    ../data/crags.json ../site/data/conditions.json ../site/api/v1 ../site
)
cp assets/social-preview.png site/og.png      # CG-45: the image of every link preview
