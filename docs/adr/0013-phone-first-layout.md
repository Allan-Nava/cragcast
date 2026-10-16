# 0013 — A layout per width: the list first on a phone

- **Status:** accepted
- **Date:** 2026-10-16
- **Backlog:** CG-53–CG-57, CG-58

## Context

On a phone the first crag sat under about 600 px of controls and a full-screen map. On a desktop
the crag's panel covered the filters and the list.

## Decision

- **Below 600 px:** the filters fold behind "Filtri (n)". A "Lista | Mappa" switch shows one view
  at a time, the list first, with the choice in the URL. Cards are compact.
- **From 900 px:** the crag's panel takes the list's column in the page's grid, below the
  controls, never over them.
- **At every width,** each layout fix comes with an end-to-end test at the width it fixes.

## Consequences

- On a phone the first crag is on the first screen. CG-58's compact cards (in progress on
  2026-10-16) bring a card from about 190 to under 125 px, about twice as many per screen.
- Two layouts to keep working; the Playwright projects (desktop, Pixel 7) and, with CG-63,
  visual snapshots guard them.
