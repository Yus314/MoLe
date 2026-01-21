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

package net.ktnx.mobileledger.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for extension functions in Extensions.kt.
 *
 * Tests verify:
 * - String extensions (emptyToNull, withWrapHints)
 * - Float extensions (isEffectivelyZero, equalsWithTolerance)
 */
class ExtensionsTest {

    // ========================================
    // String.emptyToNull tests
    // ========================================

    @Test
    fun `emptyToNull returns null for empty string`() {
        val input: String = ""
        assertNull(input.emptyToNull())
    }

    @Test
    fun `emptyToNull returns null for null string`() {
        val input: String? = null
        assertNull(input.emptyToNull())
    }

    @Test
    fun `emptyToNull returns string for non-empty input`() {
        val input = "hello"
        assertEquals("hello", input.emptyToNull())
    }

    @Test
    fun `emptyToNull returns string with only whitespace`() {
        val input = "   "
        assertEquals("   ", input.emptyToNull())
    }

    @Test
    fun `emptyToNull returns string for single character`() {
        val input = "a"
        assertEquals("a", input.emptyToNull())
    }

    // ========================================
    // String.withWrapHints tests
    // ========================================

    @Test
    fun `withWrapHints returns null for null input`() {
        val input: String? = null
        assertNull(input.withWrapHints())
    }

    @Test
    fun `withWrapHints adds hint after colon`() {
        val input = "Assets:Bank"
        val result = input.withWrapHints()
        assertTrue("Result should contain zero-width space", result!!.contains(Misc.ZERO_WIDTH_SPACE))
    }

    @Test
    fun `withWrapHints handles multiple colons`() {
        val input = "Assets:Bank:Checking"
        val result = input.withWrapHints()
        val zwsCount = result!!.count { it == Misc.ZERO_WIDTH_SPACE }
        assertEquals("Should have 2 zero-width spaces", 2, zwsCount)
    }

    @Test
    fun `withWrapHints returns empty for no colons`() {
        val input = "NoColonHere"
        val result = input.withWrapHints()
        assertEquals("", result)
    }

    @Test
    fun `withWrapHints handles empty string`() {
        val input = ""
        val result = input.withWrapHints()
        assertEquals("", result)
    }

    @Test
    fun `withWrapHints handles string starting with colon`() {
        val input = ":Bank"
        val result = input.withWrapHints()
        assertTrue("Should contain zero-width space", result!!.contains(Misc.ZERO_WIDTH_SPACE))
    }

    @Test
    fun `withWrapHints handles string ending with colon`() {
        val input = "Assets:"
        val result = input.withWrapHints()
        assertTrue("Should contain zero-width space", result!!.contains(Misc.ZERO_WIDTH_SPACE))
    }

    @Test
    fun `withWrapHints handles consecutive colons`() {
        // The algorithm only finds colons after lastPos + 1, so consecutive colons
        // result in only one ZWS after the first colon
        val input = "Assets::Bank"
        val result = input.withWrapHints()
        val zwsCount = result!!.count { it == Misc.ZERO_WIDTH_SPACE }
        assertEquals("Should have 1 zero-width space (after first colon)", 1, zwsCount)
    }

    // ========================================
    // Float.isEffectivelyZero tests
    // ========================================

    @Test
    fun `isEffectivelyZero returns true for exact zero`() {
        assertTrue(0.0f.isEffectivelyZero())
    }

    @Test
    fun `isEffectivelyZero returns true for very small positive`() {
        assertTrue(0.000001f.isEffectivelyZero())
    }

    @Test
    fun `isEffectivelyZero returns true for very small negative`() {
        assertTrue((-0.000001f).isEffectivelyZero())
    }

    @Test
    fun `isEffectivelyZero returns false for non-zero positive`() {
        assertFalse(0.01f.isEffectivelyZero())
    }

    @Test
    fun `isEffectivelyZero returns false for non-zero negative`() {
        assertFalse((-0.01f).isEffectivelyZero())
    }

    @Test
    fun `isEffectivelyZero returns false for large positive`() {
        assertFalse(100.0f.isEffectivelyZero())
    }

    @Test
    fun `isEffectivelyZero returns false for large negative`() {
        assertFalse((-100.0f).isEffectivelyZero())
    }

    @Test
    fun `isEffectivelyZero returns true for negative zero`() {
        assertTrue((-0.0f).isEffectivelyZero())
    }

    // ========================================
    // Float.equalsWithTolerance tests
    // ========================================

    @Test
    fun `equalsWithTolerance returns true for same values`() {
        assertTrue(10.5f.equalsWithTolerance(10.5f))
    }

    @Test
    fun `equalsWithTolerance returns true for very close values`() {
        assertTrue(10.0f.equalsWithTolerance(10.0000001f))
    }

    @Test
    fun `equalsWithTolerance returns false for different values`() {
        assertFalse(10.0f.equalsWithTolerance(10.1f))
    }

    @Test
    fun `equalsWithTolerance returns true for both zero`() {
        assertTrue(0.0f.equalsWithTolerance(0.0f))
    }

    @Test
    fun `equalsWithTolerance returns true for positive and negative zero`() {
        assertTrue(0.0f.equalsWithTolerance(-0.0f))
    }

    @Test
    fun `equalsWithTolerance returns false for large difference`() {
        assertFalse(100.0f.equalsWithTolerance(200.0f))
    }

    @Test
    fun `equalsWithTolerance is symmetric`() {
        val a = 10.0f
        val b = 10.0000001f
        assertEquals(a.equalsWithTolerance(b), b.equalsWithTolerance(a))
    }

    @Test
    fun `equalsWithTolerance works with negative values`() {
        assertTrue((-10.5f).equalsWithTolerance(-10.5f))
    }

    @Test
    fun `equalsWithTolerance returns false for opposite signs`() {
        assertFalse(1.0f.equalsWithTolerance(-1.0f))
    }
}
