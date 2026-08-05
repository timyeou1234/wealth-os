# Core Domain Model

## Purpose

The core domain represents financial facts and deterministic calculations over them. It
does not persist data, expose HTTP resources, or depend on Spring.

## Language and boundaries

```text
shared
├── Currency
└── Money

identity
├── User (entity)
├── UserId
└── ExternalIdentity (issuer + subject)

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
├── SnapshotId
├── SnapshotAssetPosition
├── SnapshotLiabilityPosition
├── SnapshotCorrection
└── CorrectionReason

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
The Snapshot FX slice of Issue #66 implements the following model. Legacy manual
conversion remains temporarily available until the Input UI migrates:

- TWD is the canonical valuation currency for Snapshot calculations.
- A Snapshot position preserves its original Money and an explicit applied conversion
  when the original currency is not TWD.
- An applied conversion records its decimal rate, rate date, provider, rate type, basis
  when user-declared, and rounding mode.
- `ManualConversion` stores that provenance as an optional structured value containing
  the original `Money`, a human-readable exchange-rate basis, and its effective time.
  The valuation or balance remains expressed in the Snapshot base currency; Wealth OS
  records but does not calculate the manual conversion.
- Conversion is performed by the Snapshot capture application before domain calculation;
  the immutable position value consumed by calculations is always TWD.
- Canonical TWD Money uses zero fraction digits and HALF_EVEN rounding for final
  conversion, independent of the JDK's ISO metadata.

## Identity

Asset, Liability, and Snapshot are entities. Equality is based on their UUID-backed
identity rather than mutable descriptive attributes.

Authentication claims do not enter financial domain entities. The identity application
boundary validates an external issuer and subject, resolves them to a local `UserId`, and
makes that current owner available to financial application use cases. Email is verified
allowlist and display metadata; it is not a stable domain identity.

Asset, Liability, and Snapshot persistence is partitioned by local `UserId`. HTTP clients
never supply that owner. Application services receive it only from the authenticated
security context, and financial repositories require it for every lookup and mutation.
Cross-owner identifiers are indistinguishable from missing identifiers. Snapshot
positions remain self-contained financial facts, while Snapshot ownership controls who
may load the aggregate; ownership is not duplicated into each immutable line item.

Official FX rates are system reference data and have no user owner. A user-declared rate
is evidence inside an owned Snapshot position and therefore inherits Snapshot access.

## Time

All effective times and snapshot dates use `Instant`. This avoids local-time and
timezone ambiguity in financial facts.

## Snapshot aggregate

Snapshot is an immutable point-in-time collection of self-contained asset and liability
positions. Each position freezes the display metadata, monetary fact, effective time, and
provenance required to display and recalculate history.

`asOf` records when the financial position was true. `recordedAt` separately records when
Wealth OS captured that immutable record. A Snapshot cannot be recorded before its
financial `asOf` time.

`baseCurrency` optionally records the selected currency context of an application-level
capture. Atomic entry captures always set it, even for an empty Snapshot, so the next
entry can restore the user's choice without inferring it from a position. It remains
optional for older and direct mixed-currency Snapshots and is preserved by corrections.

It enforces:

- A normal capture has exactly one valuation or balance for every in-scope current
  position, with no facts for unknown positions.
- No fact may be effective after the snapshot's `asOf` time.
- A position identity must match the identity of its valuation or balance.
- At most one position exists for an asset in one snapshot.
- At most one position exists for a liability in one snapshot.
- A correction cannot supersede itself.
- A correction cannot be recorded before the record it supersedes.
- Input collections are defensively copied.

Snapshot may contain multiple currencies because it records facts rather than silently
normalizing them. Position and structure calculations return an explicit
insufficient-data result for mixed-currency inputs. MVP application use cases are
responsible for collecting base-currency facts before calculation.

Position and structure calculations depend only on a Snapshot. Current Asset or Liability
metadata must not reinterpret historical names, classifications, or results.

A correction is a new immutable Snapshot with the same `asOf` time and explicit
`SnapshotCorrection` metadata containing its direct predecessor and a non-blank reason.
It receives complete replacement asset and liability position collections; no default
collections or patch semantics exist. The original remains unchanged.

Correction history is a linear chain. Application and persistence work must validate that
the predecessor exists and is the current terminal record, reject cycles, enforce at most
one direct successor, and resolve the chain's terminal record as the effective correction.
These history-wide checks cannot be performed by one aggregate in isolation.

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
- the previous and current effective dates for every reported position change.

It must not claim investment performance or causal attribution. Those require transaction,
cash-flow, market-price, and FX data outside the MVP.

Comparison does not classify facts as stale. Position types have materially different
update cadences, so consumers display the captured effective dates and sources rather than
applying one domain-wide expiration threshold.
