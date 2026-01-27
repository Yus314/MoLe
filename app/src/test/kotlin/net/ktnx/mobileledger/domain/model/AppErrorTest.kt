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

package net.ktnx.mobileledger.domain.model

import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AppError sealed class and AppException.
 *
 * Tests verify:
 * - AppError.Sync wrapping SyncError
 * - isRetryable propagation
 * - cause extraction
 * - AppException creation
 */
class AppErrorTest {

    // ========================================
    // AppError.Sync with NetworkError
    // ========================================

    @Test
    fun `Sync wraps NetworkError message`() {
        val syncError = SyncError.NetworkError("Connection timeout", null)
        val appError = AppError.Sync(syncError)
        assertEquals("Connection timeout", appError.message)
    }

    @Test
    fun `Sync wraps NetworkError isRetryable`() {
        val syncError = SyncError.NetworkError("Timeout", null)
        val appError = AppError.Sync(syncError)
        assertTrue(appError.isRetryable)
    }

    @Test
    fun `Sync extracts NetworkError cause`() {
        val cause = RuntimeException("Socket closed")
        val syncError = SyncError.NetworkError("Network error", cause)
        val appError = AppError.Sync(syncError)
        assertEquals(cause, appError.cause)
    }

    // ========================================
    // AppError.Sync with ParseError
    // ========================================

    @Test
    fun `Sync wraps ParseError message`() {
        val syncError = SyncError.ParseError("Invalid JSON", null)
        val appError = AppError.Sync(syncError)
        assertEquals("Invalid JSON", appError.message)
    }

    @Test
    fun `Sync wraps ParseError isRetryable`() {
        val syncError = SyncError.ParseError("Parse failed", null)
        val appError = AppError.Sync(syncError)
        assertFalse(appError.isRetryable)
    }

    @Test
    fun `Sync extracts ParseError cause`() {
        val cause = RuntimeException("Unexpected token")
        val syncError = SyncError.ParseError("Parse error", cause)
        val appError = AppError.Sync(syncError)
        assertEquals(cause, appError.cause)
    }

    // ========================================
    // AppError.Sync with UnknownError
    // ========================================

    @Test
    fun `Sync wraps UnknownError message`() {
        val syncError = SyncError.UnknownError("Something went wrong", null)
        val appError = AppError.Sync(syncError)
        assertEquals("Something went wrong", appError.message)
    }

    @Test
    fun `Sync wraps UnknownError isRetryable`() {
        val syncError = SyncError.UnknownError("Unknown", null)
        val appError = AppError.Sync(syncError)
        // UnknownError is NOT retryable
        assertFalse(appError.isRetryable)
    }

    @Test
    fun `Sync extracts UnknownError cause`() {
        val cause = RuntimeException("Unexpected")
        val syncError = SyncError.UnknownError("Unknown error", cause)
        val appError = AppError.Sync(syncError)
        assertEquals(cause, appError.cause)
    }

    // ========================================
    // AppError.Sync with errors without cause
    // ========================================

    @Test
    fun `Sync with AuthenticationError has null cause`() {
        val syncError = SyncError.AuthenticationError("Bad credentials")
        val appError = AppError.Sync(syncError)
        assertNull(appError.cause)
    }

    @Test
    fun `Sync with ServerError has null cause`() {
        val syncError = SyncError.ServerError("Internal error", 500)
        val appError = AppError.Sync(syncError)
        assertNull(appError.cause)
    }

    @Test
    fun `Sync with ApiVersionError has null cause`() {
        val syncError = SyncError.ApiVersionError(detectedVersion = "1.15")
        val appError = AppError.Sync(syncError)
        assertNull(appError.cause)
    }

    @Test
    fun `Sync with Cancelled has null cause`() {
        val syncError = SyncError.Cancelled
        val appError = AppError.Sync(syncError)
        assertNull(appError.cause)
    }

    // ========================================
    // AppError.Sync isRetryable propagation
    // ========================================

    @Test
    fun `Sync propagates isRetryable true for retryable errors`() {
        val retryableErrors = listOf(
            SyncError.NetworkError("test", null),
            SyncError.ServerError("Unavailable", 503),
            SyncError.TimeoutError("timeout")
        )

        retryableErrors.forEach { syncError ->
            val appError = AppError.Sync(syncError)
            assertTrue(
                "${syncError::class.simpleName} should be retryable",
                appError.isRetryable
            )
        }
    }

    @Test
    fun `Sync propagates isRetryable false for non-retryable errors`() {
        val nonRetryableErrors = listOf(
            SyncError.AuthenticationError("test"),
            SyncError.ParseError("test", null),
            SyncError.ApiVersionError(detectedVersion = "1.15"),
            SyncError.Cancelled,
            SyncError.UnknownError("test", null),
            SyncError.ValidationError("test"),
            SyncError.ServerError("Client error", 400)
        )

        nonRetryableErrors.forEach { syncError ->
            val appError = AppError.Sync(syncError)
            assertFalse(
                "${syncError::class.simpleName} should not be retryable",
                appError.isRetryable
            )
        }
    }

    // ========================================
    // AppError is sealed class
    // ========================================

    @Test
    fun `when expression handles AppError subclasses`() {
        val errors: List<AppError> = listOf(
            AppError.Sync(SyncError.NetworkError("test", null)),
            DatabaseError.QueryFailed(),
            FileError.ReadFailed()
        )

        errors.forEach { error ->
            val description = when (error) {
                is AppError.Sync -> "sync: ${error.error::class.simpleName}"
                is DatabaseError -> "database: ${error::class.simpleName}"
                is FileError -> "file: ${error::class.simpleName}"
            }
            assertTrue(description.isNotEmpty())
        }
    }

    // ========================================
    // AppException tests
    // ========================================

    @Test
    fun `AppException contains AppError`() {
        val appError = AppError.Sync(SyncError.NetworkError("test", null))
        val exception = AppException(appError)
        assertEquals(appError, exception.appError)
    }

    @Test
    fun `AppException has message from AppError`() {
        val appError = AppError.Sync(SyncError.NetworkError("Connection failed", null))
        val exception = AppException(appError)
        assertEquals("Connection failed", exception.message)
    }

    @Test
    fun `AppException has cause from AppError`() {
        val cause = RuntimeException("Original")
        val appError = AppError.Sync(SyncError.NetworkError("Error", cause))
        val exception = AppException(appError)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `AppException is Throwable`() {
        val appError = DatabaseError.QueryFailed()
        val exception = AppException(appError)
        assertTrue(exception is Exception)
        assertTrue(exception is Throwable)
    }

    @Test
    fun `AppException can be thrown and caught`() {
        val appError = FileError.NotFound(path = "/test.txt")
        val exception = AppException(appError)

        var caught = false
        try {
            throw exception
        } catch (e: AppException) {
            caught = true
            assertEquals(appError, e.appError)
        }
        assertTrue(caught)
    }

    @Test
    fun `AppException with DatabaseError`() {
        val appError = DatabaseError.ConstraintViolation(constraintName = "unique_uuid")
        val exception = AppException(appError)
        assertEquals("データの整合性エラーが発生しました", exception.message)
        assertTrue(exception.appError is DatabaseError.ConstraintViolation)
    }

    @Test
    fun `AppException with FileError`() {
        val appError = FileError.PermissionDenied(path = "/protected")
        val exception = AppException(appError)
        assertEquals("ファイルへのアクセス権限がありません", exception.message)
        assertTrue(exception.appError is FileError.PermissionDenied)
    }
}
