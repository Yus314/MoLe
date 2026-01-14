# Research: バックグラウンド処理アーキテクチャ技術調査

**Feature**: 013-background-reliability
**Date**: 2026-01-14
**Status**: Complete

## 調査項目

1. runBlocking 除去パターン（RetrieveTransactionsTask内の使用）
2. OkHttp + suspendCancellableCoroutine の実装例
3. BackgroundTaskManagerとの進捗報告連携
4. 既存TransactionSender（011で定義済み）との一貫性確保

---

## 1. runBlocking 除去パターン

### Decision
suspendCancellableCoroutine + withContext(Dispatchers.IO) パターンを採用

### Rationale
- 既存の TransactionSenderImpl で実績がある
- Thread.interrupt() からの移行がスムーズ
- Repository の suspend 関数と直接連携可能

### Current Usage Locations

| ファイル | 行番号 | 用途 |
|---------|--------|------|
| RetrieveTransactionsTask.kt | 717-753 | AccountAndTransactionListSaver.run() |
| RawConfigWriter.kt | 79, 127, 143 | テンプレート/通貨/プロファイル取得 |
| RawConfigReader.kt | 258, 270, 282, 298 | DB操作 |

### Recommended Pattern

```kotlin
// Before: Thread内でrunBlocking
private inner class DataSaver : Thread() {
    override fun run() {
        runBlocking {
            accountRepository.storeAccounts(list, profileId)
        }
    }
}

// After: suspend関数として実装
suspend fun saveData(list: List<Account>, profileId: Long) =
    withContext(Dispatchers.IO) {
        accountRepository.storeAccounts(list, profileId)
    }
```

### Risks & Mitigations

| リスク | 対策 |
|--------|------|
| キャンセル応答遅延 | yield() / ensureActive() を定期挿入 |
| Dispatcher誤選択 | Dispatchers.IO を明示指定 |
| 二重 withContext | Repository側で既にwithContext済み、不要なネストを避ける |

### Alternatives Considered

| アプローチ | 長所 | 短所 | 採用 |
|------------|------|------|------|
| Interface-First Wrapper | 低リスク、即テスト可能、段階的 | 一時的に2実装を維持 | ✅ |
| 直接書き換え | 最終結果がクリーン | 高リスク、完了まで長期 | ❌ |
| 並列実装 | 動作比較可能 | 移行期間中メンテ2倍 | ❌ |

---

## 2. OkHttp + suspendCancellableCoroutine 実装パターン

### Decision
HttpURLConnection を suspendCancellableCoroutine でラップ（OkHttp移行は別機能）

### Rationale
- 既存の NetworkUtil.prepareConnection() を再利用
- HTTPクライアント変更のリスクを分離
- spec.md で OkHttp 継続を明記

### Implementation Pattern

```kotlin
suspend fun <T> httpRequest(
    profile: Profile,
    path: String,
    parser: (InputStream) -> T
): Result<T> = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine { continuation ->
        var connection: HttpURLConnection? = null

        continuation.invokeOnCancellation {
            connection?.disconnect()  // キャンセル時に接続切断
        }

        try {
            connection = NetworkUtil.prepareConnection(profile, path)
            val responseCode = connection.responseCode

            when {
                responseCode in 200..299 -> {
                    val result = connection.inputStream.use(parser)
                    continuation.resume(Result.success(result))
                }
                responseCode == 401 -> {
                    continuation.resume(Result.failure(AuthenticationError(responseCode)))
                }
                else -> {
                    continuation.resume(Result.failure(ServerError(responseCode)))
                }
            }
        } catch (e: SocketTimeoutException) {
            continuation.resume(Result.failure(TimeoutError()))
        } catch (e: IOException) {
            if (!continuation.isCancelled) {
                continuation.resume(Result.failure(NetworkError(e)))
            }
        } finally {
            connection?.disconnect()
        }
    }
}
```

### Cancellation Handling

```kotlin
continuation.invokeOnCancellation {
    connection?.disconnect()  // ネットワーク接続を即座に切断
}
```

### Timeout Configuration
- NetworkUtil.prepareConnection() で connectTimeout/readTimeout = 30秒 設定済み
- 必要に応じて withTimeoutOrNull() でラップ可能

---

## 3. BackgroundTaskManager との進捗報告連携

### Decision
既存 BackgroundTaskManager インターフェースを維持し、Flow<SyncProgress> で進捗を公開

### Current Interface (BackgroundTaskManager.kt:66-107)

```kotlin
interface BackgroundTaskManager {
    val isRunning: StateFlow<Boolean>
    val progress: StateFlow<TaskProgress?>
    val runningTaskCount: Int

    fun taskStarted(taskId: String)
    fun taskFinished(taskId: String)
    fun updateProgress(progress: TaskProgress)
}
```

### TaskProgress Structure (TaskProgress.kt:46-75)

```kotlin
data class TaskProgress(
    val taskId: String,
    val state: TaskState,
    val message: String,
    val current: Int = 0,
    val total: Int = 0
) {
    val progressFraction: Float
        get() = if (total > 0) current.coerceAtMost(total).toFloat() / total else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}
```

### Recommended Integration Pattern

```kotlin
interface TransactionSyncer {
    suspend fun sync(profile: Profile): Flow<SyncProgress>
}

sealed class SyncProgress {
    data class Starting(val taskId: String) : SyncProgress()
    data class InProgress(val taskId: String, val current: Int, val total: Int) : SyncProgress()
    data class Finished(val taskId: String, val error: String? = null) : SyncProgress()
}

class TransactionSyncerImpl @Inject constructor(
    private val backgroundTaskManager: BackgroundTaskManager
) : TransactionSyncer {
    override suspend fun sync(profile: Profile): Flow<SyncProgress> = flow {
        val taskId = "sync-${profile.id}-${System.currentTimeMillis()}"
        backgroundTaskManager.taskStarted(taskId)

        try {
            emit(SyncProgress.Starting(taskId))
            // 同期処理...
            emit(SyncProgress.Finished(taskId))
        } finally {
            backgroundTaskManager.taskFinished(taskId)
        }
    }.flowOn(Dispatchers.IO)
}
```

### Progress Update Frequency
- 現在: 各処理ステップで updateProgress() 呼び出し
- 推奨: 100件ごとにバッチ更新（UI応答性とパフォーマンスのバランス）

---

## 4. 既存 TransactionSender パターンの確認

### Interface Design (TransactionSender.kt:29-49)

```kotlin
interface TransactionSender {
    suspend fun send(
        profile: Profile,
        transaction: LedgerTransaction,
        simulate: Boolean = false
    ): Result<Unit>
}
```

### Implementation Pattern (TransactionSenderImpl.kt:42-62)

```kotlin
@Singleton
class TransactionSenderImpl @Inject constructor() : TransactionSender {
    override suspend fun send(
        profile: Profile,
        transaction: LedgerTransaction,
        simulate: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            val callback = TaskCallback { error, _ ->
                if (error == null) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(Exception(error)))
                }
            }

            val task = SendTransactionTask(callback, profile, transaction, simulate)

            continuation.invokeOnCancellation {
                task.interrupt()
            }

            task.start()
        }
    }
}
```

### Fake Implementation Pattern (FakeTransactionSender.kt:45-52)

```kotlin
class FakeTransactionSender : TransactionSender {
    var shouldSucceed = true
    var errorMessage = "Simulated failure"
    val sentTransactions = mutableListOf<SentTransaction>()

    override suspend fun send(...): Result<Unit> {
        sentTransactions.add(SentTransaction(profile, transaction, simulate))
        return if (shouldSucceed) Result.success(Unit)
               else Result.failure(Exception(errorMessage))
    }
}
```

### Applicability to Other Components

| コンポーネント | 現状 | 適用方法 |
|---------------|------|----------|
| RetrieveTransactionsTask | Thread, runBlocking | TransactionSyncer（同パターン） |
| ConfigReader/Writer | Thread, runBlocking | BackupService（同パターン） |
| VersionDetection | 埋め込み | VersionDetector インターフェース抽出 |
| DatabaseInit | Thread + LiveData | DatabaseInitializer（suspend化） |

---

## 5. 共通エラー型の設計

### Decision
sealed class で統一エラー型を定義

### Proposed Design: AsyncOperationError

```kotlin
sealed class AsyncOperationError : Exception() {
    abstract val messageResId: Int
    abstract val technicalDetails: String?
    abstract val isRetryable: Boolean

    data class NetworkError(
        val cause: IOException,
        override val messageResId: Int = R.string.error_network,
        override val technicalDetails: String? = cause.message
    ) : AsyncOperationError() {
        override val isRetryable = true
    }

    data class AuthenticationError(
        val statusCode: Int,
        override val messageResId: Int = R.string.error_auth,
        override val technicalDetails: String? = "HTTP $statusCode"
    ) : AsyncOperationError() {
        override val isRetryable = false
    }

    data class TimeoutError(
        override val messageResId: Int = R.string.error_timeout,
        override val technicalDetails: String? = null
    ) : AsyncOperationError() {
        override val isRetryable = true
    }

    data class ParseError(
        val cause: Exception,
        override val messageResId: Int = R.string.error_parse,
        override val technicalDetails: String? = cause.message
    ) : AsyncOperationError() {
        override val isRetryable = false
    }

    data class FileError(
        val cause: IOException,
        override val messageResId: Int = R.string.error_file,
        override val technicalDetails: String? = cause.message
    ) : AsyncOperationError() {
        override val isRetryable = false
    }

    data class ServerError(
        val httpCode: Int,
        val serverMessage: String?,
        override val messageResId: Int = R.string.error_server,
        override val technicalDetails: String? = "HTTP $httpCode: $serverMessage"
    ) : AsyncOperationError() {
        override val isRetryable = httpCode >= 500
    }
}
```

### Usage in Result

```kotlin
suspend fun sync(profile: Profile): Result<SyncResult> {
    return try {
        Result.success(SyncResult(...))
    } catch (e: IOException) {
        Result.failure(AsyncOperationError.NetworkError(e))
    } catch (e: SocketTimeoutException) {
        Result.failure(AsyncOperationError.TimeoutError())
    }
}
```

---

## 6. DI モジュール拡張

### Current Modules

| モジュール | 提供対象 |
|-----------|---------|
| DatabaseModule | DB, 全DAO |
| ServiceModule | BackgroundTaskManager, CurrencyFormatter, AppStateService |
| RepositoryModule | ProfileRepository, TransactionRepository, etc. |
| UseCaseModule | TransactionSender |

### Proposed Extension (UseCaseModule)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {
    @Binds
    @Singleton
    abstract fun bindTransactionSender(impl: TransactionSenderImpl): TransactionSender

    // 新規追加
    @Binds
    @Singleton
    abstract fun bindTransactionSyncer(impl: TransactionSyncerImpl): TransactionSyncer

    @Binds
    @Singleton
    abstract fun bindBackupService(impl: BackupServiceImpl): BackupService

    @Binds
    @Singleton
    abstract fun bindVersionDetector(impl: VersionDetectorImpl): VersionDetector

    @Binds
    @Singleton
    abstract fun bindDatabaseInitializer(impl: DatabaseInitializerImpl): DatabaseInitializer
}
```

---

## 7. キャンセル伝播パターン

### Decision
標準的な構造化並行性パターンを採用

### Implementation

```kotlin
// 1. ネットワークリクエストのキャンセル
continuation.invokeOnCancellation {
    connection?.disconnect()
}

// 2. 処理ループでのキャンセルチェック
while (parser.hasNext()) {
    yield()  // キャンセルチェック + コンテキストスイッチ
    results.add(parser.next())
}

// 3. バッチ処理でのキャンセルチェック
transactions.chunked(100).forEach { batch ->
    ensureActive()  // バッチ間でキャンセルチェック
    transactionDao.insertAll(batch)
}
```

### Cancellation Response Time Target
- 目標: 5秒以内（spec.md FR-006）
- 達成方法: yield()/ensureActive() + invokeOnCancellation

---

## 8. Key File References

| ファイル | 行番号 | 内容 |
|---------|--------|------|
| RetrieveTransactionsTask.kt | 717 | runBlocking 使用箇所 |
| TransactionSenderImpl.kt | 44-60 | suspendCancellableCoroutine パターン |
| BackgroundTaskManager.kt | 66-107 | 進捗管理インターフェース |
| TaskProgress.kt | 46-75 | 進捗データ構造 |
| NetworkUtil.kt | 30-51 | HTTP接続準備 |
| FakeTransactionSender.kt | 45-52 | Fake実装パターン |

---

## まとめ

### 採用パターン

| 項目 | 採用パターン |
|------|-------------|
| 移行方式 | Interface-First Abstraction（ラップ後段階的移行） |
| ネットワーク | HttpURLConnection + suspendCancellableCoroutine |
| エラー型 | AsyncOperationError sealed class + Result<T> |
| キャンセル | 構造化並行性 + invokeOnCancellation |
| 進捗報告 | Flow<SyncProgress> + BackgroundTaskManager統合 |

### 次のステップ

1. **Phase 1 Design**: data-model.md で AsyncOperationError, SyncProgress, SyncResult を定義
2. **Phase 1 Design**: contracts/ で 4 つの新規インターフェースを定義
3. **Phase 1 Design**: quickstart.md で開発者向けガイドを作成
