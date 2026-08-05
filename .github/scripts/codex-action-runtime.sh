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

capture_codex_patch_schema() {
  local schema_source="$1"
  local artifact_dir="$2"
  local schema_path="${artifact_dir}/codex-patch-result.schema.json"

  install -m 0444 "${schema_source}" "${schema_path}"
  printf '%s\n' "${schema_path}"
}

capture_codex_patch_exporter() {
  local exporter_source="$1"
  local artifact_dir="$2"
  local exporter_path="${artifact_dir}/codex-patch-export.sh"

  install -m 0555 "${exporter_source}" "${exporter_path}"
  printf '%s\n' "${exporter_path}"
}

prepare_codex_patch_contract() {
  local prompt_path="$1"
  local schema_path="$2"
  local exporter_path="$3"
  local artifact_dir="$4"
  local patch_scope="${5:-full}"
  local allowed_paths_file="${6:-}"

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

  if [[ "${patch_scope}" == "conflicted-files" ]]; then
    if [[ ! -s "${allowed_paths_file}" ]]; then
      echo "Missing captured conflict path scope: ${allowed_paths_file}" >&2
      return 1
    fi
    chmod 0444 "${allowed_paths_file}"
    printf '%s\n' \
      "- For this conflict-resolution operation, export only the captured conflicted paths by running: \`${exporter_path} --paths-file ${allowed_paths_file}\`." \
      >>"${prompt_path}"
  else
    printf '%s\n' \
      "- Export the complete working-tree change by running: \`${exporter_path}\`." \
      >>"${prompt_path}"
  fi

  chmod 0444 "${prompt_path}"
  chmod 0755 "${artifact_dir}"
  printf '%s\n' "${schema_path}"
}
