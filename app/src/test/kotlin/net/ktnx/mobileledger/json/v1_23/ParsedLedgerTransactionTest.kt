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

import java.text.ParseException
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ParsedLedgerTransaction] in v1_23 API.
 */
class ParsedLedgerTransactionTest {

    @Test
    fun `toDomain converts basic transaction`() {
        val parsed = ParsedLedgerTransaction().apply {
            tdate = "2026-01-21"
            tdescription = "Test Transaction"
            tcomment = "A comment"
            tindex = 123
            tpostings = mutableListOf()
        }

        val domain = parsed.toDomain()

        assertEquals(123L, domain.ledgerId)
        assertEquals("Test Transaction", domain.description)
        assertEquals("A comment", domain.comment)
        assertEquals(SimpleDate(2026, 1, 21), domain.date)
    }

    @Test
    fun `toDomain handles null description`() {
        val parsed = ParsedLedgerTransaction().apply {
            tdate = "2026-01-21"
            tdescription = null
            tpostings = mutableListOf()
        }

        val domain = parsed.toDomain()

        assertEquals("", domain.description)
    }

    @Test
    fun `toDomain trims whitespace-only comment to null`() {
        val parsed = ParsedLedgerTransaction().apply {
            tdate = "2026-01-21"
            tdescription = "Test"
            tcomment = "   "
            tpostings = mutableListOf()
        }

        val domain = parsed.toDomain()

        assertNull(domain.comment)
    }

    @Test(expected = ParseException::class)
    fun `toDomain throws on null date`() {
        val parsed = ParsedLedgerTransaction().apply {
            tdate = null
            tdescription = "Test"
            tpostings = mutableListOf()
        }

        parsed.toDomain()
    }

    @Test
    fun `toDomain handles null postings`() {
        val parsed = ParsedLedgerTransaction().apply {
            tdate = "2026-01-21"
            tdescription = "Test"
            tpostings = null
        }

        val domain = parsed.toDomain()

        assertTrue(domain.lines.isEmpty())
    }

    @Test
    fun `fromDomain converts basic transaction`() {
        val domain = Transaction(
            id = 1L,
            ledgerId = 100L,
            date = SimpleDate(2026, 1, 21),
            description = "Test Transaction",
            comment = "A comment",
            lines = emptyList()
        )

        val parsed = ParsedLedgerTransaction.fromDomain(domain)

        assertEquals("2026-01-21", parsed.tdate)
        assertEquals("Test Transaction", parsed.tdescription)
        assertEquals("A comment", parsed.tcomment)
        assertNull(parsed.tdate2)
    }

    @Test
    fun `fromDomain handles null comment`() {
        val domain = Transaction(
            id = 1L,
            ledgerId = 100L,
            date = SimpleDate(2026, 1, 21),
            description = "Test",
            comment = null,
            lines = emptyList()
        )

        val parsed = ParsedLedgerTransaction.fromDomain(domain)

        assertEquals("", parsed.tcomment)
    }

    @Test
    fun `fromDomain filters empty account names`() {
        val domain = Transaction(
            id = 1L,
            ledgerId = 100L,
            date = SimpleDate(2026, 1, 21),
            description = "Test",
            comment = null,
            lines = listOf(
                TransactionLine(accountName = "Assets:Cash", amount = 100f, currency = "USD"),
                TransactionLine(accountName = "", amount = 0f, currency = ""),
                TransactionLine(accountName = "Expenses:Food", amount = -100f, currency = "USD")
            )
        )

        val parsed = ParsedLedgerTransaction.fromDomain(domain)

        assertEquals(2, parsed.tpostings?.size)
    }

    @Test
    fun `tindex setter propagates to postings`() {
        val posting1 = ParsedPosting()
        val posting2 = ParsedPosting()

        val parsed = ParsedLedgerTransaction().apply {
            tpostings = mutableListOf(posting1, posting2)
            tindex = 42
        }

        assertEquals(42, posting1.ptransaction_)
        assertEquals(42, posting2.ptransaction_)
        assertEquals(42, parsed.tindex)
    }

    @Test
    fun `tindex setter handles null postings`() {
        val parsed = ParsedLedgerTransaction().apply {
            tpostings = null
            tindex = 99
        }

        assertEquals(99, parsed.tindex)
    }

    @Test
    fun `addPosting sets transaction index on posting`() {
        val parsed = ParsedLedgerTransaction().apply {
            tpostings = mutableListOf()
            tindex = 55
        }

        val posting = ParsedPosting()
        parsed.addPosting(posting)

        assertEquals(55, posting.ptransaction_)
    }

    @Test
    fun `addPosting adds to postings list`() {
        val parsed = ParsedLedgerTransaction().apply {
            tpostings = mutableListOf()
            tindex = 1
        }

        val posting = ParsedPosting()
        parsed.addPosting(posting)

        assertNotNull(parsed.tpostings)
        assertEquals(1, parsed.tpostings!!.size)
    }
}
