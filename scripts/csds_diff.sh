#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${CSDS_CONFIG_FILE:-${SCRIPT_DIR}/csds.config}"

# Help and the offline check do not require database credentials.
case "${1:-}" in
  --help|--self-check)
    exec python3 "${SCRIPT_DIR}/lib/csds_diff.py" "$@"
    ;;
esac

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "Copy csds.config.example to ${CONFIG_FILE} and populate the database settings." >&2
  exit 1
fi
# shellcheck disable=SC1090
source "${CONFIG_FILE}"
for variable in PGHOST PGPORT PGDATABASE PGUSER CSDS_DB_SCHEMA; do
  if [[ -z "${!variable:-}" ]]; then
    echo "${variable} must be set in ${CONFIG_FILE}" >&2
    exit 1
  fi
done
export PGHOST PGPORT PGDATABASE PGUSER CSDS_DB_SCHEMA
# An empty password allows .pgpass or local trust authentication.
if [[ -n "${PGPASSWORD:-}" ]]; then export PGPASSWORD; fi
if [[ -n "${PGSSLMODE:-}" ]]; then export PGSSLMODE; fi
if [[ -n "${PGSSLROOTCERT:-}" ]]; then export PGSSLROOTCERT; fi
export PGCONNECT_TIMEOUT="${PGCONNECT_TIMEOUT:-10}"
# Homebrew PostgreSQL/libpq installations may be keg-only (not on PATH).
if ! command -v psql >/dev/null 2>&1; then
  for directory in /opt/homebrew/opt/libpq/bin /opt/homebrew/opt/postgresql@16/bin; do
    if [[ -x "${directory}/psql" ]]; then
      export PATH="${directory}:${PATH}"
      break
    fi
  done
fi
exec python3 "${SCRIPT_DIR}/lib/csds_diff.py" "$@"
