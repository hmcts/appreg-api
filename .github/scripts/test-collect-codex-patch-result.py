#!/usr/bin/env python3
"""Regression tests for fresh-job Codex patch materialisation."""

from __future__ import annotations

import base64
import gzip
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("collect-codex-patch-result.py")
PATCH = """diff --git a/example.txt b/example.txt
index 257cc56..5716ca5 100644
--- a/example.txt
+++ b/example.txt
@@ -1 +1 @@
-old
+new
"""
RAW_SECTION_PATCH = PATCH + """--- a/unrelated.txt
+++ b/unrelated.txt
@@ -1 +1 @@
-old
+new
"""
MISMATCHED_SOURCE_PATCH = """diff --git a/example.txt b/example.txt
index 257cc56..5716ca5 100644
--- a/unrelated.txt
+++ b/example.txt
@@ -1 +1 @@
-old
+new
"""
SPACE_PATH_PATCH = """diff --git a/space name.txt b/space name.txt
index 257cc56..5716ca5 100644
--- a/space name.txt\t
+++ b/space name.txt\t
@@ -1 +1 @@
-old
+new
"""
RENAME_PATCH = """diff --git a/old name.txt b/new name.txt
similarity index 100%
rename from old name.txt
rename to new name.txt
"""


def encoded_patch(patch: str = PATCH) -> str:
    return base64.b64encode(gzip.compress(patch.encode("utf-8"), mtime=0)).decode("ascii")


class CollectCodexPatchResultTest(unittest.TestCase):
    def run_collector(
        self,
        result: dict[str, object],
        *,
        require_changes: bool = True,
        allowed_paths: list[str] | None = None,
        poison_git_config: bool = False,
    ) -> tuple[subprocess.CompletedProcess[str], Path, Path, tempfile.TemporaryDirectory[str]]:
        temporary_directory = tempfile.TemporaryDirectory()
        root = Path(temporary_directory.name)
        output_dir = root / "output"
        marker = root / "fsmonitor-executed"
        working_directory = root
        if poison_git_config:
            repository = root / "repository"
            subprocess.run(["git", "init", "--quiet", str(repository)], check=True)
            fsmonitor = root / "fsmonitor.sh"
            fsmonitor.write_text(
                f"#!/bin/sh\ntouch {marker!s}\nprintf '0\\n'\n", encoding="utf-8"
            )
            fsmonitor.chmod(0o755)
            subprocess.run(
                ["git", "-C", str(repository), "config", "core.fsmonitor", str(fsmonitor)],
                check=True,
            )
            working_directory = repository
        environment = {
            **os.environ,
            "CODEX_RESULT": json.dumps(result),
            "OUTPUT_DIR": str(output_dir),
            "REQUIRE_CHANGES": "true" if require_changes else "false",
        }
        if allowed_paths is not None:
            allowed_file = root / "allowed-paths.txt"
            allowed_file.write_text("\n".join(allowed_paths) + "\n", encoding="utf-8")
            environment["ALLOWED_PATHS_FILE"] = str(allowed_file)
        completed = subprocess.run(
            [sys.executable, str(SCRIPT)],
            env=environment,
            cwd=working_directory,
            capture_output=True,
            text=True,
        )
        return completed, output_dir, marker, temporary_directory

    def valid_result(self) -> dict[str, object]:
        return {
            "has_changes": True,
            "patch_gzip_base64": encoded_patch(),
            "summary": "Updated the example.",
            "testing": "Inspected the diff.",
        }

    def test_materialises_valid_patch_without_running_repository_git_config(self) -> None:
        completed, output_dir, marker, temporary_directory = self.run_collector(
            self.valid_result(), poison_git_config=True
        )
        self.addCleanup(temporary_directory.cleanup)

        self.assertEqual(completed.returncode, 0, completed.stderr)
        self.assertEqual((output_dir / "changes.patch").read_text(encoding="utf-8"), PATCH)
        self.assertFalse(marker.exists())

    def test_rejects_patch_outside_allowed_paths(self) -> None:
        completed, _, _, temporary_directory = self.run_collector(
            self.valid_result(), allowed_paths=["different.txt"]
        )
        self.addCleanup(temporary_directory.cleanup)

        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("outside the allowed set", completed.stderr)

    def test_rejects_appended_raw_unified_diff_section(self) -> None:
        result = self.valid_result()
        result["patch_gzip_base64"] = encoded_patch(RAW_SECTION_PATCH)
        completed, _, _, temporary_directory = self.run_collector(
            result, allowed_paths=["example.txt"]
        )
        self.addCleanup(temporary_directory.cleanup)

        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("multiple --- path headers", completed.stderr)

    def test_rejects_mismatched_source_path_outside_allowed_paths(self) -> None:
        result = self.valid_result()
        result["patch_gzip_base64"] = encoded_patch(MISMATCHED_SOURCE_PATCH)
        completed, _, _, temporary_directory = self.run_collector(
            result, allowed_paths=["example.txt"]
        )
        self.addCleanup(temporary_directory.cleanup)

        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("outside the allowed set: unrelated.txt", completed.stderr)

    def test_accepts_git_generated_path_with_spaces(self) -> None:
        result = self.valid_result()
        result["patch_gzip_base64"] = encoded_patch(SPACE_PATH_PATCH)
        completed, _, _, temporary_directory = self.run_collector(
            result, allowed_paths=["space name.txt"]
        )
        self.addCleanup(temporary_directory.cleanup)

        self.assertEqual(completed.returncode, 0, completed.stderr)

    def test_validates_both_sides_of_a_rename(self) -> None:
        result = self.valid_result()
        result["patch_gzip_base64"] = encoded_patch(RENAME_PATCH)
        completed, _, _, temporary_directory = self.run_collector(
            result, allowed_paths=["new name.txt"]
        )
        self.addCleanup(temporary_directory.cleanup)

        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("outside the allowed set: old name.txt", completed.stderr)

    def test_rejects_git_metadata_path(self) -> None:
        unsafe_patch = PATCH.replace("example.txt", ".git/config")
        result = self.valid_result()
        result["patch_gzip_base64"] = encoded_patch(unsafe_patch)
        completed, _, _, temporary_directory = self.run_collector(result)
        self.addCleanup(temporary_directory.cleanup)

        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("Unsafe Git diff path", completed.stderr)

    def test_rejects_invalid_compressed_patch(self) -> None:
        result = self.valid_result()
        result["patch_gzip_base64"] = "not-base64"
        completed, _, _, temporary_directory = self.run_collector(result)
        self.addCleanup(temporary_directory.cleanup)

        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("gzip/base64", completed.stderr)

    def test_rejects_decompression_bomb_before_materialising_it(self) -> None:
        result = self.valid_result()
        oversized_patch = "diff --git a/a b/a\n" + ("x" * (5 * 1024 * 1024))
        result["patch_gzip_base64"] = encoded_patch(oversized_patch)
        completed, _, _, temporary_directory = self.run_collector(result)
        self.addCleanup(temporary_directory.cleanup)

        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("5 MiB safety limit", completed.stderr)

    def test_allows_explicit_no_change_result_when_requested(self) -> None:
        result = {
            "has_changes": False,
            "patch_gzip_base64": "",
            "summary": "No change was required.",
            "testing": "Reviewed the feedback.",
        }
        completed, output_dir, _, temporary_directory = self.run_collector(
            result, require_changes=False
        )
        self.addCleanup(temporary_directory.cleanup)

        self.assertEqual(completed.returncode, 0, completed.stderr)
        self.assertFalse((output_dir / "changes.patch").exists())


if __name__ == "__main__":
    unittest.main()
