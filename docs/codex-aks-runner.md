# Codex AKS Runner

This repository is wired for the Applications Register Codex pilot using GitHub Actions Runner Controller on AKS.

## Flow

```text
Jira Automation
  -> Azure Function webhook
  -> GitHub workflow_dispatch: codex_jira_dispatch.yml
  -> ARC runner scale set: codex-pilot-azure-aks
  -> Codex creates a codex/* branch and pull request
  -> Jira issue transitions to Dev Review
```

The flow is not tied to one Jira board. Each board needs its own Automation rule that sends the same webhook payload fields, and the target Jira transition must exist in that board's workflow.

## Workflows

- `.github/workflows/codex_runner_smoke.yml`: validates the AKS runner can start, authenticate Codex, create a branch, commit, and push.
- `.github/workflows/codex_jira_dispatch.yml`: receives Jira fields through `workflow_dispatch`, runs Codex, verifies the result, opens a PR, and transitions Jira.
- `.github/workflows/codex_pr_review_feedback.yml`: sends PR review feedback back to Codex for follow-up changes on the same `codex/*` branch.

All Codex workflows target:

```yaml
runs-on: codex-pilot-azure-aks
```

## Required Repository Secrets

- `OPENAI_API_KEY`: used by the runner to authenticate Codex non-interactively.
- `JIRA_USER_EMAIL`: Jira account email used to transition and comment on issues.
- `JIRA_API_TOKEN`: Jira API token for that account.

## Optional Repository Variables

- `CODEX_REVIEWER`: GitHub username to request for review on Codex PRs.
- `JIRA_BASE_URL`: defaults to `https://justice-ai-coe.atlassian.net`.
- `JIRA_TARGET_STATUS`: defaults to `Dev Review`.

## Local Verification

Use fast mode before pushing ordinary changes:

```bash
./bin/codex-local-pipeline.sh fast
```

Use full mode when the change needs Docker/Testcontainers-backed verification:

```bash
./bin/codex-local-pipeline.sh full
```

The AKS runner image currently supports fast smoke/unit-test runs. Full Docker/Testcontainers verification needs Docker-in-Docker, Kubernetes container mode, or another approved Docker strategy on the runner scale set.
