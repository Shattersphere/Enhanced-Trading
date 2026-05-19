# Agent takeover handover

Status: historical snapshot; stale in badge/private-build and pre-ship-trading areas
Scope: Weapons Procurement private repo orientation after Kotlin migration, Fixer catalog work, stock-review UI polish, and private badge isolation
Last verified: 2026-05-18 at commit `b85e7a1`
Read when: a new agent is taking over the project, resuming broad architecture work, touching stock-review UI, changing source modes, changing Fixer's Market, changing patched badges, or preparing public release work
Do not read for: one-line config edits, typo fixes, or routine build/deploy commands already covered by `AGENTS.md` and `HANDOVER.md`
Related files: `AGENTS.md`, `.agent/BRIEF.md`, `HANDOVER.md`, `PLANS.md`, `PACKAGING.md`, `.agent/PUBLIC_RELEASE.md`, `.agent/archive/INDEX.md`, `build.gradle.kts`, `build.ps1`, `mod_info.json`, `tools/deploy-live-mod.ps1`, `tools/deploy-private-badges.ps1`, `src/main/kotlin`, `src/privateBadge/kotlin`
Search tags: `takeover`, `handover`, `Kotlin migration`, `stock review`, `Fixer`, `Sector Market`, `patched badges`, `public release`, `Shattersphere-Mods`, `ship catalog debug`

Current-doc warning: this is not active guidance. Cargo-cell badges now live in the standalone private `D:\Sean Mods\Weapon Badges` repo, private badge source/build paths described here have been removed or superseded, and `Trade: Ships` now exists as a local-only feature. For current practice, read `HANDOVER.md`, `.agent/ARCHITECTURE_MAP.md`, `.agent/BRIEF.md`, and `PLANS.md` first.

## Summary

- This is a point-in-time project handover, not a living source of truth. Prefer `AGENTS.md`, `.agent/BRIEF.md`, `HANDOVER.md`, and `PLANS.md` for current practice.
- The repo is the source of truth. `C:\Games\Starsector\mods\Weapons Procurement` is a deploy target and can be stale or queue-blocked by a running Starsector process.
- The project is now a Kotlin/Gradle Starsector `0.98a` mod. `build.ps1` remains the compatibility entry point, and LazyLib is required at runtime for Kotlin support.
- The public/default product is the clean `F8` stock-review popup for weapons and fighter LPCs. Optional patched cargo-cell badges are private, isolated, and not part of public release.
- Local, Sector Market, and Fixer's Market intentionally have different trade semantics. Do not collapse them into a single "market stock" abstraction without preserving drain/virtual/access behavior.
- Fixer's Market uses deterministic runtime sale capability for weapons/LPCs plus observed market references. The old observation-only model was intentionally replaced to avoid cold-start blank catalogs.
- Ship catalog code exists as diagnostics and future groundwork only. It must not be mistaken for enabled ship purchasing.
- The stock-review UI is custom Starsector campaign UI. Build success is not enough evidence; layout, input, tooltip, and deploy behavior need runtime checks after meaningful UI changes.
- Public release work must be curated for `Shattersphere-Mods`. Never mirror the private repo directly because it contains agent docs, local paths, archives, and private badge/bytecode material.
- Recent commits after the living brief matter: `4eeff6c` fixed private badge bridge deployment, `25512bd` cleaned stock-review layout and market access, and `b85e7a1` added a gated ship catalog debug view.

## Index

- `Project shape`: what the mod is now and which docs are authoritative.
- `Build and package model`: Kotlin/Gradle, source sets, jars, and dependencies.
- `Public/private boundary`: clean popup versus private patched badges.
- `Runtime entry points`: how the mod enters Starsector and opens the popup.
- `Stock item model`: typed item keys, records, snapshots, and desired stock.
- `Source modes`: Local, Sector Market, and Fixer's Market semantics.
- `Fixer catalog model`: observed, theoretical, rarity, metadata, and ship diagnostics.
- `Trade execution model`: planning, quoting, validation, mutation, rollback, and callbacks.
- `Stock-review UI`: modal modes, row layout, icons, tooltips, and known fragility.
- `Patched badges`: why the bridge exists and how not to break it.
- `Recent work context`: important May 2026 changes not fully captured in older history files.
- `Validation posture`: which checks prove what and what remains runtime-gated.
- `Public release notes`: what future release agents must protect.
- `Practical takeover path`: where a new agent should start.

## Details

## Project shape

Weapons Procurement is a Starsector `0.98a` mod that reviews weapon and fighter LPC stock, compares player inventory/storage against desired stock, lets the player stage buys and sells, and executes confirmed trades.

The main user flow is:

- open a valid market or storage dialog;
- press `F8`, or use the optional LunaLib-enabled market-dialog entry;
- choose Local, Sector Market, or Fixer's Market as the source;
- queue buys/sells;
- review and confirm.

The current living docs have distinct roles:

- `AGENTS.md`: commands, deploy policy, hard constraints, and the project knowledge map.
- `.agent/BRIEF.md`: compact current state and next-step handoff. It may lag if not updated after a rapid commit sequence, so verify with `git log`.
- `HANDOVER.md`: stable architecture, entry points, source modes, trade flow, and validation constraints.
- `PLANS.md`: active and deferred work.
- `.agent/archive/INDEX.md`: map to deep dives and historical notes.
- `.agent/PUBLIC_RELEASE.md`: private checklist for curated public release/export work.

This archived handover intentionally overlaps some living docs, but its purpose is different: give a new agent enough social/technical context to avoid rediscovering recent decisions.

## Build and package model

The project is Kotlin-first now:

- Gradle plugin: Kotlin JVM `2.1.20`.
- JVM target: `17`.
- Public source set: `src/main/kotlin`.
- Private badge source set: `src/privateBadge/kotlin`.
- Legacy public Java source under `src` is still part of the Gradle source-set configuration for compatibility, but the migration gate should prevent repo-owned Java source from creeping back in.
- Runtime jar path: `jars/weapons-procurement.jar`.
- Main plugin class: `weaponsprocurement.plugins.WeaponsProcurementModPlugin`.
- Rule command class: `com.fs.starfarer.api.impl.campaign.rulecmd.WP_OpenDialog`.
- Mod id: `weapons_procurement`.
- Required dependencies: LazyLib and LunaLib.

`build.ps1` delegates to Gradle so old workflow commands still work. Use `-StarsectorDir` or `STARSECTOR_DIRECTORY` so Gradle can resolve Starsector core jars plus LunaLib/LazyLib jars from the local install.

Clean builds package only public-safe code. Private badge builds include `src/privateBadge/kotlin` and must be treated as local/private artifacts unless the user explicitly asks for an advanced private package.

The repo currently commits the built jar. Do not assume a source change is packaged unless the jar was rebuilt and included when appropriate.

## Public/private boundary

The clean popup is the public product. The private patched-badge path exists because exact vanilla cargo-cell badges require patching Starsector's obfuscated core UI. That is powerful but not a good public default.

Public release/export must exclude:

- `AGENTS.md`;
- `.agent/`;
- `HANDOVER.md`;
- `PLANS.md`;
- private archives and internal notes;
- local paths and deploy queues;
- private badge source/classes/assets/settings;
- patcher tools and bytecode docs unless explicitly approved;
- any prepatched `starfarer_obf.jar`.

The public target organization is `Shattersphere-Mods`, but the private repo should not be mirrored. Use the curated export flow and leak scans.

Keep the distinction between:

- clean build: safe public popup jar;
- private badge build: local jar with badge helper/config/updater classes;
- patched core jar: Starsector core modified by the cargo-stack-view patcher;
- live mod folder: what Starsector actually loads, possibly stale until deploy completes and the game restarts.

## Runtime entry points

Important runtime entry points:

- `WeaponsProcurementModPlugin`: stable mod plugin class loaded by Starsector from `mod_info.json`.
- `StockReviewHotkeyScript`: transient script that detects valid campaign contexts and opens the popup on `F8`.
- `WP_OpenDialog`: rule command used by optional market-dialog integration.
- `WeaponsProcurementFixerCatalogUpdater`: periodically observes safe real market stock to improve Fixer reference data.
- `WeaponsProcurementCountUpdater`: private source-set script for patched badge count properties. It is reflectively registered only by private builds.

Avoid top-level Kotlin helpers as Starsector entry points. Generated `*Kt` facades are unnecessary risk around classloader-sensitive surfaces. Stable loaded classes should remain named classes/objects.

## Stock item model

Stock items are weapons or fighter LPCs.

Shared keys are typed:

- `W:<weaponId>` for weapons.
- `F:<wingId>` for fighter LPCs.

The typed keys matter because weapon ids and wing ids can overlap, and because config, pending trades, filters, and snapshots need unambiguous maps. Raw ids exist only as compatibility input paths for old config behavior.

Important ownership:

- `StockItemType`: weapon versus fighter LPC typing.
- `StockItemSpecs`: safe spec lookup/display helpers.
- `StockItemStacks`: cargo-stack detection and legality/display helpers.
- `StockItemCargo`: count/add/remove/reconcile helpers for player and market cargo.
- `WeaponStockRecord`: one row's aggregate record, including source counts and metadata.
- `WeaponStockSnapshot`: snapshot of current stock, desired stock, inventory, source mode, filters, and row cache inputs.
- `WeaponStockSnapshotBuilder`: builds the snapshot from config, inventory, local market, remote market, and pending trade context.
- `DesiredStockService`: effective desired thresholds.
- `InventoryCountService`: player cargo plus accessible storage counts.

Do not treat stored-only inventory as enough reason to create buy rows unless the row is also buyable or sellable. Stored quantities appear in `Storage` for rows that otherwise exist.

## Source modes

The three source modes are not interchangeable.

`Local`:

- scans the current market/storage context;
- uses real cargo;
- can honor the Black Market toggle;
- buys drain current market cargo;
- sells go to a valid local buyer;
- should respect vanilla submarket access.

`Sector Market`:

- scans live cargo across eligible sector markets;
- can include black markets when the Black Market toggle is on;
- drains real remote cargo on purchase;
- applies the sector multiplier;
- should not be cached across popup rebuilds because it represents cargo that can be mutated by purchases or other game systems;
- must use the same submarket eligibility logic for display and execution, otherwise rows can appear that cannot be bought.

`Fixer's Market`:

- is virtual;
- uses stock count `999`;
- does not drain real market cargo;
- applies the fixer multiplier;
- uses observed stock for reference prices where available;
- uses theoretical runtime catalogs for cold-start coverage;
- should not gain a Black Market toggle meaning unless deliberately redesigned.

Recent market-access cleanup added shared submarket eligibility in `StockSubmarketAccess`. It filters non-trade/storage/local-resource cases and uses the vanilla submarket plugin enabled-state path with a lightweight core UI stub for relationship/faction access. Keep display and execution on the same helper so the UI does not advertise impossible stock.

## Fixer catalog model

Fixer's Market used to be observation-first: an item entered the catalog only after appearing in generated stock. That was safe but poor for new saves because the catalog could be blank or misleading for a long time.

The current design is:

- `ObservedStockIndex`: exact currently stocked weapons/LPCs from live cargo.
- `FixerMarketObservedCatalog`: persistent observation fallback and correction data.
- `TheoreticalSaleIndex`: deterministic runtime candidate set for weapons/LPCs from faction sell-frequency/known lists plus vanilla-style filters.
- `RarityClassifier`: static rarity/explanation layer for weapons/LPCs.
- `FixerCatalogMetadata`: row metadata such as catalog source and rarity.
- `FixerCatalogPolicy`: shared filtering policy for secret/no-sell/system/spoiler/blacklist logic.

This lets Fixer's Market offer plausible weapons and LPCs before the player has personally seen them, without forcing submarket restocks. Do not probe availability by calling submarket update methods. That mutates live market cargo, bypasses normal cadence, and still gives weak statistics for rare items.

The theoretical model should use Starsector runtime registries after campaign load, not parse mod folders. Active mods are already merged by the game, and mods can patch factions, mutate known lists, generate markets dynamically, or change ownership.

Ships are different:

- `TheoreticalShipSaleIndex`, `ObservedShipStockIndex`, `ShipRarityClassifier`, `ShipCatalogDiagnostics`, and `StockReviewShipCatalogDebugRows` exist.
- This is diagnostic/future groundwork only.
- Ships are not currently purchasable through Fixer's Market.
- The hidden debug UI is gated by JVM property `wp.debug.shipCatalogView=true`.
- Diagnostic logging uses `wp.debug.shipCatalog`.

Do not wire ship candidates into normal stock rows or purchase flow without a deliberate design pass.

## Trade execution model

Pending trades are signed staged intent:

- positive quantity means buy;
- negative quantity means sell;
- queueing a buy unwinds opposite queued sells first, and vice versa;
- reset actions clear intent only;
- `Confirm Trades` is the mutation boundary.

Execution order is intentional:

1. sells;
2. explicit source buys, if seller-specific rows are ever deliberately reintroduced;
3. generic cheapest buys.

Important trade owners:

- `StockReviewPendingTrades`: staged plan state.
- `StockReviewTradePlanner`: row action planning.
- `StockReviewTradeController`: UI-side planning mutations.
- `StockReviewQuoteBook`: seller lists, allocations, sell prices, cargo-space estimates, and line quotes.
- `StockReviewPortfolioQuote`: whole-plan pricing when several rows contend for the same sources.
- `TradeMoney`: long-money arithmetic and overflow safety.
- `StockPurchasePlan`: cheapest-source buy plan.
- `StockPurchaseChecks`: preflight and failure messages.
- `StockPurchaseMarketSources`: Local/Sector source discovery and sell-target selection.
- `StockPurchaseExecutor`: mutation, rollback journal, and credit/cargo changes.
- `StockMarketTransactionReporter`: best-effort post-commit Starsector transaction callbacks.

Transaction callbacks should remain post-commit side effects. Do not report a player market transaction before cargo/credit mutation has succeeded, because rollback should not need to undo external callback effects.

The still-important runtime proof is rollback fault validation with `wp.debug.failTradeStep` / Luna setting paths. Build checks cannot prove that touched cargo, credits, and transaction listeners behave correctly under Starsector runtime.

## Stock-review UI

The stock-review UI is a custom campaign-dialog surface built on shared `WimGui*` helpers and stock-review-specific rows/renderers.

Important shared UI files:

- `WimGuiControls`
- `WimGuiButtonSpec`
- `WimGuiButtonColors`
- `WimGuiButtonPoller`
- `WimGuiModalListRenderer`
- `WimGuiScroll`
- `WimGuiTooltip`
- `WimGuiToggleHeading`

Important stock-review UI files:

- `StockReviewPanelPlugin`
- `StockReviewRenderer`
- `StockReviewStyle`
- `StockReviewUiController`
- `StockReviewFooterRenderer`
- `StockReviewListModel`
- `StockReviewReviewListModel`
- `StockReviewTradeRowCells`
- `StockReviewItemInfoRows`
- `StockReviewItemTooltip`
- `StockReviewPlainIconPlugin`
- `StockReviewWeaponIconPlugin`

The current modal shape is deliberate:

- trade mode should use almost the full safe screen area;
- review mode should be compact/content-focused;
- filter mode should be compact and left-sized;
- color debug is a separate diagnostic mode;
- hidden ship catalog debug mode is gated by JVM property.

Row icons and tooltip icons are intentionally different:

- row icons are plain square item sprites, no mount-type motif;
- weapon tooltip icons use the larger vanilla-inspired mount/type motif;
- fighter LPC row icons should use the LPC/fighter sprite where available;
- weapon tooltip motifs use Starsector-like colors and shapes, but are public-API/procedural approximations, not copied vanilla code.

The UI has several known fragile areas:

- custom panel button events may not reliably arrive through `buttonPressed(...)`; keep the poller fallback;
- row height/gap/list viewport math can cause scroll gaps or clipping if changed casually;
- text clipping depends on fixed cell widths and Starsector font behavior;
- tooltips can clip the screen if heights/padding are changed without runtime testing;
- helper class movement can break live class validation if validators are not kept current.

When changing layout, use `StockReviewStyle` and shared `WimGui` primitives rather than ad hoc constants in row renderers.

## Patched badges

The patched badge path has two halves that must match:

- the live mod jar must include the private bridge/helper/updater classes;
- the patched core jar must embed/call the helper expected by that live jar.

The red `E` badge regression came from a mismatch: a patched core jar was active while the live mod jar did not include the private bridge/update path. The helper now returns no badge until the count bridge is ready, which is better than displaying error sprites everywhere.

Use the private deploy wrapper for this path:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-private-badges.ps1 -StarsectorDir "X:\Path\To\Starsector"
```

That wrapper builds with `-PrivateBadge`, verifies private classes are present, refreshes the embedded core helper through the patcher, validates the patch, and deploys with `-AllowPrivateBadgeJar`.

Hard rules:

- do not ship a prepatched `starfarer_obf.jar`;
- do not call campaign APIs from `WeaponsProcurementBadgeHelper`;
- do not put private badge settings/publication back into the public-safe config path;
- do not use clean deploy for private badge validation unless you explicitly want to remove private badge classes from the live jar.

## Recent work context

The living `.agent/BRIEF.md` may lag after rapid work. At this snapshot, recent commits include:

- `4eeff6c` `Fix private badge bridge deployment`: added `tools/deploy-private-badges.ps1`, hardened `WeaponsProcurementBadgeHelper`, and documented private badge deployment.
- `25512bd` `Clean up stock review layout and market access`: added plain row icons, wing icons, compact filter/review layouts, near-fullscreen trade layout, Sector black-market support, and shared submarket access gating.
- `b85e7a1` `Add gated ship catalog debug view`: added `wp.debug.shipCatalogView`, a hidden `Ships` debug mode, and read-only ship catalog rows. This is diagnostic only.

Earlier May work that explains the current project shape:

- Kotlin/Gradle migration completed in chunks, preserving mod id, jar path, entrypoint, config keys, and trade behavior.
- Package hierarchy now groups config, lifecycle, stock, trade, UI, and private badge ownership.
- Fixer theoretical catalog for weapons/LPCs replaced the observation-only catalog.
- Vanilla-style weapon tooltip work approximated the cargo weapon tooltip with public APIs and procedural icon motifs.
- Trade execution was hardened with whole-portfolio quotes, final source-stock preflight, rollback journaling, and post-commit transaction reporting.
- Public release separation was established around curated clean output for `Shattersphere-Mods`.

## Validation posture

Validation has several layers. Do not overstate what any single layer proves.

Docs-only:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1 -IncludePrivateDocs
git diff --check
```

Normal runtime/source:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -StarsectorDir "C:\Games\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-jar-classes.ps1 -JarPath .\jars\weapons-procurement.jar -Label Repo
git diff --check
```

Deploy/runtime:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1 -StarsectorDir "C:\Games\Starsector"
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1 -StarsectorDir "C:\Games\Starsector"
```

Private badge:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -StarsectorDir "C:\Games\Starsector" -PrivateBadge
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-total-badges.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-cargo-stack-view-patch.ps1
```

Public export/release:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\export-public.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1
```

The deploy script queues a background publish if Starsector locks the live jar. A queued deploy is not live validation. Close/restart Starsector, let the queue finish, and verify the live artifact before judging runtime behavior.

Manual checks still matter most for:

- opening Make Trades and Review Trades;
- Local buy/sell;
- Sector Market buy with and without black markets;
- Fixer's Market virtual buy;
- faction/military access gating;
- row icons and tooltip icons;
- filters/review layout;
- hidden ship debug view if enabled;
- patched badge private path;
- rollback fault hooks.

## Public release notes

The eventual public release should be clean and curated. Before touching public output:

- read `.agent/PUBLIC_RELEASE.md`;
- run the export script rather than manual copying;
- leak-scan for private docs, agent references, local paths, deploy queues, patcher tools, and private badge traces;
- keep README/CONFIG/PACKAGING public-facing;
- update changelog/version only for meaningful user-facing release changes.

Do not publish this archived handover. It contains private process notes, local paths, and internal history.

## Practical takeover path

For a new agent resuming ordinary code work:

1. Start with `git status --short --branch` and `git log --oneline -n 10`.
2. Read `AGENTS.md`, `.agent/BRIEF.md`, and `HANDOVER.md`.
3. Read `.agent/archive/INDEX.md`, then only the relevant deep dive:
   - UI/layout/tooltips: `starsector-ui.md` and maybe `vanilla-weapon-tooltip-bytecode.md`.
   - source/trade/Fixer: `trade-and-sources.md`.
   - badges: `patched-badges.md`.
   - deploy/runtime proof: `runtime-validation.md`.
4. Inspect the exact files for the requested surface. Avoid broad archive/code sweeps unless the task is architecture-level.
5. For runtime source changes, build first, deploy at most once after a cohesive batch, and report queued deploys honestly.
6. For docs-only work, run doc links plus `git diff --check`; do not deploy.
7. Commit and push once for a cohesive completed chunk if validation passes and the repo policy still expects it.

For the very next useful runtime check after this snapshot:

- close Starsector so queued deploys can publish;
- restart Starsector;
- verify private badges if testing the patched path;
- open the `F8` popup and test Local/Sector/Fixer behavior;
- test the recent UI changes around row icons, filter modal sizing, review modal sizing, storage-cell width, black-market Sector access, and faction access gating;
- use `wp.debug.shipCatalogView=true` only when intentionally inspecting the hidden ship catalog diagnostics.
