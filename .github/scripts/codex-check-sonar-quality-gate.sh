#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"

  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

required_env "SONAR_TOKEN"
required_env "SONAR_PROJECT_KEY"

PR_NUMBER="${PR_NUMBER:-${SONAR_PR_NUMBER:-}}"
required_env "PR_NUMBER"

sonar_host_url="${SONAR_HOST_URL:-https://sonarcloud.io}"
sonar_host_url="${sonar_host_url%/}"
timeout_seconds="${SONAR_QUALITY_GATE_API_TIMEOUT_SECONDS:-120}"
poll_seconds="${SONAR_QUALITY_GATE_API_POLL_SECONDS:-10}"
deadline=$((SECONDS + timeout_seconds))

sonar_get() {
  local path="$1"
  shift

  curl -fsS -u "${SONAR_TOKEN}:" --get "$@" "${sonar_host_url}${path}"
}

quality_gate_status() {
  local json_path="$1"

  SONAR_JSON_PATH="${json_path}" python3 -I - <<'PY'
import json
import os
from pathlib import Path

payload = json.loads(Path(os.environ["SONAR_JSON_PATH"]).read_text(encoding="utf-8"))
print(payload.get("projectStatus", {}).get("status", ""))
PY
}

print_quality_gate() {
  local json_path="$1"

  SONAR_JSON_PATH="${json_path}" python3 -I - <<'PY'
import json
import os
from pathlib import Path

payload = json.loads(Path(os.environ["SONAR_JSON_PATH"]).read_text(encoding="utf-8"))
project_status = payload.get("projectStatus", {})
print(f"SonarCloud quality gate status: {project_status.get('status', 'UNKNOWN')}")
for condition in project_status.get("conditions", []):
    metric = condition.get("metricKey", "unknown")
    status = condition.get("status", "UNKNOWN")
    actual = condition.get("actualValue", "")
    comparator = condition.get("comparator", "")
    threshold = condition.get("errorThreshold", "")
    detail = f" actual={actual}" if actual else ""
    if comparator or threshold:
        detail += f" threshold={comparator} {threshold}".rstrip()
    print(f"- {metric}: {status}{detail}")
PY
}

print_open_issues() {
  local issues_path="$1"

  SONAR_JSON_PATH="${issues_path}" python3 -I - <<'PY'
import json
import os
from pathlib import Path

payload = json.loads(Path(os.environ["SONAR_JSON_PATH"]).read_text(encoding="utf-8"))
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
}

echo "Checking SonarCloud quality gate for PR #${PR_NUMBER} in project ${SONAR_PROJECT_KEY}."

while true; do
  quality_gate_json_path="$(mktemp)"

  if sonar_get \
    "/api/qualitygates/project_status" \
    --data-urlencode "projectKey=${SONAR_PROJECT_KEY}" \
    --data-urlencode "pullRequest=${PR_NUMBER}" \
    >"${quality_gate_json_path}"; then
    status="$(quality_gate_status "${quality_gate_json_path}")"
    print_quality_gate "${quality_gate_json_path}"
    rm -f "${quality_gate_json_path}"

    case "${status}" in
      OK)
        echo "SonarCloud quality gate passed."
        exit 0
        ;;
      ERROR)
        echo "::error::SonarCloud quality gate failed for PR #${PR_NUMBER}."
        issues_json_path="$(mktemp)"
        if sonar_get \
          "/api/issues/search" \
          --data-urlencode "componentKeys=${SONAR_PROJECT_KEY}" \
          --data-urlencode "pullRequest=${PR_NUMBER}" \
          --data-urlencode "resolved=false" \
          --data-urlencode "ps=50" \
          >"${issues_json_path}"; then
          echo
          echo "Open SonarCloud issues for PR #${PR_NUMBER}:"
          print_open_issues "${issues_json_path}"
        else
          echo "Unable to fetch open SonarCloud issues for PR #${PR_NUMBER}." >&2
        fi
        rm -f "${issues_json_path}"
        exit 1
        ;;
    esac

    echo "SonarCloud quality gate status '${status:-missing}' is not final yet."
  else
    rm -f "${quality_gate_json_path}"
    echo "SonarCloud quality gate is not available yet."
  fi

  if ((SECONDS >= deadline)); then
    echo "::error::Timed out waiting for SonarCloud quality gate for PR #${PR_NUMBER}."
    exit 1
  fi

  sleep "${poll_seconds}"
done
