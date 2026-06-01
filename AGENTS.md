# Enhanced Trading Agent Guide

Repo-local instructions for Enhanced Trading. These override the workspace-level `D:\Sean Mods\AGENTS.md` where more specific.

## Project Root

- Repo: `D:\Sean Mods\Enhanced Trading`
- Game: Starsector `0.98a`
- Live clean-package target: `C:\Games\Starsector\mods\Enhanced Trading`
- Runtime jar: `jars/enhanced-trading.jar`
- Main plugin: `weaponsprocurement.plugins.WeaponsProcurementModPlugin`
- Required dependencies: LazyLib and LunaLib

## Standard Commands

Start all repo-changing work with:

```powershell
git status --short --branch
```

Docs-only checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1 -IncludePrivateDocs
git diff --check
```

Runtime/source validation:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1 -IncludePrivateDocs
git diff --check
```

Public export check:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\export-public.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
```

## Deploy Policy

- Use `tools/deploy-live-mod.ps1`; do not hand-copy runtime files unless explicitly asked.
- Use `tools/deploy-live-mod.ps1 -Status` for a no-build deploy queue/status/blocker/staging report.
- Use `tools/deploy-live-mod.ps1 -CheckOnly -RequireCurrent` when you need cheap source/live clean-package parity without deploying.
- Use `tools/deploy-live-mod.ps1 -Status -CleanStaleStaging` only for scoped cleanup of inactive deploy staging directories.
- Shared deploy helpers live in `tools/lib/Deploy.Common.ps1`; preserve deploy script CLI/output compatibility and centralize reusable status, queue, zip, state, process, and no-activate worker behavior there.
- Clean deploys reject jars containing stale badge classes. Badge ownership moved to the standalone `D:\Sean Mods\Weapon Badges` mod.
- Deploy runtime changes that affect jar code, `mod_info.json`, `data/`, `graphics/`, Luna settings, generated assets, or package metadata.
- Do not deploy docs-only changes unless the user asks or release packaging requires mirrored docs.
- If deployment is blocked by a running Starsector process or locked artifact, do not kill the process. The deploy script stages the built files and queues a minimized visible no-activate waiting deploy.
- Validate the live artifact with `tools/validate-live-gui-classes.ps1` after runtime deploys.

## Public Release Policy

- Private repo work happens here; public output for `Shattersphere-Mods` must be curated, not mirrored.
- Read `.agent/PUBLIC_RELEASE.md` before any public export, public repo sync, package prep, or release-facing docs change.
- Public output must exclude `AGENTS.md`, `.agent/`, `HANDOVER.md`, `PLANS.md`, private archives, local paths, deploy queues, and standalone badge/bytecode material unless explicitly approved.
- Public changelog entries must stay user-facing and must not mention agents, private docs, local paths, or private experiments.

## Durable Docs

- `.agent/INDEX.md`: map of active docs and archives. Read this before loading large project docs.
- `.agent/BRIEF.md`: compact current state and next-step handoff.
- `.agent/PUBLIC_RELEASE.md`: private checklist for curating public repo/package output. Never publish it.
- `HANDOVER.md`: stable architecture, commands, and current validation constraints.
- `PLANS.md`: active plan only.
- `.agent/archive/INDEX.md`: archive map. Read this before opening deep dives.

Do not read every archive file at session start. Search first, then open only the relevant deep dive.

## Project Knowledge Map

- Starsector UI/classloader and row-layout pitfalls: `.agent/archive/deep-dives/starsector-ui.md`.
  Read before campaign UI helper extraction, stock-review layout work, button/poller changes, or row-width/indent fixes.
- Vanilla weapon hover tooltip bytecode: `.agent/archive/deep-dives/vanilla-weapon-tooltip-bytecode.md`.
  Read before trying to match or reuse vanilla cargo/refit weapon hover tooltips.
- Trade planning, source modes, quote semantics, and transaction side effects: `.agent/archive/deep-dives/trade-and-sources.md`.
  Read before changing Local/Sector/Fixer behavior, pending trades, tariffs, source allocation, or cargo mutation.
- Optional cargo-cell badges now live in the standalone `D:\Sean Mods\Weapon Badges` repo. Do not add badge helper, count bridge, or bytecode patcher code back to Enhanced Trading.
- Runtime and release validation procedures: `.agent/archive/deep-dives/runtime-validation.md`.
  Read before release validation, manual in-game testing, rollback fault tests, or live deploy troubleshooting.
- Public release/export boundary: `.agent/PUBLIC_RELEASE.md`.
  Read before syncing to `Shattersphere-Mods`, preparing a public package, or removing private/badge-only traces from release output.

## Hard Constraints

- Clean `F8` popup is the public/default product. Cargo-cell badge ownership belongs in the standalone `D:\Sean Mods\Weapon Badges` mod.
- Clean builds use `src/main/kotlin` plus legacy public Java under `src/weaponsprocurement`.
- Do not ship or commit a prepatched `starfarer_obf.jar`.
- Do not reintroduce patched-core badge helpers, count bridges, generated badge sprites, or `CargoStackView` patching tools.
- Do not reintroduce visible seller-detail/source-specific local buy rows without a design pass.
- Treat compile success and jar parity as insufficient proof for runtime UI, LunaLib, or campaign behavior.
- Keep dangerous validation hooks disabled by default.


## Shared Libraries

Before copying/reimplementing reusable Starsector helpers or changing Shatter Lib, MagicLib, LazyLib, or LunaLib integration, read `.agent/SHARED_LIBRARIES.md`. Shatter Lib may be edited only for generic, consumer-neutral improvements; MagicLib, LazyLib, and LunaLib are read-only dependency libraries unless the user gives separate explicit instructions.
