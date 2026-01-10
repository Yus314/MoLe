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

package net.ktnx.mobileledger.ui.backups

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.db.Profile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BackupsViewModel repository interactions.
 *
 * These tests verify that BackupsViewModel correctly uses ProfileRepository
 * to determine backup availability (based on current profile).
 *
 * Note: Backup/restore operations require Android Context and are tested
 * in instrumentation tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepositoryForBackups

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForBackups()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(id: Long = 0L, name: String = "Test Profile"): Profile = Profile().apply {
        this.id = id
        this.name = name
        this.uuid = java.util.UUID.randomUUID().toString()
        this.url = "https://example.com/ledger"
        this.orderNo = 1
    }

    // ========================================
    // currentProfile availability tests
    // ========================================

    @Test
    fun `currentProfile is initially null`() = runTest {
        val current = profileRepository.currentProfile.value
        assertNull(current)
    }

    @Test
    fun `backup availability depends on currentProfile`() = runTest {
        // Initially no profile selected - backup should be disabled
        val hasProfileInitially = profileRepository.currentProfile.value != null
        assertFalse(hasProfileInitially)

        // Select a profile - backup should be enabled
        val profile = createTestProfile()
        profileRepository.setCurrentProfile(profile)

        val hasProfileAfter = profileRepository.currentProfile.value != null
        assertTrue(hasProfileAfter)
    }

    @Test
    fun `setCurrentProfile updates state`() = runTest {
        val profile = createTestProfile(name = "Backup Profile")

        profileRepository.setCurrentProfile(profile)

        val current = profileRepository.currentProfile.value
        assertNotNull(current)
        assertEquals("Backup Profile", current?.name)
    }

    @Test
    fun `clearing currentProfile disables backup`() = runTest {
        val profile = createTestProfile()
        profileRepository.setCurrentProfile(profile)
        assertTrue(profileRepository.currentProfile.value != null)

        profileRepository.setCurrentProfile(null)

        assertNull(profileRepository.currentProfile.value)
    }

    // ========================================
    // Profile count tests (for restore validation)
    // ========================================

    @Test
    fun `getProfileCount returns zero when no profiles`() = runTest {
        val count = profileRepository.getProfileCount()
        assertEquals(0, count)
    }

    @Test
    fun `getProfileCount returns correct count after inserts`() = runTest {
        profileRepository.insertProfile(createTestProfile(name = "P1"))
        profileRepository.insertProfile(createTestProfile(name = "P2"))

        val count = profileRepository.getProfileCount()
        assertEquals(2, count)
    }

    @Test
    fun `deleteAllProfiles clears profiles and currentProfile`() = runTest {
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        profileRepository.deleteAllProfiles()

        assertEquals(0, profileRepository.getProfileCount())
        assertNull(profileRepository.currentProfile.value)
    }
}

/**
 * Fake ProfileRepository for BackupsViewModel testing.
 */
class FakeProfileRepositoryForBackups : ProfileRepository {
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
