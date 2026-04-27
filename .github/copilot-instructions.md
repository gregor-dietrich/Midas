---
applyTo: "**"
---

# Midas Project Coding Instructions

## General

Terse like caveman. Technical substance exact. Only fluff die.
Drop: articles, filler (just/really/basically), pleasantries, hedging.
Fragments OK. Short synonyms. Code unchanged.
Pattern: [thing] [action] [reason]. [next step].
ACTIVE EVERY RESPONSE. No revert after many turns. No filler drift.
Code/commits/PRs: normal. Off: "stop caveman" / "normal mode".

## Project Structure

- **midas-api**: Backend REST API service (Quarkus 3, Java 25, PostgreSQL)
- **midas-gui**: Frontend web application (Quarkus 3 + Vaadin Flow 25); no database — REST only
- Always cross-reference corresponding files between frontend and backend
- Reference existing code in same package for consistency and avoid duplication

## Development Workflow

1. Make changes following project coding standards
2. Add clear comments where necessary
3. Run `./mvnw clean install -DskipTests && ./mvnw test` to rebuild and test
4. Fix any compilation errors or test failures
5. If issues persist, run `./mvnw quarkus:dev` (in either or both sub-projects as necessary) and check logs

## Code Quality Standards

- **Clean, maintainable code**: Clarity over cleverness. Self-documenting code. Single Responsibility Principle.
- **Eliminate duplication (DRY)**: Extract common logic immediately. No copy-paste code.
- **Security by default**: Validate all input. Use least-privilege access. No hardcoded secrets. Sanitize output.
- **Performance mindset**: Avoid N+1 queries. Use pagination and caching. Profile before optimizing.
- **Code smells**: Methods >30 lines, classes >300 lines, magic numbers, unclear naming = refactor triggers.
- **Testing**: New business logic requires tests. Run tests locally before pushing.

---

## midas-api

### Build & Test

```shell
# Install (compile without tests)
./mvnw clean install -DskipTests

# Run all tests (requires Docker for Testcontainers)
./mvnw test

# Run a single test class
./mvnw test -Dtest=PostServiceTest

# Run a single test method
./mvnw test -Dtest=PostServiceTest#testCreatePost

# Dev mode with live reload (port 8800, Dev UI at /q/dev/)
./mvnw quarkus:dev

# Checkstyle + SpotBugs lint only (fast, ~10s)
./mvnw checkstyle:check spotbugs:check

# Full verify without tests or OWASP (compile + lint + license)
./mvnw verify -DskipTests -Ddependency-check.skip=true

# Checkstyle + SpotBugs + license report (skip tests)
./mvnw verify -DskipTests

# Full verify with OWASP CVE check (slow, requires NVD_API_KEY)
NVD_API_KEY=<key> ./mvnw verify -Ddependency-check.skip=false
```

Makefile shortcuts exist (`make test`, `make dev`, `make install`, etc.) — delegate to `scripts/`.

### Copilot CLI Hooks (API)

Hooks live in `.github/hooks/` and are active automatically — no installation needed.

- **lint-guard** (`PreToolUse`): blocks Maven invocations that try to skip `checkstyle` or `spotbugs` (e.g. `-Dcheckstyle.skip=true`).
- **test-guard** (`Stop`): runs `checkstyle:check spotbugs:check test` in `midas-api/` before the session ends if any API Java files were modified.
- **format-on-save** (`PostToolUse`): auto-formats Java files after edits.

### Architecture (API)

Layered Quarkus 3 REST API, Hibernate ORM with Panache, PostgreSQL.

```
Resource (@Path, @Authenticated)  — HTTP layer, delegates to service
    ↓
Service (@ApplicationScoped, @Transactional)  — business logic, validation
    ↓
Entity (extends PanacheEntityBase)  — Active Record pattern, static query methods
    ↕
DTO  — request DTOs (validation annotations) + response DTOs (entity constructor)
```

All REST endpoints under `@ApplicationPath("/api/v1")`.

#### Security model

- Basic Auth → `UserIdentityProvider` → PBKDF2-SHA256 (100k iterations, 256-bit salt).
- `UserRankIdentityAugmentor` maps rank entity to fine-grained roles (e.g. `post:add`, `user:delete`).
- Resources: `@Authenticated` at class level; mutating endpoints add `@RolesAllowed({"resource:action"})`.

#### Database

- Quarkus Dev Services auto-starts PostgreSQL container in dev/test (port 55432).
- Production schema: `validate`; dev/test: `drop-and-create` with seed `sql/init.sql`.

### Key Conventions (API)

#### DTOs

- **Request DTOs** (`PostDto`, `UserDto`, …): public fields with Bean Validation (`@Size`, `@Email`, `@NotBlank`). Used for PUT and PATCH — services null-check fields on PATCH.
- **Response DTOs** (`PostResponseDto`, `UserResponseDto`, …): public fields including denormalized data (`username`, `categoryName`, `commentsCount`). Constructor takes the entity. Never expose sensitive fields (password, salt, activation key).

#### Entities

- Extend `PanacheEntityBase` with manual `@Id @GeneratedValue(IDENTITY) public Long id`.
- All fields `public` (no getters/setters) — deliberate. SpotBugs exclusions configured for DTO public fields.
- Use `@Column(name = "snake_case")` for multi-word columns; `@Column(columnDefinition = "TINYINT(1)")` for booleans.
- Relationships: `@ManyToOne(fetch = LAZY)` with `@JoinColumn`; child collections `@OneToMany(mappedBy, cascade = ALL)` with `@JsonIgnore`.

#### REST Responses

Use `ResponseUtil` helpers — never build `Response` objects manually:

- `ResponseUtil.ok(entity)` → 200
- `ResponseUtil.created(entity)` → 201
- `ResponseUtil.noContent()` → 204
- `ResponseUtil.okOrNotFound(optional)` → 200 or 404
- `ResponseUtil.unprocessable(error)` → 422

#### Validation & Error Handling

- `@Valid` on resource method parameters for Bean Validation.
- Throw `ValidationException` from services for business rule violations → `ValidationExceptionMapper` returns HTTP 422 `{"error": "...", "message": "..."}`.
- `ConstraintViolationExceptionMapper` aggregates field violations into a single 422.

#### Testing (API)

- `@QuarkusTest` on all classes. Docker required (Testcontainers).
- **Resource tests**: REST Assured via `TestUtil` helpers (`testUnauthorizedAccess()`, `testAuthorizedPostWithCreation()`, etc.) with admin/guest Basic Auth fixtures.
- **Service tests**: `@Inject` the service; `@Transactional` on DB-mutating tests; `ServiceTestDataBuilder` for unique test data.
- Unique names via `TestUtil.generateUniqueSuffix()` to avoid collisions.

#### Code Style (API)

- Google Java Style via Checkstyle (`checkstyle.xml`). Violations are errors — build fails. Fix before committing. 4-space indent.
- SpotBugs runs during build (`spotbugs-exclude.xml` suppresses serialization and DTO field warnings).
- Public methods (≥2 lines) require Javadoc. `@Override` and `@Test` exempt.
- OWASP disabled by default (`dependency-check.skip=true`). Enable: `./mvnw verify -Ddependency-check.skip=false` + `NVD_API_KEY`.
- License enforcement: commercial/proprietary/SSPL/Elastic/BUSL-1.1/PolyForm blocked via `license-maven-plugin`.

---

## midas-gui

### Build & Test

```shell
# Install (compile without tests)
./mvnw clean install -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=PostServiceTest

# Run a single test method
./mvnw test -Dtest=PostServiceTest#getAllPosts_shouldReturnPosts_whenAuthenticated

# Dev mode (hot reload on http://localhost:9069)
./mvnw quarkus:dev

# Checkstyle + SpotBugs lint only (fast, ~10s)
./mvnw checkstyle:check spotbugs:check

# Full verify without tests or OWASP (compile + lint + license)
./mvnw verify -DskipTests -Ddependency-check.skip=true

# Checkstyle + SpotBugs + license report (skip tests)
./mvnw verify -DskipTests

# Full verify with OWASP CVE check (slow, requires NVD_API_KEY)
NVD_API_KEY=<key> ./mvnw verify -Ddependency-check.skip=false
```

Makefile shortcuts exist (`make install`, `make test`, `make dev`, etc.).

### Copilot CLI Hooks (GUI)

Hooks live in `.github/hooks/` and are active automatically — no installation needed.

- **lint-guard** (`PreToolUse`): blocks Maven invocations that try to skip `checkstyle` or `spotbugs`.
- **test-guard** (`Stop`): runs `checkstyle:check spotbugs:check test` in `midas-gui/` before the session ends if any GUI Java files were modified.
- **format-on-save** (`PostToolUse`): auto-formats Java files after edits.

### Architecture (GUI)

Quarkus 3 + Vaadin Flow 25. No database — pure frontend over REST with Basic Auth.

```
view/           → Vaadin Flow views (UI pages)
component/      → Reusable Vaadin UI components (buttons, dialogs, layouts)
client/         → MicroProfile REST Client interfaces (one per entity)
service/        → CDI service beans: auth, error handling, resilience
dto/            → Jackson-annotated POJOs (request DTOs + *ResponseDto variants)
exception       → AuthenticationException, ServiceException
util/           → NotificationUtil (toasts), ErrorMessageUtil (HTTP error parsing)
```

Request flow: `View → Service → REST Client → Backend API`

Views never call REST clients directly. Services handle auth headers, error translation, fault tolerance.

### Key Conventions (GUI)

### Services (`service/`)

- All `@ApplicationScoped` with `@Timeout(5s)` and `@Retry(maxRetries=2, abortOn=AuthenticationException)`.
- Every method checks `authService.getAuthorizationHeader()` first; throws `AuthenticationException` if null.
- Catch `WebApplicationException` status 401 → `authService.logout()` → throw `AuthenticationException`.
- Catch other `WebApplicationException` → throw `ServiceException` with `ErrorMessageUtil.extractErrorMessage(response)`.
- Single-entity methods return `Optional<T>` (empty on 404). Collection methods throw on failure.

#### REST Clients (`client/`)

- MicroProfile REST Client interfaces with `@RegisterRestClient(configKey = "midas-api")`.
- Every method takes `@HeaderParam("Authorization") String authorization`.
- Base URL in `application.properties`: `quarkus.rest-client.midas-api.url`.

#### DTOs (`dto/`)

- Plain Java classes (not records) with `@JsonProperty`, getters/setters, and `toString()`.
- Paired naming: `PostDto` (request/write) and `PostResponseDto` (read, with denormalized fields like `username`).

#### Views (`view/`)

- `@Route(value = "...", layout = MainLayout.class)`; admin views use `AdminMainLayout`.
- Implement `BeforeEnterObserver`; redirect to `LoginView` if unauthenticated.
- UI built lazily in `beforeEnter()` via `buildUI()`, not in constructor.
- Data loading: `CompletableFuture.supplyAsync()` with `.orTimeout(30, SECONDS)`, results via `ui.access(...)` for thread safety.

#### Components (`component/`)

- Small, focused wrappers extending Vaadin base components (e.g. `CreateButton`, `ConfirmationDialog`, `SearchLayout`).
- Expose inner components via getters for callers to attach listeners.

#### Testing (GUI)

- JUnit 5 + Mockito + AssertJ; no Quarkus test container needed (unit-tested in isolation).
- Method naming: `methodName_shouldExpectedResult_whenCondition`.
- Given / When / Then structure with AssertJ fluent assertions.
- Shared utilities: `BaseServiceTest` (abstract base), `MockDataUtil` (factory DTOs), `TestDataBuilder` (unique data), `ResponseTestUtil` (mock HTTP responses), `ServiceTestUtil` (auth setup).

#### Code Organization (GUI)

For each backend Resource, create corresponding frontend:

- View (UI component)
- Client (API communication)
- Service (business logic)
- DTO classes (data transfer)

#### Code Style (GUI)

- Google Java Style via Checkstyle. Violations are errors — build fails. Fix before committing. SpotBugs runs during build.
- Javadoc required on all public methods (>2 lines) and protected+ types. `@Override` and `@Test` exempt.
- JBoss logging; DEBUG for service entry/exit, ERROR for failures.
- Notifications via `NotificationUtil.showSuccess/Error/Warning/Info()` — never construct `Notification` directly in views.
- OWASP disabled by default. Enable: `./mvnw verify -Ddependency-check.skip=false` + `NVD_API_KEY`.
- License enforcement: commercial/proprietary/SSPL/Elastic/BUSL-1.1/PolyForm blocked via `license-maven-plugin`.
