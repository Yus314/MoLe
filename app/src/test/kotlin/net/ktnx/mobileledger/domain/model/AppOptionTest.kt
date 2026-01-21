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
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [AppOption].
 *
 * Tests verify:
 * - Data class properties
 * - valueAsLong() conversion
 * - Companion object constants
 */
class AppOptionTest {

    // ========================================
    // Data class property tests
    // ========================================

    @Test
    fun `properties are stored correctly`() {
        val option = AppOption(
            profileId = 123L,
            name = "test_option",
            value = "test_value"
        )

        assertEquals(123L, option.profileId)
        assertEquals("test_option", option.name)
        assertEquals("test_value", option.value)
    }

    @Test
    fun `null value is allowed`() {
        val option = AppOption(
            profileId = 1L,
            name = "option",
            value = null
        )

        assertNull(option.value)
    }

    // ========================================
    // valueAsLong tests
    // ========================================

    @Test
    fun `valueAsLong returns Long for valid number`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = "1705689600"
        )

        assertEquals(1705689600L, option.valueAsLong())
    }

    @Test
    fun `valueAsLong returns Long for zero`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = "0"
        )

        assertEquals(0L, option.valueAsLong())
    }

    @Test
    fun `valueAsLong returns Long for negative number`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = "-123"
        )

        assertEquals(-123L, option.valueAsLong())
    }

    @Test
    fun `valueAsLong returns Long for max value`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = Long.MAX_VALUE.toString()
        )

        assertEquals(Long.MAX_VALUE, option.valueAsLong())
    }

    @Test
    fun `valueAsLong returns Long for min value`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = Long.MIN_VALUE.toString()
        )

        assertEquals(Long.MIN_VALUE, option.valueAsLong())
    }

    @Test
    fun `valueAsLong returns null for null value`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = null
        )

        assertNull(option.valueAsLong())
    }

    @Test
    fun `valueAsLong returns null for non-numeric string`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = "not_a_number"
        )

        assertNull(option.valueAsLong())
    }

    @Test
    fun `valueAsLong returns null for empty string`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = ""
        )

        assertNull(option.valueAsLong())
    }

    @Test
    fun `valueAsLong returns null for float value`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = "123.45"
        )

        assertNull(option.valueAsLong())
    }

    @Test
    fun `valueAsLong returns null for string with spaces`() {
        val option = AppOption(
            profileId = 1L,
            name = "timestamp",
            value = " 123 "
        )

        assertNull(option.valueAsLong())
    }

    // ========================================
    // Companion object tests
    // ========================================

    @Test
    fun `OPT_LAST_SCRAPE constant value`() {
        assertEquals("last_scrape", AppOption.OPT_LAST_SCRAPE)
    }

    // ========================================
    // Data class behavior tests
    // ========================================

    @Test
    fun `copy preserves unchanged fields`() {
        val original = AppOption(
            profileId = 1L,
            name = "test",
            value = "original"
        )

        val modified = original.copy(value = "modified")

        assertEquals(1L, modified.profileId)
        assertEquals("test", modified.name)
        assertEquals("modified", modified.value)
    }

    @Test
    fun `equals compares all fields`() {
        val option1 = AppOption(1L, "name", "value")
        val option2 = AppOption(1L, "name", "value")

        assertEquals(option1, option2)
    }

    @Test
    fun `hashCode is consistent`() {
        val option1 = AppOption(1L, "name", "value")
        val option2 = AppOption(1L, "name", "value")

        assertEquals(option1.hashCode(), option2.hashCode())
    }
}
