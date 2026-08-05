#!/usr/bin/env python3
"""Exercise the no-change review collector-to-publisher artifact contract."""

from __future__ import annotations

import json
import os
import shlex
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT_DIR = Path(__file__).parent
COLLECTOR = SCRIPT_DIR / "codex-pr-review-collect.sh"
PUBLISHER = SCRIPT_DIR / "codex-pr-review-publish.sh"


class CodexPrReviewHandoffTest(unittest.TestCase):
    def test_no_change_comment_uses_the_publisher_artifact_name(self) -> None:
        with tempfile.TemporaryDirectory() as temporary_directory:
            root = Path(temporary_directory)
            output_dir = root / "output"
            verification_dir = root / "verification"
            fake_bin = root / "bin"
            captured_body = root / "published-comment.md"
            fake_bin.mkdir()
            verification_dir.mkdir()

            result = {
                "has_changes": False,
                "patch_gzip_base64": "",
                "summary": "The feedback did not require a code change.",
                "testing": "Reviewed the requested behavior.",
            }
            collector_environment = {
                **os.environ,
                "CODEX_RESULT": json.dumps(result),
                "OUTPUT_DIR": str(output_dir),
                "PR_NUMBER": "42",
                "HEAD_REF": "codex/example",
                "BASE_REF": "master",
                "COMMENT_AUTHOR": "reviewer",
                "COMMENT_URL": "https://example.invalid/review/1",
            }
            collected = subprocess.run(
                ["bash", str(COLLECTOR)],
                env=collector_environment,
                capture_output=True,
                text=True,
            )
            self.assertEqual(collected.returncode, 0, collected.stderr)

            comment_path = output_dir / "codex-review-comment.md"
            self.assertTrue(comment_path.is_file())
            self.assertFalse((output_dir / "codex-comment.md").exists())
            (verification_dir / "codex-review-comment.md").write_bytes(comment_path.read_bytes())
            (verification_dir / "verification.env").write_text(
                "has_changes=false\npr_number=42\nhead_ref=codex/example\nbase_ref=master\n",
                encoding="utf-8",
            )

            fake_gh = fake_bin / "gh"
            fake_gh.write_text(
                "#!/usr/bin/env bash\n"
                "set -euo pipefail\n"
                "body_file=''\n"
                "while [[ $# -gt 0 ]]; do\n"
                "  if [[ \"$1\" == '--body-file' ]]; then body_file=\"$2\"; shift 2; else shift; fi\n"
                "done\n"
                "test -n \"$body_file\"\n"
                f"cp \"$body_file\" {shlex.quote(str(captured_body))}\n",
                encoding="utf-8",
            )
            fake_gh.chmod(0o755)

            publisher_environment = {
                **os.environ,
                "PATH": f"{fake_bin}:{os.environ['PATH']}",
                "GH_TOKEN": "test-token",
                "GITHUB_REPOSITORY": "hmcts/example",
                "OUTPUT_DIR": str(output_dir),
                "VERIFICATION_DIR": str(verification_dir),
                "EXPECTED_PR_NUMBER": "42",
                "EXPECTED_HEAD_REF": "codex/example",
                "RUNNER_TEMP": str(root / "runner-temp"),
            }
            published = subprocess.run(
                ["bash", str(PUBLISHER)],
                env=publisher_environment,
                capture_output=True,
                text=True,
            )
            self.assertEqual(published.returncode, 0, published.stderr)
            self.assertEqual(captured_body.read_bytes(), comment_path.read_bytes())
            self.assertIn("did not produce any committable changes", captured_body.read_text())


if __name__ == "__main__":
    unittest.main()
