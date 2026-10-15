"""Tests for tools/avalanche_regions.py, with the EAWS download faked."""
import contextlib
import io
import json
import pathlib
import tempfile
import unittest
from unittest import mock

import avalanche_regions


def answer(features):
    body = io.BytesIO(json.dumps({"features": features}).encode())
    body.__enter__ = lambda *a: body
    body.__exit__ = lambda *a: None
    return body


SQUARE = [[[9.123456789, 45.0, 1200], [9.2, 45.0, 1200], [9.2, 45.1, 1200], [9.123456789, 45.0, 1200]]]


class Regions(unittest.TestCase):
    def run_tool(self, features):
        with tempfile.TemporaryDirectory() as d:
            out = pathlib.Path(d, "regions.geojson")
            with mock.patch.object(avalanche_regions, "TARGET", out), \
                    mock.patch.object(avalanche_regions.urllib.request, "urlopen", return_value=answer(features)), \
                    contextlib.redirect_stdout(io.StringIO()):
                avalanche_regions.main()
            return json.loads(out.read_text())

    def test_keeps_the_regions_in_force_as_multipolygons(self):
        data = self.run_tool([
            {"properties": {"id": "IT-25-B", "start_date": "2025-11-03"}, "geometry": {"type": "Polygon", "coordinates": SQUARE}},
            {"properties": {"id": "IT-25-A", "end_date": "2025-11-03"}, "geometry": {"type": "Polygon", "coordinates": SQUARE}},
            {"properties": {"id": "IT-25-A"}, "geometry": {"type": "MultiPolygon", "coordinates": [SQUARE]}},
        ])
        self.assertEqual([f["properties"]["id"] for f in data["features"]], ["IT-25-A", "IT-25-B"])
        self.assertTrue(all(f["geometry"]["type"] == "MultiPolygon" for f in data["features"]))
        self.assertEqual(data["features"][1]["geometry"]["coordinates"][0][0][0], [9.12346, 45.0])   # 2D, 5 decimals
        self.assertIn("CC0", data["licence"])

    def test_refuses_to_write_nothing(self):
        with self.assertRaises(SystemExit):
            self.run_tool([{"properties": {"id": "IT-25-A", "end_date": "2025-11-03"},
                            "geometry": {"type": "Polygon", "coordinates": SQUARE}}])


if __name__ == "__main__":
    unittest.main()
