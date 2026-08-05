#!/usr/bin/env python3

import base64
import hashlib
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
        "root_cause": "Validation is missing at the boundary.",
        "scope_decision": "Correct the shared contract rather than intercepting one route.",
        "risk_level": "medium",
        "cross_system_change": False,
        "alternatives_considered": ["Intercept the request in one route."],
        "implementation_steps": [
            {
                "path": "src/main/java/example/Request.java",
                "change": "Add the shared validation rule.",
                "reason": "All affected routes should enforce the same contract.",
            }
        ],
        "tests_required": ["Add a request validation test."],
        "acceptance_criteria": ["Invalid requests are rejected."],
        "risks": ["Generated code may need regeneration."],
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

    @staticmethod
    def outputs(root: Path) -> dict[str, str]:
        return dict(
            line.split("=", 1)
            for line in (root / "github-output").read_text(encoding="utf-8").splitlines()
        )

    def run_materializer(
        self, payload: str, expected_sha: str
    ) -> tuple[subprocess.CompletedProcess[str], Path]:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        root = Path(temp_dir.name)
        result = subprocess.run(
            ["python3", str(SCRIPT), "--materialize"],
            env={
                **os.environ,
                "CODEX_PLAN_PAYLOAD": payload,
                "EXPECTED_PLAN_SHA": expected_sha,
                "OUTPUT_DIR": str(root / "materialized"),
            },
            text=True,
            capture_output=True,
            check=False,
        )
        return result, root

    def test_accepts_ready_plan_and_emits_bounded_private_handoff(self) -> None:
        result, root = self.run_validator(valid_plan())
        self.assertEqual(result.returncode, 0, result.stderr)
        normalised_path = root / "output" / "plan.json"
        normalised = json.loads(normalised_path.read_text(encoding="utf-8"))
        self.assertTrue(normalised["ready_to_implement"])
        self.assertFalse((root / "output" / "plan.md").exists())
        self.assertEqual(
            (root / "output" / "allowed-paths.txt").read_text(encoding="utf-8"),
            "src/main/java/example/Request.java\n",
        )

        outputs = self.outputs(root)
        self.assertEqual(outputs["approval_required"], "true")
        self.assertEqual(outputs["planned_path_count"], "1")
        decoded = base64.b64decode(outputs["plan_payload"], validate=True)
        self.assertEqual(decoded, normalised_path.read_bytes())
        self.assertEqual(hashlib.sha256(decoded).hexdigest(), outputs["plan_sha256"])
        self.assertLessEqual(len(decoded), 32 * 1024)

    def test_low_risk_ready_plan_still_requires_approval(self) -> None:
        plan = valid_plan()
        plan["risk_level"] = "low"
        plan["cross_system_change"] = False
        result, root = self.run_validator(plan)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.outputs(root)["approval_required"], "true")

    def test_blocked_plan_is_valid_but_cannot_request_implementation(self) -> None:
        plan = valid_plan()
        plan["ready_to_implement"] = False
        plan["implementation_steps"] = []
        plan["tests_required"] = []
        plan["acceptance_criteria"] = []
        plan["blockers"] = ["The ticket does not identify the failing behavior."]
        result, root = self.run_validator(plan)
        self.assertEqual(result.returncode, 0, result.stderr)
        outputs = self.outputs(root)
        self.assertEqual(outputs["ready_to_implement"], "false")
        self.assertEqual(outputs["approval_required"], "false")

        materialized, _ = self.run_materializer(
            outputs["plan_payload"], outputs["plan_sha256"]
        )
        self.assertNotEqual(materialized.returncode, 0)
        self.assertIn("blocked plan cannot be materialised", materialized.stderr)

    def test_materializes_exact_validated_job_output(self) -> None:
        result, root = self.run_validator(valid_plan())
        self.assertEqual(result.returncode, 0, result.stderr)
        outputs = self.outputs(root)
        materialized, materialized_root = self.run_materializer(
            outputs["plan_payload"], outputs["plan_sha256"]
        )
        self.assertEqual(materialized.returncode, 0, materialized.stderr)
        bundle = materialized_root / "materialized"
        self.assertEqual(
            (bundle / "plan.json").read_bytes(),
            base64.b64decode(outputs["plan_payload"], validate=True),
        )
        self.assertEqual(
            (bundle / "allowed-paths.txt").read_text(encoding="utf-8"), "src/main/java/example/Request.java\n"
        )

    def test_materializer_rejects_mismatched_hash(self) -> None:
        result, root = self.run_validator(valid_plan())
        self.assertEqual(result.returncode, 0, result.stderr)
        outputs = self.outputs(root)
        materialized, _ = self.run_materializer(outputs["plan_payload"], "0" * 64)
        self.assertNotEqual(materialized.returncode, 0)
        self.assertIn("hash does not match", materialized.stderr)

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

    def test_rejects_directory_and_duplicate_paths(self) -> None:
        directory_plan = valid_plan()
        directory_plan["implementation_steps"][0]["path"] = "src/example/"
        directory_result, _ = self.run_validator(directory_plan)
        self.assertNotEqual(directory_result.returncode, 0)
        self.assertIn("one repository-relative file", directory_result.stderr)

        duplicate_plan = valid_plan()
        duplicate_plan["implementation_steps"].append(
            dict(duplicate_plan["implementation_steps"][0])
        )
        duplicate_result, _ = self.run_validator(duplicate_plan)
        self.assertNotEqual(duplicate_result.returncode, 0)
        self.assertIn("duplicate path", duplicate_result.stderr)

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
