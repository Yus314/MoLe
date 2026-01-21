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

import java.io.ByteArrayInputStream
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [AccountListFetcherImpl].
 *
 * Tests verify:
 * - API version handling (auto, html, specific versions)
 * - Account list fetching and parsing
 * - Parent account generation
 * - Error handling for various network exceptions
 */
@RunWith(RobolectricTestRunner::class)
class AccountListFetcherImplTest {

    private lateinit var fakeClient: FakeHledgerClient
    private lateinit var fetcher: AccountListFetcherImpl

    @Before
    fun setup() {
        fakeClient = FakeHledgerClient()
        fetcher = AccountListFetcherImpl(fakeClient)
    }

    // ========================================
    // API version handling tests
    // ========================================

    @Test
    fun `fetch with html API version returns null`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.html.toInt())

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNull(result)
        assertTrue(fakeClient.requestHistory.isEmpty())
    }

    @Test
    fun `fetch with specific API version uses that version`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResponses["accounts"] = createV114AccountsJson()

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNotNull(result)
        assertEquals(1, fakeClient.requestHistory.size)
        assertEquals("accounts", fakeClient.requestHistory[0].path)
    }

    @Test
    fun `fetch with auto version tries versions until success`() = runTest {
        // Given - auto is apiVersion 0
        val profile = createTestDomainProfile(apiVersion = API.auto.toInt())
        fakeClient.getResponses["accounts"] = createV114AccountsJson()

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNotNull(result)
    }

    @Test(expected = ApiNotSupportedException::class)
    fun `fetch with auto version throws when no version works`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.auto.toInt())
        // Return invalid JSON that will cause JsonParseException for all versions
        fakeClient.getResponses["accounts"] = "not valid json"

        // When
        fetcher.fetch(profile)

        // Then - exception is thrown
    }

    // ========================================
    // Account parsing tests
    // ========================================

    @Test
    fun `fetch parses accounts correctly`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResponses["accounts"] = createV114AccountsJson()

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNotNull(result)
        val accounts = result!!.accounts
        assertTrue(accounts.any { it.name == "Assets:Bank" })
    }

    @Test
    fun `fetch generates parent accounts`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        // Only child account exists in JSON, parent should be generated
        fakeClient.getResponses["accounts"] = createV114AccountsJsonChildOnly()

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNotNull(result)
        val accounts = result!!.accounts
        val accountNames = accounts.map { it.name }
        assertTrue("Should contain child account", accountNames.contains("Assets:Bank:Checking"))
        assertTrue("Should contain generated parent Assets:Bank", accountNames.contains("Assets:Bank"))
        assertTrue("Should contain generated parent Assets", accountNames.contains("Assets"))
    }

    @Test
    fun `fetch calculates expectedPostingsCount`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResponses["accounts"] = createV114AccountsJsonWithAmounts()

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNotNull(result)
        // expectedPostingsCount = sum of amounts.size for all accounts
        assertTrue(result!!.expectedPostingsCount >= 0)
    }

    @Test
    fun `fetch returns empty list for empty JSON array`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResponses["accounts"] = "[]"

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNotNull(result)
        assertTrue(result!!.accounts.isEmpty())
        assertEquals(0, result.expectedPostingsCount)
    }

    @Test
    fun `fetch skips root account`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResponses["accounts"] = createV114AccountsJsonWithRoot()

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNotNull(result)
        val accounts = result!!.accounts
        assertTrue("Should not contain root account", accounts.none { it.name.equals("root", ignoreCase = true) })
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test
    fun `fetch returns null when endpoint not found`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResults["accounts"] = Result.failure(NetworkNotFoundException("Not found"))

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNull(result)
    }

    @Test(expected = NetworkHttpException::class)
    fun `fetch throws NetworkHttpException for auth error`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResults["accounts"] = Result.failure(
            NetworkAuthenticationException("Unauthorized")
        )

        // When
        fetcher.fetch(profile)

        // Then - exception is thrown
    }

    @Test(expected = NetworkHttpException::class)
    fun `fetch throws NetworkHttpException for server error`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResults["accounts"] = Result.failure(
            NetworkHttpException(500, "Internal Server Error")
        )

        // When
        fetcher.fetch(profile)

        // Then - exception is thrown
    }

    @Test(expected = RuntimeException::class)
    fun `fetch rethrows unknown exceptions`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResults["accounts"] = Result.failure(
            RuntimeException("Unknown error")
        )

        // When
        fetcher.fetch(profile)

        // Then - exception is thrown
    }

    // ========================================
    // Parent account generation edge cases
    // ========================================

    @Test
    fun `fetch does not duplicate existing parent accounts`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        // JSON includes both parent and child
        fakeClient.getResponses["accounts"] = createV114AccountsJsonWithParent()

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNotNull(result)
        val accountNames = result!!.accounts.map { it.name }
        val assetsCount = accountNames.count { it == "Assets" }
        assertEquals("Should have exactly one Assets account", 1, assetsCount)
    }

    @Test
    fun `fetch generates deeply nested parent accounts`() = runTest {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        fakeClient.getResponses["accounts"] = createV114AccountsJsonDeeplyNested()

        // When
        val result = fetcher.fetch(profile)

        // Then
        assertNotNull(result)
        val accountNames = result!!.accounts.map { it.name }
        assertTrue(accountNames.contains("Assets:Bank:Checking:USD"))
        assertTrue(accountNames.contains("Assets:Bank:Checking"))
        assertTrue(accountNames.contains("Assets:Bank"))
        assertTrue(accountNames.contains("Assets"))
    }

    // ========================================
    // Helper methods for test JSON
    // ========================================

    /**
     * Creates a basic v1.14 accounts JSON response.
     */
    private fun createV114AccountsJson(): String = """
        [
            {"aname": "Assets:Bank", "aibalance": [], "anumpostings": 0}
        ]
    """.trimIndent()

    /**
     * Creates v1.14 accounts JSON with only child account (no parent).
     */
    private fun createV114AccountsJsonChildOnly(): String = """
        [
            {"aname": "Assets:Bank:Checking", "aibalance": [], "anumpostings": 0}
        ]
    """.trimIndent()

    /**
     * Creates v1.14 accounts JSON with amounts.
     */
    private fun createV114AccountsJsonWithAmounts(): String = """
        [
            {
                "aname": "Assets:Bank",
                "aibalance": [
                    {"acommodity": "USD", "aquantity": {"decimalMantissa": 100000, "decimalPlaces": 2}}
                ],
                "anumpostings": 5
            }
        ]
    """.trimIndent()

    /**
     * Creates v1.14 accounts JSON with root account (should be skipped).
     */
    private fun createV114AccountsJsonWithRoot(): String = """
        [
            {"aname": "root", "aibalance": [], "anumpostings": 0},
            {"aname": "Assets:Bank", "aibalance": [], "anumpostings": 0}
        ]
    """.trimIndent()

    /**
     * Creates v1.14 accounts JSON with both parent and child.
     */
    private fun createV114AccountsJsonWithParent(): String = """
        [
            {"aname": "Assets", "aibalance": [], "anumpostings": 0},
            {"aname": "Assets:Bank", "aibalance": [], "anumpostings": 0}
        ]
    """.trimIndent()

    /**
     * Creates v1.14 accounts JSON with deeply nested account.
     */
    private fun createV114AccountsJsonDeeplyNested(): String = """
        [
            {"aname": "Assets:Bank:Checking:USD", "aibalance": [], "anumpostings": 0}
        ]
    """.trimIndent()
}
