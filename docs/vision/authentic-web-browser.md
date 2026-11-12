# The KERI wallet as the browser for the authentic web

*Status: vision draft for discussion. Owner: Fortoid fork (seriouscoderone).*

> **Parts of this draft are out of date (12 November 2026).** Later decisions
> replace them: multi-sig across devices is not planned, and each wallet (phone,
> desktop, web) is separate with its own AIDs; applications are not distributed
> as signed code bundles; and "one AID per application" is no longer assumed. A
> revised vision is being drafted and will replace this page.

## The idea

The web is shifting from **trust in locations and accounts** to **trust in
verifiable identity and data** — what Sam Smith calls the *authentic web*. A
shift that large needs a new kind of browser. The KERI wallet is that browser:
the place where people meet applications, and where every interaction is
authenticated by KERI instead of by DNS, TLS certificate authorities, and
per-site accounts.

People should experience it as **applications that solve their problems**. The
wallet — vaults, keys, identifiers, witnesses — stays in the background, the way
a web browser's cookie jar and certificate store do today.

## Today's browser vs. an authentic-web browser

| Today's web browser | Authentic-web browser (KERI wallet) |
|---|---|
| Visits a site by domain name; trusts it because a certificate authority vouched for the domain | Connects to a party by **AID**; trusts it by verifying the party's own key event log (KEL) and witnesses — no certificate authority |
| Separates sites by origin automatically (cookies, storage, permissions) | Gives each application or relationship **its own AID automatically**, so interactions are unlinkable by default |
| Logs in with passwords, OAuth, or passkeys held by each site | No logins: every exchange is **signed and verifiable**; there is no account database to breach |
| Address bar shows a domain | Shows **who** you are dealing with: a verified AID and the credentials it has presented (e.g. a vLEI) |
| Permission prompts for camera or location | Consent prompts for identity: "this app asks to see your over-18 credential" |
| Pages call browser APIs (fetch, storage, camera) | Applications call a **KERI API** the wallet provides: create a relationship AID, sign, present, verify, exchange |

## Principles

1. **KERI-only identity.** Identity, authentication and signing are KERI. No
   passwords, OAuth or passkeys as alternatives.
2. **Application-first.** People open applications, not vaults. The wallet never
   asks someone to understand AIDs before it is useful.
3. **Unlinkable by default.** The wallet, not the person, creates and manages
   per-application and per-relationship AIDs, the way browsers separate origins.
4. **Hardware-protected.** Phone secure hardware and biometrics protect and
   unlock KERI material; they never replace it.
5. **Consent at the boundary.** Anything that discloses identity or signs on the
   person's behalf passes through a wallet-owned consent prompt that applications
   cannot imitate or skip.
6. **Applications are parties too.** An application is published by an AID; the
   wallet verifies an application's publisher the same way it verifies anyone.

## Beyond "contexts"

The KERI Foundation's *context selection preview*
([fortweb#47](https://github.com/keri-foundation/fortweb/pull/47)) lets a person
pick a part of their life — "Work", "Family" — when presenting credentials, to
reduce how easily interactions are linked. It is a step away from exposing
vaults, but it still asks the person to manage identity buckets by hand.

A browser does not ask "which part of your life is this?" before loading a
site; it separates sites itself. The authentic-web browser should do the same:
each application gets its own relationship AID automatically, and the person
only sees a decision when something is disclosed. Contexts can remain as an
optional, advanced grouping — not the front door.

## Architecture sketch

```
┌──────────────────────────────────────────────────────────────┐
│ Applications (sandboxed; one per relationship/publisher AID)  │
│   call the KERI API — never touch keys or other apps' data    │
├──────────────────────────────────────────────────────────────┤
│ KERI API + consent UI (wallet-owned, cannot be spoofed)       │
│   relationship AIDs · sign · present · verify · exchange      │
├──────────────────────────────────────────────────────────────┤
│ Wallet runtime (FortWeb + KERIpy)                             │
│   vaults · KELs · keys · credentials · witnesses · watchers   │
├──────────────────────────────────────────────────────────────┤
│ Host (Fortoid on Android)                                     │
│   secure hardware · biometrics · camera/QR · lifecycle        │
└──────────────────────────────────────────────────────────────┘
```

- **Host (Fortoid).** Already provides hardened WebView hosting and a native
  bridge for phone capabilities (biometrics, hardware keystore, secret store,
  QR scanning).
- **Wallet runtime (FortWeb).** Already runs KERIpy in the WebView: vaults,
  identifiers, remotes/OOBIs, and (in the Foundation's SEDI work) credentials.
- **KERI API (new).** A small, versioned surface — the same request/response
  shape as Fortoid's native bridge — through which applications ask the wallet to
  act. The wallet maps each application to its own relationship AID.
- **Application sandbox (new).** Each application runs isolated from the wallet
  and from other applications (separate WebView/origin), loaded as a bundle signed
  by its publisher's AID and verified against that publisher's KEL before it runs.

## What exists today

Built and verified on a phone in this fork:

- Wallet-first launch: the app opens straight to the default vault; a fresh
  install shows only *Set up your wallet*.
- Fingerprint unlock per vault, with the passcode encrypted by a
  biometric-gated hardware key; the prompt appears before the runtime finishes
  booting.
- Scan an OOBI QR code to connect to another party.
- A native capability bridge (`native_request`/`native_response`) — the
  template for the KERI API.

## Open questions and risks

- **Sandboxing on Android.** Running third-party applications safely beside the
  wallet (separate WebViews, origins, storage, and bridge permissions).
- **Application distribution and trust.** How bundles are published, signed by a
  publisher AID, discovered (OOBI), updated, and revoked.
- **Key types and hardware.** KERI mostly uses Ed25519; Android secure hardware
  reliably supports P-256. Hardware-backed KERI signing needs a research spike.
- **Recovery and multiple devices.** Pre-rotation and multi-sig across devices
  so losing a phone never loses an identity.
- **Infrastructure.** Witnesses and watchers that ordinary people never have to
  think about.
- **Bridging the existing web.** How authentic-web applications interoperate
  with today's sites during the transition.

## Possible next steps

1. **KERI API v0** for the bundled demo page: "create a relationship AID for this
   app", sign, verify — with a wallet-owned consent prompt.
2. **Sandboxed app host:** load a local application bundle in its own WebView,
   verify its publisher signature, and give it only the KERI API.
3. **Per-application AIDs by default**, with an optional advanced grouping that
   can align with the Foundation's contexts.
4. **Research spike:** hardware-backed KERI signing (P-256 support in KERIpy and
   FortWeb; the phone as one member of a multi-sig group).
