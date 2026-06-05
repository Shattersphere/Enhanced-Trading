# Enhanced Trading Brief

Last updated: 2026-06-06

## Current State

Enhanced Trading is a Starsector `0.98a` mod for reviewing weapon and fighter LPC stock, planning buys/sells, and confirming trades from market dialogs or market-backed storage dialogs.

The primary product path is the clean trade popup, opened with the LunaLib-configurable hotkey that defaults to `F8`. Cargo-cell badges now live in the standalone private `D:\Sean Mods\Weapon Badges` mod.

The source tree is fully migrated to Gradle/Kotlin. `build.ps1` remains the normal entry point and delegates to the Gradle wrapper; LazyLib is a required dependency because it supplies the Kotlin runtime in Starsector.

The item-trading popup path is solid and functional as the current baseline. Ship trading is local-only behind the `Trade: Items` / `Trade: Ships` toggle; the current 4-column by 5-row ship grid layout is the user-confirmed solid baseline to preserve if this GUI regresses.

For modder handoff, `HANDOVER.md` is the deep onboarding guide and `.agent/ARCHITECTURE_MAP.md` is the diagram-first map of the runtime, UI, trade, and build/deploy surfaces. Read those before large feature work or ownership handoff.

The repo has been synced with the generic template doc system. Exact facts now live in `docs/PROJECT_FACTS.md`, validation commands in `docs/CHECKS.md`, and generated orientation in `docs/REPO_MAP.md`.

## Known-Good Source State

- Current branch: `main`
- Known-good source commit: `f772d5c7577f3966090e1e18d4745f464fc4bfad`
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
- Runtime rollback fault validation still needs in-game evidence.
- Rollback diagnostics now emit structured `WP_STOCK_REVIEW_ROLLBACK` records; use `tools/analyze-trade-rollback-diagnostics.ps1` after a forced-failure run to verify restored cargo counts and credits.
- Weapon Badges is standalone in `D:\Sean Mods\Weapon Badges`; do not reintroduce cargo-cell badge assets, core patching, or badge count publishing here.
- Starsector classloading can keep stale jar/class state until restart.
- Luna settings, data/config files, graphics, and metadata matter; jar parity alone is not sufficient for data-heavy changes.
- Public release to `Shattersphere-Mods` must be curated. Do not mirror this private repo because it contains agent docs and local/private references.
- Live deploy/runtime validation is currently blocked until the installed Shatter Lib jar has the API classes required by the current Enhanced Trading jar: `ShatterItemTooltipContext.class` and `ShatterTooltipContextLine.class`.

## Next Best Step

For code/runtime work, inspect the short active `PLANS.md`, `docs/PROJECT_FACTS.md`, `docs/CHECKS.md`, and the relevant archive deep dive through `.agent/archive/INDEX.md`. Treat archives as historical unless their status says active-reference. For shared-library work, read `.agent/SHARED_LIBRARIES.md` before inspecting Shatter Lib. For public release/export work, start with `.agent/PUBLIC_RELEASE.md`. For docs-only work, use the docs-only checks and avoid deployment.
