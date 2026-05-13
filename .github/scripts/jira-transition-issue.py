#!/usr/bin/env python3

import argparse
import base64
import json
import os
import sys
from dataclasses import dataclass
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen


@dataclass(frozen=True)
class JiraConfig:
    base_url: str
    user_email: str
    api_token: str
    issue_key: str
    target_status: str
    pr_url: str


def _required_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def _jira_base_url(issue_url: str) -> str:
    configured = os.environ.get("JIRA_BASE_URL", "").strip()
    if configured:
        return configured.rstrip("/")

    parsed = urlparse(issue_url)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise RuntimeError(
            "JIRA_BASE_URL is not configured and ISSUE_URL is not an absolute URL."
        )
    return f"{parsed.scheme}://{parsed.netloc}"


def _request(
    config: JiraConfig, method: str, path: str, body: dict[str, Any] | None = None
) -> dict[str, Any]:
    auth = base64.b64encode(
        f"{config.user_email}:{config.api_token}".encode("utf-8")
    ).decode("ascii")
    data = None if body is None else json.dumps(body).encode("utf-8")
    request = Request(
        f"{config.base_url}{path}",
        data=data,
        method=method,
        headers={
            "Accept": "application/json",
            "Authorization": f"Basic {auth}",
            "Content-Type": "application/json",
            "User-Agent": "codex-jira-dispatch",
        },
    )

    try:
        with urlopen(request, timeout=30) as response:
            payload = response.read().decode("utf-8", errors="replace")
    except HTTPError as exc:
        details = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"Jira API request failed: {method} {path} returned HTTP {exc.code}: {details}"
        ) from exc
    except URLError as exc:
        raise RuntimeError(f"Jira API request failed: {method} {path}: {exc.reason}") from exc

    return json.loads(payload) if payload else {}


def _normalise(value: str) -> str:
    return " ".join(value.casefold().split())


def _comment_body(pr_url: str) -> dict[str, Any]:
    text = f"Codex opened a pull request for this ticket: {pr_url}"
    return {
        "body": {
            "type": "doc",
            "version": 1,
            "content": [
                {
                    "type": "paragraph",
                    "content": [{"type": "text", "text": text}],
                }
            ],
        }
    }


def transition_issue(config: JiraConfig) -> None:
    issue = _request(
        config,
        "GET",
        f"/rest/api/3/issue/{config.issue_key}?fields=status",
    )
    current_status = issue.get("fields", {}).get("status", {}).get("name", "")
    if _normalise(current_status) == _normalise(config.target_status):
        print(f"{config.issue_key} is already in {config.target_status}.")
    else:
        transitions = _request(
            config,
            "GET",
            f"/rest/api/3/issue/{config.issue_key}/transitions",
        ).get("transitions", [])

        selected = None
        for transition in transitions:
            transition_name = transition.get("name", "")
            target_name = transition.get("to", {}).get("name", "")
            if _normalise(transition_name) == _normalise(config.target_status):
                selected = transition
                break
            if _normalise(target_name) == _normalise(config.target_status):
                selected = transition
                break

        if selected is None:
            available = ", ".join(
                sorted(
                    {
                        f"{item.get('name', '<unnamed>')} -> {item.get('to', {}).get('name', '<unknown>')}"
                        for item in transitions
                    }
                )
            )
            raise RuntimeError(
                f"No Jira transition found from '{current_status}' to "
                f"'{config.target_status}'. Available transitions: {available or '<none>'}"
            )

        _request(
            config,
            "POST",
            f"/rest/api/3/issue/{config.issue_key}/transitions",
            {"transition": {"id": selected["id"]}},
        )
        print(
            f"Transitioned {config.issue_key} from {current_status} "
            f"to {config.target_status} using transition '{selected.get('name')}'."
        )

    _request(
        config,
        "POST",
        f"/rest/api/3/issue/{config.issue_key}/comment",
        _comment_body(config.pr_url),
    )
    print(f"Added Jira PR comment for {config.issue_key}.")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pr-url", required=True)
    args = parser.parse_args()

    config = JiraConfig(
        base_url=_jira_base_url(_required_env("ISSUE_URL")),
        user_email=_required_env("JIRA_USER_EMAIL"),
        api_token=_required_env("JIRA_API_TOKEN"),
        issue_key=_required_env("ISSUE_KEY"),
        target_status=os.environ.get("JIRA_TARGET_STATUS", "Dev Review").strip()
        or "Dev Review",
        pr_url=args.pr_url,
    )

    transition_issue(config)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(1)
