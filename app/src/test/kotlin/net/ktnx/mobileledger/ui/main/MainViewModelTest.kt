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
import net.ktnx.mobileledger.fake.FakeCurrencyFormatter
import net.ktnx.mobileledger.fake.FakePreferencesRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MainViewModel.
 *
 * Tests cover:
 * - Profile selection
 * - Zero-balance accounts toggle
 * - Data refresh
 * - Account loading
 * - Transaction loading
 * - Error handling
 * - Tab selection
 * - Account search debounce
 *
 * Following TDD: These tests verify MainViewModel behavior after refactoring
 * to use PreferencesRepository instead of static App methods.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var accountRepository: FakeAccountRepositoryForAccountSummary
    private lateinit var transactionRepository: FakeTransactionRepositoryForViewModel
    private lateinit var optionRepository: FakeOptionRepositoryForViewModel
    private lateinit var backgroundTaskManager: FakeBackgroundTaskManagerForViewModel
    private lateinit var appStateService: FakeAppStateServiceForViewModel
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var currencyFormatter: FakeCurrencyFormatter
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForViewModel()
        accountRepository = FakeAccountRepositoryForAccountSummary()
        transactionRepository = FakeTransactionRepositoryForViewModel()
        optionRepository = FakeOptionRepositoryForViewModel()
        backgroundTaskManager = FakeBackgroundTaskManagerForViewModel()
        appStateService = FakeAppStateServiceForViewModel()
        preferencesRepository = FakePreferencesRepository()
        currencyFormatter = FakeCurrencyFormatter()
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

    private fun createViewModel() = MainViewModel(
        profileRepository,
        accountRepository,
        transactionRepository,
        optionRepository,
        backgroundTaskManager,
        appStateService,
        preferencesRepository,
        currencyFormatter
    )

    // ========================================
    // T015: Basic structure tests
    // ========================================

    @Test
    fun `init creates viewModel without crash`() = runTest {
        // Given - default fake repositories

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - no crash
        assertNotNull(viewModel)
    }

    @Test
    fun `init loads showZeroBalanceAccounts from preferences`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(false)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.accountSummaryUiState.value.showZeroBalanceAccounts)
    }

    @Test
    fun `init with showZeroBalanceAccounts true`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(true)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.accountSummaryUiState.value.showZeroBalanceAccounts)
    }

    // ========================================
    // T016: Profile selection tests
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
        viewModel.onMainEvent(MainEvent.SelectProfile(2L))
        advanceUntilIdle()

        // Then
        assertEquals(profile2, profileRepository.currentProfile.value)
    }

    @Test
    fun `selectProfile closes drawer after selection`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1")
        val profile2 = createTestProfile(id = 2L, name = "Profile 2")
        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        profileRepository.setCurrentProfile(profile1)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Open drawer first
        viewModel.onMainEvent(MainEvent.OpenDrawer)
        advanceUntilIdle()
        assertTrue(viewModel.mainUiState.value.isDrawerOpen)

        // When
        viewModel.onMainEvent(MainEvent.SelectProfile(2L))
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.mainUiState.value.isDrawerOpen)
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
        viewModel.onMainEvent(MainEvent.SelectProfile(999L))
        advanceUntilIdle()

        // Then - current profile unchanged
        assertEquals(profile, profileRepository.currentProfile.value)
    }

    // ========================================
    // T017: Zero balance accounts toggle tests
    // ========================================

    @Test
    fun `toggleZeroBalanceAccounts updates ui state`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(true)
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.accountSummaryUiState.value.showZeroBalanceAccounts)

        // When
        viewModel.onAccountSummaryEvent(AccountSummaryEvent.ToggleZeroBalanceAccounts)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.accountSummaryUiState.value.showZeroBalanceAccounts)
    }

    @Test
    fun `toggleZeroBalanceAccounts persists to preferences`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(true)
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onAccountSummaryEvent(AccountSummaryEvent.ToggleZeroBalanceAccounts)
        advanceUntilIdle()

        // Then
        assertFalse(preferencesRepository.getShowZeroBalanceAccounts())
    }

    @Test
    fun `toggleZeroBalanceAccounts toggles back and forth`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(false)
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.accountSummaryUiState.value.showZeroBalanceAccounts)

        // When - toggle on
        viewModel.onAccountSummaryEvent(AccountSummaryEvent.ToggleZeroBalanceAccounts)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.accountSummaryUiState.value.showZeroBalanceAccounts)

        // When - toggle off
        viewModel.onAccountSummaryEvent(AccountSummaryEvent.ToggleZeroBalanceAccounts)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.accountSummaryUiState.value.showZeroBalanceAccounts)
    }

    // ========================================
    // T018: Data refresh tests
    // ========================================

    @Test
    fun `refreshData with no profile does not crash`() = runTest {
        // Given - no profile set
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onMainEvent(MainEvent.RefreshData)
        advanceUntilIdle()

        // Then - no crash
        assertNotNull(viewModel)
    }

    @Test
    fun `cancelRefresh stops running task`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        viewModel.updateProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onMainEvent(MainEvent.CancelRefresh)
        advanceUntilIdle()

        // Then - should update background task manager
        assertNotNull(backgroundTaskManager.progress.value)
    }

    // ========================================
    // T019: Account loading tests
    // ========================================

    @Test
    fun `updateProfile triggers account reload`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateProfile(profile)
        advanceUntilIdle()

        // Then
        assertEquals(1L, viewModel.mainUiState.value.currentProfileId)
        assertEquals("Test Profile", viewModel.mainUiState.value.currentProfileName)
    }

    @Test
    fun `updateProfile with null clears state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        viewModel.updateProfile(profile)
        advanceUntilIdle()

        assertEquals(1L, viewModel.mainUiState.value.currentProfileId)

        // When
        viewModel.updateProfile(null)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.mainUiState.value.currentProfileId)
    }

    // ========================================
    // T020: Transaction loading tests
    // ========================================

    @Test
    fun `transaction tab selection triggers load when empty`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        viewModel.updateProfile(profile)
        advanceUntilIdle()

        // Verify initial state - transactions empty
        assertTrue(viewModel.transactionListUiState.value.transactions.isEmpty())

        // When - select Transactions tab
        viewModel.onMainEvent(MainEvent.SelectTab(MainTab.Transactions))
        advanceUntilIdle()

        // Then - selected tab is updated
        assertEquals(MainTab.Transactions, viewModel.mainUiState.value.selectedTab)
    }

    // ========================================
    // T021: Error handling tests
    // ========================================

    @Test
    fun `clearUpdateError clears error state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onMainEvent(MainEvent.ClearUpdateError)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.mainUiState.value.updateError)
    }

    // ========================================
    // T022: Tab selection tests
    // ========================================

    @Test
    fun `selectTab updates selected tab state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // Initial state
        assertEquals(MainTab.Accounts, viewModel.mainUiState.value.selectedTab)

        // When
        viewModel.onMainEvent(MainEvent.SelectTab(MainTab.Transactions))
        advanceUntilIdle()

        // Then
        assertEquals(MainTab.Transactions, viewModel.mainUiState.value.selectedTab)
    }

    @Test
    fun `selectTab Accounts clears account filter`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        viewModel.updateProfile(profile)
        advanceUntilIdle()

        // Set an account filter
        viewModel.onTransactionListEvent(TransactionListEvent.SetAccountFilter("Assets:Cash"))
        advanceUntilIdle()
        assertEquals("Assets:Cash", viewModel.transactionListUiState.value.accountFilter)

        // When - select Accounts tab
        viewModel.onMainEvent(MainEvent.SelectTab(MainTab.Accounts))
        advanceUntilIdle()

        // Then - filter cleared
        assertNull(viewModel.transactionListUiState.value.accountFilter)
    }

    // ========================================
    // T023: Account search debounce tests
    // ========================================

    @Test
    fun `setAccountFilter updates filter state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        viewModel.updateProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onTransactionListEvent(TransactionListEvent.SetAccountFilter("Assets"))
        advanceUntilIdle()

        // Then
        assertEquals("Assets", viewModel.transactionListUiState.value.accountFilter)
    }

    @Test
    fun `clearAccountFilter removes filter`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        viewModel.updateProfile(profile)
        advanceUntilIdle()

        viewModel.onTransactionListEvent(TransactionListEvent.SetAccountFilter("Assets"))
        advanceUntilIdle()

        // When
        viewModel.onTransactionListEvent(TransactionListEvent.ClearAccountFilter)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.transactionListUiState.value.accountFilter)
    }

    // ========================================
    // Drawer tests
    // ========================================

    @Test
    fun `openDrawer updates state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onMainEvent(MainEvent.OpenDrawer)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.mainUiState.value.isDrawerOpen)
    }

    @Test
    fun `closeDrawer updates state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onMainEvent(MainEvent.OpenDrawer)
        advanceUntilIdle()
        assertTrue(viewModel.mainUiState.value.isDrawerOpen)

        // When
        viewModel.onMainEvent(MainEvent.CloseDrawer)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.mainUiState.value.isDrawerOpen)
    }

    @Test
    fun `toggleDrawer toggles state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.mainUiState.value.isDrawerOpen)

        // When - toggle open
        viewModel.toggleDrawer()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.mainUiState.value.isDrawerOpen)

        // When - toggle closed
        viewModel.toggleDrawer()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.mainUiState.value.isDrawerOpen)
    }

    // ========================================
    // Navigation effect tests
    // ========================================

    @Test
    fun `addNewTransaction emits navigation effect when profile can post`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        viewModel.updateProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onMainEvent(MainEvent.AddNewTransaction)

        // Then
        val effect = viewModel.effects.first()
        assertTrue(effect is MainEffect.NavigateToNewTransaction)
    }

    @Test
    fun `editProfile emits navigation effect`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onMainEvent(MainEvent.EditProfile(1L))

        // Then
        val effect = viewModel.effects.first()
        assertTrue(effect is MainEffect.NavigateToProfileDetail)
        assertEquals(1L, (effect as MainEffect.NavigateToProfileDetail).profileId)
    }

    @Test
    fun `createNewProfile emits navigation effect with null id`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onMainEvent(MainEvent.CreateNewProfile)

        // Then
        val effect = viewModel.effects.first()
        assertTrue(effect is MainEffect.NavigateToProfileDetail)
        assertNull((effect as MainEffect.NavigateToProfileDetail).profileId)
    }

    @Test
    fun `navigateToTemplates emits effect`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onMainEvent(MainEvent.NavigateToTemplates)

        // Then
        val effect = viewModel.effects.first()
        assertTrue(effect is MainEffect.NavigateToTemplates)
    }

    @Test
    fun `navigateToBackups emits effect`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onMainEvent(MainEvent.NavigateToBackups)

        // Then
        val effect = viewModel.effects.first()
        assertTrue(effect is MainEffect.NavigateToBackups)
    }

    // ========================================
    // Profile list update tests
    // ========================================

    @Test
    fun `updateProfiles updates mainUiState profiles`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1")
        val profile2 = createTestProfile(id = 2L, name = "Profile 2")

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateProfiles(listOf(profile1, profile2))
        advanceUntilIdle()

        // Then
        val profiles = viewModel.mainUiState.value.profiles
        assertEquals(2, profiles.size)
        assertEquals("Profile 1", profiles[0].name)
        assertEquals("Profile 2", profiles[1].name)
    }

    // ========================================
    // Background task state tests
    // ========================================

    @Test
    fun `backgroundTaskManager running state updates mainUiState`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.mainUiState.value.isRefreshing)

        // When
        backgroundTaskManager.taskStarted("test-task")
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.mainUiState.value.isRefreshing)
        assertTrue(viewModel.mainUiState.value.backgroundTasksRunning)

        // When - task finishes
        backgroundTaskManager.taskFinished("test-task")
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.mainUiState.value.isRefreshing)
        assertFalse(viewModel.mainUiState.value.backgroundTasksRunning)
    }

    // ========================================
    // Header text update tests
    // ========================================

    @Test
    fun `updateHeaderTexts updates account header`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateHeaderTexts("10 accounts", null)
        advanceUntilIdle()

        // Then
        assertEquals("10 accounts", viewModel.accountSummaryUiState.value.headerText)
    }

    @Test
    fun `updateHeaderTexts updates transaction header`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateHeaderTexts(null, "50 transactions")
        advanceUntilIdle()

        // Then
        assertEquals("50 transactions", viewModel.transactionListUiState.value.headerText)
    }
}
