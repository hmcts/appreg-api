#!/usr/bin/env python3
"""Regression tests for exact-revision Codex publishers."""

from __future__ import annotations

import hashlib
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
JIRA_PUBLISHER = SCRIPT_DIR / "codex-jira-publish.sh"
REVIEW_PUBLISHER = SCRIPT_DIR / "codex-pr-review-publish.sh"
BASE_SHA = "a" * 40
HEAD_SHA = "b" * 40
NEW_SHA = "c" * 40
MOVED_SHA = "d" * 40


class PublisherRevisionTest(unittest.TestCase):
    def make_fake_tools(self, root: Path, *, remote_base: str, remote_head: str) -> Path:
        fake_bin = root / "bin"
        fake_bin.mkdir()
        fake_git = fake_bin / "git"
        fake_git.write_text(
            f"""#!/usr/bin/env bash
set -euo pipefail
args="$*"
if [[ "$args" == *"ls-remote"* ]]; then
  if [[ "$args" == *"refs/heads/master"* ]]; then
    printf '%s\\trefs/heads/master\\n' {remote_base!r}
  elif [[ "$args" == *"refs/heads/codex/example"* ]]; then
    printf '%s\\trefs/heads/codex/example\\n' {remote_head!r}
  fi
elif [[ "$args" == *"rev-parse HEAD"* ]]; then
  printf '%s\\n' {NEW_SHA!r}
fi
""",
            encoding="utf-8",
        )
        fake_git.chmod(0o755)

        fake_gh = fake_bin / "gh"
        fake_gh.write_text(
            """#!/usr/bin/env bash
set -euo pipefail
case "$*" in
  *"pr list"*) echo "https://example.invalid/pull/42" ;;
  *"pr view"*) echo "42" ;;
esac
""",
            encoding="utf-8",
        )
        fake_gh.chmod(0o755)
        return fake_bin

    @staticmethod
    def write_patch_artifacts(root: Path, *, review: bool) -> tuple[Path, Path]:
        output = root / "output"
        verified = root / "verified"
        output.mkdir()
        verified.mkdir()
        patch = b"not-a-real-patch-but-git-is-stubbed\n"
        (output / "changes.patch").write_bytes(patch)
        (verified / "changes.patch").write_bytes(patch)
        patch_sha = hashlib.sha256(patch).hexdigest()
        if review:
            metadata = (
                "has_changes=true\n"
                "pr_number=42\n"
                "head_ref=codex/example\n"
                "base_ref=master\n"
                f"head_sha={HEAD_SHA}\n"
                f"base_sha={BASE_SHA}\n"
                "comment_author=reviewer\n"
                "comment_url=https://example.invalid/comment/1\n"
            )
            verification = (
                "has_changes=true\n"
                "pr_number=42\n"
                "head_ref=codex/example\n"
                "base_ref=master\n"
                f"head_sha={HEAD_SHA}\n"
                f"base_sha={BASE_SHA}\n"
                f"patch_sha={patch_sha}\n"
            )
            (output / "codex-final-message.md").write_text("Updated the code.", encoding="utf-8")
        else:
            metadata = "branch_name=codex/example\n"
            verification = (
                "branch_name=codex/example\n"
                f"base_sha={BASE_SHA}\n"
                f"patch_sha={patch_sha}\n"
            )
            (verified / "codex-pr-body.md").write_text("PR body", encoding="utf-8")
        (output / "metadata.env").write_text(metadata, encoding="utf-8")
        (verified / "verification.env").write_text(verification, encoding="utf-8")
        return output, verified

    def run_review(self, remote_head: str) -> subprocess.CompletedProcess[str]:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            output, verified = self.write_patch_artifacts(root, review=True)
            fake_bin = self.make_fake_tools(root, remote_base=BASE_SHA, remote_head=remote_head)
            return subprocess.run(
                ["bash", str(REVIEW_PUBLISHER)],
                cwd=SCRIPT_DIR.parent.parent,
                env={
                    **os.environ,
                    "PATH": f"{fake_bin}:{os.environ['PATH']}",
                    "GH_TOKEN": "test-token",
                    "GITHUB_REPOSITORY": "hmcts/example",
                    "OUTPUT_DIR": str(output),
                    "VERIFICATION_DIR": str(verified),
                    "EXPECTED_PR_NUMBER": "42",
                    "EXPECTED_HEAD_REF": "codex/example",
                    "EXPECTED_HEAD_SHA": HEAD_SHA,
                    "RUNNER_TEMP": str(root / "runner"),
                },
                capture_output=True,
                text=True,
            )

    def run_jira(self, remote_base: str) -> subprocess.CompletedProcess[str]:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            output, verified = self.write_patch_artifacts(root, review=False)
            fake_bin = self.make_fake_tools(root, remote_base=remote_base, remote_head="")
            return subprocess.run(
                ["bash", str(JIRA_PUBLISHER)],
                cwd=SCRIPT_DIR.parent.parent,
                env={
                    **os.environ,
                    "PATH": f"{fake_bin}:{os.environ['PATH']}",
                    "GH_TOKEN": "test-token",
                    "GITHUB_REPOSITORY": "hmcts/example",
                    "GITHUB_ACTOR": "tester",
                    "ISSUE_KEY": "ARCPOC-1",
                    "ISSUE_SUMMARY": "Example",
                    "ISSUE_URL": "https://example.invalid/browse/ARCPOC-1",
                    "OUTPUT_DIR": str(output),
                    "VERIFICATION_DIR": str(verified),
                    "EXPECTED_BRANCH_NAME": "codex/example",
                    "EXPECTED_BASE_SHA": BASE_SHA,
                    "DEFAULT_BRANCH": "master",
                    "RUNNER_TEMP": str(root / "runner"),
                },
                capture_output=True,
                text=True,
            )

    def test_review_publisher_accepts_exact_verified_head(self) -> None:
        completed = self.run_review(HEAD_SHA)
        self.assertEqual(completed.returncode, 0, completed.stderr)

    def test_review_publisher_rejects_moved_head(self) -> None:
        completed = self.run_review(MOVED_SHA)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("PR branch moved", completed.stderr)

    def test_jira_publisher_accepts_exact_verified_base(self) -> None:
        completed = self.run_jira(BASE_SHA)
        self.assertEqual(completed.returncode, 0, completed.stderr)

    def test_jira_publisher_rejects_moved_base(self) -> None:
        completed = self.run_jira(MOVED_SHA)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("Source revision moved", completed.stderr)


if __name__ == "__main__":
    unittest.main()
