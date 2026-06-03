#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"

  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

required_env "GH_TOKEN"
required_env "GITHUB_REPOSITORY"
required_env "OUTPUT_DIR"
required_env "VERIFICATION_DIR"
required_env "EXPECTED_PR_NUMBER"
required_env "EXPECTED_HEAD_REF"
required_env "EXPECTED_BASE_REF"
required_env "EXPECTED_HEAD_SHA"
required_env "EXPECTED_BASE_SHA"

output_dir="${OUTPUT_DIR}"
verification_dir="${VERIFICATION_DIR}"
metadata_path="${output_dir}/metadata.env"
conflicted_files_path="${output_dir}/conflicted-files.txt"
final_message_path="${output_dir}/codex-final-message.md"
verification_path="${verification_dir}/verification.env"
patch_path="${verification_dir}/changes.patch"
comment_body_path="${verification_dir}/codex-conflict-comment.md"
sanitized_home="${RUNNER_TEMP:-/tmp}/codex-conflict-publish-home"
sanitized_tmp="${RUNNER_TEMP:-/tmp}/codex-conflict-publish-tmp"

metadata_value() {
  local key="$1"
  awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "${metadata_path}"
}

verification_value() {
  local key="$1"
  awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "${verification_path}"
}

file_sha256() {
  local path="$1"

  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "${path}" | awk '{print $1}'
  else
    shasum -a 256 "${path}" | awk '{print $1}'
  fi
}

git_authenticated() {
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

git_local() {
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
    git \
    -c core.hooksPath=/dev/null \
    -c credential.helper= \
    -c protocol.file.allow=never \
    "$@"
}

gh_authenticated() {
  env -i \
    "HOME=${sanitized_home}" \
    "PATH=${PATH:-/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin}" \
    "LANG=${LANG:-C.UTF-8}" \
    "LC_ALL=${LC_ALL:-${LANG:-C.UTF-8}}" \
    "TERM=${TERM:-xterm}" \
    "TMPDIR=${sanitized_tmp}" \
    "GH_TOKEN=${GH_TOKEN}" \
    gh "$@"
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
    if git_local cat-file -e "HEAD:${path}" 2>/dev/null; then
      git_local checkout --ours -- "${path}" 2>/dev/null || git_local restore --source=HEAD --worktree --staged -- "${path}"
      git_local add -- "${path}"
    else
      git_local rm -f --ignore-unmatch -- "${path}" >/dev/null 2>&1 || true
      rm -rf -- "${path}"
    fi
  done
}

mkdir -p "${sanitized_home}" "${sanitized_tmp}"

has_changes="$(metadata_value has_changes)"
pr_number="$(metadata_value pr_number)"
head_ref="$(metadata_value head_ref)"
base_ref="$(metadata_value base_ref)"
head_sha="$(metadata_value head_sha)"
base_sha="$(metadata_value base_sha)"
verified_has_changes="$(verification_value has_changes)"
verified_pr_number="$(verification_value pr_number)"
verified_head_ref="$(verification_value head_ref)"
verified_base_ref="$(verification_value base_ref)"
verified_head_sha="$(verification_value head_sha)"
verified_base_sha="$(verification_value base_sha)"

if [[ "${has_changes}" != "true" || "${verified_has_changes}" != "true" ]]; then
  echo "Refusing to publish missing conflict-resolution changes." >&2
  exit 1
fi

if [[ "${pr_number}" != "${EXPECTED_PR_NUMBER}" || "${head_ref}" != "${EXPECTED_HEAD_REF}" || "${base_ref}" != "${EXPECTED_BASE_REF}" || "${head_sha}" != "${EXPECTED_HEAD_SHA}" || "${base_sha}" != "${EXPECTED_BASE_SHA}" ]]; then
  echo "Refusing to publish unexpected conflict-resolution artifact metadata." >&2
  exit 1
fi

if [[ "${verified_pr_number}" != "${pr_number}" || "${verified_head_ref}" != "${head_ref}" || "${verified_base_ref}" != "${base_ref}" || "${verified_head_sha}" != "${head_sha}" || "${verified_base_sha}" != "${base_sha}" ]]; then
  echo "Refusing to publish unverified conflict-resolution artifact metadata." >&2
  exit 1
fi

if [[ ! -s "${patch_path}" ]]; then
  echo "Missing or empty verified conflict-resolution patch artifact: ${patch_path}" >&2
  exit 1
fi

verified_patch_sha="$(verification_value patch_sha)"
actual_patch_sha="$(file_sha256 "${patch_path}")"
if [[ -z "${verified_patch_sha}" || "${actual_patch_sha}" != "${verified_patch_sha}" ]]; then
  echo "Refusing to publish conflict-resolution patch because it does not match the verified patch hash." >&2
  exit 1
fi

read_conflicted_files

git_authenticated fetch origin "${base_ref}:refs/remotes/origin/${base_ref}"
git_authenticated fetch origin "${head_ref}:refs/remotes/origin/${head_ref}"

actual_head_sha="$(git_local rev-parse "refs/remotes/origin/${head_ref}")"
actual_base_sha="$(git_local rev-parse "refs/remotes/origin/${base_ref}")"
if [[ "${actual_head_sha}" != "${head_sha}" || "${actual_base_sha}" != "${base_sha}" ]]; then
  gh_authenticated pr comment "${pr_number}" --repo "${GITHUB_REPOSITORY}" --body "Codex conflict-resolution publishing stopped because the PR branch or \`${base_ref}\` moved after verification. Re-run \`/codex-resolve-conflicts\` to resolve the current conflicts."
  echo "PR branch or base branch moved after verification; refusing to publish stale conflict resolution." >&2
  exit 1
fi

git_authenticated checkout -B "${head_ref}" "refs/remotes/origin/${head_ref}"

set +e
git_local merge --no-commit --no-ff "refs/remotes/origin/${base_ref}"
merge_status=$?
set -e

actual_conflicts="$(git_local diff --name-only --diff-filter=U | sort -u)"
if [[ "${merge_status}" -eq 0 || -z "${actual_conflicts}" ]]; then
  git_local merge --abort >/dev/null 2>&1 || true
  gh_authenticated pr comment "${pr_number}" --repo "${GITHUB_REPOSITORY}" --body "Codex conflict-resolution publishing stopped because this PR no longer has merge conflicts with \`${base_ref}\`."
  echo "PR #${pr_number} no longer has conflicts with ${base_ref}; no commit pushed."
  exit 0
fi

restore_ours_for_patch
git_local apply --binary "${patch_path}"
git_local add -A -- "${conflicted_files[@]}"

remaining_conflicts="$(git_local diff --name-only --diff-filter=U | sort -u)"
if [[ -n "${remaining_conflicts}" ]]; then
  echo "Verified conflict-resolution patch left unresolved files:" >&2
  printf '%s\n' "${remaining_conflicts}" >&2
  exit 1
fi

git_authenticated \
  -c user.name="github-actions[bot]" \
  -c user.email="41898282+github-actions[bot]@users.noreply.github.com" \
  commit \
  -m "Resolve merge conflicts for PR #${pr_number}" \
  -m "Generated by Codex self-hosted runner."

git_authenticated push origin "${head_ref}"
commit_sha="$(git_local rev-parse HEAD)"

{
  echo "Codex resolved merge conflicts with \`${base_ref}\` and pushed an update."
  echo
  echo "Commit: ${commit_sha}"
  echo
  echo "Resolved files:"
  sed 's/^/- /' "${conflicted_files_path}"
  echo
  echo "Codex final message:"
  echo
  sed -n '1,200p' "${final_message_path}"
} >"${comment_body_path}"

gh_authenticated pr comment "${pr_number}" --repo "${GITHUB_REPOSITORY}" --body-file "${comment_body_path}"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "pr_number=${pr_number}"
    echo "branch_name=${head_ref}"
    echo "commit_sha=${commit_sha}"
  } >>"${GITHUB_OUTPUT}"
fi
