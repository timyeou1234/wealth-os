# Success Metrics

Success is defined first by usefulness and trust, then by system delivery.

## Product outcomes

Before broadening scope, Wealth OS should demonstrate:

| Outcome | Initial signal |
| --- | --- |
| Complete picture | All material personal assets and liabilities can be represented |
| Trustworthy net worth | Displayed totals can be reconciled to their source values |
| Historical insight | A user can explain total and item-level changes without confusing them with performance attribution |
| Sustainable usage | The creator completes a monthly full update for six consecutive months |
| Efficient maintenance | A normal monthly update can be completed in under 10 minutes |
| Decision support | The creator uses the balance sheet during at least one material financial decision or event |

These are hypotheses. Exact targets should be reviewed after a usable vertical slice
exists and should not be gamed through additional low-value features.

## Quality measures

- Every material business rule has automated tests.
- API changes are visible in generated OpenAPI and checked for unintended breakage.
- Snapshot and net-worth calculations are deterministic for the same inputs.
- Snapshot comparisons are deterministic, distinguish added, removed, and changed
  positions, and preserve the effective dates needed to judge data freshness.
- Recovery procedures are tested before real personal data is relied upon.
- Security-sensitive decisions have threat models or ADRs.
- Documentation changes accompany product and architecture changes.

## Explicit non-metrics

Lines of code, number of services, commit count, and feature count are not measures of
product or engineering success.
