# Fortoid — agent notes

Fork of `keri-foundation/fortoid` (Android WebView shell hosting FortWeb).
Upstream remote: `https://github.com/keri-foundation/fortoid.git`.

## Working agreement (from the repo owner)

- Use **beads-superpowers** skills and **bd (beads)** for task tracking.
  `.beads/issues.jsonl` is the committed source of truth; the Dolt DB is local.
  In a fresh container: `npm install -g @beads/bd`,
  `git config beads.role maintainer`, then
  `bd init --non-interactive -p fortoid --skip-agents --init-if-missing`.
- Make **very small commits** (one logical change each) and **push to the `dev`
  branch** as you go. No pull requests.
- **`main` is an exact copy of the Foundation's `main`** on every fork (fortoid,
  fortweb, fortios). Never commit to it: it only ever fast-forwards from
  `upstream/main`, so upstream changes arrive cleanly. To receive them:
  `git fetch upstream && git push origin upstream/main:main`, then
  `git merge upstream/main` on `dev`. (Owner, 2026-11-12; replaces the earlier
  "push straight to `main`".)
- A cloud session is given its own branch (`ccr-...`). Keep it identical to
  `dev` and push both.
- **`mad-discovery` has its own agent.** Do not change that repository from a
  Fortoid session. When the wallet needs something there, file a GitHub issue
  on `seriouscoderone/mad-discovery` that says what is asked and why (that
  agent's own tracker is the owner's private workspace, `bd` from `~/code/bpm`,
  which a Fortoid session cannot reach). Mirror it here as a bead with the
  label `for-mad-discovery-agent` and the issue as its external reference
  (`gh-<number>`), and tell the owner both. `git pull --ff-only` before reading
  that repository. (Owner, 2026-11-12 and 2026-11-13.)
- Current focus is a proof-of-concept demo for the KERI Foundation; signed
  release builds are out of scope for now.
- **KERI-only identity.** Everything the owner builds uses KERI for identity,
  authentication and signing. Do not propose passkeys/WebAuthn, OAuth or
  password logins as alternatives. Phone hardware (secure chip, biometrics) is
  welcome as a way to protect and unlock KERI material, never to replace it.
- **Application-first product direction.** The owner builds applications that
  solve people's problems; the wallet, vaults and identifiers stay as far in the
  background as possible (expect FortWeb to be re-skinned). Don't add identity
  setup steps to onboarding: applications will create or suggest AIDs when they
  need them.

## Build

```bash
./gradlew :app:testDebugUnitTest          # JVM tests (no payload needed)
./gradlew :app:assembleDebug              # needs a staged FortWeb payload
```

Cloud sessions need the Android SDK (`local.properties` → `sdk.dir`); there is
no emulator (no KVM), so on-device checks happen on the owner's phone.

## Working on the wallet (FortWeb fork)

The wallet UI lives in `seriouscoderone/fortweb` (attach it with `add_repo`,
clone to `/home/user/fortweb`; `upstream` = keri-foundation/fortweb). Its
`dev` = Foundation main + this project's features; its `main` mirrors the
Foundation's. Upstream PRs (e.g. #49) branch from `upstream/main`.

Build the runtime locally (Python 3.12 venv; system Python is 3.13):

```bash
python3.12 -m venv ~/fwvenv && export PATH=~/fwvenv/bin:$PATH
cd /home/user/fortweb && npm ci
rm -rf build/runtime-source
python3 scripts/acquire_runtime_source.py \
  --url https://github.com/keri-foundation/fortweb/releases/download/runtime-source-pyodide-314-hio-0.7.20-20260910/runtime-source.tar.gz \
  --sha256 394db70ecbed6e5718e6ec0afbcb43eaf6384e2ab78218e55067ec9e5d83fb34 \
  --manifest-sha256 371ef8cb8c6641b68afe6799931678435e2a629dcf4aa7ba2e344efab5a10f32 \
  --output build/runtime-source
python3 -m pip install --no-index --no-deps build/runtime-source/wheelhouse/{packaging,setuptools,wheel}-*.whl
export FORTWEB_RUNTIME_SOURCE_MANIFEST=build/runtime-source/manifest.json
export FORTWEB_RUNTIME_SOURCE_MANIFEST_SHA256=371ef8cb8c6641b68afe6799931678435e2a629dcf4aa7ba2e344efab5a10f32
npm run build:runtime            # rebuild after every app/ change
```

Tests: Playwright must use the container's Chromium, so create an uncommitted
`playwright.local.config.ts` (spread `./playwright.config`, set
`use.launchOptions.executablePath = '/opt/pw-browsers/chromium'`, add it to
`.git/info/exclude`), then
`npm run test:e2e:prepared-runtime -- -c playwright.local.config.ts --trace off`.
Also: `npm run typecheck`, `python3 -m unittest python.test_vaulting`,
`npm run test:runtime-publication-workflow`. Phone features are tested with a
fake `window.bridge` (see `playwright/fortweb-*native*`/`fingerprint` specs).

The wallet's landing page is **Roles** (fortweb `app/features/roles/roles-page.ts`):
it reads a `mad-discovery` source, lists the roles it publishes and lets the
person take one on. The rules are in fortweb `app/runtime/sourcing.py` (address,
document and SAID rules and role derivation; pure Python, opens nothing) and
`app/runtime/roling.py` (`sources.read` over the browser's fetch, outside the
worker's request lock; `roles.takeOn`, `roles.list` and `roles.remove` over the
vault's `frol.` and `fdef.` sub-databases, with no network). Their Python tests
need Blake3 in the venv: `pip install blake3` (1.0.8 or later; the runtime
bundles 1.0.8), then
`python3 -m unittest python.test_sourcing python.test_roling`. Browser tests
serve a fixture source at `https://mad.example.test/`
(`playwright/fixtures/mad-source.ts`, definitions in `python/fixtures/mad/`) and
live in the `playwright/fortweb-roles*.spec.ts` files, with their helpers in
`playwright/fixtures/roles.ts`. `package.json` names every spec in
`test:e2e:prepared-runtime`: a new spec file runs only once it is added there.
A spec whose tests take over five minutes prints a "Slow test file" notice (CI
runs them on one worker), so keep each under about 210 s and split by topic
before that.

A real source is readable only if it sends `Access-Control-Allow-Origin: *`;
otherwise the wallet says "Could not read <host>: it cannot be reached, or it
does not allow wallets to read it yet." A browser does not say which, and the
wallet makes no second request to find out: such a request would follow
redirects to other hosts. The wallet reads with the fetch cache mode
`no-store`, so the browser adds `Cache-Control: no-cache` and
`Pragma: no-cache` to every request, and a source must send the header on such
a request too: CloudFront's managed SimpleCORS policy does not. Check a source
with `curl -sI -H 'Origin: https://wallet.invalid' -H 'Cache-Control: no-cache'
-H 'Pragma: no-cache' <address>`, not with `Origin` alone. `route.fulfill`
in the browser tests cannot show what a browser does with redirects or
cross-origin rules, so a claim about either needs a real server. The limits
on what a read and a role may cost, and the sentences the person sees, are in
`.internal/specs/2026-11-12-first-browser-demonstration-design.md`. The
characters refused in a shown string (`FORBIDDEN` in `sourcing.py`) are
`mad-discovery`'s Ruling 13 and must stay equal to its `src/source.ts`.

On Pyodide a Python function handed to a browser API (`js.setTimeout`, an
event listener) is a proxy that is destroyed when the call returns: keep it
alive with `pyodide.ffi.create_proxy` and release it afterwards, or the browser
calls nothing. The unit tests run on plain CPython with stand-ins for `js`, so
only a browser test that waits for the callback shows whether it works; the
roles time-limit tests (`playwright/fortweb-roles-time.spec.ts`) do.

Ship a FortWeb change to the APK: push fortweb `dev`, set its full SHA in
Fortoid `config/fortweb-runtime.json`, verify locally with
`scripts/build-canonical-fortweb-package.sh --fortweb-dir /home/user/fortweb`
(needs `build/runtime-source` absent) + `node tools/import-fortweb-runtime-package.mjs`
+ `./gradlew :app:assembleDebug`, then push; CI publishes `demo-latest`.

After every pin, check **all** workflows on `dev`, not only Demo APK. The
Android API 36 Startup Proof boots the real wallet on a 2-core emulator that
draws in software, with a 30 s boot budget (12-23 s when nothing is drawn).
Anything that keeps drawing during start-up eats that budget: a CSS animation
on the start page failed the proof seven runs in a row. Keep start-up UI still (the
loading bar moves once a second on a timer, see fortweb
`app/ui/primitives/loading-bar.ts`). Pushing to `dev` cancels a proof run that
is still in progress, so wait for it before the next push when you need its
result.

