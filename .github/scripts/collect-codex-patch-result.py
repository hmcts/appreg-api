#!/usr/bin/env python3
"""Materialise an untrusted Codex patch result in a fresh workflow job."""

from __future__ import annotations

import base64
import binascii
import gzip
import io
import json
import os
import shlex
import sys
from pathlib import Path, PurePosixPath


MAX_ENCODED_PATCH_BYTES = 60_000
MAX_PATCH_BYTES = 5 * 1024 * 1024
EXPECTED_KEYS = {"has_changes", "patch_gzip_base64", "summary", "testing"}


def fail(message: str) -> None:
    raise ValueError(message)


def validate_text(result: dict[str, object], key: str, maximum: int) -> str:
    value = result.get(key)
    if not isinstance(value, str):
        fail(f"Codex result field {key!r} must be a string")
    if len(value) > maximum:
        fail(f"Codex result field {key!r} exceeds {maximum} characters")
    return value


def normalise_diff_path(token: str, prefix: str) -> str:
    if not token.startswith(prefix):
        fail(f"Malformed Git diff path: {token!r}")
    path = token[len(prefix) :]
    parsed = PurePosixPath(path)
    if not path or path.startswith("/") or ".." in parsed.parts or parsed.parts[0] == ".git":
        fail(f"Unsafe Git diff path: {path!r}")
    return path


def patch_paths(patch: str) -> set[str]:
    paths: set[str] = set()
    for line in patch.splitlines():
        if not line.startswith("diff --git "):
            continue
        try:
            parts = shlex.split(line)
        except ValueError as error:
            fail(f"Malformed Git diff header: {error}")
        if len(parts) != 4:
            fail(f"Malformed Git diff header: {line!r}")
        paths.add(normalise_diff_path(parts[2], "a/"))
        paths.add(normalise_diff_path(parts[3], "b/"))
    if not paths:
        fail("Codex patch contains no Git diff headers")
    return paths


def decode_patch(encoded_patch: str) -> str:
    if not encoded_patch:
        fail("Codex reported changes without a patch")
    if len(encoded_patch) > MAX_ENCODED_PATCH_BYTES:
        fail("Encoded Codex patch exceeds the workflow output limit")
    try:
        compressed = base64.b64decode(encoded_patch, validate=True)
        with gzip.GzipFile(fileobj=io.BytesIO(compressed)) as compressed_file:
            patch_bytes = compressed_file.read(MAX_PATCH_BYTES + 1)
    except (binascii.Error, gzip.BadGzipFile, EOFError, OSError) as error:
        fail(f"Codex patch is not valid gzip/base64 data: {error}")
    if len(patch_bytes) > MAX_PATCH_BYTES:
        fail("Decoded Codex patch exceeds the 5 MiB safety limit")
    if b"\0" in patch_bytes:
        fail("Decoded Codex patch contains a NUL byte")
    try:
        return patch_bytes.decode("utf-8")
    except UnicodeDecodeError as error:
        fail(f"Decoded Codex patch is not UTF-8 text: {error}")


def read_allowed_paths() -> set[str] | None:
    allowed_paths_file = os.environ.get("ALLOWED_PATHS_FILE", "")
    if not allowed_paths_file:
        return None
    values = {
        line.strip()
        for line in Path(allowed_paths_file).read_text(encoding="utf-8").splitlines()
        if line.strip()
    }
    if not values:
        fail("Allowed-paths file is empty")
    for value in values:
        parsed = PurePosixPath(value)
        if value.startswith("/") or ".." in parsed.parts or parsed.parts[0] == ".git":
            fail(f"Unsafe allowed path: {value!r}")
    return values


def main() -> None:
    raw_result = os.environ.get("CODEX_RESULT", "")
    if not raw_result:
        fail("Missing CODEX_RESULT from the final Codex Action step")
    try:
        result = json.loads(raw_result)
    except json.JSONDecodeError as error:
        fail(f"Codex result is not valid JSON: {error}")
    if not isinstance(result, dict) or set(result) != EXPECTED_KEYS:
        fail("Codex result does not match the required patch-result contract")

    has_changes = result.get("has_changes")
    if not isinstance(has_changes, bool):
        fail("Codex result field 'has_changes' must be a boolean")
    encoded_patch = validate_text(result, "patch_gzip_base64", MAX_ENCODED_PATCH_BYTES)
    summary = validate_text(result, "summary", 4_000)
    testing = validate_text(result, "testing", 4_000)

    output_dir = Path(os.environ["OUTPUT_DIR"])
    output_dir.mkdir(parents=True, exist_ok=True)
    patch_path = output_dir / "changes.patch"

    if has_changes:
        patch = decode_patch(encoded_patch)
        changed_paths = patch_paths(patch)
        allowed_paths = read_allowed_paths()
        if allowed_paths is not None and not changed_paths.issubset(allowed_paths):
            unexpected = ", ".join(sorted(changed_paths - allowed_paths))
            fail(f"Codex patch changes paths outside the allowed set: {unexpected}")
        patch_path.write_text(patch, encoding="utf-8")
    else:
        if encoded_patch:
            fail("Codex returned a patch while reporting has_changes=false")
        if os.environ.get("REQUIRE_CHANGES", "false").lower() == "true":
            fail("Codex produced no changes for an operation that requires a patch")
        patch_path.unlink(missing_ok=True)

    final_message_path = output_dir / "codex-final-message.md"
    final_message_path.write_text(
        f"## Summary\n\n{summary or 'No summary supplied.'}\n\n"
        f"## Testing\n\n{testing or 'No lightweight checks were reported.'}\n",
        encoding="utf-8",
    )
    (output_dir / "codex-result.env").write_text(
        f"has_changes={'true' if has_changes else 'false'}\n", encoding="utf-8"
    )

    github_output = os.environ.get("GITHUB_OUTPUT", "")
    if github_output:
        with Path(github_output).open("a", encoding="utf-8") as output_file:
            output_file.write(f"has_changes={'true' if has_changes else 'false'}\n")


if __name__ == "__main__":
    try:
        main()
    except (KeyError, OSError, ValueError) as error:
        print(f"Invalid Codex patch result: {error}", file=sys.stderr)
        raise SystemExit(1) from error
