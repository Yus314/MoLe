# Quickstart: テスト実行ガイド

**Feature**: 011-test-coverage
**Date**: 2026-01-13

## テスト実行コマンド

### 全テスト実行

```bash
# 推奨: Nix 経由で実行
nix run .#test

# または FHS 環境内で
nix develop .#fhs
./gradlew test
```

### 特定のテストクラス実行

```bash
# MainViewModel のテストのみ
./gradlew test --tests "net.ktnx.mobileledger.ui.main.MainViewModelTest"

# NewTransactionViewModel のテストのみ
./gradlew test --tests "net.ktnx.mobileledger.ui.transaction.NewTransactionViewModelTest"

# 特定のテストメソッド
./gradlew test --tests "*.MainViewModelTest.selectProfile*"
```

### カバレッジレポート生成

```bash
# Kover カバレッジレポート生成 (Kotlin専用、JetBrains製)
nix run .#coverage

# または FHS 環境内で
./gradlew koverHtmlReportDebug koverXmlReportDebug

# レポート場所
# HTML: app/build/reports/kover/htmlDebug/index.html
# XML:  app/build/reports/kover/reportDebug.xml
```

## テスト構造

```text
app/src/test/kotlin/net/ktnx/mobileledger/
├── ui/
│   ├── main/
│   │   ├── MainViewModelTest.kt          # MainViewModel のテスト
│   │   ├── AccountSummaryViewModelTest.kt
│   │   ├── TransactionListViewModelTest.kt
│   │   ├── ProfileSelectionViewModelTest.kt
│   │   ├── MainCoordinatorViewModelTest.kt
│   │   └── TestFakes.kt                   # 共通 Fake 実装
│   └── transaction/
│       ├── NewTransactionViewModelTest.kt # NewTransactionViewModel のテスト
│       └── TestFakes.kt                   # 取引用 Fake 実装
└── data/
    └── repository/
        └── PreferencesRepositoryTest.kt
```

## テスト作成パターン

### 基本構造

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Fake 依存関係
    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var accountRepository: FakeAccountRepositoryForViewModel
    // ...

    // テスト対象
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        profileRepository = FakeProfileRepositoryForViewModel()
        preferencesRepository = FakePreferencesRepository()
        accountRepository = FakeAccountRepositoryForViewModel()
        // ...

        viewModel = MainViewModel(
            profileRepository = profileRepository,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            optionRepository = optionRepository,
            backgroundTaskManager = backgroundTaskManager,
            appStateService = appStateService,
            preferencesRepository = preferencesRepository
        )
    }

    @Test
    fun `test name describes behavior`() = runTest {
        // Given - 前提条件の設定
        val profile = createTestProfile(id = 1, name = "Test")
        profileRepository.insertProfile(profile)

        // When - テスト対象のアクション
        viewModel.onMainEvent(MainEvent.SelectProfile(1))
        advanceUntilIdle() // コルーチン完了を待機

        // Then - 検証
        assertEquals(profile, profileRepository.currentProfile.value)
    }
}
```

### MainDispatcherRule

テスト用の Dispatcher ルール（既存または新規作成）:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### Flow テスト（Turbine 使用）

```kotlin
@Test
fun `uiState updates correctly on profile selection`() = runTest {
    // Given
    val profile = createTestProfile(id = 1)
    profileRepository.insertProfile(profile)

    // When & Then
    viewModel.mainUiState.test {
        val initial = awaitItem()
        assertNull(initial.currentProfileId)

        viewModel.onMainEvent(MainEvent.SelectProfile(1))
        advanceUntilIdle()

        val updated = awaitItem()
        assertEquals(1, updated.currentProfileId)
    }
}
```

## 検証チェックリスト

### MainViewModel テストケース

- [ ] プロファイル選択で currentProfile が更新される
- [ ] プロファイル選択でドロワーが閉じる
- [ ] toggleZeroBalanceAccounts で設定が永続化される
- [ ] refreshData でバックグラウンドタスクが開始される
- [ ] cancelRefresh でタスクがキャンセルされる
- [ ] アカウント検索がデバウンスされる
- [ ] タブ選択で適切なデータがロードされる
- [ ] エラー発生時に UiState.error が設定される

### NewTransactionViewModel テストケース

- [ ] 初期化時にプロファイルからデフォルト通貨が設定される
- [ ] 金額入力で残高ヒントが再計算される
- [ ] テンプレート適用でアカウント行が設定される
- [ ] submitTransaction で TransactionSender.send が呼ばれる
- [ ] 送信成功でフォームがクリアされナビゲートされる
- [ ] 送信失敗でエラーが UiState に設定される
- [ ] バリデーション失敗で送信がブロックされる
- [ ] アカウント検索でサジェストが更新される

## トラブルシューティング

### テストが見つからない

```bash
# Gradle キャッシュをクリア
./gradlew clean test
```

### コルーチンテストがタイムアウト

```kotlin
// advanceUntilIdle() を使用してすべてのコルーチンを完了
@Test
fun `async operation completes`() = runTest {
    viewModel.startAsyncOperation()
    advanceUntilIdle() // これがないとテストが早く終了する
    assertTrue(viewModel.uiState.value.isComplete)
}
```

### Hilt 関連のエラー

このプロジェクトでは ViewModel のユニットテストに Hilt は不要。
Fake を直接コンストラクタ経由で注入するため。

```kotlin
// ❌ 不要
@HiltAndroidTest
class MainViewModelTest

// ✅ 正しい
class MainViewModelTest {
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        viewModel = MainViewModel(
            // 直接 Fake を渡す
        )
    }
}
```

## CI 統合

### GitHub Actions での実行

```yaml
- name: Run unit tests
  run: nix run .#test

- name: Generate coverage report (Kover)
  run: nix run .#coverage

- name: Upload coverage
  uses: codecov/codecov-action@v3
  with:
    files: app/build/reports/kover/reportDebug.xml
```

## 目標カバレッジ

| コンポーネント | 現在 | 目標 | 状態 |
|---------------|------|------|------|
| MainViewModel | 58% (line) | 70%+ | 進行中 |
| NewTransactionViewModel | 0% | 70%+ | @Ignore (FutureDates修正必要) |
| 全体 | 11% | 30%+ | 進行中 |

### カバレッジツール

- **Kover** (JetBrains製、Kotlin専用)
  - `inline` 関数、`suspend` 関数を正確に計測
  - Kotlin 構文（`when`、`sealed class`）の分岐を正確にカウント
