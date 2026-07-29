# Core Domain Model

## Purpose

The core domain represents financial facts and deterministic calculations over them. It
does not persist data, expose HTTP resources, or depend on Spring.

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
├── LiabilitySource
└── LiabilityBalance (point-in-time fact)

snapshot
├── Snapshot (aggregate root)
└── SnapshotId

financialhealth (implemented package; product language is position and structure)
├── FinancialHealth
├── FinancialHealthCalculator (pure domain service)
├── FinancialHealthResult
└── FinancialRatio
```

Assets and liabilities describe positions. Their values and balances vary with time,
so those values are modeled as separate facts rather than mutable entity fields.

## Money and currency

- Currency codes are normalized and validated against ISO 4217 data from the JDK.
- Monetary values use `BigDecimal`, normalized to the currency's fraction digits.
- Precision loss fails by default.
- Callers must select a `RoundingMode` explicitly when rounding is intended.
- Arithmetic and comparison require the same currency.
- The user selects one base currency for an MVP snapshot.
- Values supplied to snapshot calculations are already expressed in that base currency.
- A manually converted value records the original amount, exchange-rate basis, and
  effective time in its provenance.
- Automatic foreign-exchange conversion is deferred. Mixed-currency facts produce an
  explicit insufficient-data result rather than implicit conversion.

## Identity

Asset, Liability, and Snapshot are entities. Equality is based on their UUID-backed
identity rather than mutable descriptive attributes.

## Time

All effective times and snapshot dates use `Instant`. This avoids local-time and
timezone ambiguity in financial facts.

## Snapshot aggregate

Snapshot is an immutable point-in-time collection of asset valuations and liability
balances. Both facts carry an effective time and identifiable provenance.

It enforces:

- No fact may be effective after the snapshot's `asOf` time.
- At most one valuation exists for an asset in one snapshot.
- At most one balance exists for a liability in one snapshot.
- Input collections are defensively copied.

Snapshot may contain multiple currencies because it records facts rather than silently
normalizing them. Position and structure calculations return an explicit
insufficient-data result for mixed-currency inputs. MVP application use cases are
responsible for collecting base-currency facts before calculation.

The current Snapshot stores values and provenance but receives Asset metadata separately
for structure calculations. Persistence must preserve or embed point-in-time liquidity
metadata before historical structure results can be considered reproducible. Current
Asset metadata must not be used to silently reinterpret an older snapshot.

## Financial-position and structure calculations

The currently named `FinancialHealthCalculator` derives financial position (total assets,
total liabilities, and net worth) and financial structure (debt ratio and immediately
liquid asset share) without mutating the source snapshot. The package name is retained
until a focused refactor issue changes public domain types.

- Assets use `LIQUID`, `SEMI_LIQUID`, or `ILLIQUID`.
- Only assets classified as `LIQUID` contribute to immediately liquid assets.
- Ratio values are fractions rather than percentages.
- Ratios use six decimal places and `RoundingMode.HALF_EVEN`.
- A zero total-assets denominator produces `FinancialRatio.Undefined`.
- Empty facts, missing or unknown asset valuations, and mixed currencies produce an
  explicit `FinancialHealthResult.InsufficientData`.
- Derived values are not stored in Snapshot and do not become a second source of truth.

## MVP asset boundary

An Asset is a material economic position whose value the user wants to track
independently. A bank deposit, named stock position, cryptocurrency position, or property
may each be an Asset in the MVP.

Accounts and Holdings are not current aggregates. Optional institution or account labels
may provide descriptive context, but they do not own calculations or introduce a separate
position hierarchy. A fuller account/instrument/holding model requires a later product
need and domain decision.

## Snapshot comparison

Basic comparison is an application/domain capability over two saved snapshots. It will
report:

- changes in total assets, total liabilities, and net worth;
- changed values for matching asset and liability identities;
- positions added or removed between snapshots; and
- facts whose effective dates indicate stale data.

It must not claim investment performance or causal attribution. Those require transaction,
cash-flow, market-price, and FX data outside the MVP.
