"""Tests for tools/aspects_todo.py — made-up crags, not real places."""
import contextlib
import io
import json
import pathlib
import tempfile
import unittest
from unittest import mock

import aspects_todo


class Todo(unittest.TestCase):
    def test_the_checklist(self):
        crags = [
            {"name": "Zeta Wall", "aspect": None, "province": "Lecco", "municipality": "Lecco", "elevation": 500,
             "sourceUrl": "https://www.openstreetmap.org/way/2"},
            {"name": "alpha Wall", "aspect": None, "province": "Lecco", "municipality": None, "elevation": None,
             "sourceUrl": "https://www.openstreetmap.org/relation/3"},
            {"name": "Odd Wall", "aspect": None, "sourceUrl": "https://example.org/x"},
            {"name": "Sunny Wall", "aspect": "S", "province": "Bergamo", "municipality": "Bergamo",
             "sourceUrl": "https://www.openstreetmap.org/node/1"},
        ]
        with tempfile.TemporaryDirectory() as d:
            src, out = pathlib.Path(d, "crags.json"), pathlib.Path(d, "docs", "todo.md")
            src.write_text(json.dumps({"crags": crags}))
            with mock.patch.object(aspects_todo, "CRAGS", src), mock.patch.object(aspects_todo, "TARGET", out), \
                    mock.patch.object(aspects_todo, "ROOT", pathlib.Path(d)), contextlib.redirect_stdout(io.StringIO()):
                aspects_todo.main()
            text = out.read_text()
        self.assertIn("**1 of 4 crags** have `climbing:orientation`", text)
        self.assertIn("## Lecco (2)", text)
        self.assertIn("## Provincia sconosciuta (1)", text)
        self.assertLess(text.index("| alpha Wall |"), text.index("| Zeta Wall |"))            # by name, any case
        self.assertIn("| Zeta Wall | Lecco | 500 m | [view](https://www.openstreetmap.org/way/2) | "
                      "[edit](https://www.openstreetmap.org/edit?way=2) |", text)
        self.assertIn("| Odd Wall |  |  | [view](https://example.org/x) |  |", text)          # no edit link to guess
        self.assertIn("- Sunny Wall (Bergamo): S", text)
        self.assertNotIn("## Bergamo", text)


if __name__ == "__main__":
    unittest.main()
