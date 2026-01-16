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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for edge cases in profile switching scenarios.
 *
 * These tests verify that:
 * - Profile switching correctly updates currentProfile
 * - Deleting the current profile handles fallback correctly
 * - Empty profile list is handled gracefully
 * - Profile-scoped data is isolated between profiles
 * - Rapid profile switches maintain consistency
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSwitchingEdgeCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: EdgeCaseFakeProfileRepository
    private lateinit var transactionRepository: EdgeCaseFakeTransactionRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = EdgeCaseFakeProfileRepository()
        transactionRepository = EdgeCaseFakeTransactionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(id: Long? = null, name: String = "Test Profile", orderNo: Int = 1): Profile =
        createTestDomainProfile(id = id, name = name, orderNo = orderNo)

    private fun createTestTransaction(
        profileId: Long,
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
    // Empty profile list edge cases
    // ========================================

    @Test
    fun `currentProfile is null when no profiles exist`() = runTest {
        assertNull(profileRepository.currentProfile.value)
    }

    @Test
    fun `getAnyProfile returns null when no profiles exist`() = runTest {
        val anyProfile = profileRepository.getAnyProfile()
        assertNull(anyProfile)
    }

    @Test
    fun `getAllProfiles returns empty list when no profiles exist`() = runTest {
        val profiles = profileRepository.getAllProfiles().first()
        assertTrue(profiles.isEmpty())
    }

    // ========================================
    // Profile deletion edge cases
    // ========================================

    @Test
    fun `deleting current profile sets currentProfile to null when last profile`() = runTest {
        val profile = createTestProfile(name = "Only Profile")
        val id = profileRepository.insertProfile(profile)
        val profileWithId = profile.copy(id = id)
        profileRepository.setCurrentProfile(profileWithId)

        profileRepository.deleteProfile(profileWithId)

        assertNull(profileRepository.currentProfile.value)
        assertEquals(0, profileRepository.getProfileCount())
    }

    @Test
    fun `deleting current profile selects another profile when available`() = runTest {
        val profile1 = createTestProfile(name = "Profile 1", orderNo = 1)
        val profile2 = createTestProfile(name = "Profile 2", orderNo = 2)
        val id1 = profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        val profile1WithId = profile1.copy(id = id1)
        profileRepository.setCurrentProfile(profile1WithId)

        profileRepository.deleteProfile(profile1WithId)

        val current = profileRepository.currentProfile.value
        assertNotNull(current)
        assertEquals("Profile 2", current?.name)
    }

    @Test
    fun `deleting non-current profile does not change currentProfile`() = runTest {
        val profile1 = createTestProfile(name = "Profile 1")
        val profile2 = createTestProfile(name = "Profile 2")
        val id1 = profileRepository.insertProfile(profile1)
        val id2 = profileRepository.insertProfile(profile2)
        val profile1WithId = profile1.copy(id = id1)
        val profile2WithId = profile2.copy(id = id2)
        profileRepository.setCurrentProfile(profile1WithId)

        profileRepository.deleteProfile(profile2WithId)

        val current = profileRepository.currentProfile.value
        assertNotNull(current)
        assertEquals("Profile 1", current?.name)
    }

    // ========================================
    // Profile switching edge cases
    // ========================================

    @Test
    fun `switching to null profile clears currentProfile`() = runTest {
        val profile = createTestProfile(name = "Profile")
        val id = profileRepository.insertProfile(profile)
        val profileWithId = profile.copy(id = id)
        profileRepository.setCurrentProfile(profileWithId)
        assertNotNull(profileRepository.currentProfile.value)

        profileRepository.setCurrentProfile(null)

        assertNull(profileRepository.currentProfile.value)
    }

    @Test
    fun `switching profile multiple times maintains consistency`() = runTest {
        val profiles = (1..5).map { i ->
            val p = createTestProfile(name = "Profile $i", orderNo = i)
            val insertedId = profileRepository.insertProfile(p)
            p.copy(id = insertedId)
        }

        // Switch through all profiles
        profiles.forEach { profile ->
            profileRepository.setCurrentProfile(profile)
            assertEquals(profile.id, profileRepository.currentProfile.value?.id)
        }

        // Switch back and forth
        profileRepository.setCurrentProfile(profiles[0])
        assertEquals(profiles[0].id, profileRepository.currentProfile.value?.id)

        profileRepository.setCurrentProfile(profiles[4])
        assertEquals(profiles[4].id, profileRepository.currentProfile.value?.id)
    }

    @Test
    fun `updating current profile updates StateFlow`() = runTest {
        val profile = createTestProfile(name = "Original Name")
        val id = profileRepository.insertProfile(profile)
        val profileWithId = profile.copy(id = id)
        profileRepository.setCurrentProfile(profileWithId)

        val updated = createTestProfile(id = id, name = "Updated Name")
        profileRepository.updateProfile(updated)

        val current = profileRepository.currentProfile.value
        assertEquals("Updated Name", current?.name)
    }

    // ========================================
    // Profile-scoped data isolation
    // ========================================

    @Test
    fun `transactions are isolated by profile`() = runTest {
        val profile1 = createTestProfile(name = "Profile 1")
        val profile2 = createTestProfile(name = "Profile 2")
        val id1 = profileRepository.insertProfile(profile1)
        val id2 = profileRepository.insertProfile(profile2)

        // Add transactions to each profile
        transactionRepository.insertTransaction(createTestTransaction(id1, "TX1 for P1"))
        transactionRepository.insertTransaction(createTestTransaction(id1, "TX2 for P1"))
        transactionRepository.insertTransaction(createTestTransaction(id2, "TX1 for P2"))

        // Verify isolation - check count and descriptions
        val p1Transactions = transactionRepository.getAllTransactions(id1).first()
        val p2Transactions = transactionRepository.getAllTransactions(id2).first()

        assertEquals(2, p1Transactions.size)
        assertEquals(1, p2Transactions.size)
        // Verify by description since domain model doesn't expose profileId
        assertTrue(p1Transactions.all { it.description.contains("P1") })
        assertTrue(p2Transactions.all { it.description.contains("P2") })
    }

    @Test
    fun `deleting profile transactions does not affect other profiles`() = runTest {
        val profile1Id = profileRepository.insertProfile(createTestProfile(name = "Profile 1"))
        val profile2Id = profileRepository.insertProfile(createTestProfile(name = "Profile 2"))

        transactionRepository.insertTransaction(createTestTransaction(profile1Id, "TX for P1"))
        transactionRepository.insertTransaction(createTestTransaction(profile2Id, "TX for P2"))

        transactionRepository.deleteAllForProfile(profile1Id)

        val p1Transactions = transactionRepository.getAllTransactions(profile1Id).first()
        val p2Transactions = transactionRepository.getAllTransactions(profile2Id).first()

        assertEquals(0, p1Transactions.size)
        assertEquals(1, p2Transactions.size)
    }

    // ========================================
    // deleteAllProfiles edge cases
    // ========================================

    @Test
    fun `deleteAllProfiles clears everything`() = runTest {
        val profile1 = createTestProfile(name = "Profile 1")
        val profile2 = createTestProfile(name = "Profile 2")
        val id1 = profileRepository.insertProfile(profile1)
        profileRepository.insertProfile(profile2)
        val profile1WithId = profile1.copy(id = id1)
        profileRepository.setCurrentProfile(profile1WithId)

        profileRepository.deleteAllProfiles()

        assertNull(profileRepository.currentProfile.value)
        assertEquals(0, profileRepository.getProfileCount())
        assertTrue(profileRepository.getAllProfiles().first().isEmpty())
    }

    @Test
    fun `deleteAllProfiles on empty repository is safe`() = runTest {
        // Should not throw
        profileRepository.deleteAllProfiles()

        assertNull(profileRepository.currentProfile.value)
        assertEquals(0, profileRepository.getProfileCount())
    }
}

/**
 * Fake ProfileRepository for edge case testing.
 */
class EdgeCaseFakeProfileRepository : ProfileRepository {
    private val profiles = mutableMapOf<Long, Profile>()
    private var nextId = 1L
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun getAllProfiles(): Flow<List<Profile>> =
        MutableStateFlow(profiles.values.sortedBy { it.orderNo }.toList())

    override suspend fun getAllProfilesSync(): List<Profile> = profiles.values.sortedBy { it.orderNo }.toList()

    override fun getProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profiles[profileId])

    override suspend fun getProfileByIdSync(profileId: Long): Profile? = profiles[profileId]

    override fun getProfileByUuid(uuid: String): Flow<Profile?> =
        MutableStateFlow(profiles.values.find { it.uuid == uuid })

    override suspend fun getProfileByUuidSync(uuid: String): Profile? = profiles.values.find { it.uuid == uuid }

    override suspend fun getAnyProfile(): Profile? = profiles.values.firstOrNull()

    override suspend fun getProfileCount(): Int = profiles.size

    override suspend fun insertProfile(profile: Profile): Long {
        val id = if (profile.id == null || profile.id == 0L) nextId++ else profile.id
        val profileWithId = profile.copy(id = id)
        profiles[id] = profileWithId
        return id
    }

    override suspend fun updateProfile(profile: Profile) {
        val id = profile.id ?: return
        profiles[id] = profile
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profile
        }
    }

    override suspend fun deleteProfile(profile: Profile) {
        val id = profile.id ?: return
        profiles.remove(id)
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profiles.values.firstOrNull()
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {
        profiles.forEachIndexed { index, profile ->
            val id = profile.id ?: return@forEachIndexed
            this.profiles[id]?.let { existing ->
                this.profiles[id] = existing.copy(orderNo = index)
            }
        }
    }

    override suspend fun deleteAllProfiles() {
        profiles.clear()
        _currentProfile.value = null
    }
}

/**
 * Fake TransactionRepository for edge case testing.
 */
class EdgeCaseFakeTransactionRepository : TransactionRepository {
    private val transactions = mutableMapOf<Long, TransactionWithAccounts>()
    private var nextId = 1L

    override fun getAllTransactions(profileId: Long): Flow<List<Transaction>> =
        MutableStateFlow(transactions.values.filter { it.transaction.profileId == profileId }.toList())
            .map { TransactionMapper.toDomainList(it) }

    override fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<Transaction>> =
        MutableStateFlow(
            transactions.values.filter { it.transaction.profileId == profileId }.toList()
        ).map { TransactionMapper.toDomainList(it) }

    override fun getTransactionById(transactionId: Long): Flow<Transaction?> =
        MutableStateFlow(transactions[transactionId])
            .map { it?.let { TransactionMapper.toDomain(it) } }

    override suspend fun getTransactionByIdSync(transactionId: Long): Transaction? =
        transactions[transactionId]?.let { TransactionMapper.toDomain(it) }

    override suspend fun searchByDescription(term: String): List<TransactionDAO.DescriptionContainer> = emptyList()

    override suspend fun getFirstByDescription(description: String): Transaction? = null

    override suspend fun getFirstByDescriptionHavingAccount(description: String, accountTerm: String): Transaction? =
        null

    // Domain model mutation methods
    override suspend fun insertTransaction(transaction: Transaction, profileId: Long): Transaction {
        val id = transaction.id ?: nextId++
        return transaction.copy(id = id)
    }

    override suspend fun storeTransaction(transaction: Transaction, profileId: Long) {
        insertTransaction(transaction, profileId)
    }

    // DB entity mutation methods (legacy)
    override suspend fun insertTransaction(transaction: TransactionWithAccounts) {
        if (transaction.transaction.id == 0L) {
            transaction.transaction.id = nextId++
        }
        transactions[transaction.transaction.id] = transaction
    }

    override suspend fun storeTransaction(transaction: TransactionWithAccounts) {
        insertTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: DbTransaction) {
        transactions.remove(transaction.id)
    }

    override suspend fun deleteTransactions(transactions: List<DbTransaction>) {
        transactions.forEach { this.transactions.remove(it.id) }
    }

    override suspend fun storeTransactions(transactions: List<TransactionWithAccounts>, profileId: Long) {
        transactions.forEach { insertTransaction(it) }
    }

    override suspend fun storeTransactionsAsDomain(transactions: List<Transaction>, profileId: Long) {
        transactions.forEach { tx ->
            val entity = TransactionMapper.toEntity(tx, profileId)
            insertTransaction(entity)
        }
    }

    override suspend fun deleteAllForProfile(profileId: Long): Int {
        val toRemove = transactions.values.filter { it.transaction.profileId == profileId }
        toRemove.forEach { transactions.remove(it.transaction.id) }
        return toRemove.size
    }

    override suspend fun getMaxLedgerId(profileId: Long): Long? = transactions.values
        .filter { it.transaction.profileId == profileId }
        .maxOfOrNull { it.transaction.ledgerId }
}
