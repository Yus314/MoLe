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
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [AccountAmount].
 */
class AccountAmountTest {

    @Test
    fun `properties stored correctly`() {
        val amount = AccountAmount(currency = "USD", amount = 100.50f)

        assertEquals("USD", amount.currency)
        assertEquals(100.50f, amount.amount)
    }

    @Test
    fun `empty currency represents default`() {
        val amount = AccountAmount(currency = "", amount = 50.0f)

        assertEquals("", amount.currency)
    }

    @Test
    fun `negative amount supported`() {
        val amount = AccountAmount(currency = "JPY", amount = -1000.0f)

        assertEquals(-1000.0f, amount.amount)
    }

    @Test
    fun `zero amount supported`() {
        val amount = AccountAmount(currency = "EUR", amount = 0.0f)

        assertEquals(0.0f, amount.amount)
    }

    @Test
    fun `equals compares all fields`() {
        val amount1 = AccountAmount("USD", 100.0f)
        val amount2 = AccountAmount("USD", 100.0f)

        assertEquals(amount1, amount2)
    }

    @Test
    fun `different currency not equal`() {
        val amount1 = AccountAmount("USD", 100.0f)
        val amount2 = AccountAmount("EUR", 100.0f)

        assertNotEquals(amount1, amount2)
    }

    @Test
    fun `different amount not equal`() {
        val amount1 = AccountAmount("USD", 100.0f)
        val amount2 = AccountAmount("USD", 200.0f)

        assertNotEquals(amount1, amount2)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = AccountAmount("USD", 100.0f)
        val modified = original.copy(amount = 200.0f)

        assertEquals("USD", modified.currency)
        assertEquals(200.0f, modified.amount)
    }
}
