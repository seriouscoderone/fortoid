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
- **The MAD designer (`seriouscoderone/mad`, private) is not changed from a
  Fortoid session either.** The owner allows issues there (2026-11-14): attach
  it with `add_repo` (push access, which filing an issue needs), say what the
  wallet needs and why, and mirror each issue here as a bead labelled
  `for-mad-agent` with the external reference `gh-mad-<number>`. That
  repository is private and this one is public: nothing from its documents or
  code goes into the beads, specs, plans or notes here. State the wallet's need
  in the wallet's own words.
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
- **Universal UI.** (Owner, 2026-11-14.) A MAD app that has no workbench of its
  own is carried out with the wallet's standard features: identifiers,
  contacts, and credentials that are issued, received, accepted or declined,
  and shown to someone. The published definitions supply the types and the
  forms. Build and improve those standard screens; do not build screens for one
  app or one kind of credential. Custom UIs (workbenches) come later, as wallet
  plugins.

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

Its `credentials` = `dev` + the Foundation's draft credential engine
(`keri-foundation/fortweb#42` at `2db99e8`, merged in `9ec62aa`) + the work
that needs it.
**Fortoid pins `credentials`**, and new wallet work goes there until the
Foundation merges the draft; then `dev` takes `upstream/main` and `credentials`
is merged into it.

Build the runtime locally (Python 3.12 venv; system Python is 3.13):

```bash
python3.12 -m venv ~/fwvenv && export PATH=~/fwvenv/bin:$PATH
cd /home/user/fortweb && npm ci
rm -rf build/runtime-source
python3 scripts/acquire_runtime_source.py \
  --url https://github.com/seriouscoderone/fortoid/releases/download/runtime-source-sedi-core-csp-20261107/runtime-source.tar.gz \
  --sha256 3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a \
  --manifest-sha256 f76cf23f5cdaa5fe3f7a0e264c862290f9c8b6be2aafca6622926e1869731431 \
  --output build/runtime-source
python3 -m pip install --no-index --no-deps build/runtime-source/wheelhouse/{packaging,setuptools,wheel}-*.whl
export FORTWEB_RUNTIME_SOURCE_MANIFEST=build/runtime-source/manifest.json
export FORTWEB_RUNTIME_SOURCE_MANIFEST_SHA256=f76cf23f5cdaa5fe3f7a0e264c862290f9c8b6be2aafca6622926e1869731431
npm run build:runtime            # rebuild after every app/ change
```

The address is this repository's own checked copy of the Foundation's archive
`runtime-source-sedi-core-csp-20261107` (`config/runtime-source-mirror.json`).

A wallet that needs another runtime source: add the source to
`config/runtime-source-mirror.json` and push that first; wait for the workflow
Runtime source mirror to make the copy (it runs on a push that changes the list,
the tool that reads it, its script or the workflow file, and cannot be started
by hand, because the file is not on the default branch, so to make a copy again
re-run its last run); only then move the reviewed pin in
`scripts/build-canonical-fortweb-package.sh` and the wallet pin. In one push the
demo would be published without the wallet. The wallet repository's own workflow
reads the Foundation's address (fortweb
`.github/actions/produce-runtime-package/action.yml`); if that address ever
fails, the workflow's three inputs take the copy's address and the two digests.

Tests: Playwright must use the container's Chromium, so create an uncommitted
`playwright.local.config.ts` (spread `./playwright.config`, set
`use.launchOptions.executablePath = '/opt/pw-browsers/chromium'`, add it to
`.git/info/exclude`), then
`npm run test:e2e:prepared-runtime -- -c playwright.local.config.ts --trace off`.
Also: `npm run typecheck`, `python3 -m unittest python.test_vaulting`,
`npm run test:runtime-publication-workflow`. After a build, also
`python3 -m unittest python/test_verify_runtime_tree.py python/test_start_wallet.py`.
The workflow's canary
(`playwright/runtime-canary.spec.ts`) passes only with the headless shell, the
browser the workflow installs (in this container under
`/opt/pw-browsers/chromium_headless_shell-*`): the full Chromium also asks for
`/favicon.ico`, the canary's server answers 404, and the test counts that as an
error. Phone features are tested with a fake `window.bridge` (see
`playwright/fortweb-*native*`/`fingerprint` specs).

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

On a computer the wallet is served by fortweb `scripts/start_wallet.py` at
`http://127.0.0.1:8791/` (never 8765, the development server's port: a browser
keeps one wallet per address). Fortoid's Demo APK workflow publishes it with
the built wallet as `fort-wallet-desktop.zip`. Its browser test
(`playwright/fortweb-desktop-start.spec.ts`) runs against an unzipped download
when `FORT_WALLET_FOLDER` names the folder. Its unit tests
(`python/test_start_wallet.py`) also run on Windows, macOS
and Linux with Python 3.11, 3.13 and 3.14, and with 3.9 on Windows and macOS
(fortweb `.github/workflows/start-script-platforms.yml`), on a push to
`credentials` that changes the script or its tests: Windows path rules differ
(a path part such as `C:..`), and only a real Windows run shows it.

Credentials: the wallet runs on the Foundation's draft engine, merged into
`credentials`. What that changes: the wallet's storage is named `@fortweb-v2/`
and vaults made by earlier builds are not shown; one engine at a time may hold
the storage (a second gets `BUSY`), so a test that needs a second engine has
the page lock its vault first; the engine writes to storage after every
request; after a request that failed with unsaved changes the vault refuses
everything until it is closed. Receiving a credential and every `ipex` method
(`credentials.receive`, `credentials.getReceived`, `ipex.grant`, `ipex.receive`,
`ipex.admit`, `ipex.present`, `ipex.verifyPresentation`, `ipex.get`) run only
because `pyscript-ci.toml` carries `fort_synthetic_recipient_testing = true`,
and each call must still pass `syntheticOnly: true`. The engine has no screens
for credentials, no spurn, and nothing that carries a message from one wallet to
another. The draft's own credential tests (`playwright/registry-*.spec.ts`,
`playwright/sedi-*.spec.ts`) are run by no workflow; one of them, "recipient API
is disabled in the ordinary runtime", fails here by design. Before changing the
draft's files, read this repository's
`.internal/specs/2026-11-14-standard-credential-screens-design.md`.

When the draft moves: read its head in fortweb with
`git fetch upstream refs/pull/42/head:refs/remotes/upstream/pr42` and compare
it with `2db99e8`. If the Foundation added commits to it, merge
`upstream/pr42` into `credentials` again. If it rewrote its branch, start a
new branch from `dev`, merge the new head the way Task 1 of this
repository's `.internal/plans/2026-11-14-credentials-base.md` did,
cherry-pick this branch's own commits onto it, and give it the name
`credentials`: neither of the wallet's workflows runs on a branch of another
name.

Ship a FortWeb change to the APK: push fortweb `credentials` and wait until its
workflows are green on that commit, set its full SHA in
Fortoid `config/fortweb-runtime.json`, verify locally with
`scripts/build-canonical-fortweb-package.sh --fortweb-dir /home/user/fortweb`
(needs `build/runtime-source` absent) + `node tools/import-fortweb-runtime-package.mjs`
+ `./gradlew :app:assembleDebug`, then push; CI publishes `demo-latest`.
The Demo APK workflow also builds `fort-wallet-desktop.zip` with
`tools/build_desktop_wallet.py`, from the same payload, the pinned wallet's
`scripts/start_wallet.py` and `desktop/README.txt`. That note repeats the
script's address, its command and its oldest Python: change them together.

After every pin, check **all** workflows on `dev`, not only Demo APK. The
Android API 36 Startup Proof boots the real wallet on a 2-core emulator that
draws in software. The wallet allows its first request, which includes
starting the worker, 30 s (`DEFAULT_REQUEST_TIMEOUT_MS` in fortweb
`app/runtime/bridge.ts`); on that emulator it took 14 to 25 s in the runs of
2026-11-15, with either engine, when nothing is drawn. The proof's own
`elapsedMs` is longer and has a limit of four minutes.
Anything that keeps drawing during start-up eats that budget: a CSS animation
on the start page failed the proof seven runs in a row. Keep start-up UI still (the
loading bar moves once a second on a timer, see fortweb
`app/ui/primitives/loading-bar.ts`). Pushing to `dev` cancels a proof run that
is still in progress, so wait for it before the next push when you need its
result.

Going back to the wallet from before the credential engine (fortweb `dev`,
`1555227`): `git revert --no-edit 873da63 08cb0cf 9d1a758`, in that order (the
first lets the second apply without a conflict), push both branches and wait
for every workflow. That wallet has no start script, so the release then
carries the APK alone and says there is no wallet for the computer. Its
runtime source is read from the Foundation's address again: this repository
keeps no copy of it. The earlier vault shows again, and a wallet made with the
credential engine does not, until the pin returns: each engine keeps its
storage in its own place.

