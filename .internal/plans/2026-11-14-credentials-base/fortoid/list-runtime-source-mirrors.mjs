#!/usr/bin/env node
// The runtime sources this repository keeps a checked copy of (config/runtime-source-mirror.json).
//
// A runtime source is an archive the wallet is built from. The Foundation publishes it as a download attached to
// one of its releases, and such a download can be removed. The workflow runtime-source-mirror.yml copies each listed
// archive to a release of this repository, after checking it against the digest given here, and the builds read
// that copy.
//
// Run as a program it prints one line for each source: tag, upstream address, SHA-256, separated by tabs.
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const SCHEMA = 'fortoid.runtime-source-mirror.v1';
const ARCHIVE = 'runtime-source.tar.gz';
const TAG = /^[a-z0-9][a-z0-9.-]{0,99}$/;
const SHA256 = /^[0-9a-f]{64}$/;

function plain(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function sameKeys(value, keys) {
  return JSON.stringify(Object.keys(value).sort()) === JSON.stringify([...keys].sort());
}

function upstreamAddress(value, index) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new Error(`source ${index}: upstream is not an address`);
  }
  if (url.protocol !== 'https:' || url.hostname !== 'github.com' || url.port || url.username || url.password
      || url.search || url.hash || url.href !== value || !url.pathname.endsWith(`/${ARCHIVE}`)) {
    throw new Error(`source ${index}: upstream must be a plain https://github.com/ address of ${ARCHIVE}`);
  }
  return value;
}

/** The list in `text`, checked. Throws an Error that says what is wrong. */
export function readMirrors(text) {
  let config;
  try {
    config = JSON.parse(text);
  } catch {
    throw new Error('the mirror list is not JSON');
  }
  if (!plain(config) || !sameKeys(config, ['schema', 'sources']) || config.schema !== SCHEMA
      || !Array.isArray(config.sources) || config.sources.length === 0) {
    throw new Error(`the mirror list must be {"schema": "${SCHEMA}", "sources": [...]} with at least one source`);
  }
  const tags = new Set();
  return config.sources.map((source, index) => {
    if (!plain(source) || !sameKeys(source, ['tag', 'upstream', 'sha256'])) {
      throw new Error(`source ${index}: must have exactly tag, upstream and sha256`);
    }
    if (typeof source.tag !== 'string' || !TAG.test(source.tag)) {
      throw new Error(`source ${index}: tag must be lower-case letters, digits, dots and hyphens`);
    }
    if (tags.has(source.tag)) {
      throw new Error(`source ${index}: tag ${source.tag} is listed twice`);
    }
    tags.add(source.tag);
    if (typeof source.sha256 !== 'string' || !SHA256.test(source.sha256)) {
      throw new Error(`source ${index}: sha256 must be 64 lower-case hex digits`);
    }
    if (typeof source.upstream !== 'string') {
      throw new Error(`source ${index}: upstream is not an address`);
    }
    return { tag: source.tag, upstream: upstreamAddress(source.upstream, index), sha256: source.sha256 };
  });
}

const here = fileURLToPath(import.meta.url);
if (process.argv[1] && path.resolve(process.argv[1]) === here) {
  const file = path.join(path.dirname(here), '..', 'config', 'runtime-source-mirror.json');
  try {
    for (const source of readMirrors(readFileSync(file, 'utf8'))) {
      process.stdout.write(`${source.tag}\t${source.upstream}\t${source.sha256}\n`);
    }
  } catch (error) {
    process.stderr.write(`error: ${error.message}\n`);
    process.exit(1);
  }
}
