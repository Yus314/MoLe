# Quickstart: Data.kt シングルトンの廃止

**Date**: 2026-01-12
**Feature**: 009-eliminate-data-singleton

## 概要

このドキュメントは実装着手時のクイックリファレンスを提供する。

## 前提条件

- Nix 開発環境がセットアップされている
- Android 実機または エミュレータが接続されている
- ブランチ `009-eliminate-data-singleton` にいる

```bash
# 開発環境に入る
nix develop .#fhs

# ブランチ確認
git branch --show-current
# → 009-eliminate-data-singleton
```

## 実装順序

```
┌─────────────────────────────────────────────────────────────────┐
│ Phase 1: プロファイル管理移行 (Day 1-2)                         │
├─────────────────────────────────────────────────────────────────┤
│ 1. MainViewModel にプロファイル関連状態を追加                    │
│ 2. MainActivityCompose の Data.observeProfile() を置換          │
│ 3. ProfileThemedActivity の observeProfile() を置換             │
│ 4. 他の Activity (Backups, NewTransaction) を更新               │
│ 5. ProfileRepositoryImpl の後方互換性コードを削除               │
│ 6. 検証: nix run .#verify + 手動テスト                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Phase 2: バックグラウンドタスク管理移行 (Day 3-4)               │
├─────────────────────────────────────────────────────────────────┤
│ 1. BackgroundTaskManager インターフェース作成                    │
│ 2. BackgroundTaskManagerImpl 実装                               │
│ 3. ServiceModule に DI 設定追加                                  │
│ 4. RetrieveTransactionsTask を更新                              │
│ 5. MainViewModel/MainActivityCompose を更新                      │
│ 6. 検証: nix run .#verify + 手動テスト (同期機能)               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Phase 3: 通貨フォーマット・UIステート移行 (Day 5-6)             │
├─────────────────────────────────────────────────────────────────┤
│ 1. CurrencyFormatter インターフェース・実装作成                  │
│ 2. AppStateService インターフェース・実装作成                    │
│ 3. ServiceModule に DI 設定追加                                  │
│ 4. App.kt を更新 (CurrencyFormatter 注入)                        │
│ 5. 残りの Data 参照を置換                                        │
│ 6. AppStateManager.kt と AppStateModule.kt を削除               │
│ 7. 最終検証: grep + nix run .#verify + 全機能テスト             │
└─────────────────────────────────────────────────────────────────┘
```

## ファイル作成場所

```
app/src/main/kotlin/net/ktnx/mobileledger/
├── service/                          # 新規ディレクトリ
│   ├── BackgroundTaskManager.kt      # Interface + Impl
│   ├── CurrencyFormatter.kt          # Interface + Impl
│   └── AppStateService.kt            # Interface + Impl
│
└── di/
    └── ServiceModule.kt              # 新規 DI モジュール
```

## Phase 1: プロファイル管理移行

### Step 1.1: MainViewModel の更新

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/ui/main/MainViewModel.kt

@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,  // 追加
    // ... 既存の依存関係
) : ViewModel() {
    // 現在のプロファイル
    val currentProfile: StateFlow<Profile?> = profileRepository.currentProfile

    // 全プロファイルリスト
    val allProfiles: StateFlow<List<Profile>> = profileRepository
        .getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // プロファイル切り替え
    fun selectProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.setCurrentProfile(profile)
        }
    }
}
```

### Step 1.2: MainActivityCompose の更新

```kotlin
// Before
val currentProfile = Data.observeProfile().observeAsState()
Data.profiles.observe(this) { profiles -> ... }

// After
val currentProfile by viewModel.currentProfile.collectAsState()
val allProfiles by viewModel.allProfiles.collectAsState()
```

### Step 1.3: ProfileThemedActivity の更新

```kotlin
// Before (in base class)
Data.observeProfile().observe(this) { profile ->
    profile?.let { applyTheme(it.themeHue) }
}

// After (inject ProfileRepository)
@Inject lateinit var profileRepository: ProfileRepository

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            profileRepository.currentProfile.collect { profile ->
                profile?.let { applyTheme(it.themeHue) }
            }
        }
    }
}
```

### Step 1.4: 検証

```bash
# ユニットテスト
nix run .#test

# ビルド・インストール
nix run .#verify

# 手動テスト
# - プロファイル切り替え
# - テーマカラー反映
# - 複数画面間の一貫性
```

## Phase 2: バックグラウンドタスク管理移行

### Step 2.1: BackgroundTaskManager 作成

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/service/BackgroundTaskManager.kt

interface BackgroundTaskManager {
    val isRunning: StateFlow<Boolean>
    val progress: StateFlow<TaskProgress?>
    fun taskStarted(taskId: String)
    fun taskFinished(taskId: String)
    fun updateProgress(progress: TaskProgress)
}

@Singleton
class BackgroundTaskManagerImpl @Inject constructor() : BackgroundTaskManager {
    private val runningTasks = mutableSetOf<String>()
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _progress = MutableStateFlow<TaskProgress?>(null)
    override val progress: StateFlow<TaskProgress?> = _progress.asStateFlow()

    @Synchronized
    override fun taskStarted(taskId: String) {
        runningTasks.add(taskId)
        _isRunning.value = true
        _progress.value = TaskProgress(taskId, TaskState.STARTING, "Starting...")
    }

    @Synchronized
    override fun taskFinished(taskId: String) {
        runningTasks.remove(taskId)
        _isRunning.value = runningTasks.isNotEmpty()
        if (runningTasks.isEmpty()) {
            _progress.value = null
        }
    }

    override fun updateProgress(progress: TaskProgress) {
        _progress.value = progress
    }
}
```

### Step 2.2: ServiceModule 作成

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/di/ServiceModule.kt

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindBackgroundTaskManager(
        impl: BackgroundTaskManagerImpl
    ): BackgroundTaskManager
}
```

### Step 2.3: RetrieveTransactionsTask 更新

```kotlin
// Before
Data.backgroundTaskStarted()
Data.backgroundTaskProgress.postValue(progress)
Data.backgroundTaskFinished()

// After (注入経由)
backgroundTaskManager.taskStarted(taskId)
backgroundTaskManager.updateProgress(TaskProgress(...))
backgroundTaskManager.taskFinished(taskId)
```

### Step 2.4: 検証

```bash
nix run .#verify

# 手動テスト
# - データ同期トリガー
# - 進捗インジケーター表示
# - 同期完了後のデータ更新
```

## Phase 3: 通貨フォーマット・UIステート移行

### Step 3.1: CurrencyFormatter 作成

```kotlin
// app/src/main/kotlin/net/ktnx/mobileledger/service/CurrencyFormatter.kt

@Singleton
class CurrencyFormatterImpl @Inject constructor() : CurrencyFormatter {
    private val _locale = MutableStateFlow(Locale.getDefault())
    override val locale: StateFlow<Locale> = _locale.asStateFlow()

    private val _config = MutableStateFlow(CurrencyFormatConfig.fromLocale(Locale.getDefault()))
    override val config: StateFlow<CurrencyFormatConfig> = _config.asStateFlow()

    override fun formatNumber(number: Float): String {
        val format = NumberFormat.getInstance(_locale.value)
        return format.format(number)
    }

    override fun refresh(locale: Locale) {
        _locale.value = locale
        _config.value = CurrencyFormatConfig.fromLocale(locale)
    }
    // ... 他のメソッド
}
```

### Step 3.2: App.kt 更新

```kotlin
// Before
Data.refreshCurrencyData(Locale.getDefault())

// After
@Inject lateinit var currencyFormatter: CurrencyFormatter

override fun onCreate() {
    super.onCreate()
    currencyFormatter.refresh(Locale.getDefault())
}
```

### Step 3.3: AppStateManager 削除

```bash
# 最終確認: Data/AppStateManager への参照がないことを確認
grep -r "Data\." app/src/main/kotlin/
grep -r "AppStateManager" app/src/main/kotlin/

# 参照がゼロなら削除
rm app/src/main/kotlin/net/ktnx/mobileledger/model/AppStateManager.kt
rm app/src/main/kotlin/net/ktnx/mobileledger/di/AppStateModule.kt
```

### Step 3.4: 最終検証

```bash
# 全テスト
nix run .#test

# ビルド・インストール
nix run .#verify

# 手動テスト（全機能）
# - アプリ起動
# - プロファイル切り替え
# - データ同期
# - 取引作成
# - テーマ変更
# - ロケール変更（設定アプリから）
```

## 成功基準チェックリスト

```bash
# SC-001: 静的参照がゼロ
grep -r "Data\." app/src/main/kotlin/ | wc -l  # → 0
grep -r "AppStateManager\." app/src/main/kotlin/ | wc -l  # → 0

# SC-002: 全テストパス
nix run .#test  # → All tests passed

# SC-005: ビルド成功
nix run .#build  # → BUILD SUCCESSFUL
```

## トラブルシューティング

### Hilt 関連エラー

```
error: [Hilt] Missing binding for BackgroundTaskManager
```

→ ServiceModule が @InstallIn されているか確認

### Flow 収集エラー

```
IllegalStateException: LifecycleOwner is not in a valid state
```

→ `repeatOnLifecycle` でライフサイクル対応の収集を使用

### ビルドエラー

```
Unresolved reference: Data
```

→ 移行完了前に AppStateManager を削除していないか確認

## 参考ドキュメント

- [spec.md](./spec.md) - 機能仕様
- [research.md](./research.md) - 技術調査結果
- [data-model.md](./data-model.md) - データモデル定義
- [contracts/](./contracts/) - サービスインターフェース
