# 0025 — Via ferratas on the page: their own switch, their own mark

- **Status:** accepted
- **Date:** 2026-10-28
- **Backlog:** CG-114

## Context

The via ferratas have their data (ADR 0023) and their verdict (ADR 0024). The page was made for
crags: its marks tell the verdict by shape and letter (circle S, diamond F, square N — ADR 0014),
its filters are provinces and French grades, its words are about walls. A reader must never take a
via ferrata's "si va" for a crag's, nor read a mark's shape as a kind of place.

## Decision

- **One kind at a time.** A "Falesie / Vie ferrate" switch, in the URL as `?k=ferrate`, shows
  the crags or the via ferratas, never both on one map. It appears only when the data has via
  ferratas, so data without them (the Pi's, until CG-115) reads as before.
- **The kind in words and by its own mark.** A via ferrata keeps the verdict's shape and letter,
  with a peak beside it — on the map at the mark's corner, in the list and the panel next to it;
  the list says "via ferrata", the panel's title starts with it, a mark's name reads
  "Via ferrata …". No colour carries the kind.
- **What is said about it.** The difficulty as OSM gives it ("difficoltà 2–3 (scala 0–6)", or
  that it is not mapped, and a dropped value such as «2+» named), the length, the start and top
  elevations, and every piece it is drawn in. Weather and hours are the top's.
- **Checked on the spot.** A line under the summary, in the panel and in the popup says the
  verdict is an estimate from the forecast at the top and conditions are checked on the spot.
- **One shape of data.** `data/conditions.json` gains `ferrate`, left out when empty; the page
  turns each into a place of the crags' shape (`asPlace`), so the list, map, panel, favourites
  and charts serve both. Province and grade filters are the crags' and never hide a via ferrata.
- **Not yet:** no share page and no API v1 entry for a via ferrata, and the Pi's page has none —
  CG-115.

## Consequences

- One more Open-Meteo request an hour for all the tops; if it fails, the crags still publish and
  the page simply has no via ferratas that hour.
- Readers can link to the via ferratas (`?k=ferrate`) and to one of them (`&crag=`), but the
  "Condividi" button stays off for them until a share page exists.
- The snapshots do not change: the fixture they use has no via ferratas. An e2e test covers the
  switch, the mark, the panel and the axe scan in both themes.
