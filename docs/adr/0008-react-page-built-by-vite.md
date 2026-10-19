# 0008 — The page in React, built by Vite

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-10

## Context

The first page was plain HTML and JavaScript with Leaflet from a CDN. It grew to search,
filters, URL state, clusters, a panel with charts, a PWA — enough state that plain DOM code
became hard to change without breaking another part.

## Decision

The page is React 19 with TypeScript, built by Vite from `web/` into `site/` (all of `site/` is
generated, never committed). Leaflet and its cluster plugin come from npm, bundled, so the page
loads no third-party script; Leaflet stays imperative inside one component (`CragMap`), and
text from OSM reaches the DOM as text, never as markup.

## Consequences

- A build step and node_modules; in return types, components and unit tests.
- The bundle was about 430 KB (130 KB gzipped). Since CG-64 the page's code is a 32 KB chunk,
  React and Leaflet chunks of their own that stay cached across deploys, and the crag's panel is
  fetched on demand; the first render's cost is mostly React drawing the list.
