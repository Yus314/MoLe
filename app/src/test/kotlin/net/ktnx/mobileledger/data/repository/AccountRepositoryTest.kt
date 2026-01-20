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
import net.ktnx.mobileledger.db.Account as DbAccount
import net.ktnx.mobileledger.db.AccountValue
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.AccountAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AccountRepository] using a fake repository implementation.
 *
 * These tests verify:
 * - Account retrieval with and without amounts
 * - Search functionality across profiles
 * - Batch sync operations for accounts
 *
 * Note: For proper Flow testing with Room, use instrumentation tests.
 * These unit tests use a fake repository that implements the interface directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryTest {

    private lateinit var repository: FakeAccountRepository
    private val testProfileId = 1L

    @Before
    fun setup() {
        repository = FakeAccountRepository()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestAccount(
        id: Long? = null,
        name: String = "Assets:Cash",
        level: Int = 1,
        amounts: List<AccountAmount> = emptyList()
    ): Account = Account(
        id = id,
        name = name,
        level = level,
        isExpanded = true,
        isVisible = true,
        amounts = amounts
    )

    private fun createTestAccountAmount(amount: Float = 100.0f, currency: String = "USD"): AccountAmount =
        AccountAmount(currency = currency, amount = amount)

    // ========================================
    // observeAllWithAmounts tests (Flow)
    // ========================================

    @Test
    fun `observeAllWithAmounts returns empty list when no accounts`() = runTest {
        val accounts = repository.observeAllWithAmounts(testProfileId, true).first()
        assertTrue(accounts.isEmpty())
    }

    @Test
    fun `observeAllWithAmounts filters by profile`() = runTest {
        repository.addTestAccount(testProfileId, createTestAccount(name = "Assets:Cash"))
        repository.addTestAccount(2L, createTestAccount(name = "Assets:Bank"))

        val accounts = repository.observeAllWithAmounts(testProfileId, true).first()

        assertEquals(1, accounts.size)
        assertEquals("Assets:Cash", accounts[0].name)
    }

    @Test
    fun `observeAllWithAmounts filters zero balances when requested`() = runTest {
        // Account with balance
        repository.addTestAccount(
            testProfileId,
            createTestAccount(
                name = "Assets:Cash",
                amounts = listOf(createTestAccountAmount(100.0f))
            )
        )

        // Account with zero balance
        repository.addTestAccount(testProfileId, createTestAccount(name = "Assets:Empty"))

        val withZero = repository.observeAllWithAmounts(testProfileId, true).first()
        val withoutZero = repository.observeAllWithAmounts(testProfileId, false).first()

        assertEquals(2, withZero.size)
        assertEquals(1, withoutZero.size)
        assertEquals("Assets:Cash", withoutZero[0].name)
    }

    // ========================================
    // getAllWithAmounts tests (suspend)
    // ========================================

    @Test
    fun `getAllWithAmounts returns accounts`() = runTest {
        repository.addTestAccount(testProfileId, createTestAccount(name = "Assets:Cash"))
        repository.addTestAccount(testProfileId, createTestAccount(name = "Expenses:Food"))

        val accounts = repository.getAllWithAmounts(testProfileId, true)

        assertEquals(2, accounts.size)
    }

    // ========================================
    // getByNameWithAmounts tests (suspend)
    // ========================================

    @Test
    fun `getByNameWithAmounts returns null for non-existent name`() = runTest {
        val result = repository.getByNameWithAmounts(testProfileId, "NonExistent")
        assertNull(result)
    }

    @Test
    fun `getByNameWithAmounts returns account when exists`() = runTest {
        repository.addTestAccount(testProfileId, createTestAccount(name = "Assets:Cash"))

        val result = repository.getByNameWithAmounts(testProfileId, "Assets:Cash")

        assertNotNull(result)
        assertEquals("Assets:Cash", result?.name)
    }

    // ========================================
    // observeByNameWithAmounts tests (Flow)
    // ========================================

    @Test
    fun `observeByNameWithAmounts returns null for non-existent name`() = runTest {
        val result = repository.observeByNameWithAmounts(testProfileId, "NonExistent").first()
        assertNull(result)
    }

    @Test
    fun `observeByNameWithAmounts scopes to profile`() = runTest {
        repository.addTestAccount(1L, createTestAccount(name = "Assets:Cash"))
        repository.addTestAccount(2L, createTestAccount(name = "Assets:Cash"))

        val result1 = repository.observeByNameWithAmounts(1L, "Assets:Cash").first()
        val result2 = repository.observeByNameWithAmounts(2L, "Assets:Cash").first()

        assertNotNull(result1)
        assertNotNull(result2)
        // Both should return the account (scoped by profile)
        assertEquals("Assets:Cash", result1?.name)
        assertEquals("Assets:Cash", result2?.name)
    }

    // ========================================
    // searchAccountNames tests (suspend)
    // ========================================

    @Test
    fun `searchAccountNames returns matching accounts`() = runTest {
        repository.addTestAccount(testProfileId, createTestAccount(name = "Assets:Cash"))
        repository.addTestAccount(testProfileId, createTestAccount(name = "Assets:Bank:Checking"))
        repository.addTestAccount(testProfileId, createTestAccount(name = "Expenses:Food"))

        val results = repository.searchAccountNames(testProfileId, "Assets")

        assertEquals(2, results.size)
        assertTrue(results.contains("Assets:Cash"))
        assertTrue(results.contains("Assets:Bank:Checking"))
    }

    @Test
    fun `searchAccountNames is case insensitive`() = runTest {
        repository.addTestAccount(testProfileId, createTestAccount(name = "Assets:Cash"))

        val results = repository.searchAccountNames(testProfileId, "ASSETS")

        assertEquals(1, results.size)
        assertEquals("Assets:Cash", results[0])
    }

    @Test
    fun `searchAccountNames matches substring`() = runTest {
        repository.addTestAccount(testProfileId, createTestAccount(name = "Assets:Bank:Checking"))

        val results = repository.searchAccountNames(testProfileId, "Bank")

        assertEquals(1, results.size)
    }

    // ========================================
    // searchAccountNamesGlobal tests (suspend)
    // ========================================

    @Test
    fun `searchAccountNamesGlobal searches across all profiles`() = runTest {
        repository.addTestAccount(1L, createTestAccount(name = "Assets:Cash"))
        repository.addTestAccount(2L, createTestAccount(name = "Assets:Bank"))
        repository.addTestAccount(3L, createTestAccount(name = "Expenses:Food"))

        val results = repository.searchAccountNamesGlobal("Assets")

        assertEquals(2, results.size)
    }

    @Test
    fun `searchAccountNamesGlobal returns empty for no match`() = runTest {
        repository.addTestAccount(testProfileId, createTestAccount(name = "Assets:Cash"))

        val results = repository.searchAccountNamesGlobal("NonExistent")

        assertTrue(results.isEmpty())
    }

    // ========================================
    // storeAccountsAsDomain tests
    // ========================================

    @Test
    fun `storeAccountsAsDomain replaces all accounts for profile`() = runTest {
        repository.addTestAccount(testProfileId, createTestAccount(name = "OldAccount"))

        val newAccounts = listOf(
            createTestAccount(name = "NewAccount1"),
            createTestAccount(name = "NewAccount2")
        )
        repository.storeAccountsAsDomain(newAccounts, testProfileId)

        val accounts = repository.observeAllWithAmounts(testProfileId, true).first()
        assertEquals(2, accounts.size)
        assertTrue(accounts.any { it.name == "NewAccount1" })
        assertTrue(accounts.any { it.name == "NewAccount2" })
    }

    @Test
    fun `storeAccountsAsDomain does not affect other profiles`() = runTest {
        repository.addTestAccount(2L, createTestAccount(name = "OtherProfile"))

        val newAccounts = listOf(createTestAccount(name = "Profile1Account"))
        repository.storeAccountsAsDomain(newAccounts, testProfileId)

        val profile1Accounts = repository.observeAllWithAmounts(testProfileId, true).first()
        val profile2Accounts = repository.observeAllWithAmounts(2L, true).first()

        assertEquals(1, profile1Accounts.size)
        assertEquals(1, profile2Accounts.size)
        assertEquals("Profile1Account", profile1Accounts[0].name)
        assertEquals("OtherProfile", profile2Accounts[0].name)
    }

    // ========================================
    // getCountForProfile tests
    // ========================================

    @Test
    fun `getCountForProfile returns zero when no accounts`() = runTest {
        val count = repository.getCountForProfile(testProfileId)
        assertEquals(0, count)
    }

    @Test
    fun `getCountForProfile returns correct count`() = runTest {
        repository.addTestAccount(testProfileId, createTestAccount(name = "A"))
        repository.addTestAccount(testProfileId, createTestAccount(name = "B"))
        repository.addTestAccount(2L, createTestAccount(name = "C"))

        val count = repository.getCountForProfile(testProfileId)
        assertEquals(2, count)
    }

    // ========================================
    // deleteAllAccounts tests
    // ========================================

    @Test
    fun `deleteAllAccounts removes all accounts`() = runTest {
        repository.addTestAccount(1L, createTestAccount(name = "A"))
        repository.addTestAccount(2L, createTestAccount(name = "B"))

        repository.deleteAllAccounts()

        val count1 = repository.getCountForProfile(1L)
        val count2 = repository.getCountForProfile(2L)
        assertEquals(0, count1)
        assertEquals(0, count2)
    }
}

/**
 * Fake implementation of [AccountRepository] for unit testing.
 *
 * This implementation provides an in-memory store that allows testing
 * without a real database or Room infrastructure.
 * Uses domain models (Account) for query operations.
 */
class FakeAccountRepository : AccountRepository {

    // Internal storage using domain models with profileId association
    private data class StoredAccount(val account: Account, val profileId: Long)

    private val accounts = mutableMapOf<Long, StoredAccount>()
    private var nextId = 1L

    // ========================================
    // Test Helper Methods (not part of interface)
    // ========================================

    /**
     * Test helper to add a single account without replacing all accounts.
     * This method is only for test setup.
     */
    fun addTestAccount(profileId: Long, account: Account): Long {
        val id = account.id ?: nextId++
        val accountWithId = account.copy(id = id)
        accounts[id] = StoredAccount(accountWithId, profileId)
        return id
    }

    fun reset() {
        accounts.clear()
        nextId = 1L
    }

    // ========================================
    // Private helpers
    // ========================================

    private fun getAccountsForProfile(profileId: Long): List<Account> =
        accounts.values.filter { it.profileId == profileId }.map { it.account }

    private fun hasNonZeroBalance(account: Account): Boolean = account.amounts.any { it.amount != 0f }

    // ========================================
    // Flow methods (observe prefix)
    // ========================================

    override fun observeAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> {
        val result = getAccountsForProfile(profileId)
            .filter { includeZeroBalances || hasNonZeroBalance(it) }
        return MutableStateFlow(result)
    }

    @Suppress("DEPRECATION")
    override fun observeByName(profileId: Long, accountName: String): Flow<DbAccount?> {
        val account = accounts.values.find { it.profileId == profileId && it.account.name == accountName }
        return MutableStateFlow(account?.let { toDbAccount(it.account, profileId) })
    }

    override fun observeByNameWithAmounts(profileId: Long, accountName: String): Flow<Account?> {
        val account = accounts.values.find { it.profileId == profileId && it.account.name == accountName }
        return MutableStateFlow(account?.account)
    }

    override fun observeSearchAccountNames(profileId: Long, term: String): Flow<List<String>> =
        MutableStateFlow(searchAccountNamesInternal(profileId, term))

    override fun observeSearchAccountNamesGlobal(term: String): Flow<List<String>> = MutableStateFlow(
        accounts.values
            .filter { it.account.name.contains(term, ignoreCase = true) }
            .map { it.account.name }
    )

    // ========================================
    // Suspend methods (no suffix)
    // ========================================

    override suspend fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): List<Account> =
        getAccountsForProfile(profileId)
            .filter { includeZeroBalances || hasNonZeroBalance(it) }

    @Suppress("DEPRECATION")
    override suspend fun getById(id: Long): DbAccount? {
        val stored = accounts[id] ?: return null
        return toDbAccount(stored.account, stored.profileId)
    }

    @Suppress("DEPRECATION")
    override suspend fun getByName(profileId: Long, accountName: String): DbAccount? {
        val account = accounts.values.find { it.profileId == profileId && it.account.name == accountName }
        return account?.let { toDbAccount(it.account, profileId) }
    }

    override suspend fun getByNameWithAmounts(profileId: Long, accountName: String): Account? {
        val account = accounts.values.find { it.profileId == profileId && it.account.name == accountName }
        return account?.account
    }

    override suspend fun searchAccountNames(profileId: Long, term: String): List<String> =
        searchAccountNamesInternal(profileId, term)

    private fun searchAccountNamesInternal(profileId: Long, term: String): List<String> = accounts.values
        .filter { it.profileId == profileId && it.account.name.contains(term, ignoreCase = true) }
        .map { it.account.name }

    override suspend fun searchAccountsWithAmounts(profileId: Long, term: String): List<Account> = accounts.values
        .filter { it.profileId == profileId && it.account.name.contains(term, ignoreCase = true) }
        .map { it.account }

    override suspend fun searchAccountNamesGlobal(term: String): List<String> = accounts.values
        .filter { it.account.name.contains(term, ignoreCase = true) }
        .map { it.account.name }

    // ========================================
    // Deprecated mutation methods (still needed for interface)
    // ========================================

    @Suppress("DEPRECATION")
    override suspend fun insertAccount(account: DbAccount): Long {
        val id = if (account.id == 0L) nextId++ else account.id
        val domainAccount = Account(
            id = id,
            name = account.name,
            level = account.level,
            isExpanded = account.expanded,
            isVisible = true,
            amounts = emptyList()
        )
        accounts[id] = StoredAccount(domainAccount, account.profileId)
        return id
    }

    @Suppress("DEPRECATION")
    override suspend fun insertAccountWithAmounts(accountWithAmounts: AccountWithAmounts) {
        val dbAccount = accountWithAmounts.account
        val id = if (dbAccount.id == 0L) nextId++ else dbAccount.id
        val domainAccount = Account(
            id = id,
            name = dbAccount.name,
            level = dbAccount.level,
            isExpanded = dbAccount.expanded,
            isVisible = true,
            amounts = accountWithAmounts.amounts.map { AccountAmount(currency = it.currency, amount = it.value) }
        )
        accounts[id] = StoredAccount(domainAccount, dbAccount.profileId)
    }

    @Suppress("DEPRECATION")
    override suspend fun updateAccount(account: DbAccount) {
        if (accounts.containsKey(account.id)) {
            val existing = accounts[account.id]!!
            val updated = existing.account.copy(
                name = account.name,
                level = account.level,
                isExpanded = account.expanded
            )
            accounts[account.id] = StoredAccount(updated, account.profileId)
        }
    }

    @Suppress("DEPRECATION")
    override suspend fun storeAccounts(accountsList: List<AccountWithAmounts>, profileId: Long) {
        // Remove existing accounts for this profile
        val toRemove = accounts.values.filter { it.profileId == profileId }.map { it.account.id }
        toRemove.filterNotNull().forEach { accounts.remove(it) }

        // Add new accounts
        accountsList.forEach { accountWithAmounts ->
            val dbAccount = accountWithAmounts.account
            dbAccount.profileId = profileId
            insertAccountWithAmounts(accountWithAmounts)
        }
    }

    override suspend fun storeAccountsAsDomain(accountsList: List<Account>, profileId: Long) {
        // Remove existing accounts for this profile
        val toRemove = accounts.values.filter { it.profileId == profileId }.map { it.account.id }
        toRemove.filterNotNull().forEach { accounts.remove(it) }

        // Add new accounts from domain models
        accountsList.forEach { account ->
            val id = account.id ?: nextId++
            accounts[id] = StoredAccount(account.copy(id = id), profileId)
        }
    }

    override suspend fun getCountForProfile(profileId: Long): Int = accounts.values.count {
        it.profileId == profileId
    }

    override suspend fun deleteAllAccounts() {
        accounts.clear()
    }

    // ========================================
    // Helper for deprecated methods
    // ========================================

    private fun toDbAccount(account: Account, profileId: Long): DbAccount = DbAccount().apply {
        this.id = account.id ?: 0L
        this.profileId = profileId
        this.name = account.name
        this.nameUpper = account.name.uppercase()
        this.level = account.level
        this.expanded = account.isExpanded
        this.parentName = account.parentName
    }
}
