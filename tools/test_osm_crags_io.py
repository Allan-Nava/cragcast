"""tools/osm_crags.py against a fake Overpass and a fake elevation API on localhost: retries,
empty and truncated answers, batches, and a whole run. Made-up places."""
import contextlib
import http.server
import io
import json
import pathlib
import tempfile
import threading
import unittest
import urllib.parse
from unittest import mock

import osm_crags as o


class Fake(http.server.BaseHTTPRequestHandler):
    """Answers from the class's script: a list of (status, body) taken in order, then the last."""
    script = []
    seen = []

    def log_message(self, *a):
        pass

    def reply(self):
        status, body = Fake.script.pop(0) if len(Fake.script) > 1 else Fake.script[0]
        data = (body if isinstance(body, str) else json.dumps(body)).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        Fake.seen.append(urllib.parse.parse_qs(self.rfile.read(int(self.headers["Content-Length"])).decode())["data"][0])
        self.reply()

    def do_GET(self):
        Fake.seen.append(self.path)
        self.reply()


class Server(unittest.TestCase):
    def setUp(self):
        self.httpd = http.server.HTTPServer(("127.0.0.1", 0), Fake)
        threading.Thread(target=self.httpd.serve_forever, daemon=True).start()
        self.url = f"http://127.0.0.1:{self.httpd.server_port}"
        Fake.seen = []
        for patch in (mock.patch.object(o.time, "sleep"), mock.patch.object(o, "OVERPASS", [self.url + "/a", self.url + "/b"]),
                      mock.patch.object(o, "ELEVATION", self.url + "/elevation")):
            patch.start()
            self.addCleanup(patch.stop)
        self.addCleanup(self.httpd.server_close)
        self.addCleanup(self.httpd.shutdown)

    def quiet(self, f, *a):
        with contextlib.redirect_stdout(io.StringIO()):
            return f(*a)


class Overpass(Server):
    def test_retries_busy_and_empty_answers_then_takes_the_data(self):
        Fake.script = [(502, "busy"), (200, {"elements": []}), (200, {"elements": [{"id": 1}]})]
        self.assertEqual(self.quiet(o.overpass, "q"), [{"id": 1}])
        self.assertEqual(len(Fake.seen), 3)

    def test_gives_up_without_data(self):
        Fake.script = [(500, "down")]
        with self.assertRaises(SystemExit):
            self.quiet(o.overpass, "q")
        self.assertEqual(len(Fake.seen), 6)                         # 3 rounds on 2 servers


class Areas(Server):
    CRAGS = [{"id": "osm-node-1", "latitude": 45.0, "longitude": 9.0},
             {"id": "osm-node-2", "latitude": 45.1, "longitude": 9.1}]

    @staticmethod
    def area(ref, *extra):
        return [{"type": "crag", "tags": {"ref": ref}}, *extra]

    def test_reads_municipality_and_province(self):
        Fake.script = [(200, {"elements": self.area("osm-node-1",
                                                    {"type": "area", "tags": {"admin_level": "8", "name": "Testville", "name:it": "Testvilla"}},
                                                    {"type": "area", "tags": {"admin_level": "6", "name": "Testshire", "ref": "TT"}})
                                         + self.area("osm-node-2",
                                                     {"type": "area", "tags": {"admin_level": "6", "name": "Other", "short_name": "bad1"}})})]
        found = self.quiet(o.areas, self.CRAGS)
        self.assertEqual(found["osm-node-1"], {"municipality": "Testvilla", "province": "Testshire", "provinceCode": "TT"})
        self.assertEqual(found["osm-node-2"], {"province": "Other"})              # a malformed code is left out
        self.assertIn('make crag ref="osm-node-2"', Fake.seen[0])

    def test_reads_the_region_cg43(self):
        Fake.script = [(200, {"elements": self.area("osm-node-1",
                                                    {"type": "area", "tags": {"admin_level": "4", "name": "Trentino-Alto Adige/Südtirol",
                                                                              "name:it": "Trentino-Alto Adige"}})
                                         + self.area("osm-node-2",
                                                     {"type": "area", "tags": {"admin_level": "4", "name": "Piemonte"}})})]
        found = self.quiet(o.areas, self.CRAGS)
        self.assertEqual((found["osm-node-1"]["region"], found["osm-node-2"]["region"]), ("Trentino-Alto Adige", "Piemonte"))
        self.assertIn('"admin_level"~"^(4|6|8)$"', Fake.seen[0])

    def test_the_query_covers_lombardy_and_its_neighbours(self):
        self.assertIn('area["ISO3166-2"~"^(IT-25|IT-21|IT-32)$"]', o.QUERY)

    def test_asks_again_when_the_answer_is_cut_short(self):
        cut = {"elements": self.area("osm-node-1")}
        whole = {"elements": self.area("osm-node-1") + self.area("osm-node-2")}
        Fake.script = [(200, cut), (200, whole)]
        self.assertEqual(set(self.quiet(o.areas, self.CRAGS)), {"osm-node-1", "osm-node-2"})
        Fake.script = [(200, cut)]
        with self.assertRaises(SystemExit):
            self.quiet(o.areas, self.CRAGS)


class Elevations(Server):
    def test_in_batches_of_a_hundred(self):
        crags = [{"latitude": 45 + i / 1000, "longitude": 9.0} for i in range(150)]
        Fake.script = [(200, {"elevation": [100.4] * 100}), (200, {"elevation": [200.6] * 50})]
        self.assertEqual(o.elevations(crags), [100] * 100 + [201] * 50)
        self.assertEqual(len(Fake.seen), 2)

    def test_pauses_between_requests_and_asks_again_after_a_429(self):
        crags = [{"latitude": 45 + i / 1000, "longitude": 9.0} for i in range(150)]
        Fake.script = [(429, "slow down"), (200, {"elevation": [10.0] * 100}), (200, {"elevation": [20.0] * 50})]
        with mock.patch.object(o.time, "sleep") as sleep:
            self.assertEqual(self.quiet(o.elevations, crags), [10] * 100 + [20] * 50)
        self.assertEqual(len(Fake.seen), 3)
        self.assertEqual([c.args[0] for c in sleep.call_args_list], [30, o.ELEVATION_PAUSE_S])

    def test_a_connection_that_times_out_is_asked_again(self):
        answers = [TimeoutError("handshake timed out"), o.urllib.error.URLError("refused")]
        real = o.urllib.request.urlopen
        def flaky(req, timeout):
            if answers:
                raise answers.pop(0)
            return real(req, timeout=timeout)
        Fake.script = [(200, {"elevation": [7.0]})]
        with mock.patch.object(o.urllib.request, "urlopen", side_effect=flaky), mock.patch.object(o.time, "sleep") as sleep:
            self.assertEqual(self.quiet(o.elevations, [{"latitude": 45, "longitude": 9}]), [7])
        self.assertEqual([c.args[0] for c in sleep.call_args_list], [30, 60])

    def test_a_connection_failing_four_times_gives_up(self):
        with mock.patch.object(o.urllib.request, "urlopen", side_effect=TimeoutError("down")), \
                mock.patch.object(o.time, "sleep"), self.assertRaises(TimeoutError):
            self.quiet(o.elevations, [{"latitude": 45, "longitude": 9}])

    def test_gives_up_after_four_429s_and_any_other_error_at_once(self):
        Fake.script = [(429, "slow down")]
        with mock.patch.object(o.time, "sleep"), self.assertRaises(o.urllib.error.HTTPError):
            self.quiet(o.elevations, [{"latitude": 45, "longitude": 9}])
        self.assertEqual(len(Fake.seen), 4)
        Fake.seen.clear()
        Fake.script = [(500, "boom")]
        with self.assertRaises(o.urllib.error.HTTPError):
            self.quiet(o.elevations, [{"latitude": 45, "longitude": 9}])
        self.assertEqual(len(Fake.seen), 1)

    def test_refuses_a_short_answer(self):
        Fake.script = [(200, {"elevation": [1.0]})]
        with self.assertRaises(SystemExit):
            o.elevations([{"latitude": 45, "longitude": 9}, {"latitude": 46, "longitude": 9}])


class WholeRun(Server):
    def test_from_a_saved_answer_to_crags_json(self):
        elements = [
            {"type": "node", "id": 1, "lat": 45.0, "lon": 9.0, "tags": {"name": "Test Crag", "climbing": "crag",
                                                                         "climbing:orientation": "sw"}},
            {"type": "way", "id": 2, "center": {"lat": 45.3, "lon": 9.2}, "tags": {"name": "Test Cliff", "sport": "climbing",
                                                                                    "natural": "cliff"}},
            {"type": "node", "id": 3, "lat": 45.5, "lon": 9.5, "tags": {"name": "Test Gym", "sport": "climbing", "leisure": "sports_centre"}},
        ]
        with tempfile.TemporaryDirectory() as d:
            saved, out = pathlib.Path(d, "answer.json"), pathlib.Path(d, "data", "crags.json")
            out.parent.mkdir()
            saved.write_text(json.dumps({"elements": elements}))
            with mock.patch.object(o, "OUT", out), mock.patch.object(o, "OBJECTS", out.with_name("osm-climbing.json")), \
                    mock.patch.object(o.sys, "argv", ["osm_crags.py", "--from", str(saved)]), \
                    mock.patch.object(o, "elevations", lambda crags: [300] * len(crags)), \
                    mock.patch.object(o, "areas", lambda crags: {c["id"]: {"municipality": "Testville", "region": "Piemonte"}
                                                                for c in crags}):
                self.quiet(o.main)
            data = json.loads(out.read_text())
            objects = json.loads(out.with_name("osm-climbing.json").read_text())["objects"]
        self.assertEqual([c["name"] for c in data["crags"]], ["Test Cliff", "Test Crag"])
        # CG-103: the gym left out of the crags is kept among the objects, as a venue.
        self.assertEqual({o["id"]: o["category"] for o in objects},
                         {"osm-node-1": "crag", "osm-way-2": "cliff", "osm-node-3": "venue"})
        crag = data["crags"][1]
        self.assertEqual((crag["aspect"], crag["elevation"], crag["municipality"]), ("SW", 300, "Testville"))
        self.assertEqual(crag["region"], "Piemonte")                     # CG-43
        self.assertEqual(data["region"], "Lombardia, Piemonte, Trentino-Alto Adige")
        self.assertNotIn("_kind", crag)
        self.assertIn("ODbL", data["licence"])

    def test_an_answer_with_no_crag_leaves_the_file_alone(self):
        with tempfile.TemporaryDirectory() as d:
            saved = pathlib.Path(d, "answer.json")
            saved.write_text(json.dumps({"elements": [{"type": "node", "id": 9, "lat": 1, "lon": 1, "tags": {"name": "Gym",
                                                                                                         "leisure": "sports_centre"}}]}))
            with mock.patch.object(o, "OUT", pathlib.Path(d, "crags.json")), mock.patch.object(o, "OBJECTS", pathlib.Path(d, "o.json")), \
                    mock.patch.object(o.sys, "argv", ["osm_crags.py", "--from", str(saved)]), self.assertRaises(SystemExit):
                self.quiet(o.main)
            self.assertFalse(pathlib.Path(d, "crags.json").exists())


class Details(unittest.TestCase):
    def test_validated_or_left_out(self):
        d = o.details({"climbing:grade:french:min": "5C", "climbing:grade:french:max": "9z",
                       "climbing:length:max": "30 m", "climbing:routes": "twelve", "climbing:rock": "Granite",
                       "climbing:sport": "yes", "climbing:trad": "no", "climbing:bolted": "yes",
                       "website": "javascript:alert(1)", "url": "https://example.org"})
        self.assertEqual(d, {"gradeMin": "5c", "lengthMax": 30, "rock": "granite", "rockIt": "granito",
                             "styles": ["sport"], "bolted": True,
                             "website": "https://example.org"})        # the unsafe website tag skipped for url
        self.assertEqual(o.details({"contact:website": "https://example.org"})["website"], "https://example.org")
        self.assertEqual((o.number("25,5"), o.number("12"), o.number("")), (25.5, 12, None))


if __name__ == "__main__":
    unittest.main()


class Objects(unittest.TestCase):
    """CG-103: data/osm-climbing.json, every object of the query."""

    def el(self, kind, i, **tags):
        e = {"type": kind, "id": i, "tags": tags}
        if kind == "node":
            e.update(lat=45.123456789, lon=9.987654321)
        else:
            e["center"] = {"lat": 45.1, "lon": 9.9}
        return e

    def test_categories(self):
        cases = [(self.el("node", 1, name="A", climbing="crag"), "crag"),
                 (self.el("way", 2, name="B", sport="climbing", natural="cliff"), "cliff"),
                 (self.el("node", 3, name="C", sport="climbing"), "point"),
                 (self.el("node", 4, name="Via", sport="climbing", climbing="route_bottom"), "route"),
                 (self.el("node", 5, name="Gym", sport="climbing", leisure="sports_centre"), "venue"),
                 (self.el("way", 6, sport="climbing", natural="cliff"), "other")]           # no name
        for e, want in cases:
            self.assertEqual(o.category(e), want, e)

    def test_an_object_keeps_its_validated_tags_and_a_route_its_grade(self):
        obj = o.osm_object(self.el("node", 7, name="Spigolo", sport="climbing", climbing="route_bottom",
                                   **{"climbing:grade:french": "6A+", "climbing:orientation": "sw",
                                      "climbing:length": "25 m", "climbing:grade:french:max": "nope"}))
        self.assertEqual(obj, {"id": "osm-node-7", "osmType": "node", "osmId": 7, "category": "route",
                               "name": "Spigolo", "climbing": "route_bottom", "latitude": 45.123457,
                               "longitude": 9.987654, "aspect": "SW", "details": {"length": 25, "grade": "6a+"}})
        self.assertIsNone(o.osm_object({"type": "way", "id": 8, "tags": {"name": "No centre"}}))

    def test_a_route_keeps_its_bolts_and_pitches(self):
        obj = o.osm_object(self.el("node", 9, name="Diedro", sport="climbing", climbing="route_bottom",
                                   **{"climbing:bolts": "12", "climbing:pitches": "3", "climbing:grade:french": "5c"}))
        self.assertEqual(obj["details"], {"grade": "5c", "bolts": 12, "pitches": 3})
        cliff = o.osm_object(self.el("way", 10, name="Parete", sport="climbing", natural="cliff", **{"climbing:bolts": "40"}))
        self.assertNotIn("bolts", cliff["details"])

    def test_each_relation_is_one_element_with_its_members(self):
        centre = {"type": "relation", "id": 1, "center": {"lat": 45, "lon": 9}, "tags": {"name": "A"}}
        body = {"type": "relation", "id": 1, "tags": {"name": "A"}, "members": [{"type": "node", "ref": 2, "role": ""}]}
        out = o.with_members([centre, {"type": "node", "id": 2}, body])
        self.assertEqual([(e["type"], e["id"]) for e in out], [("relation", 1), ("node", 2)])
        self.assertEqual(out[0]["center"], {"lat": 45, "lon": 9})
        self.assertEqual(out[0]["members"][0]["ref"], 2)

    def test_objects_only_writes_the_file_alone(self):
        with tempfile.TemporaryDirectory() as d:
            saved = pathlib.Path(d, "answer.json")
            saved.write_text(json.dumps({"elements": [self.el("node", 2, name="B"), self.el("node", 1, name="A", climbing="crag")]}))
            out = pathlib.Path(d, "osm-climbing.json")
            with mock.patch.object(o, "OBJECTS", out), mock.patch.object(o, "OUT", pathlib.Path(d, "crags.json")), \
                    mock.patch.object(o.sys, "argv", ["osm_crags.py", "--objects-only", "--from", str(saved)]), \
                    mock.patch.object(o, "elevations", side_effect=AssertionError("no elevation lookup")), \
                    contextlib.redirect_stdout(io.StringIO()):
                o.main()
            data = json.loads(out.read_text())
            self.assertFalse(pathlib.Path(d, "crags.json").exists())
        self.assertEqual([x["id"] for x in data["objects"]], ["osm-node-1", "osm-node-2"])        # by id
        self.assertIn("ODbL", data["licence"])
