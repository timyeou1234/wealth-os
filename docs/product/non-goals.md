# MVP Non-goals

## Purpose

This document keeps the first Wealth OS release focused on establishing a trustworthy
personal balance sheet and a small set of objective financial-health indicators.

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
- Background data refresh

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
- A composite financial-health score

Historical snapshots are included as trustworthy records, but advanced comparison and
interpretation can be introduced after the core data model is validated.

## Collaboration and platforms

The MVP does not include:

- Multi-user household collaboration
- Advisor or accountant access
- Public sharing
- Native iOS or Android applications
- Payment initiation, trading, or custody

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
the core experience of understanding financial position and financial health. Such a change
should be documented in a new issue and reflected in the product definition before
implementation begins.
