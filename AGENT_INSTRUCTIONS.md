# Agent Instructions (Detailed)

## Goals
- Preserve existing behavior unless a change is explicitly requested.
- Favor small, reviewable diffs and clear commit messages.

## Workflow
- Inspect before editing; avoid speculative changes.
- Prefer `rg` for search.
- Use `apply_patch` for single-file edits when practical.

## Testing
- Run relevant tests when available and report if not run.

## Documentation
- Update docs only when behavior or usage changes.
