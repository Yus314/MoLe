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

import javax.inject.Inject
import logcat.logcat
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.repository.ProfileRepository
import net.ktnx.mobileledger.service.AuthDataProvider

/**
 * Implementation of ProfilePersistence that handles profile save and delete operations.
 * Coordinates between ProfileRepository and AuthDataProvider for proper side effects.
 */
class ProfilePersistenceImpl @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authDataProvider: AuthDataProvider
) : ProfilePersistence {

    override suspend fun save(profile: Profile): Result<Unit> = runCatching {
        val profileId = profile.id
        if (profileId != null && profileId > 0) {
            profileRepository.updateProfile(profile).getOrThrow()
            logcat { "Profile updated in DB" }
        } else {
            profileRepository.insertProfile(profile).getOrThrow()
            logcat { "Profile inserted in DB" }
        }

        authDataProvider.notifyBackupDataChanged()
    }

    override suspend fun delete(profileId: Long): Result<Unit> = runCatching {
        val profile = profileRepository.getProfileById(profileId).getOrNull()
        if (profile != null) {
            profileRepository.deleteProfile(profile).getOrThrow()
            logcat { "Profile deleted from DB" }
        }
    }
}
