# ADR-004: Organize the backend by business capability

- **Status:** Accepted
- **Date:** 2026-07-29
- **Decision owners:** Wealth OS maintainers

## Context

Wealth OS is a modular monolith whose implemented MVP capabilities begin with assets,
liabilities, snapshots, and financial position and structure. These capabilities will
change at different rates. A global `controller`, `service`, `repository`, and `entity`
package layout would make ownership unclear and encourage unrelated capabilities to
share implementation details.

## Decision

Organize backend code first by business capability, then by the role required inside
that capability. For example, asset domain rules belong under an asset boundary and
snapshot rules under a snapshot boundary. Shared types are limited to genuinely common
financial concepts such as `Money` and `Currency`. Accounts, holdings, instruments, and
market data remain deferred boundaries until a concrete product need justifies them.

Within a capability, dependencies point inward:

```text
adapter -> application use case -> domain
                         |
                         v
                  repository port
```

Persistence implementations and HTTP adapters must not be imported by domain code.
Cross-capability access uses explicit application interfaces or published domain events,
not another capability's internal repository or tables.

## Consequences

### Positive

- Code ownership follows the language users and product documents use.
- A capability can change internally without exposing its persistence details.
- The package structure supports the extraction path described in ADR-001 without
  prematurely creating distributed services.
- Architecture tests can enforce boundary rules as the application gains adapters.

### Negative

- Small applications may initially have more packages than technical-layer grouping.
- Shared code requires scrutiny to avoid a broad, unowned `common` module.
- Cross-capability workflows need deliberate interfaces rather than convenient imports.

## Guardrails

- A package may be shared only when its concept has no single business owner.
- Do not create a global service layer containing unrelated business rules.
- A module must not read or write another module's internal persistence model.
- Add architecture tests when Spring and persistence adapters are introduced.

## Alternatives considered

### Package by technical layer

Rejected because it would scatter each financial capability across application-wide
packages and obscure both ownership and dependency direction.

### One Gradle module per capability immediately

Rejected because package boundaries and architecture tests provide sufficient isolation
while the domain is still evolving. Build-module extraction requires demonstrated need.
