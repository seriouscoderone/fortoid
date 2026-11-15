#!/usr/bin/env python3
"""Builds the wallet for a computer: one zip with the wallet, a start script and a note.

    python3 tools/build_desktop_wallet.py --payload app/src/main/assets/payload \\
        --start-script .deps/fortweb/scripts/start_wallet.py --readme desktop/README.txt \\
        --output fort-wallet-desktop.zip

The wallet is taken from the payload that was imported for the APK, so the phone and the computer get the same
build. Only what a wallet package holds is copied; the importer's own files are left out. Built again from the
same files with the same Python, the zip is the same byte for byte.
"""
from __future__ import annotations

import argparse
import hashlib
import os
import secrets
import stat
import sys
import zipfile
from pathlib import Path

FOLDER = "fort-wallet"
# What a wallet package holds at its top (scripts/start_wallet.py in the wallet's repository serves exactly these).
PACKAGE = ("app", "vendor", "wheels", "pyscript-ci.toml", "runtime-closure.json")
FOLDERS = ("app", "vendor", "wheels")  # the other entries are files
STAMP = (2026, 1, 1, 0, 0, 0)  # a fixed time, so the zip does not change from one build to the next


class NotAPackage(Exception):
    pass


def mode_of(path: Path, payload: Path) -> int:
    """The mode of `path` itself (a link is not followed). A failure is refused with the name shown through ascii()."""
    try:
        return path.lstat().st_mode
    except OSError as error:
        shown = ascii(path.relative_to(payload).as_posix())
        raise NotAPackage(f"{shown} cannot be looked at: {error.strerror or error}") from error


def files_in(folder: Path, payload: Path) -> list[Path]:
    """Every path under `folder` that is not a folder; a link is listed, not followed. Any listing error stops it."""
    try:
        with os.scandir(folder) as listing:
            paths = [Path(entry.path) for entry in listing]
    except OSError as error:
        shown = ascii(folder.relative_to(payload).as_posix())
        raise NotAPackage(f"the folder {shown} cannot be listed: {error.strerror or error}") from error
    found: list[Path] = []
    for path in paths:
        if not stat.S_ISDIR(mode_of(path, payload)):
            found.append(path)
        else:
            found.extend(files_in(path, payload))
    return found


def package_files(payload: Path) -> list[tuple[str, Path]]:
    """Every file of the wallet in `payload`, as (path inside the folder, file), sorted."""
    found: list[tuple[str, Path]] = []
    for name in PACKAGE:
        top = payload / name
        if top.is_symlink():
            raise NotAPackage(f"{name} is a link")
        if not top.exists():
            raise NotAPackage(f"the payload has no {name}")
        if name in FOLDERS:
            if not top.is_dir():
                raise NotAPackage(f"{name} is not a folder")
            files = sorted(files_in(top, payload))
            if not files:
                raise NotAPackage(f"{name} holds no file")
        else:
            if not top.is_file():
                raise NotAPackage(f"{name} is not a file")
            files = [top]
        for path in files:
            inside = path.relative_to(payload).as_posix()
            if not stat.S_ISREG(mode_of(path, payload)):
                raise NotAPackage(f"{ascii(inside)} is not a plain file")
            # An unzip program on Windows reads \\ as a separator (and : as a drive), so such a name could land elsewhere.
            # A name that is not UTF-8 reaches Python with surrogates, which a zip cannot hold.
            if any(char in "\\:" or ord(char) < 0x20 or ord(char) == 0x7F or 0xD800 <= ord(char) <= 0xDFFF
                   for char in inside):
                raise NotAPackage(f"the file name {ascii(inside)} is not allowed")
            found.append((inside, path))
    return sorted(found)


def same_file(one: Path, other: Path) -> bool:
    try:
        return os.path.samefile(one, other)
    except OSError:
        return False


def check_output(payload: Path, start_script: Path, readme: Path, output: Path) -> None:
    """Refuses an output that would be packed by the next build or would replace an input."""
    place = output.resolve()
    if any(same_file(folder, payload) for folder in (place, *place.parents)):
        raise NotAPackage(f"the output {ascii(str(output))} is inside the payload")
    for label, path in (("start script", start_script), ("readme", readme)):
        if place == path.resolve() or same_file(output, path):
            raise NotAPackage(f"the output is the {label}")


def build(payload: Path, start_script: Path, readme: Path, output: Path) -> str:
    """Writes the zip and returns its SHA-256."""
    check_output(payload, start_script, readme, output)
    for label, path in (("start script", start_script), ("readme", readme)):
        if not path.is_symlink() and not path.exists():
            raise NotAPackage(f"the {label} {ascii(str(path))} does not exist")
        if path.is_symlink() or not path.is_file():
            raise NotAPackage(f"the {label} is not a plain file")
    entries = []
    for name, path in package_files(payload):
        try:
            entries.append((name, path.read_bytes()))
        except OSError as error:
            raise NotAPackage(f"{ascii(name)} cannot be read: {error.strerror or error}") from error
    entries.append(("start-wallet.py", start_script.read_bytes()))
    entries.append(("README.txt", readme.read_bytes()))
    # The zip is written beside the output and put in its place only when complete.
    partial = output.with_name(f".{output.name}.{secrets.token_hex(8)}.partial")
    try:
        with open(partial, "xb") as file, \
                zipfile.ZipFile(file, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
            for name, data in sorted(entries):
                info = zipfile.ZipInfo(f"{FOLDER}/{name}", date_time=STAMP)
                info.compress_type = zipfile.ZIP_DEFLATED
                info.create_system = 3  # Unix, whatever system builds it (Python writes 0 on Windows)
                info.external_attr = (stat.S_IFREG | 0o644) << 16
                archive.writestr(info, data, compresslevel=9)  # a ZipInfo's own level is unset otherwise
        digest = hashlib.sha256(partial.read_bytes()).hexdigest()
        os.replace(partial, output)
    except BaseException:
        partial.unlink(missing_ok=True)
        raise
    return digest


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Build the wallet for a computer.")
    parser.add_argument("--payload", type=Path, required=True, help="the imported wallet payload")
    parser.add_argument("--start-script", type=Path, required=True, help="scripts/start_wallet.py of the pinned wallet")
    parser.add_argument("--readme", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        digest = build(args.payload, args.start_script, args.readme, args.output)
    except (NotAPackage, OSError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    print(f"DESKTOP_WALLET_SHA256={digest}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
