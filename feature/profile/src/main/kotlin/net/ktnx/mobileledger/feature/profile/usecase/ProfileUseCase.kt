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

package net.ktnx.mobileledger.feature.profile.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.repository.ProfileRepository

interface ObserveCurrentProfileUseCase {
    operator fun invoke(): StateFlow<Profile?>
}

interface ObserveProfilesUseCase {
    operator fun invoke(): Flow<List<Profile>>
}

interface GetProfileByIdUseCase {
    suspend operator fun invoke(profileId: Long): Result<Profile?>
}

interface GetAllProfilesUseCase {
    suspend operator fun invoke(): Result<List<Profile>>
}

interface SetCurrentProfileUseCase {
    operator fun invoke(profile: Profile?)
}

interface UpdateProfileOrderUseCase {
    suspend operator fun invoke(profiles: List<Profile>): Result<Unit>
}

class ObserveCurrentProfileUseCaseImpl @Inject constructor(
    private val profileRepository: ProfileRepository
) : ObserveCurrentProfileUseCase {
    override fun invoke(): StateFlow<Profile?> = profileRepository.currentProfile
}

class ObserveProfilesUseCaseImpl @Inject constructor(
    private val profileRepository: ProfileRepository
) : ObserveProfilesUseCase {
    override fun invoke(): Flow<List<Profile>> = profileRepository.observeAllProfiles()
}

class GetProfileByIdUseCaseImpl @Inject constructor(
    private val profileRepository: ProfileRepository
) : GetProfileByIdUseCase {
    override suspend fun invoke(profileId: Long): Result<Profile?> = profileRepository.getProfileById(profileId)
}

class GetAllProfilesUseCaseImpl @Inject constructor(
    private val profileRepository: ProfileRepository
) : GetAllProfilesUseCase {
    override suspend fun invoke(): Result<List<Profile>> = profileRepository.getAllProfiles()
}

class SetCurrentProfileUseCaseImpl @Inject constructor(
    private val profileRepository: ProfileRepository
) : SetCurrentProfileUseCase {
    override fun invoke(profile: Profile?) {
        profileRepository.setCurrentProfile(profile)
    }
}

class UpdateProfileOrderUseCaseImpl @Inject constructor(
    private val profileRepository: ProfileRepository
) : UpdateProfileOrderUseCase {
    override suspend fun invoke(profiles: List<Profile>): Result<Unit> = profileRepository.updateProfileOrder(profiles)
}
