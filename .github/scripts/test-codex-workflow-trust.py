#!/usr/bin/env python3
"""Mutation tests for the declarative workflow trust contract."""
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CHECKER = ROOT / ".github/scripts/check-codex-workflow-trust.rb"


class WorkflowTrustTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        shutil.copytree(ROOT / ".github/workflows", self.root / ".github/workflows")

    def check(self, expected=True):
        result = subprocess.run(
            ["ruby", str(CHECKER), str(self.root)],
            capture_output=True, text=True, check=False,
        )
        self.assertEqual(result.returncode == 0, expected, result.stdout + result.stderr)

    def replace(self, filename, before, after):
        path = self.root / ".github/workflows" / filename
        source = path.read_text()
        self.assertIn(before, source)
        path.write_text(source.replace(before, after, 1))

    def test_current_contract(self):
        self.check()

    def test_feature_branch_guard_is_rejected(self):
        self.replace("codex_jira_dispatch.yml", "github.ref == format(", "github.ref != format(")
        self.check(False)

    def test_alternate_workflow_ref_is_rejected(self):
        self.replace("codex_runner_smoke.yml", "@refs/heads/{1}", "@refs/tags/{1}")
        self.check(False)

    def test_pr_review_trigger_is_rejected(self):
        for event in ("pull_request_review", "pull_request_review_comment", "pull_request_target"):
            with self.subTest(event=event):
                path = self.root / ".github/workflows/codex_pr_review_feedback.yml"
                original = path.read_text()
                self.replace(path.name, "  issue_comment:", f"  {event}:")
                self.check(False)
                path.write_text(original)

    def test_missing_protected_environment_is_rejected(self):
        self.replace("codex_jira_dispatch.yml", "    environment: codex-model\n", "")
        self.check(False)

    def test_publisher_environment_on_model_job_is_rejected(self):
        self.replace("codex_jira_dispatch.yml", "environment: codex-model", "environment: codex-publisher")
        self.check(False)

    def test_unknown_secret_is_rejected(self):
        self.replace("codex_jira_dispatch.yml", "secrets.CODEX_OPENAI_API_KEY", "secrets.UNRELATED_CREDENTIAL")
        self.check(False)

    def test_dynamic_secret_is_rejected(self):
        self.replace("codex_jira_dispatch.yml", "secrets.CODEX_OPENAI_API_KEY", "secrets[inputs.credential]")
        self.check(False)

    def test_post_model_execution_is_rejected(self):
        self.replace("codex_runner_smoke.yml", "\n  verify-auth-response:", "\n      - run: git status\n\n  verify-auth-response:")
        self.check(False)

    def test_publisher_on_arc_is_rejected(self):
        self.replace("codex_runner_smoke.yml", "    needs: verify-auth-response\n    runs-on: ubuntu-latest", "    needs: verify-auth-response\n    runs-on: codex-example-aks")
        self.check(False)

    def test_model_runner_group_cannot_be_omitted(self):
        self.replace("codex_jira_dispatch.yml", "      group: appreg-codex\n", "")
        self.check(False)

    def test_dispatch_sha_checkout_is_rejected(self):
        self.replace("codex_runner_smoke.yml", "github.workflow_sha", "github.sha")
        self.check(False)

    def test_implicit_write_token_is_rejected(self):
        self.replace("codex_runner_smoke.yml", "\npermissions: {}\n", "\npermissions: write-all\n")
        self.check(False)


if __name__ == "__main__":
    unittest.main()
