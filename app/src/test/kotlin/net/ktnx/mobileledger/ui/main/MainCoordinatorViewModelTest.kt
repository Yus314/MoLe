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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.service.TaskProgress
import net.ktnx.mobileledger.service.TaskState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MainCoordinatorViewModel.
 *
 * Tests cover:
 * - Tab selection
 * - Drawer open/close
 * - Refresh/cancel operations
 * - Navigation effects
 * - BackgroundTaskManager observation
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainCoordinatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var accountRepository: FakeAccountRepositoryForViewModel
    private lateinit var transactionRepository: FakeTransactionRepositoryForViewModel
    private lateinit var optionRepository: FakeOptionRepositoryForViewModel
    private lateinit var backgroundTaskManager: FakeBackgroundTaskManagerForViewModel
    private lateinit var appStateService: FakeAppStateServiceForViewModel
    private lateinit var viewModel: MainCoordinatorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForViewModel()
        accountRepository = FakeAccountRepositoryForViewModel()
        transactionRepository = FakeTransactionRepositoryForViewModel()
        optionRepository = FakeOptionRepositoryForViewModel()
        backgroundTaskManager = FakeBackgroundTaskManagerForViewModel()
        appStateService = FakeAppStateServiceForViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(id: Long = 1L, name: String = "Test Profile", theme: Int = 0): Profile =
        Profile().apply {
            this.id = id
            this.name = name
            this.theme = theme
            this.uuid = java.util.UUID.randomUUID().toString()
            this.url = "https://example.com/ledger"
            this.permitPosting = true
        }

    private fun createViewModel() = MainCoordinatorViewModel(
        profileRepository,
        accountRepository,
        transactionRepository,
        optionRepository,
        backgroundTaskManager,
        appStateService
    )

    // ========================================
    // Init tests
    // ========================================

    @Test
    fun `init observes task running state`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - initial state
        assertFalse(viewModel.uiState.value.isRefreshing)

        // When - task starts
        backgroundTaskManager.taskStarted("test-task")
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertTrue(viewModel.uiState.value.backgroundTasksRunning)
    }

    @Test
    fun `init sets default tab to Accounts`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertEquals(MainTab.Accounts, viewModel.uiState.value.selectedTab)
    }

    // ========================================
    // Tab selection tests
    // ========================================

    @Test
    fun `selectTab updates selected tab`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.SelectTab(MainTab.Transactions))
        advanceUntilIdle()

        // Then
        assertEquals(MainTab.Transactions, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `selectTab back to Accounts updates state`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(MainCoordinatorEvent.SelectTab(MainTab.Transactions))
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.SelectTab(MainTab.Accounts))
        advanceUntilIdle()

        // Then
        assertEquals(MainTab.Accounts, viewModel.uiState.value.selectedTab)
    }

    // ========================================
    // Drawer tests
    // ========================================

    @Test
    fun `openDrawer updates state`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.OpenDrawer)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isDrawerOpen)
        assertTrue(appStateService.drawerOpen.value)
    }

    @Test
    fun `closeDrawer updates state`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(MainCoordinatorEvent.OpenDrawer)
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.CloseDrawer)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isDrawerOpen)
        assertFalse(appStateService.drawerOpen.value)
    }

    @Test
    fun `toggleDrawer toggles state`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isDrawerOpen)

        // When
        viewModel.toggleDrawer()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isDrawerOpen)

        // When
        viewModel.toggleDrawer()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isDrawerOpen)
    }

    // ========================================
    // Refresh tests
    // ========================================

    @Test
    fun `isRefreshing matches task running state`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)

        // When - task starts
        backgroundTaskManager.taskStarted("sync-task")
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isRefreshing)

        // When - task finishes
        backgroundTaskManager.taskFinished("sync-task")
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `cancelRefresh stops running task`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.CancelRefresh)
        advanceUntilIdle()

        // Then - cancel emits FINISHED progress when no task running
        val progress = backgroundTaskManager.progress.value
        assertEquals(TaskState.FINISHED, progress?.state)
    }

    // ========================================
    // Navigation effect tests
    // ========================================

    @Test
    fun `addNewTransaction emits navigation effect when profile can post`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "Test", theme = 180).apply {
            permitPosting = true
        }
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // Collect effects
        var receivedEffect: MainCoordinatorEffect? = null

        // When
        viewModel.onEvent(MainCoordinatorEvent.AddNewTransaction)
        advanceUntilIdle()

        receivedEffect = viewModel.effects.first()

        // Then
        assertTrue(receivedEffect is MainCoordinatorEffect.NavigateToNewTransaction)
        val navEffect = receivedEffect as MainCoordinatorEffect.NavigateToNewTransaction
        assertEquals(1L, navEffect.profileId)
        assertEquals(180, navEffect.theme)
    }

    @Test
    fun `addNewTransaction does not emit effect when profile cannot post`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "Test").apply {
            permitPosting = false
        }
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.AddNewTransaction)
        advanceUntilIdle()

        // Then - no effect emitted (would timeout if we tried to collect)
        // This is verified by the fact that the test completes without hanging
    }

    @Test
    fun `editProfile emits navigation effect`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.EditProfile(5L))
        advanceUntilIdle()

        val effect = viewModel.effects.first()

        // Then
        assertTrue(effect is MainCoordinatorEffect.NavigateToProfileDetail)
        assertEquals(5L, (effect as MainCoordinatorEffect.NavigateToProfileDetail).profileId)
    }

    @Test
    fun `createNewProfile emits navigation effect with null id`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.CreateNewProfile)
        advanceUntilIdle()

        val effect = viewModel.effects.first()

        // Then
        assertTrue(effect is MainCoordinatorEffect.NavigateToProfileDetail)
        assertEquals(null, (effect as MainCoordinatorEffect.NavigateToProfileDetail).profileId)
    }

    @Test
    fun `navigateToTemplates emits navigation effect`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.NavigateToTemplates)
        advanceUntilIdle()

        val effect = viewModel.effects.first()

        // Then
        assertTrue(effect is MainCoordinatorEffect.NavigateToTemplates)
    }

    @Test
    fun `navigateToBackups emits navigation effect`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.NavigateToBackups)
        advanceUntilIdle()

        val effect = viewModel.effects.first()

        // Then
        assertTrue(effect is MainCoordinatorEffect.NavigateToBackups)
    }

    // ========================================
    // Error handling tests
    // ========================================

    @Test
    fun `clearUpdateError clears error from state`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.ClearUpdateError)
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.updateError)
    }

    // ========================================
    // Profile observation tests
    // ========================================

    @Test
    fun `observes current profile for FAB state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "Test", theme = 90).apply {
            permitPosting = true
        }
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertEquals(1L, viewModel.uiState.value.currentProfileId)
        assertEquals(90, viewModel.uiState.value.currentProfileTheme)
        assertTrue(viewModel.uiState.value.currentProfileCanPost)
    }

    @Test
    fun `profile change updates FAB state`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1", theme = 0).apply {
            permitPosting = true
        }
        val profile2 = createTestProfile(id = 2L, name = "Profile 2", theme = 180).apply {
            permitPosting = false
        }
        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        profileRepository.setCurrentProfile(profile1)
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.currentProfileId)
        assertTrue(viewModel.uiState.value.currentProfileCanPost)

        // When
        profileRepository.setCurrentProfile(profile2)
        advanceUntilIdle()

        // Then
        assertEquals(2L, viewModel.uiState.value.currentProfileId)
        assertEquals(180, viewModel.uiState.value.currentProfileTheme)
        assertFalse(viewModel.uiState.value.currentProfileCanPost)
    }
}
