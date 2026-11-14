# First browser demonstration: read a source, list its roles, take one on

> **Date:** 2026-11-12
> **Bead:** fortoid-bx6
> **Status:** approved by the owner on 2026-11-12 with one change (D3). The owner
> also said yes to the header (D1) the same day. The header has been live on
> both deployed sources since 2026-11-13. Changed after the whole-feature
> review of 2026-11-13: one sentence where a browser cannot tell an unreachable
> host from one that does not allow the read, limits on what a read and a role
> may cost, a stricter text rule, and My roles read from the records alone.
> Built and shipped in the `demo-latest` APK on 2026-11-14 (wallet commit
> `1555227`); the text below describes what was built. Shown on the owner's
> phone the same day: the wallet read `mad.seriouscoder.one` and a role was
> taken on, once the sources' hosting sent the header to a request without
> cache too.

## Goal

On the owner's Android phone: type a web address into the wallet, see what that
organization publishes, and take on one of its roles. This joins three things
that exist separately today: the wallet (FortWeb inside Fortoid), the owner's
publication format (`mad-discovery`), and the owner's rulings on roles.

## What the owner will see

1. After unlocking, the wallet opens on a new **Roles** page: an address box,
   and "My roles" (empty at first). No roles can be listed before an address
   has been opened: roles come from what organizations publish.
2. Typing `mad.seriouscoder.one` and pressing Open reads that source. The wallet
   shows the host, "3 publications, 26 definitions checked", and the
   publications by name.
3. Opening "Drop-in takes a class" lists its roles: cashier, front-desk,
   instructor, patron, recorder. Each role shows its responsibilities, with what
   each relies on and what it produces.
4. "Take on" asks which identifier to use, in the style of the AWS console:
   create a new one or select an existing one. The role then appears under My
   roles with the warning "Not appointed yet".
5. My roles works with no network. Removing a role deletes the local record.
6. What was last read stays on the Roles page until another address is opened
   or the wallet is locked. It is kept in memory only.

## Owner rulings this design applies

- Organizations publish definitions from their domains; publishing there is the
  authorization, and nothing has to be signed (rulings 3, 4).
- Taking on a role is private. Nobody is told (ruling 5).
- Each node keeps its own data in its own local database (ruling 11).
- Definitions are published in whatever format the designer project settles on
  (ruling 12), found through `mad-discovery` (ruling 15).
- A role is a collection of credentials held by an AID. Missing credentials do
  not block: the wallet warns, and the AID acquires them later (rulings 13, 14).
- Applications first: the wallet, vaults and identifiers stay in the background.
- The wallet is a node. It takes on responsibilities as an identity (owner,
  2026-11-12).
- The protocol is open, in the spirit of Bitcoin's (owner, 2026-11-12). Who may
  issue a credential is set by the published definitions: some credentials are
  self-issuing, so their holder issues them to itself, and others have to be
  issued by someone else. Nobody can simply appoint themselves to any role. A
  gym owner's own credential may be self-issuing; a cashier's comes from the
  owner. A credential matters only to those who accept its issuer as an
  authority, so trust comes from whom people already know and accept, and a
  company first has to establish its own authority.

## Decisions made here (the owner may overrule any of them)

**D1. How the wallet reads a source: the source lets browsers read it.**
A source sends one response header (`Access-Control-Allow-Origin: *`) on its
source document and definitions. The wallet then reads it with the web's own
fetch, on Android now and on iOS or the web later with no extra code.
Consequences: `mad-discovery`'s design changes one statement (it says today that
a browser is not expected to read a source), and the owner adds the header to
the two CloudFront distributions.
Alternative considered: the phone app fetches natively. It needs no hosting
change, but it is Android-only until ported, and a native fetcher must carry its
own protection against being pointed at private network addresses. With the
header, a host that has not opted in simply cannot be read.

**D2. What the warning says: "Not appointed yet", once per role taken on.**
The published definitions name each role and what its responsibilities rely on.
They do not say which grant makes someone a cashier. So the wallet cannot check
for a specific credential yet, and it says so plainly. What a responsibility
relies on (for the instructor: an enrollment and an attendance) is shown as
information, not as a warning, because those are presented by other parties.
In this slice the warning cannot clear: the wallet does not hold grants yet.

**D3. Which identifier: the person chooses, create new or select existing.**
Owner, 2026-11-12: like the AWS console wherever a role is needed, offer
"create new" and "select from existing" side by side. A new AID keeps roles
from being linked to each other; an existing one keeps things simple. When the
vault has no identifier yet, only "create new" is offered.

**D4. Where the reading and checking run: in the wallet's Python engine.** It
already ships Blake3. The rules are `mad-discovery`'s, and the tests use
`mad-discovery`'s own fixtures so the two stay in agreement. `mad-discovery`'s
reader code stays Node-only for now.

**D5. One source per read.** Introductions and catalogs are shown as counts and
are not followed.

**D6. Roles is the first page after unlock.** Identifiers, Remotes and Settings
stay in the menu.

## Architecture

### `mad-discovery` (separate repository, small change)

- `docs/design.md`: a source that wants browser wallets to read it sends
  `Access-Control-Allow-Origin: *` on `source.json` and `definitions/*`.
- `verify` reports whether a source sends the header, as information.
- `deploy/README.md`: the CloudFront step for the two live sources.

### Wallet runtime (Python, in the worker)

`sourcing.py`, reading and checking:

- `normalize_address(text)` accepts `host`, `host/path` or a full address and
  returns the canonical `https://host/.../source.json`. Rules as in
  `mad-discovery`: https only, at most 512 characters, no user name, password,
  query, fragment or port, no IP literal, no `localhost`. The wallet is
  stricter in two ways, because the browser's fetch parses the address and the
  wallet cannot check what a name resolves to: a host needs at least two labels
  and a last label that starts with a letter, and a path may hold only letters,
  digits, `.`, `_`, `~`, `-` and `/`, with no empty, `.` or `..` segment.
- `read_source(address)` fetches the source document, validates it, fetches each
  publication's target and the definitions that target refers to, and checks
  every one against its SAID.
- Document rules as in `mad-discovery`: `source` must equal the address fetched;
  a known member of the wrong type discards the document; unknown members are
  ignored; an entry that fails its rule is dropped and counted; publication
  limits are name 200, description 2000, 32 tags of 64, collection 200
  characters, with no control characters.
- SAID check as in `mad-discovery`: the SAID is present in the served bytes;
  every occurrence becomes 44 `#`; the Blake3-256 digest of that, encoded as a
  44-character `E` SAID, equals the SAID; putting the SAID back gives the served
  bytes.
- Limits: one file at most 1,000,000 bytes; one request 10 s; at most 50
  definitions per source; one read 60 s; JSON nesting at most 64; and, for the
  wallet only, at most 4,000,000 bytes per read in total, so a hostile source
  cannot exhaust a phone's memory.
- Limits on what a read costs, for the wallet only (added after the
  whole-feature review, which showed a source inside every limit above freezing
  the wallet): the first 50 publications of a source are read and the rest are
  counted; each definition is parsed once per read; a read's answer holds at
  most 5,000 items (roles, responsibilities and what they refer to), and a
  publication whose roles do not fit gets the "more than the wallet reads"
  sentence; the read checks its 60 s and lets other requests in between
  publications.
- Fetch rules: https only, no credentials, no referrer, redirects refused, no
  cache, status 200 with a JSON content type (`application/json` or a type
  ending `+json`, as in `mad-discovery`), body read under the byte limit. The
  wallet makes no other request: when a read fails it does not ask again in
  another way to find out why, because such a request cannot refuse redirects
  and would let a source send the wallet to other hosts.
- Because the wallet asks without the browser's cache, the browser adds
  `Cache-Control: no-cache` and `Pragma: no-cache` to every request. A source
  has to send the header on such a request too. CloudFront's managed SimpleCORS
  policy does not: it leaves the header out when a request carries either,
  which is why the first try on the phone (2026-11-14) could not read the
  deployed sources although they sent the header to a plain request.
- Text rules, for the wallet only: JSON whose strings include a lone surrogate
  is refused, because such text cannot be stored or sent as UTF-8
  (`mad-discovery` Ruling 12 now refuses it in a publication's strings too; the
  wallet also refuses it inside a definition). A string shown to the person (a
  publication's name, description, tags and collection; a role, alias or name
  from a definition) is used only if it has no control character and no
  character that reverses the direction of text or does not show, and a name
  from a definition only if it is at most 200 characters; a role needs a name
  that shows. Which characters those are is `mad-discovery`'s Ruling 13 (the
  owner, 2026-11-12): the bidirectional controls, the zero-width space, the
  word joiner and its neighbours, the byte-order mark, the soft hyphen, the
  Mongolian vowel separator and the tag characters. The joiners that emoji
  sequences and Persian need are allowed. `mad-discovery` applies the rule to a
  publication's strings; the wallet applies the same rule there and to the
  names it takes from a definition.
- Other differences from `mad-discovery`, none of them a loosening: a host is
  ASCII letters, digits and hyphens (a name in its own script, or with `_`, is
  refused); bytes that are not valid UTF-8 are refused; a number of more than
  4,300 digits refuses the file; introductions and catalogs are counted as
  listed, without checking each entry; and a browser asks for and undoes
  transfer encodings, so the bytes checked are the decoded ones.

Interpreting definitions is kept in one function, `roles_of_selection`, because
the format (`kind`, `rrs`, `role`, `rely_on`, `attest`, `outcome`) is a
prototype and will change. A selection's `rrs` are grouped by `role`; each gives
its alias, what it relies on, what it attests and its outcome. Unknown kinds and
members are ignored.

`roling.py`, the roles a vault has taken on:

- Stored in the vault's own database, next to its KELs: one record per role
  (source address, publication target and name, role, its responsibilities as
  they were read, identifier, when taken on) and the served bytes of each
  definition by SAID.
- Taking on a role reads nothing from the network: it stores what was just read
  and checked.
- My roles is read from the records alone, so showing it never depends on how
  large the stored definitions are. A role is kept only if it is small enough:
  at most 200 items in its responsibilities and 200,000 bytes of definitions.

Runtime methods: `sources.read`, `roles.list`, `roles.takeOn`, `roles.remove`.

### Wallet UI (TypeScript)

- Route `roles` inside a vault, the landing page after unlock.
- Roles page (address box, My roles), source view (publications), publication
  view (roles and responsibilities), take-on dialog (new or existing
  identifier).
- Everything that comes from a source is shown as plain text. Nothing from a
  source becomes markup or a link.
- The host is never on the same line as text from a source: under My roles it
  has a line of its own, before the publication's name, so wording or
  characters in a name cannot hide or imitate it.
- In the take-on dialog only a fresh press of Enter takes on a role; a key
  that is being held does not.

### Fortoid

No native change. The new wallet commit is pinned and the APK builds from `dev`.

## What the person sees when something is wrong

| Situation | Message |
|---|---|
| Address is not a valid source address | "That is not an address the wallet can open." |
| Host does not answer in time | "Could not reach <host>." |
| Host cannot be reached, or does not allow browsers to read it (a browser does not say which) | "Could not read <host>: it cannot be reached, or it does not allow wallets to read it yet." |
| Source document invalid | "<host> did not return a valid source." |
| A definition fails its SAID check | The publication is refused: "A definition from <host> does not match its identifier." |
| A definition cannot be read (not served, too slow, too large, not JSON) | The publication is refused: "A definition from <host> could not be read." |
| A limit is exceeded | "<host> publishes more than the wallet reads in one go." |
| A source lists more than 50 publications | The first 50 are shown, with "<n> more publications are not shown: <host> publishes more than the wallet reads in one go." |
| A role is too large to keep | "<host> publishes more than the wallet keeps for one role." |
| Identifier name already used | A numbered name is offered. |
| A role cannot be stored | "The wallet could not keep that role." Nothing is changed, and no identifier is made. |
| My roles cannot be read | "My roles could not be read." The address box still works. |
| A role cannot be removed | "The wallet could not remove that role." |

## Privacy and security

- Reading a source contacts its host, and only its host, as visiting a website
  does. The host learns the phone's network address and the time, and what any
  browser request carries: the wallet's origin, the browser's name and version,
  and the preferred languages. No identifier, credential, cookie or referrer is
  sent.
- Taking on a role and viewing My roles send nothing.
- A source is an unsigned offering. The host name is shown prominently, because
  the domain is the authority. If a source states who runs it (`by`, `oobi`),
  that is shown as stated and marked unverified; verifying it is a later slice.
- Nothing fetched is executed or rendered as markup, and every list and file has
  a limit.
- A host that has not opted in with the header cannot be read by the wallet,
  which also keeps it from being pointed at private network hosts. The wallet
  follows no redirect and makes no request that could follow one.

## Testing

- Python unit tests: address rules, document rules, the SAID check against
  `mad-discovery`'s fixtures (passing and failing), role derivation from the gym
  test data, and the roles store.
- Browser tests with the real runtime and a local fixture source: read and list;
  take on a role with a new identifier; relaunch with the source stopped and see
  the role still listed; remove it. Negative cases: a source without the header,
  a tampered definition, an oversized file.
- All existing checks on `dev`, including the Android startup proof.
- On the phone: the owner types the address. This needs the live source to send
  the header.

## Phases

1. `mad-discovery`: design text, `verify` report, deploy note. The owner adds
   the header to CloudFront.
2. Wallet runtime: read and check a source; `sources.read`; unit tests.
3. Wallet UI: Roles page, source view, publication view.
4. Take on a role: store, My roles, the warning; browser tests.
5. Pin in Fortoid, build the APK, check on the phone.

## Later slices (not in this design)

- Holding grants, so the warning can clear, and the processes for acquiring
  them. First step: a credential the definitions mark as self-issuing, issued
  by its holder to itself (an owner's, for example), and then one that must
  come from someone else (a cashier's, from that owner). Whether anyone accepts
  the owner as an authority is each receiver's own decision. The published test
  definitions say which responsibility issues each authorization (`issue`), but
  carry no mark for self-issuing yet.
- Plainer examples to explain the idea, in rising richness (owner,
  2026-11-12): an online shop, where a person is only a customer; a
  ride-hailing service, where a person can be a customer or a provider; a
  network-marketing company, with many kinds of actor.
- Verifying who runs a source through its OOBI.
- A browser test that reads from a real local HTTPS server, because a routed
  stand-in cannot show what a browser does with redirects and cross-origin
  rules.
- Following introductions and catalogs.
- Performing a responsibility and exchanging with other actors.
- iOS, which first needs the Foundation's shell to allow the wallet network
  access.
- Definitions used as credential schemas, and links between versions of a
  definition.
