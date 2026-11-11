# A. QR scan to connect — Implementation Plan

> Spec: `.internal/specs/2026-11-11-wallet-first-fingerprint-qr-design.md` (section A).
> Each Task is a bead under the A epic. Small commits, pushed straight to `main`.

**Goal:** In the FortWeb wallet on Android, "Add Remote Identifier" can fill the
OOBI URL by scanning a QR code.

**Architecture:** FortWeb gets a typed client for Fortoid's `native_request` /
`native_response` bridge. The remotes dialog renders a Scan QR button only when
that bridge exists. Fortoid then pins the wallet build to the owner's FortWeb fork.

**Tech stack:** TypeScript (FortWeb, `tsc`), Playwright (FortWeb UI tests),
Node scripts + Gradle (Fortoid).

## Global constraints

- Browser and iOS behaviour unchanged: no Scan QR button without `window.bridge`.
- Scanning never resolves an OOBI by itself; the user still taps Resolve OOBI.
- Bridge protocol exactly as Fortoid's `NativeBridgeProtocol` (type, id, capability, params).

### Task A1: FortWeb native bridge client

**Files:** create `app/runtime/native-capabilities.ts`; modify `app/runtime/types/browser-bridge.d.ts`.

**Interfaces — produces:**
- `isNativeAvailable(): boolean`
- `callNative<T>(capability: string, params?: Record<string, unknown>): Promise<T>`
  rejecting with `NativeCallError { code: string; message: string }`
- `scanQrCode(prompt?: string): Promise<QrScanResult>` where
  `QrScanResult = { text: string; format: string; isOobi: boolean; oobiAid?: string; truncated: boolean }`

**Acceptance criteria:**
- `npm run typecheck` passes.
- Exercised through A2's Playwright tests (fake bridge round trip, error code surfaced).

### Task A2: Scan QR button in Add Remote Identifier

**Files:** modify `app/features/remotes/remotes-page.ts`; test
`playwright/fortweb-native-qr.spec.ts` (fixture route `#/_fixtures/remotes/empty`
with a fake `window.bridge`).

**Acceptance criteria:**
- With bridge: dialog shows Scan QR; an OOBI scan fills `#resolve-oobi-url`.
- Non-OOBI scan fills the field and shows "This doesn't look like an OOBI link."
- Cancelled scan leaves the field unchanged, no error shown.
- Without bridge: no Scan QR button.

### Task A3: Pin Fortoid's wallet build to the fork

**Files (Fortoid):** `config/fortweb-runtime.json`, `tools/read-fortweb-runtime-lock.mjs`,
any sync-contract test that asserts the repository, `.github/actions/prepare-fortweb-runtime`.

**Acceptance criteria:**
- Lock = `seriouscoderone/fortweb` @ the A2 commit; lock reader accepts it.
- `npm run test:sync-contract` passes; Demo APK workflow publishes with the wallet included.

### Task A4: Owner verifies on phone

**Acceptance criteria:** owner scans an OOBI QR in the wallet and resolves it.
