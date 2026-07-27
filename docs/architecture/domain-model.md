# Core Domain Model

## Purpose

The core domain represents financial facts. It does not calculate financial-health
metrics, persist data, expose HTTP resources, or depend on Spring.

## Language and boundaries

```text
shared
├── Currency
└── Money

asset
├── Asset (entity)
├── AssetId
├── AssetType
├── Liquidity
├── AssetValuation (point-in-time fact)
└── ValuationSource

liability
├── Liability (entity)
├── LiabilityId
└── LiabilityBalance (point-in-time fact)

snapshot
├── Snapshot (aggregate root)
└── SnapshotId
```

Assets and liabilities describe positions. Their values and balances vary with time,
so those values are modeled as separate facts rather than mutable entity fields.

## Money and currency

- Currency codes are normalized and validated against ISO 4217 data from the JDK.
- Monetary values use `BigDecimal`, normalized to the currency's fraction digits.
- Precision loss fails by default.
- Callers must select a `RoundingMode` explicitly when rounding is intended.
- Arithmetic and comparison require the same currency.
- Foreign-exchange conversion is not implicit and is deferred to a future explicit
  valuation policy.

## Identity

Asset, Liability, and Snapshot are entities. Equality is based on their UUID-backed
identity rather than mutable descriptive attributes.

## Time

All effective times and snapshot dates use `Instant`. This avoids local-time and
timezone ambiguity in financial facts.

## Snapshot aggregate

Snapshot is an immutable point-in-time collection of asset valuations and liability
balances.

It enforces:

- No fact may be effective after the snapshot's `asOf` time.
- At most one valuation exists for an asset in one snapshot.
- At most one balance exists for a liability in one snapshot.
- Input collections are defensively copied.

Snapshot may contain multiple currencies because it records facts rather than silently
normalizing them. Issue #8 must reject mixed-currency calculation inputs until an
explicit conversion policy exists.

## Deferred calculations

`FinancialHealth`, totals, net worth, debt ratio, and liquidity ratio belong to Issue
#8. Keeping them outside this model prevents derived values from becoming a second
source of truth.
