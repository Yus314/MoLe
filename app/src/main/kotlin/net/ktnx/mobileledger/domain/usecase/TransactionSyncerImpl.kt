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
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.async.TransactionParser
import net.ktnx.mobileledger.data.repository.AccountRepository
import net.ktnx.mobileledger.data.repository.OptionRepository
import net.ktnx.mobileledger.data.repository.TransactionRepository
import net.ktnx.mobileledger.data.repository.mapper.AccountMapper.withStateFrom
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.domain.model.Account
import net.ktnx.mobileledger.domain.model.AccountAmount
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.SyncError
import net.ktnx.mobileledger.domain.model.SyncException
import net.ktnx.mobileledger.domain.model.SyncProgress
import net.ktnx.mobileledger.domain.model.SyncResult
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.err.HTTPException
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.json.AccountListParser
import net.ktnx.mobileledger.json.ApiNotSupportedException
import net.ktnx.mobileledger.json.TransactionListParser
import net.ktnx.mobileledger.network.AuthenticationException
import net.ktnx.mobileledger.network.HledgerClient
import net.ktnx.mobileledger.network.HttpException
import net.ktnx.mobileledger.network.NotFoundException
import net.ktnx.mobileledger.service.AppStateService
import net.ktnx.mobileledger.service.SyncInfo
import net.ktnx.mobileledger.utils.Globals
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
    private val hledgerClient: HledgerClient,
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
            var accounts: List<Account>? = retrieveAccountList(profile)

            // Retrieve transactions
            coroutineContext.ensureActive()
            emit(SyncProgress.Indeterminate("取引を取得中..."))
            var transactions: List<Transaction>? = if (accounts == null) {
                null
            } else {
                retrieveTransactionList(profile) { current, total ->
                    emit(SyncProgress.Running(current, total, "取引を処理中..."))
                }
            }

            // Fall back to legacy HTML parsing if JSON API is not available
            if (accounts == null || transactions == null) {
                emit(SyncProgress.Indeterminate("HTMLモードで取得中..."))
                val mutableAccounts = ArrayList<Account>()
                val mutableTransactions = ArrayList<Transaction>()
                retrieveTransactionListLegacy(profile, mutableAccounts, mutableTransactions) { current, total ->
                    emit(SyncProgress.Running(current, total, "取引を処理中..."))
                }
                val existingNames = mutableAccounts.map { it.name }.toMutableSet()
                ensureParentAccountsExist(mutableAccounts, existingNames)
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
    private suspend fun retrieveAccountList(profile: Profile): List<Account>? {
        val apiVersion = API.valueOf(profile.apiVersion)
        return when {
            apiVersion == API.auto -> retrieveAccountListAnyVersion(profile)

            apiVersion == API.html -> {
                logcat { "Declining using JSON API for /accounts with configured legacy API version" }
                null
            }

            else -> retrieveAccountListForVersion(profile, apiVersion)
        }
    }

    private suspend fun retrieveAccountListAnyVersion(profile: Profile): List<Account>? {
        for (ver in API.allVersions) {
            try {
                return retrieveAccountListForVersion(profile, ver)
            } catch (e: JsonParseException) {
                logcat { "Error during account list retrieval using API ${ver.description}: ${e.asLog()}" }
            } catch (e: RuntimeJsonMappingException) {
                logcat { "Error during account list retrieval using API ${ver.description}: ${e.asLog()}" }
            }
        }
        throw ApiNotSupportedException()
    }

    private suspend fun retrieveAccountListForVersion(profile: Profile, version: API): List<Account>? {
        coroutineContext.ensureActive()

        val result = hledgerClient.get(profile, "accounts")

        return result.fold(
            onSuccess = { inputStream ->
                val list = ArrayList<Account>()
                val existingNames = HashSet<String>()

                inputStream.use { resp ->
                    coroutineContext.ensureActive()
                    val parser = AccountListParser.forApiVersion(version, resp)
                    expectedPostingsCount = 0

                    while (true) {
                        coroutineContext.ensureActive()
                        val acc = parser.nextAccountDomain() ?: break
                        list.add(acc)
                        existingNames.add(acc.name)
                        expectedPostingsCount += acc.amounts.size
                    }

                    logcat(LogPriority.WARN) { "Got ${list.size} accounts using protocol ${version.description}" }
                }

                // Generate parent accounts that don't exist
                ensureParentAccountsExist(list, existingNames)

                list
            },
            onFailure = { error ->
                when (error) {
                    is NotFoundException -> null
                    is AuthenticationException -> throw HTTPException(401, error.message ?: "Authentication required")
                    is HttpException -> throw HTTPException(error.statusCode, error.message ?: "HTTP error")
                    else -> throw error
                }
            }
        )
    }

    /**
     * 親アカウントが存在しない場合は生成する
     *
     * hledger API はアカウントのフラットリストを返すため、
     * 親アカウントが明示的に含まれない場合がある。
     * 例: "Assets:Cash" はあるが "Assets" がない場合、"Assets" を生成する。
     */
    private fun ensureParentAccountsExist(accounts: MutableList<Account>, existingNames: MutableSet<String>) {
        val toAdd = mutableListOf<Account>()

        for (account in accounts) {
            var parentName = account.parentName
            while (parentName != null && parentName !in existingNames) {
                val level = parentName.count { it == ':' }
                toAdd.add(
                    Account(
                        id = null,
                        name = parentName,
                        level = level,
                        isExpanded = false,
                        isVisible = true,
                        amounts = emptyList()
                    )
                )
                existingNames.add(parentName)
                parentName = parentName.lastIndexOf(':').let { idx ->
                    if (idx > 0) parentName!!.substring(0, idx) else null
                }
            }
        }

        accounts.addAll(toAdd)
    }

    /**
     * JSON API を使用して取引リストを取得する
     */
    private suspend fun retrieveTransactionList(
        profile: Profile,
        onProgress: suspend (Int, Int) -> Unit
    ): List<Transaction>? {
        val apiVersion = API.valueOf(profile.apiVersion)
        return when {
            apiVersion == API.auto -> retrieveTransactionListAnyVersion(profile, onProgress)

            apiVersion == API.html -> {
                logcat { "Declining using JSON API for /transactions with configured legacy API version" }
                null
            }

            else -> retrieveTransactionListForVersion(profile, apiVersion, onProgress)
        }
    }

    private suspend fun retrieveTransactionListAnyVersion(
        profile: Profile,
        onProgress: suspend (Int, Int) -> Unit
    ): List<Transaction>? {
        for (ver in API.allVersions) {
            try {
                return retrieveTransactionListForVersion(profile, ver, onProgress)
            } catch (e: Exception) {
                logcat { "Error during transaction list retrieval using API ${ver.description}: ${e.asLog()}" }
            }
        }
        throw ApiNotSupportedException()
    }

    private suspend fun retrieveTransactionListForVersion(
        profile: Profile,
        apiVersion: API,
        onProgress: suspend (Int, Int) -> Unit
    ): List<Transaction>? {
        coroutineContext.ensureActive()

        val result = hledgerClient.get(profile, "transactions")

        return result.fold(
            onSuccess = { inputStream ->
                val trList = ArrayList<Transaction>()
                inputStream.use { resp ->
                    coroutineContext.ensureActive()
                    val parser = TransactionListParser.forApiVersion(apiVersion, resp)
                    var processedPostings = 0

                    while (true) {
                        coroutineContext.ensureActive()
                        val transaction = parser.nextTransactionDomain() ?: break
                        trList.add(transaction)

                        processedPostings += transaction.lines.size
                        if (expectedPostingsCount > 0) {
                            onProgress(processedPostings, expectedPostingsCount)
                        }
                    }

                    logcat(LogPriority.WARN) {
                        "Got ${trList.size} transactions using protocol ${apiVersion.description}"
                    }
                }

                // Sort transactions in reverse chronological order
                trList.sortWith { o1, o2 ->
                    val res = o2.date.compareTo(o1.date)
                    if (res != 0) res else o2.ledgerId.compareTo(o1.ledgerId)
                }
                trList
            },
            onFailure = { error ->
                when (error) {
                    is NotFoundException -> null
                    is AuthenticationException -> throw HTTPException(401, error.message ?: "Authentication required")
                    is HttpException -> throw HTTPException(error.statusCode, error.message ?: "HTTP error")
                    else -> throw error
                }
            }
        )
    }

    /**
     * レガシー HTML パーシングで取引とアカウントを取得する
     *
     * hledger-web の /journal エンドポイントから HTML をパースし、
     * ドメインモデル (Account, Transaction) を直接生成する。
     */
    private suspend fun retrieveTransactionListLegacy(
        profile: Profile,
        accounts: MutableList<Account>,
        transactions: MutableList<Transaction>,
        onProgress: suspend (Int, Int) -> Unit
    ) {
        coroutineContext.ensureActive()

        val result = hledgerClient.get(profile, "journal")

        result.fold(
            onSuccess = { inputStream ->
                var maxTransactionId = -1
                val existingNames = HashSet<String>()
                val accountAmounts = HashMap<String, MutableList<AccountAmount>>()
                var lastAccountName: String? = null

                inputStream.use { resp ->
                    val buf = BufferedReader(InputStreamReader(resp, StandardCharsets.UTF_8))
                    var state = ParserState.EXPECTING_ACCOUNT
                    var processedTransactionCount = 0
                    var transactionId = 0
                    var transactionBuilder: TransactionBuilder? = null

                    lines@ while (true) {
                        val line = buf.readLine() ?: break
                        coroutineContext.ensureActive()

                        if (reComment.matcher(line).find()) continue

                        when (state) {
                            ParserState.EXPECTING_ACCOUNT -> {
                                if (line == "<h2>General Journal</h2>") {
                                    // Finalize accounts with their amounts
                                    finalizeAccountAmounts(accounts, accountAmounts)
                                    state = ParserState.EXPECTING_TRANSACTION
                                    continue
                                }
                                val m = reAccountName.matcher(line)
                                if (m.find()) {
                                    val acctEncoded = m.group(1) ?: continue
                                    var accName = URLDecoder.decode(acctEncoded, "UTF-8")
                                    accName = accName.replace("\"", "")

                                    if (existingNames.contains(accName)) continue

                                    val level = accName.count { it == ':' }
                                    accounts.add(
                                        Account(
                                            id = null,
                                            name = accName,
                                            level = level,
                                            isExpanded = false,
                                            isVisible = true,
                                            amounts = emptyList() // Will be filled later
                                        )
                                    )
                                    existingNames.add(accName)
                                    accountAmounts[accName] = mutableListOf()
                                    lastAccountName = accName
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
                                    lastAccountName?.let { name ->
                                        accountAmounts[name]?.add(AccountAmount(currency, floatVal))
                                    }
                                }
                                if (matchFound) {
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
                                    val parsedDate = Globals.parseIsoDate(date)
                                    transactionBuilder = TransactionBuilder(
                                        ledgerId = transactionId.toLong(),
                                        date = parsedDate,
                                        description = m.group(2) ?: ""
                                    )
                                    state = ParserState.EXPECTING_TRANSACTION_DETAILS
                                }
                            }

                            ParserState.EXPECTING_TRANSACTION_DETAILS -> {
                                val builder = transactionBuilder ?: continue
                                if (line.isEmpty()) {
                                    transactions.add(builder.build())
                                    transactionBuilder = null
                                    state = ParserState.EXPECTING_TRANSACTION
                                } else {
                                    val txLine = TransactionParser.parseTransactionAccountLine(line)
                                    if (txLine != null) {
                                        builder.addLine(txLine)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            onFailure = { error ->
                when (error) {
                    is AuthenticationException -> throw HTTPException(401, error.message ?: "Authentication required")
                    is HttpException -> throw HTTPException(error.statusCode, error.message ?: "HTTP error")
                    else -> throw error
                }
            }
        )
    }

    /**
     * パース中に収集したアカウント残高を Account オブジェクトに設定する
     */
    private fun finalizeAccountAmounts(
        accounts: MutableList<Account>,
        accountAmounts: Map<String, List<AccountAmount>>
    ) {
        for (i in accounts.indices) {
            val account = accounts[i]
            val amounts = accountAmounts[account.name] ?: emptyList()
            if (amounts.isNotEmpty()) {
                accounts[i] = account.copy(amounts = amounts)
            }
        }
    }

    /**
     * HTML パーシング用トランザクションビルダー
     */
    private class TransactionBuilder(
        private val ledgerId: Long,
        private val date: SimpleDate,
        private val description: String
    ) {
        private val lines = mutableListOf<TransactionLine>()

        fun addLine(line: TransactionLine) {
            lines.add(line)
        }

        fun build(): Transaction = Transaction(
            id = null,
            ledgerId = ledgerId,
            date = date,
            description = description,
            comment = null,
            lines = lines.toList()
        )
    }

    /**
     * アカウントと取引をデータベースに保存する
     *
     * ドメインモデルを直接受け取り、Repository を使用して保存する。
     * これにより、domain層からdb層への直接依存を排除する。
     */
    private suspend fun saveAccountsAndTransactions(
        profile: Profile,
        accounts: List<Account>,
        transactions: List<Transaction>
    ) {
        val profileId = profile.id ?: throw IllegalStateException("Cannot sync unsaved profile")

        logcat { "Preparing account list" }
        val accountsWithState = accounts.map { account ->
            coroutineContext.ensureActive()
            // Preserve existing UI state if account exists
            val existing = accountRepository.getByNameWithAmounts(profileId, account.name)
            account.withStateFrom(existing)
        }
        logcat { "Account list prepared. Storing" }
        accountRepository.storeAccountsAsDomain(accountsWithState, profileId)
        logcat { "Account list stored" }

        logcat { "Storing transaction list" }
        transactionRepository.storeTransactionsAsDomain(transactions, profileId)
        logcat { "Transactions stored" }

        optionRepository.setLastSyncTimestamp(profileId, Date().time)
    }

    /**
     * 例外を SyncException にマッピングする
     */
    private fun mapToSyncException(e: Throwable): SyncException {
        val syncError = when (e) {
            is SyncException -> return e

            is SocketTimeoutException -> SyncError.TimeoutError(message = "サーバーが応答しません")

            is MalformedURLException -> SyncError.ValidationError(message = "無効なサーバーURLです")

            is AuthenticationException -> SyncError.AuthenticationError(message = "認証に失敗しました", httpCode = 401)

            is HttpException -> SyncError.ServerError(message = e.message ?: "サーバーエラー", httpCode = e.statusCode)

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
        private val reComment = Pattern.compile("^\\s*;")
        private val reTransactionStart = Pattern.compile(
            "<tr class=\"title\" id=\"transaction-(\\d+)\"><td class=\"date\"[^\"]*>([\\d.-]+)</td>"
        )
        private val reTransactionDescription = Pattern.compile("<tr class=\"posting\" title=\"(\\S+)\\s(.+)")
        private val reEnd = Pattern.compile("\\bid=\"addmodal\"")
        private val reDecimalPoint = Pattern.compile("\\.\\d\\d?$")
        private val reDecimalComma = Pattern.compile(",\\d\\d?$")
        private val reAccountName = Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"")
        private val reAccountValue = Pattern.compile(
            "<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>"
        )
    }
}
