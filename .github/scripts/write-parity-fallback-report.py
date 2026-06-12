#!/usr/bin/env python3
"""Write a Jira-safe fallback parity report."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path


def _env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--summary", required=True)
    parser.add_argument("--gap", required=True)
    parser.add_argument("--recommended-next-step", required=True)
    parser.add_argument("--snapshot-id", default="unavailable")
    args = parser.parse_args()

    output_dir = Path(_env("OUTPUT_DIR"))
    if not output_dir:
        raise RuntimeError("OUTPUT_DIR is required.")

    output_dir.mkdir(parents=True, exist_ok=True)

    report = {
        "issueKey": _env("ISSUE_KEY"),
        "issueUrl": _env("ISSUE_URL"),
        "repository": _env("GITHUB_REPOSITORY"),
        "status": "UNCERTAIN",
        "confidence": "low",
        "summary": args.summary,
        "runUrl": f"{_env('GITHUB_SERVER_URL', 'https://github.com')}/{_env('GITHUB_REPOSITORY')}/actions/runs/{_env('GITHUB_RUN_ID')}",
        "snapshotId": args.snapshot_id,
        "legacyEvidence": [],
        "modernEvidence": [],
        "gaps": [args.gap],
        "recommendedNextStep": args.recommended_next_step,
    }

    (output_dir / "parity-report.json").write_text(
        json.dumps(report, indent=2, sort_keys=True),
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
