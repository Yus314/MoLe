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
import net.ktnx.mobileledger.db.Option

/**
 * Repository for managing application options.
 *
 * Options are key-value pairs associated with profiles, used for storing
 * profile-specific settings like last sync timestamp.
 *
 * Thread-safety: All methods are safe to call from any coroutine context.
 */
interface OptionRepository {

    // ========================================
    // Query Operations
    // ========================================

    /**
     * Gets an option by profile ID and name as a Flow.
     */
    fun getOption(profileId: Long, name: String): Flow<Option?>

    /**
     * Gets an option by profile ID and name synchronously.
     */
    suspend fun getOptionSync(profileId: Long, name: String): Option?

    /**
     * Gets all options for a profile synchronously.
     */
    suspend fun getAllOptionsForProfileSync(profileId: Long): List<Option>

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Inserts or replaces an option.
     * Uses OnConflictStrategy.REPLACE, so existing options with the same
     * profile ID and name will be updated.
     */
    suspend fun insertOption(option: Option): Long

    /**
     * Deletes an option.
     */
    suspend fun deleteOption(option: Option)

    /**
     * Deletes all options for a profile.
     */
    suspend fun deleteOptionsForProfile(profileId: Long)

    /**
     * Deletes all options (for backup/restore).
     */
    suspend fun deleteAllOptions()

    // ========================================
    // Convenience Operations
    // ========================================

    /**
     * Sets the last sync timestamp for a profile.
     *
     * This is a convenience method that creates/updates the OPT_LAST_SCRAPE option.
     *
     * @param profileId The profile ID
     * @param timestamp The timestamp in milliseconds
     */
    suspend fun setLastSyncTimestamp(profileId: Long, timestamp: Long)
}
