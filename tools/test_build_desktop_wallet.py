"""tools/build_desktop_wallet.py: the zip a person downloads to run the wallet on a computer."""
import importlib.util
import os
import tempfile
import unittest
import zipfile
from pathlib import Path

TOOL = Path(__file__).resolve().parent / "build_desktop_wallet.py"
spec = importlib.util.spec_from_file_location("build_desktop_wallet", TOOL)
tool = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tool)


class BuildTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        root = Path(self.temp.name)
        self.payload = root / "payload"
        (self.payload / "app" / "runtime").mkdir(parents=True)
        (self.payload / "app" / "index.html").write_text("<!doctype html>")
        (self.payload / "app" / "runtime" / "bridge.js").write_text("export {};")
        (self.payload / "vendor").mkdir()
        (self.payload / "vendor" / "pyodide.mjs").write_text("// pyodide")
        (self.payload / "wheels").mkdir()
        (self.payload / "wheels" / "keri.whl").write_bytes(b"PK\x03\x04")
        (self.payload / "pyscript-ci.toml").write_text('interpreter = "./vendor/pyodide.mjs"\n')
        (self.payload / "runtime-closure.json").write_text("{}")
        # What the importer adds for the phone, which is not part of a wallet package:
        (self.payload / "manifest.json").write_text("{}")
        (self.payload / "checksums.sha256").write_text("x")
        (self.payload / "contracts").mkdir()
        (self.payload / "contracts" / "c.json").write_text("{}")
        self.script = root / "start_wallet.py"
        self.script.write_text("print('start')\n")
        self.readme = root / "README.txt"
        self.readme.write_text("read me\n")
        self.output = root / "out.zip"

    def test_holds_the_wallet_the_script_and_the_note_and_nothing_else(self):
        tool.build(self.payload, self.script, self.readme, self.output)
        with zipfile.ZipFile(self.output) as archive:
            self.assertEqual(archive.namelist(), [
                "fort-wallet/README.txt",
                "fort-wallet/app/index.html",
                "fort-wallet/app/runtime/bridge.js",
                "fort-wallet/pyscript-ci.toml",
                "fort-wallet/runtime-closure.json",
                "fort-wallet/start-wallet.py",
                "fort-wallet/vendor/pyodide.mjs",
                "fort-wallet/wheels/keri.whl",
            ])
            self.assertEqual(archive.read("fort-wallet/start-wallet.py"), b"print('start')\n")
            self.assertEqual(archive.read("fort-wallet/wheels/keri.whl"), b"PK\x03\x04")
            self.assertIsNone(archive.testzip())

    def test_is_the_same_zip_every_time(self):
        first = tool.build(self.payload, self.script, self.readme, self.output)
        other = Path(self.temp.name) / "again.zip"
        os.utime(self.payload / "app" / "index.html", (1, 1))
        self.assertEqual(tool.build(self.payload, self.script, self.readme, other), first)
        self.assertEqual(self.output.read_bytes(), other.read_bytes())

    def test_refuses_a_payload_that_is_not_a_wallet(self):
        (self.payload / "runtime-closure.json").unlink()
        with self.assertRaisesRegex(tool.NotAPackage, "no runtime-closure.json"):
            tool.build(self.payload, self.script, self.readme, self.output)

    def test_refuses_a_link(self):
        link = self.payload / "app" / "leak.txt"
        try:
            link.symlink_to(self.readme)
        except OSError:
            self.skipTest("this system makes no symbolic links")
        with self.assertRaisesRegex(tool.NotAPackage, "not a plain file"):
            tool.build(self.payload, self.script, self.readme, self.output)

    def test_refuses_a_link_to_a_folder(self):
        link = self.payload / "app" / "linked"
        try:
            link.symlink_to(self.payload / "vendor", target_is_directory=True)
        except OSError:
            self.skipTest("this system makes no symbolic links")
        with self.assertRaisesRegex(tool.NotAPackage, "app/linked is not a plain file"):
            tool.build(self.payload, self.script, self.readme, self.output)
        self.assertFalse(self.output.exists())

    def test_refuses_a_start_script_or_a_note_that_is_not_a_plain_file(self):
        target = Path(self.temp.name) / "target.txt"
        target.write_text("target")
        link = Path(self.temp.name) / "link.txt"
        try:
            link.symlink_to(target)
        except OSError:
            link = None
        folder = Path(self.temp.name) / "folder"
        folder.mkdir()
        for bad in [path for path in (link, folder) if path]:
            with self.assertRaisesRegex(tool.NotAPackage, "start script is not a plain file"):
                tool.build(self.payload, bad, self.readme, self.output)
            with self.assertRaisesRegex(tool.NotAPackage, "readme is not a plain file"):
                tool.build(self.payload, self.script, bad, self.output)
        self.assertFalse(self.output.exists())

    def test_the_program_prints_the_digest_and_fails_plainly(self):
        self.assertEqual(tool.main(["--payload", str(self.payload), "--start-script", str(self.script),
                                    "--readme", str(self.readme), "--output", str(self.output)]), 0)
        self.assertEqual(tool.main(["--payload", str(self.payload / "missing"), "--start-script", str(self.script),
                                    "--readme", str(self.readme), "--output", str(self.output)]), 1)


if __name__ == "__main__":
    unittest.main()
