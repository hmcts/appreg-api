#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

for name in CODEX_RESULT CODEX_OPERATION OUTPUT_DIR BRANCH_NAME PLAN_DIR; do
  required_env "${name}"
done

output_dir="${OUTPUT_DIR}"
pr_body_path="${output_dir}/codex-pr-body.md"
metadata_path="${output_dir}/metadata.env"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# shellcheck source=.github/scripts/codex-action-runtime.sh
source "${script_dir}/codex-action-runtime.sh"

mkdir -p "${output_dir}"
validated_codex_plan_path "${PLAN_DIR}" >/dev/null
allowed_paths_file="${PLAN_DIR}/allowed-paths.txt"
ALLOWED_PATHS_FILE="${allowed_paths_file}" REQUIRE_CHANGES=true \
  python3 "${script_dir}/collect-codex-patch-result.py"
unlink "${output_dir}/codex-final-message.md"

case "${CODEX_OPERATION}" in
  jira-generate)
    for name in ISSUE_KEY ISSUE_SUMMARY ISSUE_URL; do
      required_env "${name}"
    done
    PR_BODY_PATH="${pr_body_path}" PLAN_SHA_PATH="${PLAN_DIR}/plan.sha256" python3 -I - <<'PY'
import os
from pathlib import Path

plan_sha = Path(os.environ["PLAN_SHA_PATH"]).read_text(encoding="ascii").strip()
body = f"""### Jira link

See [{os.environ['ISSUE_KEY']}]({os.environ['ISSUE_URL']})

### Change description

Implements Jira issue {os.environ['ISSUE_KEY']}: {os.environ['ISSUE_SUMMARY']}

Codex ran on the Azure AKS self-hosted runner scale set using the Jira issue context.

### Testing done

Codex may run lightweight targeted checks during generation. This workflow verifies the generated patch in a separate no-write job before the trusted publish job opens the pull request. See the workflow checks for the trusted verification result.

### Planning audit

- Validated plan SHA-256: `{plan_sha}`
- Human approval: required

### Security Vulnerability Assessment ###

**CVE Suppression:** Are there any CVEs present in the codebase (either newly introduced or pre-existing) that are being intentionally suppressed or ignored by this commit?
  * [ ] Yes
  * [x] No

### Checklist

- [x] commit messages are meaningful and follow good commit message guidelines
- [ ] README and other documentation has been updated / added (if needed)
- [ ] tests have been updated / new tests has been added (if needed)
- [ ] Does this PR introduce a breaking change
"""
Path(os.environ["PR_BODY_PATH"]).write_text(body, encoding="utf-8")
PY
    ;;
  jira-repair)
    required_env "INPUT_DIR"
    required_env "REPAIR_ATTEMPT"
    input_pr_body_path="${INPUT_DIR}/codex-pr-body.md"
    if [[ -s "${input_pr_body_path}" ]]; then
      cp "${input_pr_body_path}" "${pr_body_path}"
    else
      required_env "ISSUE_KEY"
      required_env "ISSUE_URL"
      {
        echo "### Jira link"
        echo
        echo "See [${ISSUE_KEY}](${ISSUE_URL})"
      } >"${pr_body_path}"
    fi
    ;;
  *)
    echo "Unsupported Codex Jira operation: ${CODEX_OPERATION}" >&2
    exit 1
    ;;
esac

{
  echo "branch_name=${BRANCH_NAME}"
  echo "has_changes=true"
  if [[ "${CODEX_OPERATION}" == "jira-repair" ]]; then
    echo "repair_attempt=${REPAIR_ATTEMPT}"
  fi
} >"${metadata_path}"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "branch_name=${BRANCH_NAME}"
    echo "has_changes=true"
  } >>"${GITHUB_OUTPUT}"
fi
