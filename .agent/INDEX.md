# Enhanced Trading Doc Index

Use this map before loading large or historical docs. Active docs stay short; detailed older guidance is archived by topic.

## Core Docs

| Path | Purpose | Read trigger | Status |
|---|---|---|---|
| `AGENTS.md` | Repo-local commands, safety rules, deploy policy, and knowledge map | Every repo-changing task | active |
| `docs/PROJECT_FACTS.md` | Exact commands, paths, versions, dependencies, Git mode, shared libraries, deploy facts | Build/check/deploy/setup/path/dependency/shared-library question | active |
| `docs/CHECKS.md` | Validation command menu and evidence rules | Validation/testing/release/deploy question | active |
| `docs/REPO_MAP.md` | Major repo layout and generated path inventory | Navigation, broad edits, unfamiliar subsystem | active |
| `docs/ARCHITECTURE.md` | Stable architecture overview and subsystem boundaries | Architecture or subsystem-boundary question | active |
| `docs/CODE_QUALITY.md` | Concise code standards | Non-trivial implementation/refactor/review | active |
| `.agent/DOC_SYSTEM.md` | Doc upkeep, budgets, archive, and routing rules | Durable-doc, archive, or guidance-routing update | active |
| `INIT_AGENT_PROMPT.md` | Template re-sync prompt | Template sync or reinitialization only | active |

## Project State And Private Context

| Path | Purpose | Read trigger | Status |
|---|---|---|---|
| `.agent/BRIEF.md` | Current status, risks, and next best step | Resuming work, ambiguous task, large/risky task | active |
| `.agent/PLAN.md` | Bridge to active `PLANS.md` | Planned work, setup gaps, multi-step tasks | active |
| `PLANS.md` | Active plan and deferred work | Feature/runtime work or plan updates | active |
| `.agent/BACKLOG.md` | Long-horizon parked work | Choosing next chunk or revisiting deferred work | active |
| `.agent/HANDOVER.md` | Bridge to root `HANDOVER.md` | Architecture/history not suitable for public docs | active |
| `HANDOVER.md` | Stable private project context | Large feature work, ownership handoff, architecture tracing | active |
| `.agent/PUBLIC_RELEASE.md` | Private public-export checklist | Public export, package prep, public docs cleanup, version/changelog release prep | active |
| `.agent/SHARED_LIBRARIES.md` | Cross-repo shared-library workflow | Shatter Lib use/inspection/improvement or LazyLib/LunaLib dependency-reference work | active |
| `.agent/archive/INDEX.md` | Archive and deep-dive catalog | Historical/deep-dive evidence may matter | active |

## Public/User Docs

| Path | Purpose | Read trigger | Status |
|---|---|---|---|
| `README.md` | Player-facing install and usage summary | Updating user-facing behavior or release packaging | active |
| `CONFIG.md` | Stock JSON, item keys, market blacklists, Luna settings, debug hook notes | Config schema, settings, blacklists, or item-key behavior | active |
| `PACKAGING.md` | Release/build/package validation and Weapon Badges split note | Preparing releases or deployment | active |
| `CHANGELOG.md` | Release notes | Bumping version or preparing a release | active |

## Archive Triggers

Do not read every archive file by default. Open `.agent/archive/INDEX.md`, check status/read-when notes, then read only the matching deep dive.

- Starsector UI/classloader and row-layout pitfalls: `.agent/archive/deep-dives/starsector-ui.md`.
- Vanilla weapon hover tooltip bytecode: `.agent/archive/deep-dives/vanilla-weapon-tooltip-bytecode.md`.
- Trade planning, source modes, quote semantics, and transaction side effects: `.agent/archive/deep-dives/trade-and-sources.md`.
- Runtime and release validation procedures: `.agent/archive/deep-dives/runtime-validation.md`.
- Historical badge split context only: `.agent/archive/deep-dives/patched-badges.md`.

## Search Patterns

Use targeted `rg` for exact local output. Suggested terms: task term, `build`, `check`, `deploy`, `known-good`, `risk`, `compat`, `release`, `Shatter Lib`, `source mode`, `rollback`, `tooltip`, `LunaLib`.

Do not search sibling, parent, or other project directories unless the user explicitly authorizes that named project or the shared-library workflow authorizes the named library checkout.
