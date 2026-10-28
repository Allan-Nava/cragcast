# 0026 — Beyond Lombardy: Piedmont and Trentino-Alto Adige, Lombardy first

- **Status:** accepted
- **Date:** 2026-10-28
- **Backlog:** CG-43

## Context

Lombard climbers drive to Piedmont (the Ossola, the Valle Orco) and to
Trentino-Alto Adige (Arco, the Dolomites) as often as to their own valleys. On 2026-10-28
OpenStreetMap held 652 climbing objects in Lombardy, 835 in Piedmont and 845 in Trentino-Alto
Adige. Everything in cragcast assumed one region: the query, the page's text and filters, the
avalanche bulletin, the size of the hourly data. The Ticino, also asked for, is another country:
no Italian province, its own avalanche service.

## Decision

- **Two Italian neighbours first.** `tools/osm_crags.py` queries Lombardy (IT-25), Piedmont
  (IT-21) and Trentino-Alto Adige (IT-32) at once, through the same reviewed monthly PR and the
  same rules (ADR 0003, ADR 0009). Each crag gets its `region` from OSM's administrative
  boundaries (admin level 4), as it gets its province — never from the query that found it.
- **Lombardy stays the default view.** The page shows one region at a time, Lombardy unless
  `?r=` says otherwise, or all of them; the provinces follow the region. A crag without a region
  (data from before) counts as Lombardy.
- **One data file, for now.** `conditions.json` holds every crag. Its size grows with them; the
  Lighthouse budget (CG-36) decides: if it fails, the next step is one file per region, loaded
  when the region is chosen.
- **`region` is added** to `conditions.json`, the API v1 crag and index entries (CG-32), and the
  Pi's crag table.
- **Every 3 hours, not every hour.** Open-Meteo's free tier counts every place — 600 a minute,
  10,000 a day — as the first run of this change found (429, minute limit exceeded). About 820
  places every hour would be ~19,700 a day; every 3 hours it is ~6,600 on GitHub Pages, and the
  Pi has its own budget. Both ask 100 places a request, 15 s apart (about 400 a minute). The
  hourly cadence of ADR 0002 becomes every 3 hours; the page warns past 7 h instead of 3 h,
  and the uptime check allows 420 minutes. Forecast models update every few hours, so the
  verdict loses little.
- **Not yet:** the avalanche bulletins of Piedmont and Trentino-Alto Adige (their crags have no
  avalanche line until then), their via ferratas, and the Ticino — each its own backlog item.

## Consequences

- Data up to 3 hours old instead of 1. A paid Open-Meteo plan would bring the hour back; the
  project stays on the free tier.
- Three and a half times the crags: a longer monthly refresh PR to review, a longer aspects
  todo and a larger history branch. On the Pi, `crag_forecast` gets 3.5 times the crags but a
  third of the refreshes — about as many rows a day as before.
- A reader in Lombardy sees what they saw before unless they choose another region.
- The verdict is the same everywhere; nothing in `ConditionsScorer` knows about regions.
