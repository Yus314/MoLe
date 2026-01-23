---
name: code-review
description: Review recent code changes for quality and security
---

# Code Review

You are about to review recent code changes using the code-reviewer agent.

## Review Process

1. **Get Recent Changes**
   ```bash
   git diff HEAD~1
   ```

2. **Launch Code Reviewer**
   - Use Task tool with `subagent_type: code-reviewer`
   - Focus on modified files

3. **Review Categories**

### CRITICAL (Must Fix)
- Security vulnerabilities
- Architecture violations (DAO in ViewModel, db.* entities)
- Missing Hilt annotations

### HIGH (Should Fix)
- Double-bang operator without justification
- var when val works
- Large functions (>60 lines)
- Missing error handling

### MEDIUM (Consider)
- Missing tests
- Performance issues
- Poor naming

### LOW (Suggestions)
- Formatting issues
- Documentation gaps

## MoLe-Specific Checks

- [ ] ViewModel uses Repository (not DAO)
- [ ] ViewModel uses Domain Models (not db.*)
- [ ] @HiltViewModel annotation present
- [ ] No double-bang (!!) without comment
- [ ] Prefer val over var
- [ ] Scope function nesting ≤ 2

## Automated Checks

Before approving:
```bash
# Run lint
pre-commit run --all-files

# Run tests
nix run .#test

# Full verification (UI changes)
nix run .#verify
```

## Review Output Format

```markdown
## Code Review: [Files]

### CRITICAL Issues
[List issues]

### HIGH Issues
[List issues]

### Verdict
- [ ] Approve
- [ ] Approve with changes
- [ ] Request changes
```
