#!/usr/bin/env python3
"""Generate a compact repository path inventory for docs/REPO_MAP.md.

Orientation helper only. It skips common generated/vendor/cache directories and
private archive payloads so agents do not waste context.
"""

from __future__ import annotations

import argparse
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO_MAP = ROOT / "docs" / "REPO_MAP.md"
BEGIN = "<!-- BEGIN GENERATED PATH INVENTORY -->"
END = "<!-- END GENERATED PATH INVENTORY -->"

SKIP_DIR_NAMES = {
    ".git", ".idea", ".vscode", ".gradle", ".mypy_cache", ".pytest_cache",
    ".ruff_cache", "__pycache__", "node_modules", "build", "dist", "out",
    "target", "bin", "obj", "coverage", "jars", ".agent-deploy",
    ".agent/tmp", ".agent/staging",
}
SKIP_SUFFIXES = {
    ".class", ".jar", ".zip", ".7z", ".png", ".jpg", ".jpeg", ".gif",
    ".webp", ".psd", ".exe", ".dll", ".so", ".dylib",
}
MAX_DEPTH = 3
MAX_ENTRIES = 240


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def should_skip_dir(path: Path) -> bool:
    relative = rel(path) if path != ROOT else ""
    return path.name in SKIP_DIR_NAMES or relative in SKIP_DIR_NAMES


def tracked_paths() -> set[Path]:
    result = subprocess.run(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard"],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    paths: set[Path] = set()
    for line in result.stdout.splitlines():
        if not line.strip():
            continue
        path = ROOT / line
        paths.add(path)
        paths.update(path.parents)
    paths.add(ROOT)
    return paths


def build_inventory() -> str:
    lines: list[str] = []
    count = 0
    tracked = tracked_paths()

    def walk(directory: Path, depth: int) -> None:
        nonlocal count
        if depth > MAX_DEPTH or count >= MAX_ENTRIES:
            return
        try:
            children = sorted(directory.iterdir(), key=lambda p: (not p.is_dir(), p.name.lower()))
        except OSError as exc:
            lines.append(f"{'  ' * depth}- `{rel(directory)}/` - unreadable: {exc}")
            return

        for child in children:
            if count >= MAX_ENTRIES:
                break
            if child not in tracked:
                continue
            if child.is_dir():
                if should_skip_dir(child):
                    continue
                lines.append(f"{'  ' * depth}- `{rel(child)}/`")
                count += 1
                walk(child, depth + 1)
            else:
                if child.suffix.lower() in SKIP_SUFFIXES:
                    continue
                if depth <= 1:
                    lines.append(f"{'  ' * depth}- `{rel(child)}`")
                    count += 1

    walk(ROOT, 0)
    if count >= MAX_ENTRIES:
        lines.append(f"\n_Inventory truncated at {MAX_ENTRIES} entries. Use `rg --files` for targeted discovery._")
    if not lines:
        lines.append("_No paths found._")
    return "\n".join(lines) + "\n"


def update_repo_map(block: str) -> None:
    text = REPO_MAP.read_text(encoding="utf-8")
    if BEGIN not in text or END not in text:
        raise SystemExit(f"Could not find generated inventory markers in {REPO_MAP}")
    before, rest = text.split(BEGIN, 1)
    _old, after = rest.split(END, 1)
    REPO_MAP.write_text(f"{before}{BEGIN}\n{block}{END}{after}", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true", help="update docs/REPO_MAP.md in place")
    args = parser.parse_args()
    block = build_inventory()
    if args.write:
        update_repo_map(block)
        print(f"Updated {REPO_MAP.relative_to(ROOT)}")
    else:
        print(block, end="")


if __name__ == "__main__":
    main()
