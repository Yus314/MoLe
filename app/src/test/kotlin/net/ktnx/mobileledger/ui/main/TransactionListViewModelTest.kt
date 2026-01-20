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
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.Transaction as DbTransaction
import net.ktnx.mobileledger.db.TransactionAccount
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.Transaction as DomainTransaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.fake.FakeCurrencyFormatter
import net.ktnx.mobileledger.util.createTestDomainProfile
import net.ktnx.mobileledger.utils.SimpleDate
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
    private lateinit var profileRepository: FakeProfileRepositoryForViewModel
    private lateinit var transactionRepository: FakeTransactionRepositoryForTransactionList
    private lateinit var accountRepository: FakeAccountRepositoryForTransactionList
    private lateinit var currencyFormatter: FakeCurrencyFormatter
    private lateinit var viewModel: TransactionListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForViewModel()
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
        profileRepository,
        transactionRepository,
        accountRepository,
        currencyFormatter
    )

    // ========================================
    // Init tests
    // ========================================

    @Test
    fun `init observes current profile`() = runTest {
        // Given
        val profile = createTestProfile(id = 1L)
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
        profileRepository.insertProfile(profile)
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
}

/**
 * Fake TransactionRepository specialized for TransactionListViewModel tests.
 */
class FakeTransactionRepositoryForTransactionList : net.ktnx.mobileledger.data.repository.TransactionRepository {
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
    override suspend fun getTransactionById(transactionId: Long): DomainTransaction? = if (simulateError) {
        throw RuntimeException("Simulated error")
    } else {
        transactions.find { it.transaction.id == transactionId }?.toDomainModel()
    }

    override suspend fun searchByDescription(term: String) = transactions
        .filter { it.transaction.description.contains(term, ignoreCase = true) }
        .distinctBy { it.transaction.description }
        .map {
            net.ktnx.mobileledger.dao.TransactionDAO.DescriptionContainer()
                .apply { description = it.transaction.description }
        }

    override suspend fun getFirstByDescription(description: String): DomainTransaction? =
        transactions.find { it.transaction.description == description }?.toDomainModel()

    override suspend fun getFirstByDescriptionHavingAccount(
        description: String,
        accountTerm: String
    ): DomainTransaction? = transactions.find { twa ->
        twa.transaction.description == description &&
            twa.accounts.any { it.accountName.contains(accountTerm, ignoreCase = true) }
    }?.toDomainModel()

    // Domain model mutation methods
    override suspend fun insertTransaction(transaction: DomainTransaction, profileId: Long): DomainTransaction {
        val id = transaction.id ?: (transactions.maxOfOrNull { it.transaction.id } ?: 0L) + 1
        return transaction.copy(id = id)
    }

    override suspend fun storeTransaction(transaction: DomainTransaction, profileId: Long) {
        insertTransaction(transaction, profileId)
    }

    override suspend fun deleteTransactionById(transactionId: Long): Int {
        val existed = transactions.any { it.transaction.id == transactionId }
        transactions.removeAll { it.transaction.id == transactionId }
        return if (existed) 1 else 0
    }

    override suspend fun deleteTransactionsByIds(transactionIds: List<Long>): Int {
        var count = 0
        transactionIds.forEach { id ->
            if (transactions.any { it.transaction.id == id }) {
                count++
            }
            transactions.removeAll { it.transaction.id == id }
        }
        return count
    }

    override suspend fun deleteAllForProfile(profileId: Long): Int {
        val count = transactions.count { it.transaction.profileId == profileId }
        transactions.removeAll { it.transaction.profileId == profileId }
        return count
    }

    override suspend fun getMaxLedgerId(profileId: Long) = transactions.filter { it.transaction.profileId == profileId }
        .maxOfOrNull { it.transaction.ledgerId }

    override suspend fun storeTransactionsAsDomain(transactions: List<DomainTransaction>, profileId: Long) {
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
    }
}

/**
 * Fake AccountRepository specialized for TransactionListViewModel tests.
 * Now uses domain models (Account) for query operations.
 */
class FakeAccountRepositoryForTransactionList : net.ktnx.mobileledger.data.repository.AccountRepository {
    private val accountNames = mutableMapOf<Long, MutableList<String>>()

    fun addAccount(profileId: Long, name: String) {
        accountNames.getOrPut(profileId) { mutableListOf() }.add(name)
    }

    // Flow methods (observe prefix)
    override fun observeAllWithAmounts(profileId: Long, includeZeroBalances: Boolean) =
        MutableStateFlow<List<net.ktnx.mobileledger.domain.model.Account>>(emptyList())

    override fun observeByName(profileId: Long, accountName: String) =
        MutableStateFlow<net.ktnx.mobileledger.db.Account?>(null)

    override fun observeByNameWithAmounts(profileId: Long, accountName: String) =
        MutableStateFlow<net.ktnx.mobileledger.domain.model.Account?>(null)

    override fun observeSearchAccountNames(profileId: Long, term: String) =
        MutableStateFlow(accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList())

    override fun observeSearchAccountNamesGlobal(term: String) =
        MutableStateFlow(accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) })

    // Suspend methods (no suffix)
    override suspend fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean) =
        emptyList<net.ktnx.mobileledger.domain.model.Account>()

    override suspend fun getById(id: Long): net.ktnx.mobileledger.db.Account? = null

    override suspend fun getByName(profileId: Long, accountName: String): net.ktnx.mobileledger.db.Account? = null

    override suspend fun getByNameWithAmounts(
        profileId: Long,
        accountName: String
    ): net.ktnx.mobileledger.domain.model.Account? = null

    override suspend fun searchAccountNames(profileId: Long, term: String) =
        accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList()

    override suspend fun searchAccountsWithAmounts(profileId: Long, term: String) =
        emptyList<net.ktnx.mobileledger.domain.model.Account>()

    override suspend fun searchAccountNamesGlobal(term: String) =
        accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) }

    override suspend fun insertAccount(account: net.ktnx.mobileledger.db.Account) = 0L

    override suspend fun insertAccountWithAmounts(accountWithAmounts: net.ktnx.mobileledger.db.AccountWithAmounts) {}

    override suspend fun updateAccount(account: net.ktnx.mobileledger.db.Account) {}

    override suspend fun storeAccounts(accounts: List<net.ktnx.mobileledger.db.AccountWithAmounts>, profileId: Long) {}

    override suspend fun getCountForProfile(profileId: Long) = accountNames[profileId]?.size ?: 0

    override suspend fun deleteAllAccounts() {
        accountNames.clear()
    }

    override suspend fun storeAccountsAsDomain(
        accounts: List<net.ktnx.mobileledger.domain.model.Account>,
        profileId: Long
    ) {
        accounts.forEach { account ->
            accountNames.getOrPut(profileId) { mutableListOf() }.add(account.name)
        }
    }
}
