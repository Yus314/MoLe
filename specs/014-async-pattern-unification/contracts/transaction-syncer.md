# Contract: TransactionSyncer

**Component**: データ同期 (Data Synchronization)
**Priority**: P5 (最も複雑、最後に移行)
**Current Status**: ラッパー実装（callbackFlow経由でRetrieveTransactionsTaskを使用）

## Interface Definition

```kotlin
package net.ktnx.mobileledger.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.model.Profile

/**
 * サーバーからトランザクションとアカウントを同期するUseCase
 *
 * 実装要件:
 * - Flowで進捗状態をストリーミング
 * - キャンセル対応（CancellationException）
 * - エラーはSyncProgress.Errorで通知
 */
interface TransactionSyncer {
    /**
     * 指定されたプロファイルのサーバーと同期を実行
     *
     * @param profile 同期対象のプロファイル（URL、認証情報を含む）
     * @return Flow<SyncProgress> 同期進捗のストリーム
     *
     * Flow emission順序:
     * 1. SyncProgress.Connecting - 接続開始
     * 2. SyncProgress.Downloading(progress) - ダウンロード中（複数回emit）
     * 3. SyncProgress.Parsing - パース中
     * 4. SyncProgress.Saving - データベース保存中
     * 5. SyncProgress.Completed(result) または SyncProgress.Error
     *
     * キャンセル時: Flowのcollectorがキャンセルされると処理中断
     */
    fun sync(profile: Profile): Flow<SyncProgress>
}
```

## Progress States

```kotlin
/**
 * データ同期の進捗状態
 */
sealed class SyncProgress {
    /** サーバーへの接続中 */
    object Connecting : SyncProgress()

    /** データダウンロード中 */
    data class Downloading(val progress: Progress) : SyncProgress()

    /** JSONパース中 */
    object Parsing : SyncProgress()

    /** データベースへの保存中 */
    object Saving : SyncProgress()

    /** 同期完了 */
    data class Completed(val result: SyncResult) : SyncProgress()

    /** エラー発生 */
    data class Error(val message: String, val cause: Throwable? = null) : SyncProgress()
}

data class SyncResult(
    val accountCount: Int,
    val transactionCount: Int,
    val duration: Long // milliseconds
)
```

## Implementation Requirements

### Required Dependencies

```kotlin
@Singleton
class TransactionSyncerImpl @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val optionRepository: OptionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSyncer
```

### Cancellation Support

```kotlin
override fun sync(profile: Profile): Flow<SyncProgress> = flow {
    emit(SyncProgress.Connecting)

    val connection = withContext(ioDispatcher) {
        // ensureActive() for cancellation check
        ensureActive()
        NetworkUtil.prepareConnection(profile.url, profile.useAuthentication)
    }

    // Process response with cancellation checks
    connection.inputStream.bufferedReader().useLines { lines ->
        lines.forEachIndexed { index, line ->
            ensureActive() // Check cancellation per line
            processLine(line)
        }
    }
}.flowOn(ioDispatcher)
```

### Error Handling

```kotlin
// Wrap network errors
try {
    connection = NetworkUtil.prepareConnection(...)
} catch (e: IOException) {
    emit(SyncProgress.Error("Network error", BackgroundTaskException.NetworkException(e.message ?: "", e)))
    return@flow
}

// Wrap parse errors
try {
    val transactions = parseTransactions(json)
} catch (e: JsonParseException) {
    emit(SyncProgress.Error("Parse error", BackgroundTaskException.ParseException(e.message ?: "", e)))
    return@flow
}
```

## Test Scenarios

### Success Path

```kotlin
@Test
fun `sync emits correct progress sequence on success`() = runTest {
    val syncer = TransactionSyncerImpl(
        fakeAccountRepository,
        fakeTransactionRepository,
        fakeOptionRepository,
        UnconfinedTestDispatcher(testScheduler)
    )

    syncer.sync(testProfile).test {
        assertEquals(SyncProgress.Connecting, awaitItem())
        assertTrue(awaitItem() is SyncProgress.Downloading)
        assertEquals(SyncProgress.Parsing, awaitItem())
        assertEquals(SyncProgress.Saving, awaitItem())
        val completed = awaitItem() as SyncProgress.Completed
        assertTrue(completed.result.transactionCount > 0)
        awaitComplete()
    }
}
```

### Error Path

```kotlin
@Test
fun `sync emits error on network failure`() = runTest {
    fakeAccountRepository.shouldThrowNetworkError = true

    syncer.sync(testProfile).test {
        assertEquals(SyncProgress.Connecting, awaitItem())
        val error = awaitItem() as SyncProgress.Error
        assertTrue(error.cause is BackgroundTaskException.NetworkException)
        awaitComplete()
    }
}
```

### Cancellation Path

```kotlin
@Test
fun `sync stops on cancellation`() = runTest {
    val syncer = TransactionSyncerImpl(/* slow dependencies */)

    val job = launch {
        syncer.sync(testProfile).collect { /* collecting */ }
    }

    // Cancel after first emission
    advanceTimeBy(100)
    job.cancel()

    // Verify cleanup (no dangling connections)
    assertTrue(job.isCancelled)
}
```

## Migration Notes

### Current Implementation (to be replaced)

`TransactionSyncerImpl` currently wraps `RetrieveTransactionsTask` using `callbackFlow`:

```kotlin
// BEFORE (current - to be replaced)
override fun sync(profile: Profile): Flow<SyncProgress> = callbackFlow {
    val taskManager = BackgroundTaskManager()
    val task = RetrieveTransactionsTask(...)
    task.start()
    // Complex wrapper thread for join()...
}
```

### Target Implementation

Pure Coroutines without Thread wrapper:

```kotlin
// AFTER (target)
override fun sync(profile: Profile): Flow<SyncProgress> = flow {
    emit(SyncProgress.Connecting)
    withContext(ioDispatcher) {
        val connection = NetworkUtil.prepareConnection(profile.url)
        // Direct HTTP processing
        // Direct JSON parsing
        // Direct repository save
    }
    emit(SyncProgress.Completed(result))
}.flowOn(ioDispatcher)
```

### Files to Remove After Migration

- `async/RetrieveTransactionsTask.kt` (813 lines)
- References in `ui/main/MainCoordinatorViewModel.kt`
- References in `json/AccountListParser.kt`
- References in `json/TransactionListParser.kt`
