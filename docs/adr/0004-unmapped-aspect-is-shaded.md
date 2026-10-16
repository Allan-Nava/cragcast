# 0004 — An unmapped wall is judged as shaded

- **Status:** accepted
- **Date:** 2026-10-13
- **Backlog:** CG-3, CG-21

## Context

Recent rain dries faster off a sunny wall: the scorer allows 10 mm in the previous 48 hours on
a wall facing S, SE or SW, half of it on any other. Most crags have no mapped aspect
([0003](0003-crags-from-openstreetmap.md)), so the scorer needs a default.

## Decision

A crag without `climbing:orientation` is scored as a shaded wall — the cautious reading — and
every surface says so: the reasons, the panel, and on the list the short "esposizione ?" tag.

## Consequences

- A sunny wall that nobody mapped gets a stricter verdict after rain than it deserves; the
  error is towards a wasted trip avoided, not a wet day promised.
- Mapping an aspect on OSM sharpens that crag's verdict at the next monthly refresh.
