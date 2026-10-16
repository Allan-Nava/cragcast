# 0006 — History on its own branch

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-34

## Context

Each day's verdicts and weather are worth keeping — "how was last week", and data for the
calibration — but a commit to main every evening would bury the code's history in data.

## Decision

`history.yml` appends each evening's rows to one CSV per month on the orphan `history` branch,
never on main; a rerun replaces the day's rows. The CSV's columns are a contract like the API:
added at the end, never renamed or dropped.

## Consequences

- Main's log stays about code; the history is one checkout away.
- Two lines of storage per crag per day; a year of 220 crags is a few megabytes.
