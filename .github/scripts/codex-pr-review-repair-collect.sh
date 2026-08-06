#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

for name in CODEX_RESULT OUTPUT_DIR INPUT_DIR REPAIR_ATTEMPT PR_NUMBER HEAD_REF BASE_REF HEAD_SHA BASE_SHA COMMENT_AUTHOR COMMENT_URL; do
  required_env "${name}"
done

output_dir="${OUTPUT_DIR}"
input_comment_body_path="${INPUT_DIR}/codex-review-comment.md"
final_message_path="${output_dir}/codex-final-message.md"
comment_body_path="${output_dir}/codex-review-comment.md"
metadata_path="${output_dir}/metadata.env"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "${output_dir}"
REQUIRE_CHANGES=true python3 "${script_dir}/collect-codex-patch-result.py"

if [[ -s "${input_comment_body_path}" ]]; then
  cp "${input_comment_body_path}" "${comment_body_path}"
else
  {
    echo "Codex addressed review feedback for PR #${PR_NUMBER}."
    echo
    echo "Feedback from @${COMMENT_AUTHOR}: ${COMMENT_URL}"
  } >"${comment_body_path}"
fi
{
  echo
  echo "## Codex Review Repair Attempt ${REPAIR_ATTEMPT}"
  echo
  sed -n '1,200p' "${final_message_path}"
} >>"${comment_body_path}"

{
  echo "has_changes=true"
  echo "pr_number=${PR_NUMBER}"
  echo "head_ref=${HEAD_REF}"
  echo "base_ref=${BASE_REF}"
  echo "head_sha=${HEAD_SHA}"
  echo "base_sha=${BASE_SHA}"
  echo "comment_author=${COMMENT_AUTHOR}"
  echo "comment_url=${COMMENT_URL}"
  echo "repair_attempt=${REPAIR_ATTEMPT}"
} >"${metadata_path}"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "has_changes=true"
    echo "pr_number=${PR_NUMBER}"
    echo "head_ref=${HEAD_REF}"
  } >>"${GITHUB_OUTPUT}"
fi
