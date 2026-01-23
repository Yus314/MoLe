---
name: build-fix
description: Diagnose and fix Kotlin/Gradle build errors
---

# Build Error Resolution

You are about to diagnose and fix build errors using the build-error-resolver agent.

## Diagnostic Steps

### Step 1: Run Build
```bash
nix run .#build
```

### Step 2: Identify Error Type

**Kotlin Compilation**
- Type mismatch
- Unresolved reference
- Null safety issues

**Gradle/Dependency**
- Version conflicts
- Missing dependencies
- Configuration errors

**Hilt/DI**
- Missing annotations
- Binding errors

**Room**
- Query syntax errors
- Schema issues

**Compose**
- Composable context errors
- State issues

### Step 3: Apply Fix

Use the build-error-resolver agent to:
1. Parse error message
2. Locate source file
3. Apply appropriate fix
4. Verify build

### Step 4: Verify
```bash
nix run .#build
nix run .#test
```

## Common Fixes

| Error | Fix |
|-------|-----|
| "Unresolved reference" | Check imports, add dependency |
| "Type mismatch" | Check nullability `?` |
| "MissingBinding" | Add `@Inject constructor` |
| "Not a function" | Add parentheses `()` |
| "Val cannot be reassigned" | Use `var` or new reference |

## Build Configuration

Key versions in MoLe:
- Kotlin: 2.0.21
- Gradle: 8.9
- AGP: 8.7.3
- Hilt: 2.51.1
- Room: 2.4.2
- Compose BOM: 2024.12.01

## Clean Build

If issues persist:
```bash
nix run .#clean
nix run .#build
```
