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
            with mock.patch.object(o, "OUT", out), mock.patch.object(o.sys, "argv", ["osm_crags.py", "--from", str(saved)]), \
                    mock.patch.object(o, "elevations", lambda crags: [300] * len(crags)), \
                    mock.patch.object(o, "areas", lambda crags: {c["id"]: {"municipality": "Testville"} for c in crags}):
                self.quiet(o.main)
            data = json.loads(out.read_text())
        self.assertEqual([c["name"] for c in data["crags"]], ["Test Cliff", "Test Crag"])
        crag = data["crags"][1]
        self.assertEqual((crag["aspect"], crag["elevation"], crag["municipality"]), ("SW", 300, "Testville"))
        self.assertNotIn("_kind", crag)
        self.assertIn("ODbL", data["licence"])

    def test_an_answer_with_no_crag_leaves_the_file_alone(self):
        with tempfile.TemporaryDirectory() as d:
            saved = pathlib.Path(d, "answer.json")
            saved.write_text(json.dumps({"elements": [{"type": "node", "id": 9, "lat": 1, "lon": 1, "tags": {"name": "Gym",
                                                                                                         "leisure": "sports_centre"}}]}))
            with mock.patch.object(o, "OUT", pathlib.Path(d, "crags.json")), \
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
