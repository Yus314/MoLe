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
import kotlinx.serialization.SerializationException
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.core.domain.model.Account
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.network.HledgerClient
import net.ktnx.mobileledger.core.network.NetworkAuthenticationException
import net.ktnx.mobileledger.core.network.NetworkHttpException
import net.ktnx.mobileledger.core.network.NetworkNotFoundException
import net.ktnx.mobileledger.core.network.json.API
import net.ktnx.mobileledger.core.network.json.AccountListParser
import net.ktnx.mobileledger.core.network.json.ApiNotSupportedException

/**
 * Implementation of AccountListFetcher using HledgerClient.
 */
@Singleton
class AccountListFetcherImpl @Inject constructor(
    private val hledgerClient: HledgerClient
) : AccountListFetcher {

    override suspend fun fetch(profile: Profile): AccountFetchResult? {
        val apiVersion = API.valueOf(profile.apiVersion)
        return when (apiVersion) {
            API.auto -> fetchAnyVersion(profile)
            API.v1_32, API.v1_40, API.v1_50 -> fetchForVersion(profile, apiVersion)
        }
    }

    private suspend fun fetchAnyVersion(profile: Profile): AccountFetchResult? {
        for (ver in API.allVersions) {
            try {
                return fetchForVersion(profile, ver)
            } catch (e: SerializationException) {
                logcat { "Error during account list retrieval using API ${ver.description}: ${e.asLog()}" }
            }
        }
        throw ApiNotSupportedException()
    }

    private suspend fun fetchForVersion(profile: Profile, version: API): AccountFetchResult? {
        coroutineContext.ensureActive()

        val result = hledgerClient.get(profile, "accounts")

        return result.fold(
            onSuccess = { inputStream ->
                val list = ArrayList<Account>()
                val existingNames = HashSet<String>()
                var expectedPostingsCount = 0

                inputStream.use { resp ->
                    coroutineContext.ensureActive()
                    val parser = AccountListParser.forApiVersion(version, resp)

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

                AccountFetchResult(list, expectedPostingsCount)
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

    /**
     * Ensures parent accounts exist for all accounts in the list.
     *
     * hledger API returns a flat list of accounts, so parent accounts may not be
     * explicitly included. For example: "Assets:Cash" exists but "Assets" may not.
     * This method generates missing parent accounts.
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
                    parentName?.takeIf { idx > 0 }?.substring(0, idx)
                }
            }
        }

        accounts.addAll(toAdd)
    }
}
