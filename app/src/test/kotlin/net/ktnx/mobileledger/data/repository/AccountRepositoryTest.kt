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
 * - CRUD operations for accounts
 * - Batch sync operations
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
        id: Long = 0L,
        profileId: Long = testProfileId,
        name: String = "Assets:Cash",
        level: Int = 1
    ): DbAccount = DbAccount().apply {
        this.id = id
        this.profileId = profileId
        this.name = name
        this.nameUpper = name.uppercase()
        this.level = level
        this.parentName = if (name.contains(":")) name.substringBeforeLast(":") else null
    }

    private fun createTestAccountWithAmounts(
        id: Long = 0L,
        profileId: Long = testProfileId,
        name: String = "Assets:Cash",
        amounts: List<AccountValue> = emptyList()
    ): AccountWithAmounts = AccountWithAmounts().apply {
        this.account = createTestAccount(id, profileId, name)
        this.amounts = amounts
    }

    private fun createTestAccountValue(
        accountId: Long,
        amount: Float = 100.0f,
        currency: String = "USD"
    ): AccountValue = AccountValue().apply {
        this.id = 0L
        this.accountId = accountId
        this.value = amount
        this.currency = currency
    }

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
        repository.insertAccount(createTestAccount(profileId = 1L, name = "Assets:Cash"))
        repository.insertAccount(createTestAccount(profileId = 2L, name = "Assets:Bank"))

        val accounts = repository.observeAllWithAmounts(1L, true).first()

        assertEquals(1, accounts.size)
        assertEquals("Assets:Cash", accounts[0].name)
    }

    @Test
    fun `observeAllWithAmounts filters zero balances when requested`() = runTest {
        // Account with balance
        val accountWithBalance = createTestAccountWithAmounts(
            name = "Assets:Cash",
            amounts = listOf(createTestAccountValue(1L, 100.0f))
        )
        repository.insertAccountWithAmounts(accountWithBalance)

        // Account with zero balance
        repository.insertAccount(createTestAccount(name = "Assets:Empty"))

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
        repository.insertAccount(createTestAccount(name = "Assets:Cash"))
        repository.insertAccount(createTestAccount(name = "Expenses:Food"))

        val accounts = repository.getAllWithAmounts(testProfileId, true)

        assertEquals(2, accounts.size)
    }

    // ========================================
    // getById tests (suspend)
    // ========================================

    @Test
    fun `getById returns null for non-existent id`() = runTest {
        val result = repository.getById(999L)
        assertNull(result)
    }

    @Test
    fun `getById returns account when exists`() = runTest {
        val account = createTestAccount(name = "Assets:Cash")
        val id = repository.insertAccount(account)

        val result = repository.getById(id)

        assertNotNull(result)
        assertEquals("Assets:Cash", result?.name)
    }

    // ========================================
    // observeByName tests (Flow)
    // ========================================

    @Test
    fun `observeByName returns null for non-existent name`() = runTest {
        val result = repository.observeByName(testProfileId, "NonExistent").first()
        assertNull(result)
    }

    @Test
    fun `observeByName scopes to profile`() = runTest {
        repository.insertAccount(createTestAccount(profileId = 1L, name = "Assets:Cash"))
        repository.insertAccount(createTestAccount(profileId = 2L, name = "Assets:Cash"))

        val result1 = repository.observeByName(1L, "Assets:Cash").first()
        val result2 = repository.observeByName(2L, "Assets:Cash").first()

        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(1L, result1?.profileId)
        assertEquals(2L, result2?.profileId)
    }

    // ========================================
    // getByName tests (suspend)
    // ========================================

    @Test
    fun `getByName returns account when exists`() = runTest {
        repository.insertAccount(createTestAccount(name = "Assets:Cash"))

        val result = repository.getByName(testProfileId, "Assets:Cash")

        assertNotNull(result)
        assertEquals("Assets:Cash", result?.name)
    }

    // ========================================
    // searchAccountNames tests (suspend)
    // ========================================

    @Test
    fun `searchAccountNames returns matching accounts`() = runTest {
        repository.insertAccount(createTestAccount(name = "Assets:Cash"))
        repository.insertAccount(createTestAccount(name = "Assets:Bank:Checking"))
        repository.insertAccount(createTestAccount(name = "Expenses:Food"))

        val results = repository.searchAccountNames(testProfileId, "Assets")

        assertEquals(2, results.size)
        assertTrue(results.contains("Assets:Cash"))
        assertTrue(results.contains("Assets:Bank:Checking"))
    }

    @Test
    fun `searchAccountNames is case insensitive`() = runTest {
        repository.insertAccount(createTestAccount(name = "Assets:Cash"))

        val results = repository.searchAccountNames(testProfileId, "ASSETS")

        assertEquals(1, results.size)
        assertEquals("Assets:Cash", results[0])
    }

    @Test
    fun `searchAccountNames matches substring`() = runTest {
        repository.insertAccount(createTestAccount(name = "Assets:Bank:Checking"))

        val results = repository.searchAccountNames(testProfileId, "Bank")

        assertEquals(1, results.size)
    }

    // ========================================
    // searchAccountNamesGlobal tests (suspend)
    // ========================================

    @Test
    fun `searchAccountNamesGlobal searches across all profiles`() = runTest {
        repository.insertAccount(createTestAccount(profileId = 1L, name = "Assets:Cash"))
        repository.insertAccount(createTestAccount(profileId = 2L, name = "Assets:Bank"))
        repository.insertAccount(createTestAccount(profileId = 3L, name = "Expenses:Food"))

        val results = repository.searchAccountNamesGlobal("Assets")

        assertEquals(2, results.size)
    }

    @Test
    fun `searchAccountNamesGlobal returns empty for no match`() = runTest {
        repository.insertAccount(createTestAccount(name = "Assets:Cash"))

        val results = repository.searchAccountNamesGlobal("NonExistent")

        assertTrue(results.isEmpty())
    }

    // ========================================
    // insertAccount tests
    // ========================================

    @Test
    fun `insertAccount assigns id and returns it`() = runTest {
        val account = createTestAccount(name = "Assets:Cash")

        val id = repository.insertAccount(account)

        assertTrue(id > 0)
        val stored = repository.getById(id)
        assertNotNull(stored)
        assertEquals("Assets:Cash", stored?.name)
    }

    // ========================================
    // updateAccount tests
    // ========================================

    @Test
    fun `updateAccount modifies existing account`() = runTest {
        val account = createTestAccount(name = "Original")
        val id = repository.insertAccount(account)

        val updated = createTestAccount(id = id, name = "Updated")
        repository.updateAccount(updated)

        val result = repository.getById(id)
        assertEquals("Updated", result?.name)
    }

    // ========================================
    // deleteAccount tests
    // ========================================

    // ========================================
    // storeAccounts tests
    // ========================================

    @Test
    fun `storeAccounts replaces all accounts for profile`() = runTest {
        repository.insertAccount(createTestAccount(name = "OldAccount"))

        val newAccounts = listOf(
            createTestAccountWithAmounts(name = "NewAccount1"),
            createTestAccountWithAmounts(name = "NewAccount2")
        )
        repository.storeAccounts(newAccounts, testProfileId)

        val accounts = repository.observeAllWithAmounts(testProfileId, true).first()
        assertEquals(2, accounts.size)
        assertTrue(accounts.any { it.name == "NewAccount1" })
        assertTrue(accounts.any { it.name == "NewAccount2" })
    }

    @Test
    fun `storeAccounts does not affect other profiles`() = runTest {
        repository.insertAccount(createTestAccount(profileId = 2L, name = "OtherProfile"))

        val newAccounts = listOf(
            createTestAccountWithAmounts(name = "Profile1Account")
        )
        repository.storeAccounts(newAccounts, testProfileId)

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
        repository.insertAccount(createTestAccount(profileId = testProfileId, name = "A"))
        repository.insertAccount(createTestAccount(profileId = testProfileId, name = "B"))
        repository.insertAccount(createTestAccount(profileId = 2L, name = "C"))

        val count = repository.getCountForProfile(testProfileId)
        assertEquals(2, count)
    }

    // ========================================
    // deleteAllAccounts tests
    // ========================================

    @Test
    fun `deleteAllAccounts removes all accounts`() = runTest {
        repository.insertAccount(createTestAccount(profileId = 1L, name = "A"))
        repository.insertAccount(createTestAccount(profileId = 2L, name = "B"))

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
 * Now uses domain models (Account) for query operations.
 */
class FakeAccountRepository : AccountRepository {

    private val dbAccounts = mutableMapOf<Long, DbAccount>()
    private val accountAmounts = mutableMapOf<Long, MutableList<AccountValue>>()
    private var nextId = 1L

    private fun emitChanges() {
        // Placeholder for Flow updates
    }

    private fun getAccountsForProfile(profileId: Long): List<DbAccount> =
        dbAccounts.values.filter { it.profileId == profileId }

    private fun hasNonZeroBalance(accountId: Long): Boolean {
        val amounts = accountAmounts[accountId] ?: return false
        return amounts.any { it.value != 0f }
    }

    private fun toDomain(dbAccount: DbAccount): Account = Account(
        id = dbAccount.id,
        name = dbAccount.name,
        level = dbAccount.level,
        isExpanded = dbAccount.expanded,
        isVisible = true,
        amounts = (accountAmounts[dbAccount.id] ?: emptyList()).map {
            AccountAmount(currency = it.currency, amount = it.value)
        }
    )

    // Flow methods (observe prefix)
    override fun observeAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> {
        val result = getAccountsForProfile(profileId)
            .filter { includeZeroBalances || hasNonZeroBalance(it.id) }
            .map { toDomain(it) }
        return MutableStateFlow(result)
    }

    override fun observeByName(profileId: Long, accountName: String): Flow<DbAccount?> = MutableStateFlow(
        dbAccounts.values.find { it.profileId == profileId && it.name == accountName }
    )

    override fun observeByNameWithAmounts(profileId: Long, accountName: String): Flow<Account?> {
        val account = dbAccounts.values.find { it.profileId == profileId && it.name == accountName }
        return MutableStateFlow(account?.let { toDomain(it) })
    }

    override fun observeSearchAccountNames(profileId: Long, term: String): Flow<List<String>> =
        MutableStateFlow(searchAccountNamesInternal(profileId, term))

    override fun observeSearchAccountNamesGlobal(term: String): Flow<List<String>> = MutableStateFlow(
        dbAccounts.values
            .filter { it.name.contains(term, ignoreCase = true) }
            .map { it.name }
    )

    // Suspend methods (no suffix)
    override suspend fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): List<Account> =
        getAccountsForProfile(profileId)
            .filter { includeZeroBalances || hasNonZeroBalance(it.id) }
            .map { toDomain(it) }

    override suspend fun getById(id: Long): DbAccount? = dbAccounts[id]

    override suspend fun getByName(profileId: Long, accountName: String): DbAccount? =
        dbAccounts.values.find { it.profileId == profileId && it.name == accountName }

    override suspend fun getByNameWithAmounts(profileId: Long, accountName: String): Account? {
        val account = dbAccounts.values.find { it.profileId == profileId && it.name == accountName }
        return account?.let { toDomain(it) }
    }

    override suspend fun searchAccountNames(profileId: Long, term: String): List<String> =
        searchAccountNamesInternal(profileId, term)

    private fun searchAccountNamesInternal(profileId: Long, term: String): List<String> = dbAccounts.values
        .filter { it.profileId == profileId && it.name.contains(term, ignoreCase = true) }
        .map { it.name }

    override suspend fun searchAccountsWithAmounts(profileId: Long, term: String): List<Account> = dbAccounts.values
        .filter { it.profileId == profileId && it.name.contains(term, ignoreCase = true) }
        .map { toDomain(it) }

    override suspend fun searchAccountNamesGlobal(term: String): List<String> = dbAccounts.values
        .filter { it.name.contains(term, ignoreCase = true) }
        .map { it.name }

    override suspend fun insertAccount(account: DbAccount): Long {
        val id = if (account.id == 0L) nextId++ else account.id
        account.id = id
        dbAccounts[id] = account
        accountAmounts[id] = mutableListOf()
        emitChanges()
        return id
    }

    override suspend fun insertAccountWithAmounts(accountWithAmounts: AccountWithAmounts) {
        val id = insertAccount(accountWithAmounts.account)
        accountAmounts[id] = accountWithAmounts.amounts.toMutableList()
    }

    override suspend fun updateAccount(account: DbAccount) {
        if (dbAccounts.containsKey(account.id)) {
            dbAccounts[account.id] = account
            emitChanges()
        }
    }

    override suspend fun storeAccounts(accounts: List<AccountWithAmounts>, profileId: Long) {
        // Remove existing accounts for this profile
        val toRemove = this.dbAccounts.values.filter { it.profileId == profileId }.map { it.id }
        toRemove.forEach {
            this.dbAccounts.remove(it)
            this.accountAmounts.remove(it)
        }

        // Add new accounts
        accounts.forEach { accountWithAmounts ->
            accountWithAmounts.account.profileId = profileId
            insertAccountWithAmounts(accountWithAmounts)
        }
    }

    override suspend fun storeAccountsAsDomain(accounts: List<Account>, profileId: Long) {
        // Remove existing accounts for this profile
        val toRemove = this.dbAccounts.values.filter { it.profileId == profileId }.map { it.id }
        toRemove.forEach {
            this.dbAccounts.remove(it)
            this.accountAmounts.remove(it)
        }

        // Add new accounts from domain models
        accounts.forEach { account ->
            val id = account.id ?: nextId++
            val dbAccount = DbAccount().apply {
                this.id = id
                this.profileId = profileId
                this.name = account.name
                this.nameUpper = account.name.uppercase()
                this.parentName = account.parentName
                this.level = account.level
                this.expanded = account.isExpanded
            }
            dbAccounts[id] = dbAccount
            accountAmounts[id] = account.amounts.map { amt ->
                AccountValue().apply {
                    this.accountId = id
                    this.currency = amt.currency
                    this.value = amt.amount
                }
            }.toMutableList()
        }
    }

    override suspend fun getCountForProfile(profileId: Long): Int = dbAccounts.values.count {
        it.profileId == profileId
    }

    override suspend fun deleteAllAccounts() {
        dbAccounts.clear()
        accountAmounts.clear()
        emitChanges()
    }
}
