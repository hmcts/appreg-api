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
required_env "PUBLISHED_COMMIT_SHA"
required_env "PR_NUMBER"

status_context="${REQUIRED_STATUS_CONTEXT:-continuous-integration/jenkins/pr-head}"
timeout_seconds="${REQUIRED_STATUS_TIMEOUT_SECONDS:-2700}"
poll_seconds="${REQUIRED_STATUS_POLL_SECONDS:-30}"
deadline=$((SECONDS + timeout_seconds))

gh_api() {
  env -i \
    "HOME=${HOME:-/tmp}" \
    "PATH=${PATH:-/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin}" \
    "LANG=${LANG:-C.UTF-8}" \
    "LC_ALL=${LC_ALL:-${LANG:-C.UTF-8}}" \
    "TERM=${TERM:-xterm}" \
    "GH_TOKEN=${GH_TOKEN}" \
    gh api "$@"
}

status_field() {
  local json_path="$1"
  local field="$2"

  STATUS_JSON_PATH="${json_path}" STATUS_CONTEXT="${status_context}" STATUS_FIELD="${field}" python3 -I - <<'PY'
import json
import os
from pathlib import Path

payload = json.loads(Path(os.environ["STATUS_JSON_PATH"]).read_text(encoding="utf-8"))
context = os.environ["STATUS_CONTEXT"]
field = os.environ["STATUS_FIELD"]

for status in payload.get("statuses", []):
    if status.get("context") == context:
        print(status.get(field) or "")
        break
PY
}

echo "Waiting for required PR status '${status_context}' on ${GITHUB_REPOSITORY}@${PUBLISHED_COMMIT_SHA}."
echo "PR: #${PR_NUMBER}; timeout: ${timeout_seconds}s; poll interval: ${poll_seconds}s."

while true; do
  status_json_path="$(mktemp)"
  gh_api "repos/${GITHUB_REPOSITORY}/commits/${PUBLISHED_COMMIT_SHA}/status" >"${status_json_path}"

  state="$(status_field "${status_json_path}" state)"
  description="$(status_field "${status_json_path}" description)"
  target_url="$(status_field "${status_json_path}" target_url)"

  rm -f "${status_json_path}"

  if [[ -z "${state}" ]]; then
    echo "Required status '${status_context}' has not been created yet."
  else
    echo "Required status '${status_context}' is '${state}': ${description}"
  fi

  case "${state}" in
    success)
      echo "Required status passed: ${target_url}"
      exit 0
      ;;
    failure | error)
      echo "::error::Required status failed: ${status_context}=${state}"
      echo "Description: ${description}"
      echo "Target URL: ${target_url}"
      exit 1
      ;;
  esac

  if (( SECONDS >= deadline )); then
    echo "::error::Timed out waiting for required status '${status_context}'."
    echo "Last state: ${state:-missing}"
    echo "Last description: ${description}"
    echo "Last target URL: ${target_url}"
    exit 1
  fi

  sleep "${poll_seconds}"
done
