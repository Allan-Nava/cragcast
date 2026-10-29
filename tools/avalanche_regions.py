#!/usr/bin/env python3
"""CG-11: write data/avalanche-regions.geojson — the avalanche micro-regions of Lombardy (IT-25-…)
and, since CG-120, Piedmont (IT-21-…) and Trentino-Alto Adige (IT-32-TN-…, IT-32-BZ-…), from the
EAWS regions repository (CC0), keeping only the versions in force (no end_date) and
rounding coordinates to 5 decimals (about 1 m). ConditionsExport uses it to find the bulletin
zone of each crag. The regions change rarely (last on 2025-11-03); run by hand when EAWS
publishes new ones, and review the diff like any change to data/.

    python3 tools/avalanche_regions.py
"""
import json
import pathlib
import urllib.request

BASE = "https://gitlab.com/eaws/eaws-regions/-/raw/master/public/micro-regions/"
# The same files the bulletins come in (AvalancheBulletins.PROVIDERS).
FILES = ("IT-25", "IT-21", "IT-32-TN", "IT-32-BZ")
SOURCE = BASE + "{}_micro-regions.geojson.json"
TARGET = pathlib.Path(__file__).resolve().parent.parent / "data" / "avalanche-regions.geojson"


def ring(points):
    return [[round(p[0], 5), round(p[1], 5)] for p in points]


def main():
    sources = []
    for name in FILES:
        with urllib.request.urlopen(SOURCE.format(name), timeout=60) as r:
            sources.append(json.load(r))
    features = []
    for f in (f for source in sources for f in source["features"]):
        if f["properties"].get("end_date"):
            continue
        g = f["geometry"]
        polygons = g["coordinates"] if g["type"] == "MultiPolygon" else [g["coordinates"]]
        features.append({
            "type": "Feature",
            "properties": {"id": f["properties"]["id"], "start_date": f["properties"].get("start_date")},
            "geometry": {"type": "MultiPolygon", "coordinates": [[ring(r) for r in p] for p in polygons]},
        })
    if not features:
        raise SystemExit("no micro-region in force: refusing to write an empty file")
    features.sort(key=lambda f: f["properties"]["id"])
    TARGET.write_text(json.dumps({
        "type": "FeatureCollection",
        "source": [SOURCE.format(name) for name in FILES],
        "licence": "CC0 1.0 (EAWS regions)",
        "note": "The avalanche micro-regions in force (no end_date) of Lombardy, Piedmont and Trentino-Alto "
                "Adige, coordinates rounded to 5 "
                "decimals. Rebuild with tools/avalanche_regions.py.",
        "features": features,
    }, separators=(",", ":")))
    print(f"{len(features)} micro-regions written to {TARGET}")


if __name__ == "__main__":
    main()
