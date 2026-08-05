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
WEALTHOS_AUTH_ISSUER_URI=https://YOUR_AUTH0_DOMAIN/ \
WEALTHOS_AUTH_AUDIENCE=https://YOUR_WEALTH_OS_API_AUDIENCE \
WEALTHOS_ALLOWED_EMAILS=YOUR_VERIFIED_EMAIL \
WEALTHOS_FX_SYNC_CLIENT_ID=YOUR_DEVELOPMENT_M2M_CLIENT_ID \
WEALTHOS_SWAGGER_ENABLED=true \
./gradlew :apps:api:bootRun
```

The API fails to start without an explicit OAuth issuer and audience. Personal endpoints
require an Auth0 user access token containing `email` and boolean `email_verified` claims;
the verified email must match the comma-separated server allowlist. `POST
/api/v1/fx-rates/sync` instead requires the
configured M2M client and separate `fx:sync` machine authority. Swagger is disabled by
default and must be explicitly enabled for local development. Never commit real tenant
identifiers, tokens, client secrets, or allowlist values.

With the application running, its generated API documentation is available at:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Documentation remains reachable for local development, but trying an authenticated
operation requires the appropriate development Bearer token.

Migrations V10-V12 intentionally clear unowned development Assets, Liabilities, and
Snapshots before adding mandatory owner keys. Re-import development data only after the
allowlisted identity has been provisioned; there is no legacy owner backfill.

The HTTP conventions that future feature endpoints must follow are documented in
[`docs/architecture/api-contract.md`](../../docs/architecture/api-contract.md).

## IntelliJ IDEA

Open the repository root as a Gradle project and configure:

- Gradle distribution: Wrapper
- Gradle JVM: Java 21
- Build and test using: Gradle

The root package is `com.wealthos`. Code is organized by business capability, and domain
types remain independent of Spring, HTTP, and persistence frameworks.
