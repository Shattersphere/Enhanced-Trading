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
3. Use the baseline resources listed in docs/PROJECT_FACTS.md only as references: Generic Template Repo for governance, Deploy Template for deploy workflow ideas, Zipper Template for package/export workflow ideas, and targeted General Archives notes for reusable lessons.
4. Add only useful template docs/scripts, customized with repo evidence.
5. Keep frequently-read current/workflow docs under the repo budget; route active work to PLANS.md, parked work to .agent/BACKLOG.md, stable facts to docs, and evidence-heavy history to .agent/archive/.
6. Run python scripts/update-repo-map.py --write after tracked structure changes.
7. Run python scripts/check-template-state.py --initialized and relevant doc checks.
8. Do not edit Shatter Lib unless .agent/SHARED_LIBRARIES.md edit gate is satisfied; write a Shatter Lib task packet instead if it is not.
```
