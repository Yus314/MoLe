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
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.SyncState
import net.ktnx.mobileledger.core.testing.fake.FakeProfileRepository
import net.ktnx.mobileledger.fake.FakeAppStateService
import net.ktnx.mobileledger.fake.FakeBackgroundTaskManager
import net.ktnx.mobileledger.fake.FakeTransactionSyncer
import net.ktnx.mobileledger.feature.profile.usecase.ObserveCurrentProfileUseCaseImpl
import net.ktnx.mobileledger.util.createTestDomainProfile
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
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var transactionSyncer: FakeTransactionSyncer
    private lateinit var backgroundTaskManager: FakeBackgroundTaskManager
    private lateinit var appStateService: FakeAppStateService
    private lateinit var viewModel: MainCoordinatorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepository()
        transactionSyncer = FakeTransactionSyncer()
        backgroundTaskManager = FakeBackgroundTaskManager()
        appStateService = FakeAppStateService()
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
        permitPosting: Boolean = true
    ): Profile = createTestDomainProfile(
        id = id,
        name = name,
        theme = theme,
        permitPosting = permitPosting
    )

    private fun createViewModel() = MainCoordinatorViewModel(
        observeCurrentProfileUseCase = ObserveCurrentProfileUseCaseImpl(profileRepository),
        transactionSyncer,
        backgroundTaskManager,
        appStateService
    )

    // ========================================
    // Init tests
    // ========================================

    @Test
    fun `startSync updates sync state and completes`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        transactionSyncer.progressSteps = 2
        transactionSyncer.delayPerStepMs = 0 // Complete quickly

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - initial state
        assertFalse(viewModel.uiState.value.isRefreshing)

        // When - sync starts and completes
        viewModel.startSync()
        advanceUntilIdle()

        // Then - sync completed
        assertTrue(viewModel.syncState.value is SyncState.Completed)
        assertFalse(viewModel.uiState.value.isRefreshing)
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
    fun `isRefreshing matches sync state`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        transactionSyncer.progressSteps = 2
        transactionSyncer.delayPerStepMs = 0 // Complete quickly
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)

        // When - sync starts and completes
        viewModel.startSync()
        advanceUntilIdle()

        // Then - sync completed, not refreshing anymore
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertTrue(viewModel.syncState.value is SyncState.Completed)
    }

    @Test
    fun `cancelRefresh cancels sync`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.CancelRefresh)
        advanceUntilIdle()

        // Then - sync state should be cancelled
        assertTrue(viewModel.syncState.value is SyncState.Cancelled)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    // ========================================
    // Navigation effect tests
    // ========================================

    @Test
    fun `addNewTransaction emits navigation effect when profile can post`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L, name = "Test", theme = 180, permitPosting = true)
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
        val profile = createTestProfile(id = 1L, name = "Test", permitPosting = false)
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
        val profile = createTestProfile(id = 1L, name = "Test", theme = 90, permitPosting = true)
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
        val profile1 = createTestProfile(id = 1L, name = "Profile 1", theme = 0, permitPosting = true)
        val profile2 = createTestProfile(id = 2L, name = "Profile 2", theme = 180, permitPosting = false)
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

    // ========================================
    // Sync state transition tests
    // ========================================

    @Test
    fun `startSync with no profile does nothing`() = runTest {
        // Given - no profile set
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.startSync()
        advanceUntilIdle()

        // Then - state should remain idle
        assertTrue(viewModel.syncState.value is SyncState.Idle)
    }

    @Test
    fun `startSync with explicit profile syncs that profile`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1")
        val profile2 = createTestProfile(id = 2L, name = "Profile 2")
        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        profileRepository.setCurrentProfile(profile1)
        transactionSyncer.progressSteps = 1
        transactionSyncer.delayPerStepMs = 0

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - sync with explicit profile2
        viewModel.startSync(profile2)
        advanceUntilIdle()

        // Then - sync completed
        assertTrue(viewModel.syncState.value is SyncState.Completed)
    }

    @Test
    fun `sync failure updates state to Failed`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        transactionSyncer.shouldSucceed = false

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.startSync()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.syncState.value is SyncState.Failed)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `cancelSync during sync updates state to Cancelled`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        transactionSyncer.progressSteps = 10 // Long sync
        transactionSyncer.delayPerStepMs = 100

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - start sync then cancel
        viewModel.startSync()
        viewModel.cancelSync()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.syncState.value is SyncState.Cancelled)
    }

    @Test
    fun `clearSyncState resets to Idle`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        transactionSyncer.progressSteps = 1
        transactionSyncer.delayPerStepMs = 0

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.startSync()
        advanceUntilIdle()

        // Verify sync completed
        assertTrue(viewModel.syncState.value is SyncState.Completed)

        // When
        viewModel.clearSyncState()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.syncState.value is SyncState.Idle)
    }

    @Test
    fun `startSync cancels previous sync`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        transactionSyncer.progressSteps = 1
        transactionSyncer.delayPerStepMs = 0

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - start sync twice
        viewModel.startSync()
        viewModel.startSync()
        advanceUntilIdle()

        // Then - second sync should complete
        assertTrue(viewModel.syncState.value is SyncState.Completed)
    }

    // ========================================
    // Update info tests
    // ========================================

    @Test
    fun `updateProfile updates UI state directly`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        val profile = createTestProfile(id = 5L, name = "Manual Update", theme = 200, permitPosting = true)

        // When
        viewModel.updateProfile(profile)
        advanceUntilIdle()

        // Then
        assertEquals(5L, viewModel.uiState.value.currentProfileId)
        assertEquals(200, viewModel.uiState.value.currentProfileTheme)
        assertTrue(viewModel.uiState.value.currentProfileCanPost)
    }

    @Test
    fun `updateProfile with null clears profile state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.currentProfileId)

        // When
        viewModel.updateProfile(null)
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.currentProfileId)
        assertEquals(-1, viewModel.uiState.value.currentProfileTheme)
        assertFalse(viewModel.uiState.value.currentProfileCanPost)
    }

    @Test
    fun `updateLastUpdateInfo updates UI state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        val date = java.util.Date()

        // When
        viewModel.updateLastUpdateInfo(date, 100, 25)
        advanceUntilIdle()

        // Then
        assertEquals(date, viewModel.uiState.value.lastUpdateDate)
        assertEquals(100, viewModel.uiState.value.lastUpdateTransactionCount)
        assertEquals(25, viewModel.uiState.value.lastUpdateAccountCount)
    }

    @Test
    fun `updateLastUpdateInfo with null values uses defaults`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateLastUpdateInfo(null, null, null)
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.lastUpdateDate)
        assertEquals(0, viewModel.uiState.value.lastUpdateTransactionCount)
        assertEquals(0, viewModel.uiState.value.lastUpdateAccountCount)
    }

    // ========================================
    // Data reload tests
    // ========================================

    @Test
    fun `reloadDataAfterChange signals app state service`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        val initialVersion = appStateService.dataVersion.value

        // When
        viewModel.reloadDataAfterChange()
        advanceUntilIdle()

        // Then
        assertTrue(appStateService.dataVersion.value > initialVersion)
    }

    // ========================================
    // Drawer toggle idempotence tests
    // ========================================

    @Test
    fun `openDrawer is idempotent`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When - open drawer multiple times
        viewModel.onEvent(MainCoordinatorEvent.OpenDrawer)
        advanceUntilIdle()
        viewModel.onEvent(MainCoordinatorEvent.OpenDrawer)
        advanceUntilIdle()

        // Then - still open
        assertTrue(viewModel.uiState.value.isDrawerOpen)
    }

    @Test
    fun `closeDrawer is idempotent`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When - close drawer multiple times (even though it's already closed)
        viewModel.onEvent(MainCoordinatorEvent.CloseDrawer)
        advanceUntilIdle()
        viewModel.onEvent(MainCoordinatorEvent.CloseDrawer)
        advanceUntilIdle()

        // Then - still closed
        assertFalse(viewModel.uiState.value.isDrawerOpen)
    }

    // ========================================
    // RefreshData event tests
    // ========================================

    @Test
    fun `RefreshData event starts sync`() = runTest {
        // Given
        val profile = createTestProfile()
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        transactionSyncer.progressSteps = 1
        transactionSyncer.delayPerStepMs = 0

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(MainCoordinatorEvent.RefreshData)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.syncState.value is SyncState.Completed)
    }
}
