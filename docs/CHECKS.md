# Checks

A lightweight validation command menu. Use the smallest check that gives useful evidence for the task. Do not run every check by default.

## Command Menu

| Check | Command | Use when | Evidence to report | Cost/risk |
|---|---|---|---|---|
| Template/project hygiene | `python scripts/check-template-state.py --initialized` | Template sync, doc-system edits, governance file changes | Pass/fail and warnings | low |
| Repo map freshness | `python scripts/update-repo-map.py --write` then review diff | Tracked structure changes | Whether `docs/REPO_MAP.md` changed | low |
| Public doc links | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1` | Public docs changes | Pass/fail | low |
| Private doc links | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1 -IncludePrivateDocs` | Agent/private docs changes | Pass/fail | low |
| Diff whitespace | `git diff --check` | Any source/docs change before commit | Pass/fail | low |
| Build | `powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1` | Runtime/source changes | Build passed/failed; key error if failed | medium; requires Starsector path; accepts `-ShatterLibDir` |
| Build environment | `.\gradlew.bat --no-daemon validateLocalBuildEnvironment -PstarsectorDir=<path>` | Dependency/path checks | Resolved Starsector, dependency paths, and Shatter Lib API freshness | low/medium; requires local install |
| GUI button style | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1` | GUI/button rendering changes | Pass/fail | low |
| Kotlin/source boundary | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1` | Source/package/build boundary changes | Pass/fail and skipped jar/export checks | low/medium |
| Compatibility surfaces | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-compatibility-surfaces.ps1` | Changing ids, config keys, data paths, plugin/rule paths, validators, CI, or modernization docs | Pass/fail by protected surface | low |
| Config contracts | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-config-contracts.ps1` | Changing Luna settings, stock JSON, blacklist JSON, item-key parsing, config parsers, sort aliases, or trade money guards | Pass/fail by config contract | low |
| Fixer persistence contracts | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-fixer-persistence-contracts.ps1` | Changing Fixer's Market observed catalog storage, lifecycle gating, policy gating, theoretical reference fallback, or save-key docs | Pass/fail by save/persistence contract | low |
| Trade rollback contracts | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-trade-rollback-contracts.ps1` | Changing trade execution, forced-failure hooks, rollback diagnostics, credit/cargo mutation guards, or rollback docs | Pass/fail by rollback contract | low |
| Source semantics contracts | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-source-semantics-contracts.ps1` | Changing Local/Sector/Fixer stock collection, source-mode dispatch, virtual Fixer stock, or trade-source rehydration | Pass/fail by source-mode contract | low |
| Ship trading contracts | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-ship-trading-contracts.ps1` | Changing ship snapshot, pending ship trades, local ship buy/sell execution, or remote ship-trading gates | Pass/fail by local exact-member ship contract | low |
| Deploy status | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1 -Status` | Runtime deploy troubleshooting | Queue/lock/staging status and Shatter Lib runtime API state | low; requires Starsector path |
| Deploy parity | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1 -CheckOnly -RequireCurrent` | Runtime changes when live parity matters | Current/stale source/live state plus Shatter Lib runtime API state | medium; requires Starsector path |
| Runtime deploy | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1` | Runtime changes that need live validation | Deploy or queued deploy result | high; writes live mod target; blocks stale Shatter Lib API |
| Live GUI classes | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1` | After runtime deploys | Live jar class validation | medium |
| Public export | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\export-public.ps1` | Explicit public release/export work | Export path and leak/boundary status | medium |
| Rollback diagnostics | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\analyze-trade-rollback-diagnostics.ps1 -ExpectFailureStep after-source-removal,after-player-cargo-remove,after-player-cargo-add,after-target-cargo-add,after-credit-mutation -RequirePass` | Manual forced rollback validation | Pass/fail by failure step | high; requires runtime log evidence |

## Tier Guidance

- Fast edit: run no check or one cheap targeted check if useful.
- Standard change: run the relevant focused build/test/lint/validator for the touched surface.
- Deep/risky/release work: plan first; run focused checks during development and broader checks at the end of a cohesive batch.
- Release boundary: run source checks, package/export checks, leak checks, and any required runtime/deploy validation.

## Baseline Workflow Sources

During explicit sync with the generic template, compare against the Generic Template Repo and, only when deploy/package workflow is in scope, use the Deploy Template and Zipper Template listed in `docs/PROJECT_FACTS.md` as references. Preserve this repo's Starsector-specific deploy/export commands and validation gates.

## Evidence Rules

Report the exact check name or command family, pass/fail/partial status, key failure message if failed, and what remains unverified.

Do not claim build, deploy, runtime, or in-game evidence unless that command or manual check actually ran.

## Missing Coverage

- Runtime UI, LunaLib behavior, campaign interactions, rollback safety, and Starsector classloader behavior require in-game evidence; compile and jar parity are not enough.
- No dedicated unit-test suite is declared in the current Gradle build.
- `tools/validate-compatibility-surfaces.ps1` guards current shipped ids and documented absences; update the modernization plan before adding new Starsector data-id families.
- `tools/validate-config-contracts.ps1` is static contract coverage for Luna/JSON/source consistency, item-key parsing, blacklist matching, and trade-money guards; it does not prove LunaLib runtime UI behavior.
- `tools/validate-fixer-persistence-contracts.ps1` is static contract coverage for the current Fixer observed-catalog save key, string-map encoding, sanitization, lifecycle gating, policy denylist, and blacklist/safety gates; it does not prove save migration in a live campaign.
- `tools/validate-trade-rollback-contracts.ps1` is static contract coverage for rollback journal order, debug failure steps, diagnostic fields, and credit/cargo guards; it does not prove rollback succeeds in a live campaign.
- `tools/validate-source-semantics-contracts.ps1` is static contract coverage for Local/Sector/Fixer source separation, virtual Fixer stock, real-cargo source rehydration, and source-mode dispatch; it does not prove live market mutation.
- `tools/validate-ship-trading-contracts.ps1` is static contract coverage for local-only exact-member ship snapshots, pending ship trades, buy/sell fleet mutation order, and remote ship-trading gates; it does not prove in-game ship buy/sell behavior.
- Build validation checks required Shatter Lib API classes. If the installed mod is stale, build with `-ShatterLibDir <checkout>` for source/package proof; runtime still requires the installed mod to be current.
- Deploy status, deploy parity, and runtime deploy now inspect the installed Shatter Lib jar for required API classes; a checkout override proves build only, not live runtime dependency parity.
