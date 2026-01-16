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

class TransactionLineTest {

    @Test
    fun `hasAmount returns true when amount is set`() {
        val line = TransactionLine(
            accountName = "Assets:Cash",
            amount = 100f
        )

        assertTrue(line.hasAmount)
    }

    @Test
    fun `hasAmount returns false when amount is null`() {
        val line = TransactionLine(
            accountName = "Assets:Cash",
            amount = null
        )

        assertFalse(line.hasAmount)
    }

    @Test
    fun `withAmount creates new line with specified amount`() {
        val original = TransactionLine(
            accountName = "Assets:Cash",
            amount = null
        )

        val updated = original.withAmount(100f)

        assertEquals(100f, updated.amount)
        assertEquals("Assets:Cash", updated.accountName)
        assertNull(original.amount) // original unchanged
    }

    @Test
    fun `withoutAmount creates new line with null amount`() {
        val original = TransactionLine(
            accountName = "Assets:Cash",
            amount = 100f
        )

        val updated = original.withoutAmount()

        assertNull(updated.amount)
        assertEquals("Assets:Cash", updated.accountName)
        assertEquals(100f, original.amount) // original unchanged
    }

    @Test
    fun `default currency is empty string`() {
        val line = TransactionLine(accountName = "Assets:Cash")

        assertEquals("", line.currency)
    }

    @Test
    fun `currency can be set`() {
        val line = TransactionLine(
            accountName = "Assets:Cash",
            currency = "USD"
        )

        assertEquals("USD", line.currency)
    }

    @Test
    fun `comment is null by default`() {
        val line = TransactionLine(accountName = "Assets:Cash")

        assertNull(line.comment)
    }

    @Test
    fun `id is null for new line`() {
        val line = TransactionLine(accountName = "Assets:Cash")

        assertNull(line.id)
    }

    @Test
    fun `line with id represents saved line`() {
        val line = TransactionLine(
            id = 123L,
            accountName = "Assets:Cash"
        )

        assertEquals(123L, line.id)
    }
}
