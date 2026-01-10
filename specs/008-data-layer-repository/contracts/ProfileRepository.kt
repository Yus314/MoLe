/*
 * Repository Interface Contract: ProfileRepository
 * Feature: 008-data-layer-repository
 *
 * This file defines the contract for ProfileRepository.
 * Implementation will be in ProfileRepositoryImpl.
 */

package net.ktnx.mobileledger.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.ktnx.mobileledger.db.Profile

/**
 * Repository for managing Profile data and current profile selection state.
 *
 * This repository provides:
 * - CRUD operations for profiles
 * - Current profile selection state management (app-wide singleton)
 * - Reactive data streams via Flow
 *
 * Thread-safety: All suspend functions are safe to call from any coroutine context.
 * The currentProfile StateFlow is thread-safe for concurrent access.
 */
interface ProfileRepository {

    // ========================================
    // Current Profile State
    // ========================================

    /**
     * The currently selected profile.
     * This is an app-wide singleton state shared across all ViewModels.
     *
     * Observers should collect this flow to react to profile changes.
     */
    val currentProfile: StateFlow<Profile?>

    /**
     * Sets the current profile.
     * This will emit to all observers of [currentProfile].
     *
     * @param profile The profile to set as current, or null to clear selection.
     */
    fun setCurrentProfile(profile: Profile?)

    // ========================================
    // Query Operations
    // ========================================

    /**
     * Get all profiles ordered by orderNo.
     *
     * @return Flow emitting the list of profiles whenever data changes.
     */
    fun getAllProfiles(): Flow<List<Profile>>

    /**
     * Get a profile by its ID.
     *
     * @param id The profile ID.
     * @return The profile, or null if not found.
     */
    suspend fun getProfileById(id: Long): Profile?

    /**
     * Get a profile by its UUID.
     *
     * @param uuid The profile UUID.
     * @return The profile, or null if not found.
     */
    suspend fun getProfileByUuid(uuid: String): Profile?

    /**
     * Get the total number of profiles.
     *
     * @return The count of profiles.
     */
    suspend fun getProfileCount(): Int

    /**
     * Get any profile (typically the first one).
     * Useful for selecting a default profile when no profile is selected.
     *
     * @return A profile, or null if no profiles exist.
     */
    suspend fun getAnyProfile(): Profile?

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert a new profile.
     * The profile will be appended at the end of the order.
     *
     * @param profile The profile to insert.
     * @return The generated ID for the new profile.
     */
    suspend fun insertProfile(profile: Profile): Long

    /**
     * Update an existing profile.
     *
     * @param profile The profile with updated values.
     */
    suspend fun updateProfile(profile: Profile)

    /**
     * Delete a profile.
     *
     * @param profile The profile to delete.
     */
    suspend fun deleteProfile(profile: Profile)

    /**
     * Update the order of profiles.
     *
     * @param profiles The list of profiles with updated orderNo values.
     */
    suspend fun updateProfileOrder(profiles: List<Profile>)
}
