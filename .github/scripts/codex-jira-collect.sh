#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"

  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

required_env "CODEX_OPERATION"
required_env "OUTPUT_DIR"
required_env "FINAL_MESSAGE_PATH"
required_env "BRANCH_NAME"

output_dir="${OUTPUT_DIR}"
final_message_path="${FINAL_MESSAGE_PATH}"
pr_body_path="${output_dir}/codex-pr-body.md"
patch_path="${output_dir}/changes.patch"
metadata_path="${output_dir}/metadata.env"
sanitized_home="${RUNNER_TEMP:-/tmp}/codex-collect-home"
sanitized_tmp="${RUNNER_TEMP:-/tmp}/codex-collect-tmp"

run_sanitized() {
  env -i \
    "HOME=${sanitized_home}" \
    "PATH=${PATH:-/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin}" \
    "SHELL=${SHELL:-/bin/bash}" \
    "USER=${USER:-runner}" \
    "LOGNAME=${LOGNAME:-${USER:-runner}}" \
    "LANG=${LANG:-C.UTF-8}" \
    "LC_ALL=${LC_ALL:-${LANG:-C.UTF-8}}" \
    "TERM=${TERM:-xterm}" \
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

case "${CODEX_OPERATION}" in
  jira-generate)
    if [[ ! -s "${pr_body_path}" ]]; then
      echo "Missing prepared pull request body: ${pr_body_path}" >&2
      exit 1
    fi
    {
      echo
      echo "## Codex Final Message"
      echo
      sed -n '1,200p' "${final_message_path}"
    } >>"${pr_body_path}"
    ;;
  jira-repair)
    required_env "INPUT_DIR"
    required_env "REPAIR_ATTEMPT"
    input_pr_body_path="${INPUT_DIR}/codex-pr-body.md"
    if [[ -s "${input_pr_body_path}" ]]; then
      cp "${input_pr_body_path}" "${pr_body_path}"
    else
      required_env "ISSUE_KEY"
      required_env "ISSUE_URL"
      {
        echo "### Jira link"
        echo
        echo "See [${ISSUE_KEY}](${ISSUE_URL})"
      } >"${pr_body_path}"
    fi
    {
      echo
      echo "## Codex Repair Attempt ${REPAIR_ATTEMPT}"
      echo
      sed -n '1,200p' "${final_message_path}"
    } >>"${pr_body_path}"
    ;;
  *)
    echo "Unsupported Codex Jira operation: ${CODEX_OPERATION}" >&2
    exit 1
    ;;
esac

if [[ -z "$(git_sanitized status --short --untracked-files=normal)" ]]; then
  echo "Codex left no committable changes." >&2
  exit 1
fi

git_sanitized add -A
if git_sanitized diff --cached --quiet; then
  echo "Codex produced no staged patch output." >&2
  exit 1
fi
git_sanitized diff --cached --binary >"${patch_path}"

{
  echo "branch_name=${BRANCH_NAME}"
  echo "has_changes=true"
  if [[ "${CODEX_OPERATION}" == "jira-repair" ]]; then
    echo "repair_attempt=${REPAIR_ATTEMPT}"
  fi
} >"${metadata_path}"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "branch_name=${BRANCH_NAME}"
    echo "has_changes=true"
  } >>"${GITHUB_OUTPUT}"
fi
