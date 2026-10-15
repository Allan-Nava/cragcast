"""Tests for tools/osm_crags.py — made-up OSM objects, not real places.

    python3 -m unittest discover -s tools -p 'test_*.py'
"""
import unittest

import osm_crags as o

LAT, LON = 45.9, 9.4
DEG_PER_M = 1 / 111_320          # latitude degrees per metre


def el(kind, osm_id, name, north_m=0.0, **tags):
    lat = LAT + north_m * DEG_PER_M
    e = {"type": kind, "id": osm_id, "tags": {"name": name, **tags}}
    if kind == "node":
        e.update(lat=lat, lon=LON)
    else:
        e["center"] = {"lat": lat, "lon": LON}
    return e


def build(*elements):
    items = [c for c in map(o.crag, elements) if c and c["_kind"]]
    crags, dropped = o.group(o.merge_duplicates([c for c in items if c["_kind"] == "crag"])
                             + o.merge_duplicates([c for c in items if c["_kind"] != "crag"]))
    return {c["name"]: c for c in crags}, dropped


class Kind(unittest.TestCase):
    def test_what_counts(self):
        self.assertEqual(o.kind(el("node", 1, "A", climbing="crag")), "crag")
        self.assertEqual(o.kind(el("way", 1, "A", sport="climbing", natural="cliff")), "shape")
        self.assertEqual(o.kind(el("relation", 1, "A", sport="climbing", type="single_rock")), "shape")
        self.assertEqual(o.kind(el("node", 1, "A", sport="climbing")), "point")
        self.assertEqual(o.kind(el("node", 1, "A", sport="climbing", type="single_rock")), "point")

    def test_what_does_not(self):
        for tags in ({"leisure": "sports_centre"}, {"shop": "sports"}, {"building": "yes"},
                     {"indoor": "yes", "natural": "cliff"}, {"climbing": "route_bottom"}):
            self.assertIsNone(o.kind(el("node", 1, "A", sport="climbing", **tags)), tags)
        self.assertIsNone(o.kind({"type": "way", "id": 1, "tags": {"sport": "climbing", "natural": "cliff"}}))


class Names(unittest.TestCase):
    def test_base_and_key(self):
        self.assertEqual(o.base("Falesia Maddalena - Settore A"), "Falesia Maddalena")
        self.assertEqual(o.base("Falesia di Brembilla (Settore Camos)"), "Falesia di Brembilla")
        self.assertEqual(o.base("Placchette del San Martino, left sector"), "Placchette del San Martino")
        self.assertEqual(o.base("Falesia di Giambi Settore paretino"), "Falesia di Giambi")
        self.assertEqual(o.base("Settore Destro"), "Settore Destro")
        self.assertEqual(o.key("La Discoteca"), o.key("discoteca"))
        self.assertEqual(o.key("Falesia del Sasso Test"), o.key("Falesia Sasso Test"))
        self.assertEqual(o.key("Còrna"), "corna")


class Group(unittest.TestCase):
    def test_sectors_with_one_base_name_become_one_crag(self):
        crags, _ = build(el("way", 1, "Falesia Test - Settore A", 0, sport="climbing", natural="cliff"),
                         el("way", 2, "Falesia Test - Settore B", 400, sport="climbing", natural="cliff"),
                         el("way", 3, "Falesia Test - Settore C", 900, sport="climbing", natural="cliff"))
        self.assertEqual(list(crags), ["Falesia Test"])
        self.assertEqual(len(crags["Falesia Test"]["alsoMapped"]), 2)

    def test_the_same_base_name_far_apart_stays_two_crags(self):
        crags, _ = build(el("way", 1, "Falesia Test - Nord", 0, sport="climbing", natural="cliff"),
                         el("way", 2, "Falesia Test - Sud", 5000, sport="climbing", natural="cliff"))
        self.assertEqual(len(crags), 2)

    def test_a_shape_on_a_crag_is_the_crag_drawn_again(self):
        crags, _ = build(el("node", 1, "Rocce Test", 0, climbing="crag"),
                         el("way", 2, "Zucco Test - Rocce", 20, sport="climbing", natural="cliff"))
        self.assertEqual(list(crags), ["Rocce Test"])
        self.assertEqual(crags["Rocce Test"]["id"], "osm-node-1")              # the public id stays
        self.assertEqual(crags["Rocce Test"]["alsoMapped"], ["https://www.openstreetmap.org/way/2"])

    def test_one_place_with_an_article_more_is_the_same_place(self):
        crags, _ = build(el("node", 1, "Test Disco", 0, climbing="crag"),
                         el("way", 2, "La Test Disco", 10, sport="climbing", natural="cliff",
                            **{"climbing:routes": "34"}))
        self.assertEqual(crags["Test Disco"]["details"]["routes"], 34)

    def test_a_sector_found_around_a_climbing_crag_only_adds_its_link(self):
        crags, _ = build(el("node", 1, "Test Disco", 0, climbing="crag",
                            **{"climbing:routes": "34", "climbing:grade:french:max": "8a+",
                               "climbing:orientation": "S"}),
                         el("node", 2, "Bastionata Test", 200, sport="climbing",
                            **{"climbing:grade:french:max": "6a", "climbing:orientation": "N"}))
        c = crags["Test Disco"]
        self.assertEqual((c["details"]["routes"], c["details"]["gradeMax"], c["aspect"]), (34, "8a+", "S"))
        self.assertEqual(c["alsoMapped"], ["https://www.openstreetmap.org/node/2"])

    def test_loose_points_near_a_crag_are_its_sectors(self):
        crags, dropped = build(el("relation", 1, "Falesie Test", 0, sport="climbing", type="single_rock"),
                               el("way", 2, "Falesia di Prova", 5, sport="climbing", natural="cliff"),
                               el("node", 3, "Oasi", 120, sport="climbing"),
                               el("node", 4, "Muro Giallo", 250, sport="climbing"),
                               el("node", 5, "Climbing point", 9000, sport="climbing"),
                               el("node", 6, "Falesia Lontana", 20000, sport="climbing"))
        self.assertEqual(sorted(crags), ["Falesia Lontana", "Falesie Test"])
        self.assertEqual(crags["Falesie Test"]["id"], "osm-relation-1")         # the umbrella is kept
        self.assertEqual(len(crags["Falesie Test"]["alsoMapped"]), 3)
        self.assertEqual(dropped, ["Climbing point"])

    def test_two_climbing_crags_are_never_grouped(self):
        crags, _ = build(el("node", 1, "Settore Destro", 0, climbing="crag"),
                         el("node", 2, "Settore Destro", 800, climbing="crag"))
        self.assertEqual(len(crags), 1)                 # one name, so a dict of one…
        items = [o.crag(e) for e in (el("node", 1, "Settore Destro", 0, climbing="crag"),
                                     el("node", 2, "Settore Destro", 800, climbing="crag"))]
        out, _ = o.group(items)
        self.assertEqual(len(out), 2)                   # …but two crags

    def test_details_and_aspect_come_from_the_sectors(self):
        crags, _ = build(el("way", 1, "Falesia Test - A", 0, sport="climbing", natural="cliff"),
                         el("way", 2, "Falesia Test - B", 300, sport="climbing", natural="cliff",
                            **{"climbing:orientation": "S", "climbing:grade:french:max": "7a"}))
        c = crags["Falesia Test"]
        self.assertEqual(c["aspect"], "S")
        self.assertEqual(c["details"]["gradeMax"], "7a")
        self.assertNotIn("_kind", c)
        self.assertNotIn("_sector_parts", c)

    def test_sectors_widen_the_grade_range_and_never_lend_their_route_count(self):
        crags, _ = build(el("way", 1, "Falesia Test - A", 0, sport="climbing", natural="cliff",
                            **{"climbing:grade:french:min": "5c", "climbing:grade:french:max": "6b+",
                               "climbing:routes": "12", "climbing:rock": "limestone"}),
                         el("way", 2, "Falesia Test - B", 300, sport="climbing", natural="cliff",
                            **{"climbing:grade:french:min": "5a", "climbing:grade:french:max": "7a",
                               "climbing:routes": "20"}))
        d = crags["Falesia Test"]["details"]
        self.assertEqual((d["gradeMin"], d["gradeMax"]), ("5a", "7a"))
        self.assertNotIn("routes", d)
        self.assertEqual(d["rock"], "limestone")

    def test_a_crag_that_is_one_sector_does_not_take_in_its_siblings(self):
        crags, _ = build(el("node", 1, "Falesia Test - Settore Basso", 0, climbing="crag"),
                         el("way", 2, "Falesia Test - Settore Alto", 30, sport="climbing", natural="cliff",
                            **{"climbing:orientation": "W"}))
        self.assertEqual(sorted(crags), ["Falesia Test - Settore Alto", "Falesia Test - Settore Basso"])
        self.assertIsNone(crags["Falesia Test - Settore Basso"]["aspect"])


    def test_sectors_facing_different_ways_leave_the_aspect_unknown(self):
        crags, _ = build(el("way", 1, "Falesia Test - A", 0, sport="climbing", natural="cliff",
                            **{"climbing:orientation": "S"}),
                         el("way", 2, "Falesia Test - B", 300, sport="climbing", natural="cliff",
                            **{"climbing:orientation": "N"}))
        self.assertIsNone(crags["Falesia Test"]["aspect"])


    def test_a_cliff_mapped_also_as_a_point_stays_a_crag(self):
        crags, dropped = build(el("node", 1, "Test Wall", 0, sport="climbing"),
                               el("way", 2, "Test Wall", 10, sport="climbing", natural="cliff"),
                               el("node", 3, "Falesia Vicina", 250, climbing="crag"))
        self.assertIn("Test Wall", crags)                  # not swallowed as a sector of the crag nearby
        self.assertEqual(dropped, [])

    def test_a_group_takes_the_base_name_and_sector_labels_alone_are_left_out(self):
        crags, dropped = build(el("node", 1, "Falesia Test (Settore Uno)", 0, sport="climbing"),
                               el("node", 2, "Falesia Test (Settore Due)", 100, sport="climbing"),
                               el("way", 3, "Settore Alto", 30000, sport="climbing", natural="cliff"),
                               el("node", 4, "Secondo Settore", 60000, sport="climbing"))
        self.assertEqual(list(crags), ["Falesia Test"])
        self.assertEqual(sorted(dropped), ["Secondo Settore", "Settore Alto"])


    def test_a_point_named_like_a_crag_a_little_away_is_that_crag(self):
        crags, _ = build(el("node", 1, "Falesia del Sasso Test", 0, sport="climbing", natural="cliff"),
                         el("node", 2, "Falesia Sasso Test", 540, sport="climbing"))
        self.assertEqual(list(crags), ["Falesia del Sasso Test"])


if __name__ == "__main__":
    unittest.main()
