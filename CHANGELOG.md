# Changelog

## Unreleased

- Added Gradle/Kotlin build support while preserving the existing `build.ps1` entry point.
- Added LazyLib as a required dependency for Kotlin runtime support.
- Moved cargo-cell badge support out of Enhanced Trading and kept this mod focused on the stock-review popup.
- Auto-trade now notifies on arrival and executes in the market screen: hailing a market posts a compact campaign-feed summary of how many items it would sell and buy, and trades run when you open a submarket (using its live stock) rather than silently on arrival.
- Replaced the black-market routing toggles with "Allow suspicion when selling" and "Allow suspicion when buying", which permit black-market auto-trades while your transponder is on. With the transponder off the black market is used automatically. Auto-buys apply only to the submarket you open; auto-sells route across markets per these toggles.
- Auto-trade now works at any reachable submarket, not just the open and black markets: military markets, faction-gated, and modded submarkets are scanned for pending buys on arrival (respecting each market's own access rules and your transponder state) and auto-buy when you open them.
- Auto-bought hullmods now learn through the game's own right-click action, so the acquisition sound, "Acquired hull mod" message, and character-data update match a manual right-click exactly.
- Added a "Confirm before auto-trading" mode: when enabled, auto-trade no longer runs automatically. The arrival summary names the pending trades, and opening a submarket with pending trades shows a simple "Press [key] to confirm auto-trade" prompt (execute hotkey defaults to Z, configurable in LunaLib). Enable it per-save via the "Confirm before auto-trading" toggle in the auto-rules editor (the LunaLib setting only sets the default for new saves).
- Replaced credit-floor and per-item sell-above / buy-below numeric controls with editable text fields. Leave a field blank to clear the rule.

## 0.2.0 - 2026-05-09

- Rebranded the mod as Enhanced Trading.
- Added the clean `F8` stock-review popup for market and storage dialogs.
- Added fighter LPC / wing support alongside weapons.
- Added staged buy/sell planning, Review Trades, and confirm execution.
- Added Local, Sector Market, and Fixer's Market source modes.
- Added observed Fixer's Market catalog learning from real market stock.
- Added per-item desired-stock overrides and Sector/Fixer blacklist config.
- Added trade rollback journaling and overflow-safe trade-money totals.
- Added packaging, deployment, validation, and non-proprietary CI checks.
