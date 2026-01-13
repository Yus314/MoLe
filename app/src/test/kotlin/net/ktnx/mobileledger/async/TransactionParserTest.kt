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

import net.ktnx.mobileledger.model.LedgerTransactionAccount
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
        assertNull(result.currency)
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

    // Note: Full HTML parsing tests are skipped because the legacy HTML format
    // is complex and specific. The parseTransactionAccountLine tests above
    // cover the core parsing logic that is reused.
    // For full integration testing, use actual HTML from a hledger-web instance.
}
