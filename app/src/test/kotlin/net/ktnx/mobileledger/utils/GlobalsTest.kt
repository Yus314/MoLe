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

import java.text.ParseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for [Globals].
 *
 * Tests verify:
 * - Date parsing (ledger format and ISO format)
 * - Date formatting
 */
class GlobalsTest {

    // ========================================
    // parseLedgerDate tests
    // ========================================

    @Test
    fun `parseLedgerDate parses full date with year`() {
        val date = Globals.parseLedgerDate("2024/06/15")
        assertEquals(2024, date.year)
        assertEquals(6, date.month)
        assertEquals(15, date.day)
    }

    @Test
    fun `parseLedgerDate parses date with single digit month and day`() {
        val date = Globals.parseLedgerDate("2024/1/5")
        assertEquals(2024, date.year)
        assertEquals(1, date.month)
        assertEquals(5, date.day)
    }

    @Test
    fun `parseLedgerDate parses date without year uses current year`() {
        val date = Globals.parseLedgerDate("6/15")
        val today = SimpleDate.today()
        assertEquals(today.year, date.year)
        assertEquals(6, date.month)
        assertEquals(15, date.day)
    }

    @Test
    fun `parseLedgerDate parses day only uses current year and month`() {
        val date = Globals.parseLedgerDate("15")
        val today = SimpleDate.today()
        assertEquals(today.year, date.year)
        assertEquals(today.month, date.month)
        assertEquals(15, date.day)
    }

    @Test(expected = ParseException::class)
    fun `parseLedgerDate throws for invalid format`() {
        Globals.parseLedgerDate("invalid-date")
    }

    @Test(expected = ParseException::class)
    fun `parseLedgerDate throws for empty string`() {
        Globals.parseLedgerDate("")
    }

    // ========================================
    // parseIsoDate tests
    // ========================================

    @Test
    fun `parseIsoDate parses standard ISO date`() {
        val date = Globals.parseIsoDate("2024-06-15")
        assertEquals(2024, date.year)
        assertEquals(6, date.month)
        assertEquals(15, date.day)
    }

    @Test
    fun `parseIsoDate parses date at beginning of year`() {
        val date = Globals.parseIsoDate("2024-01-01")
        assertEquals(2024, date.year)
        assertEquals(1, date.month)
        assertEquals(1, date.day)
    }

    @Test
    fun `parseIsoDate parses date at end of year`() {
        val date = Globals.parseIsoDate("2024-12-31")
        assertEquals(2024, date.year)
        assertEquals(12, date.month)
        assertEquals(31, date.day)
    }

    @Test(expected = ParseException::class)
    fun `parseIsoDate throws for invalid format`() {
        Globals.parseIsoDate("2024/06/15")
    }

    // ========================================
    // formatLedgerDate tests
    // ========================================

    @Test
    fun `formatLedgerDate formats date correctly`() {
        val date = SimpleDate(2024, 6, 15)
        val result = Globals.formatLedgerDate(date)
        assertEquals("2024/06/15", result)
    }

    @Test
    fun `formatLedgerDate pads single digit values`() {
        val date = SimpleDate(2024, 1, 5)
        val result = Globals.formatLedgerDate(date)
        assertEquals("2024/01/05", result)
    }

    // ========================================
    // formatIsoDate tests
    // ========================================

    @Test
    fun `formatIsoDate formats date correctly`() {
        val date = SimpleDate(2024, 6, 15)
        val result = Globals.formatIsoDate(date)
        assertEquals("2024-06-15", result)
    }

    @Test
    fun `formatIsoDate pads single digit values`() {
        val date = SimpleDate(2024, 1, 5)
        val result = Globals.formatIsoDate(date)
        assertEquals("2024-01-05", result)
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `parseLedgerDate and formatLedgerDate round-trip`() {
        val original = "2024/06/15"
        val date = Globals.parseLedgerDate(original)
        val formatted = Globals.formatLedgerDate(date)
        assertEquals(original, formatted)
    }

    @Test
    fun `parseIsoDate and formatIsoDate round-trip`() {
        val original = "2024-06-15"
        val date = Globals.parseIsoDate(original)
        val formatted = Globals.formatIsoDate(date)
        assertEquals(original, formatted)
    }

    // ========================================
    // parseLedgerDateAsCalendar tests
    // ========================================

    @Test
    fun `parseLedgerDateAsCalendar returns Calendar`() {
        val calendar = Globals.parseLedgerDateAsCalendar("2024/06/15")
        assertNotNull(calendar)
        // Note: Calendar months are 0-indexed
        assertEquals(2024, calendar.get(java.util.Calendar.YEAR))
        assertEquals(5, calendar.get(java.util.Calendar.MONTH)) // June = 5
        assertEquals(15, calendar.get(java.util.Calendar.DAY_OF_MONTH))
    }

    // ========================================
    // Constants tests
    // ========================================

    @Test
    fun `developerEmail is set`() {
        assertEquals("dam+mole-crash@ktnx.net", Globals.developerEmail)
    }
}
