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
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.util.createTestDomainProfile
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

    private fun createTestProfile(id: Long? = null, name: String = "Test Profile", orderNo: Int = 1): Profile =
        createTestDomainProfile(
            id = id,
            name = name,
            orderNo = orderNo
        )

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
        val profiles = repository.observeAllProfiles().first()
        assertTrue(profiles.isEmpty())
    }

    @Test
    fun `getAllProfiles returns profiles in order`() = runTest {
        val profile1 = createTestProfile(name = "Profile A", orderNo = 2)
        val profile2 = createTestProfile(name = "Profile B", orderNo = 1)
        repository.insertProfile(profile1)
        repository.insertProfile(profile2)

        val profiles = repository.observeAllProfiles().first()
        assertEquals(2, profiles.size)
        assertEquals("Profile B", profiles[0].name) // orderNo = 1
        assertEquals("Profile A", profiles[1].name) // orderNo = 2
    }

    // ========================================
    // getProfileById tests
    // ========================================

    @Test
    fun `observeProfileById returns null for non-existent id`() = runTest {
        val result = repository.observeProfileById(999L).first()
        assertNull(result)
    }

    @Test
    fun `getProfileById returns profile when exists`() = runTest {
        val profile = createTestProfile(name = "Test")
        val id = repository.insertProfile(profile)

        val result = repository.getProfileById(id)
        assertNotNull(result)
        assertEquals("Test", result?.name)
    }

    // ========================================
    // getProfileByUuid tests
    // ========================================

    @Test
    fun `observeProfileByUuid returns null for non-existent uuid`() = runTest {
        val result = repository.observeProfileByUuid("non-existent-uuid").first()
        assertNull(result)
    }

    @Test
    fun `getProfileByUuid returns profile when exists`() = runTest {
        // Insert profile first, then get the UUID from the inserted profile
        val profile = createTestProfile(name = "UUID Test")
        repository.insertProfile(profile)
        val uuid = profile.uuid

        val result = repository.getProfileByUuid(uuid)
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
        val stored = repository.getProfileById(id)
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

        val result = repository.getProfileById(id)
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
        val profileWithId = profile.copy(id = id)

        repository.deleteProfile(profileWithId)

        val remaining = repository.observeAllProfiles().first()
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
        val p1WithId = profile1.copy(id = id1)
        repository.setCurrentProfile(p1WithId)

        repository.deleteProfile(p1WithId)

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

        val profiles = repository.observeAllProfiles().first()
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

        val profiles = repository.observeAllProfiles().first()
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

    override fun observeAllProfiles(): Flow<List<Profile>> = MutableStateFlow(profiles.values.sortedBy { it.orderNo })

    override suspend fun getAllProfiles(): List<Profile> = profiles.values.sortedBy { it.orderNo }

    override fun observeProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profiles[profileId])

    override suspend fun getProfileById(profileId: Long): Profile? = profiles[profileId]

    override fun observeProfileByUuid(uuid: String): Flow<Profile?> = MutableStateFlow(
        profiles.values.find {
            it.uuid == uuid
        }
    )

    override suspend fun getProfileByUuid(uuid: String): Profile? = profiles.values.find { it.uuid == uuid }

    override suspend fun getAnyProfile(): Profile? = profiles.values.firstOrNull()

    override suspend fun getProfileCount(): Int = profiles.size

    override suspend fun insertProfile(profile: Profile): Long {
        val id = if (profile.id == null || profile.id == 0L) nextId++ else profile.id
        val profileWithId = profile.copy(id = id)
        profiles[id] = profileWithId
        emitChanges()
        return id
    }

    override suspend fun updateProfile(profile: Profile) {
        val id = profile.id ?: return
        if (profiles.containsKey(id)) {
            profiles[id] = profile
            emitChanges()
        }
        // Update current profile if it's the same one being updated
        _currentProfile.value?.let { current ->
            if (current.id == id) {
                _currentProfile.value = profile
            }
        }
    }

    override suspend fun deleteProfile(profile: Profile) {
        val id = profile.id ?: return
        profiles.remove(id)
        emitChanges()
        // If deleted profile was current, select another or clear
        _currentProfile.value?.let { current ->
            if (current.id == id) {
                _currentProfile.value = profiles.values.firstOrNull()
            }
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {
        profiles.forEach { profile ->
            val id = profile.id ?: return@forEach
            this.profiles[id]?.let { existing ->
                this.profiles[id] = existing.copy(orderNo = profile.orderNo)
            }
        }
        emitChanges()
    }

    override suspend fun deleteAllProfiles() {
        profiles.clear()
        emitChanges()
        _currentProfile.value = null
    }
}
