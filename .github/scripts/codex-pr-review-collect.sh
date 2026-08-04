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
comment_body_path="${output_dir}/codex-comment.md"
patch_path="${output_dir}/changes.patch"
sanitized_home="${RUNNER_TEMP:-/tmp}/codex-review-collect-home"
sanitized_tmp="${RUNNER_TEMP:-/tmp}/codex-review-collect-tmp"

# A preparation skip already wrote the complete no-change metadata artefact.
if [[ -s "${metadata_path}" ]] && grep -qx 'has_changes=false' "${metadata_path}"; then
  exit 0
fi

required_env "FINAL_MESSAGE_PATH"
required_env "FEEDBACK_ENV_PATH"
final_message_path="${FINAL_MESSAGE_PATH}"
feedback_env_path="${FEEDBACK_ENV_PATH}"

if [[ ! -s "${feedback_env_path}" ]]; then
  echo "Missing prepared review metadata: ${feedback_env_path}" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "${feedback_env_path}"
set +a

for name in PR_NUMBER HEAD_REF BASE_REF COMMENT_AUTHOR COMMENT_URL; do
  required_env "${name}"
done

run_sanitized() {
  env -i \
    "HOME=${sanitized_home}" \
    "PATH=${PATH:-/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin}" \
    "LANG=${LANG:-C.UTF-8}" \
    "LC_ALL=${LC_ALL:-${LANG:-C.UTF-8}}" \
    "TMPDIR=${sanitized_tmp}" \
    "GIT_CONFIG_GLOBAL=/dev/null" \
    "GIT_CONFIG_NOSYSTEM=1" \
    "GIT_TERMINAL_PROMPT=0" \
    "$@"
}

git_sanitized() {
  run_sanitized git \
    -c core.hooksPath=/dev/null \
    -c credential.helper= \
    -c protocol.file.allow=never \
    "$@"
}

mkdir -p "${output_dir}" "${sanitized_home}" "${sanitized_tmp}"
if [[ ! -s "${final_message_path}" ]]; then
  echo "Codex completed without writing a final message." >"${final_message_path}"
fi

if [[ -z "$(git_sanitized status --short --untracked-files=normal)" ]]; then
  {
    echo "Codex reviewed this feedback but did not produce any committable changes."
    echo
    echo "Feedback from @${COMMENT_AUTHOR}: ${COMMENT_URL}"
    echo
    echo "Codex final message:"
    echo
    sed -n '1,200p' "${final_message_path}"
  } >"${comment_body_path}"
  {
    echo "has_changes=false"
    echo "pr_number=${PR_NUMBER}"
    echo "head_ref=${HEAD_REF}"
    echo "base_ref=${BASE_REF}"
  } >"${metadata_path}"
  exit 0
fi

git_sanitized add -A
if git_sanitized diff --cached --quiet; then
  echo "Codex produced changes, but none were staged for patch output." >&2
  exit 1
fi
git_sanitized diff --cached --binary >"${patch_path}"

{
  echo "has_changes=true"
  echo "pr_number=${PR_NUMBER}"
  echo "head_ref=${HEAD_REF}"
  echo "base_ref=${BASE_REF}"
  echo "comment_author=${COMMENT_AUTHOR}"
  echo "comment_url=${COMMENT_URL}"
} >"${metadata_path}"
