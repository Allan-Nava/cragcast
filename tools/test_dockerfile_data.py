"""The backend image carries every data file the jar does (CG-103 lesson).

backend/pom.xml puts some data/ files on the classpath; backend/Dockerfile must copy each into
the build, or the image silently lacks a file that the build from the repository — and so CI —
has. osm-climbing.json was missed this way and the Pi's osm_object stayed empty.
"""
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent


class DataInTheImage(unittest.TestCase):
    def test_every_data_file_of_the_jar_is_copied_into_the_image(self):
        pom = (ROOT / "backend" / "pom.xml").read_text()
        block = re.search(r"<directory>\.\./data</directory>(.*?)</resource>", pom, re.S).group(1)
        in_jar = set(re.findall(r"<include>([^<]+)</include>", block))
        dockerfile = (ROOT / "backend" / "Dockerfile").read_text()
        copied = {pathlib.PurePosixPath(p).name for line in re.findall(r"^COPY (.+)$", dockerfile, re.M)
                  for p in line.split()[:-1] if p.startswith("data/")}
        self.assertTrue(in_jar, "no data files found in the pom")
        self.assertEqual(sorted(in_jar - copied), [], "data files in the jar but not copied by backend/Dockerfile")

    def test_the_image_workflow_rebuilds_when_one_changes(self):
        pom = (ROOT / "backend" / "pom.xml").read_text()
        block = re.search(r"<directory>\.\./data</directory>(.*?)</resource>", pom, re.S).group(1)
        workflow = (ROOT / ".github" / "workflows" / "image.yml").read_text()
        missing = [f for f in re.findall(r"<include>([^<]+)</include>", block) if f"data/{f}" not in workflow]
        self.assertEqual(missing, [], "data files whose change does not rebuild the image")


if __name__ == "__main__":
    unittest.main()
