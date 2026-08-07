#!/usr/bin/env python3

from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request
from collections.abc import Callable
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
                "User-Agent": "codex-trusted-publisher-verifier",
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
    expected_login: str,
    repository: str,
    user_payload: dict[str, Any],
    repository_payload: dict[str, Any],
) -> str:
    if expected_login.casefold() == "github-actions[bot]":
        raise PublisherVerificationError("The default GitHub Actions identity cannot publish Codex PRs")
    if not re.fullmatch(r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?", expected_login):
        raise PublisherVerificationError("CODEX_PUBLISHER_LOGIN is not a valid GitHub login")

    repository_parts = repository.split("/")
    if (
        not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository)
        or any(part in {".", ".."} for part in repository_parts)
    ):
        raise PublisherVerificationError("GITHUB_REPOSITORY is not a valid owner/repository name")

    actual_login = user_payload.get("login")
    if not isinstance(actual_login, str) or not actual_login.strip():
        raise PublisherVerificationError("Publisher token did not resolve to a GitHub login")
    actual_login = actual_login.strip()

    if actual_login.casefold() != expected_login.casefold():
        raise PublisherVerificationError(
            f"Publisher token belongs to {actual_login}, expected {expected_login}"
        )
    if user_payload.get("type") != "User":
        raise PublisherVerificationError("Publisher token must belong to a dedicated machine user")

    full_name = repository_payload.get("full_name")
    if not isinstance(full_name, str) or full_name.casefold() != repository.casefold():
        raise PublisherVerificationError("Publisher token resolved an unexpected repository")

    permissions = repository_payload.get("permissions")
    if not isinstance(permissions, dict) or permissions.get("push") is not True:
        raise PublisherVerificationError(
            f"Publisher {actual_login} does not have push permission for {repository}"
        )

    return actual_login


def main() -> int:
    try:
        token = required_environment("GH_TOKEN")
        expected_login = required_environment("CODEX_PUBLISHER_LOGIN")
        repository = required_environment("GITHUB_REPOSITORY")
        api_url = os.environ.get("GITHUB_API_URL", "https://api.github.com")
        client = GitHubClient(api_url, token)
        actual_login = validate_publisher(
            expected_login,
            repository,
            client.get_json("/user"),
            client.get_json(f"/repos/{repository}"),
        )
    except PublisherVerificationError as exc:
        print(f"Trusted publisher verification failed: {exc}", file=sys.stderr)
        return 1

    print(f"Verified trusted Codex publisher {actual_login} for {repository}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
