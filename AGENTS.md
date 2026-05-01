# Midas Project

Multi-module Quarkus 3 app. Java 25. PostgreSQL.

- **midas-api**: Backend REST API (Hibernate Panache, Basic Auth, PBKDF2-SHA256)
- **midas-gui**: Frontend (Vaadin Flow 25, MicroProfile REST Client). No DB — talks to API
- **midas-test**: Integration harness (Python 3.13+, Streamlit, SQLAlchemy). Separate from Java build

## Communication Style

Caveman mode: terse, exact, no fluff. Drop articles, filler, pleasantries. Fragments OK. Pattern: [thing] [action] [reason].

## Exact Commands

Both modules have `./mvnw`. Prefer wrapper.

```shell
# Lint (never skip — severity=error, blocks build)
./mvnw checkstyle:check spotbugs:check

# Tests
./mvnw test                                          # all
./mvnw test -Dtest=PostResourceTest                  # single class
./mvnw test -Dtest=PostResourceTest#testCreatePost   # single method

# Dev mode (API port 8080, GUI port 8081)
./mvnw quarkus:dev

# Full verification
./mvnw verify -DskipTests                            # compile + lint + license
./mvnw verify -Ddependency-check.skip=false          # + OWASP (needs NVD_API_KEY)

# Root Makefile shortcuts
make dev-api          # API dev mode
make dev-gui          # GUI dev mode
make test             # both modules
make install          # check + clean install -DskipTests
make build            # check + package + docker buildx
make password         # generate salt+hash for init.sql
```

API tests require Docker (Testcontainers). GUI tests do not.

GUI production package: `./mvnw package -Pproduction`

## Architecture

### API

```txt
Resource (@Path, @Authenticated) → Service (@ApplicationScoped, @Transactional) → Entity (PanacheEntityBase)
                                                                    ↕
                                                              DTO (request + response)
```

- All REST endpoints under `/api/v1` (via `@ApplicationPath`)
- Basic Auth → `UserIdentityProvider` → PBKDF2-SHA256 (100k iterations, 256-bit salt)
- `UserRankIdentityAugmentor` maps rank to fine-grained roles (`post:add`, `user:delete`, …)
- Resources: `@Authenticated` class-level; mutating endpoints add `@RolesAllowed({"resource:action"})`
- Dev Services auto-starts PostgreSQL on port 55432
- Production schema: `validate`; dev/test: `drop-and-create`

### GUI

```txt
View (@Route, BeforeEnterObserver) → Service → REST Client (@RegisterRestClient configKey="midas-api") → Backend API
```

- UI built lazily in `beforeEnter()`, not constructors
- Async loading: `CompletableFuture.supplyAsync()` + `.orTimeout(30, SECONDS)`, results via `ui.access(...)`
- `AuthService` stores backend Basic Auth header server-side behind opaque session ID
- Every service method checks `authService.getAuthorizationHeader()` first
- `WebApplicationException` 401 → `authService.logout()` → `AuthenticationException`

## Key Conventions

### API

- **Entities**: public fields (no getters/setters), `@ManyToOne(fetch = LAZY)`, manual `@Id @GeneratedValue(IDENTITY) public Long id`
- **Request DTOs**: public fields with Bean Validation (`@Size`, `@Email`, `@NotBlank`)
- **Response DTOs**: public fields with denormalized data (`username`, `categoryName`). Constructor takes entity. Never expose sensitive fields.
- **REST responses**: use `ResponseUtil` helpers only — never build `Response` manually
- **Validation**: `@Valid` on resource params; `ValidationException` from services → 422
- **Testing**: `@QuarkusTest`, REST Assured via `TestUtil`, unique test data via `TestUtil.generateUniqueSuffix()`
- **Style**: Google Java Style (Checkstyle severity=error), 4-space indent, SpotBugs clean
- **Javadoc**: required on public methods (≥2 lines). `@Override` and `@Test` exempt.
- **Database**: No Flyway. Project unreleased. Schema changes go in `src/main/resources/sql/init.sql`. Never create `db/migration/`.

### GUI

- **Services**: `@ApplicationScoped`, `@Timeout(5s)`, `@Retry(maxRetries=2, abortOn=AuthenticationException.class)`
- **REST Clients**: every method takes `@HeaderParam("Authorization") String authorization`
- **DTOs**: plain Java classes (not records), `@JsonProperty`, getters/setters, `toString()`
- **Views**: `@Route(value = "...", layout = MainLayout.class)` (admin views use `AdminMainLayout.class`)
- **Notifications**: use `NotificationUtil` — never construct `Notification` directly
- **Testing**: JUnit 5 + Mockito + AssertJ. Method naming: `methodName_shouldExpectedResult_whenCondition`

## Cross-Module Workflow

- Cross-reference corresponding files between frontend and backend
- Reference existing code in same package for consistency
- When adding a feature, API endpoint + GUI REST client + GUI service + GUI view all change together

## Supplementary Instructions

- Code quality: @instructions/code-quality.md
- Docker: @instructions/docker-conventions.md
- Testing: @instructions/test-conventions.md

## Agents

Use `@` to invoke specialized agents:

- `@backend-developer-api` — API resources, services, entities, DTOs, DB queries, security
- `@backend-developer-gui` — GUI REST clients, service beans, DTOs, auth, error handling
- `@frontend-developer-api` — API response DTOs, endpoint design for GUI consumption
- `@frontend-developer-gui` — Vaadin views, components, dialogs, layouts, navigation
- `@code-reviewer-api` — API code review: style, security, performance, architecture
- `@code-reviewer-gui` — GUI code review: style, security, thread safety, async patterns
- `@software-architect-api` — API architecture: module boundaries, resource/entity design
- `@software-architect-gui` — GUI architecture: view/component design, async strategies
