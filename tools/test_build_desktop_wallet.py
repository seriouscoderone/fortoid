"""tools/build_desktop_wallet.py: the zip a person downloads to run the wallet on a computer."""
import contextlib
import hashlib
import importlib.util
import io
import os
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock

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
        with self.assertRaisesRegex(tool.NotAPackage, "'app/linked' is not a plain file"):
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

    def test_refuses_an_entry_of_the_wrong_kind_or_an_empty_folder(self):
        def refused(message):
            with self.assertRaisesRegex(tool.NotAPackage, message):
                tool.build(self.payload, self.script, self.readme, self.output)
            self.assertFalse(self.output.exists())

        app = self.payload / "app"
        moved = self.payload / "app-moved"
        app.rename(moved)
        app.write_text("not a folder")
        refused("app is not a folder")
        app.unlink()
        moved.rename(app)

        toml = self.payload / "pyscript-ci.toml"
        text = toml.read_text()
        toml.unlink()
        toml.mkdir()
        refused("pyscript-ci.toml is not a file")
        toml.rmdir()
        toml.write_text(text)

        for name in ("vendor", "wheels"):
            folder = self.payload / name
            held = folder.rename(self.payload / (name + "-held"))
            folder.mkdir()
            refused(f"{name} holds no file")
            (folder / "empty" / "deeper").mkdir(parents=True)
            refused(f"{name} holds no file")
            for leftover in sorted(folder.rglob("*"), reverse=True):
                leftover.rmdir()
            folder.rmdir()
            held.rename(folder)
        tool.build(self.payload, self.script, self.readme, self.output)

    @unittest.skipIf(sys.platform == "win32", "Windows can make no such file name (c: there is a drive)")
    def test_refuses_a_file_name_an_unzip_program_could_misread(self):
        for odd in ("a\\b.txt", "c:d.txt", "e\x01f.txt", "g\x7fh.txt"):
            with self.subTest(name=odd):
                path = self.payload / "app" / odd
                try:
                    path.write_text("x")
                except OSError:
                    self.skipTest("this system cannot hold such a file name")
                shown = ascii(f"app/{odd}")
                with self.assertRaisesRegex(tool.NotAPackage, "not allowed") as caught:
                    tool.build(self.payload, self.script, self.readme, self.output)
                self.assertIn(shown, str(caught.exception))
                self.assertFalse(self.output.exists())
                self.output.write_bytes(b"older")
                errors = io.StringIO()
                with contextlib.redirect_stderr(errors):
                    code = tool.main(["--payload", str(self.payload), "--start-script", str(self.script),
                                      "--readme", str(self.readme), "--output", str(self.output)])
                self.assertEqual(code, 1)
                self.assertIn(shown, errors.getvalue())
                self.assertEqual(self.output.read_bytes(), b"older")
                self.output.unlink()
                path.unlink()

    def test_refuses_a_file_name_that_is_not_utf8(self):
        try:
            with open(os.fsencode(self.payload / "app") + b"/bad\xff\xfe.txt", "wb") as file:
                file.write(b"x")
        except (OSError, TypeError, ValueError):
            self.skipTest("this system cannot hold a file name that is not UTF-8")
        self.output.write_bytes(b"older")
        code, out, err = self.run_main()
        self.assertEqual((code, out), (1, ""))
        self.assertEqual(err, "error: the file name 'app/bad\\udcff\\udcfe.txt' is not allowed\n")
        self.assertEqual(self.output.read_bytes(), b"older")

    def test_shows_every_name_from_the_payload_through_ascii(self):
        odd = "x\n::error::made up by a file name\x1b[2J"
        shown = ascii(f"app/{odd}")
        try:
            (self.payload / "app" / odd).symlink_to(self.readme)
        except OSError:
            self.skipTest("this system makes no symbolic links or cannot hold such a name")
        code, out, err = self.run_main()
        self.assertEqual((code, out), (1, ""))
        self.assertEqual(err, f"error: {shown} is not a plain file\n")
        (self.payload / "app" / odd).unlink()

        if hasattr(os, "mkfifo"):
            os.mkfifo(self.payload / "app" / odd)
            code, out, err = self.run_main()
            self.assertEqual((code, out), (1, ""))
            self.assertEqual(err, f"error: {shown} is not a plain file\n")
            (self.payload / "app" / odd).unlink()

        unread = self.payload / "app" / "é‮.js"
        unread.write_text("x")
        real_read_bytes = Path.read_bytes

        def read_bytes(path):
            if path == unread:
                raise PermissionError(13, "Permission denied", str(path))
            return real_read_bytes(path)

        with mock.patch.object(Path, "read_bytes", read_bytes):
            code, out, err = self.run_main()
        self.assertEqual((code, out), (1, ""))
        self.assertEqual(err, "error: 'app/\\xe9\\u202e.js' cannot be read: Permission denied\n")
        self.assertFalse(self.output.exists())

    def test_a_failed_write_leaves_the_output_path_as_it_was(self):
        real_writestr = zipfile.ZipFile.writestr

        def writestr(archive, info, data, *args, **kwargs):
            if len(archive.infolist()) == 2:
                raise OSError(5, "Input/output error")
            return real_writestr(archive, info, data, *args, **kwargs)

        for older in (None, b"older"):
            with self.subTest(older=older):
                if older is not None:
                    self.output.write_bytes(older)
                before = sorted(os.listdir(self.temp.name))
                with mock.patch.object(zipfile.ZipFile, "writestr", writestr):
                    code, out, err = self.run_main()
                self.assertEqual((code, out, err), (1, "", "error: [Errno 5] Input/output error\n"))
                self.assertEqual(sorted(os.listdir(self.temp.name)), before)
                if older is None:
                    self.assertFalse(self.output.exists())
                else:
                    self.assertEqual(self.output.read_bytes(), older)

    def test_refuses_a_folder_that_cannot_be_listed(self):
        real_scandir = os.scandir
        locked = self.payload / "app" / "runtime"

        def scandir(path):
            if Path(path) == locked:
                raise PermissionError(13, "Permission denied", str(path))
            return real_scandir(path)

        with mock.patch.object(os, "scandir", scandir):
            code, out, err = self.run_main()
        self.assertEqual((code, out), (1, ""))
        self.assertEqual(err, "error: the folder 'app/runtime' cannot be listed: Permission denied\n")
        self.assertFalse(self.output.exists())

    def test_refuses_a_real_folder_that_cannot_be_listed(self):
        locked = self.payload / "app" / "locked"
        locked.mkdir()
        (locked / "needed.js").write_text("needed")
        locked.chmod(0)
        self.addCleanup(locked.chmod, 0o755)
        try:
            os.listdir(locked)
        except PermissionError:
            pass
        else:
            self.skipTest("this user can list a folder of mode 000 (root can)")
        code, out, err = self.run_main()
        self.assertEqual((code, out), (1, ""))
        self.assertEqual(err, "error: the folder 'app/locked' cannot be listed: Permission denied\n")
        self.assertFalse(self.output.exists())

    def run_main(self, payload=None, script=None, readme=None, output=None):
        """Runs the program; returns its exit code, standard output and standard error."""
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = tool.main(["--payload", str(payload or self.payload), "--start-script", str(script or self.script),
                              "--readme", str(readme or self.readme), "--output", str(output or self.output)])
        return code, out.getvalue(), err.getvalue()

    def test_the_program_prints_the_digest_and_fails_plainly(self):
        code, out, err = self.run_main()
        self.assertEqual(code, 0)
        self.assertEqual(out, f"DESKTOP_WALLET_SHA256={hashlib.sha256(self.output.read_bytes()).hexdigest()}\n")
        self.assertEqual(err, "")
        self.output.unlink()
        code, out, err = self.run_main(payload=self.payload / "missing")
        self.assertEqual(code, 1)
        self.assertEqual(out, "")
        self.assertEqual(err, "error: the payload has no app\n")


if __name__ == "__main__":
    unittest.main()
