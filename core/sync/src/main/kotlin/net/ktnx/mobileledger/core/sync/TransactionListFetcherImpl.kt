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

package net.ktnx.mobileledger.core.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.core.domain.model.API
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.network.HledgerClient
import net.ktnx.mobileledger.core.network.NetworkAuthenticationException
import net.ktnx.mobileledger.core.network.NetworkHttpException
import net.ktnx.mobileledger.core.network.NetworkNotFoundException
import net.ktnx.mobileledger.core.network.json.ApiNotSupportedException
import net.ktnx.mobileledger.core.network.json.TransactionListParser

/**
 * Implementation of TransactionListFetcher using HledgerClient.
 */
@Singleton
class TransactionListFetcherImpl @Inject constructor(
    private val hledgerClient: HledgerClient
) : TransactionListFetcher {

    override suspend fun fetch(
        profile: Profile,
        expectedPostingsCount: Int,
        onProgress: suspend (Int, Int) -> Unit
    ): List<Transaction>? {
        val apiVersion = API.valueOf(profile.apiVersion)
        return when (apiVersion) {
            API.auto -> fetchAnyVersion(profile, expectedPostingsCount, onProgress)
            API.v1_32, API.v1_40, API.v1_50 -> fetchForVersion(profile, apiVersion, expectedPostingsCount, onProgress)
        }
    }

    private suspend fun fetchAnyVersion(
        profile: Profile,
        expectedPostingsCount: Int,
        onProgress: suspend (Int, Int) -> Unit
    ): List<Transaction>? {
        for (ver in API.allVersions) {
            try {
                return fetchForVersion(profile, ver, expectedPostingsCount, onProgress)
            } catch (e: Exception) {
                logcat { "Error during transaction list retrieval using API ${ver.description}: ${e.asLog()}" }
            }
        }
        throw ApiNotSupportedException()
    }

    private suspend fun fetchForVersion(
        profile: Profile,
        apiVersion: API,
        expectedPostingsCount: Int,
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
                    is NetworkNotFoundException -> null

                    is NetworkAuthenticationException ->
                        throw NetworkHttpException(401, error.message ?: "Authentication required")

                    is NetworkHttpException -> throw error

                    else -> throw error
                }
            }
        )
    }
}
