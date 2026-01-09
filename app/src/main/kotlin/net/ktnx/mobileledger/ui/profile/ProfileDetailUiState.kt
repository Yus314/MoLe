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

import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.model.FutureDates
import net.ktnx.mobileledger.model.HledgerVersion
import net.ktnx.mobileledger.utils.Colors

data class ProfileDetailUiState(
    val profileId: Long = Profile.NO_PROFILE_ID,
    val name: String = "",
    val url: String = "https://",
    val useAuthentication: Boolean = false,
    val authUser: String = "",
    val authPassword: String = "",
    val themeHue: Int = Colors.DEFAULT_HUE_DEG,
    val initialThemeHue: Int = Colors.DEFAULT_HUE_DEG,
    val preferredAccountsFilter: String = "",
    val futureDates: FutureDates = FutureDates.None,
    val apiVersion: API = API.auto,
    val permitPosting: Boolean = true,
    val showCommentsByDefault: Boolean = true,
    val showCommodityByDefault: Boolean = false,
    val defaultCommodity: String? = null,
    val detectedVersion: HledgerVersion? = null,

    // UI状態
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: ConnectionTestResult? = null,
    val hasUnsavedChanges: Boolean = false,
    val showUnsavedChangesDialog: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val showHueRingDialog: Boolean = false,
    val showApiVersionMenu: Boolean = false,
    val showFutureDatesMenu: Boolean = false,
    val validationErrors: Map<ProfileField, String> = emptyMap()
) {
    val isNewProfile: Boolean
        get() = profileId == Profile.NO_PROFILE_ID

    val canDelete: Boolean
        get() = !isNewProfile

    val isFormValid: Boolean
        get() = validationErrors.isEmpty() &&
            name.isNotBlank() &&
            url.isNotBlank() &&
            (!useAuthentication || (authUser.isNotBlank() && authPassword.isNotBlank()))

    val showInsecureWarning: Boolean
        get() = useAuthentication &&
            (url.startsWith("http://") || (url.length >= 8 && !url.startsWith("https://")))
}

enum class ProfileField {
    NAME,
    URL,
    AUTH_USER,
    AUTH_PASSWORD
}

sealed class ConnectionTestResult {
    data object Success : ConnectionTestResult()
    data class Error(val message: String) : ConnectionTestResult()
}

sealed class ProfileDetailEvent {
    data class UpdateName(val name: String) : ProfileDetailEvent()
    data class UpdateUrl(val url: String) : ProfileDetailEvent()
    data class UpdateUseAuthentication(val enabled: Boolean) : ProfileDetailEvent()
    data class UpdateAuthUser(val user: String) : ProfileDetailEvent()
    data class UpdateAuthPassword(val password: String) : ProfileDetailEvent()
    data class UpdateThemeHue(val hue: Int) : ProfileDetailEvent()
    data class UpdateFutureDates(val futureDates: FutureDates) : ProfileDetailEvent()
    data class UpdateApiVersion(val api: API) : ProfileDetailEvent()
    data class UpdatePermitPosting(val enabled: Boolean) : ProfileDetailEvent()
    data class UpdateShowCommentsByDefault(val enabled: Boolean) : ProfileDetailEvent()
    data class UpdateShowCommodityByDefault(val enabled: Boolean) : ProfileDetailEvent()
    data class UpdateDefaultCommodity(val commodity: String?) : ProfileDetailEvent()
    data class UpdatePreferredAccountsFilter(val filter: String) : ProfileDetailEvent()

    data object Save : ProfileDetailEvent()
    data object Delete : ProfileDetailEvent()
    data object TestConnection : ProfileDetailEvent()
    data object NavigateBack : ProfileDetailEvent()
    data object ConfirmDiscardChanges : ProfileDetailEvent()
    data object DismissUnsavedChangesDialog : ProfileDetailEvent()
    data object ShowDeleteConfirmDialog : ProfileDetailEvent()
    data object DismissDeleteConfirmDialog : ProfileDetailEvent()
    data object ConfirmDelete : ProfileDetailEvent()
    data object ShowHueRingDialog : ProfileDetailEvent()
    data object DismissHueRingDialog : ProfileDetailEvent()
    data object ClearConnectionTestResult : ProfileDetailEvent()
    data object ClearValidationError : ProfileDetailEvent()
}

sealed class ProfileDetailEffect {
    data object NavigateBack : ProfileDetailEffect()
    data class ShowError(val message: String) : ProfileDetailEffect()
    data object ProfileSaved : ProfileDetailEffect()
    data object ProfileDeleted : ProfileDetailEffect()
}
