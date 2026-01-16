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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.ProfileAuthentication
import net.ktnx.mobileledger.fake.FakeVersionDetector
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * VersionDetector のテスト
 *
 * FakeVersionDetector を使用してバージョン検出ロジックをテストする。
 * T070-T072: バージョン検出成功/失敗のテストケース。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VersionDetectorImplTest {

    private lateinit var versionDetector: FakeVersionDetector

    @Before
    fun setup() {
        versionDetector = FakeVersionDetector()
    }

    // ==========================================
    // T071: Version detection success test case
    // ==========================================

    @Test
    fun `detect success returns Result success with version string`() = runTest {
        // Given
        versionDetector.shouldSucceed = true
        versionDetector.versionToReturn = "1.32"

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue("Detection should succeed", result.isSuccess)
        assertEquals("1.32", result.getOrNull())
        assertEquals(1, versionDetector.detectCallCount)
        assertEquals("https://example.com", versionDetector.lastDetectedUrl)
    }

    @Test
    fun `detect with authentication succeeds`() = runTest {
        // Given
        versionDetector.shouldSucceed = true
        versionDetector.versionToReturn = "1.30"

        // When
        val result = versionDetector.detect(
            url = "https://secure.example.com",
            useAuth = true,
            user = "testuser",
            password = "testpass"
        )

        // Then
        assertTrue("Detection should succeed", result.isSuccess)
        assertEquals("1.30", result.getOrNull())
    }

    @Test
    fun `detect from profile uses profile properties`() = runTest {
        // Given
        versionDetector.shouldSucceed = true
        versionDetector.versionToReturn = "1.28"

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
        assertEquals(profile, versionDetector.lastDetectedProfile)
        assertEquals("https://profile.example.com", versionDetector.lastDetectedUrl)
    }

    // ==========================================
    // T072: Version detection failure test cases
    // ==========================================

    @Test
    fun `detect failure returns Result failure`() = runTest {
        // Given
        versionDetector.shouldSucceed = false

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue("Detection should fail", result.isFailure)
        assertNotNull(result.exceptionOrNull())
        assertEquals(1, versionDetector.detectCallCount)
    }

    @Test
    fun `detect with network error returns correct exception`() = runTest {
        // Given
        val expectedException = java.net.ConnectException("Connection refused")
        versionDetector.errorToThrow = expectedException

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue("Detection should fail", result.isFailure)
        val actualException = result.exceptionOrNull()
        assertTrue(
            "Should be ConnectException",
            actualException is java.net.ConnectException
        )
        assertEquals("Connection refused", actualException?.message)
    }

    @Test
    fun `detect with timeout error returns correct exception`() = runTest {
        // Given
        val expectedException = java.net.SocketTimeoutException("Connection timed out")
        versionDetector.errorToThrow = expectedException

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = false,
            user = null,
            password = null
        )

        // Then
        assertTrue("Detection should fail", result.isFailure)
        val actualException = result.exceptionOrNull()
        assertTrue(
            "Should be SocketTimeoutException",
            actualException is java.net.SocketTimeoutException
        )
    }

    @Test
    fun `detect with authentication error returns correct exception`() = runTest {
        // Given
        val expectedException = java.io.IOException("HTTP 401 Unauthorized")
        versionDetector.errorToThrow = expectedException

        // When
        val result = versionDetector.detect(
            url = "https://example.com",
            useAuth = true,
            user = "wronguser",
            password = "wrongpass"
        )

        // Then
        assertTrue("Detection should fail", result.isFailure)
        assertTrue(
            "Should be IOException",
            result.exceptionOrNull() is java.io.IOException
        )
    }

    // ==========================================
    // Multiple call tests
    // ==========================================

    @Test
    fun `multiple detect calls track count correctly`() = runTest {
        // Given
        versionDetector.shouldSucceed = true

        // When
        versionDetector.detect("https://example1.com", false, null, null)
        versionDetector.detect("https://example2.com", false, null, null)
        versionDetector.detect("https://example3.com", false, null, null)

        // Then
        assertEquals(3, versionDetector.detectCallCount)
        assertEquals("https://example3.com", versionDetector.lastDetectedUrl)
    }

    // ==========================================
    // Reset tests
    // ==========================================

    @Test
    fun `reset clears all state`() = runTest {
        // Given - perform detection
        versionDetector.versionToReturn = "2.0"
        versionDetector.detect("https://example.com", false, null, null)

        assertEquals(1, versionDetector.detectCallCount)
        assertNotNull(versionDetector.lastDetectedUrl)
        assertEquals("2.0", versionDetector.versionToReturn)

        // When
        versionDetector.reset()

        // Then
        assertEquals(0, versionDetector.detectCallCount)
        assertNull(versionDetector.lastDetectedUrl)
        assertNull(versionDetector.lastDetectedProfile)
        assertEquals("1.32", versionDetector.versionToReturn)
        assertTrue(versionDetector.shouldSucceed)
    }
}
