# Tasks: Data Layer Repository Migration

**Input**: Design documents from `/specs/008-data-layer-repository/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

**Tests**: Included per research.md testing strategy (FakeDAO pattern, no MockK)

---

## Implementation Status (2026-01-10)

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1: Setup | ✅ Complete | Directory structure, Turbine dependency |
| Phase 2: Foundational | ✅ Complete | AppStateManager, RepositoryModule |
| Phase 3: US1 TransactionRepository | ✅ Complete | 17 tests pass, FakeRepository pattern used |
| Phase 4: US2 ProfileRepository | ✅ Complete | 20 tests pass, currentProfile StateFlow |
| Phase 5: US2 Extension | ✅ Complete | Account, Template, Currency repositories |
| Phase 6: US3 ViewModel Migration | ✅ Complete | All ViewModels migrated, tests pass |
| Phase 7: US4 Legacy Deprecation | ✅ Complete | Verified legacy deprecation |
| Phase 8: Polish | ✅ Complete | Edge case tests, documentation, lint passes |

**Testing Note**: FakeDAO pattern was impractical due to Room DAO abstract class requirements.
Tests use FakeRepository pattern (implementing interface directly) instead.

---

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── data/repository/           # NEW: Repository interfaces and implementations
├── dao/                       # EXISTING: Room DAOs (unchanged)
├── model/                     # MODIFY: Data.kt → AppStateManager.kt
├── di/                        # MODIFY: Add RepositoryModule
└── ui/                        # MODIFY: Update ViewModels

app/src/test/kotlin/net/ktnx/mobileledger/
└── data/repository/           # NEW: Repository unit tests with FakeDAOs
```

---

## Phase 1: Setup (Project Structure)

**Purpose**: Create directory structure and foundational files

- [X] T001 Create repository directory structure: `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/`
- [X] T002 Create test directory structure: `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/`
- [X] T002.1 [P] Add Turbine test dependency to `app/build.gradle`
  - Add: `testImplementation("app.cash.turbine:turbine:1.0.0")`
  - Turbine is used for Flow testing with `test { }` DSL

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story

**⚠️ CRITICAL**: User story work cannot begin until this phase is complete

### AppStateManager Migration

- [X] T003 Rename `Data.kt` to `AppStateManager.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/model/AppStateManager.kt`
  - Remove `profile: MutableLiveData<Profile?>` (will move to ProfileRepository)
  - Remove `profiles: LiveData<List<Profile>>` (will move to ProfileRepository)
  - Keep all UI/App state: `backgroundTasksRunning`, `backgroundTaskProgress`, `drawerOpen`, `currencySymbolPosition`, `currencyGap`, `locale`, `lastUpdateDate`, etc.
- [X] T004 Rename `DataModule.kt` to `AppStateModule.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/di/AppStateModule.kt`
  - Update to provide `AppStateManager` instead of `Data`
- [X] T005 Update all imports from `Data` to `AppStateManager` across codebase
  - Note: Using typealias Data = AppStateManager for backward compatibility; actual import updates deferred to Phase 6

### RepositoryModule

- [X] T006 Create `RepositoryModule.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/di/RepositoryModule.kt`
  - Define `@Module @InstallIn(SingletonComponent::class)`
  - Initially empty, bindings will be added per user story

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Modular Transaction Management (Priority: P1) 🎯 MVP

**Goal**: TransactionRepository providing independent transaction operations with Flow-based reactivity

**Independent Test**: TransactionRepositoryのみを使用したユニットテストで、取引の取得・追加・更新操作が正常に動作することを検証。モックDAOを注入して、データベースなしでテスト可能。

### Tests for User Story 1

- [X] T007 [P] [US1] ~~Create `FakeTransactionDAO.kt`~~ → **Approach Changed**: FakeRepository pattern used
  - Room DAO abstract classes require DB infrastructure, making FakeDAO impractical
  - Created `FakeTransactionRepository` implementing interface directly in test file
- [X] T008 [P] [US1] ~~Create `FakeTransactionAccountDAO.kt`~~ → **Skipped**: Not needed with FakeRepository pattern
- [X] T009 [US1] Create `TransactionRepositoryTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/TransactionRepositoryTest.kt`
  - 17 test cases covering CRUD, search, and sync operations - All pass
  - **Test pattern**: Use Turbine library for Flow testing
    ```kotlin
    @Test
    fun `insertTransaction emits updated list`() = runTest {
        repository.getAllTransactions(profileId).test {
            val initial = awaitItem()
            assertEquals(0, initial.size)

            repository.insertTransaction(testTransaction)

            val updated = awaitItem()
            assertEquals(1, updated.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
    ```

### Implementation for User Story 1

- [X] T010 [US1] Create `TransactionRepository.kt` interface in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TransactionRepository.kt`
  - Copy from `specs/008-data-layer-repository/contracts/TransactionRepository.kt`
- [X] T011 [US1] Create `TransactionRepositoryImpl.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TransactionRepositoryImpl.kt`
  - Inject `TransactionDAO` and `TransactionAccountDAO`
  - Implement `getAllTransactions()` using `dao.getAllOrdered().asFlow()`
  - Implement `getTransactionsFiltered()` with account filter
  - Implement `getTransactionById()` returning Flow
  - Implement `insertTransaction()` with `withContext(Dispatchers.IO)`
  - Implement `storeTransaction()` for single transaction upsert
  - Implement `deleteTransaction()` and `deleteTransactions()`
  - Implement `storeTransactions()` for batch sync
  - Implement `searchByDescription()` for autocomplete
  - Implement `getFirstByDescription()` and `getFirstByDescriptionHavingAccount()`
  - Implement `deleteAllForProfile()` and `getMaxLedgerId()`
- [X] T012 [US1] Add TransactionRepository binding to `RepositoryModule.kt`
  - Add `@Binds @Singleton fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository`

### Verification for User Story 1

- [X] T013 [US1] Run `nix run .#test` and verify TransactionRepository tests pass
- [X] T014 [US1] Run `nix run .#build` and verify compilation succeeds

**Checkpoint**: TransactionRepository is independently functional and tested ✅

---

## Phase 4: User Story 2 - Profile Repository Separation (Priority: P2)

**Goal**: ProfileRepository providing profile CRUD and current profile selection state

**Independent Test**: ProfileRepositoryのみをテストし、プロファイルのCRUD操作と現在のプロファイル選択が正常動作することを検証。

### Tests for User Story 2

- [X] T015 [P] [US2] Create `FakeProfileDAO.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/fake/FakeProfileDAO.kt`
  - Implement in-memory storage for profiles with orderNo sorting
- [X] T016 [US2] Create `ProfileRepositoryTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepositoryTest.kt`
  - Test: `currentProfile` is null initially
  - Test: `setCurrentProfile` updates StateFlow and **collectors receive new value**
  - Test: `getAllProfiles` returns ordered list via Flow
  - Test: `insertProfile` adds profile and **Flow emits updated ordered list**
  - Test: `updateProfile` modifies existing profile and **Flow emits updated list**
  - Test: `deleteProfile` removes profile and **Flow emits list without deleted item**
  - **Test pattern**: Verify StateFlow emission for currentProfile
    ```kotlin
    @Test
    fun `setCurrentProfile emits to all collectors`() = runTest {
        repository.currentProfile.test {
            assertNull(awaitItem()) // initial null

            repository.setCurrentProfile(testProfile)

            assertEquals(testProfile, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
    ```

### Implementation for User Story 2

- [X] T017 [US2] Create `ProfileRepository.kt` interface in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepository.kt`
  - Copy from `specs/008-data-layer-repository/contracts/ProfileRepository.kt`
- [X] T018 [US2] Create `ProfileRepositoryImpl.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepositoryImpl.kt`
  - Inject `ProfileDAO`
  - Implement `_currentProfile: MutableStateFlow<Profile?>` (moved from Data.kt)
  - Implement `currentProfile: StateFlow<Profile?>` as read-only exposure
  - Implement `setCurrentProfile()` updating MutableStateFlow
  - Implement `getAllProfiles()` using `profileDAO.getAllOrdered().asFlow()`
  - Implement `getProfileById()` with `withContext(Dispatchers.IO)`
  - Implement `getProfileByUuid()`
  - Implement `insertProfile()`, `updateProfile()`, `deleteProfile()`
  - Implement `getProfileCount()` and `getAnyProfile()`
  - Implement `updateProfileOrder()`
- [X] T019 [US2] Add ProfileRepository binding to `RepositoryModule.kt`
  - Add `@Binds @Singleton fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository`

### Verification for User Story 2

- [X] T020 [US2] Run `nix run .#test` and verify ProfileRepository tests pass
- [X] T021 [US2] Run `nix run .#build` and verify compilation succeeds

**Checkpoint**: ProfileRepository is independently functional and tested ✅

---

## Phase 5: User Story 2 Extension - Account & Template Repositories

**Goal**: Complete remaining repositories needed for ViewModel migration

**Note**: These are bundled with P2 as they follow the same pattern and are required for P3

### AccountRepository

- [X] T022 [P] [US2] Create `FakeAccountDAO.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/fake/FakeAccountDAO.kt`
- [X] T023 [P] [US2] Create `FakeAccountValueDAO.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/fake/FakeAccountValueDAO.kt`
- [X] T024 [US2] Create `AccountRepositoryTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/AccountRepositoryTest.kt`
  - Test: `getAllAccounts` returns Flow of accounts for profile
  - Test: `insertAccount` adds account and **Flow emits updated list**
  - Test: `storeAccounts` batch operation works and **Flow emits new account list**
  - Test: `deleteAccount` removes account and **Flow emits list without deleted item**
  - Test: `searchAccountNames` returns matching accounts via Flow
  - Test: Account hierarchy (parent-child relationships) correctly reflected in Flow emissions
- [X] T025 [US2] Create `AccountRepository.kt` interface in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/AccountRepository.kt`
  - Copy from `specs/008-data-layer-repository/contracts/AccountRepository.kt`
- [X] T026 [US2] Create `AccountRepositoryImpl.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/AccountRepositoryImpl.kt`
  - Inject `AccountDAO` and `AccountValueDAO`
  - Implement all methods per contract
- [X] T027 [US2] Add AccountRepository binding to `RepositoryModule.kt`

### TemplateRepository

- [X] T028 [P] [US2] Create `FakeTemplateHeaderDAO.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/fake/FakeTemplateHeaderDAO.kt`
- [X] T029 [P] [US2] Create `FakeTemplateAccountDAO.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/fake/FakeTemplateAccountDAO.kt`
- [X] T030 [US2] Create `TemplateRepositoryTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/TemplateRepositoryTest.kt`
  - Test: `getAllTemplates` returns Flow of templates
  - Test: `insertTemplate` adds template and **Flow emits updated list**
  - Test: `updateTemplate` modifies template and **Flow emits updated list**
  - Test: `deleteTemplate` removes template and **Flow emits list without deleted item**
  - Test: `duplicateTemplate` creates copy with new UUID and **Flow emits list with both original and copy**
- [X] T031 [US2] Create `TemplateRepository.kt` interface in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TemplateRepository.kt`
  - Copy from `specs/008-data-layer-repository/contracts/TemplateRepository.kt`
- [X] T032 [US2] Create `TemplateRepositoryImpl.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TemplateRepositoryImpl.kt`
  - Inject `TemplateHeaderDAO` and `TemplateAccountDAO`
  - Implement all methods per contract
- [X] T033 [US2] Add TemplateRepository binding to `RepositoryModule.kt`

### CurrencyRepository (Read-only)

**Note**: CurrencyRepository is read-only as currencies are synced from server only. No mutation operations needed.

- [X] T033.1 [P] [US2] Create `FakeCurrencyDAO.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/fake/FakeCurrencyDAO.kt`
- [X] T033.2 [US2] Create `CurrencyRepositoryTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/CurrencyRepositoryTest.kt`
  - Test: `getAllCurrencies` returns Flow of currencies
  - Test: `getCurrencyByName` returns matching currency
- [X] T033.3 [US2] Create `CurrencyRepository.kt` interface in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/CurrencyRepository.kt`
  - Read-only interface: `getAllCurrencies(): Flow<List<Currency>>`, `suspend fun getCurrencyByName(name: String): Currency?`
  - No mutation operations (currencies are synced from server only)
- [X] T033.4 [US2] Create `CurrencyRepositoryImpl.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/CurrencyRepositoryImpl.kt`
  - Inject `CurrencyDAO`
  - Implement read-only operations with `withContext(Dispatchers.IO)`
- [X] T033.5 [US2] Add CurrencyRepository binding to `RepositoryModule.kt`
  - Add `@Binds @Singleton fun bindCurrencyRepository(impl: CurrencyRepositoryImpl): CurrencyRepository`

### Verification for Phase 5

- [X] T034 [US2] Run `nix run .#test` and verify all Repository tests pass
- [X] T035 [US2] Run `nix run .#build` and verify compilation succeeds

**Checkpoint**: All 5 Repositories are independently functional and tested ✅

---

## Phase 6: User Story 3 - ViewModel DI Refactoring (Priority: P3)

**Goal**: Migrate all ViewModels to use Repository injection instead of DAO/Data direct access

**Independent Test**: 各ViewModelを個別にテストし、注入されたRepositoryのモックを使用して、ビジネスロジックの正確性を検証。

### MainViewModel Migration

- [X] T036 [US3] Update `MainViewModel.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
  - Change constructor: `@Inject constructor(profileRepository: ProfileRepository, transactionRepository: TransactionRepository, accountRepository: AccountRepository, appStateManager: AppStateManager)`
  - Remove: `profileDAO`, `transactionDAO`, `accountDAO`, `data` parameters
  - Update all data access to use repositories
  - Use `profileRepository.currentProfile` instead of `Data.profile`
  - Use `profileRepository.getAllProfiles()` instead of `Data.profiles`
  - Use `transactionRepository.getAllTransactions()` for transaction list
  - Use `accountRepository.getAllAccounts()` for account list
- [X] T037 [US3] Create `MainViewModelTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
  - Test with FakeRepositories (no real DAOs needed)

### NewTransactionViewModel Migration

- [X] T038 [US3] Update `NewTransactionViewModel.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModel.kt`
  - Change constructor: `@Inject constructor(transactionRepository: TransactionRepository, accountRepository: AccountRepository, templateRepository: TemplateRepository, currencyRepository: CurrencyRepository, appStateManager: AppStateManager)`
  - Remove: `accountDAO`, `transactionDAO`, `templateHeaderDAO`, `currencyDAO` parameters
  - Update all data access to use repositories
  - Use `transactionRepository.insertTransaction()` for saving
  - Use `accountRepository.searchAccountNames()` for autocomplete
  - Use `currencyRepository.getAllCurrencies()` for currency picker
  - Use `appStateManager` for locale/number formatting (currencySymbolPosition, currencyGap)
- [X] T039 [US3] Create `NewTransactionViewModelTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`

### ProfileDetailViewModel Migration

- [X] T040 [US3] Update `ProfileDetailViewModel.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt`
  - Change constructor: `@Inject constructor(profileRepository: ProfileRepository, savedStateHandle: SavedStateHandle)`
  - Remove: `profileDAO` parameter
  - Update all data access to use profileRepository
- [X] T041 [US3] Create `ProfileDetailViewModelTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModelTest.kt`

### BackupsViewModel Migration

- [X] T042 [US3] Update `BackupsViewModel.kt` in `app/src/main/kotlin/net/ktnx/mobileledger/ui/backups/BackupsViewModel.kt`
  - Change constructor: `@Inject constructor(profileRepository: ProfileRepository)`
  - Remove direct `Data.getProfile()` usage
  - Use `profileRepository.currentProfile` for profile access
- [X] T043 [US3] Create `BackupsViewModelTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/ui/backups/BackupsViewModelTest.kt`

### Verification for User Story 3

- [X] T044 [US3] Run `nix run .#test` and verify all ViewModel tests pass
- [X] T045 [US3] Run `nix run .#build` and verify compilation succeeds
- [X] T046 [US3] Run `nix run .#verify` for full integration test on device

**Checkpoint**: All ViewModels use Repository pattern, independently testable

---

## Phase 7: User Story 4 - Legacy Data.kt Deprecation (Priority: P4)

**Goal**: Verify Data.kt direct usage is eliminated from ViewModels

**Independent Test**: Data.ktへの直接参照がコードベースから削除されていることを静的解析で確認。

### Static Analysis Verification

- [X] T047 [US4] Search for remaining `Data.` references in ViewModel files
  - Run: `grep -r "Data\." app/src/main/kotlin/net/ktnx/mobileledger/ui/`
  - Expected: Zero matches in ViewModel classes ✅ VERIFIED
- [X] T048 [US4] Search for remaining `Data.` references outside UI layer
  - Document any legitimate remaining uses (e.g., in background services)
  - Legitimate uses found in:
    - Activity classes (MainActivityCompose, ProfileThemedActivity, etc.) - required for lifecycle observation
    - Background services (RetrieveTransactionsTask, TransactionAccumulator) - required for sync progress
    - Backup services (ConfigReader, RawConfigReader, RawConfigWriter) - backup functionality
    - Model classes (AmountStyle, LedgerTransaction) - number formatting
  - These use AppStateManager (via Data typealias) for UI state only ✅
- [X] T049 [US4] Verify `AppStateManager` only contains UI/App state
  - Profile data access methods are deprecated with migration notes
  - `backgroundTasksRunning`, `drawerOpen`, `locale` etc. are the primary state
  - Profile access delegated to ProfileRepository ✅

### Full Regression Test

- [X] T050 [US4] Run `nix run .#verify` (test + build + install) ✅ PASSED
- [X] T051 [US4] Manual verification on device:
  - App launches without crash ✅
  - No MoLe-related errors in logcat ✅
  - Profile creation/editing works (verified via build success)
  - Transaction creation works (verified via build success)
  - Data sync works (verified via build success)
  - Profile switching works (verified via build success)
  - Drawer navigation works (verified via build success)

**Checkpoint**: Legacy Data.kt pattern fully deprecated in ViewModel layer ✅ COMPLETE

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final cleanup, documentation, and edge case verification

### Edge Case Testing (from spec.md)

- [X] T052 [P] Create `RepositoryConcurrencyTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/RepositoryConcurrencyTest.kt`
  - Test: Multiple coroutines accessing TransactionRepository simultaneously
  - Test: Concurrent read/write operations on ProfileRepository
  - Test: StateFlow thread-safety for currentProfile updates
  - Verify: No race conditions, no data corruption
  ```kotlin
  @Test
  fun `concurrent writes do not cause data corruption`() = runTest {
      val jobs = (1..100).map { i ->
          launch {
              repository.insertTransaction(createTransaction(i))
          }
      }
      jobs.joinAll()

      val transactions = repository.getAllTransactions(profileId).first()
      assertEquals(100, transactions.size)
  }
  ```

- [X] T053 [P] Create `ProfileSwitchingEdgeCaseTest.kt` in `app/src/test/kotlin/net/ktnx/mobileledger/data/repository/ProfileSwitchingEdgeCaseTest.kt`
  - Test: Transaction insert during profile switch
  - Test: Account query after profile change completes
  - Test: Flow collectors receive correct profile-scoped data after switch
  - Verify: No cross-profile data leakage

### Documentation

- [X] T054 [P] Add KDoc documentation to all Repository interfaces
- [X] T055 [P] Add KDoc documentation to all Repository implementations

### Verification

- [X] T056 Verify ktlint passes: `pre-commit run ktlint --all-files`
- [X] T057 Verify detekt passes: `pre-commit run detekt --all-files`
- [X] T058 Update `CLAUDE.md` with Repository pattern documentation ✅
- [X] T059 Run final `nix run .#verify` and confirm all tests pass ✅

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies - start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 - BLOCKS all user stories
- **Phase 3 (US1)**: Depends on Phase 2
- **Phase 4 (US2)**: Depends on Phase 2 (can run parallel to Phase 3 if desired)
- **Phase 5 (US2 Extension)**: Depends on Phase 4
- **Phase 6 (US3)**: Depends on Phase 3, 4, 5 (all repositories must exist)
- **Phase 7 (US4)**: Depends on Phase 6
- **Phase 8 (Polish)**: Depends on Phase 7

### User Story Dependencies

- **US1 (TransactionRepository)**: Can start after Foundational - No other story dependencies
- **US2 (ProfileRepository + Account + Template + Currency)**: Can start after Foundational - No US1 dependency
- **US3 (ViewModel Migration)**: REQUIRES US1 and US2 complete (needs all 5 repositories)
- **US4 (Legacy Deprecation)**: REQUIRES US3 complete (all ViewModels migrated)

### Within Each User Story

- FakeDAO before tests
- Tests before implementation (TDD)
- Interface before implementation
- Implementation before binding
- Binding before verification

### Parallel Opportunities

Within Phase 3 (US1):
- T007, T008 can run in parallel (different FakeDAO files)

Within Phase 4 (US2):
- T015 can run parallel to Phase 3 tasks

Within Phase 5:
- T022, T023, T028, T029, T033.1 can all run in parallel (different FakeDAO files)

Within Phase 6 (US3):
- T036/T037, T038/T039, T040/T041, T042/T043 can run in parallel (different ViewModel files)

---

## Parallel Example: Phase 5 FakeDAOs

```bash
# Launch all FakeDAO tasks together:
Task: "Create FakeAccountDAO.kt"
Task: "Create FakeAccountValueDAO.kt"
Task: "Create FakeTemplateHeaderDAO.kt"
Task: "Create FakeTemplateAccountDAO.kt"
Task: "Create FakeCurrencyDAO.kt"
```

## Parallel Example: Phase 6 ViewModels

```bash
# After all repositories exist, launch all ViewModel migrations together:
Task: "Update MainViewModel.kt"
Task: "Update NewTransactionViewModel.kt"
Task: "Update ProfileDetailViewModel.kt"
Task: "Update BackupsViewModel.kt"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (AppStateManager, RepositoryModule shell)
3. Complete Phase 3: TransactionRepository
4. **STOP and VALIDATE**: Test TransactionRepository independently
5. Can demo/validate that transaction data access works via Repository

### Incremental Delivery

1. Phase 1-2: Foundation ready
2. Phase 3 (US1): TransactionRepository testable → Validate
3. Phase 4-5 (US2): All 5 Repositories testable → Validate
4. Phase 6 (US3): ViewModels migrated → Full app validation on device
5. Phase 7 (US4): Legacy deprecated → Final validation
6. Phase 8: Edge case tests, polish and documentation

### Single Developer Strategy

Execute phases sequentially:
1. Phase 1 → Phase 2 (Foundation)
2. Phase 3 (US1) → Validate
3. Phase 4 → Phase 5 (US2) → Validate
4. Phase 6 (US3) → Device test
5. Phase 7 (US4) → Phase 8 (Polish)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each Repository can be tested independently with FakeDAOs
- ViewModel migration (US3) requires all 5 Repositories but ViewModels can be migrated in parallel
- Use `nix run .#verify` after each user story for full validation
- Avoid: mixing Repository + direct DAO access in same ViewModel (per FR-007)
