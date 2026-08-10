#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

required_env "OUTPUT_DIR"
output_dir="${OUTPUT_DIR}"
metadata_path="${output_dir}/metadata.env"
comment_body_path="${output_dir}/codex-review-comment.md"
final_message_path="${output_dir}/codex-final-message.md"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "${output_dir}"

if [[ "${CODEX_SHOULD_RUN:-true}" != "true" ]]; then
  {
    echo "has_changes=false"
    echo "skip_reason=${CODEX_SKIP_REASON:-preparation skipped the Codex Action}"
  } >"${metadata_path}"
  exit 0
fi

for name in CODEX_RESULT PR_NUMBER HEAD_REF BASE_REF HEAD_SHA BASE_SHA COMMENT_AUTHOR COMMENT_URL; do
  required_env "${name}"
done

REQUIRE_CHANGES=false python3 "${script_dir}/collect-codex-patch-result.py"
has_changes="$(awk -F= '$1 == "has_changes" { print $2; exit }' "${output_dir}/codex-result.env")"

if [[ "${has_changes}" != "true" ]]; then
  {
    echo "Codex reviewed this feedback but did not produce any committable changes."
    echo
    echo "Feedback from @${COMMENT_AUTHOR}: ${COMMENT_URL}"
    echo
    echo "Codex final message:"
    echo
    sed -n '1,200p' "${final_message_path}"
  } >"${comment_body_path}"
fi

{
  echo "has_changes=${has_changes}"
  echo "pr_number=${PR_NUMBER}"
  echo "head_ref=${HEAD_REF}"
  echo "base_ref=${BASE_REF}"
  echo "head_sha=${HEAD_SHA}"
  echo "base_sha=${BASE_SHA}"
  echo "comment_author=${COMMENT_AUTHOR}"
  echo "comment_url=${COMMENT_URL}"
} >"${metadata_path}"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "has_changes=${has_changes}" >>"${GITHUB_OUTPUT}"
fi
