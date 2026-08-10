#!/usr/bin/env python3
"""Test revision-specific SonarCloud quality-gate polling."""

from __future__ import annotations

import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).parent / "codex-check-sonar-quality-gate.sh"
CURRENT_SHA = "a" * 40
STALE_SHA = "b" * 40


class SonarQualityGateTest(unittest.TestCase):
    def run_case(
        self,
        *,
        analyses: list[object],
        gates: list[object] | None = None,
        max_attempts: int = 3,
    ) -> tuple[subprocess.CompletedProcess[str], str]:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            fake_bin = root / "bin"
            fixtures = root / "fixtures"
            state = root / "state"
            fake_bin.mkdir()
            fixtures.mkdir()
            state.mkdir()

            for index, payload in enumerate(analyses, start=1):
                (fixtures / f"analyses-{index}.json").write_text(
                    payload if isinstance(payload, str) else json.dumps(payload),
                    encoding="utf-8",
                )
            for index, payload in enumerate(gates or [], start=1):
                (fixtures / f"gate-{index}.json").write_text(
                    payload if isinstance(payload, str) else json.dumps(payload),
                    encoding="utf-8",
                )
            (fixtures / "issues-1.json").write_text('{"issues": []}', encoding="utf-8")

            fake_curl = fake_bin / "curl"
            fake_curl.write_text(
                """#!/usr/bin/env bash
set -euo pipefail
printf '%s\\n' "$*" >>"$STATE_DIR/requests.log"
endpoint=""
for arg in "$@"; do
  case "$arg" in
    */api/project_analyses/search) endpoint=analyses ;;
    */api/qualitygates/project_status) endpoint=gate ;;
    */api/issues/search) endpoint=issues ;;
  esac
done
test -n "$endpoint"
counter="$STATE_DIR/$endpoint.count"
count=0
[[ -f "$counter" ]] && count="$(cat "$counter")"
count=$((count + 1))
printf '%s' "$count" >"$counter"
fixture="$FIXTURE_DIR/$endpoint-$count.json"
if [[ ! -f "$fixture" ]]; then
  fixture="$(ls "$FIXTURE_DIR"/"$endpoint"-*.json | sort | tail -1)"
fi
cat "$fixture"
""",
                encoding="utf-8",
            )
            fake_curl.chmod(0o755)

            environment = {
                **os.environ,
                "PATH": f"{fake_bin}:{os.environ['PATH']}",
                "FIXTURE_DIR": str(fixtures),
                "STATE_DIR": str(state),
                "SONAR_TOKEN": "never-print-this-token",
                "SONAR_PROJECT_KEY": "example-project",
                "PR_NUMBER": "42",
                "PUBLISHED_COMMIT_SHA": CURRENT_SHA,
                "SONAR_QUALITY_GATE_API_TIMEOUT_SECONDS": "10",
                "SONAR_QUALITY_GATE_API_POLL_SECONDS": "0",
                "SONAR_QUALITY_GATE_API_MAX_ATTEMPTS": str(max_attempts),
            }
            completed = subprocess.run(
                ["bash", str(SCRIPT)],
                env=environment,
                capture_output=True,
                text=True,
            )
            requests = (state / "requests.log").read_text(encoding="utf-8")
            self.assertNotIn("never-print-this-token", completed.stdout + completed.stderr)
            return completed, requests

    @staticmethod
    def analyses(*entries: tuple[str, str]) -> dict[str, object]:
        return {
            "analyses": [
                {"key": key, "revision": revision}
                for key, revision in entries
            ]
        }

    @staticmethod
    def gate(status: str) -> dict[str, object]:
        return {"projectStatus": {"status": status, "conditions": []}}

    def test_ignores_stale_success_and_waits_for_current_pending_analysis(self) -> None:
        completed, requests = self.run_case(
            analyses=[
                self.analyses(("stale-analysis", STALE_SHA), ("current-analysis", CURRENT_SHA)),
                self.analyses(("stale-analysis", STALE_SHA), ("current-analysis", CURRENT_SHA)),
            ],
            gates=[self.gate("NONE"), self.gate("OK")],
        )
        self.assertEqual(completed.returncode, 0, completed.stderr)
        self.assertNotIn("analysisId=stale-analysis", requests)
        self.assertEqual(requests.count("analysisId=current-analysis"), 2)

    def test_current_analysis_failure_is_rejected(self) -> None:
        completed, _ = self.run_case(
            analyses=[self.analyses(("current-analysis", CURRENT_SHA))],
            gates=[self.gate("ERROR")],
        )
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("quality gate failed", completed.stdout + completed.stderr)

    def test_current_analysis_success_is_accepted(self) -> None:
        completed, requests = self.run_case(
            analyses=[self.analyses(("current-analysis", CURRENT_SHA))],
            gates=[self.gate("OK")],
        )
        self.assertEqual(completed.returncode, 0, completed.stderr)
        self.assertIn("analysisId=current-analysis", requests)

    def test_times_out_when_only_stale_analyses_exist(self) -> None:
        completed, requests = self.run_case(
            analyses=[self.analyses(("stale-analysis", STALE_SHA))],
            max_attempts=2,
        )
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn("Timed out waiting", completed.stdout)
        self.assertNotIn("/api/qualitygates/project_status", requests)

    def test_malformed_analysis_and_gate_responses_are_rejected(self) -> None:
        malformed_analysis, _ = self.run_case(analyses=['{"analyses":'])
        self.assertNotEqual(malformed_analysis.returncode, 0)
        self.assertIn("malformed analysis data", malformed_analysis.stderr)

        malformed_gate, _ = self.run_case(
            analyses=[self.analyses(("current-analysis", CURRENT_SHA))],
            gates=[{"unexpected": {}}],
        )
        self.assertNotEqual(malformed_gate.returncode, 0)
        self.assertIn("malformed quality-gate data", malformed_gate.stderr)


if __name__ == "__main__":
    unittest.main()
