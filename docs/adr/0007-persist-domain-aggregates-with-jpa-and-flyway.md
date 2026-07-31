# ADR-007: Persist domain aggregates with JPA and Flyway

- **Status:** Accepted
- **Date:** 2026-07-30
- **Decision owners:** Wealth OS maintainers

## Context

Wealth OS needs durable storage for Assets, Liabilities, and immutable Snapshots without
coupling the domain model to Hibernate. Snapshot history must remain reproducible, and
correction chains must be append-only, linear, and safe under concurrent writes.

Allowing Hibernate to generate production schema would hide consequential constraints
inside runtime behavior. Mapping JPA annotations directly onto domain objects would also
make persistence concerns shape domain construction and invariants.

## Decision

Use PostgreSQL as the transactional source of truth, Spring Data JPA for persistence
adapters, and Flyway for forward-only schema migrations.

Keep JPA entities under capability-owned persistence adapters. Domain repository
interfaces are ports and contain no Spring or JPA types. Adapters explicitly map between
domain objects and persistence entities.

Use UUID primary keys, `timestamptz` for `Instant`, ISO currency codes, and exact
`numeric` columns for money. Store enum values as stable uppercase strings. Hibernate
validates the schema but never creates or updates it.

A Snapshot row owns immutable asset-position and liability-position rows. Correction
metadata is stored on the Snapshot row:

- `supersedes_id` references the direct predecessor;
- `correction_reason` is present exactly when `supersedes_id` is present; and
- a unique constraint on `supersedes_id` permits at most one direct successor.

Snapshots and their positions are inserted atomically and never updated or deleted by
normal application workflows. Application logic must require the predecessor to be the
terminal record before saving a correction. The database foreign key and unique
constraint provide the final concurrent-write defense.

## Consequences

### Positive

- The domain remains independent of Spring, Hibernate, and table shape.
- Database constraints preserve correction history under concurrency.
- Schema changes are explicit, reviewable, and reproducible.
- Snapshot aggregate writes can be committed or rolled back as one transaction.

### Negative

- Explicit mapping code exists between domain and persistence models.
- Snapshot corrections duplicate complete position rows by design.
- PostgreSQL-specific integration tests require a real database or container runtime.

## Guardrails

- Do not annotate domain types with JPA annotations.
- Do not use Hibernate schema generation outside disposable tests.
- Do not update or delete persisted Snapshots through normal repositories.
- Do not store `Money` as floating point or omit its currency.
- Do not select a latest correction by timestamp; follow the unique successor chain.
- Migrations are forward-only after deployment.

## Alternatives considered

### Annotate domain objects as JPA entities

Rejected because persistence construction, proxies, and mutable collection requirements
would leak into the domain model.

### Let Hibernate manage schema

Rejected because generated schema does not provide a stable, reviewed migration history
or make critical correction constraints sufficiently explicit.

### Store Snapshot as JSON

Rejected because relational constraints, item-level lookup, provenance validation, and
correction referential integrity are important parts of the data contract.
