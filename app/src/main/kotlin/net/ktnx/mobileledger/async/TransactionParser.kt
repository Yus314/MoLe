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

package net.ktnx.mobileledger.async

import java.text.ParseException
import java.util.regex.Pattern
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.model.LedgerTransactionAccount

/**
 * Parses ledger transactions from legacy HTML format.
 *
 * This class extracts the transaction parsing logic from RetrieveTransactionsTask
 * to enable unit testing without network dependencies.
 *
 * ## Usage
 *
 * ```kotlin
 * val parser = TransactionParser()
 * val lines = htmlContent.lines()
 * val result = parser.parseTransactions(lines)
 *
 * result.fold(
 *     onSuccess = { transactions -> handleTransactions(transactions) },
 *     onFailure = { error -> handleError(error) }
 * )
 * ```
 */
class TransactionParser {

    private enum class ParserState {
        EXPECTING_TRANSACTION,
        EXPECTING_TRANSACTION_DESCRIPTION,
        EXPECTING_TRANSACTION_DETAILS
    }

    /**
     * Parse transactions from legacy HTML lines.
     *
     * @param lines The HTML content lines to parse (from General Journal section)
     * @return Result containing list of parsed transactions or error
     */
    fun parseTransactions(lines: List<String>): Result<List<LedgerTransaction>> = try {
        Result.success(parseTransactionsInternal(lines))
    } catch (e: TransactionParseException) {
        Result.failure(e)
    } catch (e: ParseException) {
        Result.failure(TransactionParseException("Parse error: ${e.message}", e))
    }

    private fun parseTransactionsInternal(lines: List<String>): List<LedgerTransaction> {
        val transactions = mutableListOf<LedgerTransaction>()
        var state = ParserState.EXPECTING_TRANSACTION
        var transactionId = 0
        var transaction: LedgerTransaction? = null

        for (line in lines) {
            val commentMatcher = reComment.matcher(line)
            if (commentMatcher.find()) {
                continue
            }

            when (state) {
                ParserState.EXPECTING_TRANSACTION -> {
                    if (line.isNotEmpty() && line[0] == ' ') continue
                    val m = reTransactionStart.matcher(line)
                    if (m.find()) {
                        transactionId = requireNotNull(m.group(1)) {
                            "Transaction ID match group is null"
                        }.toInt()
                        state = ParserState.EXPECTING_TRANSACTION_DESCRIPTION
                    }
                    val mEnd = reEnd.matcher(line)
                    if (mEnd.find()) {
                        break
                    }
                }

                ParserState.EXPECTING_TRANSACTION_DESCRIPTION -> {
                    if (line.isNotEmpty() && line[0] == ' ') continue
                    val m = reTransactionDescription.matcher(line)
                    if (m.find()) {
                        if (transactionId == 0) {
                            throw TransactionParseException(
                                "Transaction Id is 0 while expecting description"
                            )
                        }

                        var date = requireNotNull(m.group(1)) { "Date match group is null" }
                        val equalsIndex = date.indexOf('=')
                        if (equalsIndex >= 0) {
                            date = date.substring(equalsIndex + 1)
                        }
                        transaction = LedgerTransaction(
                            transactionId.toLong(),
                            date,
                            m.group(2)
                        )
                        state = ParserState.EXPECTING_TRANSACTION_DETAILS
                    }
                }

                ParserState.EXPECTING_TRANSACTION_DETAILS -> {
                    val currentTransaction = requireNotNull(transaction) { "Transaction is null" }
                    if (line.isEmpty()) {
                        currentTransaction.finishLoading()
                        transactions.add(currentTransaction)
                        state = ParserState.EXPECTING_TRANSACTION
                    } else {
                        val lta = parseTransactionAccountLine(line)
                        if (lta != null) {
                            currentTransaction.addAccount(lta)
                        } else {
                            throw TransactionParseException(
                                "Can't parse transaction $transactionId details: $line"
                            )
                        }
                    }
                }
            }
        }

        return transactions
    }

    companion object {
        private val reComment = Pattern.compile("^\\s*;")
        private val reTransactionStart = Pattern.compile(
            "<tr class=\"title\" id=\"transaction-(\\d+)\"><td class=\"date\"[^\"]*>([\\d.-]+)</td>"
        )
        private val reTransactionDescription =
            Pattern.compile("<tr class=\"posting\" title=\"(\\S+)\\s(.+)")
        private val reTransactionDetails = Pattern.compile(
            "^\\s+([!*]\\s+)?(\\S[\\S\\s]+\\S)\\s\\s+" +
                "(?:([^\\d\\s+\\-]+)\\s*)?([-+]?\\d[\\d,.]*)(?:\\s*([^\\d\\s+\\-]+)\\s*\$)?"
        )
        private val reEnd = Pattern.compile("\\bid=\"addmodal\"")

        /**
         * Parse a single transaction account line.
         *
         * @param line The line to parse
         * @return The parsed account entry, or null if the line doesn't match
         */
        @JvmStatic
        fun parseTransactionAccountLine(line: String): LedgerTransactionAccount? {
            val m = reTransactionDetails.matcher(line)
            if (m.find()) {
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
    }
}

/**
 * Exception thrown when transaction parsing fails.
 */
class TransactionParseException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
