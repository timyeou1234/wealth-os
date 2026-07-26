# MVP Definition

## Purpose

The Wealth OS MVP establishes a trustworthy personal balance sheet and turns it into a
small, understandable view of financial health.

The MVP is the foundation for future financial decisions. It does not recommend what the
user should do. It helps the user understand their current financial position and the
underlying data behind it.

## Product outcome

The first release should allow a financially engaged individual to answer:

1. What is my current financial position?
2. What does that position imply about my basic financial health?
3. Which assets and liabilities explain each result?

## Primary user journey

1. The user chooses a base currency.
2. The user records their assets and current valuations.
3. The user records their liabilities and current balances.
4. The system calculates total assets, total liabilities, and net worth.
5. The system derives the MVP financial-health ratios from the same balance sheet.
6. The user can inspect the assets and liabilities behind every displayed metric.
7. The user saves a reproducible snapshot for future comparison.

## In scope

### Balance-sheet data

- Create, edit, and archive manually entered assets.
- Create, edit, and archive manually entered liabilities.
- Record a value, currency, effective date, and source for each valuation.
- Convert values into the user's base currency using an explicit exchange-rate assumption.
- Preserve enough source data to reproduce a historical snapshot.

### Financial position

- Calculate total assets.
- Calculate total liabilities.
- Calculate net worth.
- Display the balance-sheet composition behind each total.

### Financial health

- Calculate the five MVP metrics defined in `financial-health.md`.
- Explain each metric using its formula and contributing balance-sheet items.
- Avoid opaque composite scores or recommendations.

### History

- Create a point-in-time snapshot.
- View a saved snapshot.
- Preserve historical values without silently replacing them with current values.

## Minimum data required

The MVP requires only data that belongs to the personal balance sheet:

- Assets
- Liabilities
- Valuations
- Currency
- Effective date
- Source or provenance
- Snapshot date

The MVP does not require income, spending, budgets, goals, or forecasts.

## Acceptance criteria

The MVP definition is satisfied when:

- A user can enter a representative set of assets and liabilities manually.
- Every value has a currency, effective date, and identifiable source.
- Total assets, total liabilities, and net worth are reproducible from the stored data.
- Debt ratio and liquidity ratio are reproducible from the same balance sheet.
- Every metric can be traced to its contributing assets and liabilities.
- A saved snapshot can be viewed later without its historical values silently changing.
- No feature in the first release depends on transaction history, income, spending, goals,
  automated account connections, or financial advice.

## Product constraints

- Manual entry is acceptable and expected for the first release.
- Simplicity is preferred over broad asset-type automation.
- Explicit assumptions are preferred over false precision.
- A metric must answer a financial question rather than merely display another number.
- The MVP may describe financial condition but must not prescribe financial actions.

## Out of scope

See `non-goals.md` for the explicit MVP exclusions.
