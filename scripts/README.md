# Scripts

Maintenance/setup helpers. Ordinary code changes should not need most of these.

## Review Archive

```powershell
.\zip_review_code.bat
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\create_review_code_zip.ps1 -RepoRoot . -OutputDir .\zips
```

The batch file is the normal entrypoint. Direct invocation is useful for diagnostics or custom `-MaxFileSizeMb`.

## Template/project Hygiene

```powershell
python scripts/check-template-state.py
python scripts/check-template-state.py --initialized
```

The default mode checks package hygiene and doc-system expectations. `--initialized` also fails common unfilled template placeholders.

## Repo Map Helper

```powershell
python scripts/update-repo-map.py
python scripts/update-repo-map.py --write
```

This is an orientation helper, not a complete architecture document.

## Existing Project Helpers

Enhanced Trading's runtime/build/deploy validators live in `tools/`. See `docs/CHECKS.md` for the current command menu.

## Private GitHub Remote Setup

`scripts/init-private-github.ps1` and `scripts/init-private-github.sh` are template helpers retained for parity. This repo already has an `origin`; do not run these unless explicitly reinitializing remote setup.
