#!/usr/bin/env python3
"""Fetch the climbing crags of Lombardy from OpenStreetMap into data/crags.json.

Every crag keeps its OSM id and a link to it, so a position can always be traced to its source.
The wall's aspect comes from the OSM tag climbing:orientation when someone has mapped it, and
is null otherwise: never guessed. Data © OpenStreetMap contributors, ODbL 1.0.

    python3 tools/osm_crags.py                  # rewrites data/crags.json
    python3 tools/osm_crags.py --from answer.json   # from a saved Overpass answer
"""
import json
import sys
import pathlib
import time
import urllib.error
import urllib.parse
import urllib.request

# Public Overpass servers are often busy (504): try each, a few times.
OVERPASS = ["https://overpass-api.de/api/interpreter", "https://overpass.kumi.systems/api/interpreter"]
QUERY = """[out:json][timeout:90];
area["ISO3166-2"="IT-25"]->.region;
(node["climbing"="crag"](area.region);
 way["climbing"="crag"](area.region);
 relation["climbing"="crag"](area.region););
out center tags;"""
ASPECTS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"}
OUT = pathlib.Path(__file__).resolve().parent.parent / "data" / "crags.json"


def fetch():
    body = urllib.parse.urlencode({"data": QUERY}).encode()
    last = None
    for attempt in range(3):
        for url in OVERPASS:
            req = urllib.request.Request(url, data=body,
                                         headers={"User-Agent": "cragcast (github.com/Allan-Nava/cragcast)"})
            try:
                with urllib.request.urlopen(req, timeout=180) as r:
                    elements = json.load(r)["elements"]
                if elements:
                    return elements
                # A server without the regional area index answers 200 with nothing in it:
                # never let that overwrite the data.
                print(f"  {url}: empty answer; trying the next server")
            except (urllib.error.URLError, TimeoutError) as e:
                last = e
                print(f"  {url}: {e}; retrying")
        time.sleep(20 * (attempt + 1))
    raise SystemExit(f"Overpass unavailable: {last}")


def crag(e):
    tags = e.get("tags", {})
    name = tags.get("name")
    pos = e if e["type"] == "node" else e.get("center")
    if not name or not pos:
        return None
    aspect = tags.get("climbing:orientation", "").strip().upper()
    return {
        "id": f"osm-{e['type']}-{e['id']}",
        "name": name,
        "latitude": round(pos["lat"], 6),
        "longitude": round(pos["lon"], 6),
        "aspect": aspect if aspect in ASPECTS else None,
        "sourceUrl": f"https://www.openstreetmap.org/{e['type']}/{e['id']}",
    }


def main():
    # --from FILE: build from an Overpass JSON answer saved earlier (same query) when the
    # public servers are down.
    if len(sys.argv) == 3 and sys.argv[1] == "--from":
        elements = json.loads(pathlib.Path(sys.argv[2]).read_text())["elements"]
    else:
        elements = fetch()
    crags = sorted(filter(None, map(crag, elements)), key=lambda c: c["name"].lower())
    if not crags:
        raise SystemExit("no crags in the answer: data/crags.json left as it was")
    OUT.write_text(json.dumps({
        "region": "Lombardia",
        "source": "OpenStreetMap, climbing=crag",
        "licence": "© OpenStreetMap contributors, ODbL 1.0 — https://www.openstreetmap.org/copyright",
        "crags": crags,
    }, ensure_ascii=False, indent=2) + "\n")
    with_aspect = sum(1 for c in crags if c["aspect"])
    print(f"{len(crags)} crags written to {OUT.relative_to(OUT.parent.parent)}, {with_aspect} with an aspect")


if __name__ == "__main__":
    main()
