# Coding Standards Skill

Kotlin/Android coding standards for MoLe project.

## Core Principles

### KISS (Keep It Simple)
- Simple code over clever code
- Clear intent over brevity
- One thing per function

### DRY (Don't Repeat Yourself)
- Extract common logic
- Create shared utilities
- Use extension functions

### YAGNI (You Aren't Gonna Need It)
- Don't add features "just in case"
- Implement when needed
- Remove unused code

## Kotlin Standards

### Immutability
```kotlin
// Prefer val
val name = "John"  // Good
var name = "John"  // Avoid unless necessary

// Immutable collections
val list = listOf(1, 2, 3)  // Good
val list = mutableListOf(1, 2, 3)  // Only when needed
```

### Null Safety
```kotlin
// Avoid !!
val name = user!!.name  // Bad

// Use safe calls
val name = user?.name ?: "Unknown"  // Good
val name = requireNotNull(user).name  // Good (fails fast)
```

### Scope Functions
```kotlin
// Maximum 2 levels of nesting
user?.let { u ->
    u.profile?.let { p ->  // OK, 2 levels
        // ...
    }
}

// Avoid 3+ levels
user?.let { u ->
    u.profile?.let { p ->
        p.settings?.let { s ->  // Bad, 3 levels
            // ...
        }
    }
}
```

### Data Classes
```kotlin
// Use data class for data holders
data class Profile(
    val id: Long,
    val name: String,
    val email: String? = null
)
```

## File Organization

### Size Limits
- Target: 200-400 lines
- Maximum: 800 lines

### Structure
```kotlin
// 1. Package
package com.example.feature

// 2. Imports (sorted, no wildcards)
import android.content.Context
import javax.inject.Inject

// 3. Class definition
class MyClass {
    // 4. Properties (val before var)
    private val constant = "value"
    private var mutable = 0

    // 5. Init blocks

    // 6. Public methods

    // 7. Private methods

    // 8. Companion object
}
```

## Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Class | PascalCase | `ProfileViewModel` |
| Function | camelCase | `loadProfile()` |
| Variable | camelCase | `userName` |
| Constant | SCREAMING_SNAKE | `MAX_RETRY_COUNT` |
| Package | lowercase | `com.example.feature` |

## Architecture Standards

### ViewModel
- Use `@HiltViewModel`
- Use Repository (not DAO)
- Use Domain Models (not db.*)
- Maximum 300 lines

### Repository
- Interface + Implementation
- Handle data transformation
- Use Mappers for entity conversion

### Compose
- State in ViewModel
- Stateless Composables preferred
- Use `remember` for expensive operations

## Pre-commit Checks

```bash
pre-commit run ktlint --all-files
pre-commit run detekt --all-files
```
