# Midas Project

Multi-module system. Java 25 / Quarkus 3 backend & frontend. Python 3.13 integration/monitoring.

- **midas-api**: Backend REST API (Hibernate Panache, Basic Auth, PBKDF2-SHA256).
- **midas-gui**: Frontend (Vaadin Flow 25, MicroProfile REST Client). No DB.
- **midas-test**: Integration suite (Python 3.13, Streamlit, SQLAlchemy, Pytest, Ruff).

## Communication Style

Caveman mode: terse, exact, no fluff. Drop articles, filler, pleasantries. Pattern: [thing] [action] [reason].

## Exact Commands

### Root Makefile (Orchestration)

```shell
make install      # check + clean install -DskipTests (Java modules)
make test         # execute all tests (Java + Python)
make dev-api      # start API in dev mode (port 8080)
make dev-gui      # start GUI in dev mode (port 8081)
make build        # check + package + docker build
make clean        # remove build artifacts
make kill         # stop all processes/containers
```

### Java Modules (API & GUI)

```shell
# Comprehensive Lint (Severity=error, blocks build)
# Includes Checkstyle, SpotBugs, PMD, CPD, Error Prone, NullAway
./mvnw verify -DskipTests

./mvnw test                              # Test all (API needs Docker)
./mvnw quarkus:dev                       # Dev mode
```

### Python Module (Test Suite)

```shell
cd midas-test
ruff check .                             # Lint
pytest                                   # Run integration tests
streamlit run src/app.py                 # Start monitoring UI
```

## Architecture & Conventions

Detailed instructions in `.opencode/instructions/`:

- [Code Quality & Security](./.opencode/instructions/code-quality.md)
- [Docker & Deployment](./.opencode/instructions/docker-conventions.md)
- [Testing Conventions](./.opencode/instructions/test-conventions.md)

### Backend (API)

- **Pattern**: Resource → Service → Entity (Active Record).
- **Security**: Basic Auth via `UserIdentityProvider`. PBKDF2-SHA256 (100k iter, 256-bit salt).
- **Authorization**: `@Authenticated` class-level. `@RolesAllowed` for mutations.
- **DTOs**: Request (Validation) / Response (Entity constructor). No sensitive fields.

### Frontend (GUI)

- **Pattern**: View → Service → REST Client → API.
- **UI**: Vaadin Flow. Lazy build in `beforeEnter()`. Async loading via `CompletableFuture`.
- **Auth**: `AuthService` maps session to backend Basic Auth. 401 triggers logout.

### Integration (Test)

- **Stack**: Python 3.13, SQLAlchemy 2.0, Streamlit 1.55, Pytest 9.0.
- **Linting**: Ruff (target py313).

## Specialized Agents

Invoke via `@name`:

### API Agents

- [@backend-developer-api](./.opencode/agents/backend-developer-api.md) — API implementation & DB
- [@frontend-developer-api](./.opencode/agents/frontend-developer-api.md) — API contract design for GUI
- [@code-reviewer-api](./.opencode/agents/code-reviewer-api.md) — API review (security, perf, style)
- [@software-architect-api](./.opencode/agents/software-architect-api.md) — API design & structure

### GUI Agents

- [@backend-developer-gui](./.opencode/agents/backend-developer-gui.md) — GUI services, clients, auth
- [@frontend-developer-gui](./.opencode/agents/frontend-developer-gui.md) — Vaadin views & components
- [@code-reviewer-gui](./.opencode/agents/code-reviewer-gui.md) — GUI review (thread safety, async)
- [@software-architect-gui](./.opencode/agents/software-architect-gui.md) — GUI design & async strategy

### Integration Agents

- [@integration-test-developer](./.opencode/agents/integration-test-developer.md) — Python test suite & monitoring

## Cross-Module Workflow

1. Update API Resource/DTO.
2. Update GUI REST Client/DTO.
3. Update GUI Service.
4. Update GUI View.
5. Verify with `midas-test` integration suite.
