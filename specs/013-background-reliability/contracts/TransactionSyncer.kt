package net.ktnx.mobileledger.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.domain.model.SyncResult

/**
 * 取引同期処理のインターフェース
 *
 * サーバーから取引と勘定科目を取得し、ローカルDBに保存する。
 * 既存の RetrieveTransactionsTask を置き換える。
 *
 * ## 使用例
 *
 * ```kotlin
 * @HiltViewModel
 * class MainViewModel @Inject constructor(
 *     private val syncer: TransactionSyncer
 * ) : ViewModel() {
 *
 *     private var syncJob: Job? = null
 *
 *     fun startSync(profile: Profile) {
 *         syncJob?.cancel()
 *         syncJob = viewModelScope.launch {
 *             syncer.sync(profile)
 *                 .onStart { _syncState.value = SyncState.InProgress(SyncProgress.Starting()) }
 *                 .catch { e -> _syncState.value = SyncState.Failed(mapToSyncError(e)) }
 *                 .collect { progress ->
 *                     _syncState.value = SyncState.InProgress(progress)
 *                 }
 *         }
 *     }
 *
 *     fun cancelSync() {
 *         syncJob?.cancel()
 *     }
 * }
 * ```
 *
 * ## キャンセル対応
 *
 * - `sync()` はキャンセル対応しており、`Job.cancel()` で停止可能
 * - キャンセル時は自動的にネットワーク接続を切断し、DBトランザクションをロールバック
 * - キャンセル応答時間: 500ms以内
 *
 * ## エラーハンドリング
 *
 * - ネットワークエラー、認証エラー、タイムアウトは SyncError として Flow から emit
 * - 予期しない例外は Flow の catch で処理
 */
interface TransactionSyncer {

    /**
     * 指定されたプロファイルのサーバーから取引を同期する
     *
     * @param profile 同期対象のプロファイル（サーバーURL、認証情報を含む）
     * @return 進捗を報告する Flow。正常完了時は最後に SyncProgress.Completed を emit
     * @throws CancellationException キャンセルされた場合
     *
     * ## 進捗報告
     *
     * 1. SyncProgress.Starting - 処理開始時
     * 2. SyncProgress.Running - 取引/勘定科目の取得・保存中
     * 3. SyncProgress.Indeterminate - サーバー応答待ち
     * 4. SyncProgress.Completed - 正常完了時（最後に1回のみ）
     *
     * ## エラー時
     *
     * - Flow が SyncError を含む例外で終了
     * - UI層で catch して SyncState.Failed に変換
     */
    fun sync(profile: Profile): Flow<SyncProgress>

    /**
     * 同期結果を取得する（同期完了後に呼び出し）
     *
     * @return 直前の同期結果。同期未実行の場合は null
     */
    fun getLastResult(): SyncResult?
}
