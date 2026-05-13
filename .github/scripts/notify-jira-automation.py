#!/usr/bin/env python3
"""Notify the Azure webhook that a Codex PR was opened for a Jira issue."""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from typing import Any


def _env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


def _required_env(name: str) -> str:
    value = _env(name)
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def _run_url() -> str:
    server_url = _env("GITHUB_SERVER_URL", "https://github.com")
    repository = _env("GITHUB_REPOSITORY")
    run_id = _env("GITHUB_RUN_ID")

    if repository and run_id:
        return f"{server_url}/{repository}/actions/runs/{run_id}"

    return ""


def _post_json(url: str, payload: dict[str, Any], timeout: int) -> dict[str, Any]:
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        headers={
            "Accept": "application/json",
            "Content-Type": "application/json",
            "User-Agent": "codex-jira-dispatch",
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
            f"Azure Jira PR notification failed with HTTP {exc.code}: {details}"
        ) from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"Azure Jira PR notification failed: {exc.reason}") from exc


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pr-url", required=True)
    parser.add_argument("--branch-name", required=True)
    parser.add_argument("--commit-sha", required=True)
    args = parser.parse_args()

    notify_url = _env("CODEX_JIRA_PR_NOTIFY_URL")
    if not notify_url:
        print(
            "::warning::CODEX_JIRA_PR_NOTIFY_URL is not configured; "
            "skipping Jira Automation PR notification."
        )
        return 0

    payload = {
        "issueKey": _required_env("ISSUE_KEY"),
        "issueUrl": _required_env("ISSUE_URL"),
        "prUrl": args.pr_url,
        "prTitle": f"{_required_env('ISSUE_KEY')}: {_required_env('ISSUE_SUMMARY')}",
        "repository": _required_env("GITHUB_REPOSITORY"),
        "branchName": args.branch_name,
        "commitSha": args.commit_sha,
        "actor": _env("GITHUB_ACTOR"),
        "runUrl": _run_url(),
    }

    timeout = int(_env("CODEX_JIRA_PR_NOTIFY_TIMEOUT_SECONDS", "10"))
    result = _post_json(notify_url, payload, timeout)
    print(f"Azure Jira PR notification accepted for {payload['issueKey']}: {result}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"::error::{error}", file=sys.stderr)
        raise SystemExit(1)
