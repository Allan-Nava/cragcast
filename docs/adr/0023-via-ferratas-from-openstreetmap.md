# 0023 — Via ferratas from OpenStreetMap, in their own file

- **Status:** accepted
- **Date:** 2026-10-28
- **Backlog:** CG-112 (CG-111 counted what OSM has)

## Context

v1.3.0 asks "can I do that via ferrata tomorrow?" next to the crags. OpenStreetMap stays the only
source of places ([ADR 0003](0003-crags-from-openstreetmap.md)). CG-111 counted Lombardy on
2026-10-28: 74 `highway=via_ferrata` ways and 5 `route=via_ferrata` relations, about 42 via
ferratas — 35 named, 22 with a plain 0–6 `via_ferrata_scale`; 7 ways carry `2+`, `3+` or `K2`.
Most via ferratas are loose pieces, not relations, so something has to say which pieces are
one route. A via ferrata is not a crag: `data/crags.json` never holds single routes (CG-42).

## Decision

`tools/osm_ferrate.py` writes `data/ferrate.json` and `docs/ferrate-todo.md`, refreshed by the
same reviewed monthly PR as the crags (`crags-refresh.yml`); a failed query leaves both as they
were and never holds back the crags' PR.

- **Grouping.** A relation is one via ferrata, made of its via_ferrata ways. Loose ways with the
  same name (case and accents aside) join when their ends lie within 500 m; an unnamed way
  joins a piece whose end it touches (25 m); a group never holds two different names. A group
  with no name is left out and listed for a name on OSM.
- **Difficulty.** `via_ferrata_scale` as OSM documents it, a whole number 0–6, kept as the
  range of the pieces' values (`scaleMin`, `scaleMax`). Anything else — `2+`, `K2`, `7` — is
  dropped and listed in the todo, never read as the number next to it.
- **Where it starts, where it tops out.** From the track and the Copernicus DEM (90 m), as for
  the crags' elevation: the lowest end of the pieces is the start, the highest of a dozen
  points sampled along them is the top. Length is the pieces' summed length.
- Each via ferrata keeps its OSM id (the relation's, or its lowest way id) and links to every
  piece; ODbL, credited wherever it is shown.

## Consequences

- The data is what climbers have mapped. Half the via ferratas have no usable difficulty, and the
  page will say so (CG-114) rather than guess; the todo asks for it first-hand.
- The grouping is a rule, tested on made-up ways; a route drawn oddly can still split or join
  wrongly, and the fix is an edit on OSM (a relation settles it).
- The start and top are a 90 m model's: good for the weather at the top (CG-113), not for
  telling two ledges apart.
- `data/ferrate.json` is not in the backend's jar yet; CG-115 adds it, with the Dockerfile and
  `image.yml` (the CG-103 rule).
