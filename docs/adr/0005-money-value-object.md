# ADR-005: Represent money as a currency-aware value object

- **Status:** Accepted
- **Date:** 2026-07-29
- **Decision owners:** Wealth OS maintainers

## Context

Every balance-sheet value must be precise, attributable to an ISO currency, and safe to
use in repeatable calculations. Binary floating-point values cannot represent many
decimal amounts exactly. A numeric field without an explicit currency also permits
invalid arithmetic and hides foreign-exchange assumptions.

## Decision

Represent monetary amounts as an immutable `Money` value object containing a
`BigDecimal` amount and a validated ISO 4217 `Currency` value object.

`Money` normalizes its scale to the currency's fraction digits. Operations that would
lose precision fail unless the caller explicitly supplies a `RoundingMode`. Addition,
subtraction, and comparison require equal currencies. Foreign-exchange conversion is
never implicit. MVP snapshot use cases collect values already expressed in the user's
selected base currency. When a value is manually converted, its provenance preserves
the original amount, currency, exchange-rate basis, and effective time needed to explain
the conversion.

## Consequences

### Positive

- Financial values cannot silently lose precision through binary floating point.
- Invalid same-number/different-currency arithmetic fails at the domain boundary.
- Rounding choices are visible in code and tests.
- Currency metadata travels with every valuation and balance.

### Negative

- Persistence and API adapters need amount and currency mappings rather than one
  primitive numeric field.
- Callers must handle mixed currencies and explicit rounding deliberately.
- Mixed-currency calculations produce explicit insufficient data rather than silently
  selecting or converting a currency.

## Guardrails

- Never use `Double` or `Float` for a financial amount.
- Never infer a currency from locale, user preferences, or a database default.
- Preserve amount and currency together in persistence and API contracts.
- Preserve the original amount, conversion basis, effective time, and provenance when a
  base-currency value is manually converted.

## Alternatives considered

### Decimal column without a currency value object

Rejected because it permits ambiguous and invalid cross-currency operations.

### Store minor units as an integer

Rejected for now because ISO 4217 fraction digits vary and the current Kotlin domain
model already provides exact decimal normalization. It can be reconsidered only with a
clear persistence or performance need.
