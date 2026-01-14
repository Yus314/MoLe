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

import android.net.Uri
import io.mockk.mockk
import java.io.FileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.fake.FakeConfigBackup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BackupsViewModel.
 *
 * Tests cover:
 * - ProfileRepository interactions for backup availability
 * - Backup/restore operations using FakeConfigBackup
 * - Error handling for backup/restore failures
 * - UI state transitions during backup/restore
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepositoryForBackups
    private lateinit var fakeConfigBackup: FakeConfigBackup
    private lateinit var viewModel: BackupsViewModel
    private lateinit var testUri: Uri

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForBackups()
        fakeConfigBackup = FakeConfigBackup()
        viewModel = BackupsViewModel(profileRepository, fakeConfigBackup)
        testUri = mockk(relaxed = true)
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
    // ProfileRepository tests (existing)
    // ========================================

    @Test
    fun `currentProfile is initially null`() = runTest {
        val current = profileRepository.currentProfile.value
        assertNull(current)
    }

    @Test
    fun `backup availability depends on currentProfile`() = runTest {
        val hasProfileInitially = profileRepository.currentProfile.value != null
        assertFalse(hasProfileInitially)

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

    // ========================================
    // T059: BackupsViewModel tests with FakeConfigBackup
    // ========================================

    @Test
    fun `performBackup success updates UI state and sends effect`() = runTest {
        // Given
        fakeConfigBackup.shouldSucceed = true

        // When
        viewModel.performBackup(testUri)
        advanceUntilIdle()

        // Then
        assertFalse("isBackingUp should be false after completion", viewModel.uiState.value.isBackingUp)
        assertEquals(1, fakeConfigBackup.backupCallCount)
        assertEquals(testUri, fakeConfigBackup.lastBackupUri)

        // Check effect
        val effect = viewModel.effects.first()
        assertTrue("Effect should be ShowSnackbar", effect is BackupsEffect.ShowSnackbar)
        val message = (effect as BackupsEffect.ShowSnackbar).message
        assertTrue("Message should be Success", message is BackupsMessage.Success)
        assertEquals(R.string.config_saved, (message as BackupsMessage.Success).messageResId)
    }

    @Test
    fun `performBackup failure updates UI state and sends error effect`() = runTest {
        // Given
        fakeConfigBackup.shouldSucceed = false
        fakeConfigBackup.backupError = FileNotFoundException("Cannot create file")

        // When
        viewModel.performBackup(testUri)
        advanceUntilIdle()

        // Then
        assertFalse("isBackingUp should be false after failure", viewModel.uiState.value.isBackingUp)
        assertEquals(1, fakeConfigBackup.backupCallCount)

        // Check error effect
        val effect = viewModel.effects.first()
        assertTrue("Effect should be ShowSnackbar", effect is BackupsEffect.ShowSnackbar)
        val message = (effect as BackupsEffect.ShowSnackbar).message
        assertTrue("Message should be Error", message is BackupsMessage.Error)
        assertTrue(
            "Error message should contain exception info",
            (message as BackupsMessage.Error).message.contains("Cannot create file")
        )
    }

    @Test
    fun `performRestore success updates UI state and sends effect`() = runTest {
        // Given
        fakeConfigBackup.shouldSucceed = true

        // When
        viewModel.performRestore(testUri)
        advanceUntilIdle()

        // Then
        assertFalse("isRestoring should be false after completion", viewModel.uiState.value.isRestoring)
        assertEquals(1, fakeConfigBackup.restoreCallCount)
        assertEquals(testUri, fakeConfigBackup.lastRestoreUri)

        // Check effect
        val effect = viewModel.effects.first()
        assertTrue("Effect should be ShowSnackbar", effect is BackupsEffect.ShowSnackbar)
        val message = (effect as BackupsEffect.ShowSnackbar).message
        assertTrue("Message should be Success", message is BackupsMessage.Success)
        assertEquals(R.string.config_restored, (message as BackupsMessage.Success).messageResId)
    }

    @Test
    fun `performRestore failure updates UI state and sends error effect`() = runTest {
        // Given
        fakeConfigBackup.shouldSucceed = false
        fakeConfigBackup.restoreError = IllegalArgumentException("Invalid JSON format")

        // When
        viewModel.performRestore(testUri)
        advanceUntilIdle()

        // Then
        assertFalse("isRestoring should be false after failure", viewModel.uiState.value.isRestoring)
        assertEquals(1, fakeConfigBackup.restoreCallCount)

        // Check error effect
        val effect = viewModel.effects.first()
        assertTrue("Effect should be ShowSnackbar", effect is BackupsEffect.ShowSnackbar)
        val message = (effect as BackupsEffect.ShowSnackbar).message
        assertTrue("Message should be Error", message is BackupsMessage.Error)
        assertTrue(
            "Error message should contain exception info",
            (message as BackupsMessage.Error).message.contains("Invalid JSON format")
        )
    }

    @Test
    fun `isBackingUp is true during backup operation`() = runTest {
        // Given
        fakeConfigBackup.shouldSucceed = true

        // When
        viewModel.performBackup(testUri)

        // Then - check state immediately before advanceUntilIdle
        assertTrue("isBackingUp should be true during operation", viewModel.uiState.value.isBackingUp)

        advanceUntilIdle()
        assertFalse("isBackingUp should be false after completion", viewModel.uiState.value.isBackingUp)
    }

    @Test
    fun `isRestoring is true during restore operation`() = runTest {
        // Given
        fakeConfigBackup.shouldSucceed = true

        // When
        viewModel.performRestore(testUri)

        // Then - check state immediately before advanceUntilIdle
        assertTrue("isRestoring should be true during operation", viewModel.uiState.value.isRestoring)

        advanceUntilIdle()
        assertFalse("isRestoring should be false after completion", viewModel.uiState.value.isRestoring)
    }

    @Test
    fun `backup enabled state updates when profile changes`() = runTest {
        // Given - no profile
        assertFalse("backupEnabled should be false initially", viewModel.uiState.value.backupEnabled)

        // When - set a profile
        val profile = createTestProfile()
        profileRepository.setCurrentProfile(profile)
        viewModel.updateBackupEnabled(true)

        // Then
        assertTrue("backupEnabled should be true with profile", viewModel.uiState.value.backupEnabled)

        // When - clear profile
        profileRepository.setCurrentProfile(null)
        viewModel.updateBackupEnabled(false)

        // Then
        assertFalse("backupEnabled should be false without profile", viewModel.uiState.value.backupEnabled)
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

    override suspend fun getAllProfilesSync(): List<Profile> = profiles.values.sortedBy { it.orderNo }

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
