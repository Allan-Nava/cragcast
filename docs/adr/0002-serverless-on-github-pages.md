# 0002 — Run on GitHub Pages, without a server

- **Status:** accepted
- **Date:** 2026-10-15
- **Backlog:** CG-15, CG-31–CG-37; parked: CG-6, CG-7, CG-8, CG-13 (v1.0.0)

## Context

The first design was a Spring Boot service with PostgreSQL on Oracle Cloud's Always Free A1
VM in Milan, deployed with Terraform and k3s. The service was finished, but the VM never was:
39 attempts over two days, always "Out of host capacity", and the account was to stay on the
free tier (no Pay As You Go). The verdict itself needs no server: it is a pure function of a
forecast and a wall's aspect, and the forecast changes hourly.

## Decision

The same Java scorer runs every hour in a GitHub Action (`pages.yml`), fetches Open-Meteo for
every crag, writes static JSON — the page's data, a versioned API ([0005](0005-static-api-v1.md)),
a share page per crag — and GitHub Pages serves it with the page. No server, no database, no
cost. The VM path (`infra/oci/`, `deploy/k8s/`) stays in the repository, parked as v1.0.0; the
Terraform network was destroyed.

## Consequences

- Free, nothing to patch, nothing to keep alive; the hourly run is the only moving part, and
  `uptime.yml` fails loudly when its data is over 150 minutes old.
- Data is at most an hour old, never live; anything per reader (accounts, alerts by e-mail)
  needs another service.
- The Spring service and the static export must share one scorer, so a verdict change lands
  in both.
