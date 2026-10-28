# Via ferratas in Lombardy on OpenStreetMap (CG-111)

A count of what OSM holds before any import (v1.3.0), made on 2026-10-28 against OSM data of
10:57 UTC that day. It decides what CG-112 imports and what is asked of climbers first.

## The query

```overpass
[out:json][timeout:180];
area["ISO3166-2"="IT-25"]->.region;
way["highway"="via_ferrata"](area.region);
out tags center;
relation["route"="via_ferrata"](area.region);
out body;
```

Run on a mirror that has the area index (`maps.mail.ru`); `overpass.private.coffee` answered 0
even for the crags, so an empty answer is checked against a known count before it is believed.

## What OSM has

| | Count |
|---|---|
| `highway=via_ferrata` ways | 74 |
| `route=via_ferrata` relations | 5 (4 named, 4 with a scale) |
| ways that belong to such a relation | 7 of 74 |
| ways with a name | 51 of 74 |
| ways with `via_ferrata_scale` | 42 of 74 — 35 a plain 0–6, 7 not (`2+` ×3, `3+` ×3, `K2` ×1) |
| distinct via ferratas, estimated | 42 |
| … with a name | 35 of 42 |
| … with a plain 0–6 scale | 22 of 42 |
| … with both | 20 of 42 |

The estimate joins ways that share a name or lie within 500 m of each other: 27 via ferratas
are one way, 15 are drawn in 2 to 7 pieces. It is an estimate — two routes from the same
hut can join, one long route can split — and CG-112's grouping replaces it.

## What it means

- **Enough to import, not enough to trust blind.** Most are named, so a list is readable; half
  carry no usable difficulty, so the page says "difficoltà non mappata" rather than guessing.
- **Relations are nearly absent.** The start, the highest point and the length come from the
  ways' geometry, joined as above — CG-112 needs its own grouping rule, tested like `group()`.
- **`2+`, `3+` and `K2` are not the 0–6 scale** OSM documents: dropped, as malformed tags are
  for the crags, and listed for an edit on OSM.
- **Mapping first-hand helps most where the scale is missing**: a list like
  `docs/aspects-todo.md`, generated from the import, for the 20-odd without one.
