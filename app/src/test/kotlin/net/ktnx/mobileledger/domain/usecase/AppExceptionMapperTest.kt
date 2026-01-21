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
import net.ktnx.mobileledger.domain.model.AppError
import net.ktnx.mobileledger.domain.model.AppException
import net.ktnx.mobileledger.domain.model.DatabaseError
import net.ktnx.mobileledger.domain.model.FileError
import net.ktnx.mobileledger.domain.model.SyncError
import net.ktnx.mobileledger.domain.model.SyncException
import net.ktnx.mobileledger.domain.usecase.sync.SyncExceptionMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [AppExceptionMapper].
 *
 * Tests verify that various exception types are correctly mapped to
 * appropriate AppError subtypes.
 */
@RunWith(RobolectricTestRunner::class)
class AppExceptionMapperTest {

    private lateinit var mapper: AppExceptionMapper
    private lateinit var syncExceptionMapper: SyncExceptionMapper

    @Before
    fun setup() {
        syncExceptionMapper = SyncExceptionMapper()
        mapper = AppExceptionMapper(syncExceptionMapper)
    }

    // ========================================
    // Already mapped exceptions
    // ========================================

    @Test
    fun `map returns error from AppException`() {
        // Given
        val originalError = DatabaseError.QueryFailed(message = "Test error")
        val exception = AppException(originalError)

        // When
        val result = mapper.map(exception)

        // Then
        assertEquals(originalError, result)
    }

    @Test
    fun `map converts SyncException to Sync error`() {
        // Given
        val syncError = SyncError.NetworkError(message = "Network failed")
        val exception = SyncException(syncError)

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is AppError.Sync)
        assertEquals(syncError, (result as AppError.Sync).error)
    }

    // ========================================
    // Database errors
    // ========================================

    @Test
    fun `map converts SQLiteConstraintException to ConstraintViolation`() {
        // Given
        val exception = SQLiteConstraintException("UNIQUE constraint failed: profiles.uuid")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is DatabaseError.ConstraintViolation)
        assertEquals("profiles", (result as DatabaseError.ConstraintViolation).constraintName)
    }

    @Test
    fun `map converts SQLiteException to QueryFailed`() {
        // Given
        val exception = SQLiteException("no such table: test")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is DatabaseError.QueryFailed)
    }

    @Test
    fun `map converts database-related IllegalStateException to QueryFailed`() {
        // Given
        val exception = IllegalStateException("database is locked")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is DatabaseError.QueryFailed)
    }

    @Test
    fun `map converts Room-related IllegalStateException to QueryFailed`() {
        // Given
        val exception = IllegalStateException("Room cannot verify the data integrity")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is DatabaseError.QueryFailed)
    }

    @Test
    fun `map converts cursor-related IllegalStateException to QueryFailed`() {
        // Given
        val exception = IllegalStateException("Cursor is closed")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is DatabaseError.QueryFailed)
    }

    @Test
    fun `map converts non-database IllegalStateException to Sync error`() {
        // Given
        val exception = IllegalStateException("Some other error")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is AppError.Sync)
    }

    // ========================================
    // File errors
    // ========================================

    @Test
    fun `map converts FileNotFoundException to NotFound`() {
        // Given
        val exception = FileNotFoundException("/path/to/file.txt")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is FileError.NotFound)
        assertEquals("/path/to/file.txt", (result as FileError.NotFound).path)
    }

    @Test
    fun `map converts permission SecurityException to PermissionDenied`() {
        // Given
        val exception = SecurityException("Read permission denied")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is FileError.PermissionDenied)
    }

    @Test
    fun `map converts access SecurityException to PermissionDenied`() {
        // Given
        val exception = SecurityException("Access denied to file")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is FileError.PermissionDenied)
    }

    @Test
    fun `map converts write SecurityException to PermissionDenied`() {
        // Given
        val exception = SecurityException("Write access denied")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is FileError.PermissionDenied)
    }

    @Test
    fun `map converts non-file SecurityException to Sync error`() {
        // Given
        val exception = SecurityException("Network security exception")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is AppError.Sync)
    }

    // ========================================
    // Network/IO errors
    // ========================================

    @Test
    fun `map converts IOException to Sync error`() {
        // Given
        val exception = IOException("Connection reset")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is AppError.Sync)
        val syncError = (result as AppError.Sync).error
        assertTrue(syncError is SyncError.NetworkError)
    }

    // ========================================
    // Unknown errors
    // ========================================

    @Test
    fun `map converts unknown exception to Sync error`() {
        // Given
        val exception = RuntimeException("Unknown error")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is AppError.Sync)
        val syncError = (result as AppError.Sync).error
        assertNotNull(syncError)
    }

    // ========================================
    // Edge cases
    // ========================================

    @Test
    fun `map handles exception with null message`() {
        // Given
        val exception = SQLiteException(null)

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is DatabaseError.QueryFailed)
    }

    @Test
    fun `map extracts constraint name from message`() {
        // Given
        val exception = SQLiteConstraintException("UNIQUE constraint failed: accounts.name")

        // When
        val result = mapper.map(exception)

        // Then
        assertTrue(result is DatabaseError.ConstraintViolation)
        assertEquals("accounts", (result as DatabaseError.ConstraintViolation).constraintName)
    }
}
