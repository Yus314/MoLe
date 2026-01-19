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

package net.ktnx.mobileledger.ui.main

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ktnx.mobileledger.dao.TransactionDAO
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.ProfileRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.Account as DbAccount
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.db.Option
import net.ktnx.mobileledger.db.Transaction as DbTransaction
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.AccountAmount
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.BackgroundTaskManager
import net.ktnx.mobileledger.service.SyncInfo
import net.ktnx.mobileledger.service.TaskProgress
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * Fake ProfileRepository for ViewModel testing.
 *
 * This Fake implementation allows tests to control profile state and verify
 * that ViewModels correctly interact with ProfileRepository.
 */
class FakeProfileRepositoryForViewModel : ProfileRepository {
    private val profilesMap = mutableMapOf<Long, Profile>()
    private var nextId = 1L
    private val _currentProfile = MutableStateFlow<Profile?>(null)

    override val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    val profiles: List<Profile>
        get() = profilesMap.values.sortedBy { it.orderNo }

    override fun setCurrentProfile(profile: Profile?) {
        _currentProfile.value = profile
    }

    override fun observeAllProfiles(): Flow<List<Profile>> = MutableStateFlow(
        profilesMap.values.sortedBy {
            it.orderNo
        }
    )

    override suspend fun getAllProfiles(): List<Profile> = profilesMap.values.sortedBy { it.orderNo }

    override fun observeProfileById(profileId: Long): Flow<Profile?> = MutableStateFlow(profilesMap[profileId])

    override suspend fun getProfileById(profileId: Long): Profile? = profilesMap[profileId]

    override fun observeProfileByUuid(uuid: String): Flow<Profile?> =
        MutableStateFlow(profilesMap.values.find { it.uuid == uuid })

    override suspend fun getProfileByUuid(uuid: String): Profile? = profilesMap.values.find { it.uuid == uuid }

    override suspend fun getAnyProfile(): Profile? = profilesMap.values.firstOrNull()

    override suspend fun getProfileCount(): Int = profilesMap.size

    override suspend fun insertProfile(profile: Profile): Long {
        val id = if (profile.id == null || profile.id == 0L) nextId++ else profile.id
        val profileWithId = profile.copy(id = id)
        profilesMap[id] = profileWithId
        return id
    }

    override suspend fun updateProfile(profile: Profile) {
        val id = profile.id ?: return
        profilesMap[id] = profile
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profile
        }
    }

    override suspend fun deleteProfile(profile: Profile) {
        val id = profile.id ?: return
        profilesMap.remove(id)
        if (_currentProfile.value?.id == id) {
            _currentProfile.value = profilesMap.values.firstOrNull()
        }
    }

    override suspend fun updateProfileOrder(profiles: List<Profile>) {
        profiles.forEachIndexed { index, profile ->
            val id = profile.id ?: return@forEachIndexed
            profilesMap[id]?.let { existing ->
                profilesMap[id] = existing.copy(orderNo = index)
            }
        }
    }

    override suspend fun deleteAllProfiles() {
        profilesMap.clear()
        _currentProfile.value = null
    }
}

/**
 * Fake TransactionRepository for ViewModel testing.
 *
 * Now uses domain models (Transaction) for query operations.
 */
class FakeTransactionRepositoryForViewModel : TransactionRepository {
    // Internal storage uses domain models
    private val domainTransactions = mutableMapOf<Long, Transaction>()

    // Separate storage for db entities (for mutation operations)
    private val dbTransactions = mutableMapOf<Long, TransactionWithAccounts>()
    private var nextId = 1L

    // Track profile associations
    private val profileMap = mutableMapOf<Long, Long>() // transactionId -> profileId

    /**
     * Add a domain model transaction for testing.
     */
    fun addTransaction(profileId: Long, transaction: Transaction) {
        val id = transaction.id ?: nextId++
        val tx = if (transaction.id == null) transaction.copy(id = id) else transaction
        domainTransactions[id] = tx
        profileMap[id] = profileId
    }

    fun getTransactionsForProfile(profileId: Long): List<Transaction> =
        domainTransactions.values.filter { profileMap[it.id] == profileId }

    override fun getAllTransactions(profileId: Long): Flow<List<Transaction>> =
        MutableStateFlow(domainTransactions.values.filter { profileMap[it.id] == profileId }.toList())

    override fun getTransactionsFiltered(profileId: Long, accountName: String?): Flow<List<Transaction>> =
        MutableStateFlow(
            domainTransactions.values.filter { tx ->
                profileMap[tx.id] == profileId &&
                    (accountName == null || tx.lines.any { it.accountName.contains(accountName, true) })
            }.toList()
        )

    override fun getTransactionById(transactionId: Long): Flow<Transaction?> =
        MutableStateFlow(domainTransactions[transactionId])

    override suspend fun getTransactionByIdSync(transactionId: Long): Transaction? = domainTransactions[transactionId]

    override suspend fun searchByDescription(term: String): List<TransactionDAO.DescriptionContainer> =
        domainTransactions.values
            .filter { it.description.contains(term, true) }
            .distinctBy { it.description }
            .map { TransactionDAO.DescriptionContainer().apply { description = it.description } }

    override suspend fun getFirstByDescription(description: String): Transaction? =
        domainTransactions.values.find { it.description == description }

    override suspend fun getFirstByDescriptionHavingAccount(description: String, accountTerm: String): Transaction? =
        domainTransactions.values.find { tx ->
            tx.description == description &&
                tx.lines.any { it.accountName.contains(accountTerm, true) }
        }

    // Domain model mutation methods
    override suspend fun insertTransaction(transaction: Transaction, profileId: Long): Transaction {
        val id = transaction.id ?: nextId++
        val tx = transaction.copy(id = id)
        domainTransactions[id] = tx
        profileMap[id] = profileId
        return tx
    }

    override suspend fun storeTransaction(transaction: Transaction, profileId: Long) {
        insertTransaction(transaction, profileId)
    }

    // DB entity mutation methods (legacy)
    override suspend fun insertTransaction(transaction: TransactionWithAccounts) {
        if (transaction.transaction.id == 0L) {
            transaction.transaction.id = nextId++
        }
        dbTransactions[transaction.transaction.id] = transaction
        // Also add to domain transactions for query operations
        val domainTx = Transaction(
            id = transaction.transaction.id,
            ledgerId = transaction.transaction.ledgerId,
            date = SimpleDate(
                transaction.transaction.year,
                transaction.transaction.month,
                transaction.transaction.day
            ),
            description = transaction.transaction.description,
            comment = transaction.transaction.comment,
            lines = transaction.accounts.map { acc ->
                TransactionLine(
                    id = acc.id,
                    accountName = acc.accountName,
                    amount = acc.amount,
                    currency = acc.currency,
                    comment = acc.comment
                )
            }
        )
        domainTransactions[domainTx.id!!] = domainTx
        profileMap[domainTx.id!!] = transaction.transaction.profileId
    }

    override suspend fun storeTransaction(transaction: TransactionWithAccounts) {
        insertTransaction(transaction)
    }

    override suspend fun deleteTransaction(transaction: DbTransaction) {
        dbTransactions.remove(transaction.id)
        domainTransactions.remove(transaction.id)
        profileMap.remove(transaction.id)
    }

    override suspend fun deleteTransactions(transactions: List<DbTransaction>) {
        transactions.forEach {
            dbTransactions.remove(it.id)
            domainTransactions.remove(it.id)
            profileMap.remove(it.id)
        }
    }

    override suspend fun storeTransactions(transactions: List<TransactionWithAccounts>, profileId: Long) {
        transactions.forEach { twa ->
            twa.transaction.profileId = profileId
            insertTransaction(twa)
        }
    }

    override suspend fun storeTransactionsAsDomain(transactions: List<Transaction>, profileId: Long) {
        transactions.forEach { tx ->
            val id = tx.id ?: nextId++
            val txWithId = if (tx.id == null) tx.copy(id = id) else tx
            domainTransactions[id] = txWithId
            profileMap[id] = profileId
        }
    }

    override suspend fun deleteAllForProfile(profileId: Long): Int {
        val toRemove = domainTransactions.values.filter { profileMap[it.id] == profileId }
        toRemove.forEach {
            domainTransactions.remove(it.id)
            dbTransactions.remove(it.id)
            profileMap.remove(it.id)
        }
        return toRemove.size
    }

    override suspend fun getMaxLedgerId(profileId: Long): Long? = domainTransactions.values
        .filter { profileMap[it.id] == profileId }
        .maxOfOrNull { it.ledgerId }
}

/**
 * Fake AccountRepository for ViewModel testing.
 *
 * Now uses domain models (Account) for query operations.
 */
class FakeAccountRepositoryForViewModel : AccountRepository {
    // Internal storage using domain models
    private val domainAccounts = mutableMapOf<Long, MutableList<Account>>()

    // Track account names for search operations
    private val accountNames = mutableMapOf<Long, MutableList<String>>()

    private var nextId = 1L

    /**
     * Add a domain model account for testing.
     */
    fun addAccount(profileId: Long, account: Account) {
        val id = account.id ?: nextId++
        val accountWithId = if (account.id == null) account.copy(id = id) else account
        domainAccounts.getOrPut(profileId) { mutableListOf() }.add(accountWithId)
        accountNames.getOrPut(profileId) { mutableListOf() }.add(account.name)
    }

    /**
     * Convenience method to add an account by name only.
     */
    fun addAccount(profileId: Long, name: String) {
        addAccount(
            profileId,
            Account(
                id = null,
                name = name,
                level = name.count { it == ':' },
                isExpanded = true,
                amounts = emptyList()
            )
        )
    }

    override fun getAllWithAmounts(profileId: Long, includeZeroBalances: Boolean): Flow<List<Account>> {
        val accounts = domainAccounts[profileId] ?: emptyList()
        val filtered = if (includeZeroBalances) {
            accounts
        } else {
            accounts.filter { it.hasAmounts && it.amounts.any { amt -> amt.amount != 0f } }
        }
        return MutableStateFlow(filtered)
    }

    override suspend fun getAllWithAmountsSync(profileId: Long, includeZeroBalances: Boolean): List<Account> {
        val accounts = domainAccounts[profileId] ?: emptyList()
        return if (includeZeroBalances) {
            accounts
        } else {
            accounts.filter { it.hasAmounts && it.amounts.any { amt -> amt.amount != 0f } }
        }
    }

    override fun getAll(profileId: Long, includeZeroBalances: Boolean): Flow<List<DbAccount>> =
        MutableStateFlow(emptyList())

    override suspend fun getByIdSync(id: Long): DbAccount? = null

    override fun getByName(profileId: Long, accountName: String): Flow<DbAccount?> = MutableStateFlow(null)

    override suspend fun getByNameSync(profileId: Long, accountName: String): DbAccount? = null

    override fun getByNameWithAmounts(profileId: Long, accountName: String): Flow<Account?> =
        MutableStateFlow(domainAccounts[profileId]?.find { it.name == accountName })

    override suspend fun getByNameWithAmountsSync(profileId: Long, accountName: String): Account? =
        domainAccounts[profileId]?.find { it.name == accountName }

    override fun searchAccountNames(profileId: Long, term: String): Flow<List<String>> = MutableStateFlow(
        accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList()
    )

    override suspend fun searchAccountNamesSync(profileId: Long, term: String): List<String> =
        accountNames[profileId]?.filter { it.contains(term, ignoreCase = true) } ?: emptyList()

    override suspend fun searchAccountsWithAmountsSync(profileId: Long, term: String): List<Account> =
        domainAccounts[profileId]?.filter { it.name.contains(term, ignoreCase = true) } ?: emptyList()

    override fun searchAccountNamesGlobal(term: String): Flow<List<String>> =
        MutableStateFlow(accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) })

    override suspend fun searchAccountNamesGlobalSync(term: String): List<String> =
        accountNames.values.flatten().filter { it.contains(term, ignoreCase = true) }

    override suspend fun insertAccount(account: DbAccount): Long = 0L

    override suspend fun insertAccountWithAmounts(accountWithAmounts: AccountWithAmounts) {}

    override suspend fun updateAccount(account: DbAccount) {}

    override suspend fun deleteAccount(account: DbAccount) {}

    override suspend fun storeAccounts(accounts: List<AccountWithAmounts>, profileId: Long) {}

    override suspend fun storeAccountsAsDomain(accounts: List<Account>, profileId: Long) {
        domainAccounts[profileId] = accounts.toMutableList()
        accountNames[profileId] = accounts.map { it.name }.toMutableList()
    }

    override suspend fun getCountForProfile(profileId: Long): Int = domainAccounts[profileId]?.size ?: 0

    override suspend fun deleteAllAccounts() {
        domainAccounts.clear()
        accountNames.clear()
    }
}

/**
 * Fake OptionRepository for ViewModel testing.
 */
class FakeOptionRepositoryForViewModel : OptionRepository {
    private val options = mutableMapOf<String, Option>()

    private fun makeKey(profileId: Long, name: String): String = "$profileId:$name"

    override fun observeOption(profileId: Long, name: String): Flow<Option?> =
        MutableStateFlow(options[makeKey(profileId, name)])

    override suspend fun getOption(profileId: Long, name: String): Option? = options[makeKey(profileId, name)]

    override suspend fun getAllOptionsForProfile(profileId: Long): List<Option> =
        options.values.filter { it.profileId == profileId }

    override suspend fun insertOption(option: Option): Long {
        options[makeKey(option.profileId, option.name)] = option
        return 1L
    }

    override suspend fun deleteOption(option: Option) {
        options.remove(makeKey(option.profileId, option.name))
    }

    override suspend fun deleteOptionsForProfile(profileId: Long) {
        options.keys.filter { it.startsWith("$profileId:") }.forEach { options.remove(it) }
    }

    override suspend fun deleteAllOptions() {
        options.clear()
    }

    override suspend fun setLastSyncTimestamp(profileId: Long, timestamp: Long) {
        insertOption(Option(profileId, Option.OPT_LAST_SCRAPE, timestamp.toString()))
    }
}

/**
 * Fake BackgroundTaskManager for ViewModel testing.
 */
class FakeBackgroundTaskManagerForViewModel : BackgroundTaskManager {
    private val runningTasks = mutableSetOf<String>()
    private val _isRunning = MutableStateFlow(false)
    private val _progress = MutableStateFlow<TaskProgress?>(null)

    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    override val progress: StateFlow<TaskProgress?> = _progress.asStateFlow()
    override val runningTaskCount: Int get() = runningTasks.size

    override fun taskStarted(taskId: String) {
        runningTasks.add(taskId)
        _isRunning.value = runningTasks.isNotEmpty()
    }

    override fun taskFinished(taskId: String) {
        runningTasks.remove(taskId)
        _isRunning.value = runningTasks.isNotEmpty()
        if (runningTasks.isEmpty()) {
            _progress.value = null
        }
    }

    override fun updateProgress(progress: TaskProgress) {
        _progress.value = progress
    }
}

/**
 * Fake AppStateService for ViewModel testing.
 */
class FakeAppStateServiceForViewModel : AppStateService {
    private val _lastSyncInfo = MutableStateFlow(SyncInfo.EMPTY)
    private val _drawerOpen = MutableStateFlow(false)
    private val _dataVersion = MutableStateFlow(0L)

    override val lastSyncInfo: StateFlow<SyncInfo> = _lastSyncInfo.asStateFlow()
    override val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()
    override val dataVersion: StateFlow<Long> = _dataVersion.asStateFlow()

    override fun updateSyncInfo(info: SyncInfo) {
        _lastSyncInfo.value = info
    }

    override fun clearSyncInfo() {
        _lastSyncInfo.value = SyncInfo.EMPTY
    }

    override fun setDrawerOpen(open: Boolean) {
        _drawerOpen.value = open
    }

    override fun toggleDrawer() {
        _drawerOpen.value = !_drawerOpen.value
    }

    override fun signalDataChanged() {
        _dataVersion.value = _dataVersion.value + 1
    }
}
