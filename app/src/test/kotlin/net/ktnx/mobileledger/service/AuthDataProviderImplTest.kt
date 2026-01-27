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

package net.ktnx.mobileledger.service

import android.app.Activity
import android.content.res.ColorStateList
import android.content.res.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.TemporaryAuthData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [AuthDataProviderImpl].
 *
 * Tests the authentication data management and theme service delegation.
 * Uses Robolectric for BackupManager static method compatibility.
 */
@RunWith(RobolectricTestRunner::class)
class AuthDataProviderImplTest {

    private lateinit var fakeThemeService: FakeThemeService
    private lateinit var authDataProvider: AuthDataProviderImpl

    @Before
    fun setup() {
        fakeThemeService = FakeThemeService()
        authDataProvider = AuthDataProviderImpl(fakeThemeService)
    }

    // region setTemporaryAuthData / getTemporaryAuthData

    @Test
    fun `when no auth data set then getTemporaryAuthData returns null`() {
        // Given: fresh instance

        // When
        val result = authDataProvider.getTemporaryAuthData()

        // Then
        assertNull(result)
    }

    @Test
    fun `when auth data set then getTemporaryAuthData returns same data`() {
        // Given
        val authData = createTestAuthData()

        // When
        authDataProvider.setTemporaryAuthData(authData)
        val result = authDataProvider.getTemporaryAuthData()

        // Then
        assertNotNull(result)
        assertEquals(authData.url, result?.url)
        assertEquals(authData.useAuthentication, result?.useAuthentication)
        assertEquals(authData.authUser, result?.authUser)
        assertEquals(authData.authPassword, result?.authPassword)
    }

    @Test
    fun `when auth data set to null then getTemporaryAuthData returns null`() {
        // Given
        authDataProvider.setTemporaryAuthData(createTestAuthData())

        // When
        authDataProvider.setTemporaryAuthData(null)
        val result = authDataProvider.getTemporaryAuthData()

        // Then
        assertNull(result)
    }

    @Test
    fun `when auth data updated then getTemporaryAuthData returns new data`() {
        // Given
        val firstAuthData = createTestAuthData(url = "https://first.example.com")
        val secondAuthData = createTestAuthData(url = "https://second.example.com")

        // When
        authDataProvider.setTemporaryAuthData(firstAuthData)
        authDataProvider.setTemporaryAuthData(secondAuthData)
        val result = authDataProvider.getTemporaryAuthData()

        // Then
        assertEquals("https://second.example.com", result?.url)
    }

    // endregion

    // region resetAuthenticationData

    @Test
    fun `when resetAuthenticationData called then auth data cleared`() {
        // Given
        authDataProvider.setTemporaryAuthData(createTestAuthData())

        // When
        authDataProvider.resetAuthenticationData()
        val result = authDataProvider.getTemporaryAuthData()

        // Then
        assertNull(result)
    }

    @Test
    fun `when resetAuthenticationData called multiple times then no error`() {
        // Given
        authDataProvider.setTemporaryAuthData(createTestAuthData())

        // When
        authDataProvider.resetAuthenticationData()
        authDataProvider.resetAuthenticationData()
        val result = authDataProvider.getTemporaryAuthData()

        // Then
        assertNull(result)
    }

    // endregion

    // region notifyBackupDataChanged

    @Test
    fun `when notifyBackupDataChanged called then no exception thrown`() {
        // Given: Robolectric provides BackupManager stub

        // When / Then: should not throw
        authDataProvider.notifyBackupDataChanged()
    }

    // endregion

    // region getDefaultThemeHue

    @Test
    fun `when getDefaultThemeHue called then returns DEFAULT_HUE_DEG`() {
        // Given
        val expectedHue = ThemeService.DEFAULT_HUE_DEG

        // When
        val result = authDataProvider.getDefaultThemeHue()

        // Then
        assertEquals(expectedHue, result)
        assertEquals(261, result)
    }

    // endregion

    // region getNewProfileThemeHue

    @Test
    fun `when getNewProfileThemeHue with null profiles then delegates to themeService`() {
        // Given
        fakeThemeService.nextNewProfileHue = 180

        // When
        val result = authDataProvider.getNewProfileThemeHue(null)

        // Then
        assertEquals(180, result)
        assertEquals(1, fakeThemeService.getNewProfileThemeHueCallCount)
    }

    @Test
    fun `when getNewProfileThemeHue with empty list then delegates to themeService`() {
        // Given
        fakeThemeService.nextNewProfileHue = 90

        // When
        val result = authDataProvider.getNewProfileThemeHue(emptyList())

        // Then
        assertEquals(90, result)
    }

    @Test
    fun `when getNewProfileThemeHue with profiles then delegates to themeService`() {
        // Given
        val profiles = listOf(
            createTestProfile(id = 1, theme = 0),
            createTestProfile(id = 2, theme = 120)
        )
        fakeThemeService.nextNewProfileHue = 240

        // When
        val result = authDataProvider.getNewProfileThemeHue(profiles)

        // Then
        assertEquals(240, result)
        assertEquals(profiles, fakeThemeService.lastReceivedProfiles)
    }

    // endregion

    // region Helper methods

    private fun createTestAuthData(
        url: String = "https://test.example.com",
        useAuthentication: Boolean = true,
        authUser: String = "testuser",
        authPassword: String = "testpassword"
    ): TemporaryAuthData = TemporaryAuthData(
        url = url,
        useAuthentication = useAuthentication,
        authUser = authUser,
        authPassword = authPassword
    )

    private fun createTestProfile(id: Long = 1L, name: String = "Test Profile", theme: Int = 0): Profile = Profile(
        id = id,
        name = name,
        uuid = "test-uuid-$id",
        url = "https://test.example.com",
        theme = theme
    )

    // endregion

    /**
     * Fake ThemeService for testing delegation behavior.
     */
    private class FakeThemeService : ThemeService {
        private val _currentThemeHue = MutableStateFlow(ThemeService.DEFAULT_HUE_DEG)
        override val currentThemeHue: StateFlow<Int> = _currentThemeHue

        override val primaryColor: Int = 0
        override val tableRowDarkBG: Int = 0

        var nextNewProfileHue: Int = ThemeService.DEFAULT_HUE_DEG
        var getNewProfileThemeHueCallCount: Int = 0
        var lastReceivedProfiles: List<Profile>? = null

        override fun setupTheme(activity: Activity, themeHue: Int) {
            // No-op for testing
        }

        override fun refreshColors(theme: Resources.Theme) {
            // No-op for testing
        }

        override fun setCurrentThemeHue(hue: Int) {
            _currentThemeHue.value = hue
        }

        override fun getPrimaryColorForHue(hueDegrees: Int): Int = 0

        override fun getThemeIdForHue(themeHue: Int): Int = 0

        override fun getNewProfileThemeHue(profiles: List<Profile>?): Int {
            getNewProfileThemeHueCallCount++
            lastReceivedProfiles = profiles
            return nextNewProfileHue
        }

        override fun getColorStateList(hue: Int?): ColorStateList = ColorStateList.valueOf(0)

        override fun getSwipeCircleColors(hue: Int?): IntArray = intArrayOf()
    }
}
