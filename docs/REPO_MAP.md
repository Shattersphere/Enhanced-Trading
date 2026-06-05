# Repo Map

Orientation helper for major paths. Regenerate the inventory with:

```powershell
python scripts/update-repo-map.py --write
```

## Major Areas

| Path | Purpose |
|---|---|
| `src/main/kotlin/weaponsprocurement/` | Kotlin runtime source for config, stock, trade, UI, plugins, and diagnostics. |
| `src/weaponsprocurement/` | Legacy public Java source still compiled into the clean jar. |
| `data/` | Starsector rules, config, Luna settings, and runtime data. |
| `graphics/` | Runtime mod assets when present. |
| `tools/` | Build/deploy/validation/public-export helpers. |
| `docs/` | Template-synced source-of-truth docs, checks, repo map, architecture, modernization plan, and quality standards. |
| `.agent/` | Private agent-facing state, workflow, release, shared-library, and archive docs. |
| `.github/workflows/` | Public-safe repository sanity workflow. |
| `jars/` | Generated runtime jar output. |
| `build/` | Generated build/public-export output. |

## Generated Path Inventory

<!-- BEGIN GENERATED PATH INVENTORY -->
- `.agent/`
  - `.agent/archive/`
    - `.agent/archive/deep-dives/`
    - `.agent/archive/history/`
      - `.agent/archive/history/snapshots/`
  - `.agent/ARCHITECTURE_MAP.md`
  - `.agent/BACKLOG.md`
  - `.agent/BRIEF.md`
  - `.agent/DOC_SYSTEM.md`
  - `.agent/HANDOVER.md`
  - `.agent/INDEX.md`
  - `.agent/PLAN.md`
  - `.agent/PUBLIC_RELEASE.md`
  - `.agent/SHARED_LIBRARIES.md`
- `.github/`
  - `.github/workflows/`
- `data/`
  - `data/campaign/`
  - `data/config/`
- `docs/`
  - `docs/ARCHITECTURE.md`
  - `docs/ASSET_PROVENANCE.md`
  - `docs/CHECKS.md`
  - `docs/CODE_QUALITY.md`
  - `docs/CODEBASE_QUALITY_MODERNIZATION_PLAN.md`
  - `docs/DIRECTORY_DOC_POLICY.md`
  - `docs/PROJECT_FACTS.md`
  - `docs/REPO_MAP.md`
- `gradle/`
  - `gradle/wrapper/`
- `graphics/`
  - `graphics/ui/`
- `scripts/`
  - `scripts/check-template-state.py`
  - `scripts/init-private-github.ps1`
  - `scripts/init-private-github.sh`
  - `scripts/README.md`
  - `scripts/update-repo-map.py`
- `src/`
  - `src/main/`
    - `src/main/kotlin/`
      - `src/main/kotlin/com/`
      - `src/main/kotlin/weaponsprocurement/`
- `tools/`
  - `tools/lib/`
  - `tools/analyze-ship-catalog-diagnostics.ps1`
  - `tools/analyze-trade-rollback-diagnostics.ps1`
  - `tools/deploy-live-mod.ps1`
  - `tools/export-public.ps1`
  - `tools/validate-compatibility-surfaces.ps1`
  - `tools/validate-doc-links.ps1`
  - `tools/validate-gui-button-style.ps1`
  - `tools/validate-jar-classes.ps1`
  - `tools/validate-kotlin-migration.ps1`
  - `tools/validate-live-gui-classes.ps1`
- `.gitignore`
- `AGENTS.md`
- `build.gradle.kts`
- `build.ps1`
- `CHANGELOG.md`
- `CONFIG.md`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `HANDOVER.md`
- `INIT_AGENT_PROMPT.md`
- `mod_info.json`
- `PACKAGING.md`
- `PLANS.md`
- `README.md`
- `settings.gradle.kts`
<!-- END GENERATED PATH INVENTORY -->
