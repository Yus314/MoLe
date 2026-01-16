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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyTest {

    @Test
    fun `new currency has null id`() {
        val currency = Currency(name = "USD")
        assertNull(currency.id)
    }

    @Test
    fun `existing currency has id`() {
        val currency = Currency(id = 123L, name = "USD")
        assertEquals(123L, currency.id)
    }

    @Test
    fun `currency defaults`() {
        val currency = Currency(name = "USD")
        assertEquals(CurrencyPosition.AFTER, currency.position)
        assertTrue(currency.hasGap)
    }

    @Test
    fun `currency with all fields`() {
        val currency = Currency(
            id = 1L,
            name = "$",
            position = CurrencyPosition.BEFORE,
            hasGap = false
        )
        assertEquals(1L, currency.id)
        assertEquals("$", currency.name)
        assertEquals(CurrencyPosition.BEFORE, currency.position)
        assertFalse(currency.hasGap)
    }

    @Test
    fun `currency is data class with copy`() {
        val original = Currency(id = 1L, name = "USD")
        val modified = original.copy(name = "EUR")
        assertEquals("USD", original.name)
        assertEquals("EUR", modified.name)
        assertEquals(original.id, modified.id)
    }
}

class CurrencyPositionTest {

    @Test
    fun `fromInt with valid values`() {
        assertEquals(CurrencyPosition.BEFORE, CurrencyPosition.fromInt(0))
        assertEquals(CurrencyPosition.AFTER, CurrencyPosition.fromInt(1))
    }

    @Test
    fun `fromInt with invalid value returns AFTER`() {
        assertEquals(CurrencyPosition.AFTER, CurrencyPosition.fromInt(-1))
        assertEquals(CurrencyPosition.AFTER, CurrencyPosition.fromInt(99))
    }

    @Test
    fun `toInt returns ordinal`() {
        assertEquals(0, CurrencyPosition.BEFORE.toInt())
        assertEquals(1, CurrencyPosition.AFTER.toInt())
    }

    @Test
    fun `fromString with valid values`() {
        assertEquals(CurrencyPosition.BEFORE, CurrencyPosition.fromString("before"))
        assertEquals(CurrencyPosition.AFTER, CurrencyPosition.fromString("after"))
        assertEquals(CurrencyPosition.BEFORE, CurrencyPosition.fromString("BEFORE"))
        assertEquals(CurrencyPosition.AFTER, CurrencyPosition.fromString("AFTER"))
    }

    @Test
    fun `fromString with invalid value returns AFTER`() {
        assertEquals(CurrencyPosition.AFTER, CurrencyPosition.fromString("unknown"))
        assertEquals(CurrencyPosition.AFTER, CurrencyPosition.fromString(""))
        assertEquals(CurrencyPosition.AFTER, CurrencyPosition.fromString("left"))
    }

    @Test
    fun `toDbString returns lowercase`() {
        assertEquals("before", CurrencyPosition.BEFORE.toDbString())
        assertEquals("after", CurrencyPosition.AFTER.toDbString())
    }
}
