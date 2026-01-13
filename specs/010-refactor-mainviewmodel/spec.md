# Feature Specification: MainViewModel Refactoring for Improved Maintainability and Testability

**Feature Branch**: `010-refactor-mainviewmodel`
**Created**: 2026-01-13
**Status**: Draft
**Input**: User description: "MoLe アプリの MainViewModel をリファクタリングして、保守性とテスト可能性を向上させたい。"

## Problem Statement

The current MainViewModel has grown to 800 lines of code and suffers from multiple architectural issues:

### Maintainability Challenges
- File is too long to easily navigate and understand
- Single changes can have unexpected side effects across unrelated features
- Code reviews are difficult due to the scope of responsibilities in one file

### Testing Challenges
- One large test file must cover all functionality
- Tests are slow because all dependencies must be set up for every test
- When tests fail, it's difficult to identify which specific functionality broke
- Cannot test individual features in isolation

### Responsibility Overload
The MainViewModel currently handles six distinct concerns:
- Profile (user account) selection and management
- Account (ledger accounts) list display and filtering
- Transaction list display and search
- Screen-to-screen navigation
- Background data synchronization
- Drawer (side menu) open/close state

### Impact on Development
- Unclear where to add new code for features
- Adding complexity to an already complex file
- Difficult to onboard new developers
- Slows down feature development velocity

## Business Value

### Development Speed
Enable faster feature development by making code easier to understand and modify. Developers should be able to locate and change feature-specific code without navigating through unrelated functionality.

### Quality Improvement
Reduce bugs through better separation of concerns and more comprehensive, focused testing. Each component should be independently testable, making it easier to verify correctness.

### Long-term Maintainability
Create a codebase that future developers can easily understand and extend. Clear responsibility boundaries make the system easier to reason about and modify.

### User Value
- **Stability**: All existing features continue to work exactly as before (zero user-facing changes)
- **Future improvements**: Faster delivery of new features and bug fixes due to improved code organization

## User Scenarios & Testing

### User Story 1 - Developer Adds New Feature to Isolated Component (Priority: P1)

A developer needs to add pagination to the transaction list. With the refactored architecture, they can work on transaction-related code without touching account display, profile management, or navigation logic.

**Why this priority**: This is the core value proposition - enabling developers to work on isolated features without risk of breaking unrelated functionality. This directly addresses the main pain point of the current monolithic ViewModel.

**Independent Test**: Can be fully tested by attempting to add a new feature to one of the split components (e.g., adding a filter to transaction list) and verifying that the change requires touching only that specific component's code, and only that component's tests need to run.

**Acceptance Scenarios**:

1. **Given** the MainViewModel has been split into focused components, **When** a developer wants to modify transaction list behavior, **Then** they only need to open and understand the transaction-specific component (not profile, account, or navigation logic)

2. **Given** the refactored architecture, **When** a developer changes transaction list logic, **Then** only the transaction-specific tests need to run to verify the change

3. **Given** focused component responsibilities, **When** a code review is requested for transaction changes, **Then** reviewers can understand the change without needing context about profile or account management

---

### User Story 2 - Developer Debugs Failing Tests Quickly (Priority: P1)

When a test fails, a developer can immediately identify which specific functionality is broken because tests are organized by component responsibility.

**Why this priority**: This directly addresses the "testing is difficult" problem. Fast feedback cycles are critical for developer productivity and code quality.

**Independent Test**: Can be fully tested by introducing a deliberate bug in one area (e.g., account filtering) and verifying that only the relevant component's tests fail, with clear indication of which functionality broke.

**Acceptance Scenarios**:

1. **Given** component-specific test files, **When** an account filtering test fails, **Then** the developer knows the issue is in the account component without investigating profile or transaction code

2. **Given** isolated test dependencies, **When** running tests for one component, **Then** only that component's dependencies need to be mocked (not all six responsibility areas)

3. **Given** faster test execution, **When** a developer runs component-specific tests, **Then** tests complete in under 1 second (compared to the full test suite)

---

### User Story 3 - New Developer Understands Codebase Structure (Priority: P2)

A new developer joining the team can quickly understand the application's architecture because components have clear, single responsibilities with obvious file names.

**Why this priority**: While important for team scalability, this can be achieved after the core refactoring (P1) is complete. It's a consequence of good structure rather than a prerequisite.

**Independent Test**: Can be fully tested by asking a developer unfamiliar with the codebase to locate where specific functionality lives (e.g., "Where is profile switching handled?") and measuring time to locate the correct component.

**Acceptance Scenarios**:

1. **Given** clearly named components, **When** a new developer needs to find profile selection logic, **Then** they can identify the correct file by name alone (e.g., ProfileSelectionViewModel)

2. **Given** single-responsibility components, **When** a new developer reads one component's code, **Then** they understand that component's full behavior without needing to cross-reference other files

3. **Given** component boundaries, **When** a new developer needs to understand data flow, **Then** dependencies are explicit in the constructor (no hidden global state)

---

### User Story 4 - Code Review Process is Streamlined (Priority: P2)

Code reviewers can quickly assess changes because the scope of each file is limited, making it obvious what functionality could be affected by a change.

**Why this priority**: This improves team efficiency but is a secondary benefit compared to the direct development and testing improvements.

**Independent Test**: Can be fully tested by measuring code review time before and after refactoring for equivalent changes, and counting the number of "could this affect X?" questions in reviews.

**Acceptance Scenarios**:

1. **Given** focused component files (under 300 lines each), **When** a reviewer sees a change to AccountSummaryViewModel, **Then** they know transaction list and profile selection are not affected

2. **Given** clear responsibility boundaries, **When** a pull request modifies one component, **Then** reviewers can approve without needing to mentally trace through the entire application

3. **Given** component isolation, **When** reviewing a change, **Then** the reviewer can understand the full context by reading only the changed component's file

---

### User Story 5 - All Existing Features Continue to Work (Priority: P1)

End users experience zero changes in functionality. All existing features (profile selection, account display, transaction list, navigation, data sync) work exactly as before.

**Why this priority**: This is a critical constraint - the refactoring must be invisible to end users. Any breaking change would be a failure.

**Independent Test**: Can be fully tested by running the complete existing test suite and performing manual testing of all user-facing features after refactoring.

**Acceptance Scenarios**:

1. **Given** the refactored code, **When** a user selects a different profile, **Then** the account and transaction lists update correctly (same as before refactoring)

2. **Given** the refactored code, **When** a user performs pull-to-refresh, **Then** data syncs from the server exactly as it did before

3. **Given** the refactored code, **When** a user navigates between screens, **Then** all navigation flows work identically to the pre-refactoring behavior

4. **Given** the refactored code, **When** running the existing test suite, **Then** all tests pass (or are appropriately migrated to test the new structure)

---

### Edge Cases

- What happens when a component needs to react to changes in another component's state? (e.g., transaction list needs to update when profile changes)
- How are shared concerns handled without creating new coupling? (e.g., loading states, error handling)
- What happens during the migration if tests fail - how do we ensure we haven't introduced regressions?
- How do we handle components that need to coordinate multiple responsibilities? (e.g., main coordinator managing tab state)
- What if the split reveals hidden dependencies we didn't know about?

## Requirements

### Functional Requirements

- **FR-001**: System MUST maintain 100% functional parity with existing MainViewModel behavior
- **FR-002**: Each component MUST have a single, clearly defined responsibility
- **FR-003**: Components MUST NOT exceed 300 lines of code
- **FR-004**: All existing test cases MUST pass after refactoring (or be appropriately migrated to new structure)
- **FR-005**: Each component MUST be independently testable without requiring full application context
- **FR-006**: Components MUST have explicit dependencies visible in their constructors (no hidden global state)
- **FR-007**: The refactoring MUST be performed incrementally with verification at each step
- **FR-008**: Each incremental step MUST maintain a working, testable application state
- **FR-009**: Profile selection changes MUST continue to update all dependent components (accounts, transactions)
- **FR-010**: Background data synchronization MUST continue to work [NEEDS CLARIFICATION: Should Thread-based sync be migrated to coroutines in this refactoring, or handled as a separate feature? Option A: Include coroutine migration. Option B: Keep existing Thread implementation and migrate in separate feature.]
- **FR-011**: Navigation between screens MUST continue to work with the same user flows
- **FR-012**: Drawer (side menu) state management MUST remain consistent
- **FR-013**: Tab switching (Accounts/Transactions) MUST work identically to current behavior
- **FR-014**: Account filtering (show/hide zero balance) MUST continue to function
- **FR-015**: Transaction search and filtering MUST continue to work
- **FR-016**: Date navigation in transaction list MUST continue to work

### Organizational Requirements

- **OR-001**: Profile management logic [NEEDS CLARIFICATION: Should profile management remain part of the main screen, or be extracted as an independent feature? Option A: Keep as part of main screen (current architecture). Option B: Extract to separate ProfileManagement feature with its own navigation.]
- **OR-002**: Code organization MUST make it obvious where to add new features
- **OR-003**: Component names MUST clearly indicate their responsibility (e.g., AccountSummaryViewModel, TransactionListViewModel)
- **OR-004**: Each component MUST have its own dedicated test file
- **OR-005**: Component tests MUST mock only the direct dependencies of that component
- **OR-006**: Test organization MUST mirror component organization

### Key Entities

- **ViewModel Component**: A single-responsibility class that manages state and business logic for one specific area of functionality. Contains explicit dependencies injected via constructor. Exposes state via observable streams. Handles events specific to its responsibility area.

- **Component Responsibility Boundary**: The clear line defining what functionality belongs to which component. For example: profile selection is separate from account display, which is separate from transaction filtering. No component should handle concerns from another component's domain.

- **Shared State**: Data that multiple components need to observe (e.g., currently selected profile). Managed centrally (e.g., in a Repository) and observed by components that need it, rather than duplicated across components.

- **Test Component**: An isolated test file that verifies one component's behavior using mocked dependencies. Contains only the test setup needed for that specific component's functionality.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Developers can locate feature-specific code in under 30 seconds (compared to 2-3 minutes currently) [NEEDS CLARIFICATION: Which metrics should we use to measure success? Option A: File size (each component under 300 lines). Option B: Test execution time (component tests under 1 second). Option C: Code coverage (each component above 80%). Option D: All of the above.]

- **SC-002**: Individual component test suites execute in under 1 second (compared to full test suite taking 5+ seconds)

- **SC-003**: Code review time for typical changes reduces by 40% due to limited scope of changes

- **SC-004**: 100% of existing functionality continues to work without user-facing changes

- **SC-005**: New feature additions require changes to only one component in 80% of cases (compared to frequently needing to modify the monolithic MainViewModel)

- **SC-006**: Test failures immediately identify which specific functionality broke (100% clear attribution vs. current ambiguity)

- **SC-007**: New developers can understand one component's full responsibility in under 15 minutes (compared to needing hours to understand the full MainViewModel)

- **SC-008**: Zero regression bugs introduced during refactoring (verified by existing test suite + manual testing)

## Assumptions

- The existing test suite adequately covers MainViewModel behavior and can be used to verify refactoring correctness
- The current repository pattern and dependency injection architecture are sound and don't need to change
- Component boundaries can be cleanly defined along the six identified responsibility areas
- Developers are familiar with single-responsibility principle and dependency injection patterns
- The build and test infrastructure can support multiple ViewModel files without configuration changes
- Incremental refactoring is preferred over a big-bang rewrite to reduce risk

## Constraints

### Technical Constraints

- Must maintain existing repository pattern and dependency injection architecture
- Must work with current Android/Kotlin/Jetpack Compose technology stack
- Database structure cannot change
- Existing data flow patterns (StateFlow, Repository pattern) must be preserved

### Business Constraints

- Zero user-facing breaking changes allowed
- Must be implemented incrementally (not all-at-once) to manage risk
- Each incremental step must be tested and verified before proceeding
- Must be tested on real devices at each verification step
- All builds and tests must pass before any code is committed

### Timeline Constraints

- Real device verification required after each phase
- Build and test verification required after each change
- Cannot compromise existing feature stability for refactoring improvements

## Dependencies

- Existing repository implementations (ProfileRepository, AccountRepository, TransactionRepository, etc.)
- Existing dependency injection setup (Hilt)
- Existing test infrastructure
- Current build system (Gradle, Nix)

## Out of Scope

The following are explicitly NOT included in this refactoring:

- Changing database schema or Room entities
- Modifying Repository implementations (unless minor changes needed for new ViewModels)
- Adding new features beyond the refactoring itself
- Changing UI/Compose components (only ViewModel layer affected)
- Performance optimizations beyond those naturally resulting from better organization
- Migrating other ViewModels (focus is only on MainViewModel)

## Risks

- **Risk**: Hidden dependencies between responsibilities may be discovered during refactoring
  - **Mitigation**: Incremental approach allows early detection; existing tests will catch breaking changes

- **Risk**: Shared state management may be complex with multiple ViewModels
  - **Mitigation**: Repository pattern already provides shared state via singleton-scoped repositories

- **Risk**: Refactoring may take longer than expected if component boundaries are unclear
  - **Mitigation**: Start with clearest boundaries first (transaction vs account vs profile); defer ambiguous areas

- **Risk**: Tests may need significant restructuring
  - **Mitigation**: Migrate tests incrementally alongside component extraction; ensure tests pass at each step

- **Risk**: Build or runtime errors may occur during intermediate states
  - **Mitigation**: Each incremental step maintains a buildable, testable state; verify before moving to next step
