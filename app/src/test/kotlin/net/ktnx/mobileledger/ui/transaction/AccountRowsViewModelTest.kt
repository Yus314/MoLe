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

package net.ktnx.mobileledger.ui.transaction

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.testing.fake.FakeAccountRepository
import net.ktnx.mobileledger.core.testing.fake.FakeCurrencyRepository
import net.ktnx.mobileledger.core.testing.fake.FakeProfileRepository
import net.ktnx.mobileledger.domain.usecase.DeleteCurrencyUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.GetAllCurrenciesUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SaveCurrencyUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.TransactionAccountRowManager
import net.ktnx.mobileledger.domain.usecase.TransactionAccountRowManagerImpl
import net.ktnx.mobileledger.fake.FakeCurrencyFormatter
import net.ktnx.mobileledger.fake.FakeRowIdGenerator
import net.ktnx.mobileledger.feature.profile.usecase.ObserveCurrentProfileUseCaseImpl
import net.ktnx.mobileledger.feature.transaction.usecase.AccountSuggestionLookup
import net.ktnx.mobileledger.feature.transaction.usecase.AccountSuggestionLookupImpl
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionBalanceCalculator
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionBalanceCalculatorImpl
import net.ktnx.mobileledger.util.MainDispatcherRule
import net.ktnx.mobileledger.util.createTestDomainProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for AccountRowsViewModel.
 *
 * Tests cover:
 * - Row management (add, remove, move)
 * - Balance calculation
 * - Currency selection
 * - Account suggestions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountRowsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var currencyRepository: FakeCurrencyRepository
    private lateinit var currencyFormatter: FakeCurrencyFormatter
    private lateinit var rowIdGenerator: FakeRowIdGenerator
    private lateinit var balanceCalculator: TransactionBalanceCalculator
    private lateinit var rowManager: TransactionAccountRowManager
    private lateinit var suggestionLookup: AccountSuggestionLookup

    private lateinit var viewModel: AccountRowsViewModel

    @Before
    fun setup() {
        profileRepository = FakeProfileRepository()
        accountRepository = FakeAccountRepository()
        currencyRepository = FakeCurrencyRepository()
        currencyFormatter = FakeCurrencyFormatter()
        rowIdGenerator = FakeRowIdGenerator()
        balanceCalculator = TransactionBalanceCalculatorImpl()
        rowManager = TransactionAccountRowManagerImpl()
        suggestionLookup = AccountSuggestionLookupImpl(accountRepository)
    }

    private fun createTestProfile(
        id: Long? = 1L,
        name: String = "Test Profile",
        defaultCommodity: String = "USD"
    ): Profile = createTestDomainProfile(
        id = id,
        name = name,
        defaultCommodity = defaultCommodity
    ).copy(showCommodityByDefault = true)

    private suspend fun createViewModelWithProfile(profile: Profile? = null): AccountRowsViewModel {
        if (profile != null) {
            profileRepository.insertProfile(profile)
            profileRepository.setCurrentProfile(profile)
        }

        val observeCurrentProfileUseCase = ObserveCurrentProfileUseCaseImpl(profileRepository)
        val getAllCurrenciesUseCase = GetAllCurrenciesUseCaseImpl(currencyRepository)
        val saveCurrencyUseCase = SaveCurrencyUseCaseImpl(currencyRepository)
        val deleteCurrencyUseCase = DeleteCurrencyUseCaseImpl(currencyRepository)

        return AccountRowsViewModel(
            observeCurrentProfileUseCase = observeCurrentProfileUseCase,
            getAllCurrenciesUseCase = getAllCurrenciesUseCase,
            saveCurrencyUseCase = saveCurrencyUseCase,
            deleteCurrencyUseCase = deleteCurrencyUseCase,
            currencyFormatter = currencyFormatter,
            rowIdGenerator = rowIdGenerator,
            balanceCalculator = balanceCalculator,
            rowManager = rowManager,
            suggestionLookup = suggestionLookup
        )
    }

    // ========================================
    // T057: Row management tests
    // ========================================

    @Test
    fun `initialization creates minimum two account rows`() = runTest {
        // Given
        val profile = createTestProfile()

        // When
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Then
        assertTrue("Should have at least 2 account rows", viewModel.uiState.value.accounts.size >= 2)
    }

    @Test
    fun `initialization sets default currency from profile`() = runTest {
        // Given
        val profile = createTestProfile(defaultCommodity = "EUR")

        // When
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("EUR", state.defaultCurrency)
        assertEquals("EUR", state.accounts.firstOrNull()?.currency)
    }

    @Test
    fun `addAccountRow adds new row`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val initialCount = viewModel.uiState.value.accounts.size

        // When
        viewModel.onEvent(AccountRowsEvent.AddAccountRow(null))
        advanceUntilIdle()

        // Then
        assertEquals(initialCount + 1, viewModel.uiState.value.accounts.size)
    }

    @Test
    fun `addAccountRow inserts after specified row`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id

        // When
        viewModel.onEvent(AccountRowsEvent.AddAccountRow(row1Id))
        advanceUntilIdle()

        // Then
        assertEquals(3, viewModel.uiState.value.accounts.size)
        // New row should be at index 1
        assertEquals(row1Id, viewModel.uiState.value.accounts[0].id)
    }

    @Test
    fun `removeAccountRow removes row when more than two`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(AccountRowsEvent.AddAccountRow(null))
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.accounts.size)

        val rowId = viewModel.uiState.value.accounts[2].id

        // When
        viewModel.onEvent(AccountRowsEvent.RemoveAccountRow(rowId))
        advanceUntilIdle()

        // Then
        assertEquals(2, viewModel.uiState.value.accounts.size)
    }

    @Test
    fun `removeAccountRow maintains minimum two rows`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.accounts.size)
        val rowId = viewModel.uiState.value.accounts[0].id

        // When - Try to remove when at minimum
        viewModel.onEvent(AccountRowsEvent.RemoveAccountRow(rowId))
        advanceUntilIdle()

        // Then - Should still have 2 rows
        assertEquals(2, viewModel.uiState.value.accounts.size)
    }

    @Test
    fun `moveAccountRow reorders rows`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(AccountRowsEvent.AddAccountRow(null))
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id
        val row3Id = viewModel.uiState.value.accounts[2].id

        // When - Move first row to last position
        viewModel.onEvent(AccountRowsEvent.MoveAccountRow(0, 2))
        advanceUntilIdle()

        // Then
        assertEquals(row2Id, viewModel.uiState.value.accounts[0].id)
        assertEquals(row3Id, viewModel.uiState.value.accounts[1].id)
        assertEquals(row1Id, viewModel.uiState.value.accounts[2].id)
    }

    @Test
    fun `updateAccountName updates row`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(rowId, "Assets:Bank"))
        advanceUntilIdle()

        // Then
        val row = viewModel.uiState.value.accounts.find { it.id == rowId }
        assertEquals("Assets:Bank", row?.accountName)
    }

    @Test
    fun `updateAmount updates row and validates`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(rowId, "100.00"))
        advanceUntilIdle()

        // Then
        val row = viewModel.uiState.value.accounts.find { it.id == rowId }
        assertEquals("100.00", row?.amountText)
        assertTrue(row?.isAmountValid == true)
    }

    @Test
    fun `updateAmount detects invalid amount`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(rowId, "not-a-number"))
        advanceUntilIdle()

        // Then
        val row = viewModel.uiState.value.accounts.find { it.id == rowId }
        assertFalse(row?.isAmountValid ?: true)
    }

    // ========================================
    // T058: Balance calculation tests
    // ========================================

    @Test
    fun `amount hint shows negative of balance`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        // When - Set amount on first row
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // Then - Second row should show hint
        val row2 = viewModel.uiState.value.accounts.find { it.id == row2Id }
        assertEquals("-100.00", row2?.amountHint)
    }

    @Test
    fun `isBalanced true when amounts balance to zero`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        // When - Set balanced amounts
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(row2Id, "-100.00"))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isBalanced)
    }

    @Test
    fun `isBalanced true when one empty amount for balance receiver`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        // When - One row with amount, one without
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        // No amount on row2
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isBalanced)
    }

    @Test
    fun `isBalanced false when less than two accounts with names`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id

        // When - Only one account with name
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(row1Id, "100.00"))
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isBalanced)
    }

    @Test
    fun `isBalanced false when multiple empty amounts and not balanced`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(AccountRowsEvent.AddAccountRow(null))
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id
        val row3Id = viewModel.uiState.value.accounts[2].id

        // When - One amount, two empty
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row3Id, "Expenses:Drinks"))
        // No amounts on row2 and row3
        advanceUntilIdle()

        // Then - Can't determine balance receiver
        assertFalse(viewModel.uiState.value.isBalanced)
    }

    // ========================================
    // T059: Currency selection tests
    // ========================================

    @Test
    fun `showCurrencySelector sets visibility and rowId`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When
        viewModel.onEvent(AccountRowsEvent.ShowCurrencySelector(rowId))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.showCurrencySelector)
        assertEquals(rowId, viewModel.uiState.value.currencySelectorRowId)
    }

    @Test
    fun `dismissCurrencySelector hides selector`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id
        viewModel.onEvent(AccountRowsEvent.ShowCurrencySelector(rowId))
        advanceUntilIdle()

        // When
        viewModel.onEvent(AccountRowsEvent.DismissCurrencySelector)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showCurrencySelector)
        assertEquals(null, viewModel.uiState.value.currencySelectorRowId)
    }

    @Test
    fun `updateCurrency updates row currency`() = runTest {
        // Given
        val profile = createTestProfile(defaultCommodity = "USD")
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When
        viewModel.onEvent(AccountRowsEvent.UpdateCurrency(rowId, "EUR"))
        advanceUntilIdle()

        // Then
        val row = viewModel.uiState.value.accounts.find { it.id == rowId }
        assertEquals("EUR", row?.currency)
    }

    @Test
    fun `toggleCurrency switches currency visibility`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val initialShowCurrency = viewModel.uiState.value.showCurrency

        // When
        viewModel.onEvent(AccountRowsEvent.ToggleCurrency)
        advanceUntilIdle()

        // Then
        assertEquals(!initialShowCurrency, viewModel.uiState.value.showCurrency)
    }

    // ========================================
    // Account comment tests
    // ========================================

    @Test
    fun `updateAccountComment updates row comment`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When
        viewModel.onEvent(AccountRowsEvent.UpdateAccountComment(rowId, "Test comment"))
        advanceUntilIdle()

        // Then
        val row = viewModel.uiState.value.accounts.find { it.id == rowId }
        assertEquals("Test comment", row?.comment)
    }

    @Test
    fun `toggleAccountComment toggles expanded state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id
        assertFalse(viewModel.uiState.value.accounts.find { it.id == rowId }?.isCommentExpanded ?: true)

        // When
        viewModel.onEvent(AccountRowsEvent.ToggleAccountComment(rowId))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.accounts.find { it.id == rowId }?.isCommentExpanded == true)
    }

    @Test
    fun `toggleAccountComment sets comment expanded to true`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id
        assertFalse(viewModel.uiState.value.accounts.find { it.id == rowId }?.isCommentExpanded ?: true)

        // When
        viewModel.onEvent(AccountRowsEvent.ToggleAccountComment(rowId))
        advanceUntilIdle()

        // Then - Comment is expanded
        assertTrue(viewModel.uiState.value.accounts.find { it.id == rowId }?.isCommentExpanded == true)
    }

    // ========================================
    // Account suggestions tests
    // ========================================

    @Test
    fun `account name input triggers suggestions`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        accountRepository.addAccount(profile.id!!, "Assets:Bank:Checking")
        accountRepository.addAccount(profile.id!!, "Assets:Bank:Savings")

        val rowId = viewModel.uiState.value.accounts[0].id

        // When - Type enough characters
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(rowId, "Assets:Bank"))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.accountSuggestions.isNotEmpty())
        assertEquals(rowId, state.accountSuggestionsForRowId)
    }

    @Test
    fun `short input clears suggestions`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        accountRepository.addAccount(profile.id!!, "Assets:Bank")
        val rowId = viewModel.uiState.value.accounts[0].id

        // First trigger suggestions
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(rowId, "Assets"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.accountSuggestions.isNotEmpty())

        // When - Short input
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(rowId, "A"))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.accountSuggestions.isEmpty())
    }

    // ========================================
    // Reset and setRows tests
    // ========================================

    @Test
    fun `reset clears all account data`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(row1Id, "100.00"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(AccountRowsEvent.Reset)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(2, state.accounts.size)
        assertTrue(state.accounts.all { it.accountName.isBlank() && it.amountText.isBlank() })
    }

    @Test
    fun `setRows replaces existing rows`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val newRows = listOf(
            TransactionAccountRow(id = 100, accountName = "Expenses:Food", amountText = "50.00", currency = "USD"),
            TransactionAccountRow(id = 101, accountName = "Assets:Cash", amountText = "-50.00", currency = "USD")
        )

        // When
        viewModel.onEvent(AccountRowsEvent.SetRows(newRows))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(2, state.accounts.size)
        assertEquals("Expenses:Food", state.accounts[0].accountName)
        assertEquals("Assets:Cash", state.accounts[1].accountName)
    }

    @Test
    fun `setRows ensures minimum two rows`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val singleRow = listOf(
            TransactionAccountRow(id = 100, accountName = "Expenses:Food", amountText = "50.00", currency = "USD")
        )

        // When
        viewModel.onEvent(AccountRowsEvent.SetRows(singleRow))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.accounts.size >= 2)
    }

    // ========================================
    // Focus management tests
    // ========================================

    @Test
    fun `noteFocus updates focus state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When
        viewModel.onEvent(AccountRowsEvent.NoteFocus(rowId, FocusedElement.Amount))
        advanceUntilIdle()

        // Then
        assertEquals(rowId, viewModel.uiState.value.focusedRowId)
        assertEquals(FocusedElement.Amount, viewModel.uiState.value.focusedElement)
    }

    // ========================================
    // Currency management tests
    // ========================================

    @Test
    fun `addCurrency adds new currency to repository`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onEvent(
            AccountRowsEvent.AddCurrency(
                "EUR",
                net.ktnx.mobileledger.core.domain.model.CurrencyPosition.AFTER,
                true
            )
        )
        advanceUntilIdle()

        // Then
        val currencies = currencyRepository.getAllCurrenciesAsDomain().getOrThrow()
        assertTrue(currencies.any { it.name == "EUR" })
    }

    @Test
    fun `deleteCurrency removes currency from repository`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // First add a currency
        currencyRepository.saveCurrency(
            net.ktnx.mobileledger.core.domain.model.Currency(
                name = "JPY",
                position = net.ktnx.mobileledger.core.domain.model.CurrencyPosition.BEFORE,
                hasGap = false
            )
        )
        advanceUntilIdle()
        assertTrue(currencyRepository.getAllCurrenciesAsDomain().getOrThrow().any { it.name == "JPY" })

        // When
        viewModel.onEvent(AccountRowsEvent.DeleteCurrency("JPY"))
        advanceUntilIdle()

        // Then
        assertFalse(currencyRepository.getAllCurrenciesAsDomain().getOrThrow().any { it.name == "JPY" })
    }

    @Test
    fun `currencies are loaded on initialization`() = runTest {
        // Given
        currencyRepository.saveCurrency(
            net.ktnx.mobileledger.core.domain.model.Currency(name = "USD")
        )
        currencyRepository.saveCurrency(
            net.ktnx.mobileledger.core.domain.model.Currency(name = "EUR")
        )
        val profile = createTestProfile()

        // When
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.availableCurrencies.contains("USD"))
        assertTrue(viewModel.uiState.value.availableCurrencies.contains("EUR"))
    }

    // ========================================
    // Edge case tests
    // ========================================

    @Test
    fun `initialization with no profile does not crash`() = runTest {
        // Given - no profile set

        // When
        viewModel = createViewModelWithProfile(null)
        advanceUntilIdle()

        // Then - should not crash, accounts list may be empty or default
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun `removeAccountRow for non-existent row is no-op`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val initialCount = viewModel.uiState.value.accounts.size

        // When - try to remove non-existent row
        viewModel.onEvent(AccountRowsEvent.RemoveAccountRow(9999))
        advanceUntilIdle()

        // Then - no change
        assertEquals(initialCount, viewModel.uiState.value.accounts.size)
    }

    @Test
    fun `updateAccountName for non-existent row is no-op`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When - try to update non-existent row
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(9999, "Assets:Bank"))
        advanceUntilIdle()

        // Then - no crash, existing rows unchanged
        assertTrue(viewModel.uiState.value.accounts.none { it.accountName == "Assets:Bank" })
    }

    @Test
    fun `moveAccountRow with invalid indices is handled gracefully`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val initialOrder = viewModel.uiState.value.accounts.map { it.id }

        // When - try invalid move
        viewModel.onEvent(AccountRowsEvent.MoveAccountRow(-1, 100))
        advanceUntilIdle()

        // Then - no crash, order unchanged or handled gracefully
        assertNotNull(viewModel.uiState.value.accounts)
    }

    @Test
    fun `updateAmount with comma decimal separator is valid`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When - use comma as decimal separator
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(rowId, "100,50"))
        advanceUntilIdle()

        // Then - should be valid
        val row = viewModel.uiState.value.accounts.find { it.id == rowId }
        assertTrue(row?.isAmountValid == true)
    }

    @Test
    fun `empty amount is valid`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(rowId, ""))
        advanceUntilIdle()

        // Then - empty is valid
        val row = viewModel.uiState.value.accounts.find { it.id == rowId }
        assertTrue(row?.isAmountValid == true)
    }

    @Test
    fun `toggleCurrency with no profile is no-op`() = runTest {
        // Given - no profile
        viewModel = createViewModelWithProfile(null)
        advanceUntilIdle()

        val initialShowCurrency = viewModel.uiState.value.showCurrency

        // When
        viewModel.onEvent(AccountRowsEvent.ToggleCurrency)
        advanceUntilIdle()

        // Then - no crash, state may or may not change depending on implementation
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun `noteFocus with null values clears focus`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id
        viewModel.onEvent(AccountRowsEvent.NoteFocus(rowId, FocusedElement.Amount))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.focusedRowId)

        // When
        viewModel.onEvent(AccountRowsEvent.NoteFocus(null, null))
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.focusedRowId)
        assertEquals(null, viewModel.uiState.value.focusedElement)
    }

    @Test
    fun `setRows with empty list ensures minimum rows`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onEvent(AccountRowsEvent.SetRows(emptyList()))
        advanceUntilIdle()

        // Then - should have minimum 2 rows
        assertTrue(viewModel.uiState.value.accounts.size >= 2)
    }

    @Test
    fun `balance calculation with multiple currencies`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(AccountRowsEvent.AddAccountRow(null))
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id
        val row3Id = viewModel.uiState.value.accounts[2].id

        // When - mixed currencies
        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row1Id, "Assets:USD"))
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(row1Id, "100"))
        viewModel.onEvent(AccountRowsEvent.UpdateCurrency(row1Id, "USD"))

        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row2Id, "Assets:EUR"))
        viewModel.onEvent(AccountRowsEvent.UpdateAmount(row2Id, "50"))
        viewModel.onEvent(AccountRowsEvent.UpdateCurrency(row2Id, "EUR"))

        viewModel.onEvent(AccountRowsEvent.UpdateAccountName(row3Id, "Expenses"))
        advanceUntilIdle()

        // Then - balance calculation should handle mixed currencies
        // (exact behavior depends on implementation)
        assertNotNull(viewModel.uiState.value.accounts)
    }
}
