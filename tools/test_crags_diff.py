"""Tests for tools/crags_diff.py — made-up crags, not real places."""
import contextlib
import io
import json
import pathlib
import tempfile
import unittest

import crags_diff


def crag(i, name, **kw):
    c = {"id": f"osm-node-{i}", "name": name, "latitude": 45.0, "longitude": 9.0, "aspect": None,
         "sourceUrl": f"https://www.openstreetmap.org/node/{i}", "details": {}}
    c.update(kw)
    return c


class Diff(unittest.TestCase):
    def run_diff(self, old, new):
        with tempfile.TemporaryDirectory() as d:
            a, b = pathlib.Path(d, "old.json"), pathlib.Path(d, "new.json")
            a.write_text(json.dumps({"crags": old}))
            b.write_text(json.dumps({"crags": new}))
            out = io.StringIO()
            with contextlib.redirect_stdout(out):
                crags_diff.main(str(a), str(b))
            return out.getvalue()

    def test_added_merged_removed(self):
        old = [crag(1, "Kept Wall"), crag(2, "Twin Wall"), crag(3, "Gone Wall")]
        new = [crag(1, "Kept Wall", alsoMapped=["https://www.openstreetmap.org/node/2"]), crag(4, "New Wall")]
        text = self.run_diff(old, new)
        self.assertIn("**2 crags** (was 3).", text)
        self.assertIn("**Added**\n\n- New Wall — `osm-node-4`", text)
        self.assertIn("- Twin Wall — `osm-node-2` into `osm-node-1`", text)
        self.assertIn("**Removed from OSM**\n\n- Gone Wall — `osm-node-3`", text)

    def test_changes_are_spelled_out(self):
        old = [crag(1, "Old Name", municipality="A", details={"gradeMax": "6a", "rock": "limestone"})]
        new = [crag(1, "New Name", aspect="S", municipality="B", latitude=45.1,
                    details={"gradeMax": "6b", "routes": 12, "rock": "limestone"})]
        text = self.run_diff(old, new)
        self.assertIn("- New Name: aspect — → S; name was Old Name; municipality A → B; moved; "
                      "gradeMax 6a → 6b; routes — → 12", text)

    def test_a_sector_more_or_less_is_a_change(self):
        out = self.run_diff([crag(1, "Test Wall", sectors=["osm-node-2"])],
                            [crag(1, "Test Wall", sectors=["osm-node-2", "osm-way-3"])])
        self.assertIn("- Test Wall: sectors 1 → 2", out)

    def test_nothing_but_elevation(self):
        text = self.run_diff([crag(1, "Wall", elevation=500)], [crag(1, "Wall", elevation=510)])
        self.assertIn("Only elevations or formatting differ.", text)


if __name__ == "__main__":
    unittest.main()
