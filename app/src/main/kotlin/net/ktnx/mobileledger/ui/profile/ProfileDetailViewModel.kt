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

package net.ktnx.mobileledger.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.TemporaryAuthData
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.domain.model.FutureDates
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.ProfileAuthentication
import net.ktnx.mobileledger.domain.model.ServerVersion
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.model.HledgerVersion
import net.ktnx.mobileledger.service.AuthDataProvider
import net.ktnx.mobileledger.utils.NetworkUtil

@HiltViewModel
class ProfileDetailViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authDataProvider: AuthDataProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @Suppress("unused") private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileDetailUiState())
    val uiState: StateFlow<ProfileDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var originalState: ProfileDetailUiState? = null
    private var orderNo: Int = -1

    companion object {
        const val ARG_PROFILE_ID = "profileId"
        const val ARG_THEME_HUE = "themeHue"
        private const val VERSION_DETECTION_MIN_DURATION = 1000L
        private val VERSION_PATTERN = Pattern.compile("^\"(\\d+)\\.(\\d+)(?:\\.(\\d+))?\"$")
    }

    fun initialize(profileId: Long, initialThemeHue: Int) {
        if (originalState != null) return // Already initialized

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            if (profileId > 0) {
                loadProfile(profileId)
            } else {
                // New profile
                val themeHue = if (initialThemeHue >= 0) {
                    initialThemeHue
                } else {
                    val existingProfiles = profileRepository.getAllProfiles().first()
                    authDataProvider.getNewProfileThemeHue(existingProfiles)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        themeHue = themeHue,
                        initialThemeHue = themeHue
                    )
                }
            }

            originalState = _uiState.value
        }
    }

    private suspend fun loadProfile(profileId: Long) {
        withContext(ioDispatcher) {
            val profile = profileRepository.getProfileByIdSync(profileId)
            if (profile != null) {
                orderNo = profile.orderNo
                val detectedVersion = if (profile.isVersionPre_1_19) {
                    HledgerVersion(true)
                } else if (profile.detectedVersionMajor > 0) {
                    HledgerVersion(profile.detectedVersionMajor, profile.detectedVersionMinor)
                } else {
                    null
                }

                val defaultHue = authDataProvider.getDefaultThemeHue()
                _uiState.update {
                    ProfileDetailUiState(
                        profileId = profile.id ?: 0,
                        name = profile.name,
                        url = profile.url,
                        useAuthentication = profile.isAuthEnabled,
                        authUser = profile.authentication?.user ?: "",
                        authPassword = profile.authentication?.password ?: "",
                        themeHue = if (profile.theme == -1) defaultHue else profile.theme,
                        initialThemeHue = if (profile.theme == -1) defaultHue else profile.theme,
                        preferredAccountsFilter = profile.preferredAccountsFilter ?: "",
                        futureDates = profile.futureDates,
                        apiVersion = API.valueOf(profile.apiVersion),
                        permitPosting = profile.permitPosting,
                        showCommentsByDefault = profile.showCommentsByDefault,
                        showCommodityByDefault = profile.showCommodityByDefault,
                        defaultCommodity = profile.defaultCommodityOrEmpty.ifEmpty { null },
                        detectedVersion = detectedVersion,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onEvent(event: ProfileDetailEvent) {
        when (event) {
            is ProfileDetailEvent.UpdateName -> updateName(event.name)
            is ProfileDetailEvent.UpdateUrl -> updateUrl(event.url)
            is ProfileDetailEvent.UpdateUseAuthentication -> updateUseAuthentication(event.enabled)
            is ProfileDetailEvent.UpdateAuthUser -> updateAuthUser(event.user)
            is ProfileDetailEvent.UpdateAuthPassword -> updateAuthPassword(event.password)
            is ProfileDetailEvent.UpdateThemeHue -> updateThemeHue(event.hue)
            is ProfileDetailEvent.UpdateFutureDates -> updateFutureDates(event.futureDates)
            is ProfileDetailEvent.UpdateApiVersion -> updateApiVersion(event.api)
            is ProfileDetailEvent.UpdatePermitPosting -> updatePermitPosting(event.enabled)
            is ProfileDetailEvent.UpdateShowCommentsByDefault -> updateShowCommentsByDefault(event.enabled)
            is ProfileDetailEvent.UpdateShowCommodityByDefault -> updateShowCommodityByDefault(event.enabled)
            is ProfileDetailEvent.UpdateDefaultCommodity -> updateDefaultCommodity(event.commodity)
            is ProfileDetailEvent.UpdatePreferredAccountsFilter -> updatePreferredAccountsFilter(event.filter)
            ProfileDetailEvent.Save -> saveProfile()
            ProfileDetailEvent.Delete -> showDeleteConfirmDialog()
            ProfileDetailEvent.TestConnection -> testConnection()
            ProfileDetailEvent.NavigateBack -> handleNavigateBack()
            ProfileDetailEvent.ConfirmDiscardChanges -> confirmDiscardChanges()
            ProfileDetailEvent.DismissUnsavedChangesDialog -> dismissUnsavedChangesDialog()
            ProfileDetailEvent.ShowDeleteConfirmDialog -> showDeleteConfirmDialog()
            ProfileDetailEvent.DismissDeleteConfirmDialog -> dismissDeleteConfirmDialog()
            ProfileDetailEvent.ConfirmDelete -> confirmDelete()
            ProfileDetailEvent.ShowHueRingDialog -> showHueRingDialog()
            ProfileDetailEvent.DismissHueRingDialog -> dismissHueRingDialog()
            ProfileDetailEvent.ClearConnectionTestResult -> clearConnectionTestResult()
            ProfileDetailEvent.ClearValidationError -> clearValidationErrors()
        }
    }

    private fun updateName(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                hasUnsavedChanges = true,
                validationErrors = it.validationErrors - ProfileField.NAME
            )
        }
    }

    private fun updateUrl(url: String) {
        _uiState.update {
            it.copy(
                url = url,
                hasUnsavedChanges = true,
                validationErrors = it.validationErrors - ProfileField.URL
            )
        }
    }

    private fun updateUseAuthentication(enabled: Boolean) {
        _uiState.update {
            it.copy(
                useAuthentication = enabled,
                hasUnsavedChanges = true
            )
        }
    }

    private fun updateAuthUser(user: String) {
        _uiState.update {
            it.copy(
                authUser = user,
                hasUnsavedChanges = true,
                validationErrors = it.validationErrors - ProfileField.AUTH_USER
            )
        }
    }

    private fun updateAuthPassword(password: String) {
        _uiState.update {
            it.copy(
                authPassword = password,
                hasUnsavedChanges = true,
                validationErrors = it.validationErrors - ProfileField.AUTH_PASSWORD
            )
        }
    }

    private fun updateThemeHue(hue: Int) {
        _uiState.update {
            it.copy(
                themeHue = hue,
                hasUnsavedChanges = true
            )
        }
    }

    private fun updateFutureDates(futureDates: FutureDates) {
        _uiState.update {
            it.copy(
                futureDates = futureDates,
                hasUnsavedChanges = true
            )
        }
    }

    private fun updateApiVersion(api: API) {
        _uiState.update {
            it.copy(
                apiVersion = api,
                hasUnsavedChanges = true
            )
        }
    }

    private fun updatePermitPosting(enabled: Boolean) {
        _uiState.update {
            it.copy(
                permitPosting = enabled,
                hasUnsavedChanges = true
            )
        }
    }

    private fun updateShowCommentsByDefault(enabled: Boolean) {
        _uiState.update {
            it.copy(
                showCommentsByDefault = enabled,
                hasUnsavedChanges = true
            )
        }
    }

    private fun updateShowCommodityByDefault(enabled: Boolean) {
        _uiState.update {
            it.copy(
                showCommodityByDefault = enabled,
                hasUnsavedChanges = true
            )
        }
    }

    private fun updateDefaultCommodity(commodity: String?) {
        _uiState.update {
            it.copy(
                defaultCommodity = commodity,
                hasUnsavedChanges = true
            )
        }
    }

    private fun updatePreferredAccountsFilter(filter: String) {
        _uiState.update {
            it.copy(
                preferredAccountsFilter = filter,
                hasUnsavedChanges = true
            )
        }
    }

    private fun validateForm(): Boolean {
        val state = _uiState.value
        val errors = mutableMapOf<ProfileField, String>()

        if (state.name.isBlank()) {
            errors[ProfileField.NAME] = "プロファイル名は必須です"
        }

        if (state.url.isBlank()) {
            errors[ProfileField.URL] = "URLは必須です"
        } else {
            try {
                val parsedUrl = URL(state.url)
                val host = parsedUrl.host
                if (host.isNullOrEmpty()) {
                    errors[ProfileField.URL] = "無効なURLです"
                }
                val protocol = parsedUrl.protocol.uppercase()
                if (protocol != "HTTP" && protocol != "HTTPS") {
                    errors[ProfileField.URL] = "無効なURLです"
                }
            } catch (e: MalformedURLException) {
                errors[ProfileField.URL] = "無効なURLです"
            }
        }

        if (state.useAuthentication) {
            if (state.authUser.isBlank()) {
                errors[ProfileField.AUTH_USER] = "ユーザー名は必須です"
            }
            if (state.authPassword.isBlank()) {
                errors[ProfileField.AUTH_PASSWORD] = "パスワードは必須です"
            }
        }

        _uiState.update { it.copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    private fun saveProfile() {
        if (!validateForm()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val state = _uiState.value
                val version = state.detectedVersion
                val serverVersion = if (version != null) {
                    ServerVersion(
                        major = version.major,
                        minor = version.minor,
                        isPre_1_19 = version.isPre_1_20_1
                    )
                } else {
                    null
                }

                val authentication = if (state.useAuthentication) {
                    ProfileAuthentication(user = state.authUser, password = state.authPassword)
                } else {
                    null
                }

                // Load existing profile to get uuid if updating
                val existingProfile = if (state.profileId > 0) {
                    profileRepository.getProfileByIdSync(state.profileId)
                } else {
                    null
                }

                val profile = Profile(
                    id = if (state.profileId > 0) state.profileId else null,
                    name = state.name,
                    uuid = existingProfile?.uuid ?: java.util.UUID.randomUUID().toString(),
                    url = state.url,
                    authentication = authentication,
                    orderNo = this@ProfileDetailViewModel.orderNo,
                    permitPosting = state.permitPosting,
                    theme = state.themeHue,
                    preferredAccountsFilter = state.preferredAccountsFilter.ifEmpty { null },
                    futureDates = state.futureDates,
                    apiVersion = state.apiVersion.toInt(),
                    showCommodityByDefault = state.showCommodityByDefault,
                    defaultCommodity = state.defaultCommodity,
                    showCommentsByDefault = state.showCommentsByDefault,
                    serverVersion = serverVersion
                )

                if (profile.id != null && profile.id > 0) {
                    profileRepository.updateProfile(profile)
                    logcat { "Profile updated in DB" }
                } else {
                    profileRepository.insertProfile(profile)
                    logcat { "Profile inserted in DB" }
                }

                authDataProvider.notifyBackupDataChanged()

                _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                _effects.send(ProfileDetailEffect.ProfileSaved)
                _effects.send(ProfileDetailEffect.NavigateBack)
            } catch (e: Exception) {
                logcat { "Error saving profile: ${e.message}" }
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(ProfileDetailEffect.ShowError("プロファイルの保存に失敗しました"))
            }
        }
    }

    private fun testConnection() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTestingConnection = true,
                    connectionTestResult = null
                )
            }

            val startTime = System.currentTimeMillis()
            val result = withContext(ioDispatcher) {
                detectVersion()
            }

            // Ensure minimum duration for UI feedback
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < VERSION_DETECTION_MIN_DURATION) {
                delay(VERSION_DETECTION_MIN_DURATION - elapsed)
            }

            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    detectedVersion = result,
                    connectionTestResult = if (result != null) {
                        ConnectionTestResult.Success
                    } else {
                        ConnectionTestResult.Error("サーバーへの接続に失敗しました")
                    },
                    hasUnsavedChanges = true
                )
            }
        }
    }

    private fun detectVersion(): HledgerVersion? {
        val state = _uiState.value

        try {
            val http: HttpURLConnection = NetworkUtil.prepareConnection(
                state.url,
                "version",
                state.useAuthentication
            )

            // Set up authentication if needed
            if (state.useAuthentication) {
                // Authentication is handled by the global Authenticator in App
                // We need to temporarily set the profile model data
                setAuthenticationData()
            }

            try {
                when (http.responseCode) {
                    200 -> { /* continue */ }

                    404 -> return HledgerVersion(true)

                    else -> {
                        logcat {
                            "HTTP error detecting hledger-web version: [${http.responseCode}] ${http.responseMessage}"
                        }
                        return null
                    }
                }

                val reader = BufferedReader(InputStreamReader(http.inputStream))
                val version = reader.readLine()
                val m = VERSION_PATTERN.matcher(version)
                if (m.matches()) {
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
            } finally {
                resetAuthenticationData()
            }
        } catch (e: IOException) {
            logcat { "IOException during version detection: ${e.message}" }
            return null
        }
    }

    private fun setAuthenticationData() {
        val state = _uiState.value
        val authData = TemporaryAuthData(
            url = state.url,
            useAuthentication = state.useAuthentication,
            authUser = state.authUser,
            authPassword = state.authPassword
        )
        authDataProvider.setTemporaryAuthData(authData)
    }

    private fun resetAuthenticationData() {
        authDataProvider.resetAuthenticationData()
    }

    private fun handleNavigateBack() {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showUnsavedChangesDialog = true) }
        } else {
            viewModelScope.launch {
                _effects.send(ProfileDetailEffect.NavigateBack)
            }
        }
    }

    private fun confirmDiscardChanges() {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
        viewModelScope.launch {
            _effects.send(ProfileDetailEffect.NavigateBack)
        }
    }

    private fun dismissUnsavedChangesDialog() {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
    }

    private fun showDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    private fun dismissDeleteConfirmDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    private fun confirmDelete() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteConfirmDialog = false, isLoading = true) }

            try {
                val profileId = _uiState.value.profileId
                if (profileId > 0) {
                    val profile = profileRepository.getProfileByIdSync(profileId)
                    if (profile != null) {
                        profileRepository.deleteProfile(profile)
                        // Repository handles order updates internally
                        logcat { "Profile deleted from DB" }
                    }
                }

                _uiState.update { it.copy(isLoading = false) }
                _effects.send(ProfileDetailEffect.ProfileDeleted)
                _effects.send(ProfileDetailEffect.NavigateBack)
            } catch (e: Exception) {
                logcat { "Error deleting profile: ${e.message}" }
                _uiState.update { it.copy(isLoading = false) }
                _effects.send(ProfileDetailEffect.ShowError("プロファイルの削除に失敗しました"))
            }
        }
    }

    private fun showHueRingDialog() {
        _uiState.update { it.copy(showHueRingDialog = true) }
    }

    private fun dismissHueRingDialog() {
        _uiState.update { it.copy(showHueRingDialog = false) }
    }

    private fun clearConnectionTestResult() {
        _uiState.update { it.copy(connectionTestResult = null) }
    }

    private fun clearValidationErrors() {
        _uiState.update { it.copy(validationErrors = emptyMap()) }
    }
}
