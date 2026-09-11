#!/usr/bin/env ruby
# Regression guard only. GitHub environment and runner-group policies are the boundary.
require "yaml"
require "json"

root = ARGV.fetch(0, ".")
contracts = {
  "codex_jira_dispatch.yml" => ["workflow_dispatch", "codex-plan-action"],
  "codex_pr_review_feedback.yml" => ["issue_comment", "detect-codex-pr"],
  "codex_merge_conflict_resolution.yml" => ["issue_comment", "detect-conflicted-pr"],
  "codex_runner_smoke.yml" => ["workflow_dispatch", "codex-auth-smoke"]
}
secret_environments = {
  "CODEX_OPENAI_API_KEY" => "codex-model",
  "CODEX_GITHUB_APP_PRIVATE_KEY" => "codex-publisher",
  "CODEX_JIRA_PR_NOTIFY_URL" => "codex-publisher",
  "CODEX_SONAR_TOKEN" => "codex-status"
}
errors = []
contracts.each do |filename, (event, entry)|
  path = File.join(root, ".github/workflows", filename)
  workflow = YAML.load_file(path)
  triggers = workflow["on"] || workflow[true] || {}
  unless triggers.is_a?(Hash) && triggers.keys == [event]
    errors << "#{filename}: only #{event} may start this workflow"
  end
  if event == "issue_comment" && triggers.fetch(event, {}) != {"types" => ["created"]}
    errors << "#{filename}: only newly created PR conversation comments are supported"
  end
  permissions = workflow["permissions"]
  unless permissions.is_a?(Hash) && permissions.values.all? { |value| ["read", "none"].include?(value) }
    errors << "#{filename}: workflow permissions must be explicitly read-only or empty"
  end
  jobs = workflow.fetch("jobs")
  guard = "github.ref == format('refs/heads/{0}', github.event.repository.default_branch) && " \
    "github.workflow_ref == format('{0}/.github/workflows/#{filename}@refs/heads/{1}', github.repository, github.event.repository.default_branch)"
  unless jobs.fetch(entry).fetch("if", "").split.join(" ") == guard
    errors << "#{filename}: entry job must require the default branch and exact workflow ref"
  end
  jobs.each do |name, job|
    encoded = JSON.generate(job)
    secrets = encoded.scan(/secrets\.([A-Z_][A-Z0-9_]*)/).flatten.uniq - ["GITHUB_TOKEN"]
    environments = secrets.map { |secret| secret_environments[secret] }.uniq
    if encoded.match?(/secrets\s*\[/) || (secrets - secret_environments.keys).any?
      errors << "#{filename}:#{name}: unknown or dynamic credential reference"
    elsif secrets.any? && (environments.size != 1 || job["environment"] != environments.first)
      errors << "#{filename}:#{name}: credentials require their dedicated protected environment"
    elsif secrets.empty? && job.key?("environment")
      errors << "#{filename}:#{name}: credential-free jobs must not receive environment secrets"
    end
    steps = job.fetch("steps", [])
    model_index = steps.index { |step| step.fetch("uses", "").start_with?("openai/codex-action@") }
    if model_index
      unless model_index == steps.length - 1 && job["environment"] == "codex-model"
        errors << "#{filename}:#{name}: model Action must finish its isolated model job"
      end
      if secrets != ["CODEX_OPENAI_API_KEY"]
        errors << "#{filename}:#{name}: model jobs may receive only the proxy API key"
      end
    end
    runner = job["runs-on"]
    if model_index
      unless runner.is_a?(Hash) && runner["group"] == "appreg-codex" &&
             ["codex-pilot-azure-aks", "codex-frontend-azure-aks"].include?(runner["labels"])
        errors << "#{filename}:#{name}: model execution requires the restricted appreg-codex runner group"
      end
    elsif runner != "ubuntu-latest"
      errors << "#{filename}:#{name}: non-model jobs must use GitHub-hosted compute"
    end
  end
  next unless filename == "codex_runner_smoke.yml"

  unless jobs.fetch("branch-smoke")["runs-on"] == "ubuntu-latest" &&
         jobs.fetch("branch-smoke")["needs"] == "verify-auth-response"
    errors << "#{filename}: publisher smoke must follow fresh response verification on GitHub-hosted compute"
  end
  verifier = jobs.fetch("verify-auth-response")
  unless verifier["permissions"] == {} && verifier["needs"] == "codex-auth-smoke"
    errors << "#{filename}: smoke response verification must be credential-free"
  end
  ["codex-auth-smoke", "branch-smoke"].each do |name|
    checkout = jobs.fetch(name).fetch("steps").find { |step| step.fetch("uses", "").start_with?("actions/checkout@") }
    unless checkout && checkout.fetch("with", {})["ref"] == "${{ github.workflow_sha }}"
      errors << "#{filename}:#{name}: smoke scripts must use the gated workflow revision"
    end
  end
end
hosted_path = File.join(root, ".github/workflows/codex_trust_checks.yml")
hosted = YAML.load_file(hosted_path)
hosted_events = hosted["on"] || hosted[true] || {}
hosted_job = hosted.fetch("jobs", {}).fetch("trust-contract", {})
unless hosted_events.key?("pull_request") && hosted_events.key?("push") &&
       hosted_job["runs-on"] == "ubuntu-latest" && !hosted_job.key?("if") &&
       !hosted_job.key?("continue-on-error")
  errors << "codex_trust_checks.yml: hosted trust validation must be mandatory"
end
[
  "ruby .github/scripts/check-codex-workflow-trust.rb",
  "python3 .github/scripts/test-codex-workflow-trust.py",
  "python3 .github/scripts/test-audit-codex-trust-settings.py",
  "python3 .github/scripts/test-codex-review-feedback.py",
  "python3 .github/scripts/test-codex-verification-bundle.py",
].each do |command|
  step = hosted_job.fetch("steps", []).find { |item| item["run"] == command }
  unless step && !step.key?("if") && !step.key?("continue-on-error")
    errors << "codex_trust_checks.yml: must unconditionally run #{command}"
  end
end
abort errors.join("\n") unless errors.empty?
puts "Codex workflow trust contracts passed (live settings require a separate audit)."
