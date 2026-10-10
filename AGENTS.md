# Midas Project

This project is a modern web application consisting of a REST API backend and a
Vaadin frontend, built using **Quarkus 3** and **Java 25**.

## Project Overview

- **Architecture**:
  - **`midas-api`**: Backend REST API service.
  - **`midas-gui`**: Frontend web application using Vaadin Flow.
- **Build System**: Maven (multi-module) with a root `Makefile` orchestrating
  the workflow. The quality gates come from devkit's parent POM, which the root
  `pom.xml` inherits; the root `pom.xml` carries the configuration both modules
  share.

## Building and Running

The project uses a `Makefile` at the root to simplify common tasks. Its shared
targets come from devkit (see Shared configuration below); `make <target>
ONLY=<module>` limits a target to one module, and `make help` lists every
target.

### Prerequisites

- **JDK 25**
- **Maven 3.9.9+**
- **Python 3.11+** (devkit's build scripts)
- **Node.js 22.22.2+** with npm (devkit's Markdown gate in `make lint` and
  `make format`)
- **curl**, **tar** and **sha256sum** or **shasum** (devkit downloads gitleaks
  and markdownlint on the first `make lint`, which needs network access)
- **Docker** (for tests and production deployment)

### Common Commands

- `make audit`: Run OWASP dependency-check (needs `NVD_API_KEY`, see Security
  below).
- `make install`: Run `make check`, then `mvn clean install -DskipTests` for
  both modules.
- `make build`: Verify environment, package applications, and build Docker
  images.
- `make clean`: Clean Maven artifacts, logs, `midas-gui/node_modules` and the
  generated frontend sources.
- `make format`: Fix what markdownlint can in Markdown, then run
  `spotless:apply` to auto-format code for both modules.
- `make gate`: Run the push gate (`make lint-repo test`) and, when it passes on
  a strictly clean tree, record HEAD, so `pre-push` skips it for that commit.
- `make hooks`: Install devkit's git hooks, once per clone and again after
  every devkit bump (`make check` warns while a copy is stale): `pre-commit`
  runs `make lint-repo`, `pre-push` runs `make lint-repo test` unless `make
  gate` passed the HEAD being pushed.
- `make lint`: Run the repository gates (`make lint-repo`), then the quality
  gate plugins (including `spotless:check`) for both modules.
- `make test`: Run full Maven test suite for both modules (requires Docker for
  API tests).
- `make dev-api`: Start the backend API in Quarkus Dev Mode (port 8080).
- `make dev-gui`: Start the frontend GUI in Quarkus Dev Mode (port 8081).
- `make kill`: Stop this project's Quarkus/Maven JVMs and remove its docker
  compose containers and network (`docker compose down`; volumes are kept).
- `make openapi`: Regenerate the committed OpenAPI contract snapshot
  (`midas-api/openapi.yaml`).
- `make password`: Print the bcrypt hash of a password, for the API demo data
  (`db/demo/R__demo_data.sql`) or the README's break-glass admin reset. It
  prompts, or reads the password from stdin, so it never appears on a command
  line.
- `make regen-lockfile`: Update `midas-gui/package-lock.json` to `package.json`
  with the pinned Node.js, once `npm ci` rejects the pair (see Frontend
  toolchain).

## Development Conventions

### Code Quality & Standards

The project enforces very strict quality gates:

- **Repository gates** (devkit's `make lint-repo`, the first stage of `make
  lint`, over the whole repository): `lint-pins` requires every container image
  in workflows, composite actions, compose files and Dockerfiles as
  `<repository>:<tag>@sha256:<digest>` and every workflow action by full commit
  SHA with a `# vX.Y.Z` comment; the `gregordietrich/` namespace `make release`
  publishes to is exempt through `[pins] first-party` in `devkit.toml`, and its
  compose services carry `pull_policy: build`. It does not read `.properties`:
  the Dev Services image in `midas-api`'s `application.properties` is pinned by
  digest alone, since Testcontainers rejects `tag@sha256:`, with its tag in the
  `# renovate:` comment above it, and Renovate bumps it together with compose's
  `db`. Renovate keeps postgres on 18.x: a major is a person's PR, moving both
  pins, `quarkus.datasource.db-version` and the `db` volume's mount path, and
  the existing volume needs `pg_upgrade`. `lint-secrets` runs gitleaks over the
  full history and the staged and unstaged changes to tracked files (untracked
  files are not scanned); a triaged finding goes in `.gitleaksignore` by
  fingerprint. `lint-md` runs markdownlint with devkit's profile (80 columns;
  `LICENSE.md`, verbatim license text, is excluded in `.markdownlintignore`).
  `lint-decisions` skips: there is no `docs/decisions.md`.
- **Formatting**: Spotless with Eclipse Formatter (Google Java Style baseline).
  Use `make format` to auto-fix formatting.
- **Linting**: Spotless (`spotless:check`), Checkstyle (devkit's shared rules
  plus the project's own in `checkstyle-project.xml`; engine pinned via
  `${checkstyle.version}`), PMD (with CPD for duplicate detection).
- **Static Analysis**: SpotBugs (Max effort, Low threshold), ErrorProne, and
  NullAway (strict nullability checks). The compiler runs with `-Werror` and
  `-Xlint:all` (minus `serial`, `this-escape`, `classfile`), so any javac or
  ErrorProne warning fails the build.
- **Testing**: JUnit 5, Mockito, AssertJ, and REST Assured.
- **Coverage**: JaCoCo enforcement. The `report` goal is bound to
  `post-integration-test` so it includes `*IT` coverage; surefire and failsafe
  append to the same `target/jacoco.exec`, and a report generated at `test`
  would silently omit every integration test.
- **Security**: OWASP Dependency Check via `make audit` (fails on CVSS >= 7;
  false positives go in the root `dependency-check-suppression.xml`, pinned to
  the exact package version). It is deliberately not bound to a lifecycle phase,
  so `make test` does not depend on the network, an NVD API key or a clean CVE
  feed; CI runs it as its own `audit` job, which fails on findings, and fails as
  well when the `NVD_API_KEY` secret is unset. It needs `NVD_API_KEY` from the
  environment or the gitignored `.env.build` (not `.env`, which docker-compose
  reads); without a key it fails rather than running slowly (gregor/devkit#11).
  License enforcement blocks commercial/proprietary licenses.
- **Frontend pins**: devkit's `check_frontend_deps.py` verifies the committed
  `midas-gui/package.json` / `package-lock.json` against Vaadin's own version
  manifest for the pinned `${vaadin.version}`, plus the security minimums in
  `devkit.toml`'s `[frontend.min-pins]`. Runs as part of `make lint` and `make
  install`.
- **Frontend toolchain**: `vaadin.node.version` in `midas-gui/pom.xml` (Vaadin's
  default for the pinned `${vaadin.version}`) is the Node.js every frontend
  build uses: the production profile, the Quarkus build step behind `make
  install`/`make test`, and dev mode. Vaadin downloads it to `~/.vaadin` and
  ignores any `node` on `PATH`, so `package-lock.json` comes out the same on
  every machine. Bump it with `vaadin.version`.
- **Frontend installs use `npm ci`** (`vaadin.ci.build` in `midas-gui/pom.xml`):
  builds install exactly what `package-lock.json` records and fail when
  `package.json` disagrees with it, e.g. after a Vaadin bump or an `@NpmPackage`
  change. Fix that with `make regen-lockfile` and commit both files. It keeps
  the lockfile and lets npm change only the entries `package.json` no longer
  matches; deleting the lockfile instead re-seeds it from Vaadin's bundled one
  and moves unrelated packages, some to older versions. Dev mode's runtime dev
  server has no `npm ci` option and runs `npm install`, which leaves an in-sync
  lockfile unchanged.

### Shared configuration from devkit

`checkstyle.xml`, `pmd-ruleset.xml`, `eclipse-formatter.xml`, the parent POM
that configures and activates every Maven gate plugin and pins the gate tools'
versions (`de.vptr.devkit:devkit-parent`), the shared Make targets and the
scripts behind them come from devkit, pinned in `devkit.toml` and linked into
the checkout as `.devkit` by `./devkitw`. The root `pom.xml` inherits that
parent through the link
(`<relativePath>.devkit/java/parent/pom.xml</relativePath>`). Any `make` target
creates the link; a bare `./mvnw` or an IDE's Maven import fails on a fresh
clone ("Non-resolvable parent POM") until it exists. They are not hand-kept
copies: a shared gate changes in devkit, is released there, and is adopted here
by bumping `version` and `commit` in `devkit.toml` together with the root pom's
`<parent><version>` (the tag without the `v`); a release whose contract changes
lists its upgrade steps in devkit's README. `renovate.json` proposes that bump
as one PR covering both files; after merging it, run `make hooks` in each clone,
since the hooks are copies a bump does not refresh. Never edit `.devkit/` or
`devkitw`.

The poms keep only the project's own values, as devkit's contract
(`.devkit/docs/contract.md`, "Parent POM") defines them: overrides of the
parent's properties in the root pom (`error-prone.extra.args` excludes generated
sources from Error Prone), `nullaway.annotated.packages` and JaCoCo's
`<excludes>` in each module, and midas-gui's Checkstyle `<sourceDirectories>`
and PMD `<excludeRoots>` that keep its generated sources out of those gates.
Never define one of the parent's `*.version` properties (pass
`-D<name>=<version>` on the command line to try another tool version), and never
redeclare `maven-compiler-plugin`'s `<compilerArgs>` or
`<annotationProcessorPaths>` without `combine.children="append"`: Maven merges a
redeclared list by position and silently drops the inherited `-Werror`, Error
Prone or NullAway. Every target that runs Maven fails on either.

The project-specific gate files stay here: `checkstyle-project.xml` at the root
(the project's own Checkstyle rules, which the parent runs as execution
`project`; its `SuppressionFilter` reuses each module's
`checkstyle-suppressions.xml`), `dependency-check-suppression.xml`,
`spotbugs-exclude.xml` and `checkstyle-suppressions.xml`. The last two share a
common baseline with the `aimathtutor` repository and carry per-module sections.
Deviations are allowed only where they are unavoidable (package names, Vaadin
present or absent, multi-module path resolution) or carry a written reason in
the file itself — never as a convenience. When changing one of these, point out
that `aimathtutor` needs the same change, but never edit that repository unless
explicitly asked to.

You MUST ALWAYS run ALL quality gates AND tests via `make gate` before claiming
you are done and before every push (`make test` alone runs no repository gate).
Commit first, then run `make gate` on the clean tree, then push: only a
passing run on a strictly clean tree records HEAD, and `pre-push` skips its own
run of the same gate only for that HEAD, so a run over uncommitted edits is
repeated by the push. If any quality gates or tests fail, fix them in a new
commit and run `make gate` again, until a final pass succeeds.

### Design Patterns & Logic

- **API Response**: Standardized JSON error payloads with `status`, `error`, and
  `message`.
- **API Contract**: The API surface is described by OpenAPI, generated from the
  JAX-RS and `@APIResponse` annotations. `midas-api/openapi.yaml` is a committed
  snapshot of that contract and `OpenApiContractTest` fails the build when the
  two diverge. After any change to a resource signature, response status, or
  DTO, run `make openapi` and commit the diff. The contract is also served live
  at `/q/openapi`, with Swagger UI at `/q/swagger-ui` in dev mode. The GUI's
  REST clients and response DTOs are generated from that snapshot at build time
  (openapi-generator in `midas-gui/pom.xml`), so a change to a path, parameter
  or response schema that breaks the GUI fails its compile. Request DTOs,
  `AuthClient` and `BackendHealthClient` stay hand-written; the generated
  clients use a request DTO by its schema name, so it must keep that name, and
  only the `*HttpContractTest`s check its fields, or catch two same-typed
  parameters trading places. Client-wide annotations (`@Timeout`,
  `@RegisterClientHeaders`) live in the template
  `midas-gui/src/main/openapi-templates/api.mustache`, and so does each method's
  `@Retry`: only connection failures and timeouts are retried, never an HTTP
  error status, and only for idempotent verbs (`UiConstants.RETRIES_<VERB>`;
  POST and PATCH get none, a verb without a constant fails the compile).
- **Database**: Flyway owns the schema in every profile and Hibernate only
  validates it. A production launch never cleans and refuses a dev or test
  profile (`FlywayCleanGuard`, keyed on LaunchMode, #31), and refuses any
  Hibernate schema-management strategy but `validate` or `none`
  (`SchemaManagementGuard`, #48). Migrations live in
  `midas-api/src/main/resources/db/migration`; a released migration is never
  edited, a new one is added instead (V1 may still change until the first
  release). Flyway refuses a database built from an earlier V1 (checksum
  mismatch) or the pre-Flyway `init.sql` (no schema history): recreate it with
  `docker compose down -v` and never enable `baselineOnMigrate` (it would keep
  the old seeded accounts). Demo accounts live in `db/demo/R__demo_data.sql`,
  loaded in dev/test only; update it in the same change as a migration that
  alters a table it seeds. Production seeds no account: any start that finds the
  `users` table empty creates the admin from `APP_BOOTSTRAP_ADMIN_USERNAME` /
  `APP_BOOTSTRAP_ADMIN_PASSWORD`, or fails fast.
- **Last administrator**: an active administrator is an activated, unbanned user
  whose rank holds every permission (`AdministratorGuard.PERMISSIONS`). Any
  service method that deletes users or changes a user's `activated`, `banned` or
  rank, or a rank's permissions, runs its change inside
  `AdministratorGuard.guard(...)`, so the API answers 422 to a change that
  leaves no active administrator; recovery is the README's break-glass section.
  A user write outside the guard, such as the password change, never sets those
  fields and relies on `UserEntity`'s `@DynamicUpdate` to write only the columns
  it changes, so it never writes back a stale `activated`, `banned` or rank
  (#77).
- **Privilege ceiling**: `user:edit` is not administrator-equivalent.
  - Any service method that creates, changes or deletes a user or a rank takes
    the `currentUsername` and checks `PrivilegeCeiling`, a snapshot of the
    caller's rank, in the change's transaction (inside the guard where it runs
    one).
  - It refuses with 403 a target user or rank that holds a permission the
    caller's rank lacks, and a rank created, edited or given to a user that
    grants one; equal ranks pass. It refuses with 403 a caller it finds banned,
    not activated or gone, since authentication checks those only as a request
    starts (#78).
  - Two deliberate exceptions: self-service on one's own account
    (`/users/me/password`) is not checked, and creating a user at the configured
    default rank skips the grant check (so `user:add` alone can still create
    users).
  - The overloads without a caller apply no ceiling and are for bootstrap and
    tests only; resources always pass the caller.
  - The same snapshot decides a user change's role-gated fields: without
    `user-rank:edit` the rank, without `admin:view` `banned` and `activated`
    keep their current values (a new user's defaults on create: the default
    rank, not banned, not activated). The service decides them in the change's
    transaction (`PrivilegeCeiling.holds`, inside the guard where it runs one),
    never the resource from the SecurityContext, which would judge the caller by
    roles read outside the transaction and, on a change, write a value read
    there back over a concurrent change (#54).
- **Pagination**: Normalization of `page`, `size`, and `limit` parameters at
  both GUI and API levels.
- **Security**: Basic Auth with BCrypt. The GUI stores backend credentials
  behind an opaque session ID.
- **Architecture**: Prefer composition and clear abstraction. Use MicroProfile
  Fault Tolerance in the GUI for resilient API communication.

## Directory Structure

- `midas-api/`: Backend service source and configuration.
- `midas-gui/`: Frontend application source and configuration.
- `scripts/`: Project-only shell scripts (build, dev mode, OpenAPI snapshot,
  password, release).
- `renovate.json`: the forge's weekly Renovate run proposes devkit bumps
  (`devkit.toml` and the parent POM version, one PR), and bumps of the pinned
  images (compose, Dockerfiles, the Dev Services image) and workflow actions.
  Java stays on LTS releases. Image tags stay exact, never floating (`18-alpine`,
  `25-jre`): its versioning rules count the Alpine version as part of the
  version, so a newer Alpine arrives as an ordinary update.
- `devkitw`, `devkit.toml`: The devkit wrapper and its pin; `.devkit` is the
  gitignored link it creates.
- `.claude/settings.json`: enables the agent-kit plugin for Claude Code, which
  carries the generic agent method (delegation, review, debugging, design,
  planning) as skills and its guards as hooks; install it per machine as its
  README describes. Its marketplace entry declares `"autoUpdate": true` so no
  machine needs the `/plugin` auto-update toggle, which writes into the first
  settings file declaring the marketplace and would dirty this one: to change
  it on one machine, declare the marketplace in the gitignored
  `.claude/settings.local.json` instead.
- `docker-compose.yml`: Orchestration for local production-like environments.
- `.gitea/workflows/ci.yml`: Gitea Actions CI on pushes to `main` and on pull
  requests: `make lint-repo`, `make install` and `make test` in one job (full
  history, which `lint-secrets` needs), `make audit` in another.
  It reads two secrets: `DEVKIT_TOKEN`, a read-only deploy token on devkit that
  fetches it over HTTPS (the forge requires sign-in even for public repos), and
  `NVD_API_KEY`. Images are not built or published in CI; that stays `make
  release`.
- `Makefile`: Entry point for all development tasks.
- `README.md`: General project documentation.
