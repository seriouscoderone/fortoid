# Fort Android

KERI Fort mobile application shell for Android.

## Architecture

Fort Android is a hardened WebView shell that hosts the shared [FortWeb](https://github.com/keri-foundation/fortweb) runtime. FortWeb owns the product UI, routing, Python/Pyodide backend, and bridge protocol. Android owns the native wrapper, WebView security policy, and platform lifecycle.

## Native Phone Capabilities Demo

This fork adds a proof-of-concept native bridge so web code in the WebView can
use the phone's hardware, plus a **Fort Demo** launcher icon that opens a
bundled demo page (`app/src/main/assets/demo/`) exercising each capability.

### Install on a phone

Every push to `dev` builds a debug APK (`.github/workflows/demo-apk.yml`) and
publishes it as the **demo-latest** pre-release. On the phone, open the
repository's Releases page, download `fortoid-demo.apk`, and allow installs
from your browser when Android asks. Newer builds install over older ones
(debug builds share `app/demo-debug.keystore`; it is not a release key).

The APK has two icons: **KERI Wallet** (FortWeb) and **Fort Demo** (`DemoActivity`, its own task).

### Capabilities

| Capability | Params | Result |
|---|---|---|
| `device.info` | — | manufacturer, model, Android version |
| `biometric.authenticate` | `title?`, `subtitle?` | `{authenticated: true}` after a strong-biometric match |
| `keystore.generate` | `alias`, `requireBiometric?` (default `true`) | SPKI public key, security level (`strongbox`/`tee`/…) |
| `keystore.publicKey` | `alias` | same as generate |
| `keystore.sign` | `alias`, `data` (base64, ≤2048 bytes) | ES256 signature as DER and P1363 (r‖s), base64 |
| `keystore.delete` | `alias` | `{deleted}` |
| `qr.scan` | `prompt?` | `text`, `format`, `isOobi`, `oobiAid?` |
| `secret.status` | `key` | `{stored, biometricAvailable, reason?}` |
| `secret.store` | `key`, `value` (≤1024 chars), `title?`, `subtitle?` | `{stored: true}` after a strong-biometric match |
| `secret.retrieve` | `key`, `title?`, `subtitle?` | `{value}` after a strong-biometric match |
| `secret.delete` | `key` | `{deleted}` |

Keys are EC P-256 in the Android Keystore, StrongBox-backed when the phone has
it; with `requireBiometric` every signature needs a fresh fingerprint/face match
and the private key never leaves secure hardware.

The secret store uses one AES-256-GCM AndroidKeyStore key (StrongBox when
available) that needs a fresh strong biometric for every use; ciphertexts live
in app-private storage. Enrolling a new fingerprint or face wipes the key and
every stored secret (`invalidated`).

### Bridge protocol

From the page (the trusted app origin, main frame only):

```js
// app/src/main/assets/demo/fort-native.js
const info = await FortNative.call('device.info');
```

On the wire, over `window.bridge`:

```json
{"type":"native_request","id":"n1","capability":"keystore.sign","params":{"alias":"demo","data":"aGk="}}
{"type":"native_response","id":"n1","ok":true,"result":{"algorithm":"ES256","signature":"…"}}
{"type":"native_response","id":"n1","ok":false,"error":{"code":"cancelled","message":"…"}}
```

Error codes: `bad_request`, `unknown_capability`, `unavailable`, `cancelled`,
`busy`, `failed`, `invalidated`. Protocol and dispatch live in
`app/src/main/java/org/kerifoundation/fort/bridge/`.

### Wallet features (FortWeb fork)

The wallet in the APK is built from
[seriouscoderone/fortweb](https://github.com/seriouscoderone/fortweb), which
uses the bridge above:

- **Wallet-first launch.** A fresh install shows *Set up your wallet*
  (passcode only). Later launches open the default vault directly.
- **Fingerprint unlock per vault.** After a passcode unlock the wallet offers to
  save that vault's passcode in the secret store; its unlock screen then prompts
  for a fingerprint automatically.
- **Default vault.** The first vault is the default; *Settings → Make default*
  changes it.
- **Scan QR code** in *Add Remote Identifier* fills the OOBI URL.
- If the vault closes in the background, the app returns to its unlock screen.

Design and plans: `.internal/specs/` and `.internal/plans/`.

## Prerequisites

- Android Studio (latest stable)
- JDK 17+
- Android SDK 36 with build tools
- Node.js 20+
- Python 3.12+ (used by the canonical FortWeb producer build)
- Optional: a local [FortWeb](https://github.com/keri-foundation/fortweb) checkout, only for `--fortweb-dir`

## Quick Start

```bash
# Stage the canonical FortWeb runtime into the Android payload
./sync-payload.sh --fetch --locked

# Build and install on emulator
./gradlew :app:assembleDebug
```

## Payload Synchronization

Android consumes one canonical FortWeb runtime package. The producer revision is
never a parameter: `config/fortweb-runtime.json` is the sole authority, and every
mode is bound to it.

### Package mode (primary)

Stage an already-produced package. No checkout, no build, no network.

```bash
./sync-payload.sh --package /path/to/fortweb-runtime.zip
```

### Locked fetch mode

Fetch the locked revision, build the canonical package, then stage it.

```bash
./sync-payload.sh --fetch --locked
```

### Locked local checkout mode

Build from an existing checkout, which must already be at the locked revision.

```bash
./sync-payload.sh --fortweb-dir ../fortweb
```

### Inspect without running

`--print-plan` prints the exact sequence, including the canonical producer's
steps, without executing anything.

```bash
./sync-payload.sh --fetch --locked --print-plan
```

### Lock authority

- `config/fortweb-runtime.json` pins the producer repository and commit. The
  repository must be on the lock reader's allowlist (`keri-foundation/fortweb`
  or `seriouscoderone/fortweb`).
- A local checkout must equal the locked commit, or the sync is refused.
- The imported package manifest's `fortweb_commit_sha` must equal the lock, or the
  import is refused before anything is staged.
- Mutable refs are not supported. `--ref` is rejected.

There is no published Fortoid runtime release yet, so a package currently comes
from a canonical local build (`--fetch --locked` or `--fortweb-dir`) or from an
appropriate CI artifact. Do not assume a downloadable release exists.

## Build

```bash
./gradlew verifyPreparedPayload
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Generated Payload Policy

`app/src/main/assets/payload/` is generated and gitignored.

## Deferred Work

- Bridge contract generation
- Runtime-origin contract injection
- WebView host modernization (PR #22 reference)
- Emulator acceptance testing

## Relationship to PR #22

PR #22 contains substantial wrapper work but predates the canonical `dist/runtime` pipeline. It serves as an implementation reference.
