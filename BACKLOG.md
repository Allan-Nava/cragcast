# Backlog — cragcast

Single source of truth for planned work. Items keep a stable `CG-n` id; ROADMAP.md is
generated from this file (`npx backlogsync roadmap`) and the GitHub issues follow on push to
main.

```
## v0.2.0 — Title <!-- ms: phase=next -->

- [ ] **CG-99 — Short name**: what it is, why, what it touches. <!-- cg: prio=high size=M labels=backend -->
```

## v0.1.0 — A verdict for one crag <!-- ms: phase=now -->

- [x] **CG-1 — Backend skeleton**: Spring Boot 4 on Java 21 with PostgreSQL and Flyway; the
  crag table and `GET /api/crags`, `GET /api/crags/{slug}`; the conditions scorer with its
  tests; Docker Compose, k3s manifests, CI. <!-- cg: prio=high size=M labels=backend,infra ver=main -->
- [ ] **CG-2 — The crag list**: 20–30 crags in Lombardy, each with name, region, coordinates,
  wall aspect and the public source they come from, loaded by a Flyway migration. No
  coordinates typed from memory. <!-- cg: prio=high size=M labels=data -->
- [ ] **CG-3 — Open-Meteo client**: hourly precipitation, temperature and wind for a crag's
  coordinates, reduced to the `Weather` record (rain in the previous 48 h, rain in climbing
  hours, max temperature, max wind); tests against a recorded response. <!-- cg: prio=high size=M labels=backend -->
- [ ] **CG-4 — Conditions endpoint**: `GET /api/crags/{slug}/conditions?date=` returns the
  verdict and its reasons; calibrate the scorer's thresholds on a handful of real days. <!-- cg: prio=high size=M labels=backend -->
- [ ] **CG-5 — Hourly refresh**: a scheduled job fetches the forecast for every crag once an
  hour and stores it, so the API never waits on Open-Meteo. <!-- cg: prio=med size=M labels=backend -->

## v0.2.0 — On the internet, for free <!-- ms: phase=next -->

- [ ] **CG-6 — The VM as code**: Terraform for the Oracle Cloud Always Free A1.Flex VM in
  eu-milan-1 with its VCN, public subnet, internet gateway, route and security rules; a
  retry loop for "Out of host capacity". <!-- cg: prio=high size=M labels=infra -->
- [ ] **CG-7 — k3s and the first deploy**: install k3s on the VM, open 80/443 in the OCI
  security list and in the VM's iptables, create the database secret, `kubectl apply -k
  deploy/k8s`. <!-- cg: prio=high size=M labels=infra -->
- [ ] **CG-8 — Domain and HTTPS**: a domain or subdomain, cert-manager with Let's Encrypt,
  the host in the ingress. <!-- cg: prio=med size=S labels=infra -->
- [ ] **CG-9 — Continuous delivery**: CI builds the backend image for linux/arm64, pushes it
  to GHCR and rolls the deployment. <!-- cg: prio=med size=M labels=infra -->

## v0.3.0 — The page climbers use <!-- ms: phase=later -->

- [ ] **CG-10 — Web page**: React on GitHub Pages — a map of the crags with tomorrow's
  verdict as a traffic light, and the reasons on click. <!-- cg: prio=med size=L labels=frontend -->
- [ ] **CG-11 — Avalanche bulletin**: for winter routes, the AINEVA / regional bulletin next
  to the weather. <!-- cg: prio=low size=M labels=data,backend -->
- [ ] **CG-12 — Write-up**: a post on allan-nava.github.io and on LinkedIn on how the verdict
  is computed and how it runs on Oracle's free tier. <!-- cg: prio=low size=S labels=docs -->
