# Codebase Quality Modernization Plan

Target state: Enhanced Trading stays easy and safe to change because subsystem boundaries, compatibility surfaces, invariants, validation limits, and runtime risks are explicit. This is not a mandate for endless polishing, public release work, behavior redesign, remote ship trading, save/schema churn, or speculative abstraction.

## Repository Inventory

Build/package flow: `build.ps1` delegates to Gradle/Kotlin JVM `2.1.20`, Java 17, `buildMod`, and writes `jars/enhanced-trading.jar`. `mod_info.json` loads that jar and declares LazyLib, LunaLib, and Shatter Lib.

Source is Kotlin under `src/main/kotlin`. Main packages are `weaponsprocurement.config`, `lifecycle`, `plugins`, `stock.fixer`, `stock.inventory`, `stock.item`, `stock.market`, `trade.execution`, `trade.plan`, `trade.quote`, shared `ui/WimGui*`, and `ui.stockreview.*`. The Starsector rule command is `com/fs/starfarer/api/impl/campaign/rulecmd/WP_OpenDialog`.

Runtime entry points: `WeaponsProcurementModPlugin`, transient `StockReviewHotkeyScript`, transient `WeaponsProcurementFixerCatalogUpdater`, and `data/campaign/rules.csv`. Config/data surfaces are `data/config/LunaSettings.csv`, `enhanced_trading_stock.json`, `enhanced_trading_market_blacklist.json`, and `graphics/ui/wp_debug_empty_item.png`. No repo-defined hullmod, ability, weapon, ship, variant, wing, faction, market, or common-storage file is currently evidenced.

## Compatibility Map

Do not change these without explicit approval and matching validation:

- Mod identity and load path: `enhanced_trading`, `jars/enhanced-trading.jar`, `weaponsprocurement.plugins.WeaponsProcurementModPlugin`.
- Rule/dialog path: `WP_OpenDialog`, `wp_marketOpenWeaponProcurement`, `wp_openWeaponProcurement`.
- Item keys: `W:<weaponId>` and `F:<wingId>` from `StockItemType`. Raw ids are compatibility inputs only.
- Luna keys: `wp_trade_hotkey`, `wp_enable_dialog_option`, `wp_enable_sector_market`, `wp_enable_fixers_market`, `wp_enable_fixers_market_tag_inference`, price multipliers, desired counts, and `wp_enable_debug_ui`.
- JSON paths/schema: `data/config/enhanced_trading_stock.json` fields `display`, `sources`, `desiredDefaults`, `perItem`, legacy `perWeapon`; blacklist keys `BANNED_FROM_SECTOR_MARKET`, `BANNED_FROM_FIXERS_MARKET`.
- Save/custom data: `weaponsProcurement.fixerObservedCatalog.v1` in `FixerMarketObservedCatalog`, encoded as simple string maps.
- Diagnostics: `wp.debug.failTradeStep`, `wp.debug.shipCatalog`, `wp.debug.shipCatalogView`, `WP_STOCK_REVIEW_ROLLBACK`, `WP_SHIP_CATALOG_DIAG`.
- Source semantics: Starsector `SUBMARKET_OPEN`, `GENERIC_MILITARY`, `SUBMARKET_BLACK`, `SUBMARKET_STORAGE`, `LOCAL_RESOURCES`, plus virtual `wp_fixers_market`.
- Reflection/classloader-sensitive names: rule command class path and jar/live class validator lists.

## Target Architecture

Public compatibility layer: shipped ids, config keys, data paths, item-key parsing, and plugin/rule paths stay explicit and documented. Internal refactors must not rename or reinterpret them.

Settings/config: `WeaponsProcurementConfig` is the Luna/System-property bridge. `StockReviewConfig` and `WeaponMarketBlacklist` own JSON loading, defaults, and legacy compatibility.

Campaign lifecycle: `WeaponsProcurementModPlugin` registers transient scripts only. `StockReviewHotkeyScript` owns market-backed opening and close/reopen. `WeaponsProcurementFixerCatalogUpdater` owns observed catalog updates and diagnostics.

Stock/source/trade: `MarketStockService`, `GlobalWeaponMarketService`, `StockReviewQuoteBook`, `StockPurchaseService`, and `StockPurchaseExecutor` must keep Local, Sector Market, and Fixer's Market semantics distinct. Transaction callbacks are post-commit side effects.

GUI: `WimGui*` owns Starsector custom-panel quirks, button polling, scroll, modal input, text fitting, and tooltip caps. `StockReviewPanelPlugin` stays orchestration-only; rows/renderers/controllers own presentation details. Preserve event-gated button polling and sibling-safe anchoring.

Ships/fighters: ship trading remains local-only exact-member trading in `ui.stockreview.ships`. Fighter LPCs are item `WING` records with `F:` keys, not combat carrier behavior. Remote ship trading, refit, and combat HUD work require new design gates.

Persistence: keep Fixer catalog state simple, sanitized, and migration-aware. No save-key rename without approval.

Validation/tooling/docs: validators must reflect the current architecture. Maintainer docs should stay compact and link deep dives rather than duplicating history.

## Risk Register

| Area | Evidence | Impact | Mitigation | Validation | Status |
|---|---|---|---|---|---|
| Validator/CI drift | stale jar/class checks | false failures or ignored checks | keep validator lists current | jar, Kotlin, GUI, compatibility validators | active |
| GUI/classloader | `WimGui*`, UI deep dive | crashes, clipping, stale classes | bounded UI edits | GUI validator, jar/live class, in-game | active |
| Trade rollback | `StockPurchaseExecutor`, analyzer | cargo/credit corruption | forced-failure matrix | runtime analyzer | active |
| Source semantics | Local/Sector/Fixer services | wrong cargo drain/pricing | preserve contracts | build plus manual trade matrix | active |
| Fixer persistence | save key v1 | save compatibility loss | migration-before-change | static and runtime save proof | active |
| Ship trading | exact-member local code | ship loss or remote leakage | local-only gate | in-game ship buy/sell | active |
| Settings/config | Luna and JSON loaders | broken user configs | compatibility validator | static plus Luna runtime | active |
| Public/export | curated export scripts | private leak/public breakage | explicit release gate | export/leak scan | parked |
| Broad polish | many split UI owners | churn without value | avoid cosmetic moves | none unless touched | low-value |

## Workstreams And Roadmap

Phase 0: keep validation and docs current before behavior edits. Exit when jar/CI validators match source, compatibility surfaces are documented, and known runtime gaps are explicit.

Phase 1: do low-risk structure, constants, and documentation cleanup. Stop if a change becomes behavior-affecting.

Phase 2: modernize one subsystem at a time: GUI rows, config parsing, local ship trading, or trade execution. Do not mix source-mode changes with UI polish.

Phase 3: mature validation. Add cheap checks for real contracts, finish rollback runtime evidence, and record proof. Stop when checks duplicate stronger evidence.

Phase 4: pursue Shatter Lib extraction, remote ship trading, public release, refit/combat integration, or profiling only with explicit human gates.

Low-value work to avoid: package churn, cosmetic renames, broad helper extraction across runtime boundaries, public release edits during private cleanup, remote ship semantics as cleanup, or abstractions that do not remove real drift.

## Validation Matrix

Static/build/package: `build.ps1`, `validateLocalBuildEnvironment`, `validate-kotlin-migration.ps1`, `validate-gui-button-style.ps1`, `validate-jar-classes.ps1`, `validate-compatibility-surfaces.ps1`, `validate-doc-links.ps1`, `export-public.ps1`, `git diff --check`, deploy parity.

Pure logic candidates: `StockItemType`, `TradeMoney`, `StockReviewConfig`, `WeaponMarketBlacklist`, blacklist display-name matching, and Fixer catalog encode/decode. No dedicated unit-test suite is currently declared.

Manual Starsector checks: F8 open/close, dialog option, Luna settings, Local/Sector/Fixer buys, legal/black sells, mixed plans, stale stock, rollback forced failures, local ship buy/sell, ship grid/tooltip/filter, and live jar class validation.

Never conflate states: source changed, built, static-validated, deployed, parity-current, runtime-verified, benchmarked. UI, Luna behavior, campaign mutation, callbacks, and rollback cannot be proven outside Starsector.

## Definition Of Done

Substantially complete means compatibility surfaces are documented and guarded, stale validators are fixed, high-risk subsystems have clear owners, runtime-sensitive behavior has matching validation guidance, and future cleanup can be selected as bounded tasks.

Stop when remaining work is cosmetic, risky without behavior need, lacks practical validation, requires public release or save/schema changes, or adds abstraction without reducing maintenance cost.

## Immediate Actions

1. Keep `tools/validate-jar-classes.ps1`, `.github/workflows/sanity.yml`, and `tools/validate-compatibility-surfaces.ps1` green before runtime cleanup.
2. Use this document plus `docs/CODE_QUALITY.md` when selecting bounded cleanup.
3. Add parser/key tests only where they protect real shipped config or save contracts.
4. Finish rollback forced-failure runtime validation before changing trade execution semantics.
5. Treat GUI cleanup as runtime work: run GUI/static checks and get in-game proof for layout changes.
6. Keep ship trading local-only unless remote ship semantics receive a separate design.
7. Require human approval before public export/publish, save-key changes, or compatibility-breaking settings/data changes.
