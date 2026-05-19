# Enhanced Trading Active Plan

This file is active work only. Completed migrations, old public-release cleanup notes, and historical investigations belong in `.agent/archive/`.

## Current Baseline

- `Trade: Items` is the stable product baseline for weapon and fighter LPC stock review, planning, review, and confirmation.
- `Trade: Ships` is a local-only first implementation. Preserve the user-confirmed 4-column by 5-row ship-grid layout unless a later design pass explicitly changes it.
- Source modes for item trading are intentionally different: Local mutates the current market, Sector Market drains real remote cargo, and Fixer's Market is virtual catalog stock.
- Ship trading is not wired into Sector Market or Fixer's Market.
- Cargo-cell weapon/LPC badges live in the standalone private `D:\Sean Mods\Weapon Badges` repo. Do not reintroduce badge helpers, generated badge sprites, count bridges, or `CargoStackView` patching tools here.
- The worktree may contain broad in-progress UI, tooltip, debug, data, graphics, and jar changes. Treat it as functional but not release-clean until the current diff is reviewed and validated as a batch.

## Active Work

### 1. Stabilize Ship And Tooltip UI Polish

Status: active

Scope:

- Ship grid density, paging, hull-class text filtering, and filter modal layout.
- Ship, weapon, and wing tooltip sizing, truncation, stat sourcing, and debug stress records.
- Debug UI visibility behind the LunaLib debug toggle.

Constraints:

- Keep ship trading local-only.
- Do not change item trade behavior while polishing ship UI.
- Do not use obfuscated vanilla UI classes directly; use public APIs and custom-panel approximations.
- Tooltips should stay within the screen and avoid premature mid-line truncation.

Done when:

- The ship grid still uses the full available trade area cleanly.
- Weapon, wing, and ship tooltips have in-game visual acceptance at common UI scales.
- Debug weapon, wing, and ship stress records exercise worst-case content without clipping controls.

### 2. Runtime Rollback Fault Validation

Status: active, manual-runtime evidence needed

Diagnostic support emits structured `WP_STOCK_REVIEW_ROLLBACK` log records and includes `tools/analyze-trade-rollback-diagnostics.ps1` for pass/fail summarization.

Manual validation matrix:

- local legal buy;
- local sell;
- local black-market buy/sell if available;
- Sector Market buy;
- Fixer's Market buy;
- mixed sell-then-buy plan.

Run one forced-failure step at a time with `wp.debug.failTradeStep` set to:

- `after-source-removal`
- `after-player-cargo-remove`
- `after-player-cargo-add`
- `after-target-cargo-add`
- `after-credit-mutation`

Then analyze logs:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\analyze-trade-rollback-diagnostics.ps1 -ExpectFailureStep after-source-removal,after-player-cargo-remove,after-player-cargo-add,after-target-cargo-add,after-credit-mutation -RequirePass
```

Reset the setting to unset, empty, or `none` before normal play or packaging.

### 3. Curated Public Export

Status: deferred until explicitly requested

Use `.agent/PUBLIC_RELEASE.md`. Do not mirror this private repo to a public organization.

Open release questions:

- Whether the public repo should commit `jars/enhanced-trading.jar` or build jars only for packages.
- Whether the current ship-trading work is release-ready or should remain private until more runtime polish is complete.

### 4. Remote Ship Trading

Status: deferred new feature

Do not add Sector Market or Fixer's Market ship trading as part of UI polish. Remote ship trading needs a separate design for ship identity, source draining, pricing, virtual availability, and failure handling.

## Retired Or Completed

- Kotlin/Gradle migration: complete. Use `tools/validate-kotlin-migration.ps1` as the guard.
- UI primitive and stock-review package ownership split: complete. See `HANDOVER.md` and `.agent/ARCHITECTURE_MAP.md`.
- Badge split: active ownership is now standalone in `D:\Sean Mods\Weapon Badges`. Historical context is archived in `.agent/archive/deep-dives/patched-badges.md`, but it is not active implementation guidance for this repo.
- Source-mode remediation, rollback hardening, and Fixer catalog evolution: see `.agent/archive/history/2026-05-trade-source-remediation.md` and `.agent/archive/deep-dives/trade-and-sources.md`.

## Validation Commands

Docs-only:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1 -IncludePrivateDocs
git diff --check
```

Runtime/source:

```powershell
$env:STARSECTOR_DIRECTORY = "X:\Path\To\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-jar-classes.ps1 -JarPath .\jars\enhanced-trading.jar -Label Repo
git diff --check
```
