#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${APPREG_TEST_CONFIG_FILE:-${SCRIPT_DIR}/appreg_test.config.ini}"
TOKEN_HELPER="${SCRIPT_DIR}/appreg_token_fetcher.sh"
ENV_BASE_URL="${APPREG_BASE_URL:-}"
BASE_URL=""
JOB_TYPE="APPLICATION_LISTS_DATABASE_JOB"
RETENTION_POLICY_KEY="RETENTION_PERIOD_DAYS"
WRAPPED_FUNCTION="delete_expired_application_lists_count"
TARGET_RETENTION_PERIOD_DAYS_LIST="${APPREG_TARGET_RETENTION_PERIOD_DAYS_LIST:-10 20 40 100 200}"
GET_ACCEPT_HEADER="application/vnd.hmcts.appreg.v1+json"
PUT_ACCEPT_HEADER="*/*"
AUTH_HEADER=""
USE_TOKEN="${APPREG_USE_AZURE_TOKEN:-true}"
ADMIN_TOKEN_FILE="${SCRIPT_DIR}/.appref_admin_token"
USED_TOKEN_HELPER="false"
TOKEN_REFRESH_ATTEMPTED="false"
RETENTION_RESTORE_REQUIRED="false"
DB_HOST=""
DB_PORT=""
DB_NAME=""
DB_USER=""
DB_PASS=""
DB_SCHEMA=""
PSQL_BIN=""

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

DB_HOST="${APPREG_POSTGRES_HOST:-${POSTGRES_HOST:-}}"
DB_PORT="${APPREG_POSTGRES_PORT:-${POSTGRES_PORT:-5432}}"
DB_NAME="${APPREG_POSTGRES_DATABASE:-${POSTGRES_DATABASE:-}}"
DB_USER="${APPREG_POSTGRES_USER:-${POSTGRES_USER:-}}"
DB_PASS="${APPREG_POSTGRES_PASSWORD:-${POSTGRES_PASS:-}}"
DB_SCHEMA="${APPREG_POSTGRES_SCHEMA:-${POSTGRES_SCHEMA:-appreg}}"
PSQL_BIN="${APPREG_PSQL_BIN:-${PSQL_BIN:-}}"
PSQL_BIN="${PSQL_BIN//$'\r'/}"
PSQL_BIN="${PSQL_BIN#"${PSQL_BIN%%[![:space:]]*}"}"
PSQL_BIN="${PSQL_BIN%"${PSQL_BIN##*[![:space:]]}"}"

if [[ -z "${DB_HOST}" || -z "${DB_NAME}" || -z "${DB_USER}" || -z "${DB_PASS}" ]]; then
  echo "Database settings are incomplete; set POSTGRES_HOST, POSTGRES_DATABASE, POSTGRES_USER, and POSTGRES_PASS in ${CONFIG_FILE}" >&2
  exit 1
fi

resolve_psql_bin() {
  local homebrew_psql

  if [[ -n "${PSQL_BIN}" && -x "${PSQL_BIN}" ]]; then
    return 0
  fi

  if command -v psql >/dev/null 2>&1; then
    PSQL_BIN="$(command -v psql)"
    return 0
  fi

  if [[ -x /opt/homebrew/bin/psql ]]; then
    PSQL_BIN="/opt/homebrew/bin/psql"
    return 0
  fi

  for homebrew_psql in /opt/homebrew/opt/*/bin/psql; do
    if [[ -x "${homebrew_psql}" ]]; then
      PSQL_BIN="${homebrew_psql}"
      return 0
    fi
  done

  if [[ -n "${PSQL_BIN}" ]]; then
    echo "psql was configured as '${PSQL_BIN}' but is not executable, and no fallback was found under /opt/homebrew/opt" >&2
    return 1
  fi

  echo "psql is required but was not found on PATH or under /opt/homebrew/opt" >&2
  return 1
}

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

resolve_psql_bin

TMP_DIR="$(mktemp -d)"

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

request_quiet() {
  local method="$1"
  local url="$2"
  local name="$3"
  local expected_regex="$4"
  local payload="${5:-}"
  local body_file="${TMP_DIR}/${name}.json"
  local status

  perform_request_quiet() {
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

  status="$(perform_request_quiet)"

  if [[ "${status}" == "401" && "${USED_TOKEN_HELPER}" == "true" && "${TOKEN_REFRESH_ATTEMPTED}" == "false" ]]; then
    "${TOKEN_HELPER}" >/dev/null
    AUTH_HEADER="Authorization: Bearer $(tr -d '\r\n' < "${ADMIN_TOKEN_FILE}")"
    TOKEN_REFRESH_ATTEMPTED="true"
    status="$(perform_request_quiet)"
  fi

  if ! [[ "${status}" =~ ${expected_regex} ]]; then
    echo "=== ${name} ==="
    echo "${method} ${url}"
    echo "HTTP ${status}"

    if [[ -s "${body_file}" ]]; then
      cat "${body_file}"
      echo
    else
      echo "<empty body>"
    fi

    echo "Unexpected HTTP status for ${name}: ${status}" >&2
    return 1
  fi
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

assert_retention_period_days() {
  local name="$1"
  local expected="$2"
  local body_file="${TMP_DIR}/${name}.json"

  if ! rg -q "\"retentionPeriodDays\"[[:space:]]*:[[:space:]]*${expected}" "${body_file}"; then
    echo "Expected ${name} response to contain retentionPeriodDays=${expected}" >&2
    return 1
  fi
}

psql_query() {
  local sql="$1"

  PGPASSWORD="${DB_PASS}" \
    "${PSQL_BIN}" \
    -X \
    -v ON_ERROR_STOP=1 \
    -h "${DB_HOST}" \
    -p "${DB_PORT}" \
    -U "${DB_USER}" \
    -d "${DB_NAME}" \
    -At \
    -F '|' \
    -c "${sql}"
}

psql_value() {
  local sql="$1"

  psql_query "${sql}" | head -n 1
}

run_query() {
  local name="$1"
  local sql="$2"
  local output_file="${TMP_DIR}/${name}.txt"

  psql_query "${sql}" > "${output_file}"

  echo "=== ${name} ==="
  echo "psql ${DB_HOST}:${DB_PORT}/${DB_NAME} (${DB_SCHEMA})"

  if [[ -s "${output_file}" ]]; then
    cat "${output_file}"
    echo
  else
    echo "<empty body>"
  fi
}

extract_field() {
  local name="$1"
  local field_number="$2"
  local output_file="${TMP_DIR}/${name}.txt"

  awk -F'|' -v field_number="${field_number}" 'NR == 1 { print $field_number }' "${output_file}"
}

assert_equals() {
  local description="$1"
  local expected="$2"
  local actual="$3"

  if [[ "${expected}" != "${actual}" ]]; then
    echo "Expected ${description} to be ${expected} but was ${actual}" >&2
    return 1
  fi
}

restore_retention_period() {
  if [[ "${RETENTION_RESTORE_REQUIRED}" != "true" || -z "${INITIAL_RETENTION_PERIOD_DAYS:-}" ]]; then
    return 0
  fi

  if request_quiet \
    "PUT" \
    "${RETENTION_POLICY_URL}?retentionPeriodDays=${INITIAL_RETENTION_PERIOD_DAYS}" \
    "restore_retention_period" \
    '^200$'; then
    request_quiet "GET" "${RETENTION_POLICY_URL}" "verify_retention_restored" '^200$'
    assert_retention_period_days "verify_retention_restored" "${INITIAL_RETENTION_PERIOD_DAYS}"
    RETENTION_RESTORE_REQUIRED="false"
    return 0
  fi

  echo "Failed to restore retention period to ${INITIAL_RETENTION_PERIOD_DAYS}" >&2
  return 1
}

cleanup() {
  local exit_code=$?

  set +e
  restore_retention_period
  rm -rf "${TMP_DIR}"

  exit "${exit_code}"
}

trap cleanup EXIT

INITIAL_STATE_SQL="
SELECT
  CASE
    WHEN dj.job_enabled IN ('Y', 'y', 'TRUE', 'true') THEN 'true'
    WHEN dj.job_enabled IN ('N', 'n', 'FALSE', 'false') THEN 'false'
    ELSE 'unknown'
  END AS enabled,
  rp.config_value AS retention_period_days
FROM ${DB_SCHEMA}.database_jobs dj
JOIN ${DB_SCHEMA}.retention_policy rp
  ON rp.dj_dj_id = dj.dj_id
WHERE dj.job_name = '${JOB_TYPE}'
  AND rp.config_key = '${RETENTION_POLICY_KEY}'
LIMIT 1;
"

EXPECTED_ROWS_SQL="
SELECT count(*) AS eligible_row_count
FROM ${DB_SCHEMA}.application_lists al
WHERE al.application_list_status = 'CLOSED'
  AND al.application_list_date < now() - make_interval(
    days => (
      SELECT rp.config_value::integer
      FROM ${DB_SCHEMA}.retention_policy rp
      JOIN ${DB_SCHEMA}.database_jobs dj
        ON rp.dj_dj_id = dj.dj_id
      WHERE dj.job_name = '${JOB_TYPE}'
        AND rp.config_key = '${RETENTION_POLICY_KEY}'
      LIMIT 1
    )
  )
  AND NOT al.child_deleted;
"

WRAPPED_JOB_SQL="
SELECT ${DB_SCHEMA}.${WRAPPED_FUNCTION}();
"

JOB_URL="${BASE_URL}/admin/jobs/${JOB_TYPE}"
RETENTION_POLICY_URL="${JOB_URL}/retention-policy"
RESULTS_TABLE=""

echo "ARCPOC-926 intent:"
echo "- Read the current enabled state for ${JOB_TYPE}."
echo "- Read the current retention period for ${JOB_TYPE}."
echo "- Loop through retention periods: ${TARGET_RETENTION_PERIOD_DAYS_LIST}."
echo "- Compare the independent eligible-row count to ${WRAPPED_FUNCTION}() for each value."
echo "- Restore the original retention period."
echo

run_query "initial_state" "${INITIAL_STATE_SQL}"
INITIAL_ENABLED="$(extract_field "initial_state" 1)"
INITIAL_RETENTION_PERIOD_DAYS="$(extract_field "initial_state" 2)"

if [[ "${INITIAL_ENABLED}" != "true" ]]; then
  echo "Job ${JOB_TYPE} is not enabled; enable it before using this probe so a zero result is meaningful." >&2
  exit 1
fi

request_quiet "GET" "${RETENTION_POLICY_URL}" "initial_retention_get" '^200$'
assert_retention_period_days "initial_retention_get" "${INITIAL_RETENTION_PERIOD_DAYS}"

RETENTION_RESTORE_REQUIRED="true"

for TARGET_RETENTION_PERIOD_DAYS in ${TARGET_RETENTION_PERIOD_DAYS_LIST}; do
  request_quiet \
    "PUT" \
    "${RETENTION_POLICY_URL}?retentionPeriodDays=${TARGET_RETENTION_PERIOD_DAYS}" \
    "update_retention_period_${TARGET_RETENTION_PERIOD_DAYS}" \
    '^200$'
  request_quiet "GET" "${RETENTION_POLICY_URL}" "verify_retention_updated_${TARGET_RETENTION_PERIOD_DAYS}" '^200$'
  assert_retention_period_days "verify_retention_updated_${TARGET_RETENTION_PERIOD_DAYS}" "${TARGET_RETENTION_PERIOD_DAYS}"

  EXPECTED_ELIGIBLE_ROW_COUNT="$(psql_value "${EXPECTED_ROWS_SQL}")"
  WRAPPED_JOB_ROW_COUNT="$(psql_value "${WRAPPED_JOB_SQL}")"

  assert_equals "actual job row count for ${TARGET_RETENTION_PERIOD_DAYS} days" "${EXPECTED_ELIGIBLE_ROW_COUNT}" "${WRAPPED_JOB_ROW_COUNT}"
  RESULTS_TABLE+="$(printf '%-6s | %-8s | %-8s | %s\n' "${TARGET_RETENTION_PERIOD_DAYS}" "${EXPECTED_ELIGIBLE_ROW_COUNT}" "${WRAPPED_JOB_ROW_COUNT}" "PASS")"$'\n'
done

echo "ARCPOC-926 non-destructive deletion probe completed successfully."
echo "Initial state: ${INITIAL_ENABLED}"
echo "Initial retention period days: ${INITIAL_RETENTION_PERIOD_DAYS}"
printf '%-6s | %-8s | %-8s | %s\n' "Days" "Expected" "Actual" "Result"
printf '%-6s-+-%-8s-+-%-8s-+-%s\n' "------" "--------" "--------" "------"
printf '%s' "${RESULTS_TABLE}"
