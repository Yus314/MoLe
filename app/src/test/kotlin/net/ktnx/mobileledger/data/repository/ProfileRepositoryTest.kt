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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.db.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProfileRepository] using a fake repository implementation.
 *
 * These tests verify:
 * - Current profile state management
 * - CRUD operations work correctly
 * - Flow emissions occur on data changes
 * - Profile order management
 *
 * Note: For proper LiveData/Flow testing with Room, use instrumentation tests.
 * These unit tests use a fake repository that implements the interface directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryTest {

    private lateinit var repository: FakeProfileRepository

    @Before
    fun setup() {
        repository = FakeProfileRepository()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(
        id: Long = 0L,
        name: String = "Test Profile",
        uuid: String = java.util.UUID.randomUUID().toString(),
        orderNo: Int = 1
    ): Profile = Profile().apply {
        this.id = id
        this.name = name
        this.uuid = uuid
        this.orderNo = orderNo
        this.url = "https://example.com/ledger"
        this.useAuthentication = false
    }

    // ========================================
    // currentProfile tests
    // ========================================

    @Test
    fun `currentProfile is initially null`() = runTest {
        val current = repository.currentProfile.value
        assertNull(current)
    }

    @Test
    fun `setCurrentProfile updates currentProfile state`() = runTest {
        val profile = createTestProfile(id = 1L, name = "My Profile")

        repository.setCurrentProfile(profile)

        val current = repository.currentProfile.value
        assertNotNull(current)
        assertEquals("My Profile", current?.name)
    }

    @Test
    fun `setCurrentProfile to null clears selection`() = runTest {
        val profile = createTestProfile(id = 1L)
        repository.setCurrentProfile(profile)

        repository.setCurrentProfile(null)

        val current = repository.currentProfile.value
        assertNull(current)
    }

    // ========================================
    // getAllProfiles tests
    // ========================================

    @Test
    fun `getAllProfiles returns empty list when no profiles`() = runTest {
        val profiles = repository.getAllProfiles().first()
        assertTrue(profiles.isEmpty())
    }

    @Test
    fun `getAllProfiles returns profiles in order`() = runTest {
        val profile1 = createTestProfile(name = "Profile A", orderNo = 2)
        val profile2 = createTestProfile(name = "Profile B", orderNo = 1)
        repository.insertProfile(profile1)
        repository.insertProfile(profile2)

        val profiles = repository.getAllProfiles().first()
        assertEquals(2, profiles.size)
        assertEquals("Profile B", profiles[0].name) // orderNo = 1
        assertEquals("Profile A", profiles[1].name) // orderNo = 2
    }

    // ========================================
    // getProfileById tests
    // ========================================

    @Test
    fun `getProfileById returns null for non-existent id`() = runTest {
        val result = repository.getProfileById(999L).first()
        assertNull(result)
    }

    @Test
    fun `getProfileByIdSync returns profile when exists`() = runTest {
        val profile = createTestProfile(name = "Test")
        val id = repository.insertProfile(profile)

        val result = repository.getProfileByIdSync(id)
        assertNotNull(result)
        assertEquals("Test", result?.name)
    }

    // ========================================
    // getProfileByUuid tests
    // ========================================

    @Test
    fun `getProfileByUuid returns null for non-existent uuid`() = runTest {
        val result = repository.getProfileByUuid("non-existent-uuid").first()
        assertNull(result)
    }

    @Test
    fun `getProfileByUuidSync returns profile when exists`() = runTest {
        val uuid = "test-uuid-12345"
        val profile = createTestProfile(name = "UUID Test", uuid = uuid)
        repository.insertProfile(profile)

        val result = repository.getProfileByUuidSync(uuid)
        assertNotNull(result)
        assertEquals("UUID Test", result?.name)
    }

    // ========================================
    // getAnyProfile tests
    // ========================================

    @Test
    fun `getAnyProfile returns null when no profiles`() = runTest {
        val result = repository.getAnyProfile()
        assertNull(result)
    }

    @Test
    fun `getAnyProfile returns a profile when exists`() = runTest {
        val profile = createTestProfile(name = "Some Profile")
        repository.insertProfile(profile)

        val result = repository.getAnyProfile()
        assertNotNull(result)
    }

    // ========================================
    // getProfileCount tests
    // ========================================

    @Test
    fun `getProfileCount returns zero when no profiles`() = runTest {
        val count = repository.getProfileCount()
        assertEquals(0, count)
    }

    @Test
    fun `getProfileCount returns correct count`() = runTest {
        repository.insertProfile(createTestProfile(name = "P1"))
        repository.insertProfile(createTestProfile(name = "P2"))
        repository.insertProfile(createTestProfile(name = "P3"))

        val count = repository.getProfileCount()
        assertEquals(3, count)
    }

    // ========================================
    // insertProfile tests
    // ========================================

    @Test
    fun `insertProfile assigns id and returns it`() = runTest {
        val profile = createTestProfile(name = "New Profile")

        val id = repository.insertProfile(profile)

        assertTrue(id > 0)
        val stored = repository.getProfileByIdSync(id)
        assertNotNull(stored)
        assertEquals("New Profile", stored?.name)
    }

    // ========================================
    // updateProfile tests
    // ========================================

    @Test
    fun `updateProfile modifies existing profile`() = runTest {
        val profile = createTestProfile(name = "Original")
        val id = repository.insertProfile(profile)

        val updated = createTestProfile(id = id, name = "Updated")
        repository.updateProfile(updated)

        val result = repository.getProfileByIdSync(id)
        assertEquals("Updated", result?.name)
    }

    @Test
    fun `updateProfile updates currentProfile if it is the same`() = runTest {
        val profile = createTestProfile(id = 1L, name = "Original")
        repository.insertProfile(profile)
        repository.setCurrentProfile(profile)

        val updated = createTestProfile(id = 1L, name = "Updated Name")
        repository.updateProfile(updated)

        val current = repository.currentProfile.value
        assertEquals("Updated Name", current?.name)
    }

    // ========================================
    // deleteProfile tests
    // ========================================

    @Test
    fun `deleteProfile removes profile`() = runTest {
        val profile = createTestProfile(name = "ToDelete")
        val id = repository.insertProfile(profile)

        repository.deleteProfile(profile.apply { this.id = id })

        val remaining = repository.getAllProfiles().first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `deleteProfile clears currentProfile if deleted`() = runTest {
        val profile = createTestProfile(id = 1L, name = "Current")
        repository.insertProfile(profile)
        repository.setCurrentProfile(profile)

        repository.deleteProfile(profile)

        val current = repository.currentProfile.value
        assertNull(current)
    }

    @Test
    fun `deleteProfile selects fallback if another profile exists`() = runTest {
        val profile1 = createTestProfile(name = "Profile 1")
        val profile2 = createTestProfile(name = "Profile 2")
        val id1 = repository.insertProfile(profile1)
        repository.insertProfile(profile2)
        val p1 = profile1.apply { id = id1 }
        repository.setCurrentProfile(p1)

        repository.deleteProfile(p1)

        val current = repository.currentProfile.value
        assertNotNull(current)
        assertEquals("Profile 2", current?.name)
    }

    // ========================================
    // updateProfileOrder tests
    // ========================================

    @Test
    fun `updateProfileOrder reorders profiles`() = runTest {
        val p1 = createTestProfile(name = "P1", orderNo = 1)
        val p2 = createTestProfile(name = "P2", orderNo = 2)
        val p3 = createTestProfile(name = "P3", orderNo = 3)
        val id1 = repository.insertProfile(p1)
        val id2 = repository.insertProfile(p2)
        val id3 = repository.insertProfile(p3)

        // Reorder: P3, P1, P2
        repository.updateProfileOrder(
            listOf(
                createTestProfile(id = id3, name = "P3", orderNo = 1),
                createTestProfile(id = id1, name = "P1", orderNo = 2),
                createTestProfile(id = id2, name = "P2", orderNo = 3)
            )
        )

        val profiles = repository.getAllProfiles().first()
        assertEquals("P3", profiles[0].name)
        assertEquals("P1", profiles[1].name)
        assertEquals("P2", profiles[2].name)
    }

    // ========================================
    // deleteAllProfiles tests
    // ========================================

    @Test
    fun `deleteAllProfiles removes all profiles`() = runTest {
        repository.insertProfile(createTestProfile(name = "P1"))
        repository.insertProfile(createTestProfile(name = "P2"))

        repository.deleteAllProfiles()

        val profiles = repository.getAllProfiles().first()
        assertTrue(profiles.isEmpty())
    }

    @Test
    fun `deleteAllProfiles clears currentProfile`() = runTest {
        val profile = createTestProfile(name = "Current")
        repository.insertProfile(profile)
        repository.setCurrentProfile(profile)

        repository.deleteAllProfiles()

        val current = repository.currentProfile.value
        assertNull(current)
    }
}

/**
 * Fake implementation of [ProfileRepository] for unit testing.
 *
 * This implementation provides an in-memory store that allows testing
 * without a real database or Room infrastructure.
 */
class FakeProfileRepository : ProfileRepository {

    private val profiles = mutableMapOf<Long, Profile>()
    private var nextId = 1L
    private val profilesFlow = MutableStateFlow<List<Profile>>(emptyList())
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    private fun emitChanges() {
        profilesFlow.value = profiles.values.sortedBy { it.orderNo }
    }

    override fun getAllProfiles(): Flow<List<Profile>> = MutableStateFlow(profiles.values.sortedBy { it.orderNo })

    override suspend fun getAllProfilesSync(): List<Profile> = profiles.values.sortedBy { it.orderNo }

    override fun getProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profiles[profileId])

    override suspend fun getProfileByIdSync(profileId: Long): Profile? = profiles[profileId]

    override fun getProfileByUuid(uuid: String): Flow<Profile?> = MutableStateFlow(
        profiles.values.find {
            it.uuid == uuid
        }
    )

    override suspend fun getProfileByUuidSync(uuid: String): Profile? = profiles.values.find { it.uuid == uuid }

    override suspend fun getAnyProfile(): Profile? = profiles.values.firstOrNull()

    override suspend fun getProfileCount(): Int = profiles.size

    override suspend fun insertProfile(profile: Profile): Long {
        val id = if (profile.id == 0L) nextId++ else profile.id
        profile.id = id
        profiles[id] = profile
        emitChanges()
        return id
    }

    override suspend fun updateProfile(profile: Profile) {
        if (profiles.containsKey(profile.id)) {
            profiles[profile.id] = profile
            emitChanges()
        }
        // Update current profile if it's the same one being updated
        _currentProfile.value?.let { current ->
            if (current.id == profile.id) {
                _currentProfile.value = profile
            }
        }
    }

    override suspend fun deleteProfile(profile: Profile) {
        profiles.remove(profile.id)
        emitChanges()
        // If deleted profile was current, select another or clear
        _currentProfile.value?.let { current ->
            if (current.id == profile.id) {
                _currentProfile.value = profiles.values.firstOrNull()
            }
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {
        profiles.forEach { profile ->
            this.profiles[profile.id]?.orderNo = profile.orderNo
        }
        emitChanges()
    }

    override suspend fun deleteAllProfiles() {
        profiles.clear()
        emitChanges()
        _currentProfile.value = null
    }
}
