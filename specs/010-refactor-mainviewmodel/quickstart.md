# Quickstart Guide: Split ViewModels Architecture

**Feature**: MainViewModel Refactoring for Improved Maintainability and Testability
**Date**: 2026-01-13

## Overview

This guide explains how to work with the new split ViewModel architecture for the main screen. The monolithic MainViewModel (853 lines) has been split into 4 focused components:

| Component | Responsibility | Target Size |
|-----------|---------------|-------------|
| ProfileSelectionViewModel | Profile selection and ordering | ~120 lines |
| AccountSummaryViewModel | Account list, filtering, expansion | ~220 lines |
| TransactionListViewModel | Transaction list, filtering, date navigation | ~220 lines |
| MainCoordinatorViewModel | Tab/drawer/refresh coordination, navigation | ~200 lines |

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    MainActivityCompose                          │
│  @AndroidEntryPoint                                             │
│  - by viewModels() for all 4 ViewModels                         │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                      MainScreen                                  │
│  Composable that receives all 4 ViewModels                       │
│  - Collects effects from all ViewModels                         │
│  - Distributes state/events to child Composables                │
└───────────────────────┬─────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┬───────────────┐
        ▼               ▼               ▼               ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│ Navigation    │ │ AccountSummary│ │ TransactionList│ │ Tab/Drawer/   │
│ Drawer        │ │ Tab           │ │ Tab           │ │ Refresh       │
│               │ │               │ │               │ │               │
│ ProfileSelect │ │ AccountSummary│ │ TransactionList│ │ MainCoordinator│
│ ViewModel     │ │ ViewModel     │ │ ViewModel     │ │ ViewModel     │
└───────────────┘ └───────────────┘ └───────────────┘ └───────────────┘
        │               │               │               │
        └───────────────┴───────────────┴───────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Data Layer (Repositories)                     │
│  ProfileRepository | AccountRepository | TransactionRepository  │
│  PreferencesRepository | OptionRepository                        │
└─────────────────────────────────────────────────────────────────┘
```

## Quick Reference

### Adding a New Feature

**Q: Where do I add profile-related logic?**
A: ProfileSelectionViewModel

**Q: Where do I add account list logic?**
A: AccountSummaryViewModel

**Q: Where do I add transaction list logic?**
A: TransactionListViewModel

**Q: Where do I add navigation or UI coordination?**
A: MainCoordinatorViewModel

### File Locations

```
app/src/main/kotlin/net/ktnx/mobileledger/
├── ui/main/
│   ├── ProfileSelectionViewModel.kt      # Profile selection
│   ├── ProfileSelectionUiState.kt        # Profile selection state
│   ├── ProfileSelectionEvent.kt          # Profile selection events
│   ├── AccountSummaryViewModel.kt        # Account list
│   ├── AccountSummaryUiState.kt          # Account list state (existing)
│   ├── TransactionListViewModel.kt       # Transaction list
│   ├── TransactionListUiState.kt         # Transaction list state (existing)
│   ├── MainCoordinatorViewModel.kt       # UI coordination
│   ├── MainUiState.kt                    # Coordinator state (updated)
│   └── MainScreen.kt                     # Main screen Composable
│
├── data/repository/
│   └── PreferencesRepository.kt          # Preferences storage
│
└── ui/activity/
    └── MainActivityCompose.kt            # Activity with all ViewModels
```

## Common Tasks

### Task 1: Adding a Profile Feature

**Example**: Add ability to set a profile as favorite

1. **Add event** in `ProfileSelectionEvent.kt`:
```kotlin
sealed class ProfileSelectionEvent {
    // ... existing events
    data class SetFavorite(val profileId: Long) : ProfileSelectionEvent()
}
```

2. **Add state** in `ProfileSelectionUiState.kt` if needed:
```kotlin
data class ProfileSelectionUiState(
    // ... existing fields
    val favoriteProfileId: Long? = null
)
```

3. **Handle event** in `ProfileSelectionViewModel.kt`:
```kotlin
fun onEvent(event: ProfileSelectionEvent) {
    when (event) {
        // ... existing handlers
        is ProfileSelectionEvent.SetFavorite -> setFavorite(event.profileId)
    }
}

private fun setFavorite(profileId: Long) {
    // Implementation here
}
```

4. **Update UI** in `NavigationDrawer.kt`:
```kotlin
// Use ProfileSelectionViewModel state/events
```

5. **Add test** in `ProfileSelectionViewModelTest.kt`:
```kotlin
@Test
fun `setFavorite updates state`() = runTest {
    // Test implementation
}
```

### Task 2: Adding an Account Feature

**Example**: Add ability to hide specific accounts

1. **Add event** in existing `AccountSummaryEvent`:
```kotlin
sealed class AccountSummaryEvent {
    // ... existing events
    data class HideAccount(val accountId: Long) : AccountSummaryEvent()
}
```

2. **Handle in** `AccountSummaryViewModel.kt`:
```kotlin
fun onEvent(event: AccountSummaryEvent) {
    when (event) {
        // ... existing handlers
        is AccountSummaryEvent.HideAccount -> hideAccount(event.accountId)
    }
}
```

3. **Add test** in `AccountSummaryViewModelTest.kt`

### Task 3: Adding a Transaction Feature

**Example**: Add transaction search by description

1. **Add state field** in `TransactionListUiState.kt`:
```kotlin
data class TransactionListUiState(
    // ... existing fields
    val searchQuery: String = ""
)
```

2. **Add event** in `TransactionListEvent`:
```kotlin
sealed class TransactionListEvent {
    // ... existing events
    data class SearchTransactions(val query: String) : TransactionListEvent()
}
```

3. **Handle in** `TransactionListViewModel.kt`

4. **Add test** in `TransactionListViewModelTest.kt`

### Task 4: Adding Navigation

**Example**: Add navigation to a new screen

1. **Add effect** in `MainUiState.kt` (MainCoordinatorEffect):
```kotlin
sealed class MainCoordinatorEffect {
    // ... existing effects
    data object NavigateToSettings : MainCoordinatorEffect()
}
```

2. **Add event** in `MainEvent`:
```kotlin
sealed class MainEvent {
    // ... existing events
    data object NavigateToSettings : MainEvent()
}
```

3. **Handle in** `MainCoordinatorViewModel.kt`:
```kotlin
private fun navigateToSettings() {
    viewModelScope.launch {
        _effects.send(MainCoordinatorEffect.NavigateToSettings)
    }
}
```

4. **Collect effect** in `MainScreen.kt`:
```kotlin
LaunchedEffect(Unit) {
    coordinatorViewModel.effects.collect { effect ->
        when (effect) {
            // ... existing handlers
            is MainCoordinatorEffect.NavigateToSettings -> onNavigateToSettings()
        }
    }
}
```

## Testing Guidelines

### Test File Mapping

| ViewModel | Test File |
|-----------|-----------|
| ProfileSelectionViewModel | ProfileSelectionViewModelTest.kt |
| AccountSummaryViewModel | AccountSummaryViewModelTest.kt |
| TransactionListViewModel | TransactionListViewModelTest.kt |
| MainCoordinatorViewModel | MainCoordinatorViewModelTest.kt |

### Test Setup Pattern

```kotlin
class MyViewModelTest {
    // Use Fake repositories, not mocks
    private val fakeProfileRepository = FakeProfileRepositoryForViewModel()
    private val fakeAccountRepository = FakeAccountRepositoryForViewModel()
    private lateinit var viewModel: MyViewModel

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()  // For coroutines

    @Before
    fun setup() {
        viewModel = MyViewModel(fakeProfileRepository, fakeAccountRepository)
    }

    @Test
    fun `my test`() = runTest {
        // Given: Set up fake state
        fakeProfileRepository.setProfiles(listOf(testProfile))

        // When: Perform action
        viewModel.onEvent(MyEvent.SomeAction)

        // Then: Verify state
        val state = viewModel.uiState.value
        assertEquals(expectedValue, state.someField)
    }
}
```

### Running Component Tests

```bash
# Run all tests
nix run .#test

# Run specific component test (in FHS shell)
nix develop .#fhs
./gradlew test --tests "*.ProfileSelectionViewModelTest"
./gradlew test --tests "*.AccountSummaryViewModelTest"
./gradlew test --tests "*.TransactionListViewModelTest"
./gradlew test --tests "*.MainCoordinatorViewModelTest"
```

## Shared State Management

### ProfileRepository.currentProfile

All ViewModels observe `ProfileRepository.currentProfile` for profile changes:

```kotlin
// In any ViewModel
init {
    viewModelScope.launch {
        profileRepository.currentProfile.collect { profile ->
            // React to profile change
        }
    }
}
```

### Cross-ViewModel Communication

ViewModels do NOT communicate directly. Instead:

1. **Via Repository**: ProfileSelectionViewModel updates ProfileRepository, other VMs observe
2. **Via Effects**: AccountSummaryViewModel emits effect, MainScreen routes to MainCoordinatorViewModel

**Example**: Show transactions for an account

```kotlin
// AccountSummaryViewModel
private fun showAccountTransactions(accountName: String) {
    viewModelScope.launch {
        _effects.send(AccountSummaryEffect.ShowAccountTransactions(accountName))
    }
}

// MainScreen collects and routes
accountSummaryViewModel.effects.collect { effect ->
    when (effect) {
        is AccountSummaryEffect.ShowAccountTransactions -> {
            coordinatorViewModel.onEvent(MainCoordinatorEvent.SelectTab(MainTab.Transactions))
            transactionListViewModel.onEvent(TransactionListEvent.SetAccountFilter(effect.accountName))
        }
    }
}
```

## Troubleshooting

### Q: My change isn't reflected in the UI

1. Check you're updating the correct ViewModel's state
2. Verify the Composable is collecting from the correct ViewModel
3. Use Android Studio's Layout Inspector to verify Compose state

### Q: Tests are failing after my change

1. Run only the relevant component test to isolate the issue
2. Check if you need to update Fake repositories
3. Verify state updates are happening on the correct dispatcher

### Q: How do I debug state flow?

Add logging in the ViewModel:
```kotlin
private fun updateState(transform: (UiState) -> UiState) {
    val oldState = _uiState.value
    _uiState.update(transform)
    Logger.debug("ViewModel", "State: $oldState -> ${_uiState.value}")
}
```

## Best Practices

1. **Single Responsibility**: Each ViewModel handles one domain
2. **Immutable State**: Always use `copy()` to update state
3. **Event Handling**: Use sealed classes for exhaustive handling
4. **Testing**: Write tests before implementation (TDD)
5. **Under 300 Lines**: If a ViewModel grows too large, consider further splitting
6. **No Cross-VM Dependencies**: Use Repositories or Effects for communication
