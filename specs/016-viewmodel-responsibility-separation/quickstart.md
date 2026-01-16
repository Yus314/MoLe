# Quickstart: ViewModel 責務分離

**Feature**: 016-viewmodel-responsibility-separation
**Date**: 2026-01-16

## 概要

このガイドでは、ViewModel 責務分離の実装手順を説明します。

## 前提条件

- Nix 開発環境（`nix develop` または `nix develop .#fhs`）
- Android デバイスまたはエミュレータ
- 既存のテストが全て通過していること

## クイックスタート手順

### 1. 環境確認

```bash
# 開発シェルに入る
nix develop .#fhs

# 既存テストの確認
nix run .#test

# ビルド確認
nix run .#build
```

### 2. 分離対象ファイルの確認

分離対象の ViewModel:

| ファイル | 行数 | 対象 |
|----------|------|------|
| `app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt` | 830 | P1 |
| `app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/NewTransactionViewModel.kt` | 961 | P2 |
| `app/src/main/kotlin/net/ktnx/mobileledger/ui/profile/ProfileDetailViewModel.kt` | 574 | P3 |

移行先の既存 ViewModel:

| ファイル | 行数 | 役割 |
|----------|------|------|
| `ProfileSelectionViewModel.kt` | 138 | プロファイル選択 |
| `AccountSummaryViewModel.kt` | 258 | アカウント一覧 |
| `TransactionListViewModel.kt` | 423 | 取引一覧 |
| `MainCoordinatorViewModel.kt` | 307 | タブ/ドロワー/同期 |

### 3. 開発ワークフロー

#### 3.1 TDD サイクル

各 ViewModel 分離は TDD サイクルで実装:

```bash
# 1. Red: 失敗するテストを書く
# 例: ProfileSelectionViewModel に新しいロジックのテストを追加
vim app/src/test/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModelTest.kt

# 2. テストが失敗することを確認
nix run .#test

# 3. Green: テストを通過する最小限の実装
vim app/src/main/kotlin/net/ktnx/mobileledger/ui/main/ProfileSelectionViewModel.kt

# 4. テストが通過することを確認
nix run .#test

# 5. Refactor: コードを改善
# ktlint/detekt が自動でチェック（コミット時）

# 6. コミット
git add -A && git commit -m "feat: migrate profile reordering to ProfileSelectionViewModel"
```

#### 3.2 ロジック移行パターン

MainViewModel から専門化 ViewModel へのロジック移行:

```kotlin
// Before: MainViewModel (830行)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    // ... 9つの依存関係
) : ViewModel() {
    // プロファイル選択ロジック
    // アカウント表示ロジック
    // 取引表示ロジック
    // 同期ロジック
    // ナビゲーションロジック
}

// After: 分離された ViewModel
// ProfileSelectionViewModel: プロファイル選択のみ
// AccountSummaryViewModel: アカウント表示のみ
// TransactionListViewModel: 取引表示のみ
// MainCoordinatorViewModel: タブ/ドロワー/同期/ナビゲーション
```

#### 3.3 テストの移行

```kotlin
// Before: MainViewModelTest に全てのテスト
@Test
fun `select profile updates current profile`() { ... }

@Test
fun `toggle zero balance filter reloads accounts`() { ... }

// After: 各 ViewModel のテストファイルに分離
// ProfileSelectionViewModelTest.kt
@Test
fun `select profile updates current profile`() { ... }

// AccountSummaryViewModelTest.kt
@Test
fun `toggle zero balance filter reloads accounts`() { ... }
```

### 4. 新規 ViewModel 作成手順

P2 で作成する新規 ViewModel（TransactionFormViewModel 等）の手順:

#### 4.1 UiState 定義

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormUiState.kt
package net.ktnx.mobileledger.ui.transaction

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

#### 4.2 ViewModel 実装

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModel.kt
package net.ktnx.mobileledger.ui.transaction

import ...

@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionSender: TransactionSender
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormUiState())
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TransactionFormEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: TransactionFormEvent) {
        when (event) {
            is TransactionFormEvent.SetDate -> setDate(event.date)
            is TransactionFormEvent.SetDescription -> setDescription(event.description)
            is TransactionFormEvent.SetComment -> setComment(event.comment)
            is TransactionFormEvent.Submit -> submit(event.accountRows)
        }
    }

    private fun setDate(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
        validateForm()
    }

    private fun setDescription(description: String) {
        _uiState.update { it.copy(description = description) }
        validateForm()
    }

    private fun setComment(comment: String) {
        _uiState.update { it.copy(comment = comment) }
    }

    private fun validateForm() {
        val errors = mutableMapOf<FormField, String>()
        if (_uiState.value.description.isBlank()) {
            errors[FormField.Description] = "Description is required"
        }
        _uiState.update {
            it.copy(
                validationErrors = errors,
                canSubmit = errors.isEmpty()
            )
        }
    }

    private fun submit(accountRows: List<TransactionAccountRow>) {
        if (!_uiState.value.canSubmit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            try {
                val profile = profileRepository.currentProfile.value
                    ?: throw IllegalStateException("No profile selected")
                // トランザクション送信ロジック
                transactionSender.send(profile, buildTransaction(accountRows))
                _effects.send(TransactionFormEffect.TransactionSaved)
                _effects.send(TransactionFormEffect.NavigateBack)
            } catch (e: Exception) {
                _effects.send(TransactionFormEffect.ShowError(e.message ?: "Unknown error"))
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    private fun buildTransaction(accountRows: List<TransactionAccountRow>): LedgerTransaction {
        // トランザクション構築ロジック
    }
}
```

#### 4.3 テスト作成

```kotlin
// app/src/test/kotlin/net/ktnx/mobileledger/ui/transaction/TransactionFormViewModelTest.kt
package net.ktnx.mobileledger.ui.transaction

import ...

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var transactionSender: FakeTransactionSender
    private lateinit var viewModel: TransactionFormViewModel

    @Before
    fun setup() {
        profileRepository = FakeProfileRepositoryForViewModel()
        transactionSender = FakeTransactionSender()
        viewModel = TransactionFormViewModel(
            profileRepository = profileRepository,
            transactionSender = transactionSender
        )
    }

    @Test
    fun `when description is set, uiState is updated`() = runTest {
        // When
        viewModel.onEvent(TransactionFormEvent.SetDescription("Test description"))
        advanceUntilIdle()

        // Then
        assertEquals("Test description", viewModel.uiState.value.description)
    }

    @Test
    fun `when description is empty, validation error is set`() = runTest {
        // When
        viewModel.onEvent(TransactionFormEvent.SetDescription(""))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.validationErrors.containsKey(FormField.Description))
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `when submit succeeds, TransactionSaved effect is emitted`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.setCurrentProfile(profile)
        transactionSender.shouldSucceed = true
        viewModel.onEvent(TransactionFormEvent.SetDescription("Test"))
        advanceUntilIdle()

        val effects = mutableListOf<TransactionFormEffect>()
        backgroundScope.launch {
            viewModel.effects.collect { effects.add(it) }
        }

        // When
        viewModel.onEvent(TransactionFormEvent.Submit(listOf(testRow1, testRow2)))
        advanceUntilIdle()

        // Then
        assertTrue(effects.contains(TransactionFormEffect.TransactionSaved))
    }
}
```

### 5. 検証手順

各フェーズ完了時の検証:

```bash
# 1. ユニットテスト
nix run .#test

# 2. コードスタイルチェック（コミット時に自動実行）
pre-commit run --all-files

# 3. ビルド
nix run .#build

# 4. 実機インストール・検証
nix run .#verify

# 5. 手動検証
# - アプリ起動確認
# - プロファイル選択
# - アカウント一覧表示
# - 取引一覧表示
# - 取引登録
# - プロファイル編集
```

### 6. トラブルシューティング

| 問題 | 解決方法 |
|------|----------|
| テストが失敗する | `advanceUntilIdle()` を確認、Fake の設定を確認 |
| ビルドエラー | `@HiltViewModel` と `@Inject constructor` を確認 |
| 状態が更新されない | StateFlow の購読を確認、`viewModelScope` を確認 |
| UI が反応しない | Activity で `by viewModels()` を確認 |

### 7. 参考ドキュメント

- [research.md](./research.md): ViewModel 分離パターンの調査結果
- [data-model.md](./data-model.md): ViewModel エンティティ定義
- [spec.md](./spec.md): 機能仕様
- [CLAUDE.md](../../CLAUDE.md): プロジェクト開発ガイドライン
