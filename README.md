# Midas

A modern web application built with Quarkus, consisting of a REST API backend and a Vaadin frontend.

## 🏗️ Project Structure

This is a single Git repository with two Maven modules:

- **midas-api**: Backend REST API service (Quarkus)
- **midas-gui**: Frontend web application (Quarkus + Vaadin)

```txt
midas/
├── midas-api/            # Backend REST API (Maven module)
├── midas-gui/            # Frontend web application (Maven module)
├── scripts/              # Project-only scripts (build, dev, openapi, password, release)
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
make dev-api

# Terminal 2 — Start the GUI (port 8081)
make dev-gui
```

The applications will be available at:

- **API**: <http://localhost:8080> (Dev UI at <http://localhost:8080/q/dev/>)
- **GUI**: <http://localhost:8081> (Dev UI at <http://localhost:8081/q/dev/>)

### 2. Production Mode with Docker Compose

Start the full stack (backend + frontend + PostgreSQL + pgAdmin):

```shell
docker compose up -d --build
```

Set `SQL_PASSWORD` and `APP_BOOTSTRAP_ADMIN_PASSWORD` (e.g. in `.env`, see [Environment Variables](#environment-variables)) before the first `docker compose up`. `SQL_PASSWORD` set later does not reach an existing database; see [Changing the Database Password](#changing-the-database-password).

The frontend will be available at <http://localhost:8081>.
pgAdmin will be available at <http://localhost:42069>.

## 🔧 Development

### Makefile Targets

The root `Makefile` runs every target on both modules; `make <target> ONLY=midas-api` (or
`midas-gui`) limits it to one. The shared targets come from devkit, pinned in `devkit.toml`;
`make help` prints this list:

```shell
make audit           # Run OWASP dependency-check (NVD_API_KEY or .env.build)
make branch          # create or reset a git branch from a source (prompts for names and pushes)
make build           # mvn package, docker buildx (ONLY=<module> limits it to one)
make check           # verify the local environment
make check-devkit    # verify .devkit links to the pin (warns on a stale devkitw)
make check-java      # Verify the JDK, Maven >= 3.9.9 and the pom's version pins
make clean           # mvn clean, plus logs and frontend build output
make coverage        # Run all tests incl. ITs with JaCoCo; write .coverage.md
make dev-api         # start Midas API in Quarkus dev mode (port 8080)
make dev-gui         # start Midas GUI in Quarkus dev mode (port 8081)
make format          # Format Java sources with spotless
make help            # list the available targets
make install         # make check, then mvn clean install -DskipTests
make kill            # Stop this project's Quarkus/Maven JVMs and its docker compose services
make lint            # Run the quality-gate plugins and the frontend pin check
make openapi         # regenerate the committed OpenAPI contract snapshot
make password        # bcrypt-hash a password, read from a prompt or stdin (demo data, break-glass reset)
make rebase          # interactive git rebase against a target (defaults to origin/main)
make regen-lockfile  # update the frontend lockfile to package.json once npm ci rejects it
make release         # pull from origin/main, make build, make tag, and push Docker images
make tag             # create, sign and push a new git tag (auto-increments latest tag suggestion)
make test            # Run the Maven test suite (mvn verify)
make untag           # delete a local and remote git tag (prompts for tag to delete)
```

### Building the Projects

```shell
# Install both (compile without tests)
make install

# Or individually:
make install ONLY=midas-api
make install ONLY=midas-gui
```

### Running Tests

```shell
# Test both projects
make test

# Or individually:
make test ONLY=midas-api
make test ONLY=midas-gui
```

Docker must be running for API tests (Testcontainers starts a PostgreSQL container automatically).

### Database Setup

The API uses PostgreSQL. In dev/test mode, Quarkus Dev Services automatically starts a PostgreSQL container (port 55432) — no manual setup needed.

Flyway creates and upgrades the schema on startup in every profile (`midas-api/src/main/resources/db/migration`); Hibernate only validates it. Dev and test also load demo accounts from `db/demo/R__demo_data.sql`, whose password is the username (e.g. `admin`/`admin`).

Flyway refuses a database built from an earlier V1 (checksum mismatch) or from the pre-Flyway `init.sql` (no schema history): recreate it with `docker compose down -v`, and never enable `baselineOnMigrate`, which would keep its old seeded accounts.

For production, the `docker-compose.yml` at the project root includes a PostgreSQL service. Production seeds no account. On any start that finds the `users` table empty, the API creates an admin from `APP_BOOTSTRAP_ADMIN_USERNAME` (default `admin`, 3–50 characters) and `APP_BOOTSTRAP_ADMIN_PASSWORD`, which must meet the regular password rules (8–72 characters and at most 72 bytes, with a digit, a lowercase and an uppercase letter, and one of `@#$%^&+=!`). Without a password it refuses to start. Once any account exists, both settings are ignored and can be removed. To regain access later, see [Break-glass: Reset a Lost Admin Password](#break-glass-reset-a-lost-admin-password).

A production build also refuses to start under the `dev` or `test` profile (e.g. a stray `QUARKUS_PROFILE=dev`), before Flyway or Hibernate touch the database: those profiles clean the schema at start and relax logging and rate limits. Unset `QUARKUS_PROFILE` (and any `quarkus.config.profile.parent`) or set it to `prod`. This guards against a wrong profile, not against the individual settings: anyone who controls the environment can still set those directly.

## 📦 Building for Production

### Package Applications

```shell
# Package API
./mvnw package -pl midas-api

# Package GUI (with production profile)
./mvnw package -pl midas-gui -Pproduction
```

Or use `make build` (runs check + package + Docker buildx; `ONLY=<module>` limits it to one).

### Create Docker Images

```shell
# Build API image
docker build midas-api -f midas-api/src/main/docker/Dockerfile.jvm-alpine -t midas-api:1.0.0-SNAPSHOT

# Build GUI image (requires -Pproduction package first)
docker build midas-gui -f midas-gui/src/main/docker/Dockerfile.jvm-alpine -t midas-gui:1.0.0-SNAPSHOT
```

### Native Executables

```shell
# API
./mvnw package -pl midas-api -Dnative

# GUI
./mvnw package -pl midas-gui -Dnative -Pproduction
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

`SQL_PASSWORD` and `APP_BOOTSTRAP_ADMIN_PASSWORD` must be set (e.g. in `.env`) before the first `docker compose up`; see the table below.

### Environment Variables

Customize the deployment using environment variables:

```shell
REVISION=1.0.0 docker compose up -d --build
```

Key variables (see `docker-compose.yml` for all):

| Variable                       | Default             | Description                                          |
| ------------------------------ | ------------------- | ---------------------------------------------------- |
| `REVISION`                     | `1.0.0-SNAPSHOT`    | Image tag                                            |
| `TIMEZONE`                     | `UTC`               | Container timezone                                   |
| `SQL_USERNAME`                 | `midas`             | PostgreSQL username                                  |
| `SQL_PASSWORD`                 | `changeit`          | PostgreSQL password                                  |
| `SQL_DATABASE`                 | `midas`             | PostgreSQL database                                  |
| `SQL_PORT`                     | `5432`              | PostgreSQL port                                      |
| `APP_BOOTSTRAP_ADMIN_USERNAME` | `admin`             | Initial admin username (used while `users` is empty) |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | none                | Initial admin password (needed if `users` is empty)  |
| `PGADMIN_EMAIL`                | `admin@example.com` | pgAdmin login email                                  |
| `PGADMIN_PASSWORD`             | `changeit`          | pgAdmin login password                               |

In `.env`, single-quote values containing `$`, since Compose interpolates unquoted values.

### Changing the Database Password

The `db` service passes `SQL_PASSWORD` to PostgreSQL as `POSTGRES_PASSWORD`, which takes effect only when the `postgres_data` volume is first initialised. Changing it afterwards, for example after the API refused to start with the default `changeit`, changes only the password the API sends, and it then fails to authenticate against the database.

On a fresh install with nothing to keep, recreate the volumes, which also removes the encryption key in `midas_keys`:

```shell
docker compose down -v
docker compose up -d --build
```

Otherwise, set the new password in the database (with your `SQL_USERNAME` and `SQL_DATABASE` if you changed them):

```shell
docker compose exec db psql -U midas -d midas
```

```sql
\password midas
```

It prompts for the new password twice and keeps it out of the `psql` history.

### Break-glass: Reset a Lost Admin Password

The API refuses to delete, ban, deactivate or demote the last active administrator, but a forgotten password still locks everyone out. Hash a new password with `make password` (it prompts and prints `Password: <hash>`). The reset bypasses the regular password rules (see [Database Setup](#database-setup)), so pick one that meets them. `make password` runs in a checkout with the JDK and Maven from Requirements; the hash it prints can be used on any host. Then open `psql` on the database (with your `SQL_USERNAME` and `SQL_DATABASE` if you changed them):

```shell
docker compose exec db psql -U midas -d midas
```

List the active administrators to find the `<admin>` to reset:

```sql
SELECT u.username FROM users u JOIN user_ranks r ON r.id = u.rank_id
    WHERE u.activated AND NOT u.banned AND r.admin_view AND r.user_edit AND r.user_rank_edit;
```

Let `\prompt` read the hash and paste it there: that keeps it out of the `psql` history and has `psql` quote it (it contains `$` signs, which a shell would expand in `psql -c "…"`):

```sql
\prompt 'Hash: ' hash
UPDATE users SET password = :'hash' WHERE username = '<admin>';
```

If the list is empty, no active administrator remains: restore the flags of the Administrator rank seeded by the schema, then give a user that rank back:

```sql
UPDATE user_ranks SET admin_view = TRUE, user_edit = TRUE, user_rank_edit = TRUE WHERE public_id = '01AN4Z07BY79KA1307SR9X4MV3';
UPDATE users SET rank_id = (SELECT id FROM user_ranks WHERE public_id = '01AN4Z07BY79KA1307SR9X4MV3'),
    activated = TRUE, banned = FALSE WHERE username = '<admin>';
```

If the first statement reports `UPDATE 0`, that rank was deleted: recreate it with `INSERT INTO user_ranks (public_id, name, admin_view, user_edit, user_rank_edit) VALUES ('01AN4Z07BY79KA1307SR9X4MV3', 'Administrator', TRUE, TRUE, TRUE);`, then run the second one.

## 📖 Architecture

### Backend (midas-api)

- **Framework**: Quarkus 3
- **Language**: Java 25
- **Database**: PostgreSQL
- **ORM**: Hibernate ORM with Panache (Active Record pattern)
- **Validation**: Hibernate Validator (Bean Validation)
- **API**: Jakarta REST (`@ApplicationPath("/api/v1")`) with Jackson
- **Security**: Basic Auth with BCrypt, role-based access control
- **Code Quality**: Checkstyle (Google Java Style) + SpotBugs + OWASP dependency-check

### Frontend (midas-gui)

- **Framework**: Quarkus 3 + Vaadin Flow 25
- **Language**: Java 25
- **UI Components**: Vaadin
- **API Communication**: MicroProfile REST Clients and response DTOs generated from `midas-api/openapi.yaml` at build time, with backend Basic Auth stored server-side behind opaque session IDs
- **Code Quality**: Checkstyle (Google Java Style) + SpotBugs + OWASP dependency-check

### Key Features

- Modern Java 25 development with Quarkus
- Cloud-native and container-ready
- Hot reload in development mode
- Automatic database setup with Dev Services
- Production-ready Docker images (Eclipse Temurin JRE on Alpine)
- Git hooks (lint before commit, test before push) are planned in [gregor/devkit#3](https://git.vptr.de/gregor/devkit/issues/3)
- License enforcement (blocks commercial/proprietary licenses)

## 🔐 Contracts & Specifications

### API — Authentication & Access

- Protected endpoints require Basic authentication.
- Ownership-sensitive operations (e.g. content mutations) enforce ownership or admin rights and return `403` on violations.
- Invalid credentials return `401`; excessive invalid attempts return `429` with a structured error payload.
- A user delete, update or patch, or a rank update or patch, that would leave no active administrator (an activated, unbanned user whose rank has `adminView`, `userEdit` and `userRankEdit`) returns `422`.

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
- The generated REST clients get the header from `MidasClientHeadersFactory`, which reads it through `AuthService` and throws `AuthenticationException` before a request is sent when the session has none. Only `AuthClient` passes credentials explicitly, at login.
- Re-authentication and logout revoke the previous session mapping.

### GUI — Backend Error Parsing

GUI services consume API error payloads via `ErrorMessageUtil`. It reads structured JSON fields (`message`, `detail`, `error`, `title`) and falls back to `HTTP <status>` when no safe message exists: any other body, such as a proxy's plain-text or HTML error page, never reaches the user.

### GUI — Pagination Parameter Normalization

Before GUI services call paginated API endpoints, they normalize:

- `page < 0` or missing → `0`
- `size <= 0` or missing → `20`
- `size > 100` → `100`
- `recent limit <= 0` → `10`
- `recent limit > 100` → `100`

### Encryption at Rest & Key Management

The project implements transparent encryption for sensitive data (PII) at rest and in transit.

#### API — Database Encryption (PII)

- **Master Key**: A 256-bit AES key used by `EncryptionService` to encrypt fields (e.g., `UserEntity.email`) before persisting to PostgreSQL.
- **Key Discovery Order**:
  1. System property or environment variable `midas.encryption.key.path`
  2. Default value `encryption.key` in the application's current working directory
- **Generation & Persistence**:
  - If the key file is absent, a new 256-bit key is automatically generated using `SecureRandom` on first start.
  - New keys are **Base64-encoded** for easier management and backup.
  - To prevent data corruption, the key is written using an **atomic move** operation.
  - On POSIX-compliant filesystems, the file is restricted to **0600 permissions** (read/write only by the owner).
- **Backward Compatibility**: Existing 32-byte raw binary keys are automatically detected and supported.
- **Docker Deployment**: In production (Docker Compose), the key is stored in the `midas_keys` volume and mapped to `/etc/midas/keys/encryption.key`.
- **Warning**: Losing the master key makes all encrypted database content permanently unrecoverable. Ensure the key file is backed up securely.

#### GUI — Session Encryption

- **Key**: A static 256-bit AES key generated in-memory on application startup.
- **Usage**: Used by `SessionAuthorizationStore` to encrypt the backend Basic Auth header before storing it in the Vaadin session.
- **Persistence**: This key is NOT persisted and is unique to each application lifecycle.

## 🚀 Getting Started

1. Clone the repository
2. Ensure you have JDK 25, Maven, and Docker installed
3. Run `make install` to compile both modules
4. Start the API: `make dev-api`
5. Start the GUI: `make dev-gui`
6. Access the frontend at <http://localhost:8081>
7. Access the API at <http://localhost:8080>

For production deployment, use `docker compose up -d --build`.

## 🔄 Development Workflow

1. Make your changes in the respective modules
   - Use `make dev-api` and `make dev-gui` for hot-reload development
2. `make test` — Run tests for both modules

### Repository Structure

This project is a single Git repository where:

- The root holds `docker-compose.yml`, the scripts, the root `Makefile` and the root `pom.xml`
- The root `pom.xml` aggregates `midas-api` and `midas-gui` as Maven modules and carries their shared quality-gate configuration

## 📖 Related Guides

### General

- Quarkus ([guide](https://quarkus.io/guides/)): The main framework for building Java applications with a focus on cloud-native and microservices architectures.
- ArC (CDI) ([guide](https://quarkus.io/guides/cdi-reference)): A dependency injection framework that is part of Quarkus, providing support for CDI (Contexts and Dependency Injection).
- Maven Tooling ([guide](https://quarkus.io/guides/maven-tooling)): Build Tooling
- Vaadin Flow ([guide](https://vaadin.com/docs/latest/flow/integrations/quarkus)): Vaadin Flow is a unique framework that lets you build web apps without writing HTML or JavaScript

### Database

- Datasource ([guide](https://quarkus.io/guides/datasource)): A Quarkus extension for connecting to databases using JDBC, JPA, Hibernate ORM, and more.
- Flyway ([guide](https://quarkus.io/guides/flyway)): A Quarkus extension that runs versioned database schema migrations, here at application startup.
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): A Quarkus extension that simplifies the use of Hibernate ORM with a focus on ease of use and productivity.
- Hibernate Validator ([guide](https://quarkus.io/guides/hibernate-validator)): A Quarkus extension that integrates Hibernate Validator for bean validation, allowing you to validate your data models easily.

### REST

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Client ([guide](https://quarkus.io/guides/rest-client)): A client for making RESTful calls to other services.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): A Quarkus extension that provides support for JSON serialization and deserialization using Jackson, making it easy to work with JSON data in your RESTful APIs.
