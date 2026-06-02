# Template Sync Prompt

This repository has already been initialized from the generic agent template. Use this only when rechecking template specialization.

```text
Sync Enhanced Trading with the generic Codex template.

Goal:
- Keep Starsector-specific repo facts in docs/PROJECT_FACTS.md.
- Keep validation commands in docs/CHECKS.md.
- Preserve existing private docs, archives, public-release boundaries, and Starsector constraints.
- Verify .agent/SHARED_LIBRARIES.md and docs/PROJECT_FACTS.md before inspecting or changing Shatter Lib.

Required steps:
1. Start with git status --short --branch.
2. Compare against D:\Sean Code Projects\General Projects\Generic Template Repo.
3. Add only useful template docs/scripts, customized with repo evidence.
4. Run python scripts/update-repo-map.py --write after tracked structure changes.
5. Run python scripts/check-template-state.py --initialized and relevant doc checks.
6. Do not edit Shatter Lib unless .agent/SHARED_LIBRARIES.md edit gate is satisfied; write a Shatter Lib task packet instead if it is not.
```
