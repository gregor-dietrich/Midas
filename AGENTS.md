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
- `make kill`: Terminate all Quarkus/Maven processes and stop and remove **every** Docker container on the host.
- `make openapi`: Regenerate the committed OpenAPI contract snapshot (`midas-api/openapi.yaml`).
- `make password`: Generate a salt+hash for the API `init.sql`.
- `make regen-lockfile`: Update `midas-gui/package-lock.json` to `package.json` with the pinned Node.js, once `npm ci` rejects the pair (see Frontend toolchain).

## Development Conventions

### Code Quality & Standards

The project enforces very strict quality gates:

- **Formatting**: Spotless with Eclipse Formatter (Google Java Style baseline). Use `make format` to auto-fix formatting.
- **Linting**: Spotless (`spotless:check`), Checkstyle (engine pinned via `${checkstyle.version}`), PMD (with CPD for duplicate detection).
- **Static Analysis**: SpotBugs (Max effort, Low threshold), ErrorProne, and NullAway (strict nullability checks). The compiler runs with `-Werror` and `-Xlint:all` (minus `serial`, `this-escape`, `classfile`), so any javac or ErrorProne warning fails the build.
- **Testing**: JUnit 5, Mockito, AssertJ, and REST Assured.
- **Coverage**: JaCoCo enforcement. The `report` goal is bound to `post-integration-test` so it includes `*IT` coverage; surefire and failsafe append to the same `target/jacoco.exec`, and a report generated at `test` would silently omit every integration test.
- **Security**: OWASP Dependency Check via `make audit` (fails on CVSS >= 7; false positives go in the root `dependency-check-suppression.xml`, pinned to the exact package version). It is deliberately not bound to a lifecycle phase, so `make test` does not depend on the network, an NVD API key or a clean CVE feed; CI is meant to run it as its own step (#5). It needs `NVD_API_KEY` from the environment or the gitignored `.env.build` (not `.env`, which docker-compose reads); without a key the NVD download is rate-limited to a crawl. License enforcement blocks commercial/proprietary licenses.
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
- **Pagination**: Normalization of `page`, `size`, and `limit` parameters at both GUI and API levels.
- **Security**: Basic Auth with BCrypt. The GUI stores backend credentials behind an opaque session ID.
- **Architecture**: Prefer composition and clear abstraction. Use MicroProfile Fault Tolerance in the GUI for resilient API communication.

## Directory Structure

- `midas-api/`: Backend service source and configuration.
- `midas-gui/`: Frontend application source and configuration.
- `scripts/`: Project-only shell scripts (build, dev mode, OpenAPI snapshot, password, release).
- `devkitw`, `devkit.toml`: The devkit wrapper and its pin; `.devkit` is the gitignored link it creates.
- `docker-compose.yml`: Orchestration for local production-like environments.
- `Makefile`: Entry point for all development tasks.
- `README.md`: General project documentation.
