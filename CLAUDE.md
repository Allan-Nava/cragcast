# CLAUDE.md — cragcast

"Can I climb there tomorrow?" for crags in Lombardy. Java 21 + Spring Boot 4 + PostgreSQL in
`backend/`; k3s manifests in `deploy/k8s/` for an Oracle Cloud Always Free Ampere VM (arm64).

**Where it runs:** GitHub Pages, built hourly by `pages.yml` — the only live surface. The
self-hosted path (`infra/oci/`, `deploy/k8s/`) is parked since 2026-10-15: Oracle's free A1 VM
never got capacity in Milan and its network was destroyed. Do not apply it without asking.
The Spring service also runs on a Raspberry Pi on a friend's home network (CG-100, ADR 0015):
`deploy/pi/compose.yml` as a Portainer Git stack that polls main and re-pulls the GHCR image —
the home network and the owner's tailnet (CG-102, ADR 0017: Tailscale on the host, no routes
advertised, no exit node), PostgreSQL on the Pi's 127.0.0.1 only, the password in Portainer.
**Never a self-hosted runner**: the repository is public, and a fork's PR could run code on that
network. The `backup` service dumps the database nightly onto a USB disk (CG-109, ADR 0019) and
writes only where `.cragcast-backup-target` is — never onto the SD card; a change to the schema
or to the dump keeps `docs/pi-backup.md`'s restore true.

## Rules

- **No invented places.** A crag enters the database only with coordinates and aspect taken
  from a public source, whose URL goes in `source_url`. Test data is made up and says so.
- **Two ways to run, one scorer.** The serverless version (`ConditionsExport` + the page in `web/`,
  published hourly by `pages.yml` on GitHub Pages) and the Spring service (on the Pi; `deploy/k8s/`
  for the parked VM) share `ConditionsScorer`, `WeatherReducer` and `OpenMeteoClient`. A change to
  the verdict lands in both. The Pi also serves the page (CG-110, ADR 0018): `SiteData` builds
  its data with `ConditionsExport.build()` from the forecasts `WeatherRefresh` already fetched —
  never a second Open-Meteo request — and `SiteController` serves it at Pages' paths. All of `site/` is generated (`tools/build_site.sh`: Vite from `web/`,
  then the data), never committed.
- **Releases are by date and the Pi follows one** ([ADR 0020](docs/adr/0020-calendar-releases-the-pi-follows.md)):
  `release.yml`, run by hand from main, names the release `vYYYY.MM.DD` (`.2` for a second the
  same day), builds both images with it, and only then publishes the GitHub Release. Never push a
  release tag by hand, and never reuse one. The Pi's stack picks its images by `CRAGCAST_VERSION`;
  a merge reaches it only through a new release.
- **A data file in the jar is a data file in the image**: `backend/pom.xml` puts `data/` files
  on the classpath and `backend/Dockerfile` must copy each one, and `image.yml` must rebuild
  when one changes. `tools/test_dockerfile_data.py` checks both — `osm-climbing.json` was once
  in the jar built by CI and missing from the Pi's image.
- **`osm_object` is a copy of `data/osm-climbing.json`** (CG-103): every object of the same OSM
  query, with a category; `OsmObjectImporter` replaces the table whole at startup, in one
  transaction. Like the crags, it changes only through the reviewed monthly PR; nothing writes to
  the table otherwise. A column is added for what is worth filtering on, the rest stays in
  `details` as JSON text. Gyms and shops are kept there as `venue`, never shown as crags.
- **A sector keeps its own values, the crag keeps the folded ones** (CG-104): `group()` lists the
  OSM ids it folds in as sectors in the crag's `sectors` (the same place drawn twice stays in
  `alsoMapped` only); `SectorImporter` runs after `CragImporter` (`@Order`) and replaces the
  `sector` table whole from those ids and `osm-climbing.json`. The crag's grades, aspect and
  counts never change because a sector is kept; `SectorImporterTest` fails when a listed
  sector is missing from the objects file.
- **A route belongs to a crag, never is one** (CG-105): `link_routes()` in `osm_crags.py` links
  each single route by OSM relation first (the crag's, a sector's or a folded duplicate's), else
  to the nearest crag or sector within `ROUTE_M` (400 m), else leaves it unlinked — never
  guessed farther. Anchors and abseils are parts of a route, not routes. The `route` table and
  `GET /api/crags/{slug}/routes` serve them; they never enter `crags.json` or `/api/crags`.
  `grade_rank` is `FrenchGrade.rank()` (6a = 36, six steps a number) so routes sort by grade.
- **The page shows sectors and routes from the same file** (CG-117): `ConditionsExport` reads
  `data/osm-climbing.json` next to `crags.json` (`SiteData` from the jar) through `OsmClimbing`,
  so GitHub Pages and the Pi show the same lists without a database. In the panel they stay
  folded away under their count (`Climbing.tsx`); a crag without either shows nothing. Type-check
  the web app with `npm run typecheck` (`tsc --noEmit`): `tsc -b` writes `.js` files beside the
  sources, and Vitest then imports those stale copies.
- **Routes read by level** (CG-118): the level is the grade's number (`routeLevel()`: 4 and under
  together, 8 and over together); one button per level present with its count, the table grouped
  under the same levels, and one line on how the French scale reads. The words (facile, medio,
  impegnativo, difficile, estremo) are said to be indicative; the grade itself is OSM's.
- **Crags come from OpenStreetMap** via `tools/osm_crags.py` into `data/crags.json` (ODbL:
  credit OSM on every surface that shows them): `climbing=crag`, and since CG-42 named outdoor
  `sport=climbing` places (a cliff, a rock, a climbing site relation) — never gyms, shops,
  buildings or single routes. Sectors fold into their crag (`group()`): a shape within 50 m of a
  crag is the same place unless it is a sibling sector; parts sharing a base name within 2 km
  are one crag, named by that base; a loose point within 300 m is a sector. A climbing=crag
  never loses its id, its name or another climbing=crag to grouping. A sector's route count,
  mean grade or aspect never becomes the crag's: grades widen to the parts' range, and the
  aspect stays only when every part agrees. `tools/test_osm_crags.py` holds these rules. The aspect is `climbing:orientation` or null —
  never filled in by guess; the fix for a missing aspect is an edit on OSM. The same holds for
  the `details` (grades, route length, rock, style, website): read from OSM's climbing tags,
  validated in `osm_crags.py`, dropped when malformed, shown only where present.
  Duplicates (CG-26): same name and under 50 m apart is one crag mapped twice — folded, the node
  kept (stable id), the others' links in `alsoMapped`. Same name farther apart stays separate:
  generic sector names ("Settore Destro") may belong to different crags.
- **Via ferratas are not crags** (CG-112, ADR 0023): `tools/osm_ferrate.py` writes
  `data/ferrate.json` and `docs/ferrate-todo.md` (generated, never edited by hand), through the
  same reviewed monthly PR; never into `data/crags.json`. A relation is one via ferrata; loose
  ways join by the same name within `NAME_M` (500 m) or, unnamed, by touching (`TOUCH_M`), never
  two names in one; an unnamed group is left out and listed. `via_ferrata_scale` is a whole 0–6
  or dropped and listed — `2+` is never read as 2. Start and top from the DEM, sampled along
  the track. `tools/test_osm_ferrate.py` holds these rules.
- **`docs/aspects-todo.md` is generated** by `tools/aspects_todo.py` from `data/crags.json`
  (CG-16) and committed with it by `crags-refresh.yml`; never edit it by hand. It never names
  who knows which crag: the list is for anyone who climbs there.
- **Overpass is often busy** (502/500, or a mirror answering empty). `osm_crags.py` retries
  each query on two servers and refuses an empty answer, so a failed run leaves
  `data/crags.json` as it was; the monthly refresh then simply fails — run it again by hand
  (`workflow_dispatch`). Never hand-edit the file to work around it.
- **The crag list changes only through a reviewed PR.** `crags-refresh.yml` rebuilds
  `data/crags.json` on the 1st of each month and opens a PR (branch `crags/<date>`, summary from
  `tools/crags_diff.py`); it never pushes to main, and skips while such a PR is open. GitHub runs
  no CI on a PR opened with GITHUB_TOKEN: close and reopen it to start CI. The repo setting
  "Allow GitHub Actions to create … pull requests" must stay on for it to work.
- **Contributions go to OpenStreetMap, first-hand only.** The page, the README and the issue form
  ask climbers to map what they know themselves; never suggest copying from guidebooks or
  sites under copyright — OSM refuses it, and so does cragcast.
- **Observations are first-hand or nothing.** `data/observations.csv` holds only days someone saw
  the rock, with who saw it; never a guessed or reconstructed row. Smoke tests of `Calibrate`
  use a file outside the repo. `ObservationSync` (CG-108) copies it from main into `observation`,
  replaced whole; a read without the header, a bad row or a failed request keeps the table, and
  Spring tests set `cragcast.observations.enabled=false`.
- **Thresholds live in `ConditionsScorer` as named constants**, each with a one-line reason. A
  change to one is a reviewed change, with a test for the day it fixes.
- **Crag details are JSON text in the database** (`crag.details`, V3): written and read whole
  with Spring's `JsonMapper`, not mapped by Hibernate — Hibernate's JSON support and Jackson 3
  are not a pairing to rely on. Nothing may query inside it; a field worth filtering on gets
  its own column.
- **The schema belongs to Flyway** (`backend/src/main/resources/db/migration`); Hibernate only
  validates. Never edit a migration that has run — add the next one.
- **Tests that need PostgreSQL** use Testcontainers and `@EnabledIf(DockerAvailable.CHECK)`:
  skipped without Docker, always run in CI. Logic tests need no database.
- **arm64 first.** The VM is Ampere: images must be multi-arch; `image.yml` builds `linux/arm64` on a native ARM runner (`ubuntu-24.04-arm`) and pushes `ghcr.io/allan-nava/cragcast-backend` and (CG-110) `cragcast-web`, `:main` and `:<sha>`.
- **Free tier only.** Nothing in `deploy/` or `infra/` may exceed Oracle Always Free (A1.Flex
  2 OCPU / 12 GB in total, 200 GB of block storage). Secrets and Terraform state never get
  committed.
- **Integration tests are `*IT` and run under failsafe** (CG-49): a class named `*IT` that
  surefire would never run. Entry points that read the environment take it as an argument
  (`ConditionsExport.run(args, env)`, `Calibrate.run(…, url)`) so the tests point them at
  `FakeHttp`; keep `main` a thin wrapper.
- **Visual snapshots are CI's images** (CG-63): fonts differ between machines, so the reference
  images in `web/e2e/__snapshots__/` are made by CI and never by a laptop; they run locally only
  with `SNAPSHOTS=1`. A deliberate layout change updates them in the same PR from the run's
  `snapshots` artifact (`gh run download <run> -n snapshots -D web/e2e/__snapshots__`), after
  looking at the diff. The tolerance is a few pixels (`maxDiffPixels`): a ratio of 1% let a
  broken logo through.
- **The browser tests own what jsdom cannot do** (CG-51): dragging and zooming the map, real
  layout, swipes, the axe scan. They run on fixture data with tiles stubbed and the service
  worker blocked — never against OSM's tile servers. A new interaction on the map gets an
  `e2e/` test; serious or critical axe findings fail CI. Locally on WSL without sudo, the
  headless shell's libraries (libnspr4, libnss3, libasound2) come from `apt-get download`
  into a folder on `LD_LIBRARY_PATH`.
- **Workflows and scripts are linted** (CG-52): actionlint (which shellchecks every `run:`
  block) and shellcheck on `tools/*.sh` and `infra/oci/*.sh`, in CI. A finding is fixed, or
  silenced on its line with the reason next to it — never turned off for a whole file.
- **Coverage floors only go up** (CG-47): the backend's in `backend/pom.xml`
  (`coverage.floor`), the page's in `web/vite.config.ts` (`thresholds`), the tools' in
  `ci.yml` (`--fail-under`). Each is CI's figure rounded down when set; a PR that adds tests
  raises it, and none lowers it — when a change drops coverage, it brings tests. The backend's
  floor is CI's figure with the Testcontainers tests; a laptop without Docker reads lower.
- **`docs/ARCHITECTURE.md` follows the code** (CG-66): a PR that adds or changes a part, a data
  contract, a workflow or a test level updates it — its diagrams too (Mermaid; check them with
  Mermaid's parser, GitHub shows an error in their place otherwise).
- **The stylesheet speaks in tokens** (CG-62): colours (CG-60), spacing (`--space-*`), type
  (`--text-*`), radii (`--radius-*`) and the focus ring (`--focus-ring`) are variables at the top
  of `web/src/style.css`; new CSS uses them, never a new pixel value. The exceptions are the
  verdict and cluster marks and the charts' labels, drawn at fixed sizes like their shapes. One
  `:where(…):focus-visible` rule gives every control the same ring; an e2e test tabs through them.
- **Decisions get an Architectural Decision Record** (CG-65, [ADR 0001](docs/adr/0001-record-decisions.md)):
  a change to how cragcast is built, what data it trusts or what it promises its readers comes
  with a numbered record in `docs/adr/` (from `template.md`) in the same PR, listed in
  `docs/adr/README.md`. CLAUDE.md keeps the rules; the ADRs keep why. A reversed decision keeps
  its record, marked superseded by the new one — never edited away.
- **Planned work goes in `BACKLOG.md`** (ids `CG-n`); `ROADMAP.md` is generated by
  `npx backlogsync roadmap`, and the issues follow on push to main.
- Commits without a Claude co-author line.

- **The verdict is never told by colour alone** (CG-21): every mark has a shape and a letter
  (circle S, diamond F, square N) and its text label; colours keep ≥4.5:1 contrast with the black
  letter. A new verdict surface follows the same rule — the cluster mark (CG-23) is neutral grey
  and writes the split out.
- **Charts on the page** (`web/src/components/HourChart.tsx`): one measure per chart, never two y-axes; rain
  `#3987e5`, temperature `#d95926`, validated on both panels (`#111418` dark, `#ffffff` light)
  with the dataviz validator; never a verdict colour for a series; every chart has a table of
  the same numbers.
- **Two themes, one set of tokens** (CG-60): the page follows `prefers-color-scheme`; every
  colour in `web/src/style.css` is a token on `:root`, redefined for light. A verdict colour has
  two tokens: `--go` (text, borders, rings: ≥4.5:1 on the surface, so darker on white) and
  `--go-fill` (the mark's fill under its black letter, the same in both). A new colour gets a
  token in both themes; the axe scan and the snapshots run in both (Playwright's default is dark).
- **The service worker** (`web/public/sw.js`, CG-35): network first for the page, its bundled
  assets and the data, cache only as the offline fallback; **map tiles never cached** (OSM's
  tile policy). Adding a file to `SHELL` or changing the strategy means bumping `CACHE`
  (now `cragcast-v2`, bumped for CG-10), or old phones keep the old copy.
- **The page is React** (CG-10, `web/`): Leaflet is driven imperatively inside `CragMap`, and
  names and texts from OSM reach the DOM as text, never as markup (React's JSX, `textContent` in
  popups). The pure parts (`web/src/lib/`) carry the unit tests; a behaviour change comes with a
  test in `web/src/**/*.test.ts(x)`.
- **The avalanche bulletin informs, it never scores** (CG-11): the verdict is about the rock,
  so `ConditionsScorer` ignores the bulletin. Only its facts are shown (levels, bands, problem
  types), never its text — ARPA Lombardia states no licence — always with the link to the
  official bulletin. A bulletin that is missing (out of season, before the morning issue) or
  fails leaves the line out; it never fails the hourly build. The Spring service keeps the same
  facts per day and micro-region in `avalanche_bulletin` (CG-107, ADR 0022, `AvalancheArchive`):
  the latest issue replaces the day, none keeps it, and nothing scores from it. The micro-regions
  in `data/avalanche-regions.geojson` change only through `tools/avalanche_regions.py` and a PR.
- **Map moves: the reader's or the page's** (CG-38). Only a drag or a zoom by the reader offers
  "Cerca in quest'area"; moves the page makes (flying to a crag, centring on the reader) go
  through `programmatic` in `CragMap`, so a new programmatic move must too. Animations
  respect `prefers-reduced-motion`. Headless Chrome screenshots do not run Leaflet's
  animations (requestAnimationFrame stalls), so map interactions are tested in
  `CragMap.test.tsx` with the real Leaflet in jsdom, not by screenshot.
- **The panel is a dialog either way** (CG-40): non-modal `show()` from 900 px, inside `<main>`'s
  grid in the list's column (CG-56: `main.panel-open` hides the list), never fixed over the page
  (Esc handled by the component), modal `showModal()` as a bottom sheet below. Swipes never
  start on a chart (`.hchart` reads the pointer itself). Closing goes through `close()` in
  `DetailDialog` for the slide-out, and `shut()` covers browsers without `dialog.close`.
- **An open page refreshes itself** (CG-41): `App` fetches `data/conditions.json` every
  `REFRESH_MS` (10 min) and on `visibilitychange`, and swaps the data only when `generatedAt`
  changed. A refresh must never reset the view, the open panel or the position, and a failed
  one keeps the data shown — the footer's age and the 3 h notice tell the reader.
- **Link previews are static HTML** (CG-45): `SharePages` writes `site/c/<id>.html` every hour —
  link previews never run JavaScript. The day is written as a date, never "domani" (a preview
  is read later than it is made). Its reason words copy `REASON_IT` in `web/src/lib/text.ts`:
  change one, change both. Absolute URLs come from `SITE_URL` (default the Pages URL), and
  `web/index.html` carries the same URL in its own Open Graph tags.
- **Favourites never leave the browser either** (CG-46): `localStorage` only, every access in
  try/catch (`web/src/lib/favourites.ts`), never in the URL or a request. The favourites
  filter scopes the list and the summary like the province does.
- **The reader's position never leaves the browser** (CG-22): computed on, kept in a variable,
  never in the URL, never in a request, never in storage.
- **The hourly forecasts are archived, never rewritten** (CG-106, ADR 0021): `ForecastArchive`
  appends every crag-day of each successful refresh to `crag_forecast`, keyed by the refresh's
  `fetched_at`, from the `ForecastsRefreshed` forecasts — never a second Open-Meteo request.
  Insert only (`ON CONFLICT DO NOTHING`); a failure is logged and never thrown at the event, which
  the page's data shares. A retention rule, when one comes, drops whole old days.
- **History lives on the `history` branch, never on main** (CG-34). `history.yml` appends the
  day's rows each evening through a worktree; a rerun replaces the day's rows. Its CSV columns
  are a contract like the API: add columns at the end, never rename or drop one. The Spring
  service copies it into `crag_history` (CG-101, ADR 0016) with `HistorySync`, reading columns
  by name; the CSV stays the source, and Spring tests set `cragcast.history.enabled=false`.
- **`/api/v1/` is a contract** (CG-32): `ConditionsExport.api()` may add fields, never rename or
  remove one; a breaking change is a new `/api/v2/` next to v1. The README's API section lists
  every field — a new field goes there in the same PR.
- **Lighthouse budgets are a floor, not a target** (CG-36): `lighthouserc.json` fails a PR below
  performance 0.88 or accessibility / best practices / SEO 0.95 (median of 3, desktop). Lower a
  budget only with the reason in the commit; the measured baseline is in the README.
- **Uptime is checked from outside** (CG-37): `uptime.yml` runs checkfleet (pinned version, checksum
  verified) against `monitoring/checkfleet.yml` and checks the API's `generatedAt` age. A new
  public file (an API path, a page) gets a target there in the same PR.
- **Docs move with the code.** Every PR updates what it changes in README.md (the page's
  features, the "How it is built" table, how to run it), this file and BACKLOG.md. A PR whose
  docs still describe the old behaviour is not done.

## Verify

```bash
cd backend && ./mvnw verify
npx backlogsync check
```
