#!/usr/bin/env bash
# ── mirror-runtime-sources.sh ─────────────────────────────────────────────────
# Keeps a checked copy of each runtime source listed in
# config/runtime-source-mirror.json as a release of this repository. Run by the
# workflow runtime-source-mirror.yml; tested by
# tools/mirror-runtime-sources.test.mjs with stand-ins for gh and curl.
#
# For each source: a release that holds the file is checked against the digest
# and never replaced. Otherwise the archive is fetched from the upstream
# address, checked against the digest, and only then becomes a new prerelease
# (gh release create), or is uploaded to a release that lacks it (gh release
# upload). The script never deletes a release or a file and never replaces one.
#
# Takes gh, curl and node from PATH.
# Environment:
#   GITHUB_REPOSITORY  owner/name of this repository (required); every gh call
#                      acts on it at github.com (the script sets GH_REPO to
#                      github.com/<owner/name>)
#   GITHUB_SHA         the commit a new release points at (required)
#   GH_TOKEN           read by gh itself; this script never prints it
#   MIRROR_LIST        optional: a list file to read instead of the committed
#                      one; passed as FILE to tools/list-runtime-source-mirrors.mjs
#   MIRROR_RETRY_PAUSE optional: seconds between reads of a copy's public
#                      address (default 10; the tests set 0)
#
# After a copy is found or made, the script reads the public address the builds
# read, https://github.com/<repository>/releases/download/<tag>/runtime-source.tar.gz,
# and checks those bytes against the digest: gh finds a release that is still a
# draft, which that address does not serve. The address is read up to
# PUBLIC_ATTEMPTS times, since a copy just made may take a moment to answer.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ARCHIVE=runtime-source.tar.gz
PUBLIC_ATTEMPTS=6
RETRY_PAUSE="${MIRROR_RETRY_PAUSE:-10}"

: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY must be set}"
: "${GITHUB_SHA:?GITHUB_SHA must be set}"

# Every gh call acts on GITHUB_REPOSITORY at HOST, the repository whose public
# address check_public reads, never on one gh would take from the clone's
# remotes (a clone made by hand may have the Foundation's repository as a
# remote) or from a GH_REPO the caller had set. GH_REPO names the host, so
# neither GH_HOST nor gh's own configuration moves the calls to another host.
HOST=github.com
export GH_REPO="${HOST}/${GITHUB_REPOSITORY}"

WORKROOT="$(mktemp -d)"
trap 'rm -rf "$WORKROOT"' EXIT

node "${REPO_ROOT}/tools/list-runtime-source-mirrors.mjs" ${MIRROR_LIST:+"$MIRROR_LIST"} > "${WORKROOT}/mirrors.tsv"
# The list always holds a source, so a list tool that printed none and exited 0
# went wrong: a run that copied nothing must not look like one that checked all.
if [ ! -s "${WORKROOT}/mirrors.tsv" ]; then
  echo "::error::the mirror list gave no source: tools/list-runtime-source-mirrors.mjs printed nothing"
  exit 1
fi

matches() { echo "$1  $2" | sha256sum --check --status; }

# check_public TAG SHA256 WORK: the public address of TAG's copy serves bytes
# that match SHA256, or the run stops with a sentence that says which is wrong.
check_public() {
  local url="https://${HOST}/${GITHUB_REPOSITORY}/releases/download/$1/${ARCHIVE}"
  local fetched="$3/public-${ARCHIVE}"
  local attempt=1
  until curl --fail --location --silent --show-error --output "$fetched" "$url"; do
    if [ "$attempt" -ge "$PUBLIC_ATTEMPTS" ]; then
      echo "::error::${url} does not answer after ${PUBLIC_ATTEMPTS} tries: the release may still be a draft"
      exit 1
    fi
    attempt=$((attempt + 1))
    sleep "$RETRY_PAUSE"
  done
  if ! matches "$2" "$fetched"; then
    echo "::error::the bytes at ${url} do not match its digest"
    exit 1
  fi
  echo "public copy checked: ${url}"
}

while IFS=$'\t' read -r TAG UPSTREAM SHA256 <&3; do
  WORK="$(mktemp -d "${WORKROOT}/source.XXXXXX")"
  COPY="${WORK}/${ARCHIVE}"
  # gh's own messages always reach the log. Only "release not found" is read
  # as absence; any other failure stops the run.
  VIEWED=0
  NAMES="$(gh release view "$TAG" --json assets --jq '.assets[].name' 2> "${WORK}/view.err")" || VIEWED=$?
  cat "${WORK}/view.err" >&2
  if [ "$VIEWED" -eq 0 ]; then
    if grep -qxF "$ARCHIVE" <<< "$NAMES"; then
      if ! gh release download "$TAG" --pattern "$ARCHIVE" --dir "$WORK"; then
        echo "::error::gh could not download the copy under release ${TAG}"
        exit 1
      fi
      if ! matches "$SHA256" "$COPY"; then
        echo "::error::the copy under release ${TAG} does not match its digest"
        exit 1
      fi
      check_public "$TAG" "$SHA256" "$WORK"
      echo "copy present and correct: ${TAG}"
      continue
    fi
    MAKE=upload
  elif grep -qF 'release not found' "${WORK}/view.err"; then
    MAKE=create
  else
    echo "::error::gh could not read release ${TAG}"
    exit 1
  fi
  if ! curl --fail --location --silent --show-error --output "$COPY" "$UPSTREAM"; then
    echo "::error::the archive for release ${TAG} could not be fetched from ${UPSTREAM}"
    exit 1
  fi
  if ! matches "$SHA256" "$COPY"; then
    echo "::error::${UPSTREAM} does not match its digest"
    exit 1
  fi
  if [ "$MAKE" = create ]; then
    if ! gh release create "$TAG" "$COPY" \
      --prerelease \
      --target "$GITHUB_SHA" \
      --title "Runtime source copy: ${TAG}" \
      --notes "A copy of the runtime source the wallet is built from, taken from ${UPSTREAM} and checked against SHA-256 ${SHA256}. The builds of this repository read this copy."; then
      echo "::error::gh could not create release ${TAG}"
      exit 1
    fi
  elif ! gh release upload "$TAG" "$COPY"; then
    echo "::error::gh could not upload the copy to release ${TAG}"
    exit 1
  fi
  check_public "$TAG" "$SHA256" "$WORK"
  echo "copy made: ${TAG}"
done 3< "${WORKROOT}/mirrors.tsv"
