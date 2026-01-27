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
import net.ktnx.mobileledger.core.data.exception.CoreExceptionMapper
import net.ktnx.mobileledger.core.domain.model.AppError
import net.ktnx.mobileledger.core.domain.model.AppException
import net.ktnx.mobileledger.core.domain.model.SyncException
import net.ktnx.mobileledger.core.domain.repository.ExceptionMapper
import net.ktnx.mobileledger.domain.usecase.sync.SyncExceptionMapper

/**
 * 例外をAppErrorに変換するマッパー
 *
 * すべての例外を統一されたAppError型に変換する。
 * データベース例外、ファイル例外、ネットワーク例外を適切なエラー型にマッピングする。
 *
 * 責任分離:
 * - CoreExceptionMapper: データベース、ファイルエラー
 * - SyncExceptionMapper: ネットワーク、同期エラー
 * - AppExceptionMapper: 上記を統合し、適切なマッパーに委譲
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
    private val coreExceptionMapper: CoreExceptionMapper,
    private val syncExceptionMapper: SyncExceptionMapper
) : ExceptionMapper {
    /**
     * 例外をAppErrorに変換する
     *
     * マッピング優先順位:
     * 1. 既にマッピング済み (AppException, SyncException) → そのまま使用
     * 2. データベースエラー (SQLite*Exception) → CoreExceptionMapper に委譲
     * 3. ファイルエラー (FileNotFoundException, 権限エラー) → CoreExceptionMapper に委譲
     * 4. ネットワーク/IOエラー → SyncExceptionMapper に委譲
     * 5. その他 → SyncExceptionMapper に委譲
     *
     * @param e 変換する例外
     * @return 適切なAppErrorサブタイプ
     */
    override fun map(e: Throwable): AppError = when (e) {
        // 既にマッピング済みのエラー
        is AppException -> e.appError

        is SyncException -> AppError.Sync(e.syncError)

        // データベースエラー → CoreExceptionMapper に委譲
        is SQLiteConstraintException,
        is SQLiteException -> coreExceptionMapper.map(e)

        // IllegalStateException - データベース関連かどうかで分岐
        is IllegalStateException -> if (isDatabaseError(e)) {
            coreExceptionMapper.map(e)
        } else {
            AppError.Sync(syncExceptionMapper.mapToSyncException(e).syncError)
        }

        // ファイル関連エラー → CoreExceptionMapper に委譲
        is FileNotFoundException -> coreExceptionMapper.map(e)

        // SecurityException - ファイル権限関連かどうかで分岐
        is SecurityException -> if (isFilePermissionError(e)) {
            coreExceptionMapper.map(e)
        } else {
            AppError.Sync(syncExceptionMapper.mapToSyncException(e).syncError)
        }

        // ネットワーク/IO エラー - SyncExceptionMapper に委譲
        is IOException -> AppError.Sync(syncExceptionMapper.mapToSyncException(e).syncError)

        // その他 - SyncExceptionMapper に委譲
        else -> AppError.Sync(syncExceptionMapper.mapToSyncException(e).syncError)
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
