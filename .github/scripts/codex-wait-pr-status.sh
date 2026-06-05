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

PR_NUMBER="${PR_NUMBER:-${SONAR_PR_NUMBER:-}}"
required_env "PR_NUMBER"

status_context="${REQUIRED_STATUS_CONTEXT:-continuous-integration/jenkins/pr-head}"
timeout_seconds="${REQUIRED_STATUS_TIMEOUT_SECONDS:-2700}"
poll_seconds="${REQUIRED_STATUS_POLL_SECONDS:-30}"
sonar_host_url="${SONAR_HOST_URL:-https://sonarcloud.io}"
sonar_project_key="${SONAR_PROJECT_KEY:-}"
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

sonar_get() {
  local path="$1"
  local query="$2"

  if [[ -z "${SONAR_TOKEN:-}" || -z "${sonar_project_key}" ]]; then
    return 1
  fi

  curl -fsS -u "${SONAR_TOKEN}:" "${sonar_host_url}${path}?${query}"
}

print_sonar_diagnostics() {
  if [[ -z "${SONAR_TOKEN:-}" || -z "${sonar_project_key}" ]]; then
    echo "Sonar diagnostics skipped because SONAR_TOKEN or SONAR_PROJECT_KEY is not configured."
    return
  fi

  echo
  echo "SonarCloud quality gate for PR #${PR_NUMBER}:"
  if quality_gate_json="$(sonar_get "/api/qualitygates/project_status" "projectKey=${sonar_project_key}&pullRequest=${PR_NUMBER}")"; then
    SONAR_JSON="${quality_gate_json}" python3 -I - <<'PY'
import json
import os

payload = json.loads(os.environ["SONAR_JSON"])
project_status = payload.get("projectStatus", {})
print(f"- Status: {project_status.get('status', 'UNKNOWN')}")
for condition in project_status.get("conditions", []):
    metric = condition.get("metricKey", "unknown")
    status = condition.get("status", "UNKNOWN")
    actual = condition.get("actualValue", "")
    threshold = condition.get("errorThreshold", "")
    comparator = condition.get("comparator", "")
    detail = f" actual={actual}" if actual else ""
    if threshold or comparator:
        detail += f" threshold={comparator} {threshold}".rstrip()
    print(f"- {metric}: {status}{detail}")
PY
  else
    echo "- Unable to fetch SonarCloud quality gate."
  fi

  echo
  echo "Open SonarCloud issues for PR #${PR_NUMBER} (first 20):"
  if issues_json="$(
    sonar_get \
      "/api/issues/search" \
      "componentKeys=${sonar_project_key}&pullRequest=${PR_NUMBER}&resolved=false&ps=20"
  )"; then
    SONAR_JSON="${issues_json}" python3 -I - <<'PY'
import json
import os

payload = json.loads(os.environ["SONAR_JSON"])
issues = payload.get("issues", [])
if not issues:
    print("- No open issues returned by SonarCloud.")
for issue in issues:
    component = issue.get("component", "")
    path = component.split(":", 1)[-1] if ":" in component else component
    line = issue.get("line")
    location = f"{path}:{line}" if line else path
    severity = issue.get("severity") or issue.get("impactSeverity") or "UNKNOWN"
    issue_type = issue.get("type", "UNKNOWN")
    rule = issue.get("rule", "unknown-rule")
    message = " ".join((issue.get("message") or "").split())
    print(f"- [{severity} {issue_type}] {location} {rule}: {message}")
PY
  else
    echo "- Unable to fetch SonarCloud issues."
  fi
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
      print_sonar_diagnostics
      exit 1
      ;;
  esac

  if (( SECONDS >= deadline )); then
    echo "::error::Timed out waiting for required status '${status_context}'."
    echo "Last state: ${state:-missing}"
    echo "Last description: ${description}"
    echo "Last target URL: ${target_url}"
    print_sonar_diagnostics
    exit 1
  fi

  sleep "${poll_seconds}"
done
