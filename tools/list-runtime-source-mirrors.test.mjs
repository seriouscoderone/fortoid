import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import { readMirrors } from './list-runtime-source-mirrors.mjs';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '..');
const SHA = 'a'.repeat(64);
const UPSTREAM = 'https://github.com/keri-foundation/fortweb/releases/download/some-tag/runtime-source.tar.gz';
const list = (...sources) => JSON.stringify({ schema: 'fortoid.runtime-source-mirror.v1', sources });
const source = (change = {}) => ({ tag: 'some-tag', upstream: UPSTREAM, sha256: SHA, ...change });

test('reads a list', () => {
  assert.deepEqual(readMirrors(list(source(), source({ tag: 'other-1.0' }))), [
    { tag: 'some-tag', upstream: UPSTREAM, sha256: SHA },
    { tag: 'other-1.0', upstream: UPSTREAM, sha256: SHA },
  ]);
});

test('refuses a list that is not one', () => {
  for (const text of ['', '[]', 'null', '{}', JSON.stringify({ schema: 'other', sources: [source()] }),
    JSON.stringify({ schema: 'fortoid.runtime-source-mirror.v1', sources: [] }),
    JSON.stringify({ schema: 'fortoid.runtime-source-mirror.v1', sources: [source()], more: 1 })]) {
    assert.throws(() => readMirrors(text), /mirror list/, text);
  }
});

test('refuses a source that is not exact', () => {
  const bad = [
    [{ ...source(), note: 'x' }, /exactly tag, upstream and sha256/],
    [{ tag: 'some-tag', upstream: UPSTREAM }, /exactly tag, upstream and sha256/],
    [source({ tag: 'Some-Tag' }), /tag must be/],
    [source({ tag: 'a/b' }), /tag must be/],
    [source({ tag: '' }), /tag must be/],
    [source({ tag: 'a'.repeat(101) }), /tag must be/],
    [source({ tag: 'a..b' }), /tag must be/],
    [source({ tag: 'a.' }), /tag must be/],
    [source({ tag: 'a.lock' }), /tag must be/],
    [source({ sha256: SHA.toUpperCase() }), /sha256 must be/],
    [source({ sha256: SHA.slice(1) }), /sha256 must be/],
    [source({ upstream: 'http://github.com/o/r/releases/download/t/runtime-source.tar.gz' }), /upstream must be/],
    [source({ upstream: 'https://example.test/o/r/releases/download/t/runtime-source.tar.gz' }), /upstream must be/],
    [source({ upstream: 'https://user:secret@github.com/o/r/releases/download/t/runtime-source.tar.gz' }), /upstream must be/],
    [source({ upstream: 'https://github.com/o/r/releases/download/t/runtime-source.tar.gz?x=1' }), /upstream must be/],
    [source({ upstream: 'https://github.com/o/r/releases/download/t/other.tar.gz' }), /upstream must be/],
    [source({ upstream: 'https://github.com/o/r/releases/download/t/runtime-source.tar.gz?' }), /upstream must be/],
    [source({ upstream: 'https://github.com/o/r/releases/download/t/runtime-source.tar.gz#' }), /upstream must be/],
    [source({ upstream: 'https://github.com/o/r/releases/download/t/runtime-source.tar.gz?#' }), /upstream must be/],
    [source({ upstream: 'https://github.com:8443/o/r/releases/download/t/runtime-source.tar.gz' }), /upstream must be/],
    [source({ upstream: 'https://github.com/o/r/releases/download/t/runtime-source.tar.gz#x' }), /upstream must be/],
    [source({ upstream: 'https://GitHub.com/o/r/releases/download/t/runtime-source.tar.gz' }), /upstream must be/],
    [source({ upstream: 'https://github.com:443/o/r/releases/download/t/runtime-source.tar.gz' }), /upstream must be/],
    [source({ upstream: 'not an address' }), /not an address/],
    [source({ upstream: 7 }), /not an address/],
  ];
  for (const [entry, message] of bad) {
    assert.throws(() => readMirrors(list(entry)), message, JSON.stringify(entry));
  }
});

test('refuses a source that is not an object', () => {
  for (const entry of ['some-tag', 7, null, [source()]]) {
    assert.throws(() => readMirrors(list(entry)), /exactly tag, upstream and sha256/, JSON.stringify(entry));
  }
});

test('reads a tag of 100 characters', () => {
  const tag = 'a'.repeat(100);
  assert.deepEqual(readMirrors(list(source({ tag }))), [{ tag, upstream: UPSTREAM, sha256: SHA }]);
});

test('refuses a tag listed twice', () => {
  assert.throws(() => readMirrors(list(source(), source())), /listed twice/);
});

test('the committed list reads, and the program prints it', () => {
  const sources = readMirrors(readFileSync(path.join(ROOT, 'config', 'runtime-source-mirror.json'), 'utf8'));
  const printed = execFileSync(process.execPath, [path.join(ROOT, 'tools', 'list-runtime-source-mirrors.mjs')], { encoding: 'utf8' });
  assert.equal(printed, sources.map((item) => `${item.tag}\t${item.upstream}\t${item.sha256}\n`).join(''));
});
