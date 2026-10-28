# 0022 — The avalanche bulletin's facts are kept, its text never

- **Status:** accepted
- **Date:** 2026-10-28
- **Backlog:** CG-107

## Context

The page shows the day's avalanche bulletin next to a crag and forgets it the next day
([ADR 0007](0007-avalanche-bulletin-informs.md)). Next to the archived forecasts
([ADR 0021](0021-hourly-forecasts-archived.md)), the bulletin of a past day is part of "what
was it like there" — and the EAWS file for a day is not guaranteed to stay online. ARPA
Lombardia states no licence for the bulletin, so its wording cannot be copied.

## Decision

`AvalancheArchive` reads today's bulletin with the same `AvalancheBulletins` the page uses — at
startup and at 09:30, 13:30 and 18:30 Italian time — and keeps it in `avalanche_bulletin`
(Flyway V9), one row per day and micro-region: the highest level, the levels by band and part of
the day, the problem types and the link to the official bulletin. Never the text. The latest
issue of a day replaces that day whole; no bulletin (out of season, before the morning issue, a
failed request) writes nothing and keeps what is there.

## Consequences

- The facts stay, linked to their source, as on the page; the wording stays ARPA Lombardia's.
- The bulletin still informs and never scores: nothing reads the table to make a verdict.
- A day the Pi was off at every run has no row; the archive is what was seen, never filled in
  afterwards.
- Like `crag_forecast`, the rows exist only on the Pi, and the nightly dump is their copy.
