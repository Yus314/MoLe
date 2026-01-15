/**
 * UseCase Contracts for Thread to Coroutines Migration
 *
 * このファイルは移行後のインターフェースと実装のコントラクトを定義する。
 * 既存のインターフェースは変更せず、実装クラスの内部構造のみ変更する。
 *
 * Date: 2026-01-15
 * Feature: 015-thread-wrapper-coroutines
 */

// =============================================================================
// INTERFACES (NO CHANGES - for reference only)
// =============================================================================

/**
 * TransactionSyncer - 取引同期インターフェース
 *
 * Contract:
 * - sync() は Flow<SyncProgress> を返す
 * - Flow は Starting → Running(複数) → close の順で emit
 * - キャンセル時は CancellationException をスロー
 * - エラー時は SyncException をスロー
 */
interface TransactionSyncer {
    fun sync(profile: Profile): Flow<SyncProgress>
    fun getLastResult(): SyncResult?
}

/**
 * TransactionSender - 取引送信インターフェース
 *
 * Contract:
 * - send() は suspend 関数
 * - 成功時は Result.success(Unit)
 * - 失敗時は Result.failure(Exception)
 * - simulate=true の場合、実際に送信せずに検証のみ
 */
interface TransactionSender {
    suspend fun send(profile: Profile, transaction: LedgerTransaction, simulate: Boolean): Result<Unit>
}

/**
 * ConfigBackup - 設定バックアップインターフェース
 *
 * Contract:
 * - backup()/restore() は suspend 関数
 * - URI は ContentResolver で解決可能
 * - 成功時は Result.success(Unit)
 * - 失敗時は Result.failure(Exception)
 */
interface ConfigBackup {
    suspend fun backup(uri: Uri): Result<Unit>
    suspend fun restore(uri: Uri): Result<Unit>
}

/**
 * VersionDetector - バージョン検出インターフェース
 *
 * Contract:
 * - detect() は suspend 関数
 * - 成功時は Result.success(versionString)
 * - 失敗時は Result.failure(Exception)
 * - ネットワークエラー時は適切な例外でラップ
 */
interface VersionDetector {
    suspend fun detect(url: String, useAuth: Boolean, user: String?, password: String?): Result<String>
    suspend fun detect(profile: Profile): Result<String>
}

// =============================================================================
// IMPLEMENTATION CONTRACTS (NEW REQUIREMENTS)
// =============================================================================

/**
 * TransactionSyncerImpl Contract:
 *
 * 1. Thread 使用禁止
 *    - RetrieveTransactionsTask を使用しない
 *    - Thread.start(), Thread.join() を使用しない
 *
 * 2. Coroutine 要件
 *    - 全てのネットワーク I/O は withContext(ioDispatcher) 内で実行
 *    - 長時間処理中は ensureActive() でキャンセルチェック
 *    - Flow の emit は適切な頻度で行う（100件ごと等）
 *
 * 3. 依存関係
 *    - @IoDispatcher CoroutineDispatcher を注入
 *    - AccountRepository, TransactionRepository を使用
 *
 * 4. テスト要件
 *    - TestDispatcher 注入で即座にテスト完了
 *    - FakeTransactionSyncer との互換性維持
 */

/**
 * TransactionSenderImpl Contract:
 *
 * 1. Thread 使用禁止
 *    - SendTransactionTask を使用しない
 *    - Thread.sleep() を使用しない（delay() を使用）
 *    - TaskCallback を使用しない
 *
 * 2. Coroutine 要件
 *    - 全体を withContext(ioDispatcher) で実行
 *    - simulate=true 時は delay(1500) で待機
 *    - リトライ時は delay(100) で待機
 *
 * 3. テスト要件
 *    - TestDispatcher で delay() が即座にスキップ
 *    - FakeTransactionSender との互換性維持
 */

/**
 * ConfigBackupImpl Contract:
 *
 * 1. Thread 使用禁止
 *    - ConfigIO, ConfigWriter, ConfigReader を使用しない
 *    - runBlocking を使用しない
 *
 * 2. Coroutine 要件
 *    - 全体を withContext(ioDispatcher) で実行
 *    - Repository 呼び出しは suspend 関数を直接使用
 *
 * 3. テスト要件
 *    - FakeConfigBackup との互換性維持
 */

/**
 * VersionDetectorImpl Contract:
 *
 * 1. Thread 使用禁止
 *    - VersionDetectionThread を使用しない
 *    - Thread.sleep() を使用しない
 *
 * 2. Coroutine 要件
 *    - 全体を withContext(ioDispatcher) で実行
 *    - タイムアウトは withTimeout() を使用
 *
 * 3. テスト要件
 *    - FakeVersionDetector との互換性維持
 */

// =============================================================================
// DELETED COMPONENTS LIST
// =============================================================================

/**
 * 削除対象の Thread サブクラス:
 *
 * 1. RetrieveTransactionsTask (async/RetrieveTransactionsTask.kt)
 *    - 554行の Thread サブクラス
 *    - AccountAndTransactionListSaver (nested Thread)
 *    - runBlocking 使用箇所あり
 *
 * 2. SendTransactionTask (async/SendTransactionTask.kt)
 *    - deprecated マーク済み
 *    - Thread.sleep(1500), Thread.sleep(100) 使用
 *
 * 3. ConfigIO (backup/ConfigIO.kt)
 *    - Thread 抽象基底クラス
 *
 * 4. ConfigWriter (backup/ConfigWriter.kt)
 *    - ConfigIO を継承
 *
 * 5. ConfigReader (backup/ConfigReader.kt)
 *    - ConfigIO を継承
 *    - runBlocking 使用箇所あり
 *
 * 6. TransactionsDisplayedFilter (ui/main/TransactionListViewModel.kt:405)
 *    - ViewModel 内の Thread サブクラス
 *
 * 7. VersionDetectionThread (ui/profiles/ProfileDetailModel.kt:331)
 *    - ViewModel 内の Thread サブクラス
 *    - Thread.sleep() 使用
 */

/**
 * 削除対象の Executor パターン:
 *
 * 1. BaseDAO.asyncRunner (dao/BaseDAO.kt:69)
 *    - Executors.newSingleThreadExecutor()
 *    - insert(), update(), delete() で使用
 *
 * 2. GeneralBackgroundTasks (async/GeneralBackgroundTasks.kt)
 *    - Executors.newFixedThreadPool()
 */

/**
 * 削除対象の Callback インターフェース:
 *
 * 1. TaskCallback (async/TaskCallback.kt)
 *    - onTransactionSaveDone(error, args)
 *
 * 2. AsyncResultCallback (dao/AsyncResultCallback.kt)
 *    - onResult(result)
 */
