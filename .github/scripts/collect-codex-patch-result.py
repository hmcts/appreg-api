#!/usr/bin/env python3
"""Materialise an untrusted Codex patch result in a fresh workflow job."""

from __future__ import annotations

import base64
import binascii
import gzip
import io
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path, PurePosixPath


MAX_ENCODED_PATCH_BYTES = 60_000
MAX_PATCH_BYTES = 5 * 1024 * 1024
EXPECTED_KEYS = {"has_changes", "patch_gzip_base64", "summary", "testing"}
HUNK_HEADER = re.compile(
    r"^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@(?: .*)?$"
)
GIT_PATH_ESCAPES = {
    "a": 0x07,
    "b": 0x08,
    "t": 0x09,
    "n": 0x0A,
    "v": 0x0B,
    "f": 0x0C,
    "r": 0x0D,
    '"': 0x22,
    "\\": 0x5C,
}


def fail(message: str) -> None:
    raise ValueError(message)


def validate_text(result: dict[str, object], key: str, maximum: int) -> str:
    value = result.get(key)
    if not isinstance(value, str):
        fail(f"Codex result field {key!r} must be a string")
    if len(value) > maximum:
        fail(f"Codex result field {key!r} exceeds {maximum} characters")
    return value


def normalise_patch_path(path: str) -> str:
    parsed = PurePosixPath(path)
    if (
        not path
        or path.startswith("/")
        or not parsed.parts
        or ".." in parsed.parts
        or parsed.parts[0] == ".git"
    ):
        fail(f"Unsafe Git diff path: {path!r}")
    return path


def decode_git_path(value: str) -> str:
    if not value.startswith('"'):
        return value.split("\t", 1)[0]

    decoded = bytearray()
    index = 1
    while index < len(value):
        character = value[index]
        if character == '"':
            suffix = value[index + 1 :]
            if suffix and not suffix.startswith("\t"):
                fail(f"Malformed quoted Git path: {value!r}")
            try:
                return decoded.decode("utf-8")
            except UnicodeDecodeError as error:
                fail(f"Quoted Git path is not UTF-8: {error}")
        if character != "\\":
            decoded.extend(character.encode("utf-8"))
            index += 1
            continue

        index += 1
        if index >= len(value):
            fail(f"Malformed quoted Git path: {value!r}")
        escaped = value[index]
        if escaped in GIT_PATH_ESCAPES:
            decoded.append(GIT_PATH_ESCAPES[escaped])
            index += 1
            continue
        if escaped not in "01234567":
            fail(f"Unsupported Git path escape: \\{escaped}")
        octal = escaped
        index += 1
        while index < len(value) and len(octal) < 3 and value[index] in "01234567":
            octal += value[index]
            index += 1
        decoded.append(int(octal, 8))

    fail(f"Unterminated quoted Git path: {value!r}")


def metadata_path(value: str, prefix: str | None = None) -> str | None:
    decoded = decode_git_path(value)
    if decoded == "/dev/null":
        return None
    if prefix is not None:
        if not decoded.startswith(prefix):
            fail(f"Malformed Git diff path: {decoded!r}")
        decoded = decoded[len(prefix) :]
    return normalise_patch_path(decoded)


def patch_structure_paths(patch: str) -> tuple[int, set[str]]:
    section_count = 0
    paths: set[str] = set()
    in_section = False
    in_binary_patch = False
    old_remaining = 0
    new_remaining = 0
    seen_old_path = False
    seen_new_path = False
    extended_headers: set[str] = set()

    def validate_section() -> None:
        if seen_old_path != seen_new_path:
            fail("Git patch contains an incomplete ---/+++ path pair")
        if ("rename from" in extended_headers) != ("rename to" in extended_headers):
            fail("Git patch contains an incomplete rename path pair")
        if ("copy from" in extended_headers) != ("copy to" in extended_headers):
            fail("Git patch contains an incomplete copy path pair")

    for line_number, line in enumerate(patch.splitlines(), start=1):
        if old_remaining or new_remaining:
            if line == r"\ No newline at end of file":
                continue
            if not line:
                fail(f"Malformed Git hunk at line {line_number}")
            marker = line[0]
            if marker == " ":
                old_remaining -= 1
                new_remaining -= 1
            elif marker == "-":
                old_remaining -= 1
            elif marker == "+":
                new_remaining -= 1
            else:
                fail(f"Malformed Git hunk at line {line_number}")
            if old_remaining < 0 or new_remaining < 0:
                fail(f"Git hunk exceeds its declared size at line {line_number}")
            continue

        if in_binary_patch and not line.startswith("diff --git "):
            continue

        if line.startswith("diff --git "):
            if in_section:
                validate_section()
            section_count += 1
            in_section = True
            in_binary_patch = False
            seen_old_path = False
            seen_new_path = False
            extended_headers = set()
            continue

        if not in_section:
            if line:
                fail(
                    "Git patch content appears outside a diff --git section "
                    f"at line {line_number}"
                )
            continue

        hunk = HUNK_HEADER.match(line)
        if hunk:
            old_remaining = int(hunk.group(2) or "1")
            new_remaining = int(hunk.group(4) or "1")
            continue
        if line.startswith("@@"):
            fail(f"Unsupported or malformed Git hunk header at line {line_number}")
        if line == "GIT binary patch":
            in_binary_patch = True
            continue
        if line.startswith("Binary files "):
            fail("Codex patch contains an incomplete binary diff")
        if line.startswith("--- "):
            if seen_old_path:
                fail("Git patch contains multiple --- path headers in one section")
            old_path = metadata_path(line[4:], "a/")
            if old_path is not None:
                paths.add(old_path)
            seen_old_path = True
            continue
        if line.startswith("+++ "):
            if not seen_old_path or seen_new_path:
                fail("Git patch contains an unexpected +++ path header")
            new_path = metadata_path(line[4:], "b/")
            if new_path is not None:
                paths.add(new_path)
            seen_new_path = True
            continue
        for header in ("rename from", "rename to", "copy from", "copy to"):
            marker = f"{header} "
            if not line.startswith(marker):
                continue
            if header in extended_headers:
                fail(f"Git patch contains multiple {header} headers in one section")
            paths.add(metadata_path(line[len(marker) :]) or "")
            extended_headers.add(header)
            break

    if old_remaining or new_remaining:
        fail("Git patch ended before its final hunk was complete")
    if in_section:
        validate_section()
    if not section_count:
        fail("Codex patch contains no Git diff headers")
    paths.discard("")
    return section_count, paths


def git_numstat_paths(patch: str) -> list[str]:
    git = shutil.which("git")
    if git is None:
        fail("Git is required to validate the Codex patch")

    with tempfile.TemporaryDirectory(prefix="codex-patch-parse-") as parse_dir:
        environment = {
            "HOME": parse_dir,
            "PATH": os.environ.get("PATH", ""),
            "GIT_CEILING_DIRECTORIES": parse_dir,
            "GIT_CONFIG_GLOBAL": os.devnull,
            "GIT_CONFIG_NOSYSTEM": "1",
            "GIT_TERMINAL_PROMPT": "0",
        }
        completed = subprocess.run(
            [git, "-c", "core.hooksPath=/dev/null", "apply", "--numstat", "-z", "--binary", "-"],
            input=patch.encode("utf-8"),
            cwd=parse_dir,
            env=environment,
            capture_output=True,
            check=False,
        )
    if completed.returncode != 0:
        detail = completed.stderr.decode("utf-8", errors="replace").strip()[:1_000]
        fail(f"Git rejected the Codex patch: {detail or 'unknown parse error'}")
    if not completed.stdout or not completed.stdout.endswith(b"\0"):
        fail("Git found no complete file records in the Codex patch")

    paths: list[str] = []
    for record in completed.stdout[:-1].split(b"\0"):
        fields = record.split(b"\t", 2)
        if len(fields) != 3 or any(
            value != b"-" and not value.isdigit() for value in fields[:2]
        ):
            fail("Git returned malformed numstat data for the Codex patch")
        try:
            path = fields[2].decode("utf-8")
        except UnicodeDecodeError as error:
            fail(f"Git patch path is not UTF-8: {error}")
        paths.append(normalise_patch_path(path))
    return paths


def patch_paths(patch: str) -> set[str]:
    section_count, metadata_paths = patch_structure_paths(patch)
    parsed_paths = git_numstat_paths(patch)
    if len(parsed_paths) != section_count:
        fail(
            "Git patch file records do not match its diff --git sections; "
            "raw or malformed patch sections are not allowed"
        )
    return set(parsed_paths) | metadata_paths


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
    if os.environ.get("WRITE_PR_DETAIL_FILES", "false").lower() == "true":
        (output_dir / "codex-summary.txt").write_text(
            summary or "No summary supplied.", encoding="utf-8"
        )
        (output_dir / "codex-testing.txt").write_text(
            testing or "No lightweight checks were reported.", encoding="utf-8"
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
