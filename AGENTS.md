# Midas Project

This project is a modern web application consisting of a REST API backend and a Vaadin frontend, built using **Quarkus 3** and **Java 25**.

## Project Overview

- **Architecture**:
  - **`midas-api`**: Backend REST API service.
  - **`midas-gui`**: Frontend web application using Vaadin Flow.
- **Build System**: Maven (multi-module) with a root `Makefile` orchestrating the workflow. All shared configurations and quality gates are centralized in the root `pom.xml`.

## Building and Running

The project uses a `Makefile` at the root to simplify common tasks. Its shared targets come from devkit (see Shared configuration below); `make <target> ONLY=<module>` limits a target to one module, and `make help` lists every target.

### Prerequisites

- **JDK 25**
- **Maven 3.9.9+**
- **Docker** (for tests and production deployment)

### Common Commands

- `make audit`: Run OWASP dependency-check (needs `NVD_API_KEY`, see Security below).
- `make install`: Run `make check`, then `mvn clean install -DskipTests` for both modules.
- `make build`: Verify environment, package applications, and build Docker images.
- `make clean`: Clean Maven artifacts, logs, `midas-gui/node_modules` and the generated frontend sources.
- `make format`: Run `spotless:apply` to auto-format code for both modules.
- `make lint`: Run quality gate plugins (including `spotless:check`) for both modules.
- `make test`: Run full Maven test suite for both modules (requires Docker for API tests).
- `make dev-api`: Start the backend API in Quarkus Dev Mode (port 8080).
- `make dev-gui`: Start the frontend GUI in Quarkus Dev Mode (port 8081).
- `make kill`: Stop this project's Quarkus/Maven JVMs and remove its docker compose containers and network (`docker compose down`; volumes are kept).
- `make openapi`: Regenerate the committed OpenAPI contract snapshot (`midas-api/openapi.yaml`).
- `make password`: Print the bcrypt hash of a password for the API demo data (`db/demo/R__demo_data.sql`). It prompts, or reads the password from stdin, so it never appears on a command line.
- `make regen-lockfile`: Update `midas-gui/package-lock.json` to `package.json` with the pinned Node.js, once `npm ci` rejects the pair (see Frontend toolchain).

## Development Conventions

### Code Quality & Standards

The project enforces very strict quality gates:

- **Formatting**: Spotless with Eclipse Formatter (Google Java Style baseline). Use `make format` to auto-fix formatting.
- **Linting**: Spotless (`spotless:check`), Checkstyle (engine pinned via `${checkstyle.version}`), PMD (with CPD for duplicate detection).
- **Static Analysis**: SpotBugs (Max effort, Low threshold), ErrorProne, and NullAway (strict nullability checks). The compiler runs with `-Werror` and `-Xlint:all` (minus `serial`, `this-escape`, `classfile`), so any javac or ErrorProne warning fails the build.
- **Testing**: JUnit 5, Mockito, AssertJ, and REST Assured.
- **Coverage**: JaCoCo enforcement. The `report` goal is bound to `post-integration-test` so it includes `*IT` coverage; surefire and failsafe append to the same `target/jacoco.exec`, and a report generated at `test` would silently omit every integration test.
- **Security**: OWASP Dependency Check via `make audit` (fails on CVSS >= 7; false positives go in the root `dependency-check-suppression.xml`, pinned to the exact package version). It is deliberately not bound to a lifecycle phase, so `make test` does not depend on the network, an NVD API key or a clean CVE feed; CI runs it as its own `audit` job, which fails on findings, and fails as well when the `NVD_API_KEY` secret is unset. It needs `NVD_API_KEY` from the environment or the gitignored `.env.build` (not `.env`, which docker-compose reads); without a key it fails rather than running slowly (gregor/devkit#11). License enforcement blocks commercial/proprietary licenses.
- **Frontend pins**: devkit's `check_frontend_deps.py` verifies the committed `midas-gui/package.json` / `package-lock.json` against Vaadin's own version manifest for the pinned `${vaadin.version}`, plus security minimums. Runs as part of `make lint` and `make install`.
- **Frontend toolchain**: `vaadin.node.version` in `midas-gui/pom.xml` (Vaadin's default for the pinned `${vaadin.version}`) is the Node.js every frontend build uses: the production profile, the Quarkus build step behind `make install`/`make test`, and dev mode. Vaadin downloads it to `~/.vaadin` and ignores any `node` on `PATH`, so `package-lock.json` comes out the same on every machine. Bump it with `vaadin.version`.
- **Frontend installs use `npm ci`** (`vaadin.ci.build` in `midas-gui/pom.xml`): builds install exactly what `package-lock.json` records and fail when `package.json` disagrees with it, e.g. after a Vaadin bump or an `@NpmPackage` change. Fix that with `make regen-lockfile` and commit both files. It keeps the lockfile and lets npm change only the entries `package.json` no longer matches; deleting the lockfile instead re-seeds it from Vaadin's bundled one and moves unrelated packages, some to older versions. Dev mode's runtime dev server has no `npm ci` option and runs `npm install`, which leaves an in-sync lockfile unchanged.

### Shared configuration from devkit

`checkstyle.xml`, `pmd-ruleset.xml`, `eclipse-formatter.xml`, the shared Make targets and
the scripts behind them come from devkit, pinned in `devkit.toml` and linked into the
checkout as `.devkit` by `./devkitw`. Any `make` target creates the link; a bare
`./mvnw verify` (or any other gate goal) fails on a fresh clone until it exists. They are
not hand-kept copies: a shared gate changes in devkit, is released there, and is adopted
here by bumping `version` and `commit` in `devkit.toml`. Never edit `.devkit/` or `devkitw`.

The project-specific gate files stay here: `dependency-check-suppression.xml`,
`spotbugs-exclude.xml` and `checkstyle-suppressions.xml`. The last two share a common
baseline with the `aimathtutor` repository and carry per-module sections, and until
devkit ships a parent POM the gate plugin configuration in the root `pom.xml` is kept in
step with it by hand. Deviations are allowed only where they are unavoidable (package
names, Vaadin present or absent, multi-module path resolution) or carry a written reason
in the file itself — never as a convenience. When changing one of these, point out that
`aimathtutor` needs the same change, but never edit that repository unless explicitly
asked to.

You MUST ALWAYS run ALL quality gates AND tests via `make test` before claiming you are done, and especially before executing a git commit.
If any quality gates or tests fail, work on them until all problems are resolved, ensured by a final pass of `make test`.

### Design Patterns & Logic

- **API Response**: Standardized JSON error payloads with `status`, `error`, and `message`.
- **API Contract**: The API surface is described by OpenAPI, generated from the JAX-RS and
  `@APIResponse` annotations. `midas-api/openapi.yaml` is a committed snapshot of that contract and
  `OpenApiContractTest` fails the build when the two diverge. After any change to a resource
  signature, response status, or DTO, run `make openapi` and commit the diff. The contract is also
  served live at `/q/openapi`, with Swagger UI at `/q/swagger-ui` in dev mode.
  The GUI's REST clients and response DTOs are generated from that snapshot at build time
  (openapi-generator in `midas-gui/pom.xml`), so a change to a path, parameter or response schema that
  breaks the GUI fails its compile. Request DTOs, `AuthClient` and `BackendHealthClient` stay
  hand-written; the generated clients use a request DTO by its schema name, so it must keep that name,
  and only the `*HttpContractTest`s check its fields, or catch two same-typed parameters trading places. Client-wide annotations (`@Retry`, `@Timeout`,
  `@RegisterClientHeaders`) live in the template `midas-gui/src/main/openapi-templates/api.mustache`.
- **Database**: Flyway owns the schema in every profile and Hibernate only validates it. A production launch
  never cleans and refuses a dev or test profile (`FlywayCleanGuard`, keyed on LaunchMode, #31). Migrations live in
  `midas-api/src/main/resources/db/migration`; a released migration is never edited, a new one is added
  instead (V1 may still change until the first release). Flyway refuses a database built from an earlier V1
  (checksum mismatch) or the pre-Flyway `init.sql` (no schema history): recreate it with
  `docker compose down -v` and never enable `baselineOnMigrate` (it would keep the old seeded accounts).
  Demo accounts live in `db/demo/R__demo_data.sql`, loaded in dev/test only; update it in the same change
  as a migration that alters a table it seeds. Production seeds no account: any start that finds the `users` table empty creates
  the admin from `APP_BOOTSTRAP_ADMIN_USERNAME` / `APP_BOOTSTRAP_ADMIN_PASSWORD`, or fails fast.
- **Pagination**: Normalization of `page`, `size`, and `limit` parameters at both GUI and API levels.
- **Security**: Basic Auth with BCrypt. The GUI stores backend credentials behind an opaque session ID.
- **Architecture**: Prefer composition and clear abstraction. Use MicroProfile Fault Tolerance in the GUI for resilient API communication.

## Directory Structure

- `midas-api/`: Backend service source and configuration.
- `midas-gui/`: Frontend application source and configuration.
- `scripts/`: Project-only shell scripts (build, dev mode, OpenAPI snapshot, password, release).
- `devkitw`, `devkit.toml`: The devkit wrapper and its pin; `.devkit` is the gitignored link it creates.
- `docker-compose.yml`: Orchestration for local production-like environments.
- `.gitea/workflows/ci.yml`: Gitea Actions CI on pushes to `main` and on pull requests: `make install` and `make test` in one job, `make audit` in another. It reads two secrets: `DEVKIT_TOKEN`, a read-only deploy token on devkit that fetches it over HTTPS (the forge requires sign-in even for public repos), and `NVD_API_KEY`. Images are not built or published in CI; that stays `make release`.
- `Makefile`: Entry point for all development tasks.
- `README.md`: General project documentation.
