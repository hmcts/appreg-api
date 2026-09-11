# Codex trusted execution rollout

This change addresses the Codex workflow paths in ARCPOC-1674. A repository writer
can change a workflow on a feature branch before dispatching it. Checking out
master later, or adding an if condition inside that workflow, does not establish
a security boundary.

The enforceable controls are environment secret restrictions and organisation
runner-group workflow restrictions, configured by administrators outside the
repository checkout. The workflow guards and local tests are regression checks,
not protection against a writer who also modifies those checks.

## Do not activate before the control-plane migration

The PR alone does not close the finding. Do not merge until the environments,
secret migration and runner-group migration below are ready for a coordinated
cutover. No live settings are changed by the scripts in this PR. The publisher
App identity, model choices and human merge approval are unchanged.

### Protect credentials

Create these three environments in each Apps Reg repository:

| Environment | Secrets stored only in this environment |
| --- | --- |
| codex-model | CODEX_OPENAI_API_KEY |
| codex-publisher | CODEX_GITHUB_APP_PRIVATE_KEY, CODEX_JIRA_PR_NOTIFY_URL |
| codex-status | CODEX_SONAR_TOKEN |

For each environment, select **Selected branches and tags** and add only a
**branch** rule named **master**. Do not add wildcard, tag, feature-branch or
refs/pull rules. No per-run reviewer gate is required for the existing small-bug
journey. Default-branch workflow changes must still pass the team's normal
review and release process.

Re-enter the credentials from the approved secret source into their respective
environments. GitHub cannot return a stored Actions secret's plaintext for a
move. Validate the destination configuration, then remove the same credentials
from repository secrets, any organisation-secret access granted to these
repositories, and other environments. Retain a recovery copy only in the
approved secret store, not as an unrestricted GitHub fallback.

An environment name in YAML is not sufficient: GitHub can automatically create
an unprotected environment, and repository/organisation secrets remain available
when a workflow omits that environment. Remove all fallback copies before
claiming the restriction is effective. Do not expose the publisher private key
or Jira callback URL to the model environment.

Keep CODEX_GITHUB_APP_CLIENT_ID as a repository variable. Change default workflow
permissions to read-only and disable GitHub Actions PR approval. These settings
reduce ambient token privileges; they do not prevent a writer from requesting
explicit permissions in a new workflow.

### Restrict runner scheduling

Repository-scoped ARC registration cannot enforce an organisation runner-group
workflow allow-list. Migrate the two Apps Reg scale sets to organisation-scoped
registration in the **appreg-codex** runner group, keeping their distinct labels:

- appreg-api: codex-pilot-azure-aks
- appreg-frontend: codex-frontend-azure-aks

An HMCTS organisation owner must provision the group and approve a registration
identity with the required organisation runner-management permission. Repository
Administration permission alone is not sufficient for organisation registration.
Do not broaden the publishing App's permission as an implicit part of this PR;
the platform team must approve the ARC registration identity.

Set group repository access to only hmcts/appreg-api and hmcts/appreg-frontend.
Allow those selected public repositories, then restrict workflow access to these
eight exact paths:

```text
hmcts/appreg-api/.github/workflows/codex_jira_dispatch.yml@refs/heads/master
hmcts/appreg-api/.github/workflows/codex_pr_review_feedback.yml@refs/heads/master
hmcts/appreg-api/.github/workflows/codex_merge_conflict_resolution.yml@refs/heads/master
hmcts/appreg-api/.github/workflows/codex_runner_smoke.yml@refs/heads/master
hmcts/appreg-frontend/.github/workflows/codex_jira_dispatch.yml@refs/heads/master
hmcts/appreg-frontend/.github/workflows/codex_pr_review_feedback.yml@refs/heads/master
hmcts/appreg-frontend/.github/workflows/codex_merge_conflict_resolution.yml@refs/heads/master
hmcts/appreg-frontend/.github/workflows/codex_runner_smoke.yml@refs/heads/master
```

Register ARC against https://github.com/hmcts with runnerGroup appreg-codex.
Keep scale-to-zero, ephemeral runners and credential-free verification. Retire
the old repository-scoped scale sets; leaving them registered preserves an
alternative route to the AKS runners. Do not change the seven Juror scale sets
as part of this Apps Reg change.

The model jobs now explicitly select the group and repository-specific label.
Publisher and verification jobs run on fresh GitHub-hosted compute. Keep runner
network isolation, service-account permissions and absence of mounted secrets
under platform review; scheduling policy is not a substitute for pod isolation.

## Developer journey

Jira dispatch and the manual runner smoke workflow must run from master.
Selecting a feature branch is deliberately unsupported for credentialed runs.
The entry job rejects a mismatched branch or workflow ref before scheduling
normal downstream work; the server-side controls still protect against modified
copies that remove that guard.

For PR feedback, add review comments normally, then post /codex-review in the
PR's main Conversation tab. The command must be posted by a repository writer.
Putting it inside an inline review or review submission no longer starts the
agent. The existing collector still reads review feedback and updates the same
PR branch. /codex-resolve-conflicts continues to use conversation comments.

Authentication smoke ends with the model Action. A separate credential-free
job validates its structured message, and a separate GitHub-hosted publisher job
tests branch creation. No Git command runs after the model in its workspace.

## Verification and cutover

Run local regression checks from the reviewed checkout:

```bash
ruby .github/scripts/check-codex-workflow-trust.rb
python3 .github/scripts/test-codex-workflow-trust.py
python3 .github/scripts/test-audit-codex-trust-settings.py
./bin/codex-local-pipeline.sh checks-only --no-fetch
```

An administrator then runs the read-only metadata audit for each repository:

```bash
python3 .github/scripts/audit-codex-trust-settings.py \
  --repository hmcts/appreg-api --runner-group appreg-codex
python3 .github/scripts/audit-codex-trust-settings.py \
  --repository hmcts/appreg-frontend --runner-group appreg-codex
```

The audit fails on missing/inaccessible settings, unrestricted or inherited
Codex secrets, tag/feature-branch policies, unreviewed default-branch protection
and over-broad runner-group policies. It never reads secret values or changes
GitHub settings. Independently verify the live ARC registration, the absence of
legacy scale sets and other accessible unrestricted runner groups.

In an agreed change window, pause dispatch, drain active runs, migrate the
control plane, merge through normal review and resume dispatch. Run an
authentication smoke test and one small Jira-to-PR test in each repository.
Confirm bot authorship, Jira attribution, required CI and a /codex-review update.

Use a non-sensitive canary credential in an isolated test repository/environment
for negative tests: feature/tag/PR refs must not obtain it even when they omit
the guard, select the protected environment or change the workflow filename.
Verify denied workflows never allocate an appreg-codex runner. Do not use real
credential disclosure, production mutations or an admin merge bypass as tests.
A failed cutover must pause the agent; do not restore unrestricted secrets or
runner registration merely to get a run to start.

## Scope and references

This PR does not migrate the shared Juror runtime, change Jenkins release
requirements, or fix unrelated Azure redirect/ADO/branch-maintenance workflows.
Other repository-accessible credentials, Azure federated trust and unrelated
write-token jobs require their own inventory and protection. Do not close a
broader repository-wide finding solely because these Codex checks pass.

- [GitHub environment protection](https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments)
- [GitHub selected-workflow runner access](https://docs.github.com/en/enterprise-cloud%40latest/actions/how-tos/manage-runners/self-hosted-runners/manage-access)
