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
#   GITHUB_REPOSITORY  owner/name of this repository (required)
#   GITHUB_SHA         the commit a new release points at (required)
#   GH_TOKEN           read by gh itself; this script never prints it
#   MIRROR_LIST        optional: a list file to read instead of the committed
#                      one; passed as FILE to tools/list-runtime-source-mirrors.mjs

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ARCHIVE=runtime-source.tar.gz

: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY must be set}"
: "${GITHUB_SHA:?GITHUB_SHA must be set}"

WORKROOT="$(mktemp -d)"
trap 'rm -rf "$WORKROOT"' EXIT

node "${REPO_ROOT}/tools/list-runtime-source-mirrors.mjs" ${MIRROR_LIST:+"$MIRROR_LIST"} > "${WORKROOT}/mirrors.tsv"

matches() { echo "$1  $2" | sha256sum --check --status; }

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
  curl --fail --location --silent --show-error --output "$COPY" "$UPSTREAM"
  if ! matches "$SHA256" "$COPY"; then
    echo "::error::${UPSTREAM} does not match its digest"
    exit 1
  fi
  if [ "$MAKE" = create ]; then
    gh release create "$TAG" "$COPY" \
      --prerelease \
      --target "$GITHUB_SHA" \
      --title "Runtime source copy: ${TAG}" \
      --notes "A copy of the runtime source the wallet is built from, taken from ${UPSTREAM} and checked against SHA-256 ${SHA256}. The builds of this repository read this copy."
  else
    gh release upload "$TAG" "$COPY"
  fi
  echo "copy made: ${TAG}"
done 3< "${WORKROOT}/mirrors.tsv"
