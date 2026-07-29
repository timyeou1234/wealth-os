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
- MVP calculations consume values already expressed in the user's selected base currency.
- Foreign-currency values are manually converted; their source must record the original
  amount, conversion rate or basis, and effective time needed to explain the assumption.
- Mixed-currency snapshots are incomplete rather than implicitly converted.
- Historical snapshots are immutable or reproducible from immutable facts.
- Historical structure calculations require the point-in-time liquidity classification
  associated with each saved valuation; current metadata must never rewrite old results.
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

Before persistence begins, the project must resolve or record:

- How snapshots preserve point-in-time descriptive metadata, including liquidity
- Snapshot creation and correction semantics
- Database ownership and schema conventions
- Authentication and deployment threat model
- Observability and audit requirements

Automatic FX conversion, Account and Holding aggregates, security instruments, and market
data are deliberately deferred beyond the MVP.
