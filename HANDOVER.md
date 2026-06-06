# Enhanced Trading Handover

This handover is written for a modder who is taking over the private source repo. It focuses on how the mod works, where the code is intentionally shaped by Starsector runtime constraints, and where future work is likely to be risky.

## What This Mod Is

Enhanced Trading is a Starsector `0.98a` mod for opening a compact trade popup from a market or market-backed storage dialog. The popup lets the player review weapon and fighter LPC stock, queue buys and sells, review the pending plan, and confirm the trade.

The public/default user path is:

1. Open a market dialog or a market-backed storage dialog.
2. Press the LunaLib-configurable trade popup hotkey, default `F8`, or enable the optional dialog option.
3. Use `Trade: Items` for weapons/LPCs or `Trade: Ships` for local ship trading.
4. Queue trades, review them, and confirm.

Important boundary: cargo-cell weapon/LPC badges are no longer part of this repo. They moved to the standalone private `D:\Sean Mods\Weapon Badges` mod. Do not add badge helpers, count bridges, generated badge sprites, or core bytecode patching back here.

## Current State

- Repo path: `D:\Sean Code Projects\Starsector Projects\Enhanced Trading`.
- Mod identity: `enhanced_trading`.
- Main plugin: `weaponsprocurement.plugins.WeaponsProcurementModPlugin`.
- Runtime jar: `jars/enhanced-trading.jar`.
- Live deploy target: `C:\Games\Starsector\mods\Enhanced Trading`.
- Dependencies: LazyLib, LunaLib, and Shatter Lib.
- Version in `mod_info.json`: `0.2.0`.
- Primary language: Kotlin, built through Gradle via `build.ps1`.

The item-trading popup is the stable product baseline. Ship trading exists as a local-only first implementation behind the `Trade: Items` / `Trade: Ships` toggle. The 4-column by 5-row ship grid is a user-confirmed visual baseline to preserve. The ship tooltip is a public-API approximation of vanilla; treat further polish as visual/runtime work, not as a simple static refactor.

Template-synced project facts live in `docs/PROJECT_FACTS.md`; validation commands live in `docs/CHECKS.md`; generated orientation lives in `docs/REPO_MAP.md`. Do not assume `main` is a release boundary until you inspect `git status --short --branch`, `PLANS.md`, and the current diff.

## Recent Work Snapshot

The recent modernization run has been intentionally bounded and behavior-preserving. Latest pushed source baseline is `0157fad` (`Clarify stock review action construction`).

Recent commits hardened trade and ship execution around unsafe mutation failures, nonfinite numeric settings/cargo-space values, Fixer catalog decoding, post-commit transaction reports, rollback journaling, and stale runtime Shatter Lib dependency detection. They also split item tooltip code into smaller owners:

- `StockReviewTooltipModels`: shared tooltip row/layout models.
- `StockReviewTooltipIconPanelPlugin`: sprite-backed icon panel drawing for non-weapon tooltip icons.
- `StockReviewWingTooltipRenderer`: fighter LPC tooltip rendering.
- `StockReviewWeaponTooltipRows`: weapon primary/ancillary stat row derivation.
- `StockReviewItemTooltipContext`: cargo-space, price, owned-count, and Shatter Lib context-line construction.
- `StockReviewWeaponTooltipIconGridRenderer`: weapon/debug icon-grid panel rendering and measured stat-row layout.
- `StockReviewWeaponTooltipTextRenderer`: weapon description, debug description, custom primary/ancillary text, highlight substitution, and measured text truncation.
- `StockReviewWingTooltipLayoutBuilder`: real/debug fighter LPC layout construction, description lookup, system labels, and armament summary; real fighter stat rows reuse `WeaponStockRecord` labels.

`StockReviewItemTooltip` still owns item-tooltip orchestration and the padded weapon tooltip shell. Pause before further tooltip source cleanup unless another small extraction clearly reduces maintenance cost while preserving Shatter Lib `ShatterWeaponTooltip`/`ShatterWingTooltip` delegation and current debug/stress behavior.

Runtime proof has not caught up to static/source cleanup because the live installed Shatter Lib jar is stale. Until `C:\Games\Starsector\mods\Shatter Lib\jars\shatter-lib.jar` contains `ShatterItemTooltipContext.class` and `ShatterTooltipContextLine.class`, build with the Shatter Lib checkout override for source/package proof only and do not claim live deploy parity.

A generic-template sync was requested and started only as read-only comparison before the current handover request superseded it. The template repo currently has uncommitted governance updates. Treat template sync as a future docs/tooling task, not as source/runtime work.

## Document Map

Use `.agent/INDEX.md` as the routing map. The high-value starting points are:

- `AGENTS.md`: repo-local operating rules, deploy policy, hard constraints, and archive trigger map.
- `docs/PROJECT_FACTS.md` and `docs/CHECKS.md`: exact commands, paths, deploy/Git facts, validation choices, and evidence limits.
- `.agent/BRIEF.md`, `PLANS.md`, and `.agent/ARCHITECTURE_MAP.md`: current state, active work, subsystem map, and high-risk read triggers.
- `.agent/archive/INDEX.md`: deep evidence and history; open only the note matching the task.

Before large or risky work, start with those routing docs and then inspect the relevant source files or archive note.

## Entry Points And Runtime Lifecycle

`WeaponsProcurementModPlugin` registers two transient campaign scripts on game load:

- `StockReviewHotkeyScript`: owns the popup hotkey, dialog open/close tracking, and reopen requests after review/confirm flows.
- `WeaponsProcurementFixerCatalogUpdater`: periodically observes safe market stock for Fixer's Market and runs hidden ship-catalog diagnostics when requested.

`WP_OpenDialog` plus `data/campaign/rules.csv` provide the optional market-dialog entry. This path should call the same opener semantics as the hotkey path.

`StockReviewHotkeyScript` requires a current market. The docs intentionally say "market-backed storage dialog" rather than generic storage, because non-market storage has not been modeled for safe pricing or mutation. Pressing the configured hotkey while the popup is open requests a clean close.

Luna settings are read by `WeaponsProcurementConfig`. It publishes stable `System` properties because some hot paths and diagnostic hooks need low-friction reads without repeatedly reaching into LunaLib. The user-visible hotkey is `wp_trade_hotkey`.

## Stock Item Model

Items are weapons and fighter LPCs. Shared maps use typed keys:

- `W:<weaponId>`
- `F:<wingId>`

Raw ids appear only at API boundaries or backward-compatible config paths. New shared state should use typed keys.

Important model owners:

- `StockItemType`: typed key parsing and construction.
- `StockItemStacks`: reference cargo stacks, base buy/sell pricing, local tariff math, and cargo-space references.
- `StockItemSpecs`: safe access to weapon and wing specs.
- `WeaponStockRecord`: UI-facing derived data for one stock item, including real fighter LPC Advanced Info stat labels.
- `WeaponStockSnapshot`: grouped item records for one popup rebuild.
- `WeaponStockSnapshotBuilder`: merges player inventory, local or remote stock, desired thresholds, and config ignores.
- `DesiredStockService`: default and per-item desired stock.
- `InventoryCountService`: player cargo plus accessible storage counts.
- `MarketStockService`: local market stock and submarket filtering.
- `GlobalWeaponMarketService`: Sector Market and Fixer's Market item stock.

Pricing for weapons and LPCs should stay centralized through `StockItemStacks`. It applies Starsector settings such as `shipWeaponBuyPriceMult` and `shipWeaponSellPriceMult`, then the current submarket tariff where appropriate. If you change item prices, update quote, execution, source allocation, transaction reporting, and tooltip paths together.

## Source Modes

`Local` uses the current market and respects the Black Market toggle.

`Sector Market` is live sector-wide stock from real market cargo. It marks prices up by the configured sector multiplier, but keeps market/submarket/cargo identities so confirmation can drain the actual remote stacks.

`Fixer's Market` is virtual stock. It combines:

- `Live`: currently observed real market cargo.
- `Catalog`: faction catalog eligibility inferred from current market factions and sale rules.
- `Catalog+ref`: catalog item using previously observed price/cargo-space reference data.
- `Common` through `Very rare`: rarity metadata from tier and faction sell-frequency heuristics.

Fixer labels explain why an item appears and support sorting/filtering. Rarity does not change price by itself. Fixer purchases do not drain real cargo.

Remote source modes disable black-market selling. Sells while a remote source is active use the current local legal buyer.

## Item Trade Flow

Pending item trades live in `StockReviewPendingTrades` and `StockReviewPendingTrade`.

Planning:

- `StockReviewTradeController` mutates the pending plan.
- `StockReviewTradePlanner` finds filter-aware bulk buy/sell candidates.
- `StockReviewLocalMarketIntent` preserves active local buy intent while black/open market rebalancing is happening.
- `StockReviewLocalMarketRebalancer` reconciles local buy allocations across submarkets.

Quote/review:

- `StockReviewTradeContext` and `StockReviewQuoteBook` calculate display quotes, affordability, source allocations, and warning state.
- Review rows are grouped by buying/selling and item type. Hullmods are currently a placeholder heading only, not a supported trade type.

Execution:

1. Sells execute first.
2. Generic cheapest-source buys execute second.
3. The vanilla cargo screen is refreshed.
4. The popup rebuilds or reopens as needed.

Execution ownership:

- `StockReviewExecutionController`: popup confirm flow.
- `StockPurchaseService`: high-level item buy/sell orchestration.
- `StockPurchasePlan`: cheapest-source plan.
- `StockPurchaseExecutor`: cargo/credit mutations and rollback journal.
- `StockMarketTransactionReporter`: best-effort `PlayerMarketTransaction` callback reporting.

`StockPurchaseExecutor` uses `TradeMoney`/`long` totals and fails closed for unsafe credit mutations. Transaction callbacks are post-commit side effects; do not report a market transaction before rollbackable cargo and credit mutations have succeeded.

Rollback diagnostics emit `WP_STOCK_REVIEW_ROLLBACK` log records. Use `tools/analyze-trade-rollback-diagnostics.ps1 -RequirePass` after forced-failure runtime tests.

## Ship Trading

Ship trading is intentionally local-only in v1. Do not assume Sector Market or Fixer's Market ship trading exists.

Main owners:

- `StockReviewShipSnapshotBuilder`: builds buy records from eligible local submarket `mothballedShips` and sell records from the player fleet.
- `StockReviewShipRecord`: one exact ship member plus source/side/price metadata.
- `StockReviewPendingShipTrades`: queued exact ship trades.
- `StockReviewShipTradeController`: toggles ship plans.
- `StockReviewShipExecutionController`: moves exact `FleetMemberAPI` instances between local submarket storage and player fleet.
- `StockReviewShipGridRenderer`: compact 4x5 page grid.
- `StockReviewShipFilterModal`, `StockReviewShipFilters`, and `StockReviewShipHullFilterInput`: ship-specific filters and hull-class text filter.
- `StockReviewShipTooltip` and `StockReviewShipStats`: public-API vanilla-like tooltip approximation and corrected stat sourcing.

Buy execution removes the exact queued member from the source submarket's mothballed fleet, adds it to the player fleet, deducts credits, and reports a ship transaction. Sell execution removes the exact player fleet member, adds it to the local sell target's mothballed fleet, adds credits, and reports a transaction. If membership changed before confirm, the trade fails cleanly and the popup rebuilds with remaining state.

Future remote ship trading is a new feature, not a small extension. It needs clear semantics for source draining, virtual ships, exact member identity, pricing, fleet limit checks, and transaction reporting.

## GUI Architecture

The GUI is built on repo-local `WimGui*` primitives because Starsector's campaign UI has fragile nested custom panel behavior.

Shared UI owners:

- `WimGuiModalPanelPlugin`: custom dialog lifecycle, content rebuild, input routing, and close behavior.
- `WimGuiControls`: button/label/panel creation and binding registration.
- `WimGuiButtonPoller`: fallback click detection for nested custom-panel buttons.
- Shatter Lib `DialogLifecycleTracker` and `KeyPressLatch`: dialog reopen/close state and hotkey edge detection used by `StockReviewHotkeyScript`.
- `WimGuiText`: measured and approximate text fitting/wrapping helpers.
- `WimGuiTooltip` and `StockReviewTooltipPanel`: tooltip sizing, shared panel styling, and max-height cap.

Stock-review orchestration:

- `StockReviewPanelPlugin`: host boundary and controller wiring. Keep it orchestration-only.
- `StockReviewRenderer`: screen shell, cached row model, filter background pass, and item/ship mode switch.
- `StockReviewModeController`: trade/review/filter/debug modes.
- `StockReviewUiController`: high-level UI actions.
- `StockReviewActionRowButtons` / `StockReviewActionCells`: standard button routes and style guardrails.
- `StockReviewListModel`, row sections, row specs, and row renderers: item list and review list composition.

Filter modals should render the current trade screen behind a dim overlay, then draw a centered modal. Background controls must not be interactive while a modal is open.

Known Starsector UI traps:

- Nested button callbacks are unreliable; preserve the poller fallback.
- Anchoring components across non-sibling panels can crash with `May only anchor on siblings`.
- Runtime visual proof matters. Build success does not prove tooltip, row, modal, or scroll behavior.
- Keep top-level Kotlin helper generation out of classloader-sensitive paths unless live validation confirms it.

## Debug UI And Stress Records

The Luna setting `wp_enable_debug_ui` is the master gate for debug UI. When disabled, the public popup should not show Colors, debug rows, debug weapons/wings, or the debug ship.

Debug records are intentionally extreme and should reuse normal row/tooltip paths as much as possible. They exist to reveal layout failure under long names, large values, dense armaments, and worst-case descriptions. If a debug record needs special handling, keep it small and documented; do not duplicate the whole normal row or tooltip pipeline.

Hidden ship-catalog diagnostics remain controlled by system properties and should stay maintainer-only.

## Tooltips

Weapon and wing tooltips are custom panels inspired by vanilla cargo/refit tooltips. They use public APIs only.

Normal item records delegate to Shatter Lib `ShatterWeaponTooltip` and `ShatterWingTooltip` with repo-owned context lines. Repo-local custom tooltip code remains important for debug/stress records and for layout pieces that Enhanced Trading owns. Keep this split intact unless a shared-library extraction is explicitly designed and validated.

Current item-tooltip owners:

- `StockReviewItemTooltip`: item tooltip orchestration and padded weapon tooltip shell.
- `StockReviewItemTooltipContext`: cargo-space, price, owned-count, and Shatter Lib context-line construction.
- `StockReviewWeaponTooltipRows`: weapon stat rows.
- `StockReviewWeaponTooltipIconGridRenderer`: weapon/debug icon-grid panel rendering and measured stat-row layout.
- `StockReviewWeaponTooltipTextRenderer`: weapon description, debug description, custom primary/ancillary text, highlight substitution, and measured text truncation.
- `StockReviewWingTooltipLayoutBuilder`: real/debug fighter LPC layout construction, description lookup, system labels, and armament summary; real fighter stat rows reuse `WeaponStockRecord` labels.
- `StockReviewWingTooltipRenderer`: fighter LPC panel layout.
- `StockReviewTooltipIconPanelPlugin`: sprite icon panel drawing.
- `StockReviewTooltipPanel`: shared row/label/band primitives and max-height cap.

Ship tooltips approximate vanilla's ship-sale tooltip without importing obfuscated UI classes. They use `FleetMemberAPI`, `ShipHullSpecAPI`, public stats, and custom panel drawing. If you adjust ship tooltip layout, test a small frigate, a large capital, and the debug ship.

Tooltip height is capped through `WimGuiTooltip.maxTooltipHeight()`, currently 95% of the screen height. Long text should use available height before truncating. Truncation should happen at the end of the last allowed line, not mid-region.

## Build, Validation, And Deploy

Exact build, validation, deploy, status, and parity commands live in `docs/PROJECT_FACTS.md` and `docs/CHECKS.md`.

Use docs-only checks for docs-only edits. For runtime/source work, build first, then add the focused validator that matches the touched surface: GUI button style, Kotlin/source boundary, deploy parity, live GUI classes, or rollback diagnostics.

`tools/deploy-live-mod.ps1` clean-syncs repo-managed files to the live mod folder. If Starsector locks the jar, it stages the built files and queues a minimized visible no-activate deploy worker. A queued deploy is not a live/runtime fix until parity is rechecked after the lock clears.

Do not deploy docs-only/comment-only work unless explicitly asked.

## Public Release Boundary

Private repo work is not mirrored directly to `Shattersphere-Mods`. Public export must be curated through `.agent/PUBLIC_RELEASE.md` and `tools/export-public.ps1`.

Public output should include only user/contributor material. It must exclude:

- `AGENTS.md`
- `.agent/`
- `HANDOVER.md`
- `PLANS.md`
- private archives
- local paths
- deploy queues/staging logs
- badge/bytecode patching material

Changelog entries must be user-facing and must not mention agents, private docs, local paths, or private experiments.

## Unpolished Or Risky Areas

- Ship trading is local-only and still needs more runtime acceptance, especially tooltip polish and edge cases around exact member identity.
- Remote ship trading is not designed.
- Ship tooltip layout is custom and public-API-only, so exact vanilla parity is not expected.
- Runtime rollback fault validation still needs in-game evidence.
- Recent weapon/wing tooltip cleanup has source/static validation only. Visual acceptance in Starsector is still required.
- Item/wing/ship tooltip stats must be audited against vanilla when new fields are added.
- Filter modal/input handling is fragile because it mixes custom panels, input polling, dimmed background rendering, and Starsector focus behavior.
- The repo tracks `jars/enhanced-trading.jar`; keep it consistent with source when runtime code changes.
- Luna/data/graphics/live parity matters. Jar parity alone is insufficient.
- `May only anchor on siblings` crashes usually mean a UI component was anchored relative to a panel that is not its parent or sibling. Fix the UI ownership/layout path rather than masking the exception.

## Practical Recipes

Add an item filter:

1. Add state in `StockReviewFilterState` / `StockReviewFilter`.
2. Render controls through the filter row/section path.
3. Apply matching in `StockReviewFilters`.
4. Ensure bulk actions respect active filters if users would expect filtered scope.
5. Run GUI style and Kotlin migration validators.

Change item pricing:

1. Start at `StockItemStacks`.
2. Verify `WeaponStockRecord`, quote book, purchase plan, execution, summaries, transaction reports, and tooltips use the same base/final semantics.
3. Test local vanilla comparison for one weapon and one LPC.

Add a ship filter:

1. Add state to `StockReviewShipFilterState`.
2. Add UI in `StockReviewShipFilterModal`.
3. Match in `StockReviewShipFilters`.
4. Ensure the top hull-class input and filter modal combine predictably.
5. Confirm page count/scroll resets after filter changes.

Add tooltip fields:

1. Find the public API source for the value.
2. Add the field through the relevant tooltip layout helper.
3. Test real small/large examples and debug stress records.
4. Keep the tooltip under the shared max-height cap.

Prepare public export:

1. Read `.agent/PUBLIC_RELEASE.md`.
2. Build and validate the private source.
3. Run `tools/export-public.ps1`.
4. Leak-scan the public output.
5. Do not publish private handover/archive/plans.

## Recommended Next Steps

Recommended: resolve the stale installed Shatter Lib dependency, then deploy/parity-check the mod and visually smoke weapon, wing, and ship tooltips before claiming tooltip acceptance.

After tooltip visual smoke, validate the 4x5 ship grid, perform rollback fault validation for item trades, and update `.agent/BRIEF.md` with the verified runtime result.
