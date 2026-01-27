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

import java.util.Locale
import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [CurrencyFormatConfig].
 *
 * Tests verify:
 * - Factory method from locale
 * - Correct extraction of decimal/grouping separators
 * - Default values
 */
class CurrencyFormatConfigTest {

    @Test
    fun `fromLocale creates config with US locale`() {
        // When
        val config = CurrencyFormatConfig.fromLocale(Locale.US)

        // Then
        assertEquals(Locale.US, config.locale)
        assertEquals('.', config.decimalSeparator)
        assertEquals(',', config.groupingSeparator)
    }

    @Test
    fun `fromLocale creates config with German locale`() {
        // When
        val config = CurrencyFormatConfig.fromLocale(Locale.GERMANY)

        // Then
        assertEquals(Locale.GERMANY, config.locale)
        assertEquals(',', config.decimalSeparator)
        assertEquals('.', config.groupingSeparator)
    }

    @Test
    fun `fromLocale creates config with French locale`() {
        // When
        val config = CurrencyFormatConfig.fromLocale(Locale.FRANCE)

        // Then
        assertEquals(Locale.FRANCE, config.locale)
        assertEquals(',', config.decimalSeparator)
        // French uses non-breaking space as grouping separator
    }

    @Test
    fun `fromLocale sets default symbol position to BEFORE`() {
        // When
        val config = CurrencyFormatConfig.fromLocale(Locale.US)

        // Then
        assertEquals(CurrencyPosition.BEFORE, config.symbolPosition)
    }

    @Test
    fun `fromLocale sets default hasGap to true`() {
        // When
        val config = CurrencyFormatConfig.fromLocale(Locale.US)

        // Then
        assertTrue(config.hasGap)
    }

    @Test
    fun `data class equals works correctly`() {
        // Given
        val config1 = CurrencyFormatConfig(
            locale = Locale.US,
            symbolPosition = CurrencyPosition.BEFORE,
            hasGap = true,
            decimalSeparator = '.',
            groupingSeparator = ','
        )
        val config2 = CurrencyFormatConfig(
            locale = Locale.US,
            symbolPosition = CurrencyPosition.BEFORE,
            hasGap = true,
            decimalSeparator = '.',
            groupingSeparator = ','
        )

        // Then
        assertEquals(config1, config2)
    }

    @Test
    fun `data class copy works correctly`() {
        // Given
        val config = CurrencyFormatConfig.fromLocale(Locale.US)

        // When
        val modified = config.copy(symbolPosition = CurrencyPosition.AFTER)

        // Then
        assertEquals(CurrencyPosition.AFTER, modified.symbolPosition)
        assertEquals(config.locale, modified.locale)
        assertEquals(config.decimalSeparator, modified.decimalSeparator)
    }
}
