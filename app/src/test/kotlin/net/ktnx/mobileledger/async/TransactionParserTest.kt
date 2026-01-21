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

package net.ktnx.mobileledger.async

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionParserTest {

    private lateinit var parser: TransactionParser

    @Before
    fun setup() {
        parser = TransactionParser()
    }

    @Test
    fun `parseTransactionAccountLine returns null for non-matching line`() {
        val result = TransactionParser.parseTransactionAccountLine("not a transaction line")
        assertNull(result)
    }

    @Test
    fun `parseTransactionAccountLine parses simple account without currency`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name  -34.56")

        assertNotNull(result)
        assertEquals("acc:name", result!!.accountName)
        assertEquals(-34.56f, result.amount)
        assertEquals("", result.currency)
    }

    @Test
    fun `parseTransactionAccountLine parses positive amount`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name3  34.56")

        assertNotNull(result)
        assertEquals("acc:name3", result!!.accountName)
        assertEquals(34.56f, result.amount)
    }

    @Test
    fun `parseTransactionAccountLine parses explicit positive amount`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name  +34.56")

        assertNotNull(result)
        assertEquals(34.56f, result!!.amount)
    }

    @Test
    fun `parseTransactionAccountLine parses currency prefix`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name  \$-34.56")

        assertNotNull(result)
        assertEquals(-34.56f, result!!.amount)
        assertEquals("\$", result.currency)
    }

    @Test
    fun `parseTransactionAccountLine parses currency prefix with space`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name  \$ -34.56")

        assertNotNull(result)
        assertEquals(-34.56f, result!!.amount)
        assertEquals("\$", result.currency)
    }

    @Test
    fun `parseTransactionAccountLine parses currency suffix`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name  -34.56\$")

        assertNotNull(result)
        assertEquals(-34.56f, result!!.amount)
        assertEquals("\$", result.currency)
    }

    @Test
    fun `parseTransactionAccountLine parses currency suffix with space`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name  -34.56 \$")

        assertNotNull(result)
        assertEquals(-34.56f, result!!.amount)
        assertEquals("\$", result.currency)
    }

    @Test
    fun `parseTransactionAccountLine parses multi-char currency prefix`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name  AU\$-34.56")

        assertNotNull(result)
        assertEquals(-34.56f, result!!.amount)
        assertEquals("AU\$", result.currency)
    }

    @Test
    fun `parseTransactionAccountLine parses multi-char currency suffix`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name  -34.56 AU\$")

        assertNotNull(result)
        assertEquals(-34.56f, result!!.amount)
        assertEquals("AU\$", result.currency)
    }

    @Test
    fun `parseTransactionAccountLine handles comma decimal separator`() {
        val result = TransactionParser.parseTransactionAccountLine(" acc:name  -34,56")

        assertNotNull(result)
        assertEquals(-34.56f, result!!.amount)
    }

    @Test
    fun `parseTransactions returns empty list for empty input`() {
        val result = parser.parseTransactions(emptyList())

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `parseTransactions returns empty list when no transactions found`() {
        val lines = listOf(
            "<html>",
            "<body>",
            "<h2>General Journal</h2>",
            "</body>"
        )

        val result = parser.parseTransactions(lines)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    // ========================================
    // parseTransactionAccountLine edge cases
    // ========================================

    @Test
    fun `parseTransactionAccountLine handles account with spaces`() {
        val result = TransactionParser.parseTransactionAccountLine(
            "  Assets:Bank Checking  100.00"
        )

        assertNotNull(result)
        assertEquals("Assets:Bank Checking", result!!.accountName)
        assertEquals(100.00f, result.amount)
    }

    @Test
    fun `parseTransactionAccountLine handles large amounts`() {
        val result = TransactionParser.parseTransactionAccountLine(
            "  acc:name  1234567.89"
        )

        assertNotNull(result)
        val amount = result!!.amount!!
        assertTrue(amount > 1234567f && amount < 1234568f)
    }

    @Test
    fun `parseTransactionAccountLine handles integer amounts`() {
        val result = TransactionParser.parseTransactionAccountLine(
            "  acc:name  1000"
        )

        assertNotNull(result)
        assertEquals(1000f, result!!.amount)
    }

    @Test
    fun `parseTransactionAccountLine returns null when both pre and post currency present`() {
        // This is a corner case - having currency on both sides should fail
        val result = TransactionParser.parseTransactionAccountLine(
            "  acc:name  \$100.00 EUR"
        )

        assertNull(result)
    }

    @Test
    fun `parseTransactionAccountLine handles zero amount`() {
        val result = TransactionParser.parseTransactionAccountLine(
            "  acc:name  0.00"
        )

        assertNotNull(result)
        assertEquals(0.0f, result!!.amount)
    }

    @Test
    fun `parseTransactionAccountLine handles cleared status marker`() {
        val result = TransactionParser.parseTransactionAccountLine(
            "  * acc:name  100.00"
        )

        assertNotNull(result)
        assertEquals("acc:name", result!!.accountName)
        assertEquals(100.00f, result.amount)
    }

    @Test
    fun `parseTransactionAccountLine handles pending status marker`() {
        val result = TransactionParser.parseTransactionAccountLine(
            "  ! acc:name  -50.00"
        )

        assertNotNull(result)
        assertEquals("acc:name", result!!.accountName)
        assertEquals(-50.00f, result.amount)
    }

    @Test
    fun `parseTransactionAccountLine handles EUR currency`() {
        val result = TransactionParser.parseTransactionAccountLine(
            "  acc:name  EUR 100.00"
        )

        assertNotNull(result)
        assertEquals("EUR", result!!.currency)
        assertEquals(100.00f, result.amount)
    }

    @Test
    fun `parseTransactionAccountLine handles JPY currency suffix`() {
        val result = TransactionParser.parseTransactionAccountLine(
            "  acc:name  1000 JPY"
        )

        assertNotNull(result)
        assertEquals("JPY", result!!.currency)
        assertEquals(1000f, result.amount)
    }

    // ========================================
    // parseTransactions - comment handling
    // ========================================

    @Test
    fun `parseTransactions ignores comment lines`() {
        val lines = listOf(
            "; This is a comment",
            "  ; Indented comment",
            "   ;  Another comment"
        )

        val result = parser.parseTransactions(lines)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `parseTransactions ignores lines starting with space when expecting transaction`() {
        val lines = listOf(
            "    Some indented content",
            "      More indented content"
        )

        val result = parser.parseTransactions(lines)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    // ========================================
    // parseTransactions - end marker
    // ========================================

    @Test
    fun `parseTransactions stops at end marker`() {
        val lines = listOf(
            "<div>Some content</div>",
            "<div id=\"addmodal\">Add modal content</div>",
            "<tr class=\"title\" id=\"transaction-1\">Should not parse</tr>"
        )

        val result = parser.parseTransactions(lines)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    // ========================================
    // TransactionParseException tests
    // ========================================

    @Test
    fun `TransactionParseException stores message`() {
        val exception = TransactionParseException("Test error")
        assertEquals("Test error", exception.message)
    }

    @Test
    fun `TransactionParseException stores cause`() {
        val cause = RuntimeException("Original cause")
        val exception = TransactionParseException("Wrapper error", cause)

        assertEquals("Wrapper error", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `TransactionParseException is throwable`() {
        val exception = TransactionParseException("Test")

        try {
            throw exception
        } catch (e: TransactionParseException) {
            assertEquals("Test", e.message)
        }
    }

    @Test
    fun `TransactionParseException with null cause`() {
        val exception = TransactionParseException("Message", null)

        assertEquals("Message", exception.message)
        assertNull(exception.cause)
    }

    // ========================================
    // parseTransactions - various inputs
    // ========================================

    @Test
    fun `parseTransactions handles null-like empty lines`() {
        val lines = listOf("", "", "")

        val result = parser.parseTransactions(lines)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `parseTransactions handles single line input`() {
        val lines = listOf("<html></html>")

        val result = parser.parseTransactions(lines)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `parseTransactions handles mixed content`() {
        val lines = listOf(
            "<html>",
            "; comment line",
            "",
            "  indented content",
            "</html>"
        )

        val result = parser.parseTransactions(lines)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }
}
