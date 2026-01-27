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

package net.ktnx.mobileledger.domain.usecase.sync

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import net.ktnx.mobileledger.async.TransactionParser
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.common.utils.parseIsoDate
import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.domain.model.AccountAmount
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.domain.model.TransactionLine
import net.ktnx.mobileledger.core.network.HledgerClient
import net.ktnx.mobileledger.core.network.NetworkAuthenticationException
import net.ktnx.mobileledger.core.network.NetworkHttpException

/**
 * Implementation of LegacyHtmlParser that parses HTML from hledger-web /journal endpoint.
 */
@Singleton
class LegacyHtmlParserImpl @Inject constructor(
    private val hledgerClient: HledgerClient
) : LegacyHtmlParser {

    override suspend fun parse(
        profile: Profile,
        expectedPostingsCount: Int,
        onProgress: suspend (Int, Int) -> Unit
    ): LegacyParseResult {
        coroutineContext.ensureActive()

        val result = hledgerClient.get(profile, "journal")

        return result.fold(
            onSuccess = { inputStream ->
                val accounts = ArrayList<Account>()
                val transactions = ArrayList<Transaction>()
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
                                            amounts = emptyList()
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
                                    val parsedDate = date.parseIsoDate()
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

                // Generate parent accounts that don't exist
                ensureParentAccountsExist(accounts, existingNames)

                LegacyParseResult(accounts, transactions)
            },
            onFailure = { error ->
                when (error) {
                    is NetworkAuthenticationException ->
                        throw NetworkHttpException(401, error.message ?: "Authentication required")

                    is NetworkHttpException -> throw error

                    else -> throw error
                }
            }
        )
    }

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
