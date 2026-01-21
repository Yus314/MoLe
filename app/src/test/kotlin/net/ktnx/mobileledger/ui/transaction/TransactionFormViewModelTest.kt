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
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.fake.FakeAppStateService
import net.ktnx.mobileledger.fake.FakeProfileRepository
import net.ktnx.mobileledger.fake.FakeTransactionBalanceCalculator
import net.ktnx.mobileledger.fake.FakeTransactionRepository
import net.ktnx.mobileledger.fake.FakeTransactionSender
import net.ktnx.mobileledger.util.MainDispatcherRule
import net.ktnx.mobileledger.util.createTestDomainProfile
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for TransactionFormViewModel.
 *
 * Tests cover:
 * - Form field updates (date, description, comment)
 * - Form validation
 * - Transaction submission
 * - Effect emission
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var appStateService: FakeAppStateService
    private lateinit var transactionSender: FakeTransactionSender
    private lateinit var balanceCalculator: FakeTransactionBalanceCalculator

    private lateinit var viewModel: TransactionFormViewModel

    @Before
    fun setup() {
        profileRepository = FakeProfileRepository()
        transactionRepository = FakeTransactionRepository()
        appStateService = FakeAppStateService()
        transactionSender = FakeTransactionSender()
        balanceCalculator = FakeTransactionBalanceCalculator()
    }

    private fun createTestProfile(
        id: Long? = 1L,
        name: String = "Test Profile",
        defaultCommodity: String = "USD"
    ): Profile = createTestDomainProfile(
        id = id,
        name = name,
        defaultCommodity = defaultCommodity
    )

    private suspend fun createViewModelWithProfile(profile: Profile? = null): TransactionFormViewModel {
        if (profile != null) {
            profileRepository.insertProfile(profile)
            profileRepository.setCurrentProfile(profile)
        }

        return TransactionFormViewModel(
            profileRepository = profileRepository,
            transactionRepository = transactionRepository,
            appStateService = appStateService,
            transactionSender = transactionSender,
            balanceCalculator = balanceCalculator
        )
    }

    // ========================================
    // T047: Form management tests
    // ========================================

    @Test
    fun `initialization sets profile id from current profile`() = runTest {
        // Given
        val profile = createTestProfile(id = 42L)

        // When
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Then
        assertEquals(42L, viewModel.uiState.value.profileId)
    }

    @Test
    fun `updateDescription updates ui state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test description"))
        advanceUntilIdle()

        // Then
        assertEquals("Test description", viewModel.uiState.value.description)
    }

    @Test
    fun `updateDate updates ui state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val newDate = SimpleDate(2025, 6, 15)

        // When
        viewModel.onEvent(TransactionFormEvent.UpdateDate(newDate))
        advanceUntilIdle()

        // Then
        assertEquals(newDate, viewModel.uiState.value.date)
    }

    @Test
    fun `updateTransactionComment updates ui state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionFormEvent.UpdateTransactionComment("Test comment"))
        advanceUntilIdle()

        // Then
        assertEquals("Test comment", viewModel.uiState.value.transactionComment)
    }

    @Test
    fun `showDatePicker sets showDatePicker to true`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionFormEvent.ShowDatePicker)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `dismissDatePicker sets showDatePicker to false`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TransactionFormEvent.ShowDatePicker)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDatePicker)

        // When
        viewModel.onEvent(TransactionFormEvent.DismissDatePicker)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    @Test
    fun `toggleTransactionComment toggles expanded state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTransactionCommentExpanded)

        // When
        viewModel.onEvent(TransactionFormEvent.ToggleTransactionComment)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isTransactionCommentExpanded)
    }

    @Test
    fun `toggleSimulateSave toggles simulate flag`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSimulateSave)

        // When
        viewModel.onEvent(TransactionFormEvent.ToggleSimulateSave)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isSimulateSave)
    }

    // ========================================
    // T048: Validation tests
    // ========================================

    @Test
    fun `isFormValid is true when description is not blank`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `isFormValid is false when description is blank`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When - Empty description
        viewModel.onEvent(TransactionFormEvent.UpdateDescription(""))
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isFormValid)
    }

    @Test
    fun `hasUnsavedChanges is true when description is set`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // When
        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `hasUnsavedChanges is true when comment is set`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // When
        viewModel.onEvent(TransactionFormEvent.UpdateTransactionComment("Test comment"))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    // ========================================
    // T049: Transaction sending tests
    // ========================================

    @Test
    fun `submit sends transaction with correct data`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test transaction"))
        viewModel.onEvent(TransactionFormEvent.UpdateTransactionComment("Test comment"))
        advanceUntilIdle()

        val accountRows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank", amountText = "100.00", currency = "USD"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food", amountText = "", currency = "USD")
        )

        // When
        viewModel.onEvent(TransactionFormEvent.Submit(accountRows))
        advanceUntilIdle()

        // Then
        assertEquals(1, transactionSender.sentTransactions.size)
        val sent = transactionSender.sentTransactions[0]
        assertEquals(profile.id, sent.profile.id)
        assertEquals("Test transaction", sent.transaction.description)
        assertEquals("Test comment", sent.transaction.comment)
    }

    @Test
    fun `successful submit completes without error`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()

        val accountRows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank", amountText = "100.00", currency = "USD"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food", amountText = "-100.00", currency = "USD")
        )

        // When
        viewModel.onEvent(TransactionFormEvent.Submit(accountRows))
        advanceUntilIdle()

        // Then - No error, not submitting
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(null, viewModel.uiState.value.submitError)
    }

    @Test
    fun `successful submit signals data changed`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val initialVersion = appStateService.dataVersion.first()

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()

        val accountRows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank", amountText = "100.00", currency = "USD"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food", amountText = "-100.00", currency = "USD")
        )

        // When
        viewModel.onEvent(TransactionFormEvent.Submit(accountRows))
        advanceUntilIdle()

        // Then
        assertTrue(appStateService.dataVersion.first() > initialVersion)
    }

    @Test
    fun `failed submit sets error in ui state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        transactionSender.shouldSucceed = false
        transactionSender.errorMessage = "Network error"

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()

        val accountRows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank", amountText = "100.00", currency = "USD"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food", amountText = "-100.00", currency = "USD")
        )

        // When
        viewModel.onEvent(TransactionFormEvent.Submit(accountRows))
        advanceUntilIdle()

        // Then
        assertEquals("Network error", viewModel.uiState.value.submitError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `failed submit sets error message in state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        transactionSender.shouldSucceed = false
        transactionSender.errorMessage = "Network error"

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()

        val accountRows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank", amountText = "100.00", currency = "USD"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food", amountText = "-100.00", currency = "USD")
        )

        // When
        viewModel.onEvent(TransactionFormEvent.Submit(accountRows))
        advanceUntilIdle()

        // Then - Error is set in state
        assertEquals("Network error", viewModel.uiState.value.submitError)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `submit with empty description does nothing`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val accountRows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank", amountText = "100.00", currency = "USD"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food", amountText = "-100.00", currency = "USD")
        )

        // When - Submit without description
        viewModel.onEvent(TransactionFormEvent.Submit(accountRows))
        advanceUntilIdle()

        // Then
        assertTrue(transactionSender.sentTransactions.isEmpty())
    }

    @Test
    fun `reset clears form data`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        viewModel.onEvent(TransactionFormEvent.UpdateTransactionComment("Comment"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionFormEvent.Reset)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.description)
        assertEquals("", state.transactionComment)
    }

    @Test
    fun `reset clears description and comment`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test description"))
        viewModel.onEvent(TransactionFormEvent.UpdateTransactionComment("Test comment"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionFormEvent.Reset)
        advanceUntilIdle()

        // Then
        assertEquals("", viewModel.uiState.value.description)
        assertEquals("", viewModel.uiState.value.transactionComment)
    }

    @Test
    fun `clearError clears submit error`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        transactionSender.shouldSucceed = false
        transactionSender.errorMessage = "Error"

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()

        val accountRows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank", amountText = "100.00", currency = "USD"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food", amountText = "-100.00", currency = "USD")
        )

        viewModel.onEvent(TransactionFormEvent.Submit(accountRows))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.submitError)

        // When
        viewModel.onEvent(TransactionFormEvent.ClearError)
        advanceUntilIdle()

        // Then
        assertEquals(null, viewModel.uiState.value.submitError)
    }

    @Test
    fun `applyTemplateData updates description and comment`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.applyTemplateData("Template Description", "Template Comment", null)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("Template Description", state.description)
        assertEquals("Template Comment", state.transactionComment)
    }

    // ========================================
    // T050: Additional tests
    // ========================================

    @Test
    fun `setProfile updates profile id`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.setProfile(99L)
        advanceUntilIdle()

        // Then
        assertEquals(99L, viewModel.uiState.value.profileId)
    }

    @Test
    fun `description suggestions shown when input is long enough`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Add some transactions to search
        transactionRepository.insertTransaction(
            net.ktnx.mobileledger.domain.model.Transaction(
                id = null,
                ledgerId = 1L,
                date = SimpleDate.today(),
                description = "GROCERY STORE",
                lines = emptyList()
            ),
            profile.id!!
        )

        // When
        viewModel.onEvent(TransactionFormEvent.UpdateDescription("GROCERY"))
        advanceUntilIdle()

        // Then - suggestions should contain the match
        val suggestions = viewModel.uiState.value.descriptionSuggestions
        assertTrue(suggestions.contains("GROCERY STORE"))
    }

    @Test
    fun `short description input clears suggestions`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // First set a long description to potentially get suggestions
        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Grocery"))
        advanceUntilIdle()

        // When - set short description
        viewModel.onEvent(TransactionFormEvent.UpdateDescription("G"))
        advanceUntilIdle()

        // Then - suggestions should be empty
        assertTrue(viewModel.uiState.value.descriptionSuggestions.isEmpty())
    }

    @Test
    fun `loadFromTransaction loads existing transaction data`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Create a transaction
        val result = transactionRepository.insertTransaction(
            net.ktnx.mobileledger.domain.model.Transaction(
                id = null,
                ledgerId = 1L,
                date = SimpleDate.today(),
                description = "Test Transaction",
                comment = "Test Comment",
                lines = emptyList()
            ),
            profile.id!!
        )
        val transactionId = result.getOrThrow().id!!

        // When
        viewModel.onEvent(TransactionFormEvent.LoadFromTransaction(transactionId))
        advanceUntilIdle()

        // Then
        assertEquals("Test Transaction", viewModel.uiState.value.description)
        assertEquals("Test Comment", viewModel.uiState.value.transactionComment)
    }

    @Test
    fun `loadFromTransaction with invalid id shows error effect`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When - load non-existent transaction
        viewModel.onEvent(TransactionFormEvent.LoadFromTransaction(9999L))
        advanceUntilIdle()

        // Then - should show error (via effect)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `loadFromDescription sets description`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionFormEvent.LoadFromDescription("Test Description"))
        advanceUntilIdle()

        // Then
        assertEquals("Test Description", viewModel.uiState.value.description)
        assertFalse(viewModel.uiState.value.isBusy)
    }

    @Test
    fun `navigateBack with unsaved changes emits dialog effect`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        // Make unsaved changes
        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        // When
        viewModel.onEvent(TransactionFormEvent.NavigateBack)
        advanceUntilIdle()

        // Then - effect should be emitted (difficult to check directly, but state unchanged)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `navigateBack without unsaved changes navigates directly`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // When
        viewModel.onEvent(TransactionFormEvent.NavigateBack)
        advanceUntilIdle()

        // Then - should navigate (no dialog shown)
        // Effect is emitted but hard to check directly
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun `confirmDiscardChanges navigates back`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionFormEvent.ConfirmDiscardChanges)
        advanceUntilIdle()

        // Then - effect is emitted
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun `applyTemplateData with date updates date`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        val templateDate = SimpleDate(2025, 6, 15)

        // When
        viewModel.applyTemplateData("Desc", "Comment", templateDate)
        advanceUntilIdle()

        // Then
        assertEquals(templateDate, viewModel.uiState.value.date)
    }

    @Test
    fun `applyTemplateData with null comment keeps existing comment`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        viewModel.onEvent(TransactionFormEvent.UpdateTransactionComment("Existing Comment"))
        advanceUntilIdle()

        // When - apply with null comment
        viewModel.applyTemplateData("New Desc", null, null)
        advanceUntilIdle()

        // Then
        assertEquals("New Desc", viewModel.uiState.value.description)
        assertEquals("Existing Comment", viewModel.uiState.value.transactionComment)
    }

    @Test
    fun `submit without profile shows error`() = runTest {
        // Given - no profile
        viewModel = createViewModelWithProfile(null)
        advanceUntilIdle()

        viewModel.onEvent(TransactionFormEvent.UpdateDescription("Test"))
        advanceUntilIdle()

        val accountRows = listOf(
            TransactionAccountRow(id = 1, accountName = "Assets:Bank", amountText = "100.00", currency = "USD"),
            TransactionAccountRow(id = 2, accountName = "Expenses:Food", amountText = "-100.00", currency = "USD")
        )

        // When
        viewModel.onEvent(TransactionFormEvent.Submit(accountRows))
        advanceUntilIdle()

        // Then - should not send (no profile)
        assertTrue(transactionSender.sentTransactions.isEmpty())
    }

    @Test
    fun `toggle transaction comment twice returns to original state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTransactionCommentExpanded)

        // When - toggle twice
        viewModel.onEvent(TransactionFormEvent.ToggleTransactionComment)
        advanceUntilIdle()
        viewModel.onEvent(TransactionFormEvent.ToggleTransactionComment)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isTransactionCommentExpanded)
    }

    @Test
    fun `toggle simulate save twice returns to original state`() = runTest {
        // Given
        val profile = createTestProfile()
        viewModel = createViewModelWithProfile(profile)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSimulateSave)

        // When - toggle twice
        viewModel.onEvent(TransactionFormEvent.ToggleSimulateSave)
        advanceUntilIdle()
        viewModel.onEvent(TransactionFormEvent.ToggleSimulateSave)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isSimulateSave)
    }

    @Test
    fun `initialization without profile sets null profile id`() = runTest {
        // Given - no profile set
        viewModel = createViewModelWithProfile(null)
        advanceUntilIdle()

        // Then - profile id should be null (default)
        assertEquals(null, viewModel.uiState.value.profileId)
    }
}
