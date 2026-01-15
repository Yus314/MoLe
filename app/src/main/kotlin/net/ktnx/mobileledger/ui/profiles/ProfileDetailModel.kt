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

package net.ktnx.mobileledger.ui.profiles

import android.text.TextUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.regex.Pattern
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.App
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.Profile.Companion.NO_PROFILE_ID
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.model.HledgerVersion
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Misc
import net.ktnx.mobileledger.utils.NetworkUtil

class ProfileDetailModel : ViewModel() {
    private val profileName = MutableLiveData<String?>()
    private val orderNo = MutableLiveData<Int>()
    private val postingPermitted = MutableLiveData(true)
    private val defaultCommodity = MutableLiveData<String?>(null)
    private val futureDates = MutableLiveData(FutureDates.None)
    private val showCommodityByDefault = MutableLiveData(false)
    private val showCommentsByDefault = MutableLiveData(true)
    private val useAuthentication = MutableLiveData(false)
    private val apiVersion = MutableLiveData(API.auto)
    private val url = MutableLiveData<String?>(null)
    private val authUserName = MutableLiveData<String?>(null)
    private val authPassword = MutableLiveData<String?>(null)
    private val preferredAccountsFilter = MutableLiveData<String?>(null)
    private val themeId = MutableLiveData(-1)
    private val detectedVersion = MutableLiveData<HledgerVersion?>(null)
    private val detectingHledgerVersion = MutableLiveData(false)
    private val profileId = MutableLiveData(NO_PROFILE_ID)

    @JvmField
    var initialThemeHue = Colors.DEFAULT_HUE_DEG

    // Job for managing version detection cancellation
    private var versionDetectionJob: Job? = null

    private val versionPattern = Pattern.compile("^\"(\\d+)\\.(\\d+)(?:\\.(\\d+))?\"$")

    fun getProfileName(): String? = profileName.value

    fun setProfileName(newValue: String?) {
        if (Misc.nullIsEmpty(newValue) != Misc.nullIsEmpty(profileName.value)) {
            profileName.value = newValue
        }
    }

    fun setProfileName(newValue: CharSequence?) {
        setProfileName(newValue?.toString())
    }

    fun observeProfileName(lfo: LifecycleOwner, o: Observer<String?>) {
        profileName.observe(lfo, o)
    }

    fun getPostingPermitted(): Boolean? = postingPermitted.value

    fun setPostingPermitted(newValue: Boolean) {
        if (newValue != postingPermitted.value) {
            postingPermitted.value = newValue
        }
    }

    fun observePostingPermitted(lfo: LifecycleOwner, o: Observer<Boolean>) {
        postingPermitted.observe(lfo, o)
    }

    fun setShowCommentsByDefault(newValue: Boolean) {
        if (newValue != showCommentsByDefault.value) {
            showCommentsByDefault.value = newValue
        }
    }

    fun observeShowCommentsByDefault(lfo: LifecycleOwner, o: Observer<Boolean>) {
        showCommentsByDefault.observe(lfo, o)
    }

    fun getFutureDates(): FutureDates? = futureDates.value

    fun setFutureDates(newValue: FutureDates) {
        if (newValue != futureDates.value) {
            futureDates.value = newValue
        }
    }

    fun observeFutureDates(lfo: LifecycleOwner, o: Observer<FutureDates>) {
        futureDates.observe(lfo, o)
    }

    fun getDefaultCommodity(): String? = defaultCommodity.value

    fun setDefaultCommodity(newValue: String?) {
        if (!Misc.equalStrings(newValue, defaultCommodity.value)) {
            defaultCommodity.value = newValue
        }
    }

    fun observeDefaultCommodity(lfo: LifecycleOwner, o: Observer<String?>) {
        defaultCommodity.observe(lfo, o)
    }

    fun getShowCommodityByDefault(): Boolean? = showCommodityByDefault.value

    fun setShowCommodityByDefault(newValue: Boolean) {
        if (newValue != showCommodityByDefault.value) {
            showCommodityByDefault.value = newValue
        }
    }

    fun observeShowCommodityByDefault(lfo: LifecycleOwner, o: Observer<Boolean>) {
        showCommodityByDefault.observe(lfo, o)
    }

    fun getUseAuthentication(): Boolean = useAuthentication.value ?: false

    fun setUseAuthentication(newValue: Boolean) {
        if (newValue != useAuthentication.value) {
            useAuthentication.value = newValue
        }
    }

    fun observeUseAuthentication(lfo: LifecycleOwner, o: Observer<Boolean>) {
        useAuthentication.observe(lfo, o)
    }

    fun getApiVersion(): API? = apiVersion.value

    fun setApiVersion(newValue: API) {
        if (newValue != apiVersion.value) {
            apiVersion.value = newValue
        }
    }

    fun observeApiVersion(lfo: LifecycleOwner, o: Observer<API>) {
        apiVersion.observe(lfo, o)
    }

    fun getDetectedVersion(): HledgerVersion? = detectedVersion.value

    fun setDetectedVersion(newValue: HledgerVersion?) {
        if (detectedVersion.value != newValue) {
            detectedVersion.value = newValue
        }
    }

    fun observeDetectedVersion(lfo: LifecycleOwner, o: Observer<HledgerVersion?>) {
        detectedVersion.observe(lfo, o)
    }

    fun getUrl(): String = url.value ?: ""

    fun setUrl(newValue: String?) {
        if (Misc.nullIsEmpty(newValue) != Misc.nullIsEmpty(url.value)) {
            url.value = newValue
        }
    }

    fun setUrl(newValue: CharSequence?) {
        setUrl(newValue?.toString())
    }

    fun observeUrl(lfo: LifecycleOwner, o: Observer<String?>) {
        url.observe(lfo, o)
    }

    fun getAuthUserName(): String? = authUserName.value

    fun setAuthUserName(newValue: String?) {
        if (Misc.nullIsEmpty(newValue) != Misc.nullIsEmpty(authUserName.value)) {
            authUserName.value = newValue
        }
    }

    fun setAuthUserName(newValue: CharSequence?) {
        setAuthUserName(newValue?.toString())
    }

    fun observeUserName(lfo: LifecycleOwner, o: Observer<String?>) {
        authUserName.observe(lfo, o)
    }

    fun getAuthPassword(): String? = authPassword.value

    fun setAuthPassword(newValue: String?) {
        if (Misc.nullIsEmpty(newValue) != Misc.nullIsEmpty(authPassword.value)) {
            authPassword.value = newValue
        }
    }

    fun setAuthPassword(newValue: CharSequence?) {
        setAuthPassword(newValue?.toString())
    }

    fun observePassword(lfo: LifecycleOwner, o: Observer<String?>) {
        authPassword.observe(lfo, o)
    }

    fun getPreferredAccountsFilter(): String? = preferredAccountsFilter.value

    fun setPreferredAccountsFilter(newValue: String?) {
        if (Misc.nullIsEmpty(newValue) != Misc.nullIsEmpty(preferredAccountsFilter.value)) {
            preferredAccountsFilter.value = newValue
        }
    }

    fun setPreferredAccountsFilter(newValue: CharSequence?) {
        setPreferredAccountsFilter(newValue?.toString())
    }

    fun observePreferredAccountsFilter(lfo: LifecycleOwner, o: Observer<String?>) {
        preferredAccountsFilter.observe(lfo, o)
    }

    fun getThemeId(): Int = themeId.value ?: -1

    fun setThemeId(newValue: Int) {
        themeId.value = newValue
    }

    fun observeThemeId(lfo: LifecycleOwner, o: Observer<Int>) {
        themeId.observe(lfo, o)
    }

    fun observeDetectingHledgerVersion(lfo: LifecycleOwner, o: Observer<Boolean>) {
        detectingHledgerVersion.observe(lfo, o)
    }

    fun setValuesFromProfile(mProfile: Profile?) {
        if (mProfile != null) {
            profileId.value = mProfile.id
            profileName.value = mProfile.name
            orderNo.value = mProfile.orderNo
            postingPermitted.value = mProfile.canPost()
            showCommentsByDefault.value = mProfile.showCommentsByDefault
            showCommodityByDefault.value = mProfile.showCommodityByDefault
            val comm = mProfile.getDefaultCommodityOrEmpty()
            if (TextUtils.isEmpty(comm)) {
                setDefaultCommodity(null)
            } else {
                setDefaultCommodity(comm)
            }
            futureDates.value = FutureDates.valueOf(mProfile.futureDates)
            apiVersion.value = API.valueOf(mProfile.apiVersion)
            url.value = mProfile.url
            useAuthentication.value = mProfile.isAuthEnabled()
            authUserName.value = if (mProfile.isAuthEnabled()) mProfile.authUser else ""
            authPassword.value = if (mProfile.isAuthEnabled()) mProfile.authPassword else ""
            preferredAccountsFilter.value = mProfile.preferredAccountsFilter
            themeId.value = mProfile.theme
            detectedVersion.value = if (mProfile.isVersionPre_1_19()) {
                HledgerVersion(true)
            } else {
                HledgerVersion(mProfile.detectedVersionMajor, mProfile.detectedVersionMinor)
            }
        } else {
            profileId.value = NO_PROFILE_ID
            orderNo.value = -1
            profileName.value = null
            url.value = HTTPS_URL_START
            postingPermitted.value = true
            showCommentsByDefault.value = true
            showCommodityByDefault.value = false
            setFutureDates(FutureDates.None)
            setApiVersion(API.auto)
            useAuthentication.value = false
            authUserName.value = ""
            authPassword.value = ""
            preferredAccountsFilter.value = null
            detectedVersion.value = null
        }
    }

    fun updateProfile(mProfile: Profile) {
        mProfile.id = profileId.value ?: NO_PROFILE_ID
        mProfile.name = profileName.value ?: ""
        mProfile.orderNo = orderNo.value ?: -1
        mProfile.url = url.value ?: ""
        mProfile.permitPosting = postingPermitted.value ?: true
        mProfile.showCommentsByDefault = showCommentsByDefault.value ?: true
        mProfile.setDefaultCommodity(defaultCommodity.value)
        mProfile.showCommodityByDefault = showCommodityByDefault.value ?: false
        mProfile.preferredAccountsFilter = preferredAccountsFilter.value
        mProfile.useAuthentication = useAuthentication.value ?: false
        mProfile.authUser = authUserName.value
        mProfile.authPassword = authPassword.value
        mProfile.theme = themeId.value ?: -1
        mProfile.futureDates = futureDates.value?.toInt() ?: 0
        mProfile.apiVersion = apiVersion.value?.toInt() ?: 0
        val version = detectedVersion.value
        mProfile.detectedVersionPre_1_19 = version != null && version.isPre_1_20_1
        mProfile.detectedVersionMajor = version?.major ?: -1
        mProfile.detectedVersionMinor = version?.minor ?: -1
    }

    /**
     * Trigger hledger-web version detection.
     *
     * Uses viewModelScope.launch instead of Thread for proper lifecycle management
     * and deterministic testing with TestDispatcher.
     */
    fun triggerVersionDetection() {
        // Cancel any previously running detection job
        versionDetectionJob?.cancel()

        versionDetectionJob = viewModelScope.launch {
            detectingHledgerVersion.value = true
            try {
                val startTime = System.currentTimeMillis()

                App.setAuthenticationDataFromProfileModel(this@ProfileDetailModel)
                val version = detectVersion()
                App.resetAuthenticationData()

                val elapsed = System.currentTimeMillis() - startTime
                logcat { "Detection duration $elapsed" }

                // Ensure minimum UI feedback time using delay() instead of Thread.sleep()
                if (elapsed < TARGET_PROCESS_DURATION) {
                    delay(TARGET_PROCESS_DURATION - elapsed)
                }

                detectedVersion.value = version
            } finally {
                detectingHledgerVersion.value = false
            }
        }
    }

    private fun detectVersion(): HledgerVersion? {
        try {
            val http = NetworkUtil.prepareConnection(
                getUrl(),
                "version",
                getUseAuthentication()
            )
            when (http.responseCode) {
                200 -> { /* continue */ }

                404 -> return HledgerVersion(true)

                else -> {
                    logcat(LogPriority.WARN) {
                        "HTTP error detecting hledger-web version: [${http.responseCode}] ${http.responseMessage}"
                    }
                    return null
                }
            }
            val stream = http.inputStream
            val reader = BufferedReader(InputStreamReader(stream))
            val version = reader.readLine()
            val m = versionPattern.matcher(version)
            if (m.matches()) {
                // Groups are guaranteed to exist when matches() returns true
                val major = m.group(1)?.toIntOrNull() ?: return null
                val minor = m.group(2)?.toIntOrNull() ?: return null
                val patchText = m.group(3)
                val hasPatch = patchText != null
                val patch = if (hasPatch) patchText?.toIntOrNull() ?: 0 else 0

                return if (hasPatch) {
                    HledgerVersion(major, minor, patch)
                } else {
                    HledgerVersion(major, minor)
                }
            } else {
                logcat { "Unrecognised version string '$version'" }
                return null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun getProfileId(): LiveData<Long> = profileId

    companion object {
        private const val HTTPS_URL_START = "https://"
        private const val TARGET_PROCESS_DURATION = 1000L
    }
}
