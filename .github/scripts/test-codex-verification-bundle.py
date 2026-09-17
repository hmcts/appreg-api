#!/usr/bin/env python3
"""Run trusted checks against pre-rollout candidates without changing their files."""
import json
import os
import shlex
import shutil
import subprocess
import tarfile
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PARTS = (".github/scripts", ".github/workflows", ".github/schemas", "bin")


class VerificationBundleTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.repo = self.root / "candidate"
        self.repo.mkdir()
        self.trusted = self.root / "trusted"
        for part in PARTS:
            shutil.copytree(ROOT / part, self.trusted / part, ignore=shutil.ignore_patterns("__pycache__"))
        self.archive = self.root / "trusted-codex-checks.tar"
        with tarfile.open(self.archive, "w") as archive:
            for part in PARTS:
                archive.add(self.trusted / part, arcname=part)
        self.pipeline = self.trusted / "bin/codex-local-pipeline.sh"
        # The candidate predates rollout and has no trust checker or trust workflow.
        self.originals = {
            "example.txt": "original\n",
            ".github/workflows/codex_pr_review_feedback.yml": "name: pre-rollout\non: pull_request_review\n",
            ".github/scripts/legacy.py": "# pre-rollout tooling\n",
            "bin/codex-local-pipeline.sh": "#!/bin/sh\nexit 92\n",
            "flyway/migrations/V1__example.sql": "-- fixture\n",
            "gradlew": "#!/bin/sh\nexit 0\n",
            ".yarn/releases/yarn-4.10.3.cjs": "// formatter fixture\n",
        }
        for name, value in self.originals.items():
            path = self.repo / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(value)
        (self.repo / "gradlew").chmod(0o755)
        self.git("init", "--quiet", "--initial-branch=master")
        self.git("config", "user.name", "Test")
        self.git("config", "user.email", "test@example.invalid")
        self.git("add", ".")
        self.git("commit", "--quiet", "-m", "pre-rollout fixture")
        self.head = self.git("rev-parse", "HEAD").strip()
        self.base = self.head
        self.git("update-ref", "refs/remotes/origin/master", self.base)
        self.git("update-ref", "refs/remotes/origin/codex/example", self.head)
        self.output = self.root / "output"
        self.output.mkdir()
        self.environment = {
            "PATH": os.environ["PATH"], "HOME": str(self.root / "home"),
            "RUNNER_TEMP": str(self.root / "temp"), "OUTPUT_DIR": str(self.output),
            "EXPECTED_PR_NUMBER": "42", "EXPECTED_HEAD_REF": "codex/example",
            "EXPECTED_BASE_REF": "master", "EXPECTED_HEAD_SHA": self.head,
            "EXPECTED_BASE_SHA": self.base, "LOCAL_PIPELINE_MODE": "checks-only",
            "TRUSTED_PIPELINE_PATH": str(self.pipeline), "TRUSTED_CHECKS_ARCHIVE": str(self.archive),
        }

    def git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.repo), *args], text=True, stderr=subprocess.PIPE)

    def run_check(self, command, environment=None):
        result = subprocess.run(command, cwd=self.repo, env=environment or self.environment,
                                capture_output=True, text=True, timeout=180)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        return result

    def assert_candidate_untouched(self):
        for name, value in self.originals.items():
            if name != "example.txt":
                self.assertEqual((self.repo / name).read_text(), value)
        self.assertFalse((self.repo / ".github/scripts/check-codex-workflow-trust.rb").exists())
        self.assertFalse((self.repo / ".github/workflows/codex_trust_checks.yml").exists())

    def metadata(self):
        (self.output / "metadata.env").write_text(
            f"has_changes=true\npr_number=42\nhead_ref=codex/example\nbase_ref=master\n"
            f"head_sha={self.head}\nbase_sha={self.base}\n"
        )
        (self.output / "codex-review-comment.md").write_text("Fixture review.\n")

    def make_patch(self):
        (self.repo / "example.txt").write_text("resolved\n")
        patch = self.git("diff", "--binary", "HEAD", "--", "example.txt")
        (self.output / "changes.patch").write_text(patch)
        self.git("restore", "example.txt")
        return patch

    def test_current_wrapper_accepts_pre_rollout_candidate(self):
        result = self.run_check(["bash", str(self.pipeline), "checks-only", "--no-fetch"],
                                {**self.environment, "CODEX_TRUST_ROOT": str(self.trusted)})
        self.assertIn("Codex workflow trust contracts passed", result.stdout)
        self.assertIn("Local pipeline checks completed", result.stdout)
        self.assertEqual(self.git("status", "--porcelain"), "")
        self.assert_candidate_untouched()

    def test_local_checks_without_ruby_still_run_the_python_audit(self):
        no_ruby = self.root / "no-ruby"
        no_ruby.mkdir()
        for directory in os.get_exec_path():
            path = Path(directory)
            if not path.is_dir():
                continue
            for executable in path.iterdir():
                destination = no_ruby / executable.name
                if executable.name == "ruby" or destination.exists() or destination.is_symlink():
                    continue
                if executable.is_file() and os.access(executable, os.X_OK):
                    destination.symlink_to(executable.resolve())
        self.assertIsNone(shutil.which("ruby", path=str(no_ruby)))
        result = self.run_check(
            [str(no_ruby / "bash"), str(self.pipeline), "checks-only", "--no-fetch"],
            {**self.environment, "PATH": str(no_ruby), "CODEX_TRUST_ROOT": str(self.trusted)},
        )
        self.assertIn("mandatory hosted Codex Trust Checks", result.stderr)
        self.assertIn("Validating Codex trust settings audit", result.stdout)
        self.assertIn("Local pipeline checks completed", result.stdout)
        self.assert_candidate_untouched()

    def verify_review(self, attempt):
        self.metadata()
        patch = self.make_patch()
        self.run_check(["bash", str(ROOT / ".github/scripts/codex-pr-review-verify.sh")],
                       {**self.environment, "GITHUB_RUN_ATTEMPT": str(attempt)})
        self.assertEqual((self.output / "changes.patch").read_text(), patch)
        self.assertEqual((self.repo / "example.txt").read_text(), "resolved\n")
        self.assertEqual(self.git("diff", "--cached", "--name-only").strip(), "example.txt")
        self.assert_candidate_untouched()
        self.assertIn("guardrail_review_required=false", (self.output / "verification.env").read_text())

    def test_initial_review_verifier_preserves_the_generated_patch(self):
        self.verify_review(1)

    def test_repaired_review_verifier_preserves_the_generated_patch(self):
        self.verify_review(2)

    def test_missing_trusted_bundle_fails_closed(self):
        self.metadata()
        self.make_patch()
        result = subprocess.run(
            ["bash", str(ROOT / ".github/scripts/codex-pr-review-verify.sh")], cwd=self.repo,
            env={**self.environment, "TRUSTED_CHECKS_ARCHIVE": str(self.root / "missing.tar")},
            capture_output=True, text=True,
        )
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.output / "verification.env").exists())
        self.assertEqual(self.git("status", "--porcelain"), "")

    def verify_conflict(self, tamper=False):
        if tamper:
            archive = self.root / "temp/codex-conflict-verify-manual-1/trusted-checks.tar"
            (self.repo / "gradlew").write_text(f"#!/bin/sh\nprintf corrupt > {shlex.quote(str(archive))}\n")
            (self.repo / ".yarn/releases/yarn-4.10.3.cjs").write_text(
                f"require('fs').writeFileSync({json.dumps(str(archive))}, 'corrupt');\n"
            )
            self.git("add", ".")
            self.git("commit", "--quiet", "-m", "candidate formatter fixture")
            self.head = self.git("rev-parse", "HEAD").strip()
        (self.repo / "example.txt").write_text("base change\n")
        self.git("commit", "--quiet", "-am", "base change")
        self.base = self.git("rev-parse", "HEAD").strip()
        self.git("update-ref", "refs/remotes/origin/master", self.base)
        self.git("checkout", "--quiet", "-B", "codex/example", self.head)
        (self.repo / "example.txt").write_text("head change\n")
        self.git("commit", "--quiet", "-am", "head change")
        self.head = self.git("rev-parse", "HEAD").strip()
        self.metadata()
        self.make_patch()
        (self.output / "conflicted-files.txt").write_text("example.txt\n")
        formatter = self.repo / "node_modules/.bin/prettier"
        formatter.parent.mkdir(parents=True)
        formatter.write_text("#!/bin/sh\nexit 0\n")
        formatter.chmod(0o755)
        command = ["bash", str(ROOT / ".github/scripts/codex-merge-conflict-verify.sh")]
        environment = {**self.environment, "EXPECTED_HEAD_SHA": self.head, "EXPECTED_BASE_SHA": self.base}
        if tamper:
            result = subprocess.run(command, cwd=self.repo, env=environment,
                                    capture_output=True, text=True, timeout=30)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("Trusted check bundle changed", result.stderr)
            self.assertFalse((self.output / "verification.env").exists())
            return
        self.run_check(command, environment)
        self.assertEqual((self.repo / "example.txt").read_text(), "resolved\n")
        self.assertEqual(self.git("diff", "--cached", "--name-only").strip(), "example.txt")
        self.assert_candidate_untouched()
        self.assertTrue((self.output / "verification.env").is_file())

    def test_conflict_verifier_uses_the_same_trusted_bundle(self):
        self.verify_conflict()

    def test_candidate_formatter_cannot_modify_the_captured_bundle(self):
        self.verify_conflict(tamper=True)


if __name__ == "__main__":
    unittest.main()
