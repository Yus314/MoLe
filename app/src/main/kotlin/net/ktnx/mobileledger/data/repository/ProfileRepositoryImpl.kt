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

import androidx.lifecycle.asFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.ProfileDAO
import net.ktnx.mobileledger.db.Profile

/**
 * Implementation of [ProfileRepository] that wraps the existing [ProfileDAO].
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Maintains current profile state as a StateFlow
 * - Uses Dispatchers.IO for database operations
 * - Delegates all persistence operations to the underlying DAO
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 * The currentProfile StateFlow can be collected from any thread.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(private val profileDAO: ProfileDAO) : ProfileRepository {

    // ========================================
    // Current Profile State
    // ========================================

    private val _currentProfile = MutableStateFlow<Profile?>(null)

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    // ========================================
    // Query Operations
    // ========================================

    override fun getAllProfiles(): Flow<List<Profile>> = profileDAO.getAllOrdered().asFlow()

    override fun getProfileById(profileId: Long): Flow<Profile?> = profileDAO.getById(profileId).asFlow()

    override suspend fun getProfileByIdSync(profileId: Long): Profile? = withContext(Dispatchers.IO) {
        profileDAO.getByIdSync(profileId)
    }

    override fun getProfileByUuid(uuid: String): Flow<Profile?> = profileDAO.getByUuid(uuid).asFlow().map { it }

    override suspend fun getProfileByUuidSync(uuid: String): Profile? = withContext(Dispatchers.IO) {
        profileDAO.getByUuidSync(uuid)
    }

    override suspend fun getAnyProfile(): Profile? = withContext(Dispatchers.IO) {
        profileDAO.getAnySync()
    }

    override suspend fun getProfileCount(): Int = withContext(Dispatchers.IO) {
        profileDAO.getProfileCountSync()
    }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun insertProfile(profile: Profile): Long = withContext(Dispatchers.IO) {
        profileDAO.insertLastSync(profile)
    }

    override suspend fun updateProfile(profile: Profile) {
        withContext(Dispatchers.IO) {
            profileDAO.updateSync(profile)
        }
        // Update current profile if it's the same one being updated
        _currentProfile.value?.let { current ->
            if (current.id == profile.id) {
                _currentProfile.value = profile
            }
        }
    }

    override suspend fun deleteProfile(profile: Profile) {
        withContext(Dispatchers.IO) {
            profileDAO.deleteSync(profile)
        }
        // If deleted profile was current, select another or clear
        _currentProfile.value?.let { current ->
            if (current.id == profile.id) {
                val fallback = withContext(Dispatchers.IO) {
                    profileDAO.getAnySync()
                }
                _currentProfile.value = fallback
            }
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {
        withContext(Dispatchers.IO) {
            profileDAO.updateOrderSync(profiles)
        }
    }

    override suspend fun deleteAllProfiles() {
        withContext(Dispatchers.IO) {
            profileDAO.deleteAllSync()
        }
        _currentProfile.value = null
    }
}
