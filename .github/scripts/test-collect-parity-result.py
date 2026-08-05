#!/usr/bin/env python3
"""Regression tests for the fresh-job parity result collector."""

from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("collect-parity-result.py")


class CollectParityResultTest(unittest.TestCase):
    def run_collector(self, *, outcome: str, message: str, snapshot_id: str) -> dict:
        with tempfile.TemporaryDirectory() as temporary_directory:
            output_dir = Path(temporary_directory) / "output"
            environment = {
                **os.environ,
                "CODEX_ACTION_OUTCOME": outcome,
                "CODEX_FINAL_MESSAGE": message,
                "GITHUB_REPOSITORY": "hmcts/appreg-api",
                "GITHUB_RUN_ID": "123",
                "ISSUE_KEY": "ARCPOC-1",
                "ISSUE_URL": "https://jira.example/ARCPOC-1",
                "OUTPUT_DIR": str(output_dir),
                "SNAPSHOT_ID": snapshot_id,
            }
            subprocess.run(
                [sys.executable, str(SCRIPT)],
                check=True,
                env=environment,
                capture_output=True,
                text=True,
            )
            return json.loads((output_dir / "parity-report.json").read_text(encoding="utf-8"))

    def test_preserves_valid_structured_result(self) -> None:
        result = self.run_collector(
            outcome="success",
            snapshot_id="snapshot-1",
            message=json.dumps(
                {
                    "status": "MATCHES_LEGACY",
                    "confidence": "high",
                    "summary": "Behaviour matches",
                    "legacyEvidence": ["Service path"],
                    "modernEvidence": ["Acceptance criterion"],
                    "gaps": [],
                    "recommendedNextStep": "Proceed",
                }
            ),
        )

        self.assertEqual(result["status"], "MATCHES_LEGACY")
        self.assertEqual(result["snapshotId"], "snapshot-1")
        self.assertEqual(result["issueKey"], "ARCPOC-1")

    def test_writes_uncertain_result_when_codex_fails(self) -> None:
        result = self.run_collector(outcome="failure", message="", snapshot_id="")

        self.assertEqual(result["status"], "UNCERTAIN")
        self.assertEqual(result["confidence"], "low")
        self.assertEqual(result["snapshotId"], "unavailable")

    def test_suppresses_sensitive_generated_content(self) -> None:
        result = self.run_collector(
            outcome="success",
            snapshot_id="snapshot-2",
            message=json.dumps(
                {
                    "status": "GAP_FOUND",
                    "confidence": "medium",
                    "summary": "api_key=not-a-real-key",
                    "legacyEvidence": [],
                    "modernEvidence": [],
                    "gaps": [],
                    "recommendedNextStep": "Review manually",
                }
            ),
        )

        self.assertEqual(
            result["summary"],
            "Parity summary was suppressed because it contained unsafe content.",
        )


if __name__ == "__main__":
    unittest.main()
