# Architecture Overview

## Context

Wealth OS begins as a single-user personal wealth platform while preserving boundaries
that can support future capabilities. The system favors operational simplicity,
explainable financial calculations, and independent frontend/backend delivery.

## System shape

```text
┌──────────────────────────┐       HTTPS / JSON       ┌──────────────────────────┐
│ Web application          │ ───────────────────────▶ │ API application          │
│ Next.js + React          │ ◀─────────────────────── │ Kotlin + Spring Boot     │
│ Generated API client     │       OpenAPI contract   │ Modular monolith         │
└──────────────────────────┘                           └────────────┬─────────────┘
                                                                  │
                                                                  ▼
                                                     ┌──────────────────────────┐
                                                     │ PostgreSQL               │
                                                     │ Transactional source     │
                                                     │ of truth                  │
                                                     └──────────────────────────┘
```

The applications share a repository and delivery conventions, not a deployment unit.
Each must remain independently buildable, testable, versionable, and deployable.

## Backend boundaries

Initial candidate modules are:

- **Accounts:** ownership/container context for financial positions
- **Assets:** non-security assets and valuations
- **Liabilities:** debt balances, terms, and classification
- **Holdings:** investment positions held by accounts
- **Market Data:** external quotes, exchange rates, and provenance
- **Snapshots:** point-in-time balance-sheet facts and history
- **Dashboard:** read-oriented projections composed from other modules

These are candidates, not finalized service boundaries. Domain modeling in Milestone 2
must clarify ownership, invariants, and language before packages or tables are created.

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
- Historical snapshots are immutable or reproducible from immutable facts.
- Migrations are forward-only in deployed environments and reviewed like application
  code.
- Real personal financial data is never used as a test fixture.

## Security posture

Authentication remains undecided until usage, deployment, and threat models are
documented. Before production data is introduced, the project must define:

- Identity, session, and account-recovery design
- Authorization boundaries and secure defaults
- Encryption and secret management
- Audit events and sensitive logging controls
- Backup, restore, retention, export, and deletion behavior
- Dependency and container supply-chain controls

## Deferred decisions

Milestone 2 must resolve or record:

- Domain model and aggregate boundaries
- Multi-currency valuation rules
- Snapshot creation and correction semantics
- Database ownership and schema conventions
- Authentication and deployment threat model
- Observability and audit requirements
