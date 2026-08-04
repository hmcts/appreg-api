#!/usr/bin/env bash

set -euo pipefail

write_codex_usage_summary() {
  local events_path="$1"
  local summary_path="$2"
  local invocation="$3"
  local exit_status="$4"

  CODEX_EVENTS_PATH="${events_path}" \
    CODEX_USAGE_SUMMARY_PATH="${summary_path}" \
    CODEX_INVOCATION="${invocation}" \
    CODEX_EXIT_STATUS="${exit_status}" \
    python3 -I - <<'PY'
import json
import os
from pathlib import Path

events_path = Path(os.environ["CODEX_EVENTS_PATH"])
summary_path = Path(os.environ["CODEX_USAGE_SUMMARY_PATH"])

token_keys = {
    "input_tokens",
    "output_tokens",
    "cached_input_tokens",
    "cached_output_tokens",
    "reasoning_tokens",
    "reasoning_output_tokens",
    "total_tokens",
    "prompt_tokens",
    "completion_tokens",
    "cached_tokens",
}

aliases = {
    "prompt_tokens": "input_tokens",
    "completion_tokens": "output_tokens",
    "cached_tokens": "cached_input_tokens",
    "reasoning_output_tokens": "reasoning_tokens",
}

event_count = 0
invalid_json_lines = 0
usage_objects = []
observed_token_fields = {}


def normalize_key(key):
    return aliases.get(key, key)


def walk(value, path=""):
    global observed_token_fields

    if isinstance(value, dict):
        usage = {}
        for key, item in value.items():
            child_path = f"{path}.{key}" if path else key
            if key in token_keys and isinstance(item, (int, float)) and not isinstance(item, bool):
                normalized_key = normalize_key(key)
                usage[normalized_key] = int(item)
                observed_token_fields[child_path] = int(item)
            walk(item, child_path)

        if usage:
            usage_objects.append(usage)
    elif isinstance(value, list):
        for index, item in enumerate(value):
            walk(item, f"{path}[{index}]")


if events_path.exists():
    with events_path.open(encoding="utf-8", errors="replace") as events:
        for line in events:
            line = line.strip()
            if not line:
                continue
            try:
                event = json.loads(line)
            except json.JSONDecodeError:
                invalid_json_lines += 1
                continue
            event_count += 1
            walk(event)

max_usage = {}
for usage in usage_objects:
    for key, value in usage.items():
        max_usage[key] = max(max_usage.get(key, 0), value)

last_usage = usage_objects[-1] if usage_objects else {}
preferred_usage = last_usage or max_usage
if preferred_usage and "total_tokens" not in preferred_usage:
    input_tokens = preferred_usage.get("input_tokens")
    output_tokens = preferred_usage.get("output_tokens")
    if input_tokens is not None and output_tokens is not None:
        preferred_usage = dict(preferred_usage)
        preferred_usage["total_tokens"] = input_tokens + output_tokens

summary = {
    "schemaVersion": 1,
    "repository": os.environ.get("GITHUB_REPOSITORY", ""),
    "runId": os.environ.get("GITHUB_RUN_ID", ""),
    "runAttempt": os.environ.get("GITHUB_RUN_ATTEMPT", ""),
    "invocation": os.environ.get("CODEX_INVOCATION", ""),
    "issueKey": os.environ.get("ISSUE_KEY", ""),
    "prNumber": os.environ.get("PR_NUMBER", ""),
    "codexExitStatus": int(os.environ.get("CODEX_EXIT_STATUS", "0")),
    "usageAvailable": bool(preferred_usage),
    "usage": preferred_usage,
    "maxObservedUsage": max_usage,
    "usageEventCount": len(usage_objects),
    "jsonEventCount": event_count,
    "invalidJsonLineCount": invalid_json_lines,
    "observedTokenFields": observed_token_fields,
    "note": (
        "The official Codex Action does not expose a token-event file to the trusted "
        "collector. The compatibility artefact is retained with usageAvailable=false."
    ),
}

summary_path.parent.mkdir(parents=True, exist_ok=True)
summary_path.write_text(json.dumps(summary, indent=2, sort_keys=True), encoding="utf-8")
PY
}
