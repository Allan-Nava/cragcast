# cragcast

**Can I climb there tomorrow?** cragcast answers it for crags in Lombardy: for each crag,
tomorrow's weather, the rain of the last two days and the aspect of the wall become a
verdict — **go**, **maybe**, **no go** — with the reasons.

Wet rock is the main thing that ruins a climbing day, and whether a wall has dried depends on
how much it rained and how much sun the wall gets. A south-facing slab after 8 mm of rain is
usually fine the next day; a north-facing one often is not. cragcast puts that judgement in
code, with the thresholds in one place so they can be argued with.

**Status:** early. The backend serves crags and scores a day's weather; the weather client,
the crag list and the web page are on the [roadmap](ROADMAP.md).

## How it is built

| Part | What |
|---|---|
| `backend/` | Java 21, Spring Boot 4, PostgreSQL, Flyway. `GET /api/crags`, `GET /api/crags/{slug}` |
| `backend/.../conditions/ConditionsScorer.java` | the verdict: weather + aspect → go / maybe / no go, with reasons |
| `docker-compose.yml` | PostgreSQL (and the backend) for local development |
| `deploy/k8s/` | Kubernetes manifests for k3s on an Oracle Cloud Always Free Ampere VM |
| `.github/workflows/` | CI: build and tests (Testcontainers on a real PostgreSQL), backlog check; the linux/arm64 image to GHCR on every push to main |

Weather comes from [Open-Meteo](https://open-meteo.com/) (free, no key). Every crag in the
database carries the public source its position was taken from.

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
