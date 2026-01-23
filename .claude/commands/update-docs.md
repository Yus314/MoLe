---
name: update-docs
description: Update CLAUDE.md and project documentation
---

# Documentation Update

You are about to update project documentation using the doc-updater agent.

## When to Update

1. **Dependency version change**: Update Active Technologies
2. **New pattern introduced**: Add to relevant section
3. **File structure change**: Update Project Structure
4. **New command added**: Add to Commands
5. **Convention change**: Update Code Style

## CLAUDE.md Sections

1. **Active Technologies**: Version numbers
2. **Project Structure**: Directory layout
3. **Commands**: Nix and Gradle commands
4. **Development Workflow**: Procedures
5. **実機デバッグ**: adb MCP tools
6. **Code Style**: Conventions
7. **Hilt DI**: Injection patterns
8. **Repository Pattern**: Data layer
9. **Domain Model Layer**: Domain models
10. **Jetpack Compose**: UI patterns
11. **Test Coverage**: Testing guidelines
12. **Recent Changes**: Changelog

## Update Process

### Step 1: Identify Changes
```bash
git log --oneline -10
git diff --name-only HEAD~5
```

### Step 2: Categorize Impact
- Technology update → Active Technologies
- New feature → Relevant section
- Refactor → May need pattern update

### Step 3: Edit Documentation
- Update CLAUDE.md
- Add to Recent Changes
- Verify formatting

### Step 4: Verify
- Code examples compile
- Commands work
- Links valid

## Version Update Example

Before:
```markdown
- Kotlin 2.0.20 / Coroutines 1.8.0
```

After:
```markdown
- Kotlin 2.0.21 / Coroutines 1.9.0
```

## Recent Changes Format

```markdown
## Recent Changes
- [feature-id]: [Brief description]
- [feature-id]: [Another change]
```

## Documentation Checklist

- [ ] Versions accurate
- [ ] Commands tested
- [ ] Examples compile
- [ ] No dead links
- [ ] Recent Changes updated
