# Research: is iOS readiness the next piece of work?

> **Date:** 2026-11-12
> **Bead:** fortoid-uzt (work item: fortoid-bgy)
> **Status:** Complete

## Summary

The owner's iOS fork is 98 commits behind the Foundation's repository and has no
commits of its own, so syncing it is a fast-forward. The Foundation's iOS app is
itself about two months behind the wallet that Android runs, and its bridge only
carries messages from the page to the app, so none of the 11 phone features
built for Android exist there. Recommendation: do two cheap steps now (sync the
fork, then a CI health check that builds the iOS app with our wallet), and port
the phone features after the first browser demonstration.

## Key Findings

### 1. The fork can be fast-forwarded

> **Confidence:** high. Direct `git rev-list` output.

`seriouscoderone/fortios` `main` is at `b43a0c8` (2026-05-01, 28 commits).
`keri-foundation/fortios` `main` is at `c293f6a` (2026-10-05).
`git rev-list --left-right --count upstream/main...origin/main` gives `98 0`:
98 behind, 0 ahead. [S1]

### 2. The Foundation's iOS app trails the wallet that Android runs

> **Confidence:** high. Read from the workflow file and `git log`.

- iOS CI checks out `keri-foundation/fortweb` at `bdb81af` (2026-09-09) with
  runtime source `runtime-source-pyodide-314-20260909`. [S2]
- That commit is not an ancestor of FortWeb `main`; `main` (`a82bcb2`) has five
  later commits: #33, #38 (Python 3.14 / Pyodide 314), #39, #40 (runtime
  publisher), #41 (mobile accessibility). [S3]
- Fortoid and our wallet fork build against FortWeb `main` plus 17 fork commits
  and the newer runtime source `runtime-source-pyodide-314-hio-0.7.20-20260910`.
  [S4]
- Since the 2026-09-09 switch to the canonical runtime package, iOS upstream has
  had CI, release-gate and docs commits only. [S1]

So nobody has yet shown that today's wallet boots in the iOS shell. That is the
main unknown.

### 3. The iOS bridge is one-way; the phone features do not exist there

> **Confidence:** high. Read from source.

- `KeriWallet/WebBridge.swift` is a `WKScriptMessageHandler` named `bridge`. It
  accepts `js_error`, `unhandled_rejection`, `log`, `lifecycle` and
  `crypto_result` from the main frame at origin `app://local`, and has no way to
  answer the page. [S5]
- The wallet already posts its logs to `window.webkit.messageHandlers.bridge`
  (`app/runtime/logger.ts`), but `app/runtime/native-capabilities.ts` only
  speaks to Android's `window.bridge`. [S6]
- Fortoid has 11 capabilities behind one request/response protocol
  (`native_request` / `native_response`): `device.info`,
  `biometric.authenticate`, `keystore.generate/publicKey/sign/delete`,
  `secret.status/store/retrieve/delete`, `qr.scan`. [S7]
- Without a port, the wallet still runs on iOS with a passcode. Fingerprint or
  Face ID unlock and QR scanning simply do not appear, because the wallet hides
  them when no native side answers. [S6]
- `bridge-contract.json` in the iOS repository is the Foundation's single source
  for bridge constants and generates both Swift and Kotlin. A parity port should
  add the request/response message types there. [S8]

### 4. iOS can be built and tested without a Mac in this session

> **Confidence:** high for the workflow facts; medium for cost (billing page
> read earlier today, not re-read).

- `ios-security.yml` has a `swift-tests` job on `macos-26` that runs
  `xcodebuild test` (unit and UI tests) on an iPhone 17 Pro simulator, iOS 26.5,
  after building and importing the wallet package. [S2]
- GitHub's standard hosted runners are free in public repositories, and the
  forks stay public. [S9]
- Deployment target is iOS 16.4, so the APIs a port needs are available
  (reply-capable script message handlers, LocalAuthentication, Keychain access
  control, Secure Enclave keys, camera scanning). [S10]

### 5. What only a real iPhone can prove

> **Confidence:** medium. Platform knowledge, not independently verified here.

Camera QR scanning, real Face ID or Touch ID, and keys held in the secure chip
need a physical iPhone. The simulator can fake a Face ID match and has no
camera. Installing on the owner's iPhone means building from Xcode on their Mac
with their Apple ID; there is no equivalent of handing over an APK.

## Comparisons

| | Android (Fortoid fork) | iOS (Foundation upstream) |
|---|---|---|
| Wallet version | FortWeb `main` + our features | `bdb81af`, five `main` commits behind |
| Page-to-app messages | yes | yes |
| App answers the page | yes (`native_response`) | no |
| Phone features | 11 | 0 |
| Tested where | CI emulator + owner's phone | CI simulator; owner's Mac and iPhone |
| Install for a demo | APK download | Xcode on a Mac |

## Codebase Context

- Fortoid: `app/src/main/java/org/kerifoundation/fort/bridge/NativeBridgeProtocol.kt`,
  `NativeCapabilityDispatcher.kt`, and one policy + capability pair per feature
  under `org/kerifoundation/fortandroid/`.
- Wallet: `app/runtime/native-capabilities.ts` (`isNativeAvailable`,
  `callNative`), used by `vault-fingerprint.ts` and the Remotes QR button.
- iOS: `KeriWallet/WebBridge.swift`, `AppConfig.swift` (trusted origin),
  `bridge-contract.json`, `Makefile` (`payload-contract`, `test-swift`).

## Recommendations

1. **Now, cheap:** fast-forward the iOS fork to upstream `main`.
2. **Now, cheap:** health check. In the synced fork, point the iOS build at
   `seriouscoderone/fortweb` `main` and the newer runtime source, and run the
   existing `swift-tests` job. Green means the shell runs today's wallet. Red
   shows the size of the gap before any feature work.
3. **After the first browser demonstration:** port the phone features. Add the
   request/response protocol to the shared contract and `WebBridge`, implement
   the 11 capabilities in Swift, and add the iOS transport to
   `native-capabilities.ts`. This is about the size of the Android demo work.
   Doing it after the demonstration means porting once: the demonstration may
   add phone features of its own, such as a fetch for reading sources.
4. Until then, keep every phone feature behind `native-capabilities.ts` so the
   iOS port stays a matter of answering the same capability names.

Deferring step 3 is a scheduling choice, not a descope: iOS parity stays a
requirement (bead fortoid-bgy).

## Open Questions

- Does today's wallet boot in the iOS shell? Step 2 answers it.
- Does the owner's Mac have Xcode 26.5, which the project pins?
- Will the demonstration for the Foundation be shown on an iPhone? If so, step 3
  moves ahead of the demonstration work.

## Sources

- [S1] `git log`, `git rev-list` on `keri-foundation/fortios` `main` (`c293f6a`)
  and `seriouscoderone/fortios` `main` (`b43a0c8`), fetched 2026-11-12. Primary.
- [S2] `keri-foundation/fortios` `.github/workflows/ios-security.yml` at
  `c293f6a`. Primary.
- [S3] `git log bdb81af..upstream/main` and `git merge-base --is-ancestor` in
  `keri-foundation/fortweb`. Primary.
- [S4] Fortoid `config/fortweb-runtime.json` and
  `scripts/build-canonical-fortweb-package.sh`. Primary.
- [S5] `keri-foundation/fortios` `KeriWallet/WebBridge.swift`,
  `KeriWallet/AppConfig.swift` at `c293f6a`. Primary.
- [S6] `seriouscoderone/fortweb` `app/runtime/logger.ts`,
  `app/runtime/native-capabilities.ts`. Primary.
- [S7] Fortoid `MainActivity.kt` (`buildNativeCapabilities`) and
  `NativeBridgeProtocol.kt`. Primary.
- [S8] `keri-foundation/fortios` `bridge-contract.json` at `c293f6a`. Primary.
- [S9] GitHub Docs, "GitHub Actions billing"
  (https://docs.github.com/en/billing/concepts/product-billing/github-actions),
  read 2026-11-12: standard runners are free in public repositories. Primary.
- [S10] `KeriWallet.xcodeproj/project.pbxproj`:
  `IPHONEOS_DEPLOYMENT_TARGET = 16.4`. Primary.
