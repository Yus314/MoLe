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

package net.ktnx.mobileledger.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Currency] Room entity.
 */
class CurrencyEntityTest {

    @Test
    fun `default constructor sets correct defaults`() {
        val currency = Currency()

        assertEquals(0L, currency.id)
        assertEquals("", currency.name)
        assertEquals("after", currency.position)
        assertTrue(currency.hasGap)
    }

    @Test
    fun `parameterized constructor sets all fields`() {
        val currency = Currency(42L, "USD", "before", false)

        assertEquals(42L, currency.id)
        assertEquals("USD", currency.name)
        assertEquals("before", currency.position)
        assertFalse(currency.hasGap)
    }

    @Test
    fun `fields can be modified`() {
        val currency = Currency()

        currency.id = 10L
        currency.name = "EUR"
        currency.position = "before"
        currency.hasGap = false

        assertEquals(10L, currency.id)
        assertEquals("EUR", currency.name)
        assertEquals("before", currency.position)
        assertFalse(currency.hasGap)
    }

    @Test
    fun `position can be set to after`() {
        val currency = Currency(1L, "USD", "after", true)

        assertEquals("after", currency.position)
    }

    @Test
    fun `position can be set to before`() {
        val currency = Currency(1L, "USD", "before", true)

        assertEquals("before", currency.position)
    }

    @Test
    fun `hasGap can be true`() {
        val currency = Currency(1L, "USD", "after", true)

        assertTrue(currency.hasGap)
    }

    @Test
    fun `hasGap can be false`() {
        val currency = Currency(1L, "USD", "after", false)

        assertFalse(currency.hasGap)
    }

    @Test
    fun `name can be set to various currency codes`() {
        val usd = Currency(1L, "USD", "before", true)
        val eur = Currency(2L, "EUR", "after", true)
        val jpy = Currency(3L, "JPY", "after", false)

        assertEquals("USD", usd.name)
        assertEquals("EUR", eur.name)
        assertEquals("JPY", jpy.name)
    }

    @Test
    fun `name can contain symbols`() {
        val currency = Currency(1L, "$", "before", false)

        assertEquals("$", currency.name)
    }

    @Test
    fun `name can be unicode`() {
        val currency = Currency(1L, "円", "after", true)

        assertEquals("円", currency.name)
    }
}
