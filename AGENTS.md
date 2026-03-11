# Repository Guidelines

## Project Structure & Module Organization
`src/main/java` contains Spring Boot application code (controllers, services, validators, repositories, config). OpenAPI source lives in `src/main/resources/openapi`, and generated interfaces/models are emitted to `build/generated/openapi/src/main/java` under `uk.gov.hmcts.appregister.generated`. Database migrations and seed data are in `flyway/migrations`, `flyway/data_population`, and `flyway/data_population_test`. Tests are split across `src/test/java` (unit), `src/integrationTest/java`, `src/functionalTest/java`, and `src/smokeTest/java`, with shared fixtures/utilities in `src/testCommon/java`. Operational and platform assets live in `charts/`, `config/`, `docker-compose.yml`, `docker-compose-deps.yml`, `bin/`, and `init/`, while Oracle reference DDL/scripts are in `oracle/`.

## Build, Test, and Development Commands
- `./gradlew bootRun`: run the API locally (expects PostgreSQL and env vars).
- `./gradlew clean build`: full compile + unit tests + artifact build.
- `./gradlew check`: static checks (Checkstyle/PMD/SpotBugs and related verification tasks).
- `./gradlew integration`: run integration tests from `src/integrationTest`.
- `./gradlew functional`: run functional tests from `src/functionalTest`.
- `./gradlew smoke`: run smoke tests from `src/smokeTest`.
- `./gradlew jacocoUnitCoverageVerification`: enforce unit coverage threshold (80% minimum).
- `./gradlew jacocoIntegrationCoverageVerification`: enforce integration coverage threshold (80% minimum).
- `./gradlew dependencyCheck`: OWASP dependency vulnerability scan.
- `./gradlew localSonarqube`: local Sonar analysis (requires local SonarQube and `SONAR_TOKEN`).
- `./gradlew spotlessApply`: apply Java formatting using configured Spotless rules.
- `./bin/run-in-docker.sh -h`: show docker helper usage and available flags.
- `docker compose --profile testing up` or `docker compose --profile functionaltesting up`: start local dependency stack with seeded schemas.

## Coding Style & Naming Conventions
Target Java 21 with Spring Boot 4.x, Lombok, and MapStruct. The `uk.gov.hmcts.java` Gradle plugin enforces quality gates; Spotless applies `google-java-format` (AOSP style). Follow `.editorconfig`: 4-space indentation for Java, LF line endings, and 120-char maximum line length. Use constructor injection over field injection, prefer `@Slf4j` for logging, keep packages lowercase (for example, `uk.gov.hmcts.appregister.applicationlist`), and use PascalCase for classes/enums. Suffix Spring components consistently (`*Controller`, `*Service`, `*Repository`, `*Validator`). Keep ORM associations lazy by default and use `@EntityGraph`/projection-based queries for richer fetch plans instead of switching to eager fetches. Do not manually edit generated OpenAPI code in `uk.gov.hmcts.appregister.generated`.

## Testing Guidelines
JUnit 5 is the default across unit, integration, functional, and smoke suites. Mirror production packages and keep test class names as `*Test`. Keep coverage healthy by running `jacocoUnitCoverageVerification` and `jacocoIntegrationCoverageVerification` when logic changes.
- Must unit test decision-heavy logic: conditionals/branching, business rules, validations (including cross-field), mappers, error handling, sort/filter logic, and security-related guards.
- Skip trivial pass-through code (simple getters/setters, constants-only types, generated OpenAPI models/interfaces).
- Cover both happy and unhappy paths, including boundaries and null/empty input behavior where relevant.
- Mock external dependencies (repositories, clients, clocks/UUID sources) to keep unit tests deterministic and fast.
- Prefer expressive test names in `given_<precondition>_when_<action>_then_<outcome>` style.

## Code Review Guidance for Agents
- Review format: `"[Severity]: <Rule>\nProblem: ...\nWhy: ...\nFix: ..."` for actionable comments.
- `P0` blockers: security vulnerabilities, data corruption risks, auth/authorization bypasses, and behavior regressions.
- `P1` high risk: concurrency issues, inefficient queries (N+1/full scans), resource leaks, and sensitive-data logging.
- `P2` advisory: naming clarity, duplication, readability, and documentation gaps.
- Prefer clear names over abbreviations and clarity over dense one-liners.
- Validate consistency with established repository patterns (`controller -> service -> repository` layering, shared validators, centralized exception handling).
- Flag duplicated logic and suggest extraction to existing or new shared utility classes when reuse is likely.
- Ask contributors to run `./gradlew check build integration` before re-review on cross-cutting changes.

## Green Coding & Efficiency
- Push filtering, pagination, and aggregation to repository queries rather than loading large datasets into memory.
- Reuse Spring-managed beans (`ObjectMapper`, `WebClient`, DB clients) rather than constructing per-request objects.
- Use `@Transactional(readOnly = true)` for read-only flows and keep transactional boundaries in services.
- Keep logs purposeful and avoid large payload dumps or sensitive fields.
- Prefer batch operations and set-based updates where possible to minimize DB round trips.

## Definition of Done – Code Quality & Best Practice
- Each class has a single clear responsibility.
- Methods remain focused and readable; extract collaborators when complexity grows.
- Layering is preserved: controllers orchestrate HTTP concerns, services own business logic, repositories own persistence.
- Validation and error handling are explicit, consistent, and covered by tests.
- No dead code, no duplicated logic without reason, and no manual edits to generated artifacts.
- Required verification tasks pass locally for the scope of change.

## Commit & Pull Request Guidelines
Follow existing history style using ticket-prefixed messages (for example, `ARCPOC-1169: ...`) or clear conventional prefixes for maintenance work (for example, `fix(deps): ...`). Keep commits focused and self-contained; squash obvious WIP noise before opening a PR. PRs should include:
- concise change summary,
- linked Jira ticket,
- test evidence (commands run and outcomes),
- API/spec examples when behavior or contract changes.

## Security & Configuration Tips
Never commit secrets or tokens (for example: `AZURE_TENANT_ID`, `AZURE_AUDIENCE_ID`, `AZURE_ISSUER`, DB passwords, `SONAR_TOKEN`, `AZURE_DEVOPS_ARTIFACT_TOKEN`). Prefer environment variables or secret mounts (`/mnt/secrets/appreg/`). For local DB parity, use docker compose profiles (`testing` or `functionaltesting`) and keep credentials in local-only env files. Run dependency scanning (`./gradlew dependencyCheck`) before release-sensitive changes.
