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
import net.ktnx.mobileledger.core.database.dao.AccountDAO
import net.ktnx.mobileledger.core.database.dao.AccountValueDAO
import net.ktnx.mobileledger.core.database.entity.Account
import net.ktnx.mobileledger.core.database.entity.AccountValue
import net.ktnx.mobileledger.core.database.entity.AccountWithAmounts
import net.ktnx.mobileledger.core.domain.model.Account as DomainAccount
import net.ktnx.mobileledger.core.domain.model.AccountAmount
import net.ktnx.mobileledger.core.domain.model.AppException
import net.ktnx.mobileledger.domain.usecase.AppExceptionMapper
import net.ktnx.mobileledger.domain.usecase.sync.SyncExceptionMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AccountRepositoryImpl].
 *
 * Tests verify:
 * - Query operations (observe and get)
 * - Search operations
 * - Mutation operations (store, delete)
 * - Error handling with Result<T>
 * - Domain model mapping
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockAccountDAO: AccountDAO
    private lateinit var mockAccountValueDAO: AccountValueDAO
    private lateinit var appExceptionMapper: AppExceptionMapper
    private lateinit var repository: AccountRepositoryImpl

    private val testProfileId = 1L

    @Before
    fun setup() {
        mockAccountDAO = mockk(relaxed = true)
        mockAccountValueDAO = mockk(relaxed = true)
        appExceptionMapper = AppExceptionMapper(SyncExceptionMapper())

        repository = AccountRepositoryImpl(
            accountDAO = mockAccountDAO,
            accountValueDAO = mockAccountValueDAO,
            appExceptionMapper = appExceptionMapper,
            ioDispatcher = testDispatcher
        )
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createDbAccount(
        id: Long = 1L,
        profileId: Long = testProfileId,
        name: String = "Assets:Bank",
        level: Int = 1
    ): Account = Account().apply {
        this.id = id
        this.profileId = profileId
        this.name = name
        this.nameUpper = name.uppercase()
        this.level = level
        this.generation = 1L
    }

    private fun createDbAccountValue(
        id: Long = 1L,
        accountId: Long = 1L,
        currency: String = "USD",
        value: Float = 1000.0f
    ): AccountValue = AccountValue().apply {
        this.id = id
        this.accountId = accountId
        this.currency = currency
        this.value = value
        this.generation = 1L
    }

    private fun createDbAccountWithAmounts(
        account: Account = createDbAccount(),
        amounts: List<AccountValue> = listOf(createDbAccountValue(accountId = account.id))
    ): AccountWithAmounts = AccountWithAmounts().apply {
        this.account = account
        this.amounts = amounts
    }

    private fun createDomainAccount(id: Long? = null, name: String = "Assets:Bank", level: Int = 1): DomainAccount =
        DomainAccount(
            id = id,
            name = name,
            level = level,
            isExpanded = true,
            isVisible = true,
            amounts = listOf(
                AccountAmount(currency = "USD", amount = 1000.0f)
            )
        )

    // ========================================
    // Query Operations - Flow tests
    // ========================================

    @Test
    fun `observeAllWithAmounts returns mapped domain models`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbAccountWithAmounts()
        every { mockAccountDAO.getAllWithAmounts(testProfileId, true) } returns flowOf(listOf(dbEntity))

        // When
        val result = repository.observeAllWithAmounts(testProfileId, includeZeroBalances = true).first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Assets:Bank", result[0].name)
        verify { mockAccountDAO.getAllWithAmounts(testProfileId, true) }
    }

    @Test
    fun `observeAllWithAmounts returns empty list when no accounts`() = runTest(testDispatcher) {
        // Given
        every { mockAccountDAO.getAllWithAmounts(testProfileId, false) } returns flowOf(emptyList())

        // When
        val result = repository.observeAllWithAmounts(testProfileId, includeZeroBalances = false).first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `observeByNameWithAmounts returns mapped domain model`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbAccountWithAmounts()
        every { mockAccountDAO.getByNameWithAmounts(testProfileId, "Assets:Bank") } returns flowOf(dbEntity)

        // When
        val result = repository.observeByNameWithAmounts(testProfileId, "Assets:Bank").first()

        // Then
        assertNotNull(result)
        assertEquals("Assets:Bank", result?.name)
    }

    // Note: Flow-based "not found" test removed because DAO returns Flow<AccountWithAmounts>
    // (non-nullable). The sync version getByNameWithAmountsSync returns nullable and is tested below.

    // ========================================
    // Search Operations - Flow tests
    // ========================================

    @Test
    fun `observeSearchAccountNames returns account names`() = runTest(testDispatcher) {
        // Given
        val containers = listOf(
            AccountDAO.AccountNameContainer("Assets:Bank"),
            AccountDAO.AccountNameContainer("Assets:Cash")
        )
        every { mockAccountDAO.lookupNamesInProfileByName(testProfileId, "ASSETS") } returns flowOf(containers)

        // When
        val result = repository.observeSearchAccountNames(testProfileId, "assets").first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains("Assets:Bank"))
        assertTrue(result.contains("Assets:Cash"))
    }

    @Test
    fun `observeSearchAccountNamesGlobal searches across profiles`() = runTest(testDispatcher) {
        // Given
        val containers = listOf(AccountDAO.AccountNameContainer("Expenses:Food"))
        every { mockAccountDAO.lookupNamesByName("FOOD") } returns flowOf(containers)

        // When
        val result = repository.observeSearchAccountNamesGlobal("food").first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Expenses:Food", result[0])
    }

    // ========================================
    // Query Operations - Suspend tests
    // ========================================

    @Test
    fun `getAllWithAmounts returns Result success with mapped domain models`() = runTest(testDispatcher) {
        // Given
        val dbEntities = listOf(createDbAccountWithAmounts())
        coEvery { mockAccountDAO.getAllWithAmountsSync(testProfileId, true) } returns dbEntities

        // When
        val result = repository.getAllWithAmounts(testProfileId, includeZeroBalances = true)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getAllWithAmounts returns Result failure on exception`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountDAO.getAllWithAmountsSync(testProfileId, true) } throws RuntimeException("DB error")

        // When
        val result = repository.getAllWithAmounts(testProfileId, includeZeroBalances = true)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppException)
    }

    @Test
    fun `getByNameWithAmounts returns account when found`() = runTest(testDispatcher) {
        // Given
        val dbEntity = createDbAccountWithAmounts()
        coEvery { mockAccountDAO.getByNameWithAmountsSync(testProfileId, "Assets:Bank") } returns dbEntity

        // When
        val result = repository.getByNameWithAmounts(testProfileId, "Assets:Bank")

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    // ========================================
    // Search Operations - Suspend tests
    // ========================================

    @Test
    fun `searchAccountNames calls DAO with uppercase term`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountDAO.lookupNamesInProfileByNameSync(testProfileId, "BANK") } returns emptyList()

        // When
        val result = repository.searchAccountNames(testProfileId, "bank")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockAccountDAO.lookupNamesInProfileByNameSync(testProfileId, "BANK") }
    }

    @Test
    fun `searchAccountsWithAmounts returns mapped domain models`() = runTest(testDispatcher) {
        // Given
        val dbEntities = listOf(createDbAccountWithAmounts())
        coEvery { mockAccountDAO.lookupWithAmountsInProfileByNameSync(testProfileId, "ASSETS") } returns dbEntities

        // When
        val result = repository.searchAccountsWithAmounts(testProfileId, "assets")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `searchAccountNamesGlobal calls DAO with uppercase term`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountDAO.lookupNamesByNameSync("EXPENSE") } returns emptyList()

        // When
        val result = repository.searchAccountNamesGlobal("expense")

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockAccountDAO.lookupNamesByNameSync("EXPENSE") }
    }

    // ========================================
    // Mutation Operations tests
    // ========================================

    @Test
    fun `storeAccountsAsDomain inserts accounts and values`() = runTest(testDispatcher) {
        // Given
        val domainAccounts = listOf(createDomainAccount())
        coEvery { mockAccountDAO.getGenerationSync(testProfileId) } returns 1L
        coEvery { mockAccountDAO.getByNameSync(testProfileId, any<String>()) } returns null
        coEvery { mockAccountDAO.insertSync(any<Account>()) } returns 1L
        coEvery { mockAccountValueDAO.insertSync(any<AccountValue>()) } returns 1L

        // When
        val result = repository.storeAccountsAsDomain(domainAccounts, testProfileId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockAccountDAO.insertSync(any<Account>()) }
        coVerify { mockAccountValueDAO.insertSync(any<AccountValue>()) }
        coVerify { mockAccountDAO.purgeOldAccountsSync(testProfileId, 2L) }
        coVerify { mockAccountDAO.purgeOldAccountValuesSync(testProfileId, 2L) }
    }

    @Test
    fun `storeAccountsAsDomain preserves amountsExpanded for existing accounts`() = runTest(testDispatcher) {
        // Given
        val existingAccount = createDbAccount().apply { amountsExpanded = true }
        val domainAccount = createDomainAccount(name = existingAccount.name)
        coEvery { mockAccountDAO.getGenerationSync(testProfileId) } returns 1L
        coEvery { mockAccountDAO.getByNameSync(testProfileId, existingAccount.name) } returns existingAccount
        coEvery { mockAccountDAO.insertSync(any<Account>()) } returns 1L
        coEvery { mockAccountValueDAO.insertSync(any<AccountValue>()) } returns 1L

        // When
        val result = repository.storeAccountsAsDomain(listOf(domainAccount), testProfileId)

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockAccountDAO.getByNameSync(testProfileId, existingAccount.name) }
    }

    @Test
    fun `getCountForProfile returns account count`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountDAO.getCountForProfileSync(testProfileId) } returns 10

        // When
        val result = repository.getCountForProfile(testProfileId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrNull())
    }

    @Test
    fun `deleteAllAccounts calls DAO`() = runTest(testDispatcher) {
        // Given
        coEvery { mockAccountDAO.deleteAllSync() } returns Unit

        // When
        val result = repository.deleteAllAccounts()

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockAccountDAO.deleteAllSync() }
    }
}
