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

import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.domain.model.ProfileAuthentication
import net.ktnx.mobileledger.core.network.NetworkNotFoundException
import net.ktnx.mobileledger.core.testing.fake.FakeHledgerClient
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for the real [VersionDetectorImpl] implementation.
 *
 * These tests verify the actual version detection logic:
 * - Version parsing from quoted and unquoted formats
 * - 404 handling (pre-1.19 servers)
 * - Network error propagation
 * - Authentication data construction
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VersionDetectorImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeHledgerClient: FakeHledgerClient
    private lateinit var versionDetector: VersionDetectorImpl

    @Before
    fun setup() {
        fakeHledgerClient = FakeHledgerClient()
        versionDetector = VersionDetectorImpl(fakeHledgerClient, testDispatcher)
    }

    // ========================================
    // Success cases - version parsing
    // ========================================

    @Test
    fun `detect returns version from quoted format`() = runTest(testDispatcher) {
        // Given - quoted version like "1.32"
        fakeHledgerClient.getResponses["version"] = "\"1.32\""

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals("1.32", result.getOrNull())
    }

    @Test
    fun `detect returns version from quoted format with patch version`() = runTest(testDispatcher) {
        // Given - quoted version like "1.32.1"
        fakeHledgerClient.getResponses["version"] = "\"1.32.1\""

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals("1.32", result.getOrNull()) // Only major.minor returned
    }

    @Test
    fun `detect returns version from unquoted format`() = runTest(testDispatcher) {
        // Given - unquoted version like 1.32
        fakeHledgerClient.getResponses["version"] = "1.32"

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals("1.32", result.getOrNull())
    }

    @Test
    fun `detect returns version from unquoted format with patch version`() = runTest(testDispatcher) {
        // Given - unquoted version like 1.40.2
        fakeHledgerClient.getResponses["version"] = "1.40.2"

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals("1.40", result.getOrNull())
    }

    // ========================================
    // 404 handling (pre-1.19)
    // ========================================

    @Test
    fun `detect returns pre-1_19 when 404 response`() = runTest(testDispatcher) {
        // Given - 404 Not Found (old hledger-web without /version endpoint)
        fakeHledgerClient.getResults["version"] = Result.failure(
            NetworkNotFoundException("Not Found")
        )

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals("pre-1.19", result.getOrNull())
    }

    // ========================================
    // Error handling
    // ========================================

    @Test
    fun `detect returns failure on network error`() = runTest(testDispatcher) {
        // Given - network error
        val networkError = IOException("Connection refused")
        fakeHledgerClient.shouldFailWith = networkError

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue(result.isFailure)
        assertEquals(networkError, result.exceptionOrNull())
    }

    @Test
    fun `detect returns failure on unparseable version`() = runTest(testDispatcher) {
        // Given - garbage response
        fakeHledgerClient.getResponses["version"] = "not a version"

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `detect returns failure on empty response`() = runTest(testDispatcher) {
        // Given - empty response
        fakeHledgerClient.getResults["version"] = Result.success(
            ByteArrayInputStream(ByteArray(0))
        )

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue(result.isFailure)
    }

    // ========================================
    // Authentication
    // ========================================

    @Test
    fun `detect uses authentication when provided`() = runTest(testDispatcher) {
        // Given
        fakeHledgerClient.getResponses["version"] = "\"1.32\""

        // When
        versionDetector.detect(
            url = "https://example.com",
            useAuth = true,
            user = "testuser",
            password = "testpass"
        )

        // Then - verify auth was used
        val request = fakeHledgerClient.requestHistory.first()
        assertNotNull(request.temporaryAuth)
        assertEquals(true, request.temporaryAuth?.useAuthentication)
        assertEquals("testuser", request.temporaryAuth?.authUser)
        assertEquals("testpass", request.temporaryAuth?.authPassword)
    }

    @Test
    fun `detect does not use auth when useAuth is false`() = runTest(testDispatcher) {
        // Given
        fakeHledgerClient.getResponses["version"] = "\"1.32\""

        // When
        versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = "ignored",
            password = "ignored"
        )

        // Then - verify no auth
        val request = fakeHledgerClient.requestHistory.first()
        assertNull(request.temporaryAuth)
    }

    @Test
    fun `detect does not use auth when user is empty`() = runTest(testDispatcher) {
        // Given
        fakeHledgerClient.getResponses["version"] = "\"1.32\""

        // When
        versionDetector.detect(
            url = "https://example.com",
            useAuth = true,
            user = "",
            password = "password"
        )

        // Then - verify no auth (empty user)
        val request = fakeHledgerClient.requestHistory.first()
        assertNull(request.temporaryAuth)
    }

    @Test
    fun `detect uses empty password when password is null`() = runTest(testDispatcher) {
        // Given
        fakeHledgerClient.getResponses["version"] = "\"1.32\""

        // When
        versionDetector.detect(
            url = "https://example.com",
            useAuth = true,
            user = "testuser",
            password = null
        )

        // Then - verify auth with empty password
        val request = fakeHledgerClient.requestHistory.first()
        assertNotNull(request.temporaryAuth)
        assertEquals("", request.temporaryAuth?.authPassword)
    }

    // ========================================
    // Request verification
    // ========================================

    @Test
    fun `detect calls version endpoint`() = runTest(testDispatcher) {
        // Given
        fakeHledgerClient.getResponses["version"] = "\"1.32\""

        // When
        versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertEquals(1, fakeHledgerClient.requestHistory.size)
        assertEquals("version", fakeHledgerClient.requestHistory[0].path)
        assertEquals("GET", fakeHledgerClient.requestHistory[0].method)
    }

    // ========================================
    // Profile-based detection
    // ========================================

    @Test
    fun `detect from profile uses profile properties`() = runTest(testDispatcher) {
        // Given
        fakeHledgerClient.getResponses["version"] = "\"1.28\""

        val profile = createTestDomainProfile(
            id = 1L,
            name = "Test Profile",
            url = "https://profile.example.com",
            authentication = ProfileAuthentication("profileuser", "profilepass")
        )

        // When
        val result = versionDetector.detect(profile)

        // Then
        assertTrue("Detection should succeed", result.isSuccess)
        assertEquals("1.28", result.getOrNull())
    }

    @Test
    fun `detect from profile without auth does not send auth`() = runTest(testDispatcher) {
        // Given
        fakeHledgerClient.getResponses["version"] = "\"1.28\""

        val profile = createTestDomainProfile(
            id = 1L,
            name = "Test Profile",
            url = "https://profile.example.com",
            authentication = null
        )

        // When
        versionDetector.detect(profile)

        // Then - verify no auth was sent
        val request = fakeHledgerClient.requestHistory.first()
        assertNull(request.temporaryAuth)
    }
}
