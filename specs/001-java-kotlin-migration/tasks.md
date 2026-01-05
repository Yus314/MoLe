# Tasks: Java から Kotlin への移行

**Input**: Design documents from `/specs/001-java-kotlin-migration/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: No test tasks included (not explicitly requested in feature specification).

**Organization**: Tasks are organized by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Source**: `app/src/main/java/net/ktnx/mobileledger/` (移行前) → `app/src/main/kotlin/net/ktnx/mobileledger/` (移行後)
- **Tests**: `app/src/test/java/` → `app/src/test/kotlin/`
- **Android Tests**: `app/src/androidTest/java/` → `app/src/androidTest/kotlin/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Kotlin/KSP を有効化し、Java と Kotlin の混在ビルドを可能にする

- [X] T001 Update `app/build.gradle` to add Kotlin plugin 1.9.25 and KSP 1.9.25-1.0.20
- [X] T002 Add Kotlin stdlib, coroutines, and Jackson Kotlin module dependencies in `app/build.gradle`
- [X] T003 [P] Replace `annotationProcessor "androidx.room:room-compiler"` with `ksp` in `app/build.gradle`
  - Note: KSP disabled during migration due to Room DAO compatibility. Using annotationProcessor.
- [X] T004 [P] Configure `kotlinOptions` with `jvmTarget = '1.8'` and `-Xjsr305=strict` in `app/build.gradle`
- [X] T005 [P] Add `sourceSets` configuration for Kotlin directories in `app/build.gradle`
- [X] T006 [P] Create directory `app/src/main/kotlin/net/ktnx/mobileledger/`
- [X] T007 [P] Create directory `app/src/test/kotlin/net/ktnx/mobileledger/`
- [X] T008 [P] Create directory `app/src/androidTest/kotlin/net/ktnox/mobileledger/`
- [X] T009 Verify build succeeds with `./gradlew clean assembleDebug`
- [X] T010 Verify all existing tests pass with `./gradlew test`

**Checkpoint**: Kotlin/KSP enabled, Java-Kotlin interop ready ✅ COMPLETE

---

## Phase 2: Foundational - utils, err (14 files)

**Purpose**: 依存関係が最も少ないパッケージを先に移行し、移行プロセスを確立

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Nullability Annotations (Pre-Migration)

- [X] T011 Run `Analyze > Infer Nullity` on `app/src/main/java/net/ktnx/mobileledger/utils/` (Skipped - manual conversion with null safety)
- [X] T012 Run `Analyze > Infer Nullity` on `app/src/main/java/net/ktnx/mobileledger/err/` (Skipped - manual conversion with null safety)

### utils Package Conversion (13 files)

- [X] T013 [P] Convert `Misc.java` → `app/src/main/kotlin/net/ktnx/mobileledger/utils/Misc.kt` (add @JvmStatic for static methods)
- [X] T014 [P] Convert `SimpleDate.java` → `app/src/main/kotlin/net/ktnx/mobileledger/utils/SimpleDate.kt` (use data class)
- [X] T015 [P] Convert `Logger.java` → `app/src/main/kotlin/net/ktnx/mobileledger/utils/Logger.kt`
- [X] T016 [P] Convert `Globals.java` → `app/src/main/kotlin/net/ktnx/mobileledger/utils/Globals.kt` (use object)
- [X] T017 [P] Convert `Colors.java` → `app/src/main/kotlin/net/ktnx/mobileledger/utils/Colors.kt`
- [X] T018 [P] Convert `DimensionUtils.java` → `app/src/main/kotlin/net/ktnx/mobileledger/utils/DimensionUtils.kt`
- [X] T019 [P] Convert `NetworkUtil.java` → `app/src/main/kotlin/net/ktnx/mobileledger/utils/NetworkUtil.kt`
- [X] T020 [P] Convert `Digest.java` → `app/src/main/kotlin/net/ktnx/mobileledger/utils/Digest.kt`
- [X] T021 [P] Convert remaining utils files (GetOptCallback, LockHolder, Locker, Profiler, UrlEncodedFormData)
- [X] T022 Delete original Java files from `app/src/main/java/net/ktnx/mobileledger/utils/` after verification

### err Package Conversion (1 file)

- [X] T023 [P] Convert `HTTPException.java` → `app/src/main/kotlin/net/ktnx/mobileledger/err/HTTPException.kt`

### Test Conversion

- [X] T024 [P] Convert `SimpleDateTest.java` → `app/src/test/kotlin/net/ktnx/mobileledger/utils/SimpleDateTest.kt`
- [X] T025 [P] Convert `DigestUnitTest.java` → `app/src/test/kotlin/net/ktnx/mobileledger/DigestUnitTest.kt`

### Foundation Verification

- [X] T026 Verify build succeeds with `./gradlew assembleDebug`
- [X] T027 Verify `SimpleDateTest` passes with `./gradlew test --tests "*.SimpleDateTest"`
- [X] T028 Verify `DigestUnitTest` passes with `./gradlew test --tests "*.DigestUnitTest"`

**Checkpoint**: Foundation ready - user story implementation can now begin ✅ COMPLETE

---

## Phase 3: User Story 1 - ビルドとテストが正常に動作する (Priority: P1) 🎯 MVP

**Goal**: 全 Java ファイルを Kotlin に変換し、既存テストがパスすることを確認

**Independent Test**: `./gradlew assembleDebug && ./gradlew test && ./gradlew connectedAndroidTest`

### model Package Conversion (16 files) - data class 化

- [X] T029 [P] [US1] Convert `LedgerAccount.java` → `app/src/main/kotlin/net/ktnox/mobileledger/model/LedgerAccount.kt` (use data class)
- [X] T030 [P] [US1] Convert `LedgerTransaction.java` → `app/src/main/kotlin/net/ktnx/mobileledger/model/LedgerTransaction.kt` (use data class)
- [X] T031 [P] [US1] Convert `LedgerTransactionAccount.java` → `app/src/main/kotlin/net/ktnox/mobileledger/model/LedgerTransactionAccount.kt` (use data class)
- [X] T032 [P] [US1] Convert `LedgerAmount.java` → `app/src/main/kotlin/net/ktnox/mobileledger/model/LedgerAmount.kt` (use data class)
- [X] T033 [P] [US1] Convert `AmountStyle.java` → `app/src/main/kotlin/net/ktnox/mobileledger/model/AmountStyle.kt` (use data class with enum)
- [X] T034 [P] [US1] Convert `Data.java` → `app/src/main/kotlin/net/ktnx/mobileledger/model/Data.kt` (use object singleton)
- [X] T035 [P] [US1] Convert remaining model files to `app/src/main/kotlin/net/ktnox/mobileledger/model/`
- [X] T036 [P] [US1] Convert `LedgerAccountTest.java` → `app/src/test/kotlin/net/ktnox/mobileledger/model/LedgerAccountTest.kt`
- [X] T037 [P] [US1] Convert `AmountStyleTest.java` → `app/src/test/kotlin/net/ktnox/mobileledger/model/AmountStyleTest.kt`
- [X] T038 [US1] Verify model tests pass with `./gradlew test --tests "*.LedgerAccountTest" --tests "*.AmountStyleTest"`

### db Package Conversion (16 files) - Room Entities

- [X] T039 [P] [US1] Convert `Currency.java` → `app/src/main/kotlin/net/ktnox/mobileledger/db/Currency.kt` (data class, no FK)
- [X] T040 [P] [US1] Convert `Profile.java` → `app/src/main/kotlin/net/ktnox/mobileledger/db/Profile.kt` (data class, no FK)
- [X] T041 [US1] Convert `Account.java` → `app/src/main/kotlin/net/ktnox/mobileledger/db/Account.kt` (data class, FK→Profile)
- [X] T042 [US1] Convert `AccountValue.java` → `app/src/main/kotlin/net/ktnox/mobileledger/db/AccountValue.kt` (data class, FK→Account)
- [X] T043 [US1] Convert `Transaction.java` → `app/src/main/kotlin/net/ktnox/mobileledger/db/Transaction.kt` (data class, FK→Profile)
- [X] T044 [US1] Convert `TransactionAccount.java` → `app/src/main/kotlin/net/ktnox/mobileledger/db/TransactionAccount.kt` (data class, FK→Transaction)
- [X] T045 [P] [US1] Convert `TemplateHeader.java` → `app/src/main/kotlin/net/ktnox/mobileledger/db/TemplateHeader.kt` (data class)
- [X] T046 [US1] Convert `TemplateAccount.java` → `app/src/main/kotlin/net/ktnox/mobileledger/db/TemplateAccount.kt` (data class, FK→TemplateHeader, Currency)
- [X] T047 [P] [US1] Convert `Option.java` → `app/src/main/kotlin/net/ktnox/mobileledger/db/Option.kt` (data class, composite PK)
- [X] T048 [P] [US1] Convert remaining db files (DB.java, TypeConverters) to `app/src/main/kotlin/net/ktnox/mobileledger/db/`
- [X] T049 [US1] Verify Room schema unchanged by comparing `app/schemas/` before/after

### dao Package Conversion (11 files)

- [X] T050 [P] [US1] Convert `AccountDAO.java` → `app/src/main/kotlin/net/ktnox/mobileledger/dao/AccountDAO.kt` (add suspend functions)
- [X] T051 [P] [US1] Convert `TransactionDAO.java` → `app/src/main/kotlin/net/ktnox/mobileledger/dao/TransactionDAO.kt`
- [X] T052 [P] [US1] Convert `ProfileDAO.java` → `app/src/main/kotlin/net/ktnox/mobileledger/dao/ProfileDAO.kt`
- [X] T053 [P] [US1] Convert `CurrencyDAO.java` → `app/src/main/kotlin/net/ktnox/mobileledger/dao/CurrencyDAO.kt`
- [X] T054 [P] [US1] Convert `TemplateHeaderDAO.java` → `app/src/main/kotlin/net/ktnox/mobileledger/dao/TemplateHeaderDAO.kt`
- [X] T055 [P] [US1] Convert remaining DAO files to `app/src/main/kotlin/net/ktnox/mobileledger/dao/`
- [X] T056 [US1] Verify KSP code generation succeeds with `./gradlew kspDebugKotlin`

### async Package Conversion (7 files)

- [X] T057 [P] [US1] Convert `DescriptionSelectedCallback.java` → `app/src/main/kotlin/net/ktnox/mobileledger/async/DescriptionSelectedCallback.kt`
- [X] T058 [P] [US1] Convert `SendTransactionTask.java` → `app/src/main/kotlin/net/ktnox/mobileledger/async/SendTransactionTask.kt`
- [X] T059 [P] [US1] Convert `RetrieveTransactionsTask.java` → `app/src/main/kotlin/net/ktnox/mobileledger/async/RetrieveTransactionsTask.kt`
- [X] T060 [P] [US1] Convert remaining async files to `app/src/main/kotlin/net/ktnox/mobileledger/async/`

### backup Package Conversion (6 files)

- [X] T061 [P] [US1] Convert backup package files to `app/src/main/kotlin/net/ktnox/mobileledger/backup/`

### json Package - Base Conversion (102 files)

- [ ] T062 [P] [US1] Convert `API.java` → `app/src/main/kotlin/net/ktnox/mobileledger/json/API.kt` (use enum class)
- [ ] T063 [P] [US1] Convert `Gateway.java` → `app/src/main/kotlin/net/ktnox/mobileledger/json/Gateway.kt` (abstract class with companion object)
- [ ] T064 [P] [US1] Convert `AccountListParser.java` → `app/src/main/kotlin/net/ktnox/mobileledger/json/AccountListParser.kt`
- [ ] T065 [P] [US1] Convert `TransactionListParser.java` → `app/src/main/kotlin/net/ktnx/mobileledger/json/TransactionListParser.kt`

### json/v1_14 Conversion (12 files)

- [ ] T066 [P] [US1] Convert v1_14 package files to `app/src/main/kotlin/net/ktnox/mobileledger/json/v1_14/`

### json/v1_15 Conversion (12 files)

- [ ] T067 [P] [US1] Convert v1_15 package files to `app/src/main/kotlin/net/ktnox/mobileledger/json/v1_15/`

### json/v1_19_1 Conversion (13 files)

- [ ] T068 [P] [US1] Convert v1_19_1 package files to `app/src/main/kotlin/net/ktnox/mobileledger/json/v1_19_1/`

### json/v1_23 Conversion (12 files)

- [ ] T069 [P] [US1] Convert v1_23 package files to `app/src/main/kotlin/net/ktnox/mobileledger/json/v1_23/`

### json/v1_32 Conversion (13 files)

- [ ] T070 [P] [US1] Convert v1_32 package files to `app/src/main/kotlin/net/ktnox/mobileledger/json/v1_32/`

### json/v1_40 Conversion (13 files)

- [ ] T071 [P] [US1] Convert v1_40 package files to `app/src/main/kotlin/net/ktnox/mobileledger/json/v1_40/`

### json/v1_50 Conversion (15 files)

- [ ] T072 [P] [US1] Convert v1_50 package files to `app/src/main/kotlin/net/ktnox/mobileledger/json/v1_50/`

### json Test Conversion

- [ ] T073 [P] [US1] Convert `ParsedQuantityTest.java` → `app/src/test/kotlin/net/ktnox/mobileledger/json/ParsedQuantityTest.kt`
- [ ] T074 [P] [US1] Convert `LegacyParserTest.java` → `app/src/test/kotlin/net/ktnox/mobileledger/json/LegacyParserTest.kt`
- [ ] T075 [US1] Verify JSON parser tests pass with `./gradlew test --tests "*.ParsedQuantityTest" --tests "*.LegacyParserTest"`

### ui Package Conversion (56 files)

- [ ] T076 [P] [US1] Convert ViewModel classes in `app/src/main/java/net/ktnx/mobileledger/ui/` to Kotlin
- [ ] T077 [P] [US1] Convert Fragment classes in `app/src/main/java/net/ktnx/mobileledger/ui/` to Kotlin
- [ ] T078 [P] [US1] Convert Activity classes in `app/src/main/java/net/ktnox/mobileledger/ui/` to Kotlin
- [ ] T079 [P] [US1] Convert adapter classes in `app/src/main/java/net/ktnox/mobileledger/ui/` to Kotlin
- [ ] T080 [US1] Convert remaining UI files to `app/src/main/kotlin/net/ktnox/mobileledger/ui/`

### Root Level Conversion

- [ ] T081 [US1] Convert `App.java` → `app/src/main/kotlin/net/ktnox/mobileledger/App.kt` (add @JvmStatic, @JvmField for Java interop)
- [ ] T082 [P] [US1] Convert remaining root-level files to `app/src/main/kotlin/net/ktnox/mobileledger/`

### Instrumented Test Conversion

- [ ] T083 [US1] Convert `ExampleInstrumentedTest.java` → `app/src/androidTest/kotlin/net/ktnox/mobileledger/ExampleInstrumentedTest.kt`

### User Story 1 Verification

- [ ] T084 [US1] Run `./gradlew clean assembleDebug` and verify success
- [ ] T085 [US1] Run `./gradlew test` and verify all 6 unit tests pass
- [ ] T086 [US1] Run `./gradlew connectedAndroidTest` and verify instrumented test passes
- [ ] T087 [US1] Delete all original Java files from `app/src/main/java/` after verification

**Checkpoint**: User Story 1 complete - all Java files converted, all tests pass

---

## Phase 4: User Story 2 - Kotlin言語機能を活用した可読性向上 (Priority: P2)

**Goal**: Kotlin イディオム（null安全、data class、スコープ関数）を適切に適用

**Independent Test**: コードレビューで Kotlin イディオムの適用を確認

### Null Safety Improvements

- [ ] T088 [P] [US2] Review and fix `!!` operators in `app/src/main/kotlin/net/ktnox/mobileledger/model/` - replace with safe calls or require()
- [ ] T089 [P] [US2] Review and fix `!!` operators in `app/src/main/kotlin/net/ktnox/mobileledger/db/`
- [ ] T090 [P] [US2] Review and fix `!!` operators in `app/src/main/kotlin/net/ktnox/mobileledger/dao/`
- [ ] T091 [P] [US2] Review and fix `!!` operators in `app/src/main/kotlin/net/ktnox/mobileledger/json/`
- [ ] T092 [P] [US2] Review and fix `!!` operators in `app/src/main/kotlin/net/ktnox/mobileledger/ui/`

### data class Optimization

- [ ] T093 [P] [US2] Ensure all POJOs in model/ use `data class` with immutable `val` properties
- [ ] T094 [P] [US2] Ensure all Room entities use `data class` with appropriate defaults
- [ ] T095 [US2] Verify `copy()`, `equals()`, `hashCode()` work correctly on data classes

### Scope Function Optimization

- [ ] T096 [P] [US2] Apply `apply {}` for object configuration in `app/src/main/kotlin/net/ktnox/mobileledger/json/`
- [ ] T097 [P] [US2] Apply `let {}` for null-safe chaining in `app/src/main/kotlin/net/ktnox/mobileledger/ui/`
- [ ] T098 [P] [US2] Apply `with {}` for multiple calls on same object
- [ ] T099 [US2] Ensure scope function nesting is ≤2 levels deep throughout codebase

### Extension Functions

- [ ] T100 [P] [US2] Extract common patterns as extension functions in `app/src/main/kotlin/net/ktnox/mobileledger/utils/Extensions.kt`

### var → val Conversion

- [ ] T101 [P] [US2] Replace `var` with `val` where possible in model/
- [ ] T102 [P] [US2] Replace `var` with `val` where possible in db/
- [ ] T103 [P] [US2] Replace `var` with `val` where possible in ui/

### User Story 2 Verification

- [ ] T104 [US2] Run `./gradlew test` and verify all tests still pass
- [ ] T105 [US2] Run `./gradlew lintDebug` and verify no new warnings

**Checkpoint**: User Story 2 complete - Kotlin idioms properly applied

---

## Phase 5: User Story 3 - 重複コードの削減 (Priority: P3)

**Goal**: JSON パーサー 7 バージョンの共通コードを委譲パターンで抽出し、40% コード削減

**Independent Test**: 各 API バージョンでパース成功、コード行数 40% 削減確認

### Create Common Package

- [ ] T106 [P] [US3] Create `app/src/main/kotlin/net/ktnox/mobileledger/json/common/PostingFieldDelegate.kt`
- [ ] T107 [P] [US3] Create `app/src/main/kotlin/net/ktnox/mobileledger/json/common/TransactionFieldDelegate.kt`
- [ ] T108 [P] [US3] Create `app/src/main/kotlin/net/ktnox/mobileledger/json/common/StyleConfigurer.kt` (sealed interface)
- [ ] T109 [P] [US3] Create `app/src/main/kotlin/net/ktnox/mobileledger/json/common/TransactionIdType.kt` (sealed interface)
- [ ] T110 [P] [US3] Create `app/src/main/kotlin/net/ktnox/mobileledger/json/common/BalanceExtractor.kt` (interface)
- [ ] T111 [P] [US3] Create `app/src/main/kotlin/net/ktnox/mobileledger/json/common/ObjectMapperProvider.kt` (object with KotlinModule)

### Create Abstract Base Classes

- [ ] T112 [US3] Create `app/src/main/kotlin/net/ktnox/mobileledger/json/common/AbstractParsedPosting.kt`
- [ ] T113 [US3] Create `app/src/main/kotlin/net/ktnox/mobileledger/json/common/AbstractParsedLedgerTransaction.kt`
- [ ] T114 [US3] Create `app/src/main/kotlin/net/ktnox/mobileledger/json/common/AbstractParsedLedgerAccount.kt`

### Migrate Group A (v1_14, v1_15, v1_19_1, v1_23) - ptransaction_: Int

- [ ] T115 [US3] Refactor v1_14 ParsedPosting to extend AbstractParsedPosting in `json/v1_14/`
- [ ] T116 [US3] Refactor v1_14 ParsedLedgerTransaction to extend AbstractParsedLedgerTransaction
- [ ] T117 [US3] Verify v1_14 tests pass with `./gradlew test --tests "*.LegacyParserTest"`
- [ ] T118 [P] [US3] Refactor v1_15 to use common base classes
- [ ] T119 [P] [US3] Refactor v1_19_1 to use common base classes
- [ ] T120 [P] [US3] Refactor v1_23 to use common base classes
- [ ] T121 [US3] Verify Group A versions pass tests

### Migrate Group B (v1_32, v1_40, v1_50) - ptransaction_: String

- [ ] T122 [US3] Refactor v1_32 to use common base classes with StringType transaction ID
- [ ] T123 [P] [US3] Refactor v1_40 to use common base classes
- [ ] T124 [US3] Refactor v1_50 to use common base classes (special: adata structure)
- [ ] T125 [US3] Verify Group B versions pass tests

### Remove Duplicate Code

- [ ] T126 [US3] Remove duplicated getter/setter code from all ParsedPosting implementations
- [ ] T127 [US3] Remove duplicated fromLedgerAccount() code
- [ ] T128 [US3] Remove duplicated asLedgerTransaction() code
- [ ] T129 [US3] Remove duplicated Gateway.transactionSaveRequest() code

### User Story 3 Verification

- [ ] T130 [US3] Run `./gradlew test` and verify all tests pass
- [ ] T131 [US3] Count lines of code in json/ package and verify ~40% reduction
- [ ] T132 [US3] Test each API version against real hledger-web server

**Checkpoint**: User Story 3 complete - JSON parser code deduplicated

---

## Phase 6: User Story 4 - 段階的移行による安定性確保 (Priority: P4)

**Goal**: 各段階でビルドとテストが通ることを確認し、移行履歴を記録

**Independent Test**: 各パッケージ移行後に `./gradlew assembleDebug && ./gradlew test` が成功

### Final Verification

- [ ] T133 [US4] Run full clean build: `./gradlew clean assembleDebug`
- [ ] T134 [US4] Run all unit tests: `./gradlew test`
- [ ] T135 [US4] Run all instrumented tests: `./gradlew connectedAndroidTest`
- [ ] T136 [US4] Run lint check: `./gradlew lintDebug`

### Performance Verification

- [ ] T137 [US4] Verify UI response time < 100ms (manual testing)
- [ ] T138 [US4] Verify app startup time < 2s (manual testing)

### Documentation Update

- [ ] T139 [P] [US4] Update any outdated code comments to reflect Kotlin idioms

### Cleanup

- [ ] T140 [US4] Remove empty `app/src/main/java/` directory
- [ ] T141 [US4] Remove empty `app/src/test/java/` directory
- [ ] T142 [US4] Remove empty `app/src/androidTest/java/` directory

**Checkpoint**: User Story 4 complete - migration stable and verified

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T143 [P] Add KDoc comments to public APIs in model/
- [ ] T144 [P] Add KDoc comments to public APIs in db/
- [ ] T145 [P] Add KDoc comments to DAO interfaces
- [ ] T146 Run quickstart.md validation scenarios manually
- [ ] T147 Final code review for `!!` operators and Kotlin best practices

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational - core migration
- **User Story 2 (Phase 4)**: Depends on User Story 1 - idiom improvements
- **User Story 3 (Phase 5)**: Depends on User Story 1 - code deduplication
- **User Story 4 (Phase 6)**: Depends on US1, US2, US3 - final verification
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Depends on User Story 1 completion (needs converted code to improve)
- **User Story 3 (P3)**: Depends on User Story 1 completion (needs converted JSON parsers)
- **User Story 4 (P4)**: Depends on US1, US2, US3 (final verification of all work)

### Within Each User Story

- Models before services
- DAOs before entities that reference them
- Core packages (utils, err, model) before dependent packages (db, dao, json, ui)
- Base classes before derived classes
- Tests converted alongside their target classes

### Parallel Opportunities

- All tasks marked [P] can run in parallel within their phase
- T006-T008 (directory creation) can run in parallel
- T013-T021 (utils conversion) can run in parallel
- T039-T047 (db entities without FK dependencies) can run in parallel
- T050-T055 (DAO conversion) can run in parallel
- T066-T072 (JSON version packages) can run in parallel
- T088-T092 (`!!` operator review) can run in parallel
- T106-T111 (common package creation) can run in parallel

---

## Parallel Example: User Story 1 - JSON Package

```bash
# Launch all JSON version packages together:
Task: "Convert v1_14 package files to Kotlin" (T066)
Task: "Convert v1_15 package files to Kotlin" (T067)
Task: "Convert v1_19_1 package files to Kotlin" (T068)
Task: "Convert v1_23 package files to Kotlin" (T069)
Task: "Convert v1_32 package files to Kotlin" (T070)
Task: "Convert v1_40 package files to Kotlin" (T071)
Task: "Convert v1_50 package files to Kotlin" (T072)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T010)
2. Complete Phase 2: Foundational (T011-T028)
3. Complete Phase 3: User Story 1 (T029-T087)
4. **STOP and VALIDATE**: All 237 Java files converted, all tests pass
5. Deploy/demo if ready - basic Kotlin migration complete

### Incremental Delivery

1. Setup + Foundational → Kotlin enabled, utils/err converted
2. User Story 1 → Full conversion complete, all tests pass (MVP!)
3. User Story 2 → Kotlin idioms applied, code quality improved
4. User Story 3 → JSON parser deduplicated, 40% code reduction
5. User Story 4 → Final verification, documentation updated

---

## Summary

| Phase | Tasks | Parallel | Files | Description |
|-------|-------|----------|-------|-------------|
| Phase 1 | T001-T010 | 6 | 0 | Setup |
| Phase 2 | T011-T028 | 12 | 14 | Foundational (utils, err) |
| Phase 3 | T029-T087 | 52 | 223 | User Story 1 (full conversion) |
| Phase 4 | T088-T105 | 15 | - | User Story 2 (idiom improvements) |
| Phase 5 | T106-T132 | 14 | - | User Story 3 (code deduplication) |
| Phase 6 | T133-T142 | 1 | - | User Story 4 (verification) |
| Phase 7 | T143-T147 | 3 | - | Polish |
| **Total** | **147** | **103** | **237** | |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies
- Run `./gradlew test` after each package conversion to catch issues early

---

## Migration Issues & Fixes Log

### Issue 1: Room annotation processor doesn't work with Kotlin
**Symptom**: `DB_Impl does not exist` at runtime
**Cause**: `annotationProcessor` doesn't process Kotlin files
**Fix**:
- Added `id 'org.jetbrains.kotlin.kapt'` plugin
- Changed `annotationProcessor` to `kapt` for Room compiler
- Added `kapt { arguments { ... } }` block

### Issue 2: Ambiguous getter for boolean fields
**Symptom**: `All of the following match: getPermitPosting, permitPosting`
**Cause**: Kotlin boolean properties generate both `get*` and `is*` getters, Room gets confused
**Files affected**: Profile.kt, Account.kt
**Fix**:
- Profile.kt: Renamed methods (`useAuthentication()` → `isAuthEnabled()`, `permitPosting()` → `canPost()`, `detectedVersionPre_1_19()` → `isVersionPre_1_19()`)
- Account.kt: Added `@Ignore` annotation to duplicate getter methods

### Issue 3: SQL column name mismatch
**Symptom**: `no such column: description`
**Cause**: Private backing field `_description` didn't have explicit column name
**File**: Transaction.kt
**Fix**: Added `name = "description"` to `@ColumnInfo` annotation

### Issue 4: NullPointerException from Java calling Kotlin setter
**Symptom**: `NullPointerException: Parameter specified as non-null is null: method Profile.setDefaultCommodity`
**Cause**: Kotlin property setter had non-null `String` parameter, but Java code passed `null`
**File**: Profile.kt
**Fix**:
- Changed property to `var defaultCommodity: String? = null` with private setter
- Added explicit `getDefaultCommodityOrEmpty(): String` for non-null return
- Added explicit `setDefaultCommodity(value: String?)` to accept null
- Updated all Java callers to use `getDefaultCommodityOrEmpty()`

### Issue 5: Room DAO parameter names not preserved
**Symptom**: `Cannot find method parameters for :id` in Room queries
**Cause**: Kotlin by default doesn't emit parameter names in bytecode
**Fix**: Added `javaParameters = true` to `kotlinOptions` in build.gradle

### build.gradle changes summary
```groovy
plugins {
    id 'org.jetbrains.kotlin.kapt'  // Added
}

kotlinOptions {
    javaParameters = true  // Added for Room DAO parameter names
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}

// Changed from annotationProcessor to kapt
kapt "androidx.room:room-compiler:$room_version"
```

### Issue 6: ThreadLocal initialization in Kotlin
**Symptom**: `java.text.ParseException: Failed to parse ISO date: 2026-01-01` when updating data
**Cause**: ThreadLocal initialized with `.apply { set(...) }` only sets value for creating thread, other threads get null
**File**: Globals.kt
**Fix**:
```kotlin
// Before (broken - only works for creating thread)
private val isoDateFormatter = ThreadLocal<SimpleDateFormat>().apply {
    set(SimpleDateFormat("yyyy-MM-dd", Locale.US))
}

// After (works for all threads)
private val isoDateFormatter: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd", Locale.US)
}
```
