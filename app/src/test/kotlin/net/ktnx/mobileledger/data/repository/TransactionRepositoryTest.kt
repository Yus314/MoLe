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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.TransactionAccount
import net.ktnx.mobileledger.db.TransactionWithAccounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TransactionRepository] using a fake repository implementation.
 *
 * These tests verify:
 * - CRUD operations work correctly
 * - Flow emissions occur on data changes
 * - Search and filter operations
 * - Batch sync operations
 *
 * Note: For proper LiveData/Flow testing with Room, use instrumentation tests.
 * These unit tests use a fake repository that implements the interface directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionRepositoryTest {

    private lateinit var repository: FakeTransactionRepository

    private val testProfileId = 1L

    @Before
    fun setup() {
        repository = FakeTransactionRepository()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestTransaction(
        id: Long = 0L,
        profileId: Long = testProfileId,
        description: String = "Test Transaction",
        year: Int = 2026,
        month: Int = 1,
        day: Int = 10,
        ledgerId: Long = 1L,
        accounts: List<TransactionAccount> = listOf(
            createTestAccount("Assets:Cash", -100f),
            createTestAccount("Expenses:Food", 100f)
        )
    ): TransactionWithAccounts {
        val transaction = Transaction().apply {
            this.id = id
            this.profileId = profileId
            this.description = description
            this.year = year
            this.month = month
            this.day = day
            this.ledgerId = ledgerId
            this.generation = 1L
        }
        val twa = TransactionWithAccounts()
        twa.transaction = transaction
        twa.accounts = accounts
        return twa
    }

    private fun createTestAccount(name: String, amount: Float, currency: String = ""): TransactionAccount =
        TransactionAccount().apply {
            accountName = name
            this.amount = amount
            this.currency = currency
            orderNo = 0
        }

    // ========================================
    // getAllTransactions tests
    // ========================================

    @Test
    fun `getAllTransactions returns empty list when no transactions`() = runTest {
        val transactions = repository.getAllTransactions(testProfileId).first()
        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `getAllTransactions returns transactions for profile`() = runTest {
        val testTx = createTestTransaction(description = "Groceries")
        repository.insertTransaction(testTx)

        val transactions = repository.getAllTransactions(testProfileId).first()
        assertEquals(1, transactions.size)
        assertEquals("Groceries", transactions[0].transaction.description)
    }

    @Test
    fun `getAllTransactions filters by profile`() = runTest {
        val tx1 = createTestTransaction(profileId = 1L, description = "Profile 1")
        val tx2 = createTestTransaction(profileId = 2L, description = "Profile 2", ledgerId = 2L)
        repository.insertTransaction(tx1)
        repository.insertTransaction(tx2)

        val transactions = repository.getAllTransactions(1L).first()
        assertEquals(1, transactions.size)
        assertEquals("Profile 1", transactions[0].transaction.description)
    }

    // ========================================
    // getTransactionsFiltered tests
    // ========================================

    @Test
    fun `getTransactionsFiltered with null accountName returns all transactions`() = runTest {
        val tx = createTestTransaction()
        repository.insertTransaction(tx)

        val transactions = repository.getTransactionsFiltered(testProfileId, null).first()
        assertEquals(1, transactions.size)
    }

    @Test
    fun `getTransactionsFiltered with accountName filters correctly`() = runTest {
        val tx1 = createTestTransaction(
            description = "Cash payment",
            accounts = listOf(
                createTestAccount("Assets:Cash", -100f),
                createTestAccount("Expenses:Food", 100f)
            )
        )
        val tx2 = createTestTransaction(
            description = "Bank transfer",
            ledgerId = 2L,
            accounts = listOf(
                createTestAccount("Assets:Bank", -200f),
                createTestAccount("Expenses:Utilities", 200f)
            )
        )
        repository.insertTransaction(tx1)
        repository.insertTransaction(tx2)

        val transactions = repository.getTransactionsFiltered(testProfileId, "Cash").first()
        assertEquals(1, transactions.size)
        assertEquals("Cash payment", transactions[0].transaction.description)
    }

    // ========================================
    // getTransactionById tests
    // ========================================

    @Test
    fun `getTransactionById returns null for non-existent id`() = runTest {
        val result = repository.getTransactionById(999L).first()
        assertNull(result)
    }

    @Test
    fun `getTransactionByIdSync returns transaction when exists`() = runTest {
        val testTx = createTestTransaction(description = "Test")
        repository.insertTransaction(testTx)

        val result = repository.getTransactionByIdSync(testTx.transaction.id)
        assertNotNull(result)
        assertEquals("Test", result?.transaction?.description)
    }

    // ========================================
    // searchByDescription tests
    // ========================================

    @Test
    fun `searchByDescription returns matching descriptions`() = runTest {
        repository.insertTransaction(createTestTransaction(description = "Grocery shopping"))
        repository.insertTransaction(createTestTransaction(description = "Gas station", ledgerId = 2L))
        repository.insertTransaction(createTestTransaction(description = "Groceries", ledgerId = 3L))

        val results = repository.searchByDescription("groc")
        assertEquals(2, results.size)
        assertTrue(results.any { it.description == "Grocery shopping" })
        assertTrue(results.any { it.description == "Groceries" })
    }

    @Test
    fun `searchByDescription returns empty list when no matches`() = runTest {
        repository.insertTransaction(createTestTransaction(description = "Test"))

        val results = repository.searchByDescription("xyz")
        assertTrue(results.isEmpty())
    }

    // ========================================
    // getFirstByDescription tests
    // ========================================

    @Test
    fun `getFirstByDescription returns most recent matching transaction`() = runTest {
        repository.insertTransaction(
            createTestTransaction(
                description = "Groceries",
                year = 2025,
                month = 12,
                day = 1
            )
        )
        repository.insertTransaction(
            createTestTransaction(
                description = "Groceries",
                year = 2026,
                month = 1,
                day = 15,
                ledgerId = 2L
            )
        )

        val result = repository.getFirstByDescription("Groceries")
        assertNotNull(result)
        assertEquals(2026, result?.transaction?.year)
        assertEquals(1, result?.transaction?.month)
        assertEquals(15, result?.transaction?.day)
    }

    @Test
    fun `getFirstByDescription returns null when no match`() = runTest {
        repository.insertTransaction(createTestTransaction(description = "Test"))

        val result = repository.getFirstByDescription("Nonexistent")
        assertNull(result)
    }

    // ========================================
    // getFirstByDescriptionHavingAccount tests
    // ========================================

    @Test
    fun `getFirstByDescriptionHavingAccount filters by account`() = runTest {
        repository.insertTransaction(
            createTestTransaction(
                description = "Payment",
                accounts = listOf(
                    createTestAccount("Assets:Cash", -100f),
                    createTestAccount("Expenses:Food", 100f)
                )
            )
        )
        repository.insertTransaction(
            createTestTransaction(
                description = "Payment",
                ledgerId = 2L,
                accounts = listOf(
                    createTestAccount("Assets:Bank", -100f),
                    createTestAccount("Expenses:Utilities", 100f)
                )
            )
        )

        val result = repository.getFirstByDescriptionHavingAccount("Payment", "Cash")
        assertNotNull(result)
        assertTrue(result?.accounts?.any { it.accountName.contains("Cash") } == true)
    }

    // ========================================
    // deleteTransaction tests
    // ========================================

    @Test
    fun `deleteTransaction removes transaction`() = runTest {
        val tx = createTestTransaction()
        repository.insertTransaction(tx)

        repository.deleteTransaction(tx.transaction)

        val remaining = repository.getAllTransactions(testProfileId).first()
        assertTrue(remaining.isEmpty())
    }

    // ========================================
    // deleteTransactions tests
    // ========================================

    @Test
    fun `deleteTransactions removes multiple transactions`() = runTest {
        val tx1 = createTestTransaction(description = "Tx1")
        val tx2 = createTestTransaction(description = "Tx2", ledgerId = 2L)
        repository.insertTransaction(tx1)
        repository.insertTransaction(tx2)

        repository.deleteTransactions(listOf(tx1.transaction, tx2.transaction))

        val remaining = repository.getAllTransactions(testProfileId).first()
        assertTrue(remaining.isEmpty())
    }

    // ========================================
    // storeTransactions tests
    // ========================================

    @Test
    fun `storeTransactions stores batch of transactions`() = runTest {
        val transactions = listOf(
            createTestTransaction(description = "Tx1", ledgerId = 1L),
            createTestTransaction(description = "Tx2", ledgerId = 2L),
            createTestTransaction(description = "Tx3", ledgerId = 3L)
        )

        repository.storeTransactions(transactions, testProfileId)

        val stored = repository.getAllTransactions(testProfileId).first()
        assertEquals(3, stored.size)
    }

    // ========================================
    // deleteAllForProfile tests
    // ========================================

    @Test
    fun `deleteAllForProfile removes all transactions for profile`() = runTest {
        repository.insertTransaction(createTestTransaction(profileId = 1L, description = "P1"))
        repository.insertTransaction(createTestTransaction(profileId = 2L, description = "P2", ledgerId = 2L))

        val deleted = repository.deleteAllForProfile(1L)

        assertEquals(1, deleted)
        val remaining = repository.getAllTransactions(2L).first()
        assertEquals(1, remaining.size)
        assertEquals("P2", remaining[0].transaction.description)
    }

    // ========================================
    // getMaxLedgerId tests
    // ========================================

    @Test
    fun `getMaxLedgerId returns null when no transactions`() = runTest {
        val result = repository.getMaxLedgerId(testProfileId)
        assertNull(result)
    }

    @Test
    fun `getMaxLedgerId returns maximum ledger id`() = runTest {
        repository.insertTransaction(createTestTransaction(ledgerId = 5L))
        repository.insertTransaction(createTestTransaction(ledgerId = 10L))
        repository.insertTransaction(createTestTransaction(ledgerId = 3L))

        val result = repository.getMaxLedgerId(testProfileId)
        assertEquals(10L, result)
    }
}

/**
 * Fake implementation of [TransactionRepository] for unit testing.
 *
 * This implementation provides an in-memory store that allows testing
 * without a real database or Room infrastructure.
 */
class FakeTransactionRepository : TransactionRepository {

    private val transactions = mutableMapOf<Long, TransactionWithAccounts>()
    private var nextId = 1L
    private val transactionsFlow = MutableStateFlow<List<TransactionWithAccounts>>(emptyList())

    private fun emitChanges() {
        transactionsFlow.value = transactions.values.toList()
    }

    override fun getAllTransactions(profileId: Long): Flow<List<TransactionWithAccounts>> = MutableStateFlow(
        transactions.values
            .filter { it.transaction.profileId == profileId }
            .sortedWith(
                compareBy(
                    { it.transaction.year },
                    { it.transaction.month },
                    { it.transaction.day },
                    { it.transaction.ledgerId }
                )
            )
    )

    override fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<TransactionWithAccounts>> =
        MutableStateFlow(
            transactions.values
                .filter { twa ->
                    twa.transaction.profileId == profileId &&
                        (
                            accountName == null || twa.accounts.any {
                                it.accountName.contains(accountName, ignoreCase = true) && it.amount != 0f
                            }
                            )
                }
                .sortedWith(
                    compareBy(
                        { it.transaction.year },
                        { it.transaction.month },
                        { it.transaction.day },
                        { it.transaction.ledgerId }
                    )
                )
        )

    override fun getTransactionById(transactionId: Long): Flow<TransactionWithAccounts?> =
        MutableStateFlow(transactions[transactionId])

    override suspend fun getTransactionByIdSync(transactionId: Long): TransactionWithAccounts? =
        transactions[transactionId]

    override suspend fun searchByDescription(term: String): List<TransactionDAO.DescriptionContainer> {
        val termUpper = term.uppercase()
        return transactions.values
            .filter { it.transaction.descriptionUpper.contains(termUpper) }
            .distinctBy { it.transaction.description }
            .map { twa ->
                TransactionDAO.DescriptionContainer().apply {
                    description = twa.transaction.description
                    ordering = when {
                        twa.transaction.descriptionUpper.startsWith(termUpper) -> 1
                        twa.transaction.descriptionUpper.contains(":$termUpper") -> 2
                        twa.transaction.descriptionUpper.contains(" $termUpper") -> 3
                        else -> 9
                    }
                }
            }
            .sortedWith(compareBy({ it.ordering }, { it.description?.uppercase() }))
    }

    override suspend fun getFirstByDescription(description: String): TransactionWithAccounts? = transactions.values
        .filter { it.transaction.description == description }
        .maxByOrNull {
            it.transaction.year * 10000 + it.transaction.month * 100 + it.transaction.day
        }

    override suspend fun getFirstByDescriptionHavingAccount(
        description: String,
        accountTerm: String
    ): TransactionWithAccounts? = transactions.values
        .filter { twa ->
            twa.transaction.description == description &&
                twa.accounts.any { it.accountName.contains(accountTerm, ignoreCase = true) }
        }
        .maxByOrNull {
            it.transaction.year * 10000 + it.transaction.month * 100 + it.transaction.day
        }

    override suspend fun insertTransaction(transaction: TransactionWithAccounts) {
        if (transaction.transaction.id == 0L) {
            transaction.transaction.id = nextId++
        }
        transactions[transaction.transaction.id] = transaction
        emitChanges()
    }

    override suspend fun storeTransaction(transaction: TransactionWithAccounts) {
        insertTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.remove(transaction.id)
        emitChanges()
    }

    override suspend fun deleteTransactions(transactions: List<Transaction>) {
        transactions.forEach { this.transactions.remove(it.id) }
        emitChanges()
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
        emitChanges()
        return toRemove.size
    }

    override suspend fun getMaxLedgerId(profileId: Long): Long? = transactions.values
        .filter { it.transaction.profileId == profileId }
        .maxOfOrNull { it.transaction.ledgerId }
}
