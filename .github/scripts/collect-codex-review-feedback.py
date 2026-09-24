#!/usr/bin/env python3
"""Collect bounded, current review data for an authorized Conversation command."""
import datetime
import json
import os
import re
import shlex
import subprocess
import tempfile
from pathlib import Path
from urllib.parse import quote

MAX_PAGE_BYTES = 2 * 1024 * 1024
MAX_PAGES = 10
MAX_FEEDBACK_BYTES = 64 * 1024
COMMAND = re.compile(r"(^|\s)/(codex-review|codex review)(?=\s|$)", re.I)


def github(path):
    # Do not copy API error bodies (or authentication diagnostics) into build logs.
    with tempfile.TemporaryFile() as output:
        result = subprocess.run(
            ["gh", "api", "--method", "GET", path], stdout=output,
            stderr=subprocess.DEVNULL, timeout=60, check=False,
        )
        if result.returncode:
            raise RuntimeError("Unable to read review metadata or author permission.")
        if output.tell() > MAX_PAGE_BYTES:
            raise RuntimeError("Review API response exceeds the size bound.")
        output.seek(0)
        return json.load(output)


def pages(path, api):
    items = []
    for page in range(1, MAX_PAGES + 1):
        batch = api(f"{path}?per_page=100&page={page}")
        if not isinstance(batch, list) or len(batch) > 100:
            raise RuntimeError("Unexpected review pagination response.")
        items.extend(batch)
        if len(batch) < 100:
            return items
    raise RuntimeError("Review history exceeds the pagination bound; narrow it before retrying.")


def timestamp(value):
    if not isinstance(value, str):
        raise ValueError("Missing review timestamp.")
    result = datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
    if result.tzinfo is None:
        raise ValueError("Review timestamp must include a timezone.")
    return result


def collect(event, repository, api=github):
    feedback = {"SKIP_REASON": ""}
    if event.get("action") != "created" or "pull_request" not in event.get("issue", {}):
        return {"SKIP_REASON": "only newly created PR Conversation commands are supported"}
    comment = event["comment"]
    body = (comment.get("body") or "").strip()
    if not COMMAND.search(body):
        return {"SKIP_REASON": "missing explicit /codex-review command"}
    author = comment.get("user", {}).get("login", "")
    prefix = f"repos/{repository}"
    permissions = {}

    def authorized(login):
        if not login or login in {"github-actions[bot]", "app/github-actions"}:
            return False
        if login not in permissions:
            response = api(f"{prefix}/collaborators/{quote(login, safe='')}/permission")
            permissions[login] = response.get("permission") in {"write", "maintain", "admin"}
        return permissions[login]

    if not authorized(author):
        return {"SKIP_REASON": "command author does not have repository write permission"}
    number = event["issue"]["number"]
    if not isinstance(number, int) or number < 1:
        raise ValueError("Invalid PR number.")
    pr = api(f"{prefix}/pulls/{number}")
    head = pr["head"]
    if (pr["state"] != "open" or (head.get("repo") or {}).get("full_name") != repository
            or not head["ref"].startswith("codex/")):
        return {"SKIP_REASON": "PR is not an open Codex PR in this repository"}
    head_sha = head["sha"]
    if not re.fullmatch(r"[0-9a-f]{40}", head_sha):
        raise ValueError("Invalid PR head revision.")
    command_time = timestamp(comment["created_at"])
    reviews = pages(f"{prefix}/pulls/{number}/reviews", api)
    submitted = []
    approvals = {}
    for review in reviews:
        if not review.get("submitted_at") or timestamp(review["submitted_at"]) > command_time:
            continue
        login = review.get("user", {}).get("login", "")
        order = (timestamp(review["submitted_at"]), review["id"])
        submitted.append((order, review))
        if review.get("state", "").upper() == "APPROVED":
            approvals[login] = max(order, approvals.get(login, order))
    selected = {}
    for order, review in sorted(submitted, key=lambda item: item[0]):
        login = review.get("user", {}).get("login", "")
        if (review.get("state", "").upper() in {"CHANGES_REQUESTED", "COMMENTED"}
                and (login not in approvals or order > approvals[login])
                and review.get("commit_id") == head_sha and authorized(login)):
            selected[review["id"]] = review
    review_data = [
        {"author": item["user"]["login"], "state": item["state"], "body": item.get("body") or "",
         "url": item.get("html_url") or "", "commit_id": head_sha}
        for item in selected.values()
    ]
    inline_data = []
    if selected:
        for item in pages(f"{prefix}/pulls/{number}/comments", api):
            if (item.get("pull_request_review_id") not in selected or item.get("commit_id") != head_sha
                    or timestamp(item["created_at"]) > command_time
                    or timestamp(item.get("updated_at", item["created_at"])) > command_time):
                continue
            if item.get("line") is None and item.get("position") is None and item.get("subject_type") != "file":
                continue
            login = item.get("user", {}).get("login", "")
            if not authorized(login):
                continue
            inline_data.append({
                "author": login, "path": item.get("path") or "", "line": item.get("line"),
                "diff_hunk": item.get("diff_hunk") or "", "body": item.get("body") or "",
                "url": item.get("html_url") or "", "commit_id": head_sha,
            })
    data = json.dumps({"reviews": review_data, "inline_comments": inline_data}, ensure_ascii=True, indent=2)
    if len((body + data).encode("utf-8")) > MAX_FEEDBACK_BYTES:
        raise RuntimeError("Review feedback exceeds the prompt size bound.")
    if not review_data and not inline_data and not COMMAND.sub("", body).strip():
        return {"SKIP_REASON": "no current, authorized submitted review feedback"}
    feedback.update(
        PR_NUMBER=str(number), COMMENT_KIND="issue_comment", COMMENT_AUTHOR=author,
        COMMENT_BODY=body, COMMENT_URL=comment.get("html_url") or "",
        COMMENT_PATH="", COMMENT_DIFF_HUNK="", REVIEW_STATE="", REVIEW_COMMENTS=data,
        PR_STATE=pr["state"], PR_TITLE=pr["title"], PR_URL=pr["html_url"],
        HEAD_REF=head["ref"], HEAD_REPO=head["repo"]["full_name"], BASE_REF=pr["base"]["ref"],
        REVIEW_HEAD_SHA=head_sha,
    )
    if len(json.dumps(feedback).encode("utf-8")) > MAX_FEEDBACK_BYTES + 16 * 1024:
        raise RuntimeError("PR metadata exceeds the prompt size bound.")
    return feedback


def main():
    if os.environ["GITHUB_EVENT_NAME"] != "issue_comment":
        values = {"SKIP_REASON": "only Conversation commands are supported"}
    else:
        repository = os.environ["GITHUB_REPOSITORY"]
        if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
            raise ValueError("Invalid repository.")
        values = collect(json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text()), repository)
    for key, value in values.items():
        print(f"{key}={shlex.quote(value)}")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, ValueError, KeyError, TypeError, subprocess.TimeoutExpired):
        raise SystemExit("Review preparation failed: unable to verify bounded current review data.")
