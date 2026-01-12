/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.net.Authenticator
import java.net.MalformedURLException
import java.net.PasswordAuthentication
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.ui.profiles.ProfileDetailModel
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Globals
import net.ktnx.mobileledger.utils.Logger

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var profileRepository: ProfileRepository

    private var monthNamesPrepared = false

    private fun getAuthURL(): String = profileModel?.getUrl()
        ?: profileRepository.currentProfile.value?.url ?: ""

    private fun getAuthUserName(): String =
        profileModel?.getAuthUserName() ?: profileRepository.currentProfile.value?.authUser ?: ""

    private fun getAuthPassword(): String =
        profileModel?.getAuthPassword() ?: profileRepository.currentProfile.value?.authPassword ?: ""

    private fun getAuthEnabled(): Boolean =
        profileModel?.getUseAuthentication() ?: profileRepository.currentProfile.value?.isAuthEnabled() ?: false

    override fun onCreate() {
        Logger.debug("flow", "App onCreate()")
        instance = this
        super.onCreate()
        currencyFormatter.refresh(Locale.getDefault())
        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? {
                if (getAuthEnabled()) {
                    try {
                        val url = URL(getAuthURL())
                        val requestingHost = getRequestingHost()
                        val expectedHost = url.host
                        if (requestingHost.equals(expectedHost, ignoreCase = true)) {
                            return PasswordAuthentication(
                                getAuthUserName(),
                                getAuthPassword().toCharArray()
                            )
                        } else {
                            Log.w(
                                "http-auth",
                                String.format(
                                    Locale.ROOT,
                                    "Requesting host [%s] differs from expected [%s]",
                                    requestingHost,
                                    expectedHost
                                )
                            )
                        }
                    } catch (e: MalformedURLException) {
                        Logger.debug("http-auth", "Malformed URL for authentication", e)
                    }
                }

                return super.getPasswordAuthentication()
            }
        })
    }

    private fun prepareMonthNamesInternal(force: Boolean) {
        if (!force && monthNamesPrepared) return
        val rm = resources
        Globals.monthNames = rm.getStringArray(R.array.month_names)
        monthNamesPrepared = true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        prepareMonthNamesInternal(true)
        currencyFormatter.refresh(Locale.getDefault())
    }

    companion object {
        const val PREF_NAME = "MoLe"
        const val PREF_THEME_HUE = "theme-hue"
        const val PREF_PROFILE_ID = "profile-id"
        const val PREF_SHOW_ZERO_BALANCE_ACCOUNTS = "show-zero-balance-accounts"

        @JvmStatic
        lateinit var instance: App

        private var profileModel: ProfileDetailModel? = null

        @JvmStatic
        fun prepareMonthNames() {
            instance.prepareMonthNamesInternal(false)
        }

        @JvmStatic
        fun setAuthenticationDataFromProfileModel(model: ProfileDetailModel?) {
            profileModel = model
        }

        @JvmStatic
        fun resetAuthenticationData() {
            profileModel = null
        }

        @JvmStatic
        fun storeStartupProfileAndTheme(currentProfileId: Long, currentTheme: Int) {
            val prefs = instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(PREF_PROFILE_ID, currentProfileId)
                .putInt(PREF_THEME_HUE, currentTheme)
                .apply()
        }

        @JvmStatic
        fun getStartupProfile(): Long {
            val prefs = instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(PREF_PROFILE_ID, -1)
        }

        @JvmStatic
        fun getStartupTheme(): Int {
            val prefs = instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(PREF_THEME_HUE, Colors.DEFAULT_HUE_DEG)
        }

        @JvmStatic
        fun getShowZeroBalanceAccounts(): Boolean {
            val prefs = instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(PREF_SHOW_ZERO_BALANCE_ACCOUNTS, true)
        }

        @JvmStatic
        fun storeShowZeroBalanceAccounts(value: Boolean) {
            val prefs = instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(PREF_SHOW_ZERO_BALANCE_ACCOUNTS, value)
                .apply()
        }

        /**
         * Get the CurrencyFormatter instance for static access.
         * Use this only when Hilt injection is not available.
         */
        @JvmStatic
        fun currencyFormatter(): CurrencyFormatter = instance.currencyFormatter

        /**
         * Get the ProfileRepository instance for static access.
         * Use this only when Hilt injection is not available.
         */
        @JvmStatic
        fun profileRepository(): ProfileRepository = instance.profileRepository
    }
}
