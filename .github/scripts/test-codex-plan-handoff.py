#!/usr/bin/env python3
"""Regression tests for the validated plan artefact hand-off."""

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


class CodexPlanHandoffTest(unittest.TestCase):
    def make_plan(self) -> Path:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        plan_dir = Path(temp_dir.name)
        plan_bytes = b'{"ready_to_implement":true}\n'
        (plan_dir / "plan.json").write_bytes(plan_bytes)
        (plan_dir / "plan.sha256").write_text(
            f"{hashlib.sha256(plan_bytes).hexdigest()}\n", encoding="ascii"
        )
        (plan_dir / "plan.md").write_text("### Scope decision\n\nUpdate the shared validator.\n")
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

    def test_accepts_matching_plan_hash(self) -> None:
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

    def test_generation_collector_persists_plan_in_pr_body(self) -> None:
        plan_dir = self.make_plan()
        output_dir = plan_dir / "output"
        encoded_patch = base64.b64encode(
            gzip.compress(PATCH.encode("utf-8"), mtime=0)
        ).decode("ascii")
        result = {
            "has_changes": True,
            "patch_gzip_base64": encoded_patch,
            "summary": "Updated the shared validator.",
            "testing": "Added a focused test.",
        }
        completed = subprocess.run(
            ["bash", str(COLLECTOR)],
            cwd=plan_dir,
            env={
                **os.environ,
                "CODEX_RESULT": json.dumps(result),
                "CODEX_OPERATION": "jira-generate",
                "OUTPUT_DIR": str(output_dir),
                "BRANCH_NAME": "codex/test-plan",
                "ISSUE_KEY": "ARCPOC-1",
                "ISSUE_SUMMARY": "Validate requests",
                "ISSUE_URL": "https://example.invalid/ARCPOC-1",
                "PLAN_DIR": str(plan_dir),
            },
            text=True,
            capture_output=True,
            check=False,
        )
        self.assertEqual(completed.returncode, 0, completed.stderr)
        pr_body = (output_dir / "codex-pr-body.md").read_text(encoding="utf-8")
        self.assertIn("## Codex Plan", pr_body)
        self.assertIn("Update the shared validator.", pr_body)
        self.assertEqual((output_dir / "plan.json").read_bytes(), (plan_dir / "plan.json").read_bytes())


if __name__ == "__main__":
    unittest.main()
