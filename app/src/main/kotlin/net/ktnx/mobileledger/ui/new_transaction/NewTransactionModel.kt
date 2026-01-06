/*
 * Copyright © 2022 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.new_transaction

import android.annotation.SuppressLint
import android.os.Build
import android.text.TextUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.db.Currency
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.InertMutableLiveData
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.LedgerTransactionAccount
import net.ktnx.mobileledger.model.MatchedTemplate
import net.ktnx.mobileledger.utils.Globals
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import net.ktnx.mobileledger.utils.SimpleDate
import java.text.ParseException
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.MatchResult

enum class ItemType {
    generalData, transactionRow
}

enum class FocusedElement {
    Account, Comment, Amount, Description, TransactionComment
}

class NewTransactionModel : ViewModel() {
    private val showCurrency = MutableLiveData(false)
    private val isSubmittable: MutableLiveData<Boolean> = InertMutableLiveData(false)
    private val showComments = MutableLiveData(true)
    private val items = MutableLiveData<List<Item>>()
    private val simulateSave: MutableLiveData<Boolean> = InertMutableLiveData(false)
    private val busyCounter = AtomicInteger(0)
    private val busyFlag: MutableLiveData<Boolean> = InertMutableLiveData(false)
    private val profileObserver = Observer<Profile?> { profile ->
        if (profile != null) {
            showCurrency.postValue(profile.showCommodityByDefault)
            showComments.postValue(profile.showCommentsByDefault)
        }
    }
    private val focusInfo = MutableLiveData<FocusInfo>()
    private var observingDataProfile = false

    fun getShowCurrency(): LiveData<Boolean> = showCurrency

    fun getItems(): LiveData<List<Item>> = items

    private fun setItems(newList: List<Item>) {
        checkTransactionSubmittable(newList)
        setItemsWithoutSubmittableChecks(newList)
    }

    private fun replaceItems(newList: List<Item>) {
        renumberItems()
        setItems(newList)
    }

    /**
     * make old items replaceable in-place. makes the new values visually blend in
     */
    private fun renumberItems() {
        renumberItems(items.value)
    }

    private fun renumberItems(list: List<Item>?) {
        if (list == null) return

        var id = 0
        for (item in list) {
            item.id = id++
        }
    }

    private fun setItemsWithoutSubmittableChecks(list: List<Item>) {
        val cnt = list.size
        for (i in 1 until cnt - 1) {
            val item = list[i].toTransactionAccount()
            if (item.isLast) {
                val replacement = TransactionAccount(item)
                replacement.isLast = false
                (list as MutableList<Item>)[i] = replacement
            }
        }
        val last = list[cnt - 1].toTransactionAccount()
        if (!last.isLast) {
            val replacement = TransactionAccount(last)
            replacement.isLast = true
            (list as MutableList<Item>)[cnt - 1] = replacement
        }

        if (BuildConfig.DEBUG) {
            dumpItemList("Before setValue()", list)
        }
        items.value = list
    }

    private fun copyList(): MutableList<Item> {
        val copy = mutableListOf<Item>()
        val oldList = items.value

        if (oldList != null) {
            for (item in oldList) {
                copy.add(Item.from(item))
            }
        }

        return copy
    }

    private fun copyListWithoutItem(position: Int): MutableList<Item> {
        val copy = mutableListOf<Item>()
        val oldList = items.value

        if (oldList != null) {
            var i = 0
            for (item in oldList) {
                if (i++ == position) continue
                copy.add(Item.from(item))
            }
        }

        return copy
    }

    private fun shallowCopyList(): MutableList<Item> {
        return ArrayList(items.value ?: emptyList())
    }

    internal fun getShowComments(): LiveData<Boolean> = showComments

    internal fun observeDataProfile(activity: LifecycleOwner) {
        if (!observingDataProfile) {
            Data.observeProfile(activity, profileObserver)
        }
        observingDataProfile = true
    }

    internal val simulateSaveFlag: Boolean
        get() = simulateSave.value ?: false

    internal fun getSimulateSave(): LiveData<Boolean> = simulateSave

    internal fun toggleSimulateSave() {
        simulateSave.value = !simulateSaveFlag
    }

    internal fun isSubmittable(): LiveData<Boolean> = isSubmittable

    internal fun reset() {
        Logger.debug("new-trans", "Resetting model")
        val list = mutableListOf<Item>()
        Item.resetIdDispenser()
        list.add(TransactionHead(""))
        val defaultCurrency = Data.getProfile()?.getDefaultCommodityOrEmpty() ?: ""
        list.add(TransactionAccount("", defaultCurrency))
        list.add(TransactionAccount("", defaultCurrency))
        noteFocusChanged(0, FocusedElement.Description)
        renumberItems()
        isSubmittable.value = false
        setItemsWithoutSubmittableChecks(list)
    }

    internal fun accountsInInitialState(): Boolean {
        val list = items.value ?: return true

        for (item in list) {
            if (item !is TransactionAccount) continue

            if (!item.isEmpty()) return false
        }

        return true
    }

    internal fun applyTemplate(matchedTemplate: MatchedTemplate, text: String) {
        var transactionDate: SimpleDate? = null
        val matchResult = matchedTemplate.matchResult
        val templateHead = matchedTemplate.templateHead

        run {
            val day = extractIntFromMatches(
                matchResult, templateHead.dateDayMatchGroup, templateHead.dateDay
            )
            val month = extractIntFromMatches(
                matchResult, templateHead.dateMonthMatchGroup, templateHead.dateMonth
            )
            val year = extractIntFromMatches(
                matchResult, templateHead.dateYearMatchGroup, templateHead.dateYear
            )

            if (year > 0 || month > 0 || day > 0) {
                val today = SimpleDate.today()
                val finalYear = if (year <= 0) today.year else year
                val finalMonth = if (month <= 0) today.month else month
                val finalDay = if (day <= 0) today.day else day

                transactionDate = SimpleDate(finalYear, finalMonth, finalDay)

                Logger.debug("pattern", "setting transaction date to $transactionDate")
            }
        }

        val present = copyList()

        val head = TransactionHead(present[0].toTransactionHead())
        if (transactionDate != null) {
            head.date = transactionDate
        }

        val transactionDescription = extractStringFromMatches(
            matchResult,
            templateHead.transactionDescriptionMatchGroup,
            templateHead.transactionDescription
        )
        if (Misc.emptyIsNull(transactionDescription) != null) {
            head.description = transactionDescription
        }

        val transactionComment = extractStringFromMatches(
            matchResult,
            templateHead.transactionCommentMatchGroup,
            templateHead.transactionComment
        )
        if (Misc.emptyIsNull(transactionComment) != null) {
            head.comment = transactionComment
        }

        val newItems = mutableListOf<Item>()

        newItems.add(head)

        for (i in 1 until present.size) {
            val row = present[i].toTransactionAccount()
            if (!row.isEmpty()) {
                newItems.add(TransactionAccount(row))
            }
        }

        DB.get().getTemplateDAO()
            .getTemplateWithAccountsAsync(templateHead.id) { entry ->
                val accountsInInitialState = accountsInInitialState()
                for (acc in entry.accounts) {
                    val accountName = extractStringFromMatches(
                        matchResult, acc.accountNameMatchGroup, acc.accountName
                    )
                    val accountComment = extractStringFromMatches(
                        matchResult, acc.accountCommentMatchGroup, acc.accountComment
                    )
                    var amount = extractFloatFromMatches(
                        matchResult, acc.amountMatchGroup, acc.amount
                    )
                    val negateAmount = acc.negateAmount
                    if (amount != null && negateAmount == true) {
                        amount = -amount
                    }

                    val accRow = TransactionAccount(accountName)
                    accRow.comment = accountComment
                    if (amount != null) {
                        accRow.setAmount(amount)
                    }
                    accRow.currency = extractCurrencyFromMatches(
                        matchResult, acc.currencyMatchGroup, acc.getCurrencyObject()
                    )

                    newItems.add(accRow)
                }

                renumberItems(newItems)
                Misc.onMainThread { replaceItems(newItems) }
            }
    }

    private fun extractCurrencyFromMatches(m: MatchResult, group: Int?, literal: Currency?): String {
        return Misc.nullIsEmpty(
            extractStringFromMatches(m, group, literal?.name ?: "")
        )
    }

    private fun extractIntFromMatches(m: MatchResult, group: Int?, literal: Int?): Int {
        if (literal != null) return literal

        if (group != null) {
            val grp = group
            if (grp > 0 && grp <= m.groupCount()) {
                try {
                    return Integer.parseInt(m.group(grp))
                } catch (e: NumberFormatException) {
                    Logger.debug("new-trans", "Error extracting matched number", e)
                }
            }
        }

        return 0
    }

    private fun extractStringFromMatches(m: MatchResult, group: Int?, literal: String?): String? {
        if (literal != null) return literal

        if (group != null) {
            val grp = group
            if (grp > 0 && grp <= m.groupCount()) {
                return m.group(grp)
            }
        }

        return null
    }

    private fun extractFloatFromMatches(m: MatchResult, group: Int?, literal: Float?): Float? {
        if (literal != null) return literal

        if (group != null) {
            val grp = group
            if (grp > 0 && grp <= m.groupCount()) {
                try {
                    return java.lang.Float.valueOf(m.group(grp))
                } catch (e: NumberFormatException) {
                    Logger.debug("new-trans", "Error extracting matched number", e)
                }
            }
        }

        return null
    }

    internal fun removeItem(pos: Int) {
        Logger.debug("new-trans", String.format(Locale.US, "Removing item at position %d", pos))
        val newList = copyListWithoutItem(pos)
        val fi = focusInfo.value
        if (fi != null && pos < fi.position) {
            noteFocusChanged(fi.position - 1, fi.element)
        }
        setItems(newList)
    }

    internal fun noteFocusChanged(position: Int, element: FocusedElement?) {
        val present = focusInfo.value
        if (present == null || present.position != position || present.element != element) {
            focusInfo.value = FocusInfo(position, element)
        }
    }

    fun getFocusInfo(): LiveData<FocusInfo> = focusInfo

    internal fun moveItem(fromIndex: Int, toIndex: Int) {
        val newList = shallowCopyList()
        val item = newList.removeAt(fromIndex)
        newList.add(toIndex, item)

        val fi = focusInfo.value
        if (fi != null && fi.position == fromIndex) {
            noteFocusChanged(toIndex, fi.element)
        }

        items.value = newList // same count, same submittable state
    }

    internal fun moveItemLast(list: MutableList<Item>, index: Int) {
        /*   0
             1   <-- index
             2
             3   <-- desired position
                 (no bottom filler)
         */
        val itemCount = list.size

        if (index < itemCount - 1) {
            list.add(list.removeAt(index))
        }
    }

    internal fun toggleCurrencyVisible() {
        val newValue = !(showCurrency.value ?: false)

        // remove currency from all items, or reset currency to the default
        // no need to clone the list, because the removal of the currency won't lead to
        // visual changes -- the currency fields will be hidden or reset to default anyway
        // still, there may be changes in the submittable state
        val list = this.items.value ?: return
        val profile = Data.getProfile() ?: return
        for (i in 1 until list.size) {
            (list[i] as TransactionAccount).currency =
                if (newValue) profile.getDefaultCommodityOrEmpty() else ""
        }
        checkTransactionSubmittable(null)
        showCurrency.value = newValue
    }

    internal fun stopObservingBusyFlag(observer: Observer<Boolean>) {
        busyFlag.removeObserver(observer)
    }

    internal fun incrementBusyCounter() {
        val newValue = busyCounter.incrementAndGet()
        if (newValue == 1) {
            busyFlag.postValue(true)
        }
    }

    internal fun decrementBusyCounter() {
        val newValue = busyCounter.decrementAndGet()
        if (newValue == 0) {
            busyFlag.postValue(false)
        }
    }

    fun getBusyFlag(): LiveData<Boolean> = busyFlag

    fun toggleShowComments() {
        showComments.value = !(showComments.value ?: true)
    }

    fun constructLedgerTransaction(): LedgerTransaction {
        val list = requireNotNull(items.value) { "Items list must be initialized before constructing a transaction" }
        val head = list[0].toTransactionHead()
        val tr = head.asLedgerTransaction()

        tr.comment = head.comment
        val emptyAmountAccounts = HashMap<String, MutableList<LedgerTransactionAccount>>()
        val emptyAmountAccountBalance = HashMap<String, Float>()
        for (i in 1 until list.size) {
            val item = list[i].toTransactionAccount()
            val currency = item.currency
            val acc = LedgerTransactionAccount(item.accountName?.trim() ?: "", currency)
            if (acc.accountName.isEmpty()) continue

            acc.comment = item.comment

            if (item.isAmountSet) {
                acc.setAmount(item.amount)
                val emptyCurrBalance = emptyAmountAccountBalance[currency]
                if (emptyCurrBalance == null) {
                    emptyAmountAccountBalance[currency] = item.amount
                } else {
                    emptyAmountAccountBalance[currency] = emptyCurrBalance + item.amount
                }
            } else {
                var emptyCurrAccounts = emptyAmountAccounts[currency]
                if (emptyCurrAccounts == null) {
                    emptyCurrAccounts = mutableListOf()
                    emptyAmountAccounts[currency] = emptyCurrAccounts
                }
                emptyCurrAccounts.add(acc)
            }

            tr.addAccount(acc)
        }

        if (emptyAmountAccounts.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                emptyAmountAccounts.forEach { (currency, accounts) ->
                    val balance = emptyAmountAccountBalance[currency]

                    if (balance != null && !Misc.isZero(balance) && accounts.size != 1) {
                        throw RuntimeException(
                            String.format(
                                Locale.US,
                                "Should not happen: approved transaction has %d accounts " +
                                        "without amounts for currency '%s'", accounts.size, currency
                            )
                        )
                    }
                    accounts.forEach { acc -> acc.setAmount(if (balance == null) 0f else -balance) }
                }
            } else {
                for (currency in emptyAmountAccounts.keys) {
                    val accounts = emptyAmountAccounts[currency] ?: continue
                    val balance = emptyAmountAccountBalance[currency]
                    if (balance != null && !Misc.isZero(balance) && accounts.size != 1) {
                        throw RuntimeException(
                            String.format(
                                Locale.US,
                                "Should not happen: approved transaction has %d accounts for " +
                                        "currency %s", accounts.size, currency
                            )
                        )
                    }
                    for (acc in accounts) {
                        acc.setAmount(if (balance == null) 0f else -balance)
                    }
                }
            }
        }

        return tr
    }

    internal fun loadTransactionIntoModel(tr: TransactionWithAccounts) {
        val newList = mutableListOf<Item>()
        Item.resetIdDispenser()

        val currentHead = items.value?.getOrNull(0)
        val head = TransactionHead(tr.transaction.description)
        head.comment = tr.transaction.comment
        if (currentHead is TransactionHead) {
            head.date = currentHead.date
        }

        newList.add(head)

        val accounts = mutableListOf<LedgerTransactionAccount>()
        for (acc in tr.accounts) {
            accounts.add(LedgerTransactionAccount(acc))
        }

        var firstNegative: TransactionAccount? = null
        var firstPositive: TransactionAccount? = null
        var singleNegativeIndex = -1
        var singlePositiveIndex = -1
        var hasCurrency = false
        for (i in accounts.indices) {
            val acc = accounts[i]
            val item = TransactionAccount(acc.accountName, Misc.nullIsEmpty(acc.currency))
            newList.add(item)

            item.accountName = acc.accountName
            item.comment = acc.comment
            if (acc.isAmountSet) {
                item.setAmount(acc.amount)
                if (acc.amount < 0) {
                    if (firstNegative == null) {
                        firstNegative = item
                        singleNegativeIndex = i + 1
                    } else {
                        singleNegativeIndex = -1
                    }
                } else {
                    if (firstPositive == null) {
                        firstPositive = item
                        singlePositiveIndex = i + 1
                    } else {
                        singlePositiveIndex = -1
                    }
                }
            } else {
                item.resetAmount()
            }

            if (item.currency.isNotEmpty()) {
                hasCurrency = true
            }
        }
        if (BuildConfig.DEBUG) {
            dumpItemList("Loaded previous transaction", newList)
        }

        if (singleNegativeIndex != -1) {
            firstNegative?.resetAmount()
            moveItemLast(newList, singleNegativeIndex)
        } else if (singlePositiveIndex != -1) {
            firstPositive?.resetAmount()
            moveItemLast(newList, singlePositiveIndex)
        }

        val foundTransactionHasCurrency = hasCurrency
        Misc.onMainThread {
            setItems(newList)
            noteFocusChanged(1, FocusedElement.Amount)
            if (foundTransactionHasCurrency) {
                showCurrency.value = true
            }
        }
    }

    /**
     * A transaction is submittable if:
     * 0) has description
     * 1) has at least two account names
     * 2) each row with amount has account name
     * 3) for each commodity:
     * 3a) amounts must balance to 0, or
     * 3b) there must be exactly one empty amount (with account)
     * 4) empty accounts with empty amounts are ignored
     * Side effects:
     * 5) a row with an empty account name or empty amount is guaranteed to exist for each
     * commodity
     * 6) at least two rows need to be present in the ledger
     *
     * @param list - the item list to check. Can be the displayed list or a list that will be
     *             displayed soon
     */
    @SuppressLint("DefaultLocale")
    internal fun checkTransactionSubmittable(list: List<Item>?) {
        var workingList = list
        var workingWithLiveList = false
        if (workingList == null) {
            workingList = copyList()
            workingWithLiveList = true
        }

        if (BuildConfig.DEBUG) {
            dumpItemList(
                String.format(
                    "Before submittable checks (%s)",
                    if (workingWithLiveList) "LIVE LIST" else "custom list"
                ), workingList
            )
        }

        var accounts = 0
        val balance = BalanceForCurrency()
        val descriptionText = workingList[0].toTransactionHead().description
        var submittable = true
        var listChanged = false
        val itemsForCurrency = ItemsForCurrency()
        val itemsWithEmptyAmountForCurrency = ItemsForCurrency()
        val itemsWithAccountAndEmptyAmountForCurrency = ItemsForCurrency()
        val itemsWithEmptyAccountForCurrency = ItemsForCurrency()
        val itemsWithAmountForCurrency = ItemsForCurrency()
        val itemsWithAccountForCurrency = ItemsForCurrency()
        val emptyRowsForCurrency = ItemsForCurrency()

        try {
            if (descriptionText == null || descriptionText.trim().isEmpty()) {
                Logger.debug("submittable", "Transaction not submittable: missing description")
                submittable = false
            }

            var hasInvalidAmount = false

            for (i in 1 until workingList.size) {
                val item = workingList[i].toTransactionAccount()

                val accName = item.accountName?.trim() ?: ""
                val currName = item.currency

                itemsForCurrency.add(currName, item)

                if (accName.isEmpty()) {
                    itemsWithEmptyAccountForCurrency.add(currName, item)

                    if (item.isAmountSet) {
                        // 2) each amount has account name
                        Logger.debug(
                            "submittable", String.format(
                                "Transaction not submittable: row %d has no account name, but" +
                                        " has" + " amount %1.2f", i + 1, item.amount
                            )
                        )
                        submittable = false
                    } else {
                        emptyRowsForCurrency.add(currName, item)
                    }
                } else {
                    accounts++
                    itemsWithAccountForCurrency.add(currName, item)
                }

                if (item.isAmountSet && item.isAmountValid) {
                    itemsWithAmountForCurrency.add(currName, item)
                    balance.add(currName, item.amount)
                } else {
                    if (!item.isAmountValid) {
                        Logger.debug(
                            "submittable",
                            String.format("Not submittable: row %d has an invalid amount", i)
                        )
                        submittable = false
                        hasInvalidAmount = true
                    }

                    itemsWithEmptyAmountForCurrency.add(currName, item)

                    if (accName.isNotEmpty()) {
                        itemsWithAccountAndEmptyAmountForCurrency.add(currName, item)
                    }
                }
            }

            // 1) has at least two account names
            if (accounts < 2) {
                when {
                    accounts == 0 -> Logger.debug(
                        "submittable",
                        "Transaction not submittable: no account names"
                    )
                    accounts == 1 -> Logger.debug(
                        "submittable",
                        "Transaction not submittable: only one account name"
                    )
                    else -> Logger.debug(
                        "submittable",
                        String.format(
                            "Transaction not submittable: only %d account names",
                            accounts
                        )
                    )
                }
                submittable = false
            }

            // 3) for each commodity:
            // 3a) amount must balance to 0, or
            // 3b) there must be exactly one empty amount (with account)
            for (balCurrency in itemsForCurrency.currencies()) {
                val currencyBalance = balance.get(balCurrency)
                if (Misc.isZero(currencyBalance)) {
                    // remove hints from all amount inputs in that currency
                    for (i in 1 until workingList.size) {
                        val acc = workingList[i].toTransactionAccount()
                        if (Misc.equalStrings(acc.currency, balCurrency)) {
                            if (BuildConfig.DEBUG) {
                                Logger.debug(
                                    "submittable",
                                    String.format(
                                        Locale.US, "Resetting hint of %d:'%s' [%s]",
                                        i, Misc.nullIsEmpty(acc.accountName), balCurrency
                                    )
                                )
                            }
                            // skip if the amount is set, in which case the hint is not
                            // important/visible
                            if (!acc.isAmountSet && acc.amountHintIsSet &&
                                !TextUtils.isEmpty(acc.amountHint)
                            ) {
                                acc.amountHint = null
                                listChanged = true
                            }
                        }
                    }
                } else {
                    val tmpList = itemsWithAccountAndEmptyAmountForCurrency.getList(balCurrency)
                    val balanceReceiversCount = tmpList.size
                    if (balanceReceiversCount != 1) {
                        if (BuildConfig.DEBUG) {
                            if (balanceReceiversCount == 0) {
                                Logger.debug(
                                    "submittable", String.format(
                                        "Transaction not submittable [curr:%s]: non-zero balance " +
                                                "with no empty amounts with accounts", balCurrency
                                    )
                                )
                            } else {
                                Logger.debug(
                                    "submittable", String.format(
                                        "Transaction not submittable [curr:%s]: non-zero balance " +
                                                "with multiple empty amounts with accounts",
                                        balCurrency
                                    )
                                )
                            }
                        }
                        submittable = false
                    }

                    val emptyAmountList = itemsWithEmptyAmountForCurrency.getList(balCurrency)

                    // suggest off-balance amount to a row and remove hints on other rows
                    var receiver: Item? = null
                    if (tmpList.isNotEmpty()) {
                        receiver = tmpList[0]
                    } else if (emptyAmountList.isNotEmpty()) {
                        receiver = emptyAmountList[0]
                    }

                    for (i in workingList.indices) {
                        val item = workingList[i]
                        if (item !is TransactionAccount) continue

                        val acc = item.toTransactionAccount()
                        if (!Misc.equalStrings(acc.currency, balCurrency)) continue

                        if (item === receiver) {
                            val hint = Data.formatNumber(-currencyBalance)
                            if (!acc.isAmountHintSet || !Misc.equalStrings(acc.amountHint, hint)) {
                                Logger.debug(
                                    "submittable",
                                    String.format(
                                        "Setting amount hint of {%s} to %s [%s]", acc,
                                        hint, balCurrency
                                    )
                                )
                                acc.amountHint = hint
                                listChanged = true
                            }
                        } else {
                            if (BuildConfig.DEBUG) {
                                Logger.debug(
                                    "submittable",
                                    String.format(
                                        "Resetting hint of '%s' [%s]",
                                        Misc.nullIsEmpty(acc.accountName), balCurrency
                                    )
                                )
                            }
                            if (acc.amountHintIsSet && !TextUtils.isEmpty(acc.amountHint)) {
                                acc.amountHint = null
                                listChanged = true
                            }
                        }
                    }
                }
            }

            // 5) a row with an empty account name or empty amount is guaranteed to exist for
            // each commodity
            if (!hasInvalidAmount) {
                for (balCurrency in balance.currencies()) {
                    val currEmptyRows = itemsWithEmptyAccountForCurrency.size(balCurrency)
                    val currRows = itemsForCurrency.size(balCurrency)
                    val currAccounts = itemsWithAccountForCurrency.size(balCurrency)
                    val currAmounts = itemsWithAmountForCurrency.size(balCurrency)
                    if (currEmptyRows == 0 && (currRows == currAccounts || currRows == currAmounts)) {
                        val newAcc = TransactionAccount("", balCurrency)
                        val bal = balance.get(balCurrency)
                        if (!Misc.isZero(bal) && currAmounts == currRows) {
                            newAcc.amountHint = Data.formatNumber(-bal)
                        }
                        Logger.debug(
                            "submittable",
                            String.format(
                                "Adding new item with %s for currency %s",
                                newAcc.amountHint, balCurrency
                            )
                        )
                        (workingList as MutableList<Item>).add(newAcc)
                        listChanged = true
                    }
                }
            }

            // drop extra empty rows, not needed
            for (currName in emptyRowsForCurrency.currencies()) {
                val emptyItems = emptyRowsForCurrency.getList(currName).toMutableList()
                while (workingList.size > MIN_ITEMS && emptyItems.size > 1) {
                    // the list is a copy, so the empty item is no longer present
                    val itemToRemove = emptyItems.removeAt(1)
                    removeItemById(workingList as MutableList<Item>, itemToRemove.id)
                    listChanged = true
                }

                // unused currency, remove last item (which is also an empty one)
                if (workingList.size > MIN_ITEMS && emptyItems.size == 1) {
                    val currItems = itemsForCurrency.getList(currName)

                    if (currItems.size == 1) {
                        // the list is a copy, so the empty item is no longer present
                        removeItemById(workingList as MutableList<Item>, emptyItems[0].id)
                        listChanged = true
                    }
                }
            }

            // 6) at least two rows need to be present in the ledger
            //    (the list also contains header and trailer)
            while (workingList.size < MIN_ITEMS) {
                (workingList as MutableList<Item>).add(TransactionAccount(""))
                listChanged = true
            }

            Logger.debug("submittable", if (submittable) "YES" else "NO")
            isSubmittable.value = submittable

            if (BuildConfig.DEBUG) {
                dumpItemList("After submittable checks", workingList)
            }
        } catch (e: NumberFormatException) {
            Logger.debug("submittable", "NO (because of NumberFormatException)")
            isSubmittable.value = false
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.debug("submittable", "NO (because of an Exception)")
            isSubmittable.value = false
        }

        if (listChanged && workingWithLiveList) {
            setItemsWithoutSubmittableChecks(workingList)
        }
    }

    private fun removeItemById(list: MutableList<Item>, id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            list.removeIf { item -> item.id == id }
        } else {
            val iterator = list.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.id == id) {
                    iterator.remove()
                    break
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun dumpItemList(msg: String, list: List<Item>) {
        Logger.debug("submittable", "== Dump of all items $msg")
        for (i in 1 until list.size) {
            val item = list[i].toTransactionAccount()
            Logger.debug("submittable", String.format("%d:%s", i, item.toString()))
        }
    }

    fun setItemCurrency(position: Int, newCurrency: String) {
        val item = (items.value?.getOrNull(position) ?: return).toTransactionAccount()
        val oldCurrency = item.currency

        if (Misc.equalStrings(oldCurrency, newCurrency)) return

        val newList = copyList()
        newList[position].toTransactionAccount().currency = newCurrency

        setItems(newList)
    }

    fun accountListIsEmpty(): Boolean {
        val items = this.items.value ?: return true

        for (item in items) {
            if (item !is TransactionAccount) continue

            if (!item.isEmpty()) return false
        }

        return true
    }

    class FocusInfo(
        @JvmField val position: Int,
        @JvmField val element: FocusedElement?
    )

    abstract class Item {
        var id: Int = 0
            internal set

        protected constructor() {
            if (this is TransactionHead) {
                id = 0
            } else {
                synchronized(Item::class.java) {
                    id = ++idDispenser
                }
            }
        }

        constructor(id: Int) {
            this.id = id
        }

        abstract val type: ItemType

        fun toTransactionHead(): TransactionHead {
            if (this is TransactionHead) return this
            throw IllegalStateException("Wrong item type $this")
        }

        fun toTransactionAccount(): TransactionAccount {
            if (this is TransactionAccount) return this
            throw IllegalStateException("Wrong item type $this")
        }

        open fun equalContents(item: Any?): Boolean {
            if (item == null) return false
            if (javaClass != item.javaClass) return false

            // shortcut - comparing same instance
            if (item === this) return true

            if (this is TransactionHead) {
                return (item as TransactionHead).equalContents(this)
            }
            if (this is TransactionAccount) {
                return (item as TransactionAccount).equalContents(this)
            }

            throw RuntimeException("Don't know how to handle $this")
        }

        companion object {
            @JvmStatic
            private var idDispenser = 0

            @JvmStatic
            fun from(origin: Item): Item {
                return when (origin) {
                    is TransactionHead -> TransactionHead(origin)
                    is TransactionAccount -> TransactionAccount(origin)
                    else -> throw RuntimeException("Don't know how to handle $origin")
                }
            }

            @JvmStatic
            internal fun resetIdDispenser() {
                idDispenser = 0
            }
        }
    }

    //==========================================================================================

    class TransactionHead : Item {
        var date: SimpleDate? = null
        var description: String? = null
        var comment: String? = null

        internal constructor(description: String?) : super() {
            this.description = description
        }

        constructor(origin: TransactionHead) : super(origin.id) {
            date = origin.date
            description = origin.description
            comment = origin.comment
        }

        @Throws(ParseException::class)
        fun setDate(text: String?) {
            val trimmedText = Misc.emptyIsNull(text)
            if (trimmedText == null) {
                date = null
                return
            }

            date = Globals.parseLedgerDate(trimmedText)
        }

        /**
         * getFormattedDate()
         *
         * @return nicely formatted, shortest available date representation
         */
        internal fun getFormattedDate(): String? {
            val d = date ?: return null

            val today = GregorianCalendar.getInstance()

            if (today.get(Calendar.YEAR) != d.year) {
                return String.format(Locale.US, "%d/%02d/%02d", d.year, d.month, d.day)
            }

            if (today.get(Calendar.MONTH) + 1 != d.month) {
                return String.format(Locale.US, "%d/%02d", d.month, d.day)
            }

            return d.day.toString()
        }

        override fun toString(): String {
            @SuppressLint("DefaultLocale")
            val b = StringBuilder(
                String.format("id:%d/%s", id, Integer.toHexString(hashCode()))
            )

            if (TextUtils.isEmpty(description)) {
                b.append(" <<no description>>")
            } else {
                b.append(String.format(" '%s'", description))
            }

            if (date != null) {
                b.append(String.format("@%s", date))
            }

            if (!TextUtils.isEmpty(comment)) {
                b.append(String.format(" /%s/", comment))
            }

            return b.toString()
        }

        override val type: ItemType
            get() = ItemType.generalData

        fun asLedgerTransaction(): LedgerTransaction {
            return LedgerTransaction(
                0,
                date ?: SimpleDate.today(),
                description,
                requireNotNull(Data.getProfile()) { "Profile must be set before creating a transaction" }
            )
        }

        fun equalContents(other: TransactionHead?): Boolean {
            if (other == null) return false

            return date == other.date &&
                    Misc.equalStrings(description, other.description) &&
                    Misc.equalStrings(comment, other.comment)
        }
    }

    class TransactionAccount : Item {
        var accountName: String? = null
        var amountHint: String? = null
            set(value) {
                field = value
                amountHintIsSet = !TextUtils.isEmpty(value)
            }
        var comment: String? = null
        var currency: String = ""
        private var _amount: Float = 0f
        var isAmountSet: Boolean = false
            private set
        var isAmountValid: Boolean = true
        var amountText: String = ""
        var focusedElement: FocusedElement = FocusedElement.Account
        var amountHintIsSet: Boolean = false
            private set
        var isLast: Boolean = false
            internal set
        var accountNameCursorPosition: Int = 0

        constructor(origin: TransactionAccount) : super(origin.id) {
            accountName = origin.accountName
            _amount = origin._amount
            isAmountSet = origin.isAmountSet
            amountHint = origin.amountHint
            amountHintIsSet = origin.amountHintIsSet
            amountText = origin.amountText
            comment = origin.comment
            currency = origin.currency
            isAmountValid = origin.isAmountValid
            focusedElement = origin.focusedElement
            isLast = origin.isLast
            accountNameCursorPosition = origin.accountNameCursorPosition
        }

        constructor(accountName: String?) : super() {
            this.accountName = accountName
        }

        constructor(accountName: String?, currency: String) : super() {
            this.accountName = accountName
            this.currency = currency
        }

        fun setAndCheckAmountText(amountText: String): Boolean {
            val amtText = amountText.trim()
            this.amountText = amtText

            var significantChange = false

            if (amtText.isEmpty()) {
                if (isAmountSet) {
                    significantChange = true
                }
                resetAmount()
            } else {
                try {
                    val processedAmtText = amtText.replace(Data.getDecimalSeparator(), Data.decimalDot)
                    val parsedAmount = java.lang.Float.parseFloat(processedAmtText)
                    if (!isAmountSet || !isAmountValid || !Misc.equalFloats(parsedAmount, _amount)) {
                        significantChange = true
                    }
                    _amount = parsedAmount
                    isAmountSet = true
                    isAmountValid = true
                } catch (e: NumberFormatException) {
                    Logger.debug(
                        "new-trans", String.format(
                            "assuming amount is not set due to number format exception. " +
                                    "input was '%s'", amtText
                        )
                    )
                    if (isAmountValid) { // it was valid and now it's not
                        significantChange = true
                    }
                    isAmountValid = false
                }
            }

            return significantChange
        }

        val amount: Float
            get() {
                if (!isAmountSet) {
                    throw IllegalStateException("Amount is not set")
                }
                return _amount
            }

        fun setAmount(amount: Float) {
            this._amount = amount
            isAmountSet = true
            isAmountValid = true
            amountText = Data.formatNumber(amount)
        }

        fun resetAmount() {
            isAmountSet = false
            isAmountValid = true
            amountText = ""
        }

        override val type: ItemType
            get() = ItemType.transactionRow

        val isAmountHintSet: Boolean
            get() = amountHintIsSet

        fun isEmpty(): Boolean {
            return !isAmountSet && Misc.emptyIsNull(accountName) == null &&
                    Misc.emptyIsNull(comment) == null
        }

        @SuppressLint("DefaultLocale")
        override fun toString(): String {
            val b = StringBuilder()
            b.append(String.format("id:%d/%s", id, Integer.toHexString(hashCode())))
            if (!TextUtils.isEmpty(accountName)) {
                b.append(String.format(" acc'%s'", accountName))
            }

            if (isAmountSet) {
                b.append(amountText)
                    .append(" [")
                    .append(if (isAmountValid) "valid" else "invalid")
                    .append("] ")
                    .append(String.format(Locale.ROOT, " {raw %4.2f}", _amount))
            } else if (amountHintIsSet) {
                b.append(String.format(" (hint %s)", amountHint))
            }

            if (!TextUtils.isEmpty(currency)) {
                b.append(" ")
                    .append(currency)
            }

            if (!TextUtils.isEmpty(comment)) {
                b.append(String.format(" /%s/", comment))
            }

            if (isLast) {
                b.append(" last")
            }

            return b.toString()
        }

        fun equalContents(other: TransactionAccount?): Boolean {
            if (other == null) return false

            var equal = Misc.equalStrings(accountName, other.accountName)
            equal = equal && Misc.equalStrings(comment, other.comment) &&
                    (if (isAmountSet) other.isAmountSet && isAmountValid == other.isAmountValid &&
                            Misc.equalStrings(amountText, other.amountText)
                    else !other.isAmountSet)

            // compare amount hint only if there is no amount
            if (!isAmountSet) {
                equal = equal && (if (amountHintIsSet) other.amountHintIsSet &&
                        Misc.equalStrings(amountHint, other.amountHint)
                else !other.amountHintIsSet)
            }
            equal = equal && Misc.equalStrings(currency, other.currency) && isLast == other.isLast

            Logger.debug(
                "new-trans",
                String.format("Comparing {%s} and {%s}: %s", this, other, equal)
            )
            return equal
        }
    }

    private class BalanceForCurrency {
        private val hashMap = HashMap<String, Float>()

        fun get(currencyName: String): Float {
            var f = hashMap[currencyName]
            if (f == null) {
                f = 0f
                hashMap[currencyName] = f
            }
            return f
        }

        fun add(currencyName: String, amount: Float) {
            hashMap[currencyName] = get(currencyName) + amount
        }

        fun currencies(): Set<String> = hashMap.keys

        fun containsCurrency(currencyName: String): Boolean = hashMap.containsKey(currencyName)
    }

    private class ItemsForCurrency {
        private val hashMap = HashMap<String, MutableList<Item>>()

        fun getList(currencyName: String): MutableList<Item> {
            var list = hashMap[currencyName]
            if (list == null) {
                list = mutableListOf()
                hashMap[currencyName] = list
            }
            return list
        }

        fun add(currencyName: String, item: Item) {
            getList(currencyName).add(item)
        }

        fun size(currencyName: String): Int = getList(currencyName).size

        fun currencies(): Set<String> = hashMap.keys
    }

    companion object {
        private const val MIN_ITEMS = 3
    }
}
