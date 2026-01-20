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

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.ProfileDAO
import net.ktnx.mobileledger.data.repository.mapper.ProfileMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.ProfileMapper.toEntity
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.usecase.AppExceptionMapper

/**
 * Implementation of [ProfileRepository] that wraps the existing [ProfileDAO].
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Maintains current profile state as a StateFlow
 * - Uses ioDispatcher for database operations
 * - Delegates all persistence operations to the underlying DAO
 * - Returns Result<T> for all suspend operations with error handling
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 * The currentProfile StateFlow can be collected from any thread.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDAO: ProfileDAO,
    private val appExceptionMapper: AppExceptionMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ProfileRepository {

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

    override fun observeAllProfiles(): Flow<List<Profile>> =
        profileDAO.getAllOrdered().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllProfiles(): Result<List<Profile>> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.getAllOrderedSync().map { it.toDomain() }
        }
    }

    override fun observeProfileById(profileId: Long): Flow<Profile?> =
        profileDAO.getById(profileId).map { it?.toDomain() }

    override suspend fun getProfileById(profileId: Long): Result<Profile?> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.getByIdSync(profileId)?.toDomain()
        }
    }

    override fun observeProfileByUuid(uuid: String): Flow<Profile?> = profileDAO.getByUuid(uuid).map { it?.toDomain() }

    override suspend fun getProfileByUuid(uuid: String): Result<Profile?> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.getByUuidSync(uuid)?.toDomain()
        }
    }

    override suspend fun getAnyProfile(): Result<Profile?> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.getAnySync()?.toDomain()
        }
    }

    override suspend fun getProfileCount(): Result<Int> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.getProfileCountSync()
        }
    }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun insertProfile(profile: Profile): Result<Long> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.insertLastSync(profile.toEntity())
        }
    }

    override suspend fun updateProfile(profile: Profile): Result<Unit> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.updateSync(profile.toEntity())
        }
        // Update current profile if it's the same one being updated
        _currentProfile.value?.let { current ->
            if (current.id == profile.id) {
                _currentProfile.value = profile
            }
        }
    }

    override suspend fun deleteProfile(profile: Profile): Result<Unit> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.deleteSync(profile.toEntity())
        }
        // If deleted profile was current, select another or clear
        _currentProfile.value?.let { current ->
            if (current.id == profile.id) {
                val fallback = withContext(ioDispatcher) {
                    profileDAO.getAnySync()?.toDomain()
                }
                _currentProfile.value = fallback
            }
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>): Result<Unit> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.updateOrderSync(profiles.map { it.toEntity() })
        }
    }

    override suspend fun deleteAllProfiles(): Result<Unit> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            profileDAO.deleteAllSync()
        }
        _currentProfile.value = null
    }
}
