#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: bin/codex-local-pipeline.sh [checks-only|fast|codex|full] [options]

Runs a local approximation of the checks that matter before a Codex PR is opened.

Modes:
  checks-only  Validate workflow/script syntax and repository PR guardrails only.
  fast         Run checks-only plus unit tests only. Default.
  codex        Run the Codex runner preflight plus fast mode.
  full         Run fast mode plus full Gradle checks, integration, functional,
               smoke, coverage, and dependency checks.

Options:
  --base <branch>              Base branch for PR-style diff checks. Default: master.
  --no-fetch                   Do not fetch origin/<base> before diff checks.
  --include-dependency-check   Run OWASP dependencyCheck, even outside full mode.
  -h, --help                   Show this help.

Environment:
  BASE_BRANCH                  Alternative way to set --base.
  OPENAI_API_KEY               Used by codex mode if present; otherwise existing
                              Codex CLI login is used.
  GRADLE_FAST_TASKS            Space-separated Gradle tasks for fast mode.
                              Default: clean test.
  REQUIRE_DOCKER               Set true to require Docker outside full mode.
EOF
}

log() {
  printf '\n==> %s\n' "$*"
}

warn() {
  printf 'Warning: %s\n' "$*" >&2
}

require_command() {
  local command_name="$1"

  if ! command -v "${command_name}" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "${command_name}" >&2
    exit 1
  fi
}

trim_count() {
  tr -d '[:space:]'
}

repo_root="$(git rev-parse --show-toplevel)"
cd "${repo_root}"

mode="fast"
if [[ $# -gt 0 && "$1" != -* ]]; then
  mode="$1"
  shift
fi

base_branch="${BASE_BRANCH:-master}"
fetch_base="true"
include_dependency_check="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      if [[ $# -lt 2 ]]; then
        echo "--base requires a branch name" >&2
        exit 1
      fi
      base_branch="$2"
      shift 2
      ;;
    --no-fetch)
      fetch_base="false"
      shift
      ;;
    --include-dependency-check)
      include_dependency_check="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

case "${mode}" in
  checks-only|fast|codex|full)
    ;;
  *)
    echo "Unknown mode: ${mode}" >&2
    usage >&2
    exit 1
    ;;
esac

if [[ "${mode}" == "full" ]]; then
  include_dependency_check="true"
fi

log "Checking required local tools"
for command_name in git bash find awk sort uniq wc; do
  require_command "${command_name}"
done

if [[ "${mode}" != "checks-only" ]]; then
  require_command java
fi

if [[ "${mode}" == "codex" ]]; then
  log "Running Codex runner preflight"
  ./.github/scripts/codex-runner-preflight.sh
fi

log "Validating shell scripts"
bash -n \
  .github/scripts/*.sh \
  bin/*.sh

log "Validating workflow YAML syntax"
if command -v ruby >/dev/null 2>&1; then
  ruby -e 'require "yaml"; Dir[".github/workflows/*.yml", ".github/workflows/*.yaml"].each { |f| YAML.load_file(f) }; puts "workflow yaml ok"'
else
  warn "ruby is not installed; skipping workflow YAML parse"
fi

log "Checking Flyway migration numbers are unique"
flyway_script_count="$(find ./flyway -type f | wc -l | trim_count)"
unique_flyway_prefix_count="$(find ./flyway -type f | awk -F '__' '{print $1}' | sort -u | wc -l | trim_count)"

echo "Flyway script count: ${flyway_script_count}"
echo "Unique Flyway migration prefix count: ${unique_flyway_prefix_count}"

if [[ "${flyway_script_count}" != "${unique_flyway_prefix_count}" ]]; then
  echo "Duplicate Flyway migration prefixes found:" >&2
  find ./flyway -type f | awk -F '__' '{print $1}' | sort | uniq -d >&2
  exit 1
fi

base_ref="origin/${base_branch}"
if [[ "${fetch_base}" == "true" ]]; then
  log "Fetching ${base_ref}"
  git fetch origin "${base_branch}" >/dev/null
fi

if git rev-parse --verify --quiet "${base_ref}" >/dev/null; then
  merge_base="$(git merge-base "${base_ref}" HEAD)"

  log "Checking existing Flyway files were not modified or deleted"
  changed_files="$(git diff --name-status "${merge_base}" -- || true)"
  if [[ -n "${changed_files}" ]]; then
    echo "${changed_files}"
  else
    echo "No changes detected against ${base_ref}."
  fi

  if echo "${changed_files}" | grep -E '^(M|D)[[:space:]]+flyway/' >/dev/null 2>&1; then
    echo "Existing files under flyway/ were modified or deleted. New Flyway files are allowed; changing existing ones is blocked." >&2
    exit 1
  fi
else
  warn "Could not find ${base_ref}; skipping PR-style diff guardrails"
fi

if [[ "${mode}" == "checks-only" ]]; then
  log "Local pipeline checks completed"
  exit 0
fi

if [[ "${mode}" == "full" || "${REQUIRE_DOCKER:-false}" == "true" ]]; then
  log "Checking Docker daemon"
  require_command docker
  docker info >/dev/null
elif ! command -v docker >/dev/null 2>&1; then
  warn "docker is not installed; skipping Docker check for ${mode} mode"
fi

if [[ "${mode}" == "full" ]]; then
  gradle_args=(
    --no-daemon
    clean
    check
    build
    functional
    smoke
    jacocoUnitCoverageVerification
    jacocoIntegrationCoverageVerification
  )
else
  read -r -a gradle_fast_tasks <<<"${GRADLE_FAST_TASKS:-clean test}"
  gradle_args=(--no-daemon "${gradle_fast_tasks[@]}")
fi

log "Running Gradle verification: ./gradlew ${gradle_args[*]}"
./gradlew "${gradle_args[@]}"

if [[ "${include_dependency_check}" == "true" ]]; then
  log "Running OWASP dependency check"
  ./gradlew --no-daemon dependencyCheck
fi

log "Local pipeline completed"
