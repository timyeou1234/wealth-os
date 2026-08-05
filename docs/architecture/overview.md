# Architecture Overview

## Context

Wealth OS begins as a single-user personal wealth platform while preserving boundaries
that can support future capabilities. The system favors operational simplicity,
explainable financial calculations, and independent frontend/backend delivery.

## System shape

```text
Google ──▶ Auth0 ──▶ Next.js BFF ──Bearer JWT──▶ private Spring Boot API
                         │                              │
                         ▼                              ▼
                 Redis session store             PostgreSQL
                 (opaque cookie key)       (users and financial truth)

Operational client ──M2M JWT with fx:sync─────────────▶│
```

The applications share a repository and delivery conventions, not a deployment unit.
Each must remain independently buildable, testable, versionable, and deployable.
The browser reaches product APIs only through same-origin Next.js BFF routes. The BFF
stores Auth0 sessions in Redis and gives the browser only an opaque HttpOnly session key.
The Spring API remains independently secured as an OAuth resource server and does not
trust the BFF as an identity assertion.

## Backend boundaries

The implemented MVP domain begins with:

- **Assets:** material economic positions and their point-in-time valuations
- **Liabilities:** material obligations and their point-in-time balances
- **Snapshots:** point-in-time balance-sheet facts and history
- **Financial Position and Structure:** deterministic totals and objective ratios

Snapshot comparison is an MVP capability that will compare immutable snapshot facts
without performing investment-performance attribution.

Accounts, Holdings, security instruments, Market Data, and Dashboard projections remain
candidate future boundaries. In the MVP, institution and account labels are descriptive
context rather than aggregates that own positions.

Dependencies should point toward domain logic:

```text
HTTP adapter ──▶ application use cases ──▶ domain model
                         │
                         ▼
                 repository ports ◀── persistence adapters
```

Domain modules communicate through explicit application interfaces or domain events.
They must not reach into one another's persistence implementation.

## API contract

- Spring code and annotations are the source of truth.
- `springdoc-openapi` produces OpenAPI and Swagger UI.
- The published specification is treated as a build artifact and compatibility surface.
- The web app generates typed clients from the OpenAPI document.
- Hand-maintained duplicate request/response types are prohibited.
- Error, pagination, date/time, money, currency, and identifier conventions must be
  designed before feature endpoints.

## Data principles

- PostgreSQL is the transactional source of truth.
- Money is modeled with explicit amount and ISO currency; never binary floating point.
- Time-varying values include effective timestamps and provenance.
The Issue #66 target architecture uses canonical TWD valuations. Until its Snapshot slice
lands, the existing manual-conversion capture contract remains active. In the target:

- Financial calculations use canonical TWD valuations. Original-currency values remain
  attached to their immutable Snapshot facts.
- Foreign-currency conversion is explicit and reproducible: a Snapshot records the
  applied rate, rate date, provider, rate type, and original money. Display currency is a
  presentation preference and never changes canonical calculations.
- Mixed-currency snapshots are incomplete rather than implicitly converted.
- Historical snapshots are immutable or reproducible from immutable facts.
- Historical structure calculations require the point-in-time liquidity classification
  associated with each saved valuation; current metadata must never rewrite old results.
- Migrations are forward-only in deployed environments and reviewed like application
  code.
- Real personal financial data is never used as a test fixture.

## Security posture

Issue #13 establishes the first deployable identity boundary. Auth0 brokers Google OIDC
login for explicitly allowlisted identities. Next.js is a mandatory backend-for-frontend,
uses a Redis-backed server-side session, and never exposes access or refresh tokens to
browser JavaScript. Spring validates access-token signature, issuer, audience, expiry, and
authority independently, then maps issuer plus subject to a local User.

Every financial application operation derives its owner from the authenticated Spring
Security context. Request paths, query parameters, and bodies never select an owner.
Cross-owner identifiers behave as not found. Official CBC FX rates remain shared
reference data; manual operational synchronization requires a separate machine identity
with `fx:sync` rather than a privileged front-end user.

Production exposes Next.js publicly and keeps Spring, Redis, PostgreSQL, and Swagger on
private networks. Development and production use separate Auth0 applications, audiences,
callback URLs, M2M clients, Redis namespaces, and secrets. See
[Authentication and Authorization](authentication-and-authorization.md) and
[ADR-009](../adr/0009-authenticated-bff-and-owner-isolation.md).

Authentication does not complete the production security posture. Backup and restore,
retention and deletion, audit events, sensitive observability, dependency controls, and
secret rotation remain explicit follow-up work before broader production use.

## Deferred decisions

Before persistence begins, the project must resolve or record:

- Persistence enforcement for linear snapshot-correction chains and latest-revision
  selection
- Database ownership and schema conventions
- Observability and audit requirements

Account and Holding aggregates, security instruments, and market-price data remain
deferred. Auditable historical CBC FX conversion is governed by ADR-008.
