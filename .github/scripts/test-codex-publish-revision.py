#!/usr/bin/env python3
"""Regression tests for exact-revision Codex publishers."""

from __future__ import annotations

import hashlib
import os
import subprocess
import tempfile
import unittest
from pathlib import Path
from typing import Sequence

SCRIPT_DIR = Path(__file__).parent
JIRA_PUBLISHER = SCRIPT_DIR / "codex-jira-publish.sh"
REVIEW_PUBLISHER = SCRIPT_DIR / "codex-pr-review-publish.sh"
CONFLICT_PUBLISHER = SCRIPT_DIR / "codex-merge-conflict-publish.sh"
BASE_SHA = "a" * 40
HEAD_SHA = "b" * 40
NEW_SHA = "c" * 40
MOVED_SHA = "d" * 40


class PublisherRevisionTest(unittest.TestCase):
    def make_fake_tools(
        self,
        root: Path,
        *,
        remote_base: str | Sequence[str],
        remote_head: str | Sequence[str],
    ) -> tuple[Path, Path]:
        fake_bin = root / "bin"
        fake_bin.mkdir()
        command_log = root / "git-commands.log"
        conflict_counter = root / "conflict-counter"
        base_sequence = [remote_base] if isinstance(remote_base, str) else list(remote_base)
        head_sequence = [remote_head] if isinstance(remote_head, str) else list(remote_head)
        base_responses = root / "base-responses"
        head_responses = root / "head-responses"
        base_counter = root / "base-counter"
        head_counter = root / "head-counter"
        base_responses.write_text("\n".join(base_sequence) + "\n", encoding="utf-8")
        head_responses.write_text("\n".join(head_sequence) + "\n", encoding="utf-8")
        fake_git = fake_bin / "git"
        fake_git.write_text(
            f"""#!/usr/bin/env bash
set -euo pipefail
args="$*"
printf '%s\\n' "$args" >>{str(command_log)!r}
next_response() {{
  local responses_path="$1"
  local counter_path="$2"
  local count=0
  if [[ -f "$counter_path" ]]; then
    count="$(cat "$counter_path")"
  fi
  count=$((count + 1))
  echo "$count" >"$counter_path"
  response="$(sed -n "${{count}}p" "$responses_path")"
  if [[ "$count" -gt "$(wc -l <"$responses_path")" ]]; then
    response="$(tail -n 1 "$responses_path")"
  fi
  printf '%s' "$response"
}}
if [[ "$args" == *"ls-remote"* ]]; then
  if [[ "$args" == *"refs/heads/master"* ]]; then
    response="$(next_response {str(base_responses)!r} {str(base_counter)!r})"
    if [[ -n "$response" ]]; then
      printf '%s\\trefs/heads/master\\n' "$response"
    fi
  elif [[ "$args" == *"refs/heads/codex/example"* ]]; then
    response="$(next_response {str(head_responses)!r} {str(head_counter)!r})"
    if [[ -n "$response" ]]; then
      printf '%s\\trefs/heads/codex/example\\n' "$response"
    fi
  fi
elif [[ "$args" == *"rev-parse refs/remotes/origin/codex/example"* ]]; then
  printf '%s\\n' {HEAD_SHA!r}
elif [[ "$args" == *"rev-parse refs/remotes/origin/master"* ]]; then
  printf '%s\\n' {BASE_SHA!r}
elif [[ "$args" == *"rev-parse HEAD"* ]]; then
  printf '%s\\n' {NEW_SHA!r}
elif [[ "$args" == *"merge --no-commit --no-ff"* ]]; then
  exit 1
elif [[ "$args" == *"diff --name-only --diff-filter=U"* ]]; then
  count=0
  if [[ -f {str(conflict_counter)!r} ]]; then
    count="$(cat {str(conflict_counter)!r})"
  fi
  if [[ "$count" -eq 0 ]]; then
    printf '%s\\n' "example.txt"
  fi
  echo $((count + 1)) >{str(conflict_counter)!r}
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
        return fake_bin, command_log

    @staticmethod
    def assert_no_push(commands: str) -> None:
        push_lines = [
            line
            for line in commands.splitlines()
            if line.startswith("push ") or " push " in line
        ]
        if push_lines:
            raise AssertionError(f"Unexpected Git push commands: {push_lines}")

    @staticmethod
    def assert_command_logged(commands: str, command: str) -> None:
        matching_lines = [
            line
            for line in commands.splitlines()
            if line.startswith(f"{command} ") or f" {command} " in line
        ]
        if not matching_lines:
            raise AssertionError(f"Git command was not logged: {command}")

    @staticmethod
    def run_real_git(
        cwd: Path,
        *args: str,
        check: bool = True,
    ) -> subprocess.CompletedProcess[str]:
        completed = subprocess.run(
            ["git", *args],
            cwd=cwd,
            env={
                **os.environ,
                "GIT_CONFIG_GLOBAL": "/dev/null",
                "GIT_CONFIG_NOSYSTEM": "1",
                "GIT_TERMINAL_PROMPT": "0",
            },
            capture_output=True,
            text=True,
        )
        if check and completed.returncode != 0:
            raise AssertionError(
                f"git {' '.join(args)} failed:\n{completed.stdout}\n{completed.stderr}"
            )
        return completed

    def make_bare_remote(self, root: Path) -> tuple[Path, Path, Path]:
        remote = root / "remote.git"
        publisher = root / "publisher"
        racer = root / "racer"
        self.run_real_git(root, "init", "--bare", str(remote))
        self.run_real_git(root, "init", str(publisher))
        self.run_real_git(publisher, "config", "user.name", "Publisher")
        self.run_real_git(publisher, "config", "user.email", "publisher@example.invalid")
        (publisher / "seed.txt").write_text("seed\n", encoding="utf-8")
        self.run_real_git(publisher, "add", "seed.txt")
        self.run_real_git(publisher, "commit", "-m", "Seed")
        self.run_real_git(publisher, "branch", "-M", "master")
        self.run_real_git(publisher, "remote", "add", "origin", str(remote))
        self.run_real_git(publisher, "push", "-u", "origin", "master")
        self.run_real_git(root, "clone", str(remote), str(racer))
        self.run_real_git(racer, "config", "user.name", "Racer")
        self.run_real_git(racer, "config", "user.email", "racer@example.invalid")
        return remote, publisher, racer

    def commit_real_file(
        self,
        repository: Path,
        path: str,
        content: str,
        message: str,
    ) -> str:
        target = repository / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
        self.run_real_git(repository, "add", path)
        self.run_real_git(repository, "commit", "-m", message)
        return self.run_real_git(repository, "rev-parse", "HEAD").stdout.strip()

    def remote_branch_sha(self, remote: Path, branch: str) -> str:
        return self.run_real_git(
            remote,
            "rev-parse",
            f"refs/heads/{branch}",
        ).stdout.strip()

    def assert_exact_lease_rejects_atomic_race(self, branch: str) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            remote, publisher, racer = self.make_bare_remote(Path(temporary_directory))
            self.run_real_git(publisher, "checkout", "-b", branch, "master")
            expected_sha = self.commit_real_file(
                publisher,
                "expected.txt",
                f"{branch} expected\n",
                "Create expected branch head",
            )
            self.run_real_git(publisher, "push", "-u", "origin", branch)
            self.commit_real_file(
                publisher,
                "publisher.txt",
                f"{branch} publisher\n",
                "Prepare publisher update",
            )

            self.run_real_git(racer, "fetch", "origin", branch)
            self.run_real_git(racer, "checkout", "-B", branch, f"origin/{branch}")
            moved_sha = self.commit_real_file(
                racer,
                "racer.txt",
                f"{branch} racer\n",
                "Move remote branch",
            )
            self.run_real_git(racer, "push", "origin", branch)

            completed = self.run_real_git(
                publisher,
                "push",
                f"--force-with-lease=refs/heads/{branch}:{expected_sha}",
                "origin",
                branch,
                check=False,
            )
            self.assertNotEqual(completed.returncode, 0)
            self.assertEqual(self.remote_branch_sha(remote, branch), moved_sha)

    def assert_empty_lease_rejects_atomic_race(self, branch: str) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            remote, publisher, racer = self.make_bare_remote(Path(temporary_directory))
            self.run_real_git(publisher, "checkout", "-b", branch, "master")
            self.commit_real_file(
                publisher,
                "publisher.txt",
                f"{branch} publisher\n",
                "Prepare initial publication",
            )

            self.run_real_git(racer, "checkout", "-b", branch, "origin/master")
            moved_sha = self.commit_real_file(
                racer,
                "racer.txt",
                f"{branch} racer\n",
                "Create remote branch during publication",
            )
            self.run_real_git(racer, "push", "origin", branch)

            completed = self.run_real_git(
                publisher,
                "push",
                f"--force-with-lease=refs/heads/{branch}:",
                "origin",
                branch,
                check=False,
            )
            self.assertNotEqual(completed.returncode, 0)
            self.assertEqual(self.remote_branch_sha(remote, branch), moved_sha)

    @staticmethod
    def write_patch_artifacts(root: Path, *, kind: str) -> tuple[Path, Path]:
        output = root / "output"
        verified = root / "verified"
        output.mkdir()
        verified.mkdir()
        patch = b"not-a-real-patch-but-git-is-stubbed\n"
        patch_sha = hashlib.sha256(patch).hexdigest()

        if kind == "jira":
            (output / "changes.patch").write_bytes(patch)
            (verified / "changes.patch").write_bytes(patch)
            (output / "metadata.env").write_text(
                "branch_name=codex/example\n",
                encoding="utf-8",
            )
            (verified / "verification.env").write_text(
                "branch_name=codex/example\n"
                f"base_sha={BASE_SHA}\n"
                f"patch_sha={patch_sha}\n",
                encoding="utf-8",
            )
            (verified / "codex-pr-body.md").write_text("PR body", encoding="utf-8")
        elif kind == "review":
            (output / "changes.patch").write_bytes(patch)
            (verified / "changes.patch").write_bytes(patch)
            (output / "metadata.env").write_text(
                "has_changes=true\n"
                "pr_number=42\n"
                "head_ref=codex/example\n"
                "base_ref=master\n"
                f"head_sha={HEAD_SHA}\n"
                f"base_sha={BASE_SHA}\n"
                "comment_author=reviewer\n"
                "comment_url=https://example.invalid/comment/1\n",
                encoding="utf-8",
            )
            (verified / "verification.env").write_text(
                "has_changes=true\n"
                "pr_number=42\n"
                "head_ref=codex/example\n"
                "base_ref=master\n"
                f"head_sha={HEAD_SHA}\n"
                f"base_sha={BASE_SHA}\n"
                f"patch_sha={patch_sha}\n",
                encoding="utf-8",
            )
            (output / "codex-final-message.md").write_text(
                "Updated the code.",
                encoding="utf-8",
            )
        elif kind == "conflict":
            (output / "metadata.env").write_text(
                "has_changes=true\n"
                "pr_number=42\n"
                "head_ref=codex/example\n"
                "base_ref=master\n"
                f"head_sha={HEAD_SHA}\n"
                f"base_sha={BASE_SHA}\n",
                encoding="utf-8",
            )
            (output / "conflicted-files.txt").write_text(
                "example.txt\n",
                encoding="utf-8",
            )
            (output / "codex-final-message.md").write_text(
                "Resolved the conflict.",
                encoding="utf-8",
            )
            (verified / "changes.patch").write_bytes(patch)
            (verified / "verification.env").write_text(
                "has_changes=true\n"
                "pr_number=42\n"
                "head_ref=codex/example\n"
                "base_ref=master\n"
                f"head_sha={HEAD_SHA}\n"
                f"base_sha={BASE_SHA}\n"
                f"patch_sha={patch_sha}\n",
                encoding="utf-8",
            )
        else:
            raise ValueError(f"Unsupported artifact kind: {kind}")

        return output, verified

    def run_review(self, remote_head: str) -> tuple[subprocess.CompletedProcess[str], str]:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            output, verified = self.write_patch_artifacts(root, kind="review")
            fake_bin, command_log = self.make_fake_tools(
                root,
                remote_base=BASE_SHA,
                remote_head=remote_head,
            )
            completed = subprocess.run(
                ["bash", str(REVIEW_PUBLISHER)],
                cwd=SCRIPT_DIR.parent.parent,
                env={
                    **os.environ,
                    "PATH": f"{fake_bin}:{os.environ['PATH']}",
                    "GH_TOKEN": "test-token",
                    "CODEX_PUBLISHER_LOGIN": "appreg-codex-bot",
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
            commands = command_log.read_text(encoding="utf-8") if command_log.exists() else ""
            return completed, commands

    def run_jira(
        self,
        *,
        mode: str,
        remote_base: str | Sequence[str] = BASE_SHA,
        remote_head: str | Sequence[str] = "",
    ) -> tuple[subprocess.CompletedProcess[str], str]:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            output, verified = self.write_patch_artifacts(root, kind="jira")
            fake_bin, command_log = self.make_fake_tools(
                root,
                remote_base=remote_base,
                remote_head=remote_head,
            )
            env = {
                **os.environ,
                "PATH": f"{fake_bin}:{os.environ['PATH']}",
                "GH_TOKEN": "test-token",
                "CODEX_PUBLISHER_LOGIN": "appreg-codex-bot",
                "GITHUB_REPOSITORY": "hmcts/example",
                "GITHUB_ACTOR": "tester",
                "ISSUE_KEY": "ARCPOC-1",
                "ISSUE_SUMMARY": "Example",
                "ISSUE_URL": "https://example.invalid/browse/ARCPOC-1",
                "OUTPUT_DIR": str(output),
                "VERIFICATION_DIR": str(verified),
                "EXPECTED_BRANCH_NAME": "codex/example",
                "EXPECTED_BASE_SHA": BASE_SHA,
                "JIRA_PUBLISH_MODE": mode,
                "DEFAULT_BRANCH": "master",
                "RUNNER_TEMP": str(root / "runner"),
            }
            if mode == "repair":
                env["EXPECTED_BRANCH_HEAD_SHA"] = HEAD_SHA
            completed = subprocess.run(
                ["bash", str(JIRA_PUBLISHER)],
                cwd=SCRIPT_DIR.parent.parent,
                env=env,
                capture_output=True,
                text=True,
            )
            commands = command_log.read_text(encoding="utf-8") if command_log.exists() else ""
            return completed, commands

    def run_conflict(
        self,
        *,
        remote_head: str | Sequence[str] = HEAD_SHA,
        remote_base: str | Sequence[str] = BASE_SHA,
    ) -> tuple[subprocess.CompletedProcess[str], str]:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            output, verified = self.write_patch_artifacts(root, kind="conflict")
            fake_bin, command_log = self.make_fake_tools(
                root,
                remote_base=remote_base,
                remote_head=remote_head,
            )
            completed = subprocess.run(
                ["bash", str(CONFLICT_PUBLISHER)],
                cwd=SCRIPT_DIR.parent.parent,
                env={
                    **os.environ,
                    "PATH": f"{fake_bin}:{os.environ['PATH']}",
                    "GH_TOKEN": "test-token",
                    "CODEX_PUBLISHER_LOGIN": "appreg-codex-bot",
                    "GITHUB_REPOSITORY": "hmcts/example",
                    "OUTPUT_DIR": str(output),
                    "VERIFICATION_DIR": str(verified),
                    "EXPECTED_PR_NUMBER": "42",
                    "EXPECTED_HEAD_REF": "codex/example",
                    "EXPECTED_BASE_REF": "master",
                    "EXPECTED_HEAD_SHA": HEAD_SHA,
                    "EXPECTED_BASE_SHA": BASE_SHA,
                    "RUNNER_TEMP": str(root / "runner"),
                },
                capture_output=True,
                text=True,
            )
            commands = command_log.read_text(encoding="utf-8") if command_log.exists() else ""
            return completed, commands

    def test_review_publisher_accepts_exact_verified_head(self) -> None:
        completed, _ = self.run_review(HEAD_SHA)
        self.assertEqual(completed.returncode, 0, completed.stderr)

    def test_publication_code_does_not_dispatch_pr_tasks(self) -> None:
        publication_paths = [JIRA_PUBLISHER, REVIEW_PUBLISHER, CONFLICT_PUBLISHER]
        publication_paths.extend(
            sorted((SCRIPT_DIR.parent / "workflows").glob("codex*.y*ml"))
        )
        forbidden_markers = (
            "workflow run on-pr.yml",
            "/actions/workflows/on-pr.yml/dispatches",
        )

        for path in publication_paths:
            source = path.read_text(encoding="utf-8")
            for marker in forbidden_markers:
                with self.subTest(path=path, marker=marker):
                    self.assertNotIn(marker, source)

    def test_review_publisher_rejects_moved_head(self) -> None:
        completed, _ = self.run_review(MOVED_SHA)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("PR branch moved", completed.stderr)

    def test_jira_initial_publish_requires_absent_branch(self) -> None:
        completed, commands = self.run_jira(mode="initial")
        self.assertEqual(completed.returncode, 0, completed.stderr)
        self.assertIn(
            "push --force-with-lease=refs/heads/codex/example: "
            "--set-upstream origin codex/example",
            commands,
        )

    def test_jira_initial_publish_rejects_existing_branch(self) -> None:
        completed, commands = self.run_jira(mode="initial", remote_head=HEAD_SHA)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("Initial publication requires", completed.stderr)
        self.assert_no_push(commands)

    def test_jira_publisher_rejects_moved_base(self) -> None:
        completed, _ = self.run_jira(mode="initial", remote_base=MOVED_SHA)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("Source revision moved", completed.stderr)

    def test_jira_repair_accepts_expected_head_and_uses_exact_lease(self) -> None:
        completed, commands = self.run_jira(mode="repair", remote_head=HEAD_SHA)
        self.assertEqual(completed.returncode, 0, completed.stderr)
        self.assertIn(
            f"push --force-with-lease=refs/heads/codex/example:{HEAD_SHA} "
            "--set-upstream origin codex/example",
            commands,
        )

    def test_jira_repair_rejects_intervening_commit(self) -> None:
        completed, commands = self.run_jira(
            mode="repair",
            remote_head=[HEAD_SHA, MOVED_SHA],
        )
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("moved while the repaired commit was being prepared", completed.stderr)
        self.assert_command_logged(commands, "commit")
        self.assert_no_push(commands)

    def test_conflict_publish_accepts_exact_refs_and_uses_exact_lease(self) -> None:
        completed, commands = self.run_conflict()
        self.assertEqual(completed.returncode, 0, completed.stderr)
        self.assertIn(
            f"push --force-with-lease=refs/heads/codex/example:{HEAD_SHA} "
            "origin codex/example",
            commands,
        )

    def test_conflict_publish_rejects_head_movement(self) -> None:
        completed, commands = self.run_conflict(remote_head=MOVED_SHA)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("moved before push", completed.stderr)
        self.assert_command_logged(commands, "commit")
        self.assert_no_push(commands)

    def test_conflict_publish_rejects_base_movement(self) -> None:
        completed, commands = self.run_conflict(remote_base=MOVED_SHA)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("moved before push", completed.stderr)
        self.assert_command_logged(commands, "commit")
        self.assert_no_push(commands)

    def test_initial_empty_lease_rejects_atomic_branch_creation(self) -> None:
        self.assert_empty_lease_rejects_atomic_race("codex/atomic-initial")

    def test_jira_repair_exact_lease_rejects_atomic_branch_movement(self) -> None:
        self.assert_exact_lease_rejects_atomic_race("codex/atomic-repair")

    def test_conflict_exact_lease_rejects_atomic_branch_movement(self) -> None:
        self.assert_exact_lease_rejects_atomic_race("codex/atomic-conflict")


if __name__ == "__main__":
    unittest.main()
