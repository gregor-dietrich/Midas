# Docker Conventions — Midas

## Dockerfiles

Both modules have Dockerfiles in `src/main/docker/`:

- **Dockerfile.jvm-ubuntu** (primary): Eclipse Temurin JRE on Ubuntu. Used by `docker-compose.yml` and `make build`.
- **Dockerfile.jvm-alpine**: Smaller image. Use only if size is critical.
- **Dockerfile.legacy-jar**: Über-jar packaging.
- **Dockerfile.native** / **Dockerfile.native-micro**: GraalVM native.

### JVM Ubuntu Conventions

- Base image: `eclipse-temurin:<version>-jre-noble`
- Application user: UID/GID 185 (`jboss`) — non-root
- Layer caching: copy `target/quarkus-app/` in four layers (lib → jar → app → quarkus)
- Logs: `mkdir -p /deployments/logs && chown -R 185:185 /deployments/logs`
- Health check: `wget --spider http://localhost:8080/q/health/ready` (API) or `8081` (GUI)
- Exposed ports: 8080 (API), 8081 (GUI)
- Entrypoint: `java -jar /deployments/quarkus-run.jar`

### Build Prerequisite

Always package for production before building images:

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

Full-stack compose:

- **backend** (`midas-api`): port 8080, depends on `postgres` healthy
- **frontend** (`midas-gui`): port 80→8081, depends on `backend` healthy
- **postgres**: port `${SQL_PORT:-5432}`→5432
- **pgadmin**: port 42069→80

## REST Client Configuration (in compose)

Frontend connects to backend via env vars:

- `quarkus.rest-client.midas-api.url`: default `http://backend:8080/api/v1`
- `quarkus.rest-client.midas-api.connect-timeout`: 3000 ms
- `quarkus.rest-client.midas-api.read-timeout`: 5000 ms
- `quarkus.rest-client.midas-api-health.url`: `http://backend:8080`

## Environment Variables

- Substitution: `${VAR_NAME:-default}`
- Timezone: `TIMEZONE` (default `UTC`)
- Image tag: `REVISION` (default `1.0.0-SNAPSHOT`)
- Database: `SQL_USERNAME`, `SQL_PASSWORD`, `SQL_DATABASE`, `SQL_PORT`
- pgAdmin: `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`
- **Never hardcode real secrets** as defaults. Use placeholders (e.g., `changeit`).

## Logging

- Console: JSON in production, plain in dev/test
- File: enabled in production, rotated (10MB, 3 backups)
- Paths: `logs/midas-api.log` or `logs/midas-gui.log` (relative to `/deployments/`)

## Session Security (GUI)

- `quarkus.http.same-site-cookie.session.value=Strict` enforced in `application.properties`
