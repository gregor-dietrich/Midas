# Midas Project

Multi-module Quarkus 3 app: REST API backend + Vaadin Flow 25 frontend. Java 25. PostgreSQL.

## Caveman Mode

Terse. Technical substance exact. Only fluff die. Drop: articles, filler, pleasantries, hedging. Fragments OK. Short synonyms. Code unchanged. Pattern: [thing] [action] [reason]. [next step]. ACTIVE EVERY RESPONSE.

## Project Structure

- **midas-api**: Backend REST API (Quarkus 3, Java 25, PostgreSQL, Hibernate Panache)
- **midas-gui**: Frontend web app (Quarkus 3 + Vaadin Flow 25); no database — REST only
- **midas-test**: Integration test harness (Python 3.13+, Streamlit, SQLAlchemy)
- Cross-reference corresponding files between frontend and backend
- Reference existing code in same package for consistency

## Architecture

### API (midas-api)

```txt
Resource (@Path, @Authenticated)  — HTTP layer, delegates to service
    ↓
Service (@ApplicationScoped, @Transactional)  — business logic, validation
    ↓
Entity (extends PanacheEntityBase)  — Active Record, static query methods
    ↕
DTO  — request DTOs (validation) + response DTOs (entity constructor)
```

- Basic Auth → `UserIdentityProvider` → PBKDF2-SHA256 (100k iterations, 256-bit salt)
- `UserRankIdentityAugmentor` maps rank to fine-grained roles (`post:add`, `user:delete`, …)
- Resources: `@Authenticated` class-level; mutating endpoints add `@RolesAllowed({"resource:action"})`
- Dev Services auto-starts PostgreSQL (port 55432). Production schema: `validate`; dev/test: `drop-and-create`
- All REST endpoints under `/api/v1`

### GUI (midas-gui)

```txt
view/           → Vaadin Flow views (@Route, BeforeEnterObserver)
component/      → Reusable Vaadin UI (button/, dialog/, layout/, tabs/)
client/         → MicroProfile REST Client interfaces (one per entity)
service/        → CDI service beans: auth, error handling, resilience
dto/            → Jackson POJOs (request + *ResponseDto)
exception/      → AuthenticationException, ServiceException
util/           → NotificationUtil, ErrorMessageUtil
```

Request flow: `View → Service → REST Client → Backend API`. Views never call REST clients directly.

## Development Workflow

1. Make changes following project conventions
2. Add comments where necessary (explain why, not what)
3. Run quality gates (see below)
4. Fix compilation errors or test failures
5. If issues persist, run `./mvnw quarkus:dev` and check logs

## Quality Gates

**Never skip checkstyle or SpotBugs.** Lint and test before pushing.

```txt
# API (midas-api/)
./mvnw checkstyle:check spotbugs:check
./mvnw test                          # requires Docker (Testcontainers)
./mvnw verify -DskipTests            # compile + lint + license
./mvnw verify -Ddependency-check.skip=false  # + OWASP (needs NVD_API_KEY)

# GUI (midas-gui/)
mvn checkstyle:check spotbugs:check
mvn test
mvn verify -DskipTests

# Dev mode
./mvnw quarkus:dev                   # API: port 8080
./mvnw quarkus:dev                   # GUI: port 8081
```

## Code Quality Standards

- Clarity over cleverness. Self-documenting code. Single Responsibility Principle.
- Eliminate duplication immediately. Extract common logic. No copy-paste.
- Security by default: validate all input, least-privilege, no hardcoded secrets.
- Performance: avoid N+1 queries, use pagination/caching, profile before optimizing.
- Methods >30 lines, classes >300 lines, magic numbers, unclear naming = refactor triggers.
- New business logic requires tests.

## Key Conventions

### API

- **Request DTOs**: public fields with Bean Validation (`@Size`, `@Email`, `@NotBlank`)
- **Response DTOs**: public fields with denormalized data (`username`, `categoryName`). Constructor takes entity. Never expose sensitive fields.
- **Entities**: `public` fields (no getters/setters), `@ManyToOne(fetch = LAZY)`, manual `@Id @GeneratedValue(IDENTITY) public Long id`
- **REST responses**: use `ResponseUtil` helpers only — never build `Response` manually
- **Validation**: `@Valid` on resource params; `ValidationException` from services → 422
- **Testing**: `@QuarkusTest`, REST Assured via `TestUtil`, unique test data via `TestUtil.generateUniqueSuffix()`
- **Style**: Google Java Style (Checkstyle, severity=error), 4-space indent, SpotBugs clean
- **Javadoc**: required on public methods (≥2 lines). `@Override` and `@Test` exempt.
- **Database schema**: Project is unreleased — no Flyway migrations. All schema changes go in `src/main/resources/sql/init.sql`. Never create `db/migration/` directory. When project reaches production release, migrations will be introduced at that point.

### GUI

- **Services**: `@ApplicationScoped`, `@Timeout(5s)`, `@Retry(maxRetries=2, abortOn=AuthenticationException)`. Every method checks `authService.getAuthorizationHeader()` first.
- **REST Clients**: `@RegisterRestClient(configKey = "midas-api")`, every method takes `@HeaderParam("Authorization") String authorization`
- **DTOs**: plain Java classes (not records), `@JsonProperty`, getters/setters, `toString()`
- **Views**: `@Route(value = "...", layout = MainLayout.class)`, implement `BeforeEnterObserver`, redirect to `LoginView` if unauthenticated. UI built lazily in `beforeEnter()`.
- **Async loading**: `CompletableFuture.supplyAsync()` + `.orTimeout(30, SECONDS)`, results via `ui.access(...)`
- **Testing**: JUnit 5 + Mockito + AssertJ. Method naming: `methodName_shouldExpectedResult_whenCondition`
- **Notifications**: use `NotificationUtil` — never construct `Notification` directly

## Supplementary Instructions

- Code quality, security, performance: @instructions/code-quality.md
- Docker conventions: @instructions/docker-conventions.md
- Test conventions: @instructions/test-conventions.md

## Available Agents

Use `@` to invoke specialized agents:

- `@backend-developer-api` — API resources, services, entities, DTOs, DB queries, security
- `@backend-developer-gui` — GUI REST clients, service beans, DTOs, auth, error handling
- `@frontend-developer-api` — API response DTOs, endpoint design for GUI consumption
- `@frontend-developer-gui` — Vaadin views, components, dialogs, layouts, navigation
- `@code-reviewer-api` — API code review: style, security, performance, architecture
- `@code-reviewer-gui` — GUI code review: style, security, thread safety, async patterns
- `@software-architect-api` — API architecture: module boundaries, dependency review
- `@software-architect-gui` — GUI architecture: view/component design, async strategies
