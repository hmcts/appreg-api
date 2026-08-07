#!/usr/bin/env bash

set -euo pipefail

if [[ -z "${GH_TOKEN:-}" ]]; then
  echo "Missing required environment variable: GH_TOKEN" >&2
  exit 1
fi

git_authenticated() {
  GIT_CONFIG_GLOBAL=/dev/null \
    GIT_CONFIG_NOSYSTEM=1 \
    GIT_TERMINAL_PROMPT=0 \
    GH_TOKEN="${GH_TOKEN}" \
    git \
    -c core.hooksPath=/dev/null \
    -c credential.helper= \
    -c credential.helper='!f() { test "$1" = get && echo username=x-access-token && echo "password=$GH_TOKEN"; }; f' \
    -c protocol.file.allow=never \
    "$@"
}

default_branch="${DEFAULT_BRANCH:-master}"
branch_prefix="${BRANCH_PREFIX:-codex-runner-smoke}"
timestamp="$(date -u +"%Y%m%d%H%M%S")"
run_id="${GITHUB_RUN_ID:-manual}"
branch_name="${branch_prefix}-${timestamp}-${run_id}"
branch_name="${branch_name//[^A-Za-z0-9._-]/-}"

git_authenticated fetch --no-tags --prune origin \
  "+refs/heads/${default_branch}:refs/remotes/origin/${default_branch}"
git checkout -B "$default_branch" "origin/$default_branch"
git checkout -b "$branch_name"

mkdir -p .github/runner-smoke

smoke_file=".github/runner-smoke/${branch_name}.txt"
workflow_url=""
if [[ -n "${GITHUB_SERVER_URL:-}" && -n "${GITHUB_REPOSITORY:-}" && -n "${GITHUB_RUN_ID:-}" ]]; then
  workflow_url="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
fi

cat >"$smoke_file" <<EOF
codex runner smoke test
runner_name=${RUNNER_NAME:-unknown}
runner_os=${RUNNER_OS:-unknown}
runner_arch=${RUNNER_ARCH:-unknown}
repository=${GITHUB_REPOSITORY:-unknown}
workflow_url=${workflow_url}
timestamp_utc=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
EOF

git add "$smoke_file"
git commit -m "chore: codex runner smoke test ${branch_name}"
git_authenticated push --set-upstream origin "$branch_name"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "branch_name=$branch_name"
    echo "commit_sha=$(git rev-parse HEAD)"
    echo "smoke_file=$smoke_file"
  } >>"$GITHUB_OUTPUT"
fi
