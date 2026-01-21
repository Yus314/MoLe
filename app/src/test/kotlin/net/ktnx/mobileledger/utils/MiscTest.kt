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
 * Unit tests for [Misc].
 *
 * Tests verify:
 * - Float comparison utilities
 * - String utilities
 * - Null handling utilities
 */
class MiscTest {

    // ========================================
    // isZero tests
    // ========================================

    @Test
    fun `isZero returns true for exact zero`() {
        assertTrue(Misc.isZero(0.0f))
    }

    @Test
    fun `isZero returns true for very small positive`() {
        assertTrue(Misc.isZero(0.000001f))
    }

    @Test
    fun `isZero returns true for very small negative`() {
        assertTrue(Misc.isZero(-0.000001f))
    }

    @Test
    fun `isZero returns false for non-zero positive`() {
        assertFalse(Misc.isZero(0.01f))
    }

    @Test
    fun `isZero returns false for non-zero negative`() {
        assertFalse(Misc.isZero(-0.01f))
    }

    @Test
    fun `isZero returns false for large positive`() {
        assertFalse(Misc.isZero(100.0f))
    }

    @Test
    fun `isZero returns false for large negative`() {
        assertFalse(Misc.isZero(-100.0f))
    }

    // ========================================
    // equalFloats tests
    // ========================================

    @Test
    fun `equalFloats returns true for exact same values`() {
        assertTrue(Misc.equalFloats(10.5f, 10.5f))
    }

    @Test
    fun `equalFloats returns true for very close values`() {
        assertTrue(Misc.equalFloats(10.0f, 10.0000001f))
    }

    @Test
    fun `equalFloats returns false for different values`() {
        assertFalse(Misc.equalFloats(10.0f, 10.1f))
    }

    @Test
    fun `equalFloats returns true for both zero`() {
        assertTrue(Misc.equalFloats(0.0f, 0.0f))
    }

    @Test
    fun `equalFloats returns true for positive and negative zero`() {
        assertTrue(Misc.equalFloats(0.0f, -0.0f))
    }

    @Test
    fun `equalFloats returns false for large difference`() {
        assertFalse(Misc.equalFloats(100.0f, 200.0f))
    }

    // ========================================
    // emptyIsNull tests
    // ========================================

    @Test
    fun `emptyIsNull returns null for empty string`() {
        assertNull(Misc.emptyIsNull(""))
    }

    @Test
    fun `emptyIsNull returns null for null input`() {
        assertNull(Misc.emptyIsNull(null))
    }

    @Test
    fun `emptyIsNull returns string for non-empty input`() {
        assertEquals("hello", Misc.emptyIsNull("hello"))
    }

    @Test
    fun `emptyIsNull returns string with whitespace`() {
        assertEquals(" ", Misc.emptyIsNull(" "))
    }

    // ========================================
    // nullIsEmpty tests - String
    // ========================================

    @Test
    fun `nullIsEmpty returns empty for null`() {
        assertEquals("", Misc.nullIsEmpty(null as String?))
    }

    @Test
    fun `nullIsEmpty returns string for non-null`() {
        assertEquals("hello", Misc.nullIsEmpty("hello"))
    }

    @Test
    fun `nullIsEmpty returns empty for empty input`() {
        assertEquals("", Misc.nullIsEmpty(""))
    }

    // ========================================
    // equalStrings tests
    // ========================================

    @Test
    fun `equalStrings returns true for same strings`() {
        assertTrue(Misc.equalStrings("hello", "hello"))
    }

    @Test
    fun `equalStrings returns false for different strings`() {
        assertFalse(Misc.equalStrings("hello", "world"))
    }

    @Test
    fun `equalStrings returns true for both null`() {
        assertTrue(Misc.equalStrings(null, null))
    }

    @Test
    fun `equalStrings returns true for null and empty`() {
        assertTrue(Misc.equalStrings(null, ""))
    }

    @Test
    fun `equalStrings returns true for empty and null`() {
        assertTrue(Misc.equalStrings("", null))
    }

    @Test
    fun `equalStrings returns false for null and non-empty`() {
        assertFalse(Misc.equalStrings(null, "hello"))
    }

    // ========================================
    // trim tests
    // ========================================

    @Test
    fun `trim returns null for null input`() {
        assertNull(Misc.trim(null))
    }

    @Test
    fun `trim removes leading whitespace`() {
        assertEquals("hello", Misc.trim("  hello"))
    }

    @Test
    fun `trim removes trailing whitespace`() {
        assertEquals("hello", Misc.trim("hello  "))
    }

    @Test
    fun `trim removes both leading and trailing whitespace`() {
        assertEquals("hello", Misc.trim("  hello  "))
    }

    @Test
    fun `trim returns empty for whitespace only`() {
        assertEquals("", Misc.trim("   "))
    }

    @Test
    fun `trim preserves internal whitespace`() {
        assertEquals("hello world", Misc.trim("  hello world  "))
    }

    // ========================================
    // equalIntegers tests
    // ========================================

    @Test
    fun `equalIntegers returns true for same values`() {
        assertTrue(Misc.equalIntegers(5, 5))
    }

    @Test
    fun `equalIntegers returns false for different values`() {
        assertFalse(Misc.equalIntegers(5, 10))
    }

    @Test
    fun `equalIntegers returns true for both null`() {
        assertTrue(Misc.equalIntegers(null, null))
    }

    @Test
    fun `equalIntegers returns false for first null`() {
        assertFalse(Misc.equalIntegers(null, 5))
    }

    @Test
    fun `equalIntegers returns false for second null`() {
        assertFalse(Misc.equalIntegers(5, null))
    }

    @Test
    fun `equalIntegers handles zero`() {
        assertTrue(Misc.equalIntegers(0, 0))
    }

    @Test
    fun `equalIntegers handles negative values`() {
        assertTrue(Misc.equalIntegers(-5, -5))
    }

    // ========================================
    // equalLongs tests
    // ========================================

    @Test
    fun `equalLongs returns true for same values`() {
        assertTrue(Misc.equalLongs(5L, 5L))
    }

    @Test
    fun `equalLongs returns false for different values`() {
        assertFalse(Misc.equalLongs(5L, 10L))
    }

    @Test
    fun `equalLongs returns true for both null`() {
        assertTrue(Misc.equalLongs(null, null))
    }

    @Test
    fun `equalLongs returns false for first null`() {
        assertFalse(Misc.equalLongs(null, 5L))
    }

    @Test
    fun `equalLongs returns false for second null`() {
        assertFalse(Misc.equalLongs(5L, null))
    }

    @Test
    fun `equalLongs handles large values`() {
        assertTrue(Misc.equalLongs(Long.MAX_VALUE, Long.MAX_VALUE))
    }

    // ========================================
    // addWrapHints tests
    // ========================================

    @Test
    fun `addWrapHints returns null for null input`() {
        assertNull(Misc.addWrapHints(null))
    }

    @Test
    fun `addWrapHints returns empty for no colons`() {
        assertEquals("", Misc.addWrapHints("hello"))
    }

    @Test
    fun `addWrapHints adds hint after colon`() {
        val result = Misc.addWrapHints("Assets:Bank")
        assertTrue(result!!.contains(Misc.ZERO_WIDTH_SPACE))
    }

    @Test
    fun `addWrapHints handles multiple colons`() {
        val result = Misc.addWrapHints("Assets:Bank:Checking")
        // Should have zero-width space after each colon except the last part
        val zwsCount = result!!.count { it == Misc.ZERO_WIDTH_SPACE }
        assertEquals(2, zwsCount)
    }

    @Test
    fun `addWrapHints returns empty for string without colon`() {
        assertEquals("", Misc.addWrapHints("NoColonHere"))
    }

    // ========================================
    // ZERO_WIDTH_SPACE constant test
    // ========================================

    @Test
    fun `ZERO_WIDTH_SPACE is correct unicode character`() {
        assertEquals('\u200B', Misc.ZERO_WIDTH_SPACE)
    }
}
