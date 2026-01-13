# Tasks: MainViewModel Refactoring for Improved Maintainability and Testability

**Input**: Design documents from `/home/user/MoLe/specs/010-refactor-mainviewmodel/`
**Prerequisites**: plan.md ✅, spec.md ✅

**Tests**: This project follows TDD (Constitution principle II). Tests are written FIRST and must FAIL before implementation.

**Organization**: Tasks are grouped by implementation phase. This is a refactoring project where all phases contribute to achieving the outcome-based user stories defined in spec.md (US1-US5). Each phase must pass all existing tests before proceeding to the next.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story outcome this contributes to (US1-US5 from spec.md)
- Include exact file paths in descriptions

## Path Conventions

- **Android app structure**: `app/src/main/kotlin/net/ktnx/mobileledger/`
- **Tests**: `app/src/test/kotlin/net/ktnx/mobileledger/`
- **Spec docs**: `specs/010-refactor-mainviewmodel/`

---

## Phase 1: Setup & Prerequisites

**Purpose**: Verify environment and document current state

- [X] T001 Run `nix run .#test` to establish baseline (all tests must pass)
- [X] T002 Run `nix run .#build` to verify build succeeds
- [X] T003 Document current MainViewModel line count in research.md (target: 800 lines)
- [X] T004 Commit current working state before refactoring begins

**Checkpoint**: Baseline established - all tests passing, build succeeds

---

## Phase 2: Research & Design Artifacts (Phase 0-1 from plan.md)

**Purpose**: Create design artifacts that guide implementation (from plan.md Phase 0-1)

**⚠️ CRITICAL**: These artifacts MUST be complete before ANY code implementation begins

### Research Tasks (Phase 0)

- [X] T005 [P] Analyze MainViewModel.kt lines 1-800 and create responsibility mapping table in specs/010-refactor-mainviewmodel/research.md (Decision 1: Component Boundaries)
- [X] T006 [P] Research SharedPreferences vs DataStore and document decision in specs/010-refactor-mainviewmodel/research.md (Decision 2: PreferencesRepository Implementation)
- [X] T007 [P] Research multi-ViewModel Activity patterns and document recommendation in specs/010-refactor-mainviewmodel/research.md (Decision 3: Multi-ViewModel Pattern)
- [X] T008 [P] Design testing strategy for shared Repository state and document in specs/010-refactor-mainviewmodel/research.md (Decision 4: Shared State Testing)
- [X] T009 [P] Create test migration mapping table in specs/010-refactor-mainviewmodel/research.md (Decision 5: Test Migration Strategy)

### Design & Contracts Tasks (Phase 1)

- [X] T010 Create data-model.md with UiState structures for all 4 ViewModels in specs/010-refactor-mainviewmodel/data-model.md
- [X] T011 [P] Create ProfileSelectionViewModel contract in specs/010-refactor-mainviewmodel/contracts/ProfileSelectionViewModel.contract.md
- [X] T012 [P] Create AccountSummaryViewModel contract in specs/010-refactor-mainviewmodel/contracts/AccountSummaryViewModel.contract.md
- [X] T013 [P] Create TransactionListViewModel contract in specs/010-refactor-mainviewmodel/contracts/TransactionListViewModel.contract.md
- [X] T014 [P] Create MainCoordinatorViewModel contract in specs/010-refactor-mainviewmodel/contracts/MainCoordinatorViewModel.contract.md
- [X] T015 [P] Create PreferencesRepository contract in specs/010-refactor-mainviewmodel/contracts/PreferencesRepository.contract.md
- [X] T016 Create quickstart.md developer guide in specs/010-refactor-mainviewmodel/quickstart.md
- [X] T017 Run `.specify/scripts/bash/update-agent-context.sh claude` to update agent context (skipped - script expects different directory structure)

**GATE 2 (Post-Phase 1)**: Review design artifacts against Constitution ✅

**Checkpoint**: Design complete - research.md, data-model.md, contracts/, quickstart.md ready

---

## Phase 3: PreferencesRepository (Foundation) 🎯

**Goal**: Create PreferencesRepository to replace App static methods for preferences

**Independent Test**: Unit tests verify preference storage/retrieval works correctly

**Contributes to**: [US5] All existing features continue to work

### Tests First (TDD)

- [X] T018 [US5] Create PreferencesRepositoryTest in app/src/test/kotlin/net/ktnx/mobileledger/data/repository/PreferencesRepositoryTest.kt (write tests, ensure they FAIL)

### Implementation

- [X] T019 [US5] Create PreferencesRepository interface in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/PreferencesRepository.kt
- [X] T020 [US5] Implement PreferencesRepositoryImpl using SharedPreferences in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/PreferencesRepositoryImpl.kt
- [X] T021 [US5] Add PreferencesRepository binding to RepositoryModule in app/src/main/kotlin/net/ktnx/mobileledger/di/RepositoryModule.kt
- [X] T022 [US5] Run `nix run .#test` - PreferencesRepositoryTest must now PASS
- [X] T023 [US5] Run `nix run .#build` to verify build succeeds
- [X] T024 [US5] Commit: "feat: Add PreferencesRepository for preference management"

**Checkpoint**: PreferencesRepository complete and tested

---

## Phase 4: ProfileSelectionViewModel Extraction 🎯 MVP

**Goal**: Extract profile selection logic from MainViewModel into ProfileSelectionViewModel

**Independent Test**: Profile selection works, all profile-related tests pass independently

**Contributes to**: [US1] Developer adds features to isolated component, [US2] Debug tests quickly, [US5] All features work

### Tests First (TDD)

- [ ] T025 [US1,US2,US5] Create ProfileSelectionViewModelTest in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModelTest.kt (write tests covering: profile selection, profile reordering, observing currentProfile, write tests to FAIL initially)

### Implementation

- [ ] T026 [US1,US2,US5] Create ProfileSelectionUiState data class in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionUiState.kt
- [ ] T027 [US1,US2,US5] Create ProfileSelectionEvent sealed class in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionEvent.kt
- [ ] T028 [US1,US2,US5] Create ProfileSelectionViewModel in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModel.kt (~150 lines, inject ProfileRepository via Hilt)
- [ ] T029 [US1,US2,US5] Extract profile selection logic from MainViewModel.kt into ProfileSelectionViewModel (copy relevant methods, preserve behavior exactly)
- [ ] T030 [US1,US2,US5] Update MainViewModel to delegate profile operations to ProfileSelectionViewModel (temporary bridge pattern)
- [ ] T031 [US1,US2,US5] Run `nix run .#test` - ProfileSelectionViewModelTest must PASS, all existing tests must PASS
- [ ] T032 [US1,US2,US5] Verify ProfileSelectionViewModel is under 300 lines (`wc -l`)
- [ ] T033 [US1,US2,US5] Run `nix run .#build` to verify build succeeds
- [ ] T034 [US1,US2,US5] Commit: "feat: Extract ProfileSelectionViewModel from MainViewModel"

**Checkpoint**: ProfileSelectionViewModel complete - first component successfully extracted, pattern validated

---

## Phase 5: AccountSummaryViewModel Extraction

**Goal**: Extract account summary logic from MainViewModel into AccountSummaryViewModel

**Independent Test**: Account list display, filtering, expansion works independently

**Contributes to**: [US1] Developer adds features to isolated component, [US2] Debug tests quickly, [US5] All features work

### Tests First (TDD)

- [ ] T035 [US1,US2,US5] Create AccountSummaryViewModelTest in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryViewModelTest.kt (write tests covering: account list loading, zero-balance filter toggle, account expansion, amounts expansion, observing profile changes, write tests to FAIL initially)

### Implementation

- [ ] T036 [US1,US2,US5] Create AccountSummaryUiState data class (if not exists, else update) in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryUiState.kt
- [ ] T037 [US1,US2,US5] Create AccountSummaryEvent sealed class in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryEvent.kt
- [ ] T038 [US1,US2,US5] Create AccountSummaryViewModel in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryViewModel.kt (~250 lines, inject ProfileRepository, AccountRepository, PreferencesRepository via Hilt)
- [ ] T039 [US1,US2,US5] Extract account summary logic from MainViewModel.kt into AccountSummaryViewModel (account list, filtering, expansion state)
- [ ] T040 [US1,US2,US5] Update MainViewModel to delegate account operations to AccountSummaryViewModel (temporary bridge)
- [ ] T041 [US1,US2,US5] Run `nix run .#test` - AccountSummaryViewModelTest must PASS, all existing tests must PASS
- [ ] T042 [US1,US2,US5] Verify AccountSummaryViewModel is under 300 lines (`wc -l`)
- [ ] T043 [US1,US2,US5] Run `nix run .#build` to verify build succeeds
- [ ] T044 [US1,US2,US5] Commit: "feat: Extract AccountSummaryViewModel from MainViewModel"

**Checkpoint**: AccountSummaryViewModel complete - second component successfully extracted, pattern scales

---

## Phase 6: TransactionListViewModel Extraction

**Goal**: Extract transaction list logic from MainViewModel into TransactionListViewModel

**Independent Test**: Transaction list display, filtering, date navigation works independently

**Contributes to**: [US1] Developer adds features to isolated component, [US2] Debug tests quickly, [US5] All features work

### Tests First (TDD)

- [ ] T045 [US1,US2,US5] Create TransactionListViewModelTest in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModelTest.kt (write tests covering: transaction list loading, account filter, suggestions, clear filter, go to date, observing profile changes, write tests to FAIL initially)

### Implementation

- [ ] T046 [US1,US2,US5] Create TransactionListUiState data class (if not exists, else update) in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListUiState.kt
- [ ] T047 [US1,US2,US5] Create TransactionListEvent sealed class in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListEvent.kt
- [ ] T048 [US1,US2,US5] Create TransactionListViewModel in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModel.kt (~250 lines, inject ProfileRepository, TransactionRepository, AccountRepository via Hilt)
- [ ] T049 [US1,US2,US5] Extract transaction list logic from MainViewModel.kt into TransactionListViewModel (transaction list, filtering, date range, search)
- [ ] T050 [US1,US2,US5] Update MainViewModel to delegate transaction operations to TransactionListViewModel (temporary bridge)
- [ ] T051 [US1,US2,US5] Run `nix run .#test` - TransactionListViewModelTest must PASS, all existing tests must PASS
- [ ] T052 [US1,US2,US5] Verify TransactionListViewModel is under 300 lines (`wc -l`)
- [ ] T053 [US1,US2,US5] Run `nix run .#build` to verify build succeeds
- [ ] T054 [US1,US2,US5] Commit: "feat: Extract TransactionListViewModel from MainViewModel"

**Checkpoint**: TransactionListViewModel complete - third component successfully extracted, domain-specific components complete

---

## Phase 7: MainCoordinatorViewModel Conversion

**Goal**: Convert remaining MainViewModel to MainCoordinatorViewModel (UI orchestration only)

**Independent Test**: Tab selection, drawer state, navigation, refresh work correctly

**Contributes to**: [US1] Developer adds features to isolated component, [US2] Debug tests quickly, [US5] All features work

### Tests First (TDD)

- [ ] T055 [US1,US2,US5] Create MainCoordinatorViewModelTest in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModelTest.kt (write tests covering: tab selection, drawer open/close, refresh, navigation effects, write tests to FAIL initially)

### Implementation

- [ ] T056 [US1,US2,US5] Update MainUiState data class (if not exists, else update) in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainUiState.kt (keep only coordinator state: selectedTab, drawerOpen, isRefreshing, currentProfileId, currentProfileName, currentProfileCanPost)
- [ ] T057 [US1,US2,US5] Create MainEvent sealed class (if not exists, else update) in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainEvent.kt
- [ ] T058 [US1,US2,US5] Rename MainViewModel to MainCoordinatorViewModel in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModel.kt
- [ ] T059 [US1,US2,US5] Remove all domain logic from MainCoordinatorViewModel, keep only: tab selection, drawer state, refresh orchestration, navigation effects (~250 lines)
- [ ] T060 [US1,US2,US5] MainCoordinatorViewModel inject ProfileRepository, BackgroundTaskManager, AppStateService via Hilt
- [ ] T061 [US1,US2,US5] Run `nix run .#test` - MainCoordinatorViewModelTest must PASS, all existing tests must PASS
- [ ] T062 [US1,US2,US5] Verify MainCoordinatorViewModel is under 300 lines (`wc -l`)
- [ ] T063 [US1,US2,US5] Run `nix run .#build` to verify build succeeds
- [ ] T064 [US1,US2,US5] Commit: "feat: Convert MainViewModel to MainCoordinatorViewModel (UI orchestration only)"

**Checkpoint**: MainCoordinatorViewModel complete - all 4 components successfully created, refactoring structurally complete

---

## Phase 8: MainActivityCompose & MainScreen Integration

**Goal**: Update MainActivityCompose and MainScreen to use all 4 ViewModels

**Independent Test**: All UI interactions work correctly with new architecture

**Contributes to**: [US1] Developer adds features to isolated component, [US2] Debug tests quickly, [US5] All features work

- [ ] T065 [US1,US2,US5] Update MainActivityCompose to inject all 4 ViewModels (ProfileSelectionViewModel, AccountSummaryViewModel, TransactionListViewModel, MainCoordinatorViewModel) in app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/MainActivityCompose.kt
- [ ] T066 [US1,US2,US5] Update MainScreen to receive all 4 ViewModels or their UiStates/events in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainScreen.kt
- [ ] T067 [US1,US2,US5] Update NavigationDrawerContent to use ProfileSelectionViewModel state/events in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/NavigationDrawer.kt
- [ ] T068 [US1,US2,US5] Update AccountSummaryTab to use AccountSummaryViewModel state/events in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryTab.kt
- [ ] T069 [US1,US2,US5] Update TransactionListTab to use TransactionListViewModel state/events in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListTab.kt
- [ ] T070 [US1,US2,US5] Update MainScreen top-level to use MainCoordinatorViewModel for tab/drawer/refresh
- [ ] T071 [US1,US2,US5] Run `nix run .#test` - all existing tests must PASS
- [ ] T072 [US1,US2,US5] Run `nix run .#build` to verify build succeeds
- [ ] T073 [US1,US2,US5] Run `nix run .#verify` - build, test, install on device
- [ ] T074 [US1,US2,US5] Manual device testing: profile selection, account list, transaction list, tab switching, drawer, refresh (all must work)
- [ ] T075 [US1,US2,US5] Commit: "feat: Integrate all 4 ViewModels into MainActivityCompose and MainScreen"
- [ ] T075a [US5] Verify FR-010: Confirm BackgroundTaskManager still uses Thread-based implementation (no coroutine migration) - Check `BackgroundTaskManager.kt` uses `Thread` class, verify `RetrieveTransactionsTask` extends `Thread`, confirm no coroutine-related imports (viewModelScope, launch, async) in background task code

**Checkpoint**: Integration complete - all UI using new architecture, all tests passing, device testing successful, Thread-based implementation preserved

---

## Phase 9: Test Migration & Cleanup

**Goal**: Migrate MainViewModelTest cases to appropriate component tests, delete old files

**Independent Test**: All migrated tests pass, old code removed

**Contributes to**: [US2] Debug tests quickly, [US3] Understand codebase, [US4] Code review streamlined, [US5] All features work

- [ ] T076 [US2,US3,US4,US5] Migrate profile-related tests from MainViewModelTest to ProfileSelectionViewModelTest (if not already covered) in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModelTest.kt
- [ ] T077 [US2,US3,US4,US5] Migrate account-related tests from MainViewModelTest to AccountSummaryViewModelTest (if not already covered) in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryViewModelTest.kt
- [ ] T078 [US2,US3,US4,US5] Migrate transaction-related tests from MainViewModelTest to TransactionListViewModelTest (if not already covered) in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModelTest.kt
- [ ] T079 [US2,US3,US4,US5] Migrate coordinator-related tests from MainViewModelTest to MainCoordinatorViewModelTest (if not already covered) in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModelTest.kt
- [ ] T080 [US2,US3,US4,US5] Delete MainViewModelTest.kt in app/src/test/kotlin/net/ktnx/mobileledger/ui/main/MainViewModelTest.kt
- [ ] T081 [US2,US3,US4,US5] Run `nix run .#test` - all tests must PASS (100% test coverage migrated)
- [ ] T082 [US2,US3,US4,US5] Run `nix run .#build` to verify build succeeds
- [ ] T083 [US2,US3,US4,US5] Commit: "test: Migrate MainViewModelTest to component-specific tests"

**Checkpoint**: Test migration complete - all tests in appropriate component files

---

## Phase 10: Documentation & Validation

**Goal**: Update documentation, validate success criteria, perform final verification

**Independent Test**: All documentation accurate, all success criteria met

**Contributes to**: [US3] Understand codebase, [US4] Code review streamlined

- [ ] T084 [P] [US3,US4] Update CLAUDE.md with new ViewModel structure (add section on split ViewModels pattern)
- [ ] T084a [P] [US3,US4] Run `pre-commit run --all-files` to verify ktlint/detekt compliance on all modified files (Constitution principle IX: Static Analysis & Linting)
- [ ] T085 [P] [US3,US4] Update architecture documentation (if exists) with new component boundaries
- [ ] T086 [P] [US3,US4] Validate SC-001: Developers can locate feature-specific code in under 30 seconds - (a) File size: Each component under 300 lines (`wc -l app/src/main/kotlin/net/ktnx/mobileledger/ui/main/{ProfileSelection,AccountSummary,TransactionList,MainCoordinator}ViewModel.kt`), (b) Test execution time: Component tests under 1 second (measure with `time ./gradlew test --tests <TestClass>` for each ViewModel), (c) Code coverage: Each component above 80% (`./gradlew testDebugUnitTestCoverage` and check report at app/build/reports/coverage/test/debug/index.html)
- [ ] T087 [P] [US3,US4] Validate SC-002: Individual component test suites execute in under 1 second - Run each test class individually and measure execution time: `time ./gradlew test --tests ProfileSelectionViewModelTest` (< 1000ms), `time ./gradlew test --tests AccountSummaryViewModelTest` (< 1000ms), `time ./gradlew test --tests TransactionListViewModelTest` (< 1000ms), `time ./gradlew test --tests MainCoordinatorViewModelTest` (< 1000ms)
- [ ] T088 [P] [US3,US4] Validate SC-004: 100% functional parity (all existing tests pass, manual device testing checklist complete)
- [ ] T089 [US3,US4] Run `nix run .#verify` - final full verification (test + build + install)
- [ ] T090 [US3,US4] Manual device testing checklist: profile selection, account display, transaction list, tab switching, drawer, pull-to-refresh, navigation (all must work identically to pre-refactoring)
- [ ] T091 [US3,US4] Document any deviations or discovered issues in specs/010-refactor-mainviewmodel/completion-notes.md
- [ ] T092 [US3,US4] Commit: "docs: Update documentation for MainViewModel refactoring"

**Checkpoint**: Documentation complete, all success criteria validated

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Research & Design (Phase 2)**: Depends on Setup completion - BLOCKS all implementation
- **PreferencesRepository (Phase 3)**: Depends on Phase 2 completion
- **ProfileSelectionViewModel (Phase 4)**: Depends on Phase 3 completion - MVP checkpoint
- **AccountSummaryViewModel (Phase 5)**: Depends on Phase 3 completion - can run parallel with Phase 4 after MVP
- **TransactionListViewModel (Phase 6)**: Depends on Phase 3 completion - can run parallel with Phase 4-5 after MVP
- **MainCoordinatorViewModel (Phase 7)**: Depends on Phase 4-6 completion (all domain components extracted)
- **Integration (Phase 8)**: Depends on Phase 4-7 completion (all ViewModels created)
- **Test Migration (Phase 9)**: Depends on Phase 8 completion (integration working)
- **Documentation (Phase 10)**: Depends on Phase 9 completion (all code finalized)

### Critical Path

Setup → Research & Design → PreferencesRepository → ProfileSelectionViewModel → [AccountSummary + TransactionList in parallel possible] → MainCoordinatorViewModel → Integration → Test Migration → Documentation

### TDD Pattern (Repeats in Each Component Phase)

1. Write tests FIRST (tests must FAIL initially)
2. Implement component
3. Run tests (tests must PASS)
4. Run full test suite (all tests must PASS)
5. Verify build succeeds
6. Commit

### Rollback Policy (FR-008, Clarification Q3)

If ANY test fails at ANY checkpoint:
1. Immediately run `git reset --hard HEAD` or `git revert <commit>`
2. Analyze failure root cause
3. Retry with alternative approach
4. Do NOT proceed until all tests pass

### Parallel Opportunities

- **Phase 2 (Research)**: Tasks T005-T009 can run in parallel (5 research tasks)
- **Phase 2 (Contracts)**: Tasks T011-T015 can run in parallel (5 contract documents)
- **After Phase 4 MVP**: Phases 5-6 can run in parallel if team capacity allows (AccountSummary and TransactionList extraction)
- **Phase 10 (Documentation)**: Tasks T084-T088 can run in parallel (5 documentation tasks)

---

## Parallel Example: Research Phase

```bash
# Launch all research tasks together:
Task T005: "Analyze MainViewModel.kt and create responsibility mapping"
Task T006: "Research SharedPreferences vs DataStore"
Task T007: "Research multi-ViewModel Activity patterns"
Task T008: "Design shared Repository state testing"
Task T009: "Create test migration mapping table"
```

## Parallel Example: After MVP (Phase 4 Complete)

```bash
# Two developers can work in parallel:
Developer A: Phase 5 (AccountSummaryViewModel extraction)
Developer B: Phase 6 (TransactionListViewModel extraction)

# Both use PreferencesRepository (Phase 3) as shared foundation
# Both observe ProfileRepository for profile changes
# No direct dependencies between AccountSummary and TransactionList
```

---

## Implementation Strategy

### MVP First (Phase 1-4)

1. Complete Phase 1: Setup & Prerequisites
2. Complete Phase 2: Research & Design (CRITICAL - creates design artifacts)
3. Complete Phase 3: PreferencesRepository (Foundation)
4. Complete Phase 4: ProfileSelectionViewModel (First component extraction)
5. **STOP and VALIDATE**:
   - ProfileSelectionViewModel tests pass
   - All existing tests still pass
   - Build succeeds
   - Pattern validated: TDD works, component extraction approach is sound
6. Decision point: Continue to Phase 5 or refine approach

### Incremental Delivery

1. MVP (Phases 1-4) → Foundation + First Component
2. Add AccountSummaryViewModel (Phase 5) → Second component
3. Add TransactionListViewModel (Phase 6) → Third component
4. Convert MainCoordinatorViewModel (Phase 7) → Fourth component
5. Integration (Phase 8) → Wire all components together
6. Test Migration (Phase 9) → Clean up test structure
7. Documentation (Phase 10) → Finalize documentation

Each phase maintains 100% functional parity (FR-001, US5).

### Success Criteria Validation (from spec.md)

After completion, verify:

- **SC-001**: Each component under 300 lines, component tests under 1 second, 80% coverage (validated in T086-T087)
- **SC-002**: Component tests execute in under 1 second (validated in T087)
- **SC-004**: 100% functional parity - all existing tests pass, manual testing complete (validated in T088, T090)
- **SC-006**: Test failures clearly identify which component broke (achieved through separate test files)
- **SC-008**: Zero regression bugs (validated through continuous test execution at every checkpoint)

---

## Notes

- **[P] tasks**: Different files, no dependencies, can run in parallel
- **[US1,US2,US5] labels**: Map tasks to user story outcomes from spec.md
- **TDD enforced**: Tests written FIRST, must FAIL, then implementation, then tests PASS
- **Rollback policy**: Any test failure triggers immediate rollback to previous commit
- **Incremental verification**: Run `nix run .#test` and `nix run .#build` after EVERY phase
- **Device testing**: Manual testing required at integration phase (T074) and final validation (T090)
- **Constitution compliance**: All tasks follow Constitution principles (TDD, Hilt DI, incremental, Kotlin standards)
- **Static analysis**: ktlint/detekt automatically run via pre-commit hooks on every commit; explicit verification in Phase 10 (T084a)
- **Post-deployment observation metrics**: SC-003 (code review time reduction), SC-005 (80% single-component changes), and SC-007 (developer onboarding time) are observation metrics measured after deployment through:
  - SC-003: Track PR review duration for main screen ViewModel changes (compare to pre-refactoring baseline)
  - SC-005: Count PRs modifying only one ViewModel vs multiple (target: 80% single-component over 3-month observation period)
  - SC-007: Conduct developer onboarding exercises with unfamiliar developers (measure time to locate specific functionality)

**Total Tasks**: 94 tasks
- Phase 1: 4 tasks (Setup)
- Phase 2: 13 tasks (Research & Design)
- Phase 3: 7 tasks (PreferencesRepository)
- Phase 4: 10 tasks (ProfileSelectionViewModel - MVP)
- Phase 5: 10 tasks (AccountSummaryViewModel)
- Phase 6: 10 tasks (TransactionListViewModel)
- Phase 7: 10 tasks (MainCoordinatorViewModel)
- Phase 8: 12 tasks (Integration) ← +1 (T075a: Thread-based verification)
- Phase 9: 8 tasks (Test Migration)
- Phase 10: 10 tasks (Documentation & Validation) ← +1 (T084a: Static analysis verification)

**Parallel Opportunities**: 14 tasks marked [P] (5 in research, 5 in contracts, 6 in documentation) ← +1 (T084a)

**MVP Scope**: Phases 1-4 (35 tasks) - Foundation + ProfileSelectionViewModel extraction validates pattern

**Independent Test Criteria**: Each phase includes checkpoint verification with all tests passing
