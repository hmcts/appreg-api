#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"

  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

required_env "ISSUE_KEY"
required_env "ISSUE_SUMMARY"
required_env "ISSUE_URL"
required_env "GH_TOKEN"
required_env "BOT_PUBLISHER_LOGIN"
required_env "BOT_PUBLISHER_EMAIL"
required_env "OUTPUT_DIR"
required_env "VERIFICATION_DIR"
required_env "EXPECTED_BRANCH_NAME"
required_env "EXPECTED_BASE_SHA"
required_env "JIRA_PUBLISH_MODE"

case "${JIRA_PUBLISH_MODE}" in
  initial)
    expected_branch_head_sha=""
    ;;
  repair)
    required_env "EXPECTED_BRANCH_HEAD_SHA"
    expected_branch_head_sha="${EXPECTED_BRANCH_HEAD_SHA}"
    if [[ ! "${expected_branch_head_sha}" =~ ^[0-9a-f]{40}$ ]]; then
      echo "Invalid expected Jira repair branch SHA: ${expected_branch_head_sha}" >&2
      exit 1
    fi
    ;;
  *)
    echo "JIRA_PUBLISH_MODE must be either initial or repair." >&2
    exit 1
    ;;
esac

default_branch="${DEFAULT_BRANCH:-master}"
publisher_login="${BOT_PUBLISHER_LOGIN}"
publisher_email="${BOT_PUBLISHER_EMAIL}"
output_dir="${OUTPUT_DIR}"
metadata_path="${output_dir}/metadata.env"
patch_path="${output_dir}/changes.patch"
verification_dir="${VERIFICATION_DIR}"
verification_path="${verification_dir}/verification.env"
pr_body_path="${verification_dir}/codex-pr-body.md"
trusted_notify_path="${RUNNER_TEMP:-/tmp}/trusted-notify-jira-automation.py"
sanitized_home="${RUNNER_TEMP:-/tmp}/codex-jira-publish-home"
sanitized_tmp="${RUNNER_TEMP:-/tmp}/codex-jira-publish-tmp"

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

run_notify() {
  env -i \
    "HOME=${sanitized_home}" \
    "PATH=${PATH:-/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin}" \
    "LANG=${LANG:-C.UTF-8}" \
    "LC_ALL=${LC_ALL:-${LANG:-C.UTF-8}}" \
    "TERM=${TERM:-xterm}" \
    "TMPDIR=${sanitized_tmp}" \
    "ISSUE_KEY=${ISSUE_KEY}" \
    "ISSUE_SUMMARY=${ISSUE_SUMMARY}" \
    "ISSUE_URL=${ISSUE_URL}" \
    "GITHUB_REPOSITORY=${GITHUB_REPOSITORY}" \
    "GITHUB_ACTOR=${GITHUB_ACTOR:-}" \
    "GITHUB_RUN_ID=${GITHUB_RUN_ID:-}" \
    "GITHUB_SERVER_URL=${GITHUB_SERVER_URL:-https://github.com}" \
    "CODEX_JIRA_PR_NOTIFY_URL=${CODEX_JIRA_PR_NOTIFY_URL:-}" \
    "CODEX_JIRA_PR_NOTIFY_TIMEOUT_SECONDS=${CODEX_JIRA_PR_NOTIFY_TIMEOUT_SECONDS:-10}" \
    python3 -I "${trusted_notify_path}" "$@"
}

mkdir -p "${sanitized_home}" "${sanitized_tmp}"

cp .github/scripts/notify-jira-automation.py "${trusted_notify_path}"

branch_name="$(metadata_value branch_name)"
verified_branch_name="$(verification_value branch_name)"
verified_base_sha="$(verification_value base_sha)"
verified_patch_sha="$(verification_value patch_sha)"

if [[ "${branch_name}" != "${EXPECTED_BRANCH_NAME}" || "${branch_name}" != "${verified_branch_name}" || "${branch_name}" != codex/* ]]; then
  echo "Refusing to publish unexpected Codex branch name: ${branch_name}" >&2
  exit 1
fi

if [[ "${verified_base_sha}" != "${EXPECTED_BASE_SHA}" ]]; then
  echo "Refusing to publish Codex patch because the verified base SHA does not match the archived source revision." >&2
  exit 1
fi

if [[ ! -s "${patch_path}" ]]; then
  echo "Missing or empty Codex patch artifact: ${patch_path}" >&2
  exit 1
fi

actual_patch_sha="$(file_sha256 "${patch_path}")"
if [[ -z "${verified_patch_sha}" || "${actual_patch_sha}" != "${verified_patch_sha}" ]]; then
  echo "Refusing to publish Codex patch because it does not match the verified patch hash." >&2
  exit 1
fi

if [[ ! -s "${pr_body_path}" ]]; then
  echo "Missing verified Codex PR body artifact: ${pr_body_path}" >&2
  exit 1
fi

commit_subject="$(
  python3 -I - <<'PY'
import os

issue_key = os.environ["ISSUE_KEY"].strip()
summary = " ".join(os.environ["ISSUE_SUMMARY"].split())
subject = f"{issue_key}: {summary}"
print(subject[:72].rstrip())
PY
)"

remote_base_sha="$(git_authenticated ls-remote --heads origin "refs/heads/${default_branch}" | awk '{print $1}')"
if [[ -z "${remote_base_sha}" || "${remote_base_sha}" != "${verified_base_sha}" ]]; then
  echo "::error title=Source revision moved::The remote ${default_branch} branch moved after credential-free verification. Re-run the workflow; the verified patch will not be applied to a newer base." >&2
  exit 1
fi

remote_branch_sha="$(git_authenticated ls-remote --heads origin "refs/heads/${branch_name}" | awk '{print $1}')"
if [[ "${JIRA_PUBLISH_MODE}" == "initial" && -n "${remote_branch_sha}" ]]; then
  echo "::error title=Generated branch already exists::Initial publication requires refs/heads/${branch_name} to be absent. Re-run with a new generated branch or intervene manually." >&2
  exit 1
fi
if [[ "${JIRA_PUBLISH_MODE}" == "repair" && "${remote_branch_sha}" != "${expected_branch_head_sha}" ]]; then
  echo "::error title=Generated branch moved::The remote ${branch_name} branch no longer matches the trusted published commit ${expected_branch_head_sha}. Re-run the workflow or intervene manually; the repaired patch was not published." >&2
  exit 1
fi

git_authenticated fetch origin "${verified_base_sha}:refs/remotes/origin/${default_branch}"
git_authenticated checkout -B "${default_branch}" "${verified_base_sha}"
git_authenticated checkout -B "${branch_name}"
git_local apply --index --binary "${patch_path}"

git_authenticated \
  -c user.name="${publisher_login}" \
  -c user.email="${publisher_email}" \
  commit \
  -m "${commit_subject}" \
  -m "Jira: ${ISSUE_URL}" \
  -m "Generated by Codex self-hosted runner for ${ISSUE_KEY}."

latest_base_sha="$(git_authenticated ls-remote --heads origin "refs/heads/${default_branch}" | awk '{print $1}')"
if [[ "${latest_base_sha}" != "${verified_base_sha}" ]]; then
  echo "::error title=Source revision moved::The remote ${default_branch} branch moved while the verified commit was being prepared. Re-run the workflow; no stale branch will be pushed." >&2
  exit 1
fi

latest_branch_sha="$(git_authenticated ls-remote --heads origin "refs/heads/${branch_name}" | awk '{print $1}')"
if [[ "${JIRA_PUBLISH_MODE}" == "initial" ]]; then
  if [[ -n "${latest_branch_sha}" ]]; then
    echo "::error title=Generated branch appeared::The remote ${branch_name} branch was created while the verified commit was being prepared. Re-run with a new generated branch or intervene manually." >&2
    exit 1
  fi
  git_authenticated push \
    --force-with-lease="refs/heads/${branch_name}:" \
    --set-upstream origin "${branch_name}"
else
  if [[ "${latest_branch_sha}" != "${expected_branch_head_sha}" ]]; then
    echo "::error title=Generated branch moved::The remote ${branch_name} branch moved while the repaired commit was being prepared. Re-run the workflow or intervene manually; the repaired patch was not published." >&2
    exit 1
  fi
  git_authenticated push \
    --force-with-lease="refs/heads/${branch_name}:${expected_branch_head_sha}" \
    --set-upstream origin "${branch_name}"
fi

pr_url="$(gh_authenticated pr list --repo "${GITHUB_REPOSITORY}" --head "${branch_name}" --state open --json url --jq '.[0].url // empty')"
if [[ -z "${pr_url}" ]]; then
  pr_url="$(
    gh_authenticated pr create \
      --repo "${GITHUB_REPOSITORY}" \
      --base "${default_branch}" \
      --head "${branch_name}" \
      --title "${ISSUE_KEY}: ${ISSUE_SUMMARY}" \
      --body-file "${pr_body_path}"
  )"
fi

pr_number="$(gh_authenticated pr view "${pr_url}" --repo "${GITHUB_REPOSITORY}" --json number --jq '.number')"
if [[ -z "${pr_number}" ]]; then
  echo "Unable to determine PR number for ${pr_url}" >&2
  exit 1
fi

commit_sha="$(git_local rev-parse HEAD)"
run_notify \
  --pr-url "${pr_url}" \
  --branch-name "${branch_name}" \
  --commit-sha "${commit_sha}"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "branch_name=${branch_name}"
    echo "commit_sha=${commit_sha}"
    echo "pr_url=${pr_url}"
    echo "pr_number=${pr_number}"
  } >>"${GITHUB_OUTPUT}"
fi
