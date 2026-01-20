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

package net.ktnx.mobileledger.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.domain.model.Profile

/**
 * Fake ProfileRepository for ViewModel testing.
 *
 * This Fake implementation allows tests to control profile state and verify
 * that ViewModels correctly interact with ProfileRepository.
 */
class FakeProfileRepository : ProfileRepository {
    private val profilesMap = mutableMapOf<Long, Profile>()
    private var nextId = 1L
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    val profiles: List<Profile>
        get() = profilesMap.values.sortedBy { it.orderNo }

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun observeAllProfiles(): Flow<List<Profile>> = MutableStateFlow(
        profilesMap.values.sortedBy {
            it.orderNo
        }
    )

    override suspend fun getAllProfiles(): List<Profile> = profilesMap.values.sortedBy { it.orderNo }

    override fun observeProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profilesMap[profileId])

    override suspend fun getProfileById(profileId: Long): Profile? = profilesMap[profileId]

    override fun observeProfileByUuid(uuid: String): Flow<Profile?> =
        MutableStateFlow(profilesMap.values.find { it.uuid == uuid })

    override suspend fun getProfileByUuid(uuid: String): Profile? = profilesMap.values.find { it.uuid == uuid }

    override suspend fun getAnyProfile(): Profile? = profilesMap.values.firstOrNull()

    override suspend fun getProfileCount(): Int = profilesMap.size

    override suspend fun insertProfile(profile: Profile): Long {
        val id = if (profile.id == null || profile.id == 0L) nextId++ else profile.id
        val profileWithId = profile.copy(id = id)
        profilesMap[id] = profileWithId
        return id
    }

    override suspend fun updateProfile(profile: Profile) {
        val id = profile.id ?: return
        profilesMap[id] = profile
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profile
        }
    }

    override suspend fun deleteProfile(profile: Profile) {
        val id = profile.id ?: return
        profilesMap.remove(id)
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profilesMap.values.firstOrNull()
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {
        profiles.forEachIndexed { index, profile ->
            val id = profile.id ?: return@forEachIndexed
            profilesMap[id]?.let { existing ->
                profilesMap[id] = existing.copy(orderNo = index)
            }
        }
    }

    override suspend fun deleteAllProfiles() {
        profilesMap.clear()
        _currentProfile.value = null
    }
}
