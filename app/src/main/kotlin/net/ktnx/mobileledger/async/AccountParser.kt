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

import java.net.URLDecoder
import java.util.Locale
import java.util.regex.Pattern
import net.ktnx.mobileledger.model.LedgerAccount

/**
 * Parses ledger accounts from legacy HTML format.
 *
 * This class extracts the account parsing logic from RetrieveTransactionsTask
 * to enable unit testing without network dependencies.
 *
 * ## Usage
 *
 * ```kotlin
 * val parser = AccountParser()
 * val lines = htmlContent.lines()
 * val result = parser.parseAccounts(lines)
 *
 * result.fold(
 *     onSuccess = { accounts -> handleAccounts(accounts) },
 *     onFailure = { error -> handleError(error) }
 * )
 * ```
 */
class AccountParser {

    private enum class ParserState {
        EXPECTING_ACCOUNT,
        EXPECTING_ACCOUNT_AMOUNT
    }

    /**
     * Result of account parsing including synthetic parent accounts.
     */
    data class ParseResult(
        val accounts: List<LedgerAccount>,
        val totalPostings: Int
    )

    /**
     * Parse accounts from legacy HTML lines.
     *
     * @param lines The HTML content lines to parse (before General Journal section)
     * @return Result containing parsed accounts with amounts or error
     */
    fun parseAccounts(lines: List<String>): Result<ParseResult> = try {
        Result.success(parseAccountsInternal(lines))
    } catch (e: AccountParseException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(AccountParseException("Parse error: ${e.message}", e))
    }

    private fun parseAccountsInternal(lines: List<String>): ParseResult {
        val accounts = mutableListOf<LedgerAccount>()
        val map = HashMap<String, LedgerAccount>()
        var lastAccount: LedgerAccount? = null
        val syntheticAccounts = ArrayList<LedgerAccount>()
        var state = ParserState.EXPECTING_ACCOUNT
        var totalPostings = 0

        for (line in lines) {
            val commentMatcher = reComment.matcher(line)
            if (commentMatcher.find()) {
                continue
            }

            when (state) {
                ParserState.EXPECTING_ACCOUNT -> {
                    if (line == "<h2>General Journal</h2>") {
                        // End of accounts section
                        break
                    }
                    val m = reAccountName.matcher(line)
                    if (m.find()) {
                        val acctEncoded = requireNotNull(m.group(1)) {
                            "Account name match group is null"
                        }
                        var accName = URLDecoder.decode(acctEncoded, "UTF-8")
                        accName = accName.replace("\"", "")

                        lastAccount = map[accName]
                        if (lastAccount != null) {
                            // Duplicate account, skip
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
                    }
                }

                ParserState.EXPECTING_ACCOUNT_AMOUNT -> {
                    val m = reAccountValue.matcher(line)
                    var matchFound = false
                    while (m.find()) {
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

                        val floatVal = value.toFloat()
                        val currentAccount = requireNotNull(lastAccount) { "No current account" }
                        currentAccount.addAmount(floatVal, currency)
                        totalPostings++

                        for (syn in syntheticAccounts) {
                            syn.addAmount(floatVal, currency)
                        }
                    }

                    if (matchFound) {
                        syntheticAccounts.clear()
                        state = ParserState.EXPECTING_ACCOUNT
                    }
                }
            }
        }

        return ParseResult(accounts, totalPostings)
    }

    companion object {
        private val reComment = Pattern.compile("^\\s*;")
        private val reAccountName =
            Pattern.compile("/register\\?q=inacct%3A([a-zA-Z0-9%]+)\"")
        private val reAccountValue = Pattern.compile(
            "<span class=\"[^\"]*\\bamount\\b[^\"]*\">\\s*([-+]?[\\d.,]+)(?:\\s+(\\S+))?</span>"
        )
        private val reDecimalPoint = Pattern.compile("\\.\\d\\d?$")
        private val reDecimalComma = Pattern.compile(",\\d\\d?$")

        /**
         * Ensures an account exists in the map, creating synthetic parent accounts as needed.
         *
         * @param accountName The account name to ensure exists
         * @param map Map of account names to LedgerAccount instances
         * @param createdAccounts List to track created synthetic accounts
         * @return The existing or newly created account
         */
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
            map[accountName] = acc
            return acc
        }
    }
}

/**
 * Exception thrown when account parsing fails.
 */
class AccountParseException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
