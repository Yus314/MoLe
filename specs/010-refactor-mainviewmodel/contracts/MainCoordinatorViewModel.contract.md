# MainCoordinatorViewModel Contract

**Component**: MainCoordinatorViewModel
**Purpose**: Coordinates UI state (tabs, drawer, refresh) and navigation effects
**Target Size**: ~200 lines

## State

### Exposed StateFlows

| Name | Type | Description |
|------|------|-------------|
| `uiState` | `StateFlow<MainCoordinatorUiState>` | Current UI coordination state |
| `isTaskRunning` | `StateFlow<Boolean>` | Delegate to BackgroundTaskManager.isRunning |
| `taskProgress` | `StateFlow<TaskProgress?>` | Delegate to BackgroundTaskManager.progress |
| `lastSyncInfo` | `StateFlow<SyncInfo?>` | Delegate to AppStateService.lastSyncInfo |
| `drawerOpen` | `StateFlow<Boolean>` | Delegate to AppStateService.drawerOpen |
| `dataVersion` | `StateFlow<Long>` | Delegate to AppStateService.dataVersion |

### MainCoordinatorUiState

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
    val updateError: String? = null,
    val currentProfileId: Long? = null,
    val currentProfileTheme: Int = -1,
    val currentProfileCanPost: Boolean = false
)
```

## Events

### MainCoordinatorEvent

| Event | Parameters | Description |
|-------|------------|-------------|
| `SelectTab` | `tab: MainTab` | Select tab (Accounts/Transactions) |
| `OpenDrawer` | None | Open navigation drawer |
| `CloseDrawer` | None | Close navigation drawer |
| `RefreshData` | None | Trigger pull-to-refresh sync |
| `CancelRefresh` | None | Cancel ongoing sync |
| `AddNewTransaction` | None | Navigate to new transaction |
| `EditProfile` | `profileId: Long` | Navigate to profile edit |
| `CreateNewProfile` | None | Navigate to profile creation |
| `NavigateToTemplates` | None | Navigate to templates screen |
| `NavigateToBackups` | None | Navigate to backups screen |
| `ClearUpdateError` | None | Clear sync error message |

### Event Handler

```kotlin
fun onEvent(event: MainCoordinatorEvent)
```

## Effects

### MainCoordinatorEffect

| Effect | Parameters | Description |
|--------|------------|-------------|
| `NavigateToNewTransaction` | `profileId: Long, theme: Int` | Navigate to new transaction screen |
| `NavigateToProfileDetail` | `profileId: Long?` | Navigate to profile detail (null = create new) |
| `NavigateToTemplates` | None | Navigate to templates screen |
| `NavigateToBackups` | None | Navigate to backups screen |
| `ShowError` | `message: String` | Show error message |

### Effects Channel

```kotlin
val effects: Flow<MainCoordinatorEffect>
```

## Dependencies

| Dependency | Type | Purpose |
|------------|------|---------|
| `ProfileRepository` | Constructor injection | Observe current profile for navigation |
| `AccountRepository` | Constructor injection | For scheduleTransactionListRetrieval |
| `TransactionRepository` | Constructor injection | For scheduleTransactionListRetrieval |
| `OptionRepository` | Constructor injection | For scheduleTransactionListRetrieval |
| `BackgroundTaskManager` | Constructor injection | Observe/control background tasks |
| `AppStateService` | Constructor injection | Drawer state, sync info |

## Public Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `onEvent` | `fun onEvent(event: MainCoordinatorEvent)` | Handle user events |
| `toggleDrawer` | `fun toggleDrawer()` | Toggle drawer state |
| `scheduleTransactionListRetrieval` | `fun scheduleTransactionListRetrieval()` | Schedule background sync |
| `stopTransactionsRetrieval` | `fun stopTransactionsRetrieval()` | Stop background sync |
| `transactionRetrievalDone` | `fun transactionRetrievalDone()` | Mark sync as complete |
| `reloadDataAfterChange` | `fun reloadDataAfterChange()` | Notify data changed |

## Internal Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `selectTab` | `private fun selectTab(tab: MainTab)` | Handle tab selection |
| `openDrawer` | `private fun openDrawer()` | Open drawer and notify AppStateService |
| `closeDrawer` | `private fun closeDrawer()` | Close drawer and notify AppStateService |
| `refreshData` | `private fun refreshData()` | Start background sync |
| `cancelRefresh` | `private fun cancelRefresh()` | Cancel background sync |
| `observeTaskRunning` | `private fun observeTaskRunning()` | Observe BackgroundTaskManager state |
| `observeProfile` | `private fun observeProfile()` | Observe current profile for FAB state |

## Behavior

### Init

1. Observe BackgroundTaskManager.isRunning for refresh state
2. Observe ProfileRepository.currentProfile for FAB enabled state
3. Observe AppStateService for drawer state

### selectTab(tab)

1. Update selectedTab in uiState
2. If selecting Transactions tab and transactions empty, trigger load (via callback)

### refreshData()

1. Yield to allow animation frame to complete
2. Call scheduleTransactionListRetrieval()

### scheduleTransactionListRetrieval()

1. Check if task already running (AtomicReference)
2. Get current profile from ProfileRepository
3. Create RetrieveTransactionsTask
4. Start task thread

### addNewTransaction()

1. Get current profile state
2. If profile can post, emit NavigateToNewTransaction effect

## Test Cases

### Unit Tests (MainCoordinatorViewModelTest.kt)

| Test | Description |
|------|-------------|
| `init_observesTaskRunning` | Verify BackgroundTaskManager is observed |
| `selectTab_updatesState` | Verify tab selection updates state |
| `openDrawer_updatesState` | Verify drawer open updates state |
| `closeDrawer_updatesState` | Verify drawer close updates state |
| `refreshData_schedulesTask` | Verify sync task is scheduled |
| `cancelRefresh_stopsTask` | Verify sync task is cancelled |
| `addNewTransaction_emitsEffect` | Verify navigation effect is emitted |
| `addNewTransaction_cannotPost_noEffect` | Verify no effect when profile cannot post |
| `editProfile_emitsEffect` | Verify navigation effect is emitted |
| `createNewProfile_emitsEffect` | Verify navigation effect is emitted |
| `navigateToTemplates_emitsEffect` | Verify navigation effect is emitted |
| `navigateToBackups_emitsEffect` | Verify navigation effect is emitted |
| `isRefreshing_matchesTaskRunning` | Verify isRefreshing tracks task state |

### Test Setup

```kotlin
@HiltViewModelTest
class MainCoordinatorViewModelTest {
    private val fakeProfileRepository = FakeProfileRepositoryForViewModel()
    private val fakeAccountRepository = FakeAccountRepositoryForViewModel()
    private val fakeTransactionRepository = FakeTransactionRepositoryForViewModel()
    private val fakeOptionRepository = FakeOptionRepository()
    private val fakeBackgroundTaskManager = FakeBackgroundTaskManager()
    private val fakeAppStateService = FakeAppStateService()
    private lateinit var viewModel: MainCoordinatorViewModel

    @Before
    fun setup() {
        viewModel = MainCoordinatorViewModel(
            fakeProfileRepository,
            fakeAccountRepository,
            fakeTransactionRepository,
            fakeOptionRepository,
            fakeBackgroundTaskManager,
            fakeAppStateService
        )
    }
}
```
