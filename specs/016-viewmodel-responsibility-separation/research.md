# Research: ViewModel 責務分離

**Feature**: 016-viewmodel-responsibility-separation
**Date**: 2026-01-16

## 概要

このドキュメントは ViewModel 責務分離の実装に向けた調査結果をまとめたものです。

## 調査項目

### 1. ViewModel 分離パターン

**決定: Activity レベル調整パターンを採用**

**根拠:**
- Google の推奨アーキテクチャガイドラインに準拠
- 既存の MoLe コードベースのパターンと一貫性を維持
- Mediator パターンは追加の抽象化レイヤーを導入し、複雑性が増す
- 既存の専門化 ViewModel（ProfileSelectionViewModel 等）が既にこのパターンを使用

**検討した代替案:**

| パターン | 説明 | 却下理由 |
|----------|------|----------|
| Mediator パターン | 画面スコープの Mediator クラスで共有状態を管理 | 新規抽象化レイヤーの追加、既存パターンとの不整合 |
| 単一の統合 ViewModel | MainViewModel を維持し内部でデリゲートに分割 | 830行のクラスが残り、単体テストが困難 |
| Navigation Graph スコープ | Navigation Graph に共有 ViewModel をスコープ | メイン画面は単一画面であり、複数画面フロー向けパターンは不適切 |

**採用パターンの詳細:**

```kotlin
// Activity レベルで複数の ViewModel を保持
@AndroidEntryPoint
class MainActivityCompose : ComponentActivity() {
    private val profileSelectionViewModel: ProfileSelectionViewModel by viewModels()
    private val accountSummaryViewModel: AccountSummaryViewModel by viewModels()
    private val transactionListViewModel: TransactionListViewModel by viewModels()
    private val mainCoordinatorViewModel: MainCoordinatorViewModel by viewModels()
}
```

### 2. ViewModel 間通信パターン

**決定: 単方向データフロー + AppStateService 経由の状態共有**

**根拠:**
- ViewModel 間の直接依存を回避（アンチパターン）
- 既存の `AppStateService` がグローバル状態（現在のプロファイル、同期情報等）を管理
- Activity が全 ViewModel の状態を購読し、必要に応じて他の ViewModel を呼び出す

**通信フロー:**

```text
User Action → ViewModel A → AppStateService → ViewModel B observes change
              (emits event)  (updates state)   (reacts to state change)

例: プロファイル選択
1. User taps profile
2. ProfileSelectionViewModel.onSelectProfile(profileId)
3. profileRepository.setCurrentProfile(profileId)
4. ProfileRepository updates currentProfile StateFlow
5. AccountSummaryViewModel observes currentProfile, reloads accounts
6. TransactionListViewModel observes currentProfile, reloads transactions
```

**ViewModel 間直接依存の回避:**

```kotlin
// ❌ アンチパターン: 直接依存
class AccountSummaryViewModel @Inject constructor(
    private val profileSelectionViewModel: ProfileSelectionViewModel  // NG
)

// ✅ 推奨パターン: Repository/Service 経由
class AccountSummaryViewModel @Inject constructor(
    private val profileRepository: ProfileRepository  // OK
) {
    init {
        viewModelScope.launch {
            profileRepository.currentProfile.collect { profile ->
                profile?.let { loadAccounts(it.id) }
            }
        }
    }
}
```

### 3. StateFlow 公開パターン

**決定: MutableStateFlow を private、StateFlow を public で公開**

**根拠:**
- カプセル化の原則を守る
- 外部からの予期せぬ状態変更を防止
- テスト時の状態制御が明確

**パターン:**

```kotlin
@HiltViewModel
class AccountSummaryViewModel @Inject constructor(...) : ViewModel() {
    // Private: 内部で変更可能
    private val _uiState = MutableStateFlow(AccountSummaryUiState())

    // Public: 読み取り専用
    val uiState: StateFlow<AccountSummaryUiState> = _uiState.asStateFlow()

    // 状態更新は専用メソッドを通じて
    fun onEvent(event: AccountSummaryEvent) {
        when (event) {
            is AccountSummaryEvent.ToggleZeroBalance -> toggleZeroBalance()
            // ...
        }
    }

    private fun toggleZeroBalance() {
        _uiState.update { it.copy(showZeroBalances = !it.showZeroBalances) }
    }
}
```

### 4. テストパターン

**決定: 既存の Fake 実装パターンを継続使用**

**根拠:**
- 既に `FakeProfileRepositoryForViewModel` 等の豊富な Fake 実装が存在
- `MainDispatcherRule` による Dispatcher 置換パターンが確立
- 分離後も各 ViewModel を独立してテスト可能

**テスト構造:**

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class AccountSummaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var accountRepository: FakeAccountRepositoryForAccountSummary
    private lateinit var viewModel: AccountSummaryViewModel

    @Before
    fun setup() {
        profileRepository = FakeProfileRepositoryForViewModel()
        accountRepository = FakeAccountRepositoryForAccountSummary()
        viewModel = AccountSummaryViewModel(
            profileRepository = profileRepository,
            accountRepository = accountRepository,
            preferencesRepository = FakePreferencesRepository()
        )
    }

    @Test
    fun `when profile changes, accounts are reloaded`() = runTest {
        // Given
        val profile = createTestProfile(id = 1)
        profileRepository.setCurrentProfile(profile)
        accountRepository.setAccounts(profile.id, testAccounts)

        // When
        advanceUntilIdle()

        // Then
        assertEquals(testAccounts.size, viewModel.uiState.value.accounts.size)
    }
}
```

### 5. NewTransactionViewModel 分離戦略

**決定: 3つの専門化 ViewModel に分離**

| ViewModel | 責務 | 状態 |
|-----------|------|------|
| TransactionFormViewModel | 日付、説明、フォーム送信 | TransactionFormUiState |
| AccountRowsViewModel | 勘定科目行の追加/削除/移動、通貨選択 | AccountRowsUiState |
| TemplateApplicatorViewModel | テンプレート検索、適用 | TemplateApplicatorUiState |

**状態の分離:**

```kotlin
// TransactionFormUiState
data class TransactionFormUiState(
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    val comment: String = "",
    val isSending: Boolean = false,
    val validationErrors: Map<FormField, String> = emptyMap()
)

// AccountRowsUiState
data class AccountRowsUiState(
    val rows: List<TransactionAccountRow> = listOf(emptyRow(), emptyRow()),
    val focusedRowId: Int? = null,
    val selectedCurrency: String? = null,
    val currencyPickerVisible: Boolean = false
)

// TemplateApplicatorUiState
data class TemplateApplicatorUiState(
    val matchingTemplates: List<TemplateItem> = emptyList(),
    val selectedTemplateId: Long? = null,
    val isSearching: Boolean = false
)
```

**Activity での連携:**

```kotlin
@AndroidEntryPoint
class NewTransactionActivityCompose : ComponentActivity() {
    private val formViewModel: TransactionFormViewModel by viewModels()
    private val rowsViewModel: AccountRowsViewModel by viewModels()
    private val templateViewModel: TemplateApplicatorViewModel by viewModels()

    // テンプレート適用時の連携
    private fun applyTemplate(template: TemplateItem) {
        templateViewModel.applyTemplate(template)
        // テンプレートの内容を各 ViewModel に反映
        formViewModel.setDescription(template.description)
        rowsViewModel.setRows(template.accounts)
    }
}
```

### 6. ProfileDetailModel の StateFlow 移行

**決定: LiveData から StateFlow に移行**

**根拠:**
- 他の ViewModel との一貫性確保
- Compose の `collectAsState()` でネイティブにサポート
- Coroutines との統合が自然

**移行パターン:**

```kotlin
// Before: LiveData
class ProfileDetailModel : ViewModel() {
    private val _profileName = MutableLiveData<String>()
    val profileName: LiveData<String> = _profileName
}

// After: StateFlow
@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileDetailUiState())
    val uiState: StateFlow<ProfileDetailUiState> = _uiState.asStateFlow()
}
```

## 参考情報

### Google Architecture Guidelines

- ViewModel は UI Layer に属し、単一責任原則に従う
- 単方向データフロー（UDF）を採用
- Repository 経由でデータアクセス
- StateFlow/LiveData で状態を公開

### droidcon Berlin 2025 "Breaking Up with Big ViewModels"

- Mediator パターンの紹介（本プロジェクトでは Activity レベル調整を採用）
- 行動駆動テストの推奨
- 300行以下の ViewModel を目標

### 既存 MoLe パターン

- `@HiltViewModel` + `@Inject constructor`
- `FakeXxxRepository` による依存性のモック
- `MainDispatcherRule` によるテスト用 Dispatcher 置換
- `runTest` + `advanceUntilIdle()` によるコルーチンテスト
