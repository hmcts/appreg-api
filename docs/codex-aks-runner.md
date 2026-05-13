# Codex AKS Runner

This repository is wired for the Applications Register Codex pilot using GitHub Actions Runner Controller on AKS.

## Flow

```text
Jira Automation
  -> Azure Function webhook
  -> GitHub workflow_dispatch: codex_jira_dispatch.yml
  -> ARC runner scale set: codex-pilot-azure-aks
  -> Codex creates a codex/* branch and pull request
  -> Azure Function notifies Jira Automation
  -> Jira Automation transitions the issue to Dev Review
```

The current target is HMCTS Jira project `ARCPOC` on board `3010`:

```text
https://tools.hmcts.net/jira/secure/RapidBoard.jspa?rapidView=3010&projectKey=ARCPOC
```

The flow is not tied to one Jira board. Each board needs its own Automation rules that send and receive the same webhook payload fields, and the target Jira transition must exist in that board's workflow.

## Workflows

- `.github/workflows/codex_runner_smoke.yml`: validates the AKS runner can start, authenticate Codex, create a branch, commit, and push.
- `.github/workflows/codex_jira_dispatch.yml`: receives Jira fields through `workflow_dispatch`, runs Codex, verifies the result, opens a PR, and notifies Azure so Jira Automation can transition Jira.
- `.github/workflows/codex_pr_review_feedback.yml`: sends PR review feedback back to Codex for follow-up changes on the same `codex/*` branch.

All Codex workflows target:

```yaml
runs-on: codex-pilot-azure-aks
```

## Required Repository Secrets

- `OPENAI_API_KEY`: used by the runner to authenticate Codex non-interactively.
- `CODEX_JIRA_PR_NOTIFY_URL`: Azure Function URL, including its function key, for the PR-created notification endpoint.

## Optional Repository Variables

- `CODEX_REVIEWER`: GitHub username to request for review on Codex PRs.
- `CODEX_JIRA_PR_NOTIFY_TIMEOUT_SECONDS`: timeout for notifying Azure after PR creation. Defaults to `10`.

## Jira Automation

Create an incoming-webhook rule in HMCTS Jira project `ARCPOC`.

The rule should:

- Accept the Azure Function payload after a Codex PR is opened.
- Find or act on `{{webhookData.issueKey}}`.
- Transition the issue to `Dev Review`.
- Add a comment containing `{{webhookData.prUrl}}`.

This avoids storing `JIRA_USER_EMAIL` or `JIRA_API_TOKEN` in GitHub.

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
