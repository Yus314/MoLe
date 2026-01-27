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
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.database.entity.Transaction as DbTransaction
import net.ktnx.mobileledger.core.database.entity.TransactionAccount
import net.ktnx.mobileledger.core.database.entity.TransactionWithAccounts
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine
import net.ktnx.mobileledger.core.domain.repository.TransactionRepository
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
        val dbTransaction = DbTransaction().apply {
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
        twa.transaction = dbTransaction
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

    private fun createDomainTestTransaction(
        id: Long? = null,
        description: String = "Test Transaction",
        year: Int = 2026,
        month: Int = 1,
        day: Int = 10,
        ledgerId: Long = 1L,
        lines: List<TransactionLine> = listOf(
            TransactionLine(null, "Assets:Cash", -100f, "", null),
            TransactionLine(null, "Expenses:Food", 100f, "", null)
        )
    ): Transaction = Transaction(
        id = id,
        ledgerId = ledgerId,
        date = SimpleDate(year, month, day),
        description = description,
        comment = null,
        lines = lines
    )

    // ========================================
    // observeAllTransactions tests (Flow)
    // ========================================

    @Test
    fun `observeAllTransactions returns empty list when no transactions`() = runTest {
        val transactions = repository.observeAllTransactions(testProfileId).first()
        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `observeAllTransactions returns transactions for profile`() = runTest {
        repository.insertTransaction(createDomainTestTransaction(description = "Groceries"), testProfileId)

        val transactions = repository.observeAllTransactions(testProfileId).first()
        assertEquals(1, transactions.size)
        assertEquals("Groceries", transactions[0].description)
    }

    @Test
    fun `observeAllTransactions filters by profile`() = runTest {
        repository.insertTransaction(createDomainTestTransaction(description = "Profile 1"), 1L)
        repository.insertTransaction(createDomainTestTransaction(description = "Profile 2", ledgerId = 2L), 2L)

        val transactions = repository.observeAllTransactions(1L).first()
        assertEquals(1, transactions.size)
        assertEquals("Profile 1", transactions[0].description)
    }

    // ========================================
    // observeTransactionsFiltered tests (Flow)
    // ========================================

    @Test
    fun `observeTransactionsFiltered with null accountName returns all transactions`() = runTest {
        repository.insertTransaction(createDomainTestTransaction(), testProfileId)

        val transactions = repository.observeTransactionsFiltered(testProfileId, null).first()
        assertEquals(1, transactions.size)
    }

    @Test
    fun `observeTransactionsFiltered with accountName filters correctly`() = runTest {
        repository.insertTransaction(
            createDomainTestTransaction(
                description = "Cash payment",
                lines = listOf(
                    TransactionLine(null, "Assets:Cash", -100f, "", null),
                    TransactionLine(null, "Expenses:Food", 100f, "", null)
                )
            ),
            testProfileId
        )
        repository.insertTransaction(
            createDomainTestTransaction(
                description = "Bank transfer",
                ledgerId = 2L,
                lines = listOf(
                    TransactionLine(null, "Assets:Bank", -200f, "", null),
                    TransactionLine(null, "Expenses:Utilities", 200f, "", null)
                )
            ),
            testProfileId
        )

        val transactions = repository.observeTransactionsFiltered(testProfileId, "Cash").first()
        assertEquals(1, transactions.size)
        assertEquals("Cash payment", transactions[0].description)
    }

    // ========================================
    // observeTransactionById tests (Flow)
    // ========================================

    @Test
    fun `observeTransactionById returns null for non-existent id`() = runTest {
        val result = repository.observeTransactionById(999L).first()
        assertNull(result)
    }

    // ========================================
    // getTransactionById tests (suspend)
    // ========================================

    @Test
    fun `getTransactionById returns transaction when exists`() = runTest {
        val insertedTx = repository.insertTransaction(
            createDomainTestTransaction(description = "Test"),
            testProfileId
        ).getOrThrow()

        val result = repository.getTransactionById(insertedTx.id!!).getOrNull()
        assertNotNull(result)
        assertEquals("Test", result?.description)
    }

    // ========================================
    // searchByDescription tests
    // ========================================

    @Test
    fun `searchByDescription returns matching descriptions`() = runTest {
        repository.insertTransaction(
            createDomainTestTransaction(description = "Grocery shopping"),
            testProfileId
        ).getOrThrow()
        repository.insertTransaction(
            createDomainTestTransaction(description = "Gas station", ledgerId = 2L),
            testProfileId
        ).getOrThrow()
        repository.insertTransaction(
            createDomainTestTransaction(description = "Groceries", ledgerId = 3L),
            testProfileId
        ).getOrThrow()

        val results = repository.searchByDescription("groc").getOrThrow()
        assertEquals(2, results.size)
        assertTrue(results.any { it == "Grocery shopping" })
        assertTrue(results.any { it == "Groceries" })
    }

    @Test
    fun `searchByDescription returns empty list when no matches`() = runTest {
        repository.insertTransaction(createDomainTestTransaction(description = "Test"), testProfileId).getOrThrow()

        val results = repository.searchByDescription("xyz").getOrThrow()
        assertTrue(results.isEmpty())
    }

    // ========================================
    // getFirstByDescription tests
    // ========================================

    @Test
    fun `getFirstByDescription returns most recent matching transaction`() = runTest {
        repository.insertTransaction(
            createDomainTestTransaction(
                description = "Groceries",
                year = 2025,
                month = 12,
                day = 1
            ),
            testProfileId
        ).getOrThrow()
        repository.insertTransaction(
            createDomainTestTransaction(
                description = "Groceries",
                year = 2026,
                month = 1,
                day = 15,
                ledgerId = 2L
            ),
            testProfileId
        ).getOrThrow()

        val result = repository.getFirstByDescription("Groceries").getOrNull()
        assertNotNull(result)
        assertEquals(2026, result?.date?.year)
        assertEquals(1, result?.date?.month)
        assertEquals(15, result?.date?.day)
    }

    @Test
    fun `getFirstByDescription returns null when no match`() = runTest {
        repository.insertTransaction(createDomainTestTransaction(description = "Test"), testProfileId).getOrThrow()

        val result = repository.getFirstByDescription("Nonexistent").getOrNull()
        assertNull(result)
    }

    // ========================================
    // getFirstByDescriptionHavingAccount tests
    // ========================================

    @Test
    fun `getFirstByDescriptionHavingAccount filters by account`() = runTest {
        repository.insertTransaction(
            createDomainTestTransaction(
                description = "Payment",
                lines = listOf(
                    TransactionLine(null, "Assets:Cash", -100f, "", null),
                    TransactionLine(null, "Expenses:Food", 100f, "", null)
                )
            ),
            testProfileId
        ).getOrThrow()
        repository.insertTransaction(
            createDomainTestTransaction(
                description = "Payment",
                ledgerId = 2L,
                lines = listOf(
                    TransactionLine(null, "Assets:Bank", -100f, "", null),
                    TransactionLine(null, "Expenses:Utilities", 100f, "", null)
                )
            ),
            testProfileId
        ).getOrThrow()

        val result = repository.getFirstByDescriptionHavingAccount("Payment", "Cash").getOrNull()
        assertNotNull(result)
        assertTrue(result?.lines?.any { it.accountName.contains("Cash") } == true)
    }

    // ========================================
    // deleteTransactionById tests
    // ========================================

    @Test
    fun `deleteTransactionById removes transaction`() = runTest {
        val tx = repository.insertTransaction(createDomainTestTransaction(), testProfileId).getOrThrow()

        val deleted = repository.deleteTransactionById(tx.id!!).getOrThrow()

        assertEquals(1, deleted)
        val remaining = repository.observeAllTransactions(testProfileId).first()
        assertTrue(remaining.isEmpty())
    }

    // ========================================
    // deleteTransactionsByIds tests
    // ========================================

    @Test
    fun `deleteTransactionsByIds removes multiple transactions`() = runTest {
        val tx1 = repository.insertTransaction(
            createDomainTestTransaction(description = "Tx1"),
            testProfileId
        ).getOrThrow()
        val tx2 = repository.insertTransaction(
            createDomainTestTransaction(description = "Tx2", ledgerId = 2L),
            testProfileId
        ).getOrThrow()

        val deleted = repository.deleteTransactionsByIds(listOf(tx1.id!!, tx2.id!!)).getOrThrow()

        assertEquals(2, deleted)
        val remaining = repository.observeAllTransactions(testProfileId).first()
        assertTrue(remaining.isEmpty())
    }

    // ========================================
    // storeTransactionsAsDomain tests
    // ========================================

    @Test
    fun `storeTransactionsAsDomain stores batch of transactions`() = runTest {
        val transactions = listOf(
            createDomainTestTransaction(description = "Tx1", ledgerId = 1L),
            createDomainTestTransaction(description = "Tx2", ledgerId = 2L),
            createDomainTestTransaction(description = "Tx3", ledgerId = 3L)
        )

        repository.storeTransactionsAsDomain(transactions, testProfileId).getOrThrow()

        val stored = repository.observeAllTransactions(testProfileId).first()
        assertEquals(3, stored.size)
    }

    // ========================================
    // deleteAllForProfile tests
    // ========================================

    @Test
    fun `deleteAllForProfile removes all transactions for profile`() = runTest {
        repository.insertTransaction(createDomainTestTransaction(description = "P1"), 1L).getOrThrow()
        repository.insertTransaction(createDomainTestTransaction(description = "P2", ledgerId = 2L), 2L).getOrThrow()

        val deleted = repository.deleteAllForProfile(1L).getOrThrow()

        assertEquals(1, deleted)
        val remaining = repository.observeAllTransactions(2L).first()
        assertEquals(1, remaining.size)
        assertEquals("P2", remaining[0].description)
    }

    // ========================================
    // getMaxLedgerId tests
    // ========================================

    @Test
    fun `getMaxLedgerId returns null when no transactions`() = runTest {
        val result = repository.getMaxLedgerId(testProfileId).getOrNull()
        assertNull(result)
    }

    @Test
    fun `getMaxLedgerId returns maximum ledger id`() = runTest {
        repository.insertTransaction(createDomainTestTransaction(ledgerId = 5L), testProfileId).getOrThrow()
        repository.insertTransaction(createDomainTestTransaction(ledgerId = 10L), testProfileId).getOrThrow()
        repository.insertTransaction(createDomainTestTransaction(ledgerId = 3L), testProfileId).getOrThrow()

        val result = repository.getMaxLedgerId(testProfileId).getOrNull()
        assertEquals(10L, result)
    }
}

/**
 * Fake implementation of [TransactionRepository] for unit testing.
 *
 * This implementation provides an in-memory store that allows testing
 * without a real database or Room infrastructure.
 * Stores domain model Transactions with associated profileId for proper isolation.
 */
class FakeTransactionRepository : TransactionRepository {

    private data class StoredTransaction(val transaction: Transaction, val profileId: Long)

    private val storedTransactions = mutableMapOf<Long, StoredTransaction>()
    private var nextId = 1L

    // Flow methods (observe prefix)
    override fun observeAllTransactions(profileId: Long): Flow<List<Transaction>> = MutableStateFlow(
        storedTransactions.values
            .filter { it.profileId == profileId }
            .map { it.transaction }
            .sortedWith(
                compareBy(
                    { it.date.year },
                    { it.date.month },
                    { it.date.day },
                    { it.ledgerId }
                )
            )
    )

    override fun observeTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<Transaction>> =
        MutableStateFlow(
            storedTransactions.values
                .filter { stored ->
                    stored.profileId == profileId &&
                        (
                            accountName == null || stored.transaction.lines.any {
                                it.accountName.contains(accountName, ignoreCase = true) && it.amount != 0f
                            }
                            )
                }
                .map { it.transaction }
                .sortedWith(
                    compareBy(
                        { it.date.year },
                        { it.date.month },
                        { it.date.day },
                        { it.ledgerId }
                    )
                )
        )

    override fun observeTransactionById(transactionId: Long): Flow<Transaction?> =
        MutableStateFlow(storedTransactions[transactionId]?.transaction)

    // Suspend methods (no suffix)
    override suspend fun getTransactionById(transactionId: Long): Result<Transaction?> =
        Result.success(storedTransactions[transactionId]?.transaction)

    override suspend fun searchByDescription(term: String): Result<List<String>> {
        val termUpper = term.uppercase()
        return Result.success(
            storedTransactions.values
                .filter { it.transaction.description.uppercase().contains(termUpper) }
                .distinctBy { it.transaction.description }
                .map { stored ->
                    val descUpper = stored.transaction.description.uppercase()
                    val ordering = when {
                        descUpper.startsWith(termUpper) -> 1
                        descUpper.contains(":$termUpper") -> 2
                        descUpper.contains(" $termUpper") -> 3
                        else -> 9
                    }
                    Pair(stored.transaction.description, ordering)
                }
                .sortedWith(compareBy({ it.second }, { it.first.uppercase() }))
                .map { it.first }
        )
    }

    override suspend fun getFirstByDescription(description: String): Result<Transaction?> = Result.success(
        storedTransactions.values
            .filter { it.transaction.description == description }
            .maxByOrNull {
                it.transaction.date.year * 10000 + it.transaction.date.month * 100 + it.transaction.date.day
            }?.transaction
    )

    override suspend fun getFirstByDescriptionHavingAccount(
        description: String,
        accountTerm: String
    ): Result<Transaction?> = Result.success(
        storedTransactions.values
            .filter { stored ->
                stored.transaction.description == description &&
                    stored.transaction.lines.any { it.accountName.contains(accountTerm, ignoreCase = true) }
            }
            .maxByOrNull {
                it.transaction.date.year * 10000 + it.transaction.date.month * 100 + it.transaction.date.day
            }?.transaction
    )

    // Domain model mutation methods
    override suspend fun insertTransaction(transaction: Transaction, profileId: Long): Result<Transaction> {
        val id = transaction.id ?: nextId++
        val txWithId = transaction.copy(id = id)
        storedTransactions[id] = StoredTransaction(txWithId, profileId)
        return Result.success(txWithId)
    }

    override suspend fun storeTransaction(transaction: Transaction, profileId: Long): Result<Unit> {
        insertTransaction(transaction, profileId)
        return Result.success(Unit)
    }

    override suspend fun deleteTransactionById(transactionId: Long): Result<Int> {
        val existed = storedTransactions.containsKey(transactionId)
        storedTransactions.remove(transactionId)
        return Result.success(if (existed) 1 else 0)
    }

    override suspend fun deleteTransactionsByIds(transactionIds: List<Long>): Result<Int> {
        var count = 0
        transactionIds.forEach { id ->
            if (storedTransactions.containsKey(id)) {
                count++
            }
            storedTransactions.remove(id)
        }
        return Result.success(count)
    }

    override suspend fun storeTransactionsAsDomain(transactions: List<Transaction>, profileId: Long): Result<Unit> {
        transactions.forEach { tx ->
            insertTransaction(tx, profileId)
        }
        return Result.success(Unit)
    }

    override suspend fun deleteAllForProfile(profileId: Long): Result<Int> {
        val toRemove = storedTransactions.values.filter { it.profileId == profileId }
        toRemove.forEach { storedTransactions.remove(it.transaction.id) }
        return Result.success(toRemove.size)
    }

    override suspend fun getMaxLedgerId(profileId: Long): Result<Long?> = Result.success(
        storedTransactions.values
            .filter { it.profileId == profileId }
            .maxOfOrNull { it.transaction.ledgerId }
    )
}
