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
 * Unit tests for FileError sealed class.
 *
 * Tests verify:
 * - All error variants
 * - isRetryable property
 * - Custom fields
 * - Inheritance from AppError
 */
class FileErrorTest {

    // ========================================
    // ReadFailed tests
    // ========================================

    @Test
    fun `ReadFailed is not retryable`() {
        val error: FileError = FileError.ReadFailed()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `ReadFailed has default message`() {
        val error = FileError.ReadFailed()
        assertEquals("ファイルの読み込みに失敗しました", error.message)
    }

    @Test
    fun `ReadFailed with custom message`() {
        val error = FileError.ReadFailed(message = "Cannot open file")
        assertEquals("Cannot open file", error.message)
    }

    @Test
    fun `ReadFailed with cause`() {
        val cause = RuntimeException("I/O error")
        val error = FileError.ReadFailed(cause = cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `ReadFailed with path`() {
        val error = FileError.ReadFailed(path = "/storage/backup.json")
        assertEquals("/storage/backup.json", error.path)
    }

    @Test
    fun `ReadFailed with all fields`() {
        val cause = RuntimeException("EOF")
        val error = FileError.ReadFailed(
            message = "Read error",
            cause = cause,
            path = "/data/config.json"
        )
        assertEquals("Read error", error.message)
        assertEquals(cause, error.cause)
        assertEquals("/data/config.json", error.path)
    }

    @Test
    fun `ReadFailed is AppError`() {
        val error: AppError = FileError.ReadFailed()
        assertTrue(error is FileError.ReadFailed)
    }

    // ========================================
    // WriteFailed tests
    // ========================================

    @Test
    fun `WriteFailed is not retryable`() {
        val error: FileError = FileError.WriteFailed()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `WriteFailed has default message`() {
        val error = FileError.WriteFailed()
        assertEquals("ファイルの書き込みに失敗しました", error.message)
    }

    @Test
    fun `WriteFailed with custom message`() {
        val error = FileError.WriteFailed(message = "Disk full")
        assertEquals("Disk full", error.message)
    }

    @Test
    fun `WriteFailed with path`() {
        val error = FileError.WriteFailed(path = "/storage/emulated/0/backup.json")
        assertEquals("/storage/emulated/0/backup.json", error.path)
    }

    // ========================================
    // NotFound tests
    // ========================================

    @Test
    fun `NotFound is not retryable`() {
        val error: FileError = FileError.NotFound()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `NotFound has default message`() {
        val error = FileError.NotFound()
        assertEquals("ファイルが見つかりません", error.message)
    }

    @Test
    fun `NotFound with path`() {
        val error = FileError.NotFound(path = "/missing/file.txt")
        assertEquals("/missing/file.txt", error.path)
    }

    @Test
    fun `NotFound with custom message and path`() {
        val error = FileError.NotFound(
            message = "Backup file not found",
            path = "/backup/data.json"
        )
        assertEquals("Backup file not found", error.message)
        assertEquals("/backup/data.json", error.path)
    }

    // ========================================
    // PermissionDenied tests
    // ========================================

    @Test
    fun `PermissionDenied is not retryable`() {
        val error: FileError = FileError.PermissionDenied()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `PermissionDenied has default message`() {
        val error = FileError.PermissionDenied()
        assertEquals("ファイルへのアクセス権限がありません", error.message)
    }

    @Test
    fun `PermissionDenied with path`() {
        val error = FileError.PermissionDenied(path = "/root/protected.txt")
        assertEquals("/root/protected.txt", error.path)
    }

    @Test
    fun `PermissionDenied with cause`() {
        val cause = SecurityException("Access denied")
        val error = FileError.PermissionDenied(cause = cause)
        assertEquals(cause, error.cause)
    }

    // ========================================
    // InvalidFormat tests
    // ========================================

    @Test
    fun `InvalidFormat is not retryable`() {
        val error: FileError = FileError.InvalidFormat()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `InvalidFormat has default message`() {
        val error = FileError.InvalidFormat()
        assertEquals("ファイル形式が不正です", error.message)
    }

    @Test
    fun `InvalidFormat with expected format`() {
        val error = FileError.InvalidFormat(expectedFormat = "JSON")
        assertEquals("JSON", error.expectedFormat)
    }

    @Test
    fun `InvalidFormat with cause`() {
        val cause = RuntimeException("Unexpected token")
        val error = FileError.InvalidFormat(cause = cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `InvalidFormat with all fields`() {
        val cause = RuntimeException("Parse error")
        val error = FileError.InvalidFormat(
            message = "Invalid backup format",
            cause = cause,
            expectedFormat = "MoLe Backup JSON v1"
        )
        assertEquals("Invalid backup format", error.message)
        assertEquals(cause, error.cause)
        assertEquals("MoLe Backup JSON v1", error.expectedFormat)
    }

    // ========================================
    // Default values
    // ========================================

    @Test
    fun `default path is null for all error types`() {
        assertNull(FileError.ReadFailed().path)
        assertNull(FileError.WriteFailed().path)
        assertNull(FileError.NotFound().path)
        assertNull(FileError.PermissionDenied().path)
    }

    @Test
    fun `default expectedFormat is null`() {
        assertNull(FileError.InvalidFormat().expectedFormat)
    }

    // ========================================
    // When expression exhaustiveness
    // ========================================

    @Test
    fun `when expression handles all file errors`() {
        val errors: List<FileError> = listOf(
            FileError.ReadFailed(),
            FileError.WriteFailed(),
            FileError.NotFound(),
            FileError.PermissionDenied(),
            FileError.InvalidFormat()
        )

        errors.forEach { error ->
            val description = when (error) {
                is FileError.ReadFailed -> "read: ${error.path}"
                is FileError.WriteFailed -> "write: ${error.path}"
                is FileError.NotFound -> "not found: ${error.path}"
                is FileError.PermissionDenied -> "denied: ${error.path}"
                is FileError.InvalidFormat -> "invalid: ${error.expectedFormat}"
            }
            assertTrue(description.isNotEmpty())
        }
    }

    // ========================================
    // All FileErrors are not retryable
    // ========================================

    @Test
    fun `all FileErrors are not retryable`() {
        val errors: List<FileError> = listOf(
            FileError.ReadFailed(),
            FileError.WriteFailed(),
            FileError.NotFound(),
            FileError.PermissionDenied(),
            FileError.InvalidFormat()
        )

        errors.forEach { error ->
            assertFalse("${error::class.simpleName} should not be retryable", error.isRetryable)
        }
    }
}
