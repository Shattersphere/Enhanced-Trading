# Public Release Checklist

Status: private
Scope: Curating the private Enhanced Trading repo into a public GitHub repo/package for `https://github.com/Shattersphere-Mods`
Last updated: 2026-05-18

Never publish this file. It describes private-to-public export rules for keeping the public mod focused on the procurement GUI and free of private agent/archive material.

## Release Target

- Public organization: `Shattersphere-Mods`
- Intended public product: clean Enhanced Trading GUI only.
- Private source of truth: `D:\Sean Code Projects\Starsector Projects\Enhanced Trading`
- Public release output must not be a blind mirror of this private repo.

## Current Public-Release Position

Badge ownership now lives in the standalone private `D:\Sean Mods\Weapon Badges` repo. Public Enhanced Trading output must still exclude any badge helper, count bridge, generated badge sprite, or `CargoStackView` patching material if those files are ever accidentally reintroduced.

Before public export, use:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\export-public.ps1
```

The script writes a curated public tree to `build/public-export` by default and runs the leak scan below.

Do not push this private repo wholesale to the public organization.

## Public Include Surface

Public output should include only files needed for users/contributors to build and use the clean procurement GUI:

- `src/`;
- Gradle wrapper and Kotlin build files required to compile the public source tree;
- `data/campaign/rules.csv`;
- public-safe `data/config/LunaSettings.csv`;
- `data/config/enhanced_trading_market_blacklist.json`;
- `data/config/enhanced_trading_stock.json`;
- `jars/enhanced-trading.jar` if the public repo ships the built jar;
- `mod_info.json`;
- `README.md`;
- `CONFIG.md`;
- `CHANGELOG.md`;
- user-facing build/package docs that do not mention private paths, agents, archives, or badge/bytecode systems;
- public-safe `.github/workflows/` if it does not require private paths or badge-only validators;
- normal build scripts required for public contributors.
- public-safe diagnostic analyzers for clean GUI runtime troubleshooting.

## Public Exclude Surface

Exclude private and agent material:

- `AGENTS.md`;
- `.agent/`;
- `HANDOVER.md`;
- `PLANS.md`;
- `LESSONS.md`;
- archive/deep-dive/history docs;
- deploy queues, local logs, backups, build caches, and machine-specific files.

Cargo-cell badges now live in `D:\Sean Mods\Weapon Badges`. Public Enhanced Trading exports should contain no badge helpers, count bridges, generated badge sprites, or `CargoStackView` patching tools.

## Required Private-To-Public Transformations

Resolved:

- Badge ownership moved to the standalone private `Weapon Badges` mod.
- Public docs describe only the clean GUI product.
- `tools/export-public.ps1` curates public output and runs a leak scan.

Still open:

- Build the exported public tree and confirm the resulting jar contains no badge classes.
- Run `tools/validate-kotlin-migration.ps1` after public export to confirm the clean/private boundary.
- Decide whether public releases should commit/source-control the built jar or build it only for release packages.

## Leak Scan Terms

Before public push/package, scan the public output for:

```text
AGENTS.md
.agent/
.agent\
HANDOVER
PLANS
LESSONS
Codex
D:\Sean Mods
C:\Games\Starsector
WeaponsProcurementBadgeHelper
WeaponsProcurementBadgeConfig
WeaponsProcurementCountUpdater
```

Some terms such as `agent` may appear inside ordinary words; review matches rather than treating every hit as a failure.

## Suggested Public Validation

Public-output validation should eventually include:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
git diff --check
```

Badge/patcher validation belongs in the standalone private `D:\Sean Mods\Weapon Badges` repo, not in Enhanced Trading.

## Version And Changelog Rules

- Do not bump version for private archive/doc restructuring.
- Bump patch version for public release cleanup, packaging validation fixes, or public-safe config corrections.
- Bump minor version for new user-facing procurement features.
- Keep `mod_info.json` and `CHANGELOG.md` aligned when bumping.
- Public changelog entries must not mention Codex, agents, private docs, local paths, or private badge/bytecode experiments.

## Open Work Before First Public Release

- Build the exported public tree and validate it independently.
- Add a public package command if the public repo should ship `jars/enhanced-trading.jar`.
- Decide whether public repo ships the built jar or source-only plus build instructions.
