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

package net.ktnx.mobileledger.core.data.exception

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import net.ktnx.mobileledger.core.domain.model.AppError
import net.ktnx.mobileledger.core.domain.model.DatabaseError
import net.ktnx.mobileledger.core.domain.model.FileError
import net.ktnx.mobileledger.core.domain.repository.ExceptionMapper

/**
 * Core exception mapper for database and file operations.
 *
 * This mapper handles exceptions that can occur in core:data repositories:
 * - SQLite/Room database exceptions → DatabaseError
 * - File I/O exceptions → FileError
 * - Unknown exceptions → AppError.Unknown
 *
 * Sync/network exceptions are handled by SyncExceptionMapper in the app module.
 *
 * Usage:
 * ```kotlin
 * @Singleton
 * class ProfileRepositoryImpl @Inject constructor(
 *     private val dao: ProfileDAO,
 *     private val exceptionMapper: CoreExceptionMapper,
 *     @IoDispatcher private val ioDispatcher: CoroutineDispatcher
 * ) : ProfileRepository {
 *     override suspend fun getById(id: Long): Result<Profile?> = safeCall(exceptionMapper) {
 *         withContext(ioDispatcher) { dao.getByIdSync(id)?.toDomain() }
 *     }
 * }
 * ```
 */
@Singleton
class CoreExceptionMapper @Inject constructor() : ExceptionMapper {

    /**
     * Maps a throwable to an appropriate AppError.
     *
     * Mapping priority:
     * 1. Database constraint exceptions → DatabaseError.ConstraintViolation
     * 2. Other SQLite exceptions → DatabaseError.QueryFailed
     * 3. IllegalStateException (database-related) → DatabaseError.QueryFailed
     * 4. FileNotFoundException → FileError.NotFound
     * 5. SecurityException (file-related) → FileError.PermissionDenied
     * 6. Generic IOException → FileError.ReadFailed
     * 7. All other exceptions → AppError.Unknown
     *
     * @param e The throwable to map
     * @return An appropriate AppError subtype
     */
    override fun map(e: Throwable): AppError = when (e) {
        // Database constraint violation (UNIQUE, FOREIGN KEY, NOT NULL)
        is SQLiteConstraintException -> DatabaseError.ConstraintViolation(
            cause = e,
            constraintName = extractConstraintName(e)
        )

        // Other SQLite errors
        is SQLiteException -> DatabaseError.QueryFailed(
            message = e.localizedMessage ?: "データベースエラー",
            cause = e
        )

        // IllegalStateException - check if database-related
        is IllegalStateException -> if (isDatabaseError(e)) {
            DatabaseError.QueryFailed(
                message = e.localizedMessage ?: "データベースエラー",
                cause = e
            )
        } else {
            AppError.Unknown(
                message = e.localizedMessage ?: "予期しないエラーが発生しました",
                cause = e
            )
        }

        // File not found
        is FileNotFoundException -> FileError.NotFound(
            cause = e,
            path = e.message
        )

        // File permission error
        is SecurityException -> if (isFilePermissionError(e)) {
            FileError.PermissionDenied(cause = e)
        } else {
            AppError.Unknown(
                message = e.localizedMessage ?: "セキュリティエラー",
                cause = e
            )
        }

        // Generic I/O error (file read/write)
        is IOException -> FileError.ReadFailed(
            message = e.localizedMessage ?: "ファイルの読み込みに失敗しました",
            cause = e
        )

        // Unknown error
        else -> AppError.Unknown(
            message = e.localizedMessage ?: "予期しないエラーが発生しました",
            cause = e
        )
    }

    /**
     * Extracts constraint name from SQLiteConstraintException message.
     *
     * Example messages:
     * - "UNIQUE constraint failed: table.column"
     * - "FOREIGN KEY constraint failed"
     */
    private fun extractConstraintName(e: SQLiteConstraintException): String? = e.message?.let { msg ->
        Regex("constraint failed: (\\w+)").find(msg)?.groupValues?.getOrNull(1)
    }

    /**
     * Checks if IllegalStateException is database-related.
     */
    private fun isDatabaseError(e: IllegalStateException): Boolean = e.message?.let { msg ->
        msg.contains("database", ignoreCase = true) ||
            msg.contains("room", ignoreCase = true) ||
            msg.contains("cursor", ignoreCase = true)
    } ?: false

    /**
     * Checks if SecurityException is file permission-related.
     */
    private fun isFilePermissionError(e: SecurityException): Boolean = e.message?.let { msg ->
        msg.contains("permission", ignoreCase = true) ||
            msg.contains("access", ignoreCase = true) ||
            msg.contains("read", ignoreCase = true) ||
            msg.contains("write", ignoreCase = true)
    } ?: false
}
