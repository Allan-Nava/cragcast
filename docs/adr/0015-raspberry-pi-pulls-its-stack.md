# 0015 — The Raspberry Pi pulls its stack; no self-hosted runner

- **Status:** accepted
- **Date:** 2026-10-16
- **Backlog:** CG-100

## Context

The Spring service and PostgreSQL were built for an Oracle VM that never came
([0002](0002-serverless-on-github-pages.md)). A Raspberry Pi with Portainer became available on
a friend's home network, a place to run them without cost. The first idea was to register it
as a self-hosted GitHub Actions runner and deploy from a workflow.

The repository is public. A self-hosted runner runs any job that names its labels, and a pull
request from a fork can change a workflow to do so: code from a stranger would run inside
someone else's home network. A personal account has no runner groups to fence it, and GitHub
advises against self-hosted runners on public repositories. The image needs no runner anyway:
`image.yml` already builds it for linux/arm64 on a GitHub-hosted ARM runner and pushes it to
GHCR.

## Decision

No runner. The Pi runs `deploy/pi/compose.yml` as a Portainer Git stack: Portainer polls this
repository and re-pulls `ghcr.io/allan-nava/cragcast-backend:main` when the stack or the image
changes. The database password lives in the stack's environment in Portainer. The service is
reachable on the home network only; PostgreSQL publishes no port. GitHub Pages stays the only
public surface.

## Consequences

- Nothing outside can reach the Pi or run code on it; a deploy is whatever is on main, a
  polling interval later. A bad image on main reaches the Pi too — the tag `:<sha>` pins it.
- `deploy/k8s/` and `infra/oci/` stay parked; the Pi is a second, simpler way to run the same
  service, and a change to the service lands there with no extra step.
- Making it public later (a tunnel, a domain) is a new decision, with a target in
  `monitoring/checkfleet.yml`; until then uptime checks do not cover it.
