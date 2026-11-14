# Standard credential screens and hand-over between two wallets

> **Date:** 2026-11-14
> **Bead:** fortoid-xn7
> **Status:** presented to the owner in four parts on 2026-11-14 and approved in
> conversation the same day, with two additions the owner raised afterwards (a
> Schemas page, and the computer's wallet as a plain download). At the review
> the owner asked that a credential's type be called what it is, an ACDC
> schema, and this text does. This document awaits the owner's review.

## Goal

A phone and a computer, each running the wallet, with nothing between them.
They become contacts. One issues a credential that a role it has taken on may
issue. The other reviews it and adds it or declines it. The sender sees the
answer.

Every screen is a standard wallet screen. The published definitions supply the
credential schemas and the forms. This is the owner's universal UI: an
application with no workbench of its own is carried out with the wallet's
standard features.

It is also the first step toward a role's warning clearing. The same screens
will carry an appointment once the published definitions say which credential
appoints someone to a role.

## What the owner will see

1. **New builds.** The phone gets a new build and the wallet is set up again:
   the vault made with the earlier builds is not shown (see D2). The Releases
   page has a second download, the wallet for the computer. Unzipped and
   started with one command, it opens in the computer's browser at
   `http://127.0.0.1:8765/`.
2. **Roles on both.** Each wallet opens `mad.seriouscoder.one` and takes on a
   role, for example *membership* on the computer and *patron* on the phone.
3. **Contacts.** On the phone, an identifier's page has *Show*: a code on the
   screen with the same thing as text beside it. On the computer, *Receive*
   opens the camera, reads the code and offers *Add contact*. The other
   direction works the same way, with the phone scanning the computer's screen.
4. **Issue.** On the computer, the *membership* card lists *admission* under
   *Can issue*. *Issue* asks for the recipient (a contact) and shows a form
   drawn from the published definition: valid from, valid until, place. *Send
   credential* shows a cycling code.
5. **Review.** On the phone, *Receive* scans the cycling code and shows its
   progress ("7 of 14"). *Review credential* then says who it is from, which
   schema it follows and which organization publishes that schema, the values,
   and what was checked. The buttons are *Close*, *Decline* and *Add credential*.
6. **Answer.** After *Add credential* or *Decline* the phone shows the answer as
   a code. The computer scans it, and its Activity list says *Accepted* or
   *Declined*.
7. **Lists.** A **Credentials** page has Received, Issued, Activity and
   Schemas. Schemas lists the vault's ACDC schemas by organization and
   publication.
8. **What does not change yet.** A role card cannot say "appointed". It says
   that the organization does not define yet what appoints someone to the role.

## Owner rulings this design applies

- A grant may arrive unsolicited, and the receiver admits it or spurns it.
  There is no self-issued grant as a step (2026-11-14).
- Universal UI: build and improve the wallet's standard screens, with the
  definitions supplying schemas and forms. Custom UIs come later, as wallet
  plugins (2026-11-14).
- Build on the Foundation's draft credential engine; do not write a second one
  (2026-11-14).
- Messages are handed over with no server in the first version. KERI does not
  depend on the carrier, so carriers are interchangeable, and cycling QR codes
  are one of them from the start (2026-11-14).
- The exchange flow and its wording come from the Foundation's Locksmith. Role
  cards and form rules come from the owner's Locksmith fork (2026-11-14).
- The demonstration is on a phone and a computer. The computer runs the same
  wallet in a regular desktop browser, from a download (2026-11-14).
- The schemas page has to show and use many schemas from several sources and
  industries. What a credential's type is, is its ACDC schema: there is no
  separate page or notion of types (2026-11-14).
- The test wallet data on the phone may be lost (2026-11-14).
- Issuance follows the published definitions. Each node keeps its own data.
  Applications first. KERI only (2026-11-11 and 2026-11-12).

## Decisions made here (the owner may overrule any of them)

**D1. A side branch holds the Foundation's draft.** The wallet fork gets a
branch `credentials`: its `dev` plus the Foundation's draft
`keri-foundation/fortweb#42` (read at `2db99e8`) plus this work. `dev` stays as
it is until the Foundation merges the draft. Fortoid pins commits of
`credentials`. The cost is merging again whenever the draft changes.

**D2. The earlier vault is not carried over.** The draft moves the wallet's
storage to a new place and has no way to bring older vaults along. Nothing is
deleted, but the new build cannot open them. The owner accepted this for a
phone that holds test data only.

**D3. The draft's test-only switch is turned on, and said.** The draft refuses
to receive, grant or admit unless a setting that only its tests use is on. The
fork's builds set it, and every credential screen carries the word
"Experimental: for test credentials only". The draft's own word for what it
allows is "synthetic" test credentials, which is what a proof of concept
handles. The Foundation has not released this part of its engine, so nothing
built on it is fit for a credential anyone depends on.

**D4. Three carriers for one message.** A message is shown as one still code
when it fits, as a cycling code when it does not, and always as text that can
be copied. The reader accepts a scanned code or pasted text. A mailbox can be
added underneath later without changing a screen.

**D5. Cycling codes use BBQr frames.** BBQr is public domain, simple and in
wide use, and it needs no library. Every frame has to be read, which costs at
most one more cycle. Fountain-coded frames (Blockchain Commons UR) tolerate a
missed frame, but their libraries are old or unfinished; they can be added as a
further carrier if scanning proves slow.

**D6. A credential handed over is held the KERI library's way.** The library
keeps every exchange message it accepts in the vault's own store and links the
answer to it. The Foundation's draft and its Locksmith hold a received
credential there, and so does this wallet: it adds no store of its own for one.
The credential counts as one of the person's credentials only after *Add
credential*. *Decline* sends the refusal, and the credential is never listed.
Neither the library nor the draft removes a message once received, so a
declined one stays in that store, unlisted. A message whose proof is incomplete
is refused at once: nothing waits for missing parts, because with hand-over
nothing could bring them. The library's escrow, which holds what is not
complete yet, comes into play with a mailbox.

**D7. ACDC schemas are derived in the wallet, by a fixed rule.** The published
gym definitions describe a credential's attributes with a small schema. The
KERI library needs a complete ACDC schema that carries its own SAID. The wallet
builds one from the other by a written rule, `fort-derive/1`, so nothing has to
change on the publishing side for this version. The rule is offered to the MAD side as
a proposal for what its publish step should produce, and is replaced when MAD
publishes its own form.

**D8. A credential always has a recipient.** The person issuing picks a contact.
The published definitions do not say who a fact or an authorization is for, and
the draft engine issues to a named recipient only.

**D9. Shown, not checked.** What a responsibility relies on, and its rules, are
shown on the issue form as text for the person to judge. The wallet does not
link or check them in this version.

**D10. Plain words on the screen.** *Identifier*, *contact*, *credential*,
*schema*, *Send credential*, *Add credential*, *Decline*. The words IPEX, AID,
grant, admit and spurn do not appear. The buttons are the Foundation's
Locksmith's.

**D11. The phone scans with its own scanner.** The Android shell gets a
continuous scanning mode. A web page still gets no camera there. On the
computer the page reads the camera itself, because nothing else can.

**D12. The computer's wallet is a download opened in a regular browser.** A
folder and a start script, served from the computer at a fixed local address.
The Foundation's runtime allows a desktop browser only at a local address. No
bundled browser: that would be a third app shell with its own security work and
signing. A public address with the browser's "install as app" is the cheaper
way to "it just opens", later.

**D13. Three steps, three plans.** Each ends with a build the owner can try:
the base, hand-over with contacts, then schemas and credentials.

## Architecture

### The base

- **Branch.** `seriouscoderone/fortweb` branch `credentials`, started from `dev`
  (`1555227`) merged with the draft (`2db99e8`). A trial merge on 2026-11-14
  conflicted in six files, ten places, and both sides were kept everywhere. It
  exists only in the session's scratch space and is reviewed before it is used.
- **Runtime source.** The draft builds against the Foundation's archive
  `runtime-source-sedi-core-csp-20261107`. The fork's CI and Fortoid's reviewed
  pin in `scripts/build-canonical-fortweb-package.sh` change to it.
- **One engine at a time.** The draft lets only one worker write the wallet's
  storage; a second is refused. One of the fork's tests opens a second engine
  and is rewritten. What the phone does when Android discards the page in the
  background is checked on the phone.
- **The switch.** `fort_synthetic_recipient_testing = true` in the fork's
  `pyscript-ci.toml`, and the runtime verifier accepts that one key with that
  one value.
- **The draft's own tests.** They run as the Foundation runs them. Two of its
  credential tests fail on the draft alone, because they look for a line the
  draft has since changed. They are recorded and left as they are.

### A message

One format for everything that is handed over. A message is a JSON object:

| Member | Meaning |
|---|---|
| `fort` | The text `handover/1`. |
| `kind` | `identifier`, `credential` or `answer`. |
| `cesr` | The signed bytes as base64, exactly as the engine returns them. |
| `evidence` | `credential` only: the proof the receiver needs, as the engine returns it. |
| `schema` | `credential` only: the credential's ACDC schema as text, so the receiver need not have read the source. |
| `name` | `identifier` only, optional: the name the sender suggests for itself, at most 64 characters. |

The JSON text of a message is at most 262,144 bytes. **Packed** means one
marker byte followed by the message: `0x00` and the JSON text, or `0x01` and
the JSON text compressed with raw deflate. The sender uses whichever is
smaller. Unpacking stops, and the message is refused, as soon as more than
262,144 bytes come out, so a small message cannot unfold into a huge one.

A message proves nothing by itself. Everything in it is checked by the engine:
key history, signatures, registry events, the schema against its SAID. A member
that is missing, unknown or of the wrong kind makes the message unreadable.

### Codes and text

- **Text.** The prefix `fortmsg1:` followed by the packed message in base64url
  without padding. This is what *Copy* copies and what the reader accepts when
  pasted.
- **Frames.** The packed message split into BBQr frames
  (<https://github.com/coinkite/BBQr/blob/master/BBQr.md>): the header `B$`,
  encoding `2` (base32), type `B`, the total and the index in base 36, then the
  base32 text with no padding. All frames but the last carry the same number of
  bytes, a multiple of five.
- **Still or cycling.** A message that fits one code of version 20 or lower at
  error-correction level L is one still code, which is one frame. Anything
  larger cycles, with at most 300 bytes in a frame.
- **Too long for codes.** A packed message of more than 48,000 bytes would need
  more than 160 frames. It is offered as text only, and the dialog says why.
- **Cycling.** It starts when the person taps *Start*, shows at most three
  frames a second, can be paused, and shows "frame 4 of 14". When the system
  asks for reduced motion, it does not cycle by itself: the person steps with
  *Next*.
- **Drawing.** The page draws codes itself, from a small library copied into
  the wallet's tree with its licence and a recorded hash. It needs no network
  and no WebAssembly. The plan names the library and the version; the candidate
  is `qr` (MIT or Apache-2.0), which also reads codes.

Measured on the merged engine on 2026-11-14: an identifier is 588 characters as
the engine returns it, an answer 752, and a credential with its proof about
5,800 bytes of JSON. The first two fit a still code. The third needs about
fourteen to twenty frames.

### Reading a message

One reader, *Receive*, with two ways in: scan or paste.

- **On the phone.** A new capability in the Android shell, `qr.scanSequence`,
  opens the shell's own scanner and keeps reading. It returns either one code
  that is not a frame (`{kind: "single", text, truncated}`, cut at 2,048
  characters as `qr.scan` does) or a complete set of frames
  (`{kind: "frames", frames}`, in order). It shows "7 of 14" while it reads.
  The page joins the frames.
- **On the computer.** The page opens the camera, reads codes with the same
  library that draws them, and collects frames by the same rules.
- **The rules for a set of frames.** All frames agree in their first six
  characters. A frame from a different set is ignored and counted. At most 160
  frames. Reading stops after 20 seconds without a new frame and after 120
  seconds in all.
- **What the reader recognises.** A message (`fortmsg1:` text, or frames that
  join into one), or an OOBI address, which opens the existing *Add remote
  identifier* dialog with the address filled in. Anything else is refused with
  a sentence.

The joined bytes are unpacked and handed to the engine. Nothing from a message
is shown before the engine has checked it.

### Schemas

A credential's type is its ACDC schema. The wallet has one notion and one page
for it, as the Foundation's Locksmith has.

The published gym definitions that carry data are of kind `fact` and
`authorization`. Each has an `alias` and a `schema` that describes the
credential's attributes only. A responsibility names the facts it attests and
the authorizations it issues.

**Rule `fort-derive/1`.** From one such definition the wallet builds a complete
ACDC schema:

- The envelope is the one the draft's own test schema describes
  (`playwright/fixtures/default-acdc-schema.json` in the draft), narrowed so
  that the attribute block must be written out in full.
- The attribute block's properties are the library's own members followed by
  the definition's properties, in the definition's order. No other property is
  allowed.
- A property that refers to another served document by SAID is replaced by that
  document's content, without its own identifier, to a depth of four. A cycle
  or a missing document means no schema can be made from the definition.
- The title is the definition's `alias`.
- An annotation names the rule and the definition's SAID.
- The schema's own SAID is computed as the KERI library computes it and stored
  in `$id`.
- A definition whose schema has more than 200 properties in all, or is nested
  more than four levels deep, is refused.

The exact text is fixed by tests: the derived schema of every fixture
definition is kept byte for byte, and a browser test shows the engine accepting
each one.

**Where a schema comes from.** Taking on a role keeps the role's definitions,
as it does today. It now also keeps what their attribute schemas refer to, and
the publication's description, tags and collection. For each fact or
authorization the role may issue, the derived ACDC schema goes into the vault's
schema store, which is the KERI library's own.

**A schema that arrives with a credential.** The message carries the schema.
The wallet checks it against its SAID. If the wallet holds the definition the
annotation names, and deriving from it gives the same SAID, the schema is shown
as published by that organization, in that publication. Otherwise it is shown
as a schema no organization the person has opened publishes. A claim in the
schema alone proves nothing.

**Schemas from elsewhere.** The schema store holds any ACDC schema, whatever
made it. In this version schemas enter it in the two ways above. Adding one
from a file or an address, as the Foundation's Locksmith does, is a later
slice.

### The engine (Python, in the worker)

Two new modules beside `sourcing.py` and `roling.py`. They call the draft's
engine and change as little of it as possible.

- `app/runtime/deriving.py`: rule `fort-derive/1`. Pure Python; it opens
  nothing.
- `app/runtime/granting.py`: the methods below, over the draft's
  `identifiers.export`, `remotes.import`, `registries.*`, `schemas.import`,
  `credentials.*` and `ipex.*`.

| Method | What it does |
|---|---|
| `handover.identifier` | Makes the message for one of the vault's identifiers. |
| `handover.read` | Checks a message and says what it is, and returns what the screen shows. A credential from a contact is received by the draft's engine, which checks it and holds it as the library holds any message. A credential from anyone else is checked without being held. An answer is recorded if it matches a credential this vault sent. |
| `handover.addContact` | Adds an identifier as a contact under a name the person gives: from an identifier message, or from the history carried in a credential's proof. A name another contact already has is refused. |
| `schemas.list` | The schemas in the vault's schema store, each with its organization, publication, tags and collection where a kept definition gives them, the role that may issue under it, and the attributes it describes. It sits beside the draft's `schemas.import` and `schemas.get`. |
| `credentials.send` | Issues a credential under a schema a taken-on role may issue under, to a contact, and returns the message to hand over. Repeating it with the same request id issues nothing twice. |
| `credentials.listIssued`, `credentials.listReceived` | The two lists. Issued is read from the engine's own store, so a credential issued just before a failure still appears and can be shown. Received is read from the wallet's own records without checking every credential again. |
| `grants.accept` | Gives the answer that the credential is taken, which makes it one of the vault's credentials, and returns the answer message. Accepting again returns the same answer. The sender has to be a contact: the draft's engine holds a credential only from one. |
| `grants.decline` | Builds the refusal, links it to the credential as its answer where the engine holds the credential, and returns the answer message. Declining again returns the same answer. |
| `grants.message` | Returns a message again: a credential sent earlier, or an answer given earlier. |
| `activity.list` | Every exchange and its state. |

- **Declining.** The KERI library builds a refusal and accepts one; the draft's
  reader turns it away. `granting.py` adds the two missing pieces: building the
  refusal for a credential, and reading one on the sender's side.
- **Registry.** One registry for each identifier that issues, made the first
  time it issues. A registry for each credential, as the Foundation's Locksmith
  makes, would add two events to the issuer's key history for every credential
  where this adds one, and every credential message carries that whole history
  (see "How far this goes").
- **Identifiers without witnesses.** That is what the wallet makes today and
  what is tested. An identifier with witnesses has a longer history to hand
  over and needs its witnesses to issue; neither is covered here.
- **Dates and times** are entered in local time and stored with their offset.
- **What is stored, and where.** Messages and credentials stay in the engine's
  own stores: the library's exchange store, and the draft's encrypted store of
  received credentials. Whether a received credential awaits an answer, was
  added or was declined is read from the library's own link between a message
  and its answer. The wallet adds one small sub-database, encrypted with the
  vault's keys as the draft encrypts received credentials. For each exchange it
  holds the message this vault produced (a credential it sent, or an answer it
  gave), so that it can be shown again, with the other party, the schema's
  title and the times, so that the lists need not check every credential again.
- **States.** *Waiting for (name)*, *Awaiting your decision*, *Accepted*,
  *Declined*.
- **A vault with no passcode** cannot hold a received credential: the draft
  encrypts them with the vault's keys.

### Screens (TypeScript)

- **Receive.** A button in the app bar on every page. It opens the reader.
- **Show.** A dialog used wherever a message leaves the wallet: the code, *Start*
  and *Pause* when it cycles, the text with *Copy*, and the sentence "Anyone who
  can see this screen can copy what it shows."
- **Identifier page.** *Show*, beside the draft's *Export CESR*.
- **Remote identifiers page.** *Add by scanning or pasting*, beside *Resolve
  OOBI*, *Scan QR code* and the draft's *Import CESR*.
- **Add contact.** The person types the name. A name the sender suggests is
  shown as "They call themselves (name)" with *Use this name*; it is never
  filled in for them. Two contacts cannot have the same name.
- **Roles page.** Each of My roles lists *Can issue* with one entry for every
  fact and authorization the role may issue, and *Issue* on each. In place of
  "Not appointed yet" the card says "This organization does not say yet what
  appoints someone to this role." The four statuses of the owner's fork
  (Available, Pending, Active, Revoked) come with the appointment itself, in a
  later slice.
- **Issue.** The issuer is the identifier the role was taken on with. The
  recipient is chosen from contacts. A form drawn from the schema. Beneath it, as
  text, what the responsibility relies on and its rules. *Send credential*
  opens Show.
- **The form.** Text, text with a choice of values, number, whole number, yes
  or no, date and time, a group for a nested object, and a set of checkboxes
  for a list of choices. A field the wallet cannot draw is shown in place as an
  error naming it. If that field is required, the credential cannot be issued
  and the form says so.
- **Review credential.** From: the contact's name, or "an identifier that is
  not in your contacts" with its prefix. The schema, and who publishes it,
  followed by "(organization) publishes this schema. It does not say who may
  issue under it, so judge this by who it is from." The values. What was
  checked. *Close*, *Decline*, *Add credential*. Closing leaves it in Activity
  as *Awaiting your decision*.
- **A sender who is not a contact.** The review shows everything, and *Decline*
  works: the refusal is built from the message, and nothing of the credential
  is held. *Add credential* first asks the person to name the sender, which
  makes them a contact from the key history the credential came with. Naming
  them is the person's own statement of who they take the sender to be.
- **Credentials page.** Received, Issued, Activity, Schemas, the four entries
  the Foundation's Locksmith has under Credentials.
- **Schemas.** The vault's ACDC schemas, grouped by organization, then
  publication, with the publication's tags and collection, and a search box. A
  schema shows its title, its SAID, the attributes it describes and the role
  that may issue under it. A schema that came with a credential and is
  published by no organization the person has opened is in its own group.
- **Experimental.** "Experimental: for test credentials only" appears on the
  Credentials page, the issue form and the review.

### The Android shell (Fortoid)

- `qr.scanSequence`, as described under "Reading a message", built on the
  scanner library the shell already has. The existing `qr.scan` is unchanged.
- The page still gets no camera.
- The Demo APK workflow also publishes the wallet for the computer to the
  `demo-latest` release, built from the same pinned wallet commit as the APK.
- The capability table in `README.md`, the reviewed runtime-source pin, and the
  agent notes change with it.

### The wallet for the computer

- **What it is.** `fort-wallet-desktop.zip`: the built wallet, a start script
  `start-wallet.py` and a short `README.txt`.
- **Starting it.** `python3 start-wallet.py` serves the folder at
  `http://127.0.0.1:8765/` and opens the browser there. It needs Python 3 and
  nothing else.
- **A fixed address.** A browser keeps a wallet's storage per address. If the
  port is taken the script says so and stops; it does not pick another, because
  another address is an empty wallet.
- **Only this computer.** The script listens on `127.0.0.1` only. It serves
  only files inside its own folder, lists no directories, and answers a request
  only when the request names `127.0.0.1:8765` as its host. A request that
  names `localhost:8765` is sent on to `127.0.0.1:8765`, so there is one
  address and one wallet.
- **Which browser.** The automated tests use Chromium, so Chrome or Edge is the
  recommended browser. Others are not tested. Safari removes a site's storage
  after a week without use, which would remove the vault.
- **Keeping the vault.** When a vault is made, the wallet asks the browser to
  keep its storage (`navigator.storage.persist()`), and Settings says whether
  the browser agreed.
- **One tab.** The draft lets one copy of the engine write at a time. A second
  tab is told that the wallet is already open in another.
- **What the README says.** One tab. A browser profile with no extensions,
  because an extension that may read pages can read a wallet. The same release
  for the phone and the computer. This is a demonstration build.
- **Checking the download.** The release notes give the SHA-256 of the zip.
- **Same build.** The computer package and the APK are built from one pinned
  wallet commit, so two wallets that meet are the same version.

## What the person sees when something is wrong

| Situation | Sentence |
|---|---|
| The code or text is not a message or an OOBI address | "That is not something this wallet can read." |
| A cycling code was not read to the end | "Not all of the code was read. Try again." |
| Frames of two different codes were read | "The code changed while it was being read. Try again." |
| The message is larger than the limit | "That message is larger than the wallet accepts." |
| The camera cannot be opened, on the computer or the phone | "The camera could not be opened. You can paste the text instead." |
| A message is too long to show as codes | "This is too long to show as a code. Copy the text instead." |
| A contact name is already in use | "You already have a contact with that name." |
| The wallet is open in another tab | "The wallet is already open in another tab. Close it there first." |
| The wallet could not save | "The wallet could not save. Close it and open it again." |
| The identifier is one of the vault's own | "That is one of your own identifiers." |
| The identifier is already a contact | "You already have this contact as (name)." |
| Its history conflicts with the contact held | "This identifier's history does not match the contact you have. Nothing was changed." |
| The credential is for an identifier not in this vault | "This credential is for an identifier that is not in this vault." |
| A check fails | "This credential could not be verified. Nothing was kept." |
| The issuer has withdrawn it | "The issuer has withdrawn this credential." |
| It was added before | "You already have this credential." |
| It was declined before | "You declined this credential on (date)." |
| An answer matches nothing this vault sent | "This answer does not match a credential you sent." |
| The vault has no passcode | "This vault has no passcode, so it cannot hold credentials." |
| A required field cannot be drawn | "This schema has a field the wallet cannot show yet: (name). A credential cannot be issued under it from this wallet." |
| No schema can be made from a definition | "The wallet cannot make a credential schema from this definition yet." |
| Issuing failed | "The credential was not issued." |

## Privacy and security

- **No network.** None of these screens sends or fetches anything. The only
  network use stays the reading of a source.
- **Whatever is read is hostile until checked.** Limits on frames, bytes and
  time. The whole message is checked by the engine before any of it is shown.
  Text from a message or a schema (titles, property names, choices, the
  suggested name) passes the rule that source text passes, and is never placed
  on a page except as text.
- **A suggested name is a suggestion.** The review never shows the sender's own
  name for itself as if it were known. It shows the contact's name the person
  gave, or the identifier's prefix. When a contact is added, the suggested name
  is shown and never filled in, and no two contacts share a name, so a sender
  cannot slip into the place of someone the person already knows.
- **The scanning screen on the phone** belongs to the app alone: no other app
  can start it or receive what it reads.
- **"Published by" is checked, not claimed.** It is shown only when the wallet
  derives the same schema from a definition it read and kept itself.
- **The publisher of a schema is not the issuer of a credential.** Anyone who
  takes on a role can issue under its schemas until the published data says who
  is appointed. The review says so in one sentence beneath the schema. When
  appointments exist, that sentence gives way to a checked statement of whether
  the sender holds one.
- **Consent.** A credential becomes one of the person's credentials only on
  *Add credential*. Until then it is a message the vault has received, held as
  the library holds every message. The sender learns the answer only when the
  person shows it.
- **What a declined credential leaves.** From a contact: the received message
  in the library's store and the draft's encrypted record of it, neither listed
  among the person's credentials, and the refusal that was sent. From anyone
  else: the refusal only. Erasing a declined credential is a later slice.
- **The screen.** A code can be copied by anyone who sees it. The Show dialog
  says so. Hand-over by code is for two people in one place.
- **Flicker.** At most three frames a second, started by a tap, with a pause
  and a step-by-step mode for reduced motion.
- **The clipboard.** Copied text stays on the clipboard until something else is
  copied. The dialog says so.
- **Repeats.** Reading the same credential or answer again changes nothing and
  says what is already known.
- **Cameras.** On the phone the page gets no camera. On the computer the camera
  is open only while the reader shows it, and frames go nowhere but the reader.
- **Experimental.** The Foundation has not released what the switch turns on.
  The screens say so.

## Testing

- **Rule and packing.** Unit tests for `deriving.py` on plain CPython, with the
  derived schema of every fixture definition kept byte for byte and every
  refusal covered. Tests for packing, frames and the frame rules on the built
  JavaScript, from one file of vectors that the Android tests read too.
- **Two wallets in the browser.** Playwright with two separate browser
  contexts: contacts both ways; issue, review and add; decline; the answer read
  by the sender; the same message read twice; a message altered in transit; a
  credential for an identifier in another vault; a schema no kept definition
  matches; a definition no schema can be made from; a form with a field that
  cannot be drawn; a credential from a sender who is not a contact; accepting
  twice; a refusal read again after the vault was closed and opened; a message
  that unfolds past the limit; a message too long for codes.
- **Codes.** Sizes from one frame up to the limit are drawn and read back. The
  reader's camera path runs against a stream made from a canvas. Cycling,
  pausing and the reduced-motion mode are tested on the Show dialog.
- **The phone.** JVM tests for the frame rules and the capability's limits. The
  camera cannot be tested without a device, so the owner checks scanning a
  still and a cycling code from the computer's screen.
- **The computer package.** A test starts the script against the built package
  and loads the wallet from it. Tests for the script: a request for a file
  outside the folder, a request that names another host, a port already taken.
- **Every pin.** All of Fortoid's workflows on `dev`, with the start-up proof
  among them, after each pin.
- **Suite rules.** Each new browser test file is named in `package.json` and
  stays under about 210 seconds.

## Phases

Each phase has its own plan and ends with a build the owner can try.

1. **The base.** The `credentials` branch with the draft merged and reviewed.
   The fork's suites pass on it. The switch is on. Fortoid pins it with the new
   runtime source, and every workflow is green. `demo-latest` carries the APK
   and the wallet for the computer. The owner sets up a wallet on both and
   reads the source on both.
2. **Hand-over and contacts.** Messages, text, still and cycling codes, the
   reader on the computer, `qr.scanSequence` on the phone, *Show* on an
   identifier and *Add by scanning or pasting*. The owner makes the phone and
   the computer contacts of each other.
3. **Schemas and credentials.** The rule, the Schemas page, *Can issue* on a role,
   the issue form, *Review credential* with *Add credential* and *Decline*, the
   answer, Activity and the two lists. The owner sends a credential from one to
   the other and declines a second one.

## What can go wrong in this work

- **The draft changes or is merged in another form.** Then the branch is merged
  again. The two new modules keep the fork's changes to the draft's own files
  small.
- **Start-up time.** The draft writes to storage after every request. Fortoid's
  start-up proof allows 30 seconds on a slow emulator, and is checked after the
  first pin.
- **A laptop camera reading a phone.** Still codes are the easy case. For the
  demonstration the computer shows the cycling code and the phone, with the
  better camera, reads it.
- **Adding a credential gets slower.** The draft checks every credential the
  vault already holds each time it stores one. With a handful this is not felt.
  The plan measures it on the phone with twenty.
- **Whose work it is.** The draft is merged as a merge, not squashed, so its
  commits keep their author.

## How far this goes

The draft sends the issuer's whole history with every credential: every event
of the issuer's key history, and every event of the registry. Each credential
an identifier issues adds one event to both. So each credential makes that
identifier's next message longer, by about 700 bytes when packed, which is two
or three frames.

- The first credential from an identifier is about fourteen to twenty frames,
  five to seven seconds a cycle.
- After fifteen to twenty credentials a cycle takes about twenty seconds.
- After about sixty the message passes the limit for codes and can only be
  handed over as text.
- The draft itself accepts at most 256 events in a registry.

Nothing in this design changes that, because it is how the draft checks a
credential. A role taken on with its own identifier has its own history, which
keeps histories short. The real remedies are later work: sending only what the
receiver does not hold, which needs the Foundation's engine to check a history
from where the receiver's copy ends, or a mailbox, where length matters less.

## Open with the MAD side

Three questions are filed on the private MAD repository and mirrored here as
beads: how an organization's owner appoints staff (fortoid-yos), what accepting
and declining an appointment mean (fortoid-57t), and what a wallet reads from a
published role to know which appointment it needs (fortoid-4w8). This design
needs none of the answers. The rule `fort-derive/1` goes to the third as a
proposal once this document is approved.

## Later slices (not in this design)

- A role's status from its appointment: Available, Pending, Active, Revoked.
  The owner's fork has the rule for Active: a credential under the right schema,
  from a trusted issuer, not withdrawn, fully verified. It also has the rule
  that a credential the person asked for is added without a second question.
- Asking for a credential, and offers and agreements before one is sent.
- Showing a held credential to a third party.
- Withdrawing a credential, and telling the holder.
- Linking and checking what a responsibility relies on, and its rules.
- A mailbox, so a credential arrives by itself. The owner's fork has designs
  for one.
- Custom screens as wallet plugins, with a safety model fit for a phone.
- A public address for the wallet, with "install as app".
- Frames that tolerate a missed frame.
- Hand-over that only the receiver can read, for use at a distance.
- Erasing a received credential the person declined.
- Exchanging files with the Foundation's Locksmith.
- Adding a schema from a file or an address, as the Foundation's Locksmith
  does.
- iOS.

## Where the patterns come from

All public.

- The Foundation's Locksmith, `keri-foundation/locksmith` at `c97c7e3`: the
  review dialog and its buttons (`ui/vault/credentials/received/accept_grant.py`),
  the Activity list (`ui/vault/credentials/activity.py`), the form drawn from a
  schema (`ui/vault/credentials/issued/issue.py`), what is kept before consent
  and after a refusal (`core/ipexing.py`, `tests/test_credentialing.py`).
- The owner's fork, `seriouscoderone/locksmith` branch `development` at
  `0d68fad`: the form builder and its visible error for a field it cannot draw
  (`ui/onboarding/form_builder.py`), the role statuses
  (`ui/onboarding/role_states.py`), the rule that ties a role to a credential
  (`plugins/credential_gate.py`), an identifier with its history as one piece
  of text (`peer/cesr_blob.py`), a requested credential added without asking
  (`core/inbound_watch.py`).
- The owner's branch `feat/designer-plugin-spec` at `8f19a01`: one shell that
  renders every application through standard patterns, and protocol words kept
  off the screen
  (`docs/superpowers/specs/2026-05-09-micro-app-template-authoring-and-data-model.md`).
- The Foundation's draft, `keri-foundation/fortweb#42` at `2db99e8`: the engine
  this design calls.
- BBQr, <https://github.com/coinkite/BBQr/blob/master/BBQr.md>.
