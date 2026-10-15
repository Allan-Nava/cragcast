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
- [x] **CG-4 — Conditions endpoint**: `GET /api/crags/{slug}/conditions?date=` returns the
  verdict, its reasons and the weather behind it, from the stored forecast; tomorrow when no
  date is given, 404 for a day not kept. The calibration moved to CG-27. <!-- cg: prio=high size=M labels=backend ver=main -->
- [x] **CG-14 — Logo**: an SVG mark in the palette of allan-nava.github.io (green #10cf53 on
  #050505) — a crag with the sun behind it — at the top of the README, plus a 1280×640 social
  preview image for the repository. <!-- cg: prio=low size=S labels=docs ver=main -->
- [x] **CG-5 — Hourly refresh**: a scheduled job fetches the forecast for every crag once an
  hour and stores it, so the API never waits on Open-Meteo. Done: at startup and at :07 every
  hour, one request per 50 crags, today to the day after tomorrow in `crag_weather`; a failed
  run keeps the last good hour. The crags come from data/crags.json, imported at startup. <!-- cg: prio=med size=M labels=backend ver=main -->
- [ ] **CG-27 — Calibrate the thresholds on real days**: a handful of days with what the rock
  was actually like at a known crag (wet, damp, dry) — observed, not guessed — checked against
  the verdict; adjust the constants in `ConditionsScorer` with a test per corrected day. Needs
  the observations first. The replay tool is done (`Calibrate`, `data/observations.csv`, the
  Historical Forecast API); what remains is the observed days, then the adjustment. <!-- cg: prio=med size=M labels=backend,data -->

## v0.2.0 — On the internet, for free <!-- ms: phase=next -->

- [x] **CG-15 — Serverless version on GitHub Pages**: an hourly Action scores today and
  tomorrow for every crag with the Java scorer (`ConditionsExport`, one Open-Meteo request
  for all crags) and publishes a static page — Leaflet map, traffic lights, reasons, sources.
  Free, no VM. <!-- cg: prio=high size=M labels=frontend,infra ver=main -->
- [ ] **CG-16 — Wall aspects on OpenStreetMap**: 74 of 76 crags have no
  `climbing:orientation`; add it on OSM from first-hand knowledge — OSM does not accept data
  copied from guidebooks under copyright — crag by crag, then rerun `tools/osm_crags.py`.
  Every aspect added makes its verdict sharper. <!-- cg: prio=med size=M labels=data -->
- [x] **CG-17 — Refresh the crag list**: a monthly Action reruns `tools/osm_crags.py` and
  opens a PR when OSM has changed. Done: `crags-refresh.yml` on the 1st at 05:23 UTC, a summary
  of added, removed and changed crags from `tools/crags_diff.py`, one open refresh PR at a time. <!-- cg: prio=low size=S labels=data,infra ver=main -->

- [x] **CG-9 — The arm64 image**: `image.yml` builds the backend for linux/arm64 on a native
  ARM runner and pushes it to GHCR on every push to main. <!-- cg: prio=med size=S labels=infra ver=main -->

## v0.2.1 — UX and UI <!-- ms: phase=now -->

What the live page needs so a climber finds a crag and trusts the answer in seconds.

- [x] **CG-18 — Search by name**: a search box that filters the list and the map as you
  type, ignoring case and accents ("corna", "Còrna" and "CORNA" find the same crags). <!-- cg: prio=high size=S labels=frontend ver=main -->
- [x] **CG-19 — Filter by verdict**: the legend becomes three toggles (Si va / Forse / No)
  that show or hide those crags on the map and in the list. <!-- cg: prio=high size=S labels=frontend ver=main -->
- [x] **CG-20 — Shareable state**: search, verdict filter and day live in the URL, so a link
  opens the same view and the browser's back button works. <!-- cg: prio=med size=S labels=frontend ver=main -->
- [x] **CG-21 — Not by colour alone**: the verdict also as a letter or shape on the marker,
  for colour-blind readers and for print; contrast checked on the dark theme. Done: circle S,
  diamond F, square N on the map, in the list and in the legend; black letters at 9.8, 11.2 and
  5.2:1. <!-- cg: prio=med size=S labels=frontend ver=main -->
- [x] **CG-22 — Near me**: sort by distance from the browser's location, computed in the page
  and never sent anywhere; the list says how far each crag is. Done: a "Vicino a me" toggle,
  haversine km, the map centred on the reader; a second click goes back to the verdict order. <!-- cg: prio=med size=M labels=frontend ver=main -->
- [ ] **CG-23 — Crowded areas**: cluster the markers where crags sit close together (Lecco,
  Val Masino) and spread them as you zoom in. <!-- cg: prio=low size=S labels=frontend -->
- [ ] **CG-24 — Crag detail**: a panel per crag with the day's hours of rain and temperature,
  the OSM link and a directions link. <!-- cg: prio=med size=M labels=frontend -->
- [ ] **CG-26 — Duplicate crags**: OSM has some crags twice under one name (two "Falesia Corna
  De Par"): merge entries with the same name less than ~500 m apart in `tools/osm_crags.py`,
  keeping every OSM link. <!-- cg: prio=med size=S labels=data -->
- [ ] **CG-25 — Area of each crag**: the province or valley next to the name and as a filter,
  from OpenStreetMap's administrative boundaries in `tools/osm_crags.py`. <!-- cg: prio=low size=M labels=data,frontend -->

## v0.2.2 — Crag details <!-- ms: phase=now -->

What a climber reads before choosing a crag — grades, height, rock, style — from open data
only, shown where it exists and marked "not mapped" where it does not. Nothing typed from
memory or copied from guidebook sites.

- [x] **CG-28 — Crag details from OpenStreetMap**: read the climbing tags OSM already has —
  French grades (min, max, mean), route length, rock, style (sport, trad, multipitch, bolted),
  number of routes, website — in `tools/osm_crags.py` into `data/crags.json`, and show them on
  the page next to each crag. Done: validated (a grade must read like 6b+, a length be a
  number), 21 of 76 crags have at least one detail, 6 have grades. The Spring service serves
  them since CG-31. <!-- cg: prio=high size=M labels=data,frontend ver=main -->
- [x] **CG-31 — Crag details in the API**: store the details from data/crags.json in the
  database (a V3 migration) and return them from `GET /api/crags/{slug}`. Done, with the
  elevation; details stored as JSON text. <!-- cg: prio=low size=S labels=backend ver=main -->
- [x] **CG-29 — Elevation of every crag**: the altitude of each crag from a digital elevation
  model through Open-Meteo's elevation API, stored in `data/crags.json` with its source, and
  shown on the page. Done: Copernicus DEM GLO-90 (90 m) via Open-Meteo, computed once in
  `tools/osm_crags.py`, 234–2251 m across the 76 crags; the page says "about", and credits
  Copernicus. <!-- cg: prio=high size=S labels=data,frontend ver=main -->
- [x] **CG-30 — Help map your crag**: a short guide on the page and in the README, an "edit
  on OpenStreetMap" link per crag and a GitHub issue form, so climbers add aspect, grades and
  route length on OSM, where cragcast picks them up. Done, and the guide says plainly: only
  first-hand knowledge, nothing copied from guidebooks. <!-- cg: prio=med size=S labels=docs,frontend ver=main -->

## v0.3.0 — The page climbers use <!-- ms: phase=later -->

- [ ] **CG-10 — Web page in React**: rewrite the static page (CG-15) in React with a build
  step — filters by area and verdict, a page per crag. <!-- cg: prio=med size=L labels=frontend -->
- [ ] **CG-11 — Avalanche bulletin**: for winter routes, the AINEVA / regional bulletin next
  to the weather. <!-- cg: prio=low size=M labels=data,backend -->
- [ ] **CG-12 — Write-up**: a post on allan-nava.github.io and on LinkedIn on how the verdict
  is computed and how it runs on Oracle's free tier. <!-- cg: prio=low size=S labels=docs -->

## v0.4.0 — Free stack <!-- ms: phase=next -->

cragcast as a complete product on free services only: GitHub Pages for the page and a static
API, GitHub Actions for everything that runs, git for history. No VM, no card.

- [ ] **CG-32 — Static API on GitHub Pages**: `/api/v1/crags.json` and one file per crag, with
  the verdicts, the weather behind them and the crag's details, rebuilt every hour and
  documented in the README — a backend anyone can call, with no server. <!-- cg: prio=high size=M labels=backend,infra -->
- [ ] **CG-33 — Three days**: today, tomorrow and the day after; the forecast already covers
  them. <!-- cg: prio=med size=S labels=backend,frontend -->
- [ ] **CG-34 — Verdict history**: every evening, each crag's verdict and weather for the day
  saved as CSV on a `history` branch, never on main — for "how was last week" and as data for
  CG-27. <!-- cg: prio=med size=M labels=data,infra -->
- [ ] **CG-35 — Installable page (PWA)**: a manifest and a service worker, so the page can be
  added to a phone's home screen and shows the last data offline. <!-- cg: prio=med size=M labels=frontend -->
- [ ] **CG-36 — Lighthouse CI on pull requests**: performance and accessibility budgets that
  fail the PR when they drop. <!-- cg: prio=low size=S labels=infra,frontend -->
- [ ] **CG-37 — Uptime with checkfleet**: an hourly check of the page and of the data's age,
  with Allan's own checkfleet, that fails loudly when the JSON is stale. <!-- cg: prio=low size=S labels=infra -->

## v1.0.0 — Self-hosted on a VM (parked) <!-- ms: phase=later -->

Parked on 2026-10-15: Oracle Cloud's Always Free A1 VM could not be created in Milan (39 tries
over two days, "Out of host capacity"), and the account stays on the free tier. The Terraform
network was destroyed; `infra/oci/` and `deploy/k8s/` stay in the repository, ready to apply if
capacity appears or the project moves to another host. Meanwhile cragcast runs on GitHub Pages.

- [ ] **CG-6 — The VM as code**: Terraform for the Oracle Cloud Always Free A1.Flex VM in
  eu-milan-1 with its VCN, public subnet, internet gateway, route and security rules; a
  retry loop for "Out of host capacity". <!-- cg: prio=high size=M labels=infra -->
- [ ] **CG-7 — k3s and the first deploy**: install k3s on the VM, open 80/443 in the OCI
  security list and in the VM's iptables, create the database secret, `kubectl apply -k
  deploy/k8s`. <!-- cg: prio=high size=M labels=infra -->
- [ ] **CG-8 — Domain and HTTPS**: a domain or subdomain, cert-manager with Let's Encrypt,
  the host in the ingress. <!-- cg: prio=med size=S labels=infra -->
- [ ] **CG-13 — Continuous delivery**: after the image, roll the k3s deployment on the VM
  (a deploy key or a pull-based tool), once CG-7 has put the cluster up. <!-- cg: prio=med size=M labels=infra -->
