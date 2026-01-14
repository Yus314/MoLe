package net.ktnx.mobileledger.domain.usecase

import android.net.Uri
import net.ktnx.mobileledger.domain.model.BackupState

/**
 * 設定バックアップ/リストアのインターフェース
 *
 * プロファイル設定をファイルにバックアップ・リストアする。
 * 既存の ConfigIO（ConfigReader, ConfigWriter）を置き換える。
 *
 * ## 使用例
 *
 * ```kotlin
 * @HiltViewModel
 * class BackupsViewModel @Inject constructor(
 *     private val configBackup: ConfigBackup
 * ) : ViewModel() {
 *
 *     fun backup(uri: Uri) {
 *         viewModelScope.launch {
 *             _backupState.value = BackupState.InProgress("バックアップ中...")
 *             configBackup.backup(uri)
 *                 .onSuccess {
 *                     _backupState.value = BackupState.Completed
 *                     _effects.send(Effect.ShowMessage("バックアップが完了しました"))
 *                 }
 *                 .onFailure { error ->
 *                     _backupState.value = BackupState.Failed(mapToSyncError(error))
 *                 }
 *         }
 *     }
 *
 *     fun restore(uri: Uri) {
 *         viewModelScope.launch {
 *             _backupState.value = BackupState.InProgress("リストア中...")
 *             configBackup.restore(uri)
 *                 .onSuccess {
 *                     _backupState.value = BackupState.Completed
 *                     _effects.send(Effect.ShowMessage("リストアが完了しました"))
 *                 }
 *                 .onFailure { error ->
 *                     _backupState.value = BackupState.Failed(mapToSyncError(error))
 *                 }
 *         }
 *     }
 * }
 * ```
 *
 * ## エラーハンドリング
 *
 * - ファイル読み書きエラー → SyncError.UnknownError
 * - フォーマットエラー → SyncError.ParseError
 * - アクセス権限エラー → SyncError.UnknownError
 */
interface ConfigBackup {

    /**
     * 設定をファイルにバックアップする
     *
     * @param uri バックアップ先のファイルURI（SAF経由で取得）
     * @return 成功時は Result.success(Unit)、失敗時は Result.failure(Exception)
     * @throws CancellationException キャンセルされた場合
     *
     * ## バックアップ内容
     *
     * - 全プロファイル設定
     * - 各プロファイルのオプション
     * - テンプレート
     * - 通貨設定
     *
     * **注意**: 取引データはバックアップに含まれない（サーバー側で管理）
     */
    suspend fun backup(uri: Uri): Result<Unit>

    /**
     * ファイルから設定をリストアする
     *
     * @param uri リストア元のファイルURI（SAF経由で取得）
     * @return 成功時は Result.success(Unit)、失敗時は Result.failure(Exception)
     * @throws CancellationException キャンセルされた場合
     *
     * ## リストア動作
     *
     * - 既存の設定を上書き
     * - バックアップに含まれないプロファイルは削除されない
     * - 同じ名前のプロファイルが存在する場合は上書き
     *
     * ## エラーケース
     *
     * - ファイルが存在しない → UnknownError
     * - ファイル形式が不正 → ParseError
     * - アクセス権限なし → UnknownError
     */
    suspend fun restore(uri: Uri): Result<Unit>
}
