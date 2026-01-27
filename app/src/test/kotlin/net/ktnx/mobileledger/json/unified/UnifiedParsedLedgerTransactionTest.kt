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
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine
import net.ktnx.mobileledger.json.MoLeJson
import net.ktnx.mobileledger.json.config.ApiVersionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UnifiedParsedLedgerTransaction].
 *
 * Tests verify:
 * - Default values
 * - Constructor initialization
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
    // Constructor initialization tests
    // ========================================

    @Test
    fun `can create tx with tdate`() {
        val tx = UnifiedParsedLedgerTransaction(tdate = "2024-06-15")
        assertEquals("2024-06-15", tx.tdate)
    }

    @Test
    fun `can create tx with tdescription`() {
        val tx = UnifiedParsedLedgerTransaction(tdescription = "Test transaction")
        assertEquals("Test transaction", tx.tdescription)
    }

    @Test
    fun `can create tx with tcomment`() {
        val tx = UnifiedParsedLedgerTransaction(tcomment = "A comment")
        assertEquals("A comment", tx.tcomment)
    }

    @Test
    fun `can create tx with tstatus`() {
        val tx = UnifiedParsedLedgerTransaction(tstatus = "Cleared")
        assertEquals("Cleared", tx.tstatus)
    }

    @Test
    fun `can create tx with tindex`() {
        val tx = UnifiedParsedLedgerTransaction(tindex = 42)
        assertEquals(42, tx.tindex)
    }

    @Test
    fun `can create tx with tpostings`() {
        val tx = UnifiedParsedLedgerTransaction(
            tpostings = listOf(
                UnifiedParsedPosting(paccount = "Assets:Bank"),
                UnifiedParsedPosting(paccount = "Expenses:Food")
            )
        )

        assertEquals(2, tx.tpostings!!.size)
    }

    @Test
    fun `can create tx with tsourcepos`() {
        val tx = UnifiedParsedLedgerTransaction(
            tsourcepos = listOf(
                UnifiedParsedSourcePos(sourceName = "test.journal", sourceLine = 10)
            )
        )

        assertEquals(1, tx.tsourcepos.size)
        assertEquals("test.journal", tx.tsourcepos[0].sourceName)
    }

    // ========================================
    // toDomain tests
    // ========================================

    @Test
    fun `toDomain converts basic transaction`() {
        val tx = UnifiedParsedLedgerTransaction(
            tdate = "2024-06-15",
            tdescription = "Test description",
            tindex = 1
        )

        val domain = tx.toDomain()

        assertEquals("Test description", domain.description)
        assertEquals(1L, domain.ledgerId)
    }

    @Test
    fun `toDomain converts date correctly`() {
        val tx = UnifiedParsedLedgerTransaction(
            tdate = "2024-06-15",
            tindex = 1
        )

        val domain = tx.toDomain()

        assertEquals(2024, domain.date.year)
        assertEquals(6, domain.date.month)
        assertEquals(15, domain.date.day)
    }

    @Test(expected = ParseException::class)
    fun `toDomain throws for null date`() {
        val tx = UnifiedParsedLedgerTransaction(
            tdate = null,
            tdescription = "Test"
        )

        tx.toDomain()
    }

    @Test
    fun `toDomain converts postings`() {
        val tx = UnifiedParsedLedgerTransaction(
            tdate = "2024-06-15",
            tindex = 1,
            tpostings = listOf(
                UnifiedParsedPosting(paccount = "Assets:Bank"),
                UnifiedParsedPosting(paccount = "Expenses:Food")
            )
        )

        val domain = tx.toDomain()

        assertEquals(2, domain.lines.size)
        assertEquals("Assets:Bank", domain.lines[0].accountName)
        assertEquals("Expenses:Food", domain.lines[1].accountName)
    }

    @Test
    fun `toDomain handles null postings`() {
        val tx = UnifiedParsedLedgerTransaction(
            tdate = "2024-06-15",
            tpostings = null
        )

        val domain = tx.toDomain()

        assertTrue(domain.lines.isEmpty())
    }

    @Test
    fun `toDomain trims comment`() {
        val tx = UnifiedParsedLedgerTransaction(
            tdate = "2024-06-15",
            tcomment = "  trimmed comment  "
        )

        val domain = tx.toDomain()

        assertEquals("trimmed comment", domain.comment)
    }

    @Test
    fun `toDomain returns null for empty comment`() {
        val tx = UnifiedParsedLedgerTransaction(
            tdate = "2024-06-15",
            tcomment = "   "
        )

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
        val tx = UnifiedParsedLedgerTransaction(
            tsourcepos = listOf(UnifiedParsedSourcePos())
        )

        val result = UnifiedParsedLedgerTransaction.getSourcePosForSerialization(tx, ApiVersionConfig.V1_50)

        assertTrue(result is List<*>)
    }

    @Test
    fun `getSourcePosForSerialization returns single object for v1_32_40`() {
        val tx = UnifiedParsedLedgerTransaction(
            tsourcepos = listOf(UnifiedParsedSourcePos())
        )

        val result = UnifiedParsedLedgerTransaction.getSourcePosForSerialization(tx, ApiVersionConfig.V1_32_40)

        assertTrue(result is UnifiedParsedSourcePos)
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

    // ========================================
    // JSON serialization tests
    // ========================================

    @Test
    fun `deserialize transaction from JSON`() {
        val json = """
            {
                "tdate": "2024-06-15",
                "tdescription": "Test transaction",
                "tindex": 1,
                "tpostings": [
                    {"paccount": "Assets:Bank"}
                ],
                "tsourcepos": {"sourceName": "test.journal", "sourceLine": 10}
            }
        """.trimIndent()

        val tx = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)

        assertEquals("2024-06-15", tx.tdate)
        assertEquals("Test transaction", tx.tdescription)
        assertEquals(1, tx.tindex)
        assertEquals(1, tx.tpostings?.size)
        assertEquals("Assets:Bank", tx.tpostings?.get(0)?.paccount)
    }

    @Test
    fun `deserialize transaction with tsourcepos array (v1_50)`() {
        val json = """
            {
                "tdate": "2024-06-15",
                "tdescription": "Test",
                "tsourcepos": [
                    {"sourceName": "a.journal", "sourceLine": 1},
                    {"sourceName": "b.journal", "sourceLine": 2}
                ]
            }
        """.trimIndent()

        val tx = MoLeJson.decodeFromString<UnifiedParsedLedgerTransaction>(json)

        assertEquals(2, tx.tsourcepos.size)
        assertEquals("a.journal", tx.tsourcepos[0].sourceName)
        assertEquals("b.journal", tx.tsourcepos[1].sourceName)
    }
}
