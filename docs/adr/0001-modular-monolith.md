# ADR-001: Begin with a modular monolith

- **Status:** Accepted
- **Date:** 2026-07-26
- **Decision owners:** Wealth OS maintainers

## Context

Wealth OS needs clear business boundaries and high engineering quality, but its initial
scale is one primary user, one development team, and an evolving domain. Distributed
services would introduce network failure modes, duplicated operational concerns, and
cross-service data consistency before those costs solve an observed problem.

The frontend and backend still need independent release lifecycles.

## Decision

Build the backend as a Kotlin/Spring Boot modular monolith backed by PostgreSQL.

Organize code around business capabilities rather than technical layers shared across
the entire application. Each module owns its domain rules and exposes explicit
interfaces. Transport and persistence are adapters around application use cases.

Keep the Next.js web application and Spring Boot API as independently buildable and
deployable applications inside the monorepo.

## Consequences

### Positive

- Domain boundaries can evolve through ordinary refactoring.
- Transactions and consistent financial calculations remain straightforward.
- Local development, testing, deployment, and observability stay simple.
- A single repository preserves end-to-end change visibility.
- Boundaries create an extraction path if a proven scaling need appears.

### Negative

- Module isolation relies partly on build and architecture tests rather than networks.
- A single backend deployment can affect all backend modules.
- Care is required to prevent shared database access from eroding ownership.

## Guardrails

- No module reads another module's internal tables or persistence classes.
- Business logic does not live in controllers, DTOs, or framework configuration.
- Cross-module access uses explicit application interfaces or published events.
- Architecture tests should enforce package dependencies when implementation begins.
- New services require a separate ADR with measured operational or scaling evidence.

## Alternatives considered

### Microservices

Rejected for now because team size, load, deployment isolation, and independent scaling
do not justify the operational and consistency costs.

### Unstructured monolith

Rejected because it would make domain ownership unclear and future change riskier.

### Serverless functions per feature

Rejected because it fragments business transactions and introduces distributed-system
concerns without a current product need.
