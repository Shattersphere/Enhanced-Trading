# Enhanced Trading Agent Guide

Repo-local instructions for Enhanced Trading. Current task instructions and nested guidance still win when narrower.

## Project Snapshot

- Repo: `D:\Sean Code Projects\Starsector Projects\Enhanced Trading`
- Game: Starsector `0.98a`
- Mod id: `enhanced_trading`
- Runtime jar: `jars/enhanced-trading.jar`
- Main plugin: `weaponsprocurement.plugins.WeaponsProcurementModPlugin`
- Required dependencies: LazyLib and LunaLib
- Exact commands, paths, dependencies, Git mode, and compatibility notes live in `docs/PROJECT_FACTS.md`.
- Validation choices live in `docs/CHECKS.md`.

Start repo-changing work with:

```powershell
git status --short --branch
```

## Standard Checks

Use the smallest relevant check from `docs/CHECKS.md`.

Docs/template checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1 -IncludePrivateDocs
python scripts/check-template-state.py --initialized
git diff --check
```

Runtime/source checks:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1 -CheckOnly -RequireCurrent
git diff --check
```

## Deploy Policy

- Use `tools/deploy-live-mod.ps1`; do not hand-copy runtime files unless explicitly asked.
- Use `tools/deploy-live-mod.ps1 -Status` for queue/status/blocker/staging diagnostics.
- Use `tools/deploy-live-mod.ps1 -CheckOnly -RequireCurrent` for source/live clean-package parity without deploying.
- Use `tools/deploy-live-mod.ps1 -Status -CleanStaleStaging` only for scoped cleanup of inactive staging directories.
- Shared deploy helpers live in `tools/lib/Deploy.Common.ps1`; preserve deploy script CLI/output compatibility.
- Deploy runtime changes that affect jar code, `mod_info.json`, `data/`, `graphics/`, Luna settings, generated assets, or package metadata.
- Do not deploy docs-only changes unless release packaging explicitly requires mirrored docs.
- If Starsector or a locked jar blocks deployment, do not kill the process; the deploy script can stage and queue the update.
- Validate live artifacts with `tools/validate-live-gui-classes.ps1` after runtime deploys.

## Public Release Policy

- Private repo work happens here; public output for `Shattersphere-Mods` must be curated, not mirrored.
- Read `.agent/PUBLIC_RELEASE.md` before public export, public repo sync, package prep, or release-facing docs changes.
- Public output must exclude `AGENTS.md`, `.agent/`, `HANDOVER.md`, `PLANS.md`, private archives, local paths, deploy queues, and standalone badge/bytecode material unless explicitly approved.
- `CHANGELOG.md` is public release history, not an agent work diary.

## Durable Docs

- `.agent/INDEX.md`: doc map and read triggers.
- `docs/PROJECT_FACTS.md`: exact source-of-truth facts.
- `docs/CHECKS.md`: validation menu and evidence rules.
- `docs/REPO_MAP.md`: generated orientation map.
- `.agent/BRIEF.md`: current handoff summary.
- `.agent/PLAN.md`: pointer to active work in `PLANS.md`.
- `.agent/HANDOVER.md`: pointer to private handoff in `HANDOVER.md`.
- `.agent/archive/INDEX.md`: archive map. Read it before opening deep dives.

Do not read every archive file at session start. Search first, then open only the relevant deep dive.

## Project Knowledge Map

- Starsector UI/classloader and row-layout pitfalls: `.agent/archive/deep-dives/starsector-ui.md`.
  Read before campaign UI helper extraction, stock-review layout work, button/poller changes, or row-width/indent fixes.
- Vanilla weapon hover tooltip bytecode: `.agent/archive/deep-dives/vanilla-weapon-tooltip-bytecode.md`.
  Read before trying to match or reuse vanilla cargo/refit weapon hover tooltips.
- Trade planning, source modes, quote semantics, and transaction side effects: `.agent/archive/deep-dives/trade-and-sources.md`.
  Read before changing Local/Sector/Fixer behavior, pending trades, tariffs, source allocation, or cargo mutation.
- Runtime and release validation procedures: `.agent/archive/deep-dives/runtime-validation.md`.
  Read before release validation, manual in-game testing, rollback fault tests, or live deploy troubleshooting.
- Public release/export boundary: `.agent/PUBLIC_RELEASE.md`.
  Read before syncing to `Shattersphere-Mods`, preparing a public package, or removing private/badge-only traces from release output.

## Hard Constraints

- Clean `F8` popup is the public/default product.
- Cargo-cell badge ownership belongs in the standalone `D:\Sean Mods\Weapon Badges` mod.
- Clean builds use `src/main/kotlin` plus legacy public Java under `src/weaponsprocurement`.
- Do not ship or commit a prepatched `starfarer_obf.jar`.
- Do not reintroduce patched-core badge helpers, count bridges, generated badge sprites, or `CargoStackView` patching tools.
- Do not reintroduce visible seller-detail/source-specific local buy rows without a design pass.
- Treat compile success and jar parity as insufficient proof for runtime UI, LunaLib, or campaign behavior.
- Keep dangerous validation hooks disabled by default.

## Shared Libraries

Before copying/reimplementing reusable Starsector helpers or changing Shatter Lib, MagicLib, LazyLib, or LunaLib integration, read `.agent/SHARED_LIBRARIES.md` and `docs/PROJECT_FACTS.md`.

Shatter Lib may be inspected or edited only when those docs authorize the named shared-library workflow. If the Shatter Lib edit gate is not satisfied, write a task packet instead of changing Shatter Lib directly. MagicLib, LazyLib, and LunaLib are read-only dependency libraries unless the user gives separate explicit instructions.
