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

- [x] **CG-47 — Coverage in CI**: JaCoCo for the backend (with the Testcontainers tests),
  Vitest's v8 coverage for the page, coverage.py for the tools; each job writes its numbers
  to the run summary and fails below a floor that only goes up. Done: floors set from CI's
  figures on 2026-10-15 — backend 78% of lines, page 88% (statements 82, functions 81,
  branches 80), tools 45%. <!-- cg: prio=high size=S labels=infra ver=main -->
- [x] **CG-48 — Every tool tested**: unit tests for crags_diff, history_append, aspects_todo
  and avalanche_regions, and for osm_crags' network paths (retries, empty and truncated
  answers, elevation batches) against a local fake server. Done: 35 tests, tools at 97% of
  lines (floor raised from 45); they found a good url tag hidden by a bad website tag, and
  error answers and files left open. <!-- cg: prio=high size=M labels=data ver=main -->
- [x] **CG-49 — The pipeline as one**: an integration test that runs ConditionsExport's main
  against fake Open-Meteo and avalanche servers and checks every file it writes (conditions,
  API, share pages); Calibrate and CragImporter covered the same way. Done: ConditionsExportIT
  and CalibrateIT under failsafe, the importer against a mocked repository; backend 78% → 93.6%
  of lines in CI, floor 0.93. <!-- cg: prio=high size=M labels=backend ver=main -->
- [x] **CG-50 — The page's gaps**: tests for near me (granted, denied, unavailable), a failed
  data load, the controls, and the charts' pointer. Done: 74 tests, the page from 88% to 99% of
  lines (statements 96, functions 95, branches 89); dragging the map is left to the browser
  tests (CG-51), since jsdom cannot drive Leaflet's drag. <!-- cg: prio=med size=S labels=frontend ver=main -->
- [x] **CG-51 — End to end in a browser**: Playwright in CI on the built site with fixture
  data — search, filters, days, the panel and the sheet, a crag link and its share page,
  favourites, the area search after a real drag, a phone viewport, and an accessibility
  scan. Done: Playwright 1.63 with 22 tests on desktop and a Pixel 7; the axe scan found the
  day buttons' accessible name did not start with their visible text (WCAG 2.5.3), now fixed;
  the share page's redirect stays covered by the Java tests of its content. <!-- cg: prio=high size=L labels=frontend,infra ver=main -->
- [x] **CG-52 — Lint the plumbing**: actionlint on the workflows and shellcheck on the
  scripts, in CI. Done: actionlint 1.7.12 (checksum-verified) and shellcheck; they found a `cd`
  in retry-apply.sh that could fail silently, an unused variable in uptime.yml, and a
  JavaScript template in single quotes, silenced on its line. <!-- cg: prio=low size=S labels=infra ver=main -->

## v0.9.0 — UI fixes <!-- ms: phase=next -->

From the screenshots of v0.5.0–v0.8.0, asked for on 2026-10-15: on a phone the controls take
three rows and about 600 px come before the first crag; the map fills the screen and the list
starts below it; the cluster marks' split is set at 8 px; on a desktop the side panel covers
the filters and the list. Each fix comes with an end-to-end test (CG-51) at the width it fixes.

- [x] **CG-53 — Compact controls on a phone**: the day, the search and a "Filtri" button on one
  row, the verdict, province, near-me and favourite filters in a panel it opens, with a count
  of the filters on; one line for the count and the day's summary instead of two; a shorter
  header. Desktop keeps its row. Done below 600 px: the map now starts at about 280 px instead
  of about 600; e2e tests at Pixel 7 and desktop widths. <!-- cg: prio=high size=M labels=frontend ver=main -->
- [x] **CG-54 — Map or list on a phone**: a "Mappa | Lista" switch under the controls, the list
  first, the choice kept in the URL; choosing a crag in the list shows it on the map. Done below
  600 px: the first crag now on the first screen; the map is measured again whenever it comes
  back, so its tiles fill it. <!-- cg: prio=high size=M labels=frontend ver=main -->
- [x] **CG-56 — The panel beside, not over**: on a desktop the crag's panel takes the list's
  column instead of covering the filters and the list; the map keeps its place. Done: the
  dialog lives in the page's grid; the list steps aside while it is open; a short fade
  replaces the slide from the screen's edge. <!-- cg: prio=med size=M labels=frontend ver=main -->
- [x] **CG-55 — Readable cluster marks**: the count and the split legible at 100% zoom (text
  of 11 px or more), still written out, never told by colour alone. Done: 64×50 marks, the count
  at 17 px and the split at 11 px without its zero parts; the full split stays in the label read
  aloud; clusters gather within 50 px instead of 40. <!-- cg: prio=med size=S labels=frontend ver=main -->
- [x] **CG-57 — A long list**: with 220 crags, a "Torna su" button once the list is scrolled,
  and the summary kept in sight while scrolling. Done: a sticky summary; "Torna su" after
  600 px of the page or of the list's own column, smooth unless motion is reduced. It can sit
  over the last visible card's star for a moment. <!-- cg: prio=low size=S labels=frontend ver=main -->

## v0.10.0 — A better frontend <!-- ms: phase=next -->

Asked for on 2026-10-16, after v0.9.0. From its screenshots: a crag's card is about 180 px
tall on a phone, and 193 of 220 cards repeat the same unmapped-aspect sentence; a popup can
list fourteen "OSM n" links; the page is dark only; there is no order but verdict or distance
and no grade filter; and no test notices when a change moves the layout.

- [x] **CG-66 — Architecture document**: `docs/ARCHITECTURE.md`, how the parts fit — the hourly
  run and the page's modules as diagrams, the contracts, the workflows, the tests. Done on
  2026-10-16. <!-- cg: prio=med size=S labels=docs ver=main -->
- [x] **CG-65 — Architectural Decision Records**: the decisions behind cragcast written down
  in `docs/adr/`, one numbered record each, and a rule that a new decision brings its record.
  Done: ADR 0001–0014, written on 2026-10-16 from the backlog, the PRs and git. <!-- cg: prio=med size=S labels=docs ver=main -->
- [x] **CG-58 — Compact crag cards**: the unmapped-aspect sentence becomes a short "esposizione
  ?" tag (its meaning said once, in the help and the tag's label), "Dettagli" joins the card's
  first row, so a phone shows about twice as many crags per screen. Done: "Dettagli" joined the
  days' row instead ("›" when the list is under 460 px wide, by container query), the star moved
  out of the heading, numbers tied to their units; a card on a Pixel 7 from about 190 to under
  125 px. <!-- cg: prio=high size=M labels=frontend ver=main -->
- [x] **CG-63 — Visual snapshots**: Playwright screenshot comparisons of the main views (list,
  map, panel, sheet; desktop and phone) on fixture data, so a layout change is seen in review
  before it ships. Done: 7 views from CI, fixed data and clock, 25-pixel tolerance; on their first
  look they found the logo missing from the page's own build and a chart label cut at the
  edge, both fixed. <!-- cg: prio=high size=S labels=frontend,infra ver=main -->
- [x] **CG-59 — Tidy popup and panel**: the OSM links of a crag mapped many times folded into one
  line ("mappata 14 volte su OpenStreetMap"), the weather facts set as a small table, one
  button style across popup, panel and list. Done in three commits; the weather numbers now use
  the Italian comma, and the popup's button, at 2.1:1 on white, is now 5.4:1 — measured by an
  end-to-end test, since axe only flagged it for review. <!-- cg: prio=med size=S labels=frontend ver=main -->
- [x] **CG-61 — Order and grades**: order by verdict, name, distance or elevation, and a grade
  filter (e.g. "fino al 6a", "dal 7a") over the crags whose grades OSM has, saying how many
  have none. Done: `?g=le6a`/`?g=ge7a` and `?o=name|low|high` in the URL, distance in memory
  only (CG-22); a crag is kept only when its OSM grades settle the range, and the count says how
  many were left out for having none — 180 of 220 on 2026-10-16. <!-- cg: prio=med size=M labels=frontend ver=main -->
- [x] **CG-60 — A light theme**: the site's light palette when the system asks for it, both
  themes checked by the axe scan, the verdict colours re-validated on the light surface. Done:
  every colour a token, redefined under `prefers-color-scheme: light`; the verdicts' text and
  rings darker on white (green 5.5:1, amber 5.9:1, red 6.6:1), the marks' fills unchanged under
  their black letter (≥5.2:1); rain and temperature pass the dataviz validator on #ffffff; axe
  and the snapshots run in both themes. <!-- cg: prio=med size=M labels=frontend ver=main -->
- [x] **CG-62 — Design tokens**: spacing, type sizes, radii and focus rings as CSS variables, used
  everywhere, so the next change touches one place. Done in four commits: eleven spacing steps
  (odd pixels rounded, a 1–2 px shift), seven type sizes, five radii, one focus ring — before
  it, tabbing met three different rings; the marks and charts keep their fixed pixel sizes. <!-- cg: prio=low size=M labels=frontend ver=main -->
- [x] **CG-64 — Faster first paint**: the crag panel and its charts loaded on demand, Leaflet in
  its own chunk; the Lighthouse performance budget raised to what that reaches. Done, with a
  modest result: the panel (8.7 kB) is fetched on idle, React and Leaflet are chunks of their
  own (cached across deploys); local performance 0.89 → ~0.91, total blocking time 212 → ~180 ms,
  CI's median unchanged at 0.92. Tried and dropped: preconnecting to the tile server and
  content-visibility (no change), drawing the list in idle steps (worse: 0.85, 277 ms). Budget
  0.85 → 0.88. <!-- cg: prio=low size=S labels=frontend ver=main -->

## v0.7.0 — Beyond Lombardy <!-- ms: phase=later -->

Moved out of v0.6.0 on 2026-10-15, when that milestone closed: a larger job, since the
province, the avalanche bulletin (another per region, the SLF in the Ticino) and the API
assume Lombardy.

- [ ] **CG-43 — Neighbouring regions**: crags beyond Lombardy (Piedmont, Trentino, the Ticino)
  where Lombard climbers go — the province, avalanche and API fields must hold beyond one
  region. <!-- cg: prio=low size=L labels=data,backend -->

## v1.1.0 — On a Raspberry Pi <!-- ms: phase=now -->

A Raspberry Pi with Portainer became available on 2026-10-16, on a friend's home network: a
place for the Spring service and its database while the Oracle VM stays parked.

- [x] **CG-100 — The Pi pulls its stack**: `deploy/pi/compose.yml` as a Portainer Git stack
  (polling main, re-pulling `cragcast-backend:main` from GHCR), the backend on the LAN only,
  PostgreSQL on no port; no self-hosted runner, since the repository is public
  ([ADR 0015](docs/adr/0015-raspberry-pi-pulls-its-stack.md)). Done when the stack runs on the
  Pi and answers `/actuator/health`. Done on 2026-10-16: the stack runs on a 64-bit Pi with
  8 GB; the first redeploy kept the old image while the stack still pointed at the PR's branch,
  so the reference is `refs/heads/main`. <!-- cg: prio=med size=S labels=infra ver=main -->
- [x] **CG-101 — History in PostgreSQL**: `crag_history` (Flyway V4) filled by `HistorySync` from
  the `history` branch's CSVs — every month at startup, the last two each night after
  `history.yml`, each day replaced whole; the CSV stays the source
  ([ADR 0016](docs/adr/0016-history-copied-into-postgres.md)). Done when the Pi's table has the
  CSV's rows, day by day. Done on 2026-10-16: Flyway at V4, 75 rows for 10-15 and 220 for 10-16,
  the CSV's 295. <!-- cg: prio=med size=S labels=backend ver=main -->
- [ ] **CG-102 — The Pi over Tailscale**: Tailscale on the Pi's host, so its owner reaches it
  from anywhere — the backend on its tailnet address, SSH, and psql through an SSH tunnel to
  PostgreSQL, now published on the Pi's 127.0.0.1 only. The Pi advertises no routes and is no
  exit node: the friend's network stays out of the tailnet
  ([ADR 0017](docs/adr/0017-the-pi-over-tailscale.md)). Done when `/actuator/health` and psql
  answer from outside that network. <!-- cg: prio=low size=S labels=infra -->
- [x] **CG-110 — The page on the Pi**: the same page next to GitHub Pages, in an nginx image
  (`cragcast-web`, linux/arm64), its data from the Pi's backend: `SiteData` builds
  `data/conditions.json`, the API v1 and the share pages with `ConditionsExport.build()` from the
  forecasts `WeatherRefresh` already fetched — no new Open-Meteo requests
  ([ADR 0018](docs/adr/0018-the-pi-serves-the-page-from-its-backend.md)). Done when the Pi's page
  answers on port 8081 with today's data. Done on 2026-10-16: the stack's three containers up
  on the Pi, the page 200 on 8081 and `data/conditions.json` from the backend with the day's
  data. <!-- cg: prio=med size=M labels=frontend,backend,infra ver=main -->

- [x] **CG-116 — Releases**: releases by date (`vYYYY.MM.DD`), images tagged with them and the
  version inside, the GitHub Release with notes from the merged PRs, and the Pi pinned to one by
  `CRAGCAST_VERSION`, so a merge no longer restarts it ([ADR 0020](docs/adr/0020-calendar-releases-the-pi-follows.md)).
  Asked for on 2026-10-20, after a merge's restart of the Pi looked like a failure. <!-- cg: prio=med size=S labels=infra ver=main -->

## v1.2.0 — Everything in PostgreSQL <!-- ms: phase=next -->

Asked for on 2026-10-16: the Pi's PostgreSQL to hold all the climbing data OpenStreetMap has for
Lombardy and everything cragcast produces, not only the crag list and the evening summary.
OpenStreetMap stays the only outside source of places (ADR 0003): no guidebook or third-party
site, scraped or copied. Today the database is a copy rebuilt by a restart (ADR 0016); the
hourly archive below exists nowhere else, so from CG-108 on it needs a backup.

- [x] **CG-103 — OSM's climbing objects, whole**: `osm_crags.py` also writes
  `data/osm-climbing.json` — every `climbing=*` and `sport=climbing` object of the query it
  already runs (crags, sectors, single routes, route bottoms), with its type, id, position and
  validated climbing tags — committed through the same reviewed monthly PR. The backend imports
  it into `osm_object` at startup, as `CragImporter` does; tags as JSON text, a column for
  anything worth filtering on. ODbL, credited where shown. Done: 650 objects on 2026-10-20 (76
  crags, 182 cliffs, 71 points, 150 routes, 140 venues, 31 others) in Flyway V5's `osm_object`,
  replaced whole at startup; `--objects-only` writes the file without a new crags.json; the
  monthly refresh watches both files. <!-- cg: prio=med size=M labels=data,backend ver=main -->
- [x] **CG-104 — Sectors kept**: the sectors that `group()` folds into a crag, in a `sector`
  table linked to their crag, with their own position, aspect and grades — the crag's folded
  values unchanged. Done: `group()` lists them by OSM id in `crags.json`'s `sectors` (the same
  place drawn twice stays in `alsoMapped`); Flyway V6's `sector`, replaced whole at startup by
  `SectorImporter` from those ids and `osm-climbing.json`; on 2026-10-22, 76 sectors on 30
  crags, the 220 crags' values as before. <!-- cg: prio=med size=S labels=data,backend ver=main -->
- [x] **CG-105 — Routes**: OSM's single routes in a `route` table — name, grade (French, as a
  rank column), length, bolts — linked to the crag or sector they belong to by OSM relation or
  distance. Stored and served by the API, never listed as crags (the CG-42 rule holds). Done:
  the query also returns the relations' members; `link_routes()` links by relation, else to the
  nearest crag or sector within 400 m; Flyway V7's `route` with `grade_rank`, and
  `GET /api/crags/{slug}/routes`. On 2026-10-22, of 148 single routes 102 linked by relation, 14
  by distance, 32 unlinked (mountain routes far from any crag). <!-- cg: prio=med size=M labels=data,backend ver=main -->
- [x] **CG-117 — Sectors and routes on the page**: the crag's panel lists its sectors (CG-104)
  with aspect and grades, and its single routes (CG-105) as a table — grade, length, bolts, the
  sector — easiest first, each linked to OSM and folded away under its count; the same lists in
  the API v1 crag file. Read from `data/osm-climbing.json` by the export, so GitHub Pages and the
  Pi show them alike. Asked for on 2026-10-22: the sectors and routes were in the database but
  nowhere on the page. <!-- cg: prio=med size=M labels=frontend,backend ver=main -->
- [x] **CG-118 — Routes by level**: a crag's routes readable at a glance — the span of grades in
  the summary ("dal 3b al 7a+"), a button per level (the grade's number: 4 and under, 5, 6, 7, 8
  and over, ungraded) with its count and an indicative word, the table grouped by level, and a
  line on how the French scale reads (5a, 5b, 5c, 5c+, then 6a). Asked for on 2026-10-22: a plain
  list of 52 routes did not say which were within reach. <!-- cg: prio=med size=S labels=frontend ver=main -->
- [x] **CG-106 — The hourly forecasts, archived**: each `WeatherRefresh` also appends what it
  computed — per crag and day, the verdict, the reasons, the four weather numbers and the hours
  (JSON text) — to `crag_forecast`, keyed by crag, day and fetch time, so "what did cragcast say
  at 18:00 the day before" stays answerable. About 16 000 rows a day; a retention rule if the
  Pi's disk asks for one. Done: `ForecastArchive` listens to `ForecastsRefreshed`, scores the
  refresh's forecasts and inserts 660 rows an hour into Flyway V8's `crag_forecast`, never
  rewritten; the hours as parallel lists; a failure loses that hour only
  ([ADR 0021](docs/adr/0021-hourly-forecasts-archived.md)). <!-- cg: prio=med size=M labels=backend ver=main -->
- [x] **CG-107 — Avalanche bulletins, archived**: each day's bulletin per micro-region — levels,
  bands, problem types, the link — never its text (ARPA Lombardia states no licence, CG-11). Done:
  `AvalancheArchive` reads today's at startup and at 09:30, 13:30 and 18:30 into Flyway V9's
  `avalanche_bulletin`; the latest issue replaces the day, none keeps it
  ([ADR 0022](docs/adr/0022-avalanche-bulletins-kept.md)). <!-- cg: prio=low size=S labels=backend ver=main -->
- [x] **CG-108 — Observations copied**: `data/observations.csv` into an `observation` table,
  read from main like the history (CG-101); the CSV stays the source and changes only by a
  reviewed PR. Done: `ObservationSync` into Flyway V10's `observation`, at startup and at 04:40,
  replaced whole in one transaction; a read without the header or with a bad row keeps the
  table. <!-- cg: prio=low size=S labels=backend,data ver=main -->
- [ ] **CG-109 — Backup, now that it matters**: from CG-106 the database holds rows that exist
  nowhere else. A nightly `pg_dump` from a container in the Pi's stack, kept for 14 days on a
  USB disk on the Pi (not the SD card), written only where the disk's marker file is; a restore
  tried once and written down (`docs/pi-backup.md`). ADR 0016's "no backup needed" is superseded
  by [ADR 0019](docs/adr/0019-backup-on-a-usb-disk.md). Done when the first dump is on the Pi's
  disk. <!-- cg: prio=high size=S labels=infra -->

## v1.3.0 — Vie ferrate <!-- ms: phase=later -->

Proposed on 2026-10-16: "can I do that via ferrata tomorrow?" next to the crags — the same
readers, the same question, a different answer. OpenStreetMap stays the only source of places
(`highway=via_ferrata` ways, `route=via_ferrata` relations, `via_ferrata_scale` 0–6), first-hand
and ODbL like the crags. A via ferrata is not a crag: its risk is the storm on the cable and the
weather at the top, not wet rock on a wall, so it gets its own verdict, and a wrong "si va"
costs more. An ADR comes with CG-112.

- [x] **CG-111 — What OSM has**: count Lombardy's via ferratas on OSM and how many carry a name,
  `via_ferrata_scale` and a route relation — Overpass was down when this was written. Few or
  thinly tagged: the first step is asking climbers to map them first-hand, as for the crags'
  aspects (CG-16), not the import. Done ([docs/ferrate-survey.md](docs/ferrate-survey.md)): on
  2026-10-28, 74 ways and 5 relations, about 42 via ferratas — 35 named, 22 with a plain 0–6
  scale, 7 ways with `2+`, `3+` or `K2`. Enough to import with the difficulty shown only where
  mapped; relations nearly absent, so CG-112 groups the ways itself. <!-- cg: prio=med size=S labels=data ver=main -->
- [x] **CG-112 — The import**: `tools/osm_ferrate.py` into `data/ferrate.json`, through the same
  reviewed monthly PR as the crags: name, OSM links, start and highest point (from the track,
  elevation from the DEM), length, `via_ferrata_scale` validated or dropped. Never mixed into
  `data/crags.json`, whose rule against single routes holds. With the ADR. CG-111 found few
  relations: the ways are grouped by name and distance, with tests, and a generated list asks
  for the missing scales. Done ([ADR 0023](docs/adr/0023-via-ferratas-from-openstreetmap.md)):
  on 2026-10-28, 42 via ferratas in `data/ferrate.json`, 24 with a difficulty, 12 unnamed groups
  left out; `docs/ferrate-todo.md` lists what to map; the monthly refresh rebuilds both. <!-- cg: prio=med size=M labels=data ver=main -->
- [ ] **CG-113 — A verdict of its own**: `FerrataScorer`, its thresholds named constants as in
  `ConditionsScorer`: a thunderstorm forecast in the day (Open-Meteo's weather code, CAPE) is
  never "si va"; wind, cold, snow and ice read at the highest point; the day's light against
  the route. Conservative until observed days exist to calibrate it (CG-27). The avalanche
  bulletin still informs and never scores (CG-11). <!-- cg: prio=med size=M labels=backend -->
- [ ] **CG-114 — On the page**: "Falesie / Ferrate" to switch, the type said in words and by its
  own mark — never by colour alone, and the S/F/N marks keep meaning the verdict (CG-21); the
  difficulty shown as OSM gives it; a line saying conditions are checked on the spot. <!-- cg: prio=med size=M labels=frontend -->
- [ ] **CG-115 — In the API and on the Pi**: the via ferratas as additive fields or files in
  `/api/v1/` (CG-32), the README's API section with them; served by the Pi's backend from the
  forecasts it fetches (ADR 0018), and archived with the crags' once CG-106 is in. <!-- cg: prio=low size=S labels=backend -->

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
