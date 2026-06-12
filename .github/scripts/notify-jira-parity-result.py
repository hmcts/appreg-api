#!/usr/bin/env python3
"""Notify the Azure webhook that a report-only parity check completed."""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


def _env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def _post_json(url: str, payload: dict[str, Any], timeout: int) -> dict[str, Any]:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "User-Agent": "codex-parity-check",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body = response.read().decode("utf-8")
            return json.loads(body) if body else {}
    except urllib.error.HTTPError as exc:
        details = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"Azure Jira parity notification failed with HTTP {exc.code}: {details}"
        ) from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Azure Jira parity notification failed: {exc.reason}") from exc


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", required=True)
    args = parser.parse_args()

    notify_url = _env("CODEX_JIRA_PARITY_NOTIFY_URL")
    if not notify_url:
        print(
            "::error::CODEX_JIRA_PARITY_NOTIFY_URL is not configured; "
            "cannot notify Jira of the parity result.",
            file=sys.stderr,
        )
        return 1

    payload = json.loads(Path(args.report).read_text(encoding="utf-8"))
    timeout = int(_env("CODEX_JIRA_PARITY_NOTIFY_TIMEOUT_SECONDS", "10"))
    result = _post_json(notify_url, payload, timeout)
    print(f"Azure Jira parity notification accepted for {payload.get('issueKey', 'unknown')}: {result}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"::error::{error}", file=sys.stderr)
        raise SystemExit(1)
