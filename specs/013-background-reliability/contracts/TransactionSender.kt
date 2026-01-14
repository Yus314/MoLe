package net.ktnx.mobileledger.domain.usecase

import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.LedgerTransaction

/**
 * 取引送信のインターフェース
 *
 * 新規取引をサーバーに送信する。
 * 既存の SendTransactionTask を置き換える。
 *
 * **注意**: このインターフェースは既に実装済み（TransactionSender.kt, TransactionSenderImpl.kt）。
 * 本仕様書では参照用に記載。
 *
 * ## 使用例
 *
 * ```kotlin
 * @HiltViewModel
 * class NewTransactionViewModel @Inject constructor(
 *     private val sender: TransactionSender
 * ) : ViewModel() {
 *
 *     fun saveTransaction(profile: Profile, transaction: LedgerTransaction) {
 *         viewModelScope.launch {
 *             _sendState.value = SendState.Sending()
 *             sender.send(profile, transaction)
 *                 .onSuccess {
 *                     _sendState.value = SendState.Completed
 *                     _effects.send(Effect.NavigateBack)
 *                 }
 *                 .onFailure { error ->
 *                     _sendState.value = SendState.Failed(mapToSyncError(error))
 *                 }
 *         }
 *     }
 * }
 * ```
 *
 * ## キャンセル対応
 *
 * - `send()` はキャンセル対応しており、スコープキャンセルで停止
 * - ただし、送信開始後のキャンセルはサーバー側で取引が作成される可能性あり
 *
 * ## エラーハンドリング
 *
 * - Result.failure() で SyncError 相当の例外を返却
 * - ValidationError はサーバーからのエラーメッセージを含む
 */
interface TransactionSender {

    /**
     * 取引をサーバーに送信する
     *
     * @param profile 送信先のプロファイル
     * @param transaction 送信する取引
     * @param simulate true の場合、実際には保存せずバリデーションのみ実行
     * @return 成功時は Result.success(Unit)、失敗時は Result.failure(Exception)
     * @throws CancellationException キャンセルされた場合
     *
     * ## simulate パラメータ
     *
     * - true: サーバー側で取引のバリデーションのみ実行（保存しない）
     * - false: 実際に取引を保存
     *
     * テスト時や保存前の確認に使用。
     */
    suspend fun send(profile: Profile, transaction: LedgerTransaction, simulate: Boolean = false): Result<Unit>
}
