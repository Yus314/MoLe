# Research & Architectural Decisions: MainViewModel Refactoring

**Feature**: MainViewModel Refactoring for Improved Maintainability and Testability
**Date**: 2026-01-13
**Current State**: MainViewModel.kt is 853 lines (target: under 300 lines per component)

## Decision 1: Component Boundaries

**Decision**: Split MainViewModel into 4 components:
1. **ProfileSelectionViewModel** - Profile management (selection, reordering)
2. **AccountSummaryViewModel** - Account list display (loading, filtering, expansion)
3. **TransactionListViewModel** - Transaction list display (loading, filtering, date navigation)
4. **MainCoordinatorViewModel** - UI orchestration (tab state, drawer state, navigation effects)

**Rationale**: Based on analysis of MainViewModel.kt (853 lines), I identified the following distinct responsibility clusters:

| Responsibility | Lines (approx) | Target Component |
|----------------|----------------|------------------|
| Profile state/selection | 75-90, 248-336 | ProfileSelectionViewModel |
| Account summary state/logic | 98-99, 338-383, 532-656 | AccountSummaryViewModel |
| Transaction list state/logic | 101-116, 389-435, 658-851 | TransactionListViewModel |
| UI orchestration (tab, drawer, refresh) | 95-97, 190-327, 438-530, 749-814 | MainCoordinatorViewModel |

**Alternatives Considered**:
- Alternative A: 3-way split without Coordinator - rejected because navigation/drawer logic needs a home
- Alternative B: 5-way split separating navigation from coordination - rejected as over-engineering

**Implementation Notes**:
- Coordinator handles ONLY UI orchestration (tab state, drawer state, navigation events), delegates all domain logic to specialized ViewModels
- All ViewModels observe ProfileRepository.currentProfile as shared state source
- No direct ViewModel-to-ViewModel communication; all coordination via Repository StateFlows

---

## Decision 2: PreferencesRepository Implementation

**Decision**: Use SharedPreferences via PreferencesRepository interface

**Rationale**:
- SharedPreferences sufficient for simple key-value storage (showZeroBalanceAccounts boolean)
- Synchronous API acceptable for non-performance-critical reads
- Existing pattern in codebase (App.getShowZeroBalanceAccounts() uses SharedPreferences)
- Lower complexity than DataStore for this use case
- No migration needed from existing SharedPreferences storage

**Alternatives Considered**:
- Alternative A: DataStore Preferences - rejected due to added complexity for simple boolean flags, requires migration from SharedPreferences, adds Flow wrapper for synchronous read

**Implementation Notes**:
- PreferencesRepository exposes `getShowZeroBalanceAccounts(): Boolean` and `setShowZeroBalanceAccounts(value: Boolean)`
- Injected via Hilt as singleton-scoped
- Replaces static `App.getShowZeroBalanceAccounts()` and `App.storeShowZeroBalanceAccounts()`
- Interface allows easy test mocking

---

## Decision 3: Multi-ViewModel Activity Pattern

**Decision**: Pass all ViewModels to MainScreen Composable, let child Composables access specific ViewModels

**Rationale**:
- MainActivityCompose injects all 4 ViewModels via `by viewModels()`
- MainScreen receives all ViewModels and distributes to child Composables:
  - NavigationDrawerContent receives ProfileSelectionViewModel state/events
  - AccountSummaryTab receives AccountSummaryViewModel state/events
  - TransactionListTab receives TransactionListViewModel state/events
  - MainScreen top-level uses MainCoordinatorViewModel for tab/drawer/refresh

**Alternatives Considered**:
- Alternative A: Use Coordinator to mediate all ViewModel interactions - rejected as over-engineering, adds unnecessary indirection
- Alternative B: Direct hiltViewModel() calls in each Composable - rejected because it makes testing harder and creates implicit dependencies

**Implementation Notes**:
- Each child Composable receives only the UiState and event handler it needs (not the entire ViewModel)
- This follows Compose best practices: state hoisting and unidirectional data flow
- Effects from all ViewModels are collected in MainScreen and dispatched to appropriate handlers

---

## Decision 4: Shared State Testing Strategy

**Decision**: Use Fake repositories with controlled state for independent component testing

**Rationale**:
- Components observe same Repository StateFlow (e.g., ProfileRepository.currentProfile)
- Each component can be tested independently with FakeProfileRepository
- Test controls profile changes via Fake, verifies component reacts correctly
- No mocking of other ViewModels needed

**Testing Pattern**:
```kotlin
class ProfileSelectionViewModelTest {
    private val fakeProfileRepository = FakeProfileRepositoryForViewModel()
    private lateinit var viewModel: ProfileSelectionViewModel

    @Test
    fun `selectProfile updates repository`() = runTest {
        // Given
        fakeProfileRepository.setProfiles(listOf(profile1, profile2))
        viewModel = ProfileSelectionViewModel(fakeProfileRepository)

        // When
        viewModel.onEvent(ProfileSelectionEvent.SelectProfile(profile2.id))

        // Then
        assertEquals(profile2, fakeProfileRepository.currentProfile.value)
    }
}
```

**Alternatives Considered**:
- Alternative A: Mock all Repository interactions - rejected because Fakes are more maintainable and test real behavior
- Alternative B: Integration tests with all ViewModels together - rejected as too slow, doesn't isolate failures

**Implementation Notes**:
- Existing FakeProfileRepositoryForViewModel, FakeAccountRepositoryForViewModel, FakeTransactionRepositoryForViewModel can be reused
- New FakePreferencesRepository needed for AccountSummaryViewModel tests
- StandardTestDispatcher used for coroutine control

---

## Decision 5: Test Migration Strategy

**Decision**: Migrate MainViewModelTest cases to appropriate component test files incrementally

**Test Migration Mapping**:

| Current Test (MainViewModelTest) | Target Test File | Reason |
|----------------------------------|-----------------|--------|
| Profile selection tests | ProfileSelectionViewModelTest | Profile logic moves there |
| Account loading tests | AccountSummaryViewModelTest | Account logic moves there |
| Zero balance filter tests | AccountSummaryViewModelTest | Filter logic moves there |
| Account expansion tests | AccountSummaryViewModelTest | UI state for accounts |
| Transaction loading tests | TransactionListViewModelTest | Transaction logic moves there |
| Account filter tests | TransactionListViewModelTest | Filter logic moves there |
| Date navigation tests | TransactionListViewModelTest | Date logic moves there |
| Tab selection tests | MainCoordinatorViewModelTest | UI coordination |
| Drawer state tests | MainCoordinatorViewModelTest | UI coordination |
| Refresh/sync tests | MainCoordinatorViewModelTest | Orchestration |
| Navigation effect tests | MainCoordinatorViewModelTest | Navigation effects |

**Migration Order**:
1. Write new component tests FIRST (TDD - tests must fail initially)
2. Implement component
3. Run new tests (must pass)
4. Verify all existing tests still pass
5. After all components complete, delete MainViewModelTest.kt

**Alternatives Considered**:
- Alternative A: Keep MainViewModelTest and add component tests - rejected as duplication
- Alternative B: Delete MainViewModelTest first - rejected as loses safety net during migration

**Implementation Notes**:
- Each phase maintains 100% test coverage during transition
- Rollback policy: if any test fails, revert and retry
- Final deletion of MainViewModelTest only after all tests migrated and passing

---

## MainViewModel Responsibility Mapping

### Current MainViewModel Structure (853 lines)

| Section | Lines | Responsibility | Target Component |
|---------|-------|----------------|------------------|
| State declarations | 75-116 | Profile, account, transaction state | Split across all |
| Init block | 117-122 | Load initial data, observe changes | Split across all |
| observeTaskRunning | 129-140 | Refresh state from BackgroundTaskManager | MainCoordinatorViewModel |
| loadInitialData | 142-144 | Zero balance preference | AccountSummaryViewModel |
| observeAccountSearch | 151-167 | Account search debounce | TransactionListViewModel |
| onMainEvent | 190-206 | Tab/drawer/refresh/navigation events | MainCoordinatorViewModel |
| onAccountSummaryEvent | 208-215 | Account toggle/expand events | AccountSummaryViewModel |
| onTransactionListEvent | 217-227 | Filter/date/scroll events | TransactionListViewModel |
| selectTab | 230-246 | Tab selection logic | MainCoordinatorViewModel |
| selectProfile | 248-255 | Profile selection | ProfileSelectionViewModel |
| openDrawer/closeDrawer | 257-271 | Drawer state | MainCoordinatorViewModel |
| refreshData/cancelRefresh | 273-285 | Sync orchestration | MainCoordinatorViewModel |
| addNewTransaction | 287-299 | Navigation effect | MainCoordinatorViewModel |
| editProfile/createNewProfile | 301-311 | Navigation effects | MainCoordinatorViewModel |
| navigateToTemplates/Backups | 313-323 | Navigation effects | MainCoordinatorViewModel |
| reorderProfiles | 329-336 | Profile reordering | ProfileSelectionViewModel |
| toggleZeroBalanceAccounts | 339-344 | Zero balance filter | AccountSummaryViewModel |
| toggleAccountExpanded | 346-363 | Account expansion | AccountSummaryViewModel |
| toggleAmountsExpanded | 365-381 | Amounts expansion | AccountSummaryViewModel |
| showAccountTransactions | 384-387 | Cross-tab navigation | MainCoordinatorViewModel |
| setAccountFilter | 390-399 | Filter transactions | TransactionListViewModel |
| showAccountFilterInput | 401-407 | Filter UI state | TransactionListViewModel |
| clearAccountFilter | 409-417 | Clear filter | TransactionListViewModel |
| goToDate | 419-431 | Date navigation | TransactionListViewModel |
| updateProfile | 438-459 | Profile change handling | MainCoordinatorViewModel |
| updateProfiles | 461-474 | Profile list updates | ProfileSelectionViewModel |
| reloadAccounts | 532-609 | Account loading | AccountSummaryViewModel |
| removeZeroAccounts | 611-654 | Zero balance filtering | AccountSummaryViewModel |
| reloadTransactions | 658-683 | Transaction loading | TransactionListViewModel |
| updateDisplayedTransactions | 685-747 | Transaction display | TransactionListViewModel |
| scheduleTransactionListRetrieval | 749-780 | Sync scheduling | MainCoordinatorViewModel |
| stopTransactionsRetrieval | 782-789 | Sync cancellation | MainCoordinatorViewModel |
| reloadDataAfterChange | 800-814 | Data change handling | MainCoordinatorViewModel |
| updateDisplayedTransactionsFromWeb | 817-821 | Web transaction update | TransactionListViewModel |
| TransactionsDisplayedFilter | 823-852 | Filter thread | TransactionListViewModel |

---

## Component Size Estimates

| Component | Estimated Lines | Methods |
|-----------|-----------------|---------|
| ProfileSelectionViewModel | ~120 | selectProfile, reorderProfiles, observeProfiles |
| AccountSummaryViewModel | ~220 | reloadAccounts, toggleZeroBalance, toggleExpanded, removeZeroAccounts |
| TransactionListViewModel | ~220 | reloadTransactions, setAccountFilter, goToDate, observeSearch |
| MainCoordinatorViewModel | ~200 | selectTab, openDrawer, refreshData, navigate*, scheduleRetrieval |
| PreferencesRepository | ~40 | get/setShowZeroBalanceAccounts |

**Total**: ~800 lines split into 4 components + 1 repository, each under 300 lines target

---

## Implementation Order

1. **PreferencesRepository** (Phase 3) - Foundation for AccountSummaryViewModel
2. **ProfileSelectionViewModel** (Phase 4) - MVP, validates pattern
3. **AccountSummaryViewModel** (Phase 5) - Can run parallel after MVP
4. **TransactionListViewModel** (Phase 6) - Can run parallel after MVP
5. **MainCoordinatorViewModel** (Phase 7) - Depends on Phases 4-6
6. **Integration** (Phase 8) - Wire all ViewModels together
7. **Test Migration** (Phase 9) - Clean up test structure
8. **Documentation** (Phase 10) - Finalize documentation
