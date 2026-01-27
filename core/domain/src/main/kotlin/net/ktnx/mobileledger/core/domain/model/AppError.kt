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

package net.ktnx.mobileledger.core.domain.model

/**
 * アプリケーション全体で使用する統一エラー型
 *
 * すべてのエラーは以下の情報を持つ:
 * - message: ユーザーに表示するメッセージ
 * - isRetryable: リトライ可能かどうか
 * - cause: 元の例外（存在する場合）
 *
 * エラー階層:
 * - AppError.Sync: ネットワーク/同期関連エラー（既存のSyncErrorをラップ）
 * - DatabaseError: データベース操作エラー
 * - FileError: ファイルI/Oエラー
 */
sealed class AppError {
    /** ユーザーに表示するエラーメッセージ */
    abstract val message: String

    /** このエラーが発生した操作をリトライ可能かどうか */
    abstract val isRetryable: Boolean

    /** 元の例外（デバッグ/ログ用） */
    open val cause: Throwable? = null

    /**
     * 同期/ネットワーク関連エラー
     *
     * 既存の [SyncError] をラップして、AppError階層に統合する。
     * ネットワーク通信、サーバー応答、認証などのエラーを表す。
     */
    data class Sync(val error: SyncError) : AppError() {
        override val message: String = error.message
        override val isRetryable: Boolean = error.isRetryable
        override val cause: Throwable? = when (error) {
            is SyncError.NetworkError -> error.cause
            is SyncError.ParseError -> error.cause
            is SyncError.UnknownError -> error.cause
            else -> null
        }
    }

    /**
     * 不明なエラー
     *
     * 予期しない例外が発生した場合に使用する。
     * 主にリポジトリ層で、分類できない例外をラップする。
     */
    data class Unknown(
        override val message: String = "予期しないエラーが発生しました",
        override val cause: Throwable? = null
    ) : AppError() {
        override val isRetryable: Boolean = false
    }
}

/**
 * AppErrorをラップする例外クラス
 *
 * suspend関数やFlow内でAppErrorを伝播する際に使用する。
 *
 * 使用例:
 * ```kotlin
 * suspend fun doSomething(): Result<Unit> = try {
 *     // 処理
 *     Result.success(Unit)
 * } catch (e: Exception) {
 *     Result.failure(AppException(appExceptionMapper.map(e)))
 * }
 * ```
 */
class AppException(val appError: AppError) : Exception(appError.message, appError.cause)
