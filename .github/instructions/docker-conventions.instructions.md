---
description: "Use when modifying Docker configuration, Dockerfiles, or docker-compose.yml. Covers image conventions, JVM tuning, and service patterns for Midas GUI."
applyTo: "{**/Dockerfile*,**/docker-compose*.yml}"
---

# Docker Conventions — Midas GUI

## Dockerfiles

Multiple Dockerfile variants exist in `src/main/docker/`:

- **Dockerfile.jvm-ubuntu** (primary): Eclipse Temurin JRE on Ubuntu. Larger image but broader compatibility and reliability. Used by `docker-compose.yml` and `make build`.
- **Dockerfile.jvm-alpine**: Eclipse Temurin JRE on Alpine. Smaller image. Use only if size is critical and Alpine compatibility is confirmed.
- **Dockerfile.legacy-jar**: For über-jar packaging.
- **Dockerfile.native** / **Dockerfile.native-micro**: For GraalVM native executables.

### JVM Ubuntu Conventions (primary)

- **Base image**: `eclipse-temurin:<version>-jre-noble`
- **Application user**: UID/GID 185 (`jboss` user) — non-root execution
- **Layer caching**: Copy `target/quarkus-app/` in four layers (lib → jar → app → quarkus) for optimal caching
- **Logs directory**: `mkdir -p /deployments/logs && chown -R 185:185 /deployments/logs`
- **Health check**: `wget --spider http://localhost:8081/q/health/ready`
- **Exposed port**: 8081
- **JVM options**: Set via `JAVA_OPTS` and `JAVA_OPTS_APPEND` environment variables
- **Entrypoint**: `java -jar /deployments/quarkus-run.jar`

### Build prerequisite

Always package the application for production before building a Docker image:

```shell
./mvnw package -Pproduction                    # JVM mode
./mvnw package -Dnative -Pproduction           # Native mode (requires GraalVM)
```

## docker-compose.yml (project root)

The root `docker-compose.yml` defines the full stack. The GUI is the frontend service.

### Service: frontend (midas-gui)

- **Image**: `gregordietrich/midas-gui:${REVISION:-1.0.0-SNAPSHOT}`
- **Build context**: project root, Dockerfile at `midas-gui/src/main/docker/Dockerfile.jvm-ubuntu`
- **Port mapping**: host `80` → container `8081`
- **Restart policy**: `unless-stopped`
- **Depends-on**: `backend` with `condition: service_healthy`

### REST Client Configuration (in compose)

The frontend connects to the backend via environment variables:

- `quarkus.rest-client.midas-api.url`: Backend API URL (default: `http://backend:8080/api/v1`)
- `quarkus.rest-client.midas-api.connect-timeout`: Connection timeout in ms (default: `3000`)
- `quarkus.rest-client.midas-api.read-timeout`: Read timeout in ms (default: `5000`)
- `quarkus.rest-client.midas-api-health.url`: Backend health endpoint (default: `http://backend:8080`)

## Environment Variables

- **Variable substitution**: Use `${VAR_NAME:-default}` syntax
- **Timezone**: `TIMEZONE` (default: `UTC`)
- **Image tag**: `REVISION` (default: `1.0.0-SNAPSHOT`)
- **Quarkus config overrides**: Pass Quarkus configuration as environment variables
- **Never hardcode real secrets** as default values in `docker-compose.yml`. Use placeholder strings (e.g., `changeit`) that are obviously meant to be replaced.

## Logging

Logging is configured via Quarkus environment variables:

- **Console logging**: JSON-enabled in production, plain text in dev/test
- **File logging**: enabled in production, rotated (10MB, 3 backups)
- **Log path**: `logs/midas-gui.log` (relative to `/deployments/` inside container)

## Session Security

- `quarkus.http.same-site-cookie.session.value=Strict` is enforced in `application.properties`
