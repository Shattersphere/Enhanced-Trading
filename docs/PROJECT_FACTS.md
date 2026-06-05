# Project Facts

Source of truth for exact project facts. Do not guess. Unknowns stay `TODO[unknown]` until confirmed by user input or repo evidence.

## Identity

| Field | Value | Evidence |
|---|---|---|
| Project name | `Enhanced Trading` | `mod_info.json` |
| Repo slug | `Enhanced-Trading` | `git remote -v` origin URL |
| Project type | Starsector mod | `mod_info.json`, `build.gradle.kts` |
| Target app/game/platform | Starsector | `mod_info.json` |
| Target version | `0.98a` | `mod_info.json` `gameVersion` |
| Optional profile applied | Starsector mod workflow | `AGENTS.md`, `.agent/SHARED_LIBRARIES.md` |

## Toolchain

| Field | Value | Evidence |
|---|---|---|
| Primary language | Kotlin plus legacy Java | `build.gradle.kts` source sets |
| Runtime/interpreter/JDK/SDK | Java 17 target, Kotlin JVM `2.1.20` | `build.gradle.kts` |
| Package/build tool | Gradle wrapper via `build.ps1` | `build.ps1`, `gradlew.bat`, `build.gradle.kts` |
| Required external tools | PowerShell, Python for maintenance scripts, Starsector install for runtime builds | `build.ps1`, `tools/*.ps1`, `scripts/*.py` |

## Repository And Git Policy

| Field | Value | Evidence |
|---|---|---|
| Repository visibility/trust | trusted private work repo; public output is curated separately | `AGENTS.md`, `.agent/PUBLIC_RELEASE.md` |
| Git finalization mode | `commit-and-push` | personal/private repo default plus trusted `origin` |
| GitHub owner/namespace | `Shattersphere` | `git remote -v` |
| Remote URL | `https://github.com/Shattersphere/Enhanced-Trading.git` | `git remote -v` |
| Default branch | `main` | `git status --short --branch` |

If `commit-and-push` is active but no usable trusted remote exists, commit locally when safe and report the push blocker. Public `Shattersphere-Mods` export or publishing is separate explicit release work.

## Commands

Commands here are authoritative. If a command is unknown, leave it unknown rather than inventing one.

| Purpose | Command | Working directory | Notes |
|---|---|---|---|
| List files | `rg --files` | repo root | Use targeted search before opening large docs or archives. |
| Build | `powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1` | repo root | Requires `STARSECTOR_DIRECTORY` or `-StarsectorDir`. |
| Build without clean | `powershell -NoProfile -ExecutionPolicy Bypass -File .\build.ps1 -SkipClean` | repo root | Useful for local iteration only. |
| Validate local build env | `.\gradlew.bat --no-daemon validateLocalBuildEnvironment -PstarsectorDir=<path>` | repo root | Checks Starsector core jars plus LunaLib/LazyLib jars. |
| GUI style validation | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-gui-button-style.ps1` | repo root | Runtime/source UI guard. |
| Kotlin migration validation | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-kotlin-migration.ps1` | repo root | Source/package boundary guard. |
| Config contract validation | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-config-contracts.ps1` | repo root | Luna/settings, stock JSON, blacklist JSON, sort alias, and trade money guard. |
| Documentation link validation | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1` | repo root | Public docs by default. |
| Private documentation link validation | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-doc-links.ps1 -IncludePrivateDocs` | repo root | Includes agent/private docs. |
| Template/doc-system hygiene | `python scripts/check-template-state.py --initialized` | repo root | Run after template sync or doc-system edits. |
| Update repo map | `python scripts/update-repo-map.py --write` | repo root | Run after tracked structure changes. |
| Deploy | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1` | repo root | In scope only for runtime/package changes. |
| Deploy status | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1 -Status` | repo root | No-build queue/status/blocker report. |
| Deploy parity check | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\deploy-live-mod.ps1 -CheckOnly -RequireCurrent` | repo root | Cheap source/live clean-package parity. |
| Live GUI class validation | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\validate-live-gui-classes.ps1` | repo root | Run after runtime deploys. |
| Public export | `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\export-public.ps1` | repo root | Public release/export only. |

## Paths

| Purpose | Path | Notes |
|---|---|---|
| Source root | `src/main/kotlin`, `src/weaponsprocurement` | Kotlin plus legacy public Java. |
| Campaign rules/data | `data/` | Runtime mod data loaded by Starsector. |
| Config | `data/config/`, `CONFIG.md` | Stock JSON, Luna settings, blacklist docs. |
| Assets | `graphics/` | Runtime mod assets when present. |
| Build output | `jars/enhanced-trading.jar`, `build/` | Generated; do not hand-edit. |
| Deploy target | `C:\Games\Starsector\mods\Enhanced Trading` | Use `tools/deploy-live-mod.ps1`, never hand-copy by default. |
| Public export output | `build/public-export` | Must exclude private docs and agent materials. |
| Shatter Lib checkout | `D:\Sean Code Projects\Starsector Projects\Shatter Lib` | Shared-library workflow only. |

## Dependencies

| Dependency | Version/source | Required for | Notes |
|---|---|---|---|
| Starsector API/core jars | configured Starsector `starsector-core` | Compile/runtime API | Resolved from `STARSECTOR_DIRECTORY` or `-PstarsectorDir`. |
| Kotlin stdlib | `2.1.20` compile-only | Kotlin runtime support | Build file compiles against it; Starsector runtime dependency handling also relies on installed dependency mods. |
| LazyLib | installed Starsector mod, mod id `lw_lazylib` | Required dependency | Declared in `mod_info.json`; compile jars resolved from installed mod. |
| LunaLib | installed Starsector mod, mod id `lunalib` | Required dependency/settings UI | Declared in `mod_info.json`; compile jars resolved from installed mod. |
| Shatter Lib | installed Starsector mod, mod id `shatter_lib` | Required shared UI/runtime helpers | Declared in `mod_info.json`; compile jars resolved from installed mod. |

## Shared Libraries

Declare external libraries here before agents inspect or edit them. Use `.agent/SHARED_LIBRARIES.md` for the workflow and any library-specific note for detailed rules.

| Library | Role | Version/source | Read authorized | Edit authorized | Location / resolution | Validation requirements |
|---|---|---|---:|---:|---|---|
| Shatter Lib | Shattersphere shared Starsector utility library for reusable GUI models, adapters, diagnostics, validation, and storage/config primitives | Required build/runtime dependency declared in `mod_info.json` and `build.gradle.kts` | Yes, when the task may reuse or extract generic Starsector library behavior | Yes, only through `.agent/SHARED_LIBRARIES.md` edit gate | `D:\Sean Code Projects\Starsector Projects\Shatter Lib`; build resolves installed mod id `shatter_lib` | Validate Shatter Lib under its own docs, and validate this consumer integration where feasible. If the edit gate is not satisfied, write a task packet instead. |
| LazyLib | Third-party Starsector helper/dependency library | Installed dependency mod | Yes, only as dependency/API reference when relevant | No | Resolve from this repo's build files, `mod_info.json`, dependency docs, or configured Starsector install | Treat as external API. Do not edit, fork, patch, vendor, or reconfigure. |
| LunaLib | Third-party Starsector settings/UI dependency library | Installed dependency mod | Yes, only as dependency/API reference when relevant | No | Resolve from this repo's build files, `mod_info.json`, dependency docs, or configured Starsector install | Treat as external API. Do not edit, fork, patch, vendor, or reconfigure. |

Enhanced Trading consumes Shatter Lib at build/runtime. Keep `build.gradle.kts`, `mod_info.json`, this section, `.agent/SHARED_LIBRARIES.md`, and public install/config docs in sync when dependency handling changes.

## Compatibility And Persistence Risks

- Persistent IDs/names: `enhanced_trading`, `weaponsprocurement.plugins.WeaponsProcurementModPlugin`, item keys `W:<weaponId>` and `F:<wingId>`.
- Config schema/settings: Luna settings under `data/config/LunaSettings.csv`; stock/blacklist JSON in `data/config/`; debug hooks must stay disabled by default.
- File formats/generated metadata: Starsector CSV/JSON data files, jar package contents, public export manifest.
- Save/profile/user data impact: market observations, pending trade behavior, and any memory/persistent-state keys can affect campaign behavior.
- Public API/CLI/config compatibility: `build.ps1`, deploy scripts, validation scripts, `mod_info.json`, public docs, and curated export behavior.

## Known-Good Baseline

- Commit: `daa8c7c36c81b55517ddc5fee78a10a32374c317`
- Build/check status: `TODO[unknown]` for this sync until checks are run.
- Runtime/manual validation: not run for docs/template sync.
- Date: 2026-06-03

## Open Fact Questions

- TODO[unknown]: confirm whether the trusted private `origin` is private on GitHub from hosting metadata, if needed.
