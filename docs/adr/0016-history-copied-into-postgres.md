# 0016 — History copied into PostgreSQL; the CSV stays the source

- **Status:** accepted
- **Date:** 2026-10-16
- **Backlog:** CG-101

## Context

The Spring service now runs on a Raspberry Pi ([0015](0015-raspberry-pi-pulls-its-stack.md)),
and its database should hold the data cragcast has. The crags and the weather already fill it
by themselves at startup (`CragImporter`, `WeatherRefresh`). The verdicts of past days do not:
they live only as monthly CSVs on the `history` branch ([0006](0006-history-on-its-own-branch.md)),
295 rows by 2026-10-16. The observations are first-hand rows changed only by a reviewed PR, and
`Calibrate` reads them from the file.

## Decision

A table `crag_history` (Flyway V4) with the CSV's columns, keyed by day and crag slug, and no
foreign key to `crag`: a crag that leaves OSM keeps its past. `HistorySync` reads the branch's
raw files from GitHub — every month since 2026-10 at startup, the last two at 23:50 and 05:50
Italian time, after `history.yml` — and replaces each day it reads whole, as `history.yml`'s
rerun does. Columns are found by header name, so a column added at the end changes nothing.
Observations stay in git only.

## Consequences

- The CSV stays the source; the table is a copy. A lost database is rebuilt by restarting the
  service, so the Pi's database needs no backup to be correct.
- The service reads GitHub and nothing reaches into it, as [0015](0015-raspberry-pi-pulls-its-stack.md)
  requires. A failed read keeps the rows already there; the next run tries again.
- A day reaches the table up to a few hours after `history.yml`, not at once.
- Spring tests switch it off (`cragcast.history.enabled=false`), as they do the weather refresh.
