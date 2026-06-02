#!/usr/bin/env python3
"""Check agent-template/project hygiene.

Default mode checks structural hygiene. `--initialized` also checks for common
unfilled template placeholders after a copied project has supposedly been
specialized.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

REQUIRED = [
    "AGENTS.md",
    "README.md",
    "CHANGELOG.md",
    "INIT_AGENT_PROMPT.md",
    ".agent/INDEX.md",
    ".agent/DOC_SYSTEM.md",
    ".agent/BRIEF.md",
    ".agent/PLAN.md",
    ".agent/BACKLOG.md",
    ".agent/HANDOVER.md",
    ".agent/PUBLIC_RELEASE.md",
    ".agent/SHARED_LIBRARIES.md",
    ".agent/archive/INDEX.md",
    "docs/PROJECT_FACTS.md",
    "docs/CHECKS.md",
    "docs/REPO_MAP.md",
    "docs/ARCHITECTURE.md",
    "docs/CODE_QUALITY.md",
    "docs/DIRECTORY_DOC_POLICY.md",
    "docs/ASSET_PROVENANCE.md",
    "scripts/README.md",
    "scripts/update-repo-map.py",
    "scripts/init-private-github.ps1",
    "scripts/init-private-github.sh",
]

FAIL_MARKERS_ALWAYS: list[str] = []
FAIL_MARKERS_INITIALIZED = ["TODO[init]", "[PROJECT_NAME]", "[PROJECT_TYPE]", "[FULL_LOCAL_FOLDER_PATH]", "[REPO_SLUG"]
WARN_MARKERS = ["TODO[unknown]"]

MAX_BYTES = {
    "AGENTS.md": 6_000,
    ".agent/INDEX.md": 5_000,
    ".agent/BRIEF.md": 10_000,
    ".agent/PLAN.md": 12_000,
    ".agent/BACKLOG.md": 12_000,
    ".agent/DOC_SYSTEM.md": 7_000,
    ".agent/SHARED_LIBRARIES.md": 5_000,
}
FREQUENT_DOC_MARKERS = ("PLAN", "STATUS", "ROADMAP", "CURRENT", "MODERNIZATION")
FREQUENT_DOC_LIMIT = 12_000


def iter_text_files() -> list[Path]:
    skip_parts = {".git", "__pycache__", "node_modules", "build", "dist", "out", "target"}
    files: list[Path] = []
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        rel_parts = path.relative_to(ROOT).parts
        if any(part in skip_parts for part in rel_parts):
            continue
        if path.relative_to(ROOT).as_posix() == "scripts/check-template-state.py":
            continue
        if path.suffix.lower() in {".png", ".jpg", ".jpeg", ".gif", ".webp", ".zip", ".jar", ".class"}:
            continue
        files.append(path)
    return files


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return ""


def is_frequent_doc(path: Path) -> bool:
    rel = path.relative_to(ROOT).as_posix()
    if path.suffix.lower() != ".md":
        return False
    if rel.startswith(".agent/archive/") or rel.endswith("HANDOVER.md"):
        return False
    if path.name.upper() in {"README.MD", "CHANGELOG.MD", "LICENSE.MD"}:
        return False
    upper = path.name.upper()
    return any(marker in upper for marker in FREQUENT_DOC_MARKERS)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--initialized", action="store_true", help="fail unfilled project placeholders")
    args = parser.parse_args()

    errors: list[str] = []
    warnings: list[str] = []

    for rel in REQUIRED:
        if not (ROOT / rel).exists():
            errors.append(f"missing required file: {rel}")

    if (ROOT / "docs" / "VALIDATION_MATRIX.md").exists():
        errors.append("docs/VALIDATION_MATRIX.md exists; use docs/CHECKS.md instead")

    for rel in ["optional_skills", ".codex/skills/memory-maintenance"]:
        if (ROOT / rel).exists():
            errors.append(f"forbidden path exists: {rel}")

    for pycache in ROOT.rglob("__pycache__"):
        errors.append(f"generated cache directory present: {pycache.relative_to(ROOT)}")

    for rel, limit in MAX_BYTES.items():
        path = ROOT / rel
        if path.exists() and path.stat().st_size > limit:
            errors.append(f"{rel} is {path.stat().st_size} bytes; limit <= {limit}")

    for path in iter_text_files():
        rel = path.relative_to(ROOT).as_posix()
        if is_frequent_doc(path) and path.stat().st_size > FREQUENT_DOC_LIMIT:
            errors.append(f"{rel} is {path.stat().st_size} bytes; frequently-read docs limit <= {FREQUENT_DOC_LIMIT}")
        text = read_text(path)
        if not text:
            continue
        for marker in FAIL_MARKERS_ALWAYS:
            if marker in text:
                errors.append(f"{rel} contains forbidden marker {marker}")
        if args.initialized:
            for marker in FAIL_MARKERS_INITIALIZED:
                if marker in text:
                    errors.append(f"{rel} contains unfilled placeholder {marker}")
        for marker in WARN_MARKERS:
            count = text.count(marker)
            if count:
                warnings.append(f"{rel} contains {count} {marker} marker(s)")

    print("Template/project hygiene check")
    print(f"Root: {ROOT}")
    if warnings:
        print("\nWarnings:")
        for item in warnings:
            print(f"- {item}")
    if errors:
        print("\nErrors:")
        for item in errors:
            print(f"- {item}")
        return 1
    print("\nNo blocking issues found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
