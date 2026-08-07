#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: bin/codex-local-pipeline.sh [checks-only|fast|codex|full] [options]

Runs a local approximation of the checks that matter before a Codex PR is opened.

Modes:
  checks-only  Validate workflow/script syntax and repository PR guardrails only.
  fast         Run checks-only plus Gradle check. Default.
  codex        Run the runner toolchain preflight plus fast mode.
  full         Run fast mode plus full Gradle checks, integration, functional,
               smoke, coverage, and dependency checks.

Options:
  --base <branch>              Base branch for PR-style diff checks. Default: master.
  --no-fetch                   Do not fetch origin/<base> before diff checks.
  --include-dependency-check   Run OWASP dependencyCheck, even outside full mode.
  -h, --help                   Show this help.

Environment:
  BASE_BRANCH                  Alternative way to set --base.
  GRADLE_FAST_TASKS            Space-separated Gradle tasks for fast mode.
                              Default: clean check.
  REQUIRE_DOCKER               Set true to require Docker outside full mode.
EOF
}

log() {
  printf '\n==> %s\n' "$*"
}

warn() {
  printf 'Warning: %s\n' "$*" >&2
}

require_command() {
  local command_name="$1"

  if ! command -v "${command_name}" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "${command_name}" >&2
    exit 1
  fi
}

trim_count() {
  tr -d '[:space:]'
}

repo_root="$(git rev-parse --show-toplevel)"
cd "${repo_root}"

mode="fast"
if [[ $# -gt 0 && "$1" != -* ]]; then
  mode="$1"
  shift
fi

base_branch="${BASE_BRANCH:-master}"
fetch_base="true"
include_dependency_check="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      if [[ $# -lt 2 ]]; then
        echo "--base requires a branch name" >&2
        exit 1
      fi
      base_branch="$2"
      shift 2
      ;;
    --no-fetch)
      fetch_base="false"
      shift
      ;;
    --include-dependency-check)
      include_dependency_check="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

case "${mode}" in
  checks-only|fast|codex|full)
    ;;
  *)
    echo "Unknown mode: ${mode}" >&2
    usage >&2
    exit 1
    ;;
esac

if [[ "${mode}" == "full" ]]; then
  include_dependency_check="true"
fi

log "Checking required local tools"
for command_name in git bash find awk sort uniq wc python3; do
  require_command "${command_name}"
done

if [[ "${mode}" != "checks-only" ]]; then
  require_command java
fi

if [[ "${mode}" == "codex" ]]; then
  log "Running Codex runner preflight"
  ./.github/scripts/codex-runner-preflight.sh
fi

log "Validating shell scripts"
bash -n \
  .github/scripts/*.sh \
  bin/*.sh

log "Validating Python scripts"
python_cache="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/codex-pycache"
mkdir -p "${python_cache}"
PYTHONPYCACHEPREFIX="${python_cache}" python3 -m py_compile .github/scripts/*.py
PYTHONPYCACHEPREFIX="${python_cache}" python3 .github/scripts/test-collect-codex-patch-result.py
PYTHONPYCACHEPREFIX="${python_cache}" python3 .github/scripts/test-codex-patch-export.py
PYTHONPYCACHEPREFIX="${python_cache}" python3 .github/scripts/test-codex-check-sonar-quality-gate.py
PYTHONPYCACHEPREFIX="${python_cache}" python3 .github/scripts/test-codex-publish-revision.py
PYTHONPYCACHEPREFIX="${python_cache}" python3 .github/scripts/test-codex-pr-review-handoff.py
PYTHONPYCACHEPREFIX="${python_cache}" python3 .github/scripts/test-validate-codex-plan.py
PYTHONPYCACHEPREFIX="${python_cache}" python3 .github/scripts/test-codex-plan-handoff.py
PYTHONPYCACHEPREFIX="${python_cache}" python3 .github/scripts/test-codex-verify-publisher.py

log "Validating workflow YAML syntax"
if command -v ruby >/dev/null 2>&1; then
  ruby - <<'RUBY'
require "yaml"

expected_codex_version = "0.146.0"
errors = []

Dir[".github/workflows/*.yml", ".github/workflows/*.yaml"].each do |path|
  workflow = YAML.load_file(path)
  workflow_env = workflow.fetch("env", {}) || {}
  workflow.fetch("jobs", {}).each do |job_name, job|
    steps = job.fetch("steps", [])
    codex_steps = steps.select do |step|
      step.is_a?(Hash) && step.fetch("uses", "").start_with?("openai/codex-action@")
    end
    next if codex_steps.empty?

    job_env = job.fetch("env", {}) || {}
    action_exposes_token = codex_steps.any? do |step|
      (step.fetch("env", {}) || {}).key?("GH_TOKEN")
    end
    if workflow_env.key?("GH_TOKEN") || job_env.key?("GH_TOKEN") || action_exposes_token
      errors << "#{path}:#{job_name} exposes GH_TOKEN to the Codex Action"
    end

    codex_steps.each do |step|
      inputs = step.fetch("with", {})
      version = inputs.fetch("codex-version", "")
      errors << "#{path}:#{job_name} must pin codex-version to #{expected_codex_version}" unless version == expected_codex_version

      if File.basename(path) == "codex_jira_dispatch.yml" && job_name == "codex-plan-action"
        unless inputs.fetch("model", "") == "gpt-5.6-sol" && inputs.fetch("effort", "") == "ultra"
          errors << "#{path}:#{job_name} must plan with gpt-5.6-sol and ultra effort"
        end
      end

      if inputs.fetch("permission-profile", "") == ":workspace"
        action_index = steps.index(step)
        unless action_index == steps.length - 1
          errors << "#{path}:#{job_name} must end with the workspace-writing Codex Action"
        end
        unless inputs.key?("output-schema-file") && !inputs.key?("output-file")
          errors << "#{path}:#{job_name} must return a structured patch without a post-Action output file"
        end
        unless inputs.fetch("model", "") == "gpt-5.6-sol" && inputs.fetch("effort", "") == "medium"
          errors << "#{path}:#{job_name} must implement with gpt-5.6-sol and medium effort"
        end
      end

      next unless File.basename(path) == "codex_runner_smoke.yml"

      unless inputs.fetch("model", "") == "gpt-5.6-sol" && inputs.fetch("effort", "") == "ultra"
        errors << "#{path}:#{job_name} must smoke-test gpt-5.6-sol with ultra effort"
      end
    end
  end

end

jira_path = ".github/workflows/codex_jira_dispatch.yml"
jira_workflow = YAML.load_file(jira_path)
jira_jobs = jira_workflow.fetch("jobs", {})

planner = jira_jobs.fetch("codex-plan-action", {})
planner_steps = planner.fetch("steps", [])
planner_action = planner_steps.find do |step|
  step.is_a?(Hash) && step.fetch("uses", "").start_with?("openai/codex-action@")
end
if planner_action.nil?
  errors << "#{jira_path}:codex-plan-action must invoke the Codex Action"
else
  planner_inputs = planner_action.fetch("with", {})
  unless planner_inputs.fetch("model", "") == "gpt-5.6-sol" &&
         planner_inputs.fetch("effort", "") == "ultra"
    errors << "#{jira_path}:codex-plan-action must use gpt-5.6-sol with ultra effort"
  end
  unless planner_inputs.fetch("permission-profile", "") == ":read-only"
    errors << "#{jira_path}:codex-plan-action must use the read-only permission profile"
  end
  unless planner_steps.last == planner_action
    errors << "#{jira_path}:codex-plan-action must end with the Codex Action"
  end
end

validator = jira_jobs.fetch("validate-codex-plan", {})
validator_outputs = validator.fetch("outputs", {}) || {}
unless validator.fetch("needs", "") == "codex-plan-action" &&
       %w[ready_to_implement plan_sha256 plan_payload planned_path_count].all? { |name| validator_outputs.key?(name) }
  errors << "#{jira_path}:validate-codex-plan must expose the bounded validated plan hand-off"
end

blocked = jira_jobs.fetch("codex-plan-blocked", {})
unless blocked.fetch("needs", "") == "validate-codex-plan" &&
       blocked.fetch("if", "") == "needs.validate-codex-plan.outputs.ready_to_implement == 'false'" &&
       blocked.inspect.include?("exit 1")
  errors << "#{jira_path}:codex-plan-blocked must expose a terminal failure for plans that are not ready"
end

if jira_jobs.key?("approve-codex-plan")
  errors << "#{jira_path}:ready plans must proceed automatically without a plan-approval job"
end

implementation = jira_jobs.fetch("codex-generate-action", {})
implementation_needs = Array(implementation.fetch("needs", []))
unless implementation_needs.sort == %w[codex-plan-action validate-codex-plan].sort
  errors << "#{jira_path}:codex-generate-action must follow planning and trusted validation directly"
end
implementation_condition = implementation.fetch("if", "")
unless implementation_condition.include?("needs.validate-codex-plan.result == 'success'") &&
       implementation_condition.include?("needs.validate-codex-plan.outputs.ready_to_implement == 'true'") &&
       !implementation_condition.include?("approve-codex-plan") &&
       implementation.inspect.include?("needs.codex-plan-action.outputs.trusted_sha") &&
       implementation.inspect.include?("CODEX_PLAN_PAYLOAD") &&
       implementation.inspect.include?("--materialize") &&
       implementation.inspect.include?("PLAN_DIR")
  errors << "#{jira_path}:codex-generate-action must auto-start only from the validated ready-plan hand-off"
end

jira_source = File.read(jira_path)
if jira_source.include?("codex-plan-approval") || jira_source.include?("required_reviewers")
  errors << "#{jira_path}:automatic plan approval must not retain a GitHub environment approval gate"
end
if jira_source.match?(/^\s+name:\s+codex-jira-plan\s*$/) ||
   jira_source.include?("plan.md") ||
   jira_source.match?(/cat .*plan\.json.*GITHUB_STEP_SUMMARY/)
  errors << "#{jira_path}:raw validated plans must not be published as artifacts or summaries"
end

generation_collector = jira_jobs.fetch("codex-generate", {})
unless Array(generation_collector.fetch("needs", [])).include?("validate-codex-plan") &&
       generation_collector.inspect.include?("CODEX_PLAN_PAYLOAD") &&
       generation_collector.inspect.include?("--materialize") &&
       generation_collector.inspect.include?("PLAN_DIR")
  errors << "#{jira_path}:codex-generate must freshly materialise the validated plan before collecting"
end

repair_actions = jira_jobs.select { |name, _job| name.match?(/^repair-codex-output-\d+-action$|^repair-published-pr-\d+-action$/) }
unless repair_actions.length == 4
  errors << "#{jira_path}:expected four Jira repair Action jobs"
end
repair_actions.each do |job_name, job|
  unless Array(job.fetch("needs", [])).include?("validate-codex-plan") &&
         job.inspect.include?("CODEX_PLAN_PAYLOAD") &&
         job.inspect.include?("--materialize") &&
         job.inspect.include?("PLAN_DIR")
    errors << "#{jira_path}:#{job_name} must reuse the original bounded validated plan"
  end
end

repair_collectors = jira_jobs.select { |name, _job| name.match?(/^repair-codex-output-\d+$|^repair-published-pr-\d+$/) }
unless repair_collectors.length == 4
  errors << "#{jira_path}:expected four fresh Jira repair collector jobs"
end
repair_collectors.each do |job_name, job|
  unless Array(job.fetch("needs", [])).include?("validate-codex-plan") &&
         job.inspect.include?("CODEX_PLAN_PAYLOAD") &&
         job.inspect.include?("--materialize") &&
         job.inspect.include?("PLAN_DIR")
    errors << "#{jira_path}:#{job_name} must enforce the original plan in the fresh collector"
  end
end

verification_source = jira_jobs.fetch("prepare-codex-verification-source", {})
unless verification_source.fetch("permissions", {}) == { "contents" => "read" } &&
       verification_source.inspect.include?("codex-verification-source.tar.gz") &&
       !verification_source.inspect.match?(/codex-jira-verify|codex-local-pipeline|gradlew/)
  errors << "#{jira_path}:prepare-codex-verification-source must only archive the trusted checkout"
end

jira_verifiers = jira_jobs.select { |_job_name, job| job.inspect.include?("codex-jira-verify.sh") }
unless jira_verifiers.length == 7
  errors << "#{jira_path}:expected seven credential-free Jira verifier paths"
end
jira_verifiers.each do |job_name, job|
  needs = Array(job.fetch("needs", []))
  if job.fetch("permissions", nil) != {} ||
     !needs.include?("prepare-codex-verification-source") ||
     job.inspect.match?(/GH_TOKEN|SONAR_TOKEN|secrets\./) ||
     job.inspect.include?("actions/checkout@") ||
     !job.inspect.include?("Download credential-free verification source")
    errors << "#{jira_path}:#{job_name} must execute the patch without permissions or credentials"
  end
end

%w[verify-published-pr verify-published-pr-1].each do |job_name|
  status_job = jira_jobs.fetch(job_name, {})
  if status_job.inspect.include?("codex-jira-verify.sh") ||
     status_job.inspect.include?("codex-local-pipeline.sh") ||
     !status_job.inspect.include?("codex-wait-pr-status.sh") ||
     !status_job.inspect.include?("codex-check-sonar-quality-gate.sh")
    errors << "#{jira_path}:#{job_name} must query external status without executing the generated patch"
  end
end

verification_specs = [
  {
    path: ".github/workflows/codex_pr_review_feedback.yml",
    source_job: "prepare-review-verification-source",
    verifier_marker: "trusted-codex-pr-review-verify.sh",
    expected_count: File.read(".github/workflows/codex_pr_review_feedback.yml").include?("codex-review-verify-3:") ? 4 : 1,
    restore_marker: "Restore credential-free review source",
  },
  {
    path: ".github/workflows/codex_pr_review_feedback.yml",
    source_job: "prepare-published-review-repair-source",
    verifier_marker: "trusted-codex-pr-review-verify.sh",
    expected_count: 1,
    restore_marker: "Restore credential-free published review source",
  },
  {
    path: ".github/workflows/codex_merge_conflict_resolution.yml",
    source_job: "prepare-conflict-verification-source",
    verifier_marker: "trusted-codex-merge-conflict-verify.sh",
    expected_count: 1,
    restore_marker: "Restore credential-free conflict source",
  },
]

verification_specs.each do |spec|
  workflow = YAML.load_file(spec.fetch(:path))
  jobs = workflow.fetch("jobs", {})
  source_job = jobs.fetch(spec.fetch(:source_job), {})
  source_commands = Array(source_job.fetch("steps", [])).map { |step| step.is_a?(Hash) ? step.fetch("run", nil) : nil }.compact.join("\n")
  unless source_job.fetch("permissions", {}) == { "contents" => "read" } &&
         source_job.inspect.include?("fetch-depth") &&
         source_job.inspect.include?("persist-credentials") &&
         source_job.inspect.include?("credential-free") &&
         !source_commands.match?(/bash .*codex-(?:pr-review|merge-conflict)-verify\.sh|codex-local-pipeline\.sh (?:checks-only|fast|full)|gradlew|yarn (?:lint|cichecks)/)
    errors << "#{spec.fetch(:path)}:#{spec.fetch(:source_job)} must archive exact trusted source without executing repository tooling"
  end

  verifiers = jobs.select do |_job_name, job|
    Array(job.fetch("needs", [])).include?(spec.fetch(:source_job)) &&
      Array(job.fetch("steps", [])).any? do |step|
      step.is_a?(Hash) && step.fetch("run", "").match?(/bash .*#{Regexp.escape(spec.fetch(:verifier_marker))}/)
      end
  end
  unless verifiers.length == spec.fetch(:expected_count)
    errors << "#{spec.fetch(:path)}:expected #{spec.fetch(:expected_count)} credential-free patch verifier paths"
  end
  verifiers.each do |job_name, job|
    needs = Array(job.fetch("needs", []))
    if job.fetch("permissions", nil) != {} ||
       !needs.include?(spec.fetch(:source_job)) ||
       job.inspect.match?(/GH_TOKEN|SONAR_TOKEN|secrets\.|github\.token/) ||
       job.inspect.include?("actions/checkout@") ||
       !job.inspect.include?(spec.fetch(:restore_marker)) ||
       !job.inspect.include?("TRUSTED_PIPELINE_PATH")
      errors << "#{spec.fetch(:path)}:#{job_name} must restore trusted source and execute generated code without permissions or credentials"
    end
  end
end

review_workflow = YAML.load_file(".github/workflows/codex_pr_review_feedback.yml")
%w[verify-review-status verify-review-status-repair].each do |job_name|
  review_status = review_workflow.fetch("jobs", {}).fetch(job_name, {})
  if review_status.inspect.match?(/trusted-codex-pr-review-verify|codex-local-pipeline|gradlew|yarn (?:lint|cichecks)/) ||
     !review_status.inspect.include?("codex-wait-pr-status.sh") ||
     !review_status.inspect.include?("codex-check-sonar-quality-gate.sh")
    errors << ".github/workflows/codex_pr_review_feedback.yml:#{job_name} must query external status without executing model-writable content"
  end
end

review_jobs = review_workflow.fetch("jobs", {})
initial_status = review_jobs.fetch("verify-review-status", {})
external_repair = review_jobs.fetch("codex-review-external-repair-action", {})
external_verify = review_jobs.fetch("codex-review-external-repair-verify", {})
external_republish = review_jobs.fetch("codex-review-external-republish", {})
repaired_status = review_jobs.fetch("verify-review-status-repair", {})
unless initial_status.inspect.include?("verification-failure.log") &&
       initial_status.inspect.include?("actions/upload-artifact@") &&
       Array(external_repair.fetch("needs", [])).include?("verify-review-status") &&
       external_repair.inspect.include?("failure_artifact") &&
       external_repair.fetch("steps", []).last.fetch("uses", "").start_with?("openai/codex-action@") &&
       external_verify.fetch("permissions", nil) == {} &&
       Array(external_republish.fetch("needs", [])).include?("codex-review-external-repair-verify") &&
       Array(repaired_status.fetch("needs", [])).include?("codex-review-external-republish")
  errors << ".github/workflows/codex_pr_review_feedback.yml:external status failure must feed one bounded repair, credential-free verification, re-publication, and status cycle"
end

sonar_source = File.read(".github/scripts/codex-check-sonar-quality-gate.sh")
unless sonar_source.include?("PUBLISHED_COMMIT_SHA") &&
       sonar_source.include?("/api/project_analyses/search") &&
       sonar_source.include?("analysisId=") &&
       !sonar_source.match?(/project_status.*projectKey=.*pullRequest=/)
  errors << ".github/scripts/codex-check-sonar-quality-gate.sh must bind the quality gate to the published commit's exact analysis ID"
end

jira_publish_source = File.read(".github/scripts/codex-jira-publish.sh")
review_publish_source = File.read(".github/scripts/codex-pr-review-publish.sh")
conflict_publish_source = File.read(".github/scripts/codex-merge-conflict-publish.sh")
initial_jira_publisher = jira_jobs.fetch("publish-pr", {})
initial_jira_env = initial_jira_publisher.fetch("env", {})
unless initial_jira_env["JIRA_PUBLISH_MODE"] == "initial" &&
       !initial_jira_env.key?("EXPECTED_BRANCH_HEAD_SHA")
  errors << ".github/workflows/codex_jira_dispatch.yml:publish-pr must require an absent generated branch"
end

jira_republishers = jira_jobs.select { |name, _job| name.match?(/^publish-published-pr-repair-\d+$/) }
if jira_republishers.empty?
  errors << ".github/workflows/codex_jira_dispatch.yml must contain a trusted Jira repair republisher"
end
jira_republishers.each do |job_name, job|
  env = job.fetch("env", {})
  unless Array(job.fetch("needs", [])).include?("publish-pr") &&
         env["JIRA_PUBLISH_MODE"] == "repair" &&
         env["EXPECTED_BRANCH_HEAD_SHA"] == "${{ needs.publish-pr.outputs.commit_sha }}"
    errors << ".github/workflows/codex_jira_dispatch.yml:#{job_name} must lease against the exact trusted original publish SHA"
  end
end

unless jira_publish_source.include?("EXPECTED_BASE_SHA") &&
       jira_publish_source.include?('required_env "JIRA_PUBLISH_MODE"') &&
       jira_publish_source.include?('required_env "EXPECTED_BRANCH_HEAD_SHA"') &&
       jira_publish_source.include?('--force-with-lease="refs/heads/${branch_name}:"') &&
       jira_publish_source.include?('--force-with-lease="refs/heads/${branch_name}:${expected_branch_head_sha}"') &&
       !jira_publish_source.include?('--force-with-lease="refs/heads/${branch_name}:${remote_branch_sha}"')
  errors << ".github/scripts/codex-jira-publish.sh must reject a moved default branch before applying a verified patch"
end
unless review_publish_source.include?("EXPECTED_HEAD_SHA") && review_publish_source.include?("ls-remote --heads") &&
       review_publish_source.include?("--force-with-lease")
  errors << ".github/scripts/codex-pr-review-publish.sh must reject a moved PR branch before applying a verified patch"
end
unless conflict_publish_source.include?("actual_head_sha") &&
       conflict_publish_source.include?("actual_base_sha") &&
       conflict_publish_source.include?("latest_head_sha") &&
       conflict_publish_source.include?("latest_base_sha") &&
       conflict_publish_source.include?('--force-with-lease="refs/heads/${head_ref}:${head_sha}"')
  errors << ".github/scripts/codex-merge-conflict-publish.sh must recheck moved head and base branches immediately before an exact-lease push"
end

{
  ".github/scripts/codex-pr-review-verify.sh" => "TRUSTED_PIPELINE_PATH",
  ".github/scripts/codex-merge-conflict-verify.sh" => "TRUSTED_PIPELINE_PATH",
}.each do |path, trusted_marker|
  source = File.read(path)
  if source.match?(/GH_TOKEN|SONAR_TOKEN/) || !source.include?(trusted_marker)
    errors << "#{path} must run only from credential-free source with a separately captured trusted pipeline"
  end
end

jira_collector = File.read(".github/scripts/codex-jira-collect.sh")
if jira_collector.include?("## Codex Plan") || jira_collector.include?("plan.md")
  errors << ".github/scripts/codex-jira-collect.sh must not copy raw plan content into public PR bodies"
end
unless jira_collector.include?("ALLOWED_PATHS_FILE") && jira_collector.include?("validated_codex_plan_path")
  errors << ".github/scripts/codex-jira-collect.sh must enforce planned paths in every fresh collector"
end
unless jira_collector.include?("Model-generated implementation summary") &&
       jira_collector.include?("Model-generated testing details")
  errors << ".github/scripts/codex-jira-collect.sh must retain bounded model-generated PR details"
end
%w[.github/scripts/codex-jira-implement.sh .github/scripts/codex-jira-repair.sh].each do |path|
  source = File.read(path)
  unless source.include?("planned-files") && source.include?("allowed-paths.txt")
    errors << "#{path} must constrain the trusted exporter to exact planned files"
  end
end

if File.exist?(".github/scripts/codex-usage-metrics.sh")
  errors << ".github/scripts/codex-usage-metrics.sh must not emit empty compatibility telemetry"
end
Dir[".github/**/*"].select { |path| File.file?(path) }.each do |path|
  if File.read(path).include?("codex-usage-summary")
    errors << "#{path} still references the removed empty token-usage artefact"
  end
end

contract_capture_checks = {
  ".github/scripts/codex-jira-repair.sh" => /git_sanitized apply --binary/,
  ".github/scripts/codex-merge-conflict-implement.sh" => /git_sanitized checkout -B/,
  ".github/scripts/codex-pr-review-feedback.sh" => /git_sanitized checkout -B/,
}

contract_capture_checks.each do |path, untrusted_operation|
  lines = File.readlines(path)
  untrusted_index = lines.index { |line| line.match?(untrusted_operation) }
  %w[capture_codex_patch_schema capture_codex_patch_exporter].each do |capture_function|
    capture_index = lines.index { |line| line.include?(capture_function) }
    if capture_index.nil? || untrusted_index.nil? || capture_index >= untrusted_index
      errors << "#{path} must call #{capture_function} before loading untrusted repository content"
    end
  end
end

runtime = File.read(".github/scripts/codex-action-runtime.sh")
unless runtime.include?("capture_codex_patch_exporter") &&
       runtime.include?("--paths-file") &&
       runtime.include?("--strict-paths")
  errors << ".github/scripts/codex-action-runtime.sh must use the captured exporter for full and strictly scoped patches"
end
if runtime.match?(/git add (?:-A|--)/)
  errors << ".github/scripts/codex-action-runtime.sh must not instruct the workspace-scoped Action to write the real Git index"
end

revision_pinned_workflows = %w[
  codex_jira_dispatch.yml
  codex_merge_conflict_resolution.yml
  codex_pr_review_feedback.yml
]

revision_pinned_workflows.each do |workflow_name|
  path = ".github/workflows/#{workflow_name}"
  workflow = YAML.load_file(path)
  workflow.fetch("jobs", {}).each do |job_name, job|
    steps = job.fetch("steps", [])
    action_index = steps.index do |step|
      step.is_a?(Hash) && step.fetch("uses", "").start_with?("openai/codex-action@")
    end

    moving_checkouts = steps.each_index.select do |index|
      step = steps[index]
      next false unless step.is_a?(Hash) && step.fetch("uses", "").start_with?("actions/checkout@")

      ref = (step.fetch("with", {}) || {}).fetch("ref", "")
      !ref.match?(/needs\.[A-Za-z0-9_-]+\.outputs\.(?:trusted_sha|head_sha|base_sha|commit_sha)/)
    end

    declared_needs = Array(job.fetch("needs", []))
    referenced_needs = job.inspect.scan(/needs\.([A-Za-z0-9_-]+)\./).flatten.uniq
    missing_needs = referenced_needs - declared_needs
    unless missing_needs.empty?
      errors << "#{path}:#{job_name} references jobs not declared in needs: #{missing_needs.join(", ")}"
    end
    steps.each do |step|
      next unless step.is_a?(Hash) && step.fetch("uses", "").start_with?("actions/checkout@")

      ref = (step.fetch("with", {}) || {}).fetch("ref", "")
      pinned_ref = ref.match(/needs\.([A-Za-z0-9_-]+)\.outputs\.(trusted_sha|head_sha|base_sha|commit_sha)/)
      if pinned_ref
        trusted_source, output_name = pinned_ref.captures
        producer = workflow.fetch("jobs", {}).fetch(trusted_source, {})
        producer_output = (producer.fetch("outputs", {}) || {}).fetch(output_name, "")
        if producer_output.empty?
          errors << "#{path}:#{trusted_source} must expose the trusted SHA consumed by #{job_name}"
        end
        unless declared_needs.include?(trusted_source)
          errors << "#{path}:#{job_name} must directly need #{trusted_source} to consume its trusted SHA"
        end
      end
    end

    if action_index
      trusted_index = steps.index do |step|
        step.is_a?(Hash) && step.fetch("id", "") == "trusted" && step.fetch("run", "").include?("git rev-parse HEAD")
      end
      trusted_output = (job.fetch("outputs", {}) || {}).fetch("trusted_sha", "")
      if moving_checkouts.any? && (trusted_index.nil? || trusted_index >= action_index || !trusted_output.include?("steps.trusted.outputs.sha"))
        errors << "#{path}:#{job_name} must capture and expose its exact trusted checkout SHA before the Codex Action"
      end
    elsif moving_checkouts.any?
      errors << "#{path}:#{job_name} must check out the captured trusted SHA"
    end
  end
end

publisher_token = "${{ secrets.CODEX_GITHUB_TOKEN }}"
publisher_login = "${{ vars.CODEX_PUBLISHER_LOGIN }}"
publisher_specs = [
  [".github/workflows/codex_jira_dispatch.yml", "publish-pr", "publish"],
  [".github/workflows/codex_jira_dispatch.yml", "publish-published-pr-repair-1", "publish"],
  [".github/workflows/codex_pr_review_feedback.yml", "codex-review-publish", "publish"],
  [".github/workflows/codex_pr_review_feedback.yml", "codex-review-external-republish", "publish"],
  [".github/workflows/codex_merge_conflict_resolution.yml", "publish-conflict-resolution", "publish"],
  [".github/workflows/codex_runner_smoke.yml", "branch-smoke", "smoke"],
]
publisher_job_keys = publisher_specs.map { |path, job_name, _| [path, job_name] }

publisher_specs.each do |path, job_name, publish_step_id|
  workflow = YAML.load_file(path)
  job = workflow.fetch("jobs", {}).fetch(job_name, {})
  permissions = job.fetch("permissions", {}) || {}
  unless permissions == { "contents" => "read" }
    errors << "#{path}:#{job_name} must give the default GITHUB_TOKEN contents: read only"
  end

  job_env = job.fetch("env", {}) || {}
  if job_env.key?("GH_TOKEN") || job_env.key?("CODEX_PUBLISHER_LOGIN")
    errors << "#{path}:#{job_name} must not expose publisher credentials at job scope"
  end

  steps = job.fetch("steps", [])
  verify_index = steps.index do |step|
    step.is_a?(Hash) &&
      step.fetch("run", "") == "python3 -I .github/scripts/codex-verify-publisher.py"
  end
  publish_index = steps.index do |step|
    step.is_a?(Hash) && step.fetch("id", "") == publish_step_id
  end

  if verify_index.nil? || publish_index.nil? || verify_index >= publish_index
    errors << "#{path}:#{job_name} must verify the trusted publisher before publishing"
    next
  end

  [verify_index, publish_index].each do |index|
    env = steps.fetch(index).fetch("env", {}) || {}
    unless env.fetch("GH_TOKEN", "") == publisher_token &&
           env.fetch("CODEX_PUBLISHER_LOGIN", "") == publisher_login
      errors << "#{path}:#{job_name} must scope the trusted publisher secret and login to verifier/publisher steps"
    end
  end

  steps.each_with_index do |step, index|
    next unless step.is_a?(Hash)
    next if [verify_index, publish_index].include?(index)
    if step.inspect.include?("CODEX_GITHUB_TOKEN")
      errors << "#{path}:#{job_name} exposes the publisher token outside verifier/publisher steps"
    end
  end
end

Dir[".github/workflows/*.yml", ".github/workflows/*.yaml"].each do |path|
  workflow = YAML.load_file(path)
  workflow.fetch("jobs", {}).each do |job_name, job|
    if job.inspect.include?("CODEX_GITHUB_TOKEN") &&
       !publisher_job_keys.include?([path, job_name])
      errors << "#{path}:#{job_name} must not receive the trusted publisher token"
    end
  end
end

if File.exist?(".github/workflows/codex_approve_pr_workflows.yml") ||
   Dir[".github/workflows/*"].any? { |path| File.read(path).include?("codex_approve_pr_workflows") }
  errors << "unsupported pull-request workflow auto-approval machinery must not be present"
end
[
  ".github/workflows/existing_flyway_change_prevention.yml",
  ".github/workflows/flyway-dupe-checker.yml",
].each do |path|
  workflow = YAML.load_file(path)
  unless workflow.fetch("permissions", {}) == { "contents" => "read" }
    errors << "#{path} must use contents: read permissions"
  end
  if File.read(path).include?("CODEX_GITHUB_TOKEN")
    errors << "#{path} must not receive the trusted publisher token"
  end
  workflow.fetch("jobs", {}).each do |job_name, job|
    checkouts = job.fetch("steps", []).select do |step|
      step.is_a?(Hash) && step.fetch("uses", "").start_with?("actions/checkout@")
    end
    if checkouts.empty? || checkouts.any? { |step| step.fetch("with", {}).fetch("persist-credentials", true) != false }
      errors << "#{path}:#{job_name} must use checkout without persisted credentials"
    end
  end
end

abort(errors.join("\n")) unless errors.empty?
puts "workflow yaml and Codex security invariants ok"
RUBY
else
  warn "ruby is not installed; skipping workflow YAML and Codex security validation"
fi

log "Checking Flyway migration numbers are unique"
flyway_script_count="$(find ./flyway -type f | wc -l | trim_count)"
unique_flyway_prefix_count="$(find ./flyway -type f | awk -F '__' '{print $1}' | sort -u | wc -l | trim_count)"

echo "Flyway script count: ${flyway_script_count}"
echo "Unique Flyway migration prefix count: ${unique_flyway_prefix_count}"

if [[ "${flyway_script_count}" != "${unique_flyway_prefix_count}" ]]; then
  echo "Duplicate Flyway migration prefixes found:" >&2
  find ./flyway -type f | awk -F '__' '{print $1}' | sort | uniq -d >&2
  exit 1
fi

base_ref="origin/${base_branch}"
if [[ "${fetch_base}" == "true" ]]; then
  log "Fetching ${base_ref}"
  git fetch origin "${base_branch}" >/dev/null
fi

if git rev-parse --verify --quiet "${base_ref}" >/dev/null; then
  merge_base="$(git merge-base "${base_ref}" HEAD)"

  log "Checking existing Flyway files were not modified or deleted"
  changed_files="$(git diff --name-status "${merge_base}" -- || true)"
  if [[ -n "${changed_files}" ]]; then
    echo "${changed_files}"
  else
    echo "No changes detected against ${base_ref}."
  fi

  if echo "${changed_files}" | grep -E '^(M|D)[[:space:]]+flyway/' >/dev/null 2>&1; then
    echo "Existing files under flyway/ were modified or deleted. New Flyway files are allowed; changing existing ones is blocked." >&2
    exit 1
  fi
else
  warn "Could not find ${base_ref}; skipping PR-style diff guardrails"
fi

if [[ "${mode}" == "checks-only" ]]; then
  log "Local pipeline checks completed"
  exit 0
fi

if [[ "${mode}" == "full" || "${REQUIRE_DOCKER:-false}" == "true" ]]; then
  log "Checking Docker daemon"
  require_command docker
  docker info >/dev/null
elif ! command -v docker >/dev/null 2>&1; then
  warn "docker is not installed; skipping Docker check for ${mode} mode"
fi

if [[ "${mode}" == "full" ]]; then
  gradle_args=(
    --no-daemon
    clean
    check
    build
    functional
    smoke
    jacocoUnitCoverageVerification
    jacocoIntegrationCoverageVerification
  )
else
  read -r -a gradle_fast_tasks <<<"${GRADLE_FAST_TASKS:-clean check}"
  gradle_args=(--no-daemon "${gradle_fast_tasks[@]}")
fi

log "Running Gradle verification: ./gradlew ${gradle_args[*]}"
./gradlew "${gradle_args[@]}"

if [[ "${include_dependency_check}" == "true" ]]; then
  log "Running OWASP dependency check"
  ./gradlew --no-daemon dependencyCheckAnalyze
fi

log "Local pipeline completed"
