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
import stat
import sys
import zipfile
from pathlib import Path

FOLDER = "fort-wallet"
# What a wallet package holds at its top (scripts/start_wallet.py in the wallet's repository serves exactly these).
PACKAGE = ("app", "vendor", "wheels", "pyscript-ci.toml", "runtime-closure.json")
STAMP = (2026, 1, 1, 0, 0, 0)  # a fixed time, so the zip does not change from one build to the next


class NotAPackage(Exception):
    pass


def package_files(payload: Path) -> list[tuple[str, Path]]:
    """Every file of the wallet in `payload`, as (path inside the folder, file), sorted."""
    found: list[tuple[str, Path]] = []
    for name in PACKAGE:
        top = payload / name
        if top.is_symlink() or not top.exists():
            raise NotAPackage(f"the payload has no {name}")
        files = [top] if top.is_file() else sorted(path for path in top.rglob("*") if path.is_symlink() or not path.is_dir())
        for path in files:
            if path.is_symlink() or not path.is_file():
                raise NotAPackage(f"{path.relative_to(payload).as_posix()} is not a plain file")
            found.append((path.relative_to(payload).as_posix(), path))
    return sorted(found)


def build(payload: Path, start_script: Path, readme: Path, output: Path) -> str:
    """Writes the zip and returns its SHA-256."""
    entries = [(name, path.read_bytes()) for name, path in package_files(payload)]
    entries.append(("start-wallet.py", start_script.read_bytes()))
    entries.append(("README.txt", readme.read_bytes()))
    with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(entries):
            info = zipfile.ZipInfo(f"{FOLDER}/{name}", date_time=STAMP)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (stat.S_IFREG | 0o644) << 16
            archive.writestr(info, data)
    return hashlib.sha256(output.read_bytes()).hexdigest()


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
