# Enhanced Trading Brief

Last updated: 2026-06-07

## Current State

Enhanced Trading is a Starsector `0.98a` mod for reviewing weapon and fighter LPC stock, planning buys/sells, and confirming trades from market dialogs or market-backed storage dialogs.

The primary product path is the clean trade popup, opened with the LunaLib-configurable hotkey that defaults to `F8`. Cargo-cell badges now live in the standalone private `D:\Sean Mods\Weapon Badges` mod.

The source tree is fully migrated to Gradle/Kotlin. `build.ps1` remains the normal entry point and delegates to the Gradle wrapper; LazyLib is a required dependency because it supplies the Kotlin runtime in Starsector.

The item-trading popup path is solid and functional as the current baseline. Ship trading is local-only behind the `Trade: Items` / `Trade: Ships` toggle; the current 4-column by 5-row ship grid layout is the user-confirmed solid baseline to preserve if this GUI regresses.

Recent modernization work has focused on bounded, behavior-preserving hardening and cleanup:

- Trade and ship execution guardrails now fail closed around unsafe credit/cargo mutations, exact-member ship mutation failures, nonfinite settings/cargo-space values, post-commit transaction reports, and Fixer catalog decoding.
- Runtime dependency checks now surface stale installed Shatter Lib jars during deploy/parity workflows.
- Weapon and fighter LPC tooltip code has been split into smaller owners: `StockReviewTooltipModels`, `StockReviewTooltipIconPanelPlugin`, `StockReviewWingTooltipRenderer`, `StockReviewWingTooltipLayoutBuilder`, `StockReviewWeaponTooltipRows`, `StockReviewItemTooltipContext`, `StockReviewWeaponTooltipIconGridRenderer`, and `StockReviewWeaponTooltipTextRenderer`. `StockReviewItemTooltip` remains the narrow orchestration/legacy weapon shell.
- Fighter LPC Advanced Info rows now source real fighter hull stats through `WeaponStockRecord`; wing tooltip rows reuse the same labels to avoid stat drift between list rows and tooltips.
- Fixer live and persistent observation now share `FixerReferenceSourceSelector` for purchasable reference-source filtering and price/source-name tie-breaking.
- Remote-source sell quotes now match execution by using legal-only local sell pricing while preserving Sector black-market remote buys. Black-market submarket eligibility probing and item/ship transaction reporting now share `StockSubmarketTradeModes` for OPEN/SNEAK policy.
- Fixer updater registration now publishes Luna settings before the background catalog gate, and ship trading has local-only black-market state, ship-only open-gate availability, and confirm-time sell eligibility rechecks.

For modder handoff, `HANDOVER.md` is the deep onboarding guide and `.agent/ARCHITECTURE_MAP.md` is the diagram-first map of the runtime, UI, trade, and build/deploy surfaces. Read those before large feature work or ownership handoff.

The repo has been synced with the generic template doc system. Exact facts now live in `docs/PROJECT_FACTS.md`, validation commands in `docs/CHECKS.md`, generated orientation in `docs/REPO_MAP.md`, active work in `PLANS.md`, and parked long-horizon work in `.agent/BACKLOG.md`.

## Known-Good Source State

- Current branch: `main`
- Known-good source commit: `8d9ed91` (`Harden ship and Fixer lifecycle gates`)
- Template-sync baseline before generic doc-system specialization: `daa8c7c36c81b55517ddc5fee78a10a32374c317`
- Version in `mod_info.json`: `0.2.0`

## Collaboration Notes

- Collaborator branch `autotrade-first-draft` at `https://github.com/Shattersphere/Enhanced-Trading/tree/autotrade-first-draft` currently resolves to commit `08a67538bf0dda4ea0418b3b72e4bed2ad523ab2`. Expect merge conflicts with local work; branch was requested as a separate sibling checkout before integration.

## Operational Routing

- Exact commands, deploy target, dependency paths, Git mode, and shared-library facts live in `docs/PROJECT_FACTS.md`.
- Validation choices and evidence limits live in `docs/CHECKS.md`.
- Docs-only changes normally do not need deploy.

## Current Risks

- Ship trading is local-only in its first implementation. Sector Market and Fixer's Market ship trading remain intentionally out of scope.
- The ship grid layout is a good reference baseline; the public-API ship tooltip still needs in-game visual acceptance against the vanilla ship buy/sell screen after layout polish.
- Tooltip cleanup remains source/static validated only. In-game visual proof is still required for weapon, wing, and ship tooltip layout acceptance.
- Black-market eligibility probing is statically aligned with transaction-report trade modes, but in-game/API proof is still needed for vanilla and modded black-market submarket behavior.
- Ship-only open gating, ship black-market toggling from Fixer's item source, and stale ship-sell rejection are source/static validated only; in-game acceptance is still needed.
- Runtime rollback fault validation still needs in-game evidence.
- Rollback diagnostics now emit structured `WP_STOCK_REVIEW_ROLLBACK` records; use `tools/analyze-trade-rollback-diagnostics.ps1` after a forced-failure run to verify restored cargo counts and credits.
- Weapon Badges is standalone in `D:\Sean Mods\Weapon Badges`; do not reintroduce cargo-cell badge assets, core patching, or badge count publishing here.
- Starsector classloading can keep stale jar/class state until restart.
- Luna settings, data/config files, graphics, and metadata matter; jar parity alone is not sufficient for data-heavy changes.
- Public release to `Shattersphere-Mods` must be curated. Do not mirror this private repo because it contains agent docs and local/private references.
- Live deploy/runtime validation is currently blocked until the installed Shatter Lib jar has the API classes required by the current Enhanced Trading jar: `ShatterItemTooltipContext.class` and `ShatterTooltipContextLine.class`.
- Generic template sync was rechecked against the current uncommitted template worktree on 2026-06-07. Template hygiene passed; the repo already had the current script/doc-budget changes, and the only imported change was routing cleanup for `.agent/DOC_SYSTEM.md` and `.agent/BACKLOG.md`.

## Next Best Step

For code/runtime work, inspect the short active `PLANS.md`, `docs/PROJECT_FACTS.md`, `docs/CHECKS.md`, and the relevant archive deep dive through `.agent/archive/INDEX.md`. Treat archives as historical unless their status says active-reference. Recommended next runtime step is to resolve the stale installed Shatter Lib dependency, then deploy/parity-check and visually smoke weapon, wing, and ship tooltips before claiming tooltip acceptance. For shared-library work, read `.agent/SHARED_LIBRARIES.md` before inspecting Shatter Lib. For public release/export work, start with `.agent/PUBLIC_RELEASE.md`. For docs-only work, use the docs-only checks and avoid deployment.
