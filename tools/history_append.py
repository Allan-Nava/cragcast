#!/usr/bin/env python3
"""Append one day's verdicts to the history: one CSV per month, history/<YYYY>/<MM>.csv.

Reads the conditions.json the export writes and keeps, for every crag, the verdict and the
weather of one day — by default the first day in the file, i.e. today. Running it twice for the
same day replaces that day's rows instead of duplicating them.

    python3 tools/history_append.py site/data/conditions.json <history dir> [YYYY-MM-DD]
"""
import csv
import json
import pathlib
import sys

FIELDS = ["date", "crag_id", "crag_name", "province", "verdict", "rain_last48h_mm",
          "rain_day_mm", "max_temp_c", "max_wind_kmh", "reasons", "generated_at"]


def rows(conditions, day):
    out = []
    for c in conditions["crags"]:
        d = c["days"].get(day)
        if d is None:
            raise SystemExit(f"{day} is not in the conditions file ({', '.join(conditions['days'])})")
        w = d["weather"]
        out.append({
            "date": day, "crag_id": c["id"], "crag_name": c["name"], "province": c.get("provinceCode") or "",
            "verdict": d["verdict"], "rain_last48h_mm": round(w["rainLast48hMm"], 2),
            "rain_day_mm": round(w["rainDuringDayMm"], 2), "max_temp_c": round(w["maxTemperatureC"], 1),
            "max_wind_kmh": round(w["maxWindKmh"], 1), "reasons": "; ".join(d["reasons"]),
            "generated_at": conditions["generatedAt"],
        })
    return sorted(out, key=lambda r: r["crag_name"].lower())


def main(conditions_path, history_dir, day=None):
    conditions = json.loads(pathlib.Path(conditions_path).read_text())
    day = day or conditions["days"][0]
    path = pathlib.Path(history_dir) / day[:4] / f"{day[5:7]}.csv"
    kept = []
    if path.exists():
        with path.open(newline="") as f:
            kept = [r for r in csv.DictReader(f) if r["date"] != day]
    new = rows(conditions, day)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=FIELDS)
        w.writeheader()
        w.writerows(sorted(kept + new, key=lambda r: (r["date"], r["crag_name"].lower())))
    print(f"{len(new)} rows for {day} in {path}")


if __name__ == "__main__":
    main(*sys.argv[1:4])
