# AccountSummaryViewModel Contract

**Component**: AccountSummaryViewModel
**Purpose**: Manages account list display, filtering, and expansion state
**Target Size**: ~220 lines

## State

### Exposed StateFlows

| Name | Type | Description |
|------|------|-------------|
| `uiState` | `StateFlow<AccountSummaryUiState>` | Current UI state for account summary |

### AccountSummaryUiState

```kotlin
data class AccountSummaryUiState(
    val accounts: List<AccountSummaryListItem> = emptyList(),
    val showZeroBalanceAccounts: Boolean = true,
    val isLoading: Boolean = false,
    val headerText: String = "----",
    val error: String? = null
)
```

## Events

### AccountSummaryEvent (Existing)

| Event | Parameters | Description |
|-------|------------|-------------|
| `ToggleZeroBalanceAccounts` | None | Toggle zero balance accounts visibility |
| `ToggleAccountExpanded` | `accountId: Long` | Toggle account expansion state |
| `ToggleAmountsExpanded` | `accountId: Long` | Toggle amounts expansion state |
| `ShowAccountTransactions` | `accountName: String` | Navigate to transactions filtered by account |

### Event Handler

```kotlin
fun onEvent(event: AccountSummaryEvent)
```

## Effects

### AccountSummaryEffect

| Effect | Parameters | Description |
|--------|------------|-------------|
| `ShowAccountTransactions` | `accountName: String` | Request MainCoordinatorViewModel to show transactions |

### Effects Channel

```kotlin
val effects: Flow<AccountSummaryEffect>
```

## Dependencies

| Dependency | Type | Purpose |
|------------|------|---------|
| `ProfileRepository` | Constructor injection | Observe current profile |
| `AccountRepository` | Constructor injection | Load accounts |
| `PreferencesRepository` | Constructor injection | Read/write zero balance preference |

## Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `onEvent` | `fun onEvent(event: AccountSummaryEvent)` | Handle user events |
| `updateHeaderText` | `fun updateHeaderText(text: String)` | Update header text from sync info |

## Internal Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `loadAccounts` | `private fun loadAccounts()` | Load accounts from repository |
| `toggleZeroBalanceAccounts` | `private fun toggleZeroBalanceAccounts()` | Toggle zero balance filter |
| `toggleAccountExpanded` | `private fun toggleAccountExpanded(accountId: Long)` | Toggle account expansion |
| `toggleAmountsExpanded` | `private fun toggleAmountsExpanded(accountId: Long)` | Toggle amounts expansion |
| `removeZeroAccounts` | `private fun removeZeroAccounts(list: List<AccountSummaryListItem>)` | Filter zero balance accounts |
| `isParentOf` | `private fun isParentOf(parentName: String, childName: String)` | Check parent-child relationship |

## Behavior

### Init

1. Load showZeroBalanceAccounts from PreferencesRepository
2. Observe ProfileRepository.currentProfile
3. When profile changes, reload accounts

### loadAccounts()

1. Set isLoading = true
2. Get accounts from AccountRepository.getAllWithAmountsSync(profileId, showZeroBalances)
3. Build LedgerAccount hierarchy
4. Convert to AccountSummaryListItem list
5. Apply zero balance filter if needed
6. Update uiState with accounts
7. Set isLoading = false

### toggleZeroBalanceAccounts()

1. Toggle showZeroBalanceAccounts in uiState
2. Store new value via PreferencesRepository.setShowZeroBalanceAccounts()
3. Reload accounts

### toggleAccountExpanded(accountId)

1. Find account in list
2. Toggle isExpanded flag
3. Update uiState with modified list

## Test Cases

### Unit Tests (AccountSummaryViewModelTest.kt)

| Test | Description |
|------|-------------|
| `init_loadsAccountsForCurrentProfile` | Verify accounts are loaded on init |
| `profileChange_reloadsAccounts` | Verify profile change triggers reload |
| `toggleZeroBalanceAccounts_updatesPreferences` | Verify preference is stored |
| `toggleZeroBalanceAccounts_reloadsAccounts` | Verify accounts are reloaded after toggle |
| `toggleAccountExpanded_updatesState` | Verify account expansion is toggled |
| `toggleAmountsExpanded_updatesState` | Verify amounts expansion is toggled |
| `showAccountTransactions_emitsEffect` | Verify effect is emitted |
| `loadAccounts_showsLoadingState` | Verify isLoading is true during load |
| `loadAccounts_error_showsError` | Verify error state on failure |

### Test Setup

```kotlin
@HiltViewModelTest
class AccountSummaryViewModelTest {
    private val fakeProfileRepository = FakeProfileRepositoryForViewModel()
    private val fakeAccountRepository = FakeAccountRepositoryForViewModel()
    private val fakePreferencesRepository = FakePreferencesRepository()
    private lateinit var viewModel: AccountSummaryViewModel

    @Before
    fun setup() {
        viewModel = AccountSummaryViewModel(
            fakeProfileRepository,
            fakeAccountRepository,
            fakePreferencesRepository
        )
    }
}
```
