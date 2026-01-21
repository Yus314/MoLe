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

package net.ktnx.mobileledger.json.v1_23

import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.json.unified.UnifiedParsedQuantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [ParsedPosting] in v1_23 API.
 */
class ParsedPostingTest {

    @Test
    fun `toDomain converts basic posting`() {
        val posting = ParsedPosting().apply {
            paccount = "Expenses:Food"
            pamount = mutableListOf(
                ParsedAmount().apply {
                    acommodity = "USD"
                    aquantity = UnifiedParsedQuantity().apply {
                        decimalMantissa = 1050
                        decimalPlaces = 2
                    }
                }
            )
            pcomment = "Groceries"
        }

        val line = posting.toDomain()

        assertEquals("Expenses:Food", line.accountName)
        assertEquals(10.50f, line.amount!!, 0.01f)
        assertEquals("USD", line.currency)
        assertEquals("Groceries", line.comment)
    }

    @Test
    fun `toDomain handles null account`() {
        val posting = ParsedPosting().apply {
            paccount = null
            pamount = mutableListOf()
        }

        val line = posting.toDomain()

        assertEquals("", line.accountName)
    }

    @Test
    fun `toDomain handles null amount list`() {
        val posting = ParsedPosting().apply {
            paccount = "Assets:Cash"
            pamount = null
        }

        val line = posting.toDomain()

        assertNull(line.amount)
        assertEquals("", line.currency)
    }

    @Test
    fun `toDomain handles empty amount list`() {
        val posting = ParsedPosting().apply {
            paccount = "Assets:Cash"
            pamount = mutableListOf()
        }

        val line = posting.toDomain()

        assertNull(line.amount)
        assertEquals("", line.currency)
    }

    @Test
    fun `toDomain converts empty comment to null`() {
        val posting = ParsedPosting().apply {
            paccount = "Assets:Cash"
            pamount = mutableListOf()
            pcomment = ""
        }

        val line = posting.toDomain()

        assertNull(line.comment)
    }

    @Test
    fun `fromDomain converts basic line`() {
        val line = TransactionLine(
            id = 1L,
            accountName = "Assets:Cash",
            amount = 100.50f,
            currency = "USD",
            comment = "Payment"
        )

        val posting = ParsedPosting.fromDomain(line)

        assertEquals("Assets:Cash", posting.paccount)
        assertEquals("Payment", posting.pcomment)
        assertNotNull(posting.pamount)
        assertEquals(1, posting.pamount?.size)
        assertEquals("USD", posting.pamount?.get(0)?.acommodity)
    }

    @Test
    fun `fromDomain handles null comment`() {
        val line = TransactionLine(
            id = 1L,
            accountName = "Assets:Cash",
            amount = 100f,
            currency = "USD",
            comment = null
        )

        val posting = ParsedPosting.fromDomain(line)

        assertEquals("", posting.pcomment)
    }

    @Test
    fun `fromDomain handles null amount`() {
        val line = TransactionLine(
            id = 1L,
            accountName = "Assets:Cash",
            amount = null,
            currency = "USD",
            comment = null
        )

        val posting = ParsedPosting.fromDomain(line)

        assertEquals(0L, posting.pamount?.get(0)?.aquantity?.decimalMantissa)
    }

    @Test
    fun `fromDomain converts negative amount correctly`() {
        val line = TransactionLine(
            id = 1L,
            accountName = "Expenses:Food",
            amount = -50.25f,
            currency = "EUR",
            comment = null
        )

        val posting = ParsedPosting.fromDomain(line)

        assertEquals(-5025L, posting.pamount?.get(0)?.aquantity?.decimalMantissa)
        assertEquals(2, posting.pamount?.get(0)?.aquantity?.decimalPlaces)
    }

    @Test
    fun `pcomment setter trims whitespace`() {
        val posting = ParsedPosting()

        posting.pcomment = "  leading and trailing  "

        assertEquals("leading and trailing", posting.pcomment)
    }

    @Test
    fun `pcomment setter handles newlines`() {
        val posting = ParsedPosting()

        posting.pcomment = "\n  text  \n"

        assertEquals("text", posting.pcomment)
    }
}
