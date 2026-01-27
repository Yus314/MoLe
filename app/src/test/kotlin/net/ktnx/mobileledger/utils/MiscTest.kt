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

import net.ktnx.mobileledger.core.common.utils.Misc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Misc] and extension functions.
 *
 * Tests verify:
 * - Float comparison utilities (extension functions)
 * - String utilities (extension functions)
 */
class MiscTest {

    // ========================================
    // isEffectivelyZero tests (Float extension)
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

    // ========================================
    // equalsWithTolerance tests (Float extension)
    // ========================================

    @Test
    fun `equalsWithTolerance returns true for exact same values`() {
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

    // ========================================
    // emptyToNull tests (String extension)
    // ========================================

    @Test
    fun `emptyToNull returns null for empty string`() {
        assertNull("".emptyToNull())
    }

    @Test
    fun `emptyToNull returns null for null input`() {
        assertNull((null as String?).emptyToNull())
    }

    @Test
    fun `emptyToNull returns string for non-empty input`() {
        assertEquals("hello", "hello".emptyToNull())
    }

    @Test
    fun `emptyToNull returns string with whitespace`() {
        assertEquals(" ", " ".emptyToNull())
    }

    // ========================================
    // withWrapHints tests (String extension)
    // ========================================

    @Test
    fun `withWrapHints returns null for null input`() {
        assertNull((null as String?).withWrapHints())
    }

    @Test
    fun `withWrapHints returns empty for no colons`() {
        assertEquals("", "hello".withWrapHints())
    }

    @Test
    fun `withWrapHints adds hint after colon`() {
        val result = "Assets:Bank".withWrapHints()
        assertTrue(result!!.contains(Misc.ZERO_WIDTH_SPACE))
    }

    @Test
    fun `withWrapHints handles multiple colons`() {
        val result = "Assets:Bank:Checking".withWrapHints()
        // Should have zero-width space after each colon except the last part
        val zwsCount = result!!.count { it == Misc.ZERO_WIDTH_SPACE }
        assertEquals(2, zwsCount)
    }

    @Test
    fun `withWrapHints returns empty for string without colon`() {
        assertEquals("", "NoColonHere".withWrapHints())
    }

    // ========================================
    // ZERO_WIDTH_SPACE constant test
    // ========================================

    @Test
    fun `ZERO_WIDTH_SPACE is correct unicode character`() {
        assertEquals('\u200B', Misc.ZERO_WIDTH_SPACE)
    }
}
