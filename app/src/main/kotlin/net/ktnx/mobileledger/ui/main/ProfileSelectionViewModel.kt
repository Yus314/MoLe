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

package net.ktnx.mobileledger.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.domain.usecase.GetAllProfilesUseCase
import net.ktnx.mobileledger.domain.usecase.GetProfileByIdUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveCurrentProfileUseCase
import net.ktnx.mobileledger.domain.usecase.ObserveProfilesUseCase
import net.ktnx.mobileledger.domain.usecase.SetCurrentProfileUseCase
import net.ktnx.mobileledger.domain.usecase.UpdateProfileOrderUseCase

/**
 * ViewModel for profile selection in the navigation drawer.
 *
 * Manages:
 * - Current profile state
 * - List of available profiles
 * - Profile selection
 * - Profile reordering
 *
 * This ViewModel is part of the MainViewModel refactoring (010-refactor-mainviewmodel).
 * It handles profile-related state that was previously in MainViewModel.
 *
 * Target size: ~120 lines
 */
@HiltViewModel
class ProfileSelectionViewModel @Inject constructor(
    private val observeProfilesUseCase: ObserveProfilesUseCase,
    private val observeCurrentProfileUseCase: ObserveCurrentProfileUseCase,
    private val getProfileByIdUseCase: GetProfileByIdUseCase,
    private val getAllProfilesUseCase: GetAllProfilesUseCase,
    private val setCurrentProfileUseCase: SetCurrentProfileUseCase,
    private val updateProfileOrderUseCase: UpdateProfileOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSelectionUiState())
    val uiState: StateFlow<ProfileSelectionUiState> = _uiState.asStateFlow()

    /**
     * Delegate to ProfileRepository.currentProfile for shared access.
     */
    val currentProfile: StateFlow<Profile?> = observeCurrentProfileUseCase()

    init {
        observeCurrentProfile()
        observeProfiles()
    }

    /**
     * Handle profile selection events.
     */
    fun onEvent(event: ProfileSelectionEvent) {
        when (event) {
            is ProfileSelectionEvent.SelectProfile -> selectProfile(event.profileId)
            is ProfileSelectionEvent.ReorderProfiles -> reorderProfiles(event.orderedProfiles)
        }
    }

    // ========================================
    // Profile Selection
    // ========================================

    private fun selectProfile(profileId: Long) {
        viewModelScope.launch {
            val profile = getProfileByIdUseCase(profileId).getOrNull()
            if (profile != null) {
                setCurrentProfileUseCase(profile)
            }
        }
    }

    // ========================================
    // Profile Reordering
    // ========================================

    private fun reorderProfiles(orderedProfiles: List<ProfileListItem>) {
        viewModelScope.launch {
            val allProfiles = getAllProfilesUseCase().getOrElse { return@launch }
            val reorderedProfiles = orderedProfiles.mapNotNull { item ->
                allProfiles.find { it.id == item.id }
            }
            updateProfileOrderUseCase(reorderedProfiles)
        }
    }

    // ========================================
    // Observation
    // ========================================

    private fun observeCurrentProfile() {
        viewModelScope.launch {
            observeCurrentProfileUseCase().collect { profile ->
                _uiState.update { state ->
                    state.copy(
                        currentProfileId = profile?.id,
                        currentProfileName = profile?.name ?: "",
                        currentProfileTheme = profile?.theme ?: -1,
                        currentProfileCanPost = profile?.permitPosting == true
                    )
                }
            }
        }
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            observeProfilesUseCase().collect { profiles ->
                _uiState.update { state ->
                    state.copy(
                        profiles = profiles.mapNotNull { profile ->
                            val profileId = profile.id ?: return@mapNotNull null
                            ProfileListItem(
                                id = profileId,
                                name = profile.name,
                                theme = profile.theme,
                                canPost = profile.permitPosting
                            )
                        }
                    )
                }
            }
        }
    }
}
