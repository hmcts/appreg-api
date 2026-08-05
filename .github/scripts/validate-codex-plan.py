#!/usr/bin/env python3
"""Validate and normalise the untrusted Codex planning hand-off."""

from __future__ import annotations

import hashlib
import json
import os
import sys
from pathlib import Path, PurePosixPath
from typing import Any

MAX_PLAN_BYTES = 32 * 1024
EXPECTED_KEYS = {
    "ready_to_implement",
    "problem_analysis",
    "root_cause",
    "scope_decision",
    "risk_level",
    "cross_system_change",
    "alternatives_considered",
    "implementation_steps",
    "tests_required",
    "acceptance_criteria",
    "risks",
    "assumptions",
    "blockers",
}
RISK_LEVELS = {"low", "medium", "high"}
FORBIDDEN_PATH_ROOTS = {".git", ".github"}


class PlanValidationError(ValueError):
    pass


def require_string(value: Any, field: str, *, max_length: int = 8000) -> str:
    if not isinstance(value, str):
        raise PlanValidationError(f"{field} must be a string")
    normalised = " ".join(value.split())
    if not normalised:
        raise PlanValidationError(f"{field} must not be empty")
    if len(normalised) > max_length:
        raise PlanValidationError(f"{field} exceeds {max_length} characters")
    return normalised


def require_string_list(value: Any, field: str, *, max_items: int = 20) -> list[str]:
    if not isinstance(value, list):
        raise PlanValidationError(f"{field} must be an array")
    if len(value) > max_items:
        raise PlanValidationError(f"{field} contains more than {max_items} items")
    return [require_string(item, f"{field}[{index}]", max_length=2000) for index, item in enumerate(value)]


def validate_path(value: Any, field: str) -> str:
    path_text = require_string(value, field, max_length=500)
    if "\\" in path_text or "\x00" in path_text:
        raise PlanValidationError(f"{field} must use a safe repository-relative POSIX path")
    path = PurePosixPath(path_text)
    if path.is_absolute() or any(part in {"", ".", ".."} for part in path.parts):
        raise PlanValidationError(f"{field} must be a safe repository-relative path")
    if path.parts[0] in FORBIDDEN_PATH_ROOTS:
        raise PlanValidationError(f"{field} targets protected automation metadata")
    return path.as_posix()


def validate_steps(value: Any) -> list[dict[str, str]]:
    if not isinstance(value, list):
        raise PlanValidationError("implementation_steps must be an array")
    if len(value) > 30:
        raise PlanValidationError("implementation_steps contains more than 30 items")

    steps: list[dict[str, str]] = []
    for index, item in enumerate(value):
        if not isinstance(item, dict) or set(item) != {"path", "change", "reason"}:
            raise PlanValidationError(
                f"implementation_steps[{index}] must contain only path, change, and reason"
            )
        steps.append(
            {
                "path": validate_path(item["path"], f"implementation_steps[{index}].path"),
                "change": require_string(
                    item["change"], f"implementation_steps[{index}].change", max_length=3000
                ),
                "reason": require_string(
                    item["reason"], f"implementation_steps[{index}].reason", max_length=3000
                ),
            }
        )
    return steps


def validate_plan(raw: str) -> dict[str, Any]:
    if len(raw.encode("utf-8")) > MAX_PLAN_BYTES:
        raise PlanValidationError(f"plan exceeds the {MAX_PLAN_BYTES}-byte limit")
    try:
        value = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise PlanValidationError(f"plan is not valid JSON: {exc}") from exc
    if not isinstance(value, dict):
        raise PlanValidationError("plan must be a JSON object")
    if set(value) != EXPECTED_KEYS:
        missing = sorted(EXPECTED_KEYS - set(value))
        extra = sorted(set(value) - EXPECTED_KEYS)
        raise PlanValidationError(f"plan fields do not match the contract; missing={missing}, extra={extra}")
    if not isinstance(value["ready_to_implement"], bool):
        raise PlanValidationError("ready_to_implement must be a boolean")
    if not isinstance(value["cross_system_change"], bool):
        raise PlanValidationError("cross_system_change must be a boolean")

    risk_level = require_string(value["risk_level"], "risk_level", max_length=20).lower()
    if risk_level not in RISK_LEVELS:
        raise PlanValidationError("risk_level must be low, medium, or high")

    plan = {
        "ready_to_implement": value["ready_to_implement"],
        "problem_analysis": require_string(value["problem_analysis"], "problem_analysis"),
        "root_cause": require_string(value["root_cause"], "root_cause"),
        "scope_decision": require_string(value["scope_decision"], "scope_decision"),
        "risk_level": risk_level,
        "cross_system_change": value["cross_system_change"],
        "alternatives_considered": require_string_list(
            value["alternatives_considered"], "alternatives_considered", max_items=10
        ),
        "implementation_steps": validate_steps(value["implementation_steps"]),
        "tests_required": require_string_list(value["tests_required"], "tests_required"),
        "acceptance_criteria": require_string_list(
            value["acceptance_criteria"], "acceptance_criteria"
        ),
        "risks": require_string_list(value["risks"], "risks"),
        "assumptions": require_string_list(value["assumptions"], "assumptions"),
        "blockers": require_string_list(value["blockers"], "blockers"),
    }

    if plan["ready_to_implement"]:
        for field in (
            "alternatives_considered",
            "implementation_steps",
            "tests_required",
            "acceptance_criteria",
        ):
            if not plan[field]:
                raise PlanValidationError(f"a ready plan must include {field}")
        if plan["blockers"]:
            raise PlanValidationError("a ready plan must not contain blockers")
    elif not plan["blockers"]:
        raise PlanValidationError("a plan that is not ready must explain its blockers")

    return plan


def render_markdown(plan: dict[str, Any]) -> str:
    def bullets(values: list[str]) -> str:
        return "\n".join(f"- {value}" for value in values) or "- None"

    steps = "\n".join(
        f"{index}. `{step['path']}`: {step['change']}  \n   Reason: {step['reason']}"
        for index, step in enumerate(plan["implementation_steps"], start=1)
    ) or "No implementation steps proposed."

    return f"""### Planning decision

- Ready to implement: **{str(plan['ready_to_implement']).lower()}**
- Risk level: **{plan['risk_level']}**
- Cross-system change: **{str(plan['cross_system_change']).lower()}**

### Problem analysis

{plan['problem_analysis']}

### Root cause

{plan['root_cause']}

### Scope decision

{plan['scope_decision']}

### Alternatives considered

{bullets(plan['alternatives_considered'])}

### Implementation steps

{steps}

### Tests required

{bullets(plan['tests_required'])}

### Acceptance criteria

{bullets(plan['acceptance_criteria'])}

### Risks

{bullets(plan['risks'])}

### Assumptions

{bullets(plan['assumptions'])}

### Blockers

{bullets(plan['blockers'])}
"""


def write_output(name: str, value: str) -> None:
    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with Path(github_output).open("a", encoding="utf-8") as output:
            output.write(f"{name}={value}\n")


def main() -> int:
    raw = os.environ.get("CODEX_PLAN_RESULT", "")
    output_dir_value = os.environ.get("OUTPUT_DIR", "")
    if not raw or not output_dir_value:
        print("CODEX_PLAN_RESULT and OUTPUT_DIR are required", file=sys.stderr)
        return 2

    try:
        plan = validate_plan(raw)
    except PlanValidationError as exc:
        print(f"Invalid Codex plan: {exc}", file=sys.stderr)
        return 1

    output_dir = Path(output_dir_value)
    output_dir.mkdir(parents=True, exist_ok=True)
    plan_bytes = (json.dumps(plan, indent=2, sort_keys=True) + "\n").encode("utf-8")
    plan_sha256 = hashlib.sha256(plan_bytes).hexdigest()
    (output_dir / "plan.json").write_bytes(plan_bytes)
    (output_dir / "plan.md").write_text(render_markdown(plan), encoding="utf-8")
    (output_dir / "plan.sha256").write_text(f"{plan_sha256}\n", encoding="ascii")

    requires_approval = plan["risk_level"] == "high" or plan["cross_system_change"]
    write_output("ready_to_implement", str(plan["ready_to_implement"]).lower())
    write_output("requires_approval", str(requires_approval).lower())
    write_output("risk_level", plan["risk_level"])
    write_output("plan_sha256", plan_sha256)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
