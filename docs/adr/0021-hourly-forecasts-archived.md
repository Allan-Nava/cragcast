# 0021 — Every hourly forecast is archived, appended and never rewritten

- **Status:** accepted
- **Date:** 2026-10-28
- **Backlog:** CG-106

## Context

`crag_weather` holds one row per crag and day and each refresh overwrites it, so what cragcast
said at 18:00 the day before is gone by 19:07. The history branch (ADR 0006) keeps one verdict a
day, made in the evening — not the hours before it, nor how a forecast for Saturday moved from
Thursday to Saturday morning. That is the question the calibration (CG-27) and any "how good
were the forecasts" check need answered. With 220 crags and three days kept, each refresh
computes 660 crag-days: about 16 000 a day.

## Decision

After each successful refresh, `ForecastArchive` appends what it computed to `crag_forecast`
(Flyway V8): per crag and day, the verdict, the reasons, the four numbers the scorer read and
the day's hours, keyed by crag, day and the refresh's time — the same `fetched_at` as
`crag_weather`. It scores the forecasts the refresh already fetched (the `ForecastsRefreshed`
event, as the page's data does), with the same `ConditionsScorer`. Rows are only inserted, never
updated: a row already there stays as written. Reasons and hours are JSON text, like
`crag.details`; the hours as parallel lists (`hour`, `rainMm`, `temperatureC`, `windKmh`), a third
the size of one object per hour. A failed append is logged and loses that hour only; it never
fails the refresh or the page.

## Consequences

- The table exists nowhere else: the nightly dump (ADR 0019) is its only copy, and a restore
  brings it back with the rest.
- Growth is steady, a few megabytes a day. No retention rule yet; one comes when the Pi's disk
  asks for it, as its own reviewed change — and only by dropping whole old days, never by
  rewriting rows.
- A change to the scorer applies to new rows only: the archive keeps what was said at the time,
  which is the point.
