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

package net.ktnx.mobileledger.ui.transaction

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TemplateRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.db.Currency
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.TemplateHeader
import net.ktnx.mobileledger.db.TemplateWithAccounts
import net.ktnx.mobileledger.db.Transaction
import net.ktnx.mobileledger.db.TransactionWithAccounts
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NewTransactionViewModel repository interactions.
 *
 * These tests verify that NewTransactionViewModel correctly uses:
 * - ProfileRepository for current profile and settings
 * - TransactionRepository for saving transactions and description lookup
 * - AccountRepository for account name autocomplete
 * - TemplateRepository for template application
 * - CurrencyRepository for currency list
 *
 * Note: Full ViewModel testing with UI state and effects requires instrumentation tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var profileRepository: FakeProfileRepositoryForTransaction
    private lateinit var transactionRepository: FakeTransactionRepositoryForTransaction
    private lateinit var accountRepository: FakeAccountRepositoryForTransaction
    private lateinit var templateRepository: FakeTemplateRepositoryForTransaction
    private lateinit var currencyRepository: FakeCurrencyRepositoryForTransaction

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepositoryForTransaction()
        transactionRepository = FakeTransactionRepositoryForTransaction()
        accountRepository = FakeAccountRepositoryForTransaction()
        templateRepository = FakeTemplateRepositoryForTransaction()
        currencyRepository = FakeCurrencyRepositoryForTransaction()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestProfile(id: Long = 1L, name: String = "Test Profile"): Profile = Profile().apply {
        this.id = id
        this.name = name
        this.uuid = java.util.UUID.randomUUID().toString()
        this.url = "https://example.com/ledger"
        this.permitPosting = true
        this.showCommodityByDefault = true
    }

    private fun createTestTransaction(
        profileId: Long = 1L,
        description: String = "Test Transaction"
    ): TransactionWithAccounts {
        val transaction = Transaction().apply {
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
    // ProfileRepository tests
    // ========================================

    @Test
    fun `currentProfile provides profile settings`() = runTest {
        val profile = createTestProfile(name = "Active Profile")
        profileRepository.setCurrentProfile(profile)

        val current = profileRepository.currentProfile.value
        assertNotNull(current)
        assertEquals("Active Profile", current?.name)
        assertTrue(current?.permitPosting == true)
    }

    @Test
    fun `currentProfile null when no profile selected`() = runTest {
        val current = profileRepository.currentProfile.value
        assertNull(current)
    }

    // ========================================
    // TransactionRepository tests
    // ========================================

    @Test
    fun `searchByDescription returns matching transactions`() = runTest {
        transactionRepository.insertTransaction(createTestTransaction(description = "Grocery shopping"))
        transactionRepository.insertTransaction(createTestTransaction(description = "Gas station"))
        transactionRepository.insertTransaction(createTestTransaction(description = "Groceries"))

        val results = transactionRepository.searchByDescription("groc")

        assertEquals(2, results.size)
        assertTrue(results.any { it.description == "Grocery shopping" })
        assertTrue(results.any { it.description == "Groceries" })
    }

    @Test
    fun `getFirstByDescription returns most recent match`() = runTest {
        val tx1 = createTestTransaction(description = "Coffee").apply {
            transaction.year = 2025
            transaction.month = 12
        }
        val tx2 = createTestTransaction(description = "Coffee").apply {
            transaction.year = 2026
            transaction.month = 1
        }
        transactionRepository.insertTransaction(tx1)
        transactionRepository.insertTransaction(tx2)

        val result = transactionRepository.getFirstByDescription("Coffee")

        assertNotNull(result)
        assertEquals(2026, result?.transaction?.year)
    }

    @Test
    fun `storeTransaction saves new transaction`() = runTest {
        val tx = createTestTransaction(description = "New Transaction")

        transactionRepository.storeTransaction(tx)

        val stored = transactionRepository.getFirstByDescription("New Transaction")
        assertNotNull(stored)
    }

    // ========================================
    // AccountRepository tests
    // ========================================

    @Test
    fun `searchAccountNamesSync returns matching accounts`() = runTest {
        accountRepository.addAccount(1L, "Assets:Cash")
        accountRepository.addAccount(1L, "Assets:Bank:Checking")
        accountRepository.addAccount(1L, "Expenses:Food")

        val results = accountRepository.searchAccountNamesSync(1L, "Assets")

        assertEquals(2, results.size)
        assertTrue(results.contains("Assets:Cash"))
        assertTrue(results.contains("Assets:Bank:Checking"))
    }

    @Test
    fun `searchAccountNamesSync is case insensitive`() = runTest {
        accountRepository.addAccount(1L, "Assets:Cash")

        val results = accountRepository.searchAccountNamesSync(1L, "ASSETS")

        assertEquals(1, results.size)
        assertEquals("Assets:Cash", results[0])
    }

    @Test
    fun `searchAccountNamesSync returns empty for no matches`() = runTest {
        accountRepository.addAccount(1L, "Assets:Cash")

        val results = accountRepository.searchAccountNamesSync(1L, "Liabilities")

        assertTrue(results.isEmpty())
    }

    // ========================================
    // TemplateRepository tests
    // ========================================

    @Test
    fun `getAllTemplatesWithAccountsSync returns all templates`() = runTest {
        templateRepository.addTemplate(1L, "Rent Payment")
        templateRepository.addTemplate(2L, "Grocery Shopping")

        val templates = templateRepository.getAllTemplatesWithAccountsSync()

        assertEquals(2, templates.size)
    }

    @Test
    fun `getTemplateWithAccountsSync returns specific template`() = runTest {
        templateRepository.addTemplate(1L, "Rent Payment")

        val template = templateRepository.getTemplateWithAccountsSync(1L)

        assertNotNull(template)
        assertEquals("Rent Payment", template?.header?.name)
    }

    @Test
    fun `getTemplateWithAccountsSync returns null for non-existent template`() = runTest {
        val template = templateRepository.getTemplateWithAccountsSync(999L)
        assertNull(template)
    }

    // ========================================
    // CurrencyRepository tests
    // ========================================

    @Test
    fun `getAllCurrenciesSync returns all currencies`() = runTest {
        currencyRepository.addCurrency("USD")
        currencyRepository.addCurrency("EUR")
        currencyRepository.addCurrency("JPY")

        val currencies = currencyRepository.getAllCurrenciesSync()

        assertEquals(3, currencies.size)
    }

    @Test
    fun `getCurrencyByNameSync returns matching currency`() = runTest {
        currencyRepository.addCurrency("USD")
        currencyRepository.addCurrency("EUR")

        val currency = currencyRepository.getCurrencyByNameSync("USD")

        assertNotNull(currency)
        assertEquals("USD", currency?.name)
    }

    @Test
    fun `getCurrencyByNameSync returns null for non-existent currency`() = runTest {
        val currency = currencyRepository.getCurrencyByNameSync("XYZ")
        assertNull(currency)
    }
}

// ========================================
// Fake Repository Implementations
// ========================================

class FakeProfileRepositoryForTransaction : ProfileRepository {
    private val profiles = mutableMapOf<Long, Profile>()
    private var nextId = 1L
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun getAllProfiles(): Flow<List<Profile>> = MutableStateFlow(profiles.values.sortedBy { it.orderNo })

    override fun getProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profiles[profileId])

    override suspend fun getProfileByIdSync(profileId: Long): Profile? = profiles[profileId]

    override fun getProfileByUuid(uuid: String): Flow<Profile?> =
        MutableStateFlow(profiles.values.find { it.uuid == uuid })

    override suspend fun getProfileByUuidSync(uuid: String): Profile? = profiles.values.find { it.uuid == uuid }

    override suspend fun getAnyProfile(): Profile? = profiles.values.firstOrNull()
    override suspend fun getProfileCount(): Int = profiles.size

    override suspend fun insertProfile(profile: Profile): Long {
        val id = if (profile.id == 0L) nextId++ else profile.id
        profile.id = id
        profiles[id] = profile
        return id
    }

    override suspend fun updateProfile(profile: Profile) {
        profiles[profile.id] = profile
    }

    override suspend fun deleteProfile(profile: Profile) {
        profiles.remove(profile.id)
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {}
    override suspend fun deleteAllProfiles() {
        profiles.clear()
        _currentProfile.value = null
    }
}

class FakeTransactionRepositoryForTransaction : TransactionRepository {
    private val transactions = mutableListOf<TransactionWithAccounts>()
    private var nextId = 1L

    override fun getAllTransactions(profileId: Long): Flow<List<TransactionWithAccounts>> =
        MutableStateFlow(transactions.filter { it.transaction.profileId == profileId })

    override fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<TransactionWithAccounts>> =
        MutableStateFlow(
            transactions.filter { it.transaction.profileId == profileId }
        )

    override fun getTransactionById(transactionId: Long): Flow<TransactionWithAccounts?> =
        MutableStateFlow(transactions.find { it.transaction.id == transactionId })

    override suspend fun getTransactionByIdSync(transactionId: Long): TransactionWithAccounts? =
        transactions.find { it.transaction.id == transactionId }

    override suspend fun searchByDescription(term: String): List<TransactionDAO.DescriptionContainer> = transactions
        .filter { it.transaction.description.contains(term, ignoreCase = true) }
        .distinctBy { it.transaction.description }
        .map { TransactionDAO.DescriptionContainer().apply { description = it.transaction.description } }

    override suspend fun getFirstByDescription(description: String): TransactionWithAccounts? = transactions
        .filter { it.transaction.description == description }
        .maxByOrNull { it.transaction.year * 10000 + it.transaction.month * 100 + it.transaction.day }

    override suspend fun getFirstByDescriptionHavingAccount(
        description: String,
        accountTerm: String
    ): TransactionWithAccounts? = transactions.find { twa ->
        twa.transaction.description == description &&
            twa.accounts.any { it.accountName.contains(accountTerm, ignoreCase = true) }
    }

    override suspend fun insertTransaction(transaction: TransactionWithAccounts) {
        if (transaction.transaction.id == 0L) {
            transaction.transaction.id = nextId++
        }
        transactions.add(transaction)
    }

    override suspend fun storeTransaction(transaction: TransactionWithAccounts) {
        insertTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.removeAll { it.transaction.id == transaction.id }
    }

    override suspend fun deleteTransactions(transactions: List<Transaction>) {
        val ids = transactions.map { it.id }.toSet()
        this.transactions.removeAll { it.transaction.id in ids }
    }

    override suspend fun storeTransactions(transactions: List<TransactionWithAccounts>, profileId: Long) {
        transactions.forEach { insertTransaction(it) }
    }

    override suspend fun deleteAllForProfile(profileId: Long): Int {
        val count = transactions.count { it.transaction.profileId == profileId }
        transactions.removeAll { it.transaction.profileId == profileId }
        return count
    }

    override suspend fun getMaxLedgerId(profileId: Long): Long? =
        transactions.filter { it.transaction.profileId == profileId }
            .maxOfOrNull { it.transaction.ledgerId }
}

class FakeAccountRepositoryForTransaction : AccountRepository {
    private val accounts = mutableMapOf<Long, MutableList<String>>()

    fun addAccount(profileId: Long, name: String) {
        accounts.getOrPut(profileId) { mutableListOf() }.add(name)
    }

    override fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<AccountWithAmounts>> =
        MutableStateFlow(emptyList())

    override suspend fun getAllWithAmountsSync(
        profileId: Long,
        includeZeroBalances: Boolean
    ): List<AccountWithAmounts> = emptyList()

    override fun getAll(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> =
        MutableStateFlow(emptyList())

    override suspend fun getByIdSync(id: Long): Account? = null
    override fun getByName(profileId: Long, accountName: String): Flow<Account?> = MutableStateFlow(null)

    override suspend fun getByNameSync(profileId: Long, accountName: String): Account? = null
    override fun getByNameWithAmounts(profileId: Long, accountName: String): Flow<AccountWithAmounts?> =
        MutableStateFlow(null)

    override fun searchAccountNames(profileId: Long, term: String): Flow<List<String>> =
        MutableStateFlow(accounts[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList())

    override suspend fun searchAccountNamesSync(profileId: Long, term: String): List<String> =
        accounts[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList()

    override suspend fun searchAccountsWithAmountsSync(profileId: Long, term: String): List<AccountWithAmounts> =
        emptyList()

    override fun searchAccountNamesGlobal(term: String): Flow<List<String>> =
        MutableStateFlow(accounts.values.flatten().filter { it.contains(term, ignoreCase = true) })

    override suspend fun searchAccountNamesGlobalSync(term: String): List<String> =
        accounts.values.flatten().filter { it.contains(term, ignoreCase = true) }

    override suspend fun insertAccount(account: Account): Long = 0L
    override suspend fun insertAccountWithAmounts(accountWithAmounts: AccountWithAmounts) {}
    override suspend fun updateAccount(account: Account) {}
    override suspend fun deleteAccount(account: Account) {}
    override suspend fun storeAccounts(accounts: List<AccountWithAmounts>, profileId: Long) {}
    override suspend fun getCountForProfile(profileId: Long): Int = accounts[profileId]?.size ?: 0
    override suspend fun deleteAllAccounts() {
        accounts.clear()
    }
}

class FakeTemplateRepositoryForTransaction : TemplateRepository {
    private val templates = mutableMapOf<Long, TemplateWithAccounts>()

    fun addTemplate(id: Long, name: String) {
        val header = TemplateHeader(id, name, "")
        val twa = TemplateWithAccounts()
        twa.header = header
        twa.accounts = emptyList()
        templates[id] = twa
    }

    override fun getAllTemplates(): Flow<List<TemplateHeader>> = MutableStateFlow(templates.values.map { it.header })

    override fun getTemplateById(id: Long): Flow<TemplateHeader?> = MutableStateFlow(templates[id]?.header)

    override suspend fun getTemplateByIdSync(id: Long): TemplateHeader? = templates[id]?.header

    override fun getTemplateWithAccounts(id: Long): Flow<TemplateWithAccounts?> = MutableStateFlow(templates[id])

    override suspend fun getTemplateWithAccountsSync(id: Long): TemplateWithAccounts? = templates[id]

    override suspend fun getTemplateWithAccountsByUuidSync(uuid: String): TemplateWithAccounts? =
        templates.values.find { it.header.uuid == uuid }

    override suspend fun getAllTemplatesWithAccountsSync(): List<TemplateWithAccounts> = templates.values.toList()

    override suspend fun insertTemplate(template: TemplateHeader): Long {
        val twa = TemplateWithAccounts()
        twa.header = template
        twa.accounts = emptyList()
        templates[template.id] = twa
        return template.id
    }

    override suspend fun insertTemplateWithAccounts(templateWithAccounts: TemplateWithAccounts) {
        templates[templateWithAccounts.header.id] = templateWithAccounts
    }

    override suspend fun updateTemplate(template: TemplateHeader) {
        templates[template.id]?.header = template
    }

    override suspend fun deleteTemplate(template: TemplateHeader) {
        templates.remove(template.id)
    }

    override suspend fun duplicateTemplate(id: Long): TemplateWithAccounts? {
        val original = templates[id] ?: return null
        val newId = (templates.keys.maxOrNull() ?: 0) + 1
        val newHeader = TemplateHeader(newId, "${original.header.name} (copy)", "")
        val copy = TemplateWithAccounts().apply {
            header = newHeader
            accounts = original.accounts
        }
        templates[copy.header.id] = copy
        return copy
    }

    override suspend fun deleteAllTemplates() {
        templates.clear()
    }
}

class FakeCurrencyRepositoryForTransaction : CurrencyRepository {
    private val currencies = mutableListOf<Currency>()
    private var nextId = 1L

    fun addCurrency(name: String) {
        currencies.add(
            Currency().apply {
                this.id = nextId++
                this.name = name
                this.position = "after"
                this.hasGap = true
            }
        )
    }

    override fun getAllCurrencies(): Flow<List<Currency>> = MutableStateFlow(currencies.toList())

    override suspend fun getAllCurrenciesSync(): List<Currency> = currencies.toList()

    override fun getCurrencyById(id: Long): Flow<Currency?> = MutableStateFlow(currencies.find { it.id == id })

    override suspend fun getCurrencyByIdSync(id: Long): Currency? = currencies.find { it.id == id }

    override fun getCurrencyByName(name: String): Flow<Currency?> =
        MutableStateFlow(currencies.find { it.name == name })

    override suspend fun getCurrencyByNameSync(name: String): Currency? = currencies.find { it.name == name }

    override suspend fun insertCurrency(currency: Currency): Long {
        currency.id = nextId++
        currencies.add(currency)
        return currency.id
    }

    override suspend fun updateCurrency(currency: Currency) {
        val index = currencies.indexOfFirst { it.id == currency.id }
        if (index >= 0) {
            currencies[index] = currency
        }
    }

    override suspend fun deleteCurrency(currency: Currency) {
        currencies.removeAll { it.name == currency.name }
    }

    override suspend fun deleteAllCurrencies() {
        currencies.clear()
    }
}
