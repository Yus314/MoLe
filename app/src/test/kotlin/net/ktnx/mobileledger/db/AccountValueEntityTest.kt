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

import net.ktnx.mobileledger.core.database.entity.AccountValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [AccountValue] Room entity.
 */
class AccountValueEntityTest {

    @Test
    fun `default values are correct`() {
        val accountValue = AccountValue()

        assertEquals(0L, accountValue.id)
        assertEquals(0L, accountValue.accountId)
        assertEquals("", accountValue.currency)
        assertEquals(0f, accountValue.value, 0.001f)
        assertEquals(0L, accountValue.generation)
        assertNull(accountValue.amountStyle)
    }

    @Test
    fun `all fields can be set and read`() {
        val accountValue = AccountValue()

        accountValue.id = 10L
        accountValue.accountId = 5L
        accountValue.currency = "USD"
        accountValue.value = 1234.56f
        accountValue.generation = 15L
        accountValue.amountStyle = "1,234.56"

        assertEquals(10L, accountValue.id)
        assertEquals(5L, accountValue.accountId)
        assertEquals("USD", accountValue.currency)
        assertEquals(1234.56f, accountValue.value, 0.001f)
        assertEquals(15L, accountValue.generation)
        assertEquals("1,234.56", accountValue.amountStyle)
    }

    @Test
    fun `value can be negative`() {
        val accountValue = AccountValue()

        accountValue.value = -500.75f

        assertEquals(-500.75f, accountValue.value, 0.001f)
    }

    @Test
    fun `value can be zero`() {
        val accountValue = AccountValue()
        accountValue.value = 100f

        accountValue.value = 0f

        assertEquals(0f, accountValue.value, 0.001f)
    }

    @Test
    fun `currency can be empty`() {
        val accountValue = AccountValue()

        assertEquals("", accountValue.currency)
    }

    @Test
    fun `currency can be multi-character code`() {
        val accountValue = AccountValue()

        accountValue.currency = "JPY"

        assertEquals("JPY", accountValue.currency)
    }

    @Test
    fun `currency can be unicode symbol`() {
        val accountValue = AccountValue()

        accountValue.currency = "€"

        assertEquals("€", accountValue.currency)
    }

    @Test
    fun `amountStyle can be null`() {
        val accountValue = AccountValue()
        accountValue.amountStyle = "existing style"

        accountValue.amountStyle = null

        assertNull(accountValue.amountStyle)
    }

    @Test
    fun `value handles large amounts`() {
        val accountValue = AccountValue()

        accountValue.value = 999999.99f

        val value = accountValue.value
        assertEquals(999999.99f, value, 1.0f) // Float has limited precision
    }
}
