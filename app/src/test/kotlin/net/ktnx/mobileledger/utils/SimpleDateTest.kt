/*
 * Copyright © 2020 Damyan Ivanov.
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

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SimpleDate].
 *
 * Tests verify:
 * - Comparison operations (compareTo, earlierThan, laterThan)
 * - Conversion to/from Calendar and Date
 * - String representation
 * - Factory methods
 */
class SimpleDateTest {

    // ========================================
    // Comparison tests
    // ========================================

    @Test
    fun `compareTo returns positive when first date is later`() {
        val d1 = SimpleDate(2020, 6, 1)
        val d2 = SimpleDate(2019, 7, 6)
        assertTrue(d1.compareTo(d2) > 0)
    }

    @Test
    fun `compareTo returns negative when first date is earlier`() {
        val d1 = SimpleDate(2019, 6, 1)
        val d2 = SimpleDate(2020, 7, 6)
        assertTrue(d1.compareTo(d2) < 0)
    }

    @Test
    fun `compareTo returns zero for equal dates`() {
        val d1 = SimpleDate(2020, 6, 15)
        val d2 = SimpleDate(2020, 6, 15)
        assertEquals(0, d1.compareTo(d2))
    }

    @Test
    fun `compareTo compares year first`() {
        val d1 = SimpleDate(2021, 1, 1)
        val d2 = SimpleDate(2020, 12, 31)
        assertTrue(d1.compareTo(d2) > 0)
    }

    @Test
    fun `compareTo compares month when year is same`() {
        val d1 = SimpleDate(2020, 6, 1)
        val d2 = SimpleDate(2020, 5, 31)
        assertTrue(d1.compareTo(d2) > 0)
    }

    @Test
    fun `compareTo compares day when year and month are same`() {
        val d1 = SimpleDate(2020, 6, 15)
        val d2 = SimpleDate(2020, 6, 14)
        assertTrue(d1.compareTo(d2) > 0)
    }

    // ========================================
    // earlierThan tests
    // ========================================

    @Test
    fun `earlierThan returns true when first date is earlier by year`() {
        val d1 = SimpleDate(2019, 6, 1)
        val d2 = SimpleDate(2020, 6, 1)
        assertTrue(d1.earlierThan(d2))
    }

    @Test
    fun `earlierThan returns true when first date is earlier by month`() {
        val d1 = SimpleDate(2020, 5, 1)
        val d2 = SimpleDate(2020, 6, 1)
        assertTrue(d1.earlierThan(d2))
    }

    @Test
    fun `earlierThan returns true when first date is earlier by day`() {
        val d1 = SimpleDate(2020, 6, 1)
        val d2 = SimpleDate(2020, 6, 15)
        assertTrue(d1.earlierThan(d2))
    }

    @Test
    fun `earlierThan returns false for equal dates`() {
        val d1 = SimpleDate(2020, 6, 15)
        val d2 = SimpleDate(2020, 6, 15)
        assertFalse(d1.earlierThan(d2))
    }

    @Test
    fun `earlierThan returns false when first date is later`() {
        val d1 = SimpleDate(2020, 6, 15)
        val d2 = SimpleDate(2020, 6, 1)
        assertFalse(d1.earlierThan(d2))
    }

    // ========================================
    // laterThan tests
    // ========================================

    @Test
    fun `laterThan returns true when first date is later by year`() {
        val d1 = SimpleDate(2021, 6, 1)
        val d2 = SimpleDate(2020, 6, 1)
        assertTrue(d1.laterThan(d2))
    }

    @Test
    fun `laterThan returns true when first date is later by month`() {
        val d1 = SimpleDate(2020, 7, 1)
        val d2 = SimpleDate(2020, 6, 1)
        assertTrue(d1.laterThan(d2))
    }

    @Test
    fun `laterThan returns true when first date is later by day`() {
        val d1 = SimpleDate(2020, 6, 15)
        val d2 = SimpleDate(2020, 6, 1)
        assertTrue(d1.laterThan(d2))
    }

    @Test
    fun `laterThan returns false for equal dates`() {
        val d1 = SimpleDate(2020, 6, 15)
        val d2 = SimpleDate(2020, 6, 15)
        assertFalse(d1.laterThan(d2))
    }

    @Test
    fun `laterThan returns false when first date is earlier`() {
        val d1 = SimpleDate(2020, 6, 1)
        val d2 = SimpleDate(2020, 6, 15)
        assertFalse(d1.laterThan(d2))
    }

    // ========================================
    // Conversion tests
    // ========================================

    @Test
    fun `toCalendar returns calendar with correct year`() {
        val date = SimpleDate(2020, 6, 15)
        val calendar = date.toCalendar()
        assertEquals(2020, calendar.get(Calendar.YEAR))
    }

    @Test
    fun `toCalendar returns calendar with correct month`() {
        val date = SimpleDate(2020, 6, 15)
        val calendar = date.toCalendar()
        // Calendar months are 0-indexed
        assertEquals(5, calendar.get(Calendar.MONTH))
    }

    @Test
    fun `toCalendar returns calendar with correct day`() {
        val date = SimpleDate(2020, 6, 15)
        val calendar = date.toCalendar()
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `toDate returns non-null Date`() {
        val date = SimpleDate(2020, 6, 15)
        assertNotNull(date.toDate())
    }

    @Test
    fun `asCalendar returns calendar with correct values`() {
        val date = SimpleDate(2020, 6, 15)
        val calendar = date.asCalendar()
        assertEquals(2020, calendar.get(Calendar.YEAR))
        assertEquals(6, calendar.get(Calendar.MONTH))
        assertEquals(15, calendar.get(Calendar.DAY_OF_MONTH))
    }

    // ========================================
    // Factory method tests
    // ========================================

    @Test
    fun `fromCalendar creates date with correct values`() {
        val calendar = Calendar.getInstance().apply {
            set(2020, 5, 15) // Month is 0-indexed
        }
        val date = SimpleDate.fromCalendar(calendar)
        assertEquals(2020, date.year)
        assertEquals(6, date.month) // SimpleDate uses 1-indexed months
        assertEquals(15, date.day)
    }

    @Test
    fun `fromDate creates date from Date object`() {
        val calendar = Calendar.getInstance().apply {
            set(2020, 5, 15)
        }
        val date = SimpleDate.fromDate(calendar.time)
        assertEquals(2020, date.year)
        assertEquals(6, date.month)
        assertEquals(15, date.day)
    }

    @Test
    fun `today returns non-null date`() {
        val today = SimpleDate.today()
        assertNotNull(today)
        assertTrue(today.year >= 2020)
        assertTrue(today.month in 1..12)
        assertTrue(today.day in 1..31)
    }

    // ========================================
    // toString tests
    // ========================================

    @Test
    fun `toString returns ISO format`() {
        val date = SimpleDate(2020, 6, 15)
        assertEquals("2020-06-15", date.toString())
    }

    @Test
    fun `toString pads single digit month`() {
        val date = SimpleDate(2020, 1, 15)
        assertEquals("2020-01-15", date.toString())
    }

    @Test
    fun `toString pads single digit day`() {
        val date = SimpleDate(2020, 6, 5)
        assertEquals("2020-06-05", date.toString())
    }

    // ========================================
    // Data class tests
    // ========================================

    @Test
    fun `equals returns true for equal dates`() {
        val d1 = SimpleDate(2020, 6, 15)
        val d2 = SimpleDate(2020, 6, 15)
        assertEquals(d1, d2)
    }

    @Test
    fun `hashCode is same for equal dates`() {
        val d1 = SimpleDate(2020, 6, 15)
        val d2 = SimpleDate(2020, 6, 15)
        assertEquals(d1.hashCode(), d2.hashCode())
    }
}
