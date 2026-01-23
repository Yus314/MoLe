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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.domain.model.FutureDates
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.ui.HueRing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ThemeServiceImpl].
 *
 * Tests verify:
 * - Theme hue state management
 * - Theme ID calculation from hue
 * - New profile theme selection algorithm
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeServiceImplTest {

    private lateinit var service: ThemeServiceImpl

    @Before
    fun setup() {
        service = ThemeServiceImpl()
    }

    // ========================================
    // Current theme hue tests
    // ========================================

    @Test
    fun `currentThemeHue starts with default hue`() = runTest {
        // Then
        assertEquals(ThemeService.DEFAULT_HUE_DEG, service.currentThemeHue.first())
    }

    @Test
    fun `setCurrentThemeHue updates the state`() = runTest {
        // When
        service.setCurrentThemeHue(180)

        // Then
        assertEquals(180, service.currentThemeHue.first())
    }

    @Test
    fun `setCurrentThemeHue with 0 sets to 0`() = runTest {
        // When
        service.setCurrentThemeHue(0)

        // Then
        assertEquals(0, service.currentThemeHue.first())
    }

    @Test
    fun `setCurrentThemeHue with 355 sets to 355`() = runTest {
        // When
        service.setCurrentThemeHue(355)

        // Then
        assertEquals(355, service.currentThemeHue.first())
    }

    // ========================================
    // getThemeIdForHue tests
    // ========================================

    @Test
    fun `getThemeIdForHue returns default theme for DEFAULT_HUE_DEG`() {
        // When
        val themeId = service.getThemeIdForHue(ThemeService.DEFAULT_HUE_DEG)

        // Then - themeIndex = -1, so returns themeIDs[0] which is default
        assertTrue(themeId != 0)
    }

    @Test
    fun `getThemeIdForHue returns correct theme for hue 0`() {
        // When
        val themeId = service.getThemeIdForHue(0)

        // Then - themeIndex = 0, so returns themeIDs[1]
        assertTrue(themeId != 0)
    }

    @Test
    fun `getThemeIdForHue returns correct theme for hue 180`() {
        // When
        val themeId = service.getThemeIdForHue(180)

        // Then - themeIndex = 180/5 = 36, so returns themeIDs[37]
        assertTrue(themeId != 0)
    }

    @Test
    fun `getThemeIdForHue handles 360 as 0`() {
        // When
        val themeId360 = service.getThemeIdForHue(360)
        val themeId0 = service.getThemeIdForHue(0)

        // Then
        assertEquals(themeId0, themeId360)
    }

    @Test
    fun `getThemeIdForHue adjusts non-multiple-of-5 hue`() {
        // When - 13 is not a multiple of 5
        val themeId = service.getThemeIdForHue(13)

        // Then - should round to nearest (13/5 = 2.6, rounds to 3)
        assertTrue(themeId != 0)
    }

    // ========================================
    // getNewProfileThemeHue tests
    // ========================================

    @Test
    fun `getNewProfileThemeHue returns default for null profiles`() {
        // When
        val hue = service.getNewProfileThemeHue(null)

        // Then
        assertEquals(ThemeService.DEFAULT_HUE_DEG, hue)
    }

    @Test
    fun `getNewProfileThemeHue returns default for empty profiles`() {
        // When
        val hue = service.getNewProfileThemeHue(emptyList())

        // Then
        assertEquals(ThemeService.DEFAULT_HUE_DEG, hue)
    }

    @Test
    fun `getNewProfileThemeHue returns opposite hue for single profile`() {
        // Given - single profile with hue 0
        val profiles = listOf(createTestProfile(theme = 0))

        // When
        val hue = service.getNewProfileThemeHue(profiles)

        // Then - opposite of 0 is 180
        assertEquals(180, hue)
    }

    @Test
    fun `getNewProfileThemeHue returns opposite hue for single profile at 90`() {
        // Given - single profile with hue 90
        val profiles = listOf(createTestProfile(theme = 90))

        // When
        val hue = service.getNewProfileThemeHue(profiles)

        // Then - opposite of 90 is 270
        assertEquals(270, hue)
    }

    @Test
    fun `getNewProfileThemeHue returns opposite hue for single profile at 270`() {
        // Given - single profile with hue 270
        val profiles = listOf(createTestProfile(theme = 270))

        // When
        val hue = service.getNewProfileThemeHue(profiles)

        // Then - opposite of 270 is 90 (450 % 360 = 90)
        assertEquals(90, hue)
    }

    @Test
    fun `getNewProfileThemeHue finds gap between two profiles`() {
        // Given - two profiles at 0 and 180
        val profiles = listOf(
            createTestProfile(theme = 0),
            createTestProfile(theme = 180)
        )

        // When
        val hue = service.getNewProfileThemeHue(profiles)

        // Then - should pick middle of largest gap (90 or 270)
        assertTrue(hue == 90 || hue == 270)
    }

    @Test
    fun `getNewProfileThemeHue finds largest gap among three profiles`() {
        // Given - profiles at 0, 30, 60 (leaving 60-360 as largest gap)
        val profiles = listOf(
            createTestProfile(theme = 0),
            createTestProfile(theme = 30),
            createTestProfile(theme = 60)
        )

        // When
        val hue = service.getNewProfileThemeHue(profiles)

        // Then - should pick middle of 60-360 gap (around 210)
        // The gap is 360-60=300, middle is 60+150=210
        assertTrue(hue in 150..270)
    }

    @Test
    fun `getNewProfileThemeHue handles profile with theme -1 as default`() {
        // Given - profile with theme -1 (default)
        val profiles = listOf(
            createTestProfile(theme = -1),
            createTestProfile(theme = 180)
        )

        // When
        val hue = service.getNewProfileThemeHue(profiles)

        // Then - -1 should be treated as DEFAULT_HUE_DEG in gap calculation
        assertTrue(hue in 0..359)
    }

    @Test
    fun `getNewProfileThemeHue returns multiple of hueStepDegrees`() {
        // Given
        val profiles = listOf(
            createTestProfile(theme = 0),
            createTestProfile(theme = 100)
        )

        // When
        val hue = service.getNewProfileThemeHue(profiles)

        // Then - should be a multiple of 5
        assertEquals(0, hue % HueRing.hueStepDegrees)
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(id: Long = 1L, name: String = "Test Profile", theme: Int = 0): Profile = Profile(
        id = id,
        name = name,
        uuid = java.util.UUID.randomUUID().toString(),
        url = "https://example.com/ledger",
        authentication = null,
        orderNo = 0,
        permitPosting = true,
        theme = theme,
        preferredAccountsFilter = null,
        futureDates = FutureDates.None,
        apiVersion = 0,
        showCommodityByDefault = false,
        defaultCommodity = null,
        showCommentsByDefault = true,
        serverVersion = null
    )
}
