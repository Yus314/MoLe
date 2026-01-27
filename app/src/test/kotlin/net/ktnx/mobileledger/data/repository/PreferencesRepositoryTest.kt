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

import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PreferencesRepository.
 *
 * Tests the interface contract using FakePreferencesRepository.
 * The actual SharedPreferences implementation is tested via instrumentation tests.
 */
class PreferencesRepositoryTest {

    private lateinit var repository: FakePreferencesRepository

    @Before
    fun setup() {
        repository = FakePreferencesRepository()
    }

    @Test
    fun `getShowZeroBalanceAccounts defaults to true`() {
        assertTrue(repository.getShowZeroBalanceAccounts())
    }

    @Test
    fun `setShowZeroBalanceAccounts persists false`() {
        repository.setShowZeroBalanceAccounts(false)
        assertFalse(repository.getShowZeroBalanceAccounts())
    }

    @Test
    fun `setShowZeroBalanceAccounts persists true`() {
        repository.setShowZeroBalanceAccounts(false)
        assertFalse(repository.getShowZeroBalanceAccounts())

        repository.setShowZeroBalanceAccounts(true)
        assertTrue(repository.getShowZeroBalanceAccounts())
    }

    @Test
    fun `reset restores default value`() {
        repository.setShowZeroBalanceAccounts(false)
        assertFalse(repository.getShowZeroBalanceAccounts())

        repository.reset()
        assertTrue(repository.getShowZeroBalanceAccounts())
    }

    @Test
    fun `multiple set calls preserve last value`() {
        repository.setShowZeroBalanceAccounts(false)
        repository.setShowZeroBalanceAccounts(true)
        repository.setShowZeroBalanceAccounts(false)

        assertFalse(repository.getShowZeroBalanceAccounts())
    }

    @Test
    fun `getStartupProfileId defaults to -1`() {
        assertEquals(-1L, repository.getStartupProfileId())
    }

    @Test
    fun `setStartupProfileId persists value`() {
        repository.setStartupProfileId(42L)
        assertEquals(42L, repository.getStartupProfileId())
    }

    @Test
    fun `getStartupTheme defaults to -1`() {
        assertEquals(-1, repository.getStartupTheme())
    }

    @Test
    fun `setStartupTheme persists value`() {
        repository.setStartupTheme(180)
        assertEquals(180, repository.getStartupTheme())
    }
}

/**
 * Fake PreferencesRepository for testing.
 * Provides in-memory storage for unit tests.
 */
class FakePreferencesRepository : PreferencesRepository {
    private var showZeroBalanceAccounts: Boolean = true
    private var startupProfileId: Long = -1L
    private var startupTheme: Int = -1

    override fun getShowZeroBalanceAccounts(): Boolean = showZeroBalanceAccounts

    override fun setShowZeroBalanceAccounts(value: Boolean) {
        showZeroBalanceAccounts = value
    }

    override fun getStartupProfileId(): Long = startupProfileId

    override fun setStartupProfileId(profileId: Long) {
        startupProfileId = profileId
    }

    override fun getStartupTheme(): Int = startupTheme

    override fun setStartupTheme(theme: Int) {
        startupTheme = theme
    }

    fun reset() {
        showZeroBalanceAccounts = true
        startupProfileId = -1L
        startupTheme = -1
    }
}
