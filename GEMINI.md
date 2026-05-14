# Midas Project

This project is a modern web application consisting of a REST API backend and a Vaadin frontend, built using **Quarkus 3** and **Java 25**.

## Project Overview

- **Architecture**:
  - **`midas-api`**: Backend REST API service.
  - **`midas-gui`**: Frontend web application using Vaadin Flow.
- **Build System**: Maven (multi-module) with a root `Makefile` orchestrating the workflow. All shared configurations and quality gates are centralized in the root `pom.xml`.

## Building and Running

The project uses a `Makefile` at the root and in each module to simplify common tasks.

### Prerequisites

- **JDK 25**
- **Maven 3.9.9+**
- **Docker** (for tests and production deployment)

### Common Commands

- `make install`: Run `mvn clean install -DskipTests` for both modules.
- `make build`: Verify environment, package applications, and build Docker images.
- `make clean`: Clean Maven artifacts and build directories.
- `make format`: Run Spotless to format code for both modules.
- `make lint`: Run quality gate plugins for both modules.
- `make test`: Run full Maven test suite for both modules (requires Docker for API tests).
- `make dev-api`: Start the backend API in Quarkus Dev Mode (port 8080).
- `make dev-gui`: Start the frontend GUI in Quarkus Dev Mode (port 8081).
- `make kill`: Terminate all Quarkus/Maven processes and clean up Docker containers.
- `make password`: Generate a salt+hash for the API `init.sql`.

## Development Conventions

### Code Quality & Standards

The project enforces very strict quality gates:

- **Formatting**: Spotless with Eclipse Formatter (Google Java Style baseline).
- **Linting**: Checkstyle, PMD (with CPD for duplicate detection).
- **Static Analysis**: SpotBugs (Max effort, Low threshold), ErrorProne, and NullAway (strict nullability checks).
- **Testing**: JUnit 5, Mockito, AssertJ, and REST Assured.
- **Coverage**: JaCoCo enforcement.
- **Security**: OWASP Dependency Check (fails on CVSS >= 7), License enforcement (blocks commercial/proprietary licenses).

You MUST ALWAYS run ALL quality gates AND tests via `make test` before claiming you are done, and especially before executing a git commit.
If any quality gates or tests fail, work on them until all problems are resolved, ensured by a final pass of `make test`.

### Design Patterns & Logic

- **API Response**: Standardized JSON error payloads with `status`, `error`, and `message`.
- **Pagination**: Normalization of `page`, `size`, and `limit` parameters at both GUI and API levels.
- **Security**: Basic Auth with BCrypt. The GUI stores backend credentials behind an opaque session ID.
- **Architecture**: Prefer composition and clear abstraction. Use MicroProfile Fault Tolerance in the GUI for resilient API communication.

## Directory Structure

- `midas-api/`: Backend service source and configuration.
- `midas-gui/`: Frontend application source and configuration.
- `scripts/`: Shared shell scripts for build and lifecycle management.
- `docker-compose.yml`: Orchestration for local production-like environments.
- `Makefile`: Entry point for all development tasks.
- `README.md`: General project documentation.
