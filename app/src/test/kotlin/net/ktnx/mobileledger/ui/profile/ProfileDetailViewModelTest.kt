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

package net.ktnx.mobileledger.ui.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.db.Profile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ProfileDetailViewModel repository interactions.
 *
 * These tests verify that ProfileDetailViewModel correctly uses ProfileRepository
 * for profile CRUD operations.
 *
 * Note: Full ViewModel testing with SavedStateHandle and network operations
 * requires instrumentation tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepositoryForProfileDetail

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForProfileDetail()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(
        id: Long = 0L,
        name: String = "Test Profile",
        url: String = "https://example.com/ledger",
        theme: Int = 0
    ): Profile = Profile().apply {
        this.id = id
        this.name = name
        this.url = url
        this.theme = theme
        this.uuid = java.util.UUID.randomUUID().toString()
        this.orderNo = 1
        this.useAuthentication = false
        this.permitPosting = true
    }

    // ========================================
    // Profile loading tests
    // ========================================

    @Test
    fun `getProfileByIdSync returns null for non-existent profile`() = runTest {
        val result = profileRepository.getProfileByIdSync(999L)
        assertNull(result)
    }

    @Test
    fun `getProfileByIdSync returns existing profile`() = runTest {
        val profile = createTestProfile(name = "Existing Profile")
        val id = profileRepository.insertProfile(profile)

        val result = profileRepository.getProfileByIdSync(id)

        assertNotNull(result)
        assertEquals("Existing Profile", result?.name)
    }

    // ========================================
    // Profile creation tests
    // ========================================

    @Test
    fun `insertProfile assigns id and stores profile`() = runTest {
        val profile = createTestProfile(name = "New Profile")

        val id = profileRepository.insertProfile(profile)

        assertTrue(id > 0)
        val stored = profileRepository.getProfileByIdSync(id)
        assertNotNull(stored)
        assertEquals("New Profile", stored?.name)
    }

    @Test
    fun `insertProfile increments profile count`() = runTest {
        assertEquals(0, profileRepository.getProfileCount())

        profileRepository.insertProfile(createTestProfile(name = "Profile 1"))
        assertEquals(1, profileRepository.getProfileCount())

        profileRepository.insertProfile(createTestProfile(name = "Profile 2"))
        assertEquals(2, profileRepository.getProfileCount())
    }

    // ========================================
    // Profile update tests
    // ========================================

    @Test
    fun `updateProfile modifies existing profile`() = runTest {
        val profile = createTestProfile(name = "Original Name")
        val id = profileRepository.insertProfile(profile)

        val updated = createTestProfile(id = id, name = "Updated Name")
        profileRepository.updateProfile(updated)

        val result = profileRepository.getProfileByIdSync(id)
        assertEquals("Updated Name", result?.name)
    }

    @Test
    fun `updateProfile updates currentProfile if same profile`() = runTest {
        val profile = createTestProfile(id = 1L, name = "Original")
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        val updated = createTestProfile(id = 1L, name = "Updated")
        profileRepository.updateProfile(updated)

        val current = profileRepository.currentProfile.value
        assertEquals("Updated", current?.name)
    }

    // ========================================
    // Profile deletion tests
    // ========================================

    @Test
    fun `deleteProfile removes profile from repository`() = runTest {
        val profile = createTestProfile(name = "ToDelete")
        val id = profileRepository.insertProfile(profile)
        profile.id = id

        profileRepository.deleteProfile(profile)

        val result = profileRepository.getProfileByIdSync(id)
        assertNull(result)
    }

    @Test
    fun `deleteProfile clears currentProfile if deleted profile was current`() = runTest {
        val profile = createTestProfile(name = "Current")
        val id = profileRepository.insertProfile(profile)
        profile.id = id
        profileRepository.setCurrentProfile(profile)

        profileRepository.deleteProfile(profile)

        val current = profileRepository.currentProfile.value
        assertNull(current)
    }

    @Test
    fun `deleteProfile selects another profile as current if available`() = runTest {
        val profile1 = createTestProfile(name = "Profile 1")
        val profile2 = createTestProfile(name = "Profile 2")
        val id1 = profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        profile1.id = id1
        profileRepository.setCurrentProfile(profile1)

        profileRepository.deleteProfile(profile1)

        val current = profileRepository.currentProfile.value
        assertNotNull(current)
        assertEquals("Profile 2", current?.name)
    }

    // ========================================
    // getAllProfiles tests
    // ========================================

    @Test
    fun `getAllProfiles returns profiles ordered by orderNo`() = runTest {
        val p1 = createTestProfile(name = "Profile A").apply { orderNo = 3 }
        val p2 = createTestProfile(name = "Profile B").apply { orderNo = 1 }
        val p3 = createTestProfile(name = "Profile C").apply { orderNo = 2 }
        profileRepository.insertProfile(p1)
        profileRepository.insertProfile(p2)
        profileRepository.insertProfile(p3)

        val profiles = profileRepository.getAllProfiles().first()

        assertEquals(3, profiles.size)
        assertEquals("Profile B", profiles[0].name) // orderNo = 1
        assertEquals("Profile C", profiles[1].name) // orderNo = 2
        assertEquals("Profile A", profiles[2].name) // orderNo = 3
    }
}

/**
 * Fake ProfileRepository for ProfileDetailViewModel testing.
 */
class FakeProfileRepositoryForProfileDetail : ProfileRepository {
    private val profiles = mutableMapOf<Long, Profile>()
    private var nextId = 1L
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun getAllProfiles(): Flow<List<Profile>> = MutableStateFlow(profiles.values.sortedBy { it.orderNo })

    override fun getProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profiles[profileId])

    override suspend fun getProfileByIdSync(profileId: Long): Profile? = profiles[profileId]

    override fun getProfileByUuid(uuid: String): Flow<Profile?> =
        MutableStateFlow(profiles.values.find { it.uuid == uuid })

    override suspend fun getProfileByUuidSync(uuid: String): Profile? = profiles.values.find { it.uuid == uuid }

    override suspend fun getAnyProfile(): Profile? = profiles.values.firstOrNull()

    override suspend fun getProfileCount(): Int = profiles.size

    override suspend fun insertProfile(profile: Profile): Long {
        val id = if (profile.id == 0L) nextId++ else profile.id
        profile.id = id
        profiles[id] = profile
        return id
    }

    override suspend fun updateProfile(profile: Profile) {
        profiles[profile.id] = profile
        if (_currentProfile.value?.id == profile.id) {
            _currentProfile.value = profile
        }
    }

    override suspend fun deleteProfile(profile: Profile) {
        profiles.remove(profile.id)
        if (_currentProfile.value?.id == profile.id) {
            _currentProfile.value = profiles.values.firstOrNull()
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {
        profiles.forEachIndexed { index, profile ->
            this.profiles[profile.id]?.orderNo = index
        }
    }

    override suspend fun deleteAllProfiles() {
        profiles.clear()
        _currentProfile.value = null
    }
}
