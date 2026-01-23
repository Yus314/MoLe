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

package net.ktnx.mobileledger.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import net.ktnx.mobileledger.service.ThemeService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [PreferencesRepositoryImpl].
 *
 * Uses Robolectric to provide Android Context for SharedPreferences.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PreferencesRepositoryImplTest {

    private lateinit var repository: PreferencesRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = PreferencesRepositoryImpl(context)
    }

    // ========================================
    // Show Zero Balance Accounts tests
    // ========================================

    @Test
    fun `getShowZeroBalanceAccounts returns true by default`() {
        // When
        val result = repository.getShowZeroBalanceAccounts()

        // Then - default is true per implementation
        assertTrue(result)
    }

    @Test
    fun `setShowZeroBalanceAccounts persists value`() {
        // When
        repository.setShowZeroBalanceAccounts(false)

        // Then
        assertFalse(repository.getShowZeroBalanceAccounts())
    }

    @Test
    fun `setShowZeroBalanceAccounts can toggle value`() {
        // Given
        repository.setShowZeroBalanceAccounts(false)
        assertFalse(repository.getShowZeroBalanceAccounts())

        // When
        repository.setShowZeroBalanceAccounts(true)

        // Then
        assertTrue(repository.getShowZeroBalanceAccounts())
    }

    // ========================================
    // Startup Profile ID tests
    // ========================================

    @Test
    fun `getStartupProfileId returns -1 by default`() {
        // When
        val result = repository.getStartupProfileId()

        // Then
        assertEquals(-1L, result)
    }

    @Test
    fun `setStartupProfileId persists value`() {
        // When
        repository.setStartupProfileId(42L)

        // Then
        assertEquals(42L, repository.getStartupProfileId())
    }

    @Test
    fun `setStartupProfileId can update value`() {
        // Given
        repository.setStartupProfileId(1L)
        assertEquals(1L, repository.getStartupProfileId())

        // When
        repository.setStartupProfileId(99L)

        // Then
        assertEquals(99L, repository.getStartupProfileId())
    }

    @Test
    fun `setStartupProfileId handles zero`() {
        // When
        repository.setStartupProfileId(0L)

        // Then
        assertEquals(0L, repository.getStartupProfileId())
    }

    // ========================================
    // Startup Theme tests
    // ========================================

    @Test
    fun `getStartupTheme returns DEFAULT_HUE_DEG by default`() {
        // When
        val result = repository.getStartupTheme()

        // Then
        assertEquals(ThemeService.DEFAULT_HUE_DEG, result)
    }

    @Test
    fun `setStartupTheme persists value`() {
        // When
        repository.setStartupTheme(180)

        // Then
        assertEquals(180, repository.getStartupTheme())
    }

    @Test
    fun `setStartupTheme can update value`() {
        // Given
        repository.setStartupTheme(90)
        assertEquals(90, repository.getStartupTheme())

        // When
        repository.setStartupTheme(270)

        // Then
        assertEquals(270, repository.getStartupTheme())
    }

    @Test
    fun `setStartupTheme handles zero`() {
        // When
        repository.setStartupTheme(0)

        // Then
        assertEquals(0, repository.getStartupTheme())
    }

    @Test
    fun `setStartupTheme handles boundary value 359`() {
        // When
        repository.setStartupTheme(359)

        // Then
        assertEquals(359, repository.getStartupTheme())
    }

    // ========================================
    // Persistence across instances tests
    // ========================================

    @Test
    fun `values persist across repository instances`() {
        // Given - set values with first instance
        repository.setShowZeroBalanceAccounts(false)
        repository.setStartupProfileId(123L)
        repository.setStartupTheme(45)

        // When - create new instance
        val newRepository = PreferencesRepositoryImpl(context)

        // Then - values should be preserved
        assertFalse(newRepository.getShowZeroBalanceAccounts())
        assertEquals(123L, newRepository.getStartupProfileId())
        assertEquals(45, newRepository.getStartupTheme())
    }
}
