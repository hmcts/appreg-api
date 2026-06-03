#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${APPREG_TEST_CONFIG_FILE:-${SCRIPT_DIR}/appreg_test.config.ini}"
TOKEN_HELPER="${SCRIPT_DIR}/appreg_token_fetcher.sh"
ENV_BASE_URL="${APPREG_BASE_URL:-}"
BASE_URL=""
JOB_TYPE="APPLICATION_LISTS_DATABASE_JOB"
GET_ACCEPT_HEADER="application/vnd.hmcts.appreg.v1+json"
PUT_ACCEPT_HEADER="*/*"
AUTH_HEADER=""
USE_TOKEN="${APPREG_USE_AZURE_TOKEN:-true}"
ADMIN_TOKEN_FILE="${SCRIPT_DIR}/.appref_admin_token"
USED_TOKEN_HELPER="false"
TOKEN_REFRESH_ATTEMPTED="false"

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "Config file not found: ${CONFIG_FILE}" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "${CONFIG_FILE}"

BASE_URL="${ENV_BASE_URL:-${BASE_URL:-}}"

if [[ -z "${BASE_URL}" ]]; then
  echo "BASE_URL is not set" >&2
  exit 1
fi

if [[ -n "${APPREG_BEARER_TOKEN:-}" ]]; then
  AUTH_HEADER="Authorization: Bearer ${APPREG_BEARER_TOKEN}"
elif [[ -n "${APPREG_AUTH_HEADER:-}" ]]; then
  AUTH_HEADER="${APPREG_AUTH_HEADER}"
elif [[ "${USE_TOKEN}" == "true" ]]; then
  if [[ ! -x "${TOKEN_HELPER}" ]]; then
    echo "Token helper is not executable: ${TOKEN_HELPER}" >&2
    exit 1
  fi
  if [[ ! -f "${ADMIN_TOKEN_FILE}" || ! -s "${ADMIN_TOKEN_FILE}" ]]; then
    "${TOKEN_HELPER}" >/dev/null
  fi
  USED_TOKEN_HELPER="true"
  AUTH_HEADER="Authorization: Bearer $(tr -d '\r\n' < "${ADMIN_TOKEN_FILE}")"
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

request() {
  local method="$1"
  local url="$2"
  local name="$3"
  local expected_regex="$4"
  local payload="${5:-}"
  local body_file="${TMP_DIR}/${name}.json"
  local status

  perform_request() {
    local accept_value="${GET_ACCEPT_HEADER}"
    local curl_args

    if [[ "${method}" == "PUT" ]]; then
      accept_value="${PUT_ACCEPT_HEADER}"
    fi

    curl_args=(
      -sS
      -X "${method}"
      -H "Accept: ${accept_value}"
      -H "Content-Type: application/vnd.hmcts.appreg.v1+json"
      -o "${body_file}"
      -w '%{http_code}'
      "${url}"
    )

    if [[ -n "${AUTH_HEADER}" ]]; then
      curl_args=(
        -sS
        -X "${method}"
        -H "Accept: ${accept_value}"
        -H "Content-Type: application/vnd.hmcts.appreg.v1+json"
        -H "${AUTH_HEADER}"
        -o "${body_file}"
        -w '%{http_code}'
        "${url}"
      )
    fi

    if [[ -n "${payload}" ]]; then
      curl_args=("${curl_args[@]}" -d "${payload}")
    fi

    curl "${curl_args[@]}"
  }

  status="$(perform_request)"

  if [[ "${status}" == "401" && "${USED_TOKEN_HELPER}" == "true" && "${TOKEN_REFRESH_ATTEMPTED}" == "false" ]]; then
    echo "Received 401 for ${name}; refreshing admin token and retrying once." >&2
    "${TOKEN_HELPER}" >/dev/null
    AUTH_HEADER="Authorization: Bearer $(tr -d '\r\n' < "${ADMIN_TOKEN_FILE}")"
    TOKEN_REFRESH_ATTEMPTED="true"
    status="$(perform_request)"
  fi

  echo
  echo "=== ${name} ==="
  echo "${method} ${url}"
  echo "HTTP ${status}"

  if [[ -s "${body_file}" ]]; then
    cat "${body_file}"
    echo
  else
    echo "<empty body>"
  fi

  if ! [[ "${status}" =~ ${expected_regex} ]]; then
    echo "Unexpected HTTP status for ${name}: ${status}" >&2
    return 1
  fi
}

assert_enabled() {
  local name="$1"
  local expected="$2"
  local body_file="${TMP_DIR}/${name}.json"

  if ! rg -q "\"enabled\"[[:space:]]*:[[:space:]]*${expected}" "${body_file}"; then
    echo "Expected ${name} response to contain enabled=${expected}" >&2
    return 1
  fi
}

assert_retention_period_days() {
  local name="$1"
  local expected="$2"
  local body_file="${TMP_DIR}/${name}.json"

  if ! rg -q "\"retentionPeriodDays\"[[:space:]]*:[[:space:]]*${expected}" "${body_file}"; then
    echo "Expected ${name} response to contain retentionPeriodDays=${expected}" >&2
    return 1
  fi
}

extract_enabled() {
  local name="$1"
  local body_file="${TMP_DIR}/${name}.json"

  if rg -q '"enabled"[[:space:]]*:[[:space:]]*true' "${body_file}"; then
    printf 'true\n'
    return 0
  fi

  if rg -q '"enabled"[[:space:]]*:[[:space:]]*false' "${body_file}"; then
    printf 'false\n'
    return 0
  fi

  echo "Could not determine enabled state from ${body_file}" >&2
  return 1
}

extract_retention_period_days() {
  local name="$1"
  local body_file="${TMP_DIR}/${name}.json"
  local retention_period_days

  retention_period_days="$(sed -nE 's/.*"retentionPeriodDays"[[:space:]]*:[[:space:]]*([0-9]+).*/\1/p' "${body_file}" | head -n 1)"

  if [[ -n "${retention_period_days}" ]]; then
    printf '%s\n' "${retention_period_days}"
    return 0
  fi

  echo "Could not determine retentionPeriodDays from ${body_file}" >&2
  return 1
}

JOB_URL="${BASE_URL}/admin/jobs/${JOB_TYPE}"
RETENTION_POLICY_URL="${JOB_URL}/retention-policy"

echo "ARCPOC-925 intent:"
echo "- Read the current enabled state for ${JOB_TYPE}."
echo "- Read the current retention period for ${JOB_TYPE}."
echo "- Toggle the job to the opposite state and verify the change."
echo "- Update the retention period and verify it via the retention-policy GET endpoint."
echo "- Restore the original enabled state and retention period."
echo "- Base URL: ${BASE_URL}"
echo "- Token helper enabled: ${USE_TOKEN}"
echo "- Token kind: admin"
echo

request "GET" "${JOB_URL}" "initial_get" '^200$'
INITIAL_ENABLED="$(extract_enabled "initial_get")"
request "GET" "${RETENTION_POLICY_URL}" "initial_retention_get" '^200$'
INITIAL_RETENTION_PERIOD_DAYS="$(extract_retention_period_days "initial_retention_get")"

if [[ "${INITIAL_ENABLED}" == "true" ]]; then
  TOGGLE_ENABLED="false"
  TOGGLE_ACTION="disable"
  RESTORE_ACTION="enable"
else
  TOGGLE_ENABLED="true"
  TOGGLE_ACTION="enable"
  RESTORE_ACTION="disable"
fi

request \
  "PUT" \
  "${JOB_URL}?enable=${TOGGLE_ENABLED}" \
  "${TOGGLE_ACTION}_job" \
  '^(200|201)$'
request "GET" "${JOB_URL}" "verify_toggled" '^200$'
assert_enabled "verify_toggled" "${TOGGLE_ENABLED}"

TOGGLE_RETENTION_PERIOD_DAYS="365"

request \
  "PUT" \
  "${RETENTION_POLICY_URL}?retentionPeriodDays=${TOGGLE_RETENTION_PERIOD_DAYS}" \
  "update_retention_period" \
  '^200$'
request "GET" "${RETENTION_POLICY_URL}" "verify_retention_updated" '^200$'
assert_retention_period_days "verify_retention_updated" "${TOGGLE_RETENTION_PERIOD_DAYS}"

request \
  "PUT" \
  "${JOB_URL}?enable=${INITIAL_ENABLED}" \
  "restore_${RESTORE_ACTION}_job" \
  '^(200|201)$'
request \
  "PUT" \
  "${RETENTION_POLICY_URL}?retentionPeriodDays=${INITIAL_RETENTION_PERIOD_DAYS}" \
  "restore_retention_period" \
  '^200$'
request "GET" "${JOB_URL}" "verify_restored" '^200$'
assert_enabled "verify_restored" "${INITIAL_ENABLED}"
request "GET" "${RETENTION_POLICY_URL}" "verify_retention_restored" '^200$'
assert_retention_period_days "verify_retention_restored" "${INITIAL_RETENTION_PERIOD_DAYS}"

echo
echo "ARCPOC-925 verification completed successfully."
echo "Initial state: ${INITIAL_ENABLED}"
echo "Initial retention period days: ${INITIAL_RETENTION_PERIOD_DAYS}"
echo "Toggled state: ${TOGGLE_ENABLED}"
echo "Toggled retention period days: ${TOGGLE_RETENTION_PERIOD_DAYS}"
echo "Final state restored to: ${INITIAL_ENABLED}"
