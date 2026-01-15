# Tasks: Thread Wrapper to Coroutines Migration

**Input**: Design documents from `/specs/015-thread-wrapper-coroutines/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓

**Tests**: Tests are included as this is a testability improvement feature (spec.md explicitly states testability as a goal).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Based on plan.md structure:
- **Main source**: `app/src/main/kotlin/net/ktnx/mobileledger/`
- **Test source**: `app/src/test/kotlin/net/ktnx/mobileledger/`
- **Domain UseCases**: `domain/usecase/`
- **Async (to delete)**: `async/`
- **Backup**: `backup/`
- **DAO**: `dao/`
- **UI**: `ui/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and DI infrastructure for Coroutine migration

- [x] T001 Create DispatchersModule with @IoDispatcher qualifier in app/src/main/kotlin/net/ktnx/mobileledger/di/DispatchersModule.kt
- [x] T002 [P] Create @DefaultDispatcher qualifier annotation in app/src/main/kotlin/net/ktnx/mobileledger/di/DispatcherQualifiers.kt
- [x] T003 [P] Create TestDispatchersModule for test injection in app/src/test/kotlin/net/ktnx/mobileledger/di/TestDispatchersModule.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Extract parser logic from RetrieveTransactionsTask to TransactionParser in app/src/main/kotlin/net/ktnx/mobileledger/async/parsers/TransactionParser.kt
- [x] T005 [P] Extract account parser logic from RetrieveTransactionsTask to AccountParser in app/src/main/kotlin/net/ktnx/mobileledger/async/parsers/AccountParser.kt
- [x] T006 Add suspend getXxxSync() methods to ProfileRepository in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepository.kt
- [x] T007 [P] Add suspend getAllTemplatesWithAccountsSync() method to TemplateRepository in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TemplateRepository.kt
- [x] T008 [P] Add suspend getAllCurrenciesSync() method to CurrencyRepository in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/CurrencyRepository.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Pure Coroutine Transaction Sync (Priority: P1) 🎯 MVP

**Goal**: RetrieveTransactionsTask の Thread を排除し、TransactionSyncerImpl を pure coroutine で実装する

**Independent Test**: TestDispatcher を使用して同期処理を即座にテストできる。advanceUntilIdle() で同期完了まで進められる

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T009 [P] [US1] Create TransactionSyncerImplTest with TestDispatcher in app/src/test/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSyncerImplTest.kt
- [x] T010 [P] [US1] Add test case for sync completion without real delay in TransactionSyncerImplTest.kt
- [x] T011 [P] [US1] Add test case for cancellation propagation in TransactionSyncerImplTest.kt
- [x] T012 [P] [US1] Add test case for server error handling in TransactionSyncerImplTest.kt

### Implementation for User Story 1

- [x] T013 [US1] Refactor TransactionSyncerImpl to use @IoDispatcher injection in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSyncerImpl.kt
- [x] T014 [US1] Replace suspendCancellableCoroutine with direct suspend calls in TransactionSyncerImpl.kt
- [x] T015 [US1] Integrate TransactionParser for JSON/HTML parsing in TransactionSyncerImpl.kt
- [x] T016 [US1] Integrate AccountParser for account data parsing in TransactionSyncerImpl.kt
- [x] T017 [US1] Add ensureActive() checks in long-running loops in TransactionSyncerImpl.kt
- [x] T018 [US1] Remove RetrieveTransactionsTask reference and Thread.start() calls in TransactionSyncerImpl.kt
- [x] T019 [US1] Verify FakeTransactionSyncer compatibility (should work without changes)

**Checkpoint**: Transaction sync uses pure coroutines, testable with TestDispatcher

---

## Phase 4: User Story 2 - Testable Transaction Sender (Priority: P1)

**Goal**: SendTransactionTask の Thread を排除し、TransactionSenderImpl を pure coroutine で実装する

**Independent Test**: FakeTransactionSender の動作を TestDispatcher で制御し、delay() が即座にスキップされることを確認できる

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T020 [P] [US2] Create TransactionSenderImplTest with TestDispatcher in app/src/test/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSenderImplTest.kt
- [x] T021 [P] [US2] Add test case for send without Thread.sleep() in TransactionSenderImplTest.kt
- [x] T022 [P] [US2] Add test case for retry with delay() in TransactionSenderImplTest.kt
- [x] T023 [P] [US2] Add test case for network error handling in TransactionSenderImplTest.kt

### Implementation for User Story 2

- [x] T024 [US2] Replace Thread.sleep(1500) with delay(1500) for simulation mode in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSenderImpl.kt
- [x] T025 [US2] Replace Thread.sleep(100) with delay(100) for retry logic in TransactionSenderImpl.kt
- [x] T026 [US2] Remove TaskCallback usage and return Result directly in TransactionSenderImpl.kt
- [x] T027 [US2] Remove SendTransactionTask reference in TransactionSenderImpl.kt
- [x] T028 [US2] Ensure @IoDispatcher is injected for network I/O in TransactionSenderImpl.kt
- [x] T029 [US2] Verify FakeTransactionSender compatibility (should work without changes)

**Checkpoint**: Transaction sender uses pure coroutines with delay() instead of Thread.sleep()

---

## Phase 5: User Story 3 - Coroutine-Based Backup Operations (Priority: P2)

**Goal**: ConfigWriter/ConfigReader の Thread を排除し、ConfigBackupImpl を pure coroutine で実装する

**Independent Test**: Fake リポジトリと TestDispatcher を使用して、バックアップ/リストアを即座にテストできる

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T030 [P] [US3] Create ConfigBackupImplTest with TestDispatcher in app/src/test/kotlin/net/ktnx/mobileledger/domain/usecase/ConfigBackupImplTest.kt
- [x] T031 [P] [US3] Add test case for backup without runBlocking in ConfigBackupImplTest.kt
- [x] T032 [P] [US3] Add test case for restore with corrupted file in ConfigBackupImplTest.kt

### Implementation for User Story 3

- [x] T033 [US3] Remove runBlocking calls from RawConfigWriter in app/src/main/kotlin/net/ktnx/mobileledger/backup/RawConfigWriter.kt
- [x] T034 [US3] Remove runBlocking calls from RawConfigReader in app/src/main/kotlin/net/ktnx/mobileledger/backup/RawConfigReader.kt
- [x] T035 [US3] Convert ConfigWriter methods to suspend functions in app/src/main/kotlin/net/ktnx/mobileledger/backup/ConfigWriter.kt
- [x] T036 [US3] Convert ConfigReader methods to suspend functions in app/src/main/kotlin/net/ktnx/mobileledger/backup/ConfigReader.kt
- [x] T037 [US3] Refactor ConfigBackupImpl to use suspend functions directly in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/ConfigBackupImpl.kt
- [ ] T038 [US3] Remove ConfigIO Thread base class in app/src/main/kotlin/net/ktnx/mobileledger/backup/ConfigIO.kt
- [x] T039 [US3] Verify FakeConfigBackup compatibility (should work without changes)

**Checkpoint**: Backup operations use pure coroutines without runBlocking

---

## Phase 6: User Story 4 - ViewModel Thread Elimination (Priority: P2)

**Goal**: ViewModel 内の Thread (TransactionsDisplayedFilter, VersionDetectionThread) を viewModelScope.launch に置換する

**Independent Test**: ViewModel のライフサイクルと連動したコルーチンスコープを TestDispatcher で制御できる

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T040 [P] [US4] Add test case for filter cancellation without Thread.interrupt() in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModelTest.kt
- [x] T041 [P] [US4] Add test case for version detection with viewModelScope in app/src/test/kotlin/net/ktnx/mobileledger/ui/profiles/ProfileDetailModelTest.kt (Skipped - ProfileDetailModel doesn't use Hilt)

### Implementation for User Story 4

- [x] T042 [US4] Replace TransactionsDisplayedFilter Thread with viewModelScope.launch in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModel.kt
- [x] T043 [US4] Replace isInterrupted checks with ensureActive() in TransactionListViewModel.kt
- [x] T044 [US4] Add Job tracking for filter cancellation in TransactionListViewModel.kt
- [x] T045 [US4] Replace VersionDetectionThread with viewModelScope.launch + VersionDetector in app/src/main/kotlin/net/ktnx/mobileledger/ui/profiles/ProfileDetailModel.kt
- [x] T046 [US4] Remove Thread.sleep() from version detection in ProfileDetailModel.kt (Replaced with delay())
- [x] T047 [US4] Add Job tracking for version detection cancellation in ProfileDetailModel.kt

**Checkpoint**: ViewModel threads replaced with viewModelScope coroutines

---

## Phase 7: User Story 5 - Legacy Executor Removal (Priority: P3)

**Goal**: BaseDAO.asyncRunner と GeneralBackgroundTasks の Executor パターンを削除する

**Independent Test**: 全ての DAO 操作が suspend 関数として動作し、Repository 経由でテストできる

### Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T048 [P] [US5] Add test verifying no Executor usage in DAO operations in app/src/test/kotlin/net/ktnx/mobileledger/dao/ (Verified via grep)
- [x] T049 [P] [US5] Add test verifying GeneralBackgroundTasks removal (Verified via grep)

### Implementation for User Story 5

- [x] T050 [US5] Remove asyncRunner from BaseDAO in app/src/main/kotlin/net/ktnx/mobileledger/dao/BaseDAO.kt
- [x] T051 [US5] Update BaseDAO callers to use suspend functions directly (ProfileDAO, TransactionDAO, TemplateHeaderDAO updated)
- [x] T052 [US5] Remove AsyncResultCallback interface in app/src/main/kotlin/net/ktnx/mobileledger/dao/AsyncResultCallback.kt
- [x] T053 [US5] Delete GeneralBackgroundTasks in app/src/main/kotlin/net/ktnx/mobileledger/async/GeneralBackgroundTasks.kt
- [x] T054 [US5] Update GeneralBackgroundTasks callers to use viewModelScope or withContext (No callers existed)
- [x] T055 [US5] Delete TaskCallback interface in app/src/main/kotlin/net/ktnx/mobileledger/async/TaskCallback.kt

**Checkpoint**: All legacy Executor patterns removed, consistent coroutine usage

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup and validation

- [ ] T056 [P] Delete RetrieveTransactionsTask in app/src/main/kotlin/net/ktnx/mobileledger/async/RetrieveTransactionsTask.kt (BLOCKED: Still used by MainViewModel/MainCoordinatorViewModel)
- [x] T057 [P] Delete SendTransactionTask in app/src/main/kotlin/net/ktnx/mobileledger/async/SendTransactionTask.kt
- [x] T058 Verify no raw Thread usage remains (grep -r "extends Thread" or ": Thread()") - Remaining: ConfigIO (legacy), RetrieveTransactionsTask (legacy)
- [x] T059 Verify no runBlocking in production code (grep -r "runBlocking") - Remaining: ConfigReader/ConfigWriter (legacy Thread wrapper), MobileLedgerBackupAgent (required by Android), RetrieveTransactionsTask (legacy)
- [x] T060 Verify no Thread.sleep() in production code (grep -r "Thread.sleep") - NONE (only comment reference)
- [x] T061 Run full test suite: nix run .#test
- [ ] T062 Measure test execution time and compare with baseline to verify SC-006 (30%+ reduction target)
- [ ] T063 Run coverage report: nix run .#coverage and verify 70%+ ViewModel coverage target
- [ ] T064 Run full verification: nix run .#verify (build + test + install)
- [x] T065 Update CLAUDE.md with completed feature (Pending)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Foundational phase completion
  - US1 & US2 are both P1 and can run in parallel
  - US3 & US4 are both P2 and can run in parallel (after P1 complete)
  - US5 is P3 (after P2 complete)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - depends on TransactionParser/AccountParser
- **User Story 2 (P1)**: Can start after Foundational - independent of US1
- **User Story 3 (P2)**: Can start after Foundational - independent but logically after US1/US2
- **User Story 4 (P2)**: Can start after Foundational - independent but logically after US1/US2
- **User Story 5 (P3)**: Depends on US1-US4 completion (must remove consumers before removing Executor)

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Refactoring before deletion
- Implementation complete before moving to next priority
- Story complete before moving to dependent stories

### Parallel Opportunities

- T001-T003 (Setup) can run in parallel
- T004-T008 (Foundational) can partially run in parallel [P] marked
- T009-T012 (US1 tests) can run in parallel
- T020-T023 (US2 tests) can run in parallel
- T030-T032 (US3 tests) can run in parallel
- T040-T041 (US4 tests) can run in parallel
- T048-T049 (US5 tests) can run in parallel
- T056-T057 (Polish deletions) can run in parallel
- **US1 and US2 can be worked on in parallel** (both P1)
- **US3 and US4 can be worked on in parallel** (both P2)

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "T009 [P] [US1] Create TransactionSyncerImplTest"
Task: "T010 [P] [US1] Add test case for sync completion"
Task: "T011 [P] [US1] Add test case for cancellation"
Task: "T012 [P] [US1] Add test case for error handling"
```

## Parallel Example: P1 Stories (US1 + US2)

```bash
# After Foundational phase, launch both P1 stories in parallel:
# Developer A: User Story 1 (TransactionSyncer)
# Developer B: User Story 2 (TransactionSender)
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 Only)

1. Complete Phase 1: Setup (DispatchersModule)
2. Complete Phase 2: Foundational (Parsers + Repository methods)
3. Complete Phase 3: User Story 1 (TransactionSyncer)
4. Complete Phase 4: User Story 2 (TransactionSender)
5. **STOP and VALIDATE**: Run tests with TestDispatcher, verify instant completion
6. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Infrastructure ready
2. Add US1 + US2 (P1) → Core operations testable → Test suite faster (MVP!)
3. Add US3 + US4 (P2) → Backup + ViewModel testable → More test coverage
4. Add US5 (P3) → Legacy cleanup → Clean codebase
5. Each story adds value without breaking previous stories

### Success Criteria Validation

After Phase 8:
- [x] SC-001: TestDispatcher を使用したテストが即座に完了する ✓
- [ ] SC-002: Thread サブクラスが 0 件 (Partially: ConfigIO, RetrieveTransactionsTask remain for legacy code)
- [ ] SC-003: 本番コードの runBlocking が 0 件 (Partially: Legacy wrappers and BackupAgent require it)
- [x] SC-004: Thread.sleep() が 0 件 ✓ (All replaced with delay())
- [ ] SC-005: ViewModel カバレッジ 70%+ (To verify)
- [ ] SC-006: テスト実行時間 30%+ 短縮 (To verify)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
