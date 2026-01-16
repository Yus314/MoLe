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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.PreferencesRepository
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountValue
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AccountSummaryViewModel.
 *
 * Tests cover:
 * - Account list loading
 * - Zero-balance filter toggle
 * - Account expansion state
 * - Amounts expansion state
 * - Observing profile changes
 * - Error handling
 *
 * Following TDD: These tests are written FIRST and should FAIL
 * until AccountSummaryViewModel is implemented.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountSummaryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var accountRepository: FakeAccountRepositoryForAccountSummary
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var viewModel: AccountSummaryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForViewModel()
        accountRepository = FakeAccountRepositoryForAccountSummary()
        preferencesRepository = FakePreferencesRepository()
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
    ): Profile = createTestDomainProfile(
        id = id,
        name = name,
        theme = theme,
        orderNo = orderNo,
        permitPosting = true
    )

    private fun createTestAccount(
        id: Long,
        profileId: Long,
        name: String,
        parentName: String? = null,
        amount: Float = 0f,
        currency: String = "",
        expanded: Boolean = true,
        amountsExpanded: Boolean = false
    ): AccountWithAmounts {
        val account = Account().apply {
            this.id = id
            this.profileId = profileId
            this.name = name
            this.nameUpper = name.uppercase()
            this.parentName = parentName
            this.level = name.count { it == ':' }
            this.expanded = expanded
            this.amountsExpanded = amountsExpanded
        }
        val amounts = if (amount != 0f || currency.isNotEmpty()) {
            listOf(
                AccountValue().apply {
                    this.id = id
                    this.accountId = id
                    this.value = amount
                    this.currency = currency
                }
            )
        } else {
            emptyList()
        }
        return AccountWithAmounts().apply {
            this.account = account
            this.amounts = amounts
        }
    }

    private fun createViewModel() = AccountSummaryViewModel(
        profileRepository,
        accountRepository,
        preferencesRepository
    )

    // ========================================
    // Init tests
    // ========================================

    @Test
    fun `init loads showZeroBalanceAccounts from preferences`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(false)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showZeroBalanceAccounts)
    }

    @Test
    fun `init loads accounts for current profile`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100f, "USD"))
        accountRepository.addAccountWithAmounts(createTestAccount(2L, 1L, "Assets:Bank", null, 500f, "USD"))

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - first check for errors
        val error = viewModel.uiState.value.error
        assertNull("Unexpected error: $error", error)

        val accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertEquals(2, accounts.size)
    }

    @Test
    fun `init with no profile results in empty accounts`() = runTest {
        // Given - no profile set

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertTrue(accounts.isEmpty())
    }

    // ========================================
    // Profile change tests
    // ========================================

    @Test
    fun `profileChange reloads accounts`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1")
        val profile2 = createTestProfile(id = 2L, name = "Profile 2")
        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        profileRepository.setCurrentProfile(profile1)

        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100f, "USD"))
        accountRepository.addAccountWithAmounts(createTestAccount(2L, 2L, "Liabilities:Credit", null, -500f, "USD"))

        viewModel = createViewModel()
        advanceUntilIdle()

        // Verify initial state
        var accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertEquals(1, accounts.size)
        assertEquals("Assets:Cash", accounts[0].name)

        // When - change profile
        profileRepository.setCurrentProfile(profile2)
        advanceUntilIdle()

        // Then
        accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertEquals(1, accounts.size)
        assertEquals("Liabilities:Credit", accounts[0].name)
    }

    // ========================================
    // ToggleZeroBalanceAccounts tests
    // ========================================

    @Test
    fun `toggleZeroBalanceAccounts updates preferences`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(true)
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showZeroBalanceAccounts)

        // When
        viewModel.onEvent(AccountSummaryEvent.ToggleZeroBalanceAccounts)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showZeroBalanceAccounts)
        assertFalse(preferencesRepository.getShowZeroBalanceAccounts())
    }

    @Test
    fun `toggleZeroBalanceAccounts reloads accounts`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(true)
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100f, "USD"))
        accountRepository.addAccountWithAmounts(createTestAccount(2L, 1L, "Assets:Empty", null, 0f, ""))

        viewModel = createViewModel()
        advanceUntilIdle()

        // Verify accounts loaded with zero balances shown
        var accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertEquals(2, accounts.size)

        // When - toggle to hide zero balances
        viewModel.onEvent(AccountSummaryEvent.ToggleZeroBalanceAccounts)
        advanceUntilIdle()

        // Then - zero balance accounts filtered out
        accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertEquals(1, accounts.size)
        assertEquals("Assets:Cash", accounts[0].name)
    }

    // ========================================
    // ToggleAccountExpanded tests
    // ========================================

    @Test
    fun `toggleAccountExpanded updates state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets", null, 100f, "USD"))
        accountRepository.addAccountWithAmounts(createTestAccount(2L, 1L, "Assets:Cash", "Assets", 50f, "USD"))

        viewModel = createViewModel()
        advanceUntilIdle()

        val initialAccount = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .find { it.id == 1L }
        assertTrue(initialAccount?.isExpanded ?: false) // Default is expanded

        // When
        viewModel.onEvent(AccountSummaryEvent.ToggleAccountExpanded(1L))
        advanceUntilIdle()

        // Then
        val updatedAccount = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .find { it.id == 1L }
        assertFalse(updatedAccount?.isExpanded ?: true)
    }

    @Test
    fun `toggleAccountExpanded with invalid id does nothing`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100f, "USD"))

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(AccountSummaryEvent.ToggleAccountExpanded(999L))
        advanceUntilIdle()

        // Then - no crash, state unchanged
        val account = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .find { it.id == 1L }
        assertNotNull(account)
    }

    // ========================================
    // ToggleAmountsExpanded tests
    // ========================================

    @Test
    fun `toggleAmountsExpanded updates state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100f, "USD"))

        viewModel = createViewModel()
        advanceUntilIdle()

        val initialAccount = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .find { it.id == 1L }
        assertFalse(initialAccount?.amountsExpanded ?: true) // Default is collapsed

        // When
        viewModel.onEvent(AccountSummaryEvent.ToggleAmountsExpanded(1L))
        advanceUntilIdle()

        // Then
        val updatedAccount = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .find { it.id == 1L }
        assertTrue(updatedAccount?.amountsExpanded ?: false)
    }

    // ========================================
    // ShowAccountTransactions tests
    // ========================================

    @Test
    fun `showAccountTransactions emits effect`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(AccountSummaryEvent.ShowAccountTransactions("Assets:Cash"))

        // Then - effect should be emitted
        val effect = viewModel.effects.first()
        assertTrue(effect is AccountSummaryEffect.ShowAccountTransactions)
        assertEquals("Assets:Cash", (effect as AccountSummaryEffect.ShowAccountTransactions).accountName)
    }

    // ========================================
    // Loading state tests
    // ========================================

    @Test
    fun `loadAccounts clears loading state on completion`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ========================================
    // Error state tests
    // ========================================

    @Test
    fun `loadAccounts error shows error state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        accountRepository.simulateError = true

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
    }

    // ========================================
    // Header text tests
    // ========================================

    @Test
    fun `updateHeaderText updates header in state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateHeaderText("Last sync: 10 accounts")
        advanceUntilIdle()

        // Then
        assertEquals("Last sync: 10 accounts", viewModel.uiState.value.headerText)
    }
}

/**
 * Fake AccountRepository specialized for AccountSummaryViewModel tests.
 * Implements AccountRepository directly to support account with amounts storage.
 */
class FakeAccountRepositoryForAccountSummary : AccountRepository {
    private val accountsWithAmounts = mutableMapOf<Long, MutableList<AccountWithAmounts>>()
    private val accountNames = mutableMapOf<Long, MutableList<String>>()
    var simulateError = false

    fun addAccountWithAmounts(accountWithAmounts: AccountWithAmounts) {
        val profileId = accountWithAmounts.account.profileId
        accountsWithAmounts.getOrPut(profileId) { mutableListOf() }.add(accountWithAmounts)
        accountNames.getOrPut(profileId) { mutableListOf() }.add(accountWithAmounts.account.name)
    }

    override suspend fun getAllWithAmountsSync(
        profileId: Long,
        includeZeroBalances: Boolean
    ): List<AccountWithAmounts> {
        if (simulateError) {
            throw RuntimeException("Simulated error")
        }
        val accounts = accountsWithAmounts[profileId] ?: emptyList()
        return if (includeZeroBalances) {
            accounts
        } else {
            accounts.filter { acc ->
                acc.amounts.any { it.value != 0f }
            }
        }
    }

    override suspend fun getCountForProfile(profileId: Long): Int = accountsWithAmounts[profileId]?.size ?: 0

    // Unused methods - implement with defaults
    override fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean) =
        MutableStateFlow(accountsWithAmounts[profileId] ?: emptyList())

    override fun getAll(profileId: Long, includeZeroBalances: Boolean) = MutableStateFlow<List<Account>>(emptyList())

    override suspend fun getByIdSync(id: Long): Account? = null

    override fun getByName(profileId: Long, accountName: String) = MutableStateFlow<Account?>(null)

    override suspend fun getByNameSync(profileId: Long, accountName: String): Account? = null

    override fun getByNameWithAmounts(profileId: Long, accountName: String) =
        MutableStateFlow<AccountWithAmounts?>(null)

    override fun searchAccountNames(profileId: Long, term: String) =
        MutableStateFlow(accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList())

    override suspend fun searchAccountNamesSync(profileId: Long, term: String): List<String> =
        accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList()

    override suspend fun searchAccountsWithAmountsSync(profileId: Long, term: String): List<AccountWithAmounts> =
        emptyList()

    override fun searchAccountNamesGlobal(term: String) =
        MutableStateFlow(accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) })

    override suspend fun searchAccountNamesGlobalSync(term: String): List<String> =
        accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) }

    override suspend fun insertAccount(account: Account): Long = 0L

    override suspend fun insertAccountWithAmounts(accountWithAmounts: AccountWithAmounts) {}

    override suspend fun updateAccount(account: Account) {}

    override suspend fun deleteAccount(account: Account) {}

    override suspend fun storeAccounts(accounts: List<AccountWithAmounts>, profileId: Long) {}

    override suspend fun deleteAllAccounts() {
        accountsWithAmounts.clear()
        accountNames.clear()
    }
}

/**
 * Fake PreferencesRepository for ViewModel testing.
 */
class FakePreferencesRepository : PreferencesRepository {
    private var showZeroBalanceAccounts = true
    private var startupProfileId = -1L
    private var startupTheme = -1

    override fun getShowZeroBalanceAccounts(): Boolean = showZeroBalanceAccounts

    override fun setShowZeroBalanceAccounts(value: Boolean) {
        showZeroBalanceAccounts = value
    }

    override fun getStartupProfileId(): Long = startupProfileId

    override fun setStartupProfileId(profileId: Long) {
        startupProfileId = profileId
    }

    override fun getStartupTheme(): Int = startupTheme

    override fun setStartupTheme(theme: Int) {
        startupTheme = theme
    }
}
