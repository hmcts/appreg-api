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
required_env "PUBLISHED_COMMIT_SHA"

PR_NUMBER="${PR_NUMBER:-${SONAR_PR_NUMBER:-}}"
required_env "PR_NUMBER"

sonar_host_url="${SONAR_HOST_URL:-https://sonarcloud.io}"
sonar_host_url="${sonar_host_url%/}"
timeout_seconds="${SONAR_QUALITY_GATE_API_TIMEOUT_SECONDS:-120}"
poll_seconds="${SONAR_QUALITY_GATE_API_POLL_SECONDS:-10}"
max_attempts="${SONAR_QUALITY_GATE_API_MAX_ATTEMPTS:-0}"

for value_name in timeout_seconds poll_seconds max_attempts; do
  value="${!value_name}"
  if [[ ! "${value}" =~ ^[0-9]+$ ]]; then
    echo "Invalid numeric polling value for ${value_name}: ${value}" >&2
    exit 1
  fi
done
if ((timeout_seconds == 0)); then
  echo "SONAR_QUALITY_GATE_API_TIMEOUT_SECONDS must be greater than zero." >&2
  exit 1
fi
if [[ ! "${PUBLISHED_COMMIT_SHA}" =~ ^[0-9a-fA-F]{40}$ ]]; then
  echo "PUBLISHED_COMMIT_SHA must be a complete 40-character commit SHA." >&2
  exit 1
fi

deadline=$((SECONDS + timeout_seconds))
attempt=0
sonar_auth_config="$(mktemp)"
chmod 600 "${sonar_auth_config}"
printf 'user = "%s:"\n' "${SONAR_TOKEN}" >"${sonar_auth_config}"
unset SONAR_TOKEN
trap 'rm -f "${sonar_auth_config}"' EXIT

sonar_get() {
  local path="$1"
  shift

  curl -fsS --config "${sonar_auth_config}" --get "$@" "${sonar_host_url}${path}"
}

analysis_id_for_revision() {
  local json_path="$1"

  SONAR_JSON_PATH="${json_path}" EXPECTED_REVISION="${PUBLISHED_COMMIT_SHA}" python3 -I - <<'PY'
import json
import os
import sys
from pathlib import Path

try:
    payload = json.loads(Path(os.environ["SONAR_JSON_PATH"]).read_text(encoding="utf-8"))
except (OSError, json.JSONDecodeError) as exc:
    print(f"Malformed Sonar analysis response: {exc}", file=sys.stderr)
    raise SystemExit(2)

analyses = payload.get("analyses")
if not isinstance(analyses, list):
    print("Malformed Sonar analysis response: 'analyses' must be a list.", file=sys.stderr)
    raise SystemExit(2)

expected = os.environ["EXPECTED_REVISION"]
for analysis in analyses:
    if not isinstance(analysis, dict):
        print("Malformed Sonar analysis response: analysis entry must be an object.", file=sys.stderr)
        raise SystemExit(2)
    revision = analysis.get("revision")
    key = analysis.get("key")
    if revision == expected:
        if not isinstance(key, str) or not key:
            print("Malformed Sonar analysis response: matching analysis has no key.", file=sys.stderr)
            raise SystemExit(2)
        print(key)
        break
PY
}

quality_gate_status() {
  local json_path="$1"

  SONAR_JSON_PATH="${json_path}" python3 -I - <<'PY'
import json
import os
import sys
from pathlib import Path

try:
    payload = json.loads(Path(os.environ["SONAR_JSON_PATH"]).read_text(encoding="utf-8"))
except (OSError, json.JSONDecodeError) as exc:
    print(f"Malformed Sonar quality-gate response: {exc}", file=sys.stderr)
    raise SystemExit(2)

project_status = payload.get("projectStatus")
if not isinstance(project_status, dict):
    print("Malformed Sonar quality-gate response: 'projectStatus' must be an object.", file=sys.stderr)
    raise SystemExit(2)
status = project_status.get("status")
if not isinstance(status, str) or not status:
    print("Malformed Sonar quality-gate response: status is missing.", file=sys.stderr)
    raise SystemExit(2)
print(status)
PY
}

print_quality_gate() {
  local json_path="$1"

  SONAR_JSON_PATH="${json_path}" python3 -I - <<'PY'
import json
import os
from pathlib import Path

payload = json.loads(Path(os.environ["SONAR_JSON_PATH"]).read_text(encoding="utf-8"))
project_status = payload["projectStatus"]
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
if not isinstance(issues, list):
    print("- SonarCloud returned malformed issue data.")
    raise SystemExit(0)
if not issues:
    print("- No open issues returned by SonarCloud.")
for issue in issues:
    if not isinstance(issue, dict):
        continue
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

echo "Waiting for SonarCloud analysis of ${PUBLISHED_COMMIT_SHA} on PR #${PR_NUMBER} in project ${SONAR_PROJECT_KEY}."

while true; do
  attempt=$((attempt + 1))
  analyses_json_path="$(mktemp)"
  analysis_id=""

  if sonar_get \
    "/api/project_analyses/search" \
    --data-urlencode "project=${SONAR_PROJECT_KEY}" \
    --data-urlencode "pullRequest=${PR_NUMBER}" \
    --data-urlencode "p=1" \
    --data-urlencode "ps=100" \
    >"${analyses_json_path}"; then
    set +e
    analysis_id="$(analysis_id_for_revision "${analyses_json_path}")"
    parse_status=$?
    set -e
    rm -f "${analyses_json_path}"
    if ((parse_status != 0)); then
      echo "::error::SonarCloud returned malformed analysis data; refusing to use an ambiguous quality-gate result." >&2
      exit 1
    fi
  else
    rm -f "${analyses_json_path}"
    echo "SonarCloud analysis list is not available yet."
  fi

  if [[ -n "${analysis_id}" ]]; then
    quality_gate_json_path="$(mktemp)"
    if sonar_get \
      "/api/qualitygates/project_status" \
      --data-urlencode "analysisId=${analysis_id}" \
      >"${quality_gate_json_path}"; then
      set +e
      status="$(quality_gate_status "${quality_gate_json_path}")"
      parse_status=$?
      set -e
      if ((parse_status != 0)); then
        rm -f "${quality_gate_json_path}"
        echo "::error::SonarCloud returned malformed quality-gate data for analysis ${analysis_id}." >&2
        exit 1
      fi
      print_quality_gate "${quality_gate_json_path}"
      rm -f "${quality_gate_json_path}"

      case "${status}" in
        OK)
          echo "SonarCloud quality gate passed for analysis ${analysis_id} at ${PUBLISHED_COMMIT_SHA}."
          exit 0
          ;;
        ERROR)
          echo "::error::SonarCloud quality gate failed for analysis ${analysis_id} at ${PUBLISHED_COMMIT_SHA}."
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
        NONE)
          echo "SonarCloud analysis ${analysis_id} exists, but its quality gate is still pending."
          ;;
        *)
          echo "::error::Unexpected SonarCloud quality-gate status '${status}' for analysis ${analysis_id}." >&2
          exit 1
          ;;
      esac
    else
      rm -f "${quality_gate_json_path}"
      echo "The quality gate for current analysis ${analysis_id} is not available yet."
    fi
  else
    echo "No SonarCloud analysis for published revision ${PUBLISHED_COMMIT_SHA} is available yet; older analyses are ignored."
  fi

  if ((SECONDS >= deadline)) || ((max_attempts > 0 && attempt >= max_attempts)); then
    echo "::error::Timed out waiting for SonarCloud analysis of ${PUBLISHED_COMMIT_SHA} on PR #${PR_NUMBER}."
    exit 1
  fi

  sleep "${poll_seconds}"
done
