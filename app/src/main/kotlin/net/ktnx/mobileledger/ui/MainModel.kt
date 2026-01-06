/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.ktnx.mobileledger.async.RetrieveTransactionsTask
import net.ktnx.mobileledger.async.TransactionAccumulator
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.TransactionListItem
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.SimpleDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainModel @Inject constructor(
    private val data: Data
) : ViewModel() {
    @JvmField
    val foundTransactionItemIndex = MutableLiveData<Int?>(null)

    private val _updatingFlag = MutableLiveData(false)
    private val _showZeroBalanceAccounts = MutableLiveData(true)
    private val _accountFilter = MutableLiveData<String?>(null)
    private val _displayedTransactions = MutableLiveData<List<TransactionListItem>>(ArrayList())
    private val _updateError = MutableLiveData<String?>()

    var firstTransactionDate: SimpleDate? = null
    var lastTransactionDate: SimpleDate? = null

    @Transient
    private var retrieveTransactionsTask: RetrieveTransactionsTask? = null
    private var displayedTransactionsUpdater: TransactionsDisplayedFilter? = null

    fun getUpdatingFlag(): LiveData<Boolean> = _updatingFlag

    fun getUpdateError(): LiveData<String?> = _updateError

    fun getDisplayedTransactions(): LiveData<List<TransactionListItem>> = _displayedTransactions

    fun setDisplayedTransactions(list: List<TransactionListItem>, transactionCount: Int) {
        _displayedTransactions.postValue(list)
        data.lastUpdateTransactionCount.postValue(transactionCount)
    }

    fun getShowZeroBalanceAccounts(): MutableLiveData<Boolean> = _showZeroBalanceAccounts

    fun getAccountFilter(): MutableLiveData<String?> = _accountFilter

    @Synchronized
    fun scheduleTransactionListRetrieval() {
        if (retrieveTransactionsTask != null) {
            Logger.debug("db", "Ignoring request for transaction retrieval - already active")
            return
        }
        val profile = data.getProfile()
        checkNotNull(profile)

        retrieveTransactionsTask = RetrieveTransactionsTask(profile)
        Logger.debug("db", "Created a background transaction retrieval task")

        retrieveTransactionsTask?.start()
    }

    @Synchronized
    fun stopTransactionsRetrieval() {
        if (retrieveTransactionsTask != null) {
            retrieveTransactionsTask?.interrupt()
        } else {
            data.backgroundTaskProgress.value = null
        }
    }

    fun transactionRetrievalDone() {
        retrieveTransactionsTask = null
    }

    @Synchronized
    fun updateDisplayedTransactionsFromWeb(list: List<LedgerTransaction>) {
        displayedTransactionsUpdater?.interrupt()
        displayedTransactionsUpdater = TransactionsDisplayedFilter(this, list)
        displayedTransactionsUpdater?.start()
    }

    fun clearUpdateError() {
        _updateError.postValue(null)
    }

    fun clearTransactions() {
        _displayedTransactions.value = ArrayList()
    }

    internal class TransactionsDisplayedFilter(
        private val model: MainModel,
        private val list: List<LedgerTransaction>
    ) : Thread() {

        override fun run() {
            Logger.debug(
                "dFilter",
                String.format(
                    Locale.US,
                    "entered synchronized block (about to examine %d transactions)",
                    list.size
                )
            )
            val accNameFilter = model.getAccountFilter().value

            val acc = TransactionAccumulator(accNameFilter, accNameFilter)
            for (tr in list) {
                if (isInterrupted) {
                    return
                }

                if (accNameFilter == null || tr.hasAccountNamedLike(accNameFilter)) {
                    tr.date?.let { date -> acc.put(tr, date) }
                }
            }

            if (isInterrupted) return

            acc.publishResults(model)
            Logger.debug("dFilter", "transaction list updated")
        }
    }
}
