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
- [x] **CG-2 — The crag list**: 20–30 crags in Lombardy, each with name, region, coordinates,
  wall aspect and the public source they come from. Done from OpenStreetMap (climbing=crag):
  `tools/osm_crags.py` writes `data/crags.json`, 76 crags, each with its OSM link; the aspect
  is the OSM tag `climbing:orientation` where mapped (2 of 76), null otherwise. No coordinates
  typed from memory. <!-- cg: prio=high size=M labels=data ver=main -->
- [x] **CG-3 — Open-Meteo client**: hourly precipitation, temperature and wind for a crag's
  coordinates, reduced to the `Weather` record (rain in the previous 48 h, rain in climbing
  hours, max temperature, max wind); tests against a recorded response. <!-- cg: prio=high size=M labels=backend ver=main -->
- [ ] **CG-4 — Conditions endpoint**: `GET /api/crags/{slug}/conditions?date=` returns the
  verdict and its reasons; calibrate the scorer's thresholds on a handful of real days. <!-- cg: prio=high size=M labels=backend -->
- [x] **CG-14 — Logo**: an SVG mark in the palette of allan-nava.github.io (green #10cf53 on
  #050505) — a crag with the sun behind it — at the top of the README, plus a 1280×640 social
  preview image for the repository. <!-- cg: prio=low size=S labels=docs ver=main -->
- [ ] **CG-5 — Hourly refresh**: a scheduled job fetches the forecast for every crag once an
  hour and stores it, so the API never waits on Open-Meteo. <!-- cg: prio=med size=M labels=backend -->

## v0.2.0 — On the internet, for free <!-- ms: phase=next -->

- [x] **CG-15 — Serverless version on GitHub Pages**: an hourly Action scores today and
  tomorrow for every crag with the Java scorer (`ConditionsExport`, one Open-Meteo request
  for all crags) and publishes a static page — Leaflet map, traffic lights, reasons, sources.
  Free, no VM. <!-- cg: prio=high size=M labels=frontend,infra ver=main -->
- [ ] **CG-16 — Wall aspects on OpenStreetMap**: 74 of 76 crags have no
  `climbing:orientation`; add it on OSM from public guides, crag by crag, then rerun
  `tools/osm_crags.py`. Every aspect added makes its verdict sharper. <!-- cg: prio=med size=M labels=data -->
- [ ] **CG-17 — Refresh the crag list**: a monthly Action reruns `tools/osm_crags.py` and
  opens a PR when OSM has changed. <!-- cg: prio=low size=S labels=data,infra -->

- [ ] **CG-6 — The VM as code**: Terraform for the Oracle Cloud Always Free A1.Flex VM in
  eu-milan-1 with its VCN, public subnet, internet gateway, route and security rules; a
  retry loop for "Out of host capacity". <!-- cg: prio=high size=M labels=infra -->
- [ ] **CG-7 — k3s and the first deploy**: install k3s on the VM, open 80/443 in the OCI
  security list and in the VM's iptables, create the database secret, `kubectl apply -k
  deploy/k8s`. <!-- cg: prio=high size=M labels=infra -->
- [ ] **CG-8 — Domain and HTTPS**: a domain or subdomain, cert-manager with Let's Encrypt,
  the host in the ingress. <!-- cg: prio=med size=S labels=infra -->
- [x] **CG-9 — The arm64 image**: `image.yml` builds the backend for linux/arm64 on a native
  ARM runner and pushes it to GHCR on every push to main. <!-- cg: prio=med size=S labels=infra ver=main -->
- [ ] **CG-13 — Continuous delivery**: after the image, roll the k3s deployment on the VM
  (a deploy key or a pull-based tool), once CG-7 has put the cluster up. <!-- cg: prio=med size=M labels=infra -->

## v0.2.1 — UX and UI <!-- ms: phase=now -->

What the live page needs so a climber finds a crag and trusts the answer in seconds.

- [x] **CG-18 — Search by name**: a search box that filters the list and the map as you
  type, ignoring case and accents ("corna", "Còrna" and "CORNA" find the same crags). <!-- cg: prio=high size=S labels=frontend ver=main -->
- [x] **CG-19 — Filter by verdict**: the legend becomes three toggles (Si va / Forse / No)
  that show or hide those crags on the map and in the list. <!-- cg: prio=high size=S labels=frontend ver=main -->
- [x] **CG-20 — Shareable state**: search, verdict filter and day live in the URL, so a link
  opens the same view and the browser's back button works. <!-- cg: prio=med size=S labels=frontend ver=main -->
- [ ] **CG-21 — Not by colour alone**: the verdict also as a letter or shape on the marker,
  for colour-blind readers and for print; contrast checked on the dark theme. <!-- cg: prio=med size=S labels=frontend -->
- [ ] **CG-22 — Near me**: sort by distance from the browser's location, computed in the page
  and never sent anywhere; the list says how far each crag is. <!-- cg: prio=med size=M labels=frontend -->
- [ ] **CG-23 — Crowded areas**: cluster the markers where crags sit close together (Lecco,
  Val Masino) and spread them as you zoom in. <!-- cg: prio=low size=S labels=frontend -->
- [ ] **CG-24 — Crag detail**: a panel per crag with the day's hours of rain and temperature,
  the OSM link and a directions link. <!-- cg: prio=med size=M labels=frontend -->
- [ ] **CG-26 — Duplicate crags**: OSM has some crags twice under one name (two "Falesia Corna
  De Par"): merge entries with the same name less than ~500 m apart in `tools/osm_crags.py`,
  keeping every OSM link. <!-- cg: prio=med size=S labels=data -->
- [ ] **CG-25 — Area of each crag**: the province or valley next to the name and as a filter,
  from OpenStreetMap's administrative boundaries in `tools/osm_crags.py`. <!-- cg: prio=low size=M labels=data,frontend -->

## v0.3.0 — The page climbers use <!-- ms: phase=later -->

- [ ] **CG-10 — Web page in React**: rewrite the static page (CG-15) in React with a build
  step — filters by area and verdict, a page per crag. <!-- cg: prio=med size=L labels=frontend -->
- [ ] **CG-11 — Avalanche bulletin**: for winter routes, the AINEVA / regional bulletin next
  to the weather. <!-- cg: prio=low size=M labels=data,backend -->
- [ ] **CG-12 — Write-up**: a post on allan-nava.github.io and on LinkedIn on how the verdict
  is computed and how it runs on Oracle's free tier. <!-- cg: prio=low size=S labels=docs -->
