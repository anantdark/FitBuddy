#!/usr/bin/env bash
# Deterministic Support ID → donors.json hash.
# Must match app SupportIdHasher exactly:
#   trim → lowercase → strip '-' → SHA-256(UTF-8) → lowercase hex
#
# Usage:
#   ./config/hash_support_id.sh '2478bc7f-5892-4c83-8a5d-d43aaf39f3f0'
#   echo '2478bc7f-5892-4c83-8a5d-d43aaf39f3f0' | ./config/hash_support_id.sh
set -euo pipefail

if [[ $# -ge 1 ]]; then
  ID="$1"
else
  ID="$(cat)"
fi

# trim leading/trailing whitespace (bash)
ID="${ID#"${ID%%[![:space:]]*}"}"
ID="${ID%"${ID##*[![:space:]]}"}"

canon=$(printf '%s' "$ID" | tr '[:upper:]' '[:lower:]' | tr -d '-')
if [[ -z "$canon" ]]; then
  echo "error: empty Support ID after canonicalize" >&2
  exit 1
fi

printf '%s' "$canon" | shasum -a 256 | awk '{print $1}'
