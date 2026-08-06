#!/usr/bin/env bash

# Shared runtime helpers for Codex invocations authenticated through
# openai/codex-action's local Responses API proxy.

prepare_codex_action_runtime() {
  local runtime_path
  for runtime_path in "$@"; do
    [[ -e "${runtime_path}" ]] || continue
    chmod -R g+rwX "${runtime_path}"
    find "${runtime_path}" -type d -exec chmod g+s {} +
  done
}

prepare_codex_read_only_runtime() {
  local runtime_path
  for runtime_path in "$@"; do
    [[ -e "${runtime_path}" ]] || continue
    chmod -R g+rX "${runtime_path}"
    chmod -R g-w,o-w "${runtime_path}"
  done
}

capture_codex_output_schema() {
  local schema_source="$1"
  local artifact_dir="$2"
  local output_name="$3"
  local schema_path="${artifact_dir}/${output_name}"

  install -m 0444 "${schema_source}" "${schema_path}"
  printf '%s\n' "${schema_path}"
}

capture_codex_patch_schema() {
  local schema_source="$1"
  local artifact_dir="$2"

  capture_codex_output_schema "${schema_source}" "${artifact_dir}" codex-patch-result.schema.json
}

capture_codex_patch_exporter() {
  local exporter_source="$1"
  local artifact_dir="$2"
  local exporter_path="${artifact_dir}/codex-patch-export.sh"

  install -m 0555 "${exporter_source}" "${exporter_path}"
  printf '%s\n' "${exporter_path}"
}

validated_codex_plan_path() {
  local plan_dir="$1"
  local plan_path="${plan_dir}/plan.json"
  local sha_path="${plan_dir}/plan.sha256"
  local allowed_paths_path="${plan_dir}/allowed-paths.txt"

  python3 -I - "${plan_path}" "${sha_path}" "${allowed_paths_path}" <<'PY'
import hashlib
import json
import re
import sys
from pathlib import Path

plan_path = Path(sys.argv[1])
sha_path = Path(sys.argv[2])
allowed_paths_path = Path(sys.argv[3])
paths = (plan_path, sha_path, allowed_paths_path)
if any(path.is_symlink() or not path.is_file() for path in paths):
    raise SystemExit("Missing validated Codex plan hand-off")
plan_bytes = plan_path.read_bytes()
if not plan_bytes or len(plan_bytes) > 32 * 1024:
    raise SystemExit("Validated Codex plan is empty or oversized")
expected = sha_path.read_text(encoding="ascii").strip().lower()
if not re.fullmatch(r"[0-9a-f]{64}", expected):
    raise SystemExit("Validated Codex plan hash is malformed")
actual = hashlib.sha256(plan_bytes).hexdigest()
if actual != expected:
    raise SystemExit("Validated Codex plan hash does not match plan.json")
try:
    plan = json.loads(plan_bytes)
    steps = plan["implementation_steps"]
    allowed_paths = [step["path"] for step in steps]
except (json.JSONDecodeError, KeyError, TypeError) as error:
    raise SystemExit(f"Validated Codex plan structure is invalid: {error}") from error
if plan.get("ready_to_implement") is not True or not allowed_paths:
    raise SystemExit("Validated Codex plan is not ready for scoped implementation")
if len(set(allowed_paths)) != len(allowed_paths) or not all(
    isinstance(path, str) and path and "\n" not in path and "\r" not in path
    for path in allowed_paths
):
    raise SystemExit("Validated Codex plan paths are invalid")
expected_paths = ("\n".join(allowed_paths) + "\n").encode("utf-8")
if allowed_paths_path.read_bytes() != expected_paths:
    raise SystemExit("Validated Codex allowed paths do not match plan.json")
print(plan_path)
PY
}

prepare_codex_patch_contract() {
  local prompt_path="$1"
  local schema_path="$2"
  local exporter_path="$3"
  local artifact_dir="$4"
  local patch_scope="${5:-full}"
  local scoped_paths_file="${6:-}"

  if [[ ! -s "${schema_path}" ]]; then
    echo "Missing captured Codex patch schema: ${schema_path}" >&2
    return 1
  fi
  if [[ ! -x "${exporter_path}" ]]; then
    echo "Missing captured Codex patch exporter: ${exporter_path}" >&2
    return 1
  fi

  cat >>"${prompt_path}" <<'EOF'

Patch hand-off contract:
- The Codex Action is the final step in this job. No privileged process will inspect this working tree after you finish.
- The `:workspace` permission profile makes the real `.git` directory read-only. Do not run `git add` against the real checkout or try to modify `.git`.
- After making the complete intended change, run the captured trusted patch exporter specified below. It uses a temporary Git index and object store outside `.git`.
- Return only the JSON object required by the supplied output schema.
- Copy the exporter's `has_changes` and `patch_gzip_base64` values exactly into the final JSON object.
- Put the human-readable implementation summary in `summary` and checks performed in `testing`.
- If the exporter fails, do not fabricate or truncate a patch. Report the failure in `summary` with no patch so the trusted collector fails closed when changes are required.
EOF

  case "${patch_scope}" in
  conflicted-files)
    if [[ ! -s "${scoped_paths_file}" ]]; then
      echo "Missing captured conflict path scope: ${scoped_paths_file}" >&2
      return 1
    fi
    chmod 0444 "${scoped_paths_file}"
    printf '%s\n' \
      "- For this conflict-resolution operation, export only the captured conflicted paths by running: \`${exporter_path} --paths-file ${scoped_paths_file}\`." \
      >>"${prompt_path}"
    ;;
  planned-files)
    if [[ ! -s "${scoped_paths_file}" ]]; then
      echo "Missing captured implementation plan path scope: ${scoped_paths_file}" >&2
      return 1
    fi
    chmod 0444 "${scoped_paths_file}"
    printf '%s\n' \
      "- Export only the exact files approved in the validated plan by running: \`${exporter_path} --paths-file ${scoped_paths_file} --strict-paths\`. The exporter fails if any other repository path changed." \
      >>"${prompt_path}"
    ;;
  full)
    printf '%s\n' \
      "- Export the complete working-tree change by running: \`${exporter_path}\`." \
      >>"${prompt_path}"
    ;;
  *)
    echo "Unsupported Codex patch scope: ${patch_scope}" >&2
    return 1
    ;;
  esac

  chmod 0444 "${prompt_path}"
  chmod 0755 "${artifact_dir}"
  printf '%s\n' "${schema_path}"
}
