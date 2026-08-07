#!/usr/bin/env python3

from __future__ import annotations

import importlib.util
import sys
import unittest
from pathlib import Path
from typing import Any


MODULE_PATH = Path(__file__).with_name("codex-approve-pr-workflows.py")
SPEC = importlib.util.spec_from_file_location("codex_approve_pr_workflows", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


SHA = "a" * 40
REPOSITORY = "hmcts/appreg-api"
BRANCH = "codex/arcpoc-1234-1"


def config(**overrides: Any) -> Any:
    values = {
        "repository": REPOSITORY,
        "pr_number": 123,
        "expected_branch": BRANCH,
        "expected_commit_sha": SHA,
        "expected_base_ref": "master",
        "expected_author": "github-actions[bot]",
        "branch_prefix": "codex/",
        "allowed_workflow_paths": frozenset(
            {
                ".github/workflows/checks.yml",
                ".github/workflows/on-pr.yml",
            }
        ),
        "blocked_paths": (".github/", "package.json", ".yarn/"),
        "max_changed_files": 30,
        "timeout_seconds": 20,
        "poll_seconds": 1,
        "quiet_polls": 1,
        "dry_run": False,
    }
    values.update(overrides)
    return MODULE.Config(**values)


def pull_request(**overrides: Any) -> dict[str, Any]:
    value = {
        "number": 123,
        "state": "open",
        "draft": False,
        "user": {"login": "github-actions[bot]"},
        "head": {
            "ref": BRANCH,
            "sha": SHA,
            "repo": {"full_name": REPOSITORY},
        },
        "base": {
            "ref": "master",
            "repo": {"full_name": REPOSITORY},
        },
        "changed_files": 1,
    }
    value.update(overrides)
    return value


def workflow_run(
    run_id: int = 456,
    *,
    path: str = ".github/workflows/checks.yml",
    conclusion: str | None = "action_required",
    actor: str = "github-actions[bot]",
    triggering_actor: str | None = None,
) -> dict[str, Any]:
    repo_url = f"https://api.github.com/repos/{REPOSITORY}"
    return {
        "id": run_id,
        "name": "CI Checks",
        "path": path,
        "event": "pull_request",
        "head_branch": BRANCH,
        "head_sha": SHA,
        "head_repository": {"full_name": REPOSITORY},
        "actor": {"login": actor},
        "triggering_actor": {"login": triggering_actor or actor},
        "conclusion": conclusion,
        "pull_requests": [
            {
                "number": 123,
                "head": {
                    "ref": BRANCH,
                    "sha": SHA,
                    "repo": {"url": repo_url},
                },
                "base": {"ref": "master", "repo": {"url": repo_url}},
            }
        ],
    }


class FakeClock:
    def __init__(self) -> None:
        self.value = 0.0

    def monotonic(self) -> float:
        return self.value

    def sleep(self, seconds: float) -> None:
        self.value += seconds


class FakeClient:
    def __init__(
        self,
        *,
        pr: dict[str, Any] | None = None,
        files: list[dict[str, Any]] | None = None,
        runs: list[dict[str, Any]] | None = None,
    ) -> None:
        self.pr = pr or pull_request()
        self.files = files or [{"filename": "src/example.ts", "status": "modified"}]
        self.runs = runs or [workflow_run()]
        self.approved: list[int] = []

    def get_json(self, endpoint: str) -> dict[str, Any]:
        if endpoint.endswith("/pulls/123"):
            return self.pr
        if "/actions/runs?" in endpoint:
            return {"total_count": len(self.runs), "workflow_runs": self.runs}
        raise AssertionError(f"Unexpected endpoint: {endpoint}")

    def get_list(self, endpoint: str) -> list[dict[str, Any]]:
        if "/pulls/123/files?" not in endpoint:
            raise AssertionError(f"Unexpected endpoint: {endpoint}")
        return self.files

    def approve_run(self, repository: str, run_id: int) -> None:
        self.assert_repository(repository)
        self.approved.append(run_id)

    @staticmethod
    def assert_repository(repository: str) -> None:
        if repository != REPOSITORY:
            raise AssertionError(f"Unexpected repository: {repository}")


class ApproveCodexPrWorkflowsTest(unittest.TestCase):
    def test_accepts_expected_pull_request(self) -> None:
        self.assertEqual(MODULE.validate_pull_request(pull_request(), config()), 1)

    def test_rejects_wrong_pull_request_author(self) -> None:
        pr = pull_request(user={"login": "external-user"})
        with self.assertRaisesRegex(MODULE.ApprovalError, "unexpected author"):
            MODULE.validate_pull_request(pr, config())

    def test_rejects_wrong_head_repository(self) -> None:
        pr = pull_request(
            head={"ref": BRANCH, "sha": SHA, "repo": {"full_name": "fork/repo"}}
        )
        with self.assertRaisesRegex(MODULE.ApprovalError, "unexpected head repository"):
            MODULE.validate_pull_request(pr, config())

    def test_rejects_too_many_changed_files(self) -> None:
        with self.assertRaisesRegex(MODULE.ApprovalError, "allows at most 30"):
            MODULE.validate_pull_request(pull_request(changed_files=31), config())

    def test_rejects_protected_current_path(self) -> None:
        files = [{"filename": ".github/workflows/checks.yml", "status": "modified"}]
        with self.assertRaisesRegex(MODULE.ApprovalError, "protected paths"):
            MODULE.validate_changed_files(files, 1, config())

    def test_rejects_rename_from_protected_path(self) -> None:
        files = [
            {
                "filename": "src/renamed.ts",
                "previous_filename": ".github/scripts/unsafe.py",
                "status": "renamed",
            }
        ]
        with self.assertRaisesRegex(MODULE.ApprovalError, "protected paths"):
            MODULE.validate_changed_files(files, 1, config())

    def test_accepts_allow_listed_workflow_run(self) -> None:
        self.assertEqual(MODULE.validate_workflow_run(workflow_run(), config()), 456)

    def test_rejects_non_allow_listed_workflow(self) -> None:
        run = workflow_run(path=".github/workflows/unexpected.yml")
        with self.assertRaisesRegex(MODULE.ApprovalError, "non-allow-listed"):
            MODULE.validate_workflow_run(run, config())

    def test_rejects_run_from_different_actor(self) -> None:
        with self.assertRaisesRegex(MODULE.ApprovalError, "unexpected actor"):
            MODULE.validate_workflow_run(workflow_run(actor="external-user"), config())

    def test_rejects_unapproved_run_from_different_triggering_actor(self) -> None:
        run = workflow_run(triggering_actor="external-user")
        with self.assertRaisesRegex(MODULE.ApprovalError, "unexpected triggering actor"):
            MODULE.validate_workflow_run(run, config())

    def test_accepts_manually_approved_run_with_maintainer_trigger(self) -> None:
        run = workflow_run(conclusion="success", triggering_actor="maintainer")
        self.assertEqual(MODULE.validate_workflow_run(run, config()), 456)

    def test_approves_only_action_required_runs(self) -> None:
        client = FakeClient(
            runs=[
                workflow_run(456),
                workflow_run(
                    457,
                    path=".github/workflows/on-pr.yml",
                    conclusion="success",
                ),
            ]
        )
        clock = FakeClock()
        matched, approved = MODULE.approve_workflows(
            config(),
            client,
            monotonic=clock.monotonic,
            sleep=clock.sleep,
        )
        self.assertEqual((matched, approved), (2, 1))
        self.assertEqual(client.approved, [456])

    def test_dry_run_does_not_call_approval_api(self) -> None:
        client = FakeClient()
        clock = FakeClock()
        matched, approved = MODULE.approve_workflows(
            config(dry_run=True),
            client,
            monotonic=clock.monotonic,
            sleep=clock.sleep,
        )
        self.assertEqual((matched, approved), (1, 1))
        self.assertEqual(client.approved, [])


if __name__ == "__main__":
    unittest.main()
