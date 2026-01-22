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

package net.ktnx.mobileledger.json.unified

import java.text.ParseException
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.json.config.ApiVersionConfig
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedLedgerTransaction].
 *
 * Tests verify:
 * - Default values
 * - Property accessors
 * - Domain model conversion
 * - tsourcepos handling for different API versions (v1_32+)
 */
class UnifiedParsedLedgerTransactionTest {

    // ========================================
    // Default values tests
    // ========================================

    @Test
    fun `default tdate is null`() {
        val tx = UnifiedParsedLedgerTransaction()
        assertNull(tx.tdate)
    }

    @Test
    fun `default tdescription is null`() {
        val tx = UnifiedParsedLedgerTransaction()
        assertNull(tx.tdescription)
    }

    @Test
    fun `default tcode is empty string`() {
        val tx = UnifiedParsedLedgerTransaction()
        assertEquals("", tx.tcode)
    }

    @Test
    fun `default tstatus is Unmarked`() {
        val tx = UnifiedParsedLedgerTransaction()
        assertEquals("Unmarked", tx.tstatus)
    }

    @Test
    fun `default tprecedingcomment is empty string`() {
        val tx = UnifiedParsedLedgerTransaction()
        assertEquals("", tx.tprecedingcomment)
    }

    @Test
    fun `default ttags is empty list`() {
        val tx = UnifiedParsedLedgerTransaction()
        assertTrue(tx.ttags.isEmpty())
    }

    @Test
    fun `default tpostings is null`() {
        val tx = UnifiedParsedLedgerTransaction()
        assertNull(tx.tpostings)
    }

    @Test
    fun `default tindex is 0`() {
        val tx = UnifiedParsedLedgerTransaction()
        assertEquals(0, tx.tindex)
    }

    // ========================================
    // Property setter tests
    // ========================================

    @Test
    fun `tdate can be set`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tdate = "2024-06-15"
        assertEquals("2024-06-15", tx.tdate)
    }

    @Test
    fun `tdescription can be set`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tdescription = "Test transaction"
        assertEquals("Test transaction", tx.tdescription)
    }

    @Test
    fun `tcomment can be set`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tcomment = "A comment"
        assertEquals("A comment", tx.tcomment)
    }

    @Test
    fun `tstatus can be set`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tstatus = "Cleared"
        assertEquals("Cleared", tx.tstatus)
    }

    // ========================================
    // tindex setter tests
    // ========================================

    @Test
    fun `tindex setter updates postings ptransaction_`() {
        val tx = UnifiedParsedLedgerTransaction()
        val posting = UnifiedParsedPosting()
        tx.tpostings = mutableListOf(posting)

        tx.tindex = 42

        assertEquals("42", posting.ptransaction_)
    }

    @Test
    fun `tindex setter handles null postings`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tpostings = null

        // Should not throw exception
        tx.tindex = 42
        assertEquals(42, tx.tindex)
    }

    // ========================================
    // addPosting tests
    // ========================================

    @Test
    fun `addPosting creates list if null`() {
        val tx = UnifiedParsedLedgerTransaction()
        assertNull(tx.tpostings)

        tx.addPosting(UnifiedParsedPosting())

        assertNotNull(tx.tpostings)
        assertEquals(1, tx.tpostings!!.size)
    }

    @Test
    fun `addPosting appends to existing list`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tpostings = mutableListOf(UnifiedParsedPosting())

        tx.addPosting(UnifiedParsedPosting())

        assertEquals(2, tx.tpostings!!.size)
    }

    @Test
    fun `addPosting sets ptransaction_ to tindex`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tindex = 99
        val posting = UnifiedParsedPosting()

        tx.addPosting(posting)

        assertEquals("99", posting.ptransaction_)
    }

    // ========================================
    // toDomain tests
    // ========================================

    @Test
    fun `toDomain converts basic transaction`() {
        val tx = UnifiedParsedLedgerTransaction().apply {
            tdate = "2024-06-15"
            tdescription = "Test description"
            tindex = 1
        }

        val domain = tx.toDomain()

        assertEquals("Test description", domain.description)
        assertEquals(1L, domain.ledgerId)
    }

    @Test
    fun `toDomain converts date correctly`() {
        val tx = UnifiedParsedLedgerTransaction().apply {
            tdate = "2024-06-15"
            tindex = 1
        }

        val domain = tx.toDomain()

        assertEquals(2024, domain.date.year)
        assertEquals(6, domain.date.month)
        assertEquals(15, domain.date.day)
    }

    @Test(expected = ParseException::class)
    fun `toDomain throws for null date`() {
        val tx = UnifiedParsedLedgerTransaction().apply {
            tdate = null
            tdescription = "Test"
        }

        tx.toDomain()
    }

    @Test
    fun `toDomain converts postings`() {
        val tx = UnifiedParsedLedgerTransaction().apply {
            tdate = "2024-06-15"
            tindex = 1
            tpostings = mutableListOf(
                UnifiedParsedPosting().apply {
                    paccount = "Assets:Bank"
                },
                UnifiedParsedPosting().apply {
                    paccount = "Expenses:Food"
                }
            )
        }

        val domain = tx.toDomain()

        assertEquals(2, domain.lines.size)
        assertEquals("Assets:Bank", domain.lines[0].accountName)
        assertEquals("Expenses:Food", domain.lines[1].accountName)
    }

    @Test
    fun `toDomain handles null postings`() {
        val tx = UnifiedParsedLedgerTransaction().apply {
            tdate = "2024-06-15"
            tpostings = null
        }

        val domain = tx.toDomain()

        assertTrue(domain.lines.isEmpty())
    }

    @Test
    fun `toDomain trims comment`() {
        val tx = UnifiedParsedLedgerTransaction().apply {
            tdate = "2024-06-15"
            tcomment = "  trimmed comment  "
        }

        val domain = tx.toDomain()

        assertEquals("trimmed comment", domain.comment)
    }

    @Test
    fun `toDomain returns null for empty comment`() {
        val tx = UnifiedParsedLedgerTransaction().apply {
            tdate = "2024-06-15"
            tcomment = "   "
        }

        val domain = tx.toDomain()

        assertNull(domain.comment)
    }

    // ========================================
    // fromDomain tests
    // ========================================

    @Test
    fun `fromDomain creates transaction with correct date`() {
        val domain = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test",
            comment = null,
            lines = emptyList()
        )

        val tx = UnifiedParsedLedgerTransaction.fromDomain(domain, ApiVersionConfig.V1_32_40)

        assertEquals("2024-06-15", tx.tdate)
    }

    @Test
    fun `fromDomain creates transaction with description`() {
        val domain = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test description",
            comment = null,
            lines = emptyList()
        )

        val tx = UnifiedParsedLedgerTransaction.fromDomain(domain, ApiVersionConfig.V1_32_40)

        assertEquals("Test description", tx.tdescription)
    }

    @Test
    fun `fromDomain converts postings`() {
        val domain = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test",
            comment = null,
            lines = listOf(
                TransactionLine(null, "Assets:Bank", 100f, "USD", null),
                TransactionLine(null, "Expenses:Food", -100f, "USD", null)
            )
        )

        val tx = UnifiedParsedLedgerTransaction.fromDomain(domain, ApiVersionConfig.V1_32_40)

        assertEquals(2, tx.tpostings!!.size)
    }

    @Test
    fun `fromDomain filters empty account names`() {
        val domain = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test",
            comment = null,
            lines = listOf(
                TransactionLine(null, "Assets:Bank", 100f, "USD", null),
                TransactionLine(null, "", -100f, "USD", null)
            )
        )

        val tx = UnifiedParsedLedgerTransaction.fromDomain(domain, ApiVersionConfig.V1_32_40)

        assertEquals(1, tx.tpostings!!.size)
    }

    @Test
    fun `fromDomain adds single tsourcepos for v1_32_40`() {
        val domain = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test",
            comment = null,
            lines = emptyList()
        )

        val tx = UnifiedParsedLedgerTransaction.fromDomain(domain, ApiVersionConfig.V1_32_40)

        assertEquals(1, tx.tsourcepos.size)
    }

    @Test
    fun `fromDomain adds two tsourcepos for v1_50`() {
        val domain = Transaction(
            id = null,
            ledgerId = 1,
            date = SimpleDate(2024, 6, 15),
            description = "Test",
            comment = null,
            lines = emptyList()
        )

        val tx = UnifiedParsedLedgerTransaction.fromDomain(domain, ApiVersionConfig.V1_50)

        assertEquals(2, tx.tsourcepos.size)
    }

    // ========================================
    // getSourcePosForSerialization tests
    // ========================================

    @Test
    fun `getSourcePosForSerialization returns list for v1_50`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tsourcepos.add(UnifiedParsedSourcePos())

        val result = UnifiedParsedLedgerTransaction.getSourcePosForSerialization(tx, ApiVersionConfig.V1_50)

        assertTrue(result is List<*>)
    }

    @Test
    fun `getSourcePosForSerialization returns single object for v1_32_40`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tsourcepos.add(UnifiedParsedSourcePos())

        val result = UnifiedParsedLedgerTransaction.getSourcePosForSerialization(tx, ApiVersionConfig.V1_32_40)

        assertTrue(result is UnifiedParsedSourcePos)
    }

    // ========================================
    // setTsourceposFromJson tests
    // ========================================

    @Test
    fun `setTsourceposFromJson handles Map input`() {
        val tx = UnifiedParsedLedgerTransaction()
        val map = mapOf("sourceName" to "test.journal", "sourceLine" to 10, "sourceColumn" to 1)

        tx.setTsourceposFromJson(map)

        assertEquals(1, tx.tsourcepos.size)
        assertEquals("test.journal", tx.tsourcepos[0].sourceName)
        assertEquals(10, tx.tsourcepos[0].sourceLine)
    }

    @Test
    fun `setTsourceposFromJson handles List input`() {
        val tx = UnifiedParsedLedgerTransaction()
        val list = listOf(
            mapOf("sourceName" to "a.journal", "sourceLine" to 1),
            mapOf("sourceName" to "b.journal", "sourceLine" to 2)
        )

        tx.setTsourceposFromJson(list)

        assertEquals(2, tx.tsourcepos.size)
    }

    @Test
    fun `setTsourceposFromJson adds default for null`() {
        val tx = UnifiedParsedLedgerTransaction()

        tx.setTsourceposFromJson(null)

        assertEquals(1, tx.tsourcepos.size)
    }

    @Test
    fun `setTsourceposFromJson clears previous values`() {
        val tx = UnifiedParsedLedgerTransaction()
        tx.tsourcepos.add(UnifiedParsedSourcePos())
        tx.tsourcepos.add(UnifiedParsedSourcePos())

        tx.setTsourceposFromJson(mapOf("tag" to "New"))

        assertEquals(1, tx.tsourcepos.size)
    }

    // ========================================
    // getTransactionIdForPostingSerialization tests
    // ========================================

    @Test
    fun `getTransactionIdForPostingSerialization returns String converter`() {
        val tx = UnifiedParsedLedgerTransaction()
        val converter = tx.getTransactionIdForPostingSerialization()

        val result = converter(42)

        assertEquals("42", result)
    }
}
