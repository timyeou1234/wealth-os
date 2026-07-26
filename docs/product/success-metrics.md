# Success Metrics

Success is defined first by usefulness and trust, then by system delivery.

## Product outcomes

Before broadening scope, Wealth OS should demonstrate:

| Outcome | Initial signal |
| --- | --- |
| Complete picture | All material personal assets and liabilities can be represented |
| Trustworthy net worth | Displayed totals can be reconciled to their source values |
| Historical insight | A user can compare snapshots and explain material changes |
| Sustainable usage | The creator returns for a weekly review for eight consecutive weeks |
| Efficient maintenance | A normal review/update can be completed in under 10 minutes |
| Decision support | The system surfaces at least one actionable long-term insight per quarter |

These are hypotheses. Exact targets should be reviewed after a usable vertical slice
exists and should not be gamed through additional low-value features.

## Quality measures

- Every material business rule has automated tests.
- API changes are visible in generated OpenAPI and checked for unintended breakage.
- Snapshot and net-worth calculations are deterministic for the same inputs.
- Recovery procedures are tested before real personal data is relied upon.
- Security-sensitive decisions have threat models or ADRs.
- Documentation changes accompany product and architecture changes.

## Explicit non-metrics

Lines of code, number of services, commit count, and feature count are not measures of
product or engineering success.
