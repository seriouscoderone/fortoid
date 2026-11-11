# Fortoid — agent notes

Fork of `keri-foundation/fortoid` (Android WebView shell hosting FortWeb).
Upstream remote: `https://github.com/keri-foundation/fortoid.git`.

## Working agreement (from the repo owner)

- Use **beads-superpowers** skills and **bd (beads)** for task tracking.
  `.beads/issues.jsonl` is the committed source of truth; the Dolt DB is local.
  In a fresh container: `npm install -g @beads/bd`,
  `git config beads.role maintainer`, then
  `bd init --non-interactive -p fortoid --skip-agents --init-if-missing`.
- Make **very small commits** (one logical change each) and **push straight to
  `main`** on this fork as you go. No pull requests, no feature branches.
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
`main` = Foundation main + this project's features. Upstream PRs (e.g. #49)
branch from `upstream/main`.

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

Ship a FortWeb change to the APK: push fortweb `main`, set its full SHA in
Fortoid `config/fortweb-runtime.json`, verify locally with
`scripts/build-canonical-fortweb-package.sh --fortweb-dir /home/user/fortweb`
(needs `build/runtime-source` absent) + `node tools/import-fortweb-runtime-package.mjs`
+ `./gradlew :app:assembleDebug`, then push; CI publishes `demo-latest`.

