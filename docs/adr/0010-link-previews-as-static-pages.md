# 0010 — Link previews as static pages

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-44, CG-45

## Context

A crag shared on WhatsApp or Telegram should show its name and verdict. Link previews read
Open Graph tags from HTML and never run JavaScript, so the React page cannot provide them.

## Decision

The hourly export writes `c/<id>.html` per crag (`SharePages`): Open Graph tags with the name and
the next day's verdict and weather, the day written as a date — never "domani", since a preview
is read later than it is made — and a redirect that sends a browser on to `?crag=<id>`.
"Condividi" shares that page.

## Consequences

- 220 small files per hour, regenerated with the data.
- The reason words exist twice (Java and TypeScript) and must change together.
