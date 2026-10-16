# 0007 — The avalanche bulletin informs, it never scores

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-11

## Context

Lombardy's avalanche bulletin (ARPA Lombardia) is published by the EAWS as CAAML v6 JSON, with
micro-regions that are CC0. It matters for approaches and winter routes, not for whether rock
is dry. Neither ARPA Lombardia nor avalanche.report states a licence for the bulletin.

## Decision

Crags inside a micro-region show the day's danger level, elevation bands and problem types,
with the link to the official bulletin — the facts only, never the bulletin's text. The verdict
ignores the bulletin. A missing bulletin (out of season, before the morning issue) or a failed
fetch leaves the line out and never fails the hourly build.

## Consequences

- 119 of 220 crags are covered; the low ones lie outside the bulletin's area.
- The line appears by itself in season; nothing to maintain but the micro-regions file
  (`tools/avalanche_regions.py`, reviewed like any data change).
