#!/usr/bin/env python3

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from collections.abc import Callable
from pathlib import Path
from typing import Any


class PublisherVerificationError(RuntimeError):
    pass


class GitHubClient:
    def __init__(
        self,
        api_url: str,
        token: str,
        opener: Callable[..., Any] = urllib.request.urlopen,
    ) -> None:
        self.api_url = api_url.rstrip("/")
        self.token = token
        self.opener = opener

    def get_json(self, path: str) -> dict[str, Any]:
        request = urllib.request.Request(
            f"{self.api_url}{path}",
            headers={
                "Accept": "application/vnd.github+json",
                "Authorization": f"Bearer {self.token}",
                "User-Agent": "codex-github-app-publisher-verifier",
                "X-GitHub-Api-Version": "2022-11-28",
            },
        )
        try:
            with self.opener(request, timeout=20) as response:
                payload = json.load(response)
        except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as exc:
            raise PublisherVerificationError(f"GitHub API request failed for {path}: {exc}") from exc
        except (json.JSONDecodeError, TypeError, ValueError) as exc:
            raise PublisherVerificationError(f"GitHub API returned invalid JSON for {path}") from exc

        if not isinstance(payload, dict):
            raise PublisherVerificationError(f"GitHub API returned an unexpected payload for {path}")
        return payload


def required_environment(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise PublisherVerificationError(f"Missing required environment variable: {name}")
    return value


def validate_publisher(
    expected_app_slug: str,
    expected_installation_id: str,
    repository: str,
    installation_payload: dict[str, Any],
    repository_payload: dict[str, Any],
    bot_payload: dict[str, Any],
) -> tuple[str, str]:
    if not re.fullmatch(r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,98}[A-Za-z0-9])?", expected_app_slug):
        raise PublisherVerificationError("GITHUB_APP_SLUG is not a valid GitHub App slug")
    if not expected_installation_id.isdigit() or int(expected_installation_id) < 1:
        raise PublisherVerificationError("GITHUB_APP_INSTALLATION_ID is not valid")

    repository_parts = repository.split("/")
    if (
        not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository)
        or any(part in {".", ".."} for part in repository_parts)
    ):
        raise PublisherVerificationError("GITHUB_REPOSITORY is not a valid owner/repository name")

    if installation_payload.get("id") != int(expected_installation_id):
        raise PublisherVerificationError("Publisher token resolved an unexpected App installation")
    actual_app_slug = installation_payload.get("app_slug")
    if not isinstance(actual_app_slug, str) or actual_app_slug.casefold() != expected_app_slug.casefold():
        raise PublisherVerificationError(
            f"Publisher token belongs to App {actual_app_slug!r}, expected {expected_app_slug}"
        )

    installation_account = installation_payload.get("account")
    expected_owner = repository_parts[0]
    if (
        not isinstance(installation_account, dict)
        or not isinstance(installation_account.get("login"), str)
        or installation_account["login"].casefold() != expected_owner.casefold()
    ):
        raise PublisherVerificationError(
            f"GitHub App installation is not owned by {expected_owner}"
        )

    installation_permissions = installation_payload.get("permissions")
    required_permissions = ("contents", "pull_requests", "issues", "workflows")
    if not isinstance(installation_permissions, dict) or any(
        installation_permissions.get(permission) != "write"
        for permission in required_permissions
    ):
        raise PublisherVerificationError(
            "GitHub App installation lacks required contents, pull requests, issues or workflows write permission"
        )

    full_name = repository_payload.get("full_name")
    if not isinstance(full_name, str) or full_name.casefold() != repository.casefold():
        raise PublisherVerificationError("Publisher token resolved an unexpected repository")
    repository_permissions = repository_payload.get("permissions")
    if not isinstance(repository_permissions, dict) or repository_permissions.get("push") is not True:
        raise PublisherVerificationError(
            f"GitHub App {actual_app_slug} does not have push permission for {repository}"
        )

    expected_bot_login = f"{actual_app_slug}[bot]"
    actual_bot_login = bot_payload.get("login")
    bot_id = bot_payload.get("id")
    if (
        bot_payload.get("type") != "Bot"
        or not isinstance(actual_bot_login, str)
        or actual_bot_login.casefold() != expected_bot_login.casefold()
        or not isinstance(bot_id, int)
        or bot_id < 1
    ):
        raise PublisherVerificationError("GitHub App bot identity could not be verified")

    return actual_bot_login, f"{bot_id}+{actual_bot_login}@users.noreply.github.com"


def write_output(name: str, value: str) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT", "").strip()
    if not output_path:
        return
    with Path(output_path).open("a", encoding="utf-8") as output_file:
        output_file.write(f"{name}={value}\n")


def main() -> int:
    try:
        token = required_environment("GH_TOKEN")
        expected_app_slug = required_environment("GITHUB_APP_SLUG")
        expected_installation_id = required_environment("GITHUB_APP_INSTALLATION_ID")
        repository = os.environ.get("CODEX_PUBLISHER_REPOSITORY", "").strip()
        if not repository:
            repository = required_environment("GITHUB_REPOSITORY")
        api_url = os.environ.get("GITHUB_API_URL", "https://api.github.com")
        client = GitHubClient(api_url, token)
        encoded_bot_login = urllib.parse.quote(f"{expected_app_slug}[bot]", safe="")
        publisher_login, publisher_email = validate_publisher(
            expected_app_slug,
            expected_installation_id,
            repository,
            client.get_json("/installation"),
            client.get_json(f"/repos/{repository}"),
            client.get_json(f"/users/{encoded_bot_login}"),
        )
        write_output("publisher_login", publisher_login)
        write_output("publisher_email", publisher_email)
    except PublisherVerificationError as exc:
        print(f"Trusted publisher verification failed: {exc}", file=sys.stderr)
        return 1

    print(
        f"Verified GitHub App publisher {publisher_login} for {repository} "
        f"using installation {expected_installation_id}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
