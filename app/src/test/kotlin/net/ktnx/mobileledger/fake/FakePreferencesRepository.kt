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

package net.ktnx.mobileledger.fake

import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository

/**
 * Fake implementation of [PreferencesRepository] for testing.
 *
 * This fake stores preferences in memory, allowing tests to verify
 * ViewModel interactions with preferences without SharedPreferences.
 */
class FakePreferencesRepository : PreferencesRepository {

    private var _showZeroBalanceAccounts = false
    private var _startupProfileId = -1L
    private var _startupTheme = -1

    override fun getShowZeroBalanceAccounts(): Boolean = _showZeroBalanceAccounts

    override fun setShowZeroBalanceAccounts(value: Boolean) {
        _showZeroBalanceAccounts = value
    }

    override fun getStartupProfileId(): Long = _startupProfileId

    override fun setStartupProfileId(profileId: Long) {
        _startupProfileId = profileId
    }

    override fun getStartupTheme(): Int = _startupTheme

    override fun setStartupTheme(theme: Int) {
        _startupTheme = theme
    }

    /**
     * Reset all preferences to default values.
     */
    fun reset() {
        _showZeroBalanceAccounts = false
        _startupProfileId = -1L
        _startupTheme = -1
    }
}
