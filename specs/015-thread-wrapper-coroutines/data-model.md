# Data Model: Thread Wrapper to Coroutines Migration

**Date**: 2026-01-15
**Feature**: 015-thread-wrapper-coroutines

## 1. Core Interfaces (Existing - No Changes)

### 1.1 TransactionSyncer

**File**: `app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSyncer.kt`

```kotlin
interface TransactionSyncer {
    /**
     * プロファイルの取引を同期する
     * @param profile 同期対象のプロファイル
     * @return Flow<SyncProgress> 進捗を報告
     */
    fun sync(profile: Profile): Flow<SyncProgress>

    /**
     * 最後の同期結果を取得
     * @return 最後の同期結果、未実行の場合は null
     */
    fun getLastResult(): SyncResult?
}
```

**Status**: インターフェース変更なし

### 1.2 TransactionSender

**File**: `app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/TransactionSender.kt`

```kotlin
interface TransactionSender {
    /**
     * 取引をサーバーに送信する
     * @param profile 送信先のプロファイル
     * @param transaction 送信する取引
     * @param simulate シミュレーションモード
     * @return 成功時は Result.success(Unit)、失敗時は Result.failure(Exception)
     */
    suspend fun send(profile: Profile, transaction: LedgerTransaction, simulate: Boolean): Result<Unit>
}
```

**Status**: インターフェース変更なし

### 1.3 ConfigBackup

**File**: `app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/ConfigBackup.kt`

```kotlin
interface ConfigBackup {
    /**
     * 設定をバックアップする
     * @param uri バックアップ先のURI
     * @return 成功時は Result.success(Unit)
     */
    suspend fun backup(uri: Uri): Result<Unit>

    /**
     * 設定をリストアする
     * @param uri リストア元のURI
     * @return 成功時は Result.success(Unit)
     */
    suspend fun restore(uri: Uri): Result<Unit>
}
```

**Status**: インターフェース変更なし

### 1.4 VersionDetector

**File**: `app/src/main/kotlin/net/ktnx/mobileledger/domain/usecase/VersionDetector.kt`

```kotlin
interface VersionDetector {
    /**
     * hledger-web のバージョンを検出する
     * @return 成功時はバージョン文字列、失敗時は Result.failure()
     */
    suspend fun detect(url: String, useAuth: Boolean, user: String?, password: String?): Result<String>

    /**
     * プロファイルからバージョンを検出する（便利メソッド）
     */
    suspend fun detect(profile: Profile): Result<String>
}
```

**Status**: インターフェース変更なし

## 2. Domain Models (Existing - No Changes)

### 2.1 SyncProgress

**File**: `app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SyncProgress.kt`

```kotlin
sealed class SyncProgress {
    data class Starting(val message: String) : SyncProgress()
    data class Running(val current: Int, val total: Int, val message: String) : SyncProgress()
    data class Indeterminate(val message: String) : SyncProgress()
}
```

### 2.2 SyncResult

**File**: `app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SyncResult.kt`

```kotlin
data class SyncResult(
    val transactionCount: Int,
    val accountCount: Int,
    val duration: Long
)
```

### 2.3 SyncError

**File**: `app/src/main/kotlin/net/ktnx/mobileledger/domain/model/SyncError.kt`

```kotlin
sealed class SyncError {
    data class NetworkError(val message: String = "ネットワークエラー", val cause: Throwable? = null) : SyncError()
    data class ServerError(val message: String, val httpCode: Int) : SyncError()
    data class AuthenticationError(val message: String, val httpCode: Int) : SyncError()
    data class ParseError(val message: String, val cause: Throwable? = null) : SyncError()
    data class ValidationError(val message: String) : SyncError()
    data class TimeoutError(val message: String) : SyncError()
    data class ApiVersionError(val message: String) : SyncError()
    data class UnknownError(val message: String, val cause: Throwable? = null) : SyncError()
    object Cancelled : SyncError()
}
```

## 3. Implementation Classes (Modifications Required)

### 3.1 TransactionSyncerImpl (MAJOR REFACTOR)

**Current**: Thread wrapper with `suspendCancellableCoroutine`
**Target**: Pure suspend function implementation

```kotlin
@Singleton
class TransactionSyncerImpl @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val optionRepository: OptionRepository,
    private val backgroundTaskManager: BackgroundTaskManager,
    private val appStateService: AppStateService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSyncer {

    override fun sync(profile: Profile): Flow<SyncProgress> = flow {
        emit(SyncProgress.Starting("接続中..."))

        withContext(ioDispatcher) {
            // Network I/O
            val accounts = fetchAccounts(profile)
            val transactions = fetchTransactions(profile)

            // Database operations (already suspend)
            saveAccountsAndTransactions(accounts, transactions)
        }
    }

    private suspend fun fetchAccounts(profile: Profile): List<Account> {
        ensureActive()
        // HTTP request logic (extracted from RetrieveTransactionsTask)
    }

    private suspend fun fetchTransactions(profile: Profile): List<Transaction> {
        ensureActive()
        // HTTP request logic (extracted from RetrieveTransactionsTask)
    }
}
```

### 3.2 TransactionSenderImpl (MEDIUM REFACTOR)

**Current**: Thread wrapper with callback
**Target**: Pure suspend function implementation

```kotlin
@Singleton
class TransactionSenderImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSender {

    override suspend fun send(
        profile: Profile,
        transaction: LedgerTransaction,
        simulate: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (simulate) {
                delay(1500) // TestDispatcher で即座にスキップ可能
            }

            // HTTP POST logic (extracted from SendTransactionTask)
            val response = postTransaction(profile, transaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 3.3 ConfigBackupImpl (MEDIUM REFACTOR)

**Current**: Thread wrapper
**Target**: Pure suspend function implementation

```kotlin
@Singleton
class ConfigBackupImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val templateRepository: TemplateRepository,
    private val currencyRepository: CurrencyRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ConfigBackup {

    override suspend fun backup(uri: Uri): Result<Unit> = withContext(ioDispatcher) {
        try {
            val profiles = profileRepository.getAllProfilesSync()
            val templates = templateRepository.getAllTemplatesWithAccountsSync()
            val currencies = currencyRepository.getAllCurrenciesSync()

            // Write to file (no runBlocking needed)
            writeConfig(uri, profiles, templates, currencies)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## 4. Entities to Delete

### 4.1 Thread Subclasses

| File | Class | Reason |
|------|-------|--------|
| `async/RetrieveTransactionsTask.kt` | RetrieveTransactionsTask | Logic merged into TransactionSyncerImpl |
| `async/SendTransactionTask.kt` | SendTransactionTask | Logic merged into TransactionSenderImpl |
| `backup/ConfigIO.kt` | ConfigIO | Base class no longer needed |
| `backup/ConfigWriter.kt` | ConfigWriter (Thread部分) | Logic merged into ConfigBackupImpl |
| `backup/ConfigReader.kt` | ConfigReader (Thread部分) | Logic merged into ConfigBackupImpl |

### 4.2 Callback Interfaces

| File | Interface | Reason |
|------|-----------|--------|
| `async/TaskCallback.kt` | TaskCallback | Replaced by suspend function return |
| `dao/AsyncResultCallback.kt` | AsyncResultCallback | Replaced by suspend function return |

### 4.3 Executor Patterns

| File | Component | Reason |
|------|-----------|--------|
| `dao/BaseDAO.kt` | asyncRunner | Use suspend DAO methods directly |
| `async/GeneralBackgroundTasks.kt` | runner | Use viewModelScope.launch |

## 5. ViewModel Thread Replacements

### 5.1 TransactionListViewModel

**Current**: `TransactionsDisplayedFilter` (Thread subclass)
**Target**: `viewModelScope.launch` with Flow

```kotlin
private fun updateDisplayedTransactions() {
    filterJob?.cancel()
    filterJob = viewModelScope.launch(Dispatchers.Default) {
        val filtered = transactions.filter { tx ->
            ensureActive()
            matchesFilter(tx)
        }
        _displayedTransactions.value = filtered
    }
}
```

### 5.2 ProfileDetailModel

**Current**: `VersionDetectionThread` (Thread subclass)
**Target**: `viewModelScope.launch` with `VersionDetector`

```kotlin
fun detectVersion() {
    detectionJob?.cancel()
    detectionJob = viewModelScope.launch {
        _detectedVersion.value = VersionState.Detecting
        versionDetector.detect(url, useAuth, user, password)
            .onSuccess { version -> _detectedVersion.value = VersionState.Detected(version) }
            .onFailure { _detectedVersion.value = VersionState.Error }
    }
}
```

## 6. Validation Rules

### 6.1 Interface Compatibility

- 全ての既存インターフェース（TransactionSyncer, TransactionSender, ConfigBackup, VersionDetector）のシグネチャは変更しない
- 既存の Fake 実装（FakeTransactionSyncer, FakeTransactionSender, FakeConfigBackup, FakeVersionDetector）はそのまま動作する

### 6.2 Testability Requirements

- 全ての実装クラスは `CoroutineDispatcher` を注入可能
- `TestDispatcher` を使用したテストで `advanceUntilIdle()` により即座に完了
- `runBlocking` はプロダクションコードで使用禁止（テストコードのみ許可）
