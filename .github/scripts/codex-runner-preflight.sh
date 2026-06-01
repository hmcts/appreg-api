#!/usr/bin/env bash

set -euo pipefail

require_command() {
  local command_name="$1"

  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Missing required command: $command_name" >&2
    exit 1
  fi
}

for command_name in git gh java node npm python3 codex; do
  require_command "$command_name"
done

echo "Verifying installed tooling..."
git --version
gh --version
java -version
node --version
npm --version
python3 --version
codex --version
./gradlew --version

if command -v docker >/dev/null 2>&1; then
  docker --version
else
  echo "::warning::docker is not installed or not on PATH. This is acceptable for fast smoke/unit-test runs, but full Testcontainers-based verification will need Docker support."
fi

if [[ -n "${OPENAI_API_KEY:-}" ]]; then
  echo "Authenticating Codex CLI with repository secret."
  printf '%s' "$OPENAI_API_KEY" | codex login --with-api-key >/dev/null
else
  echo "::notice::OPENAI_API_KEY not set; using runner-provisioned Codex auth."
fi

codex login status
