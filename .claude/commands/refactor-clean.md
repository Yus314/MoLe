---
name: refactor-clean
description: Find and remove dead code
---

# Dead Code Cleanup

You are about to identify and remove unused code using the refactor-cleaner agent.

## Detection Process

### Step 1: Run Static Analysis
```bash
# detekt for Kotlin
pre-commit run detekt --all-files

# Android Lint
nix run .#lint
```

### Step 2: Search for Candidates

**Deprecated Code**
```bash
grep -r "@Deprecated" app/src/main/kotlin/
```

**TODO/FIXME for Removal**
```bash
grep -r "TODO.*remove\|FIXME.*delete" app/src/main/kotlin/
```

**Unused Imports**
detekt will flag these automatically.

### Step 3: Verify No References

Before removing any code:
```bash
# Search for usages
grep -r "FunctionName" app/src/

# Check tests
grep -r "FunctionName" app/src/test/
```

### Step 4: Remove Safely

1. Delete the code
2. Run tests: `nix run .#test`
3. Run build: `nix run .#build`
4. Verify: `nix run .#verify`

## MoLe-Specific Cleanup Targets

### Deprecated Models
If still present, consider removing:
- `model/LedgerTransaction.kt` → Use `domain.model.Transaction`
- `model/LedgerTransactionAccount.kt` → Use `domain.model.TransactionLine`
- `model/LedgerAccount.kt` → Use `domain.model.Account`

### Legacy Async Patterns
If still present, remove:
- `GeneralBackgroundTasks`
- `TaskCallback`
- `AsyncResultCallback`
- `BaseDAO` async methods

### XML Layouts
Directory should be empty after Compose migration:
- `app/src/main/res/layout/`

## Safe Removal Checklist

- [ ] No references in main code
- [ ] No references in test code
- [ ] Not part of public API
- [ ] Tests pass after removal
- [ ] Build succeeds after removal
- [ ] App runs correctly after removal

## Conservative Approach

If uncertain about removal:
1. Add `@Deprecated` annotation first
2. Add TODO with removal date
3. Remove in future cleanup cycle
