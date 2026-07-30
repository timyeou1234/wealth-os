# ADR-003: Model core financial facts with domain-driven design

- **Status:** Accepted
- **Date:** 2026-07-29
- **Decision owners:** Wealth OS maintainers

## Context

The MVP must represent assets, liabilities, valuations, balances, and snapshots in a
way that keeps financial facts reproducible and explainable. Those rules are more
durable than the first HTTP, database, or frontend implementation. If they are placed
in controllers, ORM entities, or client code, each adapter can interpret the same
financial fact differently.

## Decision

Model core financial concepts in an explicit domain layer using the language defined in
the architecture documentation: assets, liabilities, valuations, balances, snapshots,
money, and currency.

Domain entities and value objects own their invariants. Derived financial-position and
financial-structure values are calculated by pure domain services from an immutable
snapshot and are not persisted as a second source of truth. The domain layer does not
depend on Spring, JPA, HTTP, or database types.

Application use cases coordinate domain behavior and repository ports. HTTP and
persistence remain adapters around those use cases.

## Consequences

### Positive

- Financial invariants can be tested without infrastructure.
- The same calculations can serve API and dashboard adapters without duplication.
- Time-varying facts remain distinct from mutable position metadata.
- Persistence and transport choices can evolve without rewriting core rules.

### Negative

- Mapping is required between domain objects and persistence or transport models.
- Contributors must maintain the ubiquitous language rather than introducing
  adapter-specific synonyms.
- Some application flows need explicit use-case types before an adapter can call them.

## Guardrails

- Do not put financial calculations in controllers, DTOs, JPA entities, or SQL views.
- Do not mutate a historical snapshot to store a derived result.
- Add a domain test whenever a business invariant is introduced or changed.
- Record new aggregate boundaries or cross-module communication decisions in ADRs.

## Alternatives considered

### Database-first model

Rejected because table shape would become the primary model before financial invariants
are understood, making calculations and historical semantics harder to protect.

### Framework-centric service layer

Rejected because Spring annotations and transport concerns would couple core financial
rules to one delivery mechanism.
