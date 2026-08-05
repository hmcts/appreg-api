#!/usr/bin/env python3
"""Regression tests for exporting Codex changes without writing the real Git metadata."""

from __future__ import annotations

import base64
import gzip
import hashlib
import json
import os
import stat
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("codex-patch-export.sh")
COLLECTOR = Path(__file__).with_name("collect-codex-patch-result.py")


def run(command: list[str], *, cwd: Path, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, check=check, capture_output=True, text=True)


def git(repo: Path, *args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return run(["git", *args], cwd=repo, check=check)


def initialise_repository(path: Path) -> None:
    path.mkdir()
    git(path, "init", "--quiet")
    git(path, "config", "user.name", "Codex Test")
    git(path, "config", "user.email", "codex-test@example.invalid")


def git_metadata_snapshot(git_dir: Path) -> tuple[str, dict[str, str]]:
    index_hash = hashlib.sha256((git_dir / "index").read_bytes()).hexdigest()
    objects = {
        str(path.relative_to(git_dir)): hashlib.sha256(path.read_bytes()).hexdigest()
        for path in (git_dir / "objects").rglob("*")
        if path.is_file()
    }
    return index_hash, objects


def make_tree_read_only(root: Path) -> list[tuple[Path, int]]:
    modes: list[tuple[Path, int]] = []
    for path in [root, *root.rglob("*")]:
        if path.is_symlink():
            continue
        mode = stat.S_IMODE(path.stat().st_mode)
        modes.append((path, mode))
        path.chmod(mode & ~0o222)
    return modes


def restore_modes(modes: list[tuple[Path, int]]) -> None:
    for path, mode in sorted(modes, key=lambda item: len(item[0].parts)):
        path.chmod(mode)


def decode_export(completed: subprocess.CompletedProcess[str]) -> tuple[dict[str, object], bytes]:
    result = json.loads(completed.stdout)
    encoded = result["patch_gzip_base64"]
    patch = gzip.decompress(base64.b64decode(encoded, validate=True)) if encoded else b""
    return result, patch


class CodexPatchExportTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary_directory.cleanup)
        self.root = Path(self.temporary_directory.name)

    def export(
        self, repo: Path, *, paths_file: Path | None = None, check: bool = True
    ) -> subprocess.CompletedProcess[str]:
        command = ["bash", str(SCRIPT)]
        if paths_file is not None:
            command.extend(["--paths-file", str(paths_file)])
        return run(command, cwd=repo, check=check)

    def test_full_export_and_collector_handoff_with_read_only_git_metadata(self) -> None:
        source = self.root / "source"
        target = self.root / "target"
        initialise_repository(source)
        (source / "tracked.txt").write_text("old\n", encoding="utf-8")
        (source / "deleted.txt").write_text("delete me\n", encoding="utf-8")
        (source / "binary.bin").write_bytes(b"\x00old\xff")
        (source / "tool.sh").write_text("#!/bin/sh\necho old\n", encoding="utf-8")
        git(source, "add", "-A")
        git(source, "commit", "--quiet", "-m", "base")
        git(self.root, "clone", "--quiet", str(source), str(target))

        marker = self.root / "poisoned-git-config-ran"
        poison = self.root / "poison.sh"
        poison.write_text(f"#!/bin/sh\ntouch {marker}\nexit 1\n", encoding="utf-8")
        poison.chmod(0o755)
        git(source, "config", "core.fsmonitor", str(poison))
        git(source, "config", "diff.external", str(poison))

        (source / "tracked.txt").write_text("new\n", encoding="utf-8")
        (source / "deleted.txt").unlink()
        (source / "binary.bin").write_bytes(b"\x00new binary content\xfe")
        (source / "new path.txt").write_text("new file\n", encoding="utf-8")
        (source / "untracked.bin").write_bytes(b"\x00\x01new\xff")
        (source / "tool.sh").chmod(0o755)

        git_dir = source / ".git"
        before = git_metadata_snapshot(git_dir)
        modes = make_tree_read_only(git_dir)
        try:
            completed = self.export(source)
        finally:
            restore_modes(modes)

        result, patch = decode_export(completed)
        self.assertTrue(result["has_changes"])
        self.assertLessEqual(len(result["patch_gzip_base64"]), 60_000)
        self.assertIn(b"GIT binary patch", patch)
        self.assertEqual(git_metadata_snapshot(git_dir), before)
        self.assertFalse(marker.exists())

        patch_path = self.root / "changes.patch"
        patch_path.write_bytes(patch)
        git(target, "apply", "--check", "--binary", str(patch_path))
        git(target, "apply", "--binary", str(patch_path))
        self.assertEqual((target / "tracked.txt").read_text(encoding="utf-8"), "new\n")
        self.assertFalse((target / "deleted.txt").exists())
        self.assertEqual((target / "binary.bin").read_bytes(), b"\x00new binary content\xfe")
        self.assertEqual((target / "new path.txt").read_text(encoding="utf-8"), "new file\n")
        self.assertEqual((target / "untracked.bin").read_bytes(), b"\x00\x01new\xff")
        self.assertTrue((target / "tool.sh").stat().st_mode & stat.S_IXUSR)

        output_dir = self.root / "collector-output"
        collector_result = {
            **result,
            "summary": "Exported a complete patch.",
            "testing": "Applied it to a fresh clone.",
        }
        environment = {
            **os.environ,
            "CODEX_RESULT": json.dumps(collector_result),
            "OUTPUT_DIR": str(output_dir),
            "REQUIRE_CHANGES": "true",
        }
        collected = subprocess.run(
            [sys.executable, str(COLLECTOR)],
            cwd=self.root,
            env=environment,
            capture_output=True,
            text=True,
        )
        self.assertEqual(collected.returncode, 0, collected.stderr)
        self.assertEqual((output_dir / "changes.patch").read_bytes(), patch)

    def test_conflict_scoped_export_uses_head_without_writing_unmerged_index(self) -> None:
        source = self.root / "conflict-source"
        target = self.root / "conflict-target"
        initialise_repository(source)
        (source / "conflict.txt").write_text("common\n", encoding="utf-8")
        (source / "unrelated.txt").write_text("unchanged\n", encoding="utf-8")
        git(source, "add", "-A")
        git(source, "commit", "--quiet", "-m", "base")
        base_branch = git(source, "branch", "--show-current").stdout.strip()

        git(source, "checkout", "--quiet", "-b", "feature")
        (source / "conflict.txt").write_text("feature\n", encoding="utf-8")
        git(source, "commit", "--quiet", "-am", "feature")
        feature_sha = git(source, "rev-parse", "HEAD").stdout.strip()

        git(source, "checkout", "--quiet", base_branch)
        (source / "conflict.txt").write_text("master\n", encoding="utf-8")
        git(source, "commit", "--quiet", "-am", "master")
        git(source, "checkout", "--quiet", "feature")
        merge = git(source, "merge", base_branch, check=False)
        self.assertNotEqual(merge.returncode, 0)

        (source / "conflict.txt").write_text("resolved\n", encoding="utf-8")
        (source / "unrelated.txt").write_text("must not be exported\n", encoding="utf-8")
        paths_file = self.root / "conflicted-files.txt"
        paths_file.write_text("conflict.txt\n", encoding="utf-8")

        git_dir = source / ".git"
        before = git_metadata_snapshot(git_dir)
        modes = make_tree_read_only(git_dir)
        try:
            completed = self.export(source, paths_file=paths_file)
        finally:
            restore_modes(modes)

        result, patch = decode_export(completed)
        self.assertTrue(result["has_changes"])
        self.assertIn(b"conflict.txt", patch)
        self.assertNotIn(b"unrelated.txt", patch)
        self.assertEqual(git_metadata_snapshot(git_dir), before)
        self.assertTrue(git(source, "ls-files", "-u").stdout.strip())

        git(self.root, "clone", "--quiet", str(source), str(target))
        git(target, "checkout", "--quiet", feature_sha)
        patch_path = self.root / "conflict.patch"
        patch_path.write_bytes(patch)
        git(target, "apply", "--check", "--binary", str(patch_path))
        git(target, "apply", "--binary", str(patch_path))
        self.assertEqual((target / "conflict.txt").read_text(encoding="utf-8"), "resolved\n")
        self.assertEqual((target / "unrelated.txt").read_text(encoding="utf-8"), "unchanged\n")

    def test_scoped_export_treats_metacharacter_path_as_literal(self) -> None:
        source = self.root / "literal-source"
        target = self.root / "literal-target"
        initialise_repository(source)
        (source / "literal[1].txt").write_text("allowed old\n", encoding="utf-8")
        (source / "literal1.txt").write_text("decoy old\n", encoding="utf-8")
        git(source, "add", "-A")
        git(source, "commit", "--quiet", "-m", "base")
        git(self.root, "clone", "--quiet", str(source), str(target))

        (source / "literal[1].txt").write_text("allowed new\n", encoding="utf-8")
        (source / "literal1.txt").write_text("decoy new\n", encoding="utf-8")
        paths_file = self.root / "literal-path.txt"
        paths_file.write_text("literal[1].txt\n", encoding="utf-8")

        git_dir = source / ".git"
        before = git_metadata_snapshot(git_dir)
        modes = make_tree_read_only(git_dir)
        try:
            completed = self.export(source, paths_file=paths_file)
        finally:
            restore_modes(modes)

        result, patch = decode_export(completed)
        self.assertTrue(result["has_changes"])
        self.assertIn(b"literal[1].txt", patch)
        self.assertNotIn(b"literal1.txt", patch)
        self.assertEqual(git_metadata_snapshot(git_dir), before)

        patch_path = self.root / "literal.patch"
        patch_path.write_bytes(patch)
        git(target, "apply", "--check", "--binary", str(patch_path))
        git(target, "apply", "--binary", str(patch_path))
        self.assertEqual((target / "literal[1].txt").read_text(encoding="utf-8"), "allowed new\n")
        self.assertEqual((target / "literal1.txt").read_text(encoding="utf-8"), "decoy old\n")

    def test_no_change_export(self) -> None:
        source = self.root / "clean"
        initialise_repository(source)
        (source / "file.txt").write_text("same\n", encoding="utf-8")
        git(source, "add", "-A")
        git(source, "commit", "--quiet", "-m", "base")

        result, patch = decode_export(self.export(source))
        self.assertEqual(result, {"has_changes": False, "patch_gzip_base64": ""})
        self.assertEqual(patch, b"")

    def test_rejects_oversized_encoded_patch(self) -> None:
        source = self.root / "oversized"
        initialise_repository(source)
        git(source, "commit", "--quiet", "--allow-empty", "-m", "base")
        (source / "large.bin").write_bytes(os.urandom(80_000))

        completed = self.export(source, check=False)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("exceeds 60000", completed.stderr)
        self.assertEqual(completed.stdout, "")


if __name__ == "__main__":
    unittest.main()
