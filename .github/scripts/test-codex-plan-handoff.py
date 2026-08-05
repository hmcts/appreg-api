#!/usr/bin/env python3
"""Regression tests for the private validated-plan job-output hand-off."""

from __future__ import annotations

import base64
import gzip
import hashlib
import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

RUNTIME = Path(__file__).with_name("codex-action-runtime.sh")
COLLECTOR = Path(__file__).with_name("codex-jira-collect.sh")
PATCH = """diff --git a/example.txt b/example.txt
index 257cc56..5716ca5 100644
--- a/example.txt
+++ b/example.txt
@@ -1 +1 @@
-old
+new
"""
PLAN_DETAIL = "Sensitive Jira-derived planning detail must remain private."


class CodexPlanHandoffTest(unittest.TestCase):
    def make_plan(self) -> Path:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        plan_dir = Path(temp_dir.name)
        plan = {
            "ready_to_implement": True,
            "problem_analysis": PLAN_DETAIL,
            "root_cause": "The example is stale.",
            "scope_decision": "Update only the planned example file.",
            "risk_level": "low",
            "cross_system_change": False,
            "alternatives_considered": ["Leave the stale example unchanged."],
            "implementation_steps": [
                {
                    "path": "example.txt",
                    "change": "Update the example.",
                    "reason": "Keep the example current.",
                }
            ],
            "tests_required": ["Inspect the exported patch."],
            "acceptance_criteria": ["The example contains the new value."],
            "risks": [],
            "assumptions": [],
            "blockers": [],
        }
        plan_bytes = (json.dumps(plan, indent=2, sort_keys=True) + "\n").encode("utf-8")
        (plan_dir / "plan.json").write_bytes(plan_bytes)
        (plan_dir / "plan.sha256").write_text(
            f"{hashlib.sha256(plan_bytes).hexdigest()}\n", encoding="ascii"
        )
        (plan_dir / "allowed-paths.txt").write_text("example.txt\n", encoding="utf-8")
        return plan_dir

    def validate(self, plan_dir: Path) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                "bash",
                "-c",
                'source "$1"; validated_codex_plan_path "$2"',
                "codex-plan-test",
                str(RUNTIME),
                str(plan_dir),
            ],
            text=True,
            capture_output=True,
            check=False,
        )

    @staticmethod
    def codex_result(patch: str) -> str:
        encoded_patch = base64.b64encode(
            gzip.compress(patch.encode("utf-8"), mtime=0)
        ).decode("ascii")
        return json.dumps(
            {
                "has_changes": True,
                "patch_gzip_base64": encoded_patch,
                "summary": "Updated the shared validator.",
                "testing": "Added a focused test.",
            }
        )

    def run_collector(
        self, plan_dir: Path, operation: str, patch: str
    ) -> tuple[subprocess.CompletedProcess[str], Path]:
        output_dir = plan_dir / f"output-{operation}"
        input_dir = plan_dir / "input"
        input_dir.mkdir(exist_ok=True)
        (input_dir / "codex-pr-body.md").write_text(
            "### Planning audit\n\nPrivate plan content is omitted.\n", encoding="utf-8"
        )
        environment = {
            **os.environ,
            "CODEX_RESULT": self.codex_result(patch),
            "CODEX_OPERATION": operation,
            "OUTPUT_DIR": str(output_dir),
            "BRANCH_NAME": "codex/test-plan",
            "ISSUE_KEY": "ARCPOC-1",
            "ISSUE_SUMMARY": "Validate requests",
            "ISSUE_URL": "https://example.invalid/ARCPOC-1",
            "PLAN_DIR": str(plan_dir),
            "INPUT_DIR": str(input_dir),
            "REPAIR_ATTEMPT": "1",
        }
        completed = subprocess.run(
            ["bash", str(COLLECTOR)],
            cwd=plan_dir,
            env=environment,
            text=True,
            capture_output=True,
            check=False,
        )
        return completed, output_dir

    def test_accepts_matching_plan_hash_and_paths(self) -> None:
        plan_dir = self.make_plan()
        result = self.validate(plan_dir)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.strip(), str(plan_dir / "plan.json"))

    def test_rejects_plan_modified_after_validation(self) -> None:
        plan_dir = self.make_plan()
        (plan_dir / "plan.json").write_text('{"ready_to_implement":false}\n', encoding="utf-8")
        result = self.validate(plan_dir)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("does not match", result.stderr)

    def test_rejects_malformed_hash(self) -> None:
        plan_dir = self.make_plan()
        (plan_dir / "plan.sha256").write_text("not-a-hash\n", encoding="ascii")
        result = self.validate(plan_dir)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("malformed", result.stderr)

    def test_rejects_allowed_paths_that_do_not_match_plan(self) -> None:
        plan_dir = self.make_plan()
        (plan_dir / "allowed-paths.txt").write_text("outside.txt\n", encoding="utf-8")
        result = self.validate(plan_dir)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("allowed paths do not match", result.stderr)

    def test_generation_collector_omits_raw_plan_from_public_pr_body(self) -> None:
        plan_dir = self.make_plan()
        completed, output_dir = self.run_collector(plan_dir, "jira-generate", PATCH)
        self.assertEqual(completed.returncode, 0, completed.stderr)
        pr_body = (output_dir / "codex-pr-body.md").read_text(encoding="utf-8")
        self.assertNotIn("Codex Plan", pr_body)
        self.assertNotIn(PLAN_DETAIL, pr_body)
        self.assertNotIn("Updated the shared validator.", pr_body)
        self.assertIn("Validated plan SHA-256", pr_body)
        self.assertFalse((output_dir / "plan.json").exists())
        self.assertFalse((output_dir / "allowed-paths.txt").exists())
        self.assertFalse((output_dir / "codex-final-message.md").exists())

    def test_generation_rejects_patch_outside_planned_paths(self) -> None:
        plan_dir = self.make_plan()
        outside_patch = PATCH.replace("example.txt", "outside.txt")
        completed, _ = self.run_collector(plan_dir, "jira-generate", outside_patch)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("outside the allowed set: outside.txt", completed.stderr)

    def test_repair_rejects_patch_outside_planned_paths(self) -> None:
        plan_dir = self.make_plan()
        outside_patch = PATCH.replace("example.txt", "repair-outside.txt")
        completed, _ = self.run_collector(plan_dir, "jira-repair", outside_patch)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("outside the allowed set: repair-outside.txt", completed.stderr)


if __name__ == "__main__":
    unittest.main()
