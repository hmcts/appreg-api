#!/usr/bin/env bash

# Shared runtime helpers for Codex invocations authenticated through
# openai/codex-action's local Responses API proxy.

prepare_codex_action_runtime() {
  local runtime_path
  for runtime_path in "$@"; do
    [[ -e "${runtime_path}" ]] || continue
    chmod -R g+rwX "${runtime_path}"
    find "${runtime_path}" -type d -exec chmod g+s {} +
  done
}

capture_codex_collector() {
  local collector_source="$1"
  local source_dir trusted_dir collector_path

  source_dir="$(cd "$(dirname "${collector_source}")" && pwd)"
  trusted_dir="${CODEX_TRUSTED_DIR_ROOT:-/opt/codex-trusted}/${GITHUB_RUN_ID:-manual}-${GITHUB_RUN_ATTEMPT:-1}-${GITHUB_JOB:-job}"
  collector_path="${trusted_dir}/$(basename "${collector_source}")"

  mkdir -p "${trusted_dir}"
  chmod 0755 "${trusted_dir}"
  install -m 0555 "${collector_source}" "${collector_path}"
  install -m 0444 "${source_dir}/codex-action-runtime.sh" "${trusted_dir}/codex-action-runtime.sh"

  printf '%s\n' "${collector_path}"
}

capture_codex_metadata() {
  local source_path="$1"
  local collector_path="$2"
  local target_name="$3"
  local trusted_dir target_path

  trusted_dir="$(dirname "${collector_path}")"
  target_path="${trusted_dir}/${target_name}"
  install -m 0444 "${source_path}" "${target_path}"

  printf '%s\n' "${target_path}"
}
