#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

for name in CODEX_RESULT OUTPUT_DIR CONFLICTED_FILES PR_NUMBER HEAD_REF BASE_REF HEAD_SHA BASE_SHA; do
  required_env "${name}"
done

output_dir="${OUTPUT_DIR}"
conflicted_files_path="${output_dir}/conflicted-files.txt"
final_message_path="${output_dir}/codex-final-message.md"
comment_body_path="${output_dir}/codex-conflict-comment.md"
metadata_path="${output_dir}/metadata.env"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "${output_dir}"
printf '%s\n' "${CONFLICTED_FILES}" | sed '/^[[:space:]]*$/d' | sort -u >"${conflicted_files_path}"
if [[ ! -s "${conflicted_files_path}" ]]; then
  echo "Missing conflicted-files list." >&2
  exit 1
fi

while IFS= read -r conflicted_file; do
  if [[ "${conflicted_file}" == /* || "${conflicted_file}" == *..* || "${conflicted_file}" == *$'\r'* ]]; then
    echo "Unsafe conflicted file path: ${conflicted_file}" >&2
    exit 1
  fi
done <"${conflicted_files_path}"

ALLOWED_PATHS_FILE="${conflicted_files_path}" REQUIRE_CHANGES=true \
  python3 "${script_dir}/collect-codex-patch-result.py"

{
  echo "Codex resolved merge conflicts for this PR."
  echo
  echo "Base branch: ${BASE_REF}"
  echo
  echo "Conflicted files resolved:"
  sed 's/^/- /' "${conflicted_files_path}"
  echo
  echo "Codex final message:"
  echo
  sed -n '1,200p' "${final_message_path}"
} >"${comment_body_path}"

{
  echo "has_changes=true"
  echo "pr_number=${PR_NUMBER}"
  echo "head_ref=${HEAD_REF}"
  echo "base_ref=${BASE_REF}"
  echo "head_sha=${HEAD_SHA}"
  echo "base_sha=${BASE_SHA}"
} >"${metadata_path}"
