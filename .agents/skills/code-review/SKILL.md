---
name: code-review
description: Review the changes since a fixed point (commit, branch, tag, or merge-base) along two axes — Standards (does the code follow this repo's documented coding standards?) and Spec (does the code match what the originating issue/PRD asked for?). Runs both reviews in parallel sub-agents and reports them side by side. Use when the user wants to review a branch, a PR, work-in-progress changes, or asks to "review since X".
---

Two-axis review of the diff between `HEAD` and a fixed point the user supplies:

- **Standards** — does the code conform to this repo's documented coding standards?
- **Spec** — does the code faithfully implement the originating issue / PRD / spec?

Both axes run as **parallel sub-agents** so they don't pollute each other's context, then this skill aggregates their findings. Review only: do not modify the code unless the user separately asks for fixes.

## Process

### 1. Pin the fixed point

Use the fixed point the user supplies: a commit SHA, remote branch, tag, `HEAD~5`, etc. If none is supplied:

1. Discover the default branch name with `gh repo view --json defaultBranchRef`.
2. Fetch that branch from `origin`.
3. Use the remote-tracking ref `origin/<default-branch>`, never a potentially stale local branch. Fall back to `origin/main` only when the remote default cannot be discovered.

Resolve the fixed point, then pin the immutable comparison base once with `git merge-base <fixed-point> HEAD`. Capture the resulting SHA and use it for both axes:

- `git diff <merge-base-sha>...HEAD`
- `git log <merge-base-sha>..HEAD --oneline`

Before going further, confirm the fixed point and merge-base SHA resolve and the diff is non-empty. A bad ref or empty diff should fail here — not inside two parallel sub-agents.

### 2. Identify the spec source

Look for the originating spec in this order:

1. A path or Issue the user supplied.
2. GitHub Issue references in the PR body or commit messages (`#123`, `Closes #45`).
3. The Issue number embedded in the required branch format `<type>/<issue-number>-<description>` from `CONTRIBUTING.md`.
4. A PRD/spec file under `docs/`, `specs/`, or `.scratch/` matching the branch name or feature.
5. If nothing is found, ask the user where the spec is. If there is no spec, skip the **Spec** sub-agent and report "no spec available".

Fetch a discovered GitHub Issue with the connected GitHub tools when available. Otherwise use `gh issue view <number> --json number,title,body,url,state`. Treat the Issue body and acceptance criteria as the specification; do not infer unstated requirements from the title alone.

### 3. Identify the standards sources

Read `AGENTS.md` and `CONTRIBUTING.md`, plus relevant product, architecture, and ADR documents named by those instructions. Include any closer nested `AGENTS.md` that governs changed files.

On top of whatever the repo documents, the Standards axis always carries the **smell baseline** below — a fixed set of Fowler code smells (_Refactoring_, ch.3) that applies even when a repo documents nothing. Two rules bind it:

- **The repo overrides.** A documented repo standard always wins; where it endorses something the baseline would flag, suppress the smell.
- **Always a judgement call.** Each smell is a labelled heuristic ("possible Feature Envy"), never a hard violation — and, like any standard here, skip anything tooling already enforces.

Each smell reads *what it is* → *how to fix*; match it against the diff:

- **Mysterious Name** — a function, variable, or type whose name doesn't reveal what it does or holds. → rename it; if no honest name comes, the design's murky.
- **Duplicated Code** — the same logic shape appears in more than one hunk or file in the change. → extract the shared shape, call it from both.
- **Feature Envy** — a method that reaches into another object's data more than its own. → move the method onto the data it envies.
- **Data Clumps** — the same few fields or params keep travelling together (a type wanting to be born). → bundle them into one type, pass that.
- **Primitive Obsession** — a primitive or string standing in for a domain concept that deserves its own type. → give the concept its own small type.
- **Repeated Switches** — the same `switch`/`if`-cascade on the same type recurs across the change. → replace with polymorphism, or one map both sites share.
- **Shotgun Surgery** — one logical change forces scattered edits across many files in the diff. → gather what changes together into one module.
- **Divergent Change** — one file or module is edited for several unrelated reasons. → split so each module changes for one reason.
- **Speculative Generality** — abstraction, parameters, or hooks added for needs the spec doesn't have. → delete it; inline back until a real need shows.
- **Message Chains** — long `a.b().c().d()` navigation the caller shouldn't depend on. → hide the walk behind one method on the first object.
- **Middle Man** — a class or function that mostly just delegates onward. → cut it, call the real target direct.
- **Refused Bequest** — a subclass or implementer that ignores or overrides most of what it inherits. → drop the inheritance, use composition.

### 4. Spawn both sub-agents in parallel

In Codex, call `spawn_agent` twice without waiting between calls, once for each axis. Give each sub-agent only its axis-specific brief and the shared pinned diff context. Do not ask one sub-agent to perform both reviews. After both are running, wait for both results before aggregating. If sub-agents are unavailable, run the two reviews sequentially with isolated notes and disclose the fallback.

**Standards sub-agent prompt** — include:

- The full diff command and commit list.
- The list of standards-source files you found in step 3, **plus the smell baseline from step 3** pasted in full — the sub-agent has no other access to it.
- The brief: "Report — per file/hunk where relevant — (a) every place the diff violates a documented standard: cite the standard (file + the rule); and (b) any baseline smell you spot: name it and quote the hunk. Distinguish hard violations from judgement calls — documented-standard breaches can be hard, but baseline smells are always judgement calls, and a documented repo standard overrides the baseline. Skip anything tooling enforces. Under 400 words."

**Spec sub-agent prompt** — include:

- The diff command and commit list.
- The path or fetched contents of the spec.
- The brief: "Report: (a) requirements the spec asked for that are missing or partial; (b) behaviour in the diff that wasn't asked for (scope creep); (c) requirements that look implemented but where the implementation looks wrong. Quote the spec line for each finding. Under 400 words."

If the spec is missing, skip the Spec sub-agent and note this in the final report.

### 5. Validate and aggregate

Verify each proposed finding against the diff and its cited standard or specification before reporting it. Remove false positives and claims that are not actionable.

Present actionable findings first under `## Standards` and `## Spec`. For every finding include:

- severity (`P0`–`P3`),
- an actionable title,
- the tightest relevant file and line range,
- the violated rule or unmet requirement,
- why it matters and the smallest safe direction for resolving it.

Do **not** merge or rerank findings across the two axes (see _Why two axes_). If an axis has no findings, say so explicitly. Put a short summary after the findings, not before them.

End with a one-line summary: total findings per axis, and the worst issue _within each axis_ (if any). Don't pick a single winner across axes — that's the reranking the separation exists to prevent.

## Why two axes

A change can pass one axis and fail the other:

- Code that follows every standard but implements the wrong thing → **Standards pass, Spec fail.**
- Code that does exactly what the issue asked but breaks the project's conventions → **Spec pass, Standards fail.**

Reporting them separately stops one axis from masking the other.
