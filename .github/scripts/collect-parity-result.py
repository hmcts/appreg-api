#!/usr/bin/env python3
"""Validate and sanitise a Codex parity result in a fresh workflow job."""

from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Any


VALID_STATUSES = {"MATCHES_LEGACY", "GAP_FOUND", "UNCERTAIN"}
VALID_CONFIDENCE = {"high", "medium", "low"}
SENSITIVE_PATTERNS = [
    re.compile(pattern, re.IGNORECASE)
    for pattern in [
        r"\b(?:api[_-]?key|access[_-]?token|refresh[_-]?token|secret|password|passwd|authorization)\b\s*[:=]",
        r"\bbearer\s+[a-z0-9._~+/=-]{12,}",
        r"-----BEGIN [A-Z ]*PRIVATE KEY-----",
        r"\bAKIA[0-9A-Z]{16}\b",
        r"\bghp_[A-Za-z0-9_]{20,}\b",
        r"\bgithub_pat_[A-Za-z0-9_]{20,}\b",
        r"\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.",
        r"\b[A-Z][A-Z0-9_]{2,}\s*=",
    ]
]
CODE_PATTERNS = [
    re.compile(pattern, re.IGNORECASE)
    for pattern in [
        r"```",
        r"\b(?:if|for|while|switch|catch)\s*\(",
        r"\b(?:public|private|protected|class|interface|enum|function|const|let|var|return|throw|new)\b.*[{};]",
        r"(?:=>|->|==|!=|&&|\|\|)",
        r"[A-Za-z_][A-Za-z0-9_]*\([^)]*\)\s*[.{;]",
    ]
]


def env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def coerce_string(value: Any, limit: int) -> str:
    text = str(value or "").replace("\r", " ").replace("\n", " ").strip()
    return re.sub(r"\s+", " ", text)[:limit]


def contains_blocked_content(text: str) -> bool:
    return bool(text) and any(
        pattern.search(text) for pattern in SENSITIVE_PATTERNS + CODE_PATTERNS
    )


def safe_string(value: Any, limit: int, fallback: str) -> str:
    text = coerce_string(value, limit)
    return fallback if contains_blocked_content(text) else text


def safe_array(value: Any, limit: int, item_limit: int, fallback: str) -> list[str]:
    if not isinstance(value, list):
        return []

    items = []
    for item in value[:limit]:
        text = coerce_string(item, item_limit)
        if text and not contains_blocked_content(text):
            items.append(text)
    if value and not items:
        return [fallback]
    return items


def parse_final_message(text: str) -> dict[str, Any]:
    stripped = text.strip()
    if not stripped:
        raise ValueError("Codex did not return a parity result")

    try:
        result = json.loads(stripped)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", stripped, flags=re.DOTALL)
        if not match:
            raise
        result = json.loads(match.group(0))

    if not isinstance(result, dict):
        raise ValueError("Codex parity result was not a JSON object")
    return result


def fallback_result(outcome: str, reason: str | None = None) -> dict[str, Any]:
    if outcome != "success":
        summary = f"Codex parity check did not complete successfully. Outcome: {outcome or 'not-run'}."
        gap = "Parity workflow needs manual review because Codex did not complete successfully."
    else:
        summary = f"Codex parity report could not be parsed: {reason or 'unknown error'}"
        gap = "Parity workflow needs manual review because the Codex output was not valid JSON."

    return {
        "status": "UNCERTAIN",
        "confidence": "low",
        "summary": summary,
        "legacyEvidence": [],
        "modernEvidence": [],
        "gaps": [gap],
        "recommendedNextStep": "Inspect the GitHub Actions run logs and rerun the parity check.",
    }


def main() -> int:
    output_dir_value = env("OUTPUT_DIR")
    if not output_dir_value:
        raise RuntimeError("OUTPUT_DIR is required")

    output_dir = Path(output_dir_value)
    output_dir.mkdir(parents=True, exist_ok=True)
    outcome = env("CODEX_ACTION_OUTCOME", "not-run")

    if outcome == "success":
        try:
            raw = parse_final_message(os.environ.get("CODEX_FINAL_MESSAGE", ""))
        except Exception as error:
            raw = fallback_result(outcome, str(error))
    else:
        raw = fallback_result(outcome)

    status = raw.get("status") if raw.get("status") in VALID_STATUSES else "UNCERTAIN"
    confidence = (
        raw.get("confidence") if raw.get("confidence") in VALID_CONFIDENCE else "low"
    )
    report = {
        "issueKey": env("ISSUE_KEY"),
        "issueUrl": env("ISSUE_URL"),
        "repository": env("GITHUB_REPOSITORY"),
        "status": status,
        "confidence": confidence,
        "summary": safe_string(
            raw.get("summary"),
            900,
            "Parity summary was suppressed because it contained unsafe content.",
        ),
        "runUrl": (
            f"{env('GITHUB_SERVER_URL', 'https://github.com')}/"
            f"{env('GITHUB_REPOSITORY')}/actions/runs/{env('GITHUB_RUN_ID')}"
        ),
        "snapshotId": coerce_string(env("SNAPSHOT_ID") or "unavailable", 200),
        "legacyEvidence": safe_array(
            raw.get("legacyEvidence"),
            10,
            240,
            "Generated legacy evidence was suppressed because it contained unsafe content.",
        ),
        "modernEvidence": safe_array(
            raw.get("modernEvidence"),
            10,
            240,
            "Generated Jira ticket evidence was suppressed because it contained unsafe content.",
        ),
        "gaps": safe_array(
            raw.get("gaps"),
            10,
            320,
            "Generated gap details were suppressed because they contained unsafe content.",
        ),
        "recommendedNextStep": safe_string(
            raw.get("recommendedNextStep"),
            500,
            "Review the Jira ticket and GitHub Actions run manually.",
        ),
    }

    (output_dir / "parity-report.json").write_text(
        json.dumps(report, indent=2, sort_keys=True), encoding="utf-8"
    )

    lines = [
        "## Apps Reg legacy parity check",
        "",
        f"Result: {report['status']}",
        f"Confidence: {report['confidence']}",
        f"Snapshot: {report['snapshotId']}",
        "",
        report["summary"] or "No summary provided.",
        "",
        "Legacy evidence:",
    ]
    lines.extend(
        f"- {item}"
        for item in report["legacyEvidence"] or ["No legacy evidence identified."]
    )
    lines.extend(["", "Jira ticket evidence:"])
    lines.extend(
        f"- {item}"
        for item in report["modernEvidence"] or ["No Jira ticket evidence identified."]
    )
    lines.extend(["", "Gaps:"])
    lines.extend(f"- {item}" for item in report["gaps"] or ["No gaps listed."])
    lines.extend(
        [
            "",
            f"Recommended next step: {report['recommendedNextStep'] or 'Manual review.'}",
            f"Run: {report['runUrl']}",
        ]
    )
    (output_dir / "parity-comment.md").write_text(
        "\n".join(lines) + "\n", encoding="utf-8"
    )
    print(f"Parity report written to {output_dir / 'parity-report.json'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
