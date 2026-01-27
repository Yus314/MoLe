/*
 * Copyright © 2026 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.SyncProgress
import net.ktnx.mobileledger.core.domain.model.SyncResult

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
 * - ネットワークエラー、認証エラー、タイムアウトは SyncException として Flow から throw
 * - 予期しない例外は Flow の catch で処理
 */
interface TransactionSyncer {

    /**
     * 指定されたプロファイルのサーバーから取引を同期する
     *
     * @param profile 同期対象のプロファイル（サーバーURL、認証情報を含む）
     * @return 進捗を報告する Flow。正常完了時は Flow が正常終了
     * @throws CancellationException キャンセルされた場合
     *
     * ## 進捗報告
     *
     * 1. SyncProgress.Starting - 処理開始時
     * 2. SyncProgress.Running - 取引/勘定科目の取得・保存中
     * 3. SyncProgress.Indeterminate - サーバー応答待ち
     *
     * ## エラー時
     *
     * - Flow が SyncException で終了
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
