# Enhanced Trading Doc Index

Use this map before loading large or historical docs. The active docs stay short; detailed older guidance is archived by topic.

| Path | Category | Status | Purpose | Read when |
| --- | --- | --- | --- | --- |
| `AGENTS.md` | Instructions | active | Repo-local commands, safety rules, deploy policy, and knowledge map. | Always after `git status` for repo-changing work. |
| `.agent/BRIEF.md` | Handoff | active | Compact current state, commands, deploy target, risks, and next step. | The task depends on current project state. |
| `.agent/PUBLIC_RELEASE.md` | Private release checklist | active | Curated public repo/package export rules for `Shattersphere-Mods`. Never publish. | Any public export, package prep, public docs cleanup, version/changelog release prep, or badge-removal sync work. |
| `README.md` | User docs | active | Player-facing install and usage summary. | Updating user-facing behavior or release packaging. |
| `CONFIG.md` | User docs | active | Stock JSON, item keys, market blacklists, Luna settings, and debug hook notes. | Changing config schema, settings, blacklists, or item-key behavior. |
| `PACKAGING.md` | Release docs | active | Release/build/package validation and the Weapon Badges split note. | Preparing releases or deployment. |
| `CHANGELOG.md` | Release docs | active | Release notes. | Bumping version or preparing a release. |
| `HANDOVER.md` | Stable memory | active | Current architecture, stable decisions, validation commands, and runtime caveats. | Changing architecture, entry points, source modes, trade flow, or validation commands. |
| `.agent/ARCHITECTURE_MAP.md` | Architecture map | active | Diagrams and package maps for open flow, item trading, ship trading, UI layering, and build/deploy surfaces. | Onboarding, planning larger changes, or tracing ownership between stock, trade, UI, and deploy code. |
| `PLANS.md` | Active plan | active | Short active plan, deferred items, and retired-plan pointers. Completed migration/release history should not live here. | Large, risky, planned, or multi-session work. |
| `.agent/archive/INDEX.md` | Archive map | active | Searchable index of deep dives, history, and retired plans, with status notes for stale historical guidance. | A task may depend on historical/deep-dive knowledge. |

Do not read every archive file by default. Open `.agent/archive/INDEX.md`, check the status/read-when notes, then read only the matching deep dive. Historical badge docs are retained for context only; active badge work belongs in the standalone `D:\Sean Mods\Weapon Badges` repo.


## Shared Libraries

- `.agent/SHARED_LIBRARIES.md`: read before using, inspecting, or improving Shatter Lib, or before dependency-reference work involving MagicLib, LazyLib, or LunaLib.
