"""Tests for tools/history_append.py — made-up crags, not real places."""
import contextlib
import csv
import io
import json
import pathlib
import tempfile
import unittest

import history_append


def conditions(days, generated="2026-10-15T08:00:00Z", verdict="GO"):
    w = {"rainLast48hMm": 1.234, "rainDuringDayMm": 0.0, "maxTemperatureC": 18.26, "maxWindKmh": 7.04}
    return {"generatedAt": generated, "days": days, "crags": [
        {"id": f"osm-node-{i}", "name": n, "provinceCode": p,
         "days": {d: {"verdict": verdict, "reasons": ["cold", "strong wind"] if i == 2 else [], "weather": w} for d in days}}
        for i, n, p in ((1, "beta Wall", "LC"), (2, "Alpha Wall", None))]}


class Append(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.dir = pathlib.Path(self.tmp.name)
        self.addCleanup(self.tmp.cleanup)

    def append(self, cond, day=None):
        src = self.dir / "conditions.json"
        src.write_text(json.dumps(cond))
        with contextlib.redirect_stdout(io.StringIO()):
            history_append.main(str(src), str(self.dir / "history"), day)

    def read(self, rel):
        with (self.dir / "history" / rel).open(newline="") as f:
            return list(csv.DictReader(f))

    def test_first_day_by_default_one_file_per_month(self):
        self.append(conditions(["2026-10-15", "2026-10-16"]))
        rows = self.read("2026/10.csv")
        self.assertEqual([r["crag_name"] for r in rows], ["Alpha Wall", "beta Wall"])      # by name, any case
        self.assertEqual(rows[0]["date"], "2026-10-15")
        self.assertEqual(rows[0]["reasons"], "cold; strong wind")
        self.assertEqual((rows[1]["province"], rows[0]["province"]), ("LC", ""))
        self.assertEqual((rows[0]["rain_last48h_mm"], rows[0]["max_temp_c"], rows[0]["max_wind_kmh"]), ("1.23", "18.3", "7.0"))
        self.assertEqual(list(rows[0]), history_append.FIELDS)                            # the columns are a contract

    def test_rerunning_a_day_replaces_its_rows_and_keeps_the_others(self):
        self.append(conditions(["2026-10-14"]))
        self.append(conditions(["2026-10-15"], verdict="GO"))
        self.append(conditions(["2026-10-15"], generated="2026-10-15T20:00:00Z", verdict="NO_GO"))
        rows = self.read("2026/10.csv")
        self.assertEqual([r["date"] for r in rows], ["2026-10-14"] * 2 + ["2026-10-15"] * 2)
        self.assertEqual({r["verdict"] for r in rows if r["date"] == "2026-10-15"}, {"NO_GO"})

    def test_a_named_day_and_a_day_not_in_the_file(self):
        self.append(conditions(["2026-10-31", "2026-11-01"]), "2026-11-01")
        self.assertEqual(len(self.read("2026/11.csv")), 2)
        with self.assertRaises(SystemExit):
            self.append(conditions(["2026-10-31"]), "2026-12-25")


    def test_the_other_regions_files_are_read_too_cg43(self):
        main = conditions(["2026-10-15"])
        other = conditions(["2026-10-15"])
        other["crags"] = [dict(other["crags"][0], id="osm-node-9", name="Gamma Wall", provinceCode="TO")]
        (self.dir / "conditions-piemonte.json").write_text(json.dumps(other))
        main["regions"] = [{"name": "Piemonte", "count": 1, "file": "conditions-piemonte.json"}]
        self.append(main)
        self.assertEqual([r["crag_name"] for r in self.read("2026/10.csv")], ["Alpha Wall", "beta Wall", "Gamma Wall"])


if __name__ == "__main__":
    unittest.main()
