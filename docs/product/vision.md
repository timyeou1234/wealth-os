# Product Vision

## Vision

Provide a trustworthy historical personal balance sheet: a private record of what an
individual owns, what they owe, and how that financial position changes over time.

## Primary user

The initial user is a financially engaged individual who has assets and liabilities
spread across institutions and currently relies on fragmented apps or spreadsheets.
They value ownership of their financial picture, historical context, and decision
quality more than daily spending categorization.

The project creator is the first and most important customer. Work should prove useful
in a monthly full update and whenever a material financial decision or event calls for a
review before optimizing for a broader market.

## Problem

Existing tools commonly optimize for transactions, budgets, or a single institution.
They make it difficult to answer:

- What is my complete net worth today?
- Which assets and liabilities explain the change since an earlier date?
- How leveraged is my balance sheet, and how much is immediately liquid?
- Can I trust the values, dates, currencies, and provenance behind the answer?

## Product promise

Wealth OS will make the state and historical trajectory of personal wealth
understandable. It will favor explainable calculations, explicit assumptions, and
traceable data over false precision.

## Core capabilities

The first useful product should:

1. Represent material assets, liabilities, and their point-in-time values.
2. Calculate net worth consistently as of a point in time.
3. Preserve immutable or reproducible historical snapshots.
4. Compare adjacent snapshots using total and item-level changes.
5. Separate financial position from objective financial-structure indicators.

The MVP does not attribute changes to market performance, contributions, withdrawals,
income, or spending. That requires transaction and cash-flow data outside the first
balance-sheet model.

## Principles

- The balance sheet is the primary lens.
- A number without an effective date, currency, and source is incomplete.
- Historical facts should not silently change.
- Derived values should be reproducible and explainable.
- Manual data entry is a valid first integration.
- Sensitive data collection should be minimized.
- Automation must not reduce user trust.

## Non-goals for the initial product

- Double-entry bookkeeping or business accounting
- Transaction categorization and envelope budgeting
- Payment initiation, trading, or custody of funds
- Tax filing or regulated financial advice
- Multi-user household collaboration
- Real-time market data
- Automated bank aggregation
- Native mobile applications
- Microservices or globally distributed infrastructure

These may be revisited only when evidence shows they support the core wealth-management
experience.
