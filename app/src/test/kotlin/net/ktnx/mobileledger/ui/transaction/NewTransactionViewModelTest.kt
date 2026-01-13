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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.fake.FakeCurrencyFormatter
import net.ktnx.mobileledger.fake.FakeCurrencyRepository
import net.ktnx.mobileledger.fake.FakeTemplateRepository
import net.ktnx.mobileledger.fake.FakeTransactionSender
import net.ktnx.mobileledger.ui.main.FakeAccountRepositoryForViewModel
import net.ktnx.mobileledger.ui.main.FakeAppStateServiceForViewModel
import net.ktnx.mobileledger.ui.main.FakeProfileRepositoryForViewModel
import net.ktnx.mobileledger.ui.main.FakeTransactionRepositoryForViewModel
import net.ktnx.mobileledger.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for NewTransactionViewModel.
 *
 * Tests cover:
 * - Initialization with default currency from profile
 * - Amount input and balance hint recalculation
 * - Template application
 * - Transaction submission (success and failure)
 * - Form validation
 * - Account name suggestions
 *
 * Following TDD approach for test-first development.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewTransactionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var transactionRepository: FakeTransactionRepositoryForViewModel
    private lateinit var accountRepository: FakeAccountRepositoryForViewModel
    private lateinit var templateRepository: FakeTemplateRepository
    private lateinit var currencyRepository: FakeCurrencyRepository
    private lateinit var currencyFormatter: FakeCurrencyFormatter
    private lateinit var appStateService: FakeAppStateServiceForViewModel
    private lateinit var transactionSender: FakeTransactionSender

    private lateinit var viewModel: NewTransactionViewModel

    @Before
    fun setup() {
        profileRepository = FakeProfileRepositoryForViewModel()
        transactionRepository = FakeTransactionRepositoryForViewModel()
        accountRepository = FakeAccountRepositoryForViewModel()
        templateRepository = FakeTemplateRepository()
        currencyRepository = FakeCurrencyRepository()
        currencyFormatter = FakeCurrencyFormatter()
        appStateService = FakeAppStateServiceForViewModel()
        transactionSender = FakeTransactionSender()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(
        id: Long = 1L,
        name: String = "Test Profile",
        defaultCommodity: String = "USD"
    ): Profile = net.ktnx.mobileledger.util.createTestProfile(
        id = id,
        name = name,
        defaultCommodity = defaultCommodity
    ).apply {
        this.showCommodityByDefault = true
    }

    private suspend fun createViewModelWithProfile(profile: Profile? = null): NewTransactionViewModel {
        if (profile != null) {
            profileRepository.insertProfile(profile)
            profileRepository.setCurrentProfile(profile)
        }

        return NewTransactionViewModel(
            profileRepository = profileRepository,
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            templateRepository = templateRepository,
            currencyRepository = currencyRepository,
            currencyFormatter = currencyFormatter,
            appStateService = appStateService,
            transactionSender = transactionSender
        )
    }

    private fun createTemplateWithAccounts(
        id: Long = 1L,
        name: String = "Test Template",
        description: String = "Template Description",
        accounts: List<Pair<String, Float?>> = listOf("Assets:Bank" to 100.0f, "Expenses:Food" to null)
    ): TemplateWithAccounts {
        val header = TemplateHeader(id, name, "").apply {
            this.transactionDescription = description
        }
        val templateAccounts = accounts.mapIndexed { index, (accountName, amount) ->
            TemplateAccount(0L, id, (index + 1).toLong()).apply {
                this.accountName = accountName
                this.amount = amount
            }
        }
        return TemplateWithAccounts().apply {
            this.header = header
            this.accounts = templateAccounts
        }
    }

    // ========================================
    // T028: Initialization tests
    // ========================================

    @Test
    fun `initialization sets default currency from profile`() = runTest {
        // Given
        val profile = createTestProfile(defaultCommodity = "EUR")

        // When
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("EUR", state.accounts.firstOrNull()?.currency)
        assertEquals(profile.id, state.profileId)
    }

    @Test
    fun `initialization creates minimum two account rows`() = runTest {
        // Given
        val profile = createTestProfile()

        // When
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Should have at least 2 account rows", state.accounts.size >= 2)
    }

    @Test
    fun `initialization sets showCurrency from profile`() = runTest {
        // Given
        val profile = createTestProfile().apply {
            showCommodityByDefault = true
        }

        // When
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.showCurrency)
    }

    // ========================================
    // T029: Amount input and balance hint tests
    // ========================================

    @Test
    fun `amount hint shows negative of balance for empty amount row`() = runTest {
        // Given
        val profile = createTestProfile(defaultCommodity = "USD")
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        // When - Set amount on first row, account names on both
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // Then - Second row should show hint of -100.00
        val state = viewModel.uiState.value
        val row2 = state.accounts.find { it.id == row2Id }
        assertNotNull("Row 2 should exist", row2)
        assertEquals("-100.00", row2!!.amountHint)
    }

    @Test
    fun `amount hint updates when amount changes`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "50.00"))
        advanceUntilIdle()

        // When - Change amount
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "75.00"))
        advanceUntilIdle()

        // Then
        val row2 = viewModel.uiState.value.accounts.find { it.id == row2Id }
        assertEquals("-75.00", row2?.amountHint)
    }

    @Test
    fun `invalid amount format is detected`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val rowId = viewModel.uiState.value.accounts[0].id

        // When - Enter invalid amount
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(rowId, "not-a-number"))
        advanceUntilIdle()

        // Then
        val row = viewModel.uiState.value.accounts.find { it.id == rowId }
        assertFalse("Amount should be invalid", row?.isAmountValid ?: true)
    }

    // ========================================
    // T030: Template application tests
    // ========================================

    @Test
    fun `template application sets description and accounts`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template = createTemplateWithAccounts(
            name = "Lunch",
            description = "Lunch expense",
            accounts = listOf("Expenses:Food" to 15.0f, "Assets:Cash" to null)
        )
        templateRepository.insertTemplateWithAccounts(template)

        // When
        viewModel.onEvent(NewTransactionEvent.ApplyTemplate(template.header.id))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Lunch expense", state.description)
        assertEquals("Expenses:Food", state.accounts[0].accountName)
        assertEquals("15.00", state.accounts[0].amountText)
        assertEquals("Assets:Cash", state.accounts[1].accountName)
    }

    @Test
    fun `template selector shows available templates`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val template1 = createTemplateWithAccounts(id = 1, name = "Template 1")
        val template2 = createTemplateWithAccounts(id = 2, name = "Template 2")
        templateRepository.insertTemplateWithAccounts(template1)
        templateRepository.insertTemplateWithAccounts(template2)

        // When
        viewModel.onEvent(NewTransactionEvent.ShowTemplateSelector)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showTemplateSelector)
        assertEquals(2, state.availableTemplates.size)
    }

    // ========================================
    // T031: Transaction submission success tests
    // ========================================

    @Test
    fun `submit calls transactionSender with correct parameters`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test transaction"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()

        // Then
        assertEquals(1, transactionSender.sentTransactions.size)
        val sent = transactionSender.sentTransactions[0]
        assertEquals(profile.id, sent.profile.id)
        assertEquals("Test transaction", sent.transaction.description)
        assertFalse(sent.simulate)
    }

    @Test
    fun `submit with simulate flag calls transactionSender with simulate true`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test transaction"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        viewModel.onEvent(NewTransactionEvent.ToggleSimulateSave)
        advanceUntilIdle()

        // When
        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()

        // Then
        assertTrue(transactionSender.sentTransactions[0].simulate)
    }

    @Test
    fun `successful submit saves transaction to repository`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test transaction"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()

        // Then - Check transaction was stored
        val storedTransactions = transactionRepository.getTransactionsForProfile(profile.id)
        assertEquals(1, storedTransactions.size)
        assertEquals("Test transaction", storedTransactions[0].transaction.description)
    }

    @Test
    fun `successful submit resets form`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test transaction"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()

        // Then - Form should be reset
        val state = viewModel.uiState.value
        assertEquals("", state.description)
        assertTrue(state.accounts.all { it.accountName.isBlank() && it.amountText.isBlank() })
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `successful submit signals data changed`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val initialVersion = appStateService.dataVersion.first()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test transaction"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()

        // Then
        assertTrue(appStateService.dataVersion.first() > initialVersion)
    }

    // ========================================
    // T032: Transaction submission failure tests
    // ========================================

    @Test
    fun `failed submit sets error in ui state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        transactionSender.shouldSucceed = false
        transactionSender.errorMessage = "Network error"

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test transaction"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Network error", state.submitError)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `failed submit does not reset form`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        transactionSender.shouldSucceed = false

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test transaction"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()

        // Then - Form should NOT be reset
        val state = viewModel.uiState.value
        assertEquals("Test transaction", state.description)
        assertEquals("Assets:Bank", state.accounts[0].accountName)
    }

    @Test
    fun `failed submit does not save to repository`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        transactionSender.shouldSucceed = false

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test transaction"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()

        // Then
        val storedTransactions = transactionRepository.getTransactionsForProfile(profile.id)
        assertTrue(storedTransactions.isEmpty())
    }

    // ========================================
    // T033: Form validation tests
    // ========================================

    @Test
    fun `isSubmittable false when description is empty`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        // Then - No description, should not be submittable
        assertFalse(viewModel.uiState.value.isSubmittable)
    }

    @Test
    fun `isSubmittable false when less than two accounts`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        advanceUntilIdle()

        // Then - Only one account, should not be submittable
        assertFalse(viewModel.uiState.value.isSubmittable)
    }

    @Test
    fun `isSubmittable true when form is valid with balanced amounts`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row2Id, "-100.00"))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isSubmittable)
    }

    @Test
    fun `isSubmittable true when one account has empty amount for balance`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        // No amount on row2 - should be balance receiver
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isSubmittable)
    }

    @Test
    fun `isSubmittable false when amounts do not balance and multiple empty`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(NewTransactionEvent.AddAccountRow(null))
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id
        val row3Id = viewModel.uiState.value.accounts[2].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        // No amount on row2
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row3Id, "Expenses:Drinks"))
        // No amount on row3 - two empty amounts, can't determine which gets balance
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isSubmittable)
    }

    @Test
    fun `submit does nothing when form is not submittable`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Empty form, not submittable
        assertFalse(viewModel.uiState.value.isSubmittable)

        // When
        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()

        // Then - No transaction sent
        assertTrue(transactionSender.sentTransactions.isEmpty())
    }

    // ========================================
    // T034: Account search suggestion tests
    // ========================================

    @Test
    fun `account name input triggers suggestions lookup`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        accountRepository.addAccount(profile.id, "Assets:Bank:Checking")
        accountRepository.addAccount(profile.id, "Assets:Bank:Savings")
        accountRepository.addAccount(profile.id, "Expenses:Food")

        val rowId = viewModel.uiState.value.accounts[0].id

        // When - Type enough characters to trigger suggestion
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(rowId, "Assets:Bank"))
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.accountSuggestions.isNotEmpty())
        assertTrue(state.accountSuggestions.any { it.contains("Checking") })
        assertTrue(state.accountSuggestions.any { it.contains("Savings") })
        assertEquals(rowId, state.accountSuggestionsForRowId)
    }

    @Test
    fun `short input clears suggestions`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        accountRepository.addAccount(profile.id, "Assets:Bank")
        val rowId = viewModel.uiState.value.accounts[0].id

        // First, trigger suggestions
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(rowId, "Assets"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.accountSuggestions.isNotEmpty())

        // When - Type short input
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(rowId, "A"))
        advanceUntilIdle()

        // Then - Suggestions should be cleared
        assertTrue(viewModel.uiState.value.accountSuggestions.isEmpty())
    }

    @Test
    fun `description input triggers description suggestions`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Add some transactions with descriptions
        val twa = net.ktnx.mobileledger.db.TransactionWithAccounts().apply {
            transaction = net.ktnx.mobileledger.db.Transaction().apply {
                this.id = 1L
                this.profileId = profile.id
                this.description = "Grocery Store"
            }
            accounts = emptyList()
        }
        transactionRepository.insertTransaction(twa)

        // When
        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Groc"))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.descriptionSuggestions.isNotEmpty())
        assertTrue(viewModel.uiState.value.descriptionSuggestions.any { it.contains("Grocery") })
    }

    // ========================================
    // Additional edge case tests
    // ========================================

    @Test
    fun `adding account row maintains minimum rows`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val initialCount = viewModel.uiState.value.accounts.size

        // When
        viewModel.onEvent(NewTransactionEvent.AddAccountRow(null))
        advanceUntilIdle()

        // Then
        assertEquals(initialCount + 1, viewModel.uiState.value.accounts.size)
    }

    @Test
    fun `removing account row maintains minimum two rows`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Start with exactly 2 rows
        assertEquals(2, viewModel.uiState.value.accounts.size)
        val rowId = viewModel.uiState.value.accounts[0].id

        // When - Try to remove a row
        viewModel.onEvent(NewTransactionEvent.RemoveAccountRow(rowId))
        advanceUntilIdle()

        // Then - Should still have 2 rows
        assertEquals(2, viewModel.uiState.value.accounts.size)
    }

    @Test
    fun `reset clears all form data`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val row1Id = viewModel.uiState.value.accounts[0].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(NewTransactionEvent.Reset)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.description)
        assertTrue(state.accounts.all { it.accountName.isBlank() })
    }

    @Test
    fun `clearError removes submit error`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        transactionSender.shouldSucceed = false
        transactionSender.errorMessage = "Test error"

        val row1Id = viewModel.uiState.value.accounts[0].id
        val row2Id = viewModel.uiState.value.accounts[1].id

        viewModel.onEvent(NewTransactionEvent.UpdateDescription("Test"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row1Id, "Assets:Bank"))
        viewModel.onEvent(NewTransactionEvent.UpdateAmount(row1Id, "100.00"))
        viewModel.onEvent(NewTransactionEvent.UpdateAccountName(row2Id, "Expenses:Food"))
        advanceUntilIdle()

        viewModel.onEvent(NewTransactionEvent.Submit)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.submitError)

        // When
        viewModel.onEvent(NewTransactionEvent.ClearError)
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.submitError)
    }
}
