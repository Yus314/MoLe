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

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import net.ktnx.mobileledger.core.domain.model.AppError
import net.ktnx.mobileledger.core.domain.model.AppException
import net.ktnx.mobileledger.core.domain.model.DatabaseError
import net.ktnx.mobileledger.core.domain.model.FileError
import net.ktnx.mobileledger.core.domain.model.SyncException
import net.ktnx.mobileledger.domain.usecase.sync.SyncExceptionMapper

/**
 * 例外をAppErrorに変換するマッパー
 *
 * すべての例外を統一されたAppError型に変換する。
 * データベース例外、ファイル例外、ネットワーク例外を適切なエラー型にマッピングする。
 *
 * 使用例:
 * ```kotlin
 * try {
 *     repository.doSomething()
 * } catch (e: Exception) {
 *     val error = appExceptionMapper.map(e)
 *     _uiState.update { it.copy(error = error) }
 * }
 * ```
 */
@Singleton
class AppExceptionMapper @Inject constructor(
    private val syncExceptionMapper: SyncExceptionMapper
) {
    /**
     * 例外をAppErrorに変換する
     *
     * @param e 変換する例外
     * @return 適切なAppErrorサブタイプ
     */
    fun map(e: Throwable): AppError = when (e) {
        // 既にマッピング済みのエラー
        is AppException -> e.appError

        is SyncException -> AppError.Sync(e.syncError)

        // データベースエラー
        is SQLiteConstraintException -> DatabaseError.ConstraintViolation(
            cause = e,
            constraintName = extractConstraintName(e)
        )

        is SQLiteException -> DatabaseError.QueryFailed(
            message = e.localizedMessage ?: "データベースエラー",
            cause = e
        )

        is IllegalStateException -> if (isDatabaseError(e)) {
            DatabaseError.QueryFailed(
                message = e.localizedMessage ?: "データベースエラー",
                cause = e
            )
        } else {
            AppError.Sync(syncExceptionMapper.mapToSyncException(e).syncError)
        }

        // ファイルエラー
        is FileNotFoundException -> FileError.NotFound(
            cause = e,
            path = e.message
        )

        is SecurityException -> if (isFilePermissionError(e)) {
            FileError.PermissionDenied(cause = e)
        } else {
            AppError.Sync(syncExceptionMapper.mapToSyncException(e).syncError)
        }

        // ネットワーク/IO エラー - 既存のマッパーに委譲
        is IOException -> AppError.Sync(syncExceptionMapper.mapToSyncException(e).syncError)

        // その他 - 既存のマッパーに委譲
        else -> AppError.Sync(syncExceptionMapper.mapToSyncException(e).syncError)
    }

    /**
     * 制約名を例外メッセージから抽出する
     */
    private fun extractConstraintName(e: SQLiteConstraintException): String? = e.message?.let { msg ->
        // "UNIQUE constraint failed: table.column" のようなメッセージから抽出
        Regex("constraint failed: (\\w+)").find(msg)?.groupValues?.getOrNull(1)
    }

    /**
     * データベース関連のIllegalStateExceptionかどうかを判定
     */
    private fun isDatabaseError(e: IllegalStateException): Boolean = e.message?.let { msg ->
        msg.contains("database", ignoreCase = true) ||
            msg.contains("room", ignoreCase = true) ||
            msg.contains("cursor", ignoreCase = true)
    } ?: false

    /**
     * ファイル権限関連のSecurityExceptionかどうかを判定
     */
    private fun isFilePermissionError(e: SecurityException): Boolean = e.message?.let { msg ->
        msg.contains("permission", ignoreCase = true) ||
            msg.contains("access", ignoreCase = true) ||
            msg.contains("read", ignoreCase = true) ||
            msg.contains("write", ignoreCase = true)
    } ?: false
}
