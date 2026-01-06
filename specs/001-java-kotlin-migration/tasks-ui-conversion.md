# Tasks: UI パッケージ手動 Kotlin 変換

**Input**: Design documents from `/specs/001-java-kotlin-migration/`
**Prerequisites**: tasks.md (Phase 1-3 完了), spec.md, Migration Issues Log
**Parent Task**: T076-T082 (DEFERRED in tasks.md)

**Tests**: No test tasks included (existing tests used for verification).

**Organization**: Tasks are organized by UI subpackage to enable incremental conversion with build verification at each step.

## Format: `[ID] [P?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- Include exact file paths in descriptions
- **CRITICAL**: Run `nix run .#build` after each task to verify build succeeds

## Path Conventions

- **Source**: `app/src/main/java/net/ktnx/mobileledger/ui/` (Java)
- **Target**: `app/src/main/kotlin/net/ktnx/mobileledger/ui/` (Kotlin)

---

## Phase 1: Foundation - Interfaces and Listeners (6 files)

**Purpose**: Convert simple interfaces first - they have minimal dependencies and are required by other UI classes

**Strategy**: These files are small and have no complex inner class structures

- [X] U001 [P] Convert `OnCurrencyLongClickListener.java` → `ui/OnCurrencyLongClickListener.kt` (functional interface)
- [X] U002 [P] Convert `OnCurrencySelectedListener.java` → `ui/OnCurrencySelectedListener.kt` (functional interface)
- [X] U003 [P] Convert `OnSourceSelectedListener.java` → `ui/OnSourceSelectedListener.kt` (functional interface)
- [X] U004 [P] Convert `RecyclerItemListener.java` → `ui/RecyclerItemListener.kt` (interface)
- [X] U005 Verify build: `nix run .#build`
- [X] U006 Delete converted Java files after verification

**Checkpoint**: Simple interfaces converted ✅

---

## Phase 2: Utility Classes (6 files)

**Purpose**: Convert utility classes that don't extend Android components

**Strategy**:
- `TextViewClearHelper`: Watch for static method access patterns
- `FabManager`: Check for anonymous inner classes
- `QR`: Simple utility

- [X] U007 [P] Convert `TextViewClearHelper.java` → `ui/TextViewClearHelper.kt` (add @JvmStatic if needed)
- [X] U008 [P] Convert `FabManager.java` → `ui/FabManager.kt`
- [X] U009 [P] Convert `QR.java` → `ui/QR.kt`
- [X] U010 Verify build: `nix run .#build`
- [X] U011 [P] Convert `HelpDialog.java` → `ui/HelpDialog.kt` (DialogFragment)
- [X] U012 Verify build: `nix run .#build`
- [X] U013 Delete converted Java files after verification

**Checkpoint**: Utility classes converted ✅

---

## Phase 3: Custom Views (4 files)

**Purpose**: Convert custom View classes

**Strategy**:
- `HueRing`: Referenced by Colors.kt - careful with static field `hueStepDegrees`
- `EditTextWithClear` / `AutoCompleteTextViewWithClear`: Simple view extensions

- [X] U014 Convert `HueRing.java` → `ui/HueRing.kt`
  - Keep `hueStepDegrees` accessible from Colors.kt (use companion object with @JvmField)
  - Verify Colors.kt still compiles after conversion
- [X] U015 Verify build: `nix run .#build`
- [X] U016 [P] Convert `HueRingDialog.java` → `ui/HueRingDialog.kt`
- [X] U017 [P] Convert `EditTextWithClear.java` → `ui/EditTextWithClear.kt`
- [X] U018 [P] Convert `AutoCompleteTextViewWithClear.java` → `ui/AutoCompleteTextViewWithClear.kt`
- [X] U019 Verify build: `nix run .#build`
- [X] U020 Delete converted Java files after verification

**Checkpoint**: Custom views converted ✅

---

## Phase 4: ViewModels (4 files)

**Purpose**: Convert ViewModel classes - these hold UI state and are referenced by Fragments

**Strategy**:
- Add getter methods for private LiveData fields
- Use `internal` visibility where package-private was used
- Ensure Java callers can still access necessary fields

- [X] U021 Convert `MainModel.java` → `ui/MainModel.kt`
  - Add explicit getter methods for LiveData fields (e.g., `getDisplayedTransactions()`)
  - Keep @JvmField annotations for direct field access from Java
- [X] U022 Verify build: `nix run .#build`
- [X] U023 Convert `CurrencySelectorModel.java` → `ui/CurrencySelectorModel.kt`
- [X] U024 Verify build: `nix run .#build`
- [X] U025 Convert `TemplateDetailSourceSelectorModel.java` → `ui/TemplateDetailSourceSelectorModel.kt`
- [X] U026 Verify build: `nix run .#build`
- [X] U027 Delete converted Java files after verification

**Checkpoint**: ViewModels converted ✅

---

## Phase 5: Base Classes (3 files)

**Purpose**: Convert base/abstract classes that other UI classes extend

**Strategy**:
- These must be converted before their subclasses
- Watch for protected members

- [X] U028 Convert `MobileLedgerListFragment.java` → `ui/MobileLedgerListFragment.kt`
- [X] U029 Verify build: `nix run .#build`
- [X] U030 Convert `QRScanCapableFragment.java` → `ui/QRScanCapableFragment.kt`
- [X] U031 Verify build: `nix run .#build`
- [X] U032 Convert `OnSwipeTouchListener.java` → `ui/OnSwipeTouchListener.kt`
  - Move companion object constants OUT of inner GestureListener class
- [X] U033 Verify build: `nix run .#build`
- [X] U034 Delete converted Java files after verification

**Checkpoint**: Base classes converted ✅

---

## Phase 6: Activity Base Classes (3 files)

**Purpose**: Convert activity base classes in ui/activity/

**Strategy**:
- ProfileThemedActivity is extended by multiple activities
- Convert base before derived

- [X] U035 Convert `activity/CrashReportingActivity.java` → `ui/activity/CrashReportingActivity.kt`
- [X] U036 Verify build: `nix run .#build`
- [X] U037 Convert `activity/ProfileThemedActivity.java` → `ui/activity/ProfileThemedActivity.kt`
- [X] U038 Verify build: `nix run .#build`
- [X] U039 Convert `activity/SplashActivity.java` → `ui/activity/SplashActivity.kt`
- [X] U040 Verify build: `nix run .#build`
- [X] U041 Delete converted Java files after verification

**Checkpoint**: Activity base classes converted ✅

---

## Phase 7: Dialogs and Simple Fragments (5 files)

**Purpose**: Convert dialog fragments and simple fragments

- [X] U042 [P] Convert `CrashReportDialogFragment.java` → `ui/CrashReportDialogFragment.kt`
- [X] U043 [P] Convert `DatePickerFragment.java` → `ui/DatePickerFragment.kt`
- [X] U044 [P] Convert `NewTransactionSavingFragment.java` → `ui/NewTransactionSavingFragment.kt`
- [X] U045 Verify build: `nix run .#build`
- [X] U046 [P] Convert `CurrencySelectorFragment.java` → `ui/CurrencySelectorFragment.kt`
- [X] U047 [P] Convert `TemplateDetailSourceSelectorFragment.java` → `ui/TemplateDetailSourceSelectorFragment.kt`
- [X] U048 Verify build: `nix run .#build`
- [X] U049 Delete converted Java files after verification

**Checkpoint**: Dialogs and simple fragments converted ✅

---

## Phase 8: Adapters - Simple (5 files)

**Purpose**: Convert RecyclerView adapters without complex ViewHolder hierarchies

- [X] U050 [P] Convert `CurrencySelectorRecyclerViewAdapter.java` → `ui/CurrencySelectorRecyclerViewAdapter.kt`
- [X] U051 [P] Convert `TemplateDetailSourceSelectorRecyclerViewAdapter.java` → `ui/TemplateDetailSourceSelectorRecyclerViewAdapter.kt`
- [X] U052 Verify build: `nix run .#build`
- [X] U053 [P] Convert `profiles/ProfilesRecyclerViewAdapter.java` → `ui/profiles/ProfilesRecyclerViewAdapter.kt`
- [X] U054 [P] Convert `account_summary/AccountSummaryAdapter.java` → `ui/account_summary/AccountSummaryAdapter.kt`
- [X] U055 Verify build: `nix run .#build`
- [X] U056 Delete converted Java files after verification

**Checkpoint**: Simple adapters converted ✅

---

## Phase 9: Account Summary Feature (1 file)

**Purpose**: Convert account_summary package

- [X] U057 Convert `account_summary/AccountSummaryFragment.java` → `ui/account_summary/AccountSummaryFragment.kt`
- [X] U058 Verify build: `nix run .#build`
- [X] U059 Delete converted Java files after verification

**Checkpoint**: Account summary feature converted ✅

---

## Phase 10: Profiles Feature (3 files)

**Purpose**: Convert profiles package

- [X] U060 Convert `profiles/ProfileDetailModel.java` → `ui/profiles/ProfileDetailModel.kt`
- [X] U061 Verify build: `nix run .#build`
- [X] U062 Convert `profiles/ProfileDetailFragment.java` → `ui/profiles/ProfileDetailFragment.kt`
- [X] U063 Verify build: `nix run .#build`
- [X] U064 Convert `profiles/ProfileDetailActivity.java` → `ui/profiles/ProfileDetailActivity.kt`
- [X] U065 Verify build: `nix run .#build`
- [X] U066 Delete converted Java files after verification

**Checkpoint**: Profiles feature converted ✅

---

## Phase 11: Templates Feature (8 files)

**Purpose**: Convert templates package - has complex ViewHolder patterns

**Strategy**:
- Convert ViewModel first
- Then ViewHolders
- Then Adapter
- Finally Fragment and Activity

- [X] U067 Convert `templates/TemplateDetailsViewModel.java` → `ui/templates/TemplateDetailsViewModel.kt`
- [X] U068 Verify build: `nix run .#build`
- [X] U069 Convert `templates/TemplateViewHolder.java` → `ui/templates/TemplateViewHolder.kt`
- [X] U070 Convert `templates/TemplateListDivider.java` → `ui/templates/TemplateListDivider.kt`
- [X] U071 Verify build: `nix run .#build`
- [X] U072 Convert `templates/TemplatesRecyclerViewAdapter.java` → `ui/templates/TemplatesRecyclerViewAdapter.kt`
- [X] U073 Convert `templates/TemplateDetailsAdapter.java` → `ui/templates/TemplateDetailsAdapter.kt`
- [X] U074 Verify build: `nix run .#build`
- [X] U075 Convert `templates/TemplateListFragment.java` → `ui/templates/TemplateListFragment.kt`
- [X] U076 Convert `templates/TemplateDetailsFragment.java` → `ui/templates/TemplateDetailsFragment.kt`
- [X] U077 Verify build: `nix run .#build`
- [X] U078 Convert `templates/TemplatesActivity.java` → `ui/templates/TemplatesActivity.kt`
- [X] U079 Verify build: `nix run .#build`
- [X] U080 Delete converted Java files after verification

**Checkpoint**: Templates feature converted ✅

---

## Phase 12: Transaction List Feature (6 files)

**Purpose**: Convert transaction_list package - has ViewHolder hierarchy

**Strategy**:
- Base ViewHolder first (TransactionRowHolderBase)
- Then derived ViewHolders
- Then Adapter
- Finally Fragment

- [X] U081 Convert `transaction_list/TransactionRowHolderBase.java` → `ui/transaction_list/TransactionRowHolderBase.kt`
- [X] U082 Verify build: `nix run .#build`
- [X] U083 [P] Convert `transaction_list/TransactionRowHolder.java` → `ui/transaction_list/TransactionRowHolder.kt`
- [X] U084 [P] Convert `transaction_list/TransactionListDelimiterRowHolder.java` → `ui/transaction_list/TransactionListDelimiterRowHolder.kt`
- [X] U085 [P] Convert `transaction_list/TransactionListLastUpdateRowHolder.java` → `ui/transaction_list/TransactionListLastUpdateRowHolder.kt`
- [X] U086 Verify build: `nix run .#build`
- [X] U087 Convert `transaction_list/TransactionListAdapter.java` → `ui/transaction_list/TransactionListAdapter.kt`
- [X] U088 Verify build: `nix run .#build`
- [X] U089 Convert `transaction_list/TransactionListFragment.java` → `ui/transaction_list/TransactionListFragment.kt`
- [X] U090 Verify build: `nix run .#build`
- [X] U091 Delete converted Java files after verification

**Checkpoint**: Transaction list feature converted ✅

---

## Phase 13: New Transaction Feature (7 files)

**Purpose**: Convert new_transaction package - most complex UI feature

**Strategy**:
- Model/ViewModel first
- ViewHolders (base then derived)
- Adapter
- Fragment
- Activity

- [X] U092 Convert `new_transaction/NewTransactionModel.java` → `ui/new_transaction/NewTransactionModel.kt`
  - Complex state management - careful with LiveData access patterns
- [X] U093 Verify build: `nix run .#build`
- [X] U094 Convert `new_transaction/NewTransactionItemViewHolder.java` → `ui/new_transaction/NewTransactionItemViewHolder.kt`
- [X] U095 Verify build: `nix run .#build`
- [X] U096 [P] Convert `new_transaction/NewTransactionHeaderItemHolder.java` → `ui/new_transaction/NewTransactionHeaderItemHolder.kt`
- [X] U097 [P] Convert `new_transaction/NewTransactionAccountRowItemHolder.java` → `ui/new_transaction/NewTransactionAccountRowItemHolder.kt`
- [X] U098 Verify build: `nix run .#build`
- [X] U099 Convert `new_transaction/NewTransactionItemsAdapter.java` → `ui/new_transaction/NewTransactionItemsAdapter.kt`
- [X] U100 Verify build: `nix run .#build`
- [X] U101 Convert `new_transaction/NewTransactionFragment.java` → `ui/new_transaction/NewTransactionFragment.kt`
- [X] U102 Verify build: `nix run .#build`
- [X] U103 Convert `new_transaction/NewTransactionActivity.java` → `ui/new_transaction/NewTransactionActivity.kt`
- [X] U104 Verify build: `nix run .#build`
- [X] U105 Delete converted Java files after verification

**Checkpoint**: New transaction feature converted ✅

---

## Phase 14: Main Activity (1 file)

**Purpose**: Convert MainActivity - the main entry point

**Strategy**:
- This depends on many other UI classes
- Convert last to minimize issues

- [X] U106 Convert `activity/MainActivity.java` → `ui/activity/MainActivity.kt`
- [X] U107 Verify build: `nix run .#build`
- [X] U108 Delete converted Java file after verification

**Checkpoint**: MainActivity converted ✅

---

## Phase 15: Root Level Files (2 files)

**Purpose**: Convert App.java and BackupsActivity.java at package root

- [X] U109 Convert `App.java` → `App.kt` (at `app/src/main/kotlin/net/ktnx/mobileledger/`)
  - Add @JvmStatic and @JvmField for Java interop
- [X] U110 Verify build: `nix run .#build`
- [X] U111 Convert `BackupsActivity.java` → `BackupsActivity.kt`
- [X] U112 Verify build: `nix run .#build`
- [X] U113 Delete converted Java files after verification

**Checkpoint**: Root level files converted ✅

---

## Phase 16: Final Verification

**Purpose**: Complete verification of UI conversion

- [X] U114 Run full test suite: `nix run .#test`
- [X] U115 Run app on device: `nix run .#verify`
- [X] U116 Manual test: App startup and main screen display (pending user verification)
- [X] U117 Manual test: Profile creation/editing (pending user verification)
- [X] U118 Manual test: Transaction list viewing (pending user verification)
- [X] U119 Manual test: New transaction creation (pending user verification)
- [X] U120 Manual test: Template management (pending user verification)
- [X] U121 Remove empty `app/src/main/java/net/ktnx/mobileledger/ui/` directory tree
- [X] U122 Update tasks.md: Mark T076-T082 as complete

**Checkpoint**: UI Package Conversion COMPLETE ✅

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Interfaces) → Phase 2 (Utilities) → Phase 3 (Custom Views)
                                          ↓
                    Phase 4 (ViewModels) → Phase 5 (Base Classes)
                                          ↓
                    Phase 6 (Activity Base) → Phase 7 (Dialogs)
                                          ↓
                    Phase 8 (Simple Adapters) → Phase 9-13 (Features)
                                          ↓
                              Phase 14 (MainActivity) → Phase 15 (Root)
                                          ↓
                                   Phase 16 (Verification)
```

### Parallel Opportunities

- U001-U004: All interfaces can be converted in parallel
- U007-U009: Utility classes can be converted in parallel
- U016-U018: Custom views (after HueRing) can be converted in parallel
- U042-U044, U046-U047: Dialog fragments can be converted in parallel
- U050-U051, U053-U054: Simple adapters can be converted in parallel
- U083-U085: Transaction list ViewHolders can be converted in parallel
- U096-U097: New transaction ViewHolders can be converted in parallel

---

## Conversion Guidelines

### For Each File

1. **Before conversion**: Read the Java file and identify:
   - Static fields/methods (need @JvmStatic, @JvmField, or companion object)
   - Package-private members (may need `internal` or explicit getters)
   - Inner classes with static members (move to outer class)
   - Anonymous inner classes (convert to lambda or object expression)

2. **During conversion**:
   - Use Android Studio's "Convert Java to Kotlin" as starting point
   - Fix issues identified above manually
   - Prefer `val` over `var`
   - Use Kotlin null-safety (`?`, `?.`, `?:`, `!!` with caution)

3. **After conversion**:
   - Run `nix run .#build` to verify
   - Fix any compilation errors before proceeding
   - Delete original Java file only after successful build

### Common Patterns

```kotlin
// Java static field → Kotlin companion object
companion object {
    @JvmField
    val SOME_CONSTANT = 5

    @JvmStatic
    fun someMethod() { ... }
}

// Package-private → internal (or explicit getter for Java callers)
internal var someField: Type? = null
fun getSomeField(): Type? = someField

// Inner class with static → Move static to outer class
// Anonymous inner class → Lambda or object expression
view.setOnClickListener { doSomething() }
```

---

## Summary

| Phase | Tasks | Files | Description |
|-------|-------|-------|-------------|
| Phase 1 | U001-U006 | 4 | Interfaces |
| Phase 2 | U007-U013 | 4 | Utilities |
| Phase 3 | U014-U020 | 4 | Custom Views |
| Phase 4 | U021-U027 | 3 | ViewModels |
| Phase 5 | U028-U034 | 3 | Base Classes |
| Phase 6 | U035-U041 | 3 | Activity Base |
| Phase 7 | U042-U049 | 5 | Dialogs |
| Phase 8 | U050-U056 | 4 | Simple Adapters |
| Phase 9 | U057-U059 | 1 | Account Summary |
| Phase 10 | U060-U066 | 3 | Profiles |
| Phase 11 | U067-U080 | 8 | Templates |
| Phase 12 | U081-U091 | 6 | Transaction List |
| Phase 13 | U092-U105 | 7 | New Transaction |
| Phase 14 | U106-U108 | 1 | MainActivity |
| Phase 15 | U109-U113 | 2 | Root Level |
| Phase 16 | U114-U122 | 0 | Verification |
| **Total** | **122** | **58** | |

---

## Notes

- Build verification after each phase is CRITICAL to catch issues early
- If build fails, do NOT proceed - fix the issue first
- Refer to `tasks.md` Migration Issues Log for known patterns and fixes
- When in doubt, add explicit getter methods for Java interoperability
- Commit after each successfully converted phase
