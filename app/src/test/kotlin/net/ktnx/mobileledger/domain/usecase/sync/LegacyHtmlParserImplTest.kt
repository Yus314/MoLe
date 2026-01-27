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

package net.ktnx.mobileledger.domain.usecase.sync

import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.network.NetworkAuthenticationException
import net.ktnx.mobileledger.core.network.NetworkHttpException
import net.ktnx.mobileledger.fake.FakeHledgerClient
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [LegacyHtmlParserImpl].
 *
 * Tests verify:
 * - Account extraction from HTML
 * - Account amount parsing (various number formats)
 * - Transaction extraction from HTML
 * - Parent account generation
 * - Progress reporting
 * - Error handling
 */
@RunWith(RobolectricTestRunner::class)
class LegacyHtmlParserImplTest {

    private lateinit var fakeClient: FakeHledgerClient
    private lateinit var parser: LegacyHtmlParserImpl

    @Before
    fun setup() {
        fakeClient = FakeHledgerClient()
        parser = LegacyHtmlParserImpl(fakeClient)
    }

    // ========================================
    // Account parsing tests
    // ========================================

    @Test
    fun `parse extracts accounts from HTML`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithAccounts()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then
        assertTrue(result.accounts.any { it.name == "Assets:Bank" })
    }

    @Test
    fun `parse extracts account amounts`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithAccountAmounts()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then
        val account = result.accounts.find { it.name == "Assets:Bank" }
        assertTrue(account != null)
        assertTrue(account!!.amounts.isNotEmpty())
        assertEquals("USD", account.amounts[0].currency)
        assertEquals(1000.00f, account.amounts[0].amount, 0.01f)
    }

    @Test
    fun `parse handles decimal comma format`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithDecimalComma()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then
        val account = result.accounts.find { it.name == "Assets:Bank" }
        assertTrue(account != null)
        assertEquals(1000.50f, account!!.amounts[0].amount, 0.01f)
    }

    @Test
    fun `parse handles decimal point format`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithDecimalPoint()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then
        val account = result.accounts.find { it.name == "Assets:Bank" }
        assertTrue(account != null)
        assertEquals(1000.50f, account!!.amounts[0].amount, 0.01f)
    }

    @Test
    fun `parse generates parent accounts`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithNestedAccounts()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then
        val accountNames = result.accounts.map { it.name }
        assertTrue(accountNames.contains("Assets:Bank:Checking"))
        assertTrue(accountNames.contains("Assets:Bank"))
        assertTrue(accountNames.contains("Assets"))
    }

    @Test
    fun `parse does not duplicate accounts`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithDuplicateAccounts()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then
        val assetsCount = result.accounts.count { it.name == "Assets:Bank" }
        assertEquals(1, assetsCount)
    }

    @Test
    fun `parse skips comment lines`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithComments()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then - should parse without errors
        assertTrue(result.accounts.isNotEmpty())
    }

    // ========================================
    // Transaction parsing tests
    // ========================================

    @Test
    fun `parse extracts transactions from HTML`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithTransactions()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then
        assertTrue(result.transactions.isNotEmpty())
        assertEquals("Test transaction", result.transactions[0].description)
    }

    @Test
    fun `parse extracts transaction date`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithTransactions()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then
        assertEquals(2024, result.transactions[0].date.year)
        assertEquals(1, result.transactions[0].date.month)
        assertEquals(15, result.transactions[0].date.day)
    }

    @Test
    fun `parse extracts transaction ledger ID`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithTransactions()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then
        assertEquals(1L, result.transactions[0].ledgerId)
    }

    @Test
    fun `parse stops at end marker`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithEndMarker()

        // When
        val result = parser.parse(profile, 0) { _, _ -> }

        // Then - should only parse content before the end marker
        assertEquals(1, result.transactions.size)
    }

    // ========================================
    // Progress reporting tests
    // ========================================

    @Test
    fun `parse reports progress for transactions`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithMultipleTransactions()
        val progressReports = mutableListOf<Pair<Int, Int>>()

        // When
        parser.parse(profile, 10) { current, total ->
            progressReports.add(current to total)
        }

        // Then
        assertTrue(progressReports.isNotEmpty())
        assertEquals(10, progressReports.last().second)
    }

    @Test
    fun `parse does not report progress when expectedPostingsCount is zero`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResponses["journal"] = createHtmlWithMultipleTransactions()
        val progressReports = mutableListOf<Pair<Int, Int>>()

        // When
        parser.parse(profile, 0) { current, total ->
            progressReports.add(current to total)
        }

        // Then
        assertTrue(progressReports.isEmpty())
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test(expected = NetworkHttpException::class)
    fun `parse throws NetworkHttpException for auth error`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResults["journal"] = Result.failure(
            NetworkAuthenticationException("Unauthorized")
        )

        // When
        parser.parse(profile, 0) { _, _ -> }

        // Then - exception is thrown
    }

    @Test(expected = NetworkHttpException::class)
    fun `parse throws NetworkHttpException for server error`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResults["journal"] = Result.failure(
            NetworkHttpException(500, "Internal Server Error")
        )

        // When
        parser.parse(profile, 0) { _, _ -> }

        // Then - exception is thrown
    }

    @Test(expected = RuntimeException::class)
    fun `parse rethrows unknown exceptions`() = runTest {
        // Given
        val profile = createTestDomainProfile()
        fakeClient.getResults["journal"] = Result.failure(
            RuntimeException("Unknown error")
        )

        // When
        parser.parse(profile, 0) { _, _ -> }

        // Then - exception is thrown
    }

    // ========================================
    // Helper methods for test HTML
    // ========================================

    /**
     * Creates HTML with basic account list.
     */
    private fun createHtmlWithAccounts(): String = """
        <a href="/register?q=inacct%3AAssets%3ABank">Assets:Bank</a>
        <span class="amount">1000.00 USD</span>
        <h2>General Journal</h2>
        <div id="addmodal">
    """.trimIndent()

    /**
     * Creates HTML with account amounts.
     */
    private fun createHtmlWithAccountAmounts(): String = """
        <a href="/register?q=inacct%3AAssets%3ABank">Assets:Bank</a>
        <span class="amount">1000.00 USD</span>
        <h2>General Journal</h2>
        <div id="addmodal">
    """.trimIndent()

    /**
     * Creates HTML with decimal comma format (European style).
     */
    private fun createHtmlWithDecimalComma(): String = """
        <a href="/register?q=inacct%3AAssets%3ABank">Assets:Bank</a>
        <span class="amount">1.000,50 EUR</span>
        <h2>General Journal</h2>
        <div id="addmodal">
    """.trimIndent()

    /**
     * Creates HTML with decimal point format (US style).
     */
    private fun createHtmlWithDecimalPoint(): String = """
        <a href="/register?q=inacct%3AAssets%3ABank">Assets:Bank</a>
        <span class="amount">1,000.50 USD</span>
        <h2>General Journal</h2>
        <div id="addmodal">
    """.trimIndent()

    /**
     * Creates HTML with nested accounts.
     */
    private fun createHtmlWithNestedAccounts(): String = """
        <a href="/register?q=inacct%3AAssets%3ABank%3AChecking">Assets:Bank:Checking</a>
        <span class="amount">1000.00 USD</span>
        <h2>General Journal</h2>
        <div id="addmodal">
    """.trimIndent()

    /**
     * Creates HTML with duplicate account names.
     */
    private fun createHtmlWithDuplicateAccounts(): String = """
        <a href="/register?q=inacct%3AAssets%3ABank">Assets:Bank</a>
        <span class="amount">1000.00 USD</span>
        <a href="/register?q=inacct%3AAssets%3ABank">Assets:Bank</a>
        <span class="amount">500.00 USD</span>
        <h2>General Journal</h2>
        <div id="addmodal">
    """.trimIndent()

    /**
     * Creates HTML with comment lines.
     */
    private fun createHtmlWithComments(): String = """
        ; This is a comment
        <a href="/register?q=inacct%3AAssets%3ABank">Assets:Bank</a>
        <span class="amount">1000.00 USD</span>
        <h2>General Journal</h2>
        <div id="addmodal">
    """.trimIndent()

    /**
     * Creates HTML with transactions.
     */
    private fun createHtmlWithTransactions(): String = """
        <h2>General Journal</h2>
        <tr class="title" id="transaction-1"><td class="date">2024-01-15</td>
        <tr class="posting" title="2024-01-15 Test transaction
         Assets:Bank  100.00 USD

        <div id="addmodal">
    """.trimIndent()

    /**
     * Creates HTML with end marker.
     */
    private fun createHtmlWithEndMarker(): String = """
        <h2>General Journal</h2>
        <tr class="title" id="transaction-1"><td class="date">2024-01-15</td>
        <tr class="posting" title="2024-01-15 First transaction

        <div id="addmodal">
        <tr class="title" id="transaction-2"><td class="date">2024-01-16</td>
        <tr class="posting" title="2024-01-16 Second transaction (should not be parsed)
    """.trimIndent()

    /**
     * Creates HTML with multiple transactions.
     */
    private fun createHtmlWithMultipleTransactions(): String = """
        <h2>General Journal</h2>
        <tr class="title" id="transaction-1"><td class="date">2024-01-15</td>
        <tr class="posting" title="2024-01-15 First transaction

        <tr class="title" id="transaction-2"><td class="date">2024-01-16</td>
        <tr class="posting" title="2024-01-16 Second transaction

        <tr class="title" id="transaction-3"><td class="date">2024-01-17</td>
        <tr class="posting" title="2024-01-17 Third transaction

        <div id="addmodal">
    """.trimIndent()
}
