# Tasks: Domain Model Layer Introduction

**Input**: Design documents from `/specs/017-domain-model-layer/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Path Conventions

- **Android app**: `app/src/main/kotlin/net/ktnx/mobileledger/`
- **Tests**: `app/src/test/kotlin/net/ktnx/mobileledger/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create domain model package structure and shared types

### TDD: Tests First (Red Phase)

- [X] T001a [P] Write ValidationResultTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/ValidationResultTest.kt (test isSuccess, isError, Error.reasons)
- [X] T001b [P] Write FutureDatesTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/FutureDatesTest.kt (test enum values and fromInt conversion)

### Implementation (Green Phase)

- [X] T001 [P] Create ValidationResult sealed class in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/ValidationResult.kt
- [X] T002 [P] Create FutureDates enum in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/FutureDates.kt
- [X] T003 [P] Create CurrencyPosition enum in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/CurrencyPosition.kt
- [X] T004 Create mapper package directory at app/src/main/kotlin/net/ktnx/mobileledger/data/repository/mapper/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: None - no blocking prerequisites for this feature

**Note**: Each user story can be implemented independently after Setup phase.

**Checkpoint**: Setup ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Transaction Display Without Database Knowledge (Priority: P1) MVP

**Goal**: Enable ViewModel to display transactions using domain models without db package imports

**Independent Test**: TransactionListViewModel can be tested with Fake Repository returning domain models, no db entity mocks needed

### TDD: Tests First (Red Phase)

- [X] T005a [P] [US1] Write TransactionLineTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/TransactionLineTest.kt (test hasAmount, withAmount, withoutAmount)
- [X] T005b [P] [US1] Write TransactionTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/TransactionTest.kt (test validate(), hasAccountNamed(), withLine(), withUpdatedLine(), balance check)
- [X] T005c [US1] Write TransactionMapperTest in app/src/test/kotlin/net/ktnx/mobileledger/data/repository/mapper/TransactionMapperTest.kt (test toDomain for basic, null comment, empty accounts)

### Implementation (Green Phase)

- [X] T005 [P] [US1] Create TransactionLine domain model in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/TransactionLine.kt
- [X] T006 [P] [US1] Create Transaction domain model with validate() method in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/Transaction.kt
- [X] T007 [US1] Create TransactionMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/mapper/TransactionMapper.kt
- [X] T008 [US1] Update TransactionRepository interface to return domain models in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TransactionRepository.kt
- [X] T009 [US1] Update TransactionRepositoryImpl to use TransactionMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TransactionRepositoryImpl.kt
- [X] T010 [US1] Update FakeTransactionRepository to return domain models in app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeTransactionRepository.kt (also updated all test fakes)
- [X] T011 [US1] Update TransactionListViewModel to use domain model Transaction in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListViewModel.kt
- [X] T012 [US1] Update TransactionListUiState to use domain model Transaction in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListUiState.kt (using Transaction in convertToDisplayItems)
- [X] T013 [US1] Update TransactionListTab.kt to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/TransactionListTab.kt (no changes needed - already uses display items)
- [X] T014 [US1] Remove db package imports from TransactionListViewModel and verify no db.TransactionWithAccounts references

- [X] T014a [US1] **CHECKPOINT**: Run `nix run .#test && nix run .#build` to verify no regression

**Checkpoint**: User Story 1 complete - TransactionListViewModel no longer imports net.ktnx.mobileledger.db

---

## Phase 4: User Story 2 - Transaction Creation Without Database Coupling (Priority: P2)

**Goal**: Enable transaction creation/editing using domain models with business logic validation

**Independent Test**: NewTransactionViewModel validation logic can be tested with domain models only

### TDD: Tests First (Red Phase)

- [ ] T015a [US2] Add toEntity tests to TransactionMapperTest (test toEntity for new/existing transaction, roundTrip preservation)

### Implementation (Green Phase)

- [ ] T015 [US2] Add toEntity methods to TransactionMapper for saving in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/mapper/TransactionMapper.kt
- [ ] T016 [US2] Add insertTransaction/storeTransaction with domain model to TransactionRepository in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TransactionRepository.kt
- [ ] T017 [US2] Implement insertTransaction/storeTransaction in TransactionRepositoryImpl in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TransactionRepositoryImpl.kt
- [ ] T018 [US2] Update NewTransactionViewModel to use domain model Transaction in app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModel.kt
- [ ] T019 [US2] Update NewTransactionUiState to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionUiState.kt
- [ ] T020 [US2] Update NewTransactionScreen.kt to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionScreen.kt
- [ ] T021 [US2] Update TransactionSender/TransactionSenderImpl to accept domain model Transaction in app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/
- [ ] T022 [US2] Mark LedgerTransaction as @Deprecated in app/src/main/kotlin/net/ktnx/mobileledger/model/LedgerTransaction.kt
- [ ] T023 [US2] Mark LedgerTransactionAccount as @Deprecated in app/src/main/kotlin/net/ktnx/mobileledger/model/LedgerTransactionAccount.kt
- [ ] T024 [US2] Remove db package imports from NewTransactionViewModel and verify no db entity references
- [ ] T024a [US2] **CHECKPOINT**: Run `nix run .#verify` and manually test transaction creation flow

**Checkpoint**: User Story 2 complete - NewTransactionViewModel uses domain models for validation and saving

---

## Phase 5: User Story 3 - Profile Management Without Database Details (Priority: P3)

**Goal**: Enable profile management using domain models

**Independent Test**: ProfileDetailViewModel can be tested with Fake Repository returning domain Profile models

### TDD: Tests First (Red Phase)

- [ ] T025a [P] [US3] Write ProfileTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/ProfileTest.kt (test isAuthEnabled, canPost, defaultCommodityOrEmpty)
- [ ] T025b [P] [US3] Write ServerVersionTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/ServerVersionTest.kt (test displayString)
- [ ] T025c [US3] Write ProfileMapperTest in app/src/test/kotlin/net/ktnx/mobileledger/data/repository/mapper/ProfileMapperTest.kt (test toDomain with/without auth, toEntity, roundTrip)

### Implementation (Green Phase)

- [ ] T025 [P] [US3] Create ProfileAuthentication domain model in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/ProfileAuthentication.kt
- [ ] T026 [P] [US3] Create ServerVersion domain model in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/ServerVersion.kt
- [ ] T027 [US3] Create Profile domain model in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/Profile.kt
- [ ] T028 [US3] Create ProfileMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/mapper/ProfileMapper.kt
- [ ] T029 [US3] Update ProfileRepository interface to return domain models in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepository.kt
- [ ] T030 [US3] Update ProfileRepositoryImpl to use ProfileMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/ProfileRepositoryImpl.kt
- [ ] T031 [US3] Update FakeProfileRepository to return domain models in app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeProfileRepository.kt
- [ ] T032 [US3] Update ProfileSelectionViewModel to use domain model Profile in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModel.kt
- [ ] T033 [US3] Update ProfileSelectionUiState to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionUiState.kt
- [ ] T034 [US3] Update ProfileDetailViewModel to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/profiles/ProfileDetailViewModel.kt
- [ ] T035 [US3] Update ProfileDetailUiState to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/profiles/ProfileDetailUiState.kt
- [ ] T036 [US3] Update ProfileDetailScreen.kt to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/profiles/ProfileDetailScreen.kt
- [ ] T037 [US3] Update MainCoordinatorViewModel to use domain model Profile in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainCoordinatorViewModel.kt
- [ ] T038 [US3] Update MainActivityCompose to use domain model Profile in app/src/main/kotlin/net/ktnx/mobileledger/ui/activity/MainActivityCompose.kt
- [ ] T039 [US3] Update NavigationDrawer.kt to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/NavigationDrawer.kt
- [ ] T040 [US3] Remove db package imports from all Profile-related ViewModels
- [ ] T040a [US3] **CHECKPOINT**: Run `nix run .#verify` and test profile creation/editing flow

**Checkpoint**: User Story 3 complete - Profile ViewModels use domain models exclusively

---

## Phase 6: User Story 4 - Account Hierarchy Display (Priority: P4)

**Goal**: Enable account summary display using domain models

**Independent Test**: AccountSummaryViewModel can be tested with Fake Repository returning domain Account models

### TDD: Tests First (Red Phase)

- [ ] T041a [P] [US4] Write AccountTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/AccountTest.kt (test parentName, shortName, hasAmounts)
- [ ] T041b [US4] Write AccountMapperTest in app/src/test/kotlin/net/ktnx/mobileledger/data/repository/mapper/AccountMapperTest.kt (test toDomain with amounts, hierarchy level, expanded state)

### Implementation (Green Phase)

- [ ] T041 [P] [US4] Create AccountAmount domain model in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/AccountAmount.kt
- [ ] T042 [US4] Create Account domain model in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/Account.kt
- [ ] T043 [US4] Create AccountMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/mapper/AccountMapper.kt
- [ ] T044 [US4] Update AccountRepository interface to return domain models in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/AccountRepository.kt
- [ ] T045 [US4] Update AccountRepositoryImpl to use AccountMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/AccountRepositoryImpl.kt
- [ ] T046 [US4] Update FakeAccountRepository to return domain models in app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeAccountRepository.kt
- [ ] T047 [US4] Update AccountSummaryViewModel to use domain model Account in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryViewModel.kt
- [ ] T048 [US4] Update AccountSummaryUiState to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryUiState.kt
- [ ] T049 [US4] Update AccountSummaryTab.kt to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/main/AccountSummaryTab.kt
- [ ] T050 [US4] Mark LedgerAccount as @Deprecated in app/src/main/kotlin/net/ktnx/mobileledger/model/LedgerAccount.kt
- [ ] T051 [US4] Remove db package imports from AccountSummaryViewModel
- [ ] T051a [US4] **CHECKPOINT**: Run `nix run .#test && nix run .#build` and verify account list displays correctly

**Checkpoint**: User Story 4 complete - AccountSummaryViewModel uses domain models exclusively

---

## Phase 7: User Story 5 - Template and Currency Management (Priority: P5)

**Goal**: Enable template and currency management using domain models

**Independent Test**: Template and Currency ViewModels can be tested with Fake Repositories returning domain models

### TDD: Tests First (Red Phase)

- [ ] T052a [P] [US5] Write TemplateTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/TemplateTest.kt (basic data class tests)
- [ ] T052b [P] [US5] Write CurrencyTest in app/src/test/kotlin/net/ktnx/mobileledger/domain/model/CurrencyTest.kt (test CurrencyPosition enum)
- [ ] T052c [P] [US5] Write TemplateMapperTest in app/src/test/kotlin/net/ktnx/mobileledger/data/repository/mapper/TemplateMapperTest.kt (test toDomain with lines)
- [ ] T052d [P] [US5] Write CurrencyMapperTest in app/src/test/kotlin/net/ktnx/mobileledger/data/repository/mapper/CurrencyMapperTest.kt (test toDomain, toEntity)

### Implementation (Green Phase)

- [ ] T052 [P] [US5] Create TemplateLine domain model in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/TemplateLine.kt
- [ ] T053 [P] [US5] Create Currency domain model in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/Currency.kt
- [ ] T054 [US5] Create Template domain model in app/src/main/kotlin/net/ktnx/mobileledger/domain/model/Template.kt
- [ ] T055 [P] [US5] Create TemplateMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/mapper/TemplateMapper.kt
- [ ] T056 [P] [US5] Create CurrencyMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/mapper/CurrencyMapper.kt
- [ ] T057 [US5] Update TemplateRepository interface to return domain models in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TemplateRepository.kt
- [ ] T058 [US5] Update TemplateRepositoryImpl to use TemplateMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/TemplateRepositoryImpl.kt
- [ ] T059 [US5] Update CurrencyRepository interface to return domain models in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/CurrencyRepository.kt
- [ ] T060 [US5] Update CurrencyRepositoryImpl to use CurrencyMapper in app/src/main/kotlin/net/ktnx/mobileledger/data/repository/CurrencyRepositoryImpl.kt
- [ ] T061 [US5] Update FakeTemplateRepository to return domain models in app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeTemplateRepository.kt
- [ ] T062 [US5] Update FakeCurrencyRepository to return domain models in app/src/test/kotlin/net/ktnx/mobileledger/fake/FakeCurrencyRepository.kt
- [ ] T063 [US5] Update TemplateListViewModelCompose to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateListViewModelCompose.kt
- [ ] T064 [US5] Update TemplateDetailViewModelCompose to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplateDetailViewModelCompose.kt
- [ ] T065 [US5] Update TemplateApplicatorViewModel to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TemplateApplicatorViewModel.kt
- [ ] T066 [US5] Update TemplatesScreen.kt to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/templates/TemplatesScreen.kt
- [ ] T067 [US5] Update CurrencyPickerDialog to use domain model in app/src/main/kotlin/net/ktnx/mobileledger/ui/components/CurrencyPickerDialog.kt
- [ ] T068 [US5] Remove db package imports from Template and Currency ViewModels
- [ ] T068a [US5] **CHECKPOINT**: Run `nix run .#test && nix run .#build` and verify template management works

**Checkpoint**: User Story 5 complete - All template and currency code uses domain models

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup, verification, and documentation

- [ ] T069 Verify all ViewModels have no net.ktnx.mobileledger.db imports (static analysis check)
- [ ] T070 [P] Remove @Deprecated classes from model package (LedgerTransaction.kt, LedgerTransactionAccount.kt, LedgerAccount.kt) after all references migrated
- [ ] T071 [P] Update CLAUDE.md with domain model usage guidelines
- [ ] T072 Run `nix run .#test` to verify all unit tests pass
- [ ] T073 Run `nix run .#build` to verify build succeeds
- [ ] T074 Run `nix run .#verify` for full validation including device install
- [ ] T075 Run `nix run .#coverage` and verify domain/model + data/repository/mapper packages have ≥80% coverage (SC-005)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: None required for this feature
- **User Stories (Phase 3-7)**: All depend on Setup (Phase 1) completion
  - User stories can proceed in priority order (P1 → P2 → P3 → P4 → P5)
  - Or in parallel by different team members after Setup
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Setup only - Transaction display
- **User Story 2 (P2)**: US1 - Transaction creation (uses same Transaction domain model)
- **User Story 3 (P3)**: Setup only - Profile management (independent entity)
- **User Story 4 (P4)**: Setup only - Account display (independent entity)
- **User Story 5 (P5)**: Setup only - Template/Currency (independent entities)

### Within Each User Story

- Domain models before Mapper
- Mapper before Repository changes
- Repository changes before ViewModel updates
- ViewModel before UI updates
- All updates before @Deprecated marking

### Parallel Opportunities

**Setup Phase**:
```bash
# Launch all setup tasks together:
Task: "Create ValidationResult sealed class"
Task: "Create FutureDates enum"
Task: "Create CurrencyPosition enum"
```

**User Story 1 - Domain Models**:
```bash
# Launch domain models together:
Task: "Create TransactionLine domain model"
Task: "Create Transaction domain model"
```

**User Story 5 - Mappers**:
```bash
# Launch mappers together:
Task: "Create TemplateMapper"
Task: "Create CurrencyMapper"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 3: User Story 1 (Transaction Display)
3. **STOP and VALIDATE**: Test TransactionListViewModel with domain models
4. Verify no db imports in TransactionListViewModel

### Incremental Delivery

1. Setup → Foundation ready
2. User Story 1 → Transaction display with domain models → Deploy/Demo (MVP!)
3. User Story 2 → Transaction creation with domain models → Deploy/Demo
4. User Story 3 → Profile management with domain models → Deploy/Demo
5. User Story 4 → Account display with domain models → Deploy/Demo
6. User Story 5 → Template/Currency with domain models → Deploy/Demo
7. Polish → Cleanup and full verification

### Recommended Order

For single developer:
```text
Setup → US1 → US2 → US3 → US4 → US5 → Polish
```

US1 and US2 should be done sequentially (same Transaction entity).
US3, US4, US5 can be done in any order after US1.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Key success criterion: ViewModels have no `net.ktnx.mobileledger.db` imports
