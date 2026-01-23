---
name: tdd
description: Execute Test-Driven Development workflow
---

# Test-Driven Development Workflow

You are about to follow the TDD cycle: RED → GREEN → REFACTOR

## TDD Steps

### Step 1: RED (Write Failing Test)

Write a test that describes the desired behavior:

```kotlin
@Test
fun `when [condition] then [expected result]`() = runTest {
    // Given
    [setup state]

    // When
    [action]

    // Then
    [assertion]
}
```

Run test to confirm it fails:
```bash
nix run .#test
```

### Step 2: GREEN (Minimal Implementation)

Write just enough code to make the test pass:
- No extra features
- No premature optimization
- Focus on passing the test

Run test again:
```bash
nix run .#test
```

### Step 3: REFACTOR (Improve Code)

With tests passing, improve the code:
- Extract common logic
- Improve naming
- Remove duplication
- Keep tests green

Run tests after each change:
```bash
nix run .#test
```

## MoLe Test Patterns

### ViewModel Test Template
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: MyViewModel
    private lateinit var fakeRepository: FakeMyRepository

    @Before
    fun setup() {
        fakeRepository = FakeMyRepository()
        viewModel = MyViewModel(fakeRepository)
    }

    @Test
    fun `test case`() = runTest {
        // Given
        fakeRepository.setData(testData)

        // When
        viewModel.doAction()
        advanceUntilIdle()

        // Then
        assertEquals(expected, viewModel.uiState.value)
    }
}
```

## Available Fakes

- `FakeProfileRepository`
- `FakeTransactionRepository`
- `FakeTransactionSender`
- `FakeTransactionSyncer`
- `FakeConfigBackup`
- `FakeCurrencyFormatter`

## Coverage Check

After completing TDD cycles:
```bash
nix run .#coverage
```

Target: 70%+ for ViewModels, 60%+ for Repositories
