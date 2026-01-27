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

package net.ktnx.mobileledger.domain.usecase

import net.ktnx.mobileledger.core.domain.model.Profile

/**
 * Use case for profile persistence operations.
 * Handles save (insert/update) and delete operations with proper side effects.
 */
interface ProfilePersistence {

    /**
     * Saves a profile (insert or update based on whether id is set).
     * Also notifies backup data changed after successful save.
     *
     * @param profile The profile to save
     * @return Result indicating success or failure
     */
    suspend fun save(profile: Profile): Result<Unit>

    /**
     * Deletes a profile by its ID.
     *
     * @param profileId The ID of the profile to delete
     * @return Result indicating success or failure
     */
    suspend fun delete(profileId: Long): Result<Unit>
}
