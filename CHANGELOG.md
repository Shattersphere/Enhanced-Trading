# Changelog

## Unreleased

- Added Gradle/Kotlin build support while preserving the existing `build.ps1` entry point.
- Added LazyLib as a required dependency for Kotlin runtime support.
- Moved cargo-cell badge support out of Enhanced Trading and kept this mod focused on the stock-review popup.
- Reworked auto-trade black-market routing: when a black-market toggle is on, the black market is checked first and the open market still handles items the black market refuses; previously the open market was skipped entirely.
- Auto-trade now always routes through the black market when the transponder is off. Removed the separate "Use black market with transponder off" toggle.
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
