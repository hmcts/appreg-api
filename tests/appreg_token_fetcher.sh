#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${APPREG_TEST_CONFIG_FILE:-${SCRIPT_DIR}/appreg_test.config.ini}"
USER_TOKEN_FILE="${SCRIPT_DIR}/.appreg_user_token"
ADMIN_TOKEN_FILE="${SCRIPT_DIR}/.appref_admin_token"
TOKEN_URL=""

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "Config file not found: ${CONFIG_FILE}" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required but not installed." >&2
  echo "Install it with: brew install jq" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "${CONFIG_FILE}"

TOKEN_URL="https://login.microsoftonline.com/${TENANT_ID}/oauth2/v2.0/token"

extract_access_token() {
  local response
  response="$(cat)"
  printf '%s' "${response}" | jq -er '.access_token'
}

USER_TOKEN="$(
  curl -sS \
    -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'grant_type=password' \
    --data-urlencode "client_id=${CLIENT_ID_FE}" \
    --data-urlencode "client_secret=${CLIENT_SECRET_FE}" \
    --data-urlencode "scope=${SCOPE}" \
    --data-urlencode "username=${USER_NAME}" \
    --data-urlencode "password=${PASSWORD}" \
    "${TOKEN_URL}" |
    extract_access_token
)"

ADMIN_TOKEN="$(
  curl -sS \
    -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'grant_type=password' \
    --data-urlencode "client_id=${CLIENT_ID_FE}" \
    --data-urlencode "client_secret=${CLIENT_SECRET_FE}" \
    --data-urlencode "scope=${SCOPE}" \
    --data-urlencode "username=${ADMIN_USER_NAME}" \
    --data-urlencode "password=${PASSWORD}" \
    "${TOKEN_URL}" |
    extract_access_token
)"

printf '%s\n' "${USER_TOKEN}" > "${USER_TOKEN_FILE}"
printf '%s\n' "${ADMIN_TOKEN}" > "${ADMIN_TOKEN_FILE}"
chmod 600 "${USER_TOKEN_FILE}" "${ADMIN_TOKEN_FILE}"

echo "Fetched user token:  ${USER_TOKEN_FILE}"
echo "Fetched admin token: ${ADMIN_TOKEN_FILE}"
