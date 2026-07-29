# Financial Position and Structure

## Purpose

Wealth OS separates balance-sheet facts about **financial position** from objective,
explainable ratios about **financial structure**.

The first release does not label either category as universally healthy or unhealthy. It
does not provide a personalized target, recommendation, or composite score.

## Product principle

> Every metric exists to answer a financial question, not to display another number.

## MVP rules

Every MVP derived value must be:

- Objective: derived from recorded financial data rather than subjective judgment.
- Balance-sheet-only: calculated without income, spending, budgets, goals, or forecasts.
- Explainable: accompanied by its formula and contributing balance-sheet items.
- Reproducible: calculated consistently for the same point-in-time snapshot.
- Currency-consistent: calculated after values are represented in the selected base currency.
- Non-prescriptive: it may describe financial condition but must not recommend an action.

## Asset liquidity classification

The immediately liquid asset share requires each asset to have one of three explicit
classifications:

- **Liquid:** available in the base currency or reasonably convertible without a material
  delay, sale process, or execution uncertainty.
- **Semi-liquid:** marketable but subject to meaningful settlement time, restrictions,
  transaction costs, volatility, or other access friction.
- **Illiquid:** dependent on a longer sale process, material transaction costs, or uncertain
  execution.

The classification must be visible and editable. Wealth OS must not silently infer a
classification that the user cannot inspect.

Only **Liquid** assets contribute to the MVP immediately liquid asset share. Semi-liquid
and illiquid assets remain visible in the breakdown but are excluded from the numerator.

## Financial position

### Total assets

**Question:** What is the total value of everything I own?

**Formula:**

```text
Total Assets = Sum of all asset values in the base currency
```

**Required data:** Assets and their point-in-time valuations.

**Explainability:** The total must expand into the individual assets and valuations that
contribute to it.

### Total liabilities

**Question:** What is the total amount I owe?

**Formula:**

```text
Total Liabilities = Sum of all liability balances in the base currency
```

**Required data:** Liabilities and their point-in-time balances.

**Explainability:** The total must expand into the individual liabilities and balances that
contribute to it.

### Net worth

**Question:** What is my current financial position after accounting for debt?

**Formula:**

```text
Net Worth = Total Assets - Total Liabilities
```

**Required data:** Total assets and total liabilities from the same snapshot.

**Explainability:** Net worth must link back to both totals and ultimately to every underlying
asset and liability.

## Financial structure

### Debt ratio

**Question:** How leveraged is my balance sheet?

**Formula:**

```text
Debt Ratio = Total Liabilities / Total Assets
```

**Required data:** Total assets and total liabilities from the same snapshot.

**Interpretation:** A larger ratio indicates that a greater share of the asset base is offset
by liabilities. The MVP displays the ratio without assigning universal healthy or unhealthy
thresholds.

**Edge cases:**

- When total assets are zero, the ratio is undefined rather than zero.
- Negative liability values are invalid unless a later domain decision explicitly supports
  them.

### Immediately liquid asset share

**Question:** How much of my asset base is readily available rather than tied up in illiquid
assets?

**Formula:**

```text
Immediately Liquid Asset Share = Liquid Assets / Total Assets
```

**Required data:** Asset valuations and the explicit liquidity classification of each asset.

**Interpretation:** A larger ratio indicates that more of the asset base is immediately
available. The MVP does not assume that a universally higher ratio is always better.

**Explainability:** The user must be able to inspect which assets are included as liquid and
which are excluded.

**Edge cases:**

- When total assets are zero, the ratio is undefined rather than zero.
- A missing or unknown classification makes the metric incomplete.
- Semi-liquid assets are excluded from the numerator rather than partially weighted.

## Deliberately excluded indicators

The following indicators may be useful later but are excluded because they require data or
judgment outside the MVP balance sheet:

- Emergency-fund months, which requires monthly spending.
- Savings rate, which requires income and spending.
- Cash flow, which requires transactions or periodic income and expenses.
- Goal progress, which requires personal goals and target dates.
- Retirement readiness or FIRE progress, which requires assumptions and forecasts.
- Concentration or diversification scoring, which requires a separately agreed risk model.
- A single financial-health score, which would hide weighting and interpretation choices.

## Future evolution

Future versions may introduce personalized goals, thresholds, and recommendations only
after the underlying balance sheet and structure definitions remain trustworthy and
explainable.
