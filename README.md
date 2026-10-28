<p align="center"><img src="assets/logo.svg" width="96" height="96" alt="cragcast logo"></p>

# cragcast

**Can I climb there tomorrow?** cragcast answers it for crags in Lombardy: for each crag,
tomorrow's weather, the rain of the last two days and the aspect of the wall become a
verdict — **go**, **maybe**, **no go** — with the reasons.

Wet rock is the main thing that ruins a climbing day, and whether a wall has dried depends on
how much it rained and how much sun the wall gets. A south-facing slab after 8 mm of rain is
usually fine the next day; a north-facing one often is not. cragcast puts that judgement in
code, with the thresholds in one place so they can be argued with.

**Live:** [allan-nava.github.io/cragcast](https://allan-nava.github.io/cragcast/) — 220 crags,
updated every hour by a GitHub Action, no server.

**How it works, in Italian:** [cragcast: si arrampica domani?](https://allan-nava.github.io/blog/2026/10/15/allan-nava-cragcast.html)
— the verdict's rules, the Oracle VM that never came, and the free stack it runs on instead.

**Status:** the page is live on GitHub Pages and is the way cragcast runs. The Spring service is
complete for one verdict per crag and day but not hosted: the Oracle Always Free VM could not be
created (no A1 capacity in Milan), so that path is parked. What comes next is in the
[roadmap](ROADMAP.md).

## The page

- **dark or light**, as the system asks (CG-60): the same contrast rules in both, the verdict
  marks unchanged, the text and focus rings a darker green, amber and red on white;
- a map and a list of the crags with the verdict for **today**, **tomorrow** or **the day
  after** — never by
  colour alone: go is a green circle S, maybe a yellow diamond F, no a red square N — and the
  reasons under each crag (rain, recent rain, wind, cold or heat, an unmapped aspect);
- on a phone the controls take two rows — the days, then the search and a **"Filtri"** button
  that holds the verdict, province, grade, order, near-me and favourite filters and counts those on
  ("Filtri (2)"); the day's summary and the number of crags shown share one line;
- on a phone, **"Lista | Mappa"** shows one at a time, the list first, the choice kept in the
  URL (`show=map`); a crag chosen in the list opens on the map;
- the day's summary stays at the top while the crags scroll, and **"Torna su"** shows once
  they have been scrolled (the page on a phone, the list's column on a desktop);
- **compact crag cards**: the name and verdict, then the place, the aspect — an
  "esposizione ?" tag where it is not mapped — the reasons and the details on one line, then
  the three days and "Dettagli" ("›" in a narrow list); about twice as many per screen;
- **search** by name or municipality, ignoring case and accents, **toggles** for go / maybe /
  no go, and a **province** filter; each crag shows where it is ("Canzo (CO)");
- **grades and order** (CG-61): a grade filter — "fino al 5c" to "fino al 6c", "dal 7a" to
  "dall'8a" — over the grades OSM gives, keeping a crag only when they settle it, and the count
  saying how many were left out for having none ("· 180 senza gradi su OpenStreetMap,
  escluse"); the list ordered by verdict, name, or elevation from the lowest or the highest,
  and by distance once "Vicino a me" has the position (distance stays out of the URL, like the
  position);
- crowded areas (Lecco, Val Masino) gather into one mark with the count and the split by
  verdict written out at a readable size, the parts at zero left out (e.g. `20 · 7S·8F·5N`, `9 · 9S`); zooming in or picking a crag in the list opens it;
- **favourites**: a star on each crag, in the list and the panel, and a "Preferite" filter;
  kept in this browser only — never in the URL, never sent — and the page works when the
  browser refuses to store them (they then last for the visit);
- **near me**: the list sorted by distance from the reader, with the km, the map centred on
  them — the position is used in the browser only, never sent and never put in the URL;
- **three days at a glance**: each crag in the list shows today, tomorrow and the day after
  side by side — verdict mark, max temperature, and a drop with the mm when it rains (the data
  has no cloud cover, so no sun is drawn) — and each switches the page to that day; above the
  list, the day's verdicts counted ("Domani, venerdì 16 ottobre: 19 si va · 0 forse · 0 no")
  over the crags matching the search, province and area, whatever the verdict toggles;
- **map and list together**: pointing at a crag in the list (mouse or keyboard) lights its
  mark, or the group it is in; choosing one flies the map to it and opens it; a crag opened on
  the map is outlined in the list; after moving the map, **"Cerca in quest'area"** limits the
  list to what the map shows, until it is cleared;
- the view in the URL (`?q=&v=&p=&g=&o=&a=&crag=&show=&day=`): a link opens the same view, and back works;
- **a link per crag**: `?crag=<id>` opens the page on that crag's panel with the map on it;
  opening a panel puts it in the URL and back closes it; "Condividi" in the panel uses the
  phone's share sheet, or copies the link where there is none; the link shared is the crag's
  own page (`c/<id>.html`, rebuilt hourly), so WhatsApp or Telegram show its name and the
  verdict and weather for the next day, with the date ("Falesie di Galbiate — venerdì 16
  ottobre: si va"), and a browser goes straight on to the panel;
- per crag, where OpenStreetMap has them: grades, route length, rock, style, website; the
  **elevation** of every crag; a link to the crag on OSM and to its OSM edit view; a crag folded
  from several OSM objects says how many ("mappata 14 volte"), and its panel lists them;
- the day's **weather** behind the verdict as a small table — rain in the 48 hours before, rain
  during the day, the maximum, the wind — in the popup and the panel;
- a **panel per crag** ("Dettagli") — from 900 px in the list's column, below the controls, with
  the map still usable and the list back when it closes, and a
  sheet from the bottom on phones; the day switches inside it with buttons or a sideways swipe,
  and a downward swipe closes the sheet: the day's hours from 06:00 to 21:00 as two small charts —
  temperature and rain, never one chart with two axes — a readout that follows the pointer or
  the arrow keys, the same hours as a table, and directions (Google Maps, OSM);
- in the avalanche season, the **avalanche bulletin** of ARPA Lombardia for the crag's zone,
  in the popup and the panel: the danger level (1–5) by elevation band and part of the day,
  the avalanche problems, and a link to the official bulletin. Only the crags inside one of
  the bulletin's micro-regions get it — 119 of 220; the low crags lie outside them. The verdict
  does not change: it is about the rock, the bulletin about the approach and winter routes;
- **installable** on a phone's home screen (a web app manifest and a service worker): it shows
  the last data offline and says how old it is when it is more than 3 hours old;
- **live**: the footer says how long ago the data was made ("aggiornato 12 min fa") and keeps
  it current; an open page fetches new data every 10 minutes and when the reader returns to
  it, without a reload — the view, the open panel and the position stay; a skeleton holds the
  list's place while the first data loads;
- updated every hour by `.github/workflows/pages.yml`, at about :17;
- built with React, TypeScript and Vite from `web/`, Leaflet and its cluster plugin bundled
  from npm — no third-party script at runtime.

## History

Every evening the verdict of the day and the weather behind it are saved for every crag, one
CSV per month, on the [`history`](https://github.com/Allan-Nava/cragcast/tree/history) branch:
`date, crag_id, crag_name, province, verdict, rain_last48h_mm, rain_day_mm, max_temp_c,
max_wind_kmh, reasons, generated_at`. It answers "how was last week at this crag" and is the
data the calibration (CG-27) needs next to the observations. The Spring service copies it into
PostgreSQL (`crag_history`, CG-101): every month at startup, the last two each night; the CSV
stays the source ([ADR 0016](docs/adr/0016-history-copied-into-postgres.md)).

The Spring service also keeps every hour's answer (CG-106): each refresh appends, per crag and
day, the verdict, the reasons, the four numbers and the day's hours to `crag_forecast`, keyed by
the time it ran — so "what did cragcast say at 18:00 the day before" stays answerable. Those
rows exist only on the Pi; the nightly backup is their copy
([ADR 0021](docs/adr/0021-hourly-forecasts-archived.md)).

## API

The same data as the page, as static JSON on GitHub Pages — rebuilt every hour, free to call,
no key, no server. Version 1: fields are only ever **added**; a field is never renamed or
removed (a breaking change would go to `/api/v2/`).

```bash
curl https://allan-nava.github.io/cragcast/api/v1/crags.json
curl https://allan-nava.github.io/cragcast/api/v1/crags/osm-node-1739484761.json
```

- **`crags.json`** — `meta` (version, `generatedAt`, timezone, the `days` covered, the
  credits for crags, weather, elevation and avalanche data) and `crags`: for each one `id`, `name`,
  `municipality`, `provinceCode`, `latitude`, `longitude`, `aspect` (null when not mapped),
  `elevation`, `verdicts` by day (`GO`, `MAYBE`, `NO_GO`) and `url`, the path of its own file.
- **`crags/<id>.json`** — `meta` and the whole `crag`: the fields above plus `sourceUrl`,
  `alsoMapped`, `details` (grades, route length, rock, style, website — where OSM has them),
  `province` and `days`: per day the `verdict`, the `reasons`, the `weather` it was computed
  from (rain in the previous 48 h, rain, max temperature and max wind from 08:00 to 18:00) and
  the `hours` from 06:00 to 21:00, and `avalanche` only when a bulletin covers the crag that
  day: `region` and `name` of the EAWS micro-region, `level` (the day's highest, 1–5),
  `ratings` (`level`, `above`, `below` — metres or `treeline` — and `period`, `earlier`,
  `later` or null for all day), `problems` (CAAML v6 types such as `wind_slab`) and `bulletin`,
  the official page. Since CG-117 also `sectors` — `id`, `name`, `aspect`, `gradeMin`,
  `gradeMax` of each sector folded into the crag (CG-104) — and `routes` — `id`, `name`,
  `grade`, `length`, `bolts`, `pitches` and `sector` (a sector's `id` or null) of each single
  route OSM links to it (CG-105), easiest first; both empty lists where OSM has none.

Credit OpenStreetMap (ODbL), Open-Meteo (CC BY 4.0) and Copernicus when you show the data,
and ARPA Lombardia for the avalanche levels.

## How it is built

How the parts fit, with diagrams: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). Why it is built this way: the
[Architectural Decision Records](docs/adr/README.md) in `docs/adr/`.

| Part | What |
|---|---|
| `backend/` | Java 21, Spring Boot 4, PostgreSQL, Flyway. `GET /api/crags` and `GET /api/crags/{slug}` (position, aspect, elevation, OSM details), `GET /api/crags/{slug}/conditions?date=`, `GET /api/crags/{slug}/routes` (CG-105: the crag's single routes from OSM, easiest first — name, French grade, length, bolts, pitches, sector, OSM link); imports the crags from `data/crags.json` at startup and refreshes the weather every hour, appending each hour's verdicts to `crag_forecast` (CG-106); CG-113: `ferrata/` holds the via ferratas' own verdict, read at the route's top and stricter than a crag's — a thunderstorm is never "si va" ([ADR 0024](docs/adr/0024-a-via-ferrata-verdict-of-its-own.md)), not yet on the page |
| `backend/.../conditions/ConditionsScorer.java` | the verdict: weather + aspect → go / maybe / no go, with reasons |
| `backend/.../weather/` | Open-Meteo client and the reduction of an hourly forecast to one day: rain in the previous 48 h, and rain, max temperature and max wind from 08:00 to 18:00 |
| `docker-compose.yml` | PostgreSQL (and the backend) for local development |
| `deploy/pi/compose.yml` | the Spring service, PostgreSQL and (CG-110) the page on a Raspberry Pi, as a Portainer Git stack that polls main and re-pulls the arm64 image from GHCR; the home network and the owner's tailnet, PostgreSQL on the Pi's localhost only, no self-hosted runner ([ADR 0015](docs/adr/0015-raspberry-pi-pulls-its-stack.md), [0017](docs/adr/0017-the-pi-over-tailscale.md)) |
| `deploy/k8s/` | Kubernetes manifests for k3s on an Oracle Cloud Always Free Ampere VM; parked with `infra/oci/` |
| `infra/oci/` | Terraform for that VM and its network, with `retry-apply.sh` for "Out of host capacity"; parked — nothing is applied (the VM never got capacity, the network was destroyed) |
| `data/crags.json`, `tools/osm_crags.py` | the crags, from OpenStreetMap — `climbing=crag`, and named outdoor `sport=climbing` places (a cliff, a rock, a climbing site), never gyms, shops or single routes — with the sectors mapped separately folded into their crag and named by it (CG-42), and listed by OSM id in its `sectors` (CG-104); each with its OSM links, its aspect where mapped, its validated climbing details, its elevation (Copernicus DEM), its municipality and province (OSM administrative boundaries); a crag mapped twice under one name less than 50 m apart is folded into one, keeping both OSM links; ODbL. Rebuilt monthly by `crags-refresh.yml` through a reviewed PR, or by hand: `python3 tools/osm_crags.py`; the folding rules are tested in `tools/test_osm_crags.py` |
| `data/osm-climbing.json`, `osm_object` | CG-103: every object of the same OSM query, not only the crags — crags, cliffs, loose points, single routes, gyms and shops — with its category, name, position, raw `climbing` tag, validated climbing tags and a route's own grade; written by `osm_crags.py` (`--objects-only` for this file alone), refreshed with `crags.json` by the monthly PR, and copied at startup into PostgreSQL's `osm_object` table (replaced whole; the file stays the source); ODbL. CG-104: the sectors each crag lists are copied with them into `sector`, linked to the crag, each with its own position, aspect and grades. CG-105: each single route (`climbing=route`, `route_bottom`, `route_top`) carries the `crag` and `sector` it belongs to and `linkedBy` — `relation` when an OSM relation that is the crag, a sector or a duplicate of it lists the route, `distance` for the nearest crag or sector within 400 m, null when none is near (mountain routes) — plus its bolts and pitches; copied into `route` with the grade as a rank, never into the crag list |
| `backend/.../avalanche/`, `data/avalanche-regions.geojson`, `tools/avalanche_regions.py` | CG-11: Lombardy's avalanche bulletin (ARPA Lombardia, CAAML v6 JSON published by the EAWS on static.avalanche.report, issued each morning in season) and its micro-regions (EAWS, CC0), which place each crag in a zone; a missing or failed bulletin leaves the line out and never stops the hourly build; the Spring service keeps each day's levels, bands and problem types (never the text) in `avalanche_bulletin` (CG-107) |
| `data/ferrate.json`, `docs/ferrate-todo.md`, `tools/osm_ferrate.py` | CG-112: Lombardy's via ferratas from OpenStreetMap — `highway=via_ferrata` ways grouped into routes (a relation, or the same name within 500 m), each with its OSM links, start and top with their elevation (Copernicus DEM), length and `via_ferrata_scale` 0–6 where mapped; never in `crags.json`; ODbL. The todo lists those without a difficulty or a name. Rebuilt by the monthly refresh, or `python3 tools/osm_ferrate.py`; rules tested in `tools/test_osm_ferrate.py` ([ADR 0023](docs/adr/0023-via-ferratas-from-openstreetmap.md)) |
| `docs/aspects-todo.md`, `tools/aspects_todo.py` | CG-16: the crags whose wall aspect is not yet on OpenStreetMap, by province, each with its OSM edit link and how to add `climbing:orientation`; regenerated by the monthly refresh |
| `backend/.../calibration/`, `data/observations.csv` | the replay of observed days against the verdict (CG-27); CG-108: `ObservationSync` copies the CSV from main into `observation`, replaced whole at startup and at 04:40 |
| `docs/ferrate-survey.md` | CG-111: what OpenStreetMap holds on Lombardy's via ferratas before any import — the query, the counts (about 42, 35 named, 22 with a plain 0–6 scale, 5 route relations) and what CG-112 does with them |
| `docs/pi-backup.md` | CG-109: the Pi's nightly `pg_dump` onto a USB disk, 14 days kept, never onto the SD card; how to set up the disk and how to restore, tried ([ADR 0019](docs/adr/0019-backup-on-a-usb-disk.md)) |
| `web/Dockerfile`, `web/nginx.conf`, `backend/.../export/SiteData.java` | CG-110: the page on the Pi, next to GitHub Pages — the same build behind nginx, its data served live by the Spring service from the forecasts it already fetched, at Pages' paths ([ADR 0018](docs/adr/0018-the-pi-serves-the-page-from-its-backend.md)) |
| `backend/.../history/` | CG-101: `HistorySync` copies the `history` branch's CSVs into `crag_history` — at startup and at 23:50 and 05:50, each day replaced whole |
| `backend/.../export/ConditionsExport.java` | the serverless version: hourly scores to `site/data/conditions.json` and the static API in `site/api/v1/`, on GitHub Pages |
| `web/` | the page: React 19, TypeScript, Vite, Leaflet; `npm test` runs the Vitest unit and component tests; `npm run build` writes `site/` (generated, never committed) |
| `backend/.../export/SharePages.java` | CG-45: one static page per crag in `site/c/`, rebuilt with the data, whose Open Graph tags carry the name, the next day's verdict and weather; it forwards a browser to `?crag=<id>`. The preview image is `assets/social-preview.png`, published as `og.png` |
| `.github/workflows/ci.yml` | on every push and PR: actionlint on the workflows and shellcheck on the scripts, backend build and tests (Testcontainers on a real PostgreSQL), the page's types, tests and build, the page end to end in Chromium (Playwright, desktop and phone, axe), the tools' tests (every script, network paths against a fake local server) — each with its coverage in the run summary and a floor (backend 95%, page 99% of lines, tools 97%) that fails the job below it, Terraform fmt and validate, backlog check |
| `.github/workflows/pages.yml` | hourly and on push: scores every crag and publishes `site/` on GitHub Pages |
| `.github/workflows/image.yml` | on push to main: the linux/arm64 backend image to GHCR, on a native ARM runner |
| `.github/workflows/lighthouse.yml`, `lighthouserc.json` | on PRs touching the page or the data (a bigger crag list weighs on it): builds it with real data and fails when Lighthouse (desktop, median of 3) drops below performance 0.88 or accessibility, best practices, SEO 0.95 — measured on 2026-10-15 at 0.95 / 1 / 1 / 1; CI's performance medians 0.92–0.96 since. The page's code, React and Leaflet come in separate chunks, and the crag's panel is fetched on demand (CG-64) |
| `tools/build_site.sh` | builds `site/` as Pages serves it (the page from `web/`, then scores, data, API, logo); used by `pages.yml` and `lighthouse.yml` |
| `.github/workflows/uptime.yml`, `monitoring/checkfleet.yml` | hourly at :47: [checkfleet](https://github.com/Allan-Nava/checkfleet) checks the page, the data, the API and the TLS certificate, and a step fails when the data is over 150 minutes old; a failed run is GitHub's email alert |
| `.github/workflows/history.yml`, `tools/history_append.py` | every evening: today's verdict and weather for every crag, one CSV per month on the [`history`](https://github.com/Allan-Nava/cragcast/tree/history) branch, never on main |
| `.github/workflows/crags-refresh.yml` | on the 1st of every month: rebuilds `data/crags.json` (and, CG-112, `data/ferrate.json`) from OSM and opens a PR with the changes (`tools/crags_diff.py`) when there are any |
| `.github/workflows/backlog-issues.yml` | on push to main touching `BACKLOG.md`: issues and milestones follow the backlog |
| `.github/ISSUE_TEMPLATE/crag-data.yml` | the form to report a crag's aspect, grades or length for someone to map on OSM |
| `BACKLOG.md`, `ROADMAP.md` | planned work (`CG-n`); the roadmap is generated by `npx backlogsync roadmap` |

Weather comes from [Open-Meteo](https://open-meteo.com/) (free, no key, CC BY 4.0). Crags come
from OpenStreetMap — © OpenStreetMap contributors, ODbL — and each carries the link it was
taken from. Elevation: Copernicus DEM GLO-90 (2021,
[doi:10.5270/ESA-c5d3d65](https://doi.org/10.5270/ESA-c5d3d65)) through Open-Meteo's elevation API —
the ground of the area at 90 m resolution, not of a given route. A crag without `climbing:orientation` on OSM is judged as a shaded wall, the
cautious reading; adding the tag on OpenStreetMap sharpens its verdict at the next refresh.
Avalanche danger: the bulletin of [ARPA Lombardia](https://www.arpalombardia.it/temi-ambientali/neve-e-ghiacciai/bollettino-neve-e-valanghe/),
through the EAWS; it states no licence, so cragcast shows only its facts — levels, bands,
problem types — never its text, and always links to it. Micro-regions:
[EAWS regions](https://gitlab.com/eaws/eaws-regions), CC0.

## Calibrating the verdict

The thresholds in `ConditionsScorer` are a first heuristic. They are checked against days
someone actually saw the rock, in `data/observations.csv` (`date,crag,rock,observer,note`, rock
`dry`, `damp` or `wet`); the Spring service copies the file from main into its `observation`
table at startup and each night (CG-108), the CSV staying the source. The replay fetches each day's weather from Open-Meteo's Historical
Forecast API — the same kind of data the live verdict reads — scores it and prints a Markdown
table:

```bash
cd backend && ./mvnw -q -DskipTests package dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=runtime
java -cp "target/classes:$(cat target/cp.txt)" io.github.allannava.cragcast.calibration.Calibrate ../data/observations.csv ../data/crags.json
```

Each day is `AGREES`, `TOO_CAUTIOUS` (dry rock, cragcast said maybe or no), `MISSED_WET_ROCK`
(damp or wet rock, cragcast said go — the error that matters) or `TOO_HARSH` (damp rock, no go).
A threshold changes only when the table shows why, with a test for the day that moved it.

## Help map your crag

cragcast knows what OpenStreetMap knows. Most crags still lack their wall aspect, so cragcast
judges them as shaded walls; grades and route lengths are rarer still. If you know a crag, open
it on OpenStreetMap (every crag on the page links to its OSM edit view) and add:

| Tag | What | Example |
|---|---|---|
| `climbing:orientation` | which way the wall faces — sharpens the verdict most | `SW` |
| `climbing:grade:french:min`, `…:max` | easiest and hardest route | `5a`, `7b` |
| `climbing:length:max` | longest routes, in metres | `30` |
| `climbing:rock` | the rock | `limestone` |

**Only what you know first-hand**: OpenStreetMap does not accept data copied from guidebooks or
websites under copyright. Rather not edit OSM yourself? Open a
[crag data issue](https://github.com/Allan-Nava/cragcast/issues/new?template=crag-data.yml).
Changes reach cragcast when the crag list is rebuilt: every month a workflow opens a pull
request with what changed on OSM, and the page shows it once that PR is merged.

## Run it locally

Java 21 and Docker.

```bash
docker compose up -d db          # PostgreSQL on localhost:5432
cd backend && ./mvnw spring-boot:run
curl localhost:8080/api/crags
curl localhost:8080/api/crags/osm-node-1739484761/conditions   # tomorrow's verdict
curl localhost:8080/api/crags/osm-relation-11740994/routes      # a crag's routes, easiest first
```

Tests: `./mvnw verify`. The tests that need PostgreSQL start it with Testcontainers; on a
machine without Docker they are skipped, not failed, and CI always runs them. The `*IT`
classes run the export and the calibration whole, against fake Open-Meteo and avalanche
servers on localhost (`FakeHttp`), in verify's integration-test phase (failsafe). Coverage is
measured on every run (CG-47) and fails below a floor set from CI's figures — without Docker
the backend reads lower, so there run `./mvnw verify -Djacoco.haltOnFailure=false`. The page:
`cd web && npm run coverage`. The tools: `python3 -m unittest discover -s tools -p 'test_*.py'`.

Visual snapshots (CG-63): `web/e2e/snapshots.spec.ts` compares the page, a crag's panel, the
map and the phone's filters, desktop and phone, with CI's own images in
`web/e2e/__snapshots__/`, on fixed data at a fixed moment; a layout change fails the PR with its
diff, and is accepted by committing CI's `snapshots` artifact.

End to end (CG-51): `cd web && npx playwright install chromium && npm run e2e` builds the page and
drives it in Chromium, desktop and phone, on fixture data (`web/e2e/fixtures.ts`), with an axe
accessibility scan.

The page as Pages serves it, with real data — Java 21 and Node 22:

```bash
tools/build_site.sh              # builds web/ into site/, then scores every crag into site/data/
cd web && npx vite preview       # http://localhost:4173
cd web && npm test               # unit and component tests
```

## Releases

Releases are named by their day, `vYYYY.MM.DD` ([ADR 0020](docs/adr/0020-calendar-releases-the-pi-follows.md)).
Run **Actions → Release → Run workflow** on main: it builds the backend's and the page's arm64
images tagged with the release and carrying it (`/actuator/info`, the page's footer), then
publishes the [GitHub Release](https://github.com/Allan-Nava/cragcast/releases) with notes from
the merged PRs. The Raspberry Pi runs the release its Portainer stack names in
`CRAGCAST_VERSION`; set it and *Pull and redeploy* to move, set the previous one to go back.
Unset, the Pi follows `:main`, rebuilt at every merge. GitHub Pages is not versioned: it follows
main hourly.

## Licence

MIT — see [LICENSE](LICENSE).
