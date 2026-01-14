# Tasks: バックグラウンド処理アーキテクチャの技術的負債解消

**Input**: Design documents from `/specs/013-background-reliability/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/

**Tests**: TDD アプローチ - 各コンポーネントに Fake 実装とテストを含む

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Android Mobile**: `app/src/main/kotlin/net/ktnx/mobileledger/`
- **Tests**: `app/src/test/kotlin/net/ktnx/mobileledger/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: ドメインモデルと共通エラー型の作成

- [X] T001 [P] Create SyncError sealed class in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SyncError.kt
- [X] T002 [P] Create SyncProgress sealed class in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SyncProgress.kt
- [X] T003 [P] Create SyncResult data class in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SyncResult.kt
- [X] T004 [P] Create SyncState sealed class in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SyncState.kt
- [X] T005 [P] Create SendState sealed class in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SendState.kt
- [X] T006 [P] Create BackupState sealed class in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/BackupState.kt
- [X] T007 Create SyncException wrapper class in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SyncException.kt

**Checkpoint**: ドメインモデルが完成し、インターフェース定義の準備完了

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 全 User Story で共有されるインターフェース定義と Fake 実装

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T008 [P] Create TransactionSyncer interface in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSyncer.kt
- [X] T009 [P] Create ConfigBackup interface in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/ConfigBackup.kt
- [X] T010 [P] Create VersionDetector interface in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/VersionDetector.kt
- [X] T011 [P] Create DatabaseInitializer interface in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/DatabaseInitializer.kt
- [X] T012 [P] Create FakeTransactionSyncer in app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeTransactionSyncer.kt
- [X] T013 [P] Create FakeConfigBackup in app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeConfigBackup.kt
- [X] T014 [P] Create FakeVersionDetector in app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeVersionDetector.kt
- [X] T015 [P] Create FakeDatabaseInitializer in app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeDatabaseInitializer.kt
- [ ] T016 Extend UseCaseModule with new interface bindings in app/src/main/kotlin/net/ktnx/mobileledger/di/UseCaseModule.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - 同期処理のユニットテスト (Priority: P1) 🎯 MVP

**Goal**: TransactionSyncer インターフェースと Impl を作成し、RetrieveTransactionsTask をラップ。テストで同期ロジックを検証可能に。

**Independent Test**: FakeTransactionSyncer を使用して MainViewModel の同期処理をテスト。ネットワーク接続なしで成功/失敗/進捗をシミュレート。

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T017 [P] [US1] Create TransactionSyncerImplTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSyncerImplTest.kt
- [X] T018 [P] [US1] Add sync success test case with FakeTransactionSyncer in TransactionSyncerImplTest.kt
- [X] T019 [P] [US1] Add sync error handling test cases (NetworkError, AuthenticationError, TimeoutError) in TransactionSyncerImplTest.kt
- [X] T020 [P] [US1] Add sync progress emission test cases in TransactionSyncerImplTest.kt

### Implementation for User Story 1

- [X] T021 [US1] Create TransactionSyncerImpl wrapping RetrieveTransactionsTask with suspendCancellableCoroutine in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSyncerImpl.kt
- [X] T022 [US1] Implement Flow<SyncProgress> emission in TransactionSyncerImpl.kt
- [X] T023 [US1] Implement error mapping from Thread exceptions to SyncError in TransactionSyncerImpl.kt
- [X] T024 [US1] Add invokeOnCancellation for connection cleanup in TransactionSyncerImpl.kt
- [X] T025 [US1] Add @Binds for TransactionSyncer in UseCaseModule.kt
- [X] T026 [US1] Update MainViewModel to inject TransactionSyncer in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt
- [X] T027 [US1] Add MainViewModel sync tests using FakeTransactionSyncer in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt

**Checkpoint**: TransactionSyncer が Hilt DI 経由で利用可能。MainViewModel で同期処理のテストが可能。

---

## Phase 4: User Story 2 - Coroutines によるキャンセル制御 (Priority: P1)

**Goal**: キャンセル応答時間 5 秒以内を達成。isActive チェックポイントを追加。

**Independent Test**: キャンセルテストで 500ms 以内の応答を確認。

### Tests for User Story 2

- [X] T028 [P] [US2] Create cancellation test for TransactionSyncerImpl in TransactionSyncerImplTest.kt
- [X] T029 [P] [US2] Add cancellation response time assertion (<500ms) in TransactionSyncerImplTest.kt
- [X] T030 [P] [US2] Add structured concurrency test with supervisorScope in TransactionSyncerImplTest.kt

### Implementation for User Story 2

- [X] T031 [US2] Add isActive check points in TransactionSyncerImpl.kt at each processing step
- [X] T032 [US2] Implement connection.disconnect() in invokeOnCancellation in TransactionSyncerImpl.kt
- [X] T033 [US2] Add withTimeout wrapper for network operations in TransactionSyncerImpl.kt
- [X] T034 [US2] Update MainViewModel.cancelSync() to use Job.cancel() in MainViewModel.kt

**Checkpoint**: キャンセル処理が構造化並行性に準拠。5秒以内の応答を達成。

---

## Phase 5: User Story 3 - Hilt DI によるテスト用依存性注入 (Priority: P1)

**Goal**: 全バックグラウンド処理コンポーネントが @Inject で取得可能。テスト時の Fake 差し替えが可能。

**Independent Test**: @HiltAndroidTest で Fake 実装が注入されることを確認。

### Tests for User Story 3

- [X] T035 [P] [US3] Create Hilt test module in app/src/test/kotlin/net/ktnx/mobileledger/di/TestUseCaseModule.kt
- [X] T036 [P] [US3] Add FakeTransactionSyncer binding in TestUseCaseModule.kt
- [X] T037 [P] [US3] Add FakeConfigBackup binding in TestUseCaseModule.kt
- [X] T038 [P] [US3] Add FakeVersionDetector binding in TestUseCaseModule.kt
- [X] T039 [P] [US3] Add FakeDatabaseInitializer binding in TestUseCaseModule.kt

### Implementation for User Story 3

- [X] T040 [US3] Verify UseCaseModule provides all interfaces with @Singleton scope in UseCaseModule.kt
- [X] T041 [US3] Update all ViewModels to use @Inject constructor for background services
- [X] T042 [US3] Document DI usage in CLAUDE.md under Hilt Dependency Injection section

**Checkpoint**: 全 ViewModel がコンストラクタ注入で依存性を受け取り、テストで Fake に差し替え可能。

---

## Phase 6: User Story 4 - エラー伝播の一貫性 (Priority: P2)

**Goal**: 全エラーが SyncError sealed class で表現され、Result<T> で UI まで伝播。

**Independent Test**: 各エラータイプをシミュレートし、UiState.error に適切なエラー情報が設定されることを確認。

### Tests for User Story 4

- [X] T043 [P] [US4] Create SyncError mapping tests in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/SyncErrorTest.kt
- [X] T044 [P] [US4] Add error propagation tests for each error type in MainViewModelTest.kt
- [X] T045 [P] [US4] Add isRetryable property tests in SyncErrorTest.kt

### Implementation for User Story 4

- [X] T046 [US4] Implement mapExceptionToSyncError function in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SyncError.kt
- [X] T047 [US4] Update MainViewModel to handle Result.failure and set UiState.error in MainViewModel.kt
- [X] T048 [US4] Add retry logic based on isRetryable in MainViewModel.kt
- [X] T049 [US4] Update UI layer to display error messages from SyncError.message

**Checkpoint**: エラーハンドリングが一貫。リトライ可能エラーの判定が可能。

---

## Phase 7: User Story 5 - バックアップ処理のテスト可能化 (Priority: P2)

**Goal**: ConfigBackup インターフェースと Impl を作成し、ConfigIO をラップ。テストでバックアップ/リストアをシミュレート可能に。

**Independent Test**: FakeConfigBackup を使用して BackupsViewModel のテストが可能。

### Tests for User Story 5

- [X] T050 [P] [US5] Create ConfigBackupImplTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/usecase/ConfigBackupImplTest.kt
- [X] T051 [P] [US5] Add backup success test case in ConfigBackupImplTest.kt
- [X] T052 [P] [US5] Add restore success test case in ConfigBackupImplTest.kt
- [X] T053 [P] [US5] Add error handling test cases (file not found, parse error) in ConfigBackupImplTest.kt

### Implementation for User Story 5

- [ ] T054 [US5] Create ConfigBackupImpl wrapping ConfigIO in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/ConfigBackupImpl.kt
- [ ] T055 [US5] Implement backup() with suspendCancellableCoroutine in ConfigBackupImpl.kt
- [ ] T056 [US5] Implement restore() with suspendCancellableCoroutine in ConfigBackupImpl.kt
- [ ] T057 [US5] Add @Binds for ConfigBackup in UseCaseModule.kt
- [ ] T058 [US5] Update BackupsViewModel to inject ConfigBackup in app/src/main/kotlin/net/ktnx/mobileledger/ui/backups/BackupsViewModel.kt
- [ ] T059 [US5] Add BackupsViewModel tests using FakeConfigBackup in app/src/test/kotlin/net/ktnx/mobileledger/ui/backups/BackupsViewModelTest.kt

**Checkpoint**: ConfigBackup が DI 経由で利用可能。BackupsViewModel のテストが可能。

---

## Phase 8: User Story 6 - データベース初期化の Coroutines 移行 (Priority: P3)

**Goal**: DatabaseInitializer インターフェースと Impl を作成。SplashActivity から suspend function で完了待ち可能に。

**Independent Test**: FakeDatabaseInitializer でプロファイル有無をシミュレートし、画面遷移を確認。

### Tests for User Story 6

- [ ] T060 [P] [US6] Create DatabaseInitializerImplTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/usecase/DatabaseInitializerImplTest.kt
- [ ] T061 [P] [US6] Add initialization success test case (hasProfiles=true) in DatabaseInitializerImplTest.kt
- [ ] T062 [P] [US6] Add initialization success test case (hasProfiles=false) in DatabaseInitializerImplTest.kt
- [ ] T063 [P] [US6] Add initialization failure test case in DatabaseInitializerImplTest.kt

### Implementation for User Story 6

- [ ] T064 [US6] Create DatabaseInitializerImpl in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/DatabaseInitializerImpl.kt
- [ ] T065 [US6] Implement initialize() with Room DB access in DatabaseInitializerImpl.kt
- [ ] T066 [US6] Add @Binds for DatabaseInitializer in UseCaseModule.kt
- [ ] T067 [US6] Create SplashViewModel with DatabaseInitializer injection in app/src/main/kotlin/net/ktnx/mobileledger/ui/splash/SplashViewModel.kt
- [ ] T068 [US6] Update SplashActivity to use SplashViewModel in app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/SplashActivity.kt
- [ ] T069 [US6] Add SplashViewModel tests using FakeDatabaseInitializer in app/src/test/kotlin/net/ktnx/mobileledger/ui/splash/SplashViewModelTest.kt

**Checkpoint**: DatabaseInitializer が DI 経由で利用可能。SplashActivity が構造化並行性で初期化待ち。

---

## Phase 9: VersionDetector 移行 (Supplementary)

**Goal**: VersionDetector インターフェースと Impl を作成。ProfileDetailScreen で使用。

### Tests for VersionDetector

- [ ] T070 [P] Create VersionDetectorImplTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/usecase/VersionDetectorImplTest.kt
- [ ] T071 [P] Add version detection success test case in VersionDetectorImplTest.kt
- [ ] T072 [P] Add version detection failure test cases in VersionDetectorImplTest.kt

### Implementation for VersionDetector

- [ ] T073 Create VersionDetectorImpl in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/VersionDetectorImpl.kt
- [ ] T074 Implement detect() with HttpURLConnection in VersionDetectorImpl.kt
- [ ] T075 Add @Binds for VersionDetector in UseCaseModule.kt
- [ ] T076 Update ProfileDetailViewModel to use VersionDetector (if applicable)

**Checkpoint**: VersionDetector が DI 経由で利用可能。

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: ドキュメント更新とカバレッジ確認

- [ ] T077 [P] Update CLAUDE.md with new usecase interfaces and patterns
- [ ] T078 [P] Run Kover coverage report and verify 50% coverage target (SC-004)
- [ ] T079 Run full test suite with `nix run .#test` and verify all tests pass
- [ ] T080 Run quickstart.md validation (実機でのビルド・インストール確認)
- [ ] T081 Code cleanup: Remove deprecated thread-based code comments
- [ ] T082 [P] Verify BackgroundTaskManager interface compatibility with new TransactionSyncer implementation (FR-016)
- [ ] T083 [P] Verify AppStateService interface compatibility with new implementations (FR-017)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-8)**: All depend on Foundational phase completion
  - US1, US2, US3 are P1 and should be completed first
  - US4, US5 are P2 and can follow
  - US6 is P3 and can be completed last
- **VersionDetector (Phase 9)**: Can run in parallel with P2/P3 stories
- **Polish (Phase 10)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - TransactionSyncer is independent
- **User Story 2 (P1)**: Depends on US1 - キャンセル制御は TransactionSyncer Impl が前提
- **User Story 3 (P1)**: Can start after Foundational - DI 設定は独立
- **User Story 4 (P2)**: Depends on US1 - エラー伝播は TransactionSyncer を使用
- **User Story 5 (P2)**: Can start after Foundational - ConfigBackup is independent
- **User Story 6 (P3)**: Can start after Foundational - DatabaseInitializer is independent

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models/Interfaces before Impl
- Impl before ViewModel integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks (T001-T007) can run in parallel
- All Foundational interface tasks (T008-T015) can run in parallel
- All test tasks marked [P] within a story can run in parallel
- US1, US3, US5, US6 can potentially run in parallel after Foundational

---

## Parallel Example: Phase 1 Setup

```bash
# Launch all domain model tasks together:
Task: "Create SyncError sealed class in domain/model/SyncError.kt"
Task: "Create SyncProgress sealed class in domain/model/SyncProgress.kt"
Task: "Create SyncResult data class in domain/model/SyncResult.kt"
Task: "Create SyncState sealed class in domain/model/SyncState.kt"
Task: "Create SendState sealed class in domain/model/SendState.kt"
Task: "Create BackupState sealed class in domain/model/BackupState.kt"
```

## Parallel Example: Phase 2 Foundational

```bash
# Launch all interface tasks together:
Task: "Create TransactionSyncer interface in domain/usecase/TransactionSyncer.kt"
Task: "Create ConfigBackup interface in domain/usecase/ConfigBackup.kt"
Task: "Create VersionDetector interface in domain/usecase/VersionDetector.kt"
Task: "Create DatabaseInitializer interface in domain/usecase/DatabaseInitializer.kt"

# Launch all Fake tasks together:
Task: "Create FakeTransactionSyncer in fake/FakeTransactionSyncer.kt"
Task: "Create FakeConfigBackup in fake/FakeConfigBackup.kt"
Task: "Create FakeVersionDetector in fake/FakeVersionDetector.kt"
Task: "Create FakeDatabaseInitializer in fake/FakeDatabaseInitializer.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 + 3)

1. Complete Phase 1: Setup (domain models)
2. Complete Phase 2: Foundational (interfaces + Fakes)
3. Complete Phase 3: User Story 1 (TransactionSyncer)
4. Complete Phase 4: User Story 2 (Cancellation)
5. Complete Phase 5: User Story 3 (Hilt DI)
6. **STOP and VALIDATE**: Test all P1 stories independently
7. Deploy/demo if ready (constitution 原則 II, VI, X 準拠達成)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → TransactionSyncer テスト可能
3. Add User Story 2 → Test independently → キャンセル制御達成
4. Add User Story 3 → Test independently → DI 完全化 (MVP!)
5. Add User Story 4 → Test independently → エラー一貫性
6. Add User Story 5 → Test independently → バックアップテスト可能
7. Add User Story 6 → Test independently → 初期化 Coroutines 化

### Success Criteria Mapping

| SC | Task Coverage |
|----|---------------|
| SC-001 | T008-T011, T021, T054, T064, T073 (5 interfaces) |
| SC-002 | T016, T025, T057, T066, T075 (Hilt bindings) |
| SC-003 | T012-T015 (5 Fakes) |
| SC-004 | T078 (50% coverage check) |
| SC-005 | T021, T054, T064, T073 (suspend functions) |
| SC-006 | T028-T033 (5秒キャンセル) |
| SC-007 | All tasks (constitution compliance) |
| FR-016 | T082 (BackgroundTaskManager compatibility) |
| FR-017 | T083 (AppStateService compatibility) |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- TransactionSender は既存パターン - 参照として使用、移行不要
