#!/usr/bin/env python3

from __future__ import annotations

import importlib.util
import io
import json
import sys
import unittest
from pathlib import Path
from typing import Any


MODULE_PATH = Path(__file__).with_name("codex-verify-publisher.py")
SPEC = importlib.util.spec_from_file_location("codex_verify_publisher", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class Response(io.BytesIO):
    def __enter__(self) -> "Response":
        return self

    def __exit__(self, *_args: object) -> None:
        self.close()


class PublisherValidationTests(unittest.TestCase):
    def payloads(self) -> tuple[dict[str, Any], dict[str, Any]]:
        return (
            {"login": "appreg-codex-bot", "type": "User"},
            {
                "full_name": "hmcts/appreg-api",
                "permissions": {"pull": True, "push": True},
            },
        )

    def test_accepts_expected_publisher_with_push_permission(self) -> None:
        user, repository = self.payloads()
        login = MODULE.validate_publisher(
            "APPREG-CODEX-BOT", "hmcts/appreg-api", user, repository
        )
        self.assertEqual(login, "appreg-codex-bot")

    def test_rejects_unexpected_publisher(self) -> None:
        user, repository = self.payloads()
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "expected trusted-bot"):
            MODULE.validate_publisher("trusted-bot", "hmcts/appreg-api", user, repository)

    def test_rejects_default_actions_identity(self) -> None:
        _, repository = self.payloads()
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "default GitHub Actions"):
            MODULE.validate_publisher(
                "github-actions[bot]",
                "hmcts/appreg-api",
                {"login": "github-actions[bot]", "type": "Bot"},
                repository,
            )

    def test_rejects_non_user_identity(self) -> None:
        _, repository = self.payloads()
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "dedicated machine user"):
            MODULE.validate_publisher(
                "appreg-codex-bot",
                "hmcts/appreg-api",
                {"login": "appreg-codex-bot", "type": "Bot"},
                repository,
            )

    def test_rejects_token_without_push_permission(self) -> None:
        user, repository = self.payloads()
        repository["permissions"]["push"] = False
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "does not have push"):
            MODULE.validate_publisher(
                "appreg-codex-bot", "hmcts/appreg-api", user, repository
            )

    def test_rejects_unexpected_repository(self) -> None:
        user, repository = self.payloads()
        repository["full_name"] = "hmcts/another-repository"
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "unexpected repository"):
            MODULE.validate_publisher(
                "appreg-codex-bot", "hmcts/appreg-api", user, repository
            )

    def test_rejects_invalid_repository_name(self) -> None:
        user, repository = self.payloads()
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "valid owner/repository"):
            MODULE.validate_publisher("appreg-codex-bot", "../appreg-api", user, repository)

    def test_client_uses_bearer_token_without_putting_it_in_url(self) -> None:
        captured: dict[str, Any] = {}

        def opener(request: Any, timeout: int) -> Response:
            captured["url"] = request.full_url
            captured["authorization"] = request.get_header("Authorization")
            captured["timeout"] = timeout
            return Response(json.dumps({"login": "appreg-codex-bot"}).encode())

        client = MODULE.GitHubClient("https://api.github.test", "test-secret", opener=opener)
        self.assertEqual(client.get_json("/user"), {"login": "appreg-codex-bot"})
        self.assertEqual(captured["url"], "https://api.github.test/user")
        self.assertEqual(captured["authorization"], "Bearer test-secret")
        self.assertEqual(captured["timeout"], 20)


if __name__ == "__main__":
    unittest.main()
