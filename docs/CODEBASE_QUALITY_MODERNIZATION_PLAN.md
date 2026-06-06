# Codebase Quality Modernization Plan

Target state: Enhanced Trading stays easy and safe to change because subsystem boundaries, compatibility surfaces, invariants, validation limits, and runtime risks are explicit. This is not a mandate for polishing, release work, redesign, remote ship trading, save/schema churn, or speculative abstraction.

## Repository Inventory

Build/package: `build.ps1` delegates to Gradle/Kotlin JVM `2.1.20`, Java 17, `buildMod`, and writes `jars/enhanced-trading.jar`. `mod_info.json` loads it and declares LazyLib, LunaLib, and Shatter Lib.

Source is Kotlin under `src/main/kotlin`. Main packages: `weaponsprocurement.config`, `lifecycle`, `plugins`, `stock.fixer`, `stock.inventory`, `stock.item`, `stock.market`, `trade.execution`, `trade.plan`, `trade.quote`, `ui/WimGui*`, and `ui.stockreview.*`. Rule command: `com/fs/starfarer/api/impl/campaign/rulecmd/WP_OpenDialog`.

Runtime entry points: `WeaponsProcurementModPlugin`, transient `StockReviewHotkeyScript`, transient `WeaponsProcurementFixerCatalogUpdater`, and `data/campaign/rules.csv`. Config/data: `data/config/LunaSettings.csv`, `enhanced_trading_stock.json`, `enhanced_trading_market_blacklist.json`, and `graphics/ui/wp_debug_empty_item.png`. No repo-defined hullmod, ability, weapon, ship, variant, wing, faction, market, or common-storage file is evidenced.

## Compatibility Map

Do not change without explicit approval and matching validation:

- Mod identity and load path: `enhanced_trading`, `jars/enhanced-trading.jar`, `weaponsprocurement.plugins.WeaponsProcurementModPlugin`.
- Rule/dialog path: `WP_OpenDialog`, `wp_marketOpenWeaponProcurement`, `wp_openWeaponProcurement`.
- Item keys: `W:<weaponId>` and `F:<wingId>` from `StockItemType`. Raw ids are compatibility inputs only.
- Luna keys: `wp_trade_hotkey`, `wp_enable_dialog_option`, `wp_enable_sector_market`, `wp_enable_fixers_market`, `wp_enable_fixers_market_tag_inference`, price multipliers, desired counts, and `wp_enable_debug_ui`.
- Published system-property keys: `wp.config.*` values in `CompatibilityIds.SystemProperties`, covering update interval, source toggles, multipliers, desired counts, hotkey, and debug UI.
- JSON paths/schema: `data/config/enhanced_trading_stock.json` fields `display`, `sources`, `desiredDefaults`, `perItem`, legacy `perWeapon`; blacklist keys `BANNED_FROM_SECTOR_MARKET`, `BANNED_FROM_FIXERS_MARKET`.
- Save/custom data: `weaponsProcurement.fixerObservedCatalog.v1` in `FixerMarketObservedCatalog`, encoded as simple string maps.
- Diagnostics: `wp.debug.failTradeStep`, `wp.debug.shipCatalog`, `wp.debug.shipCatalogView`, debug item IDs, `WP_STOCK_REVIEW_ROLLBACK`, `WP_SHIP_CATALOG_DIAG`.
- Source semantics: Starsector `SUBMARKET_OPEN`, `GENERIC_MILITARY`, `SUBMARKET_BLACK`, `SUBMARKET_STORAGE`, `LOCAL_RESOURCES`, plus virtual `wp_fixers_market`.
- Reflection/classloader-sensitive names: rule command class path and jar/live class validator lists.

## Target Architecture

Public compatibility layer: shipped ids, config keys, data paths, item-key parsing, and plugin/rule paths stay explicit. Refactors must not rename or reinterpret them.

Source owner: `weaponsprocurement.CompatibilityIds`; validators check values and consumer references.

Settings/config: `WeaponsProcurementConfig` is the Luna/System-property bridge and publishes documented `CompatibilityIds.SystemProperties` keys. `StockReviewConfig` and `WeaponMarketBlacklist` own JSON loading, defaults, and legacy compatibility.

Campaign lifecycle: `WeaponsProcurementModPlugin` registers transient scripts only. `StockReviewHotkeyScript` owns market-backed opening and close/reopen. `WeaponsProcurementFixerCatalogUpdater` owns observed catalog updates and diagnostics.

Stock/source/trade: `MarketStockService`, `GlobalWeaponMarketService`, `StockReviewQuoteBook`, `StockPurchaseService`, and `StockPurchaseExecutor` keep Local, Sector Market, and Fixer's Market semantics distinct. Transaction callbacks are post-commit side effects.

GUI: `WimGui*` owns custom-panel quirks, button polling, scroll, modal input, text fitting, and tooltip caps. `StockReviewPanelPlugin` stays orchestration-only; rows/renderers/controllers own presentation. Preserve event-gated polling and sibling-safe anchoring.

Ships/fighters: ship trading remains local-only exact-member trading in `ui.stockreview.ships`. Fighter LPCs are item `WING` records with `F:` keys. Remote ship trading, refit, and combat HUD work require design gates.

Persistence: keep Fixer catalog state simple, sanitized, and migration-aware. No save-key rename without approval.

Validation/tooling/docs: validators must reflect current architecture. `tools/lib/Validation.Assertions.ps1` owns shared PowerShell assertions; validators own repo contracts. Docs stay compact and link deep dives.

## Risk Register

| Area | Evidence | Impact | Mitigation | Validation | Status |
|---|---|---|---|---|---|
| Validator/CI drift | stale jar/class checks or helper drift | false failures or ignored checks | keep validator lists current and share assertions | jar, Kotlin, GUI, compatibility validators | active |
| GUI/classloader | `WimGui*`, UI deep dive | crashes, clipping, stale classes | bounded UI edits | GUI validator, jar/live class, in-game | active |
| Trade rollback | `StockPurchaseExecutor`, analyzer | cargo/credit corruption | static contract guard plus forced-failure matrix | validator plus runtime analyzer | active |
| Source semantics | Local/Sector/Fixer services | wrong cargo drain/pricing | preserve static contracts and runtime matrix | validator plus manual trade matrix | active |
| Fixer persistence | save key v1 | save compatibility loss | migration-before-change | static and runtime save proof | active |
| Ship trading | exact-member local code | ship loss or remote leakage | static local-only gate plus in-game proof | validator plus ship buy/sell | active |
| Settings/config | Luna, `wp.config.*`, and JSON loaders | broken user configs or stale internal settings readers | compatibility and config-contract validators | static plus Luna runtime | active |
| Build/runtime dependency parity | installed Shatter Lib jar may lag checkout APIs | stale runtime dependency | API gates in build and deploy | build current Shatter Lib; deploy parity | active |
| Public/export | curated export scripts | private leak/public breakage | explicit release gate | export/leak scan | parked |
| Broad polish | many split UI owners | churn without value | avoid cosmetic moves | none unless touched | low-value |

## Workstreams And Roadmap

Phase 0: keep validation/docs current before behavior edits. Exit when jar/CI validators match source, compatibility surfaces are documented, and gaps are explicit.

Phase 1: low-risk structure, constants, and docs cleanup. Stop if a change affects behavior.

Phase 2: modernize one subsystem at a time: GUI rows, config parsing, local ship trading, or trade execution. Do not mix source-mode changes with UI polish.

Phase 3: mature validation. Add cheap checks for contracts, finish rollback runtime evidence, and record proof.

Phase 4: pursue Shatter Lib extraction, remote ship trading, public release, refit/combat integration, or profiling only with human gates.

Low-value work to avoid: package churn, cosmetic renames, broad helper extraction across runtime boundaries, release edits during private cleanup, remote ship semantics as cleanup, or abstractions that do not remove real drift.

## Validation Matrix

Static/build/package: `build.ps1`, `validateLocalBuildEnvironment`, `validatePureLogicContracts`, Kotlin/GUI/jar/compat/assertion/config/Fixer/rollback/source/ship/evidence validators, doc links, `export-public.ps1`, `git diff --check`, deploy parity/evidence.

Pure logic coverage: `validatePureLogicContracts` executes `StockItemType`, `StockSortMode`, and `TradeMoney` contracts without a test framework. Config/Fixer/rollback/source/ship/evidence validators cover Luna/source keys, JSON schema, item keys, blacklist matching, `TradeMoney`, Fixer save gates, rollback hooks/schema, Local/Sector/Fixer separation, and local exact-member ship gates. Add runtime save/rollback/trade proof before migration or trade semantics changes. No broad unit-test suite is declared.

Manual Starsector checks: F8 open/close, dialog option, Luna settings, Local/Sector/Fixer buys, legal/black sells, mixed plans, stale stock, rollback forced failures, local ship buy/sell, ship grid/tooltip/filter, and live classes.

Never conflate states: source changed, built, static-validated, deployed, parity-current, runtime-verified, benchmarked. UI, Luna behavior, campaign mutation, callbacks, and rollback cannot be proven outside Starsector.

## Definition Of Done

Substantially complete means compatibility surfaces are documented and guarded, stale validators are fixed, high-risk subsystems have owners, runtime-sensitive behavior has validation guidance, and future cleanup can be selected as bounded tasks.

Stop when remaining work is cosmetic, risky without behavior need, lacks practical validation, requires public release or save/schema changes, or adds abstraction without reducing maintenance cost.

## Immediate Actions

1. Keep `tools/validate-jar-classes.ps1`, `.github/workflows/sanity.yml`, `tools/validate-compatibility-surfaces.ps1`, and `weaponsprocurement.CompatibilityIds` green before runtime cleanup.
2. Use this document plus `docs/CODE_QUALITY.md` when selecting bounded cleanup.
3. Expand parser/key tests only where they protect real shipped config or save contracts not covered by the current validators.
4. Finish rollback forced-failure runtime validation before changing trade execution semantics.
5. Treat GUI cleanup as runtime work: run GUI/static checks and get in-game proof for layout changes.
6. Keep ship trading local-only unless remote ship semantics receive a separate design.
7. Require human approval before public export/publish, save-key changes, or compatibility-breaking settings/data changes.
