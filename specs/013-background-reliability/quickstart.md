# Quickstart: バックグラウンド処理の信頼性改善

**Feature**: 013-background-reliability
**Date**: 2026-01-14

## 概要

このドキュメントは、013-background-reliability の実装を開始するための最小限の情報を提供します。

## 前提条件

- Kotlin 2.0.21、Coroutines 1.9.0
- Hilt 2.51.1 でのDI
- Room 2.4.2（既存）
- JUnit 4、MockK、Turbine（テスト）

## ディレクトリ構成

```text
app/src/main/kotlin/net/ktnx/mobileledger/
├── domain/
│   ├── model/
│   │   ├── SyncError.kt         # 新規作成
│   │   ├── SyncProgress.kt      # 新規作成
│   │   ├── SyncState.kt         # 新規作成
│   │   └── SyncResult.kt        # 新規作成
│   └── usecase/
│       ├── TransactionSyncer.kt # 新規作成
│       ├── VersionDetector.kt   # 新規作成
│       ├── ConfigBackup.kt      # 新規作成
│       └── DatabaseInitializer.kt # 新規作成

app/src/test/kotlin/net/ktnx/mobileledger/
└── fake/
    ├── FakeTransactionSyncer.kt # 新規作成
    ├── FakeVersionDetector.kt   # 新規作成
    ├── FakeConfigBackup.kt      # 新規作成
    └── FakeDatabaseInitializer.kt # 新規作成
```

## 最初に実装するもの

### 1. SyncError sealed class

```kotlin
// domain/model/SyncError.kt
sealed class SyncError {
    abstract val message: String
    abstract val isRetryable: Boolean

    data class NetworkError(
        override val message: String = "ネットワークに接続できません",
        val cause: Throwable? = null
    ) : SyncError() {
        override val isRetryable = true
    }

    data class TimeoutError(
        override val message: String = "サーバーが応答しません",
        val timeoutMs: Long = 30_000
    ) : SyncError() {
        override val isRetryable = true
    }

    // ... 他のエラー型
}
```

### 2. TransactionSyncer インターフェース

```kotlin
// domain/usecase/TransactionSyncer.kt
interface TransactionSyncer {
    fun sync(profile: Profile): Flow<SyncProgress>
    fun getLastResult(): SyncResult?
}
```

### 3. FakeTransactionSyncer（TDD用）

```kotlin
// test/fake/FakeTransactionSyncer.kt
class FakeTransactionSyncer : TransactionSyncer {
    var shouldSucceed = true
    var progressSteps = 5

    override fun sync(profile: Profile): Flow<SyncProgress> = flow {
        emit(SyncProgress.Starting())
        if (!shouldSucceed) throw SyncException(SyncError.NetworkError())
        for (i in 1..progressSteps) {
            emit(SyncProgress.Running(i, progressSteps, "処理中"))
        }
    }
}
```

## TDD サイクル

### Step 1: 失敗するテストを書く

```kotlin
class TransactionSyncerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `sync emits Starting then Running progress`() = runTest {
        val syncer = FakeTransactionSyncer()
        syncer.progressSteps = 3

        val emissions = syncer.sync(testProfile).toList()

        assertTrue(emissions[0] is SyncProgress.Starting)
        assertTrue(emissions[1] is SyncProgress.Running)
        assertEquals(1, (emissions[1] as SyncProgress.Running).current)
    }
}
```

### Step 2: テストを通す実装

```kotlin
override fun sync(profile: Profile): Flow<SyncProgress> = flow {
    emit(SyncProgress.Starting("接続中..."))
    // 実装を追加
}
```

### Step 3: リファクタリング

コードを改善し、テストが通り続けることを確認。

## 既存コードとの統合

### MainViewModel での使用

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val syncer: TransactionSyncer  // 新規追加
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun startSync(profile: Profile) {
        viewModelScope.launch {
            syncer.sync(profile)
                .onStart { _syncState.value = SyncState.InProgress(SyncProgress.Starting()) }
                .catch { e ->
                    val error = when (e) {
                        is SyncException -> e.syncError
                        else -> SyncError.UnknownError(cause = e)
                    }
                    _syncState.value = SyncState.Failed(error)
                }
                .collect { progress ->
                    _syncState.value = SyncState.InProgress(progress)
                }
        }
    }
}
```

### Hilt モジュール

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideTransactionSyncer(
        impl: TransactionSyncerImpl
    ): TransactionSyncer = impl
}
```

## 実行コマンド

```bash
# テスト実行
nix run .#test

# ビルド
nix run .#build

# フル検証（テスト → ビルド → インストール）
nix run .#verify
```

## 参考ファイル

| パターン | 既存の参考ファイル |
|----------|-------------------|
| インターフェース定義 | `domain/usecase/TransactionSender.kt` |
| 実装 | `domain/usecase/TransactionSenderImpl.kt` |
| Fake | `fake/FakeTransactionSender.kt` |
| ViewModel | `ui/main/MainViewModel.kt` |
| sealed class | `ui/profile/ProfileDetailUiState.kt` |

## 次のステップ

1. `SyncError.kt` を作成
2. `SyncProgress.kt` を作成
3. `TransactionSyncer.kt` インターフェースを作成
4. `FakeTransactionSyncer.kt` を作成
5. テストを書く
6. `TransactionSyncerImpl.kt` を実装（既存 Thread をラップ）
7. MainViewModel に統合
