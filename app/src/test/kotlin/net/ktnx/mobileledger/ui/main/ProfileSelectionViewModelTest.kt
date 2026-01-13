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

package net.ktnx.mobileledger.ui.main

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
 * Unit tests for ProfileSelectionViewModel.
 *
 * Tests cover:
 * - Profile list loading
 * - Profile selection
 * - Profile reordering
 * - Observing profile changes from repository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSelectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var viewModel: ProfileSelectionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(
        id: Long = 1L,
        name: String = "Test Profile",
        theme: Int = 0,
        orderNo: Int = 0
    ): Profile = Profile().apply {
        this.id = id
        this.name = name
        this.theme = theme
        this.orderNo = orderNo
        this.uuid = java.util.UUID.randomUUID().toString()
        this.url = "https://example.com/ledger"
        this.permitPosting = true
    }

    private fun createViewModel() = ProfileSelectionViewModel(profileRepository)

    // ========================================
    // Init tests
    // ========================================

    @Test
    fun `init loads profiles from repository`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1", orderNo = 0)
        val profile2 = createTestProfile(id = 2L, name = "Profile 2", orderNo = 1)
        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        profileRepository.setCurrentProfile(profile1)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(2, uiState.profiles.size)
        assertEquals("Profile 1", uiState.profiles[0].name)
        assertEquals("Profile 2", uiState.profiles[1].name)
    }

    @Test
    fun `init sets current profile from repository`() = runTest {
        // Given
        val profile = createTestProfile(id = 5L, name = "Current Profile")
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(5L, uiState.currentProfileId)
        assertEquals("Current Profile", uiState.currentProfileName)
    }

    @Test
    fun `init with no profiles results in empty state`() = runTest {
        // Given - empty repository

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.profiles.isEmpty())
        assertNull(uiState.currentProfileId)
    }

    // ========================================
    // SelectProfile tests
    // ========================================

    @Test
    fun `selectProfile updates current profile`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1")
        val profile2 = createTestProfile(id = 2L, name = "Profile 2")
        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        profileRepository.setCurrentProfile(profile1)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(ProfileSelectionEvent.SelectProfile(2L))
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(2L, uiState.currentProfileId)
        assertEquals("Profile 2", uiState.currentProfileName)
        assertEquals(profile2, profileRepository.currentProfile.value)
    }

    @Test
    fun `selectProfile with invalid id does nothing`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "Profile 1")
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(ProfileSelectionEvent.SelectProfile(999L))
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(1L, uiState.currentProfileId) // Unchanged
    }

    // ========================================
    // ReorderProfiles tests
    // ========================================

    @Test
    fun `reorderProfiles updates profile order`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile A", orderNo = 0)
        val profile2 = createTestProfile(id = 2L, name = "Profile B", orderNo = 1)
        val profile3 = createTestProfile(id = 3L, name = "Profile C", orderNo = 2)
        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        profileRepository.insertProfile(profile3)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When - reorder to C, A, B
        val reordered = listOf(
            ProfileListItem(3L, "Profile C", 0, true),
            ProfileListItem(1L, "Profile A", 0, true),
            ProfileListItem(2L, "Profile B", 0, true)
        )
        viewModel.onEvent(ProfileSelectionEvent.ReorderProfiles(reordered))
        advanceUntilIdle()

        // Then
        val profiles = profileRepository.profiles
        assertEquals("Profile C", profiles[0].name)
        assertEquals("Profile A", profiles[1].name)
        assertEquals("Profile B", profiles[2].name)
    }

    // ========================================
    // Profile observation tests
    // ========================================

    @Test
    fun `observes external profile changes`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1")
        val profile2 = createTestProfile(id = 2L, name = "Profile 2")
        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        profileRepository.setCurrentProfile(profile1)
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.currentProfileId)

        // When - external change
        profileRepository.setCurrentProfile(profile2)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(2L, uiState.currentProfileId)
        assertEquals("Profile 2", uiState.currentProfileName)
    }

    @Test
    fun `currentProfile reflects repository state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "Test")
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - currentProfile flow is same as repository
        assertEquals(profileRepository.currentProfile.value, viewModel.currentProfile.value)
    }

    // ========================================
    // UiState field tests
    // ========================================

    @Test
    fun `uiState contains profile theme`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "Themed", theme = 180)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertEquals(180, viewModel.uiState.value.currentProfileTheme)
    }

    @Test
    fun `uiState contains canPost flag`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "CanPost").apply {
            permitPosting = true
        }
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.currentProfileCanPost)
    }

    @Test
    fun `uiState canPost is false when profile disallows posting`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "NoPost").apply {
            permitPosting = false
        }
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.currentProfileCanPost)
    }

    @Test
    fun `profiles list contains ProfileListItem with correct fields`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "Test", theme = 90).apply {
            permitPosting = false
        }
        profileRepository.insertProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val item = viewModel.uiState.value.profiles[0]
        assertEquals(1L, item.id)
        assertEquals("Test", item.name)
        assertEquals(90, item.theme)
        assertFalse(item.canPost)
    }
}
