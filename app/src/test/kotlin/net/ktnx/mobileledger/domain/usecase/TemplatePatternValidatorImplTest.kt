/*
 * Copyright © 2024 Damyan Ivanov.
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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TemplatePatternValidatorImplTest {

    private lateinit var validator: TemplatePatternValidatorImpl

    @Before
    fun setup() {
        validator = TemplatePatternValidatorImpl()
    }

    @Test
    fun `validate empty pattern returns error`() {
        val result = validator.validate("", "test text")

        assertNotNull(result.error)
        assertEquals("パターンが空です", result.error)
        assertNull(result.matchResult)
        assertEquals(0, result.groupCount)
    }

    @Test
    fun `validate invalid pattern returns syntax error`() {
        val result = validator.validate("[invalid", "test text")

        assertNotNull(result.error)
        assertNull(result.matchResult)
        assertEquals(0, result.groupCount)
    }

    @Test
    fun `validate valid pattern with no test text returns no match result`() {
        val result = validator.validate("test", "")

        assertNull(result.error)
        assertNull(result.matchResult)
        assertEquals(0, result.groupCount)
    }

    @Test
    fun `validate valid pattern returns group count`() {
        val result = validator.validate("(\\d+)-(\\d+)", "")

        assertNull(result.error)
        assertEquals(2, result.groupCount)
    }

    @Test
    fun `validate matching pattern returns annotated string`() {
        val result = validator.validate("test", "this is a test string")

        assertNull(result.error)
        assertNotNull(result.matchResult)
        assertEquals("this is a test string", result.matchResult?.text)
    }

    @Test
    fun `validate non-matching pattern returns grayed text`() {
        val result = validator.validate("xyz", "this is a test string")

        assertNull(result.error)
        assertNotNull(result.matchResult)
        assertEquals("this is a test string", result.matchResult?.text)
    }

    @Test
    fun `validate pattern with capturing groups`() {
        val result = validator.validate("(\\w+)@(\\w+)", "user@domain")

        assertNull(result.error)
        assertNotNull(result.matchResult)
        assertEquals(2, result.groupCount)
        assertEquals("user@domain", result.matchResult?.text)
    }

    @Test
    fun `validate pattern matching at start of text`() {
        val result = validator.validate("^hello", "hello world")

        assertNull(result.error)
        assertNotNull(result.matchResult)
        assertEquals("hello world", result.matchResult?.text)
    }

    @Test
    fun `validate pattern matching at end of text`() {
        val result = validator.validate("world$", "hello world")

        assertNull(result.error)
        assertNotNull(result.matchResult)
        assertEquals("hello world", result.matchResult?.text)
    }
}
