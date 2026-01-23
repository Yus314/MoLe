# TDD Workflow Skill

Test-Driven Development methodology for Android/Kotlin with JUnit5 and Coroutines.

## The TDD Cycle

```
    ┌─────────────────────────────────────┐
    │                                     │
    │   RED → GREEN → REFACTOR → repeat   │
    │                                     │
    └─────────────────────────────────────┘
```

### 1. RED: Write Failing Test

Write a test that describes desired behavior:

```kotlin
@Test
fun `when profile loaded then uiState contains profile`() = runTest {
    // Given
    val expected = Profile(id = 1, name = "Test")
    fakeRepository.setProfile(expected)

    // When
    viewModel.loadProfile(1)
    advanceUntilIdle()

    // Then
    assertEquals(expected, viewModel.uiState.value.profile)
}
```

Run to confirm it fails:
```bash
nix run .#test
```

### 2. GREEN: Minimal Implementation

Write just enough code to pass:

```kotlin
fun loadProfile(id: Long) {
    viewModelScope.launch {
        val profile = repository.getProfileById(id)
        _uiState.update { it.copy(profile = profile) }
    }
}
```

Run to confirm it passes:
```bash
nix run .#test
```

### 3. REFACTOR: Improve Code

With tests passing, improve:
- Extract common logic
- Improve naming
- Remove duplication
- Add error handling

```kotlin
fun loadProfile(id: Long) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val profile = repository.getProfileById(id)
            _uiState.update { it.copy(profile = profile, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }
}
```

Keep tests green:
```bash
nix run .#test
```

## Test Patterns

### Happy Path
```kotlin
@Test
fun `when valid data then success`() = runTest {
    // Given: valid state
    // When: action
    // Then: success result
}
```

### Error Path
```kotlin
@Test
fun `when error occurs then error shown`() = runTest {
    // Given: error condition
    // When: action
    // Then: error state
}
```

### Edge Cases
```kotlin
@Test
fun `when empty list then empty state shown`() = runTest {
    // Given: empty data
    // When: load
    // Then: empty state
}
```

## Coverage Check

After TDD cycles:
```bash
nix run .#coverage
```

Target: 70%+ for ViewModels
