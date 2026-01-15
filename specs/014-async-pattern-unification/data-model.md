# Data Model: 非同期処理パターンの統一

**Feature Branch**: `014-async-pattern-unification`
**Date**: 2026-01-15

## 1. Key Entities

### 1.1 処理状態 (Task State)

バックグラウンド処理の現在の状態を表す。

```kotlin
/**
 * バックグラウンド処理の状態を表すsealed class
 * 単方向データフロー: Pending -> Running -> (Completed | Cancelled | Error)
 */
sealed class TaskState {
    /** 開始前（初期状態） */
    object Pending : TaskState()

    /** 実行中（進捗情報を含む） */
    data class Running(val progress: Progress) : TaskState()

    /** 正常完了 */
    object Completed : TaskState()

    /** キャンセル済み */
    object Cancelled : TaskState()

    /** エラー発生 */
    data class Error(val message: String, val cause: Throwable? = null) : TaskState()
}
```

### 1.2 進捗情報 (Progress)

バックグラウンド処理の進捗状況を表す。

```kotlin
/**
 * 進捗情報（イミュータブル）
 * @param current 現在の処理数
 * @param total 総処理数（未知の場合はnull）
 * @param message 現在の処理内容を示すメッセージ
 */
data class Progress(
    val current: Int = 0,
    val total: Int? = null,
    val message: String = ""
) {
    /**
     * 進捗率（0.0〜1.0、totalが未知の場合はnull）
     */
    val percentage: Float?
        get() = total?.let { if (it > 0) current.toFloat() / it else null }
}
```

### 1.3 処理結果 (Result)

Kotlin標準の`Result<T>`を使用。追加のラッパーは作成しない。

```kotlin
// 使用例
suspend fun sync(profile: Profile): Result<SyncResult>
suspend fun send(transaction: LedgerTransaction): Result<Unit>
suspend fun backup(uri: Uri): Result<Unit>
```

---

## 2. UseCase Interface Patterns

### 2.1 単純な処理（結果のみ）

進捗レポートが不要な処理向け。

```kotlin
interface SimpleUseCase<in P, out R> {
    /**
     * 処理を実行
     * @param params 入力パラメータ
     * @return Result<R> 成功時はデータ、失敗時はエラー情報
     * @throws CancellationException キャンセル時
     */
    suspend operator fun invoke(params: P): Result<R>
}
```

**適用コンポーネント**:
- VersionDetector: `suspend fun detect(url: String, ...): Result<String>`
- DatabaseInitializer: `suspend fun initialize(): Result<Boolean>`
- TransactionSender: `suspend fun send(...): Result<Unit>`
- ConfigBackup: `suspend fun backup(uri: Uri): Result<Unit>`

### 2.2 ストリーミング処理（進捗あり）

進捗レポートが必要な処理向け。

```kotlin
interface StreamingUseCase<in P, out S> {
    /**
     * 処理を実行し、進捗をFlowで通知
     * @param params 入力パラメータ
     * @return Flow<S> 進捗状態のストリーム
     */
    operator fun invoke(params: P): Flow<S>
}
```

**適用コンポーネント**:
- TransactionSyncer: `fun sync(profile: Profile): Flow<SyncProgress>`

---

## 3. 状態遷移図

### 3.1 基本状態遷移

```
┌─────────┐     start()     ┌─────────┐
│ Pending │────────────────►│ Running │
└─────────┘                 └────┬────┘
                                 │
            ┌────────────────────┼────────────────────┐
            │                    │                    │
            ▼                    ▼                    ▼
      ┌───────────┐       ┌───────────┐       ┌───────────┐
      │ Completed │       │ Cancelled │       │   Error   │
      └───────────┘       └───────────┘       └───────────┘
```

### 3.2 TransactionSyncer 詳細状態遷移

```
Pending
    │
    │ sync() called
    ▼
Running(Connecting)
    │
    │ HTTP connection established
    ▼
Running(Downloading, current=0)
    │
    │ progress updates (current++, total)
    │ [loop until complete]
    ▼
Running(Parsing)
    │
    │ JSON parsing
    ▼
Running(Saving)
    │
    │ Repository.save()
    ▼
Completed ─────────── or ─────────── Error(message)
    │                                    │
    │ on cancel at any point             │
    └──────────► Cancelled ◄─────────────┘
```

---

## 4. 同期進捗 (SyncProgress)

TransactionSyncer専用の進捗情報。

```kotlin
/**
 * データ同期の進捗状態
 */
sealed class SyncProgress {
    /** 接続中 */
    object Connecting : SyncProgress()

    /** ダウンロード中 */
    data class Downloading(val progress: Progress) : SyncProgress()

    /** パース中 */
    object Parsing : SyncProgress()

    /** 保存中 */
    object Saving : SyncProgress()

    /** 完了 */
    data class Completed(val result: SyncResult) : SyncProgress()

    /** エラー */
    data class Error(val message: String, val cause: Throwable? = null) : SyncProgress()
}

/**
 * 同期結果
 */
data class SyncResult(
    val accountCount: Int,
    val transactionCount: Int,
    val duration: Long // milliseconds
)
```

---

## 5. エラー分類

### 5.1 エラー型の階層

```kotlin
/**
 * バックグラウンド処理のエラー基底クラス
 */
sealed class BackgroundTaskException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /** ネットワーク関連エラー */
    class NetworkException(
        message: String,
        cause: Throwable? = null
    ) : BackgroundTaskException(message, cause)

    /** 認証エラー */
    class AuthenticationException(
        message: String
    ) : BackgroundTaskException(message)

    /** サーバーエラー（4xx, 5xx） */
    class ServerException(
        val statusCode: Int,
        message: String
    ) : BackgroundTaskException(message)

    /** パースエラー */
    class ParseException(
        message: String,
        cause: Throwable? = null
    ) : BackgroundTaskException(message, cause)

    /** ファイルI/Oエラー */
    class FileException(
        message: String,
        cause: Throwable? = null
    ) : BackgroundTaskException(message, cause)
}
```

### 5.2 エラーハンドリングパターン

```kotlin
// ViewModel でのエラーハンドリング
viewModelScope.launch {
    syncer.sync(profile).collect { progress ->
        when (progress) {
            is SyncProgress.Error -> {
                when (progress.cause) {
                    is BackgroundTaskException.NetworkException ->
                        showError("ネットワーク接続を確認してください")
                    is BackgroundTaskException.AuthenticationException ->
                        navigateToLogin()
                    else ->
                        showError(progress.message)
                }
            }
            else -> updateProgress(progress)
        }
    }
}
```

---

## 6. キャンセルサポート

### 6.1 Coroutines キャンセルパターン

```kotlin
// UseCase実装でのキャンセル対応
suspend fun sync(profile: Profile): Flow<SyncProgress> = flow {
    emit(SyncProgress.Connecting)

    // キャンセルポイント: ensureActive() または yield()
    ensureActive()

    val connection = withContext(Dispatchers.IO) {
        NetworkUtil.prepareConnection(profile.url)
    }

    // HTTPレスポンス処理中もキャンセルを確認
    connection.inputStream.bufferedReader().useLines { lines ->
        lines.forEachIndexed { index, line ->
            ensureActive() // 各行処理前にキャンセル確認
            processLine(line)
            emit(SyncProgress.Downloading(Progress(current = index)))
        }
    }

    emit(SyncProgress.Completed(result))
}.flowOn(Dispatchers.IO)
```

### 6.2 ViewModelでのキャンセル

```kotlin
class MainViewModel @Inject constructor(
    private val syncer: TransactionSyncer
) : ViewModel() {

    private var syncJob: Job? = null

    fun startSync(profile: Profile) {
        // 既存のジョブをキャンセル
        syncJob?.cancel()

        syncJob = viewModelScope.launch {
            syncer.sync(profile).collect { progress ->
                _syncState.value = progress
            }
        }
    }

    fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        _syncState.value = SyncProgress.Cancelled
    }
}
```

---

## 7. Dispatcher 構成

### 7.1 標準Dispatcher使用

```kotlin
// I/O操作: Dispatchers.IO
suspend fun readFromNetwork(): String = withContext(Dispatchers.IO) {
    connection.inputStream.bufferedReader().readText()
}

// CPU集約処理: Dispatchers.Default
suspend fun parseJson(json: String): List<Transaction> = withContext(Dispatchers.Default) {
    parser.parse(json)
}

// UI更新: Dispatchers.Main (自動、viewModelScope使用時)
viewModelScope.launch {
    _uiState.value = newState // Main dispatcher
}
```

### 7.2 テスト用Dispatcher注入

```kotlin
// UseCase実装（Dispatcher注入可能）
class TransactionSyncerImpl @Inject constructor(
    private val repository: TransactionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TransactionSyncer {

    override fun sync(profile: Profile): Flow<SyncProgress> = flow {
        // ...
    }.flowOn(ioDispatcher)
}

// テスト
@Test
fun `sync emits correct progress`() = runTest {
    val syncer = TransactionSyncerImpl(
        repository = fakeRepository,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler)
    )

    syncer.sync(profile).test {
        assertEquals(SyncProgress.Connecting, awaitItem())
        // ...
    }
}
```

---

## 8. 既存エンティティとの関係

本機能では新しいデータベースエンティティは追加しない。
既存のRoom エンティティ（Profile, LedgerTransaction, Account等）は変更なし。

```
┌──────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ViewModel ─────── StateFlow<SyncProgress> ──────► Compose   │
└───────────────────────────────┬──────────────────────────────┘
                                │
                                │ Flow<SyncProgress>
                                ▼
┌──────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  TransactionSyncer ─── suspend/Flow ───► TransactionSender   │
│  ConfigBackup ──────── suspend/Flow ───► VersionDetector     │
└───────────────────────────────┬──────────────────────────────┘
                                │
                                │ suspend functions
                                ▼
┌──────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  Repository ────── Flow<List<Entity>> ────► Room Database    │
└──────────────────────────────────────────────────────────────┘
```

---

## 9. バリデーションルール

### 9.1 進捗情報のバリデーション

```kotlin
data class Progress(
    val current: Int = 0,
    val total: Int? = null,
    val message: String = ""
) {
    init {
        require(current >= 0) { "current must be non-negative" }
        require(total == null || total >= 0) { "total must be non-negative if specified" }
        require(total == null || current <= total) { "current must not exceed total" }
    }
}
```

### 9.2 状態遷移のバリデーション

終端状態（Completed, Cancelled, Error）からの遷移は許可しない。
これはsealed classの設計により、コンパイル時に保証される。
