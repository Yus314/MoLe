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

package net.ktnx.mobileledger.core.data.repository.impl

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository

/**
 * SharedPreferences-based implementation of PreferencesRepository.
 * Replaces the static methods in App.kt for preference access.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    override fun getShowZeroBalanceAccounts(): Boolean =
        sharedPreferences.getBoolean(KEY_SHOW_ZERO_BALANCE_ACCOUNTS, true)

    override fun setShowZeroBalanceAccounts(value: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SHOW_ZERO_BALANCE_ACCOUNTS, value)
            .apply()
    }

    override fun getStartupProfileId(): Long = sharedPreferences.getLong(KEY_PROFILE_ID, -1L)

    override fun setStartupProfileId(profileId: Long) {
        sharedPreferences.edit()
            .putLong(KEY_PROFILE_ID, profileId)
            .apply()
    }

    override fun getStartupTheme(): Int = sharedPreferences.getInt(KEY_THEME_HUE, PreferencesRepository.DEFAULT_HUE_DEG)

    override fun setStartupTheme(theme: Int) {
        sharedPreferences.edit()
            .putInt(KEY_THEME_HUE, theme)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "MoLe"
        private const val KEY_SHOW_ZERO_BALANCE_ACCOUNTS = "show-zero-balance-accounts"
        private const val KEY_PROFILE_ID = "profile-id"
        private const val KEY_THEME_HUE = "theme-hue"
    }
}
