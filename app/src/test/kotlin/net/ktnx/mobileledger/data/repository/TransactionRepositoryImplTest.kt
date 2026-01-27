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

package net.ktnx.mobileledger.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.database.dao.AccountDAO
import net.ktnx.mobileledger.core.database.dao.AccountValueDAO
import net.ktnx.mobileledger.core.database.dao.TransactionAccountDAO
import net.ktnx.mobileledger.core.database.dao.TransactionDAO
import net.ktnx.mobileledger.core.database.entity.Account
import net.ktnx.mobileledger.core.database.entity.AccountValue
import net.ktnx.mobileledger.core.database.entity.Transaction
import net.ktnx.mobileledger.core.database.entity.TransactionAccount
import net.ktnx.mobileledger.core.database.entity.TransactionWithAccounts
import net.ktnx.mobileledger.core.domain.model.AppException
import net.ktnx.mobileledger.core.domain.model.Transaction as DomainTransaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine
import net.ktnx.mobileledger.domain.usecase.AppExceptionMapper
import net.ktnx.mobileledger.domain.usecase.sync.SyncExceptionMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TransactionRepositoryImpl].
 *
 * Tests verify:
 * - Query operations (observe and get)
 * - Mutation operations (insert, store, delete)
 * - Error handling with Result<T>
 * - Domain model mapping
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockTransactionDAO: TransactionDAO
    private lateinit var mockTransactionAccountDAO: TransactionAccountDAO
    private lateinit var mockAccountDAO: AccountDAO
    private lateinit var mockAccountValueDAO: AccountValueDAO
    private lateinit var appExceptionMapper: AppExceptionMapper
    private lateinit var repository: TransactionRepositoryImpl

    private val testProfileId = 1L

    @Before
    fun setup() {
        mockTransactionDAO = mockk(relaxed = true)
        mockTransactionAccountDAO = mockk(relaxed = true)
        mockAccountDAO = mockk(relaxed = true)
        mockAccountValueDAO = mockk(relaxed = true)
        appExceptionMapper = AppExceptionMapper(SyncExceptionMapper())

        repository = TransactionRepositoryImpl(
            transactionDAO = mockTransactionDAO,
            transactionAccountDAO = mockTransactionAccountDAO,
            accountDAO = mockAccountDAO,
            accountValueDAO = mockAccountValueDAO,
            appExceptionMapper = appExceptionMapper,
            ioDispatcher = testDispatcher
        )
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createDbTransaction(
        id: Long = 1L,
        profileId: Long = testProfileId,
        ledgerId: Long = 100L,
        description: String = "Test Transaction",
        year: Int = 2024,
        month: Int = 1,
        day: Int = 15
    ): Transaction = Transaction().apply {
        this.id = id
        this.profileId = profileId
        this.ledgerId = ledgerId
        this.description = description
        this.year = year
        this.month = month
        this.day = day
        this.generation = 1L
    }

    private fun createDbTransactionAccount(
        id: Long = 1L,
        transactionId: Long = 1L,
        accountName: String = "Expenses:Food",
        amount: Float = 100.0f,
        currency: String = "USD",
        orderNo: Int = 0
    ): TransactionAccount = TransactionAccount().apply {
        this.id = id
        this.transactionId = transactionId
        this.accountName = accountName
        this.amount = amount
        this.currency = currency
        this.orderNo = orderNo
        this.generation = 1L
    }

    private fun createDbTransactionWithAccounts(
        transaction: Transaction = createDbTransaction(),
        accounts: List<TransactionAccount> = listOf(
            createDbTransactionAccount(orderNo = 0),
            createDbTransactionAccount(id = 2L, accountName = "Assets:Bank", amount = -100.0f, orderNo = 1)
        )
    ): TransactionWithAccounts = TransactionWithAccounts().apply {
        this.transaction = transaction
        this.accounts = accounts
    }

    private fun createDomainTransaction(
        id: Long? = null,
        ledgerId: Long = 100L,
        description: String = "Test Transaction"
    ): DomainTransaction = DomainTransaction(
        id = id,
        ledgerId = ledgerId,
        date = SimpleDate.today(),
        description = description,
        comment = null,
        lines = listOf(
            TransactionLine(
                id = null,
                accountName = "Expenses:Food",
                amount = 100.0f,
                currency = "USD",
                comment = null
            ),
            TransactionLine(
                id = null,
                accountName = "Assets:Bank",
                amount = -100.0f,
                currency = "USD",
                comment = null
            )
        )
    )

    // ========================================
    // Query Operations - Flow tests
    // ========================================

    @Test
    fun `observeAllTransactions returns mapped domain models`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbTransactionWithAccounts()
        every { mockTransactionDAO.getAllWithAccountsFiltered(testProfileId, null) } returns flowOf(listOf(dbEntity))

        // When
        val result = repository.observeAllTransactions(testProfileId).first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test Transaction", result[0].description)
        verify { mockTransactionDAO.getAllWithAccountsFiltered(testProfileId, null) }
    }

    @Test
    fun `observeAllTransactions returns empty list when no transactions`() = runTest(testDispatcher) {
        // Given
        every { mockTransactionDAO.getAllWithAccountsFiltered(testProfileId, null) } returns flowOf(emptyList())

        // When
        val result = repository.observeAllTransactions(testProfileId).first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeTransactionsFiltered with null account returns all transactions`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbTransactionWithAccounts()
        every { mockTransactionDAO.getAllWithAccountsFiltered(testProfileId, null) } returns flowOf(listOf(dbEntity))

        // When
        val result = repository.observeTransactionsFiltered(testProfileId, null).first()

        // Then
        assertEquals(1, result.size)
        verify { mockTransactionDAO.getAllWithAccountsFiltered(testProfileId, null) }
    }

    @Test
    fun `observeTransactionsFiltered with account name filters correctly`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbTransactionWithAccounts()
        every {
            mockTransactionDAO.getAllWithAccountsFiltered(testProfileId, "Expenses")
        } returns flowOf(listOf(dbEntity))

        // When
        val result = repository.observeTransactionsFiltered(testProfileId, "Expenses").first()

        // Then
        assertEquals(1, result.size)
        verify { mockTransactionDAO.getAllWithAccountsFiltered(testProfileId, "Expenses") }
    }

    @Test
    fun `observeTransactionById returns mapped domain model`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbTransactionWithAccounts()
        every { mockTransactionDAO.getByIdWithAccounts(1L) } returns flowOf(dbEntity)

        // When
        val result = repository.observeTransactionById(1L).first()

        // Then
        assertNotNull(result)
        assertEquals("Test Transaction", result?.description)
    }

    // Note: Flow-based "not found" test removed because DAO returns Flow<TransactionWithAccounts>
    // (non-nullable). The sync version getByIdWithAccountsSync returns nullable and is tested below.

    // ========================================
    // Query Operations - Suspend tests
    // ========================================

    @Test
    fun `getTransactionById returns Result success with mapped domain model`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbTransactionWithAccounts()
        coEvery { mockTransactionDAO.getByIdWithAccountsSync(1L) } returns dbEntity

        // When
        val result = repository.getTransactionById(1L)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("Test Transaction", result.getOrNull()?.description)
    }

    @Test
    fun `getTransactionById returns Result success with null when not found`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTransactionDAO.getByIdWithAccountsSync(999L) } returns null

        // When
        val result = repository.getTransactionById(999L)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getTransactionById returns Result failure on exception`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTransactionDAO.getByIdWithAccountsSync(1L) } throws RuntimeException("DB error")

        // When
        val result = repository.getTransactionById(1L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    @Test
    fun `searchByDescription calls DAO with uppercase term`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTransactionDAO.lookupDescriptionSync("TEST") } returns emptyList()

        // When
        val result = repository.searchByDescription("test")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockTransactionDAO.lookupDescriptionSync("TEST") }
    }

    @Test
    fun `getFirstByDescription returns mapped domain model`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbTransactionWithAccounts()
        coEvery { mockTransactionDAO.getFirstByDescriptionSync("Groceries") } returns dbEntity

        // When
        val result = repository.getFirstByDescription("Groceries")

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    // ========================================
    // Mutation Operations tests
    // ========================================

    @Test
    fun `insertTransaction calls DAO and returns success`() = runTest(testDispatcher) {
        // Given
        val domainTransaction = createDomainTransaction()
        coEvery { mockTransactionDAO.getGenerationSync(testProfileId) } returns 1L
        coEvery { mockTransactionDAO.getMaxLedgerIdSync(testProfileId) } returns 99L
        coEvery { mockTransactionDAO.insertSync(any<Transaction>()) } returns 1L
        coEvery { mockTransactionAccountDAO.insertSync(any<TransactionAccount>()) } returns 1L
        coEvery { mockAccountDAO.getByNameSync(any<Long>(), any<String>()) } returns null
        coEvery { mockAccountDAO.insertSync(any<Account>()) } returns 1L
        coEvery { mockAccountValueDAO.getByCurrencySync(any<Long>(), any<String>()) } returns null
        coEvery { mockAccountValueDAO.insertSync(any<AccountValue>()) } returns 1L

        // When
        val result = repository.insertTransaction(domainTransaction, testProfileId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockTransactionDAO.insertSync(any<Transaction>()) }
    }

    @Test
    fun `deleteTransactionById calls DAO and returns count`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTransactionDAO.deleteByIdSync(1L) } returns 1

        // When
        val result = repository.deleteTransactionById(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        coVerify { mockTransactionDAO.deleteByIdSync(1L) }
    }

    @Test
    fun `deleteTransactionsByIds calls DAO and returns count`() = runTest(testDispatcher) {
        // Given
        val ids = listOf(1L, 2L, 3L)
        coEvery { mockTransactionDAO.deleteByIdsSync(ids) } returns 3

        // When
        val result = repository.deleteTransactionsByIds(ids)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
        coVerify { mockTransactionDAO.deleteByIdsSync(ids) }
    }

    @Test
    fun `deleteAllForProfile calls DAO and returns count`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTransactionDAO.deleteAllSync(testProfileId) } returns 10

        // When
        val result = repository.deleteAllForProfile(testProfileId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrNull())
    }

    // ========================================
    // Sync Operations tests
    // ========================================

    @Test
    fun `getMaxLedgerId returns null when no transactions`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTransactionDAO.getMaxLedgerIdSync(testProfileId) } returns 0L

        // When
        val result = repository.getMaxLedgerId(testProfileId)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getMaxLedgerId returns value when transactions exist`() = runTest(testDispatcher) {
        // Given
        coEvery { mockTransactionDAO.getMaxLedgerIdSync(testProfileId) } returns 100L

        // When
        val result = repository.getMaxLedgerId(testProfileId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(100L, result.getOrNull())
    }

    @Test
    fun `storeTransactionsAsDomain purges old transactions`() = runTest(testDispatcher) {
        // Given
        val domainTransactions = listOf(createDomainTransaction())
        coEvery { mockTransactionDAO.getGenerationSync(testProfileId) } returns 1L
        coEvery { mockTransactionDAO.getByLedgerId(any(), any()) } returns null
        coEvery { mockTransactionDAO.insertSync(any()) } returns 1L
        coEvery { mockTransactionAccountDAO.insertSync(any()) } returns 1L
        coEvery { mockTransactionDAO.purgeOldTransactionsSync(testProfileId, 2L) } returns 5
        coEvery { mockTransactionDAO.purgeOldTransactionAccountsSync(testProfileId, 2L) } returns 10

        // When
        val result = repository.storeTransactionsAsDomain(domainTransactions, testProfileId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockTransactionDAO.purgeOldTransactionsSync(testProfileId, 2L) }
        coVerify { mockTransactionDAO.purgeOldTransactionAccountsSync(testProfileId, 2L) }
    }
}
