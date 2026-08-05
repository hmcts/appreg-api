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
required_env "PR_NUMBER"
required_env "HEAD_REF"
required_env "BASE_REF"
required_env "HEAD_SHA"
required_env "BASE_SHA"
required_env "CONFLICTED_FILES"

artifact_dir="${RUNNER_TEMP:-/tmp}/codex-conflict-generate-${GITHUB_RUN_ID:-manual}-${GITHUB_RUN_ATTEMPT:-1}"
pr_json_path="${artifact_dir}/pull-request.json"
prompt_path="${artifact_dir}/codex-merge-conflict-prompt.md"
conflicted_files_path="${artifact_dir}/conflicted-files.txt"
sanitized_home="${artifact_dir}/sanitized-home"
sanitized_tmp="${artifact_dir}/sanitized-tmp"
sanitized_runner_temp="${artifact_dir}/sanitized-runner-temp"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
schema_source="${script_dir}/../schemas/codex-patch-result.schema.json"
exporter_source="${script_dir}/codex-patch-export.sh"

# shellcheck source=.github/scripts/codex-action-runtime.sh
source "${script_dir}/codex-action-runtime.sh"

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

gh_read_authenticated() {
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
  mapfile -t conflicted_files < <(printf '%s\n' "${CONFLICTED_FILES}" | sed '/^[[:space:]]*$/d' | sort -u)

  if [[ "${#conflicted_files[@]}" -eq 0 ]]; then
    echo "No conflicted files were provided." >&2
    exit 1
  fi

  for path in "${conflicted_files[@]}"; do
    if [[ "${path}" == /* || "${path}" == *..* || "${path}" == *$'\n'* || "${path}" == *$'\r'* ]]; then
      echo "Unsafe conflicted file path: ${path}" >&2
      exit 1
    fi
  done
}

write_prompt() {
  PROMPT_PATH="${prompt_path}" PR_JSON_PATH="${pr_json_path}" CONFLICTED_FILES_PATH="${conflicted_files_path}" python3 - <<'PY'
import json
import os
from pathlib import Path

with open(os.environ["PR_JSON_PATH"], encoding="utf-8") as pr_file:
    pull_request = json.load(pr_file)

conflicted_files = Path(os.environ["CONFLICTED_FILES_PATH"]).read_text(encoding="utf-8").strip()

prompt = f"""You are Codex running non-interactively in GitHub Actions on a self-hosted runner.

Resolve the actual merge conflicts in this Java/Gradle backend pull request.

Operational rules:
- The working tree is already in a conflicted merge state.
- Resolve only the merge conflicts between the PR branch and the base branch.
- Resolve conflicts conservatively. Treat the base branch as the source of truth.
- Prefer base branch behavior by default.
- Re-apply PR branch changes only where they are clearly compatible with the base branch
  and necessary for the PR's stated intent.
- Do not introduce new behavior beyond what is necessary to resolve the conflict.
- If a PR-side change cannot be safely reconciled, prefer the base branch and call this
  out in your final message for human review.
- For each conflicted file, briefly explain in your final message whether you chose the
  base branch version, the PR branch version, or a combined resolution, and why.
- Do not make unrelated product changes, do not refactor unrelated code, and do not alter this automation.
- Do not include secrets, tokens, credentials, PII, runner file contents, environment variables, or auth material in patches, PR bodies, comments, logs, or artifacts.
- Preserve existing Java, Spring, Gradle, Flyway, test, API, and HMCTS coding patterns.
- Run lightweight targeted checks you can reasonably run, such as `git diff --check`, source inspection, or focused non-Gradle commands.
- Do not run `./gradlew`, `gradle`, or `./bin/codex-local-pipeline.sh` inside the Codex merge-conflict sandbox. Gradle needs cache and local socket behavior that the sandbox intentionally blocks; trusted workflow jobs run Gradle verification after Codex exits.
- Backend formatting is not fully covered by Spotless. Before finishing, check Java Checkstyle-sensitive formatting manually.
- In particular, Checkstyle `RightCurlyAlone` requires closing braces to be alone on their own line, including lambda and assertion blocks.
- Leave the working tree with no conflict markers and no unmerged files.
- Do not push branches, open pull requests, or comment on GitHub. The workflow handles publishing in a separate trusted job.

Pull request:
- Number: {os.environ["PR_NUMBER"]}
- URL: {pull_request["html_url"]}
- Title: {pull_request["title"]}
- Branch: {os.environ["HEAD_REF"]}
- Base branch: {os.environ["BASE_REF"]}

Conflicted files:
{conflicted_files}
"""

Path(os.environ["PROMPT_PATH"]).write_text(prompt, encoding="utf-8")
PY
}

mkdir -p "${artifact_dir}" "${sanitized_home}" "${sanitized_tmp}" "${sanitized_runner_temp}"
schema_path="$(capture_codex_patch_schema "${schema_source}" "${artifact_dir}")"
exporter_path="$(capture_codex_patch_exporter "${exporter_source}" "${artifact_dir}")"
read_conflicted_files
printf '%s\n' "${conflicted_files[@]}" >"${conflicted_files_path}"

gh_read_authenticated api "repos/${GITHUB_REPOSITORY}/pulls/${PR_NUMBER}" >"${pr_json_path}"

git_read_authenticated fetch origin "${BASE_REF}:refs/remotes/origin/${BASE_REF}"
git_read_authenticated fetch origin "${HEAD_REF}:refs/remotes/origin/${HEAD_REF}"

actual_head_sha="$(git_sanitized rev-parse "refs/remotes/origin/${HEAD_REF}")"
actual_base_sha="$(git_sanitized rev-parse "refs/remotes/origin/${BASE_REF}")"
if [[ "${actual_head_sha}" != "${HEAD_SHA}" || "${actual_base_sha}" != "${BASE_SHA}" ]]; then
  echo "PR branch or base branch moved after conflict detection; rerun /codex-resolve-conflicts." >&2
  exit 1
fi

git_sanitized checkout -B "${HEAD_REF}" "refs/remotes/origin/${HEAD_REF}"

set +e
git_sanitized merge --no-commit --no-ff "refs/remotes/origin/${BASE_REF}"
merge_status=$?
set -e

actual_conflicts="$(git_sanitized diff --name-only --diff-filter=U | sort -u)"
if [[ "${merge_status}" -eq 0 || -z "${actual_conflicts}" ]]; then
  echo "PR #${PR_NUMBER} no longer has merge conflicts with ${BASE_REF}." >&2
  exit 1
fi

write_prompt
unset GH_TOKEN

schema_path="$(prepare_codex_patch_contract "${prompt_path}" "${schema_path}" "${exporter_path}" "${artifact_dir}" conflicted-files "${conflicted_files_path}")"
prepare_codex_action_runtime "${PWD}"
echo "Running Codex merge-conflict resolution for PR #${PR_NUMBER} on ${HEAD_REF}"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "prompt_path=${prompt_path}"
    echo "schema_path=${schema_path}"
  } >>"${GITHUB_OUTPUT}"
fi
