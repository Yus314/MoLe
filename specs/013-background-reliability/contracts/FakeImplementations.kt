package net.ktnx.mobileledger.fake

/**
 * テスト用 Fake 実装パターン
 *
 * 各インターフェースのFake実装例。
 * ユニットテストで使用する。
 */

import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.domain.model.SyncError
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.domain.model.SyncResult
import net.ktnx.mobileledger.domain.usecase.ConfigBackup
import net.ktnx.mobileledger.domain.usecase.DatabaseInitializer
import net.ktnx.mobileledger.domain.usecase.TransactionSyncer
import net.ktnx.mobileledger.domain.usecase.VersionDetector
import net.ktnx.mobileledger.model.LedgerTransaction

// =============================================================================
// FakeTransactionSyncer
// =============================================================================

/**
 * TransactionSyncer のFake実装
 *
 * テストで使用するパターン:
 * 1. 即座に成功する
 * 2. 指定回数の進捗を報告してから成功する
 * 3. エラーで失敗する
 * 4. 遅延を伴う（キャンセルテスト用）
 */
class FakeTransactionSyncer : TransactionSyncer {

    // 設定可能なプロパティ
    var shouldSucceed: Boolean = true
    var progressSteps: Int = 5
    var delayPerStepMs: Long = 0
    var errorToThrow: SyncError? = null
    var result: SyncResult = SyncResult(
        transactionCount = 10,
        accountCount = 5,
        duration = 1000
    )

    // 呼び出し追跡
    var syncCallCount = 0
        private set
    var lastSyncedProfile: Profile? = null
        private set

    private var _lastResult: SyncResult? = null

    override fun sync(profile: Profile): Flow<SyncProgress> = flow {
        syncCallCount++
        lastSyncedProfile = profile

        emit(SyncProgress.Starting("接続中..."))

        if (delayPerStepMs > 0) {
            delay(delayPerStepMs)
        }

        if (!shouldSucceed) {
            val error = errorToThrow ?: SyncError.NetworkError()
            throw SyncException(error)
        }

        for (i in 1..progressSteps) {
            emit(
                SyncProgress.Running(
                    current = i,
                    total = progressSteps,
                    message = "処理中... ($i/$progressSteps)"
                )
            )
            if (delayPerStepMs > 0) {
                delay(delayPerStepMs)
            }
        }

        _lastResult = result
    }

    override fun getLastResult(): SyncResult? = _lastResult

    // テスト用ヘルパー
    fun reset() {
        syncCallCount = 0
        lastSyncedProfile = null
        _lastResult = null
        shouldSucceed = true
        errorToThrow = null
    }
}

/**
 * SyncError をラップする例外
 */
class SyncException(val syncError: SyncError) : Exception(syncError.message)

// =============================================================================
// FakeVersionDetector
// =============================================================================

/**
 * VersionDetector のFake実装
 */
class FakeVersionDetector : VersionDetector {

    var versionToReturn: String = "1.32"
    var shouldSucceed: Boolean = true
    var delayMs: Long = 0

    var detectCallCount = 0
        private set

    override suspend fun detect(url: String, useAuth: Boolean, user: String?, password: String?): Result<String> {
        detectCallCount++

        if (delayMs > 0) {
            delay(delayMs)
        }

        return if (shouldSucceed) {
            Result.success(versionToReturn)
        } else {
            Result.failure(Exception("Version detection failed"))
        }
    }

    fun reset() {
        detectCallCount = 0
        shouldSucceed = true
        versionToReturn = "1.32"
    }
}

// =============================================================================
// FakeConfigBackup
// =============================================================================

/**
 * ConfigBackup のFake実装
 */
class FakeConfigBackup : ConfigBackup {

    var shouldBackupSucceed: Boolean = true
    var shouldRestoreSucceed: Boolean = true
    var errorToThrow: Exception? = null
    var delayMs: Long = 0

    var backupCallCount = 0
        private set
    var restoreCallCount = 0
        private set
    var lastBackupUri: Uri? = null
        private set
    var lastRestoreUri: Uri? = null
        private set

    override suspend fun backup(uri: Uri): Result<Unit> {
        backupCallCount++
        lastBackupUri = uri

        if (delayMs > 0) {
            delay(delayMs)
        }

        return if (shouldBackupSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(errorToThrow ?: Exception("Backup failed"))
        }
    }

    override suspend fun restore(uri: Uri): Result<Unit> {
        restoreCallCount++
        lastRestoreUri = uri

        if (delayMs > 0) {
            delay(delayMs)
        }

        return if (shouldRestoreSucceed) {
            Result.success(Unit)
        } else {
            Result.failure(errorToThrow ?: Exception("Restore failed"))
        }
    }

    fun reset() {
        backupCallCount = 0
        restoreCallCount = 0
        lastBackupUri = null
        lastRestoreUri = null
        shouldBackupSucceed = true
        shouldRestoreSucceed = true
        errorToThrow = null
    }
}

// =============================================================================
// FakeDatabaseInitializer
// =============================================================================

/**
 * DatabaseInitializer のFake実装
 */
class FakeDatabaseInitializer : DatabaseInitializer {

    var hasProfiles: Boolean = true
    var shouldSucceed: Boolean = true
    var delayMs: Long = 0

    private var _isInitialized = false

    var initializeCallCount = 0
        private set

    override suspend fun initialize(): Result<Boolean> {
        initializeCallCount++

        if (delayMs > 0) {
            delay(delayMs)
        }

        return if (shouldSucceed) {
            _isInitialized = true
            Result.success(hasProfiles)
        } else {
            Result.failure(Exception("Database initialization failed"))
        }
    }

    override val isInitialized: Boolean
        get() = _isInitialized

    fun reset() {
        initializeCallCount = 0
        _isInitialized = false
        shouldSucceed = true
        hasProfiles = true
    }
}

// =============================================================================
// 使用例
// =============================================================================

/**
 * テストでの使用例
 *
 * ```kotlin
 * class MainViewModelTest {
 *     @get:Rule
 *     val mainDispatcherRule = MainDispatcherRule()
 *
 *     private lateinit var viewModel: MainViewModel
 *     private lateinit var fakeSyncer: FakeTransactionSyncer
 *
 *     @Before
 *     fun setup() {
 *         fakeSyncer = FakeTransactionSyncer()
 *         viewModel = MainViewModel(fakeSyncer, ...)
 *     }
 *
 *     @Test
 *     fun `sync success updates state`() = runTest {
 *         fakeSyncer.shouldSucceed = true
 *         fakeSyncer.result = SyncResult(100, 50, 2000)
 *
 *         viewModel.startSync(testProfile)
 *         advanceUntilIdle()
 *
 *         assertEquals(1, fakeSyncer.syncCallCount)
 *         assertTrue(viewModel.syncState.value is SyncState.Completed)
 *     }
 *
 *     @Test
 *     fun `sync failure shows error`() = runTest {
 *         fakeSyncer.shouldSucceed = false
 *         fakeSyncer.errorToThrow = SyncError.NetworkError()
 *
 *         viewModel.startSync(testProfile)
 *         advanceUntilIdle()
 *
 *         assertTrue(viewModel.syncState.value is SyncState.Failed)
 *     }
 *
 *     @Test
 *     fun `sync cancels within 500ms`() = runTest {
 *         fakeSyncer.delayPerStepMs = 1000
 *         fakeSyncer.progressSteps = 10
 *
 *         viewModel.startSync(testProfile)
 *         advanceTimeBy(100)
 *
 *         val cancelTime = measureTimeMillis {
 *             viewModel.cancelSync()
 *             advanceUntilIdle()
 *         }
 *
 *         assertTrue(cancelTime < 500)
 *     }
 * }
 * ```
 */
