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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.logcat
import net.ktnx.mobileledger.data.repository.PreferencesRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.di.BackupEntryPoint
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.utils.Colors

@SuppressLint("Registered")
open class ProfileThemedActivity : CrashReportingActivity() {
    @JvmField
    protected var mProfile: Profile? = null
    private var themeSetUp = false
    private var mThemeHue = 0

    // Lazy access to Repositories via EntryPoint (cannot use @Inject in base class)
    protected val profileRepository: ProfileRepository by lazy {
        BackupEntryPoint.get(this).profileRepository()
    }

    protected val preferencesRepository: PreferencesRepository by lazy {
        BackupEntryPoint.get(this).preferencesRepository()
    }

    protected fun setupProfileColors(newHue: Int) {
        if (themeSetUp && newHue == mThemeHue) {
            logcat { "Ignore request to set theme to the same value ($newHue)" }
            return
        }

        logcat { "Changing theme from $mThemeHue to $newHue" }

        mThemeHue = newHue
        Colors.setupTheme(this, mThemeHue)

        if (themeSetUp) {
            logcat { "setupProfileColors(): theme already set up, supposedly the activity will be recreated" }
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

        // Observe profile changes from ProfileRepository
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileRepository.currentProfile.collect { profile ->
                    if (profile == null) {
                        logcat { "No current profile, leaving" }
                        return@collect
                    }

                    mProfile = profile
                    storeProfilePref(profile)
                    val hue = profile.theme

                    if (hue != mThemeHue) {
                        logcat { "profile observer calling setupProfileColors($hue)" }
                        setupProfileColors(hue)
                    }
                }
            }
        }

        super.onCreate(savedInstanceState)
    }

    fun storeProfilePref(profile: Profile) {
        preferencesRepository.setStartupProfileId(profile.id ?: 0)
        preferencesRepository.setStartupTheme(profile.theme)
    }

    protected open fun initProfile() {
        val profileId = preferencesRepository.getStartupProfileId()
        val hue = preferencesRepository.getStartupTheme()
        if (profileId == -1L) {
            mThemeHue = Colors.DEFAULT_HUE_DEG
        }

        logcat { "initProfile() calling setupProfileColors($hue)" }
        setupProfileColors(hue)

        initProfile(profileId)
    }

    protected fun initProfile(profileId: Long) {
        lifecycleScope.launch {
            initProfileAsync(profileId)
        }
    }

    /**
     * Load profile asynchronously using coroutines and Repository pattern.
     *
     * Uses the lazy profileRepository property which is initialized via BackupEntryPoint
     * since this is a base class that cannot use @AndroidEntryPoint directly.
     */
    private suspend fun initProfileAsync(profileId: Long) {
        val profile = withContext(Dispatchers.IO) {
            logcat { "Loading profile $profileId" }

            var loadedProfile = profileRepository.getProfileByIdSync(profileId)

            if (loadedProfile == null) {
                logcat { "Profile $profileId not found. Trying any other" }
                loadedProfile = profileRepository.getAnyProfile()
            }

            if (loadedProfile == null) {
                logcat { "No profile could be loaded" }
            } else {
                logcat { "Profile $profileId loaded. posting" }
            }

            loadedProfile
        }

        // Use ProfileRepository.setCurrentProfile() to update both StateFlow and LiveData
        profileRepository.setCurrentProfile(profile)
    }

    companion object {
        const val TAG: String = "prf-thm-act"
        const val PARAM_PROFILE_ID: String = "profile-id"
        const val PARAM_THEME: String = "theme"
    }
}
