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
import java.util.Locale
import javax.inject.Inject
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import logcat.logcat
import net.ktnx.mobileledger.domain.repository.ProfileRepository
import net.ktnx.mobileledger.di.CurrencyFormatterEntryPoint
import net.ktnx.mobileledger.di.ThemeServiceEntryPoint
import net.ktnx.mobileledger.service.CurrencyFormatter

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

    override fun onCreate() {
        instance = this
        super.onCreate()

        // Initialize logcat for logging (only in debug builds)
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

        logcat { "App onCreate()" }
        currencyFormatter.refresh(Locale.getDefault())

        // Initialize CurrencyFormatterEntryPoint for static access in JSON parsers
        CurrencyFormatterEntryPoint.initialize(this)

        // Initialize ThemeServiceEntryPoint for static access in Views (HueRing)
        ThemeServiceEntryPoint.initialize(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
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
    }
}
