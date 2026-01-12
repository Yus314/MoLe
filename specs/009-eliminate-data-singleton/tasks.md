# Tasks: Data.kt シングルトンの廃止

**Input**: Design documents from `/specs/009-eliminate-data-singleton/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

```
app/src/main/kotlin/net/ktnx/mobileledger/
├── service/         # 新規サービス実装
├── di/              # Hilt DI モジュール
├── ui/              # UI Layer (Activity, ViewModel)
└── model/           # 既存モデル（AppStateManager 削除対象）

app/src/test/kotlin/net/ktnx/mobileledger/
└── service/         # 新規サービスのテスト
```

---

## Phase 1: Setup (Shared Infrastructure) ✅

**Purpose**: 新規サービス用のディレクトリ構造とベース DI モジュールの作成

- [X] T001 Create service directory structure at `app/src/main/kotlin/net/ktnx/mobileledger/service/`
- [X] T002 Create ServiceModule DI configuration at `app/src/main/kotlin/net/ktnx/mobileledger/di/ServiceModule.kt`
- [X] T003 [P] Create test directory structure at `app/src/test/kotlin/net/ktnx/mobileledger/service/`

---

## Phase 2: Foundational (Blocking Prerequisites) ✅

**Purpose**: 新規サービスのインターフェースとデータクラスの作成（全ユーザーストーリーが依存）

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

**contracts/ ディレクトリとの関係**:
- `specs/009-eliminate-data-singleton/contracts/` には設計時のインターフェース参照が含まれる
- 実装時は contracts/ の内容を参考に、下記パス（`app/src/main/kotlin/...`）に実装する
- contracts/ のファイルはそのままコピーせず、必要に応じて調整して実装すること

- [X] T004 [P] Create TaskState enum and TaskProgress data class at `app/src/main/kotlin/net/ktnx/mobileledger/service/TaskProgress.kt`
- [X] T005 [P] Create SyncInfo data class at `app/src/main/kotlin/net/ktnx/mobileledger/service/SyncInfo.kt`
- [X] T006 [P] Create CurrencyFormatConfig data class at `app/src/main/kotlin/net/ktnx/mobileledger/service/CurrencyFormatConfig.kt`
- [X] T007 [P] Create BackgroundTaskManager interface at `app/src/main/kotlin/net/ktnx/mobileledger/service/BackgroundTaskManager.kt`
- [X] T008 [P] Create CurrencyFormatter interface at `app/src/main/kotlin/net/ktnx/mobileledger/service/CurrencyFormatter.kt`
- [X] T009 [P] Create AppStateService interface at `app/src/main/kotlin/net/ktnx/mobileledger/service/AppStateService.kt`

**Checkpoint**: Foundation ready - all interfaces defined, implementation can begin

---

## Phase 3: User Story 1 - シングルトンなしでプロファイル選択が動作する (Priority: P1) 🎯 MVP ✅

**Goal**: プロファイル選択・切り替え機能を ProfileRepository 経由で動作させる

**Independent Test**: 複数のプロファイルを作成し、それらを切り替え、各プロファイルの勘定科目残高と取引リストが正しく更新されることを確認

### Implementation for User Story 1

- [X] T010 [US1] Update MainViewModel to use ProfileRepository.currentProfile at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [X] T011 [US1] Update MainViewModel to use ProfileRepository.getAllProfiles() at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [X] T012 [US1] Add selectProfile() method to MainViewModel at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [X] T013 [US1] Update MainScreen to collect currentProfile from ViewModel at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainScreen.kt` (via MainActivityCompose lifecycle observer)
- [X] T014 [US1] Update NavigationDrawer to use allProfiles from ViewModel at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/NavigationDrawer.kt` (via MainActivityCompose lifecycle observer)
- [X] T015 [US1] Replace Data.observeProfile() with ProfileRepository in ProfileThemedActivity at `app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/ProfileThemedActivity.kt`
- [X] T016 [US1] Update BackupsActivity to use ProfileRepository at `app/src/main/kotlin/net/ktnx/mobileledger/BackupsActivity.kt` (merged with T016a)
- [X] T016a [US1] Replace Data.getProfile()/Data.observeProfile() in BackupsActivity at `app/src/main/kotlin/net/ktnx/mobileledger/BackupsActivity.kt`
- [ ] T017 [US1] Remove AppStateManager.setCurrentProfile() sync in ProfileRepositoryImpl at `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepositoryImpl.kt` (implements FR-007) **BLOCKED**: Many places still use Data.getProfile() (LedgerTransaction, RetrieveTransactionsTask, etc.). Must migrate all usages first.
- [X] T017a [US1] Replace Data.observeProfile() in NewTransactionActivityCompose at `app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/NewTransactionActivityCompose.kt` (discovered during implementation)
- [X] T017b [US1] Fix Currency.Position enum case (BEFORE/AFTER/NONE) in multiple files (discovered during build)
- [X] T018 [US1] Run verification: `nix run .#verify` and test profile switching manually

**Checkpoint**: Profile selection works entirely through ProfileRepository without Data.kt

---

## Phase 4: User Story 2 - シングルトンなしでデータ同期が動作する (Priority: P1)

**Goal**: データ同期機能を BackgroundTaskManager 経由で動作させる

**Independent Test**: 更新をトリガーし、進捗インジケーターを観察し、サーバーからの新しいデータが正しく表示されることを確認

### Implementation for User Story 2

- [X] T019 [US2] Implement BackgroundTaskManagerImpl at `app/src/main/kotlin/net/ktnx/mobileledger/service/BackgroundTaskManagerImpl.kt`
- [X] T020 [US2] Add BackgroundTaskManager binding to ServiceModule at `app/src/main/kotlin/net/ktnx/mobileledger/di/ServiceModule.kt`
- [ ] T021 [US2] Create BackgroundTaskManagerTest at `app/src/test/kotlin/net/ktnx/mobileledger/service/BackgroundTaskManagerTest.kt`
- [ ] T022 [US2] Update SendTransactionTask to use BackgroundTaskManager at `app/src/main/kotlin/net/ktnx/mobileledger/async/SendTransactionTask.kt`
- [X] T023 [US2] Implement AppStateServiceImpl at `app/src/main/kotlin/net/ktnx/mobileledger/service/AppStateServiceImpl.kt`
- [X] T024 [US2] Add AppStateService binding to ServiceModule at `app/src/main/kotlin/net/ktnx/mobileledger/di/ServiceModule.kt`
- [X] T025 [US2] Update MainViewModel to inject BackgroundTaskManager at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [ ] T026 [US2] Update MainViewModel to expose isTaskRunning and taskProgress at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [X] T027 [US2] Update MainViewModel to inject AppStateService (provides lastSyncInfo and drawerOpen) at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [ ] T028 [US2] Update MainScreen to display task progress from ViewModel at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainScreen.kt`
- [ ] T029 [US2] Update MainScreen to display lastSyncInfo from ViewModel at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainScreen.kt`
- [ ] T029a [US2] Add unit test for sync-during-profile-switch edge case (spec Edge Case 3) at `app/src/test/kotlin/net/ktnx/mobileledger/service/BackgroundTaskManagerTest.kt`
- [ ] T030 [US2] Run verification: `nix run .#verify` and test data sync manually (include profile switch during sync)

**Checkpoint**: Data sync works entirely through BackgroundTaskManager and AppStateService

---

## Phase 5: User Story 3 - 通貨フォーマットが一貫して維持される (Priority: P2)

**Goal**: 通貨フォーマット機能を CurrencyFormatter 経由で動作させる

**Independent Test**: デバイスのロケールを変更し、通貨記号と小数点区切り文字がすべての画面で正しく更新されることを確認

### Implementation for User Story 3

- [X] T031 [US3] Implement CurrencyFormatterImpl at `app/src/main/kotlin/net/ktnx/mobileledger/service/CurrencyFormatterImpl.kt`
- [X] T032 [US3] Add CurrencyFormatter binding to ServiceModule at `app/src/main/kotlin/net/ktnx/mobileledger/di/ServiceModule.kt`
- [ ] T033 [US3] Create CurrencyFormatterTest at `app/src/test/kotlin/net/ktnx/mobileledger/service/CurrencyFormatterTest.kt`
- [ ] T034 [US3] Update App.kt to inject and initialize CurrencyFormatter at `app/src/main/kotlin/net/ktnx/mobileledger/App.kt`
- [ ] T035 [US3] Update App.kt onConfigurationChanged to refresh CurrencyFormatter at `app/src/main/kotlin/net/ktnx/mobileledger/App.kt`
- [ ] T036 [US3] Run verification: `nix run .#verify` and test locale change manually

**Checkpoint**: Currency formatting works entirely through CurrencyFormatter

---

## Phase 6: User Story 4 - テーマ変更が正しく適用される (Priority: P2)

**Goal**: テーマカラー変更がプロファイル切り替え時に即座に反映される

**Independent Test**: 異なるテーマカラーを持つプロファイルを作成し、それらを切り替えることでテスト可能

### Implementation for User Story 4

- [ ] T037 [US4] Verify ProfileThemedActivity uses ProfileRepository.currentProfile (from T015) at `app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/ProfileThemedActivity.kt`
- [ ] T038 [US4] Update ProfileDetailViewModel to use ProfileRepository at `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt`
- [ ] T039 [US4] Update ProfileDetailScreen to trigger theme update on save at `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailScreen.kt`
- [ ] T040 [US4] Run verification: `nix run .#verify` and test theme changes manually

**Checkpoint**: Theme changes are applied correctly when switching profiles

---

## Phase 7: User Story 5 - ナビゲーションドロワーの状態が維持される (Priority: P3)

**Goal**: ドロワー状態管理を AppStateService 経由で動作させる

**Independent Test**: ドロワーを開閉し、正しく応答することを確認

### Implementation for User Story 5

- [ ] T041 [US5] Expose drawerOpen StateFlow from MainViewModel using AppStateService (injected in T027) at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [ ] T042 [US5] Add openDrawer(), closeDrawer(), toggleDrawer() methods to MainViewModel at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [ ] T043 [US5] Update MainScreen to use drawerOpen from ViewModel at `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainScreen.kt`
- [ ] T044 [US5] Run verification: `nix run .#verify` and test drawer functionality manually

**Checkpoint**: Drawer state management works entirely through AppStateService

---

## Phase 8: User Story 6 - すべての ViewModel が独立してテスト可能になる (Priority: P3)

**Goal**: 全 ViewModel がモックリポジトリ注入によるユニットテストが可能

**Independent Test**: モックリポジトリを使用して ViewModel のユニットテストを書き、グローバル状態のセットアップなしでパスすることを確認

### Implementation for User Story 6

- [ ] T045 [US6] Update MainViewModelTest to use mock repositories without Data singleton at `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [ ] T046 [US6] Create AppStateServiceTest at `app/src/test/kotlin/net/ktnx/mobileledger/service/AppStateServiceTest.kt`
- [ ] T047 [US6] Run all unit tests: `nix run .#test`

**Checkpoint**: All ViewModel tests pass without global state setup

---

## Phase 9: Polish & Cross-Cutting Concerns (Cleanup)

**Purpose**: AppStateManager の完全削除と最終検証

**Grep 対象スコープ**:
- **対象**: `app/src/main/kotlin/` （本番コードのみ）
- **除外**: `app/src/test/kotlin/` （テストコードは移行検証のため参照可）
- **コメント内参照**: 対象（コメントも削除またはドキュメント更新）

- [ ] T048 Remove all remaining Data.* references and verify SC-001: `grep -r "Data\." app/src/main/kotlin/` must return 0 results after cleanup
- [ ] T049 Remove all remaining AppStateManager references and verify SC-001: `grep -r "AppStateManager" app/src/main/kotlin/` must return 0 results after cleanup
- [ ] T050 Delete AppStateManager.kt at `app/src/main/kotlin/net/ktnx/mobileledger/model/AppStateManager.kt`
- [ ] T051 Delete AppStateModule.kt at `app/src/main/kotlin/net/ktnx/mobileledger/di/AppStateModule.kt`
- [ ] T052 Run final verification: `nix run .#test` - all tests must pass
- [ ] T053 Run final build and install: `nix run .#verify`
- [ ] T054 Manual full feature test: profile switching, data sync, theme changes, transaction creation

**Note**: SC-001 verification is included in T048/T049 (grep must return 0 results after cleanup)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-8)**: All depend on Foundational phase completion
  - US1 and US2 are both P1 priority and can proceed in parallel
  - US3 and US4 are both P2 priority and depend on US1/US2 completion for full context
  - US5 and US6 are P3 priority
- **Polish (Phase 9)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - Uses existing ProfileRepository
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - Creates BackgroundTaskManager
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - Creates CurrencyFormatter
- **User Story 4 (P2)**: Depends on US1 (ProfileThemedActivity updates)
- **User Story 5 (P3)**: Can start after US2 (AppStateService already created)
- **User Story 6 (P3)**: Depends on US1, US2, US3 (all services must exist for testing)

### Within Each User Story

- Services/Implementations before their usage in ViewModels
- ViewModel updates before UI updates
- Unit tests after implementation
- Manual verification at checkpoint

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (T004-T009)
- US1 and US2 can start in parallel after Foundational completion
- US3 can start in parallel with US4 (both P2)
- T004, T005, T006 (data classes) can all run in parallel
- T007, T008, T009 (interfaces) can all run in parallel

---

## Parallel Example: Foundational Phase

```bash
# Launch all data class creation in parallel:
Task: "Create TaskState enum and TaskProgress data class in app/.../service/TaskProgress.kt"
Task: "Create SyncInfo data class in app/.../service/SyncInfo.kt"
Task: "Create CurrencyFormatConfig data class in app/.../service/CurrencyFormatConfig.kt"

# Launch all interface creation in parallel:
Task: "Create BackgroundTaskManager interface in app/.../service/BackgroundTaskManager.kt"
Task: "Create CurrencyFormatter interface in app/.../service/CurrencyFormatter.kt"
Task: "Create AppStateService interface in app/.../service/AppStateService.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Profile Selection)
4. Complete Phase 4: User Story 2 (Data Sync)
5. **STOP and VALIDATE**: Test both stories independently
6. At this point, core functionality works without Data.kt

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Checkpoint (Profile works)
3. Add User Story 2 → Test independently → Checkpoint (Sync works)
4. Add User Story 3 → Test independently → Checkpoint (Currency works)
5. Add User Story 4 → Test independently → Checkpoint (Theme works)
6. Add User Story 5 → Test independently → Checkpoint (Drawer works)
7. Add User Story 6 → Test independently → Checkpoint (Tests pass)
8. Complete Polish phase → Data.kt completely removed

### Success Criteria Verification

At end of each phase, verify:

- **SC-002**: `nix run .#test` - All existing unit tests pass
- **SC-003**: Manual test - All user-facing features work identically

At end of Phase 9, additionally verify:

- **SC-001**: `grep -r "Data\." app/src/main/kotlin/` returns 0 results
- **SC-001**: `grep -r "AppStateManager\." app/src/main/kotlin/` returns 0 results
- **SC-004**: New ViewModels testable with mock dependencies only
- **SC-005**: App builds successfully with Data.kt deleted
- **SC-006**: App startup time within 10% of pre-refactor performance

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
