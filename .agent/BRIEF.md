# Weapons Procurement Brief

Last updated: 2026-05-18

## Current State

Weapons Procurement is a Starsector `0.98a` mod for reviewing weapon and fighter LPC stock, planning buys/sells, and confirming trades from market dialogs or market-backed storage dialogs.

The primary product path is the clean `F8` popup. Cargo-cell badges now live in the standalone private `D:\Sean Mods\Weapon Badges` mod.

The source tree is fully migrated to Gradle/Kotlin. `build.ps1` remains the normal entry point and delegates to the Gradle wrapper; LazyLib is a required dependency because it supplies the Kotlin runtime in Starsector.

## Known-Good Source State

- Current branch: `main`
- Recent source baseline before this doc/export cleanup: `e9f6014` (`Make sanity workflow manual only`)
- Version in `mod_info.json`: `0.2.0`

## Commands

Docs-only:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

Runtime/source:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
git diff --check
```

## Deploy Target

- Live mod folder: `C:\Games\Starsector\mods\Weapons Procurement`
- Deploy command: `tools/deploy-live-mod.ps1`
- The deploy script clean-syncs repo-managed clean-package files, or stages and queues a minimized visible no-activate deploy if the live jar is locked.
- Shared deploy/status/state/zip/process helpers live in `tools/lib/Deploy.Common.ps1`; keep `deploy-live-mod.ps1` command-line behavior stable.
- Use `tools/deploy-live-mod.ps1 -Status` for queue/status/blocker/staging diagnostics, `-CheckOnly -RequireCurrent` for cheap source/live clean-package parity, and `-Status -CleanStaleStaging` for scoped stale staging cleanup.
- Docs-only changes normally do not need deploy.

## Current Risks

- Runtime rollback fault validation still needs in-game evidence.
- Rollback diagnostics now emit structured `WP_STOCK_REVIEW_ROLLBACK` records; use `tools/analyze-trade-rollback-diagnostics.ps1` after a forced-failure run to verify restored cargo counts and credits.
- Starsector classloading can keep stale jar/class state until restart.
- Luna settings, data/config files, graphics, and metadata matter; jar parity alone is not sufficient for data-heavy changes.
- Public release to `Shattersphere-Mods` must be curated. Do not mirror this private repo because it contains agent docs and local/private references.

## Next Best Step

For code/runtime work, inspect `PLANS.md` and the relevant archive deep dive through `.agent/archive/INDEX.md`. For public release/export work, start with `.agent/PUBLIC_RELEASE.md`. For docs-only work, use the docs-only checks and avoid deployment.
