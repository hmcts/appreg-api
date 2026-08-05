# Codex AKS Runner

This repository is wired for the Applications Register Codex pilot using GitHub Actions Runner Controller on AKS.

## Flow

```text
Jira Automation
  -> Azure Function webhook
  -> GitHub workflow_dispatch: codex_jira_dispatch.yml
  -> ARC runner scale set: codex-pilot-azure-aks
  -> read-only Codex planning and trusted plan validation
  -> optional human approval for high-risk or cross-system plans
  -> Codex implementation and isolated verification
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
- `.github/workflows/codex_jira_dispatch.yml`: receives Jira fields through `workflow_dispatch`, plans the implementation, validates and gates the plan, runs Codex, verifies the result, opens a PR, and notifies Azure so Jira Automation can transition Jira.
- `.github/workflows/codex_pr_review_feedback.yml`: sends PR review feedback back to Codex for follow-up changes on the same `codex/*` branch.

All Codex workflows target:

```yaml
runs-on: codex-pilot-azure-aks
```

## Codex Action authentication

The API key is stored as the GitHub Actions secret `CODEX_OPENAI_API_KEY` and
is supplied only to the pinned official `openai/codex-action`. The action keeps
the real key behind a local Responses API proxy; Codex runs as the dedicated
unprivileged `codex` user and never receives the key in its environment.
The workflows also pin the Codex CLI and proxy to `0.146.0`; update that
version consistently across all invocations only after both repository smoke
workflows pass.

Every workspace-writing Codex job ends with the official Action. Codex returns
a schema-validated, size-bounded gzip/base64 patch through the Action's
`final-message` job output; no privileged collector, Git command, or artifact
action runs against the model-writable checkout afterward. A fresh dependent
job checks out the exact trusted commit recorded by the Action job, validates
and materialises that untrusted patch, and passes it to the existing
credential-free verification and trusted publication stages. Before loading
untrusted repository content, the runner captures a read-only patch exporter;
it builds the patch with a temporary Git index and object store because the
`:workspace` profile keeps the real `.git` metadata read-only. The report-only
parity workflow follows the same final-step boundary and gives its Jira
notification secret only to a fresh dependent job.

The regional Responses endpoint is
`https://eu.api.openai.com/v1/responses`.

## Planning and approval

Each Jira dispatch starts with a separate read-only Codex invocation. The
planner intentionally omits the `model` and `effort` inputs so Codex uses its
defaults, while the Action commit, CLI version, regional endpoint,
unprivileged user and `:read-only` permission profile remain pinned. The
planner returns structured JSON containing the root cause, scope decision,
alternatives, implementation paths, tests, acceptance criteria, risks,
assumptions and blockers.

A fresh GitHub-hosted job validates and size-limits the untrusted JSON, rejects
protected automation paths, and stores a canonical plan plus its SHA-256 hash
as the `codex-jira-plan` artefact. Tickets that are not ready stop before any
workspace-writing model invocation. Plans marked high risk or cross-system
wait on the `codex-plan-approval` GitHub environment.

Repository administrators must create that environment and configure required
reviewers before enabling this workflow. Without required reviewers, GitHub
does not provide a human approval gate. Reviewers inspect the validator job
summary or downloaded plan artefact before approving.

Implementation checks out the exact commit inspected by the planner and uses
`gpt-5.6-sol` with `ultra` effort. The validated plan is included in the
implementation prompt and the generated PR body. Verification repairs reuse
the original plan; they stop and request a new planning run when repository
evidence invalidates the planned architecture or scope.

## Cost and usage monitoring

The official Action does not expose its token event stream to trusted workflow
collectors. Empty per-run token artefacts are therefore not emitted: a file with
`usageAvailable=false` is not cost telemetry and must not be used for reporting.
This is an accepted limitation of the credential-proxy migration.

Cost governance uses the OpenAI provider control plane instead:

- Run the Apps Reg agent from a dedicated OpenAI project. Use separate project
  API keys for each repository when repository-level attribution is required.
- Keep the organisation Admin API key in the CGI AI team's central monitoring
  service or Key Vault. It must not be stored in these repositories, GitHub
  Actions, or AKS.
- Export daily usage from
  `GET /v1/organization/usage/completions`, filtered by project or API key and
  grouped by model. Export daily spend from
  `GET /v1/organization/costs`, filtered by the same project.
- Retain the raw daily buckets, publish a monthly cost report, and alert against
  the agreed project budget. GitHub run history remains the source for run counts
  and operational failures; provider data is the source for tokens and cost.

Before production rollout, record the OpenAI project ID, reporting owner,
collection location, retention period, and spend-alert threshold. Until the
central export is operational, the named owner must review the OpenAI
organisation usage dashboard at least weekly. See OpenAI's
[Usage and Costs API guide](https://developers.openai.com/cookbook/examples/completions_usage_api)
and [organisation usage dashboard](https://platform.openai.com/settings/organization/usage).

## Required Repository Secrets

- `CODEX_OPENAI_API_KEY`: OpenAI API key used only by the official Codex Action proxy.
- `CODEX_JIRA_PR_NOTIFY_URL`: Azure Function URL, including its function key, for the PR-created notification endpoint.
- `CODEX_JIRA_PARITY_NOTIFY_URL`: Azure Function URL, including its function key, for parity-result notifications.

## Optional Repository Variables

- `CODEX_REVIEWER`: GitHub username to request for review on Codex PRs.
- `CODEX_JIRA_PR_NOTIFY_TIMEOUT_SECONDS`: timeout for notifying Azure after PR creation. Defaults to `10`.
- `CODEX_JIRA_PARITY_NOTIFY_TIMEOUT_SECONDS`: timeout for notifying Azure after a parity check. Defaults to `10`.

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
