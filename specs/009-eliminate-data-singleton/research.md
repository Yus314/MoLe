# Research: Data.kt シングルトンの廃止

**Date**: 2026-01-12
**Feature**: 009-eliminate-data-singleton

## 1. Executive Summary

### Decision: 段階的移行アプローチ

AppStateManager（Data.kt）シングルトンを 3 つの専用サービスに分解し、Hilt DI で管理する。

| コンポーネント | 移行先 | 理由 |
|---------------|--------|------|
| プロファイル管理 | ProfileRepository (既存) | 既に完全実装済み、currentProfile: StateFlow 公開済み |
| バックグラウンドタスク | BackgroundTaskManager (新規) | アプリケーションスコープで状態保持が必要 |
| 通貨/ロケール | CurrencyFormatter (新規) | ロケール依存の純粋なユーティリティ |
| UI ステート | AppStateService (新規) | ドロワー状態、更新情報など |

### Rationale

1. **既存 Repository の活用**: ProfileRepository は既に完全実装済みで、currentProfile を StateFlow で公開している
2. **責務の明確化**: 単一のシングルトンから責務別の小さなサービスへ分解
3. **テスト容易性**: 各サービスが独立してモック可能
4. **段階的移行**: 読み取り専用操作から開始してリスク軽減

### Alternatives Considered

| 代替案 | 却下理由 |
|--------|----------|
| 単一 AppStateService への統合 | シングルトンの問題を再現、テスト困難 |
| ViewModel への分散配置 | Activity 間での状態共有が困難 |
| GlobalScope での状態管理 | ライフサイクル管理が困難、メモリリークリスク |

---

## 2. Current State Analysis

### 2.1 AppStateManager の構造

**Location**: `app/src/main/kotlin/net/ktnx/mobileledger/model/AppStateManager.kt`

```kotlin
// Data は AppStateManager のエイリアス
@Deprecated(message = "Use AppStateManager directly")
typealias Data = AppStateManager
```

### 2.2 管理する状態カテゴリ

#### A. プロファイル関連 (既に @Deprecated)

| プロパティ/メソッド | 型 | 移行先 |
|-------------------|-----|--------|
| `profiles` | `LiveData<List<Profile>>` | ProfileRepository.getAllProfiles() |
| `profile` | `MutableLiveData<Profile?>` | ProfileRepository.currentProfile |
| `getProfile()` | `Profile?` | ProfileRepository.currentProfile.value |
| `setCurrentProfile()` | `void` | ProfileRepository.setCurrentProfile() |
| `postCurrentProfile()` | `void` | ProfileRepository.setCurrentProfile() |
| `observeProfile()` | `LiveData<Profile?>` | ProfileRepository.currentProfile |

**Status**: ProfileRepository に既に実装済み。後方互換性のため AppStateManager と同期している。

#### B. バックグラウンドタスク関連 (Active)

| プロパティ/メソッド | 型 | 用途 |
|-------------------|-----|------|
| `backgroundTasksRunning` | `MutableLiveData<Boolean>` | タスク実行中フラグ |
| `backgroundTaskProgress` | `MutableLiveData<Progress>` | 進捗情報 |
| `backgroundTaskStarted()` | `void` | タスク開始通知 |
| `backgroundTaskFinished()` | `void` | タスク完了通知 |

**Status**: 新規 BackgroundTaskManager サービスへ移行が必要。

#### C. ロケール/通貨フォーマット関連 (Active)

| プロパティ/メソッド | 型 | 用途 |
|-------------------|-----|------|
| `currencySymbolPosition` | `MutableLiveData<Position>` | 通貨記号位置 |
| `currencyGap` | `MutableLiveData<Boolean>` | 記号と数字の間のスペース |
| `locale` | `MutableLiveData<Locale>` | 現在のロケール |
| `getDecimalSeparator()` | `String` | ロケール依存の小数点区切り |
| `formatCurrency()` | `String` | 通貨フォーマット |
| `formatNumber()` | `String` | 数値フォーマット |
| `parseNumber()` | `Float` | 数値パース |
| `refreshCurrencyData()` | `void` | ロケール更新 |

**Status**: 新規 CurrencyFormatter サービスへ移行が必要。

#### D. UI ステート関連 (Active)

| プロパティ/メソッド | 型 | 用途 |
|-------------------|-----|------|
| `drawerOpen` | `MutableLiveData<Boolean>` | ドロワー開閉状態 |
| `lastUpdateDate` | `MutableLiveData<Date?>` | 最終同期日時 |
| `lastUpdateTransactionCount` | `MutableLiveData<Int>` | 取引数 |
| `lastUpdateAccountCount` | `MutableLiveData<Int>` | 表示アカウント数 |
| `lastUpdateTotalAccountCount` | `MutableLiveData<Int>` | 合計アカウント数 |
| `lastTransactionsUpdateText` | `MutableLiveData<String>` | 更新テキスト |
| `lastAccountsUpdateText` | `MutableLiveData<String>` | 更新テキスト |

**Status**: 新規 AppStateService またはドロワー状態は ViewModel へ移行検討。

---

## 3. Reference Analysis

### 3.1 ファイル別参照数

| ファイル | 参照数 | 主な使用パターン |
|---------|--------|-----------------|
| MainActivityCompose.kt | 46 | observeProfile, backgroundTask*, lastUpdate* |
| App.kt | 7 | getProfile (認証), refreshCurrencyData |
| RetrieveTransactionsTask.kt | 5 | backgroundTask*, lastUpdate* |
| ProfileThemedActivity.kt | 3 | observeProfile (テーマ) |
| BackupsActivity.kt | 2 | getProfile, observeProfile |
| NewTransactionActivityCompose.kt | 1 | observeProfile |
| ProfileDetailActivity.kt | 1 | profiles.value (テーマ色) |
| ProfileRepositoryImpl.kt | 1 | setCurrentProfile (後方互換性同期) |
| その他 | 散在 | 各種参照 |

### 3.2 MainActivityCompose の詳細分析

**最も影響の大きいファイル**で、以下のパターンを使用:

```kotlin
// 1. プロファイル監視
Data.observeProfile().observeAsState()  // → ProfileRepository.currentProfile
Data.profiles.observe()                  // → ProfileRepository.getAllProfiles()

// 2. バックグラウンドタスク監視
Data.backgroundTaskProgress.observe()    // → BackgroundTaskManager.progress
Data.backgroundTasksRunning.observe()    // → BackgroundTaskManager.isRunning

// 3. 更新情報
Data.lastUpdateDate.observe()            // → AppStateService.lastSyncInfo
Data.lastUpdateTransactionCount          // → AppStateService.lastSyncInfo
Data.lastTransactionsUpdateText          // → 廃止 (ViewModel で計算)

// 4. プロファイル操作
Data.getProfile()                        // → ProfileRepository.currentProfile.value
Data.setCurrentProfile(profile)          // → ProfileRepository.setCurrentProfile()
```

---

## 4. Existing Infrastructure

### 4.1 Repository パターン実装状況

| Repository | Interface | Impl | DI Module | Status |
|------------|-----------|------|-----------|--------|
| ProfileRepository | ✅ | ✅ | RepositoryModule | 完全実装 |
| TransactionRepository | ✅ | ✅ | RepositoryModule | 完全実装 |
| AccountRepository | ✅ | ✅ | RepositoryModule | 完全実装 |
| TemplateRepository | ✅ | ✅ | RepositoryModule | 完全実装 |
| CurrencyRepository | ✅ | ✅ | RepositoryModule | 完全実装 |
| OptionRepository | ✅ | ✅ | RepositoryModule | 完全実装 |

### 4.2 ProfileRepository の現状

```kotlin
interface ProfileRepository {
    val currentProfile: StateFlow<Profile?>           // ✅ 既に公開
    fun getAllProfiles(): Flow<List<Profile>>         // ✅ 既に公開
    suspend fun setCurrentProfile(profile: Profile?)  // ✅ 既に公開
    // ... 他の CRUD メソッド
}
```

**ProfileRepositoryImpl 内の後方互換性コード** (削除対象):

```kotlin
// line 63: 後方互換性のため AppStateManager と同期
AppStateManager.setCurrentProfile(profile)
```

### 4.3 Hilt DI モジュール

```kotlin
// DatabaseModule.kt - DAO 提供
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton fun provideDB(): DB
    @Provides fun provideProfileDAO(db: DB): ProfileDAO
    // ... 他の DAO
}

// RepositoryModule.kt - Repository 提供
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
    // ... 他の Repository
}

// AppStateModule.kt - AppStateManager 提供 (削除予定)
@Module
@InstallIn(SingletonComponent::class)
object AppStateModule {
    @Provides @Singleton fun provideAppStateManager(): AppStateManager
}
```

---

## 5. New Service Design

### 5.1 BackgroundTaskManager

**責務**: バックグラウンドタスクの状態管理とライフサイクル調整

```kotlin
interface BackgroundTaskManager {
    val isRunning: StateFlow<Boolean>
    val progress: StateFlow<TaskProgress?>

    fun taskStarted(taskId: String)
    fun taskFinished(taskId: String)
    fun updateProgress(progress: TaskProgress)
}

data class TaskProgress(
    val taskId: String,
    val state: TaskState,
    val message: String,
    val current: Int = 0,
    val total: Int = 0
)

enum class TaskState { STARTING, RUNNING, FINISHED, ERROR }
```

**スコープ**: `@Singleton` - Activity 遷移を超えて状態保持

### 5.2 CurrencyFormatter

**責務**: ロケール依存の通貨/数値フォーマット

```kotlin
interface CurrencyFormatter {
    val locale: StateFlow<Locale>
    val currencySymbolPosition: StateFlow<Currency.Position>
    val currencyGap: StateFlow<Boolean>

    fun formatCurrency(amount: Float): String
    fun formatNumber(number: Float): String
    fun parseNumber(str: String): Float
    fun getDecimalSeparator(): String
    fun refresh(locale: Locale)
}
```

**スコープ**: `@Singleton` - ロケール変更を全画面で反映

### 5.3 AppStateService

**責務**: UI レベルのアプリケーション状態管理

```kotlin
interface AppStateService {
    val lastSyncInfo: StateFlow<SyncInfo?>
    val drawerOpen: StateFlow<Boolean>

    fun updateSyncInfo(info: SyncInfo)
    fun setDrawerOpen(open: Boolean)
}

data class SyncInfo(
    val date: Date?,
    val transactionCount: Int,
    val accountCount: Int,
    val totalAccountCount: Int
)
```

**スコープ**: `@Singleton` - 全画面で共有

---

## 6. Migration Strategy Details

### 6.1 Phase 1: プロファイル管理移行

**目的**: Data.observeProfile() 系メソッドを ProfileRepository に置換

**変更対象ファイル**:
1. MainActivityCompose.kt - observeProfile → collectAsState
2. ProfileThemedActivity.kt - observeProfile → collectAsState
3. NewTransactionActivityCompose.kt - observeProfile → ViewModel 経由
4. BackupsActivity.kt - getProfile/observeProfile → ViewModel 経由
5. ProfileRepositoryImpl.kt - AppStateManager 同期コード削除

**変更パターン**:

```kotlin
// Before
Data.observeProfile().observeAsState()

// After
val profile by profileRepository.currentProfile.collectAsState()
// または ViewModel 経由
val profile by viewModel.currentProfile.collectAsState()
```

**検証**:
- プロファイル切り替えが動作する
- テーマカラーが正しく反映される
- 全画面で選択プロファイルが一致する

### 6.2 Phase 2: バックグラウンドタスク管理移行

**目的**: Data.backgroundTask* を BackgroundTaskManager に置換

**変更対象ファイル**:
1. BackgroundTaskManager.kt - 新規作成
2. ServiceModule.kt - DI 設定追加
3. RetrieveTransactionsTask.kt - BackgroundTaskManager 注入
4. MainActivityCompose.kt - BackgroundTaskManager 注入
5. MainViewModel.kt - BackgroundTaskManager 注入

**変更パターン**:

```kotlin
// Before (RetrieveTransactionsTask)
Data.backgroundTaskStarted()
Data.backgroundTaskProgress.postValue(progress)
Data.backgroundTaskFinished()

// After
backgroundTaskManager.taskStarted(taskId)
backgroundTaskManager.updateProgress(progress)
backgroundTaskManager.taskFinished(taskId)
```

**検証**:
- データ同期が正常に動作する
- 進捗インジケーターが表示される
- 同期完了後にデータが更新される

### 6.3 Phase 3: 通貨フォーマット・UIステート移行

**目的**: 残りの Data メソッドを専用サービスに置換

**変更対象ファイル**:
1. CurrencyFormatter.kt - 新規作成
2. AppStateService.kt - 新規作成
3. ServiceModule.kt - DI 設定追加
4. App.kt - CurrencyFormatter 注入
5. MainActivityCompose.kt - AppStateService 注入
6. AppStateManager.kt - **削除**
7. AppStateModule.kt - **削除**

**検証**:
- 通貨フォーマットが正しい
- ドロワーが正常に動作する
- `grep -r "Data\." app/src/` の結果が 0 件
- `grep -r "AppStateManager" app/src/` の結果が 0 件

---

## 7. Risk Analysis

### 7.1 高リスク項目

| リスク | 影響 | 軽減策 |
|--------|------|--------|
| MainActivityCompose の大規模変更 | 全機能に影響 | 段階的変更、各ステップでテスト |
| バックグラウンドタスクのタイミング問題 | データ不整合 | StateFlow で同期保証 |
| ロケール変更の反映遅延 | 表示不正 | Application.onConfigurationChanged で即時反映 |

### 7.2 中リスク項目

| リスク | 影響 | 軽減策 |
|--------|------|--------|
| 既存テストの破損 | CI 失敗 | 各フェーズでテスト実行 |
| DI グラフの複雑化 | ビルドエラー | ServiceModule で一元管理 |

### 7.3 低リスク項目

| リスク | 影響 | 軽減策 |
|--------|------|--------|
| パフォーマンス劣化 | UX 低下 | 起動時間計測、プロファイリング |

---

## 8. Testing Strategy

### 8.1 ユニットテスト

**新規サービスのテスト**:

```kotlin
@Test
fun `BackgroundTaskManager tracks running state correctly`() {
    val manager = BackgroundTaskManagerImpl()
    assertFalse(manager.isRunning.value)

    manager.taskStarted("task1")
    assertTrue(manager.isRunning.value)

    manager.taskFinished("task1")
    assertFalse(manager.isRunning.value)
}

@Test
fun `CurrencyFormatter formats according to locale`() {
    val formatter = CurrencyFormatterImpl()
    formatter.refresh(Locale.GERMANY)

    assertEquals("1.234,56", formatter.formatNumber(1234.56f))
}
```

### 8.2 統合テスト

**プロファイル切り替えの検証**:

```kotlin
@HiltAndroidTest
class ProfileSwitchingTest {
    @Test
    fun `switching profile updates all screens`() {
        // Given: Profile A selected
        // When: Switch to Profile B
        // Then: All screens show Profile B data
    }
}
```

### 8.3 手動検証チェックリスト

各フェーズ完了後:

- [ ] アプリ起動確認
- [ ] プロファイル切り替え
- [ ] データ同期トリガー
- [ ] 進捗表示確認
- [ ] 取引一覧表示
- [ ] 新規取引作成
- [ ] テーマカラー反映
- [ ] ロケール変更反映

---

## 9. Dependencies & Best Practices

### 9.1 Kotlin Coroutines/Flow ベストプラクティス

**使用パターン**:

```kotlin
// Repository → ViewModel
val profiles: StateFlow<List<Profile>> = profileRepository
    .getAllProfiles()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

// ViewModel → Compose
val profiles by viewModel.profiles.collectAsState()
```

**避けるべきパターン**:

```kotlin
// NG: GlobalScope
GlobalScope.launch { ... }

// NG: LiveData と Flow の混在
val liveData = MutableLiveData<T>()
flow.collect { liveData.postValue(it) }
```

### 9.2 Hilt DI ベストプラクティス

**推奨**:

```kotlin
// Service 注入
@Singleton
class BackgroundTaskManagerImpl @Inject constructor() : BackgroundTaskManager

// ViewModel 注入
@HiltViewModel
class MainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val backgroundTaskManager: BackgroundTaskManager,
    private val currencyFormatter: CurrencyFormatter
) : ViewModel()
```

**避けるべき**:

```kotlin
// NG: フィールドインジェクション
@Inject lateinit var manager: BackgroundTaskManager

// NG: 直接インスタンス化
val manager = BackgroundTaskManagerImpl()
```

---

## 10. Conclusion

AppStateManager シングルトンの廃止は、既存の Repository インフラを活用することで低リスクで実現可能。段階的移行アプローチと各フェーズでの検証チェックポイントにより、回帰を防ぎながら移行を完了できる。

**Key Success Factors**:
1. ProfileRepository が既に完全実装済みである
2. Hilt DI インフラが整っている
3. 段階的移行でリスク軽減
4. 各フェーズで検証チェックポイント実施
