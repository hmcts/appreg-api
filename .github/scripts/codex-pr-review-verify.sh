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
required_env "EXPECTED_PR_NUMBER"
required_env "EXPECTED_HEAD_REF"
required_env "GH_TOKEN"

output_dir="${OUTPUT_DIR}"
metadata_path="${output_dir}/metadata.env"
patch_path="${output_dir}/changes.patch"
comment_body_path="${output_dir}/codex-review-comment.md"
verification_path="${output_dir}/verification.env"
guardrail_changes_path="${output_dir}/guardrail-changes.txt"
artifact_dir="${RUNNER_TEMP:-/tmp}/codex-review-verify-${GITHUB_RUN_ID:-manual}-${GITHUB_RUN_ATTEMPT:-1}"
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
    echo "Manual verification required:"
    echo
    echo "Codex changed workflow, runner, Gradle, or verification files. These changes can affect how checks execute and must be reviewed manually."
    echo
    echo "Changed verification-sensitive files:"
    sed 's/^/- /' "${guardrail_changes_path}"
    echo
  } >>"${comment_body_path}"
}

block_sonar_for_guardrail_changes() {
  if [[ "${guardrail_review_required}" != "true" ]]; then
    return
  fi

  echo "::error::Refusing to run Sonar with SONAR_TOKEN because Codex changed verification-sensitive files." >&2
  echo "Manual review is required before exposing Sonar credentials to changed Gradle, workflow, runner, or verification tooling." >&2
  sed 's/^/- /' "${guardrail_changes_path}" >&2
  exit 1
}

run_backend_sonar_analysis() {
  if [[ "${RUN_SONAR:-true}" != "true" ]]; then
    echo "Skipping Sonar analysis because RUN_SONAR is not true."
    return
  fi

  block_sonar_for_guardrail_changes

  if [[ -z "${SONAR_TOKEN:-}" ]]; then
    echo "::error::SONAR_TOKEN is required for Codex review verification Sonar analysis." >&2
    exit 1
  fi

  local sonar_host_url="${SONAR_HOST_URL:-https://sonarcloud.io}"
  local sonar_organization="${SONAR_ORGANIZATION:-hmcts}"
  local sonar_quality_gate_timeout="${SONAR_QUALITY_GATE_TIMEOUT_SECONDS:-300}"
  local sonar_args=(
    --no-daemon
    sonar
    "-Dsonar.host.url=${sonar_host_url}"
    "-Dsonar.pullrequest.key=${pr_number}"
    "-Dsonar.pullrequest.branch=${head_ref}"
    "-Dsonar.pullrequest.base=${base_ref:-master}"
    "-Dsonar.qualitygate.wait=true"
    "-Dsonar.qualitygate.timeout=${sonar_quality_gate_timeout}"
  )

  if [[ -n "${sonar_organization}" ]]; then
    sonar_args+=("-Dsonar.organization=${sonar_organization}")
  fi

  echo "Running Sonar analysis for Codex review PR #${pr_number}."
  run_sanitized env "SONAR_TOKEN=${SONAR_TOKEN}" \
    ./gradlew "${sonar_args[@]}"
}

mkdir -p "${artifact_dir}" "${sanitized_home}" "${sanitized_tmp}"

has_changes="$(metadata_value has_changes)"
pr_number="$(metadata_value pr_number)"
head_ref="$(metadata_value head_ref)"
base_ref="$(metadata_value base_ref)"

if [[ "${has_changes}" != "true" ]]; then
  {
    echo "has_changes=false"
    echo "pr_number=${pr_number}"
    echo "head_ref=${head_ref}"
    echo "base_ref=${base_ref}"
  } >"${verification_path}"
  exit 0
fi

if [[ "${pr_number}" != "${EXPECTED_PR_NUMBER}" || "${head_ref}" != "${EXPECTED_HEAD_REF}" || "${head_ref}" != codex/* ]]; then
  echo "Refusing to verify unexpected review artifact metadata." >&2
  exit 1
fi

if [[ ! -s "${patch_path}" ]]; then
  echo "Missing or empty Codex review patch artifact: ${patch_path}" >&2
  exit 1
fi

patch_sha="$(file_sha256 "${patch_path}")"

cp bin/codex-local-pipeline.sh "${trusted_pipeline_path}"
chmod +x "${trusted_pipeline_path}"
trusted_pipeline_sha="$(file_sha256 "${trusted_pipeline_path}")"

git_read_authenticated fetch origin "${head_ref}:refs/remotes/origin/${head_ref}"
if [[ -n "${base_ref}" ]]; then
  git_read_authenticated fetch origin "${base_ref}:refs/remotes/origin/${base_ref}"
fi
git_sanitized checkout -B "${head_ref}" "origin/${head_ref}"
git_sanitized apply --index --binary "${patch_path}"

detect_guardrail_changes
append_guardrail_warning

local_pipeline_mode="${LOCAL_PIPELINE_MODE:-checks-only}"
if [[ "${SKIP_LOCAL_PIPELINE:-false}" == "true" ]]; then
  echo "Skipping local pipeline because SKIP_LOCAL_PIPELINE=true"
else
  verify_trusted_file "${trusted_pipeline_path}" "${trusted_pipeline_sha}" "pipeline wrapper"
  run_sanitized "${trusted_pipeline_path}" "${local_pipeline_mode}" --base "${base_ref:-master}" --no-fetch
fi

run_backend_sonar_analysis

{
  echo "has_changes=true"
  echo "pr_number=${pr_number}"
  echo "head_ref=${head_ref}"
  echo "base_ref=${base_ref}"
  echo "patch_sha=${patch_sha}"
  echo "guardrail_review_required=${guardrail_review_required}"
} >"${verification_path}"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "pr_number=${pr_number}"
    echo "branch_name=${head_ref}"
    echo "patch_sha=${patch_sha}"
  } >>"${GITHUB_OUTPUT}"
fi
