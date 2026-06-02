# Architecture

Stable public-safe architecture overview. Private operational context belongs in `HANDOVER.md`, `.agent/HANDOVER.md`, and `.agent/archive/`.

## System Purpose

Enhanced Trading is a Starsector `0.98a` mod that opens a compact market trade popup for reviewing weapon, fighter LPC, and local ship stock; planning buys/sells; reviewing pending trades; and confirming the trade.

## Main Components

| Component | Responsibility | Key files | Notes |
|---|---|---|---|
| Mod plugin and campaign entry | Register hotkey/open scripts and optional market dialog entry | `src/main/kotlin/weaponsprocurement/plugins`, `data/campaign/rules.csv` | Main plugin is `WeaponsProcurementModPlugin`. |
| Config and settings | LunaLib settings, stock config, market blacklist, debug hooks | `src/main/kotlin/weaponsprocurement/config`, `data/config`, `CONFIG.md` | Keep debug hooks disabled by default. |
| Stock and source models | Item keys, market stock, source modes, desired stock, catalog/fixer data | `src/main/kotlin/weaponsprocurement/stock`, `src/main/kotlin/weaponsprocurement/trade` | Item keys use `W:<weaponId>` and `F:<wingId>`. |
| Trade planning and execution | Pending plans, quotes, source allocation, rollback handling | `src/main/kotlin/weaponsprocurement/trade` | Sells execute before buys. Rollback behavior needs runtime evidence. |
| UI and rendering | Stock review popup, table/grid rendering, filters, tooltips | `src/main/kotlin/weaponsprocurement/ui` | Ship trading is local-only and UI polish is runtime-sensitive. |
| Build/deploy/release | Build jar, deploy live mod, validate boundaries, curate public export | `build.ps1`, `build.gradle.kts`, `tools/`, `.agent/PUBLIC_RELEASE.md` | Public output is curated, not mirrored. |

## Data Flow / Lifecycle

1. Starsector loads `enhanced_trading` and `WeaponsProcurementModPlugin`.
2. The plugin registers campaign scripts for hotkey/dialog entry and catalog observation.
3. The player opens a market-backed context and presses the configured popup hotkey, default `F8`.
4. The UI snapshots local/remote stock, player inventory, desired stock, and configuration.
5. The player queues trades; quote/planning code calculates affordability, source allocations, warnings, and review rows.
6. Confirmation executes sells first, then buys, mutating the appropriate local/remote/virtual sources.
7. Deploy/package scripts build and copy only clean public-safe runtime surfaces.

## Important Boundaries

- Public config boundary: `mod_info.json`, `data/config/`, `CONFIG.md`, and Luna setting keys.
- Runtime integration boundary: Starsector API calls, UI layout, classloader behavior, market cargo mutation, and LunaLib settings.
- Persistence boundary: observed catalog data, item keys, and any campaign memory/state touched by trade flows.
- Public release boundary: public export must exclude `.agent/`, `AGENTS.md`, `HANDOVER.md`, `PLANS.md`, local paths, private archives, and bytecode/badge-only traces.
- Shared-library boundary: Shatter Lib changes require `.agent/SHARED_LIBRARIES.md` edit-gate approval; consumer-specific behavior stays in Enhanced Trading.

## Design Decisions

| Decision | Status | Rationale | Evidence |
|---|---|---|---|
| Clean `F8` popup is the public/default product | active | Keeps the public mod independent of patched-core cargo badges | `AGENTS.md`, `.agent/BRIEF.md` |
| Cargo-cell badges are standalone | active | Badge/core patching ownership moved to `D:\Sean Mods\Weapon Badges` | `AGENTS.md`, `.agent/archive/deep-dives/patched-badges.md` |
| Ship trading is local-only for now | active | Remote ship identity/source draining/pricing needs a separate design | `PLANS.md`, `.agent/BRIEF.md` |
| Public output is curated | active | Private repo contains agent docs, local paths, and historical private material | `.agent/PUBLIC_RELEASE.md` |

## Known Architectural Risks

- Runtime UI correctness cannot be proven by compile checks alone.
- Starsector classloading can keep stale jar/class state until restart.
- Trade rollback and cargo/credit mutation safety need in-game fault-validation evidence.
- Adding Shatter Lib as a dependency is a compatibility and release-packaging change, not a docs-only update.
