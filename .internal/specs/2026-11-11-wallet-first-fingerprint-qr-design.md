# Wallet-first launch, per-vault fingerprint unlock, and QR connect — design

Status: draft for owner review · Brainstorm bead: `fortoid-9ay`
Repos: `seriouscoderone/fortoid` (Android host), `seriouscoderone/fortweb` (wallet UI, based on keri-foundation/fortweb `a82bcb2`)

## Goal

For a KERI Foundation demo, make the Android app feel like a phone wallet:
open the app, touch the fingerprint sensor, you are in. Vaults stay available
as an advanced concept but are not the first thing a user meets. Connecting to
someone is a QR scan, not pasting a URL.

## Background (how FortWeb works today)

- Each vault has its own passcode, encrypted storage, and KDF salt. The passcode
  is the secret that opens the vault; it cannot be replaced, only supplied.
- One vault is open at a time; opening another closes the current one.
- Vault IDs are slugs of the vault name (`my-vault`, `my-vault-2`). There is no
  rename or delete, but clearing app data and re-creating a same-named vault
  reuses the ID for a different vault.
- First run shows a logo; the user must open the Vaults drawer to create a vault.
- "Add Remote Identifier" takes a pasted OOBI URL.
- Fortoid's bridge already provides `qr.scan`, `biometric.*`, `keystore.*`
  (`native_request` / `native_response` over `window.bridge`).

## Scope — three pieces, built in order

### A. QR scan to connect

- FortWeb gets `app/runtime/native-capabilities.ts`: a typed promise client for
  the Fortoid bridge (same protocol as the demo's `fort-native.js`).
  `isNativeAvailable()` is false in a normal browser and on iOS.
- The "Add Remote Identifier" dialog shows a **Scan QR** button only when the
  native bridge is available. A scan fills the OOBI URL field; a non-OOBI scan
  still fills it and shows "This doesn't look like an OOBI link". Resolving
  still requires the user to tap **Resolve OOBI**.

### B. Fingerprint unlock, per vault

Native (Fortoid) — new **secret store** capability:

| Capability | Params | Biometric | Result |
|---|---|---|---|
| `secret.status` | `key` | no | `{stored, biometricAvailable}` |
| `secret.store` | `key`, `value` (≤1024 chars), `title?`, `subtitle?` | yes | `{stored: true}` |
| `secret.retrieve` | `key`, `title?`, `subtitle?` | yes | `{value}` |
| `secret.delete` | `key` | no | `{deleted}` |

- One AES-256-GCM key in AndroidKeyStore (`fortoid.secret-store`), StrongBox
  when available, `setUserAuthenticationRequired` with strong biometric per use
  (no time window), `setInvalidatedByBiometricEnrollment(true)`.
- Each secret: ciphertext + IV in app-private `SharedPreferences`
  (`allowBackup=false` already excludes it from backups). Never logged.
- Encrypt and decrypt both go through `BiometricPrompt` with a `CryptoObject`.
- Enrollment change → `KeyPermanentlyInvalidatedException` → delete the key and
  **all** stored secrets, return new error code `invalidated`.
- `key` validated like keystore aliases (`[A-Za-z0-9_-]`, ≤64), namespaced.

Wallet (FortWeb):

- Secret key per vault: `vault-<vaultId>-<createdAt digits>`, so a re-created
  vault that reuses an ID never matches an old secret.
- After a successful passcode unlock, if native + biometrics are available and
  this vault has no stored passcode, offer **"Use fingerprint for <vault>?"**
  (Yes → `secret.store`; Not now → remembered per vault in localStorage).
- Unlock screen: if the vault has a stored passcode, show **Unlock with
  fingerprint** and prompt automatically on arrival; passcode field remains.
  **Forget fingerprint** link deletes this vault's secret.
- Stored passcode rejected (`AUTH_FAILED`) → delete it, tell the user to type
  the passcode. `invalidated` → explain a new fingerprint was enrolled.

### C. Default vault and wallet-first launch

- Vault registry records gain `isDefault` (exactly one vault, or none if no
  vaults). New runtime method `vaults.setDefault {vaultId}`. Stored in the
  registry so it is known before any vault is unlocked.
- **First run (no vaults):** instead of the logo, show **Set up your wallet**:
  choose a passcode (name defaults to "My Wallet"). Creates the vault, marks it
  default, opens it, offers fingerprint, lands on Identifiers.
- **Launch with a default vault:** go straight to the default vault's unlock
  screen (auto fingerprint prompt if enabled). Cancel → passcode field; the
  drawer is still reachable.
- Creating or switching to other vaults never changes the default. The Vaults
  drawer gets **Make default** per vault (advanced).
- Existing installs with vaults but no default: the earliest-created vault
  becomes default on first `vaults.list` (one-time migration).

## Security notes

- Passcodes at rest exist only as ciphertext under a hardware-bound,
  biometric-gated key; released to the wallet page only after a fresh match.
  The page already receives the passcode when typed, so no new exposure.
- Trade-off stated plainly: anyone who passes the phone's strong-biometric
  check can open fingerprint-enabled vaults. Opt-in per vault.
- Bridge rules unchanged: trusted origin, main frame only, size limits.
- No weakening of FortWeb's runtime-origin, service-worker, or network policy.

## Repo wiring

- Fortoid's FortWeb lock moves to `seriouscoderone/fortweb` at the new commit;
  the lock reader's repository check accepts the fork. CI (`demo-apk.yml`)
  proves the newer FortWeb still builds into the APK.
- Small commits straight to `main` in both repos.

## Testing

- Fortoid: JVM tests for secret-store input/encoding rules; existing suite green.
- FortWeb: `npm run typecheck`; Playwright tests with a fake `window.bridge`
  for the Scan QR button, fingerprint offer/unlock/forget, first-run setup,
  and default-vault launch (fixture routes where the runtime is not needed).
- Python: unit test for `isDefault`, `vaults.setDefault`, and the migration.
- Owner verifies on phone after each piece (A, B, C).

## Out of scope

- Hardware-protected wallet keys and hardware signing of KERI events (later).
- iOS (FortWeb features stay hidden when the Android bridge is absent).
- Renaming "vault" throughout the UI beyond the first-run screen.
