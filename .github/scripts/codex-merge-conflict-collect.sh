#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

for name in OUTPUT_DIR FINAL_MESSAGE_PATH CONFLICTED_FILES_PATH PR_NUMBER HEAD_REF BASE_REF HEAD_SHA BASE_SHA; do
  required_env "${name}"
done

output_dir="${OUTPUT_DIR}"
final_message_path="${FINAL_MESSAGE_PATH}"
conflicted_files_path="${CONFLICTED_FILES_PATH}"
patch_path="${output_dir}/changes.patch"
comment_body_path="${output_dir}/codex-comment.md"
metadata_path="${output_dir}/metadata.env"
usage_summary_path="${output_dir}/codex-usage-summary.json"
sanitized_home="${RUNNER_TEMP:-/tmp}/codex-conflict-collect-home"
sanitized_tmp="${RUNNER_TEMP:-/tmp}/codex-conflict-collect-tmp"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# shellcheck source=.github/scripts/codex-usage-metrics.sh
source "${script_dir}/codex-usage-metrics.sh"

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
if [[ ! -s "${conflicted_files_path}" ]]; then
  echo "Missing conflicted-files list: ${conflicted_files_path}" >&2
  exit 1
fi

conflicted_files=()
while IFS= read -r conflicted_file; do
  [[ -n "${conflicted_file}" ]] && conflicted_files+=("${conflicted_file}")
done <"${conflicted_files_path}"

if [[ ! -s "${final_message_path}" ]]; then
  echo "Codex completed without writing a final message." >"${final_message_path}"
fi

remaining_conflicts="$(git_sanitized diff --name-only --diff-filter=U | sort -u)"
if [[ -n "${remaining_conflicts}" ]]; then
  echo "Codex left unresolved merge conflicts:" >&2
  printf '%s\n' "${remaining_conflicts}" >&2
  exit 1
fi

marker_report="${RUNNER_TEMP:-/tmp}/codex-conflict-markers.txt"
if grep -R -n -E '^(<<<<<<<|=======|>>>>>>>)' -- "${conflicted_files[@]}" >"${marker_report}" 2>/dev/null; then
  echo "Conflict markers remain in resolved files:" >&2
  cat "${marker_report}" >&2
  exit 1
fi

git_sanitized diff --binary HEAD -- "${conflicted_files[@]}" >"${patch_path}"
if [[ ! -s "${patch_path}" ]]; then
  echo "Codex did not produce a conflict-resolution patch." >&2
  exit 1
fi

{
  echo "Codex resolved merge conflicts for this PR."
  echo
  echo "Base branch: ${BASE_REF}"
  echo
  echo "Conflicted files resolved:"
  sed 's/^/- /' "${conflicted_files_path}"
  echo
  echo "Codex final message:"
  echo
  sed -n '1,200p' "${final_message_path}"
} >"${comment_body_path}"

{
  echo "has_changes=true"
  echo "pr_number=${PR_NUMBER}"
  echo "head_ref=${HEAD_REF}"
  echo "base_ref=${BASE_REF}"
  echo "head_sha=${HEAD_SHA}"
  echo "base_sha=${BASE_SHA}"
} >"${metadata_path}"

write_codex_usage_summary /dev/null "${usage_summary_path}" "merge-conflict-resolution" 0
