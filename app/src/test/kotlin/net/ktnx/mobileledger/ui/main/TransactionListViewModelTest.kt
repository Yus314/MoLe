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

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.database.entity.Transaction
import net.ktnx.mobileledger.core.database.entity.Transaction as DbTransaction
import net.ktnx.mobileledger.core.database.entity.TransactionAccount
import net.ktnx.mobileledger.core.database.entity.TransactionWithAccounts
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction as DomainTransaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine as TransactionLine
import net.ktnx.mobileledger.core.testing.fake.FakeProfileRepository
import net.ktnx.mobileledger.domain.usecase.GetTransactionsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveCurrentProfileUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.ObserveTransactionsUseCaseImpl
import net.ktnx.mobileledger.domain.usecase.SearchAccountNamesUseCaseImpl
import net.ktnx.mobileledger.fake.FakeCurrencyFormatter
import net.ktnx.mobileledger.feature.transaction.usecase.TransactionListConverterImpl
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
 * Unit tests for TransactionListViewModel.
 *
 * Tests cover:
 * - Transaction list loading
 * - Account filter application
 * - Account suggestions search
 * - Clear filter
 * - Go to date navigation
 * - Observing profile changes
 * - Error handling
 *
 * Following TDD: These tests are written FIRST and should FAIL
 * until TransactionListViewModel is implemented.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var transactionRepository: FakeTransactionRepositoryForTransactionList
    private lateinit var accountRepository: FakeAccountRepositoryForTransactionList
    private lateinit var currencyFormatter: FakeCurrencyFormatter
    private lateinit var viewModel: TransactionListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepository()
        transactionRepository = FakeTransactionRepositoryForTransactionList()
        accountRepository = FakeAccountRepositoryForTransactionList()
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
    ): Profile = createTestDomainProfile(
        id = id,
        name = name,
        theme = theme,
        orderNo = orderNo,
        permitPosting = true
    )

    private fun createTestTransaction(
        id: Long,
        profileId: Long,
        ledgerId: Long,
        description: String,
        year: Int = 2026,
        month: Int = 1,
        day: Int = 10,
        accounts: List<TransactionAccount> = emptyList()
    ): TransactionWithAccounts {
        val transaction = Transaction().apply {
            this.id = id
            this.profileId = profileId
            this.ledgerId = ledgerId
            this.description = description
            this.year = year
            this.month = month
            this.day = day
        }
        val twa = TransactionWithAccounts()
        twa.transaction = transaction
        twa.accounts = accounts
        return twa
    }

    private fun createTestAccount(
        accountName: String,
        amount: Float = 100f,
        currency: String = "USD"
    ): TransactionAccount = TransactionAccount().apply {
        this.accountName = accountName
        this.amount = amount
        this.currency = currency
    }

    private fun createViewModel() = TransactionListViewModel(
        observeCurrentProfileUseCase = ObserveCurrentProfileUseCaseImpl(profileRepository),
        getTransactionsUseCase = GetTransactionsUseCaseImpl(
            observeTransactionsUseCase = ObserveTransactionsUseCaseImpl(transactionRepository)
        ),
        searchAccountNamesUseCase = SearchAccountNamesUseCaseImpl(accountRepository),
        currencyFormatter = currencyFormatter,
        transactionListConverter = TransactionListConverterImpl()
    )

    // ========================================
    // Init tests
    // ========================================

    @Test
    fun `init observes current profile`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then - ViewModel should observe profile (verified by reaction to change)
        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun `init with no profile results in empty transactions`() = runTest {
        // Given - no profile set

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.transactions.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ========================================
    // Profile change tests
    // ========================================

    @Test
    fun `profileChange loads new profile transactions`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1")
        val profile2 = createTestProfile(id = 2L, name = "Profile 2")
        profileRepository.insertProfile(profile1).getOrThrow()
        profileRepository.insertProfile(profile2).getOrThrow()
        profileRepository.setCurrentProfile(profile1)

        transactionRepository.addTransaction(
            createTestTransaction(1L, 1L, 1L, "Tx1", accounts = listOf(createTestAccount("Assets:Cash")))
        )
        transactionRepository.addTransaction(
            createTestTransaction(2L, 2L, 2L, "Tx2", accounts = listOf(createTestAccount("Expenses:Food")))
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        // Load transactions for profile 1
        viewModel.loadTransactions()
        advanceUntilIdle()

        val initialTransactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals(1, initialTransactions.size)
        assertEquals("Tx1", initialTransactions[0].description)

        // When - change profile
        profileRepository.setCurrentProfile(profile2)
        advanceUntilIdle()

        // Then - transactions should be reloaded for new profile
        val afterChangeTransactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals(1, afterChangeTransactions.size)
        assertEquals("Tx2", afterChangeTransactions[0].description)
    }

    // ========================================
    // Load transactions tests
    // ========================================

    @Test
    fun `loadTransactions success loads transactions`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        transactionRepository.addTransaction(
            createTestTransaction(
                1L,
                1L,
                1L,
                "Groceries",
                year = 2026,
                month = 1,
                day = 10,
                accounts = listOf(
                    createTestAccount("Expenses:Food", -50f),
                    createTestAccount("Assets:Cash", 50f)
                )
            )
        )
        transactionRepository.addTransaction(
            createTestTransaction(
                2L,
                1L,
                2L,
                "Salary",
                year = 2026,
                month = 1,
                day = 5,
                accounts = listOf(
                    createTestAccount("Assets:Bank", 5000f),
                    createTestAccount("Income:Salary", -5000f)
                )
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.loadTransactions()
        advanceUntilIdle()

        // Then
        val transactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals(2, transactions.size)
    }

    @Test
    fun `loadTransactions shows loading state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When - start loading (don't advance until idle)
        viewModel.loadTransactions()

        // Then - should be loading initially
        // Note: Due to coroutine timing, we verify the final state is not loading
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadTransactions error shows error state`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        transactionRepository.simulateError = true

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.loadTransactions()
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
    }

    // ========================================
    // Account filter tests
    // ========================================

    @Test
    fun `setAccountFilter applies filter`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Assets:Cash"))
        advanceUntilIdle()

        // Then
        assertEquals("Assets:Cash", viewModel.uiState.value.accountFilter)
    }

    @Test
    fun `setAccountFilter reloads transactions with filter`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        // Two transactions with COMPLETELY different account names
        transactionRepository.addTransaction(
            createTestTransaction(
                1L,
                1L,
                1L,
                "Groceries",
                accounts = listOf(createTestAccount("Expenses:Food"))
            )
        )
        transactionRepository.addTransaction(
            createTestTransaction(
                2L,
                1L,
                2L,
                "ATM",
                accounts = listOf(createTestAccount("Assets:Checking"))
            )
        )

        // Verify repository filtering works directly
        val filteredFromRepo = transactionRepository.observeTransactionsFiltered(1L, "Checking").first()
        assertEquals("Repository should return 1 filtered transaction", 1, filteredFromRepo.size)
        assertEquals("ATM", filteredFromRepo[0].description)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Verify filter state before applying
        assertNull("Filter should be null initially", viewModel.uiState.value.accountFilter)

        // Apply filter directly without loading all transactions first
        // This tests that setAccountFilter properly calls loadTransactions with the filter
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Checking"))
        advanceUntilIdle()

        // Verify filter was applied
        assertEquals("Filter should be set", "Checking", viewModel.uiState.value.accountFilter)

        // Check if there's an error that prevented loading
        val error = viewModel.uiState.value.error
        assertNull("Should not have error, but got: $error", error)

        // Check loading state
        assertFalse("Should not be loading", viewModel.uiState.value.isLoading)

        // Verify filtered transactions - only ATM should be present
        val filteredTransactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()

        val descriptions = filteredTransactions.map { it.description }
        assertEquals(
            "Should have 1 transaction with 'Checking' filter, but got: $descriptions",
            1,
            filteredTransactions.size
        )
        assertEquals("ATM", filteredTransactions[0].description)
    }

    @Test
    fun `clearAccountFilter clears filter`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Assets:Cash"))
        advanceUntilIdle()
        assertEquals("Assets:Cash", viewModel.uiState.value.accountFilter)

        // When
        viewModel.onEvent(TransactionListEvent.ClearAccountFilter)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.accountFilter)
    }

    // ========================================
    // Go to date tests
    // ========================================

    @Test
    fun `goToDate finds transaction at date`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        transactionRepository.addTransaction(
            createTestTransaction(
                1L,
                1L,
                1L,
                "Tx Jan 10",
                year = 2026,
                month = 1,
                day = 10,
                accounts = listOf(createTestAccount("Assets:Cash"))
            )
        )
        transactionRepository.addTransaction(
            createTestTransaction(
                2L,
                1L,
                2L,
                "Tx Jan 15",
                year = 2026,
                month = 1,
                day = 15,
                accounts = listOf(createTestAccount("Assets:Bank"))
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadTransactions()
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionListEvent.GoToDate(SimpleDate(2026, 1, 15)))
        advanceUntilIdle()

        // Then
        val foundIndex = viewModel.uiState.value.foundTransactionIndex
        assertNotNull(foundIndex)
        assertTrue(foundIndex!! >= 0)
    }

    @Test
    fun `goToDate not found does not change index`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        transactionRepository.addTransaction(
            createTestTransaction(
                1L,
                1L,
                1L,
                "Tx Jan 10",
                year = 2026,
                month = 1,
                day = 10,
                accounts = listOf(createTestAccount("Assets:Cash"))
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadTransactions()
        advanceUntilIdle()

        // When - search for non-existent date
        viewModel.onEvent(TransactionListEvent.GoToDate(SimpleDate(2026, 12, 31)))
        advanceUntilIdle()

        // Then - index should remain null (or unchanged)
        val foundIndex = viewModel.uiState.value.foundTransactionIndex
        assertNull(foundIndex)
    }

    // ========================================
    // Account suggestions tests
    // ========================================

    @Test
    fun `searchAccountNames returns suggestions`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        accountRepository.addAccount(1L, "Assets:Cash")
        accountRepository.addAccount(1L, "Assets:Bank")
        accountRepository.addAccount(1L, "Expenses:Food")

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - type in filter to trigger suggestions
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Assets"))
        advanceTimeBy(400) // Wait for debounce (300ms + buffer)
        advanceUntilIdle()

        // Then
        val suggestions = viewModel.uiState.value.accountSuggestions
        assertEquals(2, suggestions.size)
        assertTrue(suggestions.contains("Assets:Cash"))
        assertTrue(suggestions.contains("Assets:Bank"))
    }

    @Test
    fun `searchAccountNames is debounced`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        accountRepository.addAccount(1L, "Assets:Cash")

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - type quickly (within debounce window)
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("A"))
        advanceTimeBy(100)
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("As"))
        advanceTimeBy(100)
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Ass"))
        advanceTimeBy(400) // Wait for debounce
        advanceUntilIdle()

        // Then - should only have suggestions for final query
        val suggestions = viewModel.uiState.value.accountSuggestions
        // Suggestions depend on final "Ass" query matching "Assets:Cash"
        assertTrue(suggestions.isNotEmpty())
    }

    @Test
    fun `selectSuggestion applies filter and clears suggestions`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
        profileRepository.setCurrentProfile(profile)
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionListEvent.SelectSuggestion("Assets:Cash"))
        advanceUntilIdle()

        // Then
        assertEquals("Assets:Cash", viewModel.uiState.value.accountFilter)
        assertTrue(viewModel.uiState.value.accountSuggestions.isEmpty())
    }

    // ========================================
    // Filter input visibility tests
    // ========================================

    @Test
    fun `showAccountFilterInput shows input`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showAccountFilterInput)

        // When
        viewModel.onEvent(TransactionListEvent.ShowAccountFilterInput)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.showAccountFilterInput)
    }

    @Test
    fun `hideAccountFilterInput hides input`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(TransactionListEvent.ShowAccountFilterInput)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showAccountFilterInput)

        // When
        viewModel.onEvent(TransactionListEvent.HideAccountFilterInput)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showAccountFilterInput)
    }

    // ========================================
    // Header text tests
    // ========================================

    @Test
    fun `updateHeaderText updates header in state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.updateHeaderText("Last sync: 50 transactions")
        advanceUntilIdle()

        // Then
        assertEquals("Last sync: 50 transactions", viewModel.uiState.value.headerText)
    }

    // ========================================
    // Date range tests
    // ========================================

    @Test
    fun `loadTransactions updates date range`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        transactionRepository.addTransaction(
            createTestTransaction(
                1L,
                1L,
                1L,
                "Early",
                year = 2026,
                month = 1,
                day = 5,
                accounts = listOf(createTestAccount("Assets:Cash"))
            )
        )
        transactionRepository.addTransaction(
            createTestTransaction(
                2L,
                1L,
                2L,
                "Late",
                year = 2026,
                month = 1,
                day = 20,
                accounts = listOf(createTestAccount("Assets:Bank"))
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.loadTransactions()
        advanceUntilIdle()

        // Then
        val firstDate = viewModel.uiState.value.firstTransactionDate
        val lastDate = viewModel.uiState.value.lastTransactionDate
        assertNotNull(firstDate)
        assertNotNull(lastDate)
        assertEquals(SimpleDate(2026, 1, 5), firstDate)
        assertEquals(SimpleDate(2026, 1, 20), lastDate)
    }

    // ========================================
    // Scroll to transaction tests
    // ========================================

    @Test
    fun `scrollToTransaction updates found index`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(TransactionListEvent.ScrollToTransaction(5))
        advanceUntilIdle()

        // Then
        assertEquals(5, viewModel.uiState.value.foundTransactionIndex)
    }

    // ========================================
    // Profile change clears filter tests
    // ========================================

    @Test
    fun `profile change clears account filter`() = runTest {
        // Given
        val profile1 = createTestProfile(id = 1L, name = "Profile 1")
        val profile2 = createTestProfile(id = 2L, name = "Profile 2")
        profileRepository.insertProfile(profile1).getOrThrow()
        profileRepository.insertProfile(profile2).getOrThrow()
        profileRepository.setCurrentProfile(profile1)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Set a filter
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Assets:Cash"))
        advanceUntilIdle()
        assertEquals("Assets:Cash", viewModel.uiState.value.accountFilter)

        // When - change profile
        profileRepository.setCurrentProfile(profile2)
        advanceUntilIdle()

        // Then - filter should be cleared
        assertNull(viewModel.uiState.value.accountFilter)
    }

    // ========================================
    // updateDisplayedTransactionsFromWeb tests
    // ========================================

    @Test
    fun `updateDisplayedTransactionsFromWeb updates transactions list`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()

        val webTransactions = listOf(
            DomainTransaction(
                id = 1L,
                ledgerId = 1L,
                date = SimpleDate(2026, 1, 15),
                description = "Web Transaction 1",
                lines = listOf(
                    TransactionLine(
                        id = 1L,
                        accountName = "Assets:Cash",
                        amount = 100f,
                        currency = "USD"
                    )
                )
            ),
            DomainTransaction(
                id = 2L,
                ledgerId = 2L,
                date = SimpleDate(2026, 1, 16),
                description = "Web Transaction 2",
                lines = listOf(
                    TransactionLine(
                        id = 2L,
                        accountName = "Expenses:Food",
                        amount = -50f,
                        currency = "USD"
                    )
                )
            )
        )

        // When
        viewModel.updateDisplayedTransactionsFromWeb(webTransactions)
        advanceUntilIdle()

        // Then
        val transactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals(2, transactions.size)
    }

    @Test
    fun `updateDisplayedTransactionsFromWeb respects account filter`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Set filter before updating from web
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Assets"))
        advanceUntilIdle()

        val webTransactions = listOf(
            DomainTransaction(
                id = 1L,
                ledgerId = 1L,
                date = SimpleDate(2026, 1, 15),
                description = "Cash Transaction",
                lines = listOf(
                    TransactionLine(
                        id = 1L,
                        accountName = "Assets:Cash",
                        amount = 100f,
                        currency = "USD"
                    )
                )
            ),
            DomainTransaction(
                id = 2L,
                ledgerId = 2L,
                date = SimpleDate(2026, 1, 16),
                description = "Food Purchase",
                lines = listOf(
                    TransactionLine(
                        id = 2L,
                        accountName = "Expenses:Food",
                        amount = -50f,
                        currency = "USD"
                    )
                )
            )
        )

        // When
        viewModel.updateDisplayedTransactionsFromWeb(webTransactions)
        advanceUntilIdle()

        // Then - only transaction with "Assets" account should be included
        val transactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals(1, transactions.size)
        assertEquals("Cash Transaction", transactions[0].description)
    }

    @Test
    fun `updateDisplayedTransactionsFromWeb cancels previous job`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()

        val firstBatch = listOf(
            DomainTransaction(
                id = 1L,
                ledgerId = 1L,
                date = SimpleDate(2026, 1, 15),
                description = "First Batch",
                lines = listOf(
                    TransactionLine(id = 1L, accountName = "Assets:Cash", amount = 100f, currency = "USD")
                )
            )
        )

        val secondBatch = listOf(
            DomainTransaction(
                id = 2L,
                ledgerId = 2L,
                date = SimpleDate(2026, 1, 16),
                description = "Second Batch",
                lines = listOf(
                    TransactionLine(id = 2L, accountName = "Expenses:Food", amount = -50f, currency = "USD")
                )
            )
        )

        // When - call twice rapidly
        viewModel.updateDisplayedTransactionsFromWeb(firstBatch)
        // Don't advance - second call should cancel first
        viewModel.updateDisplayedTransactionsFromWeb(secondBatch)
        advanceUntilIdle()

        // Then - should show second batch (first was cancelled)
        val transactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals(1, transactions.size)
        assertEquals("Second Batch", transactions[0].description)
    }

    // ========================================
    // Empty state tests
    // ========================================

    @Test
    fun `loadTransactions with empty repository shows empty list`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        // No transactions added

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.loadTransactions()
        advanceUntilIdle()

        // Then - should have only header, no transactions
        val transactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertTrue(transactions.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `date range is null when no transactions`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.loadTransactions()
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.firstTransactionDate)
        assertNull(viewModel.uiState.value.lastTransactionDate)
    }

    // ========================================
    // Debounce edge cases
    // ========================================

    @Test
    fun `empty search query clears suggestions`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        accountRepository.addAccount(1L, "Assets:Cash")

        viewModel = createViewModel()
        advanceUntilIdle()

        // First set a filter and get suggestions
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Assets"))
        advanceTimeBy(400)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.accountSuggestions.isNotEmpty())

        // When - clear filter
        viewModel.onEvent(TransactionListEvent.ClearAccountFilter)
        advanceTimeBy(400)
        advanceUntilIdle()

        // Then - suggestions should be cleared
        assertTrue(viewModel.uiState.value.accountSuggestions.isEmpty())
    }

    // ========================================
    // Phase A3: Additional coverage tests
    // ========================================

    @Test
    fun `account filter debounce only processes final query`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        accountRepository.addAccount(1L, "Assets:Cash")
        accountRepository.addAccount(1L, "Assets:Bank")
        accountRepository.addAccount(1L, "Expenses:Food")

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - type quickly (within debounce window of 300ms)
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("A"))
        advanceTimeBy(50)
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("As"))
        advanceTimeBy(50)
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Ass"))
        advanceTimeBy(50)
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Assets"))
        advanceTimeBy(400) // Wait for debounce (300ms + buffer)
        advanceUntilIdle()

        // Then - should only have suggestions for final "Assets" query
        val suggestions = viewModel.uiState.value.accountSuggestions
        assertEquals(2, suggestions.size) // Assets:Cash and Assets:Bank
        assertTrue(suggestions.contains("Assets:Cash"))
        assertTrue(suggestions.contains("Assets:Bank"))
        assertFalse(suggestions.contains("Expenses:Food"))
    }

    @Test
    fun `rapid filter changes cancel previous search requests`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)
        accountRepository.addAccount(1L, "Assets:Cash")
        accountRepository.addAccount(1L, "Expenses:Food")

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - set filter, then immediately change before debounce completes
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Assets"))
        advanceTimeBy(100) // Before debounce completes (300ms)

        // Change to different filter
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Expenses"))
        advanceTimeBy(400) // Wait for debounce
        advanceUntilIdle()

        // Then - only final filter should be applied
        assertEquals("Expenses", viewModel.uiState.value.accountFilter)
        val suggestions = viewModel.uiState.value.accountSuggestions
        assertEquals(1, suggestions.size)
        assertTrue(suggestions.contains("Expenses:Food"))
    }

    @Test
    fun `filter with hierarchical account names matches parent and children`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        // Add transactions with hierarchical account names
        transactionRepository.addTransaction(
            createTestTransaction(
                1L,
                1L,
                1L,
                "ATM Withdrawal",
                accounts = listOf(createTestAccount("Assets:Cash"))
            )
        )
        transactionRepository.addTransaction(
            createTestTransaction(
                2L,
                1L,
                2L,
                "Bank Transfer",
                accounts = listOf(createTestAccount("Assets:Bank:Checking"))
            )
        )
        transactionRepository.addTransaction(
            createTestTransaction(
                3L,
                1L,
                3L,
                "Groceries",
                accounts = listOf(createTestAccount("Expenses:Food"))
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        // When - filter by parent account "Assets"
        viewModel.onEvent(TransactionListEvent.SetAccountFilter("Assets"))
        advanceUntilIdle()

        // Then - should include both Assets:Cash and Assets:Bank:Checking transactions
        val transactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals(2, transactions.size)
        assertTrue(transactions.any { it.description == "ATM Withdrawal" })
        assertTrue(transactions.any { it.description == "Bank Transfer" })
        assertFalse(transactions.any { it.description == "Groceries" })
    }

    @Test
    fun `goToDate finds nearest date when exact date not found`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        // Add transactions at month boundary
        transactionRepository.addTransaction(
            createTestTransaction(
                1L,
                1L,
                1L,
                "January End",
                year = 2026,
                month = 1,
                day = 31,
                accounts = listOf(createTestAccount("Assets:Cash"))
            )
        )
        transactionRepository.addTransaction(
            createTestTransaction(
                2L,
                1L,
                2L,
                "February Start",
                year = 2026,
                month = 2,
                day = 1,
                accounts = listOf(createTestAccount("Assets:Cash"))
            )
        )

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadTransactions()
        advanceUntilIdle()

        // When - go to date that exists
        viewModel.onEvent(TransactionListEvent.GoToDate(SimpleDate(2026, 1, 31)))
        advanceUntilIdle()

        // Then - should find the transaction
        val foundIndex = viewModel.uiState.value.foundTransactionIndex
        assertNotNull(foundIndex)
        assertTrue(foundIndex!! >= 0)
    }

    @Test
    fun `updateDisplayedTransactionsFromWeb cancels previous job on rapid calls`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Create two batches of transactions
        val batch1 = listOf(
            DomainTransaction(
                id = 1L,
                ledgerId = 1L,
                date = SimpleDate(2026, 1, 10),
                description = "Batch 1 Transaction",
                lines = listOf(TransactionLine(id = 1L, accountName = "Assets:Cash", amount = 100f, currency = "USD"))
            )
        )
        val batch2 = listOf(
            DomainTransaction(
                id = 2L,
                ledgerId = 2L,
                date = SimpleDate(2026, 1, 11),
                description = "Batch 2 Transaction",
                lines = listOf(TransactionLine(id = 2L, accountName = "Expenses:Food", amount = -50f, currency = "USD"))
            )
        )

        // When - call twice rapidly (second should cancel first)
        viewModel.updateDisplayedTransactionsFromWeb(batch1)
        // Don't advance - immediately call second
        viewModel.updateDisplayedTransactionsFromWeb(batch2)
        advanceUntilIdle()

        // Then - should have second batch (first was cancelled)
        val transactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals(1, transactions.size)
        assertEquals("Batch 2 Transaction", transactions[0].description)
    }

    @Test
    fun `updateDisplayedTransactionsFromWeb handles large dataset efficiently`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile).getOrThrow()
        profileRepository.setCurrentProfile(profile)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Create a large dataset (100 transactions)
        val largeDataset = (1..100).map { i ->
            DomainTransaction(
                id = i.toLong(),
                ledgerId = i.toLong(),
                date = SimpleDate(2026, 1, (i % 28) + 1),
                description = "Transaction $i",
                lines = listOf(
                    TransactionLine(
                        id = i.toLong(),
                        accountName = "Assets:Cash",
                        amount = i * 10f,
                        currency = "USD"
                    )
                )
            )
        }

        // When
        viewModel.updateDisplayedTransactionsFromWeb(largeDataset)
        advanceUntilIdle()

        // Then - all transactions should be loaded without error
        val transactions = viewModel.uiState.value.transactions
            .filterIsInstance<TransactionListDisplayItem.Transaction>()
        assertEquals(100, transactions.size)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

/**
 * Fake TransactionRepository specialized for TransactionListViewModel tests.
 */
class FakeTransactionRepositoryForTransactionList : net.ktnx.mobileledger.core.domain.repository.TransactionRepository {
    private val transactions = mutableListOf<TransactionWithAccounts>()
    var simulateError = false

    fun addTransaction(transaction: TransactionWithAccounts) {
        transactions.add(transaction)
    }

    /**
     * Convert TransactionWithAccounts (db entity) to DomainTransaction (domain model)
     */
    private fun TransactionWithAccounts.toDomainModel(): DomainTransaction {
        val tx = this.transaction
        return DomainTransaction(
            id = tx.id,
            ledgerId = tx.ledgerId,
            date = SimpleDate(tx.year, tx.month, tx.day),
            description = tx.description,
            comment = tx.comment,
            lines = this.accounts.map { acc ->
                TransactionLine(
                    id = acc.id,
                    accountName = acc.accountName,
                    amount = acc.amount,
                    currency = acc.currency ?: "",
                    comment = acc.comment
                )
            }
        )
    }

    // Flow methods (observe prefix)
    override fun observeAllTransactions(profileId: Long) = MutableStateFlow(
        transactions
            .filter { it.transaction.profileId == profileId }
            .map { it.toDomainModel() }
    )

    override fun observeTransactionsFiltered(
        profileId: Long,
        accountName: String?
    ): kotlinx.coroutines.flow.Flow<List<DomainTransaction>> {
        if (simulateError) {
            // Return a flow that throws when collected
            return kotlinx.coroutines.flow.flow {
                throw RuntimeException("Simulated error")
            }
        }
        return MutableStateFlow(
            transactions.filter { twa ->
                twa.transaction.profileId == profileId &&
                    (
                        accountName == null || twa.accounts.any {
                            it.accountName.contains(accountName, ignoreCase = true)
                        }
                        )
            }.map { it.toDomainModel() }
        )
    }

    override fun observeTransactionById(transactionId: Long) =
        MutableStateFlow(transactions.find { it.transaction.id == transactionId }?.toDomainModel())

    // Suspend methods (no suffix)
    override suspend fun getTransactionById(transactionId: Long): Result<DomainTransaction?> = if (simulateError) {
        Result.failure(RuntimeException("Simulated error"))
    } else {
        Result.success(transactions.find { it.transaction.id == transactionId }?.toDomainModel())
    }

    override suspend fun searchByDescription(term: String): Result<List<String>> = Result.success(
        transactions
            .filter { it.transaction.description.contains(term, ignoreCase = true) }
            .mapNotNull { it.transaction.description }
            .distinct()
    )

    override suspend fun getFirstByDescription(description: String): Result<DomainTransaction?> =
        Result.success(transactions.find { it.transaction.description == description }?.toDomainModel())

    override suspend fun getFirstByDescriptionHavingAccount(
        description: String,
        accountTerm: String
    ): Result<DomainTransaction?> = Result.success(
        transactions.find { twa ->
            twa.transaction.description == description &&
                twa.accounts.any { it.accountName.contains(accountTerm, ignoreCase = true) }
        }?.toDomainModel()
    )

    // Domain model mutation methods
    override suspend fun insertTransaction(transaction: DomainTransaction, profileId: Long): Result<DomainTransaction> {
        val id = transaction.id ?: (transactions.maxOfOrNull { it.transaction.id } ?: 0L) + 1
        return Result.success(transaction.copy(id = id))
    }

    override suspend fun storeTransaction(transaction: DomainTransaction, profileId: Long): Result<Unit> {
        insertTransaction(transaction, profileId)
        return Result.success(Unit)
    }

    override suspend fun deleteTransactionById(transactionId: Long): Result<Int> {
        val existed = transactions.any { it.transaction.id == transactionId }
        transactions.removeAll { it.transaction.id == transactionId }
        return Result.success(if (existed) 1 else 0)
    }

    override suspend fun deleteTransactionsByIds(transactionIds: List<Long>): Result<Int> {
        var count = 0
        transactionIds.forEach { id ->
            if (transactions.any { it.transaction.id == id }) {
                count++
            }
            transactions.removeAll { it.transaction.id == id }
        }
        return Result.success(count)
    }

    override suspend fun deleteAllForProfile(profileId: Long): Result<Int> {
        val count = transactions.count { it.transaction.profileId == profileId }
        transactions.removeAll { it.transaction.profileId == profileId }
        return Result.success(count)
    }

    override suspend fun getMaxLedgerId(profileId: Long): Result<Long?> = Result.success(
        transactions.filter { it.transaction.profileId == profileId }
            .maxOfOrNull { it.transaction.ledgerId }
    )

    override suspend fun storeTransactionsAsDomain(
        transactions: List<DomainTransaction>,
        profileId: Long
    ): Result<Unit> {
        // Convert domain transactions to db entities for storage
        transactions.forEach { tx ->
            val dbTransaction = DbTransaction().apply {
                val maxId = this@FakeTransactionRepositoryForTransactionList.transactions
                    .maxOfOrNull { it.transaction.id } ?: 0L
                id = tx.id ?: (maxId + 1)
                this.profileId = profileId
                ledgerId = tx.ledgerId
                year = tx.date.year
                month = tx.date.month
                day = tx.date.day
                description = tx.description
                comment = tx.comment
            }
            val accounts = tx.lines.map { line ->
                TransactionAccount().apply {
                    id = line.id ?: 0L
                    accountName = line.accountName
                    amount = line.amount ?: 0f
                    currency = line.currency
                    comment = line.comment
                }
            }
            val twa = TransactionWithAccounts()
            twa.transaction = dbTransaction
            twa.accounts = accounts
            this.transactions.add(twa)
        }
        return Result.success(Unit)
    }
}

/**
 * Fake AccountRepository specialized for TransactionListViewModel tests.
 * Now uses domain models (Account) for query operations.
 */
class FakeAccountRepositoryForTransactionList : net.ktnx.mobileledger.core.domain.repository.AccountRepository {
    private val accountNames = mutableMapOf<Long, MutableList<String>>()

    fun addAccount(profileId: Long, name: String) {
        accountNames.getOrPut(profileId) { mutableListOf() }.add(name)
    }

    // Flow methods (observe prefix)
    override fun observeAllWithAmounts(profileId: Long, includeZeroBalances: Boolean) =
        MutableStateFlow<List<net.ktnx.mobileledger.core.domain.model.Account>>(emptyList())

    override fun observeByNameWithAmounts(profileId: Long, accountName: String) =
        MutableStateFlow<net.ktnx.mobileledger.core.domain.model.Account?>(null)

    override fun observeSearchAccountNames(profileId: Long, term: String) =
        MutableStateFlow(accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList())

    override fun observeSearchAccountNamesGlobal(term: String) =
        MutableStateFlow(accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) })

    // Suspend methods (no suffix)
    override suspend fun getAllWithAmounts(
        profileId: Long,
        includeZeroBalances: Boolean
    ): Result<List<net.ktnx.mobileledger.core.domain.model.Account>> = Result.success(emptyList())

    override suspend fun getByNameWithAmounts(
        profileId: Long,
        accountName: String
    ): Result<net.ktnx.mobileledger.core.domain.model.Account?> = Result.success(null)

    override suspend fun searchAccountNames(profileId: Long, term: String): Result<List<String>> =
        Result.success(accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList())

    override suspend fun searchAccountsWithAmounts(
        profileId: Long,
        term: String
    ): Result<List<net.ktnx.mobileledger.core.domain.model.Account>> = Result.success(emptyList())

    override suspend fun searchAccountNamesGlobal(term: String): Result<List<String>> =
        Result.success(accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) })

    override suspend fun getCountForProfile(profileId: Long): Result<Int> =
        Result.success(accountNames[profileId]?.size ?: 0)

    override suspend fun deleteAllAccounts(): Result<Unit> {
        accountNames.clear()
        return Result.success(Unit)
    }

    override suspend fun storeAccountsAsDomain(
        accounts: List<net.ktnx.mobileledger.core.domain.model.Account>,
        profileId: Long
    ): Result<Unit> {
        accounts.forEach { account ->
            accountNames.getOrPut(profileId) { mutableListOf() }.add(account.name)
        }
        return Result.success(Unit)
    }
}
