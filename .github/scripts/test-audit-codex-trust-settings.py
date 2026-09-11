#!/usr/bin/env python3
"""Test server-policy validation with metadata-only fixtures."""
import copy
import importlib.util
import unittest
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("audit", Path(__file__).with_name("audit-codex-trust-settings.py"))
AUDIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUDIT)


class SettingsAuditTests(unittest.TestCase):
    def setUp(self):
        self.repository = "hmcts/appreg-api"
        prefix = f"repos/{self.repository}"
        self.responses = {
            prefix: {"default_branch": "master", "visibility": "public"},
            f"{prefix}/actions/secrets": {"secrets": []},
            f"{prefix}/actions/organization-secrets": {"secrets": []},
            f"{prefix}/actions/permissions/workflow": {
                "default_workflow_permissions": "read", "can_approve_pull_request_reviews": False,
            },
            f"{prefix}/branches/master/protection": {
                "required_pull_request_reviews": {"required_approving_review_count": 1},
            },
            f"{prefix}/environments": {"environments": [
                {"name": name, "deployment_branch_policy": {
                    "protected_branches": False, "custom_branch_policies": True,
                }} for name in AUDIT.ENVIRONMENTS
            ]},
            "orgs/hmcts/actions/runner-groups": {"runner_groups": [{
                "id": 1, "name": "appreg-codex", "visibility": "selected",
                "allows_public_repositories": True,
                "restricted_to_workflows": True,
                "selected_workflows": [
                    f"{self.repository}/.github/workflows/{name}@refs/heads/master" for name in AUDIT.WORKFLOWS
                ],
            }]},
            "orgs/hmcts/actions/runner-groups/1/repositories": {
                "repositories": [{"full_name": self.repository}],
            },
        }
        for name, secrets in AUDIT.ENVIRONMENTS.items():
            self.responses[f"{prefix}/environments/{name}/deployment-branch-policies"] = {
                "branch_policies": [{"name": "master", "type": "branch"}],
            }
            self.responses[f"{prefix}/environments/{name}/secrets"] = {
                "secrets": [{"name": secret} for secret in sorted(secrets)],
            }

    def audit(self):
        return AUDIT.audit(self.repository, "appreg-codex", lambda path: copy.deepcopy(self.responses[path]))

    def test_complete_policy(self):
        self.assertEqual(self.audit(), [])

    def test_repository_and_inherited_duplicates_rejected(self):
        for scope in ("secrets", "organization-secrets"):
            with self.subTest(scope=scope):
                path = f"repos/{self.repository}/actions/{scope}"
                self.responses[path]["secrets"] = [{"name": "CODEX_OPENAI_API_KEY"}]
                self.assertTrue(any("Remove repository-accessible" in error for error in self.audit()))
                self.responses[path]["secrets"] = []

    def test_feature_branch_and_pull_ref_and_tag_policies_rejected(self):
        for name, kind in (("*", "branch"), ("refs/pull/*/merge", "branch"), ("master", "tag")):
            with self.subTest(name=name, kind=kind):
                path = f"repos/{self.repository}/environments/codex-model/deployment-branch-policies"
                self.responses[path]["branch_policies"] = [{"name": name, "type": kind}]
                self.assertTrue(any("allow only" in error for error in self.audit()))

    def test_missing_environment_rejected(self):
        self.responses[f"repos/{self.repository}/environments"]["environments"].pop()
        self.assertTrue(any("Create protected environment" in error for error in self.audit()))

    def test_mixed_model_and_publisher_credentials_rejected(self):
        self.responses[f"repos/{self.repository}/environments/codex-model/secrets"]["secrets"].append(
            {"name": "CODEX_GITHUB_APP_PRIVATE_KEY"})
        self.assertTrue(any("expected only" in error for error in self.audit()))

    def test_alternative_environment_duplicate_rejected(self):
        prefix = f"repos/{self.repository}/environments"
        self.responses[prefix]["environments"].append({"name": "unrestricted"})
        self.responses[f"{prefix}/unrestricted/secrets"] = {"secrets": [{"name": "CODEX_OPENAI_API_KEY"}]}
        self.assertTrue(any("duplicates from environment" in error for error in self.audit()))

    def test_write_default_token_and_self_approval_rejected(self):
        self.responses[f"repos/{self.repository}/actions/permissions/workflow"] = {
            "default_workflow_permissions": "write", "can_approve_pull_request_reviews": True}
        self.assertEqual(sum("GITHUB_TOKEN" in error for error in self.audit()), 2)

    def test_unreviewed_default_branch_rejected(self):
        self.responses[f"repos/{self.repository}/branches/master/protection"] = {}
        self.assertTrue(any("reviewed change" in error for error in self.audit()))

    def test_missing_runner_group_rejected(self):
        self.responses["orgs/hmcts/actions/runner-groups"]["runner_groups"] = []
        self.assertTrue(any("Expected one" in error for error in self.audit()))

    def test_public_repository_must_be_schedulable(self):
        group = self.responses["orgs/hmcts/actions/runner-groups"]["runner_groups"][0]
        group["allows_public_repositories"] = False
        self.assertTrue(any("selected public" in error for error in self.audit()))

    def test_extra_feature_branch_runner_access_rejected(self):
        group = self.responses["orgs/hmcts/actions/runner-groups"]["runner_groups"][0]
        group["selected_workflows"].append(f"{self.repository}/.github/workflows/codex_runner_smoke.yml@refs/heads/test")
        self.assertTrue(any("exact Apps Reg" in error for error in self.audit()))

    def test_unrestricted_runner_access_rejected(self):
        group = self.responses["orgs/hmcts/actions/runner-groups"]["runner_groups"][0]
        group["restricted_to_workflows"] = False
        self.assertTrue(any("restrict both" in error for error in self.audit()))

    def test_extra_repository_runner_access_rejected(self):
        self.responses["orgs/hmcts/actions/runner-groups/1/repositories"]["repositories"].append(
            {"full_name": "hmcts/unrelated"})
        self.assertTrue(any("limited to the Apps Reg" in error for error in self.audit()))

    def test_inaccessible_settings_fail_closed(self):
        def denied(path):
            raise RuntimeError("Cannot verify metadata")
        with self.assertRaises(RuntimeError):
            AUDIT.audit(self.repository, "appreg-codex", denied)

    def test_runner_access_denial_preserves_other_findings(self):
        self.responses[f"repos/{self.repository}/actions/secrets"]["secrets"] = [
            {"name": "CODEX_OPENAI_API_KEY"}]
        def limited_access(path):
            if path == "orgs/hmcts/actions/runner-groups":
                raise RuntimeError("Cannot verify runner groups")
            return copy.deepcopy(self.responses[path])
        errors = AUDIT.audit(self.repository, "appreg-codex", limited_access)
        self.assertTrue(any("Remove repository-accessible" in error for error in errors))
        self.assertIn("Cannot verify runner groups", errors)


if __name__ == "__main__":
    unittest.main()
