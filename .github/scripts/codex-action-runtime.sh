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

prepare_codex_patch_contract() {
  local prompt_path="$1"
  local schema_path="$2"
  local artifact_dir="$3"
  local patch_scope="${4:-full}"

  if [[ ! -s "${schema_path}" ]]; then
    echo "Missing captured Codex patch schema: ${schema_path}" >&2
    return 1
  fi

  cat >>"${prompt_path}" <<'EOF'

Patch hand-off contract:
- The Codex Action is the final step in this job. No privileged process will inspect this working tree after you finish.
- Before returning, stage the complete intended patch using Git.
- Generate a binary Git patch from the staged changes, gzip it, and base64 encode it as one line.
- Return only the JSON object required by the supplied output schema.
- Set `has_changes` to true and put the encoded patch in `patch_gzip_base64` when a patch exists.
- Set `has_changes` to false and `patch_gzip_base64` to an empty string only when no code change is appropriate.
- Put the human-readable implementation summary in `summary` and checks performed in `testing`.
- The encoded patch must be no more than 60000 characters. If it exceeds that limit, report the size problem in `summary` with no patch so the trusted job fails closed.
EOF

  if [[ "${patch_scope}" == "conflicted-files" ]]; then
    cat >>"${prompt_path}" <<'EOF'
- For this conflict-resolution operation, stage only the conflicted files listed above and generate the patch relative to HEAD for only those paths.
- A suitable command pattern is: `git add -- <conflicted paths> && git -c core.fsmonitor= -c diff.external= diff --cached --binary --no-ext-diff HEAD -- <conflicted paths> | gzip -9 | base64 | tr -d '\n'`.
EOF
  else
    cat >>"${prompt_path}" <<'EOF'
- A suitable command pattern is: `git add -A && git -c core.fsmonitor= -c diff.external= diff --cached --binary --no-ext-diff | gzip -9 | base64 | tr -d '\n'`.
EOF
  fi

  chmod 0444 "${prompt_path}"
  chmod 0755 "${artifact_dir}"
  printf '%s\n' "${schema_path}"
}
