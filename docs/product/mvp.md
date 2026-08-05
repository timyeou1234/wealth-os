# MVP Definition

## Purpose

The Wealth OS MVP establishes a trustworthy historical personal balance sheet and turns
it into an understandable view of current position, financial structure, and basic change
over time.

The MVP is the foundation for future financial decisions. It does not recommend what the
user should do. It helps the user understand their current financial position and the
underlying data behind it.

## Product outcome

The first release should allow a financially engaged individual to answer:

1. What is my current financial position?
2. How leveraged and immediately liquid is that position?
3. What changed since my previous snapshot?
4. Which assets and liabilities explain each result?

## Primary user journey

Issue #66 changes the financial-data journey incrementally. Issue #13 adds the mandatory
private-access boundary before the product is deployed with real financial data.

1. An explicitly allowlisted user signs in with Google.
2. The user records material assets and liabilities in their original currencies.
3. Wealth OS converts supported foreign-currency facts into canonical TWD valuations
   using an inspectable historical reference rate.
4. The user reviews the original facts, conversion evidence, and TWD valuations.
5. The system calculates total assets, total liabilities, and net worth.
6. The system derives debt ratio and immediately liquid asset share from the same balance
   sheet.
7. The user can inspect the assets and liabilities behind every displayed metric.
8. The user saves a reproducible snapshot owned by their local Wealth OS identity.
9. The user compares it with their previous snapshot and inspects total and item-level
   changes.

## In scope

### Balance-sheet data

- Create, edit, and archive manually entered assets.
- Create, edit, and archive manually entered liabilities.
- Keep Dashboard and Input as distinct app-level destinations. Within Input, expose
  AI-assisted import and manual entry as peer modes, with AI-assisted import selected by
  default. Snapshot date and base currency are shared context above both modes. Manual
  entry guides the user through assets, liabilities, and final review. Switching modes or
  manual-entry steps does not discard the shared draft, and applying an AI import opens
  the manual review step.
- Record an original-currency value, effective date, and source for each valuation or
  balance.
- Calculate canonical TWD valuations from CBC reference rates when available. Permit an
  explicit user-declared rate and basis when a reference rate is unavailable or replaced.
- Keep display currency separate from canonical TWD calculation currency.
- Preserve enough source data to reproduce a historical snapshot.
- Offer an optional paste-only AI-assisted entry workflow: Wealth OS provides a strict
  prompt that asks the user's agent to interview across every asset and liability
  category before producing JSON. The prompt reflects the current shared Snapshot date
  and base currency, and imported JSON must match that context exactly. Wealth OS
  validates the JSON locally, but never sends financial data to an AI service or applies
  imported changes without user review.

### Private access

- Allow only configured, verified Google identities to sign in through Auth0.
- Resolve the authenticated session to a local Wealth OS User without accepting a
  client-supplied owner identifier.
- Isolate every Asset, Liability, Snapshot, comparison, and derived financial view by
  that session-derived User.
- Route every browser product request through the Next.js backend-for-frontend. Browser
  JavaScript never receives an access or refresh token.
- Keep human product sessions separate from machine administration. A front-end user
  cannot obtain operational authority.

### Financial position

- Calculate total assets.
- Calculate total liabilities.
- Calculate net worth.
- Display the balance-sheet composition behind each total.

### Financial structure

- Calculate debt ratio and immediately liquid asset share as defined in
  `financial-position-and-structure.md`.
- Explain each metric using its formula and contributing balance-sheet items.
- Avoid opaque composite scores or recommendations.

### History

- Create a point-in-time snapshot.
- View a saved snapshot.
- Preserve historical values without silently replacing them with current values.
- Compare two snapshots using changes in total assets, total liabilities, net worth, and
  matching asset and liability items.
- Mark added, removed, and changed items explicitly.
- Display each fact's effective date and source so the user can judge its freshness.

Basic comparison describes recorded value changes. It does not attribute a change to
market performance, deposits, withdrawals, income, spending, or foreign-exchange
performance.

The MVP does not automatically label facts as stale. Appropriate update frequency varies
by position: a property estimate may remain useful far longer than a cash balance. The UI
will present effective dates without applying one universal expiration threshold.

### MVP position boundary

An **Asset** is a material economic position whose value the user wants to track
independently, such as a bank deposit, a named stock position, cryptocurrency, or real
estate. A **Liability** is a material obligation tracked independently.

Full Account, Holding, security-instrument, and transaction aggregates are outside the
MVP. Institution and account labels may be optional descriptive metadata; they do not
own positions or calculations in the first model.

## Minimum data required

The MVP requires only data that belongs to the personal balance sheet:

- Authenticated owner identity
- Assets
- Liabilities
- Valuations
- Base currency
- Effective date
- Source or provenance
- Snapshot date

The MVP does not require income, spending, budgets, goals, or forecasts.

## Acceptance criteria

The MVP definition is satisfied when:

- An unauthenticated or non-allowlisted caller cannot access personal financial data.
- An authenticated user cannot discover or access another user's resources.
- A user can enter a representative set of assets and liabilities manually.
- Every value has a currency, effective date, and identifiable source.
- Total assets, total liabilities, and net worth are reproducible from the stored data.
- Debt ratio and immediately liquid asset share are reproducible from the same balance
  sheet.
- Every metric can be traced to its contributing assets and liabilities.
- A saved snapshot can be viewed later without its historical values silently changing.
- A user can compare two snapshots and identify total, added, removed, and changed
  position values without performance attribution.
- No feature in the first release depends on transaction history, income, spending, goals,
  automated account connections, or financial advice.

## Product constraints

- Manual entry is acceptable and expected for the first release.
- A complete update is expected monthly; users may review the balance sheet whenever a
  material decision or event occurs.
- Simplicity is preferred over broad asset-type automation.
- Explicit assumptions are preferred over false precision.
- A metric must answer a financial question rather than merely display another number.
- The MVP may describe financial condition but must not prescribe financial actions.

## Out of scope

See `non-goals.md` for the explicit MVP exclusions.
