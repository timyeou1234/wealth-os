# ADR-006: Record auditable snapshot corrections

- **Status:** Accepted
- **Date:** 2026-07-30
- **Decision owners:** Wealth OS maintainers

## Context

ADR-002 makes historical snapshots immutable and self-contained, but its initial
correction contract records only the snapshot being superseded. It does not distinguish
the financial time from the recording time, explain why a correction exists, or make
full-replacement semantics explicit.

Without those rules, a correction could look like a partial patch, silently omit
positions, or make it impossible to reconstruct when and why the historical record
changed. Once persistence begins, ambiguous correction semantics would become expensive
to repair.

## Decision

Every Snapshot records two distinct times:

- `asOf` is when the represented financial position was true.
- `recordedAt` is when that immutable record was captured by Wealth OS.

A normal capture has no correction metadata. A correction:

- preserves the superseded Snapshot's `asOf`;
- has a `recordedAt` that is not earlier than the record it supersedes;
- identifies its direct predecessor with `supersedes`;
- requires a non-blank reason; and
- receives explicit, complete replacement asset and liability position collections.

Correction inputs are not patches. Empty replacement collections mean that the corrected
balance sheet intentionally contains no positions; omitted default collections are not
available. The superseded Snapshot remains unchanged.

Persisted correction history is a linear chain. The application and persistence layers
must:

- verify that the direct predecessor exists;
- reject reuse of an existing Snapshot identity;
- prevent cycles;
- allow a saved Snapshot to be directly superseded at most once; and
- select the terminal record in the chain as the latest effective representation.

These history-wide rules do not belong inside one Snapshot aggregate because an aggregate
cannot query persisted records. The domain still rejects impossible local state such as
self-supersession and invalid recording times.

## Consequences

### Positive

- Financial time and audit time cannot be confused.
- Every correction explains its direct predecessor and purpose.
- Consumers always receive a complete immutable balance sheet rather than a patch.
- Historical records remain append-only and traceable.
- Persistence has explicit constraints for selecting one effective correction.

### Negative

- Snapshot persistence needs recording-time and correction metadata columns.
- Correcting one line still duplicates the complete corrected Snapshot.
- Creating a correction requires loading and validating correction history.
- Concurrent corrections need a database constraint or equivalent serialization.

## Guardrails

- Never update or delete the superseded Snapshot to apply a correction.
- Never infer omitted replacement positions from the predecessor.
- A correction must preserve the predecessor's `asOf`.
- A correction reason must remain attributable to that immutable record.
- `recordedAt` must not precede `asOf` or the predecessor's `recordedAt`.
- Persistence must enforce referential integrity and at most one direct successor.
- Application logic must reject corrections against a non-terminal record.

## Alternatives considered

### Mutate the original Snapshot

Rejected because it destroys the prior historical fact and its audit trail.

### Store partial correction patches

Rejected because replay and completeness become dependent on every prior patch, making
display, comparison, and recovery more complex.

### Use only `asOf`

Rejected because a backdated correction needs a separate timestamp showing when Wealth OS
recorded it.

### Choose the newest correction by recording time

Rejected because concurrent branches would make the effective record ambiguous. A linear
chain prevents that ambiguity instead of hiding it with ordering.
