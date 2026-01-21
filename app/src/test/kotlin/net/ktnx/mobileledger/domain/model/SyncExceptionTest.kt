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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SyncException class.
 *
 * Tests verify:
 * - Exception wraps SyncError correctly
 * - Message is derived from SyncError
 * - Can be thrown and caught
 */
class SyncExceptionTest {

    // ========================================
    // Construction tests
    // ========================================

    @Test
    fun `SyncException wraps NetworkError`() {
        val error = SyncError.NetworkError("Connection failed")
        val exception = SyncException(error)

        assertEquals(error, exception.syncError)
        assertTrue(exception.syncError is SyncError.NetworkError)
    }

    @Test
    fun `SyncException wraps TimeoutError`() {
        val error = SyncError.TimeoutError("Request timed out", 30000L)
        val exception = SyncException(error)

        assertEquals(error, exception.syncError)
        assertTrue(exception.syncError is SyncError.TimeoutError)
    }

    @Test
    fun `SyncException wraps AuthenticationError`() {
        val error = SyncError.AuthenticationError("Invalid credentials")
        val exception = SyncException(error)

        assertEquals(error, exception.syncError)
        assertTrue(exception.syncError is SyncError.AuthenticationError)
    }

    @Test
    fun `SyncException wraps ServerError`() {
        val error = SyncError.ServerError("Internal error", 500)
        val exception = SyncException(error)

        assertEquals(error, exception.syncError)
        assertTrue(exception.syncError is SyncError.ServerError)
    }

    @Test
    fun `SyncException wraps ValidationError`() {
        val error = SyncError.ValidationError("Invalid data")
        val exception = SyncException(error)

        assertEquals(error, exception.syncError)
        assertTrue(exception.syncError is SyncError.ValidationError)
    }

    @Test
    fun `SyncException wraps ParseError`() {
        val error = SyncError.ParseError("Failed to parse")
        val exception = SyncException(error)

        assertEquals(error, exception.syncError)
        assertTrue(exception.syncError is SyncError.ParseError)
    }

    @Test
    fun `SyncException wraps ApiVersionError`() {
        val error = SyncError.ApiVersionError("Unsupported version")
        val exception = SyncException(error)

        assertEquals(error, exception.syncError)
        assertTrue(exception.syncError is SyncError.ApiVersionError)
    }

    @Test
    fun `SyncException wraps Cancelled`() {
        val error = SyncError.Cancelled
        val exception = SyncException(error)

        assertEquals(error, exception.syncError)
        assertTrue(exception.syncError is SyncError.Cancelled)
    }

    @Test
    fun `SyncException wraps UnknownError`() {
        val error = SyncError.UnknownError("Unknown issue")
        val exception = SyncException(error)

        assertEquals(error, exception.syncError)
        assertTrue(exception.syncError is SyncError.UnknownError)
    }

    // ========================================
    // Message tests
    // ========================================

    @Test
    fun `exception message matches SyncError message`() {
        val error = SyncError.NetworkError("Connection failed")
        val exception = SyncException(error)

        assertEquals(error.message, exception.message)
    }

    @Test
    fun `exception message for default NetworkError`() {
        val error = SyncError.NetworkError()
        val exception = SyncException(error)

        assertEquals("ネットワークに接続できません", exception.message)
    }

    @Test
    fun `exception message for custom message`() {
        val error = SyncError.ServerError("Custom server error", 500)
        val exception = SyncException(error)

        assertEquals("Custom server error", exception.message)
    }

    // ========================================
    // Throw and catch tests
    // ========================================

    @Test
    fun `can throw and catch SyncException`() {
        val error = SyncError.NetworkError("Test error")

        var caught = false
        try {
            throw SyncException(error)
        } catch (e: SyncException) {
            caught = true
            assertEquals(error, e.syncError)
        }

        assertTrue(caught)
    }

    @Test
    fun `SyncException is an Exception`() {
        val error = SyncError.NetworkError("Test")
        val exception = SyncException(error)

        assertTrue(exception is Exception)
    }

    @Test
    fun `can extract SyncError from caught exception`() {
        val originalError = SyncError.ServerError("Server down", 503)

        val extracted: SyncError = try {
            throw SyncException(originalError)
        } catch (e: SyncException) {
            e.syncError
        }

        assertEquals(originalError, extracted)
        assertTrue(extracted is SyncError.ServerError)
        assertEquals(503, (extracted as SyncError.ServerError).httpCode)
    }

    // ========================================
    // When expression with exception
    // ========================================

    @Test
    fun `can pattern match on syncError type`() {
        val exception = SyncException(SyncError.ServerError("Error", 500))

        val isRetryable = when (exception.syncError) {
            is SyncError.NetworkError -> true
            is SyncError.TimeoutError -> true
            is SyncError.ServerError -> (exception.syncError as SyncError.ServerError).isRetryable
            else -> false
        }

        assertTrue(isRetryable) // 500 is retryable
    }

    @Test
    fun `handle different error types in when expression`() {
        val exceptions = listOf(
            SyncException(SyncError.NetworkError()),
            SyncException(SyncError.TimeoutError()),
            SyncException(SyncError.AuthenticationError()),
            SyncException(SyncError.ServerError("Error", 500)),
            SyncException(SyncError.Cancelled)
        )

        exceptions.forEach { exception ->
            val description = when (exception.syncError) {
                is SyncError.NetworkError -> "network"
                is SyncError.TimeoutError -> "timeout"
                is SyncError.AuthenticationError -> "auth"
                is SyncError.ServerError -> "server"
                is SyncError.ValidationError -> "validation"
                is SyncError.ParseError -> "parse"
                is SyncError.ApiVersionError -> "api"
                is SyncError.Cancelled -> "cancelled"
                is SyncError.UnknownError -> "unknown"
            }
            assertTrue(description.isNotEmpty())
        }
    }
}
