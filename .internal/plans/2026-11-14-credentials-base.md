# The Base for Credentials Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use beads-superpowers:subagent-driven-development (recommended) or beads-superpowers:executing-plans to implement this plan task-by-task. Each Task becomes a bead (`bd create -t task --parent <epic-id>`). Steps within tasks use checkbox (`- [ ]`) syntax for human readability.

**Goal:** The wallet runs on the Foundation's draft credential engine on the phone and in a desktop browser, with nothing new on screen for credentials yet.

**Architecture:** Two repositories. In `seriouscoderone/fortweb` a branch `credentials` starts from `dev` and merges the Foundation's draft `keri-foundation/fortweb#42`; three small additions sit on top of the merge: the draft's test switch turned on, asking the browser to keep the wallet's storage, and a start script for a computer. In `seriouscoderone/fortoid` a workflow keeps a checked copy of the draft's runtime source on the Releases page, the wallet pin and the reviewed runtime-source pin move to it, and the Demo APK workflow also publishes the wallet for a computer. This is phase 1 of the design; phases 2 (hand-over and contacts) and 3 (schemas and credentials) get their own plans.

**Tech Stack:** TypeScript compiled with `tsc` (no bundler); Python 3.12 for tools and tests; KERIpy on Pyodide in a web worker; Playwright with the container's Chromium; Kotlin and Gradle (Fortoid); GitHub Actions.

## Global Constraints

- The design is `.internal/specs/2026-11-14-standard-credential-screens-design.md`. This plan covers its phase 1, "The base", and nothing of phases 2 and 3.
- Wallet work is committed on the fortweb branch `credentials`. The fortweb branches `dev` and `main` are not touched. Fortoid work is committed on the session branch and pushed to `dev` and the session branch together, so the two stay identical. The one exception is the pin in Task 6: there the session branch goes first for the length of one proof run, because a push to `dev` publishes the demo.
- Very small commits, one logical change each. Every commit subject ends with the task's bead id in parentheses, as in `(fortoid-abc.2)`; the commands below write it as `(<bead>)`. Every commit message carries the attribution lines the session gives. No pull requests.
- The draft is merged as a merge commit, not squashed, so its commits keep their author.
- The draft's engine (`app/runtime/vaulting.py`, `app/runtime/wallet-worker.py` and the files the draft adds) changes only where a conflict requires it. Beyond the conflicts this plan touches the draft's work in two places: one key in `pyscript-ci.toml` and one rule in `scripts/verify_runtime_tree.py`, with two tests for that rule.
- No test is weakened, skipped or deleted. One of the fork's tests is rewritten in Task 1, because the draft lets one engine at a time hold the wallet's storage; what the test proves stays the same.
- The draft is read at commit `2db99e8e0411b0215b0dc6a9b5c0a24548bb740d`. If `upstream/pr42` is at another commit when Task 1 starts, stop and report: this plan was checked against `2db99e8`.
- The runtime source for `credentials` is the Foundation's archive `runtime-source-sedi-core-csp-20261107`: archive SHA-256 `3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a`, manifest SHA-256 `f76cf23f5cdaa5fe3f7a0e264c862290f9c8b6be2aafca6622926e1869731431`.
- The switch is the top-level key `fort_synthetic_recipient_testing = true` in the wallet's `pyscript-ci.toml`.
- Playwright runs with `-c playwright.local.config.ts --trace off` (the local config only names the container's Chromium). Every new spec file is named in `package.json` under `test:e2e:prepared-runtime`. Each spec file stays under about 210 seconds.
- The wallet's address on a computer is `http://127.0.0.1:8791/`. Not 8765: the wallet's development server uses that port, and a browser keeps one wallet per address.
- Words on the Settings page are exact: the label `Kept by the Browser`, and the values `Yes`, `No. The browser may remove this wallet's storage when it runs short of space.` and `This browser does not say.`
- After every push to Fortoid `dev`, check every workflow, not only Demo APK. Push once and wait: a new push to `dev` cancels a start-up proof that is still running.
- Never push to `keri-foundation/*`. Never commit to `main`. Never run an `aws` command. Nothing from the owner's private MAD repositories goes into either repository.
- Only the controller runs `bd`.
- Starting a workflow by hand and reading workflow runs need the GitHub tools. An implementer that has none pushes, reports the pushed commit and which runs remain to be checked, and stops there; the controller starts and checks them before the task is closed.
- The files and patches named in the steps are committed beside this plan, in `.internal/plans/2026-11-14-credentials-base/`. Each was run on a scratch copy on 2026-11-14. Where a step names one, copy or apply that file; the text shown in the step is the same text.

## What was tried before this plan was written

On 2026-11-14, on scratch copies. Nothing was pushed.

- **The merge.** `dev` (`1555227`) and the draft (`2db99e8`) conflict in six files, ten places. Both sides are kept in every place. The resolution is recorded as commit `25756acc863aab8d855be15d6bdbc2d5ac550f9e` (tree `d23d8d751935706f02d9f925449ce1c57be28048`), carried in `fortweb/recorded-merge.bundle`. The steps of Task 1 were replayed on a fresh checkout and gave that tree.
- **The wallet with every change of Tasks 1 to 4.** 245 Python tests, the type check, the build, 146 browser tests with no slow-file notice, and the 15 publication-workflow tests passed. So did the Python and Node test lists the fork's workflow runs, its build-twice check, its canary, its wheelhouse test, and the draft's lifecycle suite served as the workflow serves it. The canary passes only with the browser the workflow installs, the headless shell: the container's full Chromium also asks for `/favicon.ico`, the canary's server answers 404, and the test counts that as an error.
- **A real browser's answer.** Chromium 153, asked at the fresh address `http://127.0.0.1:8791/` to keep storage, answers no and shows no prompt. Chrome and Edge decide by themselves and favour sites that have been used. So the wallet asks at every opening of a vault, not only when one is made (Task 3).
- **A phone that had the earlier wallet.** A browser test with the earlier vault still remembered for a fast launch, and its fingerprint passcode still saved, shows the old unlock screen and then *Set up your wallet*, and a wallet can be made from there. One sentence, "Vault 'my-wallet' was not found.", reaches a screen reader; nothing of it is on the screen. The fork's test "a remembered vault that no longer exists falls back to normal launch" keeps covering the path.
- **Fortoid.** Pinned to that wallet and to the draft's runtime source, Fortoid built the package, imported it (174 files), built the APK and passed its JVM tests, its lint and its payload checks. The wallet for a computer was built from the imported payload, unzipped, started from its own folder and opened by the wallet's browser test. All of this was repeated on 2026-11-15 after the stress-test's changes.
- **The two workflow scripts.** The copying script was run against a stand-in for `gh` in five situations: no copy yet (with a real download of the Foundation's archive, which matched its digest), a correct copy, a wrong copy, a release without the file, and wrong bytes from upstream. The publishing script was run for a built payload and for a failed one.
- **Not tried, because this container has no emulator:** Fortoid's eight emulator workflows. Task 6 is where the merged wallet first runs on Android.
- **Known and left as it is.** `playwright/registry-receipt.spec.ts`, one of the draft's own files that no workflow runs, has a test named "recipient API is disabled in the ordinary runtime". It fails on `credentials` once the switch is on, because the switch is what turns that API on (decision D3 of the design). The file's other seven tests pass. Two tests of `playwright/registry-backend.spec.ts` fail on the draft alone, because they look for a line the draft has since changed (noted for the Foundation in `fortoid-dvq`). Three tests of `tools/runtime-safe-output.test.mjs`, also run by no workflow, need a Python 3.14 environment at `.venv-pyodide-314` and fail without it, before and after this plan.

## File Structure

`seriouscoderone/fortweb`, branch `credentials`:

| File | Responsibility |
|---|---|
| six conflicted files, four test files | The merge (Task 1). |
| `.github/workflows/fortweb-runtime-playwright.yml` | Also runs on pushes to `credentials` (Task 1). |
| `pyscript-ci.toml`, `scripts/verify_runtime_tree.py` | The draft's test switch, on, and the verifier's rule for it (Task 2). |
| `app/runtime/storage-keeping.ts` (new) | Asking the browser to keep storage, and reading its answer (Task 3). |
| `app/app/main.ts`, `app/app/page-loader.ts`, `app/app/page-factory.ts`, `app/features/settings/settings-page.ts` | Ask when a vault is opened; show the answer in Settings in a regular browser (Task 3). |
| `scripts/start_wallet.py` (new) | Serves a wallet package to a browser on the same computer (Task 4). |
| `playwright/fortweb-credentials-switch.spec.ts`, `playwright/fortweb-storage-keeping.spec.ts`, `playwright/fortweb-desktop-start.spec.ts`, `python/test_start_wallet.py` (new) | Their tests. |
| `.github/actions/produce-runtime-package/action.yml`, `package.json` | The lists of tests the workflow runs (Tasks 2, 3 and 4). |

`seriouscoderone/fortoid`, branch `dev`:

| File | Responsibility |
|---|---|
| `config/runtime-source-mirror.json`, `tools/list-runtime-source-mirrors.mjs` (new) | Which runtime sources the repository keeps a copy of (Task 5). |
| `.github/workflows/runtime-source-mirror.yml` (new) | Makes and checks the copies (Task 5). |
| `.github/workflows/android-startup-proof.yml` | Also prints the proof's start-up time (Task 5). |
| `config/fortweb-runtime.json`, `scripts/build-canonical-fortweb-package.sh`, `scripts/test-sync-contract.sh` | The wallet pin and the reviewed runtime-source pin (Task 6). |
| `tools/build_desktop_wallet.py`, `desktop/README.txt` (new) | The zip a person downloads for a computer (Task 7). |
| `.github/workflows/demo-apk.yml` | Also publishes that zip (Task 7). |
| `.github/workflows/android-runtime-contract.yml` | Runs the two new tool tests (Tasks 5 and 7). |
| `README.md`, `CLAUDE.md` | What changed, for a reader and for the next session (Tasks 6 and 7). |

## Order

Tasks 1 to 4 are in the wallet's repository and follow one another. Task 5 is in Fortoid and needs none of them. Task 6 needs Tasks 4 and 5. Task 7 needs Task 6. Task 8 needs Task 7.

---

### Task 1: The `credentials` branch, with the Foundation's draft merged

**Repository:** `/home/user/fortweb`

**Files:**
- Modify (conflicts): `.github/workflows/fortweb-runtime-playwright.yml`, `app/app/main.ts`, `app/features/remotes/remotes-page.ts`, `app/runtime/method-catalog.ts`, `app/runtime/vaulting.py`, `app/runtime/wallet-worker.py`
- Modify (tests the merge breaks): `python/test_vaulting.py`, `python/test_roling.py`, `playwright/fortweb-roles-take-on.spec.ts`, `playwright/webbaser-lifecycle.spec.ts`
- Modify: `.github/workflows/fortweb-runtime-playwright.yml` (the branches it runs on)

**Interfaces:**
- Consumes: `dev` at `15552276911a00d5c3434504580284f40ffe7809`; the draft at `2db99e8e0411b0215b0dc6a9b5c0a24548bb740d`; the recorded resolution in `/home/user/fortoid/.internal/plans/2026-11-14-credentials-base/fortweb/recorded-merge.bundle`.
- Produces: the branch `credentials` on `origin`, and the build environment every later wallet task uses:

```bash
[ -d ~/fwvenv ] || { python3.12 -m venv ~/fwvenv && ~/fwvenv/bin/python3 -m pip install blake3; }
export PATH=~/fwvenv/bin:$PATH
cd /home/user/fortweb
rm -rf build/runtime-source
python3 scripts/acquire_runtime_source.py \
  --url https://github.com/keri-foundation/fortweb/releases/download/runtime-source-sedi-core-csp-20261107/runtime-source.tar.gz \
  --sha256 3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a \
  --manifest-sha256 f76cf23f5cdaa5fe3f7a0e264c862290f9c8b6be2aafca6622926e1869731431 \
  --output build/runtime-source
python3 -m pip install --no-index --no-deps build/runtime-source/wheelhouse/{packaging,setuptools,wheel}-*.whl
export FORTWEB_RUNTIME_SOURCE_MANIFEST=build/runtime-source/manifest.json
export FORTWEB_RUNTIME_SOURCE_MANIFEST_SHA256=f76cf23f5cdaa5fe3f7a0e264c862290f9c8b6be2aafca6622926e1869731431
npm ci
npm run build:runtime
```

  Every shell starts fresh, so the two `export` lines and the `PATH` line are repeated in each new shell. The local Playwright config is uncommitted; if `playwright.local.config.ts` is missing, create it and name it in `.git/info/exclude`:

```ts
// Local-only: use the container's preinstalled Chromium. Not committed.
import base from './playwright.config';
export default {
    ...base,
    use: { ...base.use, launchOptions: { executablePath: '/opt/pw-browsers/chromium' } },
};
```

**Acceptance Criteria:**
- `git log --merges --first-parent -1 --format=%P credentials` prints `15552276911a00d5c3434504580284f40ffe7809 2db99e8e0411b0215b0dc6a9b5c0a24548bb740d`.
- The merge commit's tree is `d23d8d751935706f02d9f925449ce1c57be28048` (`git rev-parse <merge>^{tree}`), or every difference from the recorded merge is explained in the report.
- `python3 -m unittest python.test_sourcing python.test_roling python.test_vaulting python.test_transporting python.test_onboarding` runs 225 tests, all passing.
- `npm run typecheck` reports no error.
- `npm run test:e2e:prepared-runtime -- -c playwright.local.config.ts --trace off` passes all 139 tests, with no "Slow test file" notice.
- `npm run test:runtime-publication-workflow` passes (15 tests).
- The workflow "FortWeb Runtime and Playwright Checks" is green for the pushed head of `credentials`.

- [ ] **Step 1: Check the starting points**

```bash
cd /home/user/fortweb
git status --short            # expected: nothing
git switch dev && git pull --ff-only
git rev-parse HEAD            # expected: 15552276911a00d5c3434504580284f40ffe7809
git fetch upstream refs/pull/42/head:refs/remotes/upstream/pr42
git rev-parse upstream/pr42   # expected: 2db99e8e0411b0215b0dc6a9b5c0a24548bb740d
```

If either commit differs, stop and report.

- [ ] **Step 2: Fetch the recorded resolution**

```bash
git fetch /home/user/fortoid/.internal/plans/2026-11-14-credentials-base/fortweb/recorded-merge.bundle \
  refs/heads/spike/pr42-merge:refs/heads/recorded-merge
git rev-parse recorded-merge  # expected: 25756acc863aab8d855be15d6bdbc2d5ac550f9e
```

- [ ] **Step 3: Merge, and take the recorded resolution**

```bash
git switch -c credentials dev
git merge --no-ff --no-commit upstream/pr42   # expected: "Automatic merge failed"
git diff --name-only --diff-filter=U
```

Expected conflicts, exactly these six:

```
.github/workflows/fortweb-runtime-playwright.yml
app/app/main.ts
app/features/remotes/remotes-page.ts
app/runtime/method-catalog.ts
app/runtime/vaulting.py
app/runtime/wallet-worker.py
```

Take the recorded versions of the six, and of the four test files the merge breaks:

```bash
git checkout recorded-merge -- \
  .github/workflows/fortweb-runtime-playwright.yml app/app/main.ts app/features/remotes/remotes-page.ts \
  app/runtime/method-catalog.ts app/runtime/vaulting.py app/runtime/wallet-worker.py \
  python/test_vaulting.py python/test_roling.py playwright/fortweb-roles-take-on.spec.ts \
  playwright/webbaser-lifecycle.spec.ts
git diff --cached --quiet recorded-merge && echo "tree equals the recorded merge"
grep -rn '^<<<<<<<\|^=======$\|^>>>>>>>' app python playwright .github || echo "no conflict markers"
```

Expected: `tree equals the recorded merge` and `no conflict markers`.

- [ ] **Step 4: Read every resolution against this table before committing**

Taking the files is not the review. Open each place and check that it says what the table says. `git diff 1555227 -- <file>` shows what the draft's side added to a file, and `git diff upstream/pr42 -- <file>` what `dev`'s side added.

| # | Place | What the result must hold |
|---|---|---|
| 1 | the workflow, `runs-on` | `dev`'s two comment lines about Playwright and Ubuntu 26.04, and `runs-on: ubuntu-24.04`. |
| 2 | `method-catalog.ts`, end of `METHODS` | The list ends with `kfAccountWatchersStatus`, then `dev`'s four names `sourcesRead`, `rolesList`, `rolesTakeOn`, `rolesRemove`, then `} as const);`. Every name the draft adds is in the list: `identifiersExport`, `registriesCreate`, `registriesGet`, `registriesPresent`, `schemasImport`, `schemasGet`, `credentialsIssueSigned`, `credentialsIssueRegistered`, `credentialsGetIssued`, `credentialsRevokeRegistered`, `credentialsReceive`, `credentialsGetReceived`, `ipexGrant`, `ipexReceive`, `ipexAdmit`, `ipexPresent`, `ipexVerifyPresentation`, `ipexGet`, `remotesImport`. |
| 3 | `remotes-page.ts`, the form in `createResolveRemoteDialog` | In this order: the draft's OOBI field, without `required: true`; `dev`'s *Scan QR code* button line; the Alias field; the draft's label, file input and note about CESR files; the status line. |
| 4 | `main.ts`, in `actions` | `dev`'s `readSource`, `lastRead`, `showRoles`, `takeOnRole` and `removeRole`, unchanged, then the draft's `exportIdentifier`, then `resolveRemoteOobi`. |
| 5 | `vaulting.py`, the parameters of `configure_runtime` | `kf_state_subdb: str,` `roles_subdb: str,` `role_definitions_subdb: str,` `allow_synthetic_receipt: bool = False,` in that order. |
| 6 | `vaulting.py`, what `configure_runtime` stores | `kf_state_subdb=`, `roles_subdb=`, `role_definitions_subdb=` and `allow_synthetic_receipt=`, each from its parameter. |
| 7 | `vaulting.py`, `_build_vault_state` | One loop: `for subdb in (_CONFIG["kf_state_subdb"], _CONFIG["roles_subdb"], _CONFIG["role_definitions_subdb"], "fwic.", "fwpn.", "fwrc."):` adding each name to `baser.SubDbNames` when it is absent. |
| 8 | `wallet-worker.py`, module globals | `dev`'s comment and `_UNLOCKED_METHODS = frozenset({"sources.read"})`, then `_RUNTIME_READY_TASK`, then the draft's `_WRITER_LOCK`, `_WRITER_REQUEST`, `_WRITER_CALLBACK`, `_WRITER_RELEASING`, `_WRITER_LOST`, `_ACTIVE_REQUEST`, `_acquire_writer` and `_release_writer`, unchanged. |
| 9 | `wallet-worker.py`, the call `vaulting.configure_runtime(...)` | `roles_subdb=ROLES_SUBDB,` and `role_definitions_subdb=ROLE_DEFINITIONS_SUBDB,` then `allow_synthetic_receipt=_config_dict().get("fort_synthetic_recipient_testing") is True,`. |
| 10 | `wallet-worker.py`, dispatch | The draft's `_dispatch_owned`, unchanged. `dev`'s `_dispatch_locked`, unchanged but for one line: it awaits `_dispatch_owned(method, params)` where it awaited `_dispatch(method, params)`. `handle_request` as on `dev`: a method in `_UNLOCKED_METHODS` goes straight to `_dispatch`; every other goes to `_dispatch_locked` under `_REQUEST_LOCK`. The draft's `except asyncio.CancelledError` branch is kept. |

Also check three places that merged without a conflict: `ALLOWED_METHODS` in `wallet-worker.py` holds `dev`'s `vaults.setDefault`, `sources.read`, `roles.list`, `roles.takeOn`, `roles.remove` and all of the draft's new names; `PageActions` in `app/app/page-factory.ts` holds both sides' methods; `package.json` still names the fork's twenty spec files under `test:e2e:prepared-runtime`.

The four test files change as follows. The full text is in `fortweb/task1-merge-test-fixes-vs-dev.diff` and `fortweb/task1-merge-test-fix-vs-draft.diff` beside this plan.

| File | Change | Why |
|---|---|---|
| `python/test_vaulting.py` | `DefaultVaultTest` no longer passes `legacy_root_salt`; its "record written before `isDefault`" carries `rootSalt` and `storageName`; the sub-database list expects `fwic.`, `fwpn.` and `fwrc.` as well. | The draft removed `legacy_root_salt`, refuses a vault record without a root salt and a storage name, and adds three sub-databases. |
| `python/test_roling.py` | The stand-in for `js` gains `navigator.locks` (`FakeLocks`) and `Object.new`; the stand-in Habery gains `ks.flush` and `db.flush`; the registry fixture's vault records gain `storageName`. | The draft's worker takes a Web Lock before each request and writes to storage after it. |
| `playwright/fortweb-roles-take-on.spec.ts` | `IDBMap/@fortweb/…` becomes `IDBMap/@fortweb-v2/…` in two places. | The draft renamed the storage. |
| `playwright/webbaser-lifecycle.spec.ts` (the draft's own) | After unlock it expects the heading `Roles` at level 1 where it expected `Local Identifiers`. | The fork's landing page is Roles. |

- [ ] **Step 5: Build with the draft's runtime source and run the quick checks**

Run the build block under **Interfaces** above. Then:

```bash
python3 -m unittest python.test_sourcing python.test_roling python.test_vaulting python.test_transporting python.test_onboarding
npm run typecheck
```

Expected: `Ran 225 tests` and `OK`; the type check prints no error.

- [ ] **Step 6: Commit the merge**

```bash
git commit -m "Merge the Foundation's draft credential engine (keri-foundation/fortweb#42 at 2db99e8) into credentials (<bead>)

Both sides are kept in all ten conflicting places. Four test files follow the
draft's changes: its storage is renamed, its worker takes a lock, and a vault
record needs a root salt and a storage name."
git diff HEAD recorded-merge --stat          # expected: no output
git rev-parse 'HEAD^{tree}'                  # expected: d23d8d751935706f02d9f925449ce1c57be28048
git branch -D recorded-merge
```

- [ ] **Step 7: Rewrite the one test that opened a second engine**

The test "after the runtime is discarded in the background, taking on a role needs the address opened again" starts a second engine while the page's own holds the vault. The draft refuses that with `BUSY`. The page's engine gives the vault up first.

```bash
git apply /home/user/fortoid/.internal/plans/2026-11-14-credentials-base/fortweb/task1-rewritten-test.diff
```

It is this change:

```diff
diff --git a/playwright/fortweb-roles-take-on.spec.ts b/playwright/fortweb-roles-take-on.spec.ts
index 51185a5..83d44fb 100644
--- a/playwright/fortweb-roles-take-on.spec.ts
+++ b/playwright/fortweb-roles-take-on.spec.ts
@@ -203,7 +203,10 @@ test.describe('Taking on a role', () => {
             const roles = rolesPage(page);
             await expect(roles.address).toHaveValue('');
             await expect(roles.cards).toHaveCount(0);
-            // A fresh runtime, as after the discard, refuses to take on a role from a read it has not made.
+            // A fresh runtime, as after the discard, refuses to take on a role from a read it has not made. Only one
+            // runtime at a time may hold the wallet's storage, so the page's own gives the vault up first.
+            await page.getByRole('button', { name: 'Lock vault' }).click();
+            await expect(page.getByRole('heading', { name: 'Open My Wallet' })).toBeVisible({ timeout: 240_000 });
             await startRequest(page, 'vaults.open', { vaultId: 'my-wallet', passcode: 'roles-pass-1' });
             expect((await finishRequest(page)).ok).toBe(true);
             await startRequest(page, 'roles.takeOn', {
```

Run it:

```bash
npx playwright test -c playwright.local.config.ts --trace off playwright/fortweb-roles-take-on.spec.ts -g "discarded in the background"
```

Expected: `1 passed` (about 30 seconds).

```bash
git add playwright/fortweb-roles-take-on.spec.ts
git commit -m "test(roles): the page gives the vault up before a second engine opens it (<bead>)

The draft lets one engine at a time hold the wallet's storage."
```

- [ ] **Step 8: Let the workflow run on the new branch**

In `.github/workflows/fortweb-runtime-playwright.yml`, under `on.push.branches`, add `credentials` after `dev`:

```yaml
  push:
    branches:
      - main
      - dev
      - credentials
```

```bash
git add .github/workflows/fortweb-runtime-playwright.yml
git commit -m "ci: the checks also run on pushes to credentials (<bead>)"
```

- [ ] **Step 9: Run the whole check**

```bash
npm run test:e2e:prepared-runtime -- -c playwright.local.config.ts --trace off 2>&1 | tee /tmp/credentials-task1-browser.log | tail -15
npm run test:runtime-publication-workflow
```

Expected: `139 passed`, no `failed`, no `flaky`, no `Slow test file` (about nine minutes); the workflow test reports 15 passing.

- [ ] **Step 10: Push and watch the fork's workflow**

```bash
git push -u origin credentials
```

The run of "FortWeb Runtime and Playwright Checks" for the pushed commit must end green. Besides what was run above it runs the draft's lifecycle suite, a canary and a wheelhouse test. If a job fails, use the systematic-debugging skill on its log; do not change a test to make it pass. Put the run's number in the report.

---

### Task 2: The draft's test switch is on

**Repository:** `/home/user/fortweb`, branch `credentials`

**Files:**
- Modify: `pyscript-ci.toml`
- Modify: `scripts/verify_runtime_tree.py` (in `validate_runtime_config`)
- Modify: `python/test_verify_runtime_tree.py`
- Create: `playwright/fortweb-credentials-switch.spec.ts`
- Modify: `package.json` (`test:e2e:prepared-runtime`)
- Modify: `.github/actions/produce-runtime-package/action.yml` (the list of Python tests)

**Interfaces:**
- Consumes: Task 1's branch and build environment. The draft reads the switch in `app/runtime/wallet-worker.py`: `allow_synthetic_receipt=_config_dict().get("fort_synthetic_recipient_testing") is True`.
- Produces: in every build of `credentials`, the methods `ipex.grant`, `ipex.receive`, `ipex.admit`, `ipex.present`, `ipex.verifyPresentation`, `ipex.get`, `credentials.receive` and `credentials.getReceived` run when the caller passes `syntheticOnly: true`. Later plans rely on this.

**Acceptance Criteria:**
- A build of `credentials` carries `fort_synthetic_recipient_testing = true` in `dist/runtime/pyscript-ci.toml`.
- The verifier refuses a packaged config without the key, and one where the key is not `true`.
- In a browser, `ipex.get` for an exchange that does not exist answers `NOT_FOUND` when the caller passes `syntheticOnly: true`, and `UNSUPPORTED` when it does not.
- `python3 -m unittest python/test_verify_runtime_tree.py` passes (7 tests), and the fork's workflow runs that file.

- [ ] **Step 1: Write the failing tests**

Add two tests to `python/test_verify_runtime_tree.py`, before `test_rejects_changed_worker_file_map`:

```bash
git apply --include='python/test_verify_runtime_tree.py' \
  /home/user/fortoid/.internal/plans/2026-11-14-credentials-base/fortweb/task2-switch.diff
```

They are:

```python
    def test_rejects_the_credential_switch_turned_off(self):
        raw = (RUNTIME / "pyscript-ci.toml").read_text()
        changed = raw.replace("fort_synthetic_recipient_testing = true", "fort_synthetic_recipient_testing = false")
        self.assertNotEqual(changed, raw)
        with self.assertRaisesRegex(RuntimeError, "credential test switch is not exact"):
            verifier.validate_runtime_config(changed.encode(), RUNTIME)

    def test_rejects_a_config_without_the_credential_switch(self):
        raw = (RUNTIME / "pyscript-ci.toml").read_text()
        changed = "\n".join(line for line in raw.splitlines()
                            if not line.startswith("fort_synthetic_recipient_testing")) + "\n"
        self.assertNotEqual(changed, raw)
        with self.assertRaisesRegex(RuntimeError, "unexpected top-level keys"):
            verifier.validate_runtime_config(changed.encode(), RUNTIME)
```

Create `playwright/fortweb-credentials-switch.spec.ts` (the file is `fortweb/fortweb-credentials-switch.spec.ts` beside this plan):

```ts
import { expect, test } from '@playwright/test';

import { finishRequest, setUpWallet, startRequest } from './fixtures/roles';

const NO_SUCH_EXCHANGE = `E${'A'.repeat(43)}`;

test.describe('The credential engine in this build', () => {
    test('is switched on: an exchange that does not exist is not found, where a build without the switch says unsupported',
        async ({ page }) => {
            await setUpWallet(page);
            // Only one runtime at a time may hold the wallet's storage, so the page's own gives the vault up first.
            await page.getByRole('button', { name: 'Lock vault' }).click();
            await expect(page.getByRole('heading', { name: 'Open My Wallet' })).toBeVisible({ timeout: 240_000 });
            await startRequest(page, 'vaults.open', { vaultId: 'my-wallet', passcode: 'roles-pass-1' });
            expect((await finishRequest(page)).ok).toBe(true);

            await startRequest(page, 'ipex.get', { vaultId: 'my-wallet', syntheticOnly: true, said: NO_SUCH_EXCHANGE });
            expect(await finishRequest(page)).toMatchObject({ ok: false, code: 'NOT_FOUND' });

            // The engine still asks every caller to say that the credentials are test ones.
            await startRequest(page, 'ipex.get', { vaultId: 'my-wallet', said: NO_SUCH_EXCHANGE });
            expect(await finishRequest(page)).toMatchObject({ ok: false, code: 'UNSUPPORTED' });
        });
});
```

In `package.json`, at the end of the list in `test:e2e:prepared-runtime`, after `playwright/fortweb-roles-time.spec.ts`, add ` playwright/fortweb-credentials-switch.spec.ts`.

In `.github/actions/produce-runtime-package/action.yml`, in the step "Test source contracts", the list of Python tests ends with `python/test_roling.py`. Add the verifier's tests after it:

```yaml
          python/test_sourcing.py \
          python/test_roling.py \
          python/test_verify_runtime_tree.py
```

- [ ] **Step 2: Run them and see them fail**

```bash
python3 -m unittest python/test_verify_runtime_tree.py
npx playwright test -c playwright.local.config.ts --trace off playwright/fortweb-credentials-switch.spec.ts
```

Expected: the two new Python tests fail at `assertNotEqual` (the built config has no such line yet). The browser test fails at the first `ipex.get`: it answers `UNSUPPORTED` where `NOT_FOUND` is expected.

- [ ] **Step 3: Turn the switch on and teach the verifier**

```bash
git apply --include='pyscript-ci.toml' --include='scripts/verify_runtime_tree.py' \
  /home/user/fortoid/.internal/plans/2026-11-14-credentials-base/fortweb/task2-switch.diff
```

The whole of `task2-switch.diff` is this; its test part is in place from Step 1:

```diff
diff --git a/pyscript-ci.toml b/pyscript-ci.toml
index 57ddf01..854e612 100644
--- a/pyscript-ci.toml
+++ b/pyscript-ci.toml
@@ -1,4 +1,7 @@
 interpreter = "./vendor/pyodide/314.0.5/pyodide.mjs"
+# This fork turns on the Foundation's test-only switch for received credentials (decision D3 of Fortoid's design,
+# .internal/specs/2026-11-14-standard-credential-screens-design.md).
+fort_synthetic_recipient_testing = true
 
 [fort_runtime_packages]
 manifest = "./runtime-closure.json"
diff --git a/python/test_verify_runtime_tree.py b/python/test_verify_runtime_tree.py
index 899be52..9b1080b 100644
--- a/python/test_verify_runtime_tree.py
+++ b/python/test_verify_runtime_tree.py
@@ -67,6 +67,21 @@ class RuntimeRuntimeVerifierTest(unittest.TestCase):
                 RUNTIME,
             )
 
+    def test_rejects_the_credential_switch_turned_off(self):
+        raw = (RUNTIME / "pyscript-ci.toml").read_text()
+        changed = raw.replace("fort_synthetic_recipient_testing = true", "fort_synthetic_recipient_testing = false")
+        self.assertNotEqual(changed, raw)
+        with self.assertRaisesRegex(RuntimeError, "credential test switch is not exact"):
+            verifier.validate_runtime_config(changed.encode(), RUNTIME)
+
+    def test_rejects_a_config_without_the_credential_switch(self):
+        raw = (RUNTIME / "pyscript-ci.toml").read_text()
+        changed = "\n".join(line for line in raw.splitlines()
+                            if not line.startswith("fort_synthetic_recipient_testing")) + "\n"
+        self.assertNotEqual(changed, raw)
+        with self.assertRaisesRegex(RuntimeError, "unexpected top-level keys"):
+            verifier.validate_runtime_config(changed.encode(), RUNTIME)
+
     def test_rejects_changed_worker_file_map(self):
         raw = (RUNTIME / "pyscript-ci.toml").read_text()
         changed = raw.replace(
diff --git a/scripts/verify_runtime_tree.py b/scripts/verify_runtime_tree.py
index 6e8dea1..8580c17 100644
--- a/scripts/verify_runtime_tree.py
+++ b/scripts/verify_runtime_tree.py
@@ -94,8 +94,12 @@ def safe_config_reference(value: object, label: str) -> str:
 
 def validate_runtime_config(raw: bytes, root: Path) -> dict:
     config = tomllib.loads(raw.decode())
-    if set(config) != {"interpreter", "fort_runtime_packages", "files"}:
+    if set(config) != {"interpreter", "fort_runtime_packages", "files", "fort_synthetic_recipient_testing"}:
         raise RuntimeError("packaged PyScript config has unexpected top-level keys")
+    # This fork turns on the Foundation's test-only switch for received credentials. The value is exact: anything
+    # but true would turn receiving a credential off without a word.
+    if config["fort_synthetic_recipient_testing"] is not True:
+        raise RuntimeError("packaged credential test switch is not exact")
     if config["interpreter"] != "./vendor/pyodide/314.0.5/pyodide.mjs":
         raise RuntimeError("packaged PyScript interpreter is not exact")
     package_config = config["fort_runtime_packages"]
```

- [ ] **Step 4: Build, and run the tests again**

```bash
npm run build:runtime
grep -n fort_synthetic_recipient_testing dist/runtime/pyscript-ci.toml
python3 -m unittest python/test_verify_runtime_tree.py
npx playwright test -c playwright.local.config.ts --trace off playwright/fortweb-credentials-switch.spec.ts
npm run test:runtime-publication-workflow
```

Expected: the key is in the built file; `Ran 7 tests`, `OK`; `1 passed`; 15 passing.

- [ ] **Step 5: Commit**

```bash
git add pyscript-ci.toml scripts/verify_runtime_tree.py python/test_verify_runtime_tree.py .github/actions/produce-runtime-package/action.yml
git commit -m "feat(runtime): the Foundation's test switch for received credentials is on in this fork (<bead>)

The verifier takes the key only with the value true, and the workflow runs
the verifier's tests."
git add playwright/fortweb-credentials-switch.spec.ts package.json
git commit -m "test(credentials): a built wallet answers ipex.get, and still asks callers to say the credentials are test ones (<bead>)"
```

One of the draft's own tests, which no workflow runs, states the opposite of this task and fails from here on: "recipient API is disabled in the ordinary runtime" in `playwright/registry-receipt.spec.ts`. Leave the file as it is and say so in the report.

---

### Task 3: The wallet asks the browser to keep its storage

**Repository:** `/home/user/fortweb`, branch `credentials`

**Files:**
- Create: `app/runtime/storage-keeping.ts`
- Modify: `app/app/main.ts` (`openVault`, one import)
- Modify: `app/app/page-loader.ts` (`RouteDataResult`, the `settings` route, two imports)
- Modify: `app/app/page-factory.ts` (the `settings` route)
- Modify: `app/features/settings/settings-page.ts`
- Modify: `tsconfig.json`, `tsconfig.build.json` (their `include` lists)
- Create: `playwright/fortweb-storage-keeping.spec.ts`
- Modify: `package.json` (`test:e2e:prepared-runtime`)

**Interfaces:**
- Consumes: Task 1's branch; `isNativeAvailable()` from `app/runtime/native-capabilities.ts`, which is true inside the phone app.
- Produces, in `app/runtime/storage-keeping.ts`:
  - `type StorageKept = "kept" | "not-kept" | "unknown"`
  - `askToKeepStorage(): Promise<StorageKept>` asks (`navigator.storage.persist()`).
  - `storageKept(): Promise<StorageKept>` reads without asking (`navigator.storage.persisted()`).
  - `RouteDataResult.storageKept?: StorageKept` for the `settings` route, absent inside the phone app.

**Acceptance Criteria:**
- Each time a vault is opened, which includes the moment a wallet is made, the wallet calls `navigator.storage.persist()` once and does not wait for the answer. A browser that said no is asked again at the next opening: a real Chrome says no to a site it has just met.
- In a regular browser, Settings shows `Kept by the Browser` with `Yes`, with `No. The browser may remove this wallet's storage when it runs short of space.`, or with `This browser does not say.`
- Inside the phone app the wallet asks too, and Settings shows no such line: what Android's WebView answers, and what it then does with the storage, is not known yet, and the wallet does not say what it does not know.
- Nothing is asked before a vault is opened.
- `npm run typecheck` reports no error; `playwright/fortweb-storage-keeping.spec.ts` passes (4 tests).

- [ ] **Step 1: Write the failing test**

Create `playwright/fortweb-storage-keeping.spec.ts` (`fortweb/fortweb-storage-keeping.spec.ts` beside this plan):

```ts
import { expect, test, type Page } from '@playwright/test';

import { setUpWallet } from './fixtures/roles';

interface Asked { asked: number; kept: boolean }

/**
 * The browser's answers are made the test's own: it counts how often it is asked and gives `answers` in turn, the
 * last one again from then on.
 */
async function browserThatSays(page: Page, ...answers: boolean[]): Promise<void> {
    await page.addInitScript((given) => {
        const state = { asked: 0, kept: false };
        (window as unknown as { storageAsked: typeof state }).storageAsked = state;
        StorageManager.prototype.persist = async function persist(): Promise<boolean> {
            state.kept = given[Math.min(state.asked, given.length - 1)] === true;
            state.asked += 1;
            return state.kept;
        };
        StorageManager.prototype.persisted = async function persisted(): Promise<boolean> {
            return state.kept;
        };
    }, answers);
}

/** Stands in for the phone app: its bridge is there, and offers nothing. */
async function insideThePhoneApp(page: Page): Promise<void> {
    await page.addInitScript(() => {
        const listeners: Array<(event: { data: string }) => void> = [];
        (window as unknown as Record<string, unknown>)['bridge'] = {
            addEventListener(type: string, listener: (event: { data: string }) => void) {
                if (type === 'message') listeners.push(listener);
            },
            postMessage(raw: string) {
                const request = JSON.parse(raw) as { type?: string; id?: string };
                if (request.type !== 'native_request') return;
                const response = {
                    type: 'native_response', id: request.id, ok: false, error: { code: 'unknown_capability', message: 'none' },
                };
                setTimeout(() => listeners.forEach((listener) => listener({ data: JSON.stringify(response) })), 0);
            },
        };
    });
}

async function asked(page: Page): Promise<Asked> {
    return page.evaluate(() => (window as unknown as { storageAsked: Asked }).storageAsked);
}

async function openSettings(page: Page): Promise<void> {
    await page.getByRole('navigation', { name: 'Vault navigation' }).getByRole('link', { name: 'Settings', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Settings', exact: true, level: 1 })).toBeVisible({ timeout: 60_000 });
}

function keptByTheBrowser(page: Page) {
    return page.locator('.detail-item', { has: page.getByText('Kept by the Browser', { exact: true }) }).locator('dd');
}

test.describe('Keeping the wallet\'s storage', () => {
    test('making a wallet asks the browser once, and Settings says the browser agreed', async ({ page }) => {
        await browserThatSays(page, true);
        await setUpWallet(page);
        await expect.poll(async () => (await asked(page)).asked).toBe(1);
        await openSettings(page);
        await expect(keptByTheBrowser(page)).toHaveText('Yes');
    });

    test('Settings says so when the browser did not agree', async ({ page }) => {
        await browserThatSays(page, false);
        await setUpWallet(page);
        await expect.poll(async () => (await asked(page)).asked).toBe(1);
        await openSettings(page);
        await expect(keptByTheBrowser(page))
            .toHaveText('No. The browser may remove this wallet\'s storage when it runs short of space.');
    });

    test('a browser that said no is asked again when the vault is next opened, and Settings follows its answer', async ({ page }) => {
        await browserThatSays(page, false, true);
        await setUpWallet(page);
        await expect.poll(async () => (await asked(page)).asked).toBe(1);
        await page.getByRole('button', { name: 'Lock vault' }).click();
        await expect(page.getByRole('heading', { name: 'Open My Wallet' })).toBeVisible({ timeout: 60_000 });
        await page.getByLabel('Passcode', { exact: true }).fill('roles-pass-1');
        await page.getByRole('button', { name: 'Open', exact: true }).click();
        await expect(page.getByRole('heading', { name: 'Roles', exact: true, level: 1 })).toBeVisible({ timeout: 60_000 });
        await expect.poll(async () => (await asked(page)).asked).toBe(2);
        await openSettings(page);
        await expect(keptByTheBrowser(page)).toHaveText('Yes');
    });

    test('inside the phone app the wallet still asks, and Settings has no line about a browser', async ({ page }) => {
        await browserThatSays(page, false);
        await insideThePhoneApp(page);
        await setUpWallet(page);
        await expect.poll(async () => (await asked(page)).asked).toBe(1);
        await openSettings(page);
        await expect(page.getByText('Storage Backend', { exact: true })).toBeVisible();
        await expect(page.getByText('Kept by the Browser', { exact: true })).toHaveCount(0);
    });
});
```

In `package.json`, at the end of the list in `test:e2e:prepared-runtime`, add ` playwright/fortweb-storage-keeping.spec.ts`.

- [ ] **Step 2: Run it and see it fail**

```bash
npx playwright test -c playwright.local.config.ts --trace off playwright/fortweb-storage-keeping.spec.ts
```

Expected: all four tests fail at their first `expect.poll(...).toBe(1)`: the browser was asked 0 times.

- [ ] **Step 3: Write the module**

Create `app/runtime/storage-keeping.ts` (`fortweb/storage-keeping.ts` beside this plan):

```ts
/**
 * Whether the browser has agreed to keep this wallet's storage.
 *
 * A browser may remove a site's storage when space runs short unless the site has asked to keep it and the browser
 * has agreed. Chrome and Edge decide by themselves, without asking the person, and say no to a site they have just
 * met; they may say yes once it has been used. So the wallet asks each time a vault is opened, which includes the
 * moment it is made. In a regular browser, Settings shows the answer. Inside the phone app the wallet asks too and
 * shows nothing: what Android's WebView answers, and what it then does, is not yet known.
 */
export type StorageKept = "kept" | "not-kept" | "unknown";

function storageManager(): StorageManager | null {
    return typeof navigator !== "undefined" && navigator.storage ? navigator.storage : null;
}

async function answer(ask: (storage: StorageManager) => Promise<boolean>): Promise<StorageKept> {
    const storage = storageManager();
    if (!storage) {
        return "unknown";
    }
    try {
        return (await ask(storage)) ? "kept" : "not-kept";
    } catch {
        return "unknown";
    }
}

/** Asks the browser to keep the wallet's storage. A browser that says no is asked again at the next opening. */
export function askToKeepStorage(): Promise<StorageKept> {
    return answer((storage) => (typeof storage.persist === "function" ? storage.persist() : Promise.reject()));
}

/** What the browser says now, without asking for anything. */
export function storageKept(): Promise<StorageKept> {
    return answer((storage) => (typeof storage.persisted === "function" ? storage.persisted() : Promise.reject()));
}
```

- [ ] **Step 4: Ask when a vault is opened, and show the answer in Settings**

```bash
git apply /home/user/fortoid/.internal/plans/2026-11-14-credentials-base/fortweb/task3-keeping-storage.diff
```

It is this change:

```diff
diff --git a/app/app/main.ts b/app/app/main.ts
index 602642c..ca0ebcf 100644
--- a/app/app/main.ts
+++ b/app/app/main.ts
@@ -29,6 +29,7 @@ import { renderSetupPage } from "../features/vaults/setup-page.js";
 import { installGlobalHandlers } from "../runtime/global-handlers.js";
 import { fingerprintState } from "../runtime/vault-fingerprint.js";
 import { METHODS } from "../runtime/method-catalog.js";
+import { askToKeepStorage } from "../runtime/storage-keeping.js";
 import {
     readErrorMessage,
     type KeptRead,
@@ -376,6 +377,9 @@ actions = {
 
     async openVault(vaultId: string, passcode = ""): Promise<Record<string, unknown>> {
         const { vault } = await bridge.request<{ vault: Record<string, unknown> }>(METHODS.vaultsOpen, { vaultId, passcode });
+        // A browser that has just met the wallet says no; it may say yes once the wallet has been used. So the
+        // wallet asks at every opening, and does not wait for the answer.
+        void askToKeepStorage();
         session.patch({
             unlockedVaultId: vaultId,
             vaultSummary: assumeType<VaultSummary>(vault),
diff --git a/app/app/page-factory.ts b/app/app/page-factory.ts
index 7c77360..ba9e29a 100644
--- a/app/app/page-factory.ts
+++ b/app/app/page-factory.ts
@@ -210,6 +210,7 @@ export async function loadPage({
             page: renderSettingsPage({
                 vault: assumeType<SettingsPageProps["vault"]>(findVault(route.params.vaultId)),
                 settings: assumeType<SettingsPageProps["settings"]>(pageData.settings),
+                storageKept: assumeType<SettingsPageProps["storageKept"]>(pageData.storageKept),
                 onMakeDefault: (vaultId: string) => actions.setDefaultVault(vaultId),
             }),
             vault: findVault(route.params.vaultId),
diff --git a/app/app/page-loader.ts b/app/app/page-loader.ts
index 0fb215c..973d36e 100644
--- a/app/app/page-loader.ts
+++ b/app/app/page-loader.ts
@@ -1,5 +1,7 @@
 import { type Route } from "./router.js";
 import { METHODS } from "../runtime/method-catalog.js";
+import { isNativeAvailable } from "../runtime/native-capabilities.js";
+import { storageKept, type StorageKept } from "../runtime/storage-keeping.js";
 import { postLog } from "../runtime/logger.js";
 
 type RecordValue = Record<string, unknown>;
@@ -27,6 +29,8 @@ export interface RouteDataResult {
     /** roles.list failed, not because the vault closed: the page says My roles could not be read. */
     rolesUnreadable?: boolean;
     settings?: RecordValue;
+    /** What the browser says about keeping the wallet's storage. Absent inside the phone app, which is no browser. */
+    storageKept?: StorageKept;
     watcherError?: string;
     watchers?: RecordValue[];
     witnessError?: string;
@@ -80,9 +84,10 @@ export async function loadRouteData({ bridge, route }: RouteDataLoaderContext):
     }
 
     if (route.name === "settings") {
-        return bridge.request<{ settings: RecordValue }>(METHODS.settingsGet, {
+        const loaded = await bridge.request<{ settings: RecordValue }>(METHODS.settingsGet, {
             vaultId: route.params.vaultId,
         });
+        return isNativeAvailable() ? loaded : { ...loaded, storageKept: await storageKept() };
     }
 
     if (route.name === "kf-identifiers") {
diff --git a/app/features/settings/settings-page.ts b/app/features/settings/settings-page.ts
index 371da78..fe86d10 100644
--- a/app/features/settings/settings-page.ts
+++ b/app/features/settings/settings-page.ts
@@ -22,6 +22,8 @@ interface SettingsRecord {
 interface SettingsPageProps {
     vault: VaultRecord;
     settings: SettingsRecord;
+    /** What the browser says about keeping this wallet's storage. Absent inside the phone app and in fixtures. */
+    storageKept?: "kept" | "not-kept" | "unknown";
     onDeleteVault?(vaultId: string): Promise<void>;
     /** Makes this the vault that opens when the app starts. */
     onMakeDefault?(vaultId: string): Promise<void>;
@@ -31,6 +33,16 @@ function enabledLabel(value: boolean): string {
     return value ? "Enabled" : "Disabled";
 }
 
+function keptLabel(value: NonNullable<SettingsPageProps["storageKept"]>): string {
+    if (value === "kept") {
+        return "Yes";
+    }
+    if (value === "not-kept") {
+        return "No. The browser may remove this wallet's storage when it runs short of space.";
+    }
+    return "This browser does not say.";
+}
+
 function detailItem(label: string, value: string): HTMLDivElement {
     const wrapper = document.createElement("div");
     wrapper.className = "detail-item";
@@ -173,7 +185,7 @@ function startupCard(vault: VaultRecord, onMakeDefault: SettingsPageProps["onMak
     return card;
 }
 
-export function renderSettingsPage({ vault, settings, onDeleteVault, onMakeDefault }: SettingsPageProps) {
+export function renderSettingsPage({ vault, settings, storageKept, onDeleteVault, onMakeDefault }: SettingsPageProps) {
     return {
         title: "Settings",
         render(container: HTMLElement): void {
@@ -230,10 +242,11 @@ export function renderSettingsPage({ vault, settings, onDeleteVault, onMakeDefau
 
             const runtimeGrid = document.createElement("dl");
             runtimeGrid.className = "detail-grid";
-            runtimeGrid.append(
-                detailItem("Storage Backend", settings.storageBackend),
-                detailItem("Runtime Status", settings.runtimeStatus),
-            );
+            runtimeGrid.append(detailItem("Storage Backend", settings.storageBackend));
+            if (storageKept) {
+                runtimeGrid.append(detailItem("Kept by the Browser", keptLabel(storageKept)));
+            }
+            runtimeGrid.append(detailItem("Runtime Status", settings.runtimeStatus));
 
             runtimeCard.append(runtimeTitle, runtimeGrid);
             columns.append(startupCard(vault, onMakeDefault), defaultsCard, runtimeCard);
diff --git a/tsconfig.build.json b/tsconfig.build.json
index 4035c44..f9c138a 100644
--- a/tsconfig.build.json
+++ b/tsconfig.build.json
@@ -10,6 +10,7 @@
   "include": [
     "app/runtime/messages.ts",
     "app/runtime/method-catalog.ts",
+    "app/runtime/storage-keeping.ts",
     "app/shared/dom.ts",
     "app/shared/components.ts",
     "app/app/router.ts",
diff --git a/tsconfig.json b/tsconfig.json
index 8ea4c8b..0852904 100644
--- a/tsconfig.json
+++ b/tsconfig.json
@@ -17,6 +17,7 @@
   "include": [
     "app/runtime/messages.ts",
     "app/runtime/method-catalog.ts",
+    "app/runtime/storage-keeping.ts",
     "app/shared/dom.ts",
     "app/shared/components.ts",
     "app/app/router.ts",
```

`void askToKeepStorage()` is deliberate: the answer is not waited for, so opening a vault is no slower, and a browser that never answers holds nothing up. It sits in `openVault`, the one place every opening passes through; a vault that was just made is opened at once.

- [ ] **Step 5: Check, build and run the tests**

```bash
npm run typecheck
npm run build:runtime
npx playwright test -c playwright.local.config.ts --trace off playwright/fortweb-storage-keeping.spec.ts
npx playwright test -c playwright.local.config.ts --trace off playwright/fortweb-wallet-first.spec.ts playwright/fortweb-accessibility.spec.ts playwright/fortweb-fingerprint-unlock.spec.ts
```

Expected: no type error; `4 passed`; the wallet-first, accessibility and fingerprint specs pass as before, 24 tests (they make and open wallets).

- [ ] **Step 6: Commit**

```bash
git add app/runtime/storage-keeping.ts tsconfig.json tsconfig.build.json
git commit -m "feat(storage): ask the browser to keep the wallet's storage, and read its answer (<bead>)"
git add app/app/main.ts app/app/page-loader.ts app/app/page-factory.ts app/features/settings/settings-page.ts
git commit -m "feat(settings): the wallet asks at every opening of a vault, and Settings shows what the browser said (<bead>)

Chrome says no to a site it has just met, so one question when the vault
is made would be the wrong moment. Inside the phone app there is no such
line: what Android's WebView does with the answer is not known yet."
git add playwright/fortweb-storage-keeping.spec.ts package.json
git commit -m "test(storage): opening a vault asks; a browser that said no is asked again; nothing is shown inside the phone app (<bead>)"
```

---

### Task 4: A start script for a computer

**Repository:** `/home/user/fortweb`, branch `credentials`

**Files:**
- Create: `scripts/start_wallet.py`
- Create: `python/test_start_wallet.py`
- Create: `playwright/fortweb-desktop-start.spec.ts`
- Modify: `package.json` (`test:e2e:prepared-runtime`)
- Modify: `.github/actions/produce-runtime-package/action.yml` (the list of Python tests)

**Interfaces:**
- Consumes: a wallet package folder: `app/`, `vendor/`, `wheels/`, `pyscript-ci.toml`, `runtime-closure.json` (what `npm run build:runtime` writes to `dist/runtime`, and what Fortoid imports as its payload).
- Produces:
  - `python3 scripts/start_wallet.py [--root FOLDER] [--no-browser]`. It serves `FOLDER` (default: the folder the script is in) at `http://127.0.0.1:8791/` and prints `The wallet is at http://127.0.0.1:8791/`. Exit code 1 when the port is taken, 2 when the folder is not a wallet package. Names the tests use: `HOST`, `PORT`, `SERVED`, `resolve(root, target)`, `Server(root, port)`, `main(argv)`.
  - The browser test reads `FORT_WALLET_FOLDER`. Unset, it serves this checkout's `dist/runtime` with `scripts/start_wallet.py`. Set to an unzipped download, it starts that folder's own `start-wallet.py`. Task 7 uses this.

**Acceptance Criteria:**
- The wallet opens in Chromium from `http://127.0.0.1:8791/`, a wallet can be made there, and it is still there after a reload.
- A request that names `localhost:8791` is sent on to `127.0.0.1:8791`. A request that names any other host gets status 421 and none of the wallet.
- Nothing outside the five package entries is served: not the script, not a README, not a directory listing, not a path with `..`, not a link that points out of the folder.
- The server listens on `127.0.0.1` only. A taken port stops the script with a sentence that says why it does not pick another.
- Every file it serves carries `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`, `Cross-Origin-Opener-Policy: same-origin` and `Cross-Origin-Embedder-Policy: require-corp`.
- `python3 -m unittest python/test_start_wallet.py` passes (13 tests) and the fork's workflow runs that file; `playwright/fortweb-desktop-start.spec.ts` passes (2 tests).

- [ ] **Step 1: Write the failing tests**

Create `python/test_start_wallet.py` (`fortweb/test_start_wallet.py` beside this plan):

```python
"""The script that serves the wallet to a browser on a computer (scripts/start_wallet.py)."""
import http.client
import importlib.util
import os
import socket
import tempfile
import threading
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent.parent / "scripts" / "start_wallet.py"
spec = importlib.util.spec_from_file_location("start_wallet", SCRIPT)
start_wallet = importlib.util.module_from_spec(spec)
spec.loader.exec_module(start_wallet)


def package(folder: Path) -> Path:
    """A folder shaped like a wallet package, with two files that are not part of one."""
    folder.mkdir()
    (folder / "app").mkdir()
    (folder / "app" / "index.html").write_text("<!doctype html><title>wallet</title>")
    (folder / "app" / "main.js").write_text("export {};")
    (folder / "vendor").mkdir()
    (folder / "wheels").mkdir()
    (folder / "wheels" / "a.whl").write_bytes(b"PK")
    (folder / "pyscript-ci.toml").write_text('interpreter = "./vendor/x.mjs"\n')
    (folder / "runtime-closure.json").write_text("{}")
    (folder / "start-wallet.py").write_text("# the script itself is not served")
    (folder / "README.txt").write_text("not served")
    return folder


class ResolveTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.outside = Path(self.temp.name).resolve() / "outside.txt"
        self.outside.write_text("secret")
        self.root = package(Path(self.temp.name).resolve() / "wallet")

    def test_names_a_served_file(self):
        self.assertEqual(start_wallet.resolve(self.root, "/app/index.html"), self.root / "app" / "index.html")
        self.assertEqual(start_wallet.resolve(self.root, "/pyscript-ci.toml?x=1"), self.root / "pyscript-ci.toml")
        self.assertEqual(start_wallet.resolve(self.root, "/wheels/a.whl"), self.root / "wheels" / "a.whl")

    def test_names_nothing_outside_what_a_package_holds(self):
        for target in ("/", "/README.txt", "/start-wallet.py", "/app", "/app/", "/app/missing.js", "/vendor",
                       "/../outside.txt", "/app/../../outside.txt", "/app/%2e%2e/%2e%2e/outside.txt",
                       "/app/..%2f..%2foutside.txt", "/app\\..\\..\\outside.txt", "/app/index.html%00.js",
                       "app/index.html", "//app/index.html", "/APP/index.html" if os.name != "nt" else "/nothing"):
            with self.subTest(target=target):
                self.assertIsNone(start_wallet.resolve(self.root, target))

    def test_a_link_out_of_the_folder_is_not_followed(self):
        link = self.root / "app" / "leak.txt"
        try:
            link.symlink_to(self.outside)
        except OSError:
            self.skipTest("this system makes no symbolic links")
        self.assertIsNone(start_wallet.resolve(self.root, "/app/leak.txt"))


class ServerTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = package(Path(self.temp.name).resolve() / "wallet")
        self.server = start_wallet.Server(self.root, 0)
        self.port = self.server.server_address[1]
        thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        thread.start()
        self.addCleanup(thread.join)
        self.addCleanup(self.server.server_close)
        self.addCleanup(self.server.shutdown)

    def get(self, target, host=None, method="GET"):
        connection = http.client.HTTPConnection("127.0.0.1", self.port, timeout=10)
        self.addCleanup(connection.close)
        connection.putrequest(method, target, skip_host=True)
        connection.putheader("Host", host if host is not None else f"127.0.0.1:{self.port}")
        connection.endheaders()
        response = connection.getresponse()
        return response, response.read()

    def test_serves_a_file_with_its_type_and_the_isolation_headers(self):
        response, body = self.get("/app/main.js")
        self.assertEqual(response.status, 200)
        self.assertEqual(body, b"export {};")
        self.assertEqual(response.getheader("Content-Type"), "text/javascript; charset=utf-8")
        self.assertEqual(response.getheader("Cache-Control"), "no-store")
        self.assertEqual(response.getheader("X-Content-Type-Options"), "nosniff")
        self.assertEqual(response.getheader("Cross-Origin-Opener-Policy"), "same-origin")
        self.assertEqual(response.getheader("Cross-Origin-Embedder-Policy"), "require-corp")

    def test_head_sends_no_body(self):
        response, body = self.get("/app/main.js", method="HEAD")
        self.assertEqual((response.status, body, response.getheader("Content-Length")), (200, b"", "10"))

    def test_the_root_goes_to_the_start_page(self):
        response, _ = self.get("/")
        self.assertEqual(response.status, 307)
        self.assertEqual(response.getheader("Location"), f"http://127.0.0.1:{self.port}/app/index.html")

    def test_localhost_is_sent_on_to_the_one_address(self):
        response, _ = self.get("/app/index.html?a=b", host=f"localhost:{self.port}")
        self.assertEqual(response.status, 307)
        self.assertEqual(response.getheader("Location"), f"http://127.0.0.1:{self.port}/app/index.html?a=b")

    def test_another_host_is_refused(self):
        for host in ("wallet.example.test", f"wallet.example.test:{self.port}", "127.0.0.1", f"127.0.0.1:{self.port + 1}", ""):
            with self.subTest(host=host):
                response, body = self.get("/app/index.html", host=host)
                self.assertEqual(response.status, 421)
                self.assertNotIn(b"wallet</title>", body)

    def test_what_is_not_in_a_package_is_not_found(self):
        for target in ("/README.txt", "/start-wallet.py", "/app/", "/app/../README.txt", "/nothing"):
            with self.subTest(target=target):
                self.assertEqual(self.get(target)[0].status, 404)

    def test_other_methods_are_refused(self):
        response, _ = self.get("/app/index.html", method="POST")
        self.assertEqual(response.status, 501)

    def test_it_listens_on_this_computer_only(self):
        self.assertEqual(self.server.server_address[0], "127.0.0.1")


class MainTest(unittest.TestCase):
    def test_a_folder_that_is_not_a_package_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            self.assertEqual(start_wallet.main(["--root", folder, "--no-browser"]), 2)

    def test_a_taken_port_stops_the_script(self):
        with tempfile.TemporaryDirectory() as folder:
            root = package(Path(folder).resolve() / "wallet")
            holder = socket.socket()
            self.addCleanup(holder.close)
            try:
                holder.bind(("127.0.0.1", start_wallet.PORT))
            except OSError:
                self.skipTest(f"port {start_wallet.PORT} is already in use on this machine")
            holder.listen(1)
            self.assertEqual(start_wallet.main(["--root", str(root), "--no-browser"]), 1)


if __name__ == "__main__":
    unittest.main()
```

Create `playwright/fortweb-desktop-start.spec.ts` (`fortweb/fortweb-desktop-start.spec.ts` beside this plan):

```ts
import { spawn, type ChildProcess } from 'node:child_process';
import { join } from 'node:path';

import { expect, test } from '@playwright/test';

// The wallet as a person gets it on a computer: a wallet package served by the start script at its one address.
// Without FORT_WALLET_FOLDER that is this checkout's build, served by scripts/start_wallet.py. With it, it is an
// unzipped download, started by the script in that folder as its README says.
const ADDRESS = 'http://127.0.0.1:8791/';
const DOWNLOAD = process.env['FORT_WALLET_FOLDER'];
const COMMAND = DOWNLOAD
    ? [join(DOWNLOAD, 'start-wallet.py'), '--no-browser']
    : ['scripts/start_wallet.py', '--root', 'dist/runtime', '--no-browser'];
let script: ChildProcess | undefined;

test.beforeAll(async () => {
    const started = spawn('python3', COMMAND, { stdio: ['ignore', 'pipe', 'pipe'] });
    script = started;
    await new Promise<void>((resolve, reject) => {
        let said = '';
        const timer = setTimeout(() => reject(new Error(`the start script said nothing in 15 s: ${said}`)), 15_000);
        const hear = (chunk: Buffer): void => {
            said += chunk.toString();
            if (said.includes(ADDRESS)) {
                clearTimeout(timer);
                resolve();
            }
        };
        started.stdout?.on('data', hear);
        started.stderr?.on('data', hear);
        started.once('exit', (code) => {
            clearTimeout(timer);
            reject(new Error(`the start script stopped with code ${code}: ${said}`));
        });
    });
});

test.afterAll(() => {
    script?.kill();
});

test('the wallet opens from the start script, and a wallet made there is there after a reload', async ({ page }) => {
    await page.goto(ADDRESS);
    await expect(page).toHaveURL(/^http:\/\/127\.0\.0\.1:8791\/app\/index\.html/);
    await expect(page.getByRole('heading', { name: 'Set up your wallet' })).toBeVisible({ timeout: 240_000 });
    await page.getByLabel('Passcode', { exact: true }).fill('desktop-pass-1');
    await page.getByLabel('Confirm passcode', { exact: true }).fill('desktop-pass-1');
    await page.getByRole('button', { name: 'Create wallet' }).click();
    await expect(page.getByRole('heading', { name: 'Roles', exact: true, level: 1 })).toBeVisible({ timeout: 180_000 });
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Open My Wallet' })).toBeVisible({ timeout: 240_000 });
});

test('localhost is sent on to the one address, so there is one wallet', async ({ page }) => {
    await page.goto('http://localhost:8791/');
    await expect(page).toHaveURL(/^http:\/\/127\.0\.0\.1:8791\/app\/index\.html/);
});
```

In `package.json`, at the end of the list in `test:e2e:prepared-runtime`, add ` playwright/fortweb-desktop-start.spec.ts`.

In `.github/actions/produce-runtime-package/action.yml`, in the step "Test source contracts", add the new file to the end of the list of Python tests:

```yaml
          python/test_roling.py \
          python/test_verify_runtime_tree.py \
          python/test_start_wallet.py
```

- [ ] **Step 2: Run them and see them fail**

```bash
python3 -m unittest python/test_start_wallet.py
```

Expected: an error on import, `FileNotFoundError` for `scripts/start_wallet.py`.

- [ ] **Step 3: Write the script**

Create `scripts/start_wallet.py` (`fortweb/start_wallet.py` beside this plan):

```python
#!/usr/bin/env python3
"""Serves the wallet to a browser on this computer.

    python3 start-wallet.py

The wallet opens at http://127.0.0.1:8791/ in the default browser. A browser keeps a wallet's storage under the
address it was opened at, so the address is fixed: when the port is taken this script says so and stops, because
another port would be an empty wallet. It listens on this computer only, serves only the wallet's own files, and
answers only requests made to its own address.
"""
from __future__ import annotations

import argparse
import http.server
import mimetypes
import sys
import webbrowser
from pathlib import Path, PurePosixPath
from urllib.parse import unquote, urlsplit

HOST = "127.0.0.1"
PORT = 8791  # not 8765: the wallet's development server uses that, and one address is one wallet
START_PAGE = "app/index.html"
# What a wallet package holds at its top. Nothing else in the folder is served.
SERVED = ("app", "vendor", "wheels", "pyscript-ci.toml", "runtime-closure.json")
TYPES = {
    ".css": "text/css; charset=utf-8",
    ".html": "text/html; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".mjs": "text/javascript; charset=utf-8",
    ".py": "text/x-python; charset=utf-8",
    ".svg": "image/svg+xml",
    ".toml": "application/toml; charset=utf-8",
    ".wasm": "application/wasm",
    ".whl": "application/octet-stream",
}


def resolve(root: Path, target: str) -> Path | None:
    """The file a request target names, or None when it names nothing this script serves."""
    parts = PurePosixPath(unquote(urlsplit(target).path)).parts
    if len(parts) < 2 or parts[0] != "/" or parts[1] not in SERVED:
        return None
    if any(part in (".", "..") or "\\" in part or "\x00" in part for part in parts[1:]):
        return None
    try:
        found = root.joinpath(*parts[1:]).resolve(strict=True)
    except OSError:
        return None
    if not found.is_file() or root not in found.parents:
        return None
    return found


class Handler(http.server.BaseHTTPRequestHandler):
    server_version = "FortWallet"
    sys_version = ""

    def do_GET(self) -> None:
        self._answer(with_body=True)

    def do_HEAD(self) -> None:
        self._answer(with_body=False)

    def _answer(self, with_body: bool) -> None:
        port = self.server.server_address[1]
        own = f"{HOST}:{port}"
        host = self.headers.get("Host", "")
        if not self.path.startswith("/"):
            self._refuse(400, "Bad request.")
        elif host == f"localhost:{port}":
            # One address, one wallet: localhost would be a second, empty one.
            self._send_on(f"http://{own}{self.path}")
        elif host != own:
            self._refuse(421, f"This wallet answers only at http://{own}/.")
        elif urlsplit(self.path).path == "/":
            self._send_on(f"http://{own}/{START_PAGE}")
        else:
            found = resolve(self.server.wallet_root, self.path)
            if found is None:
                self._refuse(404, "Not found.")
            else:
                self._send(200, found.read_bytes(), TYPES.get(found.suffix.lower())
                           or mimetypes.guess_type(found.name)[0] or "application/octet-stream", with_body)

    def _send(self, status: int, body: bytes, content_type: str, with_body: bool = True) -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        # As the phone's shell and the wallet's development server send them.
        self.send_header("Cross-Origin-Opener-Policy", "same-origin")
        self.send_header("Cross-Origin-Embedder-Policy", "require-corp")
        self.end_headers()
        if with_body:
            self.wfile.write(body)

    def _refuse(self, status: int, message: str) -> None:
        self._send(status, (message + "\n").encode(), "text/plain; charset=utf-8")

    def _send_on(self, location: str) -> None:
        self.send_response(307)
        self.send_header("Location", location)
        self.send_header("Content-Length", "0")
        self.send_header("Cache-Control", "no-store")
        self.end_headers()

    def log_message(self, format: str, *args) -> None:  # noqa: A002 (the base class's name)
        pass  # quiet: a wallet's requests are nobody's business, and the terminal stays readable


class Server(http.server.ThreadingHTTPServer):
    daemon_threads = True
    # On Windows, address reuse would let a second program listen on the same port.
    allow_reuse_address = sys.platform != "win32"

    def __init__(self, root: Path, port: int):
        self.wallet_root = root.resolve()
        super().__init__((HOST, port), Handler)


def missing(root: Path) -> list[str]:
    return [name for name in SERVED if not (root / name).exists()]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Serve the wallet to a browser on this computer.")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent,
                        help="the unzipped wallet folder (default: the folder this script is in)")
    parser.add_argument("--no-browser", action="store_true", help="do not open the browser")
    args = parser.parse_args(argv)
    root = args.root.resolve()
    absent = missing(root)
    if absent:
        print(f"This folder is not a wallet package: it has no {', '.join(absent)}.", file=sys.stderr)
        return 2
    address = f"http://{HOST}:{PORT}/"
    try:
        server = Server(root, PORT)
    except OSError:
        print(f"Port {PORT} on this computer is in use, perhaps by another copy of this wallet.\n"
              "The wallet keeps its storage under one address, so this script does not pick another port.\n"
              "Close the other program and start again. Until this script says the wallet is running,\n"
              f"do not open {address}: whatever holds the port would be answering there.", file=sys.stderr)
        return 1
    print(f"The wallet is at {address}\nLeave this window open while you use it. Press Ctrl+C to stop.\n"
          "If it opens in a browser other than Chrome or Edge, copy the address into one of those.", flush=True)
    if not args.no_browser:
        webbrowser.open(address)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 4: Run the tests**

```bash
python3 -m unittest python/test_start_wallet.py
npm run build:runtime
npx playwright test -c playwright.local.config.ts --trace off playwright/fortweb-desktop-start.spec.ts
```

Expected: `Ran 13 tests`, `OK` (the sentences the script prints for a folder that is not a package and for a taken port appear in the output); `2 passed`.

- [ ] **Step 5: Run the whole check once more, since three tasks have added to it**

```bash
python3 -m unittest python.test_sourcing python.test_roling python.test_vaulting python.test_transporting python.test_onboarding python.test_verify_runtime_tree python.test_start_wallet
npm run typecheck
npm run test:e2e:prepared-runtime -- -c playwright.local.config.ts --trace off 2>&1 | tee /tmp/credentials-task4-browser.log | tail -15
npm run test:runtime-publication-workflow
```

Expected: `Ran 245 tests`, `OK`; no type error; `146 passed`, none failed, no `Slow test file`; 15 passing.

- [ ] **Step 6: Commit and push**

```bash
git add scripts/start_wallet.py python/test_start_wallet.py .github/actions/produce-runtime-package/action.yml
git commit -m "feat(desktop): a start script serves a wallet package to a browser on the same computer (<bead>)

One address, http://127.0.0.1:8791/, this computer only, the package's own
files only."
git add playwright/fortweb-desktop-start.spec.ts package.json
git commit -m "test(desktop): the wallet opens from the start script and keeps a wallet made there (<bead>)"
git push origin credentials
```

The fork's workflow must end green for the pushed commit. Put the run's number and the full SHA of the branch head in the report: Task 6 pins that SHA.

---

### Task 5: Fortoid before the pin: a checked copy of the runtime source, and a start-up time to compare with

**Repository:** `/home/user/fortoid`

**Files:**
- Create: `config/runtime-source-mirror.json`
- Create: `tools/list-runtime-source-mirrors.mjs`
- Create: `tools/list-runtime-source-mirrors.test.mjs`
- Create: `.github/workflows/runtime-source-mirror.yml`
- Modify: `.github/workflows/android-runtime-contract.yml` (one step)
- Modify: `.github/workflows/android-startup-proof.yml` (one step)

**Interfaces:**
- Consumes: nothing from Tasks 1 to 4.
- Produces:
  - The release `runtime-source-sedi-core-csp-20261107` of `seriouscoderone/fortoid` with the file `runtime-source.tar.gz`, byte for byte the Foundation's archive. Its address, which Task 6 pins: `https://github.com/seriouscoderone/fortoid/releases/download/runtime-source-sedi-core-csp-20261107/runtime-source.tar.gz`.
  - `node tools/list-runtime-source-mirrors.mjs` prints one line for each listed source: tag, the Foundation's address, SHA-256, separated by tabs. `readMirrors(text)` is its exported check.
  - The start-up time of the wallet that is pinned today, in milliseconds, for Task 6 to compare with.

**Acceptance Criteria:**
- `node --test tools/list-runtime-source-mirrors.test.mjs` passes (5 tests), and the workflow "Android Runtime Contract" runs it.
- The workflow "Runtime source mirror" has run green on `dev`, and run a second time it changes nothing and stays green.
- `curl -sSL <the copy's address> | sha256sum` prints `3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a`.
- A copy whose bytes do not match its digest makes the workflow fail. A copy is never replaced.
- The "Android API 36 Startup Proof" run of the pushed commit is green and its log has a line `STAGE=READY elapsedMs=<n>`. The report gives `<n>`.

- [ ] **Step 1: Write the failing test**

Create `tools/list-runtime-source-mirrors.test.mjs` (`fortoid/list-runtime-source-mirrors.test.mjs` beside this plan):

```js
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import { readMirrors } from './list-runtime-source-mirrors.mjs';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '..');
const SHA = 'a'.repeat(64);
const UPSTREAM = 'https://github.com/keri-foundation/fortweb/releases/download/some-tag/runtime-source.tar.gz';
const list = (...sources) => JSON.stringify({ schema: 'fortoid.runtime-source-mirror.v1', sources });
const source = (change = {}) => ({ tag: 'some-tag', upstream: UPSTREAM, sha256: SHA, ...change });

test('reads a list', () => {
  assert.deepEqual(readMirrors(list(source(), source({ tag: 'other-1.0' }))), [
    { tag: 'some-tag', upstream: UPSTREAM, sha256: SHA },
    { tag: 'other-1.0', upstream: UPSTREAM, sha256: SHA },
  ]);
});

test('refuses a list that is not one', () => {
  for (const text of ['', '[]', 'null', '{}', JSON.stringify({ schema: 'other', sources: [source()] }),
    JSON.stringify({ schema: 'fortoid.runtime-source-mirror.v1', sources: [] }),
    JSON.stringify({ schema: 'fortoid.runtime-source-mirror.v1', sources: [source()], more: 1 })]) {
    assert.throws(() => readMirrors(text), /mirror list/, text);
  }
});

test('refuses a source that is not exact', () => {
  const bad = [
    [{ ...source(), note: 'x' }, /exactly tag, upstream and sha256/],
    [{ tag: 'some-tag', upstream: UPSTREAM }, /exactly tag, upstream and sha256/],
    [source({ tag: 'Some-Tag' }), /tag must be/],
    [source({ tag: 'a/b' }), /tag must be/],
    [source({ tag: '' }), /tag must be/],
    [source({ sha256: SHA.toUpperCase() }), /sha256 must be/],
    [source({ sha256: SHA.slice(1) }), /sha256 must be/],
    [source({ upstream: 'http://github.com/o/r/releases/download/t/runtime-source.tar.gz' }), /upstream must be/],
    [source({ upstream: 'https://example.test/o/r/releases/download/t/runtime-source.tar.gz' }), /upstream must be/],
    [source({ upstream: 'https://user:secret@github.com/o/r/releases/download/t/runtime-source.tar.gz' }), /upstream must be/],
    [source({ upstream: 'https://github.com/o/r/releases/download/t/runtime-source.tar.gz?x=1' }), /upstream must be/],
    [source({ upstream: 'https://github.com/o/r/releases/download/t/other.tar.gz' }), /upstream must be/],
    [source({ upstream: 'not an address' }), /not an address/],
    [source({ upstream: 7 }), /not an address/],
  ];
  for (const [entry, message] of bad) {
    assert.throws(() => readMirrors(list(entry)), message, JSON.stringify(entry));
  }
});

test('refuses a tag listed twice', () => {
  assert.throws(() => readMirrors(list(source(), source())), /listed twice/);
});

test('the committed list reads, and the program prints it', () => {
  const sources = readMirrors(readFileSync(path.join(ROOT, 'config', 'runtime-source-mirror.json'), 'utf8'));
  const printed = execFileSync(process.execPath, [path.join(ROOT, 'tools', 'list-runtime-source-mirrors.mjs')], { encoding: 'utf8' });
  assert.equal(printed, sources.map((item) => `${item.tag}\t${item.upstream}\t${item.sha256}\n`).join(''));
});
```

```bash
cd /home/user/fortoid
node --test tools/list-runtime-source-mirrors.test.mjs
```

Expected: it fails to import `./list-runtime-source-mirrors.mjs`.

- [ ] **Step 2: Write the list and the tool**

Create `config/runtime-source-mirror.json` (`fortoid/runtime-source-mirror.json` beside this plan):

```json
{
  "schema": "fortoid.runtime-source-mirror.v1",
  "sources": [
    {
      "tag": "runtime-source-sedi-core-csp-20261107",
      "upstream": "https://github.com/keri-foundation/fortweb/releases/download/runtime-source-sedi-core-csp-20261107/runtime-source.tar.gz",
      "sha256": "3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a"
    }
  ]
}
```

Create `tools/list-runtime-source-mirrors.mjs` (`fortoid/list-runtime-source-mirrors.mjs` beside this plan):

```js
#!/usr/bin/env node
// The runtime sources this repository keeps a checked copy of (config/runtime-source-mirror.json).
//
// A runtime source is an archive the wallet is built from. The Foundation publishes it as a download attached to
// one of its releases, and such a download can be removed. The workflow runtime-source-mirror.yml copies each listed
// archive to a release of this repository, after checking it against the digest given here, and the builds read
// that copy.
//
// Run as a program it prints one line for each source: tag, upstream address, SHA-256, separated by tabs.
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const SCHEMA = 'fortoid.runtime-source-mirror.v1';
const ARCHIVE = 'runtime-source.tar.gz';
const TAG = /^[a-z0-9][a-z0-9.-]{0,99}$/;
const SHA256 = /^[0-9a-f]{64}$/;

function plain(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function sameKeys(value, keys) {
  return JSON.stringify(Object.keys(value).sort()) === JSON.stringify([...keys].sort());
}

function upstreamAddress(value, index) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new Error(`source ${index}: upstream is not an address`);
  }
  if (url.protocol !== 'https:' || url.hostname !== 'github.com' || url.port || url.username || url.password
      || url.search || url.hash || url.href !== value || !url.pathname.endsWith(`/${ARCHIVE}`)) {
    throw new Error(`source ${index}: upstream must be a plain https://github.com/ address of ${ARCHIVE}`);
  }
  return value;
}

/** The list in `text`, checked. Throws an Error that says what is wrong. */
export function readMirrors(text) {
  let config;
  try {
    config = JSON.parse(text);
  } catch {
    throw new Error('the mirror list is not JSON');
  }
  if (!plain(config) || !sameKeys(config, ['schema', 'sources']) || config.schema !== SCHEMA
      || !Array.isArray(config.sources) || config.sources.length === 0) {
    throw new Error(`the mirror list must be {"schema": "${SCHEMA}", "sources": [...]} with at least one source`);
  }
  const tags = new Set();
  return config.sources.map((source, index) => {
    if (!plain(source) || !sameKeys(source, ['tag', 'upstream', 'sha256'])) {
      throw new Error(`source ${index}: must have exactly tag, upstream and sha256`);
    }
    if (typeof source.tag !== 'string' || !TAG.test(source.tag)) {
      throw new Error(`source ${index}: tag must be lower-case letters, digits, dots and hyphens`);
    }
    if (tags.has(source.tag)) {
      throw new Error(`source ${index}: tag ${source.tag} is listed twice`);
    }
    tags.add(source.tag);
    if (typeof source.sha256 !== 'string' || !SHA256.test(source.sha256)) {
      throw new Error(`source ${index}: sha256 must be 64 lower-case hex digits`);
    }
    if (typeof source.upstream !== 'string') {
      throw new Error(`source ${index}: upstream is not an address`);
    }
    return { tag: source.tag, upstream: upstreamAddress(source.upstream, index), sha256: source.sha256 };
  });
}

const here = fileURLToPath(import.meta.url);
if (process.argv[1] && path.resolve(process.argv[1]) === here) {
  const file = path.join(path.dirname(here), '..', 'config', 'runtime-source-mirror.json');
  try {
    for (const source of readMirrors(readFileSync(file, 'utf8'))) {
      process.stdout.write(`${source.tag}\t${source.upstream}\t${source.sha256}\n`);
    }
  } catch (error) {
    process.stderr.write(`error: ${error.message}\n`);
    process.exit(1);
  }
}
```

```bash
node --test tools/list-runtime-source-mirrors.test.mjs
node tools/list-runtime-source-mirrors.mjs
```

Expected: 5 passing; one line with the tag, the Foundation's address and the digest, separated by tabs.

- [ ] **Step 3: Run the test in CI**

In `.github/workflows/android-runtime-contract.yml`, in the job `contract-tests`, after the step "Run runtime-requirements compatibility tests", add:

```yaml
      - name: Run runtime source mirror list tests
        run: node --test tools/list-runtime-source-mirrors.test.mjs
```

- [ ] **Step 4: Write the workflow**

Create `.github/workflows/runtime-source-mirror.yml` (`fortoid/runtime-source-mirror.yml` beside this plan):

```yaml
name: Runtime source mirror

# Keeps a checked copy of each runtime source the wallet is built from, as a
# release of this repository. The Foundation publishes a runtime source as a
# download attached to one of its releases, and such a download can be removed.
# The list is config/runtime-source-mirror.json. A copy is made once, from the
# Foundation's address, and only when its bytes match the digest in the list.
# A copy that exists is checked against the digest and never replaced. Every
# build checks the archive against the same digest again when it reads it
# (scripts/build-canonical-fortweb-package.sh).

permissions:
  contents: write

on:
  push:
    branches:
      - dev
    paths:
      - config/runtime-source-mirror.json
      - tools/list-runtime-source-mirrors.mjs
      - .github/workflows/runtime-source-mirror.yml
  workflow_dispatch:

concurrency:
  group: runtime-source-mirror
  cancel-in-progress: false

jobs:
  mirror:
    name: Copy and check the runtime sources
    runs-on: ubuntu-latest
    timeout-minutes: 15

    steps:
      - name: Check out Fortoid
        uses: actions/checkout@v4

      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: '22'

      - name: Copy and check
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          set -euo pipefail
          node tools/list-runtime-source-mirrors.mjs > "${RUNNER_TEMP}/mirrors.tsv"
          matches() { echo "$1  $2" | sha256sum --check --status; }
          while IFS=$'\t' read -r TAG UPSTREAM SHA256 <&3; do
            WORK="$(mktemp -d)"
            COPY="${WORK}/runtime-source.tar.gz"
            if gh release view "$TAG" > /dev/null 2>&1; then
              if gh release download "$TAG" --pattern runtime-source.tar.gz --dir "$WORK" 2> /dev/null; then
                if ! matches "$SHA256" "$COPY"; then
                  echo "::error::the copy under release ${TAG} does not match its digest"
                  exit 1
                fi
                echo "copy present and correct: ${TAG}"
                continue
              fi
              MAKE=upload
            else
              MAKE=create
            fi
            curl --fail --location --silent --show-error --output "$COPY" "$UPSTREAM"
            if ! matches "$SHA256" "$COPY"; then
              echo "::error::${UPSTREAM} does not match its digest"
              exit 1
            fi
            if [ "$MAKE" = create ]; then
              gh release create "$TAG" "$COPY" \
                --prerelease \
                --target "$GITHUB_SHA" \
                --title "Runtime source copy: ${TAG}" \
                --notes "A copy of the runtime source the wallet is built from, taken from ${UPSTREAM} and checked against SHA-256 ${SHA256}. The builds of this repository read this copy."
            else
              gh release upload "$TAG" "$COPY"
            fi
            echo "copy made: ${TAG}"
          done 3< "${RUNNER_TEMP}/mirrors.tsv"
```

The copy is checked here against the archive's digest. Every build checks the archive and the manifest inside it again when it reads the copy (`scripts/acquire_runtime_source.py` in the wallet's repository), so a wrong copy cannot enter a build.

- [ ] **Step 5: Have the start-up proof say its time**

The proof logs `STAGE=READY elapsedMs=<n>`: its own time from launch to a wallet that answered a request. Today the workflow prints that line only when the proof fails. In `.github/workflows/android-startup-proof.yml`, after the step "Verify startup JUnit XML" and before "Capture startup diagnostics on failure", add:

```yaml
      # The proof's own time from launch to a wallet that answered a request,
      # so a change of wallet can be compared with the one before it.
      - name: Report the start-up time
        if: always() && steps.runproof.outcome != 'skipped'
        run: |
          LINES="$(adb logcat -d -s MainActivityStartupProof:* 2>/dev/null | grep -E "STAGE=READY|LIVE_EVENT" | tail -4 || true)"
          echo "${LINES:-(the proof logged no READY line)}"
```

- [ ] **Step 6: Commit, push once, and check**

```bash
git add config/runtime-source-mirror.json tools/list-runtime-source-mirrors.mjs tools/list-runtime-source-mirrors.test.mjs
git commit -m "feat(mirror): the list of runtime sources this repository keeps a copy of (<bead>)"
git add .github/workflows/android-runtime-contract.yml
git commit -m "ci: test the runtime source mirror list (<bead>)"
git add .github/workflows/runtime-source-mirror.yml
git commit -m "ci: keep a checked copy of each listed runtime source as a release (<bead>)"
git add .github/workflows/android-startup-proof.yml
git commit -m "ci(startup): the proof's run prints its start-up time (<bead>)"
git push origin HEAD:ccr-0427da9b-gq0ryv HEAD:dev
```

Wait for every workflow of this push to end; all must be green. Then:

```bash
curl -sSL https://github.com/seriouscoderone/fortoid/releases/download/runtime-source-sedi-core-csp-20261107/runtime-source.tar.gz | sha256sum
```

Expected: `3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a  -`.

From the log of the "Android API 36 Startup Proof" run, step "Report the start-up time", take the number after `elapsedMs=`. This is the time of the wallet pinned today.

- [ ] **Step 7: Run the copying workflow a second time**

Run the "Runtime source mirror" run of Step 6 again ("Re-run all jobs"; with the GitHub tools, `rerun_workflow_run` with that run's number). GitHub starts a workflow by hand only when its file is on the default branch, and this one is on `dev` only; running a run again needs no such thing. It must end green, and its log must say `copy present and correct: runtime-source-sedi-core-csp-20261107`.

---

### Task 6: Fortoid pins the `credentials` wallet

**Repository:** `/home/user/fortoid`

**Files:**
- Modify: `scripts/test-sync-contract.sh:262-267`
- Modify: `scripts/build-canonical-fortweb-package.sh:35-39`
- Modify: `config/fortweb-runtime.json`
- Modify: `CLAUDE.md` (the section "Working on the wallet (FortWeb fork)")

**Interfaces:**
- Consumes: the full SHA of the head of fortweb `credentials` after Task 4, written `<WALLET_SHA>` below; the copy and the start-up time from Task 5.
- Produces: `dev` of Fortoid builds the APK from `<WALLET_SHA>` with the draft's runtime source. Task 7 builds on this pin.

**Acceptance Criteria:**
- `config/fortweb-runtime.json` names `<WALLET_SHA>`.
- The reviewed runtime-source pin reads the repository's own copy and carries both of the draft's digests.
- `bash scripts/test-sync-contract.sh` ends with `80 passed, 0 failed`, one check more than before: the pinned source is in the list of copies with the same digest.
- Locally: the package builds from a clean checkout at `<WALLET_SHA>`, imports, and `./gradlew :app:assembleDebug :app:testDebugUnitTest verifyPreparedPayload :app:lintDebug` succeeds.
- `dev` moves to the pin only after the nine workflows that publish nothing were green for the same commits on the session branch: Android JVM, Android API 36 Persistence Proof, Android API 36 Pyodide Worker Lifecycle Proof, Android API 36 Pyodide Worker Proof, Android Runtime Contract, Android API 36 Service Worker Prohibition Proof, Android API 36 Startup Proof, Android API 36 Worker Proof, Docs build. So `dev` and the published demo never carry a wallet that has not started on Android.
- On `dev`, every workflow of the pin's push is green: those nine and Demo APK.
- The report gives the Startup Proof's `elapsedMs` beside the number from Task 5, and says so plainly if the new number is above 25,000.

- [ ] **Step 1: Write the failing check**

```bash
cd /home/user/fortoid
git apply --include='scripts/test-sync-contract.sh' .internal/plans/2026-11-14-credentials-base/fortoid/task6-pin.diff
bash scripts/test-sync-contract.sh 2>&1 | tail -1
```

Expected: `=== Results: 77 passed, 3 failed ===`. The three are the address and the two digests; the new check of the list of copies passes already.

The whole change of this step and the next is:

```diff
diff --git a/scripts/build-canonical-fortweb-package.sh b/scripts/build-canonical-fortweb-package.sh
index 4f91f22..8467d2d 100644
--- a/scripts/build-canonical-fortweb-package.sh
+++ b/scripts/build-canonical-fortweb-package.sh
@@ -34,9 +34,11 @@ LOCK_FILE="${REPO_ROOT}/config/fortweb-runtime.json"
 
 # Reviewed runtime-source pin. These values are the reviewed contract and must not
 # be changed here; they must agree with the FortWeb producer's declared pin.
-SOURCE_URL="https://github.com/keri-foundation/fortweb/releases/download/runtime-source-pyodide-314-hio-0.7.20-20260910/runtime-source.tar.gz"
-SOURCE_ARCHIVE_SHA256="394db70ecbed6e5718e6ec0afbcb43eaf6384e2ab78218e55067ec9e5d83fb34"
-SOURCE_MANIFEST_SHA256="371ef8cb8c6641b68afe6799931678435e2a629dcf4aa7ba2e344efab5a10f32"
+# The address is this repository's own checked copy of the Foundation's archive
+# (config/runtime-source-mirror.json, .github/workflows/runtime-source-mirror.yml).
+SOURCE_URL="https://github.com/seriouscoderone/fortoid/releases/download/runtime-source-sedi-core-csp-20261107/runtime-source.tar.gz"
+SOURCE_ARCHIVE_SHA256="3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a"
+SOURCE_MANIFEST_SHA256="f76cf23f5cdaa5fe3f7a0e264c862290f9c8b6be2aafca6622926e1869731431"
 SOURCE_MANIFEST_REL="build/runtime-source/manifest.json"
 PACKAGE_REF="refs/heads/pyodide-314-runtime"
 
diff --git a/scripts/test-sync-contract.sh b/scripts/test-sync-contract.sh
index 824e560..319434e 100755
--- a/scripts/test-sync-contract.sh
+++ b/scripts/test-sync-contract.sh
@@ -259,12 +259,16 @@ BUILDER_PLAN="$(bash "${BUILDER_SCRIPT}" --fortweb-dir /nonexistent-anywhere --p
 
 expect_contains "plan runs npm ci" "npm ci" "${BUILDER_PLAN}"
 expect_contains "plan acquires the reviewed runtime source" "acquire_runtime_source.py" "${BUILDER_PLAN}"
-expect_contains "plan pins the reviewed source URL" \
-  "runtime-source-pyodide-314-hio-0.7.20-20260910/runtime-source.tar.gz" "${BUILDER_PLAN}"
+expect_contains "plan pins this repository's copy of the reviewed source" \
+  "https://github.com/seriouscoderone/fortoid/releases/download/runtime-source-sedi-core-csp-20261107/runtime-source.tar.gz" "${BUILDER_PLAN}"
 expect_contains "plan pins the archive digest" \
-  "394db70ecbed6e5718e6ec0afbcb43eaf6384e2ab78218e55067ec9e5d83fb34" "${BUILDER_PLAN}"
+  "3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a" "${BUILDER_PLAN}"
 expect_contains "plan pins the manifest digest" \
-  "371ef8cb8c6641b68afe6799931678435e2a629dcf4aa7ba2e344efab5a10f32" "${BUILDER_PLAN}"
+  "f76cf23f5cdaa5fe3f7a0e264c862290f9c8b6be2aafca6622926e1869731431" "${BUILDER_PLAN}"
+# The pinned address must be a copy this repository keeps, with the same digest in the list of copies.
+MIRROR_LINE="$(node "${REPO_ROOT}/tools/list-runtime-source-mirrors.mjs" | grep -F "runtime-source-sedi-core-csp-20261107" || true)"
+expect_contains "the pinned source is in the list of copies, with the same digest" \
+  "3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a" "${MIRROR_LINE}"
 expect_contains "plan builds the runtime" "npm run build:runtime" "${BUILDER_PLAN}"
 expect_contains "plan packages the runtime" "npm run package:runtime" "${BUILDER_PLAN}"
 expect_contains "plan passes --runtime-dir" "--runtime-dir dist/runtime" "${BUILDER_PLAN}"
```

- [ ] **Step 2: Move the reviewed runtime-source pin**

```bash
git apply --include='scripts/build-canonical-fortweb-package.sh' .internal/plans/2026-11-14-credentials-base/fortoid/task6-pin.diff
bash scripts/test-sync-contract.sh 2>&1 | tail -1
```

Expected: `=== Results: 80 passed, 0 failed ===`.

- [ ] **Step 3: Move the wallet pin**

In `config/fortweb-runtime.json`, set `"commit"` to `<WALLET_SHA>`. Nothing else in the file changes.

- [ ] **Step 4: Verify locally from a clean checkout**

```bash
export PATH=~/fwvenv/bin:$PATH
git -C /home/user/fortweb-pkg fetch origin credentials
git -C /home/user/fortweb-pkg checkout --detach <WALLET_SHA>
git -C /home/user/fortweb-pkg status --short        # expected: nothing
rm -rf /home/user/fortweb-pkg/build /home/user/fortweb-pkg/dist
OUT=/tmp/credentials-pkg-$$
bash scripts/build-canonical-fortweb-package.sh --fortweb-dir /home/user/fortweb-pkg --output-dir "$OUT" 2>&1 | tail -4
ZIP="$(find "$OUT" -maxdepth 1 -name '*.zip' -type f -print -quit)"
node tools/import-fortweb-runtime-package.mjs --expected-fortweb-commit <WALLET_SHA> "$ZIP" | tail -2
./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest verifyPreparedPayload :app:lintDebug 2>&1 | tail -4
node tools/validate-staged-payload.mjs | tail -1
grep -c fort_synthetic_recipient_testing app/src/main/assets/payload/pyscript-ci.toml
```

If `/home/user/fortweb-pkg` does not exist, make it with `git clone https://github.com/seriouscoderone/fortweb /home/user/fortweb-pkg`.

Expected: one `PACKAGE_PATH=` line; `staged 174 files`; `BUILD SUCCESSFUL`; `PASS: staged payload validated`; `1`. The package build reads the runtime source from the copy Task 5 made.

- [ ] **Step 5: Update the notes for the next session**

In `CLAUDE.md`, section "Working on the wallet (FortWeb fork)":

1. After the first paragraph (it ends "Upstream PRs (e.g. #49) branch from `upstream/main`."), add this paragraph:

```markdown
Its `credentials` = `dev` + the Foundation's draft credential engine
(`keri-foundation/fortweb#42`, merged at `2db99e8`) + the work that needs it.
**Fortoid pins `credentials`**, and new wallet work goes there until the
Foundation merges the draft; then `dev` takes `upstream/main` and `credentials`
is merged into it.
```

2. In the build block, the three pinned values become the draft's: the address after `--url` becomes `https://github.com/seriouscoderone/fortoid/releases/download/runtime-source-sedi-core-csp-20261107/runtime-source.tar.gz`, the value after `--sha256` becomes `3618b16ee66aca5704d76310b792383f0a32260c2e774fd5e42f8b62e5f7dd9a`, and the manifest digest becomes `f76cf23f5cdaa5fe3f7a0e264c862290f9c8b6be2aafca6622926e1869731431` in both places that name it. After the block, add: "The address is this repository's own checked copy of the Foundation's archive `runtime-source-sedi-core-csp-20261107` (`config/runtime-source-mirror.json`)."

3. In the paragraph that begins "Tests: Playwright must use the container's Chromium", after "`npm run test:runtime-publication-workflow`." add: "After a build, also `python3 -m unittest python/test_verify_runtime_tree.py python/test_start_wallet.py`. The workflow's canary (`playwright/runtime-canary.spec.ts`) passes only with the headless shell, the browser the workflow installs (in this container under `/opt/pw-browsers/chromium_headless_shell-*`): the full Chromium also asks for `/favicon.ico`, the canary's server answers 404, and the test counts that as an error."

4. Replace the paragraph that begins "Credentials: the Foundation's draft" with:

```markdown
Credentials: the wallet runs on the Foundation's draft engine, merged into
`credentials`. What that changes: the wallet's storage is named `@fortweb-v2/`
and vaults made by earlier builds are not shown; one engine at a time may hold
the storage (a second gets `BUSY`), so a test that needs a second engine has
the page lock its vault first; the engine writes to storage after every
request; after a request that failed with unsaved changes the vault refuses
everything until it is closed. Receiving, granting and admitting run because
`pyscript-ci.toml` carries `fort_synthetic_recipient_testing = true`, and each
call must pass `syntheticOnly: true`. The draft's own credential tests
(`playwright/registry-*.spec.ts`, `playwright/sedi-*.spec.ts`) are run by no
workflow; one of them, "recipient API is disabled in the ordinary runtime",
fails here by design. Before changing the draft's files, read
`.internal/specs/2026-11-14-standard-credential-screens-design.md`.

When the draft moves: if the Foundation added commits to it, merge
`upstream/pr42` into `credentials` again. If it rewrote its branch, start a
new branch from `dev`, merge the new head the way Task 1 of
`.internal/plans/2026-11-14-credentials-base.md` did, and cherry-pick this
branch's own commits onto it.
```

5. After the paragraph on Pyodide proxies (it ends "the roles time-limit tests (`playwright/fortweb-roles-time.spec.ts`) do."), add:

```markdown
On a computer the wallet is served by fortweb `scripts/start_wallet.py` at
`http://127.0.0.1:8791/` (never 8765, the development server's port: a browser
keeps one wallet per address). Fortoid's Demo APK workflow publishes it with
the built wallet as `fort-wallet-desktop.zip`. Its browser test
(`playwright/fortweb-desktop-start.spec.ts`) runs against an unzipped download
when `FORT_WALLET_FOLDER` names the folder.
```

6. In the paragraph that begins "Ship a FortWeb change to the APK", "push fortweb `dev`" becomes "push fortweb `credentials`".

- [ ] **Step 6: Commit, and prove the pin on the session branch first**

```bash
git add scripts/test-sync-contract.sh scripts/build-canonical-fortweb-package.sh
git commit -m "build: the reviewed runtime source is the draft's, read from this repository's copy (<bead>)"
git add config/fortweb-runtime.json
git commit -m "chore(pin): the wallet is fortweb credentials <first seven characters of WALLET_SHA> (<bead>)"
git add CLAUDE.md
git commit -m "docs(agents): the credentials branch, its runtime source, and what the draft engine changes (<bead>)"
git push origin HEAD:ccr-0427da9b-gq0ryv
```

This one time the session branch goes ahead of `dev`, for the length of one proof run. A push to `dev` publishes `demo-latest` whatever the proofs say, and this is the first time the merged wallet runs on Android.

Start each of the nine workflows named in the acceptance criteria by hand on the branch `ccr-0427da9b-gq0ryv` (`workflow_dispatch`). Do not start Demo APK. Wait until all nine have ended.

- [ ] **Step 7: If a proof fails**

Three things the draft changed can show here, and each is a finding to report, not something to work around:

- the worker cannot take a Web Lock (`UNSUPPORTED`, "Exclusive browser storage locking is unavailable.");
- a worker started while an earlier one still holds the lock (`BUSY`, "This wallet is already open in another browser worker.");
- start-up passes the wallet's 30-second limit because the engine now writes to storage after every request.

Do not disable, skip or loosen a proof, and do not push to `dev`. Use the systematic-debugging skill, read the failing job's log for the wallet's own lines (`[fortweb.runtime] event=…`), find the cause, and report it with the evidence. `dev` and the published demo stay on the wallet that works while the controller decides what follows.

- [ ] **Step 8: Move `dev`, and wait for every workflow**

When all nine are green on the session branch:

```bash
git push origin HEAD:dev
```

The session branch and `dev` are identical again. Do not push again until every workflow of this push has ended: the nine and Demo APK, which publishes the release.

- [ ] **Step 9: Report**

For each workflow, on the session branch and on `dev`: the run's number and its result. From the Startup Proof's step "Report the start-up time", both runs: the number after `elapsedMs=`, beside the number from Task 5. If a new number is above 25,000, say so: the wallet's limit is 30,000, and little room is left.

**Going back.** If the pin has to be taken back after `dev` has moved, revert the two pin commits (the reviewed runtime source and the wallet) with `git revert`, push both branches together, and wait for every workflow. The earlier wallet comes back with its vault: the new engine keeps its storage under another name and never touches the earlier one. A wallet made with the new engine is then not shown, for the same reason, and shows again when the pin returns.

---

### Task 7: The wallet for a computer on the Releases page

**Repository:** `/home/user/fortoid`

**Files:**
- Create: `tools/build_desktop_wallet.py`
- Create: `tools/test_build_desktop_wallet.py`
- Create: `desktop/README.txt`
- Modify: `.github/workflows/android-runtime-contract.yml` (one step)
- Modify: `.github/workflows/demo-apk.yml`
- Modify: `README.md` (the sections "Install on a phone" and "Wallet features (FortWeb fork)")

**Interfaces:**
- Consumes: the imported payload `app/src/main/assets/payload/`; `scripts/start_wallet.py` of the pinned wallet, which the workflow's prepare step checks out at `.deps/fortweb/scripts/start_wallet.py`; the wallet's browser test and its `FORT_WALLET_FOLDER` (Task 4).
- Produces: the file `fort-wallet-desktop.zip` on the `demo-latest` release, holding one folder `fort-wallet/` with the wallet package, `start-wallet.py` and `README.txt`. `python3 tools/build_desktop_wallet.py --payload DIR --start-script FILE --readme FILE --output ZIP` prints `DESKTOP_WALLET_SHA256=<digest>`.

**Acceptance Criteria:**
- `python3 -m unittest tools/test_build_desktop_wallet.py` passes (5 tests), and the workflow "Android Runtime Contract" runs it.
- The zip holds the five package entries, `start-wallet.py` and `README.txt`, and none of the importer's own files (`manifest.json`, `checksums.sha256`, `contracts/`).
- Built twice from the same files, the zip is the same byte for byte.
- Unzipped, the folder's own `start-wallet.py` serves the wallet, and the wallet's browser test passes against that folder.
- `demo-latest` carries `fortoid-demo.apk` and `fort-wallet-desktop.zip`, built from one wallet commit, and its notes give the zip's SHA-256 and how to start it.
- When the wallet payload could not be built, the release still carries the APK, and its notes say that there is no wallet for the computer in this build.

- [ ] **Step 1: Write the failing test**

Create `tools/test_build_desktop_wallet.py` (`fortoid/test_build_desktop_wallet.py` beside this plan):

```python
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

    def test_the_program_prints_the_digest_and_fails_plainly(self):
        self.assertEqual(tool.main(["--payload", str(self.payload), "--start-script", str(self.script),
                                    "--readme", str(self.readme), "--output", str(self.output)]), 0)
        self.assertEqual(tool.main(["--payload", str(self.payload / "missing"), "--start-script", str(self.script),
                                    "--readme", str(self.readme), "--output", str(self.output)]), 1)


if __name__ == "__main__":
    unittest.main()
```

```bash
cd /home/user/fortoid
python3 -m unittest tools/test_build_desktop_wallet.py
```

Expected: an error on import, `FileNotFoundError` for `tools/build_desktop_wallet.py`.

- [ ] **Step 2: Write the tool and the note**

Create `tools/build_desktop_wallet.py` (`fortoid/build_desktop_wallet.py` beside this plan):

```python
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
        files = [top] if top.is_file() else sorted(path for path in top.rglob("*") if not path.is_dir())
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
```

Create `desktop/README.txt` (`fortoid/desktop-README.txt` beside this plan):

```text
The wallet for a computer
=========================

This is the same wallet that is in the phone app, for a desktop browser.
It is a demonstration build. Do not use it for anything you depend on.

To start it
-----------

1. Open a terminal in this folder.
2. Run:    python3 start-wallet.py
   (on Windows:    py start-wallet.py)
3. The wallet opens in your browser at http://127.0.0.1:8791/

Leave the terminal open while you use the wallet. Press Ctrl+C in it to stop.
You need Python 3 and nothing else.

Good to know
------------

- Use Chrome or Edge. If the wallet opens in another browser, copy the address
  into Chrome or Edge. Other browsers are not tested, and Safari removes a
  site's storage after a week without use, which would remove your vault.
- Use one tab. A second tab is told that the wallet is already open.
- Use a browser profile with no extensions. An extension that may read pages
  can read a wallet.
- The wallet lives at one address: http://127.0.0.1:8791/. Your vault is kept
  by the browser under that address. If something else on this computer is
  using port 8791 the script says so and stops. It does not pick another port,
  because another address would be an empty wallet. Do not open the address
  while the script is not running: whatever holds the port would answer there.
- The browser decides whether it keeps the wallet's storage for good. Chrome
  and Edge usually say no to a site they have just met. The wallet asks again
  each time you open your vault. Settings shows the answer as
  "Kept by the Browser".
- Set the computer's clock to automatic time.
- Use the phone app and this download from the same release, so both are the
  same version.
- The start script sends nothing anywhere. It serves this folder, and only to
  this computer.
```

```bash
python3 -m unittest tools/test_build_desktop_wallet.py
```

Expected: `Ran 5 tests`, `OK` (the test of a missing payload prints one line, `error: the payload has no app`).

- [ ] **Step 3: Build the real zip and open the wallet from it**

The payload imported in Task 6 must still be in place (`ls app/src/main/assets/payload/app` lists the wallet; if not, repeat Task 6, Step 4). `/home/user/fortweb` must be on `credentials` at `<WALLET_SHA>` with `dist/runtime` built.

```bash
python3 tools/build_desktop_wallet.py --payload app/src/main/assets/payload \
  --start-script /home/user/fortweb/scripts/start_wallet.py --readme desktop/README.txt \
  --output /tmp/fort-wallet-desktop.zip
rm -rf /tmp/fort-wallet-unzipped && mkdir /tmp/fort-wallet-unzipped
unzip -q /tmp/fort-wallet-desktop.zip -d /tmp/fort-wallet-unzipped
ls /tmp/fort-wallet-unzipped/fort-wallet
cd /home/user/fortweb
FORT_WALLET_FOLDER=/tmp/fort-wallet-unzipped/fort-wallet \
  npx playwright test -c playwright.local.config.ts --trace off playwright/fortweb-desktop-start.spec.ts
```

Expected: a line `DESKTOP_WALLET_SHA256=…`; the folder holds `README.txt app pyscript-ci.toml runtime-closure.json start-wallet.py vendor wheels`; `2 passed`.

- [ ] **Step 4: Run the tool's test in CI**

In `.github/workflows/android-runtime-contract.yml`, in the job `contract-tests`, after the step added in Task 5, add:

```yaml
      - name: Run desktop wallet builder tests
        run: python3 -m unittest tools/test_build_desktop_wallet.py
```

- [ ] **Step 5: Publish the zip with the APK**

```bash
cd /home/user/fortoid
git apply .internal/plans/2026-11-14-credentials-base/fortoid/task7-demo-apk.diff
```

It is this change:

```diff
diff --git a/.github/workflows/demo-apk.yml b/.github/workflows/demo-apk.yml
index 1fee74e..164d7a9 100644
--- a/.github/workflows/demo-apk.yml
+++ b/.github/workflows/demo-apk.yml
@@ -3,6 +3,8 @@ name: Demo APK
 # Builds an installable debug APK (FortWeb wallet + Fort Demo launcher) on every
 # push to dev and publishes it to the rolling "demo-latest" pre-release, so it
 # can be downloaded straight onto a phone from the repository's Releases page.
+# The same release carries the wallet for a desktop browser, built from the same
+# wallet payload.
 
 permissions:
   contents: write
@@ -53,27 +55,55 @@ jobs:
           fi
           cp app/build/outputs/apk/debug/app-debug.apk fortoid-demo.apk
 
+      # The wallet for a computer is the payload the APK carries, with the pinned
+      # wallet's own start script. No payload, no wallet for the computer.
+      - name: Build the wallet for a computer
+        id: desktop
+        if: steps.payload.outcome == 'success'
+        run: |
+          set -euo pipefail
+          OUT="$(python3 tools/build_desktop_wallet.py \
+            --payload app/src/main/assets/payload \
+            --start-script .deps/fortweb/scripts/start_wallet.py \
+            --readme desktop/README.txt \
+            --output fort-wallet-desktop.zip)"
+          echo "$OUT"
+          echo "sha256=${OUT#DESKTOP_WALLET_SHA256=}" >> "$GITHUB_OUTPUT"
+
       - name: Publish demo-latest pre-release
         env:
           GH_TOKEN: ${{ github.token }}
           PAYLOAD: ${{ steps.payload.outcome }}
+          DESKTOP_SHA256: ${{ steps.desktop.outputs.sha256 }}
         run: |
           SHORT_SHA="${GITHUB_SHA::7}"
+          ASSETS=(fortoid-demo.apk)
           if [ "$PAYLOAD" = "success" ]; then
-            WALLET="Includes the FortWeb wallet (KERI Wallet icon)."
+            WALLET="Includes the FortWeb wallet (KERI Wallet icon). A wallet made with a build from before the credential engine is not shown by this one: the engine keeps its storage in a new place."
           else
             WALLET="FortWeb wallet build failed for this commit; only the Fort Demo icon works."
           fi
+          DESKTOP="There is no wallet for the computer in this build."
+          if [ -n "$DESKTOP_SHA256" ] && [ -f fort-wallet-desktop.zip ]; then
+            ASSETS+=(fort-wallet-desktop.zip)
+            DESKTOP="On a computer: download fort-wallet-desktop.zip, unzip it, and in the
+          unzipped folder run \`python3 start-wallet.py\`. The wallet opens in the
+          browser at http://127.0.0.1:8791/. It needs Python 3. Use Chrome or Edge,
+          and one tab. The phone app and this download are the same wallet build.
+          SHA-256 of fort-wallet-desktop.zip: ${DESKTOP_SHA256}"
+          fi
           NOTES="Debug build of ${SHORT_SHA}. ${WALLET}
 
           Install on Android: open this page on the phone, tap fortoid-demo.apk,
           and allow installs from your browser when asked. Newer builds install
           over older ones.
 
-          Then open the **Fort Demo** icon to try the native phone features."
+          Then open the **Fort Demo** icon to try the native phone features.
+
+          ${DESKTOP}"
 
           gh release delete demo-latest --yes --cleanup-tag || true
-          gh release create demo-latest fortoid-demo.apk \
+          gh release create demo-latest "${ASSETS[@]}" \
             --prerelease \
             --target "$GITHUB_SHA" \
             --title "Fortoid demo (${SHORT_SHA})" \
```

The new step has no `continue-on-error`. If the zip cannot be built from a payload that was imported, the run fails and the release of the build before it stays as it was.

- [ ] **Step 6: Say it in the README**

In `README.md`, after the paragraph "The APK has two icons: …" and before "### Capabilities", add:

```markdown
### Run the wallet on a computer

The same release carries `fort-wallet-desktop.zip`: the wallet for a desktop
browser, built from the same wallet commit as the APK. Unzip it and, in the
unzipped folder, run `python3 start-wallet.py`. The wallet opens at
`http://127.0.0.1:8791/`. It needs Python 3 and nothing else. Use Chrome or
Edge, one tab, in a browser profile with no extensions. The script serves the
folder to that computer only, and the browser keeps the vault under that one
address. The release notes give the zip's SHA-256; `README.txt` in the zip
says the rest.

The Releases page has one more entry, "Runtime source copy": this
repository's checked copy of the archive the wallet is built from. It is not
something to install.
```

In "Wallet features (FortWeb fork)", the sentence "The wallet in the APK is built from [seriouscoderone/fortweb](https://github.com/seriouscoderone/fortweb), which uses the bridge above:" becomes "The wallet in the APK is built from the `credentials` branch of [seriouscoderone/fortweb](https://github.com/seriouscoderone/fortweb), which uses the bridge above:". After the last bullet of that list ("If the vault closes in the background, …"), add:

```markdown
- **A new engine underneath.** The `credentials` branch carries the
  Foundation's draft credential engine. Vaults made with builds before it are
  not shown: the engine keeps its storage in a new place. Nothing new is on
  screen for credentials yet.
- **Keeping the vault.** Each time a vault is opened the wallet asks the
  browser to keep its storage. On a computer, *Settings* shows whether the
  browser agreed. Chrome and Edge decide by themselves and usually say no to a
  site they have just met.
```

- [ ] **Step 7: Commit, push once, and check the release**

```bash
git add tools/build_desktop_wallet.py tools/test_build_desktop_wallet.py desktop/README.txt
git commit -m "feat(desktop): build the wallet for a computer from the imported payload (<bead>)"
git add .github/workflows/android-runtime-contract.yml
git commit -m "ci: test the desktop wallet builder (<bead>)"
git add .github/workflows/demo-apk.yml
git commit -m "ci(demo): demo-latest also carries the wallet for a computer (<bead>)"
git add README.md
git commit -m "docs(readme): the wallet on a computer, the new engine, keeping the vault (<bead>)"
git push origin HEAD:ccr-0427da9b-gq0ryv HEAD:dev
```

Wait for every workflow of this push; all must be green. Then check the release as a person gets it:

```bash
rm -rf /tmp/demo-latest && mkdir /tmp/demo-latest && cd /tmp/demo-latest
curl -sSLO https://github.com/seriouscoderone/fortoid/releases/download/demo-latest/fort-wallet-desktop.zip
curl -sSLO https://github.com/seriouscoderone/fortoid/releases/download/demo-latest/fortoid-demo.apk
sha256sum fort-wallet-desktop.zip fortoid-demo.apk
mkdir unzipped && unzip -q fort-wallet-desktop.zip -d unzipped
cd /home/user/fortweb
FORT_WALLET_FOLDER=/tmp/demo-latest/unzipped/fort-wallet \
  npx playwright test -c playwright.local.config.ts --trace off playwright/fortweb-desktop-start.spec.ts
```

Expected: the zip's SHA-256 equals the one in the notes of the `demo-latest` release; `2 passed`. Put both digests in the report.

---

### Task 8: The owner tries it on the phone and on the computer

**Files:** none.

**Interfaces:**
- Consumes: the `demo-latest` release after Task 7.

**Acceptance Criteria:**
- On the phone: the new APK installs over the old one; the wallet ends at *Set up your wallet* (the earlier vault is not shown, as agreed in the design, D2); a wallet is made; fingerprint unlock is offered and works; *Roles* reads `mad.seriouscoder.one` and a role can be taken on; after the app has been in the background for a minute it returns to the unlock screen and unlocks.
- On the computer: the zip from the same release starts with one command; the wallet opens in Chrome or Edge at `http://127.0.0.1:8791/`; a wallet is made; it is still there after the browser is closed and the script is started again; *Roles* reads `mad.seriouscoder.one`; *Settings* shows a line "Kept by the Browser", with whatever the browser answered.
- The owner has said that both work, or has said what does not.

- [ ] **Step 1: Tell the owner what to do**, in a few plain lines: where the release is; install the APK; on the computer, unzip and run `python3 start-wallet.py` (on Windows `py start-wallet.py`; the computer needs Python 3, so ask which computer it is if that is not known); what to look for on each. Say what could surprise:

  - On the phone, the first start after the update shows the earlier wallet's unlock screen for a few seconds, and may ask for a fingerprint. It then moves to *Set up your wallet* by itself.
  - The earlier vault is not shown. Its data stays on the phone, out of the new build's reach, until the app's storage is cleared in Android's settings. Going back to an earlier build would show it again.
  - On the computer, *Settings* will most likely say "No. The browser may remove this wallet's storage when it runs short of space." Chrome says that to a site it has just met; the wallet asks again each time the vault is opened.
  - A second browser tab shows the unlock screen and then says "This wallet is already open in another browser worker."

- [ ] **Step 2: Record what the owner reports** on the task's bead. Anything that does not work becomes a bug bead with what was seen, and this task stays open until it is settled.

---

## Coverage of the design's phase 1

| The design says | Task |
|---|---|
| The `credentials` branch with the draft merged and reviewed; merged as a merge | 1 |
| The fork's suites pass on it; the test that opened a second engine is rewritten | 1 |
| The switch `fort_synthetic_recipient_testing = true`, and the verifier accepts that one key with that one value | 2 |
| The fork's CI and Fortoid's reviewed pin change to the draft's runtime source | 1 (the draft's own build action names it), 6 |
| Fortoid keeps its own copy of the runtime source, checked, and its builds read that copy | 5, 6 |
| Fortoid pins the branch; every workflow is green; start-up time is checked after the first pin | 6 (with the time to compare from 5) |
| `demo-latest` carries the APK and the wallet for the computer, from one wallet commit; the notes give the zip's SHA-256 | 7 |
| The start script: one fixed address, this computer only, the host check, `localhost` sent on, a taken port, no listing | 4 |
| The wallet asks the browser to keep its storage, and Settings says whether it agreed | 3 |
| What the README says: one tab, no extensions, the same release for both, a demonstration build, automatic time | 7 |
| A test starts the script against the built package; tests for a file outside the folder, another host, a taken port | 4 |
| `README.md`, the reviewed runtime-source pin and the agent notes change | 6, 7 |
| The owner sets up a wallet on both and reads the source on both | 8 |

Left for the later plans, as the design's phases have it: the label "Experimental: for test credentials only" (with the credential screens, phase 3); the replay settings of D14 (phase 3); every message, code, screen and list of phases 2 and 3; the two sentences for a wallet open in another tab and for a wallet that could not save (with the first new screens, phase 2; until then a second tab shows the engine's own sentence). The beads for the two later plans are `fortoid-dls` (phase 2, which carries those two sentences in its notes) and `fortoid-cl1` (phase 3).

Three things this plan settles differently from the design's first wording. The design now says all three:

- **Who reads the copy.** Fortoid's builds read the repository's own copy of the runtime source. The wallet repository's own workflow keeps reading the Foundation's address, which the draft's build action names, so that the fork's copy of that action stays as the Foundation wrote it. If that address ever fails, the workflow's three inputs take the copy's address and the two digests.
- **Where Settings shows the browser's answer.** In a regular browser only. Inside the phone app the wallet asks too, and shows nothing, because what Android's WebView answers and then does is not known. Finding that out is the follow-up `fortoid-5wf`.
- **When the wallet asks.** At every opening of a vault, not only when one is made: a real Chrome says no to a site it has just met, and may say yes once the wallet has been used.

## Stress Test Results: the plan for phase 1 (the base)

Run on 2026-11-15, after the owner approved the plan. Twelve branches were mapped and three more came out of the self-review. Fourteen were settled from the code and by trying things on scratch copies; one was put to the owner.

### Resolved Decisions

- **The draft may move under the merge.** It had not: `upstream/pr42` is still `2db99e8` and the Foundation's `main` is still `a82bcb2`. Task 1 stops if that changes. How to take a newer draft later is now in the agent notes (Task 6): merge it again if the Foundation added commits; if it rewrote its branch, start again from `dev` as Task 1 does and cherry-pick the branch's own commits.
- **Starting workflows by hand.** GitHub starts a workflow by hand only when its file is on the default branch. The nine that Task 6 starts on the session branch are on `main`, and one of them was started that way on 2026-11-12. The copying workflow of Task 5 is on `dev` only, so its second run is a re-run of the first.
- **What a browser answers when asked to keep storage.** Tried for real: Chromium 153 at the fresh address `http://127.0.0.1:8791/` answers no, with no prompt. Asking once, when the vault is made, would have been the worst moment. The wallet now asks at every opening of a vault.
- **A phone that had the earlier wallet.** Tried with the earlier vault remembered and its fingerprint passcode saved: the old unlock screen shows, then *Set up your wallet*, and a wallet can be made. Nothing wrong is on the screen. The earlier vault's data stays on the phone, out of reach, which is also what makes going back lossless.
- **Going back.** Reverting the two pin commits restores the earlier wallet with its vault. Written into Task 6.
- **What the computer's wallet is safe for (the owner agreed on 2026-11-15).** A local address belongs to whichever program takes its port first; such a program could pose as the wallet. The script cannot prevent that. The computer's wallet is for the demonstration with test credentials. The script stops and says not to open the address when the port is taken; the README says the same; the design records it, and a desktop application for real use is a later slice.
- **The switch is on two phases before its label.** Kept as the design has it. No screen calls what the switch turns on until phase 3, and only the wallet's own page can reach the engine. One of the draft's own tests, run by no workflow, fails by design.
- **The copy of the runtime source.** The workflow's script was read for injection: every value it uses comes from the checked list, is quoted, and the tag, the digest and the address are restricted to plain characters. A wrong copy cannot enter a build, because every build checks both digests again.
- **Time and capacity.** No browser test file passes 190 seconds locally. Nine workflows started together fit GitHub's limit for a public repository.
- **The fork's CI steps that had not been run locally.** The build-twice check and the wheelhouse test pass. The canary passes with the headless shell, the browser the workflow installs; with the container's full Chromium it fails on a request for `/favicon.ico`, which is a matter of the local browser and is now in the agent notes.
- **The new tests under the workflow's browser.** The three new browser test files and the rewritten test pass with the headless shell as well as with the container's Chromium.
- **Web Locks on Android.** The draft needs `navigator.locks` inside the worker. The feature has been in every Chromium since 2018 and works in workers; that it works in the phone's WebView is expected and unproven until Task 6, which is why the pin is proved on the session branch before `dev` moves.

### Changes Made

- Task 3: the wallet asks in `openVault`, at every opening; a fourth test (a browser that said no is asked again, and Settings follows); the module's comment says why.
- Task 2: the switch test holds the engine to its codes (`NOT_FOUND`, `UNSUPPORTED`), not to the draft's sentences.
- Task 4: the start script says not to open the address when the port is taken, and to copy the address into Chrome or Edge if another browser opens.
- Task 5: the second run of the copying workflow is a re-run.
- Task 6: the agent notes gain how to take a newer draft and the canary's need for the headless shell; a paragraph "Going back".
- Task 7: the README in the zip gains three sentences (another browser, a taken port, how the browser decides about storage); the release notes say that a wallet from before the credential engine is not shown; Fortoid's README says what the "Runtime source copy" entry is.
- Task 8: what the owner will see on first start of the phone, in Settings on the computer, and where the earlier vault's data stays.
- The design: the wallet asks at every opening; what a local address is safe for; a desktop application as a later slice.
- Numbers: the browser suite has 146 tests after Task 4.

### Deferred / Parking Lot

- What Android's WebView answers when asked to keep storage, and what Settings should say on the phone: `fortoid-5wf`.
- The sentence "Vault 'my-wallet' was not found." that reaches a screen reader on the first start after the update. It happens once, on a phone that had the earlier wallet. Left as it is.
- A desktop application around the wallet, for real use on a computer: a later slice in the design.
- Firefox asks the person, by a prompt, each time the wallet asks to keep storage, until the person answers. Firefox is not a recommended browser for this build.

### Confidence Assessment

- Overall: High for Tasks 1 to 5 and 7, which were run end to end on scratch copies. Medium for Task 6: the merged wallet has never run on Android, and the plan is built so that `dev` and the published demo do not move until it has.
- Areas of concern: Web Locks inside the worker in Android's WebView; start-up time now that the engine writes to storage after every request; a second engine meeting `BUSY` when Android restarts the page.
