# 0012 — Tests at every level, floors that only go up

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-47–CG-52

## Context

Asked for on 2026-10-15: unit and integration tests for everything. The baseline that day was
backend 64% of lines without Docker, page 88%, osm_crags.py 67% and four tools at 0%, with no
test of the whole pipeline and none in a browser.

## Decision

- Unit tests per part: JUnit, Vitest, unittest.
- Integration tests run whole entry points against fake servers on localhost (`*IT` under
  failsafe; `FakeHttp`; a fake Overpass).
- Playwright drives the built page in Chromium on fixture data, desktop and phone, with an axe
  scan, for what jsdom cannot do: dragging the map, layout, swipes.
- Coverage is measured in CI, with floors set from CI's figures that only go up. A change that
  drops coverage brings tests.
- actionlint and shellcheck lint the plumbing.

## Consequences

- Floors on 2026-10-16: backend 93%, page 99% of lines, tools 97%.
- The tests found real defects on the way: a WCAG 2.5.3 failure, integration tests that Maven
  never ran, a valid URL hidden by a malformed tag, and a `cd` that could fail silently.
- Every PR pays about two minutes of CI.
