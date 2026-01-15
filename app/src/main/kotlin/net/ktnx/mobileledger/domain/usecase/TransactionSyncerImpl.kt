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

package net.ktnx.mobileledger.domain.usecase

import android.os.OperationCanceledException
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.RuntimeJsonMappingException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.ParseException
import java.util.Date
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.db.Account
import net.ktnx.mobileledger.db.AccountWithAmounts
import net.ktnx.mobileledger.db.Option
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.db.TransactionWithAccounts
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.domain.model.SyncError
import net.ktnx.mobileledger.domain.model.SyncException
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.domain.model.SyncResult
import net.ktnx.mobileledger.err.HTTPException
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.json.AccountListParser
import net.ktnx.mobileledger.json.ApiNotSupportedException
import net.ktnx.mobileledger.json.TransactionListParser
import net.ktnx.mobileledger.model.LedgerAccount
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.LedgerTransactionAccount
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.SyncInfo
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.NetworkUtil
import net.ktnx.mobileledger.utils.SimpleDate

/**
 * TransactionSyncer の Pure Coroutines 実装
 *
 * RetrieveTransactionsTask のロジックを suspend 関数として直接実装し、
 * Thread を完全に排除する。TestDispatcher を使用したテストで即座に完了可能。
 *
 * ## 特徴
 * - Thread 使用なし (Thread.start(), Thread.join() 不使用)
 * - 全ての I/O は withContext(ioDispatcher) で実行
 * - ensureActive() による迅速なキャンセル応答
 * - Flow<SyncProgress> で進捗報告
 */
@Singleton
class TransactionSyncerImpl @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val optionRepository: OptionRepository,
    private val appStateService: AppStateService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSyncer {

    private var _lastResult: SyncResult? = null
    private var expectedPostingsCount = -1

    override fun sync(profile: Profile): Flow<SyncProgress> = flow {
        val startTime = System.currentTimeMillis()
        expectedPostingsCount = -1

        try {
            emit(SyncProgress.Starting("接続中..."))

            // Retrieve accounts
            coroutineContext.ensureActive()
            emit(SyncProgress.Indeterminate("アカウントを取得中..."))
            var accounts: List<LedgerAccount>? = retrieveAccountList(profile)

            // Retrieve transactions
            coroutineContext.ensureActive()
            emit(SyncProgress.Indeterminate("取引を取得中..."))
            var transactions: List<LedgerTransaction>? = if (accounts == null) {
                null
            } else {
                retrieveTransactionList(profile) { current, total ->
                    emit(SyncProgress.Running(current, total, "取引を処理中..."))
                }
            }

            // Fall back to legacy HTML parsing if JSON API is not available
            if (accounts == null || transactions == null) {
                emit(SyncProgress.Indeterminate("HTMLモードで取得中..."))
                val mutableAccounts = ArrayList<LedgerAccount>()
                val mutableTransactions = ArrayList<LedgerTransaction>()
                retrieveTransactionListLegacy(profile, mutableAccounts, mutableTransactions) { current, total ->
                    emit(SyncProgress.Running(current, total, "取引を処理中..."))
                }
                accounts = mutableAccounts
                transactions = mutableTransactions
            }

            // Save to database
            coroutineContext.ensureActive()
            emit(SyncProgress.Indeterminate("データを保存中..."))
            saveAccountsAndTransactions(profile, accounts, transactions)

            // Update sync info
            appStateService.updateSyncInfo(
                SyncInfo(
                    date = Date(),
                    transactionCount = transactions.size,
                    accountCount = accounts.size,
                    totalAccountCount = accounts.size
                )
            )

            val duration = System.currentTimeMillis() - startTime
            _lastResult = SyncResult(
                transactionCount = transactions.size,
                accountCount = accounts.size,
                duration = duration
            )
        } catch (e: Exception) {
            throw mapToSyncException(e)
        }
    }.flowOn(ioDispatcher)

    override fun getLastResult(): SyncResult? = _lastResult

    /**
     * JSON API を使用してアカウントリストを取得する
     */
    private suspend fun retrieveAccountList(profile: Profile): List<LedgerAccount>? {
        val apiVersion = API.valueOf(profile.apiVersion)
        return when {
            apiVersion == API.auto -> retrieveAccountListAnyVersion(profile)

            apiVersion == API.html -> {
                Logger.debug("json", "Declining using JSON API for /accounts with configured legacy API version")
                null
            }

            else -> retrieveAccountListForVersion(profile, apiVersion)
        }
    }

    private suspend fun retrieveAccountListAnyVersion(profile: Profile): List<LedgerAccount>? {
        for (ver in API.allVersions) {
            try {
                return retrieveAccountListForVersion(profile, ver)
            } catch (e: JsonParseException) {
                Logger.debug("json", "Error during account list retrieval using API ${ver.description}", e)
            } catch (e: RuntimeJsonMappingException) {
                Logger.debug("json", "Error during account list retrieval using API ${ver.description}", e)
            }
        }
        throw ApiNotSupportedException()
    }

    private suspend fun retrieveAccountListForVersion(profile: Profile, version: API): List<LedgerAccount>? {
        coroutineContext.ensureActive()
        val http = NetworkUtil.prepareConnection(profile, "accounts")
        http.allowUserInteraction = false

        return try {
            when (http.responseCode) {
                200 -> { /* continue */ }
                404 -> return null
                else -> throw HTTPException(http.responseCode, http.responseMessage)
            }

            val list = ArrayList<LedgerAccount>()
            val map = HashMap<String, LedgerAccount>()

            http.inputStream.use { resp ->
                coroutineContext.ensureActive()
                val parser = AccountListParser.forApiVersion(version, resp)
                expectedPostingsCount = 0

                while (true) {
                    coroutineContext.ensureActive()
                    val acc = parser.nextAccountWithoutTask(map) ?: break
                    list.add(acc)
                    expectedPostingsCount += acc.amountCount
                }

                Logger.warn("accounts", "Got ${list.size} accounts using protocol ${version.description}")
            }
            list
        } finally {
            http.disconnect()
        }
    }

    /**
     * JSON API を使用して取引リストを取得する
     */
    private suspend fun retrieveTransactionList(
        profile: Profile,
        onProgress: suspend (Int, Int) -> Unit
    ): List<LedgerTransaction>? {
        val apiVersion = API.valueOf(profile.apiVersion)
        return when {
            apiVersion == API.auto -> retrieveTransactionListAnyVersion(profile, onProgress)

            apiVersion == API.html -> {
                Logger.debug("json", "Declining using JSON API for /transactions with configured legacy API version")
                null
            }

            else -> retrieveTransactionListForVersion(profile, apiVersion, onProgress)
        }
    }

    private suspend fun retrieveTransactionListAnyVersion(
        profile: Profile,
        onProgress: suspend (Int, Int) -> Unit
    ): List<LedgerTransaction>? {
        for (ver in API.allVersions) {
            try {
                return retrieveTransactionListForVersion(profile, ver, onProgress)
            } catch (e: Exception) {
                Logger.debug("json", "Error during transaction list retrieval using API ${ver.description}", e)
            }
        }
        throw ApiNotSupportedException()
    }

    private suspend fun retrieveTransactionListForVersion(
        profile: Profile,
        apiVersion: API,
        onProgress: suspend (Int, Int) -> Unit
    ): List<LedgerTransaction>? {
        coroutineContext.ensureActive()
        val http = NetworkUtil.prepareConnection(profile, "transactions")
        http.allowUserInteraction = false

        return try {
            when (http.responseCode) {
                200 -> { /* continue */ }
                404 -> return null
                else -> throw HTTPException(http.responseCode, http.responseMessage)
            }

            val trList = ArrayList<LedgerTransaction>()
            http.inputStream.use { resp ->
                coroutineContext.ensureActive()
                val parser = TransactionListParser.forApiVersion(apiVersion, resp)
                var processedPostings = 0

                while (true) {
                    coroutineContext.ensureActive()
                    val transaction = parser.nextTransaction() ?: break
                    trList.add(transaction)

                    processedPostings += transaction.accounts.size
                    if (expectedPostingsCount > 0) {
                        onProgress(processedPostings, expectedPostingsCount)
                    }
                }

                Logger.warn("transactions", "Got ${trList.size} transactions using protocol ${apiVersion.description}")
            }

            // Sort transactions in reverse chronological order
            trList.sortWith { o1, o2 ->
                val res = (o2.getDateIfAny() ?: SimpleDate.today()).compareTo(
                    o1.getDateIfAny() ?: SimpleDate.today()
                )
                if (res != 0) res else o2.ledgerId.compareTo(o1.ledgerId)
            }
            trList
        } finally {
            http.disconnect()
        }
    }

    /**
     * レガシー HTML パーシングで取引とアカウントを取得する
     */
    private suspend fun retrieveTransactionListLegacy(
        profile: Profile,
        accounts: MutableList<LedgerAccount>,
        transactions: MutableList<LedgerTransaction>,
        onProgress: suspend (Int, Int) -> Unit
    ) {
        coroutineContext.ensureActive()
        val http = NetworkUtil.prepareConnection(profile, "journal")
        http.allowUserInteraction = false

        try {
            if (http.responseCode != 200) {
                throw HTTPException(http.responseCode, http.responseMessage)
            }

            var maxTransactionId = -1
            val map = HashMap<String, LedgerAccount>()
            var lastAccount: LedgerAccount? = null
            val syntheticAccounts = ArrayList<LedgerAccount>()

            http.inputStream.use { resp ->
                val buf = BufferedReader(InputStreamReader(resp, StandardCharsets.UTF_8))
                var state = ParserState.EXPECTING_ACCOUNT
                var processedTransactionCount = 0
                var transactionId = 0
                var transaction: LedgerTransaction? = null

                lines@ while (true) {
                    val line = buf.readLine() ?: break
                    coroutineContext.ensureActive()

                    if (reComment.matcher(line).find()) continue

                    when (state) {
                        ParserState.EXPECTING_ACCOUNT -> {
                            if (line == "<h2>General Journal</h2>") {
                                state = ParserState.EXPECTING_TRANSACTION
                                continue
                            }
                            val m = reAccountName.matcher(line)
                            if (m.find()) {
                                val acctEncoded = m.group(1) ?: continue
                                var accName = URLDecoder.decode(acctEncoded, "UTF-8")
                                accName = accName.replace("\"", "")

                                if (map.containsKey(accName)) continue

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
                            }
                        }

                        ParserState.EXPECTING_ACCOUNT_AMOUNT -> {
                            val m = reAccountValue.matcher(line)
                            var matchFound = false
                            while (m.find()) {
                                coroutineContext.ensureActive()
                                matchFound = true
                                var value = m.group(1) ?: continue
                                val currency = m.group(2) ?: ""

                                if (reDecimalComma.matcher(value).find()) {
                                    value = value.replace(".", "").replace(',', '.')
                                }
                                if (reDecimalPoint.matcher(value).find()) {
                                    value = value.replace(",", "").replace(" ", "")
                                }

                                val floatVal = value.toFloat()
                                lastAccount?.addAmount(floatVal, currency)
                                for (syn in syntheticAccounts) {
                                    syn.addAmount(floatVal, currency)
                                }
                            }
                            if (matchFound) {
                                syntheticAccounts.clear()
                                state = ParserState.EXPECTING_ACCOUNT
                            }
                        }

                        ParserState.EXPECTING_TRANSACTION -> {
                            if (line.isNotEmpty() && line[0] == ' ') continue
                            val m = reTransactionStart.matcher(line)
                            if (m.find()) {
                                transactionId = (m.group(1) ?: "0").toInt()
                                state = ParserState.EXPECTING_TRANSACTION_DESCRIPTION
                                processedTransactionCount++
                                if (maxTransactionId < transactionId) {
                                    maxTransactionId = transactionId
                                }
                                if (expectedPostingsCount > 0) {
                                    onProgress(processedTransactionCount, expectedPostingsCount)
                                }
                            }
                            if (reEnd.matcher(line).find()) break@lines
                        }

                        ParserState.EXPECTING_TRANSACTION_DESCRIPTION -> {
                            if (line.isNotEmpty() && line[0] == ' ') continue
                            val m = reTransactionDescription.matcher(line)
                            if (m.find()) {
                                var date = m.group(1) ?: continue
                                val equalsIndex = date.indexOf('=')
                                if (equalsIndex >= 0) {
                                    date = date.substring(equalsIndex + 1)
                                }
                                transaction = LedgerTransaction(transactionId.toLong(), date, m.group(2))
                                state = ParserState.EXPECTING_TRANSACTION_DETAILS
                            }
                        }

                        ParserState.EXPECTING_TRANSACTION_DETAILS -> {
                            val currentTransaction = transaction ?: continue
                            if (line.isEmpty()) {
                                currentTransaction.finishLoading()
                                transactions.add(currentTransaction)
                                state = ParserState.EXPECTING_TRANSACTION
                            } else {
                                val lta = parseTransactionAccountLine(line)
                                if (lta != null) {
                                    currentTransaction.addAccount(lta)
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            http.disconnect()
        }
    }

    /**
     * アカウントと取引をデータベースに保存する
     */
    private suspend fun saveAccountsAndTransactions(
        profile: Profile,
        accounts: List<LedgerAccount>,
        transactions: List<LedgerTransaction>
    ) {
        Logger.debug(TAG, "Preparing account list")
        val list = ArrayList<AccountWithAmounts>()
        for (acc in accounts) {
            coroutineContext.ensureActive()
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
            coroutineContext.ensureActive()
            tranList.add(tr.toDBO())
        }

        Logger.debug(TAG, "Storing transaction list")
        transactionRepository.storeTransactions(tranList, profile.id)
        Logger.debug(TAG, "Transactions stored")

        optionRepository.insertOption(
            Option(profile.id, Option.OPT_LAST_SCRAPE, Date().time.toString())
        )
    }

    private fun ensureAccountExists(
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
        map[accountName] = acc
        return acc
    }

    /**
     * 例外を SyncException にマッピングする
     */
    private fun mapToSyncException(e: Throwable): SyncException {
        val syncError = when (e) {
            is SyncException -> return e

            is SocketTimeoutException -> SyncError.TimeoutError(message = "サーバーが応答しません")

            is MalformedURLException -> SyncError.ValidationError(message = "無効なサーバーURLです")

            is HTTPException -> when (e.responseCode) {
                401 -> SyncError.AuthenticationError(message = "認証に失敗しました", httpCode = e.responseCode)
                else -> SyncError.ServerError(message = e.message ?: "サーバーエラー", httpCode = e.responseCode)
            }

            is IOException -> SyncError.NetworkError(message = e.localizedMessage ?: "ネットワークエラー", cause = e)

            is RuntimeJsonMappingException -> SyncError.ParseError(message = "JSONパースエラー", cause = e)

            is ParseException -> SyncError.ParseError(message = "データ解析エラー", cause = e)

            is OperationCanceledException -> SyncError.Cancelled

            is ApiNotSupportedException -> SyncError.ApiVersionError(message = "サポートされていないAPIバージョンです")

            is kotlinx.coroutines.CancellationException -> SyncError.Cancelled

            else -> SyncError.UnknownError(
                message = e.localizedMessage ?: "予期しないエラーが発生しました",
                cause = e
            )
        }
        return SyncException(syncError)
    }

    private enum class ParserState {
        EXPECTING_ACCOUNT,
        EXPECTING_ACCOUNT_AMOUNT,
        EXPECTING_TRANSACTION,
        EXPECTING_TRANSACTION_DESCRIPTION,
        EXPECTING_TRANSACTION_DETAILS
    }

    companion object {
        private const val TAG = "TransactionSyncerImpl"
        private val reComment = Pattern.compile("^\\s*;")
        private val reTransactionStart = Pattern.compile(
            "<tr class=\"title\" id=\"transaction-(\\d+)\"><td class=\"date\"[^\"]*>([\\d.-]+)</td>"
        )
        private val reTransactionDescription = Pattern.compile("<tr class=\"posting\" title=\"(\\S+)\\s(.+)")
        private val reTransactionDetails = Pattern.compile(
            "^\\s+([!*]\\s+)?(\\S[\\S\\s]+\\S)\\s\\s+" +
                "(?:([^\\d\\s+\\-]+)\\s*)?([-+]?\\d[\\d,.]*)(?:\\s*([^\\d\\s+\\-]+)\\s*\$)?"
        )
        private val reEnd = Pattern.compile("\\bid=\"addmodal\"")
        private val reDecimalPoint = Pattern.compile("\\.\\d\\d?$")
        private val reDecimalComma = Pattern.compile(",\\d\\d?$")
        private val reAccountName = Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"")
        private val reAccountValue = Pattern.compile(
            "<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>"
        )

        @JvmStatic
        fun parseTransactionAccountLine(line: String): LedgerTransactionAccount? {
            val m = reTransactionDetails.matcher(line)
            if (m.find()) {
                val accName = m.group(2) ?: return null
                val currencyPre = m.group(3)
                var amount = m.group(4) ?: return null
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
    }
}
