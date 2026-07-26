# Wealth OS

> A balance-sheet-first operating system for understanding and improving personal wealth.

Wealth OS is a modern personal wealth platform centered on the questions that matter over
decades: What do I own? What do I owe? How is my net worth changing? Is my financial
position becoming healthier?

It is intentionally **not** a bookkeeping or budgeting application. Transactions and
spending may eventually provide useful context, but the product's primary model is the
personal balance sheet.

## Why Wealth OS exists

Financial information is usually fragmented across banks, brokerages, property records,
loans, and spreadsheets. That fragmentation makes it difficult to see a complete,
historical picture or make long-term decisions with confidence.

Wealth OS aims to provide:

- One coherent view of assets, liabilities, and net worth
- Historical snapshots that explain how wealth changes over time
- Signals about concentration, leverage, liquidity, and long-term financial health
- A trustworthy foundation for future portfolio, debt, insurance, estate, tax, and
  AI-assisted planning capabilities

The first customer is the project's creator. Daily usefulness, data trust, and decision
quality take priority over feature volume.

## Product principles

- **Balance sheet first:** assets, liabilities, and net worth form the core model.
- **Product first:** every feature starts with a user problem and measurable outcome.
- **Documentation first:** important decisions are written down before implementation.
- **API first:** Spring annotations generate the OpenAPI contract; clients are generated.
- **Security first:** financial data is sensitive by default.
- **Simple by design:** start with a modular monolith and earn additional complexity.
- **Maintainable for years:** favor clear boundaries, naming, and tests over shortcuts.

Read the full [product vision](docs/product/vision.md) and
[success measures](docs/product/success-metrics.md).

## Architecture

Wealth OS is a monorepo with independently deployable applications:

```text
wealth-os/
├── apps/
│   ├── web/                 # Next.js, React, TypeScript, Tailwind CSS
│   └── api/                 # Kotlin, Spring Boot, REST, OpenAPI
├── docs/
│   ├── product/             # Vision, users, outcomes, and non-goals
│   ├── architecture/        # System and domain design
│   └── adr/                 # Architecture decision records
├── infra/                   # Docker and local infrastructure
├── scripts/                 # Repository automation
└── .github/                 # Contribution workflows and templates
```

The backend will begin as a modular monolith with explicit domain boundaries for
accounts, assets, liabilities, holdings, market data, snapshots, and dashboard
projections. Business rules belong inside domain modules; transport and persistence
remain adapters.

See the [architecture overview](docs/architecture/overview.md) and
[ADR-001](docs/adr/0001-modular-monolith.md).

## Technology

| Area | Technology |
| --- | --- |
| Web | Next.js, React, TypeScript, Tailwind CSS |
| API | Kotlin, Spring Boot |
| Data | PostgreSQL |
| Contract | REST, springdoc-openapi, Swagger UI |
| Infrastructure | Docker, Docker Compose |
| Authentication | Deliberately undecided pending threat modeling |

## Roadmap

1. **Milestone 0 — Repository bootstrap:** documentation, structure, contribution
   workflow, and GitHub project setup.
2. **Milestone 1 — Product definition:** vision, primary user, problem, outcomes,
   success measures, and non-goals.
3. **Milestone 2 — Architecture:** ADRs, domain model, data design, and security model.
4. **Milestone 3 — API design:** resource model, DTOs, OpenAPI conventions, and
   generated-client workflow.
5. **Implementation:** only after the preceding foundations and corresponding issues
   are approved.

## Development workflow

Every change begins with a GitHub issue:

```text
Idea → Product discussion → GitHub issue → Architecture discussion (when needed)
     → API design → Implementation → Pull request → Review → Merge
```

Use [Conventional Commits](https://www.conventionalcommits.org/), keep pull requests
small, and link each pull request to its issue. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Project status

Wealth OS is in its documentation and repository-bootstrap phase. Production
application code has not started.

## License

Licensed under the [MIT License](LICENSE).
