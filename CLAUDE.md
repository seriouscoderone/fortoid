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
