# Enhanced Trading Active Plan

This file is active work only. Completed migrations, old public-release cleanup notes, and historical investigations belong in `.agent/archive/`.

## Current Baseline

- `Trade: Items` is the stable product baseline for weapon and fighter LPC stock review, planning, review, and confirmation.
- `Trade: Ships` is a local-only first implementation. Preserve the user-confirmed 4-column by 5-row ship-grid layout unless a later design pass explicitly changes it.
- Source modes for item trading are intentionally different: Local mutates the current market, Sector Market drains real remote cargo, and Fixer's Market is virtual catalog stock.
- Ship trading is not wired into Sector Market or Fixer's Market.
- Cargo-cell weapon/LPC badges live in the standalone private `D:\Sean Mods\Weapon Badges` repo. Do not reintroduce badge helpers, generated badge sprites, count bridges, or `CargoStackView` patching tools here.
- Template-synced project facts and checks now live in `docs/PROJECT_FACTS.md` and `docs/CHECKS.md`. Use those as the source of truth for commands, paths, dependencies, Git mode, and shared-library authorization.
- Live deploy/runtime proof is currently blocked by the installed Shatter Lib jar at `C:\Games\Starsector\mods\Shatter Lib\jars\shatter-lib.jar`, which is missing `ShatterItemTooltipContext.class` and `ShatterTooltipContextLine.class`. Build with the Shatter Lib checkout override for source/package proof only; do not claim live parity until the installed dependency is updated and deploy parity passes.
- Recent pushed modernization baseline: `7a3d42c` extracts ship tooltip stat-row derivation into `StockReviewShipTooltipRows`, leaving `StockReviewShipTooltip` focused on panel layout, wrapping, loadout text, and height sizing. Earlier source commits split tooltip owners, moved ship hull filtering into its filter owner, sourced fighter LPC Advanced Info stats, and added pure logic contract validation. Source/static checks passed for that work, but in-game tooltip and ship UI acceptance were not run.
- Generic template sync was rechecked against the current uncommitted `D:\Sean Code Projects\General Projects\Generic Template Repo` worktree on 2026-06-07. Template hygiene passed; Starsector-specific commands, compatibility surfaces, deploy policy, shared-library gates, and public/private boundaries were preserved.

## Active Work

### 1. Stabilize Ship And Tooltip UI Polish

Status: active

Scope:

- Ship grid density, paging, hull-class text filtering, and filter modal layout.
- Ship, weapon, and wing tooltip sizing, truncation, stat sourcing, and debug stress records.
- Debug UI visibility behind the LunaLib debug toggle.
- Source cleanup around tooltip owners only where it reduces real maintenance cost; pause before another source-only tooltip split unless a clear maintenance target remains.

Constraints:

- Keep ship trading local-only.
- Do not change item trade behavior while polishing ship UI.
- Do not use obfuscated vanilla UI classes directly; use public APIs and custom-panel approximations.
- Tooltips should stay within the screen and avoid premature mid-line truncation.
- Preserve Shatter Lib `ShatterWeaponTooltip`/`ShatterWingTooltip` delegation for normal records; custom tooltip code is primarily for debug/stress records and repo-owned context/layout work.

Done when:

- The ship grid still uses the full available trade area cleanly.
- Weapon, wing, and ship tooltips have in-game visual acceptance at common UI scales.
- Debug weapon, wing, and ship stress records exercise worst-case content without clipping controls.

Suggested next bounded step:

- Resolve the stale installed Shatter Lib dependency, then deploy/parity-check and visually smoke weapon, wing, and ship tooltips at common UI scales. Runtime visual proof remains required before claiming tooltip behavior acceptance.

### 2. Runtime Rollback Fault Validation

Status: active, blocked by stale installed Shatter Lib runtime dependency

Diagnostic support emits structured `WP_STOCK_REVIEW_ROLLBACK` log records and includes `tools/analyze-trade-rollback-diagnostics.ps1` for pass/fail summarization.

Before running the matrix, resolve the current deploy blocker reported by `tools/deploy-live-mod.ps1 -Status -StarsectorDir 'C:\Games\Starsector'`: the installed Shatter Lib jar must contain `com/shattersphere/shatterlib/starsector/ui/tooltip/ShatterItemTooltipContext.class` and `com/shattersphere/shatterlib/starsector/ui/tooltip/ShatterTooltipContextLine.class`.

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

- Generic template sync: rechecked 2026-06-07. Current template governance changes were already mostly present here; imported only safe doc-routing cleanup and left Starsector deploy, validation, compatibility, shared-library, Git, and public/private rules intact.
- Kotlin/Gradle migration: complete. Use `tools/validate-kotlin-migration.ps1` as the guard.
- UI primitive and stock-review package ownership split: complete. See `HANDOVER.md` and `.agent/ARCHITECTURE_MAP.md`.
- Badge split: active ownership is now standalone in `D:\Sean Mods\Weapon Badges`. Historical context is archived in `.agent/archive/deep-dives/patched-badges.md`, but it is not active implementation guidance for this repo.
- Source-mode remediation, rollback hardening, and Fixer catalog evolution: see `.agent/archive/history/2026-05-trade-source-remediation.md` and `.agent/archive/deep-dives/trade-and-sources.md`.

## Validation Routing

Use `docs/CHECKS.md` for exact validation commands and evidence rules. For docs-only edits, use the private/public doc-link checks as appropriate plus `git diff --check`; for runtime/source work, combine the build with the focused GUI, Kotlin migration, deploy parity, live-class, or rollback diagnostic check that covers the touched surface.
