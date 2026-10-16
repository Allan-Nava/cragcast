# 0005 — A static, versioned API

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-32, CG-11, CG-45

## Context

The data the page reads is useful to others (apps, bots), and GitHub Pages can only serve
files ([0002](0002-serverless-on-github-pages.md)).

## Decision

The hourly export writes `/api/v1/crags.json` (every crag, its verdict per day, the path of its
own file) and `/api/v1/crags/<id>.json` (one crag whole). Version 1 is a contract: fields are
only ever added, never renamed or removed; a breaking change goes to `/api/v2/`.

## Consequences

- Free to call, no key, cached by GitHub's CDN.
- Fields added later (`avalanche`, `meta.avalancheData`) are optional and documented in the
  README; a client that ignores them keeps working.
