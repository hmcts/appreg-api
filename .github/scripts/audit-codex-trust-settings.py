#!/usr/bin/env python3
"""Read-only GitHub control-plane audit. Never reads secret values or changes settings."""
import argparse
import json
import subprocess
from urllib.parse import quote

REPOSITORIES = ("hmcts/appreg-api", "hmcts/appreg-frontend")
ENVIRONMENTS = {
    "codex-model": {"CODEX_OPENAI_API_KEY"},
    "codex-publisher": {"CODEX_GITHUB_APP_PRIVATE_KEY", "CODEX_JIRA_PR_NOTIFY_URL"},
    "codex-status": {"CODEX_SONAR_TOKEN"},
}
WORKFLOWS = (
    "codex_jira_dispatch.yml",
    "codex_pr_review_feedback.yml",
    "codex_merge_conflict_resolution.yml",
    "codex_runner_smoke.yml",
)


def github(path):
    result = subprocess.run(
        ["gh", "api", "--method", "GET", "--paginate", "--slurp", path],
        capture_output=True, text=True, timeout=60, check=False,
    )
    if result.returncode:
        raise RuntimeError(f"Cannot verify GET {path}; check GitHub administration access.")
    pages = json.loads(result.stdout)
    if not isinstance(pages, list) or not pages:
        raise RuntimeError(f"Unexpected response for GET {path}")
    merged = dict(pages[0])
    for page in pages[1:]:
        for key, value in page.items():
            if isinstance(value, list):
                merged.setdefault(key, []).extend(value)
    return merged


def audit(repository, runner_group, api=github):
    errors = []
    repo = api(f"repos/{repository}")
    branch = repo["default_branch"]
    secrets = set().union(*ENVIRONMENTS.values())
    for scope in ("secrets", "organization-secrets"):
        available = api(f"repos/{repository}/actions/{scope}")["secrets"]
        exposed = secrets.intersection(item["name"] for item in available)
        if exposed:
            errors.append(f"Remove repository-accessible {scope}: {', '.join(sorted(exposed))}.")
    settings = api(f"repos/{repository}/actions/permissions/workflow")
    if settings.get("default_workflow_permissions") != "read":
        errors.append("Set the default GITHUB_TOKEN permissions to read.")
    if settings.get("can_approve_pull_request_reviews") is not False:
        errors.append("Disable GITHUB_TOKEN pull-request approval.")
    protection = api(f"repos/{repository}/branches/{quote(branch, safe='')}/protection")
    reviews = protection.get("required_pull_request_reviews") or {}
    if reviews.get("required_approving_review_count", 0) < 1:
        errors.append("Require a reviewed change before workflow code enters the default branch.")
    if not (reviews.get("dismiss_stale_reviews") is True or
            reviews.get("require_last_push_approval") is True):
        errors.append("Require stale review dismissal or approval of the latest push on the default branch.")

    environments = api(f"repos/{repository}/environments")["environments"]
    by_name = {item["name"]: item for item in environments}
    for name, required in ENVIRONMENTS.items():
        environment = by_name.get(name)
        if environment is None:
            errors.append(f"Create protected environment {name}.")
            continue
        policy = environment.get("deployment_branch_policy")
        if policy != {"protected_branches": False, "custom_branch_policies": True}:
            errors.append(f"{name}: select explicit deployment branch policies.")
        policies = api(f"repos/{repository}/environments/{name}/deployment-branch-policies")["branch_policies"]
        if [(item.get("name"), item.get("type")) for item in policies] != [(branch, "branch")]:
            errors.append(f"{name}: allow only the {branch} branch, with no tag or PR-ref policies.")
        names = {item["name"] for item in api(f"repos/{repository}/environments/{name}/secrets")["secrets"]}
        if names != required:
            errors.append(f"{name}: expected only {', '.join(sorted(required))}.")
    # A duplicate in any other environment is an alternative path to the same credential.
    for name in by_name.keys() - ENVIRONMENTS.keys():
        names = {item["name"] for item in api(f"repos/{repository}/environments/{quote(name, safe='')}/secrets")["secrets"]}
        if names.intersection(secrets):
            errors.append(f"Remove Codex credential duplicates from environment {name}.")

    try:
        groups = api("orgs/hmcts/actions/runner-groups")["runner_groups"]
    except RuntimeError as error:
        errors.append(str(error))
        return errors
    matches = [group for group in groups if group["name"] == runner_group]
    if len(matches) != 1:
        errors.append(f"Expected one organisation runner group named {runner_group}.")
    else:
        group = matches[0]
        if repo.get("visibility") == "public" and group.get("allows_public_repositories") is not True:
            errors.append("Runner group must allow the selected public Apps Reg repositories.")
        if group.get("visibility") != "selected" or group.get("restricted_to_workflows") is not True:
            errors.append("Runner group must restrict both repositories and workflows.")
        allowed = {f"{repo}/.github/workflows/{name}@refs/heads/master" for repo in REPOSITORIES for name in WORKFLOWS}
        required = {f"{repository}/.github/workflows/{name}@refs/heads/{branch}" for name in WORKFLOWS}
        selected = set(group.get("selected_workflows", []))
        if not required <= selected or not selected <= allowed:
            errors.append("Runner group must allow only the exact Apps Reg default-branch Codex workflow paths.")
        repositories = api(f"orgs/hmcts/actions/runner-groups/{group['id']}/repositories")["repositories"]
        names = {item["full_name"] for item in repositories}
        if repository not in names or not names <= set(REPOSITORIES):
            errors.append("Runner group repository access must be limited to the Apps Reg repositories.")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True, choices=REPOSITORIES)
    parser.add_argument("--runner-group", required=True)
    arguments = parser.parse_args()
    try:
        errors = audit(arguments.repository, arguments.runner_group)
    except (RuntimeError, subprocess.TimeoutExpired, ValueError, KeyError, TypeError) as error:
        print(f"UNVERIFIED: {error}")
        return 1
    if errors:
        print("\n".join(f"FAIL: {error}" for error in errors))
        return 1
    print("GitHub policy checks passed. Separately confirm ARC group registration and removal of legacy repository-scoped scale sets.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
