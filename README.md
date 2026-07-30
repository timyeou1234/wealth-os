# Wealth OS

> A trustworthy historical personal balance sheet.

Wealth OS is a private, balance-sheet-first tool for periodically recording, reviewing,
and understanding a person's material financial position over time. It centers on the
questions that matter over decades: What do I own? What do I owe? What is my net worth?
What changed since the last review?

It is intentionally **not** a bookkeeping or budgeting application. Transactions and
spending may eventually provide useful context, but the product's primary model is the
personal balance sheet.

## Why Wealth OS exists

Financial information is usually fragmented across banks, brokerages, property records,
loans, and spreadsheets. That fragmentation makes it difficult to see a complete,
historical picture or make long-term decisions with confidence.

Wealth OS aims to provide:

- One coherent view of assets, liabilities, and net worth
- Reproducible historical snapshots and basic comparisons between them
- Objective views of leverage and immediate liquidity
- A trustworthy foundation for future portfolio, debt, insurance, estate, tax, and
  AI-assisted planning capabilities

The first customer is the project's creator. A complete monthly update, trustworthy data,
and useful review during material financial decisions take priority over feature volume.

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

The backend begins as a modular monolith. The MVP domain centers on assets, liabilities,
point-in-time valuations and balances, snapshots, financial-position calculations, and
financial-structure calculations. Accounts, holdings, instruments, and market data remain
future candidates rather than current MVP aggregates. Business rules belong inside domain
modules; transport and persistence remain adapters.

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

1. **Foundation — Complete:** repository workflow, product vision, modular-monolith ADR,
   Kotlin build, core financial facts, and initial calculation engine.
2. **Product convergence — In progress:** align snapshot comparison, structured valuation
   provenance, and remaining domain language before persistence.
3. **Data design — In progress:** snapshot reproducibility and correction semantics are
   defined; next are PostgreSQL schema, repository ports, and persistence adapters.
4. **API design:** resources, DTOs, OpenAPI conventions, and generated-client workflow.
5. **First vertical slice:** manual entry, current position and structure, saved snapshots,
   basic snapshot comparison, and the web dashboard.

## Development workflow

Every change begins with a GitHub issue:

```text
Idea → Product discussion → GitHub issue → Architecture discussion (when needed)
     → API design → Implementation → Pull request → Review → Merge
```

Use [Conventional Commits](https://www.conventionalcommits.org/), keep pull requests
small, and link each pull request to its issue. See [CONTRIBUTING.md](CONTRIBUTING.md).

### API build

The Kotlin API uses the committed Gradle Wrapper and a Java 21 toolchain. A global
Gradle installation is neither required nor supported as the project build contract.

```bash
./gradlew :apps:api:test
./gradlew :apps:api:build
```

## Project status

Product definition and the first Kotlin domain foundation are implemented. The repository
contains immutable monetary facts, assets, liabilities, snapshots, and deterministic
financial calculations with automated tests.

The current work is completing domain convergence before persistence and HTTP APIs.
Snapshots now preserve point-in-time metadata and distinguish financial time from audit
time; corrections are immutable, attributable full replacements. Remaining work includes
structured valuation provenance, correction-chain persistence constraints, and database
schema design. No web vertical slice exists yet.

## License

Licensed under the [MIT License](LICENSE).
