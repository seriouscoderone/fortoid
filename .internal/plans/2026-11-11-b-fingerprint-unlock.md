# B. Fingerprint unlock per vault — Implementation Plan

> Spec: `.internal/specs/2026-11-11-wallet-first-fingerprint-qr-design.md` (section B).
> Each Task is a bead under the B epic. Small commits, pushed straight to `main`.

**Goal:** Any vault can be opted in to fingerprint unlock after one passcode
unlock; its unlock screen then prompts automatically, with passcode fallback.

**Architecture:** Fortoid adds a biometric-gated secret store (one AES-GCM key in
AndroidKeyStore, ciphertexts in app-private prefs). FortWeb stores each opted-in
vault's passcode there under a key derived from (vault id, createdAt) and uses it
on the unlock page.

## Global constraints

- Secrets released only after a fresh strong-biometric match (CryptoObject).
- New fingerprint enrolled → key invalidated → all secrets wiped → `invalidated`.
- Internal keystore alias must not be reachable through `keystore.*` page aliases
  (those are `fortoid.<alias>`; the store uses `fortoid-internal.secret-store`).
- Browser/iOS unchanged: no fingerprint UI without the native bridge.

### B1 (Fortoid) Secret store policy — pure Kotlin + JVM tests
`SecretStorePolicy`: `storageKey(params)` (`[A-Za-z0-9_-]{1,64}`), `secretValue(params)`
(1..1024 chars), `encodeBlob(iv, ct)` / `decodeBlob(text)`.
AC: invalid key/value rejected; blob round-trips; malformed blob → null.

### B2 (Fortoid) `secret.*` capabilities + demo card
`SecretStore`: `secret.status|store|retrieve|delete`; new `ERROR_INVALIDATED`.
AC: compiles, unit tests green, APK builds; demo card stores/retrieves/forgets a test secret.

### B3 (FortWeb) Vault passcode client
`app/runtime/vault-fingerprint.ts`: `vaultSecretKey(vault)` (SHA-256 of `id|createdAt`,
`vault-<32 hex>`), `fingerprintStatus`, `rememberPasscode`, `recallPasscode`,
`forgetPasscode`, "not now" memory in localStorage.
AC: typecheck; covered by B4/B5 Playwright tests.

### B4 (FortWeb) Unlock page: fingerprint unlock + forget
Auto-prompt when stored; **Unlock with fingerprint** button; **Forget fingerprint** link;
`AUTH_FAILED` with recalled passcode → forget + message; `invalidated` → message.
New fixture `unlock/auth-fails`. AC: Playwright with fake bridge.

### B5 (FortWeb) Offer after passcode unlock
After a successful passcode open: if native + biometrics available, not stored, and
not declined → modal "Use fingerprint for <vault>?" (Yes → store; Not now → remember).
AC: Playwright: offer shown/hidden in each condition; Yes stores; Not now persists.

### B6 (FortWeb) Layout fix (fortoid-z7l)
Scan QR button spacing; status lines wrap long URLs.
AC: Playwright/visual check at 411px: no horizontal overflow in the dialog.

### B7 (Fortoid) Pin wallet to new FortWeb commit; APK published.
### B8 Owner verifies on phone.
