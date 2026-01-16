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

package net.ktnx.mobileledger.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.ktnx.mobileledger.domain.model.Profile

/**
 * Repository interface for Profile data access and management.
 *
 * This repository provides:
 * - Reactive access to all profiles via Flow
 * - Current profile selection state management
 * - CRUD operations for profiles
 *
 * ## Migration Note (008-data-layer-repository)
 *
 * This repository replaces the following from AppStateManager:
 * - `profiles` -> [getAllProfiles]
 * - `profile` -> [currentProfile]
 * - `getProfile()` -> [currentProfile].value
 * - `setCurrentProfile()` -> [setCurrentProfile]
 *
 * ## Usage
 *
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val profileRepository: ProfileRepository
 * ) : ViewModel() {
 *     val profiles = profileRepository.getAllProfiles()
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
 *
 *     val currentProfile = profileRepository.currentProfile
 *
 *     fun selectProfile(profile: Profile) {
 *         profileRepository.setCurrentProfile(profile)
 *     }
 * }
 * ```
 */
interface ProfileRepository {

    // ========================================
    // Current Profile State
    // ========================================

    /**
     * The currently selected profile as a StateFlow.
     *
     * This is a hot flow that always holds the current profile value
     * and emits updates when the profile changes.
     *
     * @return StateFlow of the current profile, or null if no profile is selected
     */
    val currentProfile: StateFlow<Profile?>

    /**
     * Set the current profile.
     *
     * This updates [currentProfile] and notifies all observers.
     *
     * @param profile The profile to set as current, or null to clear selection
     */
    fun setCurrentProfile(profile: Profile?)

    // ========================================
    // Query Operations
    // ========================================

    /**
     * Get all profiles ordered by their display order.
     *
     * @return Flow that emits the complete profile list whenever it changes
     */
    fun getAllProfiles(): Flow<List<Profile>>

    /**
     * Get all profiles ordered by their display order synchronously.
     *
     * @return The complete profile list
     */
    suspend fun getAllProfilesSync(): List<Profile>

    /**
     * Get a profile by its ID.
     *
     * @param profileId The profile ID
     * @return Flow that emits the profile when it changes, or null if not found
     */
    fun getProfileById(profileId: Long): Flow<Profile?>

    /**
     * Get a profile by its ID synchronously.
     *
     * @param profileId The profile ID
     * @return The profile or null if not found
     */
    suspend fun getProfileByIdSync(profileId: Long): Profile?

    /**
     * Get a profile by its UUID.
     *
     * @param uuid The profile UUID
     * @return Flow that emits the profile when it changes, or null if not found
     */
    fun getProfileByUuid(uuid: String): Flow<Profile?>

    /**
     * Get a profile by its UUID synchronously.
     *
     * @param uuid The profile UUID
     * @return The profile or null if not found
     */
    suspend fun getProfileByUuidSync(uuid: String): Profile?

    /**
     * Get any available profile (useful for fallback/initialization).
     *
     * @return The first available profile or null if none exist
     */
    suspend fun getAnyProfile(): Profile?

    /**
     * Get the total number of profiles.
     *
     * @return The profile count
     */
    suspend fun getProfileCount(): Int

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert a new profile at the end of the list.
     *
     * The profile's orderNo will be set automatically.
     *
     * @param profile The profile to insert
     * @return The ID of the inserted profile
     */
    suspend fun insertProfile(profile: Profile): Long

    /**
     * Update an existing profile.
     *
     * If this profile is the current profile, observers will be notified.
     *
     * @param profile The profile to update
     */
    suspend fun updateProfile(profile: Profile)

    /**
     * Delete a profile.
     *
     * If the deleted profile is the current profile, currentProfile will be
     * set to another available profile or null if no profiles remain.
     *
     * @param profile The profile to delete
     */
    suspend fun deleteProfile(profile: Profile)

    /**
     * Update the order of profiles.
     *
     * @param profiles The list of profiles in their new order
     */
    suspend fun updateProfileOrder(profiles: List<Profile>)

    /**
     * Delete all profiles.
     */
    suspend fun deleteAllProfiles()
}
