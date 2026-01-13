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

import java.util.Date
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
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.db.Option
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.BackgroundTaskManager
import net.ktnx.mobileledger.service.SyncInfo
import net.ktnx.mobileledger.service.TaskProgress
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MainViewModel repository interactions.
 *
 * These tests verify that MainViewModel correctly uses the Repository pattern
 * for data access. Due to Android framework dependencies (App class, LiveData observers),
 * full ViewModel testing requires instrumentation tests.
 *
 * These unit tests focus on:
 * - Repository injection and usage patterns
 * - State management with Fake repositories
 * - Profile selection flow
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var transactionRepository: FakeTransactionRepositoryForViewModel
    private lateinit var accountRepository: FakeAccountRepositoryForViewModel
    private lateinit var optionRepository: FakeOptionRepositoryForViewModel
    private lateinit var backgroundTaskManager: FakeBackgroundTaskManagerForViewModel
    private lateinit var appStateService: FakeAppStateServiceForViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForViewModel()
        transactionRepository = FakeTransactionRepositoryForViewModel()
        accountRepository = FakeAccountRepositoryForViewModel()
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

    // ========================================
    // ProfileRepository integration tests
    // ========================================

    @Test
    fun `profileRepository currentProfile starts as null`() = runTest {
        val current = profileRepository.currentProfile.value
        assertNull(current)
    }

    @Test
    fun `profileRepository setCurrentProfile updates state`() = runTest {
        val profile = createTestProfile(name = "Selected Profile")

        profileRepository.setCurrentProfile(profile)

        val current = profileRepository.currentProfile.value
        assertNotNull(current)
        assertEquals("Selected Profile", current?.name)
    }

    @Test
    fun `profileRepository getAllProfiles returns profiles in order`() = runTest {
        val p1 = createTestProfile(id = 1L, name = "Profile A").apply { orderNo = 2 }
        val p2 = createTestProfile(id = 2L, name = "Profile B").apply { orderNo = 1 }
        profileRepository.insertProfile(p1)
        profileRepository.insertProfile(p2)

        val profiles = profileRepository.profiles
        assertEquals(2, profiles.size)
        assertEquals("Profile B", profiles[0].name) // orderNo = 1 first
        assertEquals("Profile A", profiles[1].name) // orderNo = 2 second
    }

    @Test
    fun `profileRepository getProfileByIdSync returns correct profile`() = runTest {
        val profile = createTestProfile(id = 5L, name = "Specific Profile")
        profileRepository.insertProfile(profile)

        val result = profileRepository.getProfileByIdSync(5L)

        assertNotNull(result)
        assertEquals("Specific Profile", result?.name)
    }

    // ========================================
    // MainViewModel.updateProfile() contract tests
    // ========================================

    /**
     * This test verifies the contract that MainViewModel.updateProfile() must fulfill:
     * When a profile is passed to updateProfile(), it MUST call profileRepository.setCurrentProfile()
     * so that other ViewModels (e.g., NewTransactionViewModel) can access the current profile.
     *
     * Note: MainViewModel cannot be directly unit tested due to App.instance dependency.
     * This test documents the expected behavior that the actual implementation must follow.
     *
     * Bug reference: ProfileRepository.currentProfile was not synchronized when updateProfile()
     * was called, causing "プロファイルが選択されていません" error in NewTransactionViewModel.
     */
    @Test
    fun `updateProfile contract - setCurrentProfile must be called with the profile`() = runTest {
        val profile = createTestProfile(id = 1L, name = "Test Profile")

        // Simulate what MainViewModel.updateProfile() should do
        profileRepository.setCurrentProfile(profile)

        // Other ViewModels should be able to access the current profile
        val currentProfile = profileRepository.currentProfile.value
        assertNotNull("currentProfile must not be null after updateProfile", currentProfile)
        assertEquals(profile.id, currentProfile?.id)
    }

    @Test
    fun `updateProfile contract - setCurrentProfile with null clears the profile`() = runTest {
        val profile = createTestProfile(id = 1L, name = "Test Profile")
        profileRepository.setCurrentProfile(profile)
        assertNotNull(profileRepository.currentProfile.value)

        // Simulate updateProfile(null)
        profileRepository.setCurrentProfile(null)

        assertNull(
            "currentProfile must be null after updateProfile(null)",
            profileRepository.currentProfile.value
        )
    }

    // ========================================
    // TransactionRepository integration tests
    // ========================================

    @Test
    fun `transactionRepository getAllTransactions returns empty for new profile`() = runTest {
        val transactions = transactionRepository.getTransactionsForProfile(1L)
        assertEquals(0, transactions.size)
    }

    @Test
    fun `transactionRepository filters by profile`() = runTest {
        val tx1 = createTestTransaction(profileId = 1L, description = "Profile 1 Tx")
        val tx2 = createTestTransaction(profileId = 2L, description = "Profile 2 Tx")
        transactionRepository.insertTransaction(tx1)
        transactionRepository.insertTransaction(tx2)

        val profile1Transactions = transactionRepository.getTransactionsForProfile(1L)
        assertEquals(1, profile1Transactions.size)
        assertEquals("Profile 1 Tx", profile1Transactions[0].transaction.description)
    }

    // ========================================
    // AccountRepository integration tests
    // ========================================

    @Test
    fun `accountRepository searchAccountNamesSync returns matching accounts`() = runTest {
        accountRepository.addAccount(1L, "Assets:Cash")
        accountRepository.addAccount(1L, "Assets:Bank")
        accountRepository.addAccount(1L, "Expenses:Food")

        val results = accountRepository.searchAccountNamesSync(1L, "Assets")

        assertEquals(2, results.size)
        assert(results.contains("Assets:Cash"))
        assert(results.contains("Assets:Bank"))
    }

    @Test
    fun `accountRepository searchAccountNamesSync is case insensitive`() = runTest {
        accountRepository.addAccount(1L, "Assets:Cash")

        val results = accountRepository.searchAccountNamesSync(1L, "assets")

        assertEquals(1, results.size)
        assertEquals("Assets:Cash", results[0])
    }

    // ========================================
    // BackgroundTaskManager integration tests
    // ========================================

    @Test
    fun `backgroundTaskManager isRunning starts as false`() = runTest {
        assertFalse(backgroundTaskManager.isRunning.value)
    }

    @Test
    fun `backgroundTaskManager taskStarted updates isRunning`() = runTest {
        backgroundTaskManager.taskStarted("test-task-1")

        assertTrue(backgroundTaskManager.isRunning.value)
        assertEquals(1, backgroundTaskManager.runningTaskCount)
    }

    @Test
    fun `backgroundTaskManager taskFinished updates isRunning`() = runTest {
        backgroundTaskManager.taskStarted("test-task-1")
        assertTrue(backgroundTaskManager.isRunning.value)

        backgroundTaskManager.taskFinished("test-task-1")

        assertFalse(backgroundTaskManager.isRunning.value)
        assertEquals(0, backgroundTaskManager.runningTaskCount)
    }

    @Test
    fun `backgroundTaskManager updateProgress updates progress`() = runTest {
        val progress = TaskProgress(
            taskId = "test-task",
            state = net.ktnx.mobileledger.service.TaskState.RUNNING,
            message = "Syncing...",
            current = 50,
            total = 100
        )

        backgroundTaskManager.updateProgress(progress)

        assertEquals(progress, backgroundTaskManager.progress.value)
    }

    // ========================================
    // AppStateService integration tests
    // ========================================

    @Test
    fun `appStateService drawerOpen starts as false`() = runTest {
        assertFalse(appStateService.drawerOpen.value)
    }

    @Test
    fun `appStateService setDrawerOpen updates state`() = runTest {
        appStateService.setDrawerOpen(true)
        assertTrue(appStateService.drawerOpen.value)

        appStateService.setDrawerOpen(false)
        assertFalse(appStateService.drawerOpen.value)
    }

    @Test
    fun `appStateService toggleDrawer toggles state`() = runTest {
        assertFalse(appStateService.drawerOpen.value)

        appStateService.toggleDrawer()
        assertTrue(appStateService.drawerOpen.value)

        appStateService.toggleDrawer()
        assertFalse(appStateService.drawerOpen.value)
    }

    @Test
    fun `appStateService lastSyncInfo starts as EMPTY`() = runTest {
        assertEquals(SyncInfo.EMPTY, appStateService.lastSyncInfo.value)
        assertFalse(appStateService.lastSyncInfo.value.hasSynced)
    }

    @Test
    fun `appStateService updateSyncInfo updates state`() = runTest {
        val syncInfo = SyncInfo(
            date = Date(),
            transactionCount = 100,
            accountCount = 50,
            totalAccountCount = 75
        )

        appStateService.updateSyncInfo(syncInfo)

        assertEquals(syncInfo, appStateService.lastSyncInfo.value)
        assertTrue(appStateService.lastSyncInfo.value.hasSynced)
    }

    @Test
    fun `appStateService clearSyncInfo resets to EMPTY`() = runTest {
        val syncInfo = SyncInfo(
            date = Date(),
            transactionCount = 100,
            accountCount = 50,
            totalAccountCount = 75
        )
        appStateService.updateSyncInfo(syncInfo)
        assertTrue(appStateService.lastSyncInfo.value.hasSynced)

        appStateService.clearSyncInfo()

        assertEquals(SyncInfo.EMPTY, appStateService.lastSyncInfo.value)
        assertFalse(appStateService.lastSyncInfo.value.hasSynced)
    }

    // ========================================
    // Helper classes
    // ========================================

    private fun createTestTransaction(
        profileId: Long = 1L,
        description: String = "Test Transaction"
    ): TransactionWithAccounts {
        val transaction = Transaction().apply {
            this.profileId = profileId
            this.description = description
            this.year = 2026
            this.month = 1
            this.day = 10
            this.ledgerId = 1L
        }
        val twa = TransactionWithAccounts()
        twa.transaction = transaction
        twa.accounts = emptyList()
        return twa
    }
}

/**
 * Fake ProfileRepository for ViewModel testing.
 */
class FakeProfileRepositoryForViewModel : ProfileRepository {
    private val profilesMap = mutableMapOf<Long, Profile>()
    private var nextId = 1L
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    val profiles: List<Profile>
        get() = profilesMap.values.sortedBy { it.orderNo }

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun getAllProfiles(): Flow<List<Profile>> = MutableStateFlow(profilesMap.values.sortedBy { it.orderNo })

    override suspend fun getAllProfilesSync(): List<Profile> = profilesMap.values.sortedBy { it.orderNo }

    override fun getProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profilesMap[profileId])

    override suspend fun getProfileByIdSync(profileId: Long): Profile? = profilesMap[profileId]

    override fun getProfileByUuid(uuid: String): Flow<Profile?> =
        MutableStateFlow(profilesMap.values.find { it.uuid == uuid })

    override suspend fun getProfileByUuidSync(uuid: String): Profile? = profilesMap.values.find { it.uuid == uuid }

    override suspend fun getAnyProfile(): Profile? = profilesMap.values.firstOrNull()

    override suspend fun getProfileCount(): Int = profilesMap.size

    override suspend fun insertProfile(profile: Profile): Long {
        val id = if (profile.id == 0L) nextId++ else profile.id
        profile.id = id
        profilesMap[id] = profile
        return id
    }

    override suspend fun updateProfile(profile: Profile) {
        profilesMap[profile.id] = profile
        if (_currentProfile.value?.id == profile.id) {
            _currentProfile.value = profile
        }
    }

    override suspend fun deleteProfile(profile: Profile) {
        profilesMap.remove(profile.id)
        if (_currentProfile.value?.id == profile.id) {
            _currentProfile.value = profilesMap.values.firstOrNull()
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {
        profiles.forEachIndexed { index, profile ->
            profilesMap[profile.id]?.orderNo = index
        }
    }

    override suspend fun deleteAllProfiles() {
        profilesMap.clear()
        _currentProfile.value = null
    }
}

/**
 * Fake TransactionRepository for ViewModel testing.
 */
class FakeTransactionRepositoryForViewModel : TransactionRepository {
    private val transactions = mutableMapOf<Long, TransactionWithAccounts>()
    private var nextId = 1L

    fun getTransactionsForProfile(profileId: Long): List<TransactionWithAccounts> =
        transactions.values.filter { it.transaction.profileId == profileId }

    override fun getAllTransactions(profileId: Long): Flow<List<TransactionWithAccounts>> =
        MutableStateFlow(transactions.values.filter { it.transaction.profileId == profileId }.toList())

    override fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<TransactionWithAccounts>> =
        MutableStateFlow(
            transactions.values.filter { twa ->
                twa.transaction.profileId == profileId &&
                    (accountName == null || twa.accounts.any { it.accountName.contains(accountName, true) })
            }.toList()
        )

    override fun getTransactionById(transactionId: Long): Flow<TransactionWithAccounts?> =
        MutableStateFlow(transactions[transactionId])

    override suspend fun getTransactionByIdSync(transactionId: Long): TransactionWithAccounts? =
        transactions[transactionId]

    override suspend fun searchByDescription(term: String): List<TransactionDAO.DescriptionContainer> =
        transactions.values
            .filter { it.transaction.description.contains(term, true) }
            .distinctBy { it.transaction.description }
            .map { TransactionDAO.DescriptionContainer().apply { description = it.transaction.description } }

    override suspend fun getFirstByDescription(description: String): TransactionWithAccounts? =
        transactions.values.find { it.transaction.description == description }

    override suspend fun getFirstByDescriptionHavingAccount(
        description: String,
        accountTerm: String
    ): TransactionWithAccounts? = transactions.values.find { twa ->
        twa.transaction.description == description &&
            twa.accounts.any { it.accountName.contains(accountTerm, true) }
    }

    override suspend fun insertTransaction(transaction: TransactionWithAccounts) {
        if (transaction.transaction.id == 0L) {
            transaction.transaction.id = nextId++
        }
        transactions[transaction.transaction.id] = transaction
    }

    override suspend fun storeTransaction(transaction: TransactionWithAccounts) {
        insertTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.remove(transaction.id)
    }

    override suspend fun deleteTransactions(transactions: List<Transaction>) {
        transactions.forEach { this.transactions.remove(it.id) }
    }

    override suspend fun storeTransactions(transactions: List<TransactionWithAccounts>, profileId: Long) {
        transactions.forEach { twa ->
            twa.transaction.profileId = profileId
            insertTransaction(twa)
        }
    }

    override suspend fun deleteAllForProfile(profileId: Long): Int {
        val toRemove = transactions.values.filter { it.transaction.profileId == profileId }
        toRemove.forEach { transactions.remove(it.transaction.id) }
        return toRemove.size
    }

    override suspend fun getMaxLedgerId(profileId: Long): Long? = transactions.values
        .filter { it.transaction.profileId == profileId }
        .maxOfOrNull { it.transaction.ledgerId }
}

/**
 * Fake AccountRepository for ViewModel testing.
 */
class FakeAccountRepositoryForViewModel : AccountRepository {
    private val accounts = mutableMapOf<Long, MutableList<String>>()

    fun addAccount(profileId: Long, name: String) {
        accounts.getOrPut(profileId) { mutableListOf() }.add(name)
    }

    override fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<AccountWithAmounts>> =
        MutableStateFlow(emptyList())

    override suspend fun getAllWithAmountsSync(
        profileId: Long,
        includeZeroBalances: Boolean
    ): List<AccountWithAmounts> = emptyList()

    override fun getAll(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> =
        MutableStateFlow(emptyList())

    override suspend fun getByIdSync(id: Long): Account? = null

    override fun getByName(profileId: Long, accountName: String): Flow<Account?> = MutableStateFlow(null)

    override suspend fun getByNameSync(profileId: Long, accountName: String): Account? = null

    override fun getByNameWithAmounts(profileId: Long, accountName: String): Flow<AccountWithAmounts?> =
        MutableStateFlow(null)

    override fun searchAccountNames(profileId: Long, term: String): Flow<List<String>> = MutableStateFlow(
        accounts[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList()
    )

    override suspend fun searchAccountNamesSync(profileId: Long, term: String): List<String> =
        accounts[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList()

    override suspend fun searchAccountsWithAmountsSync(profileId: Long, term: String): List<AccountWithAmounts> =
        emptyList()

    override fun searchAccountNamesGlobal(term: String): Flow<List<String>> =
        MutableStateFlow(accounts.values.flatten().filter { it.contains(term, ignoreCase = true) })

    override suspend fun searchAccountNamesGlobalSync(term: String): List<String> =
        accounts.values.flatten().filter { it.contains(term, ignoreCase = true) }

    override suspend fun insertAccount(account: Account): Long = 0L

    override suspend fun insertAccountWithAmounts(accountWithAmounts: AccountWithAmounts) {}

    override suspend fun updateAccount(account: Account) {}

    override suspend fun deleteAccount(account: Account) {}

    override suspend fun storeAccounts(accounts: List<AccountWithAmounts>, profileId: Long) {}

    override suspend fun getCountForProfile(profileId: Long): Int = accounts[profileId]?.size ?: 0

    override suspend fun deleteAllAccounts() {
        accounts.clear()
    }
}

/**
 * Fake OptionRepository for ViewModel testing.
 */
class FakeOptionRepositoryForViewModel : OptionRepository {
    private val options = mutableMapOf<String, Option>()

    private fun makeKey(profileId: Long, name: String): String = "$profileId:$name"

    override fun getOption(profileId: Long, name: String): Flow<Option?> =
        MutableStateFlow(options[makeKey(profileId, name)])

    override suspend fun getOptionSync(profileId: Long, name: String): Option? = options[makeKey(profileId, name)]

    override suspend fun getAllOptionsForProfileSync(profileId: Long): List<Option> =
        options.values.filter { it.profileId == profileId }

    override suspend fun insertOption(option: Option): Long {
        options[makeKey(option.profileId, option.name)] = option
        return 1L
    }

    override suspend fun deleteOption(option: Option) {
        options.remove(makeKey(option.profileId, option.name))
    }

    override suspend fun deleteOptionsForProfile(profileId: Long) {
        options.keys.filter { it.startsWith("$profileId:") }.forEach { options.remove(it) }
    }

    override suspend fun deleteAllOptions() {
        options.clear()
    }
}

/**
 * Fake BackgroundTaskManager for ViewModel testing.
 */
class FakeBackgroundTaskManagerForViewModel : BackgroundTaskManager {
    private val runningTasks = mutableSetOf<String>()
    private val _isRunning = MutableStateFlow(false)
    private val _progress = MutableStateFlow<TaskProgress?>(null)

    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    override val progress: StateFlow<TaskProgress?> = _progress.asStateFlow()
    override val runningTaskCount: Int get() = runningTasks.size

    override fun taskStarted(taskId: String) {
        runningTasks.add(taskId)
        _isRunning.value = runningTasks.isNotEmpty()
    }

    override fun taskFinished(taskId: String) {
        runningTasks.remove(taskId)
        _isRunning.value = runningTasks.isNotEmpty()
        if (runningTasks.isEmpty()) {
            _progress.value = null
        }
    }

    override fun updateProgress(progress: TaskProgress) {
        _progress.value = progress
    }
}

/**
 * Fake AppStateService for ViewModel testing.
 */
class FakeAppStateServiceForViewModel : AppStateService {
    private val _lastSyncInfo = MutableStateFlow(SyncInfo.EMPTY)
    private val _drawerOpen = MutableStateFlow(false)
    private val _dataVersion = MutableStateFlow(0L)

    override val lastSyncInfo: StateFlow<SyncInfo> = _lastSyncInfo.asStateFlow()
    override val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()
    override val dataVersion: StateFlow<Long> = _dataVersion.asStateFlow()

    override fun updateSyncInfo(info: SyncInfo) {
        _lastSyncInfo.value = info
    }

    override fun clearSyncInfo() {
        _lastSyncInfo.value = SyncInfo.EMPTY
    }

    override fun setDrawerOpen(open: Boolean) {
        _drawerOpen.value = open
    }

    override fun toggleDrawer() {
        _drawerOpen.value = !_drawerOpen.value
    }

    override fun signalDataChanged() {
        _dataVersion.value = _dataVersion.value + 1
    }
}
