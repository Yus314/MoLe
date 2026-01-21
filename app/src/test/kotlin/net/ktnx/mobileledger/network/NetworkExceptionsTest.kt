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

package net.ktnx.mobileledger.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for network exception classes.
 *
 * Tests verify:
 * - Exception message handling
 * - Additional properties for each exception type
 */
class NetworkExceptionsTest {

    // ========================================
    // NetworkApiNotSupportedException tests
    // ========================================

    @Test
    fun `NetworkApiNotSupportedException stores message`() {
        val exception = NetworkApiNotSupportedException("API not supported")
        assertEquals("API not supported", exception.message)
    }

    @Test
    fun `NetworkApiNotSupportedException stores response body`() {
        val exception = NetworkApiNotSupportedException("API not supported", "Error body")
        assertEquals("Error body", exception.responseBody)
    }

    @Test
    fun `NetworkApiNotSupportedException response body defaults to null`() {
        val exception = NetworkApiNotSupportedException("API not supported")
        assertNull(exception.responseBody)
    }

    // ========================================
    // NetworkAuthenticationException tests
    // ========================================

    @Test
    fun `NetworkAuthenticationException stores message`() {
        val exception = NetworkAuthenticationException("Auth failed")
        assertEquals("Auth failed", exception.message)
    }

    @Test
    fun `NetworkAuthenticationException is throwable`() {
        val exception = NetworkAuthenticationException("Unauthorized")
        try {
            throw exception
        } catch (e: NetworkAuthenticationException) {
            assertEquals("Unauthorized", e.message)
        }
    }

    // ========================================
    // NetworkNotFoundException tests
    // ========================================

    @Test
    fun `NetworkNotFoundException stores message`() {
        val exception = NetworkNotFoundException("Resource not found")
        assertEquals("Resource not found", exception.message)
    }

    @Test
    fun `NetworkNotFoundException is throwable`() {
        val exception = NetworkNotFoundException("404 Not Found")
        try {
            throw exception
        } catch (e: NetworkNotFoundException) {
            assertEquals("404 Not Found", e.message)
        }
    }

    // ========================================
    // NetworkHttpException tests
    // ========================================

    @Test
    fun `NetworkHttpException stores status code`() {
        val exception = NetworkHttpException(500, "Internal Server Error")
        assertEquals(500, exception.statusCode)
    }

    @Test
    fun `NetworkHttpException stores message`() {
        val exception = NetworkHttpException(503, "Service Unavailable")
        assertEquals("Service Unavailable", exception.message)
    }

    @Test
    fun `NetworkHttpException handles client error codes`() {
        val exception = NetworkHttpException(400, "Bad Request")
        assertEquals(400, exception.statusCode)
        assertEquals("Bad Request", exception.message)
    }

    @Test
    fun `NetworkHttpException handles server error codes`() {
        val exception = NetworkHttpException(502, "Bad Gateway")
        assertEquals(502, exception.statusCode)
        assertEquals("Bad Gateway", exception.message)
    }

    @Test
    fun `NetworkHttpException is throwable`() {
        val exception = NetworkHttpException(429, "Too Many Requests")
        try {
            throw exception
        } catch (e: NetworkHttpException) {
            assertEquals(429, e.statusCode)
            assertEquals("Too Many Requests", e.message)
        }
    }
}
