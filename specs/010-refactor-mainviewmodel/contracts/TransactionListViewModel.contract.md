# TransactionListViewModel Contract

**Component**: TransactionListViewModel
**Purpose**: Manages transaction list display, filtering, and date navigation
**Target Size**: ~220 lines

## State

### Exposed StateFlows

| Name | Type | Description |
|------|------|-------------|
| `uiState` | `StateFlow<TransactionListUiState>` | Current UI state for transaction list |

### TransactionListUiState (Existing)

```kotlin
data class TransactionListUiState(
    val transactions: ImmutableList<TransactionListDisplayItem> = persistentListOf(),
    val isLoading: Boolean = false,
    val accountFilter: String? = null,
    val showAccountFilterInput: Boolean = false,
    val accountSuggestions: ImmutableList<String> = persistentListOf(),
    val foundTransactionIndex: Int? = null,
    val firstTransactionDate: SimpleDate? = null,
    val lastTransactionDate: SimpleDate? = null,
    val headerText: String = "----",
    val error: String? = null
)
```

## Events

### TransactionListEvent (Existing)

| Event | Parameters | Description |
|-------|------------|-------------|
| `SetAccountFilter` | `accountName: String?` | Set account filter |
| `ShowAccountFilterInput` | None | Show filter input field |
| `HideAccountFilterInput` | None | Hide filter input field |
| `ClearAccountFilter` | None | Clear account filter |
| `GoToDate` | `date: SimpleDate` | Navigate to specific date |
| `ScrollToTransaction` | `index: Int` | Scroll to transaction at index |
| `SelectSuggestion` | `accountName: String` | Select account suggestion |

### Event Handler

```kotlin
fun onEvent(event: TransactionListEvent)
```

## Effects

None. All actions are contained within the transaction list.

## Dependencies

| Dependency | Type | Purpose |
|------------|------|---------|
| `ProfileRepository` | Constructor injection | Observe current profile |
| `TransactionRepository` | Constructor injection | Load transactions |
| `AccountRepository` | Constructor injection | Search account names for suggestions |

## Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `onEvent` | `fun onEvent(event: TransactionListEvent)` | Handle user events |
| `updateHeaderText` | `fun updateHeaderText(text: String)` | Update header text from sync info |
| `updateDisplayedTransactionsFromWeb` | `fun updateDisplayedTransactionsFromWeb(list: List<LedgerTransaction>)` | Update transactions from web sync |

## Internal Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `loadTransactions` | `private fun loadTransactions()` | Load transactions from repository |
| `setAccountFilter` | `private fun setAccountFilter(accountName: String?)` | Apply account filter |
| `clearAccountFilter` | `private fun clearAccountFilter()` | Clear account filter |
| `goToDate` | `private fun goToDate(date: SimpleDate)` | Find transaction at date |
| `searchAccountNames` | `private fun searchAccountNames(query: String)` | Search for account name suggestions |
| `observeAccountSearch` | `private fun observeAccountSearch()` | Debounced account search |
| `updateDisplayedTransactions` | `private fun updateDisplayedTransactions(items: List<TransactionListItem>, count: Int)` | Convert and display transactions |

## Behavior

### Init

1. Observe ProfileRepository.currentProfile
2. When profile changes, clear transactions (will reload when tab selected)
3. Set up debounced account search observation

### loadTransactions()

1. Get profileId and accountFilter from state
2. Set isLoading = true
3. Get transactions from TransactionRepository.getTransactionsFiltered()
4. Build TransactionListDisplayItem list via TransactionAccumulator
5. Update date range (firstTransactionDate, lastTransactionDate)
6. Update uiState with transactions
7. Set isLoading = false

### setAccountFilter(accountName)

1. Update accountFilter in uiState
2. Update accountSearchQuery for suggestions
3. Reload transactions

### goToDate(date)

1. Find index of transaction or delimiter at date
2. Update foundTransactionIndex in uiState

### searchAccountNames(query)

1. Get account name suggestions from AccountRepository.searchAccountNamesSync()
2. Update accountSuggestions in uiState

## Test Cases

### Unit Tests (TransactionListViewModelTest.kt)

| Test | Description |
|------|-------------|
| `init_observesCurrentProfile` | Verify profile observation is set up |
| `profileChange_clearsTransactions` | Verify transactions are cleared on profile change |
| `loadTransactions_success` | Verify transactions are loaded correctly |
| `loadTransactions_showsLoadingState` | Verify isLoading is true during load |
| `loadTransactions_error_showsError` | Verify error state on failure |
| `setAccountFilter_appliesFilter` | Verify filter is applied |
| `setAccountFilter_reloadsTransactions` | Verify transactions are reloaded with filter |
| `clearAccountFilter_clearsFilter` | Verify filter is cleared |
| `goToDate_findsTransaction` | Verify correct transaction index is found |
| `goToDate_notFound_noChange` | Verify no change when date not found |
| `searchAccountNames_returnsSuggestions` | Verify suggestions are returned |
| `searchAccountNames_debounced` | Verify search is debounced |
| `selectSuggestion_appliesFilter` | Verify suggestion selection applies filter |

### Test Setup

```kotlin
@HiltViewModelTest
class TransactionListViewModelTest {
    private val fakeProfileRepository = FakeProfileRepositoryForViewModel()
    private val fakeTransactionRepository = FakeTransactionRepositoryForViewModel()
    private val fakeAccountRepository = FakeAccountRepositoryForViewModel()
    private lateinit var viewModel: TransactionListViewModel

    @Before
    fun setup() {
        viewModel = TransactionListViewModel(
            fakeProfileRepository,
            fakeTransactionRepository,
            fakeAccountRepository
        )
    }
}
```
