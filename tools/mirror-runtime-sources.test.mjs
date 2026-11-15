// Tests for scripts/mirror-runtime-sources.sh, the copying logic of the workflow runtime-source-mirror.yml.
//
// The script runs with stand-ins for gh and curl put first on PATH. The stand-ins keep the releases of a made-up
// repository as folders under a state folder, serve the "upstream" address and the copies' public addresses from
// files there, and log every call as one JSON line. Nothing here talks to GitHub.
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { chmodSync, copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '..');
const SCRIPT = path.join(ROOT, 'scripts', 'mirror-runtime-sources.sh');
const REPOSITORY = 'example-owner/example-repo';
const COMMIT = '0123456789abcdef0123456789abcdef01234567';
const TOKEN = 'token-that-must-never-be-printed';
const TAG = 'runtime-source-example-1';
const UPSTREAM = `https://github.com/example-foundation/fortweb/releases/download/${TAG}/runtime-source.tar.gz`;
const PUBLIC = `https://github.com/${REPOSITORY}/releases/download/${TAG}/runtime-source.tar.gz`;
const BYTES = 'made-up runtime source archive\n';
const SHA = createHash('sha256').update(BYTES).digest('hex');

const GH = `#!/usr/bin/env node
const fs = require('node:fs');
const path = require('node:path');
const state = process.env.STANDIN_STATE;
const args = process.argv.slice(2);
fs.appendFileSync(path.join(state, 'calls.log'), JSON.stringify(['gh', ...args]) + '\\n');
// The repository this call acts on, as gh resolves it: --repo or -R, then GH_REPO, otherwise the clone's remotes
// (recorded as null: a run must never fall back to them).
const flag = args.findIndex((arg) => arg === '--repo' || arg === '-R');
const repository = flag >= 0 ? args[flag + 1] : (process.env.GH_REPO || null);
fs.appendFileSync(path.join(state, 'repositories.log'), JSON.stringify(repository) + '\\n');
const fail = (message, status = 1) => { process.stderr.write(message + '\\n'); process.exit(status); };
if (args[0] !== 'release') fail('stand-in gh: unknown command ' + args.join(' '), 2);
const [, command, tag, ...rest] = args;
const failure = path.join(state, 'fail-' + command);
if (fs.existsSync(failure)) fail(fs.readFileSync(failure, 'utf8').trim());
const release = path.join(state, 'releases', tag);
const option = (name) => { const at = rest.indexOf(name); return at < 0 ? undefined : rest[at + 1]; };
if (command === 'view') {
  if (!fs.existsSync(release)) fail('release not found');
  if (JSON.stringify(rest) === JSON.stringify(['--json', 'assets', '--jq', '.assets[].name'])) {
    for (const name of fs.readdirSync(release)) process.stdout.write(name + '\\n');
  } else {
    process.stdout.write('title:\\tRuntime source copy: ' + tag + '\\n');
  }
} else if (command === 'download') {
  if (!fs.existsSync(release)) fail('release not found');
  const file = path.join(release, option('--pattern'));
  if (!fs.existsSync(file)) fail('no assets match the file pattern');
  fs.copyFileSync(file, path.join(option('--dir'), option('--pattern')));
} else if (command === 'create') {
  if (fs.existsSync(release)) fail('a release with the same tag name already exists: ' + tag);
  fs.mkdirSync(release, { recursive: true });
  fs.copyFileSync(rest[0], path.join(release, path.basename(rest[0])));
} else if (command === 'upload') {
  if (!fs.existsSync(release)) fail('release not found');
  const target = path.join(release, path.basename(rest[0]));
  if (fs.existsSync(target) && !rest.includes('--clobber')) fail('asset under the same name already exists');
  fs.copyFileSync(rest[0], target);
} else {
  fail('stand-in gh: unknown command ' + args.join(' '), 2);
}
`;

const CURL = `#!/usr/bin/env node
const fs = require('node:fs');
const path = require('node:path');
const state = process.env.STANDIN_STATE;
const args = process.argv.slice(2);
fs.appendFileSync(path.join(state, 'calls.log'), JSON.stringify(['curl', ...args]) + '\\n');
const out = args[args.indexOf('--output') + 1];
const url = args[args.length - 1];
// As GitHub answers: a missing download is a 404, which only --fail turns into a failure (otherwise curl saves the
// error page and exits 0); a download that exists is a redirect, which only --location follows (otherwise curl saves
// the redirect page and exits 0).
const missing = () => {
  if (!args.includes('--fail')) { fs.writeFileSync(out, 'Not Found'); process.exit(0); }
  process.stderr.write('curl: (22) The requested URL returned error: 404\\n');
  process.exit(22);
};
const serve = (file) => {
  if (!args.includes('--location')) { fs.writeFileSync(out, '<html><body>You are being redirected.</body></html>'); return; }
  fs.copyFileSync(file, out);
};
const prefix = 'https://github.com/' + process.env.GITHUB_REPOSITORY + '/releases/download/';
if (url.startsWith(prefix)) {
  const [tag, name] = url.slice(prefix.length).split('/');
  const misses = path.join(state, 'public-misses');
  if (fs.existsSync(misses)) {
    const left = Number(fs.readFileSync(misses, 'utf8'));
    if (left > 0) { fs.writeFileSync(misses, String(left - 1)); missing(); }
  }
  const override = path.join(state, 'public-bytes');
  const file = fs.existsSync(override) ? override : path.join(state, 'releases', tag, name);
  if (!fs.existsSync(file)) missing();
  serve(file);
} else if (url === fs.readFileSync(path.join(state, 'upstream-url'), 'utf8')) {
  serve(path.join(state, 'upstream-bytes'));
} else {
  missing();
}
`;

/** A fresh made-up world: stand-ins on PATH, a list of one source, the upstream archive and no releases. With
 * `listTool`, the script runs from a copy in a scratch tree whose tools/list-runtime-source-mirrors.mjs is that text. */
function world({ listTool } = {}) {
  const dir = mkdtempSync(path.join(tmpdir(), 'fortoid-mirror-'));
  let script = SCRIPT;
  if (listTool !== undefined) {
    const tree = path.join(dir, 'tree');
    mkdirSync(path.join(tree, 'scripts'), { recursive: true });
    mkdirSync(path.join(tree, 'tools'));
    script = path.join(tree, 'scripts', path.basename(SCRIPT));
    copyFileSync(SCRIPT, script);
    writeFileSync(path.join(tree, 'tools', 'list-runtime-source-mirrors.mjs'), listTool);
  }
  const bin = path.join(dir, 'bin');
  const state = path.join(dir, 'state');
  mkdirSync(bin);
  mkdirSync(path.join(state, 'releases'), { recursive: true });
  for (const [name, text] of [['gh', GH], ['curl', CURL]]) {
    writeFileSync(path.join(bin, name), text);
    chmodSync(path.join(bin, name), 0o755);
  }
  const list = path.join(dir, 'list.json');
  writeFileSync(list, JSON.stringify({
    schema: 'fortoid.runtime-source-mirror.v1',
    sources: [{ tag: TAG, upstream: UPSTREAM, sha256: SHA }],
  }));
  writeFileSync(path.join(state, 'upstream-url'), UPSTREAM);
  writeFileSync(path.join(state, 'upstream-bytes'), BYTES);
  writeFileSync(path.join(state, 'calls.log'), '');
  writeFileSync(path.join(state, 'repositories.log'), '');
  const release = (files = {}) => {
    mkdirSync(path.join(state, 'releases', TAG));
    for (const [name, text] of Object.entries(files)) writeFileSync(path.join(state, 'releases', TAG, name), text);
  };
  const run = (extraEnv = {}) => {
    const result = spawnSync('bash', [script], {
      cwd: dir,
      encoding: 'utf8',
      env: {
        PATH: `${bin}:${path.dirname(process.execPath)}:/usr/bin:/bin`,
        HOME: dir,
        TMPDIR: dir,
        GITHUB_REPOSITORY: REPOSITORY,
        GITHUB_SHA: COMMIT,
        GH_TOKEN: TOKEN,
        MIRROR_LIST: list,
        MIRROR_RETRY_PAUSE: '0',
        STANDIN_STATE: state,
        ...extraEnv,
      },
    });
    const output = result.stdout + result.stderr;
    assert.ok(!output.includes(TOKEN), 'the token is never printed');
    const repositories = readFileSync(path.join(state, 'repositories.log'), 'utf8').split('\n').filter(Boolean)
      .map((line) => JSON.parse(line));
    for (const repository of repositories) {
      assert.equal(repository, REPOSITORY, 'every gh call acts on GITHUB_REPOSITORY and nothing else');
    }
    return { status: result.status, output };
  };
  const calls = () => readFileSync(path.join(state, 'calls.log'), 'utf8').split('\n').filter(Boolean)
    .map((line) => JSON.parse(line));
  const ghCalls = (command) => calls().filter((call) => call[0] === 'gh' && call[2] === command);
  const asset = () => {
    const file = path.join(state, 'releases', TAG, 'runtime-source.tar.gz');
    return existsSync(file) ? readFileSync(file, 'utf8') : undefined;
  };
  const done = () => rmSync(dir, { recursive: true, force: true });
  return { state, release, run, calls, ghCalls, asset, done };
}

const GH_CALLS = ['view', 'download', 'create', 'upload'];

function inWorld(use, options) {
  const w = world(options);
  try {
    use(w);
    // The script never deletes or edits: the only gh calls it makes are these four.
    for (const call of w.calls().filter((item) => item[0] === 'gh')) {
      assert.ok(call[1] === 'release' && GH_CALLS.includes(call[2]), `a gh call the script never makes: ${JSON.stringify(call)}`);
    }
  } finally {
    w.done();
  }
}

function assertNothingWritten(w) {
  assert.deepEqual(w.ghCalls('create'), []);
  assert.deepEqual(w.ghCalls('upload'), []);
  for (const call of w.calls()) assert.ok(!call.includes('--clobber'), JSON.stringify(call));
}

test('no release yet: the checked archive becomes a new prerelease', () => inWorld((w) => {
  const { status, output } = w.run();
  assert.equal(status, 0, output);
  assert.ok(w.calls().some((call) => call[0] === 'curl' && call.at(-1) === UPSTREAM), 'fetched from upstream');
  const creates = w.ghCalls('create');
  assert.equal(creates.length, 1);
  const [, , , tag, file, ...flags] = creates[0];
  assert.equal(tag, TAG);
  assert.equal(path.basename(file), 'runtime-source.tar.gz');
  assert.deepEqual(flags, [
    '--prerelease',
    '--target', COMMIT,
    '--title', `Runtime source copy: ${TAG}`,
    '--notes', `A copy of the runtime source the wallet is built from, taken from ${UPSTREAM} and checked against `
      + `SHA-256 ${SHA}. The builds of this repository read this copy.`,
  ]);
  assert.equal(w.asset(), BYTES);
  assert.deepEqual(w.ghCalls('upload'), []);
  assert.match(output, new RegExp(`^copy made: ${TAG}$`, 'm'));
}));

test('a correct published copy: nothing is created or uploaded', () => inWorld((w) => {
  w.release({ 'runtime-source.tar.gz': BYTES });
  const { status, output } = w.run();
  assert.equal(status, 0, output);
  assertNothingWritten(w);
  assert.ok(!w.calls().some((call) => call[0] === 'curl' && call.at(-1) === UPSTREAM), 'upstream not fetched');
  assert.match(output, new RegExp(`^copy present and correct: ${TAG}$`, 'm'));
}));

test('a copy whose bytes do not match the digest fails and is not replaced', () => inWorld((w) => {
  w.release({ 'runtime-source.tar.gz': 'other bytes\n' });
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assertNothingWritten(w);
  assert.equal(w.asset(), 'other bytes\n');
  assert.match(output, /does not match its digest/);
}));

test('a release without the file: the checked archive is uploaded to it', () => inWorld((w) => {
  w.release();
  const { status, output } = w.run();
  assert.equal(status, 0, output);
  assert.deepEqual(w.ghCalls('create'), []);
  const uploads = w.ghCalls('upload');
  assert.equal(uploads.length, 1);
  assert.equal(uploads[0][3], TAG);
  assert.equal(path.basename(uploads[0][4]), 'runtime-source.tar.gz');
  assert.equal(uploads[0].length, 5, `no other argument: ${JSON.stringify(uploads[0])}`);
  assert.equal(w.asset(), BYTES);
  assert.match(output, new RegExp(`^copy made: ${TAG}$`, 'm'));
}));

test('wrong bytes from upstream: nothing is created', () => inWorld((w) => {
  writeFileSync(path.join(w.state, 'upstream-bytes'), 'tampered\n');
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assertNothingWritten(w);
  assert.equal(w.asset(), undefined);
  assert.match(output, /does not match its digest/);
}));

test('gh failing for another reason: its message is shown and nothing is written', () => inWorld((w) => {
  const message = 'HTTP 502: Bad Gateway (https://api.github.com/repos/example-owner/example-repo/releases/tags/x)';
  writeFileSync(path.join(w.state, 'fail-view'), message);
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assert.ok(output.includes(message), output);
  assertNothingWritten(w);
}));

test('gh failing to download a copy it lists: its message is shown and nothing is written', () => inWorld((w) => {
  w.release({ 'runtime-source.tar.gz': BYTES });
  const message = 'HTTP 503: Service Unavailable';
  writeFileSync(path.join(w.state, 'fail-download'), message);
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assert.ok(output.includes(message), output);
  assertNothingWritten(w);
}));

test('a release gh finds whose public address does not answer (a draft) fails and says so', () => inWorld((w) => {
  w.release({ 'runtime-source.tar.gz': BYTES });
  writeFileSync(path.join(w.state, 'public-misses'), '1000');
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assertNothingWritten(w);
  assert.ok(w.calls().some((call) => call[0] === 'curl' && call.at(-1) === PUBLIC), 'the public address was read');
  assert.match(output, new RegExp(`${PUBLIC.replaceAll('.', '\\.')} does not answer`));
}));

test('a copy just made whose public address answers after a moment passes', () => inWorld((w) => {
  writeFileSync(path.join(w.state, 'public-misses'), '2');
  const { status, output } = w.run();
  assert.equal(status, 0, output);
  assert.equal(w.ghCalls('create').length, 1);
  assert.equal(w.calls().filter((call) => call[0] === 'curl' && call.at(-1) === PUBLIC).length, 3);
  assert.match(output, new RegExp(`^copy made: ${TAG}$`, 'm'));
}));

test('a public address whose bytes do not match the digest fails and says so', () => inWorld((w) => {
  w.release({ 'runtime-source.tar.gz': BYTES });
  writeFileSync(path.join(w.state, 'public-bytes'), 'served by someone else\n');
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assertNothingWritten(w);
  assert.match(output, new RegExp(`the bytes at ${PUBLIC.replaceAll('.', '\\.')} do not match its digest`));
}));

test('gh acts on GITHUB_REPOSITORY even when GH_REPO names another repository', () => inWorld((w) => {
  const { status, output } = w.run({ GH_REPO: 'example-foundation/fortoid' });
  assert.equal(status, 0, output);
  assert.ok(w.ghCalls('view').length > 0, 'gh was called');
  assert.equal(w.ghCalls('create').length, 1);
}));

test('a list that gives no source stops the run and says so', () => inWorld((w) => {
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assert.deepEqual(w.calls(), [], 'neither gh nor curl was called');
  assert.match(output, /^::error::the mirror list gave no source/m);
}, { listTool: '// prints nothing and exits 0\n' }));

test('a release without the file and wrong bytes from upstream: nothing is uploaded', () => inWorld((w) => {
  w.release();
  writeFileSync(path.join(w.state, 'upstream-bytes'), 'tampered\n');
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assertNothingWritten(w);
  assert.equal(w.asset(), undefined);
  assert.match(output, /does not match its digest/);
}));

test('an upstream address that does not answer stops the run with a sentence naming the tag', () => inWorld((w) => {
  writeFileSync(path.join(w.state, 'upstream-url'), 'https://github.com/example-foundation/fortweb/elsewhere');
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assertNothingWritten(w);
  assert.ok(output.includes('curl: (22)'), `curl's own message stays: ${output}`);
  assert.match(output, new RegExp(`^::error::the archive for release ${TAG} could not be fetched from `
    + `${UPSTREAM.replaceAll('.', '\\.')}$`, 'm'));
}));

test('gh failing to create the release stops the run with a sentence naming the tag', () => inWorld((w) => {
  const message = 'HTTP 422: Validation Failed';
  writeFileSync(path.join(w.state, 'fail-create'), message);
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assert.ok(output.includes(message), `gh's own message stays: ${output}`);
  assert.match(output, new RegExp(`^::error::gh could not create release ${TAG}$`, 'm'));
}));

test('gh failing to upload the copy stops the run with a sentence naming the tag', () => inWorld((w) => {
  w.release();
  const message = 'HTTP 500: Internal Server Error';
  writeFileSync(path.join(w.state, 'fail-upload'), message);
  const { status, output } = w.run();
  assert.notEqual(status, 0, output);
  assert.ok(output.includes(message), `gh's own message stays: ${output}`);
  assert.match(output, new RegExp(`^::error::gh could not upload the copy to release ${TAG}$`, 'm'));
}));

test('a release holding only files with similar names: the archive is uploaded, the others left alone', () => inWorld((w) => {
  w.release({ 'runtime-source.tar.gz.sig': 'signature\n', 'old-runtime-source.tar.gz': BYTES });
  const { status, output } = w.run();
  assert.equal(status, 0, output);
  assert.deepEqual(w.ghCalls('download'), [], 'no other file is taken for the archive');
  assert.equal(w.ghCalls('upload').length, 1);
  assert.equal(w.asset(), BYTES);
  const release = path.join(w.state, 'releases', TAG);
  assert.equal(readFileSync(path.join(release, 'runtime-source.tar.gz.sig'), 'utf8'), 'signature\n');
  assert.equal(readFileSync(path.join(release, 'old-runtime-source.tar.gz'), 'utf8'), BYTES);
  assert.match(output, new RegExp(`^copy made: ${TAG}$`, 'm'));
}));

test('the workflow runs when the script, its list, the list tool or the workflow itself changes', () => {
  const lines = readFileSync(path.join(ROOT, '.github', 'workflows', 'runtime-source-mirror.yml'), 'utf8').split('\n');
  const start = lines.findIndex((line) => line.trim() === 'paths:');
  assert.ok(start >= 0, 'the workflow has a paths trigger');
  const indent = lines[start].search(/\S/);
  const paths = [];
  for (const line of lines.slice(start + 1)) {
    if (line.search(/\S/) <= indent) break;
    paths.push(line.trim().replace(/^- /, ''));
  }
  assert.deepEqual(paths.sort(), [
    '.github/workflows/runtime-source-mirror.yml',
    'config/runtime-source-mirror.json',
    'scripts/mirror-runtime-sources.sh',
    'tools/list-runtime-source-mirrors.mjs',
  ]);
});
