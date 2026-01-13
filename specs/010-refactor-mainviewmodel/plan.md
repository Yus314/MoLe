# Implementation Plan: MainViewModel Refactoring for Improved Maintainability and Testability

**Branch**: `010-refactor-mainviewmodel` | **Date**: 2026-01-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/010-refactor-mainviewmodel/spec.md`

## Summary

Refactor the monolithic 800-line MainViewModel into focused, single-responsibility components following Google's layered architecture guidelines. Split into ProfileSelectionViewModel, AccountSummaryViewModel, TransactionListViewModel, and MainCoordinatorViewModel. Each component manages its own state via UiState pattern, observes shared Repository state via StateFlow, and maintains independent loading/error states. The refactoring follows TDD principles with incremental verification at each step, ensuring 100% functional parity and zero regression throughout the process.

## Technical Context

**Language/Version**: Kotlin 2.0.21 (JVM target 1.8)
**Primary Dependencies**: Hilt 2.51.1, Jetpack Compose (composeBom 2024.12.01), Room 2.4.2, Coroutines 1.9.0, AndroidX Lifecycle 2.4.1
**Storage**: Room Database (SQLite) - existing, no schema changes
**Testing**: JUnit 4, MockK for mocking, Fake repositories for ViewModel tests, StandardTestDispatcher for coroutines
**Target Platform**: Android (minimum SDK level maintained by existing project)
**Project Type**: Mobile (Android application)
**Performance Goals**: Component tests execute in under 1 second, each component under 300 lines
**Constraints**: Zero user-facing changes, all existing tests must pass, Thread-based sync preserved (coroutine migration deferred)
**Scale/Scope**: 4 new ViewModels, 4 new test files, 1 new PreferencesRepository, affects MainActivityCompose integration

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Compliance Status

✅ **I. Code Readability & Maintainability**
- Single-responsibility components with clear naming (AccountSummaryViewModel, TransactionListViewModel)
- Each component under 300 lines (FR-003)
- Explicit dependencies via constructor injection (FR-006)

✅ **II. Test-Driven Development (TDD)**
- Tests for each new component required before implementation
- Existing MainViewModelTest serves as contract verification
- 80% coverage target per component (SC-001)
- Fake repositories for deterministic testing already exist

✅ **III. Incremental Development**
- Refactoring performed in phases with verification at each step (FR-007)
- Each incremental step maintains working state (FR-008)
- Test failure triggers immediate rollback (clarification Q3)
- Commits after each phase completion

✅ **IV. Performance Optimization**
- Component tests target under 1 second execution (SC-002)
- No premature optimization - focus on architecture first

✅ **V. Accessibility**
- No UI/Compose component changes (only ViewModel layer affected)
- Accessibility not impacted by this refactoring

✅ **VI. Kotlin Code Standards**
- All code in Kotlin 2.0.21
- Use coroutines (viewModelScope), StateFlow, data classes
- No `!!` operator without justification
- Prefer `val` over `var`

✅ **VII. Nix Development Environment**
- Build via `nix run .#build`
- Test via `nix run .#test`
- Verify via `nix run .#verify` (test + build + install)

✅ **VIII. Dependency Injection (Hilt)**
- All new ViewModels use `@HiltViewModel` and `@Inject constructor`
- MainActivityCompose is `@AndroidEntryPoint`
- ViewModels obtain dependencies via constructor (ProfileRepository, AccountRepository, etc.)

✅ **IX. Static Analysis & Linting**
- ktlint/detekt via pre-commit hooks
- Existing .editorconfig rules apply
- detekt.yml: maxIssues = -1 (warnings only)

✅ **X. Layered Architecture (Google Guidelines)**
- **Data Layer**: Existing Repository pattern (ProfileRepository, AccountRepository, TransactionRepository, etc.)
- **Domain Layer**: Not required for this refactoring (simple CRUD operations, no complex UseCase needed)
- **UI Layer**: ViewModels expose UiState via StateFlow, handle events, delegate to Repositories
- **Unidirectional Data Flow**: Events → ViewModel → Repository → StateFlow → UI
- **Single Source of Truth**: Repositories (singleton-scoped) provide shared state via StateFlow

### Gates

- [ ] **GATE 1 (Pre-Phase 0)**: All Constitution principles reviewed - **PASS** ✅
- [ ] **GATE 2 (Post-Phase 1)**: Design artifacts reviewed against Constitution - **PENDING**

## Project Structure

### Documentation (this feature)

```text
specs/010-refactor-mainviewmodel/
├── spec.md              # Feature specification (completed)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (architectural decisions)
├── data-model.md        # Phase 1 output (UiState structures, component boundaries)
├── contracts/           # Phase 1 output (ViewModel contracts, Event/Effect interfaces)
├── quickstart.md        # Phase 1 output (developer guide for working with split ViewModels)
├── checklists/          # Quality validation checklists
│   └── requirements.md  # Spec validation checklist (completed)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

**Current Structure** (Android application):

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── data/repository/                  # Data Layer (existing)
│   ├── ProfileRepository.kt
│   ├── AccountRepository.kt
│   ├── TransactionRepository.kt
│   └── (other repositories...)
│
├── dao/                              # Room DAOs (existing)
├── db/                               # Room Entities (existing)
│
├── di/                               # Hilt DI modules
│   ├── DatabaseModule.kt             # Existing
│   └── RepositoryModule.kt           # Existing
│
├── ui/                               # UI Layer
│   ├── main/                         # Main screen (refactoring target)
│   │   ├── MainViewModel.kt          # 🔴 TO BE SPLIT (800 lines)
│   │   ├── MainUiState.kt            # 🟡 TO BE UPDATED (new states)
│   │   ├── MainScreen.kt             # 🟡 TO BE UPDATED (new ViewModels)
│   │   ├── AccountSummaryTab.kt      # Existing (minimal changes)
│   │   └── TransactionListTab.kt     # Existing (minimal changes)
│   │
│   ├── activity/
│   │   └── MainActivityCompose.kt    # 🟡 TO BE UPDATED (inject 4 ViewModels)
│   │
│   ├── transaction/                  # Other screens (no changes)
│   ├── profile/
│   ├── theme/
│   └── components/
│
└── service/                          # Services
    ├── AppStateService.kt            # Existing (used by Coordinator)
    └── BackgroundTaskManager.kt      # Existing (Thread-based, preserved)

app/src/test/kotlin/net/ktnx/mobileledger/
└── ui/main/
    ├── MainViewModelTest.kt          # 🔴 TO BE SPLIT (migrate tests)
    ├── FakeProfileRepositoryForViewModel.kt      # Existing
    ├── FakeAccountRepositoryForViewModel.kt      # Existing
    └── FakeTransactionRepositoryForViewModel.kt  # Existing
```

**After Refactoring** (new files marked with ✨):

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── data/repository/
│   ├── PreferencesRepository.kt      # ✨ NEW (replace App static methods)
│   └── (existing repositories...)
│
├── di/
│   └── RepositoryModule.kt           # 🟡 UPDATED (bind PreferencesRepository)
│
├── ui/main/
│   ├── ProfileSelectionViewModel.kt  # ✨ NEW (~150 lines)
│   ├── AccountSummaryViewModel.kt    # ✨ NEW (~250 lines)
│   ├── TransactionListViewModel.kt   # ✨ NEW (~250 lines)
│   ├── MainCoordinatorViewModel.kt   # ✨ NEW (~250 lines)
│   ├── MainViewModel.kt              # 🗑 TO BE DELETED (after migration)
│   ├── MainUiState.kt                # 🟡 UPDATED (split into component UiStates)
│   ├── MainScreen.kt                 # 🟡 UPDATED (inject 4 ViewModels)
│   ├── AccountSummaryTab.kt
│   └── TransactionListTab.kt
│
└── ui/activity/
    └── MainActivityCompose.kt        # 🟡 UPDATED (inject 4 ViewModels)

app/src/test/kotlin/net/ktnx/mobileledger/
├── data/repository/
│   └── PreferencesRepositoryTest.kt  # ✨ NEW
│
└── ui/main/
    ├── ProfileSelectionViewModelTest.kt  # ✨ NEW
    ├── AccountSummaryViewModelTest.kt    # ✨ NEW
    ├── TransactionListViewModelTest.kt   # ✨ NEW
    ├── MainCoordinatorViewModelTest.kt   # ✨ NEW
    ├── MainViewModelTest.kt              # 🗑 TO BE DELETED
    └── (existing Fake repositories...)
```

**Structure Decision**: Android application with existing Repository pattern. Refactoring affects only the UI Layer (ViewModels). Data Layer remains unchanged except for new PreferencesRepository. No database schema changes. No UI/Compose component changes (only ViewModel integration points in MainActivityCompose and MainScreen).

## Complexity Tracking

**No Constitution violations requiring justification.**

All requirements align with Constitution principles:
- TDD enforced at each phase
- Layered architecture maintained (Data → UI)
- Hilt DI used throughout
- Incremental development with rollback policy
- No performance trade-offs

**Architectural Patterns Applied**:
1. **Repository Pattern**: Shared state via singleton-scoped Repositories (existing)
2. **UiState + Events + Effects**: Immutable state management per component
3. **Single Responsibility Principle**: Each ViewModel handles one domain
4. **Dependency Injection**: All dependencies via Hilt constructor injection
5. **Unidirectional Data Flow**: Events → ViewModel → Repository → StateFlow → UI

## Phases

### Phase 0: Outline & Research

**Prerequisites**: Constitution Check GATE 1 passed ✅

**Objectives**:
1. Document architectural decisions for component boundaries
2. Define UiState structures for each ViewModel
3. Clarify shared state management strategy (Repository-based reactive updates)
4. Document PreferencesRepository design
5. Define Event/Effect patterns for each component

**Unknowns to Research** (extracted from Technical Context):
- ❓ Optimal component boundary definition (which responsibilities belong to which ViewModel)
- ❓ PreferencesRepository implementation approach (SharedPreferences vs DataStore)
- ❓ Best practices for coordinating multiple ViewModels in one Activity
- ❓ Testing strategy for ViewModel interaction via shared Repository state
- ❓ Migration path for existing MainViewModel tests

**Research Tasks**:

1. **Component Boundary Analysis**
   - Task: Analyze MainViewModel.kt lines 1-800 to identify distinct responsibility clusters
   - Goal: Define clear boundaries for ProfileSelection, AccountSummary, TransactionList, Coordinator
   - Output: Responsibility mapping table in research.md

2. **Preferences Repository Design**
   - Task: Evaluate SharedPreferences vs DataStore Preferences for simple key-value storage
   - Goal: Choose implementation approach for PreferencesRepository
   - Alternatives: SharedPreferences (simple, synchronous), DataStore Preferences (Flow-based, modern)
   - Output: Decision in research.md with rationale

3. **Multi-ViewModel Activity Pattern**
   - Task: Research best practices for Activity with multiple ViewModels
   - Goal: Define pattern for MainActivityCompose to coordinate 4 ViewModels
   - Alternatives: Pass all ViewModels to Screen Composable, Use Coordinator to mediate, Direct ViewModel access in child Composables
   - Output: Pattern recommendation in research.md

4. **Shared State Testing Strategy**
   - Task: Design testing approach for components observing same Repository StateFlow
   - Goal: Ensure components can be tested independently despite shared state
   - Approach: Use Fake repositories with controlled state, verify each ViewModel reacts correctly
   - Output: Testing pattern in research.md

5. **Test Migration Strategy**
   - Task: Plan migration of MainViewModelTest cases to new component tests
   - Goal: Map each existing test to appropriate new ViewModel test
   - Output: Test migration table in research.md

**Output Artifact**: `specs/010-refactor-mainviewmodel/research.md`

**Format**:
```markdown
# Research & Architectural Decisions: MainViewModel Refactoring

## Decision 1: Component Boundaries

**Decision**: [Split MainViewModel into 4 components: ProfileSelection (profile mgmt), AccountSummary (account list), TransactionList (transaction list), Coordinator (UI orchestration)]

**Rationale**: [Based on analysis of MainViewModel.kt, these boundaries align with FR-002 (single responsibility), minimize cross-component dependencies, and allow independent testing]

**Alternatives Considered**:
- Alternative A: [3-way split without Coordinator - rejected because navigation/drawer logic needs a home]
- Alternative B: [5-way split separating navigation from coordination - rejected as over-engineering]

**Implementation Notes**: [Coordinator handles ONLY UI orchestration (tab state, drawer state, navigation events), delegates all domain logic to specialized ViewModels]

---

## Decision 2: PreferencesRepository Implementation

**Decision**: [Use SharedPreferences via PreferencesRepository interface]

**Rationale**: [SharedPreferences sufficient for simple key-value storage (showZeroBalanceAccounts), synchronous API acceptable for non-performance-critical reads, existing pattern in codebase, lower complexity than DataStore for this use case]

**Alternatives Considered**:
- Alternative A: [DataStore Preferences - rejected due to added complexity for simple boolean flags, requires migration from SharedPreferences]

**Implementation Notes**: [PreferencesRepository exposes getShowZeroBalanceAccounts(): Boolean and setShowZeroBalanceAccounts(Boolean), injected via Hilt]

---

[Continue with Decision 3, 4, 5...]
```

### Phase 1: Design & Contracts

**Prerequisites**: `research.md` complete, Constitution GATE 1 passed ✅

**Objectives**:
1. Define UiState data classes for each ViewModel
2. Define Event/Effect sealed classes for each ViewModel
3. Document ViewModel contracts (public API surface)
4. Create developer quickstart guide
5. Update agent context files with new components

**Tasks**:

1. **Data Model Design** (`data-model.md`)

   Extract entities from feature spec and research.md:

   **Entities**:
   - **ProfileSelectionUiState**: Selected profile, profile list, loading/error states
   - **AccountSummaryUiState**: Account list, zero-balance filter, expanded states, loading/error
   - **TransactionListUiState**: Transaction list, filter input, suggestions, date range, loading/error
   - **MainCoordinatorUiState**: Selected tab, drawer state, refresh state, navigation effects
   - **PreferencesRepository**: showZeroBalanceAccounts storage

   **Relationships**:
   - All ViewModels observe ProfileRepository.currentProfile (shared state)
   - AccountSummaryViewModel reads PreferencesRepository for filter preference
   - MainCoordinatorViewModel coordinates UI state but doesn't own domain data

   **Validation Rules**:
   - Component UiState must include isLoading, error fields (clarification Q2)
   - UiState must be immutable data classes
   - Effects must be delivered via Channel<Effect>.receiveAsFlow()

   **State Transitions**:
   - Profile selection: SelectProfile event → Repository updates → all VMs react via Flow
   - Loading flow: isLoading=true → Repository call → isLoading=false + data/error

2. **API Contracts** (`contracts/`)

   Generate ViewModel contracts from functional requirements:

   **File**: `contracts/ProfileSelectionViewModel.contract.md`
   ```markdown
   # ProfileSelectionViewModel Contract

   ## State
   - `currentProfile: StateFlow<Profile?>`
   - `allProfiles: StateFlow<List<Profile>>`
   - `uiState: StateFlow<ProfileSelectionUiState>`

   ## Events
   - `SelectProfile(profileId: Long)`
   - `ReorderProfiles(orderedProfiles: List<ProfileListItem>)`

   ## Effects
   - None (profile changes propagate via Repository StateFlow)

   ## Dependencies
   - ProfileRepository
   ```

   **File**: `contracts/AccountSummaryViewModel.contract.md`
   [Similar structure for AccountSummary]

   **File**: `contracts/TransactionListViewModel.contract.md`
   [Similar structure for TransactionList]

   **File**: `contracts/MainCoordinatorViewModel.contract.md`
   [Similar structure for Coordinator]

   **File**: `contracts/PreferencesRepository.contract.md`
   ```markdown
   # PreferencesRepository Contract

   ## Methods
   - `getShowZeroBalanceAccounts(): Boolean`
   - `setShowZeroBalanceAccounts(value: Boolean)`

   ## Implementation
   - SharedPreferences-based
   - Singleton-scoped
   ```

3. **Agent Context Update**

   Run agent context update script:
   ```bash
   .specify/scripts/bash/update-agent-context.sh claude
   ```

   **Expected Updates** (to appropriate agent-specific context file):
   - Add: PreferencesRepository (new component)
   - Add: ProfileSelectionViewModel, AccountSummaryViewModel, TransactionListViewModel, MainCoordinatorViewModel (new components)
   - Add: UiState + Events + Effects pattern
   - Preserve: Existing Manual Additions section

**Output Artifacts**:
- `specs/010-refactor-mainviewmodel/data-model.md`
- `specs/010-refactor-mainviewmodel/contracts/*.contract.md`
- `specs/010-refactor-mainviewmodel/quickstart.md`
- `.specify/memory/agent-context-claude.md` (or equivalent agent-specific file)

**GATE 2 (Post-Phase 1)**: Review design artifacts against Constitution ✅

### Phase 2: Task Decomposition

**Note**: Phase 2 (`/speckit.tasks` command) is NOT executed by `/speckit.plan`.

Phase 2 will generate `tasks.md` with dependency-ordered implementation tasks based on the design artifacts created in Phase 0-1.

**Expected Task Groups**:
1. Phase A: PreferencesRepository creation
2. Phase B: ProfileSelectionViewModel extraction
3. Phase C: AccountSummaryViewModel extraction
4. Phase D: TransactionListViewModel extraction
5. Phase E: MainCoordinatorViewModel conversion
6. Phase F: MainActivityCompose integration
7. Phase G: Test migration and cleanup

## Verification Strategy

**At Each Phase**:
- Run `nix run .#test` - all tests must pass
- Run `nix run .#build` - build must succeed
- Review against Constitution principles
- Commit with descriptive message

**Test Failure Policy** (FR-008, clarification Q3):
- If any test fails after a change:
  1. Immediately discard changes (`git reset --hard` or `git revert`)
  2. Analyze failure root cause
  3. Retry with alternative approach
  4. Do NOT proceed until all tests pass

**Integration Verification**:
- After all phases complete: `nix run .#verify`
- Manual testing on real device:
  - Profile selection works
  - Account list displays
  - Transaction list displays
  - Tab switching works
  - Pull-to-refresh works
  - Navigation works

## Risks & Mitigations

**Risk**: Hidden dependencies discovered during refactoring
- **Mitigation**: Document in Assumptions, resolve via Repository pattern, or adjust component boundaries (clarification Q5)
- **Checkpoint**: Each phase verifies tests pass

**Risk**: Test restructuring more complex than expected
- **Mitigation**: research.md documents test migration table, migrate incrementally per component
- **Checkpoint**: Existing Fake repositories reused, no new test infrastructure needed

**Risk**: MainActivityCompose integration breaks existing behavior
- **Mitigation**: Incremental integration per ViewModel, verify with existing test suite after each
- **Checkpoint**: Manual device testing at Phase F completion

## Success Criteria Tracking

**From spec.md Success Criteria**:

- **SC-001**: File size under 300 lines per component
  - Measure: `wc -l` on each new ViewModel file
  - Target: ProfileSelectionViewModel ~150, others ~250

- **SC-002**: Component tests execute in under 1 second
  - Measure: `./gradlew test --tests <ComponentViewModelTest>`
  - Target: Each test class under 1000ms

- **SC-003**: Code review time reduces by 40%
  - Measure: Track PR review duration after refactoring vs before
  - Target: Typical change review under 30 minutes

- **SC-004**: 100% functional parity
  - Measure: All existing MainViewModelTest cases migrated and passing
  - Measure: Manual device testing checklist complete

- **SC-005**: 80% of changes affect only one component
  - Measure: Track future PRs touching main screen ViewModels
  - Target: 8/10 PRs modify single ViewModel

- **SC-006**: Test failures immediately identify broken functionality
  - Measure: Test names clearly indicate component (ProfileSelectionViewModelTest, etc.)
  - Target: 100% clear attribution

- **SC-007**: New developers understand component in under 15 minutes
  - Measure: Time for unfamiliar developer to locate functionality
  - Target: <15 minutes to find profile selection logic

- **SC-008**: Zero regression bugs
  - Measure: All tests pass, manual device testing complete
  - Target: No user-reported regressions post-release

## Next Steps

1. **Run `/speckit.plan`**: Generate research.md, data-model.md, contracts/ (Phase 0-1) ✅ **THIS FILE**
2. **Run `/speckit.tasks`**: Generate tasks.md with implementation steps (Phase 2)
3. **Run `/speckit.implement`**: Execute tasks.md implementation (Phase 3)
4. **Manual verification**: Test on real device
5. **Code review**: PR with all changes
6. **Merge**: After approval and CI success

---

**Plan Status**: ✅ Phase 0-1 planning complete. Ready for `/speckit.tasks` to generate implementation tasks.
