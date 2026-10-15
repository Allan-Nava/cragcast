#!/usr/bin/env python3
"""Fetch the climbing crags of Lombardy from OpenStreetMap into data/crags.json.

Every crag keeps its OSM id and a link to it, so a position can always be traced to its source.
The wall's aspect comes from the OSM tag climbing:orientation when someone has mapped it, and
is null otherwise: never guessed. Data © OpenStreetMap contributors, ODbL 1.0.

    python3 tools/osm_crags.py                  # rewrites data/crags.json
    python3 tools/osm_crags.py --from answer.json   # from a saved Overpass answer
"""
import json
import math
import re
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
ELEVATION = "https://api.open-meteo.com/v1/elevation"
ELEVATION_SOURCE = ("Copernicus DEM GLO-90 (2021 release, 90 m), https://doi.org/10.5270/ESA-c5d3d65, "
                    "through the Open-Meteo elevation API")


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


GRADE = re.compile(r"^[1-9][abc]?\+?$")          # French grade: 4, 5c, 6b+, 8a
ROCK_IT = {"limestone": "calcare", "granite": "granito", "gneiss": "gneiss", "sandstone": "arenaria",
           "conglomerate": "conglomerato", "dolomite": "dolomia", "serpentinite": "serpentino",
           "porphyry": "porfido", "quartzite": "quarzite", "schist": "scisto"}


def number(value):
    """A length or count as a number: "30", "30 m", "25.5" -> 30, 30, 25.5; anything else -> None."""
    m = re.fullmatch(r"\s*(\d+(?:[.,]\d+)?)\s*m?\s*", value or "")
    if not m:
        return None
    n = float(m.group(1).replace(",", "."))
    return int(n) if n.is_integer() else n


def details(tags):
    """The climbing tags OSM has for a crag, validated; keys are left out when absent or malformed."""
    d = {}
    for key, name in (("min", "gradeMin"), ("max", "gradeMax"), ("mean", "gradeMean")):
        g = tags.get(f"climbing:grade:french:{key}", "").strip().lower()
        if GRADE.match(g):
            d[name] = g
    for key, name in (("climbing:length", "length"), ("climbing:length:min", "lengthMin"),
                      ("climbing:length:max", "lengthMax"), ("climbing:routes", "routes")):
        n = number(tags.get(key))
        if n is not None:
            d[name] = n
    rock = tags.get("climbing:rock", "").strip().lower()
    if rock:
        d["rock"] = rock
        if rock in ROCK_IT:
            d["rockIt"] = ROCK_IT[rock]
    styles = [s for s in ("sport", "trad", "multipitch") if tags.get(f"climbing:{s}", "no") not in ("no", "")]
    if styles:
        d["styles"] = styles
    if tags.get("climbing:bolted") == "yes":
        d["bolted"] = True
    site = tags.get("website") or tags.get("url") or tags.get("contact:website")
    if site and site.startswith(("http://", "https://")):
        d["website"] = site
    return d


MERGE_WITHIN_M = 50   # same name closer than this: one crag mapped twice (a node and a relation…)
KIND = {"node": 0, "way": 1, "relation": 2}


def metres(a, b):
    r = math.pi / 180
    dlat, dlon = (b["latitude"] - a["latitude"]) * r, (b["longitude"] - a["longitude"]) * r
    h = math.sin(dlat / 2) ** 2 + math.cos(a["latitude"] * r) * math.cos(b["latitude"] * r) * math.sin(dlon / 2) ** 2
    return 2 * 6371000 * math.asin(math.sqrt(h))


def merge_duplicates(crags):
    """Folds crags with the same name less than MERGE_WITHIN_M apart into one. The one kept is the
    node (then way, then relation), whose id stays stable; every other OSM link goes to
    alsoMapped, and missing details or aspect are filled from the duplicates. Same-name crags
    farther apart (generic names like "Settore Destro") are left alone: they may be different crags."""
    out = []
    for c in sorted(crags, key=lambda c: (KIND[c["id"].split("-")[1]], c["id"])):
        twin = next((k for k in out if k["name"].strip().lower() == c["name"].strip().lower()
                     and metres(k, c) < MERGE_WITHIN_M), None)
        if twin is None:
            out.append(c)
            continue
        twin.setdefault("alsoMapped", []).append(c["sourceUrl"])
        twin["aspect"] = twin["aspect"] or c["aspect"]
        for k, v in c["details"].items():
            twin["details"].setdefault(k, v)
    return sorted(out, key=lambda c: c["name"].lower())


def elevations(crags):
    """Ground elevation in metres for every crag, 100 coordinates per request (the API's limit).
    A 90 m model: the altitude of the area, not of a given route."""
    out = []
    for i in range(0, len(crags), 100):
        batch = crags[i:i + 100]
        query = urllib.parse.urlencode({
            "latitude": ",".join(str(c["latitude"]) for c in batch),
            "longitude": ",".join(str(c["longitude"]) for c in batch),
        })
        req = urllib.request.Request(f"{ELEVATION}?{query}",
                                     headers={"User-Agent": "cragcast (github.com/Allan-Nava/cragcast)"})
        with urllib.request.urlopen(req, timeout=60) as r:
            values = json.load(r)["elevation"]
        if len(values) != len(batch):
            raise SystemExit(f"elevation API returned {len(values)} values for {len(batch)} crags")
        out.extend(round(v) for v in values)
    return out


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
        "details": details(tags),
    }


def main():
    # --from FILE: build from an Overpass JSON answer saved earlier (same query) when the
    # public servers are down.
    if len(sys.argv) == 3 and sys.argv[1] == "--from":
        elements = json.loads(pathlib.Path(sys.argv[2]).read_text())["elements"]
    else:
        elements = fetch()
    crags = merge_duplicates(list(filter(None, map(crag, elements))))
    if not crags:
        raise SystemExit("no crags in the answer: data/crags.json left as it was")
    for c, ele in zip(crags, elevations(crags)):
        c["elevation"] = ele
    OUT.write_text(json.dumps({
        "region": "Lombardia",
        "source": "OpenStreetMap, climbing=crag",
        "licence": "© OpenStreetMap contributors, ODbL 1.0 — https://www.openstreetmap.org/copyright",
        "elevationSource": ELEVATION_SOURCE,
        "crags": crags,
    }, ensure_ascii=False, indent=2) + "\n")
    with_aspect = sum(1 for c in crags if c["aspect"])
    with_grades = sum(1 for c in crags if "gradeMin" in c["details"] or "gradeMax" in c["details"])
    print(f"{len(crags)} crags written to {OUT.relative_to(OUT.parent.parent)}: "
          f"{with_aspect} with an aspect, {with_grades} with grades")


if __name__ == "__main__":
    main()
