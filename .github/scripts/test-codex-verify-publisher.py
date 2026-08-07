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
    def payloads(self) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
        return (
            {
                "id": 12345,
                "app_slug": "hmcts-codex-agent",
                "account": {"login": "hmcts"},
                "permissions": {
                    "contents": "write",
                    "pull_requests": "write",
                    "issues": "write",
                    "workflows": "write",
                },
            },
            {
                "full_name": "hmcts/appreg-api",
                "permissions": {"pull": True, "push": True},
            },
            {"login": "hmcts-codex-agent[bot]", "id": 98765, "type": "Bot"},
        )

    def validate(self, **overrides: Any) -> tuple[str, str]:
        installation, repository, bot = self.payloads()
        return MODULE.validate_publisher(
            overrides.get("app_slug", "hmcts-codex-agent"),
            overrides.get("installation_id", "12345"),
            overrides.get("repository_name", "hmcts/appreg-api"),
            overrides.get("installation", installation),
            overrides.get("repository", repository),
            overrides.get("bot", bot),
        )

    def test_accepts_expected_app_installation_with_push_permission(self) -> None:
        login, email = self.validate()
        self.assertEqual(login, "hmcts-codex-agent[bot]")
        self.assertEqual(email, "98765+hmcts-codex-agent[bot]@users.noreply.github.com")

    def test_rejects_unexpected_app(self) -> None:
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "expected another-app"):
            self.validate(app_slug="another-app")

    def test_rejects_unexpected_installation(self) -> None:
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "unexpected App installation"):
            self.validate(installation_id="54321")

    def test_rejects_installation_owned_by_another_account(self) -> None:
        installation, _, _ = self.payloads()
        installation["account"]["login"] = "another-org"
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "not owned by hmcts"):
            self.validate(installation=installation)

    def test_rejects_missing_installation_permission(self) -> None:
        installation, _, _ = self.payloads()
        installation["permissions"]["issues"] = "read"
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "lacks required"):
            self.validate(installation=installation)

    def test_rejects_token_without_push_permission(self) -> None:
        _, repository, _ = self.payloads()
        repository["permissions"]["push"] = False
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "does not have push"):
            self.validate(repository=repository)

    def test_rejects_unexpected_repository(self) -> None:
        _, repository, _ = self.payloads()
        repository["full_name"] = "hmcts/another-repository"
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "unexpected repository"):
            self.validate(repository=repository)

    def test_rejects_invalid_repository_name(self) -> None:
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "valid owner/repository"):
            self.validate(repository_name="../appreg-api")

    def test_rejects_non_bot_identity(self) -> None:
        _, _, bot = self.payloads()
        bot["type"] = "User"
        with self.assertRaisesRegex(MODULE.PublisherVerificationError, "bot identity"):
            self.validate(bot=bot)

    def test_client_uses_bearer_token_without_putting_it_in_url(self) -> None:
        captured: dict[str, Any] = {}

        def opener(request: Any, timeout: int) -> Response:
            captured["url"] = request.full_url
            captured["authorization"] = request.get_header("Authorization")
            captured["timeout"] = timeout
            return Response(json.dumps({"id": 12345}).encode())

        client = MODULE.GitHubClient("https://api.github.test", "test-secret", opener=opener)
        self.assertEqual(client.get_json("/installation"), {"id": 12345})
        self.assertEqual(captured["url"], "https://api.github.test/installation")
        self.assertEqual(captured["authorization"], "Bearer test-secret")
        self.assertEqual(captured["timeout"], 20)


if __name__ == "__main__":
    unittest.main()
