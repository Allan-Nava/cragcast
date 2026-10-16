# 0018 — The Pi serves the page, with data from its own backend

- **Status:** accepted
- **Date:** 2026-10-16
- **Backlog:** CG-110

## Context

GitHub Pages is the public page ([0002](0002-serverless-on-github-pages.md)); the Pi runs the
Spring service and PostgreSQL for its owner ([0015](0015-raspberry-pi-pulls-its-stack.md)). The
owner wanted the page on the Pi too, next to Pages. Three sources for its data were weighed:
a copy of Pages' files (no independence), its own hourly export (a third set of Open-Meteo
requests — 220 crags an hour each, against a free limit of under 10 000 calls a day, counting
per location undocumented), or the Pi's backend, which already fetches every crag's hourly
forecast each hour.

## Decision

The backend. After each `WeatherRefresh`, the forecasts it fetched go out as an event;
`SiteData` passes them through the same `ConditionsExport.build()` as Pages and keeps
`data/conditions.json`, the API v1 files and the share pages in memory; `SiteController` serves
them at Pages' paths. The page is the same Vite build, in an nginx image (`web/Dockerfile`,
linux/arm64, `cragcast-web` on GHCR) that hands those paths to the backend. Two instances run
side by side: Pages for everyone, the Pi on the home network and the owner's tailnet.

## Consequences

- No new Open-Meteo requests, and one code path for the verdict: the Pi's JSON has Pages'
  shape field for field, and a change to the scorer reaches both.
- The two instances fetch at different minutes (:07 on the Pi, :17 on Pages), so their numbers
  can differ by an hour's update; neither is the other's copy.
- The Pi's data lives in memory: after a restart the page answers 503 until the first refresh,
  seconds later. Archiving it is CG-106.
- Share pages on the Pi link to `SITE_URL`, the Pi's address, set in Portainer.
