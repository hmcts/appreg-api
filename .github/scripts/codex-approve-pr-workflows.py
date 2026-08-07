#!/usr/bin/env python3
"""Approve allow-listed workflow runs for an independently verified Codex PR."""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import PurePosixPath
from typing import Any, Callable
from urllib.parse import urlencode


class ApprovalError(RuntimeError):
    """Raised when a PR or workflow run fails closed validation."""


@dataclass(frozen=True)
class Config:
    repository: str
    pr_number: int
    expected_branch: str
    expected_commit_sha: str
    expected_base_ref: str
    expected_author: str
    branch_prefix: str
    allowed_workflow_paths: frozenset[str]
    blocked_paths: tuple[str, ...]
    max_changed_files: int
    timeout_seconds: int
    poll_seconds: int
    quiet_polls: int
    dry_run: bool


class GitHubClient:
    def get_json(self, endpoint: str) -> dict[str, Any]:
        completed = subprocess.run(
            ["gh", "api", "--method", "GET", endpoint],
            check=False,
            capture_output=True,
            text=True,
        )
        if completed.returncode != 0:
            detail = completed.stderr.strip()[-2000:]
            raise ApprovalError(f"GitHub API GET failed for {endpoint}: {detail}")
        try:
            value = json.loads(completed.stdout)
        except json.JSONDecodeError as error:
            raise ApprovalError(f"GitHub API returned invalid JSON for {endpoint}") from error
        if not isinstance(value, dict):
            raise ApprovalError(f"GitHub API returned an unexpected value for {endpoint}")
        return value

    def get_list(self, endpoint: str) -> list[dict[str, Any]]:
        completed = subprocess.run(
            ["gh", "api", "--method", "GET", endpoint],
            check=False,
            capture_output=True,
            text=True,
        )
        if completed.returncode != 0:
            detail = completed.stderr.strip()[-2000:]
            raise ApprovalError(f"GitHub API GET failed for {endpoint}: {detail}")
        try:
            value = json.loads(completed.stdout)
        except json.JSONDecodeError as error:
            raise ApprovalError(f"GitHub API returned invalid JSON for {endpoint}") from error
        if not isinstance(value, list) or not all(isinstance(item, dict) for item in value):
            raise ApprovalError(f"GitHub API returned an unexpected list for {endpoint}")
        return value

    def approve_run(self, repository: str, run_id: int) -> None:
        endpoint = f"repos/{repository}/actions/runs/{run_id}/approve"
        completed = subprocess.run(
            ["gh", "api", "--method", "POST", endpoint],
            check=False,
            capture_output=True,
            text=True,
        )
        if completed.returncode != 0:
            detail = completed.stderr.strip()[-2000:]
            raise ApprovalError(f"GitHub API approval failed for run {run_id}: {detail}")


def require_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise ApprovalError(f"{name} is required")
    return value


def positive_int(name: str, default: str | None = None) -> int:
    raw = os.environ.get(name, default or "").strip()
    try:
        value = int(raw)
    except ValueError as error:
        raise ApprovalError(f"{name} must be an integer") from error
    if value <= 0:
        raise ApprovalError(f"{name} must be greater than zero")
    return value


def parse_boolean(name: str, default: str = "false") -> bool:
    raw = os.environ.get(name, default).strip().lower()
    if raw not in {"true", "false"}:
        raise ApprovalError(f"{name} must be true or false")
    return raw == "true"


def parse_lines(name: str) -> tuple[str, ...]:
    values = tuple(line.strip() for line in require_env(name).splitlines() if line.strip())
    if not values:
        raise ApprovalError(f"{name} must contain at least one value")
    return values


def validate_repo_path(path: str, label: str) -> str:
    if not isinstance(path, str) or not path:
        raise ApprovalError(f"{label} is missing")
    if path.startswith("/") or "\\" in path or "\x00" in path or "\n" in path or "\r" in path:
        raise ApprovalError(f"{label} is not a safe repository path: {path!r}")
    if any(part in {"", ".", ".."} for part in path.split("/")):
        raise ApprovalError(f"{label} is not a canonical repository path: {path!r}")
    if str(PurePosixPath(path)) != path:
        raise ApprovalError(f"{label} is not a canonical repository path: {path!r}")
    return path


def load_config() -> Config:
    repository = require_env("GITHUB_REPOSITORY")
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise ApprovalError("GITHUB_REPOSITORY is invalid")

    expected_branch = require_env("EXPECTED_HEAD_REF")
    branch_prefix = os.environ.get("CODEX_BRANCH_PREFIX", "codex/").strip()
    if not branch_prefix or not expected_branch.startswith(branch_prefix):
        raise ApprovalError(
            f"Expected branch {expected_branch!r} does not start with {branch_prefix!r}"
        )

    expected_commit_sha = require_env("EXPECTED_HEAD_SHA").lower()
    if not re.fullmatch(r"[0-9a-f]{40}", expected_commit_sha):
        raise ApprovalError("EXPECTED_HEAD_SHA must be a full Git commit SHA")

    allowed_workflow_paths = frozenset(
        validate_repo_path(path, "allowed workflow path")
        for path in parse_lines("CODEX_APPROVAL_WORKFLOW_PATHS")
    )
    if any(not path.startswith(".github/workflows/") for path in allowed_workflow_paths):
        raise ApprovalError("Allowed workflow paths must be under .github/workflows/")

    blocked_paths = parse_lines("CODEX_APPROVAL_BLOCKED_PATHS")
    for rule in blocked_paths:
        candidate = rule[:-1] if rule.endswith("/") else rule
        validate_repo_path(candidate, "blocked path")

    return Config(
        repository=repository,
        pr_number=positive_int("EXPECTED_PR_NUMBER"),
        expected_branch=expected_branch,
        expected_commit_sha=expected_commit_sha,
        expected_base_ref=require_env("EXPECTED_BASE_REF"),
        expected_author=os.environ.get("CODEX_PR_AUTHOR", "github-actions[bot]").strip(),
        branch_prefix=branch_prefix,
        allowed_workflow_paths=allowed_workflow_paths,
        blocked_paths=blocked_paths,
        max_changed_files=positive_int("CODEX_APPROVAL_MAX_CHANGED_FILES", "30"),
        timeout_seconds=positive_int("CODEX_APPROVAL_TIMEOUT_SECONDS", "120"),
        poll_seconds=positive_int("CODEX_APPROVAL_POLL_SECONDS", "5"),
        quiet_polls=positive_int("CODEX_APPROVAL_QUIET_POLLS", "3"),
        dry_run=parse_boolean("CODEX_APPROVAL_DRY_RUN"),
    )


def nested(value: dict[str, Any], *keys: str) -> Any:
    current: Any = value
    for key in keys:
        if not isinstance(current, dict) or key not in current:
            return None
        current = current[key]
    return current


def validate_pull_request(pr: dict[str, Any], config: Config) -> int:
    checks = {
        "number": (pr.get("number"), config.pr_number),
        "state": (pr.get("state"), "open"),
        "draft": (pr.get("draft"), False),
        "author": (nested(pr, "user", "login"), config.expected_author),
        "head repository": (nested(pr, "head", "repo", "full_name"), config.repository),
        "base repository": (nested(pr, "base", "repo", "full_name"), config.repository),
        "head branch": (nested(pr, "head", "ref"), config.expected_branch),
        "head SHA": (nested(pr, "head", "sha"), config.expected_commit_sha),
        "base branch": (nested(pr, "base", "ref"), config.expected_base_ref),
    }
    for label, (actual, expected) in checks.items():
        if actual != expected:
            raise ApprovalError(
                f"Pull request {config.pr_number} has unexpected {label}: "
                f"expected {expected!r}, got {actual!r}"
            )

    changed_files = pr.get("changed_files")
    if not isinstance(changed_files, int) or changed_files <= 0:
        raise ApprovalError("Pull request must contain at least one changed file")
    if changed_files > config.max_changed_files:
        raise ApprovalError(
            f"Pull request changes {changed_files} files; automatic approval allows at most "
            f"{config.max_changed_files}"
        )
    return changed_files


def path_is_blocked(path: str, blocked_paths: tuple[str, ...]) -> bool:
    for rule in blocked_paths:
        if rule.endswith("/") and path.startswith(rule):
            return True
        if path == rule:
            return True
    return False


def validate_changed_files(
    files: list[dict[str, Any]], expected_count: int, config: Config
) -> set[str]:
    if len(files) != expected_count:
        raise ApprovalError(
            f"GitHub returned {len(files)} changed-file records; expected {expected_count}"
        )

    paths: set[str] = set()
    for index, item in enumerate(files):
        filename = validate_repo_path(item.get("filename"), f"changed file {index}")
        paths.add(filename)
        previous_filename = item.get("previous_filename")
        if previous_filename is not None:
            paths.add(
                validate_repo_path(previous_filename, f"changed file {index} previous path")
            )

    blocked = sorted(path for path in paths if path_is_blocked(path, config.blocked_paths))
    if blocked:
        raise ApprovalError(
            "Automatic workflow approval is disabled because the PR changes protected paths: "
            + ", ".join(blocked)
        )
    return paths


def pull_request_is_attached(run: dict[str, Any], config: Config) -> bool:
    pull_requests = run.get("pull_requests")
    if not isinstance(pull_requests, list):
        return False

    expected_repo_url = f"https://api.github.com/repos/{config.repository}"

    def repository_matches(value: Any) -> bool:
        return isinstance(value, dict) and (
            value.get("full_name") == config.repository
            or value.get("url") == expected_repo_url
        )

    return any(
        isinstance(pr, dict)
        and pr.get("number") == config.pr_number
        and nested(pr, "head", "ref") == config.expected_branch
        and nested(pr, "head", "sha") == config.expected_commit_sha
        and repository_matches(nested(pr, "head", "repo"))
        and nested(pr, "base", "ref") == config.expected_base_ref
        and repository_matches(nested(pr, "base", "repo"))
        for pr in pull_requests
    )


def validate_workflow_run(run: dict[str, Any], config: Config) -> int:
    run_id = run.get("id")
    if not isinstance(run_id, int) or run_id <= 0:
        raise ApprovalError("Workflow run has an invalid ID")

    checks = {
        "event": (run.get("event"), "pull_request"),
        "head branch": (run.get("head_branch"), config.expected_branch),
        "head SHA": (run.get("head_sha"), config.expected_commit_sha),
        "head repository": (nested(run, "head_repository", "full_name"), config.repository),
        "actor": (nested(run, "actor", "login"), config.expected_author),
    }
    for label, (actual, expected) in checks.items():
        if actual != expected:
            raise ApprovalError(
                f"Workflow run {run_id} has unexpected {label}: "
                f"expected {expected!r}, got {actual!r}"
            )

    if (
        run.get("conclusion") == "action_required"
        and nested(run, "triggering_actor", "login") != config.expected_author
    ):
        raise ApprovalError(
            f"Workflow run {run_id} awaiting approval has unexpected triggering actor: "
            f"expected {config.expected_author!r}, got "
            f"{nested(run, 'triggering_actor', 'login')!r}"
        )

    workflow_path = run.get("path")
    if workflow_path not in config.allowed_workflow_paths:
        raise ApprovalError(
            f"Workflow run {run_id} uses non-allow-listed workflow {workflow_path!r}"
        )
    if not pull_request_is_attached(run, config):
        raise ApprovalError(
            f"Workflow run {run_id} is not attached to the expected pull request"
        )
    return run_id


def fetch_changed_files(
    client: GitHubClient, config: Config, expected_count: int
) -> list[dict[str, Any]]:
    files: list[dict[str, Any]] = []
    page = 1
    while len(files) < expected_count:
        endpoint = (
            f"repos/{config.repository}/pulls/{config.pr_number}/files?"
            + urlencode({"per_page": 100, "page": page})
        )
        batch = client.get_list(endpoint)
        files.extend(batch)
        if len(batch) < 100:
            break
        page += 1
    return files


def fetch_workflow_runs(client: GitHubClient, config: Config) -> list[dict[str, Any]]:
    endpoint = (
        f"repos/{config.repository}/actions/runs?"
        + urlencode(
            {
                "event": "pull_request",
                "head_sha": config.expected_commit_sha,
                "per_page": 100,
            }
        )
    )
    payload = client.get_json(endpoint)
    runs = payload.get("workflow_runs")
    if not isinstance(runs, list) or not all(isinstance(run, dict) for run in runs):
        raise ApprovalError("GitHub returned an invalid workflow-run list")
    if payload.get("total_count") != len(runs):
        raise ApprovalError("More than 100 matching workflow runs were returned; refusing approval")
    return runs


def append_github_output(name: str, value: str) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT")
    if output_path:
        with open(output_path, "a", encoding="utf-8") as output:
            output.write(f"{name}={value}\n")


def append_summary(config: Config, matched: int, approved: int) -> None:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        return
    mode = "Dry run" if config.dry_run else "Completed"
    with open(summary_path, "a", encoding="utf-8") as summary:
        summary.write("## Codex PR workflow approval\n\n")
        summary.write(f"- Result: {mode}\n")
        summary.write(f"- Pull request: #{config.pr_number}\n")
        summary.write(f"- Commit: `{config.expected_commit_sha}`\n")
        summary.write(f"- Matching workflow runs: {matched}\n")
        summary.write(f"- Approved workflow runs: {approved}\n")


def approve_workflows(
    config: Config,
    client: GitHubClient,
    *,
    monotonic: Callable[[], float] = time.monotonic,
    sleep: Callable[[float], None] = time.sleep,
) -> tuple[int, int]:
    pr_endpoint = f"repos/{config.repository}/pulls/{config.pr_number}"
    pr = client.get_json(pr_endpoint)
    expected_file_count = validate_pull_request(pr, config)
    files = fetch_changed_files(client, config, expected_file_count)
    changed_paths = validate_changed_files(files, expected_file_count, config)
    print(
        f"Validated Codex PR #{config.pr_number} at {config.expected_commit_sha} "
        f"with {len(changed_paths)} path(s)."
    )

    deadline = monotonic() + config.timeout_seconds
    observed_ids: set[int] = set()
    approved_ids: set[int] = set()
    quiet_count = 0

    while True:
        validate_pull_request(client.get_json(pr_endpoint), config)
        runs = fetch_workflow_runs(client, config)
        validated_runs: list[tuple[int, dict[str, Any]]] = []
        for run in runs:
            validated_runs.append((validate_workflow_run(run, config), run))

        current_ids = {run_id for run_id, _ in validated_runs}
        new_ids = current_ids - observed_ids
        observed_ids.update(current_ids)

        newly_approved = 0
        for run_id, run in validated_runs:
            if run.get("conclusion") != "action_required" or run_id in approved_ids:
                continue
            validate_pull_request(client.get_json(pr_endpoint), config)
            workflow_name = run.get("name", run.get("path", "workflow"))
            if config.dry_run:
                print(f"Would approve {workflow_name} run {run_id}.")
            else:
                client.approve_run(config.repository, run_id)
                print(f"Approved {workflow_name} run {run_id}.")
            approved_ids.add(run_id)
            newly_approved += 1

        if current_ids and not new_ids and newly_approved == 0:
            quiet_count += 1
        else:
            quiet_count = 0

        if current_ids and quiet_count >= config.quiet_polls:
            append_github_output("matched_count", str(len(observed_ids)))
            append_github_output("approved_count", str(len(approved_ids)))
            append_summary(config, len(observed_ids), len(approved_ids))
            return len(observed_ids), len(approved_ids)

        if monotonic() >= deadline:
            if not observed_ids:
                raise ApprovalError("No pull-request workflow runs appeared before the timeout")
            raise ApprovalError("Pull-request workflow runs did not settle before the timeout")
        sleep(config.poll_seconds)


def main() -> int:
    try:
        config = load_config()
        matched, approved = approve_workflows(config, GitHubClient())
        print(f"Validated {matched} workflow run(s); approved {approved}.")
        return 0
    except ApprovalError as error:
        print(f"::error::{error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
