#!/usr/bin/env bash

set -euo pipefail

max_encoded_patch_bytes=60000
paths_file=""
strict_paths=false

usage() {
  echo "Usage: $0 [--paths-file <path>] [--strict-paths]" >&2
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --paths-file)
      if [[ $# -lt 2 ]]; then
        usage
        exit 2
      fi
      paths_file="$2"
      shift 2
      ;;
    --strict-paths)
      strict_paths=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 2
      ;;
  esac
done

repo_root="$(git rev-parse --show-toplevel)"
common_dir="$(git -C "${repo_root}" rev-parse --path-format=absolute --git-common-dir)"
scratch_dir="$(mktemp -d "${TMPDIR:-/tmp}/codex-patch-export.XXXXXX")"
trap 'rm -rf -- "${scratch_dir}"' EXIT

mkdir -p "${scratch_dir}/objects"

patch_git() {
  env \
    GIT_INDEX_FILE="${scratch_dir}/index" \
    GIT_OBJECT_DIRECTORY="${scratch_dir}/objects" \
    GIT_ALTERNATE_OBJECT_DIRECTORIES="${common_dir}/objects" \
    GIT_CONFIG_GLOBAL=/dev/null \
    GIT_CONFIG_NOSYSTEM=1 \
    GIT_TERMINAL_PROMPT=0 \
    git -C "${repo_root}" \
      -c core.fsmonitor= \
      -c core.hooksPath=/dev/null \
      -c diff.external= \
      -c credential.helper= \
      -c protocol.file.allow=never \
      "$@"
}

validate_path() {
  local path="$1"
  local component
  local components=()

  if [[ -z "${path}" || "${path}" == /* || "${path}" == $'\r'* || "${path}" == *$'\r'* ]]; then
    echo "Unsafe patch scope path: ${path}" >&2
    exit 1
  fi

  IFS='/' read -r -a components <<<"${path}"
  if [[ "${components[0]}" == ".git" ]]; then
    echo "Unsafe patch scope path: ${path}" >&2
    exit 1
  fi
  for component in "${components[@]}"; do
    if [[ -z "${component}" || "${component}" == "." || "${component}" == ".." ]]; then
      echo "Unsafe patch scope path: ${path}" >&2
      exit 1
    fi
  done
}

pathspecs=()
if [[ -n "${paths_file}" ]]; then
  if [[ ! -s "${paths_file}" ]]; then
    echo "Missing or empty patch scope file: ${paths_file}" >&2
    exit 1
  fi
  while IFS= read -r path || [[ -n "${path}" ]]; do
    [[ -n "${path}" ]] || continue
    validate_path "${path}"
    pathspecs+=(":(top,literal)${path}")
  done <"${paths_file}"
  if [[ ${#pathspecs[@]} -eq 0 ]]; then
    echo "Patch scope file contains no paths: ${paths_file}" >&2
    exit 1
  fi
else
  pathspecs=(.)
fi

patch_git read-tree HEAD
if [[ "${strict_paths}" == "true" ]]; then
  if [[ -z "${paths_file}" ]]; then
    echo "--strict-paths requires --paths-file" >&2
    exit 2
  fi

  patch_git add -A -- .
  changed_paths_file="${scratch_dir}/changed-paths.bin"
  patch_git diff \
    --cached \
    --name-only \
    -z \
    --no-renames \
    --no-ext-diff \
    HEAD \
    -- . >"${changed_paths_file}"

  ALLOWED_PATHS_FILE="${paths_file}" CHANGED_PATHS_FILE="${changed_paths_file}" python3 -I - <<'PY'
import os
from pathlib import Path

allowed = {
    line
    for line in Path(os.environ["ALLOWED_PATHS_FILE"]).read_text(encoding="utf-8").splitlines()
    if line
}
raw_paths = Path(os.environ["CHANGED_PATHS_FILE"]).read_bytes()
try:
    changed = {
        value.decode("utf-8")
        for value in raw_paths.split(b"\0")
        if value
    }
except UnicodeDecodeError as error:
    raise SystemExit(f"Changed repository path is not UTF-8: {error}") from error

unexpected = sorted(changed - allowed)
if unexpected:
    detail = ", ".join(repr(path) for path in unexpected)
    raise SystemExit(f"Refusing to export changes outside the validated plan: {detail}")
PY
else
  patch_git add -A -- "${pathspecs[@]}"
fi

if patch_git diff --cached --quiet --no-ext-diff HEAD -- "${pathspecs[@]}"; then
  printf '%s\n' '{"has_changes":false,"patch_gzip_base64":""}'
  exit 0
fi

patch_path="${scratch_dir}/changes.patch"
encoded_path="${scratch_dir}/changes.patch.gz.b64"
patch_git diff \
  --cached \
  --binary \
  --full-index \
  --no-ext-diff \
  --no-textconv \
  --no-renames \
  HEAD \
  -- "${pathspecs[@]}" >"${patch_path}"

gzip -9 -n -c "${patch_path}" | base64 | tr -d '\r\n' >"${encoded_path}"
encoded_size="$(wc -c <"${encoded_path}" | tr -d '[:space:]')"
if [[ "${encoded_size}" -gt "${max_encoded_patch_bytes}" ]]; then
  echo "Encoded Codex patch exceeds ${max_encoded_patch_bytes} characters (${encoded_size})." >&2
  exit 1
fi

printf '{"has_changes":true,"patch_gzip_base64":"'
cat "${encoded_path}"
printf '"}\n'
