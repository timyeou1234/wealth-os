# ADR-008: Use auditable historical FX conversion into TWD

- **Status:** Proposed
- **Date:** 2026-08-03
- **Decision owners:** Wealth OS maintainers

## Context

Foreign-currency positions currently require users to calculate a base-currency value and
describe the assumption in free text. Repeated manual conversion is slow and cannot
reliably reproduce which historical rate generated a Snapshot value. Calculation currency
and display preference have also been described by the same base-currency term.

## Decision

This decision supersedes ADR-005 only where ADR-005 defers automatic conversion and
requires callers to pre-convert every Snapshot fact. ADR-005's exact `Money`, explicit
currency, and rounding guardrails remain in force.

TWD is the canonical valuation currency. Positions retain original ISO 4217 Money. For a
Snapshot date, Wealth OS selects the latest CBC reference rate whose rate date is not in
the future, previews the result, and converts only after explicit confirmation. Valid ISO
currencies unsupported by CBC require a user-declared rate and basis.

Rates are stored in PostgreSQL as direct original-currency/TWD quotes with provider and
rate date. A memory cache may accelerate reads but is never authoritative. Frankfurter is
the initial CBC transport adapter. Synchronization bootstraps available history, catches
up on startup, and runs at 18:30 Asia/Taipei on weekdays. Provider failures never trigger
a silent provider change.

Once the Snapshot slice of Issue #66 lands, each converted immutable Snapshot fact stores
original Money, TWD Money, applied rate, actual rate date, provider, rate type, optional
user basis, and HALF_EVEN rounding mode.
Display currency is a presentation preference; historical displays resolve conversion
using each Snapshot date and never alter canonical results.

## Consequences

### Positive

- Snapshot totals remain deterministic and currency-consistent.
- Historical values are reproducible from self-contained conversion evidence.
- Users can distinguish CBC reference values from their own declared assumptions.
- Display preferences cannot silently change calculations.

### Negative

- Snapshot position storage and API contracts become larger.
- The system operates a historical-rate synchronization job and upstream adapter.
- CBC reference rates are objective references, not executable bank trading rates.

## Guardrails

- Never use a rate dated after the Snapshot date.
- Never mutate an existing Snapshot when rates synchronize.
- Never silently fall back from CBC to another provider.
- Use `BigDecimal`; permit at most 12 decimal places for a rate and round only final TWD
  Money to zero fraction digits with `RoundingMode.HALF_EVEN`.
- Record `REFERENCE_RATE` for accepted CBC data and `USER_DECLARED` for every override.
- Keep PostgreSQL authoritative and synchronization idempotent.
- Do not attribute a Snapshot comparison delta to FX or position changes.

## Alternatives considered

### Keep manual free-text conversion

Rejected because it repeats low-value work and cannot reliably reproduce the applied rate.

### Use USD as an intermediate hub

Rejected because canonical calculations are TWD and an intermediate hop adds needless
rates, rounding choices, and provenance.

### Revalue old Snapshots with current rates

Rejected because it violates self-contained historical Snapshot semantics.

### Automatically fall back to blended providers

Rejected because a provider change would silently change the meaning of stored values.
