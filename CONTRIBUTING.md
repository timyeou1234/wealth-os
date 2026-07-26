# Contributing to Wealth OS

Wealth OS uses a documentation-first, issue-driven workflow. The objective is not only
to ship features, but to preserve the reasoning and quality expected of a long-lived
financial system.

## Before starting

1. Search existing issues and documentation.
2. Describe the user problem before proposing a solution.
3. Open or select a GitHub issue with explicit acceptance criteria.
4. Record a consequential architecture decision as an ADR before implementation.
5. Design or update the API contract before endpoint implementation.

Do not write production code without a corresponding issue.

## Branches and commits

Create a focused branch from `main`. Use Conventional Commits:

```text
feat(api): add asset endpoint
feat(web): add dashboard page
fix(api): validate snapshot date
docs: clarify snapshot semantics
chore: initialize project
```

Keep commits cohesive. Avoid combining refactors, formatting, and product behavior in
one change unless they cannot reasonably be separated.

## Pull requests

- Link the issue using `Closes #<issue>`.
- Explain the product reason, not only the implementation.
- Identify architecture, API, data, security, and privacy effects.
- Include verification evidence appropriate to the change.
- Update documentation in the same pull request.
- Prefer small, reviewable pull requests.

At least one reviewer should be able to understand the change without reconstructing
unstated decisions from code.

## Engineering expectations

- Keep controllers and transport adapters thin.
- Put business rules in the owning domain module.
- Prefer immutable domain values and constructor injection.
- Use generated API clients rather than manually duplicating contracts.
- Never commit credentials or real personal financial data.
- Treat logs, fixtures, screenshots, exports, and backups as potential sensitive data.
