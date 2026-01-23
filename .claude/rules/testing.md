# Testing Rules

## Coverage Requirements

- **ViewModels**: 70%+ line coverage (Kover)
- **Repositories**: 60%+ line coverage
- **Overall**: Target 30%+ project-wide

## Test Commands

```bash
nix run .#test      # Run all tests
nix run .#coverage  # Generate coverage report
nix run .#verify    # Full workflow (test + build + install)
```

## Test Structure

### Unit Tests
- Location: `app/src/test/kotlin/`
- Framework: JUnit5
- Coroutines: `kotlinx-coroutines-test`
- Dispatcher: `MainDispatcherRule`

### Use Fakes, Not Mocks

```kotlin
// GOOD: Fake implementation
class FakeProfileRepository : ProfileRepository {
    private val profiles = mutableListOf<Profile>()
    fun setProfile(p: Profile) { profiles.add(p) }
    override suspend fun getById(id: Long) = profiles.find { it.id == id }
}

// BAD: Mockito mock
val mockRepo = mock<ProfileRepository>()
```

## Test Naming

```kotlin
// Pattern: `when [condition] then [expected]`
@Test
fun `when profile loaded then uiState updated`()

@Test
fun `when network fails then error shown`()
```

## Before Commit

1. Run tests: `nix run .#test`
2. Check coverage: `nix run .#coverage`
3. Ensure no test failures
