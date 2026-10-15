#!/usr/bin/env python3
"""Summarise what changed between two versions of data/crags.json, as Markdown for a PR body.

    python3 tools/crags_diff.py old.json new.json
"""
import json
import sys


def load(path):
    return {c["id"]: c for c in json.load(open(path))["crags"]}


def main(old_path, new_path):
    old, new = load(old_path), load(new_path)
    added = sorted((new[i]["name"], i) for i in new.keys() - old.keys())
    # A crag that left the list because tools/osm_crags.py folded it into a same-name twin is a
    # merge, not a removal: its OSM link lives on in the twin's alsoMapped.
    folded = {u: c for c in new.values() for u in c.get("alsoMapped", [])}
    merged = sorted((old[i]["name"], i, folded[old[i]["sourceUrl"]]["id"])
                    for i in old.keys() - new.keys() if old[i]["sourceUrl"] in folded)
    removed = sorted((old[i]["name"], i) for i in old.keys() - new.keys() if old[i]["sourceUrl"] not in folded)
    lines = [f"**{len(new)} crags** (was {len(old)}).", ""]
    if added:
        lines += ["**Added**", ""] + [f"- {n} — `{i}`" for n, i in added] + [""]
    if merged:
        lines += ["**Merged** (same name, under 50 m)", ""] + [f"- {n} — `{i}` into `{t}`" for n, i, t in merged] + [""]
    if removed:
        lines += ["**Removed from OSM**", ""] + [f"- {n} — `{i}`" for n, i in removed] + [""]
    changes = []
    for i in sorted(old.keys() & new.keys(), key=lambda k: new[k]["name"]):
        o, n = old[i], new[i]
        what = []
        if o.get("aspect") != n.get("aspect"):
            what.append(f"aspect {o.get('aspect') or '—'} → {n.get('aspect') or '—'}")
        if o.get("name") != n.get("name"):
            what.append(f"name was {o['name']}")
        for k in ("municipality", "province"):
            if o.get(k) != n.get(k):
                what.append(f"{k} {o.get(k) or '—'} → {n.get(k) or '—'}")
        if (o["latitude"], o["longitude"]) != (n["latitude"], n["longitude"]):
            what.append("moved")
        od, nd = o.get("details", {}), n.get("details", {})
        for k in sorted(od.keys() | nd.keys()):
            if od.get(k) != nd.get(k):
                what.append(f"{k} {od.get(k, '—')} → {nd.get(k, '—')}")
        if what:
            changes.append(f"- {n['name']}: " + "; ".join(what))
    if changes:
        lines += ["**Changed**", ""] + changes + [""]
    if not (added or merged or removed or changes):
        lines.append("Only elevations or formatting differ.")
    print("\n".join(lines))


if __name__ == "__main__":
    main(*sys.argv[1:3])
