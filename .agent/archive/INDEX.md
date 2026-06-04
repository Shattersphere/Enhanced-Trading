# Enhanced Trading Archive Index

Archives preserve useful detail that should remain searchable but should not be startup context. Read current docs first: `.agent/BRIEF.md`, `HANDOVER.md`, `.agent/ARCHITECTURE_MAP.md`, and `PLANS.md`.

Open archive files selectively. Several files are historical snapshots from before the standalone `D:\Sean Mods\Weapon Badges` split and before ship trading was added; use their status and notes before treating them as guidance.

## Deep Dives

| Path | Category | Status | Read when | Key tags |
| --- | --- | --- | --- | --- |
| `.agent/archive/deep-dives/starsector-ui.md` | Starsector UI / classloader | active-reference | Changing stock-review layout, row sizing, `WimGui*`, buttons, scrolling, text fitting, helper extraction, or live-jar class validation. | `WimGui`, `buttonPressed`, `indent`, `a02e507`, `NoClassDefFoundError`, `validate-live-gui-classes` |
| `.agent/archive/deep-dives/vanilla-weapon-tooltip-bytecode.md` | Starsector UI / bytecode | active-reference | Attempting to match vanilla weapon hover tooltips, cargo-cell tooltip behavior, weapon row icons, or codex weapon tooltip layout. | `CargoDataGridView`, `CargoTooltipFactory`, `StandardTooltipV2`, `TooltipLocation.RIGHT`, `StockReviewWeaponIconPlugin`, `codex` |
| `.agent/archive/deep-dives/trade-and-sources.md` | Trade/source behavior | active-reference | Changing Local/Sector/Fixer sources, Fixer catalog inference, ship catalog prototypes, pending trades, quotes, tariffs, cargo mutation, transaction callbacks, or market blacklists. | `Sector Market`, `Fixer's Market`, `TheoreticalSaleIndex`, `ObservedStockIndex`, `RarityClassifier`, `TheoreticalShipSaleIndex`, `ObservedShipStockIndex`, `FactionAPI`, `StockPurchaseExecutor`, `StockReviewQuoteBook`, `W:`, `F:` |
| `.agent/archive/deep-dives/patched-badges.md` | Historical patched badge path | retired/historical | Understanding why badge ownership was removed from Enhanced Trading, or migrating old context into the standalone `D:\Sean Mods\Weapon Badges` repo. Do not use it to add badge code here. | `CargoStackView`, `starfarer_obf.jar`, `WeaponsProcurementBadgeHelper`, `wp.private.patchedBadgesEnabled`, `wp.counts.ready`, `javap`, `bytecode` |
| `.agent/archive/deep-dives/runtime-validation.md` | Runtime/release validation | active-reference with retired badge notes | Preparing manual in-game validation, rollback fault checks, release validation, or clean deploy troubleshooting. Badge validation sections are historical only. | `deploy-live-mod`, `queued deploy`, `rollback`, `wp.debug.failTradeStep` |

## History

| Path | Category | Status | Read when | Key tags |
| --- | --- | --- | --- | --- |
| `.agent/archive/history/2026-05-gui-framework-migration.md` | GUI migration history | historical | Investigating why the popup uses the current `WimGui*` custom-panel/list architecture or why a helper/validator exists. | `WimGui`, `ACG`, `StockReviewRenderer`, `NoClassDefFoundError`, `modal list` |
| `.agent/archive/history/2026-05-trade-source-remediation.md` | Trade/source remediation history | historical | Investigating completed review-agent tasks, source-mode evolution, rollback hardening, or trade execution boundaries. | `rollback`, `Sector Market`, `Fixer's Market`, `perItem`, `BUY_FROM_SUBMARKET`, `TradeMoney` |
| `.agent/archive/history/2026-05-product-and-validation-history.md` | Product/release history | historical | Investigating clean-vs-patched product boundary, release validation posture, or why badge approaches are avoided. | `clean popup`, `patched badges`, `CargoStackView`, `late over-icon`, `Sanity`, `deploy-live-mod` |
| `.agent/archive/history/2026-05-18-agent-takeover-handover.md` | Agent takeover snapshot summary | historical summary | Exact May 18 point-in-time context may matter. For ordinary takeover or broad architecture work, read current `HANDOVER.md`, `.agent/ARCHITECTURE_MAP.md`, `.agent/BRIEF.md`, and `PLANS.md` first. | `takeover`, `snapshot`, `Kotlin migration`, `stock review`, `Fixer`, `Sector Market`, `patched badges`, `public release` |
| `.agent/archive/history/snapshots/2026-05-18-agent-takeover-handover-full.md` | Full May 18 takeover snapshot | historical full snapshot/stale in places | Exact commit-era evidence is required after reading the summary. Do not use as current implementation guidance. | `takeover`, `full snapshot`, `b85e7a1`, `private badge`, `ship catalog debug` |

## Retired Plans

No retired plan files yet. Keep `PLANS.md` active-only; move old roadmap material here only when preserving it has clear value.
