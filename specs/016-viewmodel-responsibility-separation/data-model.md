# Data Model: ViewModel 責務分離

**Feature**: 016-viewmodel-responsibility-separation
**Date**: 2026-01-16

## 概要

本機能は内部リファクタリングであり、データベーススキーマや永続化データモデルへの変更はありません。このドキュメントでは、分離後の ViewModel エンティティと UiState 定義を記述します。

## ViewModel エンティティ定義

### P1: MainViewModel 分離

#### ProfileSelectionViewModel（既存 → 拡充）

**責務**: プロファイル選択、プロファイル一覧管理、プロファイル並べ替え

```kotlin
@HiltViewModel
class ProfileSelectionViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val appStateService: AppStateService
) : ViewModel()

data class ProfileSelectionUiState(
    val profiles: List<ProfileListItem> = emptyList(),
    val currentProfileId: Long? = null,
    val isReordering: Boolean = false
)

sealed class ProfileSelectionEvent {
    data class SelectProfile(val profileId: Long) : ProfileSelectionEvent()
    data class ReorderProfiles(val orderedProfiles: List<ProfileListItem>) : ProfileSelectionEvent()
    data object StartReorder : ProfileSelectionEvent()
    data object EndReorder : ProfileSelectionEvent()
}

sealed class ProfileSelectionEffect {
    data class ProfileSelected(val profileId: Long) : ProfileSelectionEffect()
}
```

**依存関係**:
- `ProfileRepository`: プロファイル CRUD、currentProfile StateFlow
- `AppStateService`: UI 状態管理

**推定行数**: 150-200行

---

#### AccountSummaryViewModel（既存 → 拡充）

**責務**: アカウント一覧表示、残高フィルタ、展開/折りたたみ状態

```kotlin
@HiltViewModel
class AccountSummaryViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val preferencesRepository: PreferencesRepository,
    private val currencyFormatter: CurrencyFormatter
) : ViewModel()

data class AccountSummaryUiState(
    val accounts: List<AccountSummaryListItem> = emptyList(),
    val showZeroBalances: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val headerText: String = ""
)

sealed class AccountSummaryEvent {
    data object ToggleZeroBalance : AccountSummaryEvent()
    data class ToggleAccountExpanded(val accountName: String) : AccountSummaryEvent()
    data class ToggleAmountsExpanded(val accountName: String) : AccountSummaryEvent()
    data object Refresh : AccountSummaryEvent()
    data object ClearError : AccountSummaryEvent()
}
```

**依存関係**:
- `ProfileRepository`: currentProfile の購読
- `AccountRepository`: アカウント取得
- `PreferencesRepository`: 残高フィルタ設定の永続化
- `CurrencyFormatter`: 金額フォーマット

**推定行数**: 250-300行

---

#### TransactionListViewModel（既存 → 拡充）

**責務**: 取引一覧表示、アカウントフィルタ、日付ナビゲーション、取引検索

```kotlin
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val currencyFormatter: CurrencyFormatter
) : ViewModel()

data class TransactionListUiState(
    val transactions: List<TransactionListItem> = emptyList(),
    val accountFilter: String = "",
    val accountSuggestions: List<String> = emptyList(),
    val showAccountSuggestions: Boolean = false,
    val dateNavigationState: DateNavigationState = DateNavigationState(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class DateNavigationState(
    val earliestDate: LocalDate? = null,
    val latestDate: LocalDate? = null,
    val currentMonthStart: LocalDate = LocalDate.now().withDayOfMonth(1)
)

sealed class TransactionListEvent {
    data class SetAccountFilter(val filter: String) : TransactionListEvent()
    data object ClearAccountFilter : TransactionListEvent()
    data class SelectSuggestion(val accountName: String) : TransactionListEvent()
    data class NavigateToMonth(val monthStart: LocalDate) : TransactionListEvent()
    data object NavigateToPreviousMonth : TransactionListEvent()
    data object NavigateToNextMonth : TransactionListEvent()
    data object Refresh : TransactionListEvent()
    data object ClearError : TransactionListEvent()
}
```

**依存関係**:
- `ProfileRepository`: currentProfile の購読
- `TransactionRepository`: 取引取得
- `AccountRepository`: アカウント名サジェスト
- `CurrencyFormatter`: 金額フォーマット

**推定行数**: 280-350行

---

#### MainCoordinatorViewModel（既存 → 拡充）

**責務**: タブ選択、ドロワー制御、同期オーケストレーション、ナビゲーション

```kotlin
@HiltViewModel
class MainCoordinatorViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val appStateService: AppStateService,
    private val transactionSyncer: TransactionSyncer
) : ViewModel()

data class MainCoordinatorUiState(
    val selectedTab: MainTab = MainTab.Accounts,
    val isDrawerOpen: Boolean = false,
    val syncState: SyncState = SyncState.Idle,
    val syncProgress: Float = 0f,
    val lastSyncInfo: LastSyncInfo? = null
)

data class LastSyncInfo(
    val date: Date?,
    val transactionCount: Int,
    val accountCount: Int
)

enum class MainTab { Accounts, Transactions }

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Error(val message: String) : SyncState()
    data object Completed : SyncState()
}

sealed class MainCoordinatorEvent {
    data class SelectTab(val tab: MainTab) : MainCoordinatorEvent()
    data object OpenDrawer : MainCoordinatorEvent()
    data object CloseDrawer : MainCoordinatorEvent()
    data object StartSync : MainCoordinatorEvent()
    data object CancelSync : MainCoordinatorEvent()
    data object ClearSyncError : MainCoordinatorEvent()
}

sealed class MainCoordinatorEffect {
    data object NavigateToNewTransaction : MainCoordinatorEffect()
    data class NavigateToProfileDetail(val profileId: Long) : MainCoordinatorEffect()
    data object NavigateToNewProfile : MainCoordinatorEffect()
    data object NavigateToTemplates : MainCoordinatorEffect()
    data object NavigateToBackups : MainCoordinatorEffect()
    data class ShowSnackbar(val message: String) : MainCoordinatorEffect()
}
```

**依存関係**:
- `ProfileRepository`: currentProfile の購読（同期対象の決定）
- `AppStateService`: ドロワー状態、同期情報
- `TransactionSyncer`: データ同期実行

**推定行数**: 250-300行

---

### P2: NewTransactionViewModel 分離

#### TransactionFormViewModel（新規）

**責務**: 取引フォーム管理（日付、説明、コメント）、フォーム検証、取引送信

```kotlin
@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionSender: TransactionSender
) : ViewModel()

data class TransactionFormUiState(
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val comment: String = "",
    val isSending: Boolean = false,
    val validationErrors: Map<FormField, String> = emptyMap(),
    val canSubmit: Boolean = false
)

enum class FormField { Date, Description }

sealed class TransactionFormEvent {
    data class SetDate(val date: LocalDate) : TransactionFormEvent()
    data class SetDescription(val description: String) : TransactionFormEvent()
    data class SetComment(val comment: String) : TransactionFormEvent()
    data class Submit(val accountRows: List<TransactionAccountRow>) : TransactionFormEvent()
}

sealed class TransactionFormEffect {
    data object TransactionSaved : TransactionFormEffect()
    data class ShowError(val message: String) : TransactionFormEffect()
    data object NavigateBack : TransactionFormEffect()
}
```

**依存関係**:
- `ProfileRepository`: currentProfile（送信先サーバー情報）
- `TransactionSender`: 取引送信

**推定行数**: 180-220行

---

#### AccountRowsViewModel（新規）

**責務**: 勘定科目行の追加/削除/移動、金額計算、通貨選択、フォーカス管理

```kotlin
@HiltViewModel
class AccountRowsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyFormatter: CurrencyFormatter
) : ViewModel()

data class AccountRowsUiState(
    val rows: List<TransactionAccountRow> = listOf(emptyRow(), emptyRow()),
    val focusedRowId: Int? = null,
    val focusedElement: FocusedElement? = null,
    val selectedCurrency: String? = null,
    val currencyPickerVisible: Boolean = false,
    val availableCurrencies: List<Currency> = emptyList(),
    val accountSuggestions: List<String> = emptyList(),
    val showAccountSuggestions: Boolean = false,
    val isBalanced: Boolean = false,
    val balanceDifference: BigDecimal = BigDecimal.ZERO
)

data class TransactionAccountRow(
    val id: Int,
    val accountName: String = "",
    val amount: String = "",
    val currency: String? = null,
    val comment: String = "",
    val isAmountHint: Boolean = false
)

enum class FocusedElement { AccountName, Amount, Comment }

sealed class AccountRowsEvent {
    data class SetAccountName(val rowId: Int, val name: String) : AccountRowsEvent()
    data class SetAmount(val rowId: Int, val amount: String) : AccountRowsEvent()
    data class SetRowComment(val rowId: Int, val comment: String) : AccountRowsEvent()
    data class SetRowCurrency(val rowId: Int, val currency: String?) : AccountRowsEvent()
    data class SetFocus(val rowId: Int, val element: FocusedElement) : AccountRowsEvent()
    data object AddRow : AccountRowsEvent()
    data class RemoveRow(val rowId: Int) : AccountRowsEvent()
    data class MoveRow(val fromIndex: Int, val toIndex: Int) : AccountRowsEvent()
    data object ShowCurrencyPicker : AccountRowsEvent()
    data object HideCurrencyPicker : AccountRowsEvent()
    data class SelectCurrency(val currency: String) : AccountRowsEvent()
    data class SelectAccountSuggestion(val rowId: Int, val name: String) : AccountRowsEvent()
}

sealed class AccountRowsEffect {
    data class RequestFocus(val rowId: Int, val element: FocusedElement) : AccountRowsEffect()
    data object HideKeyboard : AccountRowsEffect()
}

// ヘルパー関数
fun emptyRow(): TransactionAccountRow = TransactionAccountRow(
    id = nextRowId.getAndIncrement()
)

private val nextRowId = AtomicInteger(1)
```

**依存関係**:
- `ProfileRepository`: currentProfile（デフォルト通貨）
- `AccountRepository`: アカウント名サジェスト
- `CurrencyRepository`: 利用可能通貨一覧
- `CurrencyFormatter`: 金額フォーマット

**推定行数**: 280-350行

---

#### TemplateApplicatorViewModel（新規）

**責務**: テンプレート検索、テンプレートマッチング、テンプレート適用

```kotlin
@HiltViewModel
class TemplateApplicatorViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val templateRepository: TemplateRepository
) : ViewModel()

data class TemplateApplicatorUiState(
    val matchingTemplates: List<TemplateItem> = emptyList(),
    val selectedTemplateId: Long? = null,
    val isSearching: Boolean = false
)

data class TemplateItem(
    val id: Long,
    val name: String,
    val description: String,
    val accounts: List<TemplateAccountItem>
)

data class TemplateAccountItem(
    val accountName: String,
    val amount: BigDecimal?,
    val currency: String?
)

sealed class TemplateApplicatorEvent {
    data class SearchTemplates(val query: String) : TemplateApplicatorEvent()
    data class SelectTemplate(val templateId: Long) : TemplateApplicatorEvent()
    data object ClearSelection : TemplateApplicatorEvent()
    data object ApplySelectedTemplate : TemplateApplicatorEvent()
}

sealed class TemplateApplicatorEffect {
    data class ApplyTemplate(
        val description: String,
        val accounts: List<TemplateAccountItem>
    ) : TemplateApplicatorEffect()
}
```

**依存関係**:
- `ProfileRepository`: currentProfile（テンプレートのフィルタ）
- `TemplateRepository`: テンプレート検索

**推定行数**: 120-180行

---

### P3: ProfileDetailViewModel（既存 → StateFlow 移行）

**責務**: プロファイル作成/編集、フォーム検証、接続テスト、バージョン検出

```kotlin
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val versionDetector: VersionDetector,
    private val savedStateHandle: SavedStateHandle
) : ViewModel()

data class ProfileDetailUiState(
    val profileId: Long = NO_PROFILE_ID,
    val profileName: String = "",
    val url: String = "",
    val useAuthentication: Boolean = false,
    val authUserName: String = "",
    val authPassword: String = "",
    val apiVersion: ApiVersion = ApiVersion.Auto,
    val preferredAccountsFilter: String = "",
    val themeHue: Int = -1,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isTesting: Boolean = false,
    val validationErrors: Map<ProfileField, String> = emptyMap(),
    val connectionTestResult: ConnectionTestResult? = null,
    val detectedVersion: String? = null
) {
    val isNewProfile: Boolean get() = profileId == NO_PROFILE_ID
    val canDelete: Boolean get() = !isNewProfile
    val isFormValid: Boolean get() = validationErrors.isEmpty() &&
        profileName.isNotBlank() && url.isNotBlank()
    val showInsecureWarning: Boolean get() = url.startsWith("http://") && !useAuthentication
}

enum class ProfileField { Name, Url, AuthUserName, AuthPassword }

sealed class ConnectionTestResult {
    data class Success(val serverVersion: String) : ConnectionTestResult()
    data class Error(val message: String) : ConnectionTestResult()
}

sealed class ProfileDetailEvent {
    data class SetProfileName(val name: String) : ProfileDetailEvent()
    data class SetUrl(val url: String) : ProfileDetailEvent()
    data class SetUseAuthentication(val use: Boolean) : ProfileDetailEvent()
    data class SetAuthUserName(val userName: String) : ProfileDetailEvent()
    data class SetAuthPassword(val password: String) : ProfileDetailEvent()
    data class SetApiVersion(val version: ApiVersion) : ProfileDetailEvent()
    data class SetPreferredAccountsFilter(val filter: String) : ProfileDetailEvent()
    data class SetThemeHue(val hue: Int) : ProfileDetailEvent()
    data object Save : ProfileDetailEvent()
    data object Delete : ProfileDetailEvent()
    data object TestConnection : ProfileDetailEvent()
    data object DetectVersion : ProfileDetailEvent()
    data object ClearConnectionTestResult : ProfileDetailEvent()
}

sealed class ProfileDetailEffect {
    data object NavigateBack : ProfileDetailEffect()
    data class ShowError(val message: String) : ProfileDetailEffect()
    data object ShowDeleteConfirmation : ProfileDetailEffect()
    data class ProfileSaved(val profileId: Long) : ProfileDetailEffect()
}
```

**依存関係**:
- `ProfileRepository`: プロファイル CRUD
- `VersionDetector`: サーバーバージョン検出
- `SavedStateHandle`: プロセス再生時の状態復元

**推定行数**: 300-400行

---

## 状態遷移図

### SyncState（MainCoordinatorViewModel）

```text
┌─────────┐     StartSync      ┌──────────┐
│  Idle   │ ─────────────────► │ Syncing  │
└─────────┘                    └──────────┘
     ▲                              │
     │                    ┌─────────┴─────────┐
     │                    │                   │
     │              Success               Error
     │                    │                   │
     │                    ▼                   ▼
     │             ┌───────────┐       ┌──────────┐
     │             │ Completed │       │  Error   │
     │             └───────────┘       └──────────┘
     │                    │                   │
     └────────────────────┴───────────────────┘
           ClearSyncError / 自動クリア
```

### ConnectionTestResult（ProfileDetailViewModel）

```text
┌─────────┐    TestConnection    ┌───────────┐
│  null   │ ──────────────────► │ (testing) │
└─────────┘                      └───────────┘
                                      │
                           ┌──────────┴──────────┐
                           │                     │
                     Success                 Failure
                           │                     │
                           ▼                     ▼
                    ┌───────────┐          ┌──────────┐
                    │  Success  │          │  Error   │
                    └───────────┘          └──────────┘
                           │                     │
                           └─────────────────────┘
                                    │
                    ClearConnectionTestResult
                                    │
                                    ▼
                             ┌─────────┐
                             │  null   │
                             └─────────┘
```

## 関連図

### メイン画面 ViewModel 関係図

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                        MainActivityCompose                               │
│                                                                         │
│  ┌─────────────────────┐ ┌─────────────────────┐                        │
│  │ProfileSelectionVM   │ │MainCoordinatorVM    │                        │
│  │ - profiles          │ │ - selectedTab       │                        │
│  │ - currentProfileId  │ │ - isDrawerOpen      │                        │
│  │ - isReordering      │ │ - syncState         │                        │
│  └──────────┬──────────┘ └──────────┬──────────┘                        │
│             │                       │                                   │
│             │  ProfileRepository    │                                   │
│             │     (共有)            │                                   │
│             ▼                       ▼                                   │
│  ┌─────────────────────┐ ┌─────────────────────┐                        │
│  │AccountSummaryVM     │ │TransactionListVM    │                        │
│  │ - accounts          │ │ - transactions      │                        │
│  │ - showZeroBalances  │ │ - accountFilter     │                        │
│  │ - isLoading         │ │ - dateNavigation    │                        │
│  └─────────────────────┘ └─────────────────────┘                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘

データフロー:
ProfileSelectionVM.selectProfile()
    → ProfileRepository.setCurrentProfile()
        → AccountSummaryVM observes → reloads accounts
        → TransactionListVM observes → reloads transactions
```

### 取引登録画面 ViewModel 関係図

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                    NewTransactionActivityCompose                         │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                     TransactionFormVM                            │    │
│  │  - date, description, comment                                    │    │
│  │  - isSending, validationErrors                                   │    │
│  │  - submit(accountRows: List<TransactionAccountRow>)              │    │
│  └──────────────────────────────────────────────────────────────────┘    │
│                                ▲                                        │
│                                │ accountRows                            │
│                                │                                        │
│  ┌─────────────────────────────┴────────────────────────────────────┐   │
│  │                     AccountRowsVM                                 │   │
│  │  - rows: List<TransactionAccountRow>                             │   │
│  │  - focusedRowId, focusedElement                                  │   │
│  │  - selectedCurrency, currencyPickerVisible                       │   │
│  │  - isBalanced, balanceDifference                                 │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                ▲                                        │
│                                │ applyTemplate                          │
│                                │                                        │
│  ┌─────────────────────────────┴────────────────────────────────────┐   │
│  │                  TemplateApplicatorVM                             │   │
│  │  - matchingTemplates                                             │   │
│  │  - selectedTemplateId                                            │   │
│  │  - Effect: ApplyTemplate(description, accounts)                  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘

データフロー:
1. TemplateApplicatorVM.applySelectedTemplate()
    → emits ApplyTemplate effect
2. Activity observes effect
    → TransactionFormVM.setDescription(template.description)
    → AccountRowsVM.setRows(template.accounts)
3. User edits and submits
    → TransactionFormVM.submit(AccountRowsVM.uiState.value.rows)
```
