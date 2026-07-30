# ADR-002: Capture self-contained historical snapshots

- **Status:** Accepted
- **Date:** 2026-07-30
- **Decision owners:** Wealth OS maintainers

## Context

Wealth OS promises a trustworthy historical personal balance sheet. Recalculating an old
snapshot must therefore produce the same financial position and structure even when the
current description or classification of an asset changes.

The initial domain model stored asset valuations and liability balances in `Snapshot` but
provided current `Asset` metadata separately to the calculation service. A later change to
an asset's name, type, or liquidity could silently reinterpret historical structure.
Historical display and item-level comparison would have the same problem.

The project also needs explicit correction semantics. Mutating a stored snapshot in place
would erase the original fact and make an audit trail impossible to explain.

## Decision

A Snapshot owns immutable, self-contained line items:

- An asset line item captures asset identity, display name, type, liquidity,
  point-in-time value, effective time, and provenance.
- A liability line item captures liability identity, display name, point-in-time balance,
  effective time, and provenance.

Financial-position and financial-structure calculations use only the captured Snapshot.
They do not consult current Asset or Liability entities.

A normal capture receives the complete in-scope Asset and Liability collections together
with their point-in-time valuations and balances. Capture fails when a position is missing
its monetary fact, a fact references an unknown position, or either collection contains a
duplicate identity. Only after this completeness check does the aggregate freeze
self-contained line items.

A normal Snapshot has no predecessor. Correcting a saved Snapshot creates a new immutable
Snapshot with the same `asOf` time and a `supersedes` reference to the Snapshot being
corrected. The prior Snapshot remains unchanged. The application and persistence layers
will later validate that the referenced Snapshot exists and manage correction chains.

## Consequences

### Positive

- Historical totals and liquidity structure remain reproducible.
- Historical screens do not display current names or classifications as if they were old
  facts.
- Snapshot comparison has stable item identities and point-in-time labels.
- Corrections remain explainable without deleting the original record.
- Persistence can treat a Snapshot as an append-only historical record.

### Negative

- Snapshot line items intentionally duplicate descriptive metadata from current entities.
- Renaming or reclassifying a position affects only future snapshots.
- Snapshot storage is larger than storing monetary facts alone.
- Application and persistence layers must later validate supersession references and
  correction-chain rules.

## Guardrails

- A Snapshot is immutable after creation.
- A line item's identity must match its valuation or balance identity.
- A normal capture has exactly one monetary fact for every in-scope position.
- No line item may be effective after the Snapshot's `asOf` time.
- A Snapshot contains at most one line item for each asset or liability identity.
- A Snapshot cannot supersede itself.
- Calculation services must not load current position metadata to interpret history.
- Editing current Asset or Liability metadata never rewrites a saved Snapshot.

## Alternatives considered

### Version all Asset and Liability metadata

Rejected for the MVP. A temporal entity model could reconstruct point-in-time metadata,
but it adds version selection and persistence complexity before the first vertical slice.

### Store only derived totals in Snapshot

Rejected because totals would not explain contributing positions, support item-level
comparison, or allow calculation rules to be verified later.

### Recalculate history using current metadata

Rejected because it violates the product promise that historical facts do not silently
change.

### Mutate an incorrect Snapshot

Rejected because it destroys the original record and obscures when and why history was
corrected.
