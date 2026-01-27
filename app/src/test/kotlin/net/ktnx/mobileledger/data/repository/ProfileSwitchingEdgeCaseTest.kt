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
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine
import net.ktnx.mobileledger.core.domain.repository.ProfileRepository
import net.ktnx.mobileledger.core.domain.repository.TransactionRepository
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

    private fun createTestTransaction(profileId: Long, description: String = "Test Transaction"): Transaction =
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

    // ========================================
    // Empty profile list edge cases
    // ========================================

    @Test
    fun `currentProfile is null when no profiles exist`() = runTest {
        assertNull(profileRepository.currentProfile.value)
    }

    @Test
    fun `getAnyProfile returns null when no profiles exist`() = runTest {
        val anyProfile = profileRepository.getAnyProfile().getOrNull()
        assertNull(anyProfile)
    }

    @Test
    fun `getAllProfiles returns empty list when no profiles exist`() = runTest {
        val profiles = profileRepository.observeAllProfiles().first()
        assertTrue(profiles.isEmpty())
    }

    // ========================================
    // Profile deletion edge cases
    // ========================================

    @Test
    fun `deleting current profile sets currentProfile to null when last profile`() = runTest {
        val profile = createTestProfile(name = "Only Profile")
        val id = profileRepository.insertProfile(profile).getOrThrow()
        val profileWithId = profile.copy(id = id)
        profileRepository.setCurrentProfile(profileWithId)

        profileRepository.deleteProfile(profileWithId).getOrThrow()

        assertNull(profileRepository.currentProfile.value)
        assertEquals(0, profileRepository.getProfileCount().getOrThrow())
    }

    @Test
    fun `deleting current profile selects another profile when available`() = runTest {
        val profile1 = createTestProfile(name = "Profile 1", orderNo = 1)
        val profile2 = createTestProfile(name = "Profile 2", orderNo = 2)
        val id1 = profileRepository.insertProfile(profile1).getOrThrow()
        profileRepository.insertProfile(profile2).getOrThrow()
        val profile1WithId = profile1.copy(id = id1)
        profileRepository.setCurrentProfile(profile1WithId)

        profileRepository.deleteProfile(profile1WithId).getOrThrow()

        val current = profileRepository.currentProfile.value
        assertNotNull(current)
        assertEquals("Profile 2", current?.name)
    }

    @Test
    fun `deleting non-current profile does not change currentProfile`() = runTest {
        val profile1 = createTestProfile(name = "Profile 1")
        val profile2 = createTestProfile(name = "Profile 2")
        val id1 = profileRepository.insertProfile(profile1).getOrThrow()
        val id2 = profileRepository.insertProfile(profile2).getOrThrow()
        val profile1WithId = profile1.copy(id = id1)
        val profile2WithId = profile2.copy(id = id2)
        profileRepository.setCurrentProfile(profile1WithId)

        profileRepository.deleteProfile(profile2WithId).getOrThrow()

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
        val id = profileRepository.insertProfile(profile).getOrThrow()
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
            val insertedId = profileRepository.insertProfile(p).getOrThrow()
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
        val id = profileRepository.insertProfile(profile).getOrThrow()
        val profileWithId = profile.copy(id = id)
        profileRepository.setCurrentProfile(profileWithId)

        val updated = createTestProfile(id = id, name = "Updated Name")
        profileRepository.updateProfile(updated).getOrThrow()

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
        val id1 = profileRepository.insertProfile(profile1).getOrThrow()
        val id2 = profileRepository.insertProfile(profile2).getOrThrow()

        // Add transactions to each profile
        transactionRepository.insertTransaction(createTestTransaction(id1, "TX1 for P1"), id1).getOrThrow()
        transactionRepository.insertTransaction(createTestTransaction(id1, "TX2 for P1"), id1).getOrThrow()
        transactionRepository.insertTransaction(createTestTransaction(id2, "TX1 for P2"), id2).getOrThrow()

        // Verify isolation - check count and descriptions
        val p1Transactions = transactionRepository.observeAllTransactions(id1).first()
        val p2Transactions = transactionRepository.observeAllTransactions(id2).first()

        assertEquals(2, p1Transactions.size)
        assertEquals(1, p2Transactions.size)
        // Verify by description since domain model doesn't expose profileId
        assertTrue(p1Transactions.all { it.description.contains("P1") })
        assertTrue(p2Transactions.all { it.description.contains("P2") })
    }

    @Test
    fun `deleting profile transactions does not affect other profiles`() = runTest {
        val profile1Id = profileRepository.insertProfile(createTestProfile(name = "Profile 1")).getOrThrow()
        val profile2Id = profileRepository.insertProfile(createTestProfile(name = "Profile 2")).getOrThrow()

        transactionRepository.insertTransaction(createTestTransaction(profile1Id, "TX for P1"), profile1Id).getOrThrow()
        transactionRepository.insertTransaction(createTestTransaction(profile2Id, "TX for P2"), profile2Id).getOrThrow()

        transactionRepository.deleteAllForProfile(profile1Id).getOrThrow()

        val p1Transactions = transactionRepository.observeAllTransactions(profile1Id).first()
        val p2Transactions = transactionRepository.observeAllTransactions(profile2Id).first()

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
        val id1 = profileRepository.insertProfile(profile1).getOrThrow()
        profileRepository.insertProfile(profile2).getOrThrow()
        val profile1WithId = profile1.copy(id = id1)
        profileRepository.setCurrentProfile(profile1WithId)

        profileRepository.deleteAllProfiles().getOrThrow()

        assertNull(profileRepository.currentProfile.value)
        assertEquals(0, profileRepository.getProfileCount().getOrThrow())
        assertTrue(profileRepository.observeAllProfiles().first().isEmpty())
    }

    @Test
    fun `deleteAllProfiles on empty repository is safe`() = runTest {
        // Should not throw
        profileRepository.deleteAllProfiles().getOrThrow()

        assertNull(profileRepository.currentProfile.value)
        assertEquals(0, profileRepository.getProfileCount().getOrThrow())
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

    override fun observeAllProfiles(): Flow<List<Profile>> =
        MutableStateFlow(profiles.values.sortedBy { it.orderNo }.toList())

    override suspend fun getAllProfiles(): Result<List<Profile>> = Result.success(
        profiles.values.sortedBy {
            it.orderNo
        }.toList()
    )

    override fun observeProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profiles[profileId])

    override suspend fun getProfileById(profileId: Long): Result<Profile?> = Result.success(profiles[profileId])

    override fun observeProfileByUuid(uuid: String): Flow<Profile?> =
        MutableStateFlow(profiles.values.find { it.uuid == uuid })

    override suspend fun getProfileByUuid(uuid: String): Result<Profile?> = Result.success(
        profiles.values.find {
            it.uuid ==
                uuid
        }
    )

    override suspend fun getAnyProfile(): Result<Profile?> = Result.success(profiles.values.firstOrNull())

    override suspend fun getProfileCount(): Result<Int> = Result.success(profiles.size)

    override suspend fun insertProfile(profile: Profile): Result<Long> {
        val existingId = profile.id
        val id = if (existingId == null || existingId == 0L) nextId++ else existingId
        val profileWithId = profile.copy(id = id)
        profiles[id] = profileWithId
        return Result.success(id)
    }

    override suspend fun updateProfile(profile: Profile): Result<Unit> {
        val id = profile.id ?: return Result.success(Unit)
        profiles[id] = profile
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profile
        }
        return Result.success(Unit)
    }

    override suspend fun deleteProfile(profile: Profile): Result<Unit> {
        val id = profile.id ?: return Result.success(Unit)
        profiles.remove(id)
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profiles.values.firstOrNull()
        }
        return Result.success(Unit)
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>): Result<Unit> {
        profiles.forEachIndexed { index, profile ->
            val id = profile.id ?: return@forEachIndexed
            this.profiles[id]?.let { existing ->
                this.profiles[id] = existing.copy(orderNo = index)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun deleteAllProfiles(): Result<Unit> {
        profiles.clear()
        _currentProfile.value = null
        return Result.success(Unit)
    }
}

/**
 * Fake TransactionRepository for edge case testing.
 * Stores domain model Transactions with associated profileId.
 */
class EdgeCaseFakeTransactionRepository : TransactionRepository {
    private data class StoredTransaction(val transaction: Transaction, val profileId: Long)

    private val transactions = mutableMapOf<Long, StoredTransaction>()
    private var nextId = 1L

    // Flow methods (observe prefix)
    override fun observeAllTransactions(profileId: Long): Flow<List<Transaction>> = MutableStateFlow(
        transactions.values
            .filter { it.profileId == profileId }
            .map { it.transaction }
            .toList()
    )

    override fun observeTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<Transaction>> =
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

    override fun observeTransactionById(transactionId: Long): Flow<Transaction?> =
        MutableStateFlow(transactions[transactionId]?.transaction)

    // Suspend methods (no suffix)
    override suspend fun getTransactionById(transactionId: Long): Result<Transaction?> =
        Result.success(transactions[transactionId]?.transaction)

    override suspend fun searchByDescription(term: String): Result<List<String>> = Result.success(emptyList())

    override suspend fun getFirstByDescription(description: String): Result<Transaction?> = Result.success(null)

    override suspend fun getFirstByDescriptionHavingAccount(
        description: String,
        accountTerm: String
    ): Result<Transaction?> = Result.success(null)

    // Domain model mutation methods
    override suspend fun insertTransaction(transaction: Transaction, profileId: Long): Result<Transaction> {
        val id = transaction.id ?: nextId++
        val txWithId = transaction.copy(id = id)
        transactions[id] = StoredTransaction(txWithId, profileId)
        return Result.success(txWithId)
    }

    override suspend fun storeTransaction(transaction: Transaction, profileId: Long): Result<Unit> {
        insertTransaction(transaction, profileId)
        return Result.success(Unit)
    }

    override suspend fun deleteTransactionById(transactionId: Long): Result<Int> {
        val existed = transactions.containsKey(transactionId)
        transactions.remove(transactionId)
        return Result.success(if (existed) 1 else 0)
    }

    override suspend fun deleteTransactionsByIds(transactionIds: List<Long>): Result<Int> {
        var count = 0
        transactionIds.forEach { id ->
            if (transactions.containsKey(id)) {
                count++
            }
            transactions.remove(id)
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
        val toRemove = transactions.values.filter { it.profileId == profileId }
        toRemove.forEach { transactions.remove(it.transaction.id) }
        return Result.success(toRemove.size)
    }

    override suspend fun getMaxLedgerId(profileId: Long): Result<Long?> = Result.success(
        transactions.values
            .filter { it.profileId == profileId }
            .maxOfOrNull { it.transaction.ledgerId }
    )
}
