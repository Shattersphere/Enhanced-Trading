#!/usr/bin/env bash
set -euo pipefail

OWNER="${OWNER:-${GITHUB_OWNER:-}}"
REPO_NAME="${REPO_NAME:-}"
DESCRIPTION="${DESCRIPTION:-}"
RUN="${RUN:-0}"
CONFIRM_PRIVATE_REMOTE="${CONFIRM_PRIVATE_REMOTE:-}"

fail() { echo "ERROR: $*" >&2; exit 1; }

slugify() {
  local value="$1"
  value="$(printf '%s' "$value" | tr '[:upper:]' '[:lower:]')"
  value="$(printf '%s' "$value" | sed -E 's/[^a-z0-9._-]+/-/g; s/^-+//; s/-+$//')"
  [ -n "$value" ] || fail "Could not derive a repo slug. Set REPO_NAME."
  printf '%s' "$value"
}

command -v git >/dev/null 2>&1 || fail "git is not available on PATH."
command -v gh >/dev/null 2>&1 || fail "GitHub CLI 'gh' is not available on PATH."
[ -n "$OWNER" ] || fail "OWNER is required. Example: OWNER=my-org REPO_NAME=my-project ./scripts/init-private-github.sh"

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "Not inside a Git repo. Initialize and commit locally first."
git rev-parse --verify HEAD >/dev/null 2>&1 || fail "No local commit exists. Commit intentional files before creating remote."

status="$(git status --porcelain)"
[ -z "$status" ] || fail "Working tree is not clean. Commit, revert, or intentionally leave remote creation for later."

if [ -z "$REPO_NAME" ]; then
  REPO_NAME="$(slugify "$(basename "$(pwd)")")"
else
  REPO_NAME="$(slugify "$REPO_NAME")"
fi

TARGET="$OWNER/$REPO_NAME"
echo "Repo root: $(pwd)"
echo "Target GitHub repo: $TARGET"

gh auth status >/dev/null || fail "GitHub CLI is not authenticated. Run: gh auth login"

git status --short --branch
if git remote get-url origin >/dev/null 2>&1; then
  echo "origin already exists: $(git remote get-url origin)"
  echo "No remote will be created. Verify it is the intended private repo."
  exit 0
fi

cmd=(gh repo create "$TARGET" --private --source=. --remote=origin --push)
[ -z "$DESCRIPTION" ] || cmd+=(--description "$DESCRIPTION")

if [ "$RUN" = "1" ]; then
  [ "$CONFIRM_PRIVATE_REMOTE" = "$TARGET" ] || fail "Refusing real run. Set CONFIRM_PRIVATE_REMOTE=$TARGET to confirm private repo creation."
  "${cmd[@]}"
else
  printf 'DRY RUN: would run:'
  printf ' %q' "${cmd[@]}"
  printf '\nReal run requires: RUN=1 CONFIRM_PRIVATE_REMOTE=%q\n' "$TARGET"
fi
