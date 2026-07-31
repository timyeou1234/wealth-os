# Wealth OS API

The API is an independently deployable Kotlin and Spring Boot modular monolith. It owns
the domain model, PostgreSQL persistence, database migrations, and HTTP API contract
described in the architecture documentation.

## Requirements

- JDK 21
- Docker Desktop (for PostgreSQL-backed integration tests and local development)
- No globally installed Gradle

## Commands

Run from the repository root:

```bash
./gradlew :apps:api:test
./gradlew :apps:api:build
./gradlew :apps:api:bootRun
```

With the application running, its generated API documentation is available at:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

The HTTP conventions that future feature endpoints must follow are documented in
[`docs/architecture/api-contract.md`](../../docs/architecture/api-contract.md).

## IntelliJ IDEA

Open the repository root as a Gradle project and configure:

- Gradle distribution: Wrapper
- Gradle JVM: Java 21
- Build and test using: Gradle

The root package is `com.wealthos`. Code is organized by business capability, and domain
types remain independent of Spring, HTTP, and persistence frameworks.
