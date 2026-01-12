/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.async

import android.annotation.SuppressLint
import android.os.OperationCanceledException
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.RuntimeJsonMappingException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlinx.coroutines.runBlocking
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.db.Option
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.err.HTTPException
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.json.AccountListParser
import net.ktnx.mobileledger.json.ApiNotSupportedException
import net.ktnx.mobileledger.json.TransactionListParser
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.LedgerTransactionAccount
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.NetworkUtil
import net.ktnx.mobileledger.utils.SimpleDate

class RetrieveTransactionsTask(
    private val profile: Profile,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val optionRepository: OptionRepository
) : Thread() {
    private var expectedPostingsCount = -1

    private fun publishProgress(progress: Progress) {
        Data.backgroundTaskProgress.postValue(progress)
    }

    private fun finish(result: Result) {
        val progress = Progress()
        progress.state = ProgressState.FINISHED
        progress.error = result.error
        publishProgress(progress)
    }

    private fun cancel() {
        val progress = Progress()
        progress.state = ProgressState.FINISHED
        publishProgress(progress)
    }

    @Throws(IOException::class, HTTPException::class)
    private fun retrieveTransactionListLegacy(
        accounts: MutableList<LedgerAccount>,
        transactions: MutableList<LedgerTransaction>
    ) {
        val progress = Progress.indeterminate()
        progress.state = ProgressState.RUNNING
        progress.setTotal(expectedPostingsCount)
        var maxTransactionId = -1
        val map = HashMap<String, LedgerAccount>()
        var lastAccount: LedgerAccount? = null
        val syntheticAccounts = ArrayList<LedgerAccount>()

        val http = NetworkUtil.prepareConnection(profile, "journal")
        http.allowUserInteraction = false
        publishProgress(progress)
        if (http.responseCode != 200) {
            throw HTTPException(http.responseCode, http.responseMessage)
        }

        http.inputStream.use { resp ->
            if (http.responseCode != 200) {
                throw IOException(String.format("HTTP error %d", http.responseCode))
            }

            var matchedTransactionsCount = 0

            var state = ParserState.EXPECTING_ACCOUNT
            val buf = BufferedReader(InputStreamReader(resp, StandardCharsets.UTF_8))

            var processedTransactionCount = 0
            var transactionId = 0
            var transaction: LedgerTransaction? = null

            lines@ while (true) {
                val line = buf.readLine() ?: break
                throwIfCancelled()

                val commentMatcher = reComment.matcher(line)
                if (commentMatcher.find()) {
                    continue
                }

                when (state) {
                    ParserState.EXPECTING_ACCOUNT -> {
                        if (line == "<h2>General Journal</h2>") {
                            state = ParserState.EXPECTING_TRANSACTION
                            L("→ expecting transaction")
                            continue
                        }
                        val m = reAccountName.matcher(line)
                        if (m.find()) {
                            val acctEncoded = requireNotNull(m.group(1)) { "Account name match group is null" }
                            var accName = URLDecoder.decode(acctEncoded, "UTF-8")
                            accName = accName.replace("\"", "")
                            L(String.format("found account: %s", accName))

                            lastAccount = map[accName]
                            if (lastAccount != null) {
                                L(String.format("ignoring duplicate account '%s'", accName))
                                continue
                            }
                            val parentAccountName = LedgerAccount.extractParentName(accName)
                            val parentAccount = if (parentAccountName != null) {
                                ensureAccountExists(parentAccountName, map, syntheticAccounts)
                            } else {
                                null
                            }
                            val newAccount = LedgerAccount(accName, parentAccount)
                            lastAccount = newAccount

                            accounts.add(newAccount)
                            map[accName] = newAccount

                            state = ParserState.EXPECTING_ACCOUNT_AMOUNT
                            L("→ expecting account amount")
                        }
                    }

                    ParserState.EXPECTING_ACCOUNT_AMOUNT -> {
                        val m = reAccountValue.matcher(line)
                        var matchFound = false
                        while (m.find()) {
                            throwIfCancelled()

                            matchFound = true
                            var value = requireNotNull(m.group(1)) { "Value match group is null" }
                            var currency = m.group(2) ?: ""

                            val tmpM = reDecimalComma.matcher(value)
                            if (tmpM.find()) {
                                value = value.replace(".", "")
                                value = value.replace(',', '.')
                            }

                            val tmpM2 = reDecimalPoint.matcher(value)
                            if (tmpM2.find()) {
                                value = value.replace(",", "")
                                value = value.replace(" ", "")
                            }

                            L("curr=$currency, value=$value")
                            val floatVal = value.toFloat()
                            val currentAccount = requireNotNull(lastAccount) { "No current account" }
                            currentAccount.addAmount(floatVal, currency)
                            for (syn in syntheticAccounts) {
                                L(
                                    String.format(
                                        Locale.ENGLISH,
                                        "propagating %s %1.2f to %s",
                                        currency,
                                        floatVal,
                                        syn.name
                                    )
                                )
                                syn.addAmount(floatVal, currency)
                            }
                        }

                        if (matchFound) {
                            syntheticAccounts.clear()
                            state = ParserState.EXPECTING_ACCOUNT
                            L("→ expecting account")
                        }
                    }

                    ParserState.EXPECTING_TRANSACTION -> {
                        if (line.isNotEmpty() && line[0] == ' ') continue
                        val m = reTransactionStart.matcher(line)
                        if (m.find()) {
                            transactionId = requireNotNull(m.group(1)) { "Transaction ID match group is null" }.toInt()
                            state = ParserState.EXPECTING_TRANSACTION_DESCRIPTION
                            L(
                                String.format(
                                    Locale.ENGLISH,
                                    "found transaction %d → expecting description",
                                    transactionId
                                )
                            )
                            progress.setProgress(++processedTransactionCount)
                            if (maxTransactionId < transactionId) {
                                maxTransactionId = transactionId
                            }
                            if (progress.isIndeterminate || progress.getTotal() < transactionId) {
                                progress.setTotal(transactionId)
                            }
                            publishProgress(progress)
                        }
                        val mEnd = reEnd.matcher(line)
                        if (mEnd.find()) {
                            L("--- transaction value complete ---")
                            break@lines
                        }
                    }

                    ParserState.EXPECTING_TRANSACTION_DESCRIPTION -> {
                        if (line.isNotEmpty() && line[0] == ' ') continue
                        val m = reTransactionDescription.matcher(line)
                        if (m.find()) {
                            if (transactionId == 0) {
                                throw TransactionParserException(
                                    "Transaction Id is 0 while expecting description"
                                )
                            }

                            var date = requireNotNull(m.group(1)) { "Date match group is null" }
                            try {
                                val equalsIndex = date.indexOf('=')
                                if (equalsIndex >= 0) {
                                    date = date.substring(equalsIndex + 1)
                                }
                                transaction = LedgerTransaction(transactionId.toLong(), date, m.group(2))
                            } catch (e: ParseException) {
                                throw TransactionParserException(
                                    String.format("Error parsing date '%s'", date)
                                )
                            }
                            state = ParserState.EXPECTING_TRANSACTION_DETAILS
                            L(
                                String.format(
                                    Locale.ENGLISH,
                                    "transaction %d created for %s (%s) → expecting details",
                                    transactionId,
                                    date,
                                    m.group(2)
                                )
                            )
                        }
                    }

                    ParserState.EXPECTING_TRANSACTION_DETAILS -> {
                        val currentTransaction = requireNotNull(transaction) { "Transaction is null" }
                        if (line.isEmpty()) {
                            // transaction data collected
                            currentTransaction.finishLoading()
                            transactions.add(transaction)

                            state = ParserState.EXPECTING_TRANSACTION
                            L(
                                String.format(
                                    "transaction %s parsed → expecting transaction",
                                    currentTransaction.ledgerId
                                )
                            )
                        } else {
                            val lta = parseTransactionAccountLine(line)
                            if (lta != null) {
                                currentTransaction.addAccount(lta)
                                L(
                                    String.format(
                                        Locale.ENGLISH,
                                        "%d: %s = %s",
                                        currentTransaction.ledgerId,
                                        lta.accountName,
                                        lta.amount
                                    )
                                )
                            } else {
                                throw IllegalStateException(
                                    String.format(
                                        "Can't parse transaction %d details: %s",
                                        transactionId,
                                        line
                                    )
                                )
                            }
                        }
                    }
                }
            }

            throwIfCancelled()
        }
    }

    fun ensureAccountExists(
        accountName: String,
        map: HashMap<String, LedgerAccount>,
        createdAccounts: ArrayList<LedgerAccount>
    ): LedgerAccount {
        map[accountName]?.let { return it }

        val parentName = LedgerAccount.extractParentName(accountName)
        val parentAccount = if (parentName != null) {
            ensureAccountExists(parentName, map, createdAccounts)
        } else {
            null
        }

        val acc = LedgerAccount(accountName, parentAccount)
        createdAccounts.add(acc)
        return acc
    }

    fun addNumberOfPostings(number: Int) {
        expectedPostingsCount += number
    }

    @Throws(IOException::class, HTTPException::class, ApiNotSupportedException::class)
    private fun retrieveAccountList(): List<LedgerAccount>? {
        val apiVersion = API.valueOf(profile.apiVersion)
        return when {
            apiVersion == API.auto -> retrieveAccountListAnyVersion()

            apiVersion == API.html -> {
                Logger.debug(
                    "json",
                    "Declining using JSON API for /accounts with configured legacy API version"
                )
                null
            }

            else -> retrieveAccountListForVersion(apiVersion)
        }
    }

    @Throws(ApiNotSupportedException::class, IOException::class, HTTPException::class)
    private fun retrieveAccountListAnyVersion(): List<LedgerAccount>? {
        for (ver in API.allVersions) {
            try {
                return retrieveAccountListForVersion(ver)
            } catch (e: JsonParseException) {
                Logger.debug(
                    "json",
                    String.format(
                        Locale.US,
                        "Error during account list retrieval using API %s",
                        ver.description
                    ),
                    e
                )
            } catch (e: RuntimeJsonMappingException) {
                Logger.debug(
                    "json",
                    String.format(
                        Locale.US,
                        "Error during account list retrieval using API %s",
                        ver.description
                    ),
                    e
                )
            }
        }
        throw ApiNotSupportedException()
    }

    @Throws(IOException::class, HTTPException::class)
    private fun retrieveAccountListForVersion(version: API): List<LedgerAccount>? {
        val http = NetworkUtil.prepareConnection(profile, "accounts")
        http.allowUserInteraction = false
        when (http.responseCode) {
            200 -> { /* continue */ }
            404 -> return null
            else -> throw HTTPException(http.responseCode, http.responseMessage)
        }
        publishProgress(Progress.indeterminate())
        val list = ArrayList<LedgerAccount>()
        val map = HashMap<String, LedgerAccount>()
        throwIfCancelled()
        http.inputStream.use { resp ->
            throwIfCancelled()
            if (http.responseCode != 200) {
                throw IOException(String.format("HTTP error %d", http.responseCode))
            }

            val parser = AccountListParser.forApiVersion(version, resp)
            expectedPostingsCount = 0

            while (true) {
                throwIfCancelled()
                val acc = parser.nextAccount(this, map) ?: break
                list.add(acc)
            }
            throwIfCancelled()

            Logger.warn(
                "accounts",
                String.format(
                    Locale.US,
                    "Got %d accounts using protocol %s",
                    list.size,
                    version.description
                )
            )
        }

        return list
    }

    @Throws(ParseException::class, HTTPException::class, IOException::class, ApiNotSupportedException::class)
    private fun retrieveTransactionList(): List<LedgerTransaction>? {
        val apiVersion = API.valueOf(profile.apiVersion)
        return when {
            apiVersion == API.auto -> retrieveTransactionListAnyVersion()

            apiVersion == API.html -> {
                Logger.debug(
                    "json",
                    "Declining using JSON API for /accounts with configured legacy API version"
                )
                null
            }

            else -> retrieveTransactionListForVersion(apiVersion)
        }
    }

    @Throws(ApiNotSupportedException::class)
    private fun retrieveTransactionListAnyVersion(): List<LedgerTransaction>? {
        for (ver in API.allVersions) {
            try {
                return retrieveTransactionListForVersion(ver)
            } catch (e: Exception) {
                Logger.debug(
                    "json",
                    String.format(
                        Locale.US,
                        "Error during transaction list retrieval using API %s",
                        ver.description
                    ),
                    e
                )
            }
        }
        throw ApiNotSupportedException()
    }

    @Throws(IOException::class, ParseException::class, HTTPException::class)
    private fun retrieveTransactionListForVersion(apiVersion: API): List<LedgerTransaction>? {
        val progress = Progress()
        progress.setTotal(expectedPostingsCount)

        val http = NetworkUtil.prepareConnection(profile, "transactions")
        http.allowUserInteraction = false
        publishProgress(progress)
        when (http.responseCode) {
            200 -> { /* continue */ }
            404 -> return null
            else -> throw HTTPException(http.responseCode, http.responseMessage)
        }
        val trList = ArrayList<LedgerTransaction>()
        http.inputStream.use { resp ->
            throwIfCancelled()

            val parser = TransactionListParser.forApiVersion(apiVersion, resp)

            var processedPostings = 0

            while (true) {
                throwIfCancelled()
                val transaction = parser.nextTransaction()
                throwIfCancelled()
                if (transaction == null) break

                trList.add(transaction)

                processedPostings += transaction.accounts.size
                progress.setProgress(processedPostings)
                publishProgress(progress)
            }

            throwIfCancelled()

            Logger.warn(
                "transactions",
                String.format(
                    Locale.US,
                    "Got %d transactions using protocol %s",
                    trList.size,
                    apiVersion.description
                )
            )
        }

        // json interface returns transactions in file order and the rest of the machinery
        // expects them in reverse chronological order
        trList.sortWith { o1, o2 ->
            val res = (o2.getDateIfAny() ?: SimpleDate.today()).compareTo(
                o1.getDateIfAny() ?: SimpleDate.today()
            )
            if (res != 0) res else o2.ledgerId.compareTo(o1.ledgerId)
        }
        return trList
    }

    @SuppressLint("DefaultLocale")
    override fun run() {
        Data.backgroundTaskStarted()
        var accounts: List<LedgerAccount>?
        var transactions: List<LedgerTransaction>?
        try {
            accounts = retrieveAccountList()
            // accounts is null in API-version auto-detection and means
            // requesting 'html' API version via the JSON classes
            // this can't work, and the null results in the legacy code below
            // being called
            transactions = if (accounts == null) null else retrieveTransactionList()

            if (accounts == null || transactions == null) {
                val mutableAccounts = ArrayList<LedgerAccount>()
                val mutableTransactions = ArrayList<LedgerTransaction>()
                retrieveTransactionListLegacy(mutableAccounts, mutableTransactions)
                accounts = mutableAccounts
                transactions = mutableTransactions
            }

            AccountAndTransactionListSaver(accounts, transactions).start()

            Data.lastUpdateDate.postValue(Date())

            finish(Result(null))
        } catch (e: MalformedURLException) {
            Logger.warn(TAG, "Invalid server URL", e)
            finish(Result("Invalid server URL"))
        } catch (e: HTTPException) {
            Logger.warn(TAG, "HTTP error: ${e.responseCode}", e)
            finish(
                Result(
                    String.format("HTTP error %d: %s", e.responseCode, e.message)
                )
            )
        } catch (e: IOException) {
            Logger.warn(TAG, "IO error during retrieval", e)
            finish(Result(e.localizedMessage))
        } catch (e: RuntimeJsonMappingException) {
            Logger.warn(TAG, "JSON parsing error", e)
            finish(Result(Result.ERR_JSON_PARSER_ERROR))
        } catch (e: ParseException) {
            Logger.warn(TAG, "Parse error during retrieval", e)
            finish(Result("Network error"))
        } catch (e: OperationCanceledException) {
            Logger.debug("RTT", "Retrieval was cancelled", e)
            finish(Result(null))
        } catch (e: ApiNotSupportedException) {
            Logger.warn(TAG, "API version not supported", e)
            finish(Result("Server version not supported"))
        } finally {
            Data.backgroundTaskFinished()
        }
    }

    fun throwIfCancelled() {
        if (isInterrupted) {
            throw OperationCanceledException(null)
        }
    }

    private enum class ParserState {
        EXPECTING_ACCOUNT,
        EXPECTING_ACCOUNT_AMOUNT,
        EXPECTING_TRANSACTION,
        EXPECTING_TRANSACTION_DESCRIPTION,
        EXPECTING_TRANSACTION_DETAILS
    }

    enum class ProgressState { STARTING, RUNNING, FINISHED }

    class Progress {
        private var progress = 0
        private var total = 0
        var state = ProgressState.RUNNING
        var error: String? = null
        var isIndeterminate = true
            private set

        constructor() {
            isIndeterminate = true
        }

        constructor(progress: Int, total: Int) {
            this.isIndeterminate = false
            this.progress = progress
            this.total = total
        }

        fun getProgress(): Int {
            ensureState(ProgressState.RUNNING)
            return progress
        }

        fun setProgress(progress: Int) {
            this.progress = progress
            this.state = ProgressState.RUNNING
        }

        fun getTotal(): Int {
            ensureState(ProgressState.RUNNING)
            return total
        }

        fun setTotal(total: Int) {
            this.total = total
            state = ProgressState.RUNNING
            isIndeterminate = total == -1
        }

        private fun ensureState(wanted: ProgressState) {
            if (state != wanted) {
                throw IllegalStateException(
                    String.format("Bad state: %s, expected %s", state, wanted)
                )
            }
        }

        companion object {
            @JvmStatic
            fun indeterminate(): Progress = Progress()

            @JvmStatic
            fun finished(error: String?): Progress = Progress().apply {
                state = ProgressState.FINISHED
                this.error = error
            }
        }
    }

    private class TransactionParserException(message: String) : IllegalStateException(message)

    class Result {
        var error: String?
        var accounts: List<LedgerAccount>? = null
        var transactions: List<LedgerTransaction>? = null

        constructor(error: String?) {
            this.error = error
        }

        constructor(accounts: List<LedgerAccount>, transactions: List<LedgerTransaction>) {
            this.error = null
            this.accounts = accounts
            this.transactions = transactions
        }

        companion object {
            const val ERR_JSON_PARSER_ERROR = "err_json_parser"
        }
    }

    private inner class AccountAndTransactionListSaver(
        private val accounts: List<LedgerAccount>,
        private val transactions: List<LedgerTransaction>
    ) : Thread() {
        override fun run() {
            runBlocking {
                Logger.debug(TAG, "Preparing account list")
                val list = ArrayList<AccountWithAmounts>()
                for (acc in accounts) {
                    val a = acc.toDBOWithAmounts()
                    val existing: Account? = accountRepository.getByNameSync(profile.id, acc.name)
                    if (existing != null) {
                        a.account.expanded = existing.expanded
                        a.account.amountsExpanded = existing.amountsExpanded
                        a.account.id = existing.id
                    }
                    list.add(a)
                }
                Logger.debug(TAG, "Account list prepared. Storing")
                accountRepository.storeAccounts(list, profile.id)
                Logger.debug(TAG, "Account list stored")

                Logger.debug(TAG, "Preparing transaction list")
                val tranList = ArrayList<TransactionWithAccounts>()

                for (tr in transactions) {
                    tranList.add(tr.toDBO())
                }

                Logger.debug(TAG, "Storing transaction list")
                transactionRepository.storeTransactions(tranList, profile.id)

                Logger.debug(TAG, "Transactions stored")

                optionRepository.insertOption(
                    Option(
                        profile.id,
                        Option.OPT_LAST_SCRAPE,
                        Date().time.toString()
                    )
                )
            }
        }
    }

    companion object {
        private const val MATCHING_TRANSACTIONS_LIMIT = 150
        private val reComment = Pattern.compile("^\\s*;")
        private val reTransactionStart = Pattern.compile(
            "<tr class=\"title\" id=\"transaction-(\\d+)\"><td class=\"date\"[^\"]*>([\\d.-]+)</td>"
        )
        private val reTransactionDescription =
            Pattern.compile("<tr class=\"posting\" title=\"(\\S+)\\s(.+)")
        private val reTransactionDetails = Pattern.compile(
            "^\\s+([!*]\\s+)?(\\S[\\S\\s]+\\S)\\s\\s+(?:([^\\d\\s+\\-]+)\\s*)?([-+]?\\d[\\d,.]*)(?:\\s*([^\\d\\s+\\-]+)\\s*$)?"
        )
        private val reEnd = Pattern.compile("\\bid=\"addmodal\"")
        private val reDecimalPoint = Pattern.compile("\\.\\d\\d?$")
        private val reDecimalComma = Pattern.compile(",\\d\\d?$")
        private const val TAG = "RTT"

        @JvmStatic
        fun parseTransactionAccountLine(line: String): LedgerTransactionAccount? {
            val m = reTransactionDetails.matcher(line)
            if (m.find()) {
                val postingStatus = m.group(1)
                val accName = requireNotNull(m.group(2)) { "Account name is null" }
                val currencyPre = m.group(3)
                var amount = requireNotNull(m.group(4)) { "Amount is null" }
                val currencyPost = m.group(5)

                val currency: String? = when {
                    !currencyPre.isNullOrEmpty() -> {
                        if (!currencyPost.isNullOrEmpty()) return null
                        currencyPre
                    }

                    !currencyPost.isNullOrEmpty() -> currencyPost

                    else -> null
                }

                amount = amount.replace(',', '.')

                return LedgerTransactionAccount(accName, amount.toFloat(), currency, null)
            }
            return null
        }

        private fun L(msg: String) {
            // debug("transaction-parser", msg)
        }
    }

    // %3A is '='
    private val reAccountName =
        Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"")
    private val reAccountValue = Pattern.compile(
        "<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>"
    )
}
