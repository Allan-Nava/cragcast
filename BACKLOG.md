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
  Every aspect added makes its verdict sharper. The checklist with an edit link per crag is
  `docs/aspects-todo.md`. <!-- cg: prio=med size=M labels=data -->
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
- [x] **CG-23 — Crowded areas**: cluster the markers where crags sit close together (Lecco,
  Val Masino) and spread them as you zoom in. Done with Leaflet.markercluster 1.5.3 (cdnjs, SRI):
  a neutral mark with the count and the split written out; no clustering from zoom 12. <!-- cg: prio=low size=S labels=frontend ver=main -->
- [x] **CG-24 — Crag detail**: a panel per crag with the day's hours of rain and temperature,
  the OSM link and a directions link. Done: a dialog with temperature and rain as two charts
  (06–21), a readout on pointer and arrow keys, the hours as a table, directions via Google Maps
  and OSM; the export carries the hours. <!-- cg: prio=med size=M labels=frontend ver=main -->
- [x] **CG-26 — Duplicate crags**: OSM has some crags twice under one name (two "Falesia Corna
  De Par"): merge entries with the same name in `tools/osm_crags.py`, keeping every OSM link.
  Done at under 50 m, not ~500: the only other same-name pair is two "Settore Destro" 134 m
  apart, a generic name that may be two crags — left apart (see CG-25). 76 → 75 crags. <!-- cg: prio=med size=S labels=data ver=main -->
- [x] **CG-25 — Area of each crag**: the province or valley next to the name and as a filter,
  from OpenStreetMap's administrative boundaries in `tools/osm_crags.py`. Also tells apart sectors
  with a generic name (two "Settore Destro"). Done: municipality and province for all 75 crags
  (6 provinces), shown as "Canzo (CO)", searchable, and a province filter in the URL (`p=`).
  Valleys are not reliably mapped on OSM, so the area is administrative. <!-- cg: prio=low size=M labels=data,frontend ver=main -->

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

- [x] **CG-10 — Web page in React**: rewrite the static page (CG-15) in React with a build
  step — filters by area and verdict, a page per crag. Done: React 19, TypeScript and Vite in
  `web/`, same features; the page per crag is the panel of CG-24. <!-- cg: prio=med size=L labels=frontend ver=main -->
- [x] **CG-11 — Avalanche bulletin**: for winter routes, the AINEVA / regional bulletin next
  to the weather. Done: ARPA Lombardia's bulletin through the EAWS (CAAML v6 JSON), placed by
  the EAWS micro-regions; level, bands and problems in the popup and the panel, 44 of 75 crags
  covered; the verdict is unchanged. <!-- cg: prio=low size=M labels=data,backend ver=main -->
- [x] **CG-12 — Write-up**: a post on allan-nava.github.io and on LinkedIn on how the verdict
  is computed and how it runs on Oracle's free tier. Done: the post (Italian) at
  allan-nava.github.io/blog/2026/10/15/allan-nava-cragcast.html — the verdict's thresholds, the
  Oracle VM that never got capacity, and how it runs hourly on GitHub Pages instead; the
  LinkedIn text lives in the cv repo (`linkedin/post-cragcast.txt`). <!-- cg: prio=low size=S labels=docs ver=main -->

## v0.4.0 — Free stack <!-- ms: phase=next -->

cragcast as a complete product on free services only: GitHub Pages for the page and a static
API, GitHub Actions for everything that runs, git for history. No VM, no card.

- [x] **CG-32 — Static API on GitHub Pages**: `/api/v1/crags.json` and one file per crag, with
  the verdicts, the weather behind them and the crag's details, rebuilt every hour and
  documented in the README — a backend anyone can call, with no server. Done: `/api/v1/crags.json`
  (index with verdicts by day) and `/api/v1/crags/<id>.json` (75 files), about 320 KB, rebuilt by
  `pages.yml`; fields only ever added. <!-- cg: prio=high size=M labels=backend,infra ver=main -->
- [x] **CG-33 — Three days**: today, tomorrow and the day after; the forecast already covers
  them. Done: the export, the page ("Dopodomani") and the API's `days` carry three days. <!-- cg: prio=med size=S labels=backend,frontend ver=main -->
- [x] **CG-34 — Verdict history**: every evening, each crag's verdict and weather for the day
  saved as CSV on a `history` branch, never on main — for "how was last week" and as data for
  CG-27. Done: `history.yml` at 20:30 UTC, `tools/history_append.py`, one CSV per month,
  idempotent per day. <!-- cg: prio=med size=M labels=data,infra ver=main -->
- [x] **CG-35 — Installable page (PWA)**: a manifest and a service worker, so the page can be
  added to a phone's home screen and shows the last data offline. Done: manifest with any and
  maskable icons from the logo, network-first service worker, a notice when the data is over
  3 h old; map tiles never cached. <!-- cg: prio=med size=M labels=frontend ver=main -->
- [x] **CG-36 — Lighthouse CI on pull requests**: performance and accessibility budgets that
  fail the PR when they drop. Done: measured 0.95 / 1 / 1 / 1, budgets 0.85 / 0.95 / 0.95 /
  0.95 on the median of three desktop runs; `tools/build_site.sh` shared with pages.yml. <!-- cg: prio=low size=S labels=infra,frontend ver=main -->
- [x] **CG-37 — Uptime with checkfleet**: an hourly check of the page and of the data's age,
  with Allan's own checkfleet, that fails loudly when the JSON is stale. Done: checkfleet 1.34.2
  (http, certs) on the page, data, API and TLS, plus a freshness step on `generatedAt` (150
  min); checkfleet has no JSON-field age check, so the step lives in the workflow. <!-- cg: prio=low size=S labels=infra ver=main -->

## v0.5.0 — A livelier page <!-- ms: phase=next -->

The page works but feels static: map and list live apart, comparing days takes clicks, the
panel is a modal, and nothing moves on its own. Asked for on 2026-10-15.

- [x] **CG-38 — Map and list together**: hovering or focusing a crag in the list highlights
  its mark on the map; choosing one flies the map to it; a "Cerca in quest'area" button limits
  the list to what the map shows. Done: the mark or its cluster is ringed (pulsing unless
  reduced motion), the map flies to zoom 13 and opens the popup, the crag opened is outlined in
  the list, and the area lives in the URL as `a=s,w,n,e`. <!-- cg: prio=high size=M labels=frontend ver=main -->
- [x] **CG-39 — Three days at a glance**: each crag in the list shows the three verdicts
  (today, tomorrow, the day after) with a weather symbol, and a summary above the list counts
  the day's verdicts ("Domani: 60 si va, 10 forse, 5 no"). Never by colour alone. Done: the
  weather symbol is the max temperature and, from 0.5 mm, a rain drop with the mm — no sun,
  since the data has no cloud cover; each day is a button that switches the page. <!-- cg: prio=high size=M labels=frontend ver=main -->
- [x] **CG-40 — Panel as a sheet**: the crag's panel becomes a side panel on desktop and a
  bottom sheet on phones, with the day switchable inside it (buttons and swipe) and gentle
  transitions that respect `prefers-reduced-motion`. Done: non-modal beside the map from
  900 px, modal sheet below with a downward swipe to close; the day buttons carry the
  verdicts and switch the page's day. <!-- cg: prio=med size=L labels=frontend ver=main -->
- [x] **CG-41 — A live page**: a skeleton while the data loads, "aggiornato 12 min fa" that
  keeps itself current, the data reloaded every hour without a page reload, and short
  transitions when filters change — all off under `prefers-reduced-motion`. Done: the page
  looks for new data every 10 minutes and on return to the tab, swapping it only when
  `generatedAt` changed; crags entering the list rise in. <!-- cg: prio=med size=M labels=frontend ver=main -->

## v0.6.0 — More crags, one link each <!-- ms: phase=next -->

Many Lombard crags are on OpenStreetMap without `climbing=crag` — Galbiate, Corna Rossa di
Valmadrera, San Tomas — so cragcast never sees them; and a crag cannot be shared as a link.
Asked for on 2026-10-15.

- [x] **CG-42 — Crags tagged only sport=climbing**: import the named outdoor climbing places
  that OSM tags `sport=climbing` (a cliff, a rock, a `single_rock` relation) without
  `climbing=crag`; leave out gyms, shops, buildings and single routes; fold the sectors mapped
  as loose points into the crag around them. Every import keeps its OSM link. Done: 75 → 220
  crags (Maddalena, Predore, Cimbergo, Galbiate…), the 75 keep their ids, names and data;
  sectors fold by base name or distance without lending their route count or aspect; a
  truncated Overpass answer for the municipalities is now asked again, never written. <!-- cg: prio=high size=M labels=data ver=main -->
- [x] **CG-44 — A link per crag**: `?crag=<id>` opens the page on that crag's panel, a "Copia
  link" in the panel, and the browser's share sheet where there is one. Done: the open panel
  is part of the URL state, so back closes it; the shared link carries no day, so it opens on
  the reader's tomorrow. <!-- cg: prio=high size=S labels=frontend ver=main -->
- [x] **CG-45 — A preview when shared**: a static page per crag, rebuilt hourly, whose Open
  Graph tags carry the name, tomorrow's verdict and weather, so WhatsApp and Telegram show
  them; it sends a browser on to the crag's panel. Done: `site/c/<id>.html` from
  `SharePages`, the day as a date, the project's social image; "Condividi" shares it; the
  main page gained its own Open Graph tags. <!-- cg: prio=med size=M labels=frontend,backend ver=main -->
- [x] **CG-46 — Favourite crags**: a star on each crag, kept in the browser only, and a
  "Preferite" filter. Done: in the list and the panel; the filter is remembered too; storage
  failures leave favourites for the visit. <!-- cg: prio=med size=S labels=frontend ver=main -->

## v0.8.0 — Tested end to end <!-- ms: phase=now -->

Asked for on 2026-10-15: unit and integration tests for everything. Baseline that day: backend
64% of lines without Docker (the Testcontainers tests run in CI only), page 88%, osm_crags.py
67%, the other four tools 0%; no test of the whole pipeline, none in a real browser.

- [ ] **CG-47 — Coverage in CI**: JaCoCo for the backend (with the Testcontainers tests),
  Vitest's v8 coverage for the page, coverage.py for the tools; each job writes its numbers
  to the run summary and fails below a floor that only goes up. <!-- cg: prio=high size=S labels=infra -->
- [ ] **CG-48 — Every tool tested**: unit tests for crags_diff, history_append, aspects_todo
  and avalanche_regions, and for osm_crags' network paths (retries, empty and truncated
  answers, elevation batches) against a local fake server. <!-- cg: prio=high size=M labels=data -->
- [ ] **CG-49 — The pipeline as one**: an integration test that runs ConditionsExport's main
  against fake Open-Meteo and avalanche servers and checks every file it writes (conditions,
  API, share pages); Calibrate and CragImporter covered the same way. <!-- cg: prio=high size=M labels=backend -->
- [ ] **CG-50 — The page's gaps**: tests for near me (granted, denied, unavailable), a failed
  data load, the controls, and the charts' pointer. <!-- cg: prio=med size=S labels=frontend -->
- [ ] **CG-51 — End to end in a browser**: Playwright in CI on the built site with fixture
  data — search, filters, days, the panel and the sheet, a crag link and its share page,
  favourites, the area search after a real drag, a phone viewport, and an accessibility
  scan. <!-- cg: prio=high size=L labels=frontend,infra -->
- [ ] **CG-52 — Lint the plumbing**: actionlint on the workflows and shellcheck on the
  scripts, in CI. <!-- cg: prio=low size=S labels=infra -->

## v0.7.0 — Beyond Lombardy <!-- ms: phase=later -->

Moved out of v0.6.0 on 2026-10-15, when that milestone closed: a larger job, since the
province, the avalanche bulletin (another per region, the SLF in the Ticino) and the API
assume Lombardy.

- [ ] **CG-43 — Neighbouring regions**: crags beyond Lombardy (Piedmont, Trentino, the Ticino)
  where Lombard climbers go — the province, avalanche and API fields must hold beyond one
  region. <!-- cg: prio=low size=L labels=data,backend -->

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
