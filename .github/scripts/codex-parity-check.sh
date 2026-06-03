#!/usr/bin/env bash

set -euo pipefail

required_env() {
  local name="$1"

  if [[ -z "${!name:-}" ]]; then
    echo "Missing required environment variable: ${name}" >&2
    exit 1
  fi
}

required_env "ISSUE_KEY"
required_env "ISSUE_SUMMARY"
required_env "ISSUE_DESCRIPTION"
required_env "ISSUE_URL"
required_env "OUTPUT_DIR"
required_env "LEGACY_SNAPSHOT_DIR"

run_id="${GITHUB_RUN_ID:-manual}"
run_attempt="${GITHUB_RUN_ATTEMPT:-1}"
artifact_dir="${RUNNER_TEMP:-/tmp}/codex-parity-${run_id}-${run_attempt}"
runner_home="${HOME:-/home/runner}"
codex_home="${artifact_dir}/codex-home"
codex_tmp="${artifact_dir}/codex-tmp"
codex_runner_temp="${artifact_dir}/codex-runner-temp"
output_dir="${OUTPUT_DIR}"
legacy_snapshot_dir="${LEGACY_SNAPSHOT_DIR}"
prompt_path="${artifact_dir}/codex-parity-prompt.md"
schema_path="${artifact_dir}/codex-parity-schema.json"
final_message_path="${output_dir}/codex-parity-final.json"
report_path="${output_dir}/parity-report.json"
comment_path="${output_dir}/parity-comment.md"
usage_events_path="${artifact_dir}/codex-events.jsonl"
usage_summary_path="${output_dir}/codex-usage-summary.json"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# shellcheck source=.github/scripts/codex-usage-metrics.sh
source "${script_dir}/codex-usage-metrics.sh"

prepare_codex_home() {
  mkdir -p "${codex_home}/.codex" "${codex_home}/.cache" "${codex_home}/.config" "${codex_tmp}" "${codex_runner_temp}"

  if [[ -f "${runner_home}/.codex/auth.json" ]]; then
    cp "${runner_home}/.codex/auth.json" "${codex_home}/.codex/auth.json"
    chmod 600 "${codex_home}/.codex/auth.json"
  fi

  if [[ -f "${runner_home}/.codex/config.toml" ]]; then
    cp "${runner_home}/.codex/config.toml" "${codex_home}/.codex/config.toml"
    chmod 600 "${codex_home}/.codex/config.toml"
  fi
}

run_codex() {
  env -i \
    "HOME=${codex_home}" \
    "CODEX_HOME=${codex_home}/.codex" \
    "XDG_CACHE_HOME=${codex_home}/.cache" \
    "XDG_CONFIG_HOME=${codex_home}/.config" \
    "PATH=${PATH:-/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin}" \
    "SHELL=${SHELL:-/bin/bash}" \
    "USER=${USER:-runner}" \
    "LOGNAME=${LOGNAME:-${USER:-runner}}" \
    "LANG=${LANG:-C.UTF-8}" \
    "LC_ALL=${LC_ALL:-${LANG:-C.UTF-8}}" \
    "TERM=${TERM:-xterm}" \
    "TMPDIR=${codex_tmp}" \
    "RUNNER_TEMP=${codex_runner_temp}" \
    "$@"
}

snapshot_manifest_path="${legacy_snapshot_dir}/manifest.json"
if [[ ! -s "${snapshot_manifest_path}" ]]; then
  echo "Legacy snapshot manifest is missing: ${snapshot_manifest_path}" >&2
  exit 1
fi

mkdir -p "${artifact_dir}" "${output_dir}"
prepare_codex_home

snapshot_id="$(
  SNAPSHOT_MANIFEST_PATH="${snapshot_manifest_path}" python3 - <<'PY'
import json
import os
from pathlib import Path

manifest = json.loads(Path(os.environ["SNAPSHOT_MANIFEST_PATH"]).read_text(encoding="utf-8"))
print(
    manifest.get("snapshotId")
    or manifest.get("id")
    or manifest.get("snapshotTimestamp")
    or manifest.get("createdAt")
    or "unknown-snapshot"
)
PY
)"

manifest_summary="$(
  SNAPSHOT_MANIFEST_PATH="${snapshot_manifest_path}" python3 - <<'PY'
import json
import os
from pathlib import Path

manifest = json.loads(Path(os.environ["SNAPSHOT_MANIFEST_PATH"]).read_text(encoding="utf-8"))
repos = manifest.get("repositories") or manifest.get("repos") or []

if isinstance(repos, dict):
    repos = [
        {"name": name, **details} if isinstance(details, dict) else {"name": name, "value": details}
        for name, details in repos.items()
    ]

lines = []
for repo in repos:
    if not isinstance(repo, dict):
        continue
    name = repo.get("name") or repo.get("repo") or "unknown"
    branch = repo.get("branch") or ""
    sha = repo.get("commitSha") or repo.get("sha") or repo.get("commit") or ""
    url = repo.get("url") or repo.get("gitlabUrl") or ""
    parts = [str(name)]
    if branch:
        parts.append(f"branch={branch}")
    if sha:
        parts.append(f"sha={sha}")
    if url:
        parts.append(f"url={url}")
    lines.append(" - " + " ".join(parts))

print("\n".join(lines) if lines else " - manifest did not contain a repositories list")
PY
)"

cat >"${schema_path}" <<'JSON'
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "status",
    "confidence",
    "summary",
    "legacyEvidence",
    "modernEvidence",
    "gaps",
    "recommendedNextStep"
  ],
  "properties": {
    "status": {
      "type": "string",
      "enum": ["MATCHES_LEGACY", "GAP_FOUND", "UNCERTAIN"]
    },
    "confidence": {
      "type": "string",
      "enum": ["high", "medium", "low"]
    },
    "summary": {
      "type": "string",
      "maxLength": 900
    },
    "legacyEvidence": {
      "type": "array",
      "maxItems": 10,
      "items": {
        "type": "string",
        "maxLength": 240
      }
    },
    "modernEvidence": {
      "type": "array",
      "maxItems": 10,
      "items": {
        "type": "string",
        "maxLength": 240
      }
    },
    "gaps": {
      "type": "array",
      "maxItems": 10,
      "items": {
        "type": "string",
        "maxLength": 320
      }
    },
    "recommendedNextStep": {
      "type": "string",
      "maxLength": 500
    }
  }
}
JSON

PROMPT_PATH="${prompt_path}" \
ISSUE_KEY="${ISSUE_KEY}" \
ISSUE_SUMMARY="${ISSUE_SUMMARY}" \
ISSUE_DESCRIPTION="${ISSUE_DESCRIPTION}" \
ISSUE_URL="${ISSUE_URL}" \
GITHUB_REPOSITORY="${GITHUB_REPOSITORY:-hmcts/appreg-api}" \
LEGACY_SNAPSHOT_DIR="${legacy_snapshot_dir}" \
SNAPSHOT_ID="${snapshot_id}" \
MANIFEST_SUMMARY="${manifest_summary}" \
python3 - <<'PY'
import os
from pathlib import Path

prompt = f"""You are Codex running a report-only legacy parity check for Apps Reg.

Task:
Compare the Jira ticket against the legacy Apps Reg source snapshot and the modern backend repository.
Decide whether the functionality described in the Jira ticket appears to match the legacy implementation.

Hard rules:
- Do not modify files.
- Do not create branches, commits, or pull requests.
- Do not include secrets, tokens, credentials, PII, runner file contents, environment variables, or auth material.
- Do not quote or copy legacy source code snippets.
- Evidence must be references only: repository name, file path, class/component/service/function names.
- If you cannot find enough evidence, return UNCERTAIN rather than guessing.

Modern repository:
- Repository: {os.environ["GITHUB_REPOSITORY"]}
- Working directory: current checkout

Legacy snapshot:
- Root: {os.environ["LEGACY_SNAPSHOT_DIR"]}
- Snapshot: {os.environ["SNAPSHOT_ID"]}
{os.environ["MANIFEST_SUMMARY"]}

Jira ticket:
- Key: {os.environ["ISSUE_KEY"]}
- URL: {os.environ["ISSUE_URL"]}
- Summary: {os.environ["ISSUE_SUMMARY"]}

Description:
{os.environ["ISSUE_DESCRIPTION"]}

Return only JSON matching the supplied schema.
"""

Path(os.environ["PROMPT_PATH"]).write_text(prompt, encoding="utf-8")
PY

echo "Running report-only Apps Reg legacy parity check for ${ISSUE_KEY}"
run_codex_exec_with_usage "legacy-parity-check" "${usage_events_path}" "${usage_summary_path}" \
  run_codex codex exec \
  --json \
  --cd "${PWD}" \
  --add-dir "${legacy_snapshot_dir}" \
  --sandbox read-only \
  --ephemeral \
  --output-schema "${schema_path}" \
  --output-last-message "${final_message_path}" \
  - <"${prompt_path}"

SNAPSHOT_ID="${snapshot_id}" \
FINAL_MESSAGE_PATH="${final_message_path}" \
REPORT_PATH="${report_path}" \
COMMENT_PATH="${comment_path}" \
python3 - <<'PY'
import json
import os
import re
from pathlib import Path

valid_statuses = {"MATCHES_LEGACY", "GAP_FOUND", "UNCERTAIN"}
valid_confidence = {"high", "medium", "low"}


def coerce_string(value, limit):
    text = str(value or "").replace("\r", " ").replace("\n", " ").strip()
    text = re.sub(r"\s+", " ", text)
    return text[:limit]


def coerce_array(value, limit, item_limit):
    if not isinstance(value, list):
        return []
    return [coerce_string(item, item_limit) for item in value[:limit] if coerce_string(item, item_limit)]


def parse_final_message(path):
    text = Path(path).read_text(encoding="utf-8", errors="replace").strip()
    if not text:
        return {}
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", text, flags=re.DOTALL)
        if match:
            return json.loads(match.group(0))
        raise


try:
    raw = parse_final_message(os.environ["FINAL_MESSAGE_PATH"])
except Exception as error:
    raw = {
        "status": "UNCERTAIN",
        "confidence": "low",
        "summary": f"Codex parity report could not be parsed: {error}",
        "legacyEvidence": [],
        "modernEvidence": [],
        "gaps": ["Parity workflow needs manual review because the Codex output was not valid JSON."],
        "recommendedNextStep": "Inspect the GitHub Actions run logs and rerun the parity check."
    }

status = raw.get("status") if raw.get("status") in valid_statuses else "UNCERTAIN"
confidence = raw.get("confidence") if raw.get("confidence") in valid_confidence else "low"

report = {
    "issueKey": os.environ.get("ISSUE_KEY", ""),
    "issueUrl": os.environ.get("ISSUE_URL", ""),
    "repository": os.environ.get("GITHUB_REPOSITORY", ""),
    "status": status,
    "confidence": confidence,
    "summary": coerce_string(raw.get("summary"), 900),
    "runUrl": f"{os.environ.get('GITHUB_SERVER_URL', 'https://github.com')}/{os.environ.get('GITHUB_REPOSITORY', '')}/actions/runs/{os.environ.get('GITHUB_RUN_ID', '')}",
    "snapshotId": os.environ["SNAPSHOT_ID"],
    "legacyEvidence": coerce_array(raw.get("legacyEvidence"), 10, 240),
    "modernEvidence": coerce_array(raw.get("modernEvidence"), 10, 240),
    "gaps": coerce_array(raw.get("gaps"), 10, 320),
    "recommendedNextStep": coerce_string(raw.get("recommendedNextStep"), 500),
}

Path(os.environ["REPORT_PATH"]).write_text(json.dumps(report, indent=2, sort_keys=True), encoding="utf-8")

lines = [
    "## Apps Reg legacy parity check",
    "",
    f"Result: {report['status']}",
    f"Confidence: {report['confidence']}",
    f"Snapshot: {report['snapshotId']}",
    "",
    report["summary"] or "No summary provided.",
    "",
    "Legacy evidence:",
]
lines.extend(f"- {item}" for item in report["legacyEvidence"] or ["No legacy evidence identified."])
lines.append("")
lines.append("Modern evidence:")
lines.extend(f"- {item}" for item in report["modernEvidence"] or ["No modern evidence identified."])
lines.append("")
lines.append("Gaps:")
lines.extend(f"- {item}" for item in report["gaps"] or ["No gaps listed."])
lines.append("")
lines.append(f"Recommended next step: {report['recommendedNextStep'] or 'Manual review.'}")
lines.append(f"Run: {report['runUrl']}")

Path(os.environ["COMMENT_PATH"]).write_text("\n".join(lines) + "\n", encoding="utf-8")
PY

echo "Parity report written to ${report_path}"
