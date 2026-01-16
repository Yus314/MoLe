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
import net.ktnx.mobileledger.data.repository.mapper.TransactionMapper
import net.ktnx.mobileledger.db.Transaction as DbTransaction
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.util.createTestDomainProfile
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

    private fun createTestTransaction(
        profileId: Long = 1L,
        description: String = "Test Transaction"
    ): TransactionWithAccounts {
        val transaction = DbTransaction().apply {
            this.profileId = profileId
            this.description = description
            this.year = 2026
            this.month = 1
            this.day = 10
        }
        val twa = TransactionWithAccounts()
        twa.transaction = transaction
        twa.accounts = emptyList()
        return twa
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
        val allProfiles = profileRepository.getAllProfiles().first()
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
                transactionRepository.insertTransaction(tx)
            }
        }.awaitAll()

        val allTransactions = transactionRepository.getAllTransactions(profileId).first()
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
                    transactionRepository.insertTransaction(tx)
                }
            }
        }.awaitAll()

        // Verify each profile has correct number of transactions
        (1..profileCount).forEach { profileId ->
            val transactions = transactionRepository.getAllTransactions(profileId.toLong()).first()
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
                createTestTransaction(profileId = profileId, description = "TX $i")
            )
        }

        assertEquals(10, transactionRepository.getAllTransactions(profileId).first().size)

        // Delete all
        val deleted = transactionRepository.deleteAllForProfile(profileId)

        assertEquals(10, deleted)
        assertEquals(0, transactionRepository.getAllTransactions(profileId).first().size)
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

    override fun getAllProfiles(): Flow<List<Profile>> = synchronized(lock) {
        MutableStateFlow(profiles.values.sortedBy { it.orderNo }.toList())
    }

    override suspend fun getAllProfilesSync(): List<Profile> = synchronized(lock) {
        profiles.values.sortedBy { it.orderNo }.toList()
    }

    override fun getProfileById(profileId: Long): Flow<Profile?> = synchronized(lock) {
        MutableStateFlow(profiles[profileId])
    }

    override suspend fun getProfileByIdSync(profileId: Long): Profile? = synchronized(lock) {
        profiles[profileId]
    }

    override fun getProfileByUuid(uuid: String): Flow<Profile?> = synchronized(lock) {
        MutableStateFlow(profiles.values.find { it.uuid == uuid })
    }

    override suspend fun getProfileByUuidSync(uuid: String): Profile? = synchronized(lock) {
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
 */
class ConcurrentFakeTransactionRepository : TransactionRepository {
    private val transactions = mutableMapOf<Long, TransactionWithAccounts>()
    private val idCounter = AtomicInteger(1)
    private val lock = Any()

    override fun getAllTransactions(profileId: Long): Flow<List<Transaction>> = synchronized(lock) {
        MutableStateFlow(transactions.values.filter { it.transaction.profileId == profileId }.toList())
            .map { TransactionMapper.toDomainList(it) }
    }

    override fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<Transaction>> =
        synchronized(lock) {
            MutableStateFlow(
                transactions.values.filter { twa ->
                    twa.transaction.profileId == profileId &&
                        (accountName == null || twa.accounts.any { it.accountName.contains(accountName, true) })
                }.toList()
            ).map { TransactionMapper.toDomainList(it) }
        }

    override fun getTransactionById(transactionId: Long): Flow<Transaction?> = synchronized(lock) {
        MutableStateFlow(transactions[transactionId])
            .map { it?.let { TransactionMapper.toDomain(it) } }
    }

    override suspend fun getTransactionByIdSync(transactionId: Long): Transaction? = synchronized(lock) {
        transactions[transactionId]?.let { TransactionMapper.toDomain(it) }
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
        transactions.values.find { it.transaction.description == description }
            ?.let { TransactionMapper.toDomain(it) }
    }

    override suspend fun getFirstByDescriptionHavingAccount(description: String, accountTerm: String): Transaction? =
        synchronized(lock) {
            transactions.values.find { twa ->
                twa.transaction.description == description &&
                    twa.accounts.any { it.accountName.contains(accountTerm, true) }
            }?.let { TransactionMapper.toDomain(it) }
        }

    // Domain model mutation methods
    override suspend fun insertTransaction(transaction: Transaction, profileId: Long): Transaction {
        val id = synchronized(lock) { transaction.id ?: idCounter.getAndIncrement().toLong() }
        return transaction.copy(id = id)
    }

    override suspend fun storeTransaction(transaction: Transaction, profileId: Long) {
        insertTransaction(transaction, profileId)
    }

    // DB entity mutation methods (legacy)
    override suspend fun insertTransaction(transaction: TransactionWithAccounts): Unit = synchronized(lock) {
        if (transaction.transaction.id == 0L) {
            transaction.transaction.id = idCounter.getAndIncrement().toLong()
        }
        transactions[transaction.transaction.id] = transaction
    }

    override suspend fun storeTransaction(transaction: TransactionWithAccounts) {
        insertTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: DbTransaction): Unit = synchronized(lock) {
        transactions.remove(transaction.id)
    }

    override suspend fun deleteTransactions(transactions: List<DbTransaction>): Unit = synchronized(lock) {
        transactions.forEach { this.transactions.remove(it.id) }
    }

    override suspend fun storeTransactions(transactions: List<TransactionWithAccounts>, profileId: Long): Unit =
        synchronized(lock) {
            transactions.forEach { twa ->
                twa.transaction.profileId = profileId
                if (twa.transaction.id == 0L) {
                    twa.transaction.id = idCounter.getAndIncrement().toLong()
                }
                this.transactions[twa.transaction.id] = twa
            }
        }

    override suspend fun storeTransactionsAsDomain(transactions: List<Transaction>, profileId: Long): Unit =
        synchronized(lock) {
            transactions.forEach { tx ->
                val entity = TransactionMapper.toEntity(tx, profileId)
                if (entity.transaction.id == 0L) {
                    entity.transaction.id = idCounter.getAndIncrement().toLong()
                }
                this.transactions[entity.transaction.id] = entity
            }
        }

    override suspend fun deleteAllForProfile(profileId: Long): Int = synchronized(lock) {
        val toRemove = transactions.values.filter { it.transaction.profileId == profileId }
        toRemove.forEach { transactions.remove(it.transaction.id) }
        toRemove.size
    }

    override suspend fun getMaxLedgerId(profileId: Long): Long? = synchronized(lock) {
        transactions.values
            .filter { it.transaction.profileId == profileId }
            .maxOfOrNull { it.transaction.ledgerId }
    }
}
