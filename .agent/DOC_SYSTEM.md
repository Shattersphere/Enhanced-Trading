# Agent Documentation System

The rule is not "avoid docs"; the rule is "put durable facts in the right document and retrieve them by trigger." Private `.agent/` files are not memory and should not be published by default.

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

- Global guidance: 10-12 KB.
- Root `AGENTS.md`: target 3-6 KB.
- Nested `AGENTS.md`/overrides: target 1-3 KB.
- `.agent/INDEX.md`: target 2-4 KB; hard cap about 5 KB.
- `.agent/BRIEF.md` and `.agent/PLAN.md`: target 3-8 KB; hard cap about 10-12 KB.
- Frequently read workflow/current-state docs, including `*PLAN*`, `*STATUS*`, `*ROADMAP*`, and `*MODERNIZATION*`: hard cap 10-12 KB unless archived or generated exact-reference docs.
- `HANDOVER.md` may be larger when it is not auto-read, but keep headings searchable and split bulky sections into archive.

Compaction should preserve decisions, current facts, risks, blockers, and next actions. Do not delete useful history merely to shorten a file; archive it and leave a pointer.

## Current-State Docs

`BRIEF` should orient the next agent in under a minute. Exclude raw logs, completed history, long investigations, and architecture explanations.

`PLAN` is for active work only. In this repo, `.agent/PLAN.md` points to the established root `PLANS.md`.

`BACKLOG` is for longer sequencing and parked candidates. Keep it decision-oriented; archive evidence-heavy material.

`HANDOVER` is for stable private context. In this repo, `.agent/HANDOVER.md` points to the established root `HANDOVER.md`.

## Archive Rules

Do not read every archive file by default. Open `.agent/archive/INDEX.md`, check status/read triggers, then read only the matching note.

Suggested folders:

```text
.agent/archive/deep-dives/      # investigations, decisions, compatibility analysis
.agent/archive/history/         # project history and backfill notes
.agent/archive/retired-plans/   # abandoned or completed plans worth preserving
.agent/archive/incidents/       # failure analysis and debug records, if needed
```

Create folders only when needed. Archive notes should be self-contained, concise, evidence-linked, explicit about current vs historical status, and indexed. A bad note is a transcript, command dump, duplicate facts page, or stale "latest status" page.

After a deep dive, put exact facts in `PROJECT_FACTS`, validation implications in `CHECKS`, architecture implications in `ARCHITECTURE` or `HANDOVER`, active tasks in `PLAN`, long-horizon candidates in `BACKLOG`, and only a short pointer in `BRIEF` if it affects current work.

Index significant notes in `.agent/archive/INDEX.md`:

```markdown
| Date | Path | Status | Tags | Summary | Read trigger |
|---|---|---|---|---|---|
| YYYY-MM-DD | `deep-dives/topic.md` | historical/evidence | `tag` | One-line value. | Read when changing/investigating <trigger>. |
```

Update `.agent/INDEX.md` only when a new document category, topic doc, or important read trigger changes.

## Compaction Triggers

Compact when `BRIEF` or `PLAN` exceeds 10-12 KB, `PLAN` contains completed work, a frequently read active doc exceeds 10-12 KB, `HANDOVER` becomes hard to search by heading, archive notes are not indexed, a fact is duplicated across three or more docs, or stale "current" statements contradict source files.
