# 0020 — Releases by date, and the Pi follows a release

- **Status:** accepted
- **Date:** 2026-10-20
- **Backlog:** CG-116

## Context

Since [ADR 0015](0015-raspberry-pi-pulls-its-stack.md) the Pi re-pulls `cragcast-backend:main`
and `cragcast-web:main` on every poll, so every merge that touches the backend — a test, a doc
comment — restarts it, and the page answers 502 for a minute or two while the new image starts.
On 2026-10-20 a merge restarted it and the restart looked like a failure. There was also no
name for what ran on the Pi: going back meant finding a commit's image by its SHA.

The milestone names (v0.7.0, v1.2.0…) are groups of work, opened and closed out of order; a
version scheme that reused them would read as a sequence they are not.

## Decision

- **Releases are named by their day in Italy:** `vYYYY.MM.DD`, with `.2`, `.3` for a second or
  third on the same day — as the cv repository's tags are.
- **`release.yml`, run by hand from main,** builds the backend's and the page's arm64 images
  tagged with that name and carrying it inside (`/actuator/info`, the page's footer). Only once
  the images exist does it create the GitHub Release, with notes generated from the merged PRs.
- **The Pi's stack follows a release.** Both images come from `:${CRAGCAST_VERSION:-main}`; with
  the variable set in Portainer, the Pi changes only when it does. Going back is setting the
  previous release and redeploying. `:main` is still pushed on every merge, for anyone who wants
  to follow it, and is the default when the variable is unset.

## Consequences

- A merge no longer restarts the Pi; a release does, when its owner chooses.
- What runs is named: `/actuator/info` and the footer say which release.
- Releasing is a manual step: nothing reaches the Pi until someone runs the workflow and sets the
  variable.
- GitHub Pages is not versioned — it still follows main hourly ([ADR 0002](0002-serverless-on-github-pages.md)).
