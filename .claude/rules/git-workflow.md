# Git Workflow Rules

## Commit Message Format

```
<type>: <description>

[optional body]

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code restructuring
- `test`: Adding tests
- `docs`: Documentation
- `chore`: Maintenance

### Examples
```
feat(sync): Add TransactionSyncer implementation

fix(ui): Correct date picker display issue

refactor(viewmodel): Split MainViewModel responsibilities

test(coverage): Add ViewModel tests for 70% coverage
```

## Pre-Push Checklist

1. **Run tests**: `nix run .#test`
2. **Check lint**: `pre-commit run --all-files`
3. **Verify build**: `nix run .#build`
4. **For UI changes**: `nix run .#verify`

## Branch Strategy

- `master`: Main branch, always stable
- Feature branches: `feature/description`
- Bug fixes: `fix/description`

## Pull Request Guidelines

### Before Creating PR
- All tests pass
- Lint checks pass
- Build succeeds
- UI verified on device (if applicable)

### PR Description
```markdown
## Summary
[1-3 bullet points]

## Test Plan
- [ ] Unit tests pass
- [ ] Build succeeds
- [ ] Verified on device

## Screenshots (if UI change)
[Before/After images]
```

## Never Do

- Force push to master
- Skip pre-commit hooks
- Commit with failing tests
- Commit sensitive data
