# ViewModel インターフェース契約

**Feature**: 016-viewmodel-responsibility-separation
**Date**: 2026-01-16

## 概要

本機能は内部リファクタリングであり、外部 API は存在しません。このドキュメントでは、分離後の ViewModel 間のインターフェース契約を定義します。

## ViewModel 公開インターフェース

### P1: メイン画面 ViewModel

#### ProfileSelectionViewModel

```kotlin
interface ProfileSelectionViewModelContract {
    // 状態
    val uiState: StateFlow<ProfileSelectionUiState>

    // イベントハンドラ
    fun onEvent(event: ProfileSelectionEvent)

    // エフェクト
    val effects: Flow<ProfileSelectionEffect>
}

// 状態契約
data class ProfileSelectionUiState(
    val profiles: List<ProfileListItem>,      // 必須: プロファイル一覧
    val currentProfileId: Long?,              // 必須: 選択中プロファイル ID
    val isReordering: Boolean                 // 必須: 並べ替えモード
)

// イベント契約
sealed class ProfileSelectionEvent {
    data class SelectProfile(val profileId: Long)
    data class ReorderProfiles(val orderedProfiles: List<ProfileListItem>)
    object StartReorder
    object EndReorder
}

// エフェクト契約
sealed class ProfileSelectionEffect {
    data class ProfileSelected(val profileId: Long)
}
```

#### AccountSummaryViewModel

```kotlin
interface AccountSummaryViewModelContract {
    // 状態
    val uiState: StateFlow<AccountSummaryUiState>

    // イベントハンドラ
    fun onEvent(event: AccountSummaryEvent)
}

// 状態契約
data class AccountSummaryUiState(
    val accounts: List<AccountSummaryListItem>,  // 必須: アカウント一覧
    val showZeroBalances: Boolean,               // 必須: 残高ゼロ表示フラグ
    val isLoading: Boolean,                      // 必須: ローディング状態
    val error: String?,                          // オプション: エラーメッセージ
    val headerText: String                       // 必須: ヘッダーテキスト
)

// イベント契約
sealed class AccountSummaryEvent {
    object ToggleZeroBalance
    data class ToggleAccountExpanded(val accountName: String)
    data class ToggleAmountsExpanded(val accountName: String)
    object Refresh
    object ClearError
}
```

#### TransactionListViewModel

```kotlin
interface TransactionListViewModelContract {
    // 状態
    val uiState: StateFlow<TransactionListUiState>

    // イベントハンドラ
    fun onEvent(event: TransactionListEvent)
}

// 状態契約
data class TransactionListUiState(
    val transactions: List<TransactionListItem>,  // 必須: 取引一覧
    val accountFilter: String,                    // 必須: アカウントフィルタ
    val accountSuggestions: List<String>,         // 必須: サジェスト一覧
    val showAccountSuggestions: Boolean,          // 必須: サジェスト表示フラグ
    val dateNavigationState: DateNavigationState, // 必須: 日付ナビ状態
    val isLoading: Boolean,                       // 必須: ローディング状態
    val error: String?                            // オプション: エラーメッセージ
)

// イベント契約
sealed class TransactionListEvent {
    data class SetAccountFilter(val filter: String)
    object ClearAccountFilter
    data class SelectSuggestion(val accountName: String)
    data class NavigateToMonth(val monthStart: LocalDate)
    object NavigateToPreviousMonth
    object NavigateToNextMonth
    object Refresh
    object ClearError
}
```

#### MainCoordinatorViewModel

```kotlin
interface MainCoordinatorViewModelContract {
    // 状態
    val uiState: StateFlow<MainCoordinatorUiState>

    // イベントハンドラ
    fun onEvent(event: MainCoordinatorEvent)

    // エフェクト
    val effects: Flow<MainCoordinatorEffect>
}

// 状態契約
data class MainCoordinatorUiState(
    val selectedTab: MainTab,      // 必須: 選択中タブ
    val isDrawerOpen: Boolean,     // 必須: ドロワー開閉状態
    val syncState: SyncState,      // 必須: 同期状態
    val syncProgress: Float,       // 必須: 同期進捗 (0.0-1.0)
    val lastSyncInfo: LastSyncInfo? // オプション: 最終同期情報
)

// イベント契約
sealed class MainCoordinatorEvent {
    data class SelectTab(val tab: MainTab)
    object OpenDrawer
    object CloseDrawer
    object StartSync
    object CancelSync
    object ClearSyncError
}

// エフェクト契約
sealed class MainCoordinatorEffect {
    object NavigateToNewTransaction
    data class NavigateToProfileDetail(val profileId: Long)
    object NavigateToNewProfile
    object NavigateToTemplates
    object NavigateToBackups
    data class ShowSnackbar(val message: String)
}
```

---

### P2: 取引登録画面 ViewModel

#### TransactionFormViewModel

```kotlin
interface TransactionFormViewModelContract {
    // 状態
    val uiState: StateFlow<TransactionFormUiState>

    // イベントハンドラ
    fun onEvent(event: TransactionFormEvent)

    // エフェクト
    val effects: Flow<TransactionFormEffect>
}

// 状態契約
data class TransactionFormUiState(
    val date: LocalDate,                          // 必須: 日付
    val description: String,                      // 必須: 説明
    val comment: String,                          // 必須: コメント
    val isSending: Boolean,                       // 必須: 送信中フラグ
    val validationErrors: Map<FormField, String>, // 必須: 検証エラー
    val canSubmit: Boolean                        // 必須: 送信可能フラグ
)

// イベント契約
sealed class TransactionFormEvent {
    data class SetDate(val date: LocalDate)
    data class SetDescription(val description: String)
    data class SetComment(val comment: String)
    data class Submit(val accountRows: List<TransactionAccountRow>)
}

// エフェクト契約
sealed class TransactionFormEffect {
    object TransactionSaved
    data class ShowError(val message: String)
    object NavigateBack
}
```

#### AccountRowsViewModel

```kotlin
interface AccountRowsViewModelContract {
    // 状態
    val uiState: StateFlow<AccountRowsUiState>

    // イベントハンドラ
    fun onEvent(event: AccountRowsEvent)

    // エフェクト
    val effects: Flow<AccountRowsEffect>
}

// 状態契約
data class AccountRowsUiState(
    val rows: List<TransactionAccountRow>,    // 必須: 勘定科目行一覧
    val focusedRowId: Int?,                   // オプション: フォーカス行 ID
    val focusedElement: FocusedElement?,      // オプション: フォーカス要素
    val selectedCurrency: String?,            // オプション: 選択通貨
    val currencyPickerVisible: Boolean,       // 必須: 通貨選択表示フラグ
    val availableCurrencies: List<Currency>,  // 必須: 利用可能通貨
    val accountSuggestions: List<String>,     // 必須: サジェスト一覧
    val showAccountSuggestions: Boolean,      // 必須: サジェスト表示フラグ
    val isBalanced: Boolean,                  // 必須: バランス状態
    val balanceDifference: BigDecimal         // 必須: バランス差額
)

// イベント契約
sealed class AccountRowsEvent {
    data class SetAccountName(val rowId: Int, val name: String)
    data class SetAmount(val rowId: Int, val amount: String)
    data class SetRowComment(val rowId: Int, val comment: String)
    data class SetRowCurrency(val rowId: Int, val currency: String?)
    data class SetFocus(val rowId: Int, val element: FocusedElement)
    object AddRow
    data class RemoveRow(val rowId: Int)
    data class MoveRow(val fromIndex: Int, val toIndex: Int)
    object ShowCurrencyPicker
    object HideCurrencyPicker
    data class SelectCurrency(val currency: String)
    data class SelectAccountSuggestion(val rowId: Int, val name: String)
}

// エフェクト契約
sealed class AccountRowsEffect {
    data class RequestFocus(val rowId: Int, val element: FocusedElement)
    object HideKeyboard
}
```

#### TemplateApplicatorViewModel

```kotlin
interface TemplateApplicatorViewModelContract {
    // 状態
    val uiState: StateFlow<TemplateApplicatorUiState>

    // イベントハンドラ
    fun onEvent(event: TemplateApplicatorEvent)

    // エフェクト
    val effects: Flow<TemplateApplicatorEffect>
}

// 状態契約
data class TemplateApplicatorUiState(
    val matchingTemplates: List<TemplateItem>,  // 必須: マッチするテンプレート
    val selectedTemplateId: Long?,              // オプション: 選択テンプレート ID
    val isSearching: Boolean                    // 必須: 検索中フラグ
)

// イベント契約
sealed class TemplateApplicatorEvent {
    data class SearchTemplates(val query: String)
    data class SelectTemplate(val templateId: Long)
    object ClearSelection
    object ApplySelectedTemplate
}

// エフェクト契約
sealed class TemplateApplicatorEffect {
    data class ApplyTemplate(
        val description: String,
        val accounts: List<TemplateAccountItem>
    )
}
```

---

### P3: プロファイル詳細画面 ViewModel

#### ProfileDetailViewModel

```kotlin
interface ProfileDetailViewModelContract {
    // 状態
    val uiState: StateFlow<ProfileDetailUiState>

    // イベントハンドラ
    fun onEvent(event: ProfileDetailEvent)

    // エフェクト
    val effects: Flow<ProfileDetailEffect>
}

// 状態契約
data class ProfileDetailUiState(
    val profileId: Long,                           // 必須: プロファイル ID
    val profileName: String,                       // 必須: プロファイル名
    val url: String,                               // 必須: サーバー URL
    val useAuthentication: Boolean,                // 必須: 認証使用フラグ
    val authUserName: String,                      // 必須: 認証ユーザー名
    val authPassword: String,                      // 必須: 認証パスワード
    val apiVersion: ApiVersion,                    // 必須: API バージョン
    val preferredAccountsFilter: String,           // 必須: アカウントフィルタ
    val themeHue: Int,                             // 必須: テーマ色相
    val isLoading: Boolean,                        // 必須: ローディング状態
    val isSaving: Boolean,                         // 必須: 保存中フラグ
    val isTesting: Boolean,                        // 必須: テスト中フラグ
    val validationErrors: Map<ProfileField, String>, // 必須: 検証エラー
    val connectionTestResult: ConnectionTestResult?, // オプション: 接続テスト結果
    val detectedVersion: String?                   // オプション: 検出バージョン
) {
    // 計算プロパティ
    val isNewProfile: Boolean
    val canDelete: Boolean
    val isFormValid: Boolean
    val showInsecureWarning: Boolean
}

// イベント契約
sealed class ProfileDetailEvent {
    data class SetProfileName(val name: String)
    data class SetUrl(val url: String)
    data class SetUseAuthentication(val use: Boolean)
    data class SetAuthUserName(val userName: String)
    data class SetAuthPassword(val password: String)
    data class SetApiVersion(val version: ApiVersion)
    data class SetPreferredAccountsFilter(val filter: String)
    data class SetThemeHue(val hue: Int)
    object Save
    object Delete
    object TestConnection
    object DetectVersion
    object ClearConnectionTestResult
}

// エフェクト契約
sealed class ProfileDetailEffect {
    object NavigateBack
    data class ShowError(val message: String)
    object ShowDeleteConfirmation
    data class ProfileSaved(val profileId: Long)
}
```

---

## Activity 統合契約

### MainActivityCompose

```kotlin
// Activity は以下の ViewModel を保持し、状態を購読する
class MainActivityCompose : ComponentActivity() {
    // ViewModel 保持
    private val profileSelectionViewModel: ProfileSelectionViewModel
    private val accountSummaryViewModel: AccountSummaryViewModel
    private val transactionListViewModel: TransactionListViewModel
    private val mainCoordinatorViewModel: MainCoordinatorViewModel

    // エフェクトの処理
    // - ProfileSelectionEffect.ProfileSelected → 他 VM が自動で反応
    // - MainCoordinatorEffect.Navigate* → Navigation 実行
    // - MainCoordinatorEffect.ShowSnackbar → Snackbar 表示
}
```

### NewTransactionActivityCompose

```kotlin
// Activity は以下の ViewModel を保持し、状態を購読する
class NewTransactionActivityCompose : ComponentActivity() {
    // ViewModel 保持
    private val formViewModel: TransactionFormViewModel
    private val rowsViewModel: AccountRowsViewModel
    private val templateViewModel: TemplateApplicatorViewModel

    // エフェクトの処理
    // - TemplateApplicatorEffect.ApplyTemplate → formVM/rowsVM に反映
    // - TransactionFormEffect.TransactionSaved → 通知表示
    // - TransactionFormEffect.NavigateBack → Activity 終了
    // - AccountRowsEffect.RequestFocus → フォーカス移動
}
```

---

## 依存関係契約

各 ViewModel は以下の Repository/Service にのみ依存可能:

| ViewModel | 許可された依存関係 |
|-----------|-------------------|
| ProfileSelectionViewModel | ProfileRepository, AppStateService |
| AccountSummaryViewModel | ProfileRepository, AccountRepository, PreferencesRepository, CurrencyFormatter |
| TransactionListViewModel | ProfileRepository, TransactionRepository, AccountRepository, CurrencyFormatter |
| MainCoordinatorViewModel | ProfileRepository, AppStateService, TransactionSyncer |
| TransactionFormViewModel | ProfileRepository, TransactionSender |
| AccountRowsViewModel | ProfileRepository, AccountRepository, CurrencyRepository, CurrencyFormatter |
| TemplateApplicatorViewModel | ProfileRepository, TemplateRepository |
| ProfileDetailViewModel | ProfileRepository, VersionDetector, SavedStateHandle |

**禁止**: ViewModel 間の直接依存（例: AccountSummaryViewModel → ProfileSelectionViewModel）
