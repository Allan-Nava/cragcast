<p align="center"><img src="assets/logo.svg" width="96" height="96" alt="cragcast logo"></p>

# cragcast

**Can I climb there tomorrow?** cragcast answers it for crags in Lombardy: for each crag,
tomorrow's weather, the rain of the last two days and the aspect of the wall become a
verdict — **go**, **maybe**, **no go** — with the reasons.

Wet rock is the main thing that ruins a climbing day, and whether a wall has dried depends on
how much it rained and how much sun the wall gets. A south-facing slab after 8 mm of rain is
usually fine the next day; a north-facing one often is not. cragcast puts that judgement in
code, with the thresholds in one place so they can be argued with.

**Live:** [allan-nava.github.io/cragcast](https://allan-nava.github.io/cragcast/) — 76 crags,
updated every hour by a GitHub Action, no server.

**Status:** early. The backend serves crags, fetches the hourly forecast from Open-Meteo and
scores a day's weather; the crag list, the conditions endpoint and the web page are on the
[roadmap](ROADMAP.md).

## How it is built

| Part | What |
|---|---|
| `backend/` | Java 21, Spring Boot 4, PostgreSQL, Flyway. `GET /api/crags`, `GET /api/crags/{slug}` |
| `backend/.../conditions/ConditionsScorer.java` | the verdict: weather + aspect → go / maybe / no go, with reasons |
| `backend/.../weather/` | Open-Meteo client and the reduction of an hourly forecast to one day: rain in the previous 48 h, and rain, max temperature and max wind from 08:00 to 18:00 |
| `docker-compose.yml` | PostgreSQL (and the backend) for local development |
| `deploy/k8s/` | Kubernetes manifests for k3s on an Oracle Cloud Always Free Ampere VM |
| `infra/oci/` | Terraform for that VM and its network, with a retry loop for "Out of host capacity" |
| `data/crags.json`, `tools/osm_crags.py` | the crags, from OpenStreetMap (`climbing=crag`), each with its OSM link; ODbL |
| `backend/.../export/ConditionsExport.java`, `site/` | the serverless version: hourly scores to `site/data/conditions.json`, a static page with a map, on GitHub Pages |
| `.github/workflows/` | CI: build and tests (Testcontainers on a real PostgreSQL), backlog check; the linux/arm64 image to GHCR on every push to main |

Weather comes from [Open-Meteo](https://open-meteo.com/) (free, no key, CC BY 4.0). Crags come
from OpenStreetMap — © OpenStreetMap contributors, ODbL — and each carries the link it was
taken from. A crag without `climbing:orientation` on OSM is judged as a shaded wall, the
cautious reading; adding the tag on OpenStreetMap sharpens its verdict at the next refresh.

## Run it locally

Java 21 and Docker.

```bash
docker compose up -d db          # PostgreSQL on localhost:5432
cd backend && ./mvnw spring-boot:run
curl localhost:8080/api/crags
```

Tests: `./mvnw verify`. The tests that need PostgreSQL start it with Testcontainers; on a
machine without Docker they are skipped, not failed, and CI always runs them.

## Licence

MIT — see [LICENSE](LICENSE).
