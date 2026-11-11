# C. Default vault and wallet-first launch — Implementation Plan

> Spec: `.internal/specs/2026-11-11-wallet-first-fingerprint-qr-design.md` (section C),
> refined by the contexts review (`fortoid-zoy`): one default vault holds all of the
> user's identities (contexts ≈ AIDs inside it). Small commits straight to `main`.

**Goal:** Opening the app goes straight to the user's wallet (fingerprint if enabled);
a new user sees "Set up your wallet", never the vault concept; a vault that closes
behind the user's back returns them to the default vault's unlock.

## Findings that shape the plan

- Vault registry records (`app/runtime/vaulting.py`) are whitelisted in
  `_registry_payload` / `_vault_view`; a new field must be added in both.
- Auto-close root cause (`fortoid-du7`): `app/runtime/bridge.ts` discards the Python
  worker after the app is hidden ≥30 s (`BACKGROUND_STALE_THRESHOLD_MS`). The open
  vault lives in that worker, but the page's session still says "unlocked".
- `vaults.create` accepts an empty passcode and then creates an **unencrypted** vault.
  First-run setup must require a passcode.

## Global constraints

- Wording on new screens says "wallet", never "vault" or "context".
- Vaults stay fully available from the drawer (advanced).
- Browser behaviour stays sensible: same flow, just no fingerprint.
- No security control weakened: passcode required for the setup vault.

### C1 (Python) Default vault in the registry
`isDefault` on registry records (payload + view); `vaults.setDefault {vaultId}` (exactly one
default); creating the first vault makes it default; `vaults.list` migration: vaults exist but
none is default → earliest `createdAt` becomes default. Wire the method in `wallet-worker.py`
and `method-catalog.ts`.
AC: `python/test_vaulting.py` cases with a fake registry: first vault default, second not,
setDefault moves it, migration picks earliest, unknown id → NOT_FOUND.

### C2 (TS) Wallet-first launch routing
Home route: default vault exists → its unlock (or last core route if already open);
no vaults → first-run setup. The logo splash only shows while loading.
AC: Playwright (fake runtime responses where needed): launch with a default vault lands on
its unlock; launch with none lands on setup.

### C3 (TS) "Set up your wallet" first-run screen
Passcode + confirm (eye toggles), name defaults to "My Wallet", creates the vault (default),
opens it, offers fingerprint (reuse the B offer, moved to a shared module), lands on Identifiers.
AC: Playwright: mismatched/empty passcode blocked; success path calls create → open; offer shown.

### C4 (TS) "Make default" in the Vaults drawer
"Default" badge on the default vault; "Make default" on the others.
AC: Playwright: badge shown; Make default calls `vaults.setDefault` and moves the badge.

### C5 (TS) Closed-vault recovery (fortoid-du7)
Bridge reports a worker reset; the app clears the unlocked vault and navigates to that vault's
unlock (fingerprint prompt appears); a vault op answered with LOCKED does the same.
AC: Playwright: simulated reset while on a vault page → unlock route; LOCKED from an action → unlock.

### C6 (Fortoid) Pin wallet to the new FortWeb commit; APK published.
### C7 Owner verifies on phone.
