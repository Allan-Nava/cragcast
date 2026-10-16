# 0009 — Import sport=climbing crags, folding their sectors

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-42

## Context

Many Lombard crags are on OSM as `sport=climbing` cliffs, rocks or climbing sites without
`climbing=crag` — Galbiate, Maddalena, Predore — and many are mapped as several sectors or as
loose points. Importing every object would list sectors as crags; importing only
`climbing=crag` left out two thirds of the crags.

## Decision

`osm_crags.py` also imports named outdoor `sport=climbing` places, never gyms, shops, buildings
or single routes, and folds what is one crag: a shape within 50 m of a crag is the same place
unless it is a sibling sector; parts sharing a base name within 2 km are one crag named by that
base; a loose point within 300 m is a sector. A `climbing=crag` keeps its id, name and data. A
sector never lends its route count, mean grade or aspect: grades widen to the parts' range and
the aspect stays only when every part agrees.

## Consequences

- 75 → 220 crags; the 75 kept their ids. The rules are tested in `tools/test_osm_crags.py`.
- Seventeen loose points that could not be placed are left out and named in the run's log.
- The rules are heuristics over other people's mapping; the monthly PR is where a wrong fold is
  caught.
