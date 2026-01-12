/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import java.util.Locale
import net.ktnx.mobileledger.App
import net.ktnx.mobileledger.dao.BaseDAO
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Logger

@SuppressLint("Registered")
open class ProfileThemedActivity : CrashReportingActivity() {
    @JvmField
    protected var mProfile: Profile? = null
    private var themeSetUp = false
    private var mThemeHue = 0

    protected fun setupProfileColors(newHue: Int) {
        if (themeSetUp && newHue == mThemeHue) {
            Logger.debug(
                TAG,
                String.format(
                    Locale.ROOT,
                    "Ignore request to set theme to the same value (%d)",
                    newHue
                )
            )
            return
        }

        Logger.debug(
            TAG,
            String.format(Locale.ROOT, "Changing theme from %d to %d", mThemeHue, newHue)
        )

        mThemeHue = newHue
        Colors.setupTheme(this, mThemeHue)

        if (themeSetUp) {
            Logger.debug(
                TAG,
                "setupProfileColors(): theme already set up, supposedly the activity will be " +
                    "recreated"
            )
            return
        }
        themeSetUp = true

        Colors.profileThemeId = mThemeHue
    }

    override fun onStart() {
        super.onStart()
        Colors.refreshColors(theme)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initProfile()

        Data.observeProfile(this) { profile ->
            if (profile == null) {
                Logger.debug(TAG, "No current profile, leaving")
                return@observeProfile
            }

            mProfile = profile
            storeProfilePref(profile)
            val hue = profile.theme

            if (hue != mThemeHue) {
                Logger.debug(
                    TAG,
                    String.format(
                        Locale.US,
                        "profile observer calling setupProfileColors(%d)",
                        hue
                    )
                )
                setupProfileColors(hue)
            }
        }

        super.onCreate(savedInstanceState)
    }

    fun storeProfilePref(profile: Profile) {
        App.storeStartupProfileAndTheme(profile.id, profile.theme)
    }

    protected open fun initProfile() {
        val profileId = App.getStartupProfile()
        val hue = App.getStartupTheme()
        if (profileId == -1L) {
            mThemeHue = Colors.DEFAULT_HUE_DEG
        }

        Logger.debug(
            TAG,
            String.format(Locale.US, "initProfile() calling setupProfileColors(%d)", hue)
        )
        setupProfileColors(hue)

        initProfile(profileId)
    }

    protected fun initProfile(profileId: Long) {
        BaseDAO.runAsync { initProfileSync(profileId) }
    }

    /**
     * Load profile synchronously on a background thread.
     *
     * TODO: Migrate to ProfileRepository when this base class is refactored
     * to use Hilt injection and coroutines. Currently using DB.get() directly
     * because BaseDAO.runAsync() is not compatible with suspend functions.
     */
    private fun initProfileSync(profileId: Long) {
        Logger.debug(TAG, String.format(Locale.US, "Loading profile %d", profileId))
        val dao = DB.get().getProfileDAO()
        var profile = dao.getByIdSync(profileId)

        if (profile == null) {
            Logger.debug(
                TAG,
                String.format(
                    Locale.ROOT,
                    "Profile %d not found. Trying any other",
                    profileId
                )
            )

            profile = dao.getAnySync()
        }

        if (profile == null) {
            Logger.debug(TAG, "No profile could be loaded")
        } else {
            Logger.debug(TAG, String.format(Locale.ROOT, "Profile %d loaded. posting", profileId))
        }
        Data.postCurrentProfile(profile)
    }

    companion object {
        const val TAG: String = "prf-thm-act"

        @JvmField
        val PARAM_PROFILE_ID: String = "profile-id"

        @JvmField
        val PARAM_THEME: String = "theme"
    }
}
