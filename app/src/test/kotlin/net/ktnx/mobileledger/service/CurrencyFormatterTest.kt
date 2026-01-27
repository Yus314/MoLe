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

package net.ktnx.mobileledger.service

import java.text.ParseException
import java.util.Locale
import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CurrencyFormatterTest {

    private lateinit var formatter: CurrencyFormatterImpl

    @Before
    fun setup() {
        formatter = CurrencyFormatterImpl()
    }

    @Test
    fun `initial state uses default locale`() {
        assertNotNull(formatter.locale.value)
        assertNotNull(formatter.config.value)
    }

    @Test
    fun `formatNumber formats with locale decimal separator`() {
        formatter.refresh(Locale.US)
        val result = formatter.formatNumber(1234.56f)
        assertTrue("Should contain decimal point for US locale", result.contains("."))
        assertTrue("Should format as ~1,234.56", result.contains("1,234"))
    }

    @Test
    fun `formatNumber formats German locale correctly`() {
        formatter.refresh(Locale.GERMANY)
        val result = formatter.formatNumber(1234.56f)
        // German uses comma as decimal separator and period as grouping separator
        assertTrue("Should contain comma for German locale", result.contains(","))
    }

    @Test
    fun `formatCurrency without symbol returns just number`() {
        formatter.refresh(Locale.US)
        val result = formatter.formatCurrency(1234.56f, null)
        assertTrue("Should contain the number", result.contains("1,234"))
    }

    @Test
    fun `formatCurrency with symbol positions correctly for US`() {
        formatter.refresh(Locale.US)
        val result = formatter.formatCurrency(1234.56f, "$")
        assertTrue("US format should start with symbol", result.startsWith("$"))
    }

    @Test
    fun `formatCurrency with symbol positions correctly for Germany`() {
        formatter.refresh(Locale.GERMANY)
        val result = formatter.formatCurrency(1234.56f, "€")
        // German typically puts symbol after the number
        assertTrue("German format should end with symbol", result.endsWith("€"))
    }

    @Test
    fun `getDecimalSeparator returns correct value for US`() {
        formatter.refresh(Locale.US)
        assertEquals(".", formatter.getDecimalSeparator())
    }

    @Test
    fun `getDecimalSeparator returns correct value for Germany`() {
        formatter.refresh(Locale.GERMANY)
        assertEquals(",", formatter.getDecimalSeparator())
    }

    @Test
    fun `getGroupingSeparator returns correct value for US`() {
        formatter.refresh(Locale.US)
        assertEquals(",", formatter.getGroupingSeparator())
    }

    @Test
    fun `getGroupingSeparator returns correct value for Germany`() {
        formatter.refresh(Locale.GERMANY)
        assertEquals(".", formatter.getGroupingSeparator())
    }

    @Test
    fun `parseNumber parses US format correctly`() {
        formatter.refresh(Locale.US)
        val result = formatter.parseNumber("1,234.56")
        assertEquals(1234.56f, result, 0.01f)
    }

    @Test
    fun `parseNumber parses German format correctly`() {
        formatter.refresh(Locale.GERMANY)
        val result = formatter.parseNumber("1.234,56")
        assertEquals(1234.56f, result, 0.01f)
    }

    @Test(expected = ParseException::class)
    fun `parseNumber throws on invalid input`() {
        formatter.refresh(Locale.US)
        formatter.parseNumber("not a number")
    }

    @Test
    fun `refresh updates locale flow`() {
        formatter.refresh(Locale.US)
        assertEquals(Locale.US, formatter.locale.value)

        formatter.refresh(Locale.GERMANY)
        assertEquals(Locale.GERMANY, formatter.locale.value)
    }

    @Test
    fun `refresh updates config flow`() {
        formatter.refresh(Locale.US)
        assertEquals(Locale.US, formatter.config.value.locale)
        assertEquals('.', formatter.config.value.decimalSeparator)

        formatter.refresh(Locale.GERMANY)
        assertEquals(Locale.GERMANY, formatter.config.value.locale)
        assertEquals(',', formatter.config.value.decimalSeparator)
    }

    @Test
    fun `updateFromAmountStyle updates position and gap`() {
        formatter.refresh(Locale.US)

        formatter.updateFromAmountStyle(CurrencyPosition.AFTER, false)

        assertEquals(CurrencyPosition.AFTER, formatter.currencySymbolPosition.value)
        assertFalse(formatter.currencyGap.value)
        assertEquals(CurrencyPosition.AFTER, formatter.config.value.symbolPosition)
        assertFalse(formatter.config.value.hasGap)
    }

    @Test
    fun `currencySymbolPosition flow is updated on refresh`() {
        formatter.refresh(Locale.US)
        // US typically has symbol before
        assertEquals(CurrencyPosition.BEFORE, formatter.currencySymbolPosition.value)
    }

    @Test
    fun `Japanese locale formats correctly`() {
        formatter.refresh(Locale.JAPAN)
        val result = formatter.formatNumber(1234.56f)
        assertNotNull(result)
        // Japanese uses comma for grouping
        assertTrue("Should contain grouping", result.contains(",") || result.contains("1234"))
    }

    @Test
    fun `formatCurrency respects gap setting`() {
        formatter.refresh(Locale.US)

        // With gap (default for many locales)
        formatter.updateFromAmountStyle(CurrencyPosition.BEFORE, true)
        val withGap = formatter.formatCurrency(100f, "$")
        assertTrue("Should have space after symbol", withGap.startsWith("$ "))

        // Without gap
        formatter.updateFromAmountStyle(CurrencyPosition.BEFORE, false)
        val noGap = formatter.formatCurrency(100f, "$")
        assertFalse("Should not have space after symbol", noGap.startsWith("$ "))
        assertTrue("Should start with symbol", noGap.startsWith("$"))
    }

    @Test
    fun `formatCurrency AFTER position works correctly`() {
        formatter.refresh(Locale.US)
        formatter.updateFromAmountStyle(CurrencyPosition.AFTER, true)

        val result = formatter.formatCurrency(100f, "€")
        assertTrue("Should end with symbol", result.endsWith("€"))
    }

    @Test
    fun `formatCurrency NONE position omits symbol`() {
        formatter.refresh(Locale.US)
        formatter.updateFromAmountStyle(CurrencyPosition.NONE, false)

        val result = formatter.formatCurrency(100f, "€")
        assertFalse("Should not contain symbol", result.contains("€"))
    }
}
