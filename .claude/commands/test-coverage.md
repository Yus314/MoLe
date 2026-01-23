---
name: test-coverage
description: Analyze and improve test coverage
---

# Test Coverage Analysis

Analyze current test coverage and identify areas for improvement.

## Generate Coverage Report

```bash
nix run .#coverage
```

Report locations:
- HTML: `app/build/reports/kover/htmlDebug/index.html`
- XML: `app/build/reports/kover/reportDebug.xml`

## Coverage Targets

| Component | Target | Current |
|-----------|--------|---------|
| ViewModels | 70%+ | Check report |
| Repositories | 60%+ | Check report |
| Overall | 30%+ | Check report |

## Priority Testing Areas

### High Priority (Must Test)
1. **ViewModels**: All public functions
2. **Repositories**: Data transformations
3. **Use Cases**: Business logic

### Medium Priority
1. **Mappers**: Domain ↔ Entity conversion
2. **Validators**: Input validation
3. **Formatters**: Data formatting

### Lower Priority
1. **UI Composables**: Basic rendering
2. **Extension functions**: Simple utilities
3. **Constants**: No logic to test

## Finding Untested Code

1. Open coverage HTML report
2. Look for red (uncovered) lines
3. Focus on:
   - Error handling paths
   - Edge cases
   - Conditional branches

## Test Template

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: MyViewModel
    private lateinit var fakeRepo: FakeMyRepository

    @Before
    fun setup() {
        fakeRepo = FakeMyRepository()
        viewModel = MyViewModel(fakeRepo)
    }

    @Test
    fun `happy path test`() = runTest {
        // Given
        fakeRepo.setData(validData)

        // When
        viewModel.loadData()
        advanceUntilIdle()

        // Then
        assertEquals(expected, viewModel.uiState.value.data)
    }

    @Test
    fun `error path test`() = runTest {
        // Given
        fakeRepo.setShouldFail(true)

        // When
        viewModel.loadData()
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.error)
    }
}
```

## Improving Coverage

1. **Identify gaps**: Review coverage report
2. **Write tests**: Focus on uncovered branches
3. **Run coverage**: `nix run .#coverage`
4. **Repeat**: Until targets met
