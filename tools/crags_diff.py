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
    removed = sorted((old[i]["name"], i) for i in old.keys() - new.keys())
    lines = [f"**{len(new)} crags** (was {len(old)}).", ""]
    if added:
        lines += ["**Added**", ""] + [f"- {n} — `{i}`" for n, i in added] + [""]
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
    if not (added or removed or changes):
        lines.append("Only elevations or formatting differ.")
    print("\n".join(lines))


if __name__ == "__main__":
    main(*sys.argv[1:3])
