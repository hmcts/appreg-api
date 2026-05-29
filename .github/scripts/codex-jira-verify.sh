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
required_env "EXPECTED_BRANCH_NAME"
required_env "GH_TOKEN"

default_branch="${DEFAULT_BRANCH:-master}"
output_dir="${OUTPUT_DIR}"
metadata_path="${output_dir}/metadata.env"
patch_path="${output_dir}/changes.patch"
pr_body_path="${output_dir}/codex-pr-body.md"
verification_path="${output_dir}/verification.env"
guardrail_changes_path="${output_dir}/guardrail-changes.txt"
artifact_dir="${RUNNER_TEMP:-/tmp}/codex-jira-verify-${GITHUB_RUN_ID:-manual}-${GITHUB_RUN_ATTEMPT:-1}"
sanitized_home="${artifact_dir}/sanitized-home"
sanitized_tmp="${artifact_dir}/sanitized-tmp"
trusted_pipeline_path="${artifact_dir}/trusted-codex-local-pipeline.sh"
trusted_pipeline_sha=""
guardrail_review_required="false"
guardrail_pathspecs=(
  "bin/codex-local-pipeline.sh"
  ".github/scripts"
  ".github/workflows"
  "build.gradle"
  "settings.gradle"
  "gradle.properties"
  "gradle"
  "gradlew"
  "gradlew.bat"
  "init.gradle"
  "buildSrc"
)

metadata_value() {
  local key="$1"
  awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "${metadata_path}"
}

run_sanitized() {
  local sanitized_env=(
    env -i
    "HOME=${sanitized_home}"
    "PATH=${PATH:-/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin}"
    "SHELL=${SHELL:-/bin/bash}"
    "USER=${USER:-runner}"
    "LOGNAME=${LOGNAME:-${USER:-runner}}"
    "LANG=${LANG:-C.UTF-8}"
    "LC_ALL=${LC_ALL:-${LANG:-C.UTF-8}}"
    "TERM=${TERM:-xterm}"
    "TMPDIR=${sanitized_tmp}"
    "RUNNER_TEMP=${RUNNER_TEMP:-/tmp}"
    "CI=${CI:-true}"
    "GITHUB_ACTIONS=${GITHUB_ACTIONS:-true}"
    "GRADLE_USER_HOME=${sanitized_home}/.gradle"
    "GIT_CONFIG_GLOBAL=/dev/null"
    "GIT_CONFIG_NOSYSTEM=1"
    "GIT_TERMINAL_PROMPT=0"
  )

  if [[ -n "${JAVA_HOME:-}" ]]; then
    sanitized_env+=("JAVA_HOME=${JAVA_HOME}")
  fi

  "${sanitized_env[@]}" "$@"
}

git_sanitized() {
  run_sanitized git \
    -c core.hooksPath=/dev/null \
    -c credential.helper= \
    -c protocol.file.allow=never \
    "$@"
}

git_read_authenticated() {
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
    "GH_TOKEN=${GH_TOKEN}" \
    git \
    -c core.hooksPath=/dev/null \
    -c credential.helper= \
    -c credential.helper='!f() { test "$1" = get && echo username=x-access-token && echo "password=$GH_TOKEN"; }; f' \
    -c protocol.file.allow=never \
    "$@"
}

file_sha256() {
  local path="$1"

  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "${path}" | awk '{print $1}'
  else
    shasum -a 256 "${path}" | awk '{print $1}'
  fi
}

verify_trusted_file() {
  local path="$1"
  local expected_sha="$2"
  local label="$3"
  local actual_sha

  actual_sha="$(file_sha256 "${path}")"
  if [[ "${actual_sha}" != "${expected_sha}" ]]; then
    echo "::error::Trusted ${label} changed after capture; refusing to execute it." >&2
    exit 1
  fi
}

detect_guardrail_changes() {
  local guardrail_changes

  guardrail_changes="$(
    {
      git_sanitized diff --cached --name-status -- "${guardrail_pathspecs[@]}" || true
      git_sanitized status --short --untracked-files=normal -- "${guardrail_pathspecs[@]}" || true
    } | sed '/^[[:space:]]*$/d'
  )"

  printf '%s\n' "${guardrail_changes}" >"${guardrail_changes_path}"
  if [[ -n "${guardrail_changes}" ]]; then
    guardrail_review_required="true"
    echo "::warning::Codex changed workflow, runner, Gradle, or verification files. Manual verification is required."
    printf '%s\n' "${guardrail_changes}"
  fi
}

append_guardrail_warning() {
  if [[ "${guardrail_review_required}" != "true" ]]; then
    return
  fi

  {
    echo
    echo "### Manual verification required"
    echo
    echo "Codex changed workflow, runner, Gradle, or verification files. These changes can affect how checks execute and must be reviewed manually."
    echo
    echo "Changed verification-sensitive files:"
    echo
    sed 's/^/- /' "${guardrail_changes_path}"
  } >>"${pr_body_path}"
}

mkdir -p "${artifact_dir}" "${sanitized_home}" "${sanitized_tmp}"

branch_name="$(metadata_value branch_name)"
if [[ "${branch_name}" != "${EXPECTED_BRANCH_NAME}" || "${branch_name}" != codex/* ]]; then
  echo "Refusing to verify unexpected Codex branch name: ${branch_name}" >&2
  exit 1
fi

if [[ ! -s "${patch_path}" ]]; then
  echo "Missing or empty Codex patch artifact: ${patch_path}" >&2
  exit 1
fi

patch_sha="$(file_sha256 "${patch_path}")"

cp bin/codex-local-pipeline.sh "${trusted_pipeline_path}"
chmod +x "${trusted_pipeline_path}"
trusted_pipeline_sha="$(file_sha256 "${trusted_pipeline_path}")"

git_read_authenticated fetch origin "${default_branch}:refs/remotes/origin/${default_branch}"
git_sanitized checkout -B "${default_branch}" "origin/${default_branch}"
git_sanitized apply --index --binary "${patch_path}"

detect_guardrail_changes
append_guardrail_warning

local_pipeline_mode="${LOCAL_PIPELINE_MODE:-checks-only}"
if [[ "${SKIP_LOCAL_PIPELINE:-false}" == "true" ]]; then
  echo "Skipping local pipeline because SKIP_LOCAL_PIPELINE=true"
else
  verify_trusted_file "${trusted_pipeline_path}" "${trusted_pipeline_sha}" "pipeline wrapper"
  run_sanitized "${trusted_pipeline_path}" "${local_pipeline_mode}" --base "${default_branch}" --no-fetch
fi

{
  echo "branch_name=${branch_name}"
  echo "patch_sha=${patch_sha}"
  echo "guardrail_review_required=${guardrail_review_required}"
} >"${verification_path}"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "branch_name=${branch_name}"
    echo "patch_sha=${patch_sha}"
  } >>"${GITHUB_OUTPUT}"
fi
