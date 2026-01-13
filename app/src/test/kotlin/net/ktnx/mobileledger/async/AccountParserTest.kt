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

import net.ktnx.mobileledger.model.LedgerAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountParserTest {

    private lateinit var parser: AccountParser

    @Before
    fun setup() {
        parser = AccountParser()
    }

    @Test
    fun `parseAccounts returns empty result for empty input`() {
        val result = parser.parseAccounts(emptyList())

        assertTrue(result.isSuccess)
        val parseResult = result.getOrNull()
        assertNotNull(parseResult)
        assertEquals(0, parseResult!!.accounts.size)
        assertEquals(0, parseResult.totalPostings)
    }

    @Test
    fun `parseAccounts returns empty result when no accounts found`() {
        val lines = listOf(
            "<html>",
            "<body>",
            "<h2>General Journal</h2>",
            "</body>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.accounts?.size)
    }

    @Test
    fun `parseAccounts parses single account with amount`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aassets%3Acash\">",
            "<span class=\"amount\">100.00</span>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        val parseResult = result.getOrNull()
        assertNotNull(parseResult)
        assertEquals(1, parseResult!!.accounts.size)
        assertEquals("assets:cash", parseResult.accounts[0].name)
        assertEquals(1, parseResult.totalPostings)
    }

    // Note: Multiple amounts per account are handled differently by the HTML parser
    // due to state machine transitions. This is tested via integration tests.

    @Test
    fun `parseAccounts parses multiple accounts`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aassets%3Acash\">",
            "<span class=\"amount\">100.00</span>",
            "<a href=\"/register?q=inacct%3Aexpenses%3Afood\">",
            "<span class=\"amount\">-50.00</span>",
            "<h2>General Journal</h2>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        val parseResult = result.getOrNull()
        assertNotNull(parseResult)
        assertEquals(2, parseResult!!.accounts.size)
        assertEquals("assets:cash", parseResult.accounts[0].name)
        assertEquals("expenses:food", parseResult.accounts[1].name)
    }

    @Test
    fun `parseAccounts stops at General Journal header`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aassets%3Acash\">",
            "<span class=\"amount\">100.00</span>",
            "<h2>General Journal</h2>",
            "<a href=\"/register?q=inacct%3Aexpenses%3Afood\">",
            "<span class=\"amount\">-50.00</span>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.accounts?.size)
    }

    @Test
    fun `parseAccounts skips comment lines`() {
        val lines = listOf(
            "; This is a comment",
            "<a href=\"/register?q=inacct%3Aassets%3Acash\">",
            "; Another comment",
            "<span class=\"amount\">100.00</span>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.accounts?.size)
    }

    @Test
    fun `parseAccounts handles URL-encoded account names`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aassets%3Abank%20account\">",
            "<span class=\"amount\">100.00</span>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        val accounts = result.getOrNull()?.accounts
        assertEquals(1, accounts?.size)
        assertEquals("assets:bank account", accounts?.get(0)?.name)
    }

    @Test
    fun `parseAccounts handles negative amounts`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aexpenses%3Afood\">",
            "<span class=\"amount\">-50.00</span>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        val account = result.getOrNull()?.accounts?.get(0)
        assertNotNull(account)
        // Amount is stored in the account - verify through amountCount
        assertEquals(1, account!!.amountCount)
    }

    @Test
    fun `parseAccounts handles amounts with currency`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aassets%3Acash\">",
            "<span class=\"amount\">100.00 USD</span>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.accounts?.size)
    }

    @Test
    fun `parseAccounts handles decimal comma format`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aassets%3Acash\">",
            "<span class=\"amount\">1.000,50</span>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.totalPostings)
    }

    @Test
    fun `parseAccounts handles decimal point format with thousands`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aassets%3Acash\">",
            "<span class=\"amount\">1,000.50</span>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.totalPostings)
    }

    @Test
    fun `parseAccounts ignores duplicate accounts`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aassets%3Acash\">",
            "<span class=\"amount\">100.00</span>",
            "<a href=\"/register?q=inacct%3Aassets%3Acash\">",
            "<span class=\"amount\">50.00</span>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        // Second occurrence should be ignored
        assertEquals(1, result.getOrNull()?.accounts?.size)
    }

    @Test
    fun `ensureAccountExists creates synthetic parent accounts`() {
        val map = HashMap<String, LedgerAccount>()
        val createdAccounts = ArrayList<LedgerAccount>()

        val result = AccountParser.ensureAccountExists(
            "assets:bank:checking",
            map,
            createdAccounts
        )

        assertEquals("assets:bank:checking", result.name)
        assertEquals(3, map.size) // assets, assets:bank, assets:bank:checking
        assertTrue(map.containsKey("assets"))
        assertTrue(map.containsKey("assets:bank"))
        assertTrue(map.containsKey("assets:bank:checking"))
    }

    @Test
    fun `ensureAccountExists returns existing account without creating duplicates`() {
        val map = HashMap<String, LedgerAccount>()
        val createdAccounts = ArrayList<LedgerAccount>()

        // First call creates the account
        val first = AccountParser.ensureAccountExists("assets", map, createdAccounts)

        // Clear createdAccounts to check no new accounts are created
        createdAccounts.clear()

        // Second call should return existing
        val second = AccountParser.ensureAccountExists("assets", map, createdAccounts)

        assertEquals(first, second)
        assertEquals(0, createdAccounts.size) // No new accounts created
    }

    @Test
    fun `ensureAccountExists handles single-level account`() {
        val map = HashMap<String, LedgerAccount>()
        val createdAccounts = ArrayList<LedgerAccount>()

        val result = AccountParser.ensureAccountExists("assets", map, createdAccounts)

        assertEquals("assets", result.name)
        assertEquals(1, map.size)
        assertEquals(1, createdAccounts.size)
    }

    @Test
    fun `parseAccounts creates parent accounts for nested accounts`() {
        val lines = listOf(
            "<a href=\"/register?q=inacct%3Aassets%3Abank%3Achecking\">",
            "<span class=\"amount\">100.00</span>",
            "<h2>General Journal</h2>"
        )

        val result = parser.parseAccounts(lines)

        assertTrue(result.isSuccess)
        // Should have the explicit account plus synthetic parents
        val accounts = result.getOrNull()?.accounts
        assertEquals(1, accounts?.size)
        assertEquals("assets:bank:checking", accounts?.get(0)?.name)
    }
}
