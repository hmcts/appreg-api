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

output_dir="${OUTPUT_DIR}"
metadata_path="${output_dir}/metadata.env"
patch_path="${output_dir}/changes.patch"
final_message_path="${output_dir}/codex-final-message.md"
verification_dir="${VERIFICATION_DIR}"
verification_path="${verification_dir}/verification.env"
comment_body_path="${verification_dir}/codex-review-comment.md"
sanitized_home="${RUNNER_TEMP:-/tmp}/codex-review-publish-home"
sanitized_tmp="${RUNNER_TEMP:-/tmp}/codex-review-publish-tmp"

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

mkdir -p "${sanitized_home}" "${sanitized_tmp}"

has_changes="$(metadata_value has_changes)"
pr_number="$(metadata_value pr_number)"
head_ref="$(metadata_value head_ref)"
verified_has_changes="$(verification_value has_changes)"
verified_pr_number="$(verification_value pr_number)"
verified_head_ref="$(verification_value head_ref)"

if [[ "${has_changes}" != "true" ]]; then
  if [[ -z "${pr_number}" || -z "${head_ref}" ]]; then
    echo "Codex review generation skipped before selecting a PR branch."
    exit 0
  fi
  if [[ "${pr_number}" != "${EXPECTED_PR_NUMBER}" || "${head_ref}" != "${EXPECTED_HEAD_REF}" || "${head_ref}" != codex/* ]]; then
    echo "Refusing to publish unexpected review artifact metadata." >&2
    exit 1
  fi
  if [[ "${verified_has_changes}" != "false" || "${verified_pr_number}" != "${pr_number}" || "${verified_head_ref}" != "${head_ref}" ]]; then
    echo "Refusing to publish unverified no-change review result." >&2
    exit 1
  fi
  if [[ -s "${comment_body_path}" ]]; then
    gh_authenticated pr comment "${pr_number}" --repo "${GITHUB_REPOSITORY}" --body-file "${comment_body_path}"
  fi
  echo "Codex produced no review-feedback changes for PR #${pr_number}."
  exit 0
fi

if [[ "${pr_number}" != "${EXPECTED_PR_NUMBER}" || "${head_ref}" != "${EXPECTED_HEAD_REF}" || "${head_ref}" != codex/* ]]; then
  echo "Refusing to publish unexpected review artifact metadata." >&2
  exit 1
fi

if [[ "${verified_has_changes}" != "true" || "${verified_pr_number}" != "${pr_number}" || "${verified_head_ref}" != "${head_ref}" ]]; then
  echo "Refusing to publish unexpected review artifact metadata." >&2
  exit 1
fi

if [[ ! -s "${patch_path}" ]]; then
  echo "Missing or empty Codex review patch artifact: ${patch_path}" >&2
  exit 1
fi

verified_patch_sha="$(verification_value patch_sha)"
actual_patch_sha="$(file_sha256 "${patch_path}")"
if [[ -z "${verified_patch_sha}" || "${actual_patch_sha}" != "${verified_patch_sha}" ]]; then
  echo "Refusing to publish Codex review patch because it does not match the verified patch hash." >&2
  exit 1
fi

commit_subject="Address Codex review feedback on PR #${EXPECTED_PR_NUMBER}"
commit_subject="${commit_subject:0:72}"

git_authenticated fetch origin "${head_ref}:refs/remotes/origin/${head_ref}"
git_authenticated checkout -B "${head_ref}" "origin/${head_ref}"
git_local apply --index --binary "${patch_path}"

comment_url="$(metadata_value comment_url)"
comment_author="$(metadata_value comment_author)"

git_authenticated \
  -c user.name="github-actions[bot]" \
  -c user.email="41898282+github-actions[bot]@users.noreply.github.com" \
  commit \
  -m "${commit_subject}" \
  -m "Feedback: ${comment_url}" \
  -m "Generated by Codex self-hosted runner."

git_authenticated push origin "${head_ref}"
commit_sha="$(git_local rev-parse HEAD)"

{
  echo "Codex pushed an update for review feedback from @${comment_author}."
  echo
  echo "Feedback: ${comment_url}"
  echo
  echo "Commit: ${commit_sha}"
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
