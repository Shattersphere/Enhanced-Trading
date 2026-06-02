# Directory Documentation Policy

Do not create an `INDEX.md` or `README.md` in every folder. Add local docs only where they prevent likely mistakes.

## Prefer Existing Central Docs When

- the directory is obvious from `docs/REPO_MAP.md`;
- the behavior is simple;
- the folder is generated, vendored, cached, or build output;
- the doc would merely restate filenames;
- the subsystem is still volatile.

## Add A Local README Or Package Doc When

- the directory is a stable subsystem boundary;
- setup or generation rules are easy to get wrong;
- public contributors/users need the explanation;
- the folder has non-obvious lifecycle, ownership, or data flow;
- local conventions differ from the rest of the repo.

## Add Nested `AGENTS.md` Only When

- agents regularly make mistakes in that subtree;
- commands or safety rules differ materially from the root;
- generated/vendor/runtime-sensitive files need special handling;
- the subtree has its own validation or deploy workflow.

Nested `AGENTS.md` files consume instruction budget. Keep them short and specific.

## Local Doc Template

```md
# Directory Name

Purpose: ...

Read this when: ...

Do not edit: ...

Commands/checks: ...

Key invariants: ...
```
