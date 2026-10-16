# 0003 — Crags from OpenStreetMap, first-hand data only

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-2, CG-16, CG-17, CG-25, CG-26, CG-27

## Context

A verdict is only as good as the crag's position and wall aspect. Guidebooks have the data but
are under copyright; inventing an aspect would make a verdict look precise when it is not.

## Decision

Crags come only from OpenStreetMap (`tools/osm_crags.py`, ODbL), each with the link of the
object it came from; the aspect is OSM's `climbing:orientation` or null, never guessed. Missing
data is fixed on OSM, first-hand, by someone who knows the crag — never copied from a guide.
The same holds for the calibration's observations (`data/observations.csv`): only days someone
saw the rock. The list changes only through a reviewed PR (`crags-refresh.yml`, monthly).

## Consequences

- Every position traces to a source; credit OSM on every surface that shows the data.
- Coverage depends on OSM: on 2026-10-16 only 27 of 220 crags had an aspect, so most verdicts
  rest on the cautious default of [0004](0004-unmapped-aspect-is-shaded.md).
- The page asks readers to map their crag (the help section, `docs/aspects-todo.md`).
