#!/usr/bin/env python3
"""CG-112: fetch Lombardy's via ferratas from OpenStreetMap into data/ferrate.json, and the list
of what is missing on OSM into docs/ferrate-todo.md.

A via ferrata is never a crag: it has its own file, and data/crags.json's rule against single
routes holds. OSM draws most of them as loose highway=via_ferrata ways, rarely as a
route=via_ferrata relation (CG-111: 74 ways, 5 relations), so the pieces are grouped here:

- a relation is one via ferrata, made of its via_ferrata ways;
- loose ways with the same name join when an end of one lies within NAME_M of an end of the
  other — two routes of the same name farther apart stay two;
- a way without a name joins a piece whose end it touches (TOUCH_M), never bridging two
  different names;
- a group with no name at all is left out and listed for a name on OSM.

The difficulty is via_ferrata_scale, 0 to 6 as OSM documents it; anything else ("2+", "K2") is
dropped and listed for an edit, never read as a number near it. Start and top come from the
track with the Copernicus DEM: the lowest end and the highest point sampled along it.
Data © OpenStreetMap contributors, ODbL 1.0.

    python3 tools/osm_ferrate.py                     # rewrites both files
    python3 tools/osm_ferrate.py --from answer.json  # from a saved Overpass answer (same query)
"""
import json
import pathlib
import re
import sys
import time
import unicodedata

import osm_crags as o

QUERY = """[out:json][timeout:120];
area["ISO3166-2"="IT-25"]->.region;
way["highway"="via_ferrata"](area.region);
out tags geom;
relation["route"="via_ferrata"](area.region);
out body;"""
ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "data" / "ferrate.json"
TODO = ROOT / "docs" / "ferrate-todo.md"
NAME_M = 500          # pieces of one named route, split by a stretch of path between cables
TOUCH_M = 25          # an unnamed piece drawn onto a named one: the ends meet
SAMPLES = 12          # DEM points per via ferrata, ends included: a 90 m model needs no more
PAUSE_S = 3           # between DEM requests: Open-Meteo answers 429 to a burst
SCALE = re.compile(r"^[0-6]$")


def norm(name):
    """For comparing names: lower case, no accents, single spaces."""
    folded = "".join(ch for ch in unicodedata.normalize("NFD", name or "") if unicodedata.category(ch) != "Mn")
    return " ".join(folded.lower().split())


def point(g):
    return {"latitude": g["lat"], "longitude": g["lon"]}


def ends(way):
    return [point(way["geometry"][0]), point(way["geometry"][-1])]


def apart(a, b):
    """The shortest distance in metres between an end of way a and an end of way b."""
    return min(o.metres(p, q) for p in ends(a) for q in ends(b))


def length(way):
    g = [point(p) for p in way["geometry"]]
    return sum(o.metres(g[i], g[i + 1]) for i in range(len(g) - 1))


def group(elements):
    """The via ferratas in an Overpass answer: a list of (relation or None, [ways])."""
    ways = {e["id"]: e for e in elements if e["type"] == "way" and len(e.get("geometry", [])) >= 2}
    rels = [e for e in elements if e["type"] == "relation"]
    groups, used = [], set()
    for r in rels:
        members = [ways[m["ref"]] for m in r.get("members", []) if m["type"] == "way" and m["ref"] in ways]
        if members:
            groups.append((r, members))
            used.update(w["id"] for w in members)
        else:
            print(f"  relation {r['id']}: no via_ferrata way among its members, left out")
    loose = [w for w in ways.values() if w["id"] not in used]
    loose.sort(key=lambda w: w["id"])
    parent = {w["id"]: w["id"] for w in loose}
    names = {w["id"]: {norm(w["tags"]["name"])} if w.get("tags", {}).get("name") else set() for w in loose}

    def find(i):
        while parent[i] != i:
            parent[i] = parent[parent[i]]
            i = parent[i]
        return i

    def join(a, b):
        ra, rb = find(a), find(b)
        if ra == rb or (names[ra] and names[rb] and names[ra] != names[rb]):
            return                                      # never two names in one via ferrata
        parent[rb] = ra
        names[ra] |= names[rb]

    for i, a in enumerate(loose):
        for b in loose[i + 1:]:
            na, nb = a.get("tags", {}).get("name"), b.get("tags", {}).get("name")
            d = apart(a, b)
            if (na and nb and norm(na) == norm(nb) and d <= NAME_M) or ((not na or not nb) and d <= TOUCH_M):
                join(a["id"], b["id"])
    by_root = {}
    for w in loose:
        by_root.setdefault(find(w["id"]), []).append(w)
    groups.extend((None, members) for members in by_root.values())
    return groups


def scales(rel, members):
    """The valid 0–6 values, and the others as written, over the relation and its pieces."""
    values = [e.get("tags", {}).get("via_ferrata_scale", "").strip() for e in ([rel] if rel else []) + members]
    good = sorted({int(v) for v in values if SCALE.match(v)})
    bad = sorted({v for v in values if v and not SCALE.match(v)})
    return good, bad


def name_of(rel, members):
    if rel and rel.get("tags", {}).get("name"):
        return rel["tags"]["name"]
    named = [w for w in members if w.get("tags", {}).get("name")]
    return max(named, key=length)["tags"]["name"] if named else None


def samples(members):
    """Points along the track for the DEM: every end, then evenly spaced points, at most SAMPLES."""
    pts = [p for w in members for p in ends(w)]
    track = [point(g) for w in members for g in w["geometry"]]
    room = max(SAMPLES - len(pts), 0)
    step = max(len(track) // room, 1) if room else len(track) + 1
    pts += track[::step][:room]
    return pts


def ferrata(rel, members, heights):
    """One via ferrata as data/ferrate.json writes it; heights: elevation by sampled point."""
    good, bad = scales(rel, members)
    first = rel if rel else min(members, key=lambda w: w["id"])
    kind = "relation" if rel else "way"
    pts = samples(members)
    tail = [(p, heights[(p["latitude"], p["longitude"])]) for p in pts]
    start = min(tail[:2 * len(members)], key=lambda t: t[1])
    top = max(tail, key=lambda t: t[1])
    at = lambda t: {"latitude": round(t[0]["latitude"], 6), "longitude": round(t[0]["longitude"], 6), "elevation": t[1]}
    return {
        "id": f"osm-{kind}-{first['id']}",
        "name": name_of(rel, members),
        "sourceUrl": f"https://www.openstreetmap.org/{kind}/{first['id']}",
        "pieces": [f"https://www.openstreetmap.org/way/{w['id']}" for w in sorted(members, key=lambda w: w["id"])],
        "start": at(start),
        "top": at(top),
        "lengthM": round(sum(length(w) for w in members)),
        "scaleMin": good[0] if good else None,
        "scaleMax": good[-1] if good else None,
        "scaleDropped": bad,
    }


def dem(points):
    """Elevations for the points, 100 per request (o.elevations), pausing between requests."""
    out = []
    for i in range(0, len(points), 100):
        if i:
            time.sleep(PAUSE_S)
        out.extend(o.elevations(points[i:i + 100]))
    return out


def build(elements, elevations=None):
    """(via ferratas, unnamed groups) from an Overpass answer, the DEM asked once for all points."""
    elevations = elevations or dem
    groups = group(elements)
    pts = [p for _, members in groups for p in samples(members)]
    unique = list({(p["latitude"], p["longitude"]): p for p in pts}.values())
    heights = dict(zip(((p["latitude"], p["longitude"]) for p in unique), elevations(unique))) if unique else {}
    made = [ferrata(r, m, heights) for r, m in groups]
    named = sorted((f for f in made if f["name"]), key=lambda f: (norm(f["name"]), f["id"]))
    return named, [f for f in made if not f["name"]]


def edit(url):
    m = re.search(r"openstreetmap\.org/(node|way|relation)/(\d+)", url)
    return f"https://www.openstreetmap.org/edit?{m.group(1)}={m.group(2)}"


def todo(ferrate, unnamed):
    """docs/ferrate-todo.md: what to map on OSM, first-hand, for the via ferratas to read well."""
    no_scale = [f for f in ferrate if f["scaleMin"] is None]
    lines = [
        "# Via ferratas to map on OpenStreetMap (CG-112)",
        "",
        "Generated by `tools/osm_ferrate.py` from OpenStreetMap — do not edit by hand.",
        "",
        f"**{len(ferrate) - len(no_scale)} of {len(ferrate)} via ferratas** have a difficulty"
        " (`via_ferrata_scale`, 0 to 6). Without it cragcast says the difficulty is not mapped;"
        " it never guesses one.",
        "",
        "## How to add it",
        "",
        "Only for a via ferrata you have done yourself — OpenStreetMap does not accept data copied",
        "from guidebooks or websites under copyright.",
        "",
        "1. Open **edit** next to the via ferrata (log in to OpenStreetMap; the iD editor opens on it).",
        "2. Add `via_ferrata_scale` with a whole number from `0` (a protected path) to `6` (the",
        "   hardest): no `+`, no other scale's letters.",
        "3. A via ferrata drawn in pieces: the tag on each piece, or a `route=via_ferrata` relation",
        "   holding them, named once.",
        "",
        "The change reaches cragcast with the monthly refresh (`crags-refresh.yml`, on the 1st).",
        "",
    ]

    def section(title, rows, note=lambda f: ""):
        if not rows:
            return
        lines.extend([f"## {title} ({len(rows)})", ""])
        for f in rows:
            label = f["name"] or "senza nome"
            lines.append(f"- {label}{note(f)} — [OSM]({f['sourceUrl']}) · [edit]({edit(f['sourceUrl'])})")
        lines.append("")

    section("Difficulty not mapped", no_scale,
            lambda f: f" (`{'`, `'.join(f['scaleDropped'])}` is not on the 0–6 scale)" if f["scaleDropped"] else "")
    section("Difficulty mapped, with a value not on the scale", [f for f in ferrate if f["scaleMin"] is not None and f["scaleDropped"]],
            lambda f: f" (`{'`, `'.join(f['scaleDropped'])}` dropped)")
    section("No name: left out of cragcast until it has one", unnamed)
    return "\n".join(lines).rstrip() + "\n"


def main():
    args = sys.argv[1:]
    if len(args) == 2 and args[0] == "--from":
        elements = json.loads(pathlib.Path(args[1]).read_text())["elements"]
    else:
        elements = o.overpass(QUERY)
    ferrate, unnamed = build(elements)
    if not ferrate:
        raise SystemExit("no via ferratas in the answer: data/ferrate.json left as it was")
    OUT.write_text(json.dumps({
        "region": "Lombardia",
        "source": "OpenStreetMap: highway=via_ferrata ways and route=via_ferrata relations (CG-112)",
        "licence": "© OpenStreetMap contributors, ODbL 1.0 — https://www.openstreetmap.org/copyright",
        "elevationSource": o.ELEVATION_SOURCE,
        "ferrate": ferrate,
    }, ensure_ascii=False, indent=2) + "\n")
    TODO.write_text(todo(ferrate, unnamed))
    scaled = sum(1 for f in ferrate if f["scaleMin"] is not None)
    print(f"{len(ferrate)} via ferratas written to {OUT.relative_to(ROOT)}: {scaled} with a difficulty, "
          f"{len(unnamed)} unnamed left out")


if __name__ == "__main__":
    main()
