#!/usr/bin/env python3
"""Fetch the climbing crags of Lombardy and (CG-43) Piedmont and Trentino-Alto Adige from
OpenStreetMap into data/crags.json.

Two kinds of OSM object become crags: climbing=crag, and (CG-42) named outdoor places tagged
only sport=climbing — a cliff, a rock, a climbing site. Gyms, shops, buildings and single routes
are left out; the sectors of one crag, mapped as separate cliffs or loose points, are folded
into it (see group()) and listed by OSM id in its "sectors" (CG-104).

Every crag keeps its OSM id and a link to it, so a position can always be traced to its source.
The wall's aspect comes from the OSM tag climbing:orientation when someone has mapped it, and
is null otherwise: never guessed. Data © OpenStreetMap contributors, ODbL 1.0.

    python3 tools/osm_crags.py                  # rewrites data/crags.json
    python3 tools/osm_crags.py --from answer.json   # from a saved Overpass answer
    python3 tools/osm_crags.py --objects-only       # data/osm-climbing.json only (CG-103)
"""
import json
import math
import re
import sys
import pathlib
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request

# Public Overpass servers are often busy (504): try each, a few times.
OVERPASS = ["https://overpass-api.de/api/interpreter", "https://overpass.kumi.systems/api/interpreter"]
# CG-43: Lombardy, and the neighbouring Italian regions Lombard climbers go to: Piedmont and
# Trentino-Alto Adige. Each crag says which (areas(), admin_level 4), as it says its province.
REGIONS = ("IT-25", "IT-21", "IT-32")
QUERY = """[out:json][timeout:300];
area["ISO3166-2"~"^(""" + "|".join(REGIONS) + """)$"]->.region;
(nwr["climbing"="crag"](area.region);
 nwr["sport"="climbing"]["climbing"!="crag"](area.region);)->.found;
.found out center tags;
rel.found;
out body;"""                     # CG-105: the relations again, with their members (see with_members())
REGION_NAMES = "Lombardia, Piemonte, Trentino-Alto Adige"
ASPECTS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"}
OUT = pathlib.Path(__file__).resolve().parent.parent / "data" / "crags.json"
# CG-103: every object of the same query, whole — crags, sectors, routes, gyms — for the database.
OBJECTS = OUT.with_name("osm-climbing.json")
ELEVATION = "https://api.open-meteo.com/v1/elevation"
ELEVATION_SOURCE = ("Copernicus DEM GLO-90 (2021 release, 90 m), https://doi.org/10.5270/ESA-c5d3d65, "
                    "through the Open-Meteo elevation API")


def fetch():
    return with_members(overpass(QUERY))


def with_members(elements):
    """The query answers each relation twice, with its centre and then with its members (CG-105):
    one element each, the centre and the members together."""
    seen, out = {}, []
    for e in elements:
        k = (e["type"], e["id"])
        if k in seen:
            if "members" in e:
                seen[k].setdefault("members", e["members"])
            continue
        seen[k] = e
        out.append(e)
    return out


def overpass(query):
    """POST a query to the public Overpass servers, retrying while they are busy; never accept
    an empty answer as data."""
    body = urllib.parse.urlencode({"data": query}).encode()
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
                if isinstance(e, urllib.error.HTTPError):
                    e.close()                         # an error answer holds its connection open
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
    # The first web address that is one: a malformed website tag does not hide a good url tag.
    site = next((v for v in (tags.get(k, "").strip() for k in ("website", "url", "contact:website"))
                 if v.startswith(("http://", "https://"))), None)
    if site:
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
        if c.get("_kind") == "shape":            # a cliff drawn as well as a point: it is a shape
            twin["_kind"] = "shape"
        for k, v in c["details"].items():
            twin["details"].setdefault(k, v)
    return sorted(out, key=lambda c: c["name"].lower())


def areas(crags, batch=40):
    """Municipality, province and (CG-43) region of every crag, from OSM's administrative
    boundaries (levels 8, 6 and 4): one Overpass query per batch, each crag announced by a `make crag` marker so the
    answer can be matched back even when a point falls outside every boundary."""
    found = {}
    for i in range(0, len(crags), batch):
        part = crags[i:i + batch]
        q = "[out:json][timeout:120];\n"
        for c in part:
            q += (f'make crag ref="{c["id"]}";out;\n'
                  f'is_in({c["latitude"]},{c["longitude"]})->.a;\n'
                  'area.a["boundary"="administrative"]["admin_level"~"^(4|6|8)$"];out tags;\n')
        # A busy server can answer 200 with the answer cut short: every crag of the batch must
        # come back with its marker, or the batch is asked again — never a silent gap. Every
        # crag lies inside one of the regions, so a batch with markers and no boundary at all is
        # a server answering without its area index (40 crags lost their place on 2026-10-29).
        for attempt in range(3):
            answer = overpass(q)
            refs = {e["tags"]["ref"] for e in answer if e["type"] == "crag"}
            bounds = sum(1 for e in answer if e["type"] == "area")
            if all(c["id"] in refs for c in part) and bounds:
                break
            print(f"  areas: {len(refs)} of {len(part)} crags and {bounds} boundaries in the answer; asking again")
            time.sleep(30 * (attempt + 1))
        else:
            raise SystemExit("areas: Overpass keeps cutting the answer short; data/crags.json left as it was")
        current = None
        for e in answer:
            tags = e.get("tags", {})
            if e["type"] == "crag":
                current = found.setdefault(tags["ref"], {})
            elif current is not None and tags.get("admin_level") == "8":
                current["municipality"] = tags.get("name:it") or tags.get("name")
            elif current is not None and tags.get("admin_level") == "4":
                current["region"] = tags.get("name:it") or tags.get("name")
            elif current is not None and tags.get("admin_level") == "6":
                current["province"] = tags.get("name:it") or tags.get("name")
                code = tags.get("ref") or tags.get("short_name")
                if code and re.fullmatch(r"[A-Z]{2}", code):
                    current["provinceCode"] = code
    return found


ELEVATION_PAUSE_S = 3          # CG-43: ten requests in a row drew a 429 from Open-Meteo


def elevations(crags):
    """Ground elevation in metres for every crag, 100 coordinates per request (the API's limit),
    with a pause between requests; a 429, or a connection that fails or times out, is asked again
    after a longer one.
    A 90 m model: the altitude of the area, not of a given route."""
    out = []
    for i in range(0, len(crags), 100):
        if i:
            time.sleep(ELEVATION_PAUSE_S)
        batch = crags[i:i + 100]
        query = urllib.parse.urlencode({
            "latitude": ",".join(str(c["latitude"]) for c in batch),
            "longitude": ",".join(str(c["longitude"]) for c in batch),
        })
        req = urllib.request.Request(f"{ELEVATION}?{query}",
                                     headers={"User-Agent": "cragcast (github.com/Allan-Nava/cragcast)"})
        for attempt in range(4):
            try:
                with urllib.request.urlopen(req, timeout=60) as r:
                    values = json.load(r)["elevation"]
                break
            except urllib.error.HTTPError as e:
                e.close()
                if e.code != 429 or attempt == 3:
                    raise
                print(f"  elevation: 429, waiting {30 * (attempt + 1)} s")
                time.sleep(30 * (attempt + 1))
            except (urllib.error.URLError, TimeoutError) as e:
                # A TLS handshake that timed out ended the refresh of 2026-10-29: the network, not
                # the answer — asked again like a 429.
                if attempt == 3:
                    raise
                print(f"  elevation: {e}, waiting {30 * (attempt + 1)} s")
                time.sleep(30 * (attempt + 1))
        if len(values) != len(batch):
            raise SystemExit(f"elevation API returned {len(values)} values for {len(batch)} crags")
        out.extend(round(v) for v in values)
    return out


# CG-42: which sport=climbing objects are outdoor crags.
NATURAL = {"cliff", "bare_rock", "rock", "stone"}
NOT_A_CRAG = ("leisure", "shop", "building", "amenity", "office", "club")
ROUTE_PARTS = {"route", "route_bottom", "route_top", "anchor", "abseil_route", "summit_log"}
# A loose point far from any crag counts only when its name says it is one.
# CG-43: "Klettergarten", the German word for an outdoor crag in Alto Adige.
CRAG_WORD = re.compile(r"falesi|palestra di roccia|parete|placc|sasso|corna|torrion|muro|grotta|rocca|klettergarten",
                       re.IGNORECASE)
# A name that is only a sector's label says nothing on its own: "Settore Alto", "Secondo Settore".
SECTOR_ONLY = re.compile(r"^((primo|secondo|terzo|quarto)\s+)?settore\b", re.IGNORECASE)
SAME_PLACE_M = 50       # two shapes this close are one crag drawn twice, whatever their names
SECTOR_M = 300          # a loose point this close to a crag is one of its sectors
GROUP_M = 2000          # sectors sharing a base name this close form one crag


def kind(e):
    """'crag' (climbing=crag), 'shape' (a cliff, rock or site with sport=climbing), 'point' (a
    named node with nothing but sport=climbing), or None (not a crag)."""
    t = e.get("tags", {})
    if not t.get("name"):
        return None
    if t.get("climbing") == "crag":
        return "crag"
    if t.get("sport") != "climbing" or any(k in t for k in NOT_A_CRAG) or t.get("indoor") == "yes":
        return None
    if t.get("climbing") in ROUTE_PARTS:
        return None
    # type=site / single_rock only means something on a relation; on a node it is a loose point.
    if (t.get("natural") in NATURAL or t.get("climbing") == "area"
            or (e["type"] == "relation" and t.get("type") in ("site", "single_rock"))):
        return "shape"
    if e["type"] == "node" and t.get("natural") not in NATURAL:
        return "point"
    return None


FILLER = {"la", "il", "lo", "le", "i", "gli", "l", "falesia", "falesie", "di", "del", "della", "dei", "delle", "d"}
SUFFIX = re.compile(r"\s+-\s+.*$|\s*\(.*\)\s*$|,\s.*$|\s+settore\b.*$", re.IGNORECASE)


def base(name):
    """The crag a sector belongs to: "Falesia Maddalena - Settore A" -> "Falesia Maddalena"."""
    return SUFFIX.sub("", name).strip() or name.strip()


def key(name):
    """Base name for comparing: lower case, no accents, without articles, "falesia" and "di/del…":
    "Falesia del Sasso Pelo" and "Falesia Sasso Pelo" compare equal."""
    folded = "".join(ch for ch in unicodedata.normalize("NFD", base(name)) if unicodedata.category(ch) != "Mn")
    words = [w for w in re.split(r"[^a-z0-9]+", folded.lower()) if w and w not in FILLER]
    return " ".join(words) or folded.lower().strip()


def grade_order(g):
    """5c < 6a < 6a+: number, letter, plus."""
    m = re.match(r"(\d)([abc]?)(\+?)", g)
    return (int(m.group(1)), m.group(2), m.group(3)) if m else (0, "", "")


# What a sector says that holds for the whole crag; the rest (routes, mean grade, one length)
# describes that sector only.
CRAG_WIDE = ("rock", "rockIt", "styles", "bolted", "website")


def absorb(into, other, sector=False):
    """Fold one OSM object into a crag: its link to alsoMapped, and its details. The same place
    drawn twice (sector=False) fills what is missing. A different sector (sector=True) widens the
    grade and length ranges and adds only what holds crag-wide — or, into a climbing=crag, only
    the link and what holds crag-wide where missing. Sectors can face different ways,
    so the aspect is kept only while every part that has one agrees (settled in group())."""
    into.setdefault("alsoMapped", []).append(other["sourceUrl"])
    into["alsoMapped"].extend(other.pop("alsoMapped", []))
    if sector:
        # CG-104: the sector's own OSM object, kept by id so the database can list it apart.
        into.setdefault("_sectors", []).append(other["id"])
        into["_sectors"].extend(other.pop("_sectors", []))
    if sector and into.get("_kind") == "crag":
        # A climbing=crag is a crag in its own right: a sector found around it adds its link
        # and what holds crag-wide where missing, never its grades, counts or aspect.
        other.pop("_aspects", None)
        for k in CRAG_WIDE:
            if k in other["details"]:
                into["details"].setdefault(k, other["details"][k])
        return
    into.setdefault("_aspects", {into["aspect"]} - {None})
    into["_aspects"] |= other.pop("_aspects", {other["aspect"]} - {None})
    d, o = into["details"], other["details"]
    if not sector:
        for k, v in o.items():
            d.setdefault(k, v)
        return
    if into.get("_sector_parts") is None:              # first sector folded in: keep the crag-wide part only
        into["_sector_parts"] = True
        for k in ("routes", "gradeMean", "length"):
            d.pop(k, None)
    for k, pick in (("gradeMin", min), ("gradeMax", max)):
        if k in o:
            d[k] = pick(filter(None, (d.get(k), o[k])), key=grade_order)
    for k, pick in (("lengthMin", min), ("lengthMax", max)):
        if k in o:
            d[k] = pick(filter(lambda x: x is not None, (d.get(k), o[k])))
    for k in CRAG_WIDE:
        if k in o:
            d.setdefault(k, o[k])


def rank(c):
    """Which object of a group keeps its id: a climbing=crag first (its id is already public),
    then a relation (an umbrella over sectors), then the one named like the group, then a way."""
    return (c["_kind"] != "crag", not c["id"].startswith("osm-relation"), c["name"] != base(c["name"]),
            KIND[c["id"].split("-")[1]] * -1, c["id"])


def group(items):
    """CG-42: crags from climbing=crag and from sport=climbing shapes and points, sectors folded.
    1. A shape within SAME_PLACE_M of a crag or another shape is the same crag drawn twice.
    2. Crags and shapes sharing a base name within GROUP_M are sectors of one crag, named by
       the base name when the one kept carries a sector suffix.
    3. A point sharing a crag's base name within GROUP_M, or any point within SECTOR_M of a
       crag, is part of it; a point farther from all counts
       only when its name says crag (CRAG_WORD).
    A crag gathered from parts takes their base name; a shape or point named only as a sector
    ("Settore Alto") is left out. What is left out is returned, for the run's log."""
    crags = [c for c in items if c["_kind"] == "crag"]
    shapes = [c for c in items if c["_kind"] == "shape"]
    points = [c for c in items if c["_kind"] == "point"]
    kept = list(crags)
    for s in sorted(shapes, key=rank):
        near = min(kept, key=lambda k: metres(k, s), default=None)
        # Same base, different sector ("… - Settore Alto" by "… - Settore Basso"): siblings, not
        # one place drawn twice — step 2 decides.
        siblings = (near is not None and key(near["name"]) == key(s["name"]) and near["name"] != s["name"]
                    and near["name"] != base(near["name"]) and s["name"] != base(s["name"]))
        if near is not None and metres(near, s) < SAME_PLACE_M and not siblings:
            absorb(near, s)
        else:
            kept.append(s)
    out = []
    for c in sorted(kept, key=rank):
        # Two climbing=crag are never grouped: generic names ("Settore Destro") recur on
        # different crags, and both ids are already public. Nor is anything grouped into a
        # climbing=crag that is itself one sector ("… - Settore Basso"): its siblings stay apart.
        head = next((o for o in out if key(o["name"]) == key(c["name"]) and metres(o, c) < GROUP_M
                     and not (o["_kind"] == "crag" and c["_kind"] == "crag")
                     and not (o["_kind"] == "crag" and o["name"] != base(o["name"]))), None)
        if head is None:
            out.append(c)
            continue
        absorb(head, c, sector=True)
    dropped = [c["name"] for c in out if c["_kind"] != "crag" and SECTOR_ONLY.match(c["name"])]
    out = [c for c in out if c["_kind"] == "crag" or not SECTOR_ONLY.match(c["name"])]
    for p in points:
        near = min(out, key=lambda k: metres(k, p), default=None)
        named = next((k for k in out if key(k["name"]) == key(p["name"]) and metres(k, p) < GROUP_M), None)
        if named is not None:
            absorb(named, p, sector=True)
        elif near is not None and metres(near, p) < SECTOR_M:
            absorb(near, p, sector=True)
        elif CRAG_WORD.search(p["name"]) and not SECTOR_ONLY.match(p["name"]):
            out.append(p)
        else:
            dropped.append(p["name"])
    for c in out:
        # CG-104: the sectors folded in, by OSM id. A crag gathered from sectors kept the id of
        # one of them ("… - Settore A"): that one is a sector too, and listed first.
        sectors = c.pop("_sectors", [])
        if sectors and c["_kind"] != "crag" and c["name"] != base(c["name"]):
            sectors.insert(0, c["id"])
        if sectors:
            c["sectors"] = sectors
        # A crag gathered from sectors takes their common name: "Falesia Maddalena", not the
        # name of the sector whose id it kept. A climbing=crag keeps its own name.
        if c["_kind"] != "crag" and c.get("alsoMapped"):
            c["name"] = base(c["name"])
        c.pop("_sector_parts", None)
        aspects = c.pop("_aspects", None)
        if aspects is not None:
            c["aspect"] = next(iter(aspects)) if len(aspects) == 1 else None
        c.pop("_kind", None)
        if not c.get("alsoMapped"):
            c.pop("alsoMapped", None)
    return out, dropped


def category(e):
    """CG-103: what an object of the query is, for data/osm-climbing.json. 'crag', 'cliff' and
    'point' are kind()'s (the places a crag can come from), 'route' a single route or one of its
    parts, 'venue' a gym, shop or building, 'other' anything else (often a nameless cliff)."""
    t = e.get("tags", {})
    if t.get("climbing") in ROUTE_PARTS:
        return "route"
    if any(k in t for k in NOT_A_CRAG) or t.get("indoor") == "yes":
        return "venue"
    return {"crag": "crag", "shape": "cliff", "point": "point"}.get(kind(e), "other")


def osm_object(e):
    """One object as data/osm-climbing.json holds it: OSM's type and id, the category, the name and
    position, the raw climbing tag, and the climbing tags validated as for crags (plus a route's
    own French grade). None for an object without a position."""
    t = e.get("tags", {})
    pos = e if e["type"] == "node" else e.get("center")
    if not pos:
        return None
    aspect = t.get("climbing:orientation", "").strip().upper()
    d = details(t)
    grade = t.get("climbing:grade:french", "").strip().lower()
    if GRADE.match(grade):
        d["grade"] = grade
    if category(e) == "route":
        # CG-105: what a single route says of itself, beyond the crag's tags.
        for key, name in (("climbing:bolts", "bolts"), ("climbing:pitches", "pitches")):
            n = number(t.get(key))
            if n is not None:
                d[name] = n
    return {
        "id": f"osm-{e['type']}-{e['id']}",
        "osmType": e["type"],
        "osmId": e["id"],
        "category": category(e),
        "name": t.get("name"),
        "climbing": t.get("climbing"),
        "latitude": round(pos["lat"], 6),
        "longitude": round(pos["lon"], 6),
        "aspect": aspect if aspect in ASPECTS else None,
        "details": d,
    }


# CG-105: a single route, not one of its parts (an anchor, an abseil), is linked to a crag.
ROUTES = {"route", "route_bottom", "route_top"}
ROUTE_M = 400           # a route this close to a crag or sector belongs to it (none on 2026-10-22
                        # lay between 343 m and 788 m: the farther ones are on mountains, not crags)


def osm_id(url):
    """https://www.openstreetmap.org/node/123 -> osm-node-123."""
    kind, n = url.rstrip("/").split("/")[-2:]
    return f"osm-{kind}-{n}"


def link_routes(elements, objects, crags):
    """CG-105: each single route of the objects gets the crag it belongs to ("crag"), the sector
    when there is one ("sector"), and how that was found ("linkedBy"): "relation" when it is a
    member of an OSM relation that is the crag, one of its sectors or a duplicate folded into
    it; else "distance", the nearest crag or sector within ROUTE_M; else all three null."""
    by_id = {o["id"]: o for o in objects}
    owner, places = {}, []
    for c in crags:
        for u in c.get("alsoMapped", []):
            owner[osm_id(u)] = (c["id"], None)
        owner[c["id"]] = (c["id"], None)
        if c["id"] not in c.get("sectors", []):
            places.append((c, (c["id"], None)))
        for s in c.get("sectors", []):           # after alsoMapped: a sector's link is there too
            owner[s] = (c["id"], s)
            if s in by_id:
                places.append((by_id[s], (c["id"], s)))
    members = {}
    for e in elements:
        rid = f"osm-{e['type']}-{e['id']}"
        for m in e.get("members", []) if rid in owner else []:
            members.setdefault(f"osm-{m['type']}-{m['ref']}", owner[rid])
    for o in objects:
        if o["category"] != "route" or o["climbing"] not in ROUTES:
            continue
        link, by = members.get(o["id"]), "relation"
        if link is None:
            near = min(places, key=lambda p: metres(p[0], o), default=None)
            link, by = (near[1], "distance") if near and metres(near[0], o) < ROUTE_M else ((None, None), None)
        o.update(crag=link[0], sector=link[1], linkedBy=by)
    return objects


def write_objects(elements, crags=()):
    """data/osm-climbing.json: every object of the answer, by id, its single routes linked to the
    crags (CG-105); ODbL like crags.json."""
    objects = link_routes(elements, sorted(filter(None, map(osm_object, elements)), key=lambda o: o["id"]), crags)
    OBJECTS.write_text(json.dumps({
        "region": REGION_NAMES,
        "source": "OpenStreetMap: every climbing=crag and sport=climbing object of tools/osm_crags.py's query (CG-103), "
                  "single routes linked to their crag (CG-105)",
        "licence": "© OpenStreetMap contributors, ODbL 1.0 — https://www.openstreetmap.org/copyright",
        "objects": objects,
    }, ensure_ascii=False, indent=1) + "\n")
    counts = {}
    for o in objects:
        counts[o["category"]] = counts.get(o["category"], 0) + 1
    linked = [o["linkedBy"] for o in objects if "linkedBy" in o]
    print(f"{len(objects)} OSM objects written to {OBJECTS.relative_to(OBJECTS.parent.parent)}: "
          + ", ".join(f"{n} {c}" for c, n in sorted(counts.items()))
          + f"; of {len(linked)} single routes, {linked.count('relation')} linked by relation, "
            f"{linked.count('distance')} by distance")
    return objects


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
        "_kind": kind(e),
    }


def main():
    # --from FILE: build from an Overpass JSON answer saved earlier (same query) when the
    # public servers are down. --objects-only: write data/osm-climbing.json alone (CG-103),
    # without the elevation and municipality lookups or a new crags.json.
    args = sys.argv[1:]
    only_objects = "--objects-only" in args
    args = [a for a in args if a != "--objects-only"]
    if len(args) == 2 and args[0] == "--from":
        elements = with_members(json.loads(pathlib.Path(args[1]).read_text())["elements"])
    else:
        elements = fetch()
    items = [c for c in filter(None, map(crag, elements)) if c["_kind"]]
    primary = merge_duplicates([c for c in items if c["_kind"] == "crag"])
    rest = merge_duplicates([c for c in items if c["_kind"] != "crag"])
    crags, dropped = group(primary + rest)
    crags.sort(key=lambda c: c["name"].lower())
    if only_objects:
        write_objects(elements, crags)           # the grouping only, for the routes' links
        return
    if dropped:
        print(f"{len(dropped)} sport=climbing places left out (a sector label alone, or a point far from any crag without crag in its name): "
              + ", ".join(sorted(dropped)))
    if not crags:
        raise SystemExit("no crags in the answer: data/crags.json left as it was")
    for c, ele in zip(crags, elevations(crags)):
        c["elevation"] = ele
    where = areas(crags)
    for c in crags:
        c.update(where.get(c["id"], {}))
    write_objects(elements, crags)
    OUT.write_text(json.dumps({
        "region": REGION_NAMES,
        "source": "OpenStreetMap: climbing=crag, and named outdoor sport=climbing places (CG-42)",
        "licence": "© OpenStreetMap contributors, ODbL 1.0 — https://www.openstreetmap.org/copyright",
        "elevationSource": ELEVATION_SOURCE,
        "crags": crags,
    }, ensure_ascii=False, indent=2) + "\n")
    with_aspect = sum(1 for c in crags if c["aspect"])
    with_grades = sum(1 for c in crags if "gradeMin" in c["details"] or "gradeMax" in c["details"])
    with_area = sum(1 for c in crags if c.get("municipality"))
    print(f"{len(crags)} crags written to {OUT.relative_to(OUT.parent.parent)}: "
          f"{with_aspect} with an aspect, {with_grades} with grades, {with_area} with a municipality")
    by_region = {}
    for c in crags:
        by_region[c.get("region") or "?"] = by_region.get(c.get("region") or "?", 0) + 1
    print("by region: " + ", ".join(f"{r} {n}" for r, n in sorted(by_region.items())))


if __name__ == "__main__":
    main()
