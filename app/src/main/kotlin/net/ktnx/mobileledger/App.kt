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
import android.content.res.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.net.Authenticator
import java.net.MalformedURLException
import java.net.PasswordAuthentication
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.di.CurrencyFormatterEntryPoint
import net.ktnx.mobileledger.service.AuthDataProvider
import net.ktnx.mobileledger.service.CurrencyFormatter
import net.ktnx.mobileledger.utils.Globals

/**
 * Simple data holder for temporary authentication credentials.
 * Used during profile editing when testing connection to a server.
 */
data class TemporaryAuthData(
    val url: String,
    val useAuthentication: Boolean,
    val authUser: String,
    val authPassword: String
)

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var authDataProvider: AuthDataProvider

    private var monthNamesPrepared = false

    private fun getAuthURL(): String = authDataProvider.getTemporaryAuthData()?.url
        ?: profileRepository.currentProfile.value?.url ?: ""

    private fun getAuthUserName(): String = authDataProvider.getTemporaryAuthData()?.authUser
        ?: profileRepository.currentProfile.value?.authentication?.user ?: ""

    private fun getAuthPassword(): String = authDataProvider.getTemporaryAuthData()?.authPassword
        ?: profileRepository.currentProfile.value?.authentication?.password ?: ""

    private fun getAuthEnabled(): Boolean = authDataProvider.getTemporaryAuthData()?.useAuthentication
        ?: profileRepository.currentProfile.value?.isAuthEnabled ?: false

    override fun onCreate() {
        instance = this
        super.onCreate()

        // Initialize logcat for logging (only in debug builds)
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

        logcat { "App onCreate()" }
        currencyFormatter.refresh(Locale.getDefault())

        // Initialize CurrencyFormatterEntryPoint for static access in JSON parsers
        CurrencyFormatterEntryPoint.initialize(this)

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
                            logcat(LogPriority.WARN) {
                                "Requesting host [$requestingHost] differs from expected [$expectedHost]"
                            }
                        }
                    } catch (e: MalformedURLException) {
                        logcat { "Malformed URL for authentication: ${e.asLog()}" }
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
        // Constants for SharedPreferences (used by PreferencesRepositoryImpl)
        const val PREF_NAME = "MoLe"
        const val PREF_THEME_HUE = "theme-hue"
        const val PREF_PROFILE_ID = "profile-id"
        const val PREF_SHOW_ZERO_BALANCE_ACCOUNTS = "show-zero-balance-accounts"

        /**
         * Application instance for legacy code access.
         * @deprecated Use Hilt dependency injection instead
         */
        @JvmStatic
        lateinit var instance: App

        /**
         * Get the ProfileRepository instance for static access.
         * @deprecated Use Hilt injection with @Inject instead. This remains only for
         * LedgerTransaction compatibility which will be removed in a future refactor.
         */
        @Deprecated("Use Hilt @Inject instead")
        @JvmStatic
        fun profileRepository(): ProfileRepository = instance.profileRepository
    }
}
