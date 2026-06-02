# Code Quality

Generic repo-local standards. `AGENTS.md` owns operating rules; Starsector runtime/deploy constraints may override this file.

## Defaults

- Prefer small, reviewable patches.
- Preserve public behavior, APIs, config formats, persistence, and compatibility unless the task requires change.
- Use clear names and focused functions/classes/modules.
- Keep parsing, validation, persistence, rendering/presentation, mutation, and side effects separated where practical.
- Prefer explicit state ownership over hidden globals.
- Fail fast. Do not add silent fallbacks, empty catches, broad exceptions, or generic defaults unless there is explicit recovery logic.
- Do not introduce dependencies, scripts, files, or build steps unless they solve a real problem.

## Duplication And Abstraction

- Apply DRY when duplication can drift or cause inconsistent fixes.
- Keep local duplication when a helper would obscure simple code, cross fragile runtime boundaries, or force unlike surfaces together.
- When fixing one repeated pattern, inspect siblings for the same issue before stopping.
- Put consumer-specific IDs, settings, schemas, assets, UI copy, gameplay policy, and one-off glue in this repo, not Shatter Lib.

## Starsector Runtime Care

- Treat per-frame, per-render, input-polling, and UI rebuild paths as hot paths.
- Treat classloader/reflection/API-adapter boundaries as runtime-risky even when compilation passes.
- Do not use obfuscated vanilla classes directly in public-safe implementation paths.
- Keep dangerous validation and forced-failure hooks disabled by default.

## Comments

Comment why, not obvious mechanics. Useful comment topics include invariants, failure modes, compatibility constraints, performance tradeoffs, reflection/classloader boundaries, generated-data assumptions, migration risks, and deploy/release invariants.

## Tests And Validators

- Add or update tests/checks when they provide durable signal.
- Prefer validators/static checks over full tests when they give the same signal more cheaply.
- Do not weaken tests or validators to pass.
- If useful coverage is unavailable or flaky, document the gap in `docs/CHECKS.md` or `.agent/PLAN.md`.
