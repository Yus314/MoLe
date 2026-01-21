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

package net.ktnx.mobileledger.json

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ApiNotSupportedException].
 *
 * Tests verify:
 * - All constructors work correctly
 * - Exception properties are set correctly
 */
class ApiNotSupportedExceptionTest {

    @Test
    fun `constructor with no arguments creates exception`() {
        val exception = ApiNotSupportedException()
        assertNotNull(exception)
        assertNull(exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `constructor with message creates exception with message`() {
        val exception = ApiNotSupportedException("Test message")
        assertEquals("Test message", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `constructor with message and cause creates exception`() {
        val cause = RuntimeException("Original error")
        val exception = ApiNotSupportedException("Test message", cause)
        assertEquals("Test message", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `constructor with cause creates exception`() {
        val cause = IllegalStateException("Root cause")
        val exception = ApiNotSupportedException(cause)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `constructor with all parameters creates exception`() {
        val cause = Exception("Cause")
        val exception = ApiNotSupportedException("Message", cause, true, true)
        assertEquals("Message", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `exception extends Throwable`() {
        val exception = ApiNotSupportedException("Test")
        assertTrue(exception is Throwable)
    }

    @Test
    fun `exception can be thrown and caught`() {
        var caught = false
        try {
            throw ApiNotSupportedException("API not supported")
        } catch (e: ApiNotSupportedException) {
            caught = true
            assertEquals("API not supported", e.message)
        }
        assertTrue(caught)
    }

    @Test
    fun `constructor with null message`() {
        val exception = ApiNotSupportedException(null as String?)
        assertNull(exception.message)
    }

    @Test
    fun `constructor with null cause`() {
        val exception = ApiNotSupportedException(null as Throwable?)
        assertNull(exception.cause)
    }
}
