# First Browser Demonstration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use beads-superpowers:subagent-driven-development (recommended) or beads-superpowers:executing-plans to implement this plan task-by-task. Each Task becomes a bead (`bd create -t task --parent <epic-id>`). Steps within tasks use checkbox (`- [ ]`) syntax for human readability.

**Goal:** In the wallet, open a web address, see the roles an organization publishes there (every definition checked against its SAID), and take one on with an identifier, with a "Not appointed yet" warning.

**Architecture:** The wallet's Python engine (Pyodide worker) reads a `mad-discovery` source with the web's own fetch, applies `mad-discovery`'s address, document and SAID rules, and derives roles from the checked definitions. Roles taken on are stored in the vault's own database, with the definition bytes, so they work offline. A new Roles page (TypeScript) is the landing page after unlock.

**Tech Stack:** FortWeb (`/home/user/fortweb`, branch `dev`): TypeScript ES modules, Python on Pyodide with KERIpy (`keri.db.koming.Komer` on `WebBaser`), `blake3` wheel, Playwright with the real runtime. Fortoid (`/home/user/fortoid`): pin + APK. Spec: `.internal/specs/2026-11-12-first-browser-demonstration-design.md`.

## Global Constraints

- Work on `dev` in every fork; never commit to `main`. Very small commits, pushed as you go. No pull requests.
- Commit messages include the bead id and end with the attribution lines your session's instructions give you.
- KERI-only identity. Nothing here adds a login of any kind.
- Nothing fetched from a source is executed, rendered as markup, or turned into a link. Render with `escapeHtml` (`app/shared/dom.ts`) or `textContent`.
- Address rules (from `mad-discovery` `src/address.ts`): https only, at most 512 characters, no user name, password, query, fragment or port, no IP literal, no `localhost` or `*.localhost`, host not ending in `.`, path ends `/source.json`. The wallet is stricter in two ways, because it hands the address to the browser's fetch and cannot check what a name resolves to: a host needs at least two labels and a last label that starts with a letter (this refuses every shorthand a browser would read as an IP address, such as `2130706433` or `127.1`), and a path may hold only letters, digits, `.`, `_`, `~`, `-` and `/`, with no empty, `.` or `..` segment.
- Limits (from `mad-discovery` `src/limits.ts`): one file at most 1,000,000 bytes; one request 10,000 ms; at most 50 definitions per read; one read 60,000 ms; JSON nesting at most 64; publication name 200, description 2000, at most 32 tags of 64, collection 200 characters, no C0/C1 control characters. Wallet-only limits added in review: at most 4,000,000 bytes per read over the source document and all definitions (a publication cut by it gets `problem: "limit"`), and 3,000 ms for the probe that tells "not readable" from "unreachable".
- Text rules added in the Task 6 review. JSON whose strings (member names or values) include a lone surrogate is refused wherever the wallet parses a source document or a definition: such text cannot be stored or sent as UTF-8. `mad-discovery` accepts it, so this is a third way the wallet is stricter. A string taken from a definition for display (`role`, `alias`, `name`, `as`) is used only if it is at most 200 UTF-16 units and has no C0/C1 control character; otherwise it is treated as a member of the wrong type.
- A read never holds the worker's request lock (it touches no vault), reads may overlap, and the newest read's result is the one kept for taking on a role.
- Known limitation: a source whose `/source.json` redirects is told "<host> does not allow wallets to read it yet", because the read refuses redirects and the probe then succeeds.
- SAID pattern `^E[A-P][A-Za-z0-9_-]{42}$`; fill is 44 `#`.
- Fetch options: `mode: "cors"`, `credentials: "omit"`, `redirect: "error"`, `referrerPolicy: "no-referrer"`, `cache: "no-store"`; status 200; content type starts with `application/json`.
- Exact user-facing strings are in the spec's table "What the person sees when something is wrong"; the warning text is `Not appointed yet`.
- Start-up UI stays still (see Fortoid `CLAUDE.md`): no animation or timer-driven redraw on the Roles page.
- Build and test commands are in Fortoid `CLAUDE.md` ("Working on the wallet"). Rebuild with `npm run build:runtime` after every `app/` change. Browser tests: `npx playwright test <spec> -c playwright.local.config.ts --trace off`.
- Implementation code is written test-first during execution. This plan fixes files, interfaces, data shapes, rules and test cases; the code for the security-critical checks is given in full in Task 2.

---

### Task 1: `mad-discovery` lets browser wallets read a source

Released 2026-11-12: the owner said yes to the header (decision D1). Commits go to `mad-discovery`'s `main` (it is the owner's own repository, not a fork). Nothing in Tasks 2 to 6 depends on it; browser tests use a routed fixture source. The phone check in Task 7 needs the owner to add the header in CloudFront.

**Files (repository `seriouscoderone/mad-discovery`, cloned with push access at `/home/user/mad-discovery`):**
- Modify: `docs/design.md` (the paragraph "A browser is not expected to read a source"), `README.md` (rulings), `deploy/README.md`
- Modify: `src/fetch.ts`, `src/cli.ts` (`verify` reports the header)
- Test: `tests/fetch.test.ts`, `tests/cli.test.ts`

**Acceptance Criteria:**
- The design states that a source that wants browser wallets to read it sends `Access-Control-Allow-Origin: *` on `source.json` and `definitions/*`.
- `verify <address>` prints whether the source sends that header, and does not fail when it is absent.
- `deploy/README.md` gives the CloudFront step for both distributions: attach the managed response headers policy "SimpleCORS" (ID `60669652-455b-4ae9-85a4-c4c02393f86c`, which adds `Access-Control-Allow-Origin: *` to responses for simple CORS requests) to the cache behavior. Console: Distributions, the distribution, Behaviors, select the behavior, Edit, "Response headers policy", SimpleCORS, Save changes. CLI: `aws cloudfront get-distribution-config --id <id> --output yaml > dist-config.yaml`; in the cache behavior add `ResponseHeadersPolicyId: 60669652-455b-4ae9-85a4-c4c02393f86c`; rename the `ETag` field to `IfMatch`; `aws cloudfront update-distribution --id <id> --cli-input-yaml file://dist-config.yaml`. (Checked against the AWS CloudFront Developer Guide on 2026-11-12.)
- `npm run check` and `npm test` pass. The repository's three rules in its `CLAUDE.md` hold.

---

### Task 2: SAID check and address rules in the wallet engine

**Files (FortWeb):**
- Create: `app/runtime/sourcing.py`
- Create: `python/test_sourcing.py`
- Create: `python/fixtures/mad/compact/<SAID>.json` (copy all 37 files from `mad-discovery` `fixtures/compact/`), `python/fixtures/mad/refused/` (copy `index.json` and its two files)
- Modify: `tools/` runtime closure lists only if the build reports `sourcing.py` as unlisted (run `npm run build:runtime` to find out)

**Interfaces:**
- Produces (module `sourcing`):
  - `class SourceFault(Exception)` with attributes `code: str`, `message: str`. Codes used in this plan: `BAD_ADDRESS`, `UNREACHABLE`, `NOT_READABLE`, `BAD_SOURCE`, `BAD_DEFINITION`, `TOO_MUCH`.
  - `is_said(text: str) -> bool`
  - `encode_said(digest: bytes) -> str`
  - `check_said(served: bytes, said: str, *, digest) -> bool` where `digest` is a callable `bytes -> bytes` returning the 32-byte Blake3-256 digest
  - `normalize_address(text: str) -> dict` returning `{"address": str, "base": str, "host": str}`; raises `SourceFault("BAD_ADDRESS", ...)`
  - `definition_address(base: str, said: str) -> str`
  - constants `FILE_BYTES = 1_000_000`, `REQUEST_MS = 10_000`, `READ_MS = 60_000`, `DEFINITIONS_PER_READ = 50`, `MAX_ADDRESS = 512`, `JSON_DEPTH = 64`

**Acceptance Criteria:**
- Every file in `python/fixtures/mad/compact/` passes `check_said` against the SAID in its file name; both refused vectors fail; a pretty-printed copy fails; a wrong SAID fails.
- `normalize_address` accepts `mad.seriouscoder.one`, `mad.seriouscoder.one/`, `https://mad.seriouscoder.one/source.json`, `example.org/apps` and returns the canonical address ending `/source.json`; it rejects every case in the test table below.
- `python3 -m unittest python.test_sourcing` passes (needs `pip install blake3` in the local venv; the wallet itself uses the bundled wheel).

- [ ] **Step 1: Copy the fixtures**

```bash
cd /home/user/fortweb && mkdir -p python/fixtures/mad/compact python/fixtures/mad/refused
cp /home/user/seriouscoderone/mad-discovery/fixtures/compact/*.json python/fixtures/mad/compact/
cp /home/user/seriouscoderone/mad-discovery/fixtures/refused/* python/fixtures/mad/refused/
~/fwvenv/bin/pip install blake3
```

- [ ] **Step 2: Write the failing tests** (`python/test_sourcing.py`)

```python
"""mad-discovery's address and SAID rules, as the wallet engine applies them."""

from __future__ import annotations

import importlib.util
import json
import unittest
from pathlib import Path

import blake3

REPO = Path(__file__).resolve().parent.parent
FIXTURES = REPO / "python" / "fixtures" / "mad"


def _load():
    spec = importlib.util.spec_from_file_location("fortweb_sourcing_test", REPO / "app" / "runtime" / "sourcing.py")
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def digest(data: bytes) -> bytes:
    return blake3.blake3(data).digest()


class SaidCheckTest(unittest.TestCase):
    def setUp(self):
        self.s = _load()

    def test_every_compact_fixture_verifies(self):
        files = sorted((FIXTURES / "compact").glob("*.json"))
        self.assertEqual(len(files), 37)
        for f in files:
            self.assertTrue(self.s.check_said(f.read_bytes(), f.stem, digest=digest), f.name)

    def test_refused_vectors_fail(self):
        for row in json.loads((FIXTURES / "refused" / "index.json").read_text()):
            served = (FIXTURES / "refused" / row["file"]).read_bytes()
            self.assertFalse(self.s.check_said(served, row["said"], digest=digest), row["file"])

    def test_reformatted_bytes_fail(self):
        f = sorted((FIXTURES / "compact").glob("*.json"))[0]
        pretty = json.dumps(json.loads(f.read_text()), indent=2).encode()
        self.assertFalse(self.s.check_said(pretty, f.stem, digest=digest))

    def test_wrong_or_malformed_said_fails(self):
        files = sorted((FIXTURES / "compact").glob("*.json"))
        self.assertFalse(self.s.check_said(files[0].read_bytes(), files[1].stem, digest=digest))
        self.assertFalse(self.s.check_said(files[0].read_bytes(), "not-a-said", digest=digest))

    def test_encode_said_shape(self):
        said = self.s.encode_said(bytes(32))
        self.assertEqual(said, "E" + "A" * 43)
        self.assertTrue(self.s.is_said(said))
        with self.assertRaises(ValueError):
            self.s.encode_said(b"short")


class AddressTest(unittest.TestCase):
    def setUp(self):
        self.s = _load()

    def test_accepts_and_canonicalizes(self):
        cases = {
            "mad.seriouscoder.one": ("https://mad.seriouscoder.one/source.json", "https://mad.seriouscoder.one/"),
            " mad.seriouscoder.one/ ": ("https://mad.seriouscoder.one/source.json", "https://mad.seriouscoder.one/"),
            "https://mad.seriouscoder.one/source.json": ("https://mad.seriouscoder.one/source.json", "https://mad.seriouscoder.one/"),
            "example.org/apps": ("https://example.org/apps/source.json", "https://example.org/apps/"),
            "EXAMPLE.org/apps/source.json": ("https://example.org/apps/source.json", "https://example.org/apps/"),
        }
        for text, (address, base) in cases.items():
            got = self.s.normalize_address(text)
            self.assertEqual((got["address"], got["base"]), (address, base), text)
            self.assertEqual(got["host"], address.split("/")[2])

    def test_rejects(self):
        bad = [
            "", "   ", "http://example.org", "ftp://example.org", "https://example.org:8443",
            "https://user@example.org", "https://user:pw@example.org", "example.org/?q=1", "example.org/#x",
            "127.0.0.1", "https://[::1]/source.json", "localhost", "app.localhost", "example.org.",
            "exa mple.org", "example.org/" + "a" * 600, "javascript:alert(1)", "//example.org",
        ]
        for text in bad:
            with self.assertRaises(self.s.SourceFault, msg=text) as raised:
                self.s.normalize_address(text)
            self.assertEqual(raised.exception.code, "BAD_ADDRESS", text)

    def test_definition_address(self):
        said = "E" + "A" * 43
        self.assertEqual(self.s.definition_address("https://example.org/apps/", said),
                         f"https://example.org/apps/definitions/{said}.json")
        with self.assertRaises(ValueError):
            self.s.definition_address("https://example.org/apps/", "nope")


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 3: Run them and see them fail**

Run: `cd /home/user/fortweb && ~/fwvenv/bin/python -m unittest python.test_sourcing -v`
Expected: errors, `sourcing.py` does not exist.

- [ ] **Step 4: Implement** (`app/runtime/sourcing.py`)

> **Corrected after review (2026-11-12).** The `normalize_address` below, as first written in this plan, accepted IP shorthands (`2130706433`, `0x7f.1`, `127.1`) and non-canonical paths. The reviewed version is in FortWeb at `982f732`; it adds the two stricter rules now listed in Global Constraints. Use the repository's function, not this listing.

```python
"""Reading a mad-discovery source: the address and SAID rules.

The rules are mad-discovery's (src/address.ts, src/said.ts). Keep them identical:
python/test_sourcing.py runs mad-discovery's own fixtures against this module.
"""

from __future__ import annotations

import base64
import re
from urllib.parse import urlsplit

SAID_PATTERN = re.compile(r"^E[A-P][A-Za-z0-9_-]{42}$")
FILL = b"#" * 44
FILE_BYTES = 1_000_000
REQUEST_MS = 10_000
READ_MS = 60_000
DEFINITIONS_PER_READ = 50
MAX_ADDRESS = 512
JSON_DEPTH = 64
_IPV4 = re.compile(r"^\d+\.\d+\.\d+\.\d+$")
_HOST = re.compile(r"^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$")
_ENDING = "/source.json"


class SourceFault(Exception):
    """A source could not be read. `code` is stable; `message` is for the person."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


def is_said(text) -> bool:
    return isinstance(text, str) and SAID_PATTERN.match(text) is not None


def encode_said(digest: bytes) -> str:
    """One zero byte, then the 32 digest bytes, as base64url; the first character (always A) becomes E."""
    if len(digest) != 32:
        raise ValueError("a Blake3-256 digest is 32 bytes")
    return "E" + base64.urlsafe_b64encode(b"\x00" + digest).decode("ascii")[1:]


def check_said(served: bytes, said: str, *, digest) -> bool:
    """mad-discovery's byte check: present; every occurrence becomes 44 '#'; the digest is the SAID; it rebuilds."""
    if not is_said(said):
        return False
    needle = said.encode("ascii")
    if needle not in served:
        return False
    hashed = served.replace(needle, FILL)
    if encode_said(digest(hashed)) != said:
        return False
    return hashed.replace(FILL, needle) == served


def _bad(text: str):
    return SourceFault("BAD_ADDRESS", "That is not an address the wallet can open.")


def normalize_address(text: str) -> dict:
    """A host, a host with a path, or a full address -> the canonical https address ending /source.json."""
    raw = str(text or "").strip()
    if not raw or any(ch.isspace() for ch in raw) or len(raw) > MAX_ADDRESS:
        raise _bad(raw)
    if "://" not in raw:
        if raw.startswith("/") or ":" in raw.split("/", 1)[0]:
            raise _bad(raw)
        raw = "https://" + raw
    parts = urlsplit(raw)
    if parts.scheme != "https" or parts.query or parts.fragment or "?" in raw or "#" in raw:
        raise _bad(raw)
    if parts.username is not None or parts.password is not None or "@" in parts.netloc:
        raise _bad(raw)
    try:
        port = parts.port
    except ValueError:
        raise _bad(raw) from None
    host = (parts.hostname or "").lower()
    if port is not None or ":" in parts.netloc or not host or host.endswith("."):
        raise _bad(raw)
    if _IPV4.match(host) or host.startswith("[") or host == "localhost" or host.endswith(".localhost"):
        raise _bad(raw)
    if not _HOST.match(host):
        raise _bad(raw)
    path = parts.path or "/"
    if not path.endswith(_ENDING):
        path = path.rstrip("/") + _ENDING
    address = f"https://{host}{path}"
    if len(address) > MAX_ADDRESS:
        raise _bad(raw)
    return {"address": address, "base": address[: -len("source.json")], "host": host}


def definition_address(base: str, said: str) -> str:
    if not is_said(said):
        raise ValueError(f"not a SAID: {said}")
    if not base.endswith("/"):
        raise ValueError("a base ends with /")
    return f"{base}definitions/{said}.json"
```

- [ ] **Step 5: Run the tests and see them pass**

Run: `~/fwvenv/bin/python -m unittest python.test_sourcing -v`
Expected: all pass. If an address case fails, fix the function, not the table: the table is the rule.

- [ ] **Step 6: Commit and push**

```bash
git add app/runtime/sourcing.py python/test_sourcing.py python/fixtures/mad
git commit   # "feat(runtime): mad-discovery address and SAID rules in the wallet engine (<bead>)"
git push origin dev
```

---

### Task 3: Source document rules and role derivation

**Files (FortWeb):**
- Modify: `app/runtime/sourcing.py`
- Modify: `python/test_sourcing.py`
- Create: `python/fixtures/mad/source.json` (the live source document fetched 2026-11-12 from `https://mad.seriouscoder.one/source.json`, unchanged; a copy is in the session scratchpad under `livecheck/source.json`)

**Interfaces:**
- Consumes: `SourceFault`, `is_said`, `JSON_DEPTH` from Task 2.
- Produces:
  - `read_source_document(raw: bytes, address: str) -> dict` returning `{"document": {"source": str, "by": str | None, "oobi": str | None, "serves": {said: [refers...]}, "publications": [{"target","name","description","tags","collection"}], "introductions": int, "catalogs": int}, "dropped": {"entries": int, "publications": int}}`; raises `SourceFault("BAD_SOURCE", "<host> did not return a valid source.")`
  - `closure(serves: dict, target: str) -> list[str]`: the target and everything reachable through `refers`, sorted; `[]` when the target is not served
  - `roles_of_selection(definitions: dict[str, dict], target: str) -> dict` returning `{"title": str, "roles": [{"role": str, "responsibilities": [{"said": str, "alias": str, "reliesOn": [{"said","alias","kind","as"}], "attests": [{"said","alias","kind"}], "outcome": {"said","alias","kind"} | None}]}]}`; roles sorted by name, responsibilities in the selection's `rrs` order; a referenced SAID with no definition gets alias `""` and kind `""`

**Rules to implement (from `mad-discovery` `src/document.ts`, `src/source.ts`):**
- Parse JSON with nesting at most 64; the top level must be an object.
- `source` must equal `address`, else fault.
- `by`, `oobi` must be strings if present; `pages` a boolean; `serves`, `publications`, `introductions`, `catalogs` lists. A known member of the wrong type faults. Unknown members are ignored.
- `serves`: each entry an object with a SAID `said` not seen before; `refers` a list (default empty) of SAIDs other than `said`, without duplicates. A bad entry or bad `refers` item is dropped and counted in `dropped.entries`.
- `publications`: keep only the members `target`, `by`, `name`, `description`, `tags`, `collection`; `target` a SAID; texts are strings within the limits and without control characters `[\u0000-\u001f\u007f-\u009f]`; at most 32 tags. A publication that breaks a rule is dropped and counted in `dropped.publications`.
- `by` is kept only if it matches `^[A-Za-z0-9_-]{1,64}$`; `oobi` only if it is an https address under the address rules without the `/source.json` ending. Otherwise it is dropped and counted.
- `introductions` and `catalogs` are only counted (decision D5).
- Role derivation: the definition at `target` must have `kind == "selection"` and a list `rrs`; otherwise `roles` is empty and `title` is the publication name given by the caller (pass `""` here). `title` is the selection's `name` if it is a string, else its `alias`, else `""`. For each SAID in `rrs` whose definition has `kind == "rr"` and a string `role`: `alias` from the rr; `reliesOn` from `rely_on` items (`said`, `as`), `attests` from `attest` SAIDs, `outcome` from `outcome.said`. Names and kinds of referenced definitions come from `definitions`.

**Acceptance Criteria:**
- The fixture `source.json` read with its own address gives 3 publications, 26 served entries, 0 dropped.
- A document whose `source` differs, or whose `serves` is an object, faults with `BAD_SOURCE`.
- A serves entry with a malformed SAID and a publication with a 201-character name are each dropped and counted, and the rest is kept.
- `closure` of the `dropin-class` target returns only SAIDs that are in `serves`, includes the target, and has no duplicates.
- `roles_of_selection` for `dropin-class` returns the roles `cashier`, `front-desk`, `instructor`, `patron`, `recorder`; the instructor's `class-attended` relies on `enrollment` (kind `authorization`, as `seat`) and `attended` (kind `fact`, as `visit`), attests `class-attended`, and has outcome `class-attended` (kind `fact`).

- [ ] **Step 1:** Add test classes `SourceDocumentTest`, `ClosureTest`, `RolesTest` to `python/test_sourcing.py`, one test per acceptance criterion above, loading `python/fixtures/mad/source.json` and the compact definitions by SAID.
- [ ] **Step 2:** Run `~/fwvenv/bin/python -m unittest python.test_sourcing -v`; the new tests fail (functions missing).
- [ ] **Step 3:** Implement the three functions in `sourcing.py` following the rules above. Bounded JSON parsing: reject input whose bracket nesting exceeds 64 before calling `json.loads` (scan bytes outside strings, counting `{`/`[`).
- [ ] **Step 4:** Run the tests; all pass.
- [ ] **Step 5:** Commit and push ("feat(runtime): source document rules and role derivation").

---

### Task 4: Reading a source over the network: `sources.read`

**Files (FortWeb):**
- Modify: `app/runtime/sourcing.py` (the read orchestration, no `js` import)
- Create: `app/runtime/roling.py` (runtime methods; imports `js`, `sourcing`, `vaulting`)
- Modify: `app/runtime/wallet-worker.py` (`import roling`; add `"sources.read"`, `"roles.list"`, `"roles.takeOn"`, `"roles.remove"` to `ALLOWED_METHODS`; in `_dispatch` route methods starting `sources.` or `roles.` to `roling.dispatch`)
- Modify: `app/runtime/method-catalog.ts` (`sourcesRead: "sources.read"`, `rolesList: "roles.list"`, `rolesTakeOn: "roles.takeOn"`, `rolesRemove: "roles.remove"`)
- Modify: whichever build list names the runtime's Python files (find it with `grep -rn "vaulting.py" tools pyscript-ci.toml app/runtime/*.toml`), so `sourcing.py` and `roling.py` are packaged and loaded
- Test: `python/test_sourcing.py` (orchestration with a fake fetch), `playwright/fortweb-roles.spec.ts`, `playwright/fixtures/mad-source.ts`
- Modify: `package.json` (add the new spec to `test:e2e:prepared-runtime`)

**Interfaces:**
- Consumes: Tasks 2 and 3.
- Produces:
  - `async sourcing.read_source(text: str, *, fetch_bytes, digest, clock) -> dict` where `fetch_bytes(url: str, timeout_ms: int) -> bytes` raises `SourceFault`, and `clock() -> float` returns milliseconds. Returns
    `{"source": {"address","host","by","oobi","introductions","catalogs"}, "checked": int, "dropped": {"entries","publications"}, "publications": [{"target","name","description","tags","collection","title","roles":[...],"problem": str | None}], "definitions": {said: str}}`.
    `problem` is `None`, `"mismatch"` (a definition failed its SAID check or was not served), or `"limit"` (the read's definition limit was reached before this publication). A publication with a problem has `roles == []`.
  - `async roling.dispatch(method: str, params: dict) -> dict`. `sources.read` takes `{"address": str}` and returns the dictionary above without `"definitions"`; it keeps the full result in the module variable `_LAST_READ` for Task 6.
  - `SourceFault` codes map to the wallet's existing `vaulting.RuntimeFault` codes: `BAD_ADDRESS` to `VALIDATION`; `UNREACHABLE` and `NOT_READABLE` to `NETWORK_ERROR`; `BAD_SOURCE` and `TOO_MUCH` to `BAD_RESPONSE`. Never `TIMEOUT`, which makes the bridge restart the worker. The message is the spec's sentence with the host filled in. (Decided during execution: the plan first named a new code `UPSTREAM`; the existing two already mean this.)
  - `app/runtime/bridge.ts`: `sources.read` gets its own entry in `METHOD_TIMEOUT_MS`, 75,000 ms, because a read may take 60 s and the default is 30 s.
- `roling._fetch_bytes(url, timeout_ms)`: `js.fetch` with the fetch options in Global Constraints and an `AbortController` timeout; requires status 200 and a JSON content type; reads `response.body` with a reader and stops with `TOO_MUCH` past 1,000,000 bytes. When the fetch rejects, it makes one probe with `mode: "no-cors"`: if the probe resolves, raise `NOT_READABLE` ("<host> does not allow wallets to read it yet."), else `UNREACHABLE` ("Could not reach <host>.").
- The digest is `blake3.blake3(data).digest()` from the bundled wheel.

**Read order:** normalize; fetch and validate the source document; for publications in document order, take `closure(serves, target)`; fetch each not-yet-fetched definition from `definition_address(base, said)` until 50 have been fetched in this read; check each against its SAID; a publication whose closure is empty, holds a failed definition, or was cut by the limit gets its `problem`; derive roles for the others. The whole read stops with `UNREACHABLE` if `clock()` passes 60,000 ms.

**Browser test fixture (`playwright/fixtures/mad-source.ts`):** exports `routeFixtureSource(context, options)` which uses `context.route("https://mad.example.test/**", ...)` to serve `source.json` (the fixture document with `source` set to `https://mad.example.test/source.json`) and `definitions/<SAID>.json` from `python/fixtures/mad/compact/`, with `access-control-allow-origin: *`. Options: `cors: false` omits the header; `tamper: "<SAID>"` serves that definition with one byte changed; `oversize: true` serves a 1,000,001-byte source document. Playwright intercepts fetches made by the worker (verified 2026-11-12).

**Acceptance Criteria (browser tests call the runtime method through the page's bridge, UI comes in Task 5):**
- With the fixture source, `sources.read {address: "mad.example.test"}` returns host `mad.example.test`, 3 publications, `checked` 26, and the `dropin-class` publication's roles as in Task 3.
- `cors: false` gives the `NETWORK_ERROR` error "mad.example.test does not allow wallets to read it yet."
- `tamper` on an rr of `class-brick` gives every publication that uses it `problem: "mismatch"` and no roles (in this fixture all three use both of its rrs); tampering a definition only `dropin-class` uses leaves the other two with their roles.
- `oversize` gives "mad.example.test publishes more than the wallet reads in one go."
- An unroutable host gives "Could not reach <host>."
- `sources.read {address: "http://x"}` gives the `VALIDATION` error "That is not an address the wallet can open."
- Python unit tests cover the orchestration with a fake `fetch_bytes`: the 50-definition limit sets `problem: "limit"`, and the 60 s clock stops the read.
- `npm run typecheck` passes; the existing 60 browser tests still pass.

- [ ] **Step 1:** Python unit tests for `read_source` with a fake fetch (success, limit, clock, mismatch). See them fail.
- [ ] **Step 2:** Implement `read_source`. Tests pass. Commit.
- [ ] **Step 3:** Write `playwright/fixtures/mad-source.ts` and `playwright/fortweb-roles.spec.ts` with one test per browser criterion. Each test sets up a wallet through the "Set up your wallet" page (as `playwright/fortweb-wallet-first.spec.ts` does), then calls the runtime through the same bridge object the page uses. See them fail (`sources.read` is not an allowed method).
- [ ] **Step 4:** Implement `roling.py` (`dispatch`, `_fetch_bytes`, the fault mapping), wire the worker, the method catalog and the packaging list. `npm run build:runtime`. Tests pass.
- [ ] **Step 5:** Add the spec to `test:e2e:prepared-runtime`; run the whole prepared-runtime suite and `npm run typecheck`. Commit and push.

---

### Task 5: The Roles page: open an address, see publications and roles

**Files (FortWeb):**
- Create: `app/features/roles/roles-page.ts`
- Modify: `app/app/router.ts` (route `roles`, `/vaults/<id>/roles`, same flags as `identifiers`; `rolesHref(vaultId)`)
- Modify: `app/app/page-factory.ts`, `app/app/page-loader.ts` (route data: `roles.list`, `identifiers.list`), `app/app/main.ts` (actions `readSource`, `takeOnRole`, `removeRole`; the landing page after unlock and after launch routing becomes `rolesHref`, replacing the `identifiersHref` default)
- Modify: `app/app/shell.ts` (navigation: "Roles" first, then Identifiers, Remotes, Settings)
- Modify: `app/styles/components.css`
- Test: `playwright/fortweb-roles.spec.ts`; update existing specs that assert the landing heading "Local Identifiers" after unlock

**Interfaces:**
- Consumes: `sources.read` (Task 4).
- Produces: `renderRolesPage(props)` returning the page record used by `page-factory.ts`; `props` holds `vault`, `roles` (from `roles.list`, empty until Task 6), `identifiers`, and the actions `onReadSource(address) -> Promise<SourceView>`, `onTakeOn(...)`, `onRemove(id)`.

**Page content:**
- Heading "Roles". A form with a labelled text field "Address" (placeholder `example.org`), a button "Open", and a status line.
- After a read: the host as a heading, a line "<n> publications, <m> definitions checked", "Unverified: says it is run by <by>" when the source states `by`, and one card per publication with its name, description, collection and tags. Opening a card (a button "Show roles") lists its roles; each role lists its responsibilities as "<alias>: relies on <names>; attests <names>; produces <name>" with empty parts left out, and a button "Take on" (wired in Task 6; disabled until then). A publication with `problem == "mismatch"` shows "A definition from <host> does not match its identifier." and no roles; `"limit"` shows "<host> publishes more than the wallet reads in one go."
- Section "My roles" with the empty state "No roles yet. Open an address to see what you can take on."
- No animation; the status line shows "Reading..." as plain text while the read runs, and the Open button is disabled. (Decided during execution: the page does not echo what was typed before the engine has validated it.)

**Acceptance Criteria:**
- After setting up or unlocking a wallet, the page shown is Roles, with the empty state.
- Typing `mad.example.test` and pressing Open (or Enter) shows the host, "3 publications, 26 definitions checked" and three cards.
- "Show roles" on the drop-in publication lists cashier, front-desk, instructor, patron and recorder; the instructor shows "class-attended: relies on enrollment (as seat), attended (as visit); attests class-attended; produces class-attended". (Decided during execution: a relied-on item shows what it is relied on as.)
- The error cases of Task 4 show their sentence in the status line, announced to assistive technology (`announce` from `app/ui/core/a11y.ts`).
- A source whose publication name contains `<img src=x onerror=alert(1)>` shows that text literally and creates no element (assert no `img` inside the page section and no dialog event).
- The accessibility spec (`playwright/fortweb-accessibility.spec.ts`) passes with the Roles page added to its routes.
- All existing browser tests pass, with landing expectations updated to Roles.

- [ ] **Step 1:** Browser tests for each criterion. See them fail.
- [ ] **Step 2:** Route, navigation, page, loader, actions, landing change, styles. Rebuild. Tests pass.
- [ ] **Step 3:** Whole prepared-runtime suite, `npm run typecheck`. Commit and push.

---

### Task 6: Take on a role, My roles, the warning

**Files (FortWeb):**
- Modify: `app/runtime/roling.py` (store and the three `roles.*` methods)
- Modify: `app/runtime/vaulting.py` (`_build_vault_state`: add the two sub-database names to `baser.SubDbNames` next to `kf_state_subdb`; pass them through `configure_runtime`), `app/runtime/wallet-worker.py` (constants `ROLES_SUBDB = "frol."`, `ROLE_DEFINITIONS_SUBDB = "fdef."`)
- Modify: `app/features/roles/roles-page.ts` (take-on dialog, My roles list, remove)
- Modify: `python/test_vaulting.py` setup calls if `configure_runtime` gains parameters
- Test: `playwright/fortweb-roles.spec.ts`

**Interfaces:**
- Consumes: `_LAST_READ` (Task 4), `vaulting.require_open_state`, `vaulting.persist_and_reload`, `koming.Komer`.
- Store: `Komer(db=hby.db, subkey="frol.", klas=RoleRecord)` keyed by `(id,)`; `Komer(db=hby.db, subkey="fdef.", klas=DefinitionRecord)` keyed by `(said,)`.
  - `RoleRecord`: `id` (uuid4 hex), `address`, `host`, `target`, `publication` (name), `title`, `role`, `responsibilities` (list of rr SAIDs), `definitions` (list of every SAID the role's view needs), `aid`, `taken_at` (ISO 8601 UTC).
  - `DefinitionRecord`: `said`, `text` (the served bytes, decoded as UTF-8).
- Methods:
  - `roles.takeOn {"address": str, "target": str, "role": str, "identifier": {"mode": "new", "alias": str} | {"mode": "existing", "aid": str}}` returns `{"role": <role view>}`. It requires that `_LAST_READ` is for the same normalized address and holds that publication without a problem and that role; otherwise `CONFLICT` "Open the address again before taking on a role." It makes no network request. `mode: "new"` creates the identifier exactly as `identifiers.create` does (alias conflict is `CONFLICT`). The same address, target, role and identifier twice is `CONFLICT` "This identifier has already taken on that role."
  - `roles.list {}` returns `{"roles": [<role view>]}`, newest first. Role view: `{"id","address","host","target","publication","title","role","aid","identifierAlias","takenAt","responsibilities": [...as in Task 3...],"warnings": [{"code": "NOT_APPOINTED", "message": "Not appointed yet"}]}`. The responsibilities are derived from the stored definitions with `roles_of_selection`.
  - `roles.remove {"id": str}` returns `{"removed": true}`; deletes the record and any stored definition no other role lists. Unknown id is `NOT_FOUND`.
  - Each writing method ends with `await vaulting.persist_and_reload()`.
- The last read stays on the Roles page (address, result, which publications are open) until another address is opened or the vault is locked or switched. It is kept in the app's memory only, per open vault, so a re-render (the phone menu, a refresh of My roles after taking on a role) does not wipe it. A read that finishes after its page is gone neither writes nor announces. (Decided after the Task 5 review.)
- Dialog: title "Take on <role>"; radio "Create a new identifier" with a name field prefilled with the role name (a number is appended if that name is taken: `cashier 2`), and radio "Select an existing identifier" with a list of the vault's identifiers. With no identifiers, only the first option is shown. Buttons "Take on" and "Cancel".
- Decided in the Task 6 review (fix round 1):
  - A failed `roles.takeOn` changes nothing. Every refusal comes first; the role's data is written, and so proven storable, before the identifier is made; a failure before the persist removes what the call wrote, emits a diagnostic and answers `BAD_RESPONSE` "The wallet could not keep that role."
  - `roles.takeOn` also takes `"publication": str`, the publication's name as read, so two publications with one target are told apart.
  - A role stores only the definitions the read resolved for its publication, so it reads the same from the store as it did when read.
  - The engine forgets its last read when a vault is opened, locked or closed.
  - `roles.list` treats a stored definition it cannot parse as missing. If `roles.list` itself fails, the Roles page still shows the address box and My roles says "My roles could not be read."
  - `roles.takeOn` has the bridge budget of `identifiers.create` (90,000 ms): it makes an identifier too.
  - Dialog: focus opens on the selected choice; an empty name ("Enter a name for the new identifier.") is announced and focus moves to the name field; a failure that arrives after the dialog was closed is shown on the page. The shared modal composite is not changed.
  - "Remove" buttons are named "Remove <role>, identifier <alias>".
  - The browser tests for roles are three spec files (reading, taking on, the last read), so no file is reported as slow.
- My roles card: role, "<publication> at <host>", "Identifier: <alias>", the warning "Not appointed yet" with the sentence "Nobody has granted this identifier the role. This wallet cannot hold grants yet.", the responsibilities, and a button "Remove".

**Acceptance Criteria:**
- Taking on `cashier` from "Drop-in takes a class" with a new identifier lists it under My roles with host `mad.example.test`, identifier `cashier`, and "Not appointed yet"; the identifier appears on the Identifiers page.
- No request leaves the page while taking on a role (count routed and unrouted requests in the test from the click to the listing).
- After a reload with every request to `mad.example.test` aborted, unlocking shows the role with its responsibilities.
- Taking on `instructor` with "Select an existing identifier" lists it under that identifier; doing it again shows "This identifier has already taken on that role."
- After the worker is discarded (simulate 31 s hidden, as `fortweb-wallet-first.spec.ts` does) and the vault reopened, the Roles page shows no read, so there is no "Take on" to press, and a runtime that has made no read answers `roles.takeOn` with `CONFLICT` "Open the address again before taking on a role." (Changed during Task 6: the kept read is cleared when the vault closes underneath the page, so pressing "Take on" cannot reach that sentence on this path.)
- "Remove" deletes the role; after a reload it is still gone.
- All browser tests and `python.test_vaulting` pass; `npm run typecheck` passes.

- [ ] **Step 1:** Browser tests per criterion. See them fail.
- [ ] **Step 2:** Sub-database registration and the store; the three methods. Rebuild; runtime-level assertions pass.
- [ ] **Step 3:** Dialog, My roles, remove. Rebuild; all tests pass.
- [ ] **Step 4:** Whole suite, typecheck, `python.test_vaulting`. Commit and push.

---

### Task 7: Ship to the phone

**Files (Fortoid):**
- Modify: `config/fortweb-runtime.json` (the new FortWeb `dev` commit)
- Modify: `CLAUDE.md` (one paragraph: the Roles page, `sourcing.py` and `roling.py`, the fixture source, `pip install blake3` for local Python tests), `README.md` (one paragraph on the Roles page)

**Acceptance Criteria:**
- Canonical package built from the pinned commit and imported; `./gradlew :app:assembleDebug :app:testDebugUnitTest` and `scripts/test-sync-contract.sh` pass locally.
- Every workflow on Fortoid `dev` is green for the pin commit, the Startup Proof included.
- The owner is told how to try it: install the APK, open the wallet, type the address. With Task 1 still held, the live source answers "mad.seriouscoder.one does not allow wallets to read it yet", which is the expected result until the header is added.

- [ ] **Step 1:** Pin, build and import the package, assemble, run the JVM and contract tests.
- [ ] **Step 2:** Docs. Commit, push to `dev` and the session branch.
- [ ] **Step 3:** Wait for CI on the pin commit and check every workflow.

---

## Spec coverage

| Spec item | Task |
|---|---|
| D1 header in `mad-discovery`, deploy note | 1 (held for the owner's yes) |
| Address, document and SAID rules; limits | 2, 3, 4 |
| Fetch rules; "does not allow wallets" vs "could not reach" | 4 |
| Role derivation in one function | 3 |
| Roles page as landing; publications and roles; plain text only | 5 |
| D3 identifier chooser | 6 |
| Roles store in the vault; offline; nothing sent when taking on | 6 |
| D2 warning "Not appointed yet" | 6 |
| D5 introductions and catalogs counted only | 3, 5 |
| Error sentences | 4, 5, 6 |
| Tests: Python units, browser tests, existing checks | 2 to 7 |
| Phone check | 7 (complete once Task 1 is released) |
