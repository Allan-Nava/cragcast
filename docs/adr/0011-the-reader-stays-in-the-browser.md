# 0011 — What is personal stays in the browser

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-22, CG-46

## Context

"Near me" needs the reader's position; favourites are a list of crags someone cares about.
Neither is needed anywhere but on the reader's own screen, and a static site has nowhere safe
to keep them.

## Decision

The position lives in the page's state only; favourites in `localStorage` only, every access
guarded. Neither is ever put in the URL, in a request or in shared state. A browser that
refuses storage keeps favourites for the visit, and nothing breaks.

## Consequences

- Nothing personal to protect, leak or delete on our side.
- Favourites do not follow the reader across devices.
