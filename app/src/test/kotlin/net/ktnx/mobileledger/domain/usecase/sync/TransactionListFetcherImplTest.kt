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
import net.ktnx.mobileledger.fake.FakeHledgerClient
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.json.ApiNotSupportedException
import net.ktnx.mobileledger.network.NetworkAuthenticationException
import net.ktnx.mobileledger.network.NetworkHttpException
import net.ktnx.mobileledger.network.NetworkNotFoundException
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [TransactionListFetcherImpl].
 *
 * Tests verify:
 * - API version handling (auto, specific versions v1_32+)
 * - Transaction list fetching and parsing
 * - Progress reporting
 * - Transaction sorting
 * - Error handling for various network exceptions
 */
@RunWith(RobolectricTestRunner::class)
class TransactionListFetcherImplTest {

    private lateinit var fakeClient: FakeHledgerClient
    private lateinit var fetcher: TransactionListFetcherImpl

    @Before
    fun setup() {
        fakeClient = FakeHledgerClient()
        fetcher = TransactionListFetcherImpl(fakeClient)
    }

    // ========================================
    // API version handling tests
    // ========================================

    @Test
    fun `fetch with specific API version uses that version`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResponses["transactions"] = createV132TransactionsJson()

        // When
        val result = fetcher.fetch(profile, 0) { _, _ -> }

        // Then
        assertNotNull(result)
        assertEquals(1, fakeClient.requestHistory.size)
        assertEquals("transactions", fakeClient.requestHistory[0].path)
    }

    @Test
    fun `fetch with auto version tries versions until success`() = runTest {
        // Given - auto is apiVersion 0
        val profile = createTestDomainProfile(apiVersion = API.auto.toInt())
        fakeClient.getResponses["transactions"] = createV132TransactionsJson()

        // When
        val result = fetcher.fetch(profile, 0) { _, _ -> }

        // Then
        assertNotNull(result)
    }

    @Test(expected = ApiNotSupportedException::class)
    fun `fetch with auto version throws when no version works`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.auto.toInt())
        // Return invalid JSON that will cause exceptions for all versions
        fakeClient.getResponses["transactions"] = "not valid json"

        // When
        fetcher.fetch(profile, 0) { _, _ -> }

        // Then - exception is thrown
    }

    // ========================================
    // Transaction parsing tests
    // ========================================

    @Test
    fun `fetch parses transactions correctly`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResponses["transactions"] = createV132TransactionsJson()

        // When
        val result = fetcher.fetch(profile, 0) { _, _ -> }

        // Then
        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals("Test transaction", result[0].description)
    }

    @Test
    fun `fetch parses transaction with postings`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResponses["transactions"] = createV132TransactionWithPostingsJson()

        // When
        val result = fetcher.fetch(profile, 0) { _, _ -> }

        // Then
        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals(2, result[0].lines.size)
    }

    @Test
    fun `fetch returns empty list for empty JSON array`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResponses["transactions"] = "[]"

        // When
        val result = fetcher.fetch(profile, 0) { _, _ -> }

        // Then
        assertNotNull(result)
        assertTrue(result!!.isEmpty())
    }

    // ========================================
    // Progress reporting tests
    // ========================================

    @Test
    fun `fetch reports progress for transactions with postings`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResponses["transactions"] = createV132TransactionWithPostingsJson()
        val progressReports = mutableListOf<Pair<Int, Int>>()

        // When
        fetcher.fetch(profile, 10) { current, total ->
            progressReports.add(current to total)
        }

        // Then
        assertTrue(progressReports.isNotEmpty())
        assertEquals(10, progressReports.last().second)
    }

    @Test
    fun `fetch does not report progress when expectedPostingsCount is zero`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResponses["transactions"] = createV132TransactionWithPostingsJson()
        val progressReports = mutableListOf<Pair<Int, Int>>()

        // When
        fetcher.fetch(profile, 0) { current, total ->
            progressReports.add(current to total)
        }

        // Then
        assertTrue(progressReports.isEmpty())
    }

    // ========================================
    // Transaction sorting tests
    // ========================================

    @Test
    fun `fetch sorts transactions by date descending`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResponses["transactions"] = createV132MultipleTransactionsJson()

        // When
        val result = fetcher.fetch(profile, 0) { _, _ -> }

        // Then
        assertNotNull(result)
        assertEquals(3, result!!.size)
        // Should be sorted by date descending
        assertEquals("Third transaction", result[0].description)
        assertEquals("Second transaction", result[1].description)
        assertEquals("First transaction", result[2].description)
    }

    @Test
    fun `fetch sorts by ledgerId when dates are equal`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResponses["transactions"] = createV132SameDateTransactionsJson()

        // When
        val result = fetcher.fetch(profile, 0) { _, _ -> }

        // Then
        assertNotNull(result)
        assertEquals(2, result!!.size)
        // Same date, should be sorted by ledgerId descending
        assertTrue(result[0].ledgerId > result[1].ledgerId)
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test
    fun `fetch returns null when endpoint not found`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResults["transactions"] = Result.failure(NetworkNotFoundException("Not found"))

        // When
        val result = fetcher.fetch(profile, 0) { _, _ -> }

        // Then
        assertEquals(null, result)
    }

    @Test(expected = NetworkHttpException::class)
    fun `fetch throws NetworkHttpException for auth error`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResults["transactions"] = Result.failure(
            NetworkAuthenticationException("Unauthorized")
        )

        // When
        fetcher.fetch(profile, 0) { _, _ -> }

        // Then - exception is thrown
    }

    @Test(expected = NetworkHttpException::class)
    fun `fetch throws NetworkHttpException for server error`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResults["transactions"] = Result.failure(
            NetworkHttpException(500, "Internal Server Error")
        )

        // When
        fetcher.fetch(profile, 0) { _, _ -> }

        // Then - exception is thrown
    }

    @Test(expected = RuntimeException::class)
    fun `fetch rethrows unknown exceptions`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_32.toInt())
        fakeClient.getResults["transactions"] = Result.failure(
            RuntimeException("Unknown error")
        )

        // When
        fetcher.fetch(profile, 0) { _, _ -> }

        // Then - exception is thrown
    }

    // ========================================
    // Helper methods for test JSON
    // ========================================

    /**
     * Creates a basic v1.32 transactions JSON response with one transaction.
     */
    private fun createV132TransactionsJson(): String = """
        [
            {
                "tindex": 1,
                "tdate": "2024-01-15",
                "tdescription": "Test transaction",
                "tpostings": []
            }
        ]
    """.trimIndent()

    /**
     * Creates v1.32 transactions JSON with postings.
     */
    private fun createV132TransactionWithPostingsJson(): String = """
        [
            {
                "tindex": 1,
                "tdate": "2024-01-15",
                "tdescription": "Grocery shopping",
                "tpostings": [
                    {
                        "paccount": "Expenses:Food",
                        "pamount": [{"acommodity": "USD", "aquantity": {"decimalMantissa": 5000, "decimalPlaces": 2}}]
                    },
                    {
                        "paccount": "Assets:Checking",
                        "pamount": [{"acommodity": "USD", "aquantity": {"decimalMantissa": -5000, "decimalPlaces": 2}}]
                    }
                ]
            }
        ]
    """.trimIndent()

    /**
     * Creates v1.32 transactions JSON with multiple transactions on different dates.
     */
    private fun createV132MultipleTransactionsJson(): String = """
        [
            {
                "tindex": 1,
                "tdate": "2024-01-10",
                "tdescription": "First transaction",
                "tpostings": []
            },
            {
                "tindex": 2,
                "tdate": "2024-01-15",
                "tdescription": "Second transaction",
                "tpostings": []
            },
            {
                "tindex": 3,
                "tdate": "2024-01-20",
                "tdescription": "Third transaction",
                "tpostings": []
            }
        ]
    """.trimIndent()

    /**
     * Creates v1.32 transactions JSON with transactions on the same date.
     */
    private fun createV132SameDateTransactionsJson(): String = """
        [
            {
                "tindex": 1,
                "tdate": "2024-01-15",
                "tdescription": "Transaction 1",
                "tpostings": []
            },
            {
                "tindex": 2,
                "tdate": "2024-01-15",
                "tdescription": "Transaction 2",
                "tpostings": []
            }
        ]
    """.trimIndent()
}
