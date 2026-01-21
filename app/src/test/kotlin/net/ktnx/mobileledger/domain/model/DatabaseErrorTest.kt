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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DatabaseError sealed class.
 *
 * Tests verify:
 * - All error variants
 * - isRetryable property
 * - Custom fields
 * - Inheritance from AppError
 */
class DatabaseErrorTest {

    // ========================================
    // ConstraintViolation tests
    // ========================================

    @Test
    fun `ConstraintViolation is not retryable`() {
        val error: DatabaseError = DatabaseError.ConstraintViolation()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `ConstraintViolation has default message`() {
        val error = DatabaseError.ConstraintViolation()
        assertEquals("データの整合性エラーが発生しました", error.message)
    }

    @Test
    fun `ConstraintViolation with custom message`() {
        val error = DatabaseError.ConstraintViolation(message = "Duplicate entry")
        assertEquals("Duplicate entry", error.message)
    }

    @Test
    fun `ConstraintViolation with cause`() {
        val cause = RuntimeException("UNIQUE constraint failed")
        val error = DatabaseError.ConstraintViolation(cause = cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `ConstraintViolation with constraint name`() {
        val error = DatabaseError.ConstraintViolation(constraintName = "idx_unique_uuid")
        assertEquals("idx_unique_uuid", error.constraintName)
    }

    @Test
    fun `ConstraintViolation is AppError`() {
        val error: AppError = DatabaseError.ConstraintViolation()
        assertTrue(error is DatabaseError.ConstraintViolation)
    }

    // ========================================
    // QueryFailed tests
    // ========================================

    @Test
    fun `QueryFailed is not retryable`() {
        val error: DatabaseError = DatabaseError.QueryFailed()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `QueryFailed has default message`() {
        val error = DatabaseError.QueryFailed()
        assertEquals("データベースクエリに失敗しました", error.message)
    }

    @Test
    fun `QueryFailed with custom message`() {
        val error = DatabaseError.QueryFailed(message = "Syntax error near SELECT")
        assertEquals("Syntax error near SELECT", error.message)
    }

    @Test
    fun `QueryFailed with cause`() {
        val cause = RuntimeException("no such table: accounts")
        val error = DatabaseError.QueryFailed(cause = cause)
        assertEquals(cause, error.cause)
    }

    // ========================================
    // ConnectionFailed tests
    // ========================================

    @Test
    fun `ConnectionFailed is retryable`() {
        val error: DatabaseError = DatabaseError.ConnectionFailed()
        assertTrue(error.isRetryable)
    }

    @Test
    fun `ConnectionFailed has default message`() {
        val error = DatabaseError.ConnectionFailed()
        assertEquals("データベースに接続できません", error.message)
    }

    @Test
    fun `ConnectionFailed with custom message`() {
        val error = DatabaseError.ConnectionFailed(message = "Database locked")
        assertEquals("Database locked", error.message)
    }

    @Test
    fun `ConnectionFailed with cause`() {
        val cause = RuntimeException("disk I/O error")
        val error = DatabaseError.ConnectionFailed(cause = cause)
        assertEquals(cause, error.cause)
    }

    // ========================================
    // NotFound tests
    // ========================================

    @Test
    fun `NotFound is not retryable`() {
        val error: DatabaseError = DatabaseError.NotFound()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `NotFound has default message`() {
        val error = DatabaseError.NotFound()
        assertEquals("データが見つかりません", error.message)
    }

    @Test
    fun `NotFound with entity type`() {
        val error = DatabaseError.NotFound(entityType = "Profile")
        assertEquals("Profile", error.entityType)
    }

    @Test
    fun `NotFound with entity id`() {
        val error = DatabaseError.NotFound(entityId = 123L)
        assertEquals(123L, error.entityId)
    }

    @Test
    fun `NotFound with all fields`() {
        val error = DatabaseError.NotFound(
            message = "Profile not found",
            entityType = "Profile",
            entityId = 42L
        )
        assertEquals("Profile not found", error.message)
        assertEquals("Profile", error.entityType)
        assertEquals(42L, error.entityId)
    }

    @Test
    fun `NotFound cause is always null`() {
        val error = DatabaseError.NotFound()
        assertNull(error.cause)
    }

    // ========================================
    // MigrationFailed tests
    // ========================================

    @Test
    fun `MigrationFailed is not retryable`() {
        val error: DatabaseError = DatabaseError.MigrationFailed()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `MigrationFailed has default message`() {
        val error = DatabaseError.MigrationFailed()
        assertEquals("データベースの移行に失敗しました", error.message)
    }

    @Test
    fun `MigrationFailed with versions`() {
        val error = DatabaseError.MigrationFailed(fromVersion = 5, toVersion = 6)
        assertEquals(5, error.fromVersion)
        assertEquals(6, error.toVersion)
    }

    @Test
    fun `MigrationFailed with cause`() {
        val cause = RuntimeException("Migration 5 to 6 failed")
        val error = DatabaseError.MigrationFailed(cause = cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `MigrationFailed with all fields`() {
        val cause = RuntimeException("Column mismatch")
        val error = DatabaseError.MigrationFailed(
            message = "Migration error",
            cause = cause,
            fromVersion = 10,
            toVersion = 11
        )
        assertEquals("Migration error", error.message)
        assertEquals(cause, error.cause)
        assertEquals(10, error.fromVersion)
        assertEquals(11, error.toVersion)
    }

    // ========================================
    // When expression exhaustiveness
    // ========================================

    @Test
    fun `when expression handles all database errors`() {
        val errors: List<DatabaseError> = listOf(
            DatabaseError.ConstraintViolation(),
            DatabaseError.QueryFailed(),
            DatabaseError.ConnectionFailed(),
            DatabaseError.NotFound(),
            DatabaseError.MigrationFailed()
        )

        errors.forEach { error ->
            val description = when (error) {
                is DatabaseError.ConstraintViolation -> "constraint: ${error.constraintName}"
                is DatabaseError.QueryFailed -> "query failed"
                is DatabaseError.ConnectionFailed -> "connection failed"
                is DatabaseError.NotFound -> "not found: ${error.entityType}"
                is DatabaseError.MigrationFailed -> "migration ${error.fromVersion}->${error.toVersion}"
            }
            assertTrue(description.isNotEmpty())
        }
    }

    // ========================================
    // Retryable patterns
    // ========================================

    @Test
    fun `only ConnectionFailed is retryable`() {
        val errors: List<DatabaseError> = listOf(
            DatabaseError.ConstraintViolation(),
            DatabaseError.QueryFailed(),
            DatabaseError.ConnectionFailed(),
            DatabaseError.NotFound(),
            DatabaseError.MigrationFailed()
        )

        val retryableCount = errors.count { it.isRetryable }
        assertEquals(1, retryableCount)
        assertTrue(errors.single { it.isRetryable } is DatabaseError.ConnectionFailed)
    }
}
