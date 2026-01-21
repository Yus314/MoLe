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

package net.ktnx.mobileledger.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.network.FormPostResponse
import net.ktnx.mobileledger.network.HledgerClient
import net.ktnx.mobileledger.network.NetworkApiNotSupportedException
import net.ktnx.mobileledger.network.NetworkAuthenticationException
import net.ktnx.mobileledger.util.createTestDomainProfile
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration tests for [TransactionSenderImpl] using Robolectric for Android framework support
 * and MockK for mocking [HledgerClient].
 *
 * These tests verify the actual implementation logic including:
 * - JSON API success paths for specific API versions
 * - Automatic API version detection
 * - HTML form fallback when JSON API not supported
 * - Error handling (network errors, authentication errors)
 * - Retry logic for HTML form submission
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TransactionSenderImplIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockHledgerClient: HledgerClient
    private lateinit var sender: TransactionSenderImpl
    private lateinit var testTransaction: Transaction

    @Before
    fun setup() {
        mockHledgerClient = mockk(relaxed = true)
        sender = TransactionSenderImpl(mockHledgerClient, testDispatcher)

        testTransaction = Transaction(
            id = null,
            ledgerId = 0L,
            date = SimpleDate.today(),
            description = "Test transaction",
            comment = null,
            lines = listOf(
                TransactionLine(
                    id = null,
                    accountName = "Expenses:Test",
                    amount = 100.0f,
                    currency = "USD",
                    comment = null
                ),
                TransactionLine(
                    id = null,
                    accountName = "Assets:Bank",
                    amount = -100.0f,
                    currency = "USD",
                    comment = null
                )
            )
        )
    }

    // ========================================
    // Validation tests
    // ========================================

    @Test
    fun `send fails when profile has no id`() = runTest(testDispatcher) {
        // Given
        val profileWithoutId = createTestDomainProfile(id = null)

        // When
        val result = sender.send(profileWithoutId, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should fail for unsaved profile", result.isFailure)
        assertTrue(
            "Error should indicate unsaved profile",
            result.exceptionOrNull() is IllegalStateException
        )
    }

    // ========================================
    // JSON API success tests
    // ========================================

    @Test
    fun `send via JSON API v1_50 succeeds`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_50.toInt())
        coEvery {
            mockHledgerClient.putJson(any(), any(), any(), any())
        } returns Result.success(Unit)

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 1) { mockHledgerClient.putJson(profile, "add", any(), null) }
    }

    @Test
    fun `send via JSON API v1_14 succeeds`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_14.toInt())
        coEvery {
            mockHledgerClient.putJson(any(), any(), any(), any())
        } returns Result.success(Unit)

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockHledgerClient.putJson(profile, "add", any(), null) }
    }

    @Test
    fun `send via JSON API v1_23 succeeds`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_23.toInt())
        coEvery {
            mockHledgerClient.putJson(any(), any(), any(), any())
        } returns Result.success(Unit)

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should succeed", result.isSuccess)
    }

    // ========================================
    // Auto version detection tests
    // ========================================

    @Test
    fun `auto version tries v1_50 first then succeeds`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.auto.toInt())
        coEvery {
            mockHledgerClient.putJson(any(), any(), any(), any())
        } returns Result.success(Unit)

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        // v1_50 is tried first in API.allVersions
        coVerify(atLeast = 1) { mockHledgerClient.putJson(any(), "add", any(), null) }
    }

    @Test
    fun `auto version falls back to older version when newer not supported`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.auto.toInt())
        var callCount = 0
        coEvery {
            mockHledgerClient.putJson(any(), any(), any(), any())
        } answers {
            callCount++
            if (callCount <= 2) {
                // First 2 versions fail (v1_50, v1_40)
                Result.failure(NetworkApiNotSupportedException("Not supported"))
            } else {
                // Third version succeeds (v1_32)
                Result.success(Unit)
            }
        }

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should succeed after fallback", result.isSuccess)
        assertEquals("Should try 3 versions", 3, callCount)
    }

    // ========================================
    // HTML form fallback tests
    // ========================================

    @Test
    fun `HTML mode sends form post directly`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.html.toInt())
        coEvery {
            mockHledgerClient.postForm(any(), any(), any(), any(), any())
        } returns Result.success(
            FormPostResponse(
                statusCode = 303,
                body = "",
                cookies = emptyMap()
            )
        )

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockHledgerClient.postForm(profile, "add", any(), any(), null) }
    }

    @Test
    fun `auto mode falls back to HTML when all JSON versions fail`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.auto.toInt())

        // All JSON API calls fail
        coEvery {
            mockHledgerClient.putJson(any(), any(), any(), any())
        } returns Result.failure(NetworkApiNotSupportedException("Not supported"))

        // HTML form succeeds
        coEvery {
            mockHledgerClient.postForm(any(), any(), any(), any(), any())
        } returns Result.success(
            FormPostResponse(
                statusCode = 303,
                body = "",
                cookies = emptyMap()
            )
        )

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should succeed via HTML fallback", result.isSuccess)
        // Should try all JSON versions (7) then HTML
        coVerify(exactly = API.allVersions.size) {
            mockHledgerClient.putJson(any(), "add", any(), null)
        }
        coVerify(atLeast = 1) { mockHledgerClient.postForm(any(), "add", any(), any(), null) }
    }

    @Test
    fun `HTML form retry on 200 with token extraction`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.html.toInt())
        var callCount = 0

        coEvery {
            mockHledgerClient.postForm(any(), any(), any(), any(), any())
        } answers {
            callCount++
            if (callCount == 1) {
                // First call: server returns 200 with token (needs retry)
                Result.success(
                    FormPostResponse(
                        statusCode = 200,
                        body = """<input type="hidden" name="_token" value="abc123">""",
                        cookies = mapOf("_SESSION" to "session456")
                    )
                )
            } else {
                // Second call: success with 303 redirect
                Result.success(
                    FormPostResponse(
                        statusCode = 303,
                        body = "",
                        cookies = emptyMap()
                    )
                )
            }
        }

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should succeed after retry", result.isSuccess)
        assertEquals("Should retry once", 2, callCount)
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test
    fun `network error returns failure`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_50.toInt())
        coEvery {
            mockHledgerClient.putJson(any(), any(), any(), any())
        } returns Result.failure(IOException("Network unreachable"))

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should fail on network error", result.isFailure)
        assertTrue("Should be IOException", result.exceptionOrNull() is IOException)
    }

    @Test
    fun `authentication error returns failure`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.v1_50.toInt())
        coEvery {
            mockHledgerClient.putJson(any(), any(), any(), any())
        } returns Result.failure(NetworkAuthenticationException("Unauthorized"))

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should fail on auth error", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Should have exception", exception)
        assertTrue(
            "Should be auth exception",
            exception is NetworkAuthenticationException
        )
    }

    @Test
    fun `HTML form max retry exceeded returns failure`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.html.toInt())

        // Always return 200 with token (triggers retry)
        coEvery {
            mockHledgerClient.postForm(any(), any(), any(), any(), any())
        } returns Result.success(
            FormPostResponse(
                statusCode = 200,
                body = """<input type="hidden" name="_token" value="abc123">""",
                cookies = mapOf("_SESSION" to "session456")
            )
        )

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should fail after max retries", result.isFailure)
        assertTrue(
            "Should indicate retry failure",
            result.exceptionOrNull()?.message?.contains("aborting after") == true
        )
    }

    @Test
    fun `HTML form error response returns failure`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.html.toInt())
        coEvery {
            mockHledgerClient.postForm(any(), any(), any(), any(), any())
        } returns Result.success(
            FormPostResponse(
                statusCode = 500,
                body = "Internal Server Error",
                cookies = emptyMap()
            )
        )

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should fail on server error", result.isFailure)
    }

    @Test
    fun `HTML form missing token in response fails`() = runTest(testDispatcher) {
        // Given
        val profile = createTestDomainProfile(apiVersion = API.html.toInt())
        coEvery {
            mockHledgerClient.postForm(any(), any(), any(), any(), any())
        } returns Result.success(
            FormPostResponse(
                statusCode = 200,
                body = "No token here", // Missing _token field
                cookies = mapOf("_SESSION" to "session456")
            )
        )

        // When
        val result = sender.send(profile, testTransaction, simulate = false)

        advanceUntilIdle()

        // Then
        assertTrue("Should fail without token", result.isFailure)
        assertTrue(
            "Should indicate missing token",
            result.exceptionOrNull()?.message?.contains("_token") == true
        )
    }
}
