# Wealth OS repository instructions

Before making changes, read and follow:

- `CONTRIBUTING.md`
- the relevant documents under `docs/product/`
- the relevant documents under `docs/architecture/`
- applicable decisions under `docs/adr/`

`CONTRIBUTING.md` is the canonical source for branch naming, commits, testing, and pull
requests. Do not write production code without a corresponding GitHub Issue. Keep
changes within that Issue's scope and acceptance criteria.

## Planning and alignment

Before implementing work with unclear requirements, material product decisions,
architecture changes, or data-model changes, use the repository's `grilling` skill.

- Resolve one decision at a time and include a recommended answer with each question.
- Discover facts from the repository instead of asking the user for them.
- Do not begin implementation until the user confirms shared understanding.
- Skip grilling for documentation, formatting, mechanical changes, and work governed by
  a complete accepted specification.

## Test-driven development

Use the repository's `tdd` skill for new behavior, reproducible bug fixes, and changes
to calculations, validation, permissions, or data transformations.

- Before writing tests, identify the public seams to test and confirm them with the user.
- Work in vertical slices: one failing behavior test, the smallest implementation that
  passes it, then repeat.
- Test observable behavior through public interfaces, not implementation details.
- Use independent expected values; do not restate the implementation in assertions.
- Mock only at system boundaries. Prefer real internal collaborators and a test database
  where practical.
- Keep refactoring for the review stage after the red-green implementation cycles.
- TDD may be skipped for documentation, formatting, generated files, non-behavioral
  configuration, and explicitly disposable prototypes. For other skipped behavioral
  changes, state the reason and use the closest practical verification method.

## Code review

Before opening or marking a pull request ready for review, use the repository's
`code-review` skill for product and code changes.

- Compare the branch against the merge base of the remote default branch.
- Review repository standards and the originating Issue or specification as separate
  axes so one cannot mask problems in the other.
- Report actionable findings first with severity and tight file/line context.
- Do not silently modify code while reviewing. Apply fixes only when the user asks.
- Documentation-only and agent-workflow changes may use a single-pass review, but must
  still check scope, instruction consistency, broken references, and validation evidence.
