# Midas

A modern web application built with Quarkus, consisting of a REST API backend and a Vaadin frontend.

## 🏗️ Project Structure

This is a multi-module project containing:

- **midas-api**: Backend REST API service (Quarkus)
- **midas-gui**: Frontend web application (Quarkus + Vaadin)

```txt
midas/
├── midas-api/            # Backend REST API (separate Git repo)
├── midas-gui/            # Frontend web application (separate Git repo)
├── scripts/              # Repository management scripts (pull, push)
├── docker-compose.yml    # Full-stack Docker Compose
└── Makefile              # Orchestrates both subprojects
```

## ⚠️ Requirements

- JDK 25: [Download (Adoptium)](https://adoptium.net/temurin/releases/?os=any&arch=any&version=25)
- Maven 3.9.9 (or later): [Download (Apache)](https://maven.apache.org/download.cgi)
- Docker: [Download (Docker Engine)](https://docs.docker.com/engine/install/) or [Download (Docker Desktop)](https://docs.docker.com/desktop/)

## 🚀 Quick Start

### 1. Development Mode

Start both applications in development mode:

```shell
# Terminal 1 — Start the API (port 8080)
cd midas-api && make dev

# Terminal 2 — Start the GUI (port 8081)
cd midas-gui && make dev
```

The applications will be available at:

- **API**: <http://localhost:8080> (Dev UI at <http://localhost:8080/q/dev/>)
- **GUI**: <http://localhost:8081> (Dev UI at <http://localhost:8081/q/dev/>)

### 2. Production Mode with Docker Compose

Start the full stack (backend + frontend + PostgreSQL + pgAdmin):

```shell
docker compose up -d --build
```

The frontend will be available at <http://localhost> (port 80).
pgAdmin will be available at <http://localhost:42069>.

## 🔧 Development

### Makefile Targets

The root `Makefile` delegates to both subprojects:

```shell
make branch       # create or reset a git branch from a source (prompts for names and pushes)
make build        # make check, mvn package, docker buildx (both modules)
make check        # verify local environment (both modules)
make clean        # run mvn clean, and remove build artifacts (both modules)
make coverage     # run tests with JaCoCo and generate combined coverage report
make dev          # start Quarkus in dev mode (specify: make dev-api or make dev-gui)
make format       # run spotless to format code (both modules)
make install      # make check, mvn clean install -DskipTests (both modules)
make kill         # stop/kill Quarkus and Maven processes and remove Docker containers
make lint         # run quality gate plugins (both modules)
make password     # generate a salt+hash for a password (for API init.sql)
make rebase       # interactive git rebase against a target (defaults to origin/main)
make release      # pull from origin/main, make build, make tag, and push Docker images
make tag          # create, sign and push a new git tag (auto-increments latest tag suggestion)
make test         # run full Maven test suite (both modules)
make untag        # delete a local and remote git tag (prompts for tag to delete)
```

### Building the Projects

```shell
# Install both (compile without tests)
make install

# Or individually:
cd midas-api && make install
cd midas-gui && make install
```

### Running Tests

```shell
# Test both projects
make test

# Or individually:
cd midas-api && make test
cd midas-gui && make test
```

Docker must be running for API tests (Testcontainers starts a PostgreSQL container automatically).

### Database Setup

The API uses PostgreSQL. In dev/test mode, Quarkus Dev Services automatically starts a PostgreSQL container (port 55432) — no manual setup needed.

For production, the `docker-compose.yml` at the project root includes a PostgreSQL service.

## 📦 Building for Production

### Package Applications

```shell
# Package API
cd midas-api && ./mvnw package

# Package GUI (with production profile)
cd midas-gui && ./mvnw package -Pproduction
```

Or use `make build` in each subproject (runs check + package + Docker buildx).

### Create Docker Images

```shell
# Build API image
cd midas-api && docker build . -f src/main/docker/Dockerfile.jvm-alpine -t midas-api:1.0.0-SNAPSHOT

# Build GUI image (requires -Pproduction package first)
cd midas-gui && docker build . -f src/main/docker/Dockerfile.jvm-alpine -t midas-gui:1.0.0-SNAPSHOT
```

### Native Executables

```shell
# API
cd midas-api && ./mvnw package -Dnative

# GUI
cd midas-gui && ./mvnw package -Dnative -Pproduction
```

To build in a container (no GraalVM required):

```shell
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

## 🐳 Docker Deployment

### Full Stack Deployment

Deploy the complete application stack with PostgreSQL:

```shell
docker compose up -d --build
```

### Environment Variables

Customize the deployment using environment variables:

```shell
REVISION=1.0.0 docker compose up -d --build
```

Key variables (see `docker-compose.yml` for all):

| Variable           | Default             | Description            |
| ------------------ | ------------------- | ---------------------- |
| `REVISION`         | `1.0.0-SNAPSHOT`    | Image tag              |
| `TIMEZONE`         | `UTC`               | Container timezone     |
| `SQL_USERNAME`     | `midas`             | PostgreSQL username    |
| `SQL_PASSWORD`     | `changeit`          | PostgreSQL password    |
| `SQL_DATABASE`     | `midas`             | PostgreSQL database    |
| `SQL_PORT`         | `5432`              | PostgreSQL port        |
| `PGADMIN_EMAIL`    | `admin@example.com` | pgAdmin login email    |
| `PGADMIN_PASSWORD` | `changeit`          | pgAdmin login password |

## 📖 Architecture

### Backend (midas-api)

- **Framework**: Quarkus 3
- **Language**: Java 25
- **Database**: PostgreSQL
- **ORM**: Hibernate ORM with Panache (Active Record pattern)
- **Validation**: Hibernate Validator (Bean Validation)
- **API**: Jakarta REST (`@ApplicationPath("/api/v1")`) with Jackson
- **Security**: Basic Auth with PBKDF2-SHA256, role-based access control
- **Code Quality**: Checkstyle (Google Java Style) + SpotBugs + OWASP dependency-check

### Frontend (midas-gui)

- **Framework**: Quarkus 3 + Vaadin Flow 25
- **Language**: Java 25
- **UI Components**: Vaadin
- **API Communication**: MicroProfile REST Client with backend Basic Auth stored server-side behind opaque session IDs
- **Code Quality**: Checkstyle (Google Java Style) + SpotBugs + OWASP dependency-check

### Key Features

- Modern Java 25 development with Quarkus
- Cloud-native and container-ready
- Hot reload in development mode
- Automatic database setup with Dev Services
- Production-ready Docker images (Eclipse Temurin JRE on Alpine)
- Git hooks for lint-guard (pre-commit) and test-guard (pre-push)
- License enforcement (blocks commercial/proprietary licenses)

## 🔐 Contracts & Specifications

### API — Authentication & Access

- Protected endpoints require Basic authentication.
- Ownership-sensitive operations (e.g. content mutations) enforce ownership or admin rights and return `403` on violations.
- Invalid credentials return `401`; excessive invalid attempts return `429` with a structured error payload.

### API — Error Payload

All exception mappers return a structured JSON payload:

```json
{
  "status": 422,
  "error": "Validation failed",
  "message": "Email invalid"
}
```

Server-side internals are not exposed for `5xx` responses.

### API — Pagination Normalization

List and recent endpoints normalize pagination inputs before service calls:

- `page < 0` → `0`
- `size <= 0` → `20`
- `size > 100` → `100`
- `limit <= 0` → `10`
- `limit > 100` → `100`

### GUI — Session Authorization

- `AuthService` validates credentials against the API, then stores the backend `Authorization` header only in `SessionAuthorizationStore`.
- Vaadin session stores an opaque `authenticated.session-id`, not the raw Basic header.
- GUI REST services must read headers through `AuthService.getAuthorizationHeader()`.
- Re-authentication and logout revoke the previous session mapping.

### GUI — Backend Error Parsing

GUI services consume API error payloads via `ErrorMessageUtil`. It prefers structured JSON fields (`message`, `detail`, `error`, `title`) and falls back to `HTTP <status>` when no safe message exists.

### GUI — Pagination Parameter Normalization

Before GUI services call paginated API endpoints, they normalize:

- `page < 0` or missing → `0`
- `size <= 0` or missing → `20`
- `size > 100` → `100`
- `recent limit <= 0` → `10`
- `recent limit > 100` → `100`

## 🚀 Getting Started

1. Clone the repository with submodules
2. Ensure you have JDK 25, Maven, and Docker installed
3. Run `make pull` to sync submodules
4. Run `make install` to compile both projects
5. Start the API: `cd midas-api && make dev`
6. Start the GUI: `cd midas-gui && make dev`
7. Access the frontend at <http://localhost:8081>
8. Access the API at <http://localhost:8080>

For production deployment, use `docker compose up -d --build`.

## 🔄 Development Workflow

1. `make pull` — Sync all repositories with latest changes
2. Make your changes in the respective projects
   - Use `make dev` in each subproject for hot-reload development
3. `make test` — Run tests for both projects
4. `make push` — Commit and push changes to all repositories

### Repository Structure

This project uses a multi-repository approach where:

- Main repository contains `docker-compose.yml`, orchestration scripts, and the root `Makefile`
- `midas-api` and `midas-gui` are separate Git repositories (submodules)
- Use `make pull` and `make push` to manage all repositories together

## 📖 Related Guides

### General

- Quarkus ([guide](https://quarkus.io/guides/)): The main framework for building Java applications with a focus on cloud-native and microservices architectures.
- ArC (CDI) ([guide](https://quarkus.io/guides/cdi-reference)): A dependency injection framework that is part of Quarkus, providing support for CDI (Contexts and Dependency Injection).
- Maven Tooling ([guide](https://quarkus.io/guides/maven-tooling)): Build Tooling
- Vaadin Flow ([guide](https://vaadin.com/docs/latest/flow/integrations/quarkus)): Vaadin Flow is a unique framework that lets you build web apps without writing HTML or JavaScript

### Database

- Datasource ([guide](https://quarkus.io/guides/datasource)): A Quarkus extension for connecting to databases using JDBC, JPA, Hibernate ORM, and more.
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): A Quarkus extension that simplifies the use of Hibernate ORM with a focus on ease of use and productivity.
- Hibernate Validator ([guide](https://quarkus.io/guides/hibernate-validator)): A Quarkus extension that integrates Hibernate Validator for bean validation, allowing you to validate your data models easily.

### REST

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Client ([guide](https://quarkus.io/guides/rest-client)): A client for making RESTful calls to other services.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): A Quarkus extension that provides support for JSON serialization and deserialization using Jackson, making it easy to work with JSON data in your RESTful APIs.
