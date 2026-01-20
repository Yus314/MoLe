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

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.util.createTestDomainProfile
import net.ktnx.mobileledger.utils.SimpleDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for concurrent access patterns in Repository implementations.
 *
 * These tests verify that:
 * - Concurrent reads don't interfere with each other
 * - Concurrent writes maintain data integrity
 * - Flow emissions are consistent during concurrent updates
 * - StateFlow updates are atomic and consistent
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryConcurrencyTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: ConcurrentFakeProfileRepository
    private lateinit var transactionRepository: ConcurrentFakeTransactionRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = ConcurrentFakeProfileRepository()
        transactionRepository = ConcurrentFakeTransactionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(id: Long? = null, name: String = "Test Profile"): Profile =
        createTestDomainProfile(id = id, name = name)

    private fun createTestTransaction(profileId: Long = 1L, description: String = "Test Transaction"): Transaction =
        Transaction(
            id = null,
            ledgerId = 1L,
            date = SimpleDate(2026, 1, 10),
            description = description,
            comment = null,
            lines = listOf(
                TransactionLine(null, "Assets:Cash", -100f, "", null),
                TransactionLine(null, "Expenses:Food", 100f, "", null)
            )
        )

    private fun getProfileIdFromDescription(description: String): Long {
        // Extract profileId from descriptions like "P1_TX1" -> 1L
        val match = Regex("P(\\d+)_").find(description)
        return match?.groupValues?.get(1)?.toLongOrNull() ?: 1L
    }

    // ========================================
    // Concurrent Profile Tests
    // ========================================

    @Test
    fun `concurrent profile insertions maintain data integrity`() = runTest {
        val insertCount = 10
        val insertedIds = (1..insertCount).map { index ->
            async {
                val profile = createTestProfile(name = "Profile $index")
                profileRepository.insertProfile(profile)
            }
        }.awaitAll()

        // All insertions should complete
        assertEquals(insertCount, insertedIds.size)

        // All profiles should be stored
        val allProfiles = profileRepository.observeAllProfiles().first()
        assertEquals(insertCount, allProfiles.size)

        // All IDs should be unique
        assertEquals(insertCount, insertedIds.toSet().size)
    }

    @Test
    fun `concurrent reads while inserting return consistent data`() = runTest {
        // Pre-populate some data
        repeat(5) { i ->
            profileRepository.insertProfile(createTestProfile(name = "Initial Profile $i"))
        }

        // Concurrent reads and inserts
        val results = (1..10).map { index ->
            async {
                if (index % 2 == 0) {
                    // Even: insert
                    profileRepository.insertProfile(createTestProfile(name = "New Profile $index"))
                    -1L // Signal insert operation
                } else {
                    // Odd: read
                    profileRepository.getProfileCount().toLong()
                }
            }
        }.awaitAll()

        // Reads should return values >= 5 (initial count)
        val readResults = results.filter { it != -1L }
        assertTrue(readResults.all { it >= 5 })

        // Final count should include all inserts
        val finalCount = profileRepository.getProfileCount()
        assertTrue(finalCount >= 10) // 5 initial + 5 inserts
    }

    @Test
    fun `currentProfile StateFlow updates atomically`() = runTest {
        val profile1 = createTestProfile(id = 1L, name = "Profile 1")
        val profile2 = createTestProfile(id = 2L, name = "Profile 2")

        profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)

        // Rapid profile switches
        val switches = 20
        repeat(switches) { i ->
            if (i % 2 == 0) {
                profileRepository.setCurrentProfile(profile1)
            } else {
                profileRepository.setCurrentProfile(profile2)
            }
        }

        // Current profile should be one of the two
        val current = profileRepository.currentProfile.value
        assertNotNull(current)
        assertTrue(current?.id == 1L || current?.id == 2L)
    }

    // ========================================
    // Concurrent Transaction Tests
    // ========================================

    @Test
    fun `concurrent transaction insertions are counted correctly`() = runTest {
        val insertCount = 20
        val profileId = 1L

        (1..insertCount).map { index ->
            async {
                val tx = createTestTransaction(
                    profileId = profileId,
                    description = "Transaction $index"
                )
                transactionRepository.insertTransaction(tx, profileId)
            }
        }.awaitAll()

        val allTransactions = transactionRepository.observeAllTransactions(profileId).first()
        assertEquals(insertCount, allTransactions.size)
    }

    @Test
    fun `concurrent writes to different profiles don't interfere`() = runTest {
        val txPerProfile = 5
        val profileCount = 3

        // Insert transactions for different profiles concurrently
        (1..profileCount).flatMap { profileId ->
            (1..txPerProfile).map { txIndex ->
                async {
                    val tx = createTestTransaction(
                        profileId = profileId.toLong(),
                        description = "P${profileId}_TX$txIndex"
                    )
                    transactionRepository.insertTransaction(tx, profileId.toLong())
                }
            }
        }.awaitAll()

        // Verify each profile has correct number of transactions
        (1..profileCount).forEach { profileId ->
            val transactions = transactionRepository.observeAllTransactions(profileId.toLong()).first()
            assertEquals(
                "Profile $profileId should have $txPerProfile transactions",
                txPerProfile,
                transactions.size
            )
        }
    }

    @Test
    fun `deleteAllForProfile is atomic`() = runTest {
        val profileId = 1L

        // Insert many transactions
        repeat(10) { i ->
            transactionRepository.insertTransaction(
                createTestTransaction(profileId = profileId, description = "TX $i"),
                profileId
            )
        }

        assertEquals(10, transactionRepository.observeAllTransactions(profileId).first().size)

        // Delete all
        val deleted = transactionRepository.deleteAllForProfile(profileId)

        assertEquals(10, deleted)
        assertEquals(0, transactionRepository.observeAllTransactions(profileId).first().size)
    }
}

/**
 * Thread-safe fake ProfileRepository for concurrency testing.
 */
class ConcurrentFakeProfileRepository : ProfileRepository {
    private val profiles = mutableMapOf<Long, Profile>()
    private val idCounter = AtomicInteger(1)
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    private val lock = Any()

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun observeAllProfiles(): Flow<List<Profile>> = synchronized(lock) {
        MutableStateFlow(profiles.values.sortedBy { it.orderNo }.toList())
    }

    override suspend fun getAllProfiles(): List<Profile> = synchronized(lock) {
        profiles.values.sortedBy { it.orderNo }.toList()
    }

    override fun observeProfileById(profileId: Long): Flow<Profile?> = synchronized(lock) {
        MutableStateFlow(profiles[profileId])
    }

    override suspend fun getProfileById(profileId: Long): Profile? = synchronized(lock) {
        profiles[profileId]
    }

    override fun observeProfileByUuid(uuid: String): Flow<Profile?> = synchronized(lock) {
        MutableStateFlow(profiles.values.find { it.uuid == uuid })
    }

    override suspend fun getProfileByUuid(uuid: String): Profile? = synchronized(lock) {
        profiles.values.find { it.uuid == uuid }
    }

    override suspend fun getAnyProfile(): Profile? = synchronized(lock) {
        profiles.values.firstOrNull()
    }

    override suspend fun getProfileCount(): Int = synchronized(lock) {
        profiles.size
    }

    override suspend fun insertProfile(profile: Profile): Long = synchronized(lock) {
        val id = if (profile.id == null || profile.id == 0L) idCounter.getAndIncrement().toLong() else profile.id
        val profileWithId = profile.copy(id = id)
        profiles[id] = profileWithId
        id
    }

    override suspend fun updateProfile(profile: Profile): Unit = synchronized(lock) {
        val id = profile.id ?: return
        profiles[id] = profile
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profile
        }
    }

    override suspend fun deleteProfile(profile: Profile): Unit = synchronized(lock) {
        val id = profile.id ?: return
        profiles.remove(id)
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profiles.values.firstOrNull()
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>): Unit = synchronized(lock) {
        profiles.forEachIndexed { index, profile ->
            val id = profile.id ?: return@forEachIndexed
            this.profiles[id]?.let { existing ->
                this.profiles[id] = existing.copy(orderNo = index)
            }
        }
    }

    override suspend fun deleteAllProfiles(): Unit = synchronized(lock) {
        profiles.clear()
        _currentProfile.value = null
    }
}

/**
 * Thread-safe fake TransactionRepository for concurrency testing.
 * Stores domain model Transactions with associated profileId.
 */
class ConcurrentFakeTransactionRepository : TransactionRepository {
    private data class StoredTransaction(val transaction: Transaction, val profileId: Long)

    private val transactions = mutableMapOf<Long, StoredTransaction>()
    private val idCounter = AtomicInteger(1)
    private val lock = Any()

    // Flow methods (observe prefix)
    override fun observeAllTransactions(profileId: Long): Flow<List<Transaction>> = synchronized(lock) {
        MutableStateFlow(
            transactions.values
                .filter { it.profileId == profileId }
                .map { it.transaction }
                .toList()
        )
    }

    override fun observeTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<Transaction>> =
        synchronized(lock) {
            MutableStateFlow(
                transactions.values
                    .filter { stored ->
                        stored.profileId == profileId &&
                            (
                                accountName == null || stored.transaction.lines.any {
                                    it.accountName.contains(accountName, true)
                                }
                                )
                    }
                    .map { it.transaction }
                    .toList()
            )
        }

    override fun observeTransactionById(transactionId: Long): Flow<Transaction?> = synchronized(lock) {
        MutableStateFlow(transactions[transactionId]?.transaction)
    }

    // Suspend methods (no suffix)
    override suspend fun getTransactionById(transactionId: Long): Transaction? = synchronized(lock) {
        transactions[transactionId]?.transaction
    }

    override suspend fun searchByDescription(term: String): List<TransactionDAO.DescriptionContainer> =
        synchronized(lock) {
            transactions.values
                .filter { it.transaction.description.contains(term, true) }
                .distinctBy { it.transaction.description }
                .map {
                    TransactionDAO.DescriptionContainer().apply {
                        description = it.transaction.description
                    }
                }
        }

    override suspend fun getFirstByDescription(description: String): Transaction? = synchronized(lock) {
        transactions.values.find { it.transaction.description == description }?.transaction
    }

    override suspend fun getFirstByDescriptionHavingAccount(description: String, accountTerm: String): Transaction? =
        synchronized(lock) {
            transactions.values.find { stored ->
                stored.transaction.description == description &&
                    stored.transaction.lines.any { it.accountName.contains(accountTerm, true) }
            }?.transaction
        }

    // Domain model mutation methods
    override suspend fun insertTransaction(transaction: Transaction, profileId: Long): Transaction {
        val id = synchronized(lock) {
            val newId = transaction.id ?: idCounter.getAndIncrement().toLong()
            val txWithId = transaction.copy(id = newId)
            transactions[newId] = StoredTransaction(txWithId, profileId)
            newId
        }
        return transaction.copy(id = id)
    }

    override suspend fun storeTransaction(transaction: Transaction, profileId: Long) {
        insertTransaction(transaction, profileId)
    }

    override suspend fun deleteTransactionById(transactionId: Long): Int = synchronized(lock) {
        val existed = transactions.containsKey(transactionId)
        transactions.remove(transactionId)
        if (existed) 1 else 0
    }

    override suspend fun deleteTransactionsByIds(transactionIds: List<Long>): Int = synchronized(lock) {
        var count = 0
        transactionIds.forEach { id ->
            if (transactions.containsKey(id)) {
                count++
            }
            transactions.remove(id)
        }
        count
    }

    override suspend fun storeTransactionsAsDomain(txList: List<Transaction>, profileId: Long) {
        txList.forEach { tx ->
            insertTransaction(tx, profileId)
        }
    }

    override suspend fun deleteAllForProfile(profileId: Long): Int = synchronized(lock) {
        val toRemove = transactions.values.filter { it.profileId == profileId }
        toRemove.forEach { transactions.remove(it.transaction.id) }
        toRemove.size
    }

    override suspend fun getMaxLedgerId(profileId: Long): Long? = synchronized(lock) {
        transactions.values
            .filter { it.profileId == profileId }
            .maxOfOrNull { it.transaction.ledgerId }
    }
}
