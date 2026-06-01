# Shared Libraries Workflow

Read this only when a task may use, inspect, or improve a declared shared library outside this repo.

## Declared Starsector Libraries

| Library | Role | Read allowed | Edit allowed | Location / resolution | How to engage |
|---|---|---:|---:|---|---|
| Shatter Lib | Shattersphere shared Starsector utility library for reusable GUI models, helper functions, runtime adapters, diagnostics, validation, and storage/config primitives. | Yes, when the task may reuse or extract generic Starsector library behavior. | Yes, only through the edit gate below. | `D:\Sean Code Projects\Starsector Projects\Shatter Lib` | Read Shatter Lib `AGENTS.md`, `docs/API_STABILITY.md`, then `docs/LIBRARY_INDEX.md`; use `docs/LIBRARY_SYMBOLS.md` only for exact declaration lookup. |
| MagicLib | Third-party Starsector framework/dependency library. | Yes, only as dependency/API reference when this repo declares or uses MagicLib. | No. | Resolve from this repo's build files, `mod_info.json`, dependency docs, or the configured Starsector install. | Treat as external API. Do not modify MagicLib files or repos. |
| LazyLib | Third-party Starsector helper/dependency library. | Yes, only as dependency/API reference when this repo declares or uses LazyLib. | No. | Resolve from this repo's build files, `mod_info.json`, dependency docs, or the configured Starsector install. | Treat as external API. Do not modify LazyLib files or repos. |
| LunaLib | Third-party Starsector settings/UI dependency library. | Yes, only as dependency/API reference when this repo declares or uses LunaLib. | No. | Resolve from this repo's build files, `mod_info.json`, dependency docs, or the configured Starsector install. | Treat as external API. Do not modify LunaLib files or repos. |

## Use Rule

Prefer consuming shared libraries through their public APIs instead of copying code into this repo. Consumer-specific glue, IDs, settings keys, save schemas, assets, UI copy, gameplay policy, local paths, and workflow behavior stay in this repo.

## Shatter Lib Edit Gate

Edit Shatter Lib only when all are true:

1. Existing Shatter Lib APIs do not already solve the problem.
2. The change is a generic fix or improvement that could plausibly serve more than one current/future Starsector mod.
3. A consumer-side adapter would not be simpler, safer, or less disruptive.
4. The change avoids this repo's package names, IDs, settings keys, schemas, UI copy, assets, local paths, gameplay policy, and workflow behavior.
5. Validation covers Shatter Lib and, where feasible, this consumer integration.

## Cross-Repo Workflow

- Keep this repo's diff and Shatter Lib's diff separate.
- Follow each repo's own `AGENTS.md`, `docs/CHECKS.md`, and Git finalization mode.
- Commit and push each repo independently when that repo's rules call for it.
- Final reports must name both repos' validation, commit hash, branch, push status, and unverified integration behavior.

## External Dependency Rule

MagicLib, LazyLib, and LunaLib are read-only dependency libraries for this workflow. Inspect their public docs/API only when this repo actually uses or declares them. Do not edit, fork, patch, vendor, or reconfigure those libraries unless the user gives separate explicit instructions.
