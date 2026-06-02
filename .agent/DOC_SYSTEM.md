# Agent Documentation System

Put durable facts in the right document and retrieve them by trigger. Private `.agent/` files are not memory and should not be published by default.

## Source-Of-Truth Hierarchy

1. `docs/PROJECT_FACTS.md`: exact commands, paths, versions, dependencies, deploy target, Git-finalization mode, release format, and compatibility constraints.
2. `docs/CHECKS.md`: validation choices and evidence expectations.
3. `docs/ARCHITECTURE.md`: stable design and subsystem boundaries.
4. `.agent/BRIEF.md`: current operational state and next step.
5. `.agent/PLAN.md`: active goals and pointer to `PLANS.md`.
6. `.agent/BACKLOG.md`: long-horizon work, parked candidates, and sequencing notes.
7. `.agent/HANDOVER.md`: stable private project context pointer to `HANDOVER.md`.
8. `.agent/archive/`: deep evidence, histories, decisions, retired plans, and investigations.

If the same fact appears in multiple places, update the source of truth and replace duplicates with pointers. LeanCTX memory should be compact routing hints or pointers only.

## Context Budgets

- Root `AGENTS.md`: target 3-6 KB.
- `.agent/INDEX.md`: target 2-4 KB; hard cap about 5 KB.
- `.agent/BRIEF.md` and `.agent/PLAN.md`: target 3-8 KB; hard cap about 10-12 KB.
- Frequently read workflow/current-state docs: hard cap 10-12 KB unless archived or generated exact-reference docs.
- `HANDOVER.md` may be larger when it is not auto-read, but keep headings searchable and split bulky sections into archive.

## Current-State Docs

`BRIEF` should orient the next agent in under a minute. Exclude raw logs, completed history, long investigations, and architecture explanations.

`PLAN` is for active work only. In this repo, `.agent/PLAN.md` points to the established root `PLANS.md`.

`BACKLOG` is for longer sequencing and parked candidates. Keep it decision-oriented; archive evidence-heavy material.

`HANDOVER` is for stable private context. In this repo, `.agent/HANDOVER.md` points to the established root `HANDOVER.md`.

## Archive Rules

Do not read every archive file by default. Open `.agent/archive/INDEX.md`, check status/read triggers, then read only the matching note.

After a deep dive, put exact facts in `PROJECT_FACTS`, validation implications in `CHECKS`, architecture implications in `ARCHITECTURE` or `HANDOVER`, active tasks in `PLAN`, long-horizon candidates in `BACKLOG`, and only a short pointer in `BRIEF` if it affects current work.
