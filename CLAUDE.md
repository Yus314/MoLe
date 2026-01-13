# MoLe Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-01-10

## Active Technologies
- AGP 8.7.3 / Gradle 8.9 (002-agp-update)
- Kotlin 2.0.21 / Coroutines 1.9.0 / Java 8 互換 (JVM target 1.8) (003-kotlin-update)
- Room Database (SQLite) with KSP 2.0.21-1.0.26 (004-kapt-ksp-migration)
- Hilt 2.51.1 for Dependency Injection (005-hilt-di-setup)
- AndroidX Lifecycle 2.4.1, Room 2.4.2, Navigation 2.4.2, Jackson 2.17.1, Material 1.5.0 (001-java-kotlin-migration)
- Jetpack Compose with Material3 (composeBom 2024.12.01) (006-compose-ui-rebuild)
- Room Database（既存、変更なし） (006-compose-ui-rebuild)
- Kotlin 2.0.21 / JVM target 1.8 + Hilt 2.51.1, Room 2.4.2, Coroutines 1.9.0, Jetpack Compose (008-data-layer-repository)
- Kotlin 2.0.21 / JVM target 1.8 + Hilt 2.51.1, Room 2.4.2, Jetpack Compose (composeBom 2024.12.01), Coroutines 1.9.0 (009-eliminate-data-singleton)
- Room Database (SQLite) - 既存、変更なし (009-eliminate-data-singleton)

## Project Structure

```text
app/
├── src/
│   ├── main/
│   │   ├── kotlin/net/ktnx/mobileledger/ # Kotlin ソースファイル
│   │   │   └── di/                       # Hilt DI モジュール
│   │   └── res/                          # Android リソース
│   ├── test/                             # ユニットテスト
│   └── androidTest/                      # インストルメンテーションテスト
├── build.gradle                          # アプリビルド設定
└── schemas/                              # Room データベーススキーマ
specs/
└── 007-complete-compose-migration/       # 現在の機能仕様
```

## Commands

### Nix Flake コマンド（推奨）

| コマンド | 説明 |
|----------|------|
| `nix run .#build` | デバッグ APK ビルド |
| `nix run .#test` | ユニットテスト実行 |
| `nix run .#clean` | ビルド成果物のクリーン |
| `nix run .#install` | ビルド → 実機インストール |
| `nix run .#verify` | **フルワークフロー（テスト → ビルド → インストール）** |
| `nix run .#buildRelease` | リリース APK ビルド（署名必要）|
| `nix run .#lint` | Android Lint チェック（CI 用）|

### 開発シェル

```bash
nix develop .#fhs    # FHS 互換環境（ビルド可能）
nix develop          # 通常の開発シェル
```

### 注意事項

`nix run .#test` や `nix run .#build` などの出力を確認したい場合、パイプで `tail` や `head` を使用しないでください。出力が確認できなくなります。

```bash
# NG: 出力が表示されない
nix run .#test | tail -100

# OK: そのまま実行
nix run .#test
```

### Gradle コマンド（FHS 環境内）

```bash
./gradlew assembleDebug              # デバッグビルド
./gradlew test                       # ユニットテスト
./gradlew test --tests "*.TestName"  # 特定テスト実行
./gradlew lintDebug                  # Lint チェック
```

## Development Workflow

### 機能実装後の検証フロー

**重要**: 全ての機能変更は実機で検証する

```bash
# 推奨: フルワークフロー
nix run .#verify
```

このコマンドで以下が自動実行されます:
1. ユニットテスト
2. デバッグ APK ビルド
3. 実機へのインストール

その後、手動で以下を確認:
- アプリが正常に起動する
- データ更新が動作する
- プロファイル作成/編集ができる
- 取引登録ができる

## 実機デバッグ

### adb MCP ツール

Claude Code は以下の adb ツールを使用可能:

| ツール | 用途 | 使用例 |
|--------|------|--------|
| `adb_devices` | 接続確認 | デバイス一覧取得 |
| `adb_logcat` | ログ確認 | エラー調査、クラッシュ原因特定 |
| `inspect_ui` | UI階層取得 | Compose レイアウトデバッグ |
| `dump_image` | スクリーンショット | UI表示確認 |
| `adb_activity_manager` | Activity操作 | 画面遷移テスト、アプリ起動 |
| `adb_shell` | シェルコマンド | DB確認、任意操作 |
| `adb_package_manager` | パッケージ操作 | インストール確認 |

### パッケージ情報

| ビルド | パッケージ名 | Activity パス |
|--------|-------------|---------------|
| Debug | `net.ktnx.mobileledger.debug` | `net.ktnx.mobileledger.ui.activity.*` |
| Release | `net.ktnx.mobileledger` | `net.ktnx.mobileledger.ui.activity.*` |

**注意**: Debug ビルドでも Activity クラスのパスは元のパッケージ名を使用する。

### ログ確認パターン

```bash
# 基本（grep使用）
adb logcat -d | grep -E "(mobileledger|MoLe)"

# PIDベース（アプリ実行中のみ）
adb shell "logcat -d --pid=$(pidof net.ktnx.mobileledger.debug) | tail -50"

# エラーのみ
adb logcat -d *:E | grep mobileledger
```

### デバッグワークフロー

1. **ビルド・インストール**: `nix run .#verify`
2. **起動確認**: `adb_activity_manager` で SplashActivity 起動
   ```
   amCommand: start
   amArgs: -n net.ktnx.mobileledger.debug/net.ktnx.mobileledger.ui.activity.SplashActivity
   ```
3. **UI確認**: `dump_image` でスクリーンショット取得
4. **ログ確認**: `adb_logcat` でエラーチェック
5. **UI階層**: `inspect_ui` で Compose 階層確認（問題時）

### 検証チェックリスト

実機検証時に確認すべき項目:

- [ ] アプリ起動: SplashActivity → MainActivityCompose への遷移成功
- [ ] エラーなし: `adb_logcat` で E レベルログなし
- [ ] UI表示: `dump_image` で期待通りの画面
- [ ] 基本操作: プロファイル選択、取引一覧表示

### トラブルシューティング

| 症状 | 確認方法 | 対処 |
|------|----------|------|
| アプリ起動しない | `adb_logcat` でクラッシュログ | スタックトレース確認 |
| UI表示崩れ | `dump_image` + `inspect_ui` | レイアウト確認 |
| データ不整合 | `adb_shell` で DB 確認 | Room マイグレーション確認 |
| ビルド失敗 | `nix run .#build` 出力 | 依存関係・構文エラー確認 |
| Activity not found | パッケージ名とActivityパス確認 | Debug版はパッケージ名が異なる |

## Pre-commit Hooks

開発環境（`nix develop`）に入ると、pre-commit フックが自動的にインストールされます。

### 有効なフック

- **ktlint**: Kotlin コードスタイルチェック
- **detekt**: Kotlin 静的解析

### 手動実行

```bash
pre-commit run --all-files    # 全ファイルをチェック
pre-commit run ktlint         # ktlint のみ実行
pre-commit run detekt         # detekt のみ実行
```

### 設定ファイル

- `.editorconfig`: ktlint のルール設定
- `detekt.yml`: detekt の設定（maxIssues: -1 で警告のみ、エラーにしない）

### ktlint ルール状況

ほとんどのルールが有効化されています。以下のルールのみ永久無効化:

| ルール | 無効化理由 |
|--------|-----------|
| `filename` | Abstract* プレフィックスは意図的 |
| `property-naming` | JSON フィールド名との互換性（ptransaction_ 等）|
| `backing-property-naming` | 特殊なパターンの許容 |
| `function-naming` | 特殊な関数名の許容 |
| `no-wildcard-imports` | Android Studio デフォルト動作 |
| `package-name` | API バージョンサフィックス（v1_14 等）|
| `enum-entry-name-case` | JSON シリアライズ互換性 |
| `kdoc` | KDoc スタイル強制なし |

## Code Style

Kotlin 2.0.21 / Java 8 互換 (JVM target 1.8): Follow standard conventions

### Kotlin コードレビュー基準

- `!!` 演算子は原則禁止（使用時は理由コメント必須）
- `var` より `val` を優先
- data class を適切に使用
- スコープ関数のネストは 2 段階まで

## Hilt Dependency Injection

### 新しい ViewModel の作成方法

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    // ビジネスロジック
}
```

### Activity での使用

```kotlin
@AndroidEntryPoint
class MyActivity : AppCompatActivity() {
    private val viewModel: MyViewModel by viewModels()
}
```

### 利用可能な依存関係

- **RepositoryModule**: ProfileRepository, TransactionRepository, AccountRepository, TemplateRepository, CurrencyRepository
- **DatabaseModule**: DB, ProfileDAO, TransactionDAO, AccountDAO, AccountValueDAO, TemplateHeaderDAO, TemplateAccountDAO, CurrencyDAO, OptionDAO （レガシー、新規コードでは Repository を使用）

### DI ベストプラクティス

- 新しい ViewModel は `@HiltViewModel` と `@Inject constructor` を使用
- Activity は `@AndroidEntryPoint` でマーク
- `by viewModels()` デリゲートで ViewModel 取得
- **新規コードでは Repository を使用**（DAO 直接アクセスは非推奨）
- `Data` / `AppStateManager` は UI 状態（ロケール、背景タスク）のみに使用

## Repository Pattern (008-data-layer-repository)

### 概要

MoLe は **Repository パターン**を採用し、データアクセスをカプセル化しています。

### 利用可能なリポジトリ

| Repository | 用途 | 主なメソッド |
|------------|------|-------------|
| `ProfileRepository` | プロファイル管理 | `currentProfile`, `getAllProfiles()`, `insertProfile()` |
| `TransactionRepository` | 取引管理 | `getAllTransactions()`, `insertTransaction()`, `searchByDescription()` |
| `AccountRepository` | 勘定科目管理 | `getAllWithAmounts()`, `searchAccountNames()` |
| `TemplateRepository` | テンプレート管理 | `getAllTemplates()`, `getTemplateWithAccounts()` |
| `CurrencyRepository` | 通貨管理 | `getAllCurrencies()`, `getCurrencyByName()` |

### ViewModel での使用例

```kotlin
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val transactions = profileRepository.currentProfile
        .flatMapLatest { profile ->
            profile?.let { transactionRepository.getAllTransactions(it.id) }
                ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
```

### 注意事項

- **DAO 直接アクセスは非推奨**: ViewModel では Repository を使用
- **Flow を使用**: リアクティブデータストリーム
- **Data.getProfile() は非推奨**: `profileRepository.currentProfile.value` を使用
- **Data.profiles は非推奨**: `profileRepository.getAllProfiles()` を使用

## Jetpack Compose

### 概要

MoLe は **Jetpack Compose への完全移行が完了**しています（007-complete-compose-migration）。

**重要な制約**:
- XMLレイアウトファイル: **0件**（`app/src/main/res/layout/` は空）
- Fragment/DialogFragment: **使用禁止**（全てCompose Dialogに移行済み）
- ViewBinding: **使用禁止**（全てCompose UIに移行済み）

### Compose 実装済み画面

- **MainActivityCompose**: メイン画面（アカウント一覧、取引一覧タブ）
- **ProfileDetailActivity**: プロファイル詳細
- **TemplatesActivity**: テンプレート管理
- **NewTransactionActivityCompose**: 取引登録画面
- **SplashActivity**: スプラッシュ画面（Compose移行済み）
- **BackupsActivity**: バックアップ/リストア画面（Compose移行済み）

### Compose 実装済みダイアログ

- **DatePickerDialog**: 日付選択（Material3 DatePicker）
- **CurrencyPickerDialog**: 通貨選択（LazyVerticalGrid）
- **CrashReportDialog**: クラッシュレポート送信

### Compose ファイル構成

```text
app/src/main/kotlin/net/ktnx/mobileledger/ui/
├── activity/
│   ├── MainActivityCompose.kt           # メインActivity
│   ├── NewTransactionActivityCompose.kt # 取引登録Activity
│   ├── SplashActivity.kt                # スプラッシュActivity（Compose）
│   └── CrashReportingActivity.kt        # クラッシュレポートActivity
├── main/
│   ├── MainScreen.kt                    # メイン画面のComposable
│   ├── MainUiState.kt                   # メイン画面の状態
│   ├── MainViewModel.kt                 # メイン画面のViewModel（統合状態管理）
│   ├── ProfileSelectionViewModel.kt     # プロファイル選択ViewModel
│   ├── ProfileSelectionUiState.kt       # プロファイル選択状態
│   ├── AccountSummaryViewModel.kt       # アカウント一覧ViewModel
│   ├── AccountSummaryUiState.kt         # アカウント一覧状態
│   ├── TransactionListViewModel.kt      # 取引一覧ViewModel
│   ├── TransactionListUiState.kt        # 取引一覧状態
│   ├── MainCoordinatorViewModel.kt      # UI調整ViewModel（タブ/ドロワー/ナビ）
│   ├── MainCoordinatorUiState.kt        # UI調整状態
│   ├── AccountSummaryTab.kt             # アカウント一覧タブ
│   ├── TransactionListTab.kt            # 取引一覧タブ
│   └── NavigationDrawer.kt              # ナビゲーションドロワー
├── transaction/
│   ├── NewTransactionScreen.kt          # 取引登録画面
│   ├── NewTransactionUiState.kt         # 取引登録の状態
│   ├── NewTransactionViewModel.kt       # 取引登録のViewModel
│   ├── AccountAutocomplete.kt           # アカウント名オートコンプリート
│   └── TransactionRowItem.kt            # 取引行コンポーネント
├── profiles/
│   └── ProfileDetailScreen.kt           # プロファイル詳細画面
├── templates/
│   └── TemplatesScreen.kt               # テンプレート管理画面
├── splash/
│   ├── SplashScreen.kt                  # スプラッシュ画面Composable
│   └── SplashUiState.kt                 # スプラッシュ状態
├── backups/
│   ├── BackupsScreen.kt                 # バックアップ画面Composable
│   ├── BackupsViewModel.kt              # バックアップViewModel
│   └── BackupsUiState.kt                # バックアップ状態
├── theme/
│   ├── Theme.kt                         # MoLeTheme
│   ├── Color.kt                         # カラー定義
│   └── Type.kt                          # タイポグラフィ
└── components/
    ├── LoadingIndicator.kt              # ローディング表示
    ├── ErrorSnackbar.kt                 # エラー表示
    ├── ConfirmDialog.kt                 # 確認ダイアログ
    ├── DatePickerDialog.kt              # 日付選択ダイアログ
    ├── CurrencyPickerDialog.kt          # 通貨選択ダイアログ
    ├── CurrencyPickerUiState.kt         # 通貨選択状態
    ├── CrashReportDialog.kt             # クラッシュレポートダイアログ
    └── CrashReportUiState.kt            # クラッシュレポート状態
```

### Compose 開発パターン

#### UiState パターン

```kotlin
data class MyScreenUiState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val error: String? = null
)

sealed class MyScreenEvent {
    data class ItemClicked(val id: Long) : MyScreenEvent()
    object Refresh : MyScreenEvent()
}

sealed class MyScreenEffect {
    data class ShowError(val message: String) : MyScreenEffect()
    object NavigateBack : MyScreenEffect()
}
```

#### ViewModel パターン

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val someDAO: SomeDAO
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyScreenUiState())
    val uiState: StateFlow<MyScreenUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MyScreenEffect>()
    val effects = _effects.receiveAsFlow()

    fun onEvent(event: MyScreenEvent) {
        when (event) {
            is MyScreenEvent.ItemClicked -> handleItemClick(event.id)
            is MyScreenEvent.Refresh -> refresh()
        }
    }
}
```

#### Screen Composable パターン

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MyScreenEffect.NavigateBack -> onNavigateBack()
                is MyScreenEffect.ShowError -> { /* handle error */ }
            }
        }
    }

    Scaffold(
        topBar = { /* TopAppBar */ }
    ) { paddingValues ->
        // Content
    }
}
```

### Split ViewModels パターン（メイン画面）

メイン画面は責務分離のため複数の ViewModel に分割されています:

| ViewModel | 責務 | 主な状態 |
|-----------|------|----------|
| `MainViewModel` | 統合状態管理、UI状態の単一ソース | MainUiState（統合） |
| `ProfileSelectionViewModel` | プロファイル選択・並べ替え | ProfileSelectionUiState |
| `AccountSummaryViewModel` | アカウント一覧、残高フィルタ | AccountSummaryUiState |
| `TransactionListViewModel` | 取引一覧、フィルタ、日付ナビ | TransactionListUiState |
| `MainCoordinatorViewModel` | タブ選択、ドロワー、リフレッシュ | MainCoordinatorUiState |

**注意事項**:
- `MainViewModel` は現在も UI 状態の単一ソースとして使用
- 専門化された ViewModel は将来の完全移行に向けて準備済み
- すべての ViewModel は Repository パターンでデータアクセス
- 各 ViewModel は独立してテスト可能（300行以下の目標）

**Activity での使用例**:

```kotlin
@AndroidEntryPoint
class MainActivityCompose : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val profileSelectionViewModel: ProfileSelectionViewModel by viewModels()
    private val accountSummaryViewModel: AccountSummaryViewModel by viewModels()
    private val transactionListViewModel: TransactionListViewModel by viewModels()
    private val mainCoordinatorViewModel: MainCoordinatorViewModel by viewModels()
}
```

### Material3 注意事項

- **ExposedDropdownMenuBox**: `menuAnchor()` は deprecated だが、新しい API は composeBom 2024.12.01 では使用不可。`@Suppress("DEPRECATION")` を使用
- **DatePicker**: `rememberDatePickerState()` を使用
- **テーマ**: `MoLeTheme` でプロファイル色のカスタマイズをサポート

### テスト

Compose UI テストは `androidTest/` に配置:

```kotlin
@HiltAndroidTest
class MyScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun myScreen_displaysContent() {
        composeTestRule.setContent {
            MyScreen()
        }
        composeTestRule.onNodeWithText("Expected Text").assertIsDisplayed()
    }
}
```

## Recent Changes
- 010-refactor-mainviewmodel: **MainViewModel リファクタリング** - モノリシックな MainViewModel を 4 つの専門化された ViewModel に分割（ProfileSelectionViewModel, AccountSummaryViewModel, TransactionListViewModel, MainCoordinatorViewModel）。各コンポーネントは 300 行以下の目標達成、独立テスト可能、Repository パターン経由でデータアクセス。MainViewModel は統合状態管理として維持
- 009-eliminate-data-singleton: Added Kotlin 2.0.21 / JVM target 1.8 + Hilt 2.51.1, Room 2.4.2, Jetpack Compose (composeBom 2024.12.01), Coroutines 1.9.0
- 008-data-layer-repository: **Repository パターン導入** - ProfileRepository, TransactionRepository, AccountRepository, TemplateRepository, CurrencyRepository を追加。ViewModel は DAO 直接アクセスから Repository 経由に移行。Data.getProfile() / Data.profiles は ProfileRepository に移行済み
- 007-complete-compose-migration: **Compose移行完了** - 全XMLレイアウト削除、Fragment/DialogFragment全廃止、ViewBinding全廃止。DatePickerDialog、CurrencyPickerDialog、CrashReportDialog、SplashScreen、BackupsScreen をCompose化。ProfilesRecyclerViewAdapter等レガシーアダプター削除

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
