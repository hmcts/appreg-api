# Application Register API

This file contains basic repository information and a setup guide for development.

Setup guide is copied from [Confluence](https://tools.hmcts.net/confluence/display/ARM/Backend+development+for+new+users).

## Prerequisites

- HMCTS.NET account
- GitHub account linked to HMCTS.NET, and Git installed
- Access to required GitHub repositories (see internal guide)
- Postgres database (local or remote) - see [Database setup]#DatabaseSetup) below

## Guide

The steps work on Windows, macOS, and Linux.

1. **Install IntelliJ IDEA.**
   You may request a license via your line manager. Other IDEs work, but IntelliJ is recommended.

2. **Ensure JDK 21.**
   IntelliJ includes a JDK. Confirm the version is 21.
   ```bash
   java -version
   ```

3. **Clone and open the repo.**
   ```bash
   git clone <repo-url>
   cd appreg-api
   ```
   Open the project directory in IntelliJ.

4. **Configure run settings.**
   After indexing completes, open `.run/appreg-api-bootRun.run.xml` and set these environment variables:
  - `OIDC_TENANT_ID`
  - `POSTGRES_HOST` - Only required if the database was not setup using the ./docker-compose.yml file, otherwise it defaults to `localhost`
  - `POSTGRES_PASS` - Only required if not setup using the ./docker-compose.yml file, otherwise it defaults to `password`
 -  `POSTGRES_DATABASE` - Only required if not setup using the ./docker-compose.yml file, otherwise it defaults to `appreg`
Ask an existing developer for values.

If the file is missing, create a new Run/Debug configuration in IntelliJ:
  - Type: **Gradle**
  - Tasks: `bootRun`
  - Environment variables: add the three variables above

5. **Run the application.**
   ```bash
   ./gradlew bootRun
   ```
   Or use the IntelliJ *appreg-api-bootRun* configuration by clicking the dropdown on the top right.

6. **Expected first-run errors.**
   PostgreSQL, SQL, or JDBC errors are expected until the database is provisioned and reachable. See [Database setup](#Database-setup) below.
## Common tasks

- **Build and test**
  ```bash
  ./gradlew clean build
  ```

- **Static analysis (includes Checkstyle)**
  ```bash
  ./gradlew check
  ```

- **Dependency vulnerability scan (OWASP Dependency-Check)**
  ```bash
  ./gradlew dependencyCheck
  ```

- **Code coverage report (JaCoCo)**
  ```bash
  ./gradlew jacocoTestReport
  # Report: build/reports/jacoco/test/html/index.html
  ```

- **Find dependency updates**
  ```bash
  ./gradlew dependencyUpdates -Drevision=release
  ```

## Plugins

- **HMCTS Gradle Java plugin**
  Applies HMCTS defaults for analysis.
  Repo: https://github.com/hmcts/gradle-java-plugin
  Includes:
  - **Checkstyle** — Style checks, part of `./gradlew check`. Docs: https://docs.gradle.org/current/userguide/checkstyle_plugin.html
  - **OWASP Dependency-Check** — Scans dependencies for known CVEs. Docs: https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/index.html

- **JaCoCo**
  Code coverage for Java. Docs: https://docs.gradle.org/current/userguide/jacoco_plugin.html

- **Spring Dependency Management**
  Maven-like dependency management. Docs: https://github.com/spring-gradle-plugins/dependency-management-plugin

- **Spring Boot**
  Reduces boilerplate for Spring applications. Site: http://projects.spring.io/spring-boot/

- **Gradle Versions Plugin**
  Reports dependency updates. Docs: https://github.com/ben-manes/gradle-versions-plugin


## Database setup

### Local Execution
Run the local docker compose file [`docker-compose.yml`](docker-compose.yml) to start a PostgreSQL instance with thd default schema

The database can be configured using the flyway migration CLI tool. The configuration is located at ./flyway. Or the
app register service can launch the baseline flyway content automatically by being run.

#### Baseline data setup

Baseline data is typically setup when running locally for the first time. The baseline data can be used by applying the
_SPRING_PROFILE_ACTIVE=local_ environment variable (see ./docker-compose-local.yaml.

## Calling the Rest API

The postman file can be located here [App Registration.postman_collection.json](App Registration.postman_collection.json). Import this
and set the environment variables to match your local setup. The postman suite can then be used to drive the application registration API.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE).
