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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.core.database.entity.Account as DbAccount
import net.ktnx.mobileledger.core.database.entity.AccountValue
import net.ktnx.mobileledger.core.database.entity.AccountWithAmounts
import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.domain.model.AccountAmount as DomainAccountAmount
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.repository.AccountRepository
import net.ktnx.mobileledger.core.domain.repository.PreferencesRepository
import net.ktnx.mobileledger.core.testing.fake.FakeProfileRepository
import net.ktnx.mobileledger.feature.account.usecase.AccountHierarchyResolverImpl
import net.ktnx.mobileledger.feature.account.usecase.GetShowZeroBalanceUseCaseImpl
import net.ktnx.mobileledger.feature.account.usecase.ObserveAccountsWithAmountsUseCaseImpl
import net.ktnx.mobileledger.feature.account.usecase.SetShowZeroBalanceUseCaseImpl
import net.ktnx.mobileledger.feature.profile.usecase.ObserveCurrentProfileUseCaseImpl
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
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var accountRepository: FakeAccountRepositoryForAccountSummary
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var viewModel: AccountSummaryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepository()
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
        val account = DbAccount().apply {
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
        observeCurrentProfileUseCase = ObserveCurrentProfileUseCaseImpl(profileRepository),
        observeAccountsWithAmountsUseCase = ObserveAccountsWithAmountsUseCaseImpl(accountRepository),
        getShowZeroBalanceUseCase = GetShowZeroBalanceUseCaseImpl(preferencesRepository),
        setShowZeroBalanceUseCase = SetShowZeroBalanceUseCaseImpl(preferencesRepository),
        accountHierarchyResolver = AccountHierarchyResolverImpl()
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
        profileRepository.insertProfile(profile).getOrThrow()
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
        profileRepository.insertProfile(profile1).getOrThrow()
        profileRepository.insertProfile(profile2).getOrThrow()
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
        profileRepository.insertProfile(profile).getOrThrow()
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
        profileRepository.insertProfile(profile).getOrThrow()
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
        profileRepository.insertProfile(profile).getOrThrow()
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
        profileRepository.insertProfile(profile).getOrThrow()
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
        profileRepository.insertProfile(profile).getOrThrow()
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
        profileRepository.insertProfile(profile).getOrThrow()
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
        profileRepository.insertProfile(profile).getOrThrow()
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
        profileRepository.insertProfile(profile).getOrThrow()
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
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateHeaderText("Last sync: 10 accounts")
        advanceUntilIdle()

        // Then
        assertEquals("Last sync: 10 accounts", viewModel.uiState.value.headerText)
    }

    // ========================================
    // Additional coverage tests
    // ========================================

    @Test
    fun `reloadAccounts reloads with current profile`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100f, "USD"))

        viewModel = createViewModel()
        advanceUntilIdle()

        // Add a new account after initial load
        accountRepository.addAccountWithAmounts(createTestAccount(2L, 1L, "Assets:Bank", null, 500f, "USD"))

        // When
        viewModel.reloadAccounts()
        advanceUntilIdle()

        // Then - should have both accounts
        val accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertEquals(2, accounts.size)
    }

    @Test
    fun `reloadAccounts does nothing when no profile`() = runTest {
        // Given - no profile set
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.reloadAccounts()
        advanceUntilIdle()

        // Then - no crash, empty list
        val accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertTrue(accounts.isEmpty())
    }

    @Test
    fun `updateHeaderText updates header item in list`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100f, "USD"))

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateHeaderText("Updated header")
        advanceUntilIdle()

        // Then - header item in list should be updated
        val headerItem = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Header>()
            .firstOrNull()
        assertNotNull(headerItem)
        assertEquals("Updated header", headerItem?.text)
    }

    @Test
    fun `toggleAmountsExpanded with invalid id does nothing`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100f, "USD"))

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(AccountSummaryEvent.ToggleAmountsExpanded(999L))
        advanceUntilIdle()

        // Then - no crash, state unchanged
        val account = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .find { it.id == 1L }
        assertNotNull(account)
        assertFalse(account!!.amountsExpanded)
    }

    @Test
    fun `profile cleared to null clears accounts`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100f, "USD"))

        viewModel = createViewModel()
        advanceUntilIdle()

        // Verify accounts loaded
        var accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertEquals(1, accounts.size)

        // When - clear profile
        profileRepository.setCurrentProfile(null)
        advanceUntilIdle()

        // Then - accounts cleared
        accounts = viewModel.uiState.value.accounts.filterIsInstance<AccountSummaryListItem.Account>()
        assertTrue(accounts.isEmpty())
    }

    @Test
    fun `accounts are formatted with currency`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 1234.56f, "EUR"))

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val account = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .firstOrNull()
        assertNotNull(account)
        assertEquals(1, account!!.amounts.size)
        assertTrue(account.amounts[0].formattedAmount.contains("EUR"))
    }

    @Test
    fun `accounts without currency format correctly`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        accountRepository.addAccountWithAmounts(createTestAccount(1L, 1L, "Assets:Cash", null, 100.50f, ""))

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val account = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .firstOrNull()
        assertNotNull(account)
        assertEquals(1, account!!.amounts.size)
        assertEquals("100.50", account.amounts[0].formattedAmount)
    }

    @Test
    fun `toggle zero balance twice restores original state`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(true)
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showZeroBalanceAccounts)

        // When - toggle twice
        viewModel.onEvent(AccountSummaryEvent.ToggleZeroBalanceAccounts)
        advanceUntilIdle()
        viewModel.onEvent(AccountSummaryEvent.ToggleZeroBalanceAccounts)
        advanceUntilIdle()

        // Then - back to original
        assertTrue(viewModel.uiState.value.showZeroBalanceAccounts)
    }

    // ========================================
    // Phase A3: Additional coverage tests
    // ========================================

    @Test
    fun `account with multiple currencies displays all amounts`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        // Add account with multiple currency amounts
        val multiCurrencyAccount = Account(
            id = 1L,
            name = "Assets:Bank",
            level = 1,
            isExpanded = true,
            isVisible = true,
            amounts = listOf(
                DomainAccountAmount(currency = "USD", amount = 1000f),
                DomainAccountAmount(currency = "EUR", amount = 500f),
                DomainAccountAmount(currency = "JPY", amount = 10000f)
            )
        )
        accountRepository.addAccount(1L, multiCurrencyAccount)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - account should have all three amounts
        val account = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
            .find { it.name == "Assets:Bank" }
        assertNotNull(account)
        assertEquals(3, account!!.amounts.size)
        assertTrue(account.amounts.any { it.formattedAmount.contains("USD") })
        assertTrue(account.amounts.any { it.formattedAmount.contains("EUR") })
        assertTrue(account.amounts.any { it.formattedAmount.contains("JPY") })
    }

    @Test
    fun `deeply nested account hierarchy preserves levels`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        // Create deeply nested account hierarchy (5 levels deep)
        val accounts = listOf(
            Account(
                id = 1L,
                name = "Assets",
                level = 0,
                isExpanded = true,
                isVisible = true,
                amounts = listOf(DomainAccountAmount("USD", 10000f))
            ),
            Account(
                id = 2L,
                name = "Assets:Bank",
                level = 1,
                isExpanded = true,
                isVisible = true,
                amounts = listOf(DomainAccountAmount("USD", 8000f))
            ),
            Account(
                id = 3L,
                name = "Assets:Bank:Checking",
                level = 2,
                isExpanded = true,
                isVisible = true,
                amounts = listOf(DomainAccountAmount("USD", 5000f))
            ),
            Account(
                id = 4L,
                name = "Assets:Bank:Checking:Primary",
                level = 3,
                isExpanded = true,
                isVisible = true,
                amounts = listOf(DomainAccountAmount("USD", 3000f))
            ),
            Account(
                id = 5L,
                name = "Assets:Bank:Checking:Primary:Active",
                level = 4,
                isExpanded = true,
                isVisible = true,
                amounts = listOf(DomainAccountAmount("USD", 1000f))
            )
        )
        accounts.forEach { accountRepository.addAccount(1L, it) }

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - all 5 levels should be present with correct nesting
        val loadedAccounts = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()
        assertEquals(5, loadedAccounts.size)

        // Verify levels are preserved
        val deepestAccount = loadedAccounts.find { it.name == "Assets:Bank:Checking:Primary:Active" }
        assertNotNull(deepestAccount)
        assertEquals(4, deepestAccount!!.level)
    }

    @Test
    fun `zero balance filter correctly handles mixed positive and negative amounts`() = runTest {
        // Given
        preferencesRepository.setShowZeroBalanceAccounts(false)
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        // Add accounts with various balance states:
        // 1. Positive balance (should show)
        // 2. Negative balance (should show)
        // 3. Zero balance single currency (should hide)
        // 4. Multi-currency where one is zero but another is non-zero (should show)
        val accounts = listOf(
            Account(
                id = 1L,
                name = "Assets:Cash",
                level = 1,
                isExpanded = true,
                isVisible = true,
                amounts = listOf(DomainAccountAmount("USD", 100f))
            ),
            Account(
                id = 2L,
                name = "Liabilities:CreditCard",
                level = 1,
                isExpanded = true,
                isVisible = true,
                amounts = listOf(DomainAccountAmount("USD", -500f))
            ),
            Account(
                id = 3L,
                name = "Assets:Empty",
                level = 1,
                isExpanded = true,
                isVisible = true,
                amounts = listOf(DomainAccountAmount("USD", 0f))
            ),
            Account(
                id = 4L,
                name = "Assets:Mixed",
                level = 1,
                isExpanded = true,
                isVisible = true,
                amounts = listOf(
                    DomainAccountAmount("USD", 0f),
                    DomainAccountAmount("EUR", 50f)
                )
            )
        )
        accounts.forEach { accountRepository.addAccount(1L, it) }

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - zero balance filter should hide only the pure zero balance account
        val visibleAccounts = viewModel.uiState.value.accounts
            .filterIsInstance<AccountSummaryListItem.Account>()

        // Should have 3 accounts: Cash (positive), CreditCard (negative), Mixed (has non-zero EUR)
        assertEquals(3, visibleAccounts.size)
        assertTrue(visibleAccounts.any { it.name == "Assets:Cash" })
        assertTrue(visibleAccounts.any { it.name == "Liabilities:CreditCard" })
        assertTrue(visibleAccounts.any { it.name == "Assets:Mixed" })
        assertFalse(visibleAccounts.any { it.name == "Assets:Empty" })
    }
}

/**
 * Fake AccountRepository specialized for AccountSummaryViewModel tests.
 * Implements AccountRepository directly to support account with amounts storage.
 * Now uses domain models (Account) for query operations.
 * Uses shared MutableStateFlows to support reactive testing.
 */
class FakeAccountRepositoryForAccountSummary : AccountRepository {
    private val domainAccounts = mutableMapOf<Long, MutableList<Account>>()
    private val accountNames = mutableMapOf<Long, MutableList<String>>()
    var simulateError = false

    // Shared StateFlows for reactive updates
    private val accountFlows = mutableMapOf<Long, MutableStateFlow<List<Account>>>()

    private fun getOrCreateFlow(profileId: Long): MutableStateFlow<List<Account>> = accountFlows.getOrPut(profileId) {
        MutableStateFlow(emptyList())
    }

    private fun emitAccounts(profileId: Long) {
        val accounts = domainAccounts[profileId] ?: emptyList()
        getOrCreateFlow(profileId).value = accounts
    }

    /**
     * Add a domain model account for testing.
     */
    fun addAccount(profileId: Long, account: Account) {
        domainAccounts.getOrPut(profileId) { mutableListOf() }.add(account)
        accountNames.getOrPut(profileId) { mutableListOf() }.add(account.name)
        emitAccounts(profileId)
    }

    /**
     * Helper to add account with amounts using db entities (converts to domain model).
     */
    fun addAccountWithAmounts(accountWithAmounts: AccountWithAmounts) {
        val profileId = accountWithAmounts.account.profileId
        val domainAccount = Account(
            id = accountWithAmounts.account.id,
            name = accountWithAmounts.account.name,
            level = accountWithAmounts.account.level,
            isExpanded = accountWithAmounts.account.expanded,
            isVisible = true,
            amounts = accountWithAmounts.amounts.map { av ->
                DomainAccountAmount(currency = av.currency, amount = av.value)
            }
        )
        addAccount(profileId, domainAccount)
    }

    // Flow methods (observe prefix)
    override fun observeAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> =
        getOrCreateFlow(profileId).map { accounts ->
            if (simulateError) {
                throw RuntimeException("Simulated error")
            }
            if (includeZeroBalances) {
                accounts
            } else {
                accounts.filter { acc -> acc.amounts.any { it.amount != 0f } }
            }
        }

    override fun observeByNameWithAmounts(profileId: Long, accountName: String) =
        MutableStateFlow(domainAccounts[profileId]?.find { it.name == accountName })

    override fun observeSearchAccountNames(profileId: Long, term: String) =
        MutableStateFlow(accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList())

    override fun observeSearchAccountNamesGlobal(term: String) =
        MutableStateFlow(accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) })

    // Suspend methods (no suffix)
    override suspend fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Result<List<Account>> {
        if (simulateError) {
            return Result.failure(RuntimeException("Simulated error"))
        }
        val accounts = domainAccounts[profileId] ?: emptyList()
        return Result.success(
            if (includeZeroBalances) {
                accounts
            } else {
                accounts.filter { acc ->
                    acc.amounts.any { it.amount != 0f }
                }
            }
        )
    }

    override suspend fun getByNameWithAmounts(profileId: Long, accountName: String): Result<Account?> =
        Result.success(domainAccounts[profileId]?.find { it.name == accountName })

    override suspend fun searchAccountNames(profileId: Long, term: String): Result<List<String>> =
        Result.success(accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList())

    override suspend fun searchAccountsWithAmounts(profileId: Long, term: String): Result<List<Account>> =
        Result.success(domainAccounts[profileId]?.filter { it.name.contains(term, ignoreCase = true) } ?: emptyList())

    override suspend fun searchAccountNamesGlobal(term: String): Result<List<String>> =
        Result.success(accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) })

    override suspend fun getCountForProfile(profileId: Long): Result<Int> =
        Result.success(domainAccounts[profileId]?.size ?: 0)

    override suspend fun deleteAllAccounts(): Result<Unit> {
        domainAccounts.clear()
        accountNames.clear()
        accountFlows.values.forEach { it.value = emptyList() }
        return Result.success(Unit)
    }

    override suspend fun storeAccountsAsDomain(accounts: List<Account>, profileId: Long): Result<Unit> {
        domainAccounts[profileId] = accounts.toMutableList()
        accountNames[profileId] = accounts.map { it.name }.toMutableList()
        emitAccounts(profileId)
        return Result.success(Unit)
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
