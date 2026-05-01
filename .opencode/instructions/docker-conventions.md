# Docker Conventions — Midas

Covers image conventions, JVM tuning, and service patterns for both API and GUI modules.

## Dockerfiles

Both modules have Dockerfiles in `src/main/docker/`:

### API (midas-api)

- **Dockerfile.jvm-ubuntu** (primary): Eclipse Temurin JRE on Ubuntu. Used by `docker-compose.yml` and `make build`.
- **Dockerfile.jvm-alpine**: Eclipse Temurin JRE on Alpine. Smaller image. Use only if size is critical.
- **Dockerfile.legacy-jar**: For über-jar packaging.
- **Dockerfile.native** / **Dockerfile.native-micro**: For GraalVM native executables.

#### API JVM Ubuntu Conventions

- **Base image**: `eclipse-temurin:<version>-jre-noble`
- **Application user**: UID/GID 185 (`jboss` user) — non-root execution
- **Layer caching**: Copy `target/quarkus-app/` in four layers (lib → jar → app → quarkus) for optimal caching
- **Logs directory**: `mkdir -p /deployments/logs && chown -R 185:185 /deployments/logs`
- **Health check**: `wget --spider http://localhost:8080/q/health/ready`
- **Exposed port**: 8080
- **JVM options**: Set via `JAVA_OPTS` and `JAVA_OPTS_APPEND` environment variables
- **Entrypoint**: `java -jar /deployments/quarkus-run.jar`

### GUI (midas-gui)

- **Dockerfile.jvm-ubuntu** (primary): Same structure as API.
- **Dockerfile.jvm-alpine**: Same structure as API.
- **Dockerfile.legacy-jar**: For über-jar packaging.
- **Dockerfile.native** / **Dockerfile.native-micro**: For GraalVM native executables.

#### GUI JVM Ubuntu Conventions

- **Base image**: `eclipse-temurin:<version>-jre-noble`
- **Application user**: UID/GID 185 (`jboss` user) — non-root execution
- **Layer caching**: Copy `target/quarkus-app/` in four layers (lib → jar → app → quarkus) for optimal caching
- **Logs directory**: `mkdir -p /deployments/logs && chown -R 185:185 /deployments/logs`
- **Health check**: `wget --spider http://localhost:8081/q/health/ready`
- **Exposed port**: 8081
- **JVM options**: Set via `JAVA_OPTS` and `JAVA_OPTS_APPEND` environment variables
- **Entrypoint**: `java -jar /deployments/quarkus-run.jar`

### Build Prerequisite

Always package the application for production before building a Docker image:

```shell
# API
cd midas-api && ./mvnw package

# GUI (with production profile)
cd midas-gui && ./mvnw package -Pproduction

# Native mode (requires GraalVM)
./mvnw package -Dnative          # API
./mvnw package -Dnative -Pproduction  # GUI
```

## docker-compose.yml (project root)

The root `docker-compose.yml` defines the full stack.

### Service: backend (midas-api)

- **Image**: `gregordietrich/midas-api:${REVISION:-1.0.0-SNAPSHOT}`
- **Build context**: project root, Dockerfile at `midas-api/src/main/docker/Dockerfile.jvm-ubuntu`
- **Port mapping**: host `8080` → container `8080`
- **Restart policy**: `unless-stopped`
- **Depends-on**: `postgres` with `condition: service_healthy`

### Service: frontend (midas-gui)

- **Image**: `gregordietrich/midas-gui:${REVISION:-1.0.0-SNAPSHOT}`
- **Build context**: project root, Dockerfile at `midas-gui/src/main/docker/Dockerfile.jvm-ubuntu`
- **Port mapping**: host `80` → container `8081`
- **Restart policy**: `unless-stopped`
- **Depends-on**: `backend` with `condition: service_healthy`

### Service: postgres

- **Image**: `postgres:17-alpine`
- **Port mapping**: host `${SQL_PORT:-5432}` → container `5432`
- **Environment**: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`

### Service: pgadmin

- **Image**: `dpage/pgadmin4`
- **Port mapping**: host `42069` → container `80`
- **Environment**: `PGADMIN_DEFAULT_EMAIL`, `PGADMIN_DEFAULT_PASSWORD`

## REST Client Configuration (in compose)

The frontend connects to the backend via environment variables:

- `quarkus.rest-client.midas-api.url`: Backend API URL (default: `http://backend:8080/api/v1`)
- `quarkus.rest-client.midas-api.connect-timeout`: Connection timeout in ms (default: `3000`)
- `quarkus.rest-client.midas-api.read-timeout`: Read timeout in ms (default: `5000`)
- `quarkus.rest-client.midas-api-health.url`: Backend health endpoint (default: `http://backend:8080`)

## Environment Variables

- **Variable substitution**: Use `${VAR_NAME:-default}` syntax
- **Timezone**: `TIMEZONE` (default: `UTC`)
- **Image tag**: `REVISION` (default: `1.0.0-SNAPSHOT`)
- **Database**: `SQL_USERNAME`, `SQL_PASSWORD`, `SQL_DATABASE`, `SQL_PORT`
- **pgAdmin**: `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`
- **Quarkus config overrides**: Pass Quarkus configuration as environment variables
- **Never hardcode real secrets** as default values in `docker-compose.yml`. Use placeholder strings (e.g., `changeit`) that are obviously meant to be replaced.

## Logging

Logging is configured via Quarkus environment variables:

- **Console logging**: JSON-enabled in production, plain text in dev/test
- **File logging**: enabled in production, rotated (10MB, 3 backups)
- **Log path**: `logs/midas-api.log` or `logs/midas-gui.log` (relative to `/deployments/` inside container)

## Session Security (GUI)

- `quarkus.http.same-site-cookie.session.value=Strict` is enforced in `application.properties`
