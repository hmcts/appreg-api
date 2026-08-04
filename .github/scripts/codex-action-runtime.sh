#!/usr/bin/env bash

# Shared runtime helpers for Codex invocations authenticated through
# openai/codex-action's local Responses API proxy.

prepare_codex_action_runtime() {
  if [[ -z "${CODEX_RUN_USER:-}" ]]; then
    return 0
  fi

  local runtime_path
  for runtime_path in "$@"; do
    [[ -e "${runtime_path}" ]] || continue
    chmod -R g+rwX "${runtime_path}"
    find "${runtime_path}" -type d -exec chmod g+s {} +
  done
}

run_codex_as_action_user() {
  if [[ -n "${CODEX_RUN_USER:-}" ]]; then
    sudo -n -u "${CODEX_RUN_USER}" -- "$@"
  else
    "$@"
  fi
}

shutdown_codex_action_proxy() {
  [[ -n "${CODEX_ACTION_HOME:-}" ]] || return 0
  [[ -n "${GITHUB_RUN_ID:-}" ]] || {
    echo "Missing GITHUB_RUN_ID while shutting down the Codex Action proxy." >&2
    return 1
  }

  local server_info_path="${CODEX_ACTION_HOME}/${GITHUB_RUN_ID}.json"
  if [[ ! -s "${server_info_path}" ]]; then
    echo "Missing Codex Action proxy server information: ${server_info_path}" >&2
    return 1
  fi

  local proxy_port
  proxy_port="$(jq -r '.port // empty' "${server_info_path}")"
  if [[ ! "${proxy_port}" =~ ^[0-9]+$ ]]; then
    echo "Invalid Codex Action proxy port in ${server_info_path}." >&2
    return 1
  fi

  curl --fail --silent --show-error "http://127.0.0.1:${proxy_port}/shutdown" >/dev/null
}

arm_codex_action_proxy_shutdown() {
  if [[ -n "${CODEX_ACTION_HOME:-}" ]]; then
    trap shutdown_codex_action_proxy EXIT
  fi
}

disarm_codex_action_proxy_shutdown() {
  trap - EXIT
}
