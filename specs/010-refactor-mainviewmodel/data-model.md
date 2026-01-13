# Data Model: MainViewModel Refactoring

**Feature**: MainViewModel Refactoring for Improved Maintainability and Testability
**Date**: 2026-01-13

## Overview

This document defines the UiState structures for the 4 new ViewModels that will replace the monolithic MainViewModel.

## Entity Definitions

### 1. ProfileSelectionUiState

**Purpose**: State for profile selection and management in the navigation drawer

```kotlin
data class ProfileSelectionUiState(
    val currentProfileId: Long? = null,
    val currentProfileName: String = "",
    val currentProfileTheme: Int = -1,
    val currentProfileCanPost: Boolean = false,
    val profiles: List<ProfileListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Existing class, reused
data class ProfileListItem(
    val id: Long,
    val name: String,
    val theme: Int,
    val canPost: Boolean
)
```

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| currentProfileId | Long? | ID of the currently selected profile |
| currentProfileName | String | Name of the current profile |
| currentProfileTheme | Int | Theme ID for the current profile |
| currentProfileCanPost | Boolean | Whether the current profile can post transactions |
| profiles | List<ProfileListItem> | All available profiles |
| isLoading | Boolean | Loading state for profile operations |
| error | String? | Error message if profile operation fails |

---

### 2. AccountSummaryUiState

**Purpose**: State for the account summary tab (existing, minimal changes)

```kotlin
// Existing class - add error field
data class AccountSummaryUiState(
    val accounts: List<AccountSummaryListItem> = emptyList(),
    val showZeroBalanceAccounts: Boolean = true,
    val isLoading: Boolean = false,
    val headerText: String = "----",
    val error: String? = null  // NEW: Error state
)
```

**Changes from Existing**:
- Add `error: String?` field for error state (per clarification Q2)
- All other fields remain unchanged

---

### 3. TransactionListUiState

**Purpose**: State for the transaction list tab (existing, minimal changes)

```kotlin
// Existing class - add error field
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
    val error: String? = null  // NEW: Error state
)
```

**Changes from Existing**:
- Add `error: String?` field for error state (per clarification Q2)
- All other fields remain unchanged

---

### 4. MainCoordinatorUiState

**Purpose**: State for UI coordination (tab, drawer, refresh, navigation)

```kotlin
data class MainCoordinatorUiState(
    val selectedTab: MainTab = MainTab.Accounts,
    val isDrawerOpen: Boolean = false,
    val isRefreshing: Boolean = false,
    val backgroundTaskProgress: Float = 0f,
    val backgroundTasksRunning: Boolean = false,
    val lastUpdateDate: Date? = null,
    val lastUpdateTransactionCount: Int = 0,
    val lastUpdateAccountCount: Int = 0,
    val updateError: String? = null
)
```

**Note**: This replaces `MainUiState` but removes profile-specific fields (moved to ProfileSelectionUiState)

**Fields**:
| Field | Type | Description |
|-------|------|-------------|
| selectedTab | MainTab | Currently selected tab (Accounts/Transactions) |
| isDrawerOpen | Boolean | Whether navigation drawer is open |
| isRefreshing | Boolean | Whether pull-to-refresh is active |
| backgroundTaskProgress | Float | Progress of background sync (0-1) |
| backgroundTasksRunning | Boolean | Whether background tasks are running |
| lastUpdateDate | Date? | Date of last successful sync |
| lastUpdateTransactionCount | Int | Number of transactions in last sync |
| lastUpdateAccountCount | Int | Number of accounts in last sync |
| updateError | String? | Error message if sync fails |

---

### 5. PreferencesRepository

**Purpose**: Abstract preferences storage (replaces App static methods)

```kotlin
interface PreferencesRepository {
    fun getShowZeroBalanceAccounts(): Boolean
    fun setShowZeroBalanceAccounts(value: Boolean)
}
```

**Implementation Notes**:
- Uses SharedPreferences internally (per research.md Decision 2)
- Singleton-scoped via Hilt
- No reactive updates needed (simple get/set)

---

## Event Definitions

### ProfileSelectionEvent

```kotlin
sealed class ProfileSelectionEvent {
    data class SelectProfile(val profileId: Long) : ProfileSelectionEvent()
    data class ReorderProfiles(val orderedProfiles: List<ProfileListItem>) : ProfileSelectionEvent()
}
```

### AccountSummaryEvent (Existing)

```kotlin
// No changes needed
sealed class AccountSummaryEvent {
    data object ToggleZeroBalanceAccounts : AccountSummaryEvent()
    data class ToggleAccountExpanded(val accountId: Long) : AccountSummaryEvent()
    data class ToggleAmountsExpanded(val accountId: Long) : AccountSummaryEvent()
    data class ShowAccountTransactions(val accountName: String) : AccountSummaryEvent()
}
```

### TransactionListEvent (Existing)

```kotlin
// No changes needed
sealed class TransactionListEvent {
    data class SetAccountFilter(val accountName: String?) : TransactionListEvent()
    data object ShowAccountFilterInput : TransactionListEvent()
    data object HideAccountFilterInput : TransactionListEvent()
    data object ClearAccountFilter : TransactionListEvent()
    data class GoToDate(val date: SimpleDate) : TransactionListEvent()
    data class ScrollToTransaction(val index: Int) : TransactionListEvent()
    data class SelectSuggestion(val accountName: String) : TransactionListEvent()
}
```

### MainCoordinatorEvent

```kotlin
sealed class MainCoordinatorEvent {
    data class SelectTab(val tab: MainTab) : MainCoordinatorEvent()
    data object OpenDrawer : MainCoordinatorEvent()
    data object CloseDrawer : MainCoordinatorEvent()
    data object RefreshData : MainCoordinatorEvent()
    data object CancelRefresh : MainCoordinatorEvent()
    data object AddNewTransaction : MainCoordinatorEvent()
    data class EditProfile(val profileId: Long) : MainCoordinatorEvent()
    data object CreateNewProfile : MainCoordinatorEvent()
    data object NavigateToTemplates : MainCoordinatorEvent()
    data object NavigateToBackups : MainCoordinatorEvent()
    data object ClearUpdateError : MainCoordinatorEvent()
}
```

---

## Effect Definitions

### ProfileSelectionEffect

```kotlin
// No effects - profile changes propagate via ProfileRepository.currentProfile StateFlow
// Other ViewModels observe this StateFlow and react accordingly
```

### AccountSummaryEffect

```kotlin
sealed class AccountSummaryEffect {
    data class ShowAccountTransactions(val accountName: String) : AccountSummaryEffect()
}
```

**Note**: When user taps an account to see its transactions, this effect is sent to MainCoordinatorViewModel to handle the tab switch.

### TransactionListEffect

```kotlin
// No effects - all actions are contained within the transaction list
```

### MainCoordinatorEffect

```kotlin
// Reuses existing MainEffect with minor rename
sealed class MainCoordinatorEffect {
    data class NavigateToNewTransaction(val profileId: Long, val theme: Int) : MainCoordinatorEffect()
    data class NavigateToProfileDetail(val profileId: Long?) : MainCoordinatorEffect()
    data object NavigateToTemplates : MainCoordinatorEffect()
    data object NavigateToBackups : MainCoordinatorEffect()
    data class ShowError(val message: String) : MainCoordinatorEffect()
}
```

---

## Relationships

### Shared State Flow

```
ProfileRepository.currentProfile (StateFlow<Profile?>)
        │
        ├─── ProfileSelectionViewModel (observes, updates)
        ├─── AccountSummaryViewModel (observes, reloads accounts on change)
        ├─── TransactionListViewModel (observes, reloads transactions on change)
        └─── MainCoordinatorViewModel (observes, uses for navigation effects)
```

### Dependency Graph

```
PreferencesRepository
        │
        └─── AccountSummaryViewModel (reads/writes zero balance preference)

ProfileRepository
        │
        ├─── ProfileSelectionViewModel (primary owner)
        ├─── AccountSummaryViewModel (observes profile changes)
        ├─── TransactionListViewModel (observes profile changes)
        └─── MainCoordinatorViewModel (observes for navigation)

AccountRepository
        │
        ├─── AccountSummaryViewModel (loads accounts)
        └─── TransactionListViewModel (searches account names)

TransactionRepository
        │
        └─── TransactionListViewModel (loads transactions)

BackgroundTaskManager
        │
        └─── MainCoordinatorViewModel (observes task state)

AppStateService
        │
        └─── MainCoordinatorViewModel (drawer state, sync info)
```

---

## Validation Rules

### UiState Requirements

1. All UiState classes MUST include `isLoading: Boolean` and `error: String?` fields
2. All UiState classes MUST be immutable data classes
3. Use `ImmutableList` from kotlinx.collections for list stability in Compose

### Event Requirements

1. Events are sealed classes for exhaustive handling
2. Events should be named in imperative form (e.g., `SelectProfile`, `ToggleZeroBalanceAccounts`)
3. Events carry only the data needed to perform the action

### Effect Requirements

1. Effects are one-shot, delivered via `Channel<Effect>.receiveAsFlow()`
2. Effects represent actions that happen once (navigation, showing dialogs)
3. Effects should not be replayed on configuration change

---

## State Transitions

### Profile Selection Flow

```
User taps profile in drawer
    │
    ▼
ProfileSelectionEvent.SelectProfile(profileId)
    │
    ▼
ProfileSelectionViewModel.selectProfile()
    │
    ├─── ProfileRepository.setCurrentProfile(profile)
    │         │
    │         ▼
    │    ProfileRepository.currentProfile emits new value
    │         │
    │         ├─── AccountSummaryViewModel observes → reloads accounts
    │         ├─── TransactionListViewModel observes → clears transactions
    │         └─── MainCoordinatorViewModel observes → updates profile info for FAB
    │
    └─── Close drawer
```

### Refresh Flow

```
User pulls to refresh
    │
    ▼
MainCoordinatorEvent.RefreshData
    │
    ▼
MainCoordinatorViewModel.refreshData()
    │
    ├─── scheduleTransactionListRetrieval()
    │         │
    │         ▼
    │    RetrieveTransactionsTask starts (Thread-based)
    │         │
    │         ▼
    │    BackgroundTaskManager.isRunning = true
    │         │
    │         ▼
    │    MainCoordinatorViewModel observes → isRefreshing = true
    │
    ▼
Task completes
    │
    ├─── AppStateService.dataVersion increments
    │         │
    │         ├─── AccountSummaryViewModel observes → reloads accounts
    │         └─── TransactionListViewModel observes → reloads transactions
    │
    └─── BackgroundTaskManager.isRunning = false
              │
              ▼
         MainCoordinatorViewModel observes → isRefreshing = false
```

### Account Filter Flow

```
User taps account in AccountSummaryTab
    │
    ▼
AccountSummaryEvent.ShowAccountTransactions(accountName)
    │
    ▼
AccountSummaryViewModel emits AccountSummaryEffect.ShowAccountTransactions
    │
    ▼
MainScreen collects effect → calls MainCoordinatorViewModel
    │
    ├─── MainCoordinatorEvent.SelectTab(Transactions)
    └─── TransactionListEvent.SetAccountFilter(accountName)
              │
              ▼
         TransactionListViewModel.setAccountFilter()
              │
              ▼
         reloadTransactions() with filter
```
