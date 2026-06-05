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
required_env "EXPECTED_BASE_REF"
required_env "EXPECTED_HEAD_SHA"
required_env "EXPECTED_BASE_SHA"
required_env "GH_TOKEN"

output_dir="${OUTPUT_DIR}"
metadata_path="${output_dir}/metadata.env"
conflicted_files_path="${output_dir}/conflicted-files.txt"
patch_path="${output_dir}/changes.patch"
verification_path="${output_dir}/verification.env"
artifact_dir="${RUNNER_TEMP:-/tmp}/codex-conflict-verify-${GITHUB_RUN_ID:-manual}-${GITHUB_RUN_ATTEMPT:-1}"
sanitized_home="${artifact_dir}/sanitized-home"
sanitized_tmp="${artifact_dir}/sanitized-tmp"
sanitized_runner_temp="${artifact_dir}/sanitized-runner-temp"
trusted_pipeline_path="${artifact_dir}/trusted-codex-local-pipeline.sh"
trusted_pipeline_sha=""

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
    "RUNNER_TEMP=${sanitized_runner_temp}"
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

read_conflicted_files() {
  mapfile -t conflicted_files < <(sed '/^[[:space:]]*$/d' "${conflicted_files_path}" | sort -u)

  if [[ "${#conflicted_files[@]}" -eq 0 ]]; then
    echo "Missing conflicted files artifact." >&2
    exit 1
  fi

  for path in "${conflicted_files[@]}"; do
    if [[ "${path}" == /* || "${path}" == *..* || "${path}" == *$'\n'* || "${path}" == *$'\r'* ]]; then
      echo "Unsafe conflicted file path: ${path}" >&2
      exit 1
    fi
  done
}

restore_ours_for_patch() {
  local path

  for path in "${conflicted_files[@]}"; do
    if git_sanitized cat-file -e "HEAD:${path}" 2>/dev/null; then
      git_sanitized checkout --ours -- "${path}" 2>/dev/null || git_sanitized restore --source=HEAD --worktree --staged -- "${path}"
      git_sanitized add -- "${path}"
    else
      git_sanitized rm -f --ignore-unmatch -- "${path}" >/dev/null 2>&1 || true
      rm -rf -- "${path}"
    fi
  done
}

mkdir -p "${artifact_dir}" "${sanitized_home}" "${sanitized_tmp}" "${sanitized_runner_temp}"

has_changes="$(metadata_value has_changes)"
pr_number="$(metadata_value pr_number)"
head_ref="$(metadata_value head_ref)"
base_ref="$(metadata_value base_ref)"
head_sha="$(metadata_value head_sha)"
base_sha="$(metadata_value base_sha)"

if [[ "${has_changes}" != "true" || "${pr_number}" != "${EXPECTED_PR_NUMBER}" || "${head_ref}" != "${EXPECTED_HEAD_REF}" || "${base_ref}" != "${EXPECTED_BASE_REF}" || "${head_sha}" != "${EXPECTED_HEAD_SHA}" || "${base_sha}" != "${EXPECTED_BASE_SHA}" ]]; then
  echo "Refusing to verify unexpected conflict-resolution artifact metadata." >&2
  exit 1
fi

if [[ ! -s "${patch_path}" ]]; then
  echo "Missing or empty conflict-resolution patch artifact: ${patch_path}" >&2
  exit 1
fi

read_conflicted_files
cp bin/codex-local-pipeline.sh "${trusted_pipeline_path}"
chmod +x "${trusted_pipeline_path}"
trusted_pipeline_sha="$(file_sha256 "${trusted_pipeline_path}")"

git_read_authenticated fetch origin "${base_ref}:refs/remotes/origin/${base_ref}"
git_read_authenticated fetch origin "${head_ref}:refs/remotes/origin/${head_ref}"
unset GH_TOKEN

actual_head_sha="$(git_sanitized rev-parse "refs/remotes/origin/${head_ref}")"
actual_base_sha="$(git_sanitized rev-parse "refs/remotes/origin/${base_ref}")"
if [[ "${actual_head_sha}" != "${head_sha}" || "${actual_base_sha}" != "${base_sha}" ]]; then
  echo "PR branch or base branch moved after conflict generation; rerun /codex-resolve-conflicts." >&2
  exit 1
fi

git_sanitized checkout -B "${head_ref}" "refs/remotes/origin/${head_ref}"

set +e
git_sanitized merge --no-commit --no-ff "refs/remotes/origin/${base_ref}"
merge_status=$?
set -e

actual_conflicts="$(git_sanitized diff --name-only --diff-filter=U | sort -u)"
if [[ "${merge_status}" -eq 0 || -z "${actual_conflicts}" ]]; then
  echo "PR #${pr_number} no longer has merge conflicts with ${base_ref}." >&2
  exit 1
fi

restore_ours_for_patch
git_sanitized apply --binary "${patch_path}"
git_sanitized add -A -- "${conflicted_files[@]}"

remaining_conflicts="$(git_sanitized diff --name-only --diff-filter=U | sort -u)"
if [[ -n "${remaining_conflicts}" ]]; then
  echo "Conflict-resolution patch left unresolved files:" >&2
  printf '%s\n' "${remaining_conflicts}" >&2
  exit 1
fi

echo "Applying Java formatting before verifying and publishing the conflict-resolution patch."
run_sanitized ./gradlew --no-daemon spotlessApply
echo "Running backend Checkstyle probe before creating the conflict-resolution patch."
if ! run_sanitized ./gradlew --no-daemon \
  checkstyleMain \
  checkstyleTest \
  checkstyleTestCommon \
  checkstyleIntegrationTest \
  checkstyleFunctionalTest \
  checkstyleSmokeTest; then
  echo "::warning::Backend Checkstyle probe failed. Trusted verification will capture the failure."
fi
git_sanitized add -A -- "${conflicted_files[@]}"

if grep -R -n -E '^(<<<<<<<|=======|>>>>>>>)' -- "${conflicted_files[@]}" >/tmp/codex-conflict-markers.txt 2>/dev/null; then
  echo "Conflict markers remain in resolved files:" >&2
  cat /tmp/codex-conflict-markers.txt >&2
  exit 1
fi

git_sanitized diff --binary HEAD -- "${conflicted_files[@]}" >"${patch_path}"
patch_sha="$(file_sha256 "${patch_path}")"

local_pipeline_mode="${LOCAL_PIPELINE_MODE:-fast}"
verify_trusted_file "${trusted_pipeline_path}" "${trusted_pipeline_sha}" "pipeline wrapper"
run_sanitized "${trusted_pipeline_path}" "${local_pipeline_mode}" --base "${base_ref}" --no-fetch

{
  echo "has_changes=true"
  echo "pr_number=${pr_number}"
  echo "head_ref=${head_ref}"
  echo "base_ref=${base_ref}"
  echo "head_sha=${head_sha}"
  echo "base_sha=${base_sha}"
  echo "patch_sha=${patch_sha}"
} >"${verification_path}"
