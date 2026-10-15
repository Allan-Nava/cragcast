<p align="center"><img src="assets/logo.svg" width="96" height="96" alt="cragcast logo"></p>

# cragcast

**Can I climb there tomorrow?** cragcast answers it for crags in Lombardy: for each crag,
tomorrow's weather, the rain of the last two days and the aspect of the wall become a
verdict — **go**, **maybe**, **no go** — with the reasons.

Wet rock is the main thing that ruins a climbing day, and whether a wall has dried depends on
how much it rained and how much sun the wall gets. A south-facing slab after 8 mm of rain is
usually fine the next day; a north-facing one often is not. cragcast puts that judgement in
code, with the thresholds in one place so they can be argued with.

**Live:** [allan-nava.github.io/cragcast](https://allan-nava.github.io/cragcast/) — 75 crags,
updated every hour by a GitHub Action, no server.

**How it works, in Italian:** [cragcast: si arrampica domani?](https://allan-nava.github.io/blog/2026/10/15/allan-nava-cragcast.html)
— the verdict's rules, the Oracle VM that never came, and the free stack it runs on instead.

**Status:** the page is live on GitHub Pages and is the way cragcast runs. The Spring service is
complete for one verdict per crag and day but not hosted: the Oracle Always Free VM could not be
created (no A1 capacity in Milan), so that path is parked. What comes next is in the
[roadmap](ROADMAP.md).

## The page

- a map and a list of the crags with the verdict for **today**, **tomorrow** or **the day
  after** — never by
  colour alone: go is a green circle S, maybe a yellow diamond F, no a red square N — and the
  reasons under each crag (rain, recent rain, wind, cold or heat, an unmapped aspect);
- **search** by name or municipality, ignoring case and accents, **toggles** for go / maybe /
  no go, and a **province** filter; each crag shows where it is ("Canzo (CO)");
- crowded areas (Lecco, Val Masino) gather into one mark with the count and the split by
  verdict written out (e.g. `20 · 7S·8F·5N`); zooming in or picking a crag in the list opens it;
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
- the view in the URL (`?q=&v=&p=&a=&day=`): a link opens the same view, and back works;
- per crag, where OpenStreetMap has them: grades, route length, rock, style, website; the
  **elevation** of every crag; a link to the crag on OSM and to its OSM edit view;
- a **panel per crag** ("Dettagli") — beside the map from 900 px, which stays usable, and a
  sheet from the bottom on phones; the day switches inside it with buttons or a sideways swipe,
  and a downward swipe closes the sheet: the day's hours from 06:00 to 21:00 as two small charts —
  temperature and rain, never one chart with two axes — a readout that follows the pointer or
  the arrow keys, the same hours as a table, and directions (Google Maps, OSM);
- in the avalanche season, the **avalanche bulletin** of ARPA Lombardia for the crag's zone,
  in the popup and the panel: the danger level (1–5) by elevation band and part of the day,
  the avalanche problems, and a link to the official bulletin. Only the crags inside one of
  the bulletin's micro-regions get it — 44 of 75; the low crags lie outside them. The verdict
  does not change: it is about the rock, the bulletin about the approach and winter routes;
- **installable** on a phone's home screen (a web app manifest and a service worker): it shows
  the last data offline and says how old it is when it is more than 3 hours old;
- updated every hour by `.github/workflows/pages.yml`, at about :17;
- built with React, TypeScript and Vite from `web/`, Leaflet and its cluster plugin bundled
  from npm — no third-party script at runtime.

## History

Every evening the verdict of the day and the weather behind it are saved for every crag, one
CSV per month, on the [`history`](https://github.com/Allan-Nava/cragcast/tree/history) branch:
`date, crag_id, crag_name, province, verdict, rain_last48h_mm, rain_day_mm, max_temp_c,
max_wind_kmh, reasons, generated_at`. It answers "how was last week at this crag" and is the
data the calibration (CG-27) needs next to the observations.

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
  the official page.

Credit OpenStreetMap (ODbL), Open-Meteo (CC BY 4.0) and Copernicus when you show the data,
and ARPA Lombardia for the avalanche levels.

## How it is built

| Part | What |
|---|---|
| `backend/` | Java 21, Spring Boot 4, PostgreSQL, Flyway. `GET /api/crags` and `GET /api/crags/{slug}` (position, aspect, elevation, OSM details), `GET /api/crags/{slug}/conditions?date=`; imports the crags from `data/crags.json` at startup and refreshes the weather every hour |
| `backend/.../conditions/ConditionsScorer.java` | the verdict: weather + aspect → go / maybe / no go, with reasons |
| `backend/.../weather/` | Open-Meteo client and the reduction of an hourly forecast to one day: rain in the previous 48 h, and rain, max temperature and max wind from 08:00 to 18:00 |
| `docker-compose.yml` | PostgreSQL (and the backend) for local development |
| `deploy/k8s/` | Kubernetes manifests for k3s on an Oracle Cloud Always Free Ampere VM; parked with `infra/oci/` |
| `infra/oci/` | Terraform for that VM and its network, with `retry-apply.sh` for "Out of host capacity"; parked — nothing is applied (the VM never got capacity, the network was destroyed) |
| `data/crags.json`, `tools/osm_crags.py` | the crags, from OpenStreetMap (`climbing=crag`), each with its OSM link, its aspect where mapped, its validated climbing details, its elevation (Copernicus DEM), its municipality and province (OSM administrative boundaries); a crag mapped twice under one name less than 50 m apart is folded into one, keeping both OSM links; ODbL. Rebuilt monthly by `crags-refresh.yml` through a reviewed PR, or by hand: `python3 tools/osm_crags.py` |
| `backend/.../avalanche/`, `data/avalanche-regions.geojson`, `tools/avalanche_regions.py` | CG-11: Lombardy's avalanche bulletin (ARPA Lombardia, CAAML v6 JSON published by the EAWS on static.avalanche.report, issued each morning in season) and its micro-regions (EAWS, CC0), which place each crag in a zone; a missing or failed bulletin leaves the line out and never stops the hourly build |
| `docs/aspects-todo.md`, `tools/aspects_todo.py` | CG-16: the crags whose wall aspect is not yet on OpenStreetMap, by province, each with its OSM edit link and how to add `climbing:orientation`; regenerated by the monthly refresh |
| `backend/.../calibration/`, `data/observations.csv` | the replay of observed days against the verdict (CG-27) |
| `backend/.../export/ConditionsExport.java` | the serverless version: hourly scores to `site/data/conditions.json` and the static API in `site/api/v1/`, on GitHub Pages |
| `web/` | the page: React 19, TypeScript, Vite, Leaflet; `npm test` runs the Vitest unit and component tests; `npm run build` writes `site/` (generated, never committed) |
| `.github/workflows/ci.yml` | on every push and PR: backend build and tests (Testcontainers on a real PostgreSQL), the page's types, tests and build, Terraform fmt and validate, backlog check |
| `.github/workflows/pages.yml` | hourly and on push: scores every crag and publishes `site/` on GitHub Pages |
| `.github/workflows/image.yml` | on push to main: the linux/arm64 backend image to GHCR, on a native ARM runner |
| `.github/workflows/lighthouse.yml`, `lighthouserc.json` | on PRs touching the page: builds it with real data and fails when Lighthouse (desktop, median of 3) drops below performance 0.85 or accessibility, best practices, SEO 0.95 — measured on 2026-10-15 at 0.95 / 1 / 1 / 1 |
| `tools/build_site.sh` | builds `site/` as Pages serves it (the page from `web/`, then scores, data, API, logo); used by `pages.yml` and `lighthouse.yml` |
| `.github/workflows/uptime.yml`, `monitoring/checkfleet.yml` | hourly at :47: [checkfleet](https://github.com/Allan-Nava/checkfleet) checks the page, the data, the API and the TLS certificate, and a step fails when the data is over 150 minutes old; a failed run is GitHub's email alert |
| `.github/workflows/history.yml`, `tools/history_append.py` | every evening: today's verdict and weather for every crag, one CSV per month on the [`history`](https://github.com/Allan-Nava/cragcast/tree/history) branch, never on main |
| `.github/workflows/crags-refresh.yml` | on the 1st of every month: rebuilds `data/crags.json` from OSM and opens a PR with the changes (`tools/crags_diff.py`) when there are any |
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
`dry`, `damp` or `wet`). The replay fetches each day's weather from Open-Meteo's Historical
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
```

Tests: `./mvnw verify`. The tests that need PostgreSQL start it with Testcontainers; on a
machine without Docker they are skipped, not failed, and CI always runs them.

The page as Pages serves it, with real data — Java 21 and Node 22:

```bash
tools/build_site.sh              # builds web/ into site/, then scores every crag into site/data/
cd web && npx vite preview       # http://localhost:4173
cd web && npm test               # unit and component tests
```

## Licence

MIT — see [LICENSE](LICENSE).
