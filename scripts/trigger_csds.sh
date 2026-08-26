#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${TRIGGER_CSDS_CONFIG_FILE:-${SCRIPT_DIR}/trigger_csds.config}"

usage() {
  cat <<'EOF'
Usage: trigger_csds.sh [--check-config|--help]

Fetch a fresh AppReg admin token and trigger all enabled CSDS ingress processors.

Configuration is sourced from trigger_csds.config in the same directory. Copy
trigger_csds.config.example to that path and provide all required values locally.

Options:
  --check-config  Call an existing read-only admin endpoint instead of triggering CSDS.
  --help          Show this help text.
EOF
}

mode="trigger"
case "${1:-}" in
  --check-config)
    mode="check"
    ;;
  --help)
    usage
    exit 0
    ;;
  "")
    ;;
  *)
    echo "Unknown argument: $1" >&2
    usage >&2
    exit 2
    ;;
esac

if [[ $# -gt 1 ]]; then
  echo "Too many arguments." >&2
  usage >&2
  exit 2
fi

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "Config file not found: ${CONFIG_FILE}" >&2
  echo "Copy ${CONFIG_FILE}.example to ${CONFIG_FILE} and provide its values." >&2
  exit 1
fi

for command in curl jq; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "${command} is required but is not installed." >&2
    exit 1
  fi
done

# shellcheck disable=SC1090
source "${CONFIG_FILE}"

required_variables=(
  APPREG_BASE_URL
  AZURE_TENANT_ID
  AZURE_CLIENT_ID
  AZURE_CLIENT_SECRET
  AZURE_SCOPE
  APPREG_ADMIN_USERNAME
  APPREG_ADMIN_PASSWORD
)

for variable in "${required_variables[@]}"; do
  if [[ -z "${!variable:-}" ]]; then
    echo "${variable} must be set in ${CONFIG_FILE}" >&2
    exit 1
  fi
done

token_url="https://login.microsoftonline.com/${AZURE_TENANT_ID}/oauth2/v2.0/token"
echo "Attempting to fetch administrative token"
if ! admin_token="$(
  curl --fail-with-body -sS \
    -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'grant_type=password' \
    --data-urlencode "client_id=${AZURE_CLIENT_ID}" \
    --data-urlencode "client_secret=${AZURE_CLIENT_SECRET}" \
    --data-urlencode "scope=${AZURE_SCOPE}" \
    --data-urlencode "username=${APPREG_ADMIN_USERNAME}" \
    --data-urlencode "password=${APPREG_ADMIN_PASSWORD}" \
    "${token_url}" |
    jq -er '.access_token'
)"; then
  echo "Failed to fetch the AppReg admin token." >&2
  exit 1
fi
echo "Fetched AppReg admin token"

if [[ "${mode}" == "check" ]]; then
  operation_name="Test call"
  request_method="GET"
  request_url="${APPREG_BASE_URL%/}/admin/jobs/APPLICATION_LISTS_DATABASE_JOB"
else
  operation_name="CSDS ingress"
  request_method="POST"
  request_url="${APPREG_BASE_URL%/}/admin/csds/trigger"
fi
echo "Calling ${request_url}"

if ! response="$(
  curl -sS \
    -X "${request_method}" \
    -H 'Accept: application/vnd.hmcts.appreg.v1+json' \
    -H "Authorization: Bearer ${admin_token}" \
    -o - \
    -w $'\n%{http_code}' \
    "${request_url}"
)"; then
  echo "${operation_name} request failed for ${request_url}." >&2
  exit 1
fi

status="${response##*$'\n'}"
body="${response%$'\n'*}"

if [[ -n "${body}" ]]; then
  printf '%s\n' "${body}"
fi

case "${status}" in
  2??)
    echo "${operation_name} completed successfully (HTTP ${status})."
    ;;
  401 | 403)
    echo "${operation_name} was not authorised (HTTP ${status})." >&2
    exit 1
    ;;
  423)
    echo "${operation_name} could not run because CSDS ingress is already running or the job is disabled (HTTP 423)." >&2
    exit 1
    ;;
  *)
    echo "${operation_name} failed (HTTP ${status})." >&2
    exit 1
    ;;
esac
