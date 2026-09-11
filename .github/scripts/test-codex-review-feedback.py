#!/usr/bin/env python3
"""Conversation command-to-prompt and bounded review collection regressions."""
import copy
import importlib.util
import json
import os
import shlex
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / ".github/scripts/codex-pr-review-feedback.sh"
SPEC = importlib.util.spec_from_file_location("feedback", SCRIPT.with_name("collect-codex-review-feedback.py"))
FEEDBACK = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(FEEDBACK)
REPOSITORY = "example/project"
PREFIX = f"repos/{REPOSITORY}"
NOW = "2026-09-11T09:00:00Z"


class ReviewFeedbackTests(unittest.TestCase):
    def setUp(self):
        self.head = "a" * 40
        self.event = {
            "action": "created", "issue": {"number": 42, "pull_request": {}},
            "comment": {"body": "/codex-review", "user": {"login": "maintainer"},
                        "created_at": NOW, "html_url": "https://example.invalid/command"},
        }
        self.pr = {"state": "open", "title": "Example", "html_url": "https://example.invalid/pr/42",
                   "head": {"ref": "codex/example", "sha": self.head, "repo": {"full_name": REPOSITORY}},
                   "base": {"ref": "master"}}
        self.review = {
            "id": 10, "user": {"login": "reviewer"}, "state": "CHANGES_REQUESTED",
            "submitted_at": "2026-09-11T08:00:00Z", "commit_id": self.head,
            "body": "Handle an empty collection.", "html_url": "https://example.invalid/review/10",
        }
        self.inline = {
            "pull_request_review_id": 10, "user": {"login": "reviewer"}, "commit_id": self.head,
            "created_at": "2026-09-11T08:00:00Z", "line": 2, "path": "example.txt",
            "body": "Add a regression here.", "diff_hunk": "@@ -1 +1 @@",
        }
        self.responses = {
            f"{PREFIX}/collaborators/maintainer/permission": {"permission": "write"},
            f"{PREFIX}/collaborators/reviewer/permission": {"permission": "maintain"},
            f"{PREFIX}/pulls/42": self.pr,
            f"{PREFIX}/pulls/42/reviews?per_page=100&page=1": [self.review],
            f"{PREFIX}/pulls/42/comments?per_page=100&page=1": [self.inline],
        }

    def collect(self):
        return FEEDBACK.collect(self.event, REPOSITORY, lambda path: copy.deepcopy(self.responses[path]))

    def test_current_review_body_and_inline_are_collected(self):
        data = json.loads(self.collect()["REVIEW_COMMENTS"])
        self.assertEqual(data["reviews"][0]["body"], self.review["body"])
        self.assertEqual(data["inline_comments"][0]["body"], self.inline["body"])

    def test_unauthorized_command_cannot_read_reviews(self):
        self.responses[f"{PREFIX}/collaborators/maintainer/permission"]["permission"] = "read"
        self.assertIn("write permission", self.collect()["SKIP_REASON"])

    def test_untrusted_review_author_is_excluded(self):
        self.responses[f"{PREFIX}/collaborators/reviewer/permission"]["permission"] = "read"
        self.assertIn("no current", self.collect()["SKIP_REASON"])

    def test_dismissed_pending_approved_and_stale_reviews_are_excluded(self):
        for state, revision in (("DISMISSED", self.head), ("PENDING", self.head),
                                ("APPROVED", self.head), ("CHANGES_REQUESTED", "b" * 40)):
            with self.subTest(state=state, revision=revision):
                self.review.update(state=state, commit_id=revision)
                self.assertIn("no current", self.collect()["SKIP_REASON"])

    def test_latest_approval_supersedes_earlier_requested_changes(self):
        later = {**self.review, "id": 11, "submitted_at": "2026-09-11T08:30:00Z", "state": "APPROVED"}
        self.responses[f"{PREFIX}/pulls/42/reviews?per_page=100&page=1"].append(later)
        self.assertIn("no current", self.collect()["SKIP_REASON"])

    def test_later_comment_does_not_erase_current_unresolved_review_feedback(self):
        later = {**self.review, "id": 11, "submitted_at": "2026-09-11T08:30:00Z",
                 "state": "COMMENTED", "body": "Also check the boundary."}
        self.responses[f"{PREFIX}/pulls/42/reviews?per_page=100&page=1"].append(later)
        data = json.loads(self.collect()["REVIEW_COMMENTS"])
        self.assertEqual(len(data["reviews"]), 2)
        self.assertEqual(len(data["inline_comments"]), 1)

    def test_outdated_or_future_inline_comments_are_excluded(self):
        for mutation in ({"commit_id": "b" * 40}, {"line": None},
                         {"created_at": "2026-09-11T10:00:00Z"},
                         {"updated_at": "2026-09-11T10:00:00Z"}):
            with self.subTest(mutation=mutation):
                self.responses[f"{PREFIX}/pulls/42/comments?per_page=100&page=1"] = [{**self.inline, **mutation}]
                self.assertEqual(json.loads(self.collect()["REVIEW_COMMENTS"])["inline_comments"], [])

    def test_inline_author_is_independently_authorized(self):
        self.inline["user"] = {"login": "visitor"}
        self.responses[f"{PREFIX}/collaborators/visitor/permission"] = {"permission": "read"}
        self.assertEqual(json.loads(self.collect()["REVIEW_COMMENTS"])["inline_comments"], [])

    def test_reviews_after_the_command_are_excluded(self):
        self.review["submitted_at"] = "2026-09-11T10:00:00Z"
        self.assertIn("no current", self.collect()["SKIP_REASON"])

    def test_oversized_feedback_fails_closed(self):
        self.review["body"] = "x" * FEEDBACK.MAX_FEEDBACK_BYTES
        with self.assertRaisesRegex(RuntimeError, "size bound"):
            self.collect()

    def test_pagination_is_complete_and_bounded(self):
        seen = []
        def api(path):
            seen.append(path)
            return list(range(100)) if len(seen) == 1 else [100]
        self.assertEqual(len(FEEDBACK.pages("example", api)), 101)
        self.assertEqual(len(seen), 2)
        with self.assertRaisesRegex(RuntimeError, "pagination bound"):
            FEEDBACK.pages("example", lambda path: list(range(100)))

    def test_missing_command_non_pr_and_edited_comments_are_rejected(self):
        for event in ({**self.event, "action": "edited"}, {**self.event, "issue": {"number": 42}},
                      {**self.event, "comment": {**self.event["comment"], "body": "please help"}}):
            with self.subTest(event=event):
                self.assertTrue(FEEDBACK.collect(event, REPOSITORY, lambda _: self.fail("Unexpected API call"))["SKIP_REASON"])

    def test_fork_closed_and_non_codex_prs_are_rejected(self):
        for field, value in (("state", "closed"), ("ref", "feature/example"), ("repo", {"full_name": "other/project"})):
            with self.subTest(field=field):
                pr = copy.deepcopy(self.pr)
                if field == "state":
                    pr[field] = value
                else:
                    pr["head"][field] = value
                self.responses[f"{PREFIX}/pulls/42"] = pr
                self.assertTrue(self.collect()["SKIP_REASON"])

    def run_prompt(self, moved=False):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            repo = root / "repo"
            repo.mkdir()
            def git(*arguments):
                return subprocess.check_output(["git", "-C", str(repo), *arguments], text=True).strip()
            git("init", "--quiet", "--initial-branch=master")
            git("config", "user.name", "Test")
            git("config", "user.email", "test@example.invalid")
            (repo / "example.txt").write_text("original\n")
            git("add", ".")
            git("commit", "--quiet", "-m", "fixture")
            sha = git("rev-parse", "HEAD")
            git("update-ref", "refs/remotes/origin/codex/example", sha)
            git("update-ref", "refs/remotes/origin/master", sha)
            self.pr["head"]["sha"] = sha
            self.review["commit_id"] = sha
            self.inline["commit_id"] = sha
            if moved:
                self.pr["head"]["sha"] = "b" * 40
                self.review["commit_id"] = "b" * 40
                self.inline["commit_id"] = "b" * 40
            responses = root / "responses.json"
            responses.write_text(json.dumps(self.responses))
            event = root / "event.json"
            event.write_text(json.dumps(self.event))
            fake_bin = root / "bin"
            fake_bin.mkdir()
            gh = fake_bin / "gh"
            gh.write_text("#!/usr/bin/env python3\nimport json, sys\n"
                          f"data = json.load(open({str(responses)!r}))\n"
                          "print(json.dumps(data[sys.argv[-1]]))\n")
            gh.chmod(0o755)
            git_wrapper = fake_bin / "git"
            git_wrapper.write_text(
                "#!/usr/bin/env bash\n"
                'for arg in "$@"; do\n'
                '  case "$arg" in fetch|ls-remote) exit 0 ;; esac\n'
                "done\n"
                f"exec {shlex.quote(shutil.which('git'))} \"$@\"\n"
            )
            git_wrapper.chmod(0o755)
            environment = {
                "PATH": f"{fake_bin}:{os.environ['PATH']}", "HOME": str(root),
                "GH_TOKEN": "unused-by-local-fixture", "GITHUB_EVENT_NAME": "issue_comment",
                "GITHUB_EVENT_PATH": str(event), "GITHUB_REPOSITORY": REPOSITORY,
                "OUTPUT_DIR": str(root / "output"), "RUNNER_TEMP": str(root / "temp"),
            }
            result = subprocess.run(["bash", str(SCRIPT)], cwd=repo, env=environment,
                                    capture_output=True, text=True, timeout=30)
            if moved:
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("PR head moved", result.stderr)
                return
            self.assertEqual(result.returncode, 0, result.stderr)
            prompt = next((root / "temp").rglob("codex-review-feedback-prompt.md")).read_text()
            self.assertIn("Handle an empty collection.", prompt)
            self.assertIn("Add a regression here.", prompt)
            self.assertIn("untrusted JSON data", prompt)
            self.assertEqual((repo / "example.txt").read_text(), "original\n")

    def test_conversation_command_reaches_the_real_prompt(self):
        self.run_prompt()

    def test_head_movement_aborts_prompt_handoff(self):
        self.run_prompt(moved=True)


if __name__ == "__main__":
    unittest.main()
