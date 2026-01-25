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

package net.ktnx.mobileledger.domain.repository

/**
 * Repository for application preferences.
 * Abstracts SharedPreferences access for testability and replaces App static methods.
 */
interface PreferencesRepository {

    /**
     * Get whether to show zero balance accounts in the account list.
     * @return true if zero balance accounts should be shown, false otherwise
     */
    fun getShowZeroBalanceAccounts(): Boolean

    /**
     * Set whether to show zero balance accounts in the account list.
     * @param value true to show zero balance accounts, false to hide them
     */
    fun setShowZeroBalanceAccounts(value: Boolean)

    /**
     * Get the startup profile ID.
     * @return the profile ID to use at startup, or -1 if not set
     */
    fun getStartupProfileId(): Long

    /**
     * Set the startup profile ID.
     * @param profileId the profile ID to use at startup
     */
    fun setStartupProfileId(profileId: Long)

    /**
     * Get the startup theme hue.
     * @return the theme hue to use at startup, or -1 if not set
     */
    fun getStartupTheme(): Int

    /**
     * Set the startup theme hue.
     * @param theme the theme hue to use at startup
     */
    fun setStartupTheme(theme: Int)
}
