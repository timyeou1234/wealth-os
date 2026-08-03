# MVP Non-goals

## Purpose

This document keeps the first Wealth OS release focused on establishing a trustworthy
historical personal balance sheet, objective financial-position and structure views, and
basic snapshot comparison.

An excluded capability is not necessarily a bad idea. It is excluded because it is not
required to prove the first product outcome.

## Financial activity tracking

The MVP does not include:

- Transaction import or transaction history
- Expense categorization
- Budgeting or envelope budgeting
- Income tracking
- Savings-rate calculation
- Cash-flow statements

These capabilities require a different data model and ongoing transaction maintenance. They
are not necessary to understand a point-in-time balance sheet.

## Automated integrations

The MVP does not include:

- Bank account aggregation
- Brokerage account synchronization
- Crypto exchange or wallet synchronization
- Real-time market prices
- Automatic property or vehicle valuation
- Background refresh of financial positions or market prices

Historical CBC foreign-exchange reference-rate synchronization is an explicit exception.
The first Issue #66 slice stores and serves those rates; later slices consume them during
user-confirmed Snapshot capture. Synchronization never mutates a saved Snapshot or position.

Manual entry is an intentional first integration. Automation may be added later only when it
preserves provenance, reproducibility, and user trust.

## Advice and planning

The MVP does not include:

- Investment recommendations
- Regulated financial advice
- AI-generated financial actions
- Personalized healthy or unhealthy thresholds
- Goal tracking
- Retirement planning
- FIRE calculations
- Tax planning or filing
- Wealth forecasting or scenario simulation

The first release describes the user's current financial structure. It does not prescribe
what the user should do next.

## Advanced analytics

The MVP does not include:

- Investment performance attribution
- Benchmark comparison
- Diversification or concentration scoring
- Risk-adjusted returns
- Net-worth growth attribution between snapshots
- Attribution of snapshot changes to market performance, contributions, withdrawals,
  income, spending, or foreign exchange
- A composite financial-health score

Basic snapshot comparison is included: totals and item-level values may be compared, and
positions may be marked added, removed, or changed. Effective dates remain visible, but
the MVP does not impose one automatic stale-data threshold across all position types.
Performance and causal attribution remain excluded until the required transaction and
market data exists.

## Collaboration and platforms

The MVP does not include:

- Multi-user household collaboration
- Advisor or accountant access
- Public sharing
- Native iOS or Android applications
- Payment initiation, trading, or custody
- Full Account, Holding, and security-instrument domain models

The initial product is a single-user web application focused on the project creator as the
first customer.

## Engineering exclusions

The MVP does not require:

- Microservices
- Event-driven distributed infrastructure
- Global multi-region deployment
- Premature support for every asset or liability subtype

Engineering complexity must be justified by the first end-to-end product flow rather than by
hypothetical future scale.

## Reconsideration rule

A non-goal may move into scope only when there is evidence that it is necessary to improve
the core experience of understanding historical financial position and structure. Such a
change should be documented in a new issue and reflected in the product definition before
implementation begins.
