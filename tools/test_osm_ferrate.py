"""Tests for tools/osm_ferrate.py — made-up OSM ways and relations, not real via ferratas.

    python3 -m unittest discover -s tools -p 'test_*.py'
"""
import contextlib
import io
import json
import pathlib
import tempfile
import unittest
from unittest import mock

import osm_ferrate as f

LAT, LON = 45.9, 9.4
DEG_PER_M = 1 / 111_320          # latitude degrees per metre


def way(osm_id, name=None, north_m=0.0, long_m=100.0, east_m=0.0, **tags):
    """A straight piece running north from north_m for long_m metres, east_m to the east."""
    if name:
        tags["name"] = name
    lon = LON + east_m * DEG_PER_M / 0.7
    geometry = [{"lat": LAT + (north_m + k * long_m / 4) * DEG_PER_M, "lon": lon} for k in range(5)]
    return {"type": "way", "id": osm_id, "tags": {"highway": "via_ferrata", **tags}, "geometry": geometry}


def relation(osm_id, name, *way_ids, **tags):
    return {"type": "relation", "id": osm_id, "tags": {"route": "via_ferrata", "name": name, **tags},
            "members": [{"type": "way", "ref": i, "role": ""} for i in way_ids]}


def rising(points):
    """A made-up DEM: higher to the north, 1 m per metre."""
    return [round((p["latitude"] - LAT) / DEG_PER_M) + 1000 for p in points]


def names(groups):
    return sorted(sorted(w["id"] for w in members) for _, members in groups)


class Grouping(unittest.TestCase):
    def test_a_relation_is_one_via_ferrata_of_its_pieces(self):
        g = f.group([way(1), way(2, north_m=900), relation(10, "Ferrata Test", 1, 2)])
        self.assertEqual(len(g), 1)
        self.assertEqual(g[0][0]["id"], 10)
        self.assertEqual(names(g), [[1, 2]])

    def test_a_relation_without_via_ferrata_pieces_is_left_out(self):
        with contextlib.redirect_stdout(io.StringIO()) as out:
            g = f.group([relation(10, "Ferrata Test", 99)])
        self.assertEqual(g, [])
        self.assertIn("relation 10", out.getvalue())

    def test_the_same_name_joins_within_name_m_and_not_beyond(self):
        near = f.group([way(1, "Ferrata Test"), way(2, "ferrata  tést", north_m=100 + f.NAME_M - 10)])
        far = f.group([way(1, "Ferrata Test"), way(2, "Ferrata Test", north_m=100 + f.NAME_M + 50)])
        self.assertEqual(names(near), [[1, 2]])
        self.assertEqual(names(far), [[1], [2]])

    def test_different_names_never_join_even_touching(self):
        g = f.group([way(1, "Ferrata Alta"), way(2, "Ferrata Bassa", north_m=100)])
        self.assertEqual(names(g), [[1], [2]])

    def test_an_unnamed_piece_joins_the_piece_it_touches(self):
        g = f.group([way(1, "Ferrata Test"), way(2, north_m=100 + f.TOUCH_M - 5), way(3, north_m=1000)])
        self.assertEqual(names(g), [[1, 2], [3]])

    def test_an_unnamed_piece_never_bridges_two_names(self):
        g = f.group([way(1, "Ferrata Alta"), way(2, north_m=100), way(3, "Ferrata Bassa", north_m=200)])
        self.assertEqual(len(g), 2)
        self.assertIn([1, 2], names(g))
        self.assertIn([3], names(g))

    def test_a_way_without_geometry_is_ignored(self):
        broken = {"type": "way", "id": 5, "tags": {"highway": "via_ferrata", "name": "X"}, "geometry": [{"lat": LAT, "lon": LON}]}
        self.assertEqual(f.group([broken]), [])


class Scale(unittest.TestCase):
    def test_zero_to_six_kept_others_dropped_as_written(self):
        good, bad = f.scales(relation(10, "R", 1, via_ferrata_scale="3"),
                             [way(1, via_ferrata_scale="2+"), way(2, via_ferrata_scale="K2"), way(3, via_ferrata_scale="5"),
                              way(4, via_ferrata_scale="7"), way(5)])
        self.assertEqual(good, [3, 5])
        self.assertEqual(bad, ["2+", "7", "K2"])


class Build(unittest.TestCase):
    def test_one_via_ferrata_whole(self):
        elements = [way(1, "Ferrata Test", via_ferrata_scale="2"), way(2, "Ferrata Test", north_m=300, via_ferrata_scale="4"),
                    way(3, north_m=400 + 10, via_ferrata_scale="3+")]
        ferrate, unnamed = f.build(elements, elevations=rising)
        self.assertEqual(unnamed, [])
        (one,) = ferrate
        self.assertEqual(one["id"], "osm-way-1")
        self.assertEqual(one["sourceUrl"], "https://www.openstreetmap.org/way/1")
        self.assertEqual(one["pieces"], [f"https://www.openstreetmap.org/way/{i}" for i in (1, 2, 3)])
        self.assertEqual((one["scaleMin"], one["scaleMax"], one["scaleDropped"]), (2, 4, ["3+"]))
        self.assertEqual(one["start"]["elevation"], 1000)          # the lowest end
        self.assertEqual(one["top"]["elevation"], 1510)            # the highest point, 510 m north
        self.assertAlmostEqual(one["lengthM"], 300, delta=2)

    def test_a_relation_names_and_identifies_it(self):
        ferrate, _ = f.build([way(1, "Pezzo"), relation(10, "Ferrata Test", 1)], elevations=rising)
        self.assertEqual((ferrate[0]["id"], ferrate[0]["name"]), ("osm-relation-10", "Ferrata Test"))

    def test_the_longest_named_piece_names_a_group(self):
        ferrate, _ = f.build([way(1, "Corta", long_m=50), way(2, "Lunga", north_m=500, long_m=200), relation(10, "", 1, 2)],
                             elevations=rising)
        self.assertEqual(ferrate[0]["name"], "Lunga")

    def test_unnamed_groups_apart_and_named_ones_by_name(self):
        ferrate, unnamed = f.build([way(1, "Zeta"), way(2, "Alfa", north_m=5000), way(3, north_m=9000)], elevations=rising)
        self.assertEqual([x["name"] for x in ferrate], ["Alfa", "Zeta"])
        self.assertEqual([x["id"] for x in unnamed], ["osm-way-3"])

    def test_the_dem_is_asked_once_with_each_point_once(self):
        asked = []
        f.build([way(1, "A"), way(2, "A", north_m=100)], elevations=lambda pts: asked.append(pts) or rising(pts))
        self.assertEqual(len(asked), 1)
        self.assertEqual(len(asked[0]), len({(p["latitude"], p["longitude"]) for p in asked[0]}))
        self.assertLessEqual(len(asked[0]), f.SAMPLES)

    def test_nothing_asks_the_dem_for_nothing(self):
        self.assertEqual(f.build([], elevations=lambda pts: self.fail("asked")), ([], []))


class Dem(unittest.TestCase):
    def test_a_hundred_points_a_request_with_a_pause_between(self):
        pts = [{"latitude": LAT + i * DEG_PER_M, "longitude": LON} for i in range(250)]
        with mock.patch.object(f.o, "elevations", side_effect=rising) as ask, mock.patch.object(f.time, "sleep") as sleep:
            self.assertEqual(f.dem(pts), rising(pts))
        self.assertEqual([len(c.args[0]) for c in ask.call_args_list], [100, 100, 50])
        self.assertEqual(sleep.call_count, 2)


class Todo(unittest.TestCase):
    def test_lists_missing_dropped_and_unnamed_with_edit_links(self):
        ferrate, unnamed = f.build([way(1, "Senza Scala"), way(2, "Con Scala", north_m=5000, via_ferrata_scale="3"),
                                    way(3, "Plus", north_m=9000, via_ferrata_scale="2+"),
                                    way(4, "Mista", north_m=13000, via_ferrata_scale="4"),
                                    way(5, "Mista", north_m=13100, via_ferrata_scale="K2"), way(6, north_m=20000)],
                                   elevations=rising)
        md = f.todo(ferrate, unnamed)
        self.assertIn("**2 of 4 via ferratas**", md)
        self.assertIn("## Difficulty not mapped (2)", md)
        self.assertIn("- Plus (`2+` is not on the 0–6 scale)", md)
        self.assertIn("https://www.openstreetmap.org/edit?way=1", md)
        self.assertIn("## Difficulty mapped, with a value not on the scale (1)\n\n- Mista (`K2` dropped)", md)
        self.assertIn("## No name: left out of cragcast until it has one (1)\n\n- senza nome", md)
        self.assertNotIn("Con Scala", md)


class Main(unittest.TestCase):
    def run_main(self, args, elements):
        with tempfile.TemporaryDirectory() as tmp:
            out, todo = pathlib.Path(tmp) / "ferrate.json", pathlib.Path(tmp) / "todo.md"
            answer = pathlib.Path(tmp) / "answer.json"
            answer.write_text(json.dumps({"elements": elements}))
            argv = ["osm_ferrate.py"] + [str(answer) if a == "ANSWER" else a for a in args]
            with mock.patch.object(f, "OUT", out), mock.patch.object(f, "TODO", todo), \
                    mock.patch.object(f, "ROOT", pathlib.Path(tmp)), mock.patch.object(f.sys, "argv", argv), \
                    mock.patch.object(f.o, "elevations", rising), mock.patch.object(f.time, "sleep"), \
                    mock.patch.object(f.o, "overpass", return_value=elements) as overpass, \
                    contextlib.redirect_stdout(io.StringIO()) as printed:
                f.main()
                return json.loads(out.read_text()), todo.read_text(), printed.getvalue(), overpass

    def test_from_a_saved_answer(self):
        data, todo, printed, overpass = self.run_main(["--from", "ANSWER"], [way(1, "Ferrata Test", via_ferrata_scale="2")])
        overpass.assert_not_called()
        self.assertEqual([x["name"] for x in data["ferrate"]], ["Ferrata Test"])
        self.assertIn("ODbL", data["licence"])
        self.assertIn("Copernicus", data["elevationSource"])
        self.assertIn("**1 of 1 via ferratas**", todo)
        self.assertIn("1 via ferratas written", printed)

    def test_from_overpass(self):
        data, _, _, overpass = self.run_main([], [way(1, "Ferrata Test")])
        self.assertEqual(overpass.call_args.args[0], f.QUERY)
        self.assertEqual(len(data["ferrate"]), 1)

    def test_an_answer_without_via_ferratas_writes_nothing(self):
        with self.assertRaises(SystemExit):
            self.run_main(["--from", "ANSWER"], [way(1)])


if __name__ == "__main__":
    unittest.main()
