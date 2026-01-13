# Research: テストカバレッジ向上のための技術調査

**Feature**: 011-test-coverage
**Date**: 2026-01-13
**Status**: Complete

## 調査項目と決定事項

### 1. App シングルトン置換方法

**問題**: MainViewModel 143行目と342行目で `App.getShowZeroBalanceAccounts()` / `App.storeShowZeroBalanceAccounts()` を直接呼び出しており、Android フレームワーク依存のためユニットテスト不可。

**決定**: PreferencesRepository を注入で使用

**根拠**:
- PreferencesRepository インターフェースは既に存在（`/app/src/main/kotlin/net/ktnx/mobileledger/data/repository/PreferencesRepository.kt`）
- `getShowZeroBalanceAccounts()` と `setShowZeroBalanceAccounts()` メソッドが既に定義済み
- RepositoryModule で既にバインド済み
- 既存の Repository パターンと一貫性あり

**代替案（却下）**:
- MockK でスタティックメソッドをモック → Android SDK 依存が残る、テスト実行速度低下
- テスト専用のビルドバリアント → 複雑すぎる

**実装手順**:
1. MainViewModel のコンストラクタに PreferencesRepository を追加
2. `App.getShowZeroBalanceAccounts()` を `preferencesRepository.getShowZeroBalanceAccounts()` に置換
3. `App.storeShowZeroBalanceAccounts()` を `preferencesRepository.setShowZeroBalanceAccounts()` に置換

### 2. Thread → Coroutine 移行戦略

**問題**:
- MainViewModel 779行目: `TransactionsDisplayedFilter extends Thread()` 内部クラス
- NewTransactionViewModel 764行目: `SendTransactionTask` (Thread サブクラス) の直接インスタンス化

**決定**: TransactionSender インターフェースを導入し、Coroutine ベースの実装を提供

**根拠**:
- spec.md で `suspend fun send(transaction: LedgerTransaction): Result<Unit>` と定義済み
- Coroutine は viewModelScope でキャンセル可能、テストで StandardTestDispatcher を使用可能
- 既存の非同期パターン（Repository の suspend 関数）と一貫性あり

**代替案（却下）**:
- Thread をそのまま残し、テストでは別のメソッドを呼ぶ → テストカバレッジが不完全
- RxJava 導入 → 新しい依存関係、学習コスト

**実装手順**:
```kotlin
// domain/usecase/TransactionSender.kt
interface TransactionSender {
    suspend fun send(
        profile: Profile,
        transaction: LedgerTransaction,
        simulate: Boolean = false
    ): Result<Unit>
}

// 本番実装: SendTransactionTaskAdapter（既存 Thread をラップ）
// テスト実装: FakeTransactionSender
```

### 3. TransactionsDisplayedFilter の Coroutine 移行

**問題**: MainViewModel の内部クラス `TransactionsDisplayedFilter` が Thread を継承

**決定**: viewModelScope.launch + withContext(Dispatchers.Default) に置換

**根拠**:
- フィルタリングは CPU バウンド処理、Dispatchers.Default が適切
- viewModelScope を使用することでキャンセル処理が自動化
- コードの可読性向上（スコープ関数使用可能）

**実装手順**:
1. Thread 継承クラスを削除
2. フィルタリング処理を suspend 関数に変換
3. viewModelScope.launch で呼び出し
4. AtomicReference の代わりに Job を使用してキャンセル管理

### 4. Fake 実装戦略

**問題**: テストに必要な Fake 実装の設計

**決定**: 既存の TestFakes.kt パターンを踏襲し、FakePreferencesRepository と FakeTransactionSender を追加

**既存 Fake 実装（350行）**:
- FakeProfileRepositoryForViewModel
- FakeTransactionRepositoryForViewModel
- FakeAccountRepositoryForViewModel
- FakeOptionRepositoryForViewModel
- FakeBackgroundTaskManagerForViewModel
- FakeAppStateServiceForViewModel

**新規 Fake 実装**:
```kotlin
// FakePreferencesRepository
class FakePreferencesRepository : PreferencesRepository {
    var showZeroBalanceAccounts = false
    var startupProfileId = -1L
    var startupTheme = -1

    override fun getShowZeroBalanceAccounts() = showZeroBalanceAccounts
    override fun setShowZeroBalanceAccounts(value: Boolean) { showZeroBalanceAccounts = value }
    // ...
}

// FakeTransactionSender
class FakeTransactionSender : TransactionSender {
    var shouldSucceed = true
    var sentTransactions = mutableListOf<LedgerTransaction>()

    override suspend fun send(profile: Profile, transaction: LedgerTransaction, simulate: Boolean): Result<Unit> {
        sentTransactions.add(transaction)
        return if (shouldSucceed) Result.success(Unit) else Result.failure(Exception("Simulated failure"))
    }
}
```

### 5. テストパターンと構造

**決定**: JUnit 4 + runTest + Turbine パターン

**根拠**:
- プロジェクトで既に使用されている構成
- kotlinx-coroutines-test が設定済み
- Turbine で Flow のテストが簡潔に書ける

**テスト構造**:
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: MainViewModel
    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var preferencesRepository: FakePreferencesRepository
    // ...

    @Before
    fun setup() {
        // Fake 初期化
        viewModel = MainViewModel(
            profileRepository = profileRepository,
            preferencesRepository = preferencesRepository,
            // ...
        )
    }

    @Test
    fun `selectProfile updates currentProfile and closes drawer`() = runTest {
        // Given
        val profile = createTestProfile(id = 1)
        profileRepository.insertProfile(profile)

        // When
        viewModel.onMainEvent(MainEvent.SelectProfile(1))

        // Then
        assertEquals(profile, profileRepository.currentProfile.value)
        assertFalse(appStateService.drawerOpen.value)
    }
}
```

### 6. カバレッジ測定ツール

**決定**: JaCoCo（Gradle プラグイン）

**根拠**:
- Android/Gradle プロジェクトの標準ツール
- プロジェクトで既に設定済み（build.gradle に testCoverageEnabled = true）

**設定確認**:
```groovy
android {
    buildTypes {
        debug {
            testCoverageEnabled true
        }
    }
}
```

**レポート生成**:
```bash
./gradlew testDebugUnitTestCoverage
# 出力: app/build/reports/jacoco/testDebugUnitTestCoverage/html/index.html
```

### 7. テスト実行時間最適化

**決定**: StandardTestDispatcher + advanceUntilIdle パターン

**根拠**:
- 実際の時間待機を回避（delay() がテスト時間に影響しない）
- テストの決定性が向上

**実装**:
```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()

@Test
fun `debounced search executes after delay`() = runTest {
    viewModel.onTransactionListEvent(TransactionListEvent.SetAccountFilter("test"))
    advanceUntilIdle() // すべての保留中コルーチンを完了

    assertEquals("test", viewModel.transactionListUiState.value.accountFilter)
}
```

## 未解決事項

なし - すべての技術的な不明点が解決済み。

## 次のステップ

1. Phase 1: data-model.md でインターフェース設計を文書化
2. Phase 1: contracts/ に TransactionSender インターフェースを定義
3. Phase 1: quickstart.md でテスト実行ガイドを作成
