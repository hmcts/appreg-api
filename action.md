# Action Plan (Project-Structure Aligned)

## Objective
Run `appreg-api` locally and validate the full repo-specific verification path (unit + integration + functional + smoke + coverage + API contract assets).

## 1) Environment and dependencies
- [ ] Confirm `java -version` is JDK 21.
- [ ] Start dependencies: `docker compose up -d postgres`.
- [ ] Choose a Spring profile for seeded data and set it for local run:
- [ ] `functional` profile uses `flyway/data_population`.
- [ ] `testing` profile uses `flyway/data_population_test`.
- [ ] Example: `SPRING_PROFILES_ACTIVE=functional APP_REG_SCHEMA=functionalschema ENABLE_DB_MIGRATE=true ./gradlew bootRun`.
- [ ] Example: `SPRING_PROFILES_ACTIVE=testing APP_REG_SCHEMA=testingschema ENABLE_DB_MIGRATE=true ./gradlew bootRun`.
- [ ] Export auth variables used by the app if required in your environment (`AZURE_TENANT_ID`, `AZURE_AUDIENCE_ID`, `AZURE_ISSUER`).

## 2) Local runtime validation
- [ ] Run app: `./gradlew bootRun`.
- [ ] Confirm service is up on port `4550`.
- [ ] Validate endpoints:
- [ ] Health: `http://localhost:4550/health`.
- [ ] OpenAPI JSON: `http://localhost:4550/specs/openapi.json`.

## 3) Code quality and verification tasks
- [ ] Baseline build: `./gradlew clean build`.
- [ ] Standard verification: `./gradlew check` (this finalizes with `integration` in this project).
- [ ] Run explicit suites:
- [ ] Unit tests: `./gradlew test`.
- [ ] Integration tests: `./gradlew integration`.
- [ ] Functional tests: `./gradlew functional`.
- [ ] Smoke tests: `./gradlew smoke`.

## 4) Coverage gates (80% thresholds)
- [ ] Unit coverage report: `./gradlew jacocoUnitReport`.
- [ ] Unit coverage gate: `./gradlew jacocoUnitCoverageVerification`.
- [ ] Integration coverage report: `./gradlew jacocoIntegrationReport`.
- [ ] Integration coverage gate: `./gradlew jacocoIntegrationCoverageVerification`.

## 5) API spec and contract artifacts
- [ ] Generate bundled OpenAPI: `./gradlew bundleOpenApi`.
- [ ] Copy bundled spec to app resources: `./gradlew copySpecIntoMainResources`.
- [ ] Validate `src/main/resources/openapi/openapi.yaml` changes are reflected in generated `openapi.json` output.

## 6) Optional repository checks
- [ ] Security dependency scan: `./gradlew dependencyCheck`.
- [ ] Formatting: `./gradlew spotlessApply`.
- [ ] Rewrite static analysis recipes: `./gradlew rewriteRun`.
- [ ] Local SonarQube run (if token/server available): `./gradlew localSonarqube`.

## 7) API exercise assets
- [ ] Use `postman/dev/App Registration.postman_collection.json` for local verification.
- [ ] Use `postman/test/hmcts-app-reg.postman_collection.json` for broader environment-aligned scenarios.

## Definition of Done
- [ ] App starts locally and serves `health` and `specs/openapi.json` on `:4550`.
- [ ] `test`, `integration`, `functional`, and `smoke` tasks pass.
- [ ] Unit and integration Jacoco coverage verification tasks pass (80% minimum).
- [ ] OpenAPI bundle task succeeds and contract output is validated.
- [ ] No blocking issues in `check` and selected optional quality/security tasks.
