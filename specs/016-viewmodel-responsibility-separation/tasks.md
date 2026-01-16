# Tasks: ViewModel 責務分離

**Input**: Design documents from `/specs/016-viewmodel-responsibility-separation/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/viewmodel-interfaces.md

**Tests**: This feature focuses on internal refactoring. Tests are included as part of each user story to ensure proper coverage of migrated logic.

**Organization**: Tasks are grouped by user story (P1: MainViewModel分離, P2: NewTransactionViewModel分離, P3: ProfileDetailModel StateFlow移行) to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Source**: `app/src/main/kotlin/net/ktnx/mobileledger/`
- **Tests**: `app/src/test/kotlin/net/ktnx/mobileledger/`

---

## Phase 1: Setup (Preparation) ✅ COMPLETE

**Purpose**: Analyze existing code and prepare for refactoring

- [X] T001 Analyze MainViewModel dependencies and categorize by target ViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [X] T002 [P] Analyze NewTransactionViewModel dependencies and categorize by target ViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModel.kt`
- [X] T003 [P] Analyze ProfileDetailModel LiveData usage and identify StateFlow migration points in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailModel.kt`
- [X] T004 Document existing MainViewModelTest coverage to ensure test migration completeness in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`

---

## Phase 2: Foundational (Shared Infrastructure) ✅ COMPLETE

**Purpose**: Create shared UiState definitions and ensure Fake implementations are ready

**⚠️ CRITICAL**: These foundations must be complete before user story implementation

- [X] T005 [P] Create/verify PreferencesRepository exists for AccountSummaryViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/data/repository/`
- [X] T006 Create FakePreferencesRepository for testing (depends on T005) in `app/src/test/kotlin/net/ktnx/mobileledger/fake/FakePreferencesRepository.kt`
- [X] T007 [P] Verify FakeTransactionSyncer exists and is complete in `app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeTransactionSyncer.kt`
- [X] T008 Create FakeAccountRepositoryForAccountSummary if needed in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/TestFakes.kt`

**Checkpoint**: Foundation ready - user story implementation can begin

---

## Phase 3: User Story 1 - MainViewModel 責務分離 (Priority: P1) 🎯 MVP ✅ COMPLETE

**Goal**: Migrate MainViewModel (830行) logic to existing specialized ViewModels, delete/minimize MainViewModel

**Independent Test**: Each specialized ViewModel (ProfileSelectionViewModel, AccountSummaryViewModel, TransactionListViewModel, MainCoordinatorViewModel) passes unit tests independently without cross-ViewModel dependencies

### 3.1 ProfileSelectionViewModel Migration

- [X] T009 [P] [US1] Create ProfileSelectionUiState.kt with state, events, and effects in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionUiState.kt`
- [X] T010 [US1] Migrate profile selection logic from MainViewModel to ProfileSelectionViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModel.kt`
- [X] T011 [US1] Migrate profile reordering logic from MainViewModel to ProfileSelectionViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModel.kt`
- [X] T012 [US1] Add ProfileSelectionViewModel tests for profile selection in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModelTest.kt`
- [X] T013 [US1] Add ProfileSelectionViewModel tests for profile reordering in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModelTest.kt`

### 3.2 AccountSummaryViewModel Migration

- [X] T014 [P] [US1] Create/update AccountSummaryUiState.kt with state and events in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryUiState.kt`
- [X] T015 [US1] Migrate account loading logic from MainViewModel to AccountSummaryViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryViewModel.kt`
- [X] T016 [US1] Migrate zero balance filter logic from MainViewModel to AccountSummaryViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryViewModel.kt`
- [X] T017 [US1] Migrate account expand/collapse logic from MainViewModel to AccountSummaryViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryViewModel.kt`
- [X] T018 [US1] Add AccountSummaryViewModel tests for account loading in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryViewModelTest.kt`
- [X] T019 [US1] Add AccountSummaryViewModel tests for zero balance filter in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryViewModelTest.kt`

### 3.3 TransactionListViewModel Migration

- [X] T020 [P] [US1] Create/update TransactionListUiState.kt with state and events in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListUiState.kt`
- [X] T021 [US1] Migrate transaction loading logic from MainViewModel to TransactionListViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModel.kt`
- [X] T022 [US1] Migrate account filter logic from MainViewModel to TransactionListViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModel.kt`
- [X] T023 [US1] Migrate date navigation logic from MainViewModel to TransactionListViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModel.kt`
- [X] T024 [US1] Add TransactionListViewModel tests for transaction loading in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModelTest.kt`
- [X] T025 [US1] Add TransactionListViewModel tests for account filter in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModelTest.kt`
- [X] T026 [US1] Add TransactionListViewModel tests for date navigation in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModelTest.kt`

### 3.4 MainCoordinatorViewModel Migration

- [X] T027 [P] [US1] Create/update MainCoordinatorUiState.kt with state, events, and effects in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorUiState.kt`
- [X] T028 [US1] Migrate tab selection logic from MainViewModel to MainCoordinatorViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModel.kt`
- [X] T029 [US1] Migrate drawer control logic from MainViewModel to MainCoordinatorViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModel.kt`
- [X] T030 [US1] Migrate sync orchestration logic from MainViewModel to MainCoordinatorViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModel.kt`
- [X] T031 [US1] Migrate navigation effects from MainViewModel to MainCoordinatorViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModel.kt`
- [X] T032 [US1] Add MainCoordinatorViewModel tests for tab selection in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModelTest.kt`
- [X] T033 [US1] Add MainCoordinatorViewModel tests for sync orchestration in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModelTest.kt`

### 3.5 Activity Integration & MainViewModel Cleanup

- [X] T034 [US1] Update MainActivityCompose to use all four specialized ViewModels in `app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/MainActivityCompose.kt`
- [X] T035 [US1] Update MainScreen.kt to consume states from specialized ViewModels in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainScreen.kt`
- [X] T036 [US1] Remove/minimize MainViewModel after all logic migrated in `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt`
- [X] T037 [US1] Update/remove MainViewModelTest after migration in `app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt`
- [X] T038 [US1] Verify all specialized ViewModels are ≤300 lines (document exceptions)
- [X] T039 [US1] Run full test suite and verify no regressions with `nix run .#test`
- [X] T040 [US1] Run `nix run .#verify` for real device validation

### 3.6 Bug Fix: Profile URL Empty Issue

- [X] T040a [US1] Fix profile.url empty when syncing - Changed MainActivityCompose to use ProfileRepository.getAllProfiles() directly instead of recreating incomplete Profile objects from ProfileListItem
- [X] T040b [US1] Update TransactionListViewModelTest to reflect new profile change behavior (auto-loads transactions)

**Checkpoint**: User Story 1 complete - MainViewModel logic fully migrated to specialized ViewModels

---

## Phase 4: User Story 2 - NewTransactionViewModel 責務分離 (Priority: P2)

**Goal**: Split NewTransactionViewModel (961行) into TransactionFormViewModel, AccountRowsViewModel, TemplateApplicatorViewModel

**Independent Test**: Each new ViewModel passes unit tests independently without cross-ViewModel dependencies

### 4.1 TransactionFormViewModel (New)

- [X] T041 [P] [US2] Create TransactionFormUiState.kt with state, events, and effects in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormUiState.kt`
- [X] T042 [US2] Create TransactionFormViewModel with form management logic in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModel.kt`
- [X] T043 [US2] Migrate date handling from NewTransactionViewModel to TransactionFormViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModel.kt`
- [X] T044 [US2] Migrate description/comment handling from NewTransactionViewModel to TransactionFormViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModel.kt`
- [X] T045 [US2] Migrate form validation logic from NewTransactionViewModel to TransactionFormViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModel.kt`
- [X] T046 [US2] Migrate transaction sending logic from NewTransactionViewModel to TransactionFormViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModel.kt`
- [X] T047 [US2] Create TransactionFormViewModelTest with form management tests in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModelTest.kt`
- [X] T048 [US2] Add TransactionFormViewModel tests for validation in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModelTest.kt`
- [X] T049 [US2] Add TransactionFormViewModel tests for transaction sending in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModelTest.kt`

### 4.2 AccountRowsViewModel (New)

- [X] T050 [P] [US2] Create AccountRowsUiState.kt with state, events, and effects in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsUiState.kt`
- [X] T051 [US2] Create AccountRowsViewModel with row management logic in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsViewModel.kt`
- [X] T052 [US2] Migrate account row add/remove/move logic from NewTransactionViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsViewModel.kt`
- [X] T053 [US2] Migrate amount calculation/balance logic from NewTransactionViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsViewModel.kt`
- [X] T054 [US2] Migrate currency selection logic from NewTransactionViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsViewModel.kt`
- [X] T055 [US2] Migrate account name suggestions logic from NewTransactionViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsViewModel.kt`
- [X] T056 [US2] Migrate focus management logic from NewTransactionViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsViewModel.kt`
- [X] T057 [US2] Create AccountRowsViewModelTest with row management tests in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsViewModelTest.kt`
- [X] T058 [US2] Add AccountRowsViewModel tests for balance calculation in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsViewModelTest.kt`
- [X] T059 [US2] Add AccountRowsViewModel tests for currency selection in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/AccountRowsViewModelTest.kt`

### 4.3 TemplateApplicatorViewModel (New)

- [X] T060 [P] [US2] Create TemplateApplicatorUiState.kt with state, events, and effects in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TemplateApplicatorUiState.kt`
- [X] T061 [US2] Create TemplateApplicatorViewModel with template search logic in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TemplateApplicatorViewModel.kt`
- [X] T062 [US2] Migrate template matching logic from NewTransactionViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TemplateApplicatorViewModel.kt`
- [X] T063 [US2] Migrate template apply effect logic from NewTransactionViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TemplateApplicatorViewModel.kt`
- [X] T064 [US2] Create TemplateApplicatorViewModelTest with template search tests in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/TemplateApplicatorViewModelTest.kt`
- [X] T065 [US2] Add TemplateApplicatorViewModel tests for template application in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/TemplateApplicatorViewModelTest.kt`

### 4.4 Activity Integration & NewTransactionViewModel Cleanup

- [ ] T066 [US2] Update NewTransactionActivityCompose to use three specialized ViewModels in `app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/NewTransactionActivityCompose.kt`
- [ ] T067 [US2] Update NewTransactionScreen.kt to consume states from specialized ViewModels in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionScreen.kt`
- [ ] T068 [US2] Implement template apply effect handling in Activity (coordinate between ViewModels) in `app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/NewTransactionActivityCompose.kt`
- [ ] T069 [US2] Remove/minimize NewTransactionViewModel after all logic migrated in `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModel.kt`
- [ ] T070 [US2] Update/remove NewTransactionViewModelTest after migration in `app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModelTest.kt`
- [ ] T071 [US2] Verify all new ViewModels are ≤300 lines (document exceptions)
- [ ] T072 [US2] Run full test suite and verify no regressions with `nix run .#test`
- [ ] T073 [US2] Run `nix run .#verify` for real device validation of transaction registration

**Checkpoint**: User Story 2 complete - NewTransactionViewModel split into 3 specialized ViewModels

---

## Phase 5: User Story 3 - ProfileDetailModel StateFlow 移行 (Priority: P3)

**Goal**: Migrate ProfileDetailModel (574行) from LiveData to StateFlow for consistency with other ViewModels

**Independent Test**: ProfileDetailViewModel passes unit tests using MainDispatcherRule + runTest pattern (same as other ViewModels)

### 5.1 StateFlow Migration

- [ ] T074 [P] [US3] Create ProfileDetailUiState.kt with state, events, and effects in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailUiState.kt`
- [ ] T075 [US3] Create ProfileDetailViewModel replacing ProfileDetailModel with StateFlow in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt`
- [ ] T076 [US3] Migrate form fields (name, url, auth) to StateFlow pattern in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt`
- [ ] T077 [US3] Migrate validation logic to StateFlow pattern in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt`
- [ ] T078 [US3] Migrate connection test logic to StateFlow pattern in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt`
- [ ] T079 [US3] Migrate version detection logic to StateFlow pattern in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt`
- [ ] T080 [US3] Migrate save/delete logic to StateFlow pattern with effects in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt`

### 5.2 Tests

- [ ] T081 [US3] Create ProfileDetailViewModelTest with form field tests in `app/src/test/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModelTest.kt`
- [ ] T082 [US3] Add ProfileDetailViewModel tests for validation in `app/src/test/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModelTest.kt`
- [ ] T083 [US3] Add ProfileDetailViewModel tests for connection test in `app/src/test/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModelTest.kt`
- [ ] T084 [US3] Add ProfileDetailViewModel tests for save/delete in `app/src/test/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModelTest.kt`

### 5.3 UI Integration & Cleanup

- [ ] T085 [US3] Update ProfileDetailActivity to use ProfileDetailViewModel in `app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/ProfileDetailActivity.kt`
- [ ] T086 [US3] Update ProfileDetailScreen.kt to use collectAsState() in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailScreen.kt`
- [ ] T087 [US3] Remove ProfileDetailModel after migration complete in `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailModel.kt`
- [ ] T088 [US3] Verify ProfileDetailViewModel is ≤400 lines (document if exceeds due to form complexity)
- [ ] T089 [US3] Run full test suite and verify no regressions with `nix run .#test`
- [ ] T090 [US3] Run `nix run .#verify` for real device validation of profile editing

**Checkpoint**: User Story 3 complete - ProfileDetailModel migrated to StateFlow-based ProfileDetailViewModel

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and documentation

- [ ] T091 [P] Verify all ViewModels follow consistent UiState/Event/Effect pattern
- [ ] T092 [P] Run `nix run .#coverage` and verify 70%+ coverage on new ViewModels
- [ ] T093 Update CLAUDE.md with new ViewModel structure documentation
- [ ] T094 Remove any dead code or unused imports across all modified files
- [ ] T095 Run full `nix run .#verify` and complete manual testing checklist
- [ ] T096 Create summary of line counts for all ViewModels (document any exceptions to 300-line target)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - analysis only
- **Foundational (Phase 2)**: Depends on Setup - prepares shared infrastructure
- **User Story 1 (Phase 3)**: Depends on Foundational - can start after Phase 2
- **User Story 2 (Phase 4)**: Depends on Foundational - can start after Phase 2 (parallel with US1 if desired)
- **User Story 3 (Phase 5)**: Depends on Foundational - can start after Phase 2 (parallel with US1/US2 if desired)
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Independent - MainViewModel migration
- **User Story 2 (P2)**: Independent - NewTransactionViewModel separation
- **User Story 3 (P3)**: Independent - ProfileDetailModel StateFlow migration

**Note**: All user stories can be implemented in parallel after Phase 2 since they modify different files

### Within Each User Story

- UiState files before ViewModel implementation
- ViewModel implementation before tests
- Tests before Activity/UI integration
- Integration before cleanup/removal of old code
- Verification after all changes

### Parallel Opportunities

**Phase 1 (Setup)**:
- T002, T003 can run in parallel (different files to analyze)

**Phase 2 (Foundational)**:
- T005, T006, T007 can run in parallel (different files)

**Phase 3 (US1)**:
- T009, T014, T020, T027 can run in parallel (different UiState files)
- Within each ViewModel subsection, tests can run in parallel

**Phase 4 (US2)**:
- T041, T050, T060 can run in parallel (different UiState files)

**Phase 5 (US3)**:
- T074 can start immediately in this phase

---

## Parallel Example: User Story 1 UiState Files

```bash
# Launch all UiState creation tasks together:
Task: "Create ProfileSelectionUiState.kt in app/src/main/kotlin/.../ProfileSelectionUiState.kt"
Task: "Create AccountSummaryUiState.kt in app/src/main/kotlin/.../AccountSummaryUiState.kt"
Task: "Create TransactionListUiState.kt in app/src/main/kotlin/.../TransactionListUiState.kt"
Task: "Create MainCoordinatorUiState.kt in app/src/main/kotlin/.../MainCoordinatorUiState.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (analysis)
2. Complete Phase 2: Foundational (Fake implementations)
3. Complete Phase 3: User Story 1 (MainViewModel migration)
4. **STOP and VALIDATE**: Test specialized ViewModels independently, verify app works
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 (MainViewModel) → Test → Verify (MVP!)
3. Add User Story 2 (NewTransactionViewModel) → Test → Verify
4. Add User Story 3 (ProfileDetailModel) → Test → Verify
5. Polish phase → Final validation

### Sequential Recommended Order

Due to this being an internal refactoring, recommended order is P1 → P2 → P3:
1. MainViewModel migration establishes the pattern
2. NewTransactionViewModel separation follows the established pattern
3. ProfileDetailModel migration applies the pattern

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story (US1, US2, US3)
- Each user story is independently completable and testable
- Verify tests pass before proceeding to next subsection
- Commit after each logical group of tasks
- Run `nix run .#verify` at each checkpoint
- Avoid: modifying same file in parallel, breaking existing functionality
- Target: Each ViewModel ≤300 lines (400 lines max with documented reason)
