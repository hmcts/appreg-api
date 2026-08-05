#!/usr/bin/env python3

import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("validate-codex-plan.py")


def valid_plan() -> dict:
    return {
        "ready_to_implement": True,
        "problem_analysis": "The controller accepts an invalid request.",
        "root_cause": "Validation is missing at the API boundary.",
        "scope_decision": "Correct the shared request contract rather than intercepting one endpoint.",
        "risk_level": "medium",
        "cross_system_change": False,
        "alternatives_considered": ["Intercept the request in the controller."],
        "implementation_steps": [
            {
                "path": "src/main/java/example/Request.java",
                "change": "Add the shared validation rule.",
                "reason": "All generated endpoints should enforce the same contract.",
            }
        ],
        "tests_required": ["Add a request validation test."],
        "acceptance_criteria": ["Invalid requests return HTTP 400."],
        "risks": ["Generated API code may need regeneration."],
        "assumptions": [],
        "blockers": [],
    }


class ValidateCodexPlanTest(unittest.TestCase):
    def run_validator(self, plan: object) -> tuple[subprocess.CompletedProcess[str], Path]:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        root = Path(temp_dir.name)
        output_dir = root / "output"
        github_output = root / "github-output"
        environment = {
            **os.environ,
            "CODEX_PLAN_RESULT": json.dumps(plan),
            "OUTPUT_DIR": str(output_dir),
            "GITHUB_OUTPUT": str(github_output),
        }
        result = subprocess.run(
            ["python3", str(SCRIPT)],
            env=environment,
            text=True,
            capture_output=True,
            check=False,
        )
        return result, root

    def test_accepts_and_normalises_ready_plan(self) -> None:
        result, root = self.run_validator(valid_plan())
        self.assertEqual(result.returncode, 0, result.stderr)
        normalised = json.loads((root / "output" / "plan.json").read_text())
        self.assertTrue(normalised["ready_to_implement"])
        self.assertTrue((root / "output" / "plan.md").is_file())
        outputs = (root / "github-output").read_text()
        self.assertIn("requires_approval=false", outputs)

    def test_marks_high_risk_plan_for_approval(self) -> None:
        plan = valid_plan()
        plan["risk_level"] = "high"
        result, root = self.run_validator(plan)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("requires_approval=true", (root / "github-output").read_text())

    def test_marks_cross_system_plan_for_approval(self) -> None:
        plan = valid_plan()
        plan["cross_system_change"] = True
        result, root = self.run_validator(plan)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("requires_approval=true", (root / "github-output").read_text())

    def test_rejects_ready_plan_with_blockers(self) -> None:
        plan = valid_plan()
        plan["blockers"] = ["Missing acceptance criteria from the ticket."]
        result, _ = self.run_validator(plan)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("must not contain blockers", result.stderr)

    def test_rejects_protected_automation_path(self) -> None:
        plan = valid_plan()
        plan["implementation_steps"][0]["path"] = ".github/workflows/unsafe.yml"
        result, _ = self.run_validator(plan)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("protected automation metadata", result.stderr)

    def test_requires_blocker_when_not_ready(self) -> None:
        plan = valid_plan()
        plan["ready_to_implement"] = False
        plan["implementation_steps"] = []
        plan["tests_required"] = []
        plan["acceptance_criteria"] = []
        result, _ = self.run_validator(plan)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("must explain its blockers", result.stderr)

    def test_rejects_oversized_plan(self) -> None:
        plan = valid_plan()
        plan["problem_analysis"] = "x" * (64 * 1024)
        result, _ = self.run_validator(plan)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("byte limit", result.stderr)


if __name__ == "__main__":
    unittest.main()
