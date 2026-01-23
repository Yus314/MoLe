# Kotlin Coding Style Rules

## Immutability

- Prefer `val` over `var`
- Use immutable collections by default
- Create new objects instead of mutating

```kotlin
// GOOD
val updated = state.copy(loading = true)

// BAD
state.loading = true
```

## Nullability

- Avoid double-bang (!!) operator
- If !! is necessary, add justification comment
- Use `?.let`, `?:`, `requireNotNull` instead

```kotlin
// BAD
val name = user!!.name

// GOOD
val name = user?.name ?: "Unknown"

// ACCEPTABLE (with comment)
val name = user!!.name  // Safe: validated in init()
```

## Scope Functions

- Maximum nesting: 2 levels
- Prefer explicit over clever

```kotlin
// BAD: Too nested
user?.let { u ->
    u.profile?.let { p ->
        p.settings?.let { s ->
            // ...
        }
    }
}

// GOOD: Flat
val profile = user?.profile ?: return
val settings = profile.settings ?: return
```

## File Size

- Target: 200-400 lines
- Maximum: 800 lines
- Split large files by responsibility

## Function Size

- Target: <30 lines
- Maximum: 60 lines (detekt rule)
- Extract helper functions for clarity

## Naming

- Classes: PascalCase
- Functions: camelCase
- Constants: SCREAMING_SNAKE_CASE
- Variables: camelCase

## Pre-commit Checks

```bash
pre-commit run ktlint --all-files
pre-commit run detekt --all-files
```
