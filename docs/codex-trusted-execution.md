# Codex trusted execution

Codex runs in this repository through the shared HMCTS
[codex-agent-workflows](https://github.com/hmcts/codex-agent-workflows), pinned to a full commit SHA. This page
covers the controls this repository relies on and how to check them. The shared
repository documents the workflows themselves.

The enforceable controls are environment secret restrictions and organisation
runner-group workflow restrictions, configured by administrators outside the
repository checkout. The shared credential safety gate and the workflow guards
are regression checks, not protection against a writer who also modifies those
checks.

## Protect credentials

Two environments in each Apps Reg repository hold the Codex secrets:

- `codex-model`: `CODEX_OPENAI_API_KEY`.
- `codex-publisher`: `CODEX_GITHUB_APP_PRIVATE_KEY` and `CODEX_JIRA_PR_NOTIFY_URL`.

The thin callers map all three secrets. The shared workflows accept the empty
values the callers pass and read each secret only inside a job that declares
its environment.

For each environment, select **Selected branches and tags** and add only a
**branch** rule named **master**. Do not add wildcard, tag, feature-branch or
refs/pull rules. Default-branch workflow changes must pass the team's normal
review and release process. Require at least one approval and either dismiss
stale approvals after new commits or require approval of the latest push.

An environment name in YAML is not sufficient: GitHub can automatically create
an unprotected environment, and repository or organisation secrets remain
available when a workflow omits that environment. Keep no fallback copies of
these secrets as repository or organisation secrets, and do not expose the
publisher private key or Jira callback URL to the model environment.

Keep `CODEX_GITHUB_APP_CLIENT_ID` as a repository variable. Keep default
workflow permissions read-only and GitHub Actions PR approval disabled.

## Restrict runner scheduling

Model jobs run in the `appreg-codex` organisation runner group, each on its
repository's own label:

- appreg-api: `codex-pilot-azure-aks`
- appreg-frontend: `codex-frontend-azure-aks`

Set group repository access to only hmcts/appreg-api and hmcts/appreg-frontend.
Restrict workflow access to the shared model workflows at the release SHA the
callers pin, plus the two runner smoke tests:

```text
hmcts/codex-agent-workflows/.github/workflows/codex-plan.yml@<release SHA>
hmcts/codex-agent-workflows/.github/workflows/codex-generate.yml@<release SHA>
hmcts/codex-agent-workflows/.github/workflows/codex-repair-round.yml@<release SHA>
hmcts/codex-agent-workflows/.github/workflows/codex-post-repair.yml@<release SHA>
hmcts/codex-agent-workflows/.github/workflows/codex-review-generate.yml@<release SHA>
hmcts/codex-agent-workflows/.github/workflows/codex-review-repair.yml@<release SHA>
hmcts/codex-agent-workflows/.github/workflows/codex-review-repair-round.yml@<release SHA>
hmcts/appreg-api/.github/workflows/codex_runner_smoke.yml@refs/heads/master
hmcts/appreg-frontend/.github/workflows/codex_runner_smoke.yml@refs/heads/master
```

The first shared release is `84cd0d5adffbc9d2c09a7dddff6a0dd25485db10`. A later release needs the same seven
entries at its SHA before its pin PRs merge. Keep scale-to-zero, ephemeral
runners and credential-free verification. Publisher and verification jobs run
on fresh GitHub-hosted compute.

## Developer journey

- The Azure Function dispatches `codex_jira_dispatch.yml` on `master` for a
  `codex-ready` ARCPOC ticket. A run from any other branch cannot obtain the
  environment secrets.
- Plans follow the strict policy: no `.github`, build or dependency changes,
  and no high-risk or cross-system plans. A blocked plan stops before
  implementation.
- Only verified work is published, and Jira hears only `pr-created`. A required
  status reporting that the commit cannot be built gets one repair attempt.
- For PR feedback, add review comments normally, then post exactly
  `/codex-review` in the PR's Conversation tab. The command author must have
  write access. Codex addresses every change-request or comment review of the
  current head submitted by the time of the command, from reviewers with
  write, maintain or admin permission. Feedback a reviewer has since approved,
  outdated inline comments and feedback posted or edited after the command are
  left out. Feedback is capped at 64 KiB. If the PR head moves during
  collection, post a fresh command.
- The `/codex-resolve-conflicts` command is retired.

## Verification

- `./bin/codex-local-pipeline.sh checks-only --no-fetch` runs this repository's
  guardrails, and Jenkins runs it on every PR. The shared workflows run
  fast mode to verify each Codex change without credentials.
- Before merging a change to this repository's workflows, run the credential
  safety gate from a checkout of codex-agent-workflows at the pinned release:
  `ruby .github/scripts/check-codex-pr-safety.rb --repository-root <this checkout> --trusted-repository-root <this checkout>`.
  The shared workflows also run it on every Codex patch and before every
  publication.
- New releases arrive as pin PRs from the shared repository's
  `Update caller workflow pins` workflow, dispatched with `callers: appreg`.
  Review them like any other change.

Use a non-sensitive canary credential in an isolated test repository or
environment for negative tests: feature, tag and PR refs must not obtain it.
Verify denied workflows never get an `appreg-codex` runner. Do not use real
credential disclosure, production mutations or an admin merge bypass as tests.
A failed cutover must pause the agent; do not restore unrestricted secrets or
runner registration merely to get a run to start.

## Scope and references

Other repository-accessible credentials, Azure federated trust and unrelated
write-token jobs need their own inventory and protection. Do not close a
broader repository-wide finding solely because the Codex checks pass.

- [Shared workflow caller contract](https://github.com/hmcts/codex-agent-workflows/blob/main/docs/caller-contract.md)
- [Apps Reg rollout runbook](https://github.com/hmcts/codex-agent-workflows/blob/main/docs/appreg-rollout.md)
- [GitHub environment protection](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)
- [GitHub selected-workflow runner access](https://docs.github.com/en/enterprise-cloud%40latest/actions/how-tos/manage-runners/self-hosted-runners/manage-access)
