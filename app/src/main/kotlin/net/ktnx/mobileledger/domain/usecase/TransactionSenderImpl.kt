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

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.core.common.di.IoDispatcher
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.Transaction
import net.ktnx.mobileledger.core.network.HledgerClient
import net.ktnx.mobileledger.core.network.NetworkApiNotSupportedException
import net.ktnx.mobileledger.core.network.json.API
import net.ktnx.mobileledger.core.network.json.ApiNotSupportedException
import net.ktnx.mobileledger.core.network.json.Gateway
import net.ktnx.mobileledger.service.CurrencyFormatter

/**
 * Pure Coroutines implementation of [TransactionSender].
 *
 * This implementation uses JSON API only (v1_32+).
 * HTML form fallback has been removed as of the v1_32+ only support update.
 *
 * ## Key Features
 * - No Thread usage (Thread.start(), Thread.join() not used)
 * - All I/O runs via withContext(ioDispatcher)
 * - Fast cancellation via ensureActive()
 * - Result<Unit> for success/failure handling
 */
@Singleton
class TransactionSenderImpl @Inject constructor(
    private val hledgerClient: HledgerClient,
    private val currencyFormatter: CurrencyFormatter,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSender {

    override suspend fun send(profile: Profile, transaction: Transaction, simulate: Boolean): Result<Unit> {
        profile.id ?: return Result.failure(IllegalStateException("Cannot send from unsaved profile"))
        return sendInternalWithDomain(profile, transaction, simulate)
    }

    /**
     * Send transaction using domain models directly.
     * Uses JSON API with domain model serialization (v1_32+ only).
     */
    private suspend fun sendInternalWithDomain(
        profile: Profile,
        transaction: Transaction,
        simulate: Boolean
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            coroutineContext.ensureActive()

            val profileApiVersion = API.valueOf(profile.apiVersion)
            when (profileApiVersion) {
                API.auto -> {
                    var sendOK = false
                    for (ver in API.allVersions) {
                        coroutineContext.ensureActive()
                        logcat { "Trying version $ver" }
                        try {
                            sendOKViaAPIWithDomain(profile, transaction, ver, simulate)
                            sendOK = true
                            logcat { "Version $ver request succeeded" }
                            break
                        } catch (e: ApiNotSupportedException) {
                            logcat { "Version $ver not supported: ${e.message}" }
                        }
                    }

                    if (!sendOK) {
                        throw ApiNotSupportedException(
                            "No supported API version found. Server must support hledger-web 1.32 or later."
                        )
                    }
                }

                API.v1_32, API.v1_40, API.v1_50 -> {
                    sendOKViaAPIWithDomain(profile, transaction, profileApiVersion, simulate)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Error sending transaction: ${e.asLog()}" }
            Result.failure(e)
        }
    }

    /**
     * Send transaction via JSON API using domain model directly.
     */
    private suspend fun sendOKViaAPIWithDomain(
        profile: Profile,
        transaction: Transaction,
        apiVersion: API,
        simulate: Boolean
    ) {
        coroutineContext.ensureActive()

        val gateway = Gateway.forApiVersion(apiVersion)
        val body = gateway.transactionSaveRequest(transaction, currencyFormatter)

        logcat { "Sending using API $apiVersion" }

        if (simulate) {
            logcat { "The request would be: $body" }
            delay(1500)
            if (Math.random() > 0.3) {
                throw RuntimeException("Simulated test exception")
            }
            return
        }

        logcat { "Request body: $body" }

        val result = hledgerClient.putJson(profile, "add", body)
        result.fold(
            onSuccess = {
                logcat { "PUT request succeeded" }
            },
            onFailure = { error ->
                when (error) {
                    is NetworkApiNotSupportedException -> {
                        logcat { "API not supported: ${error.message}" }
                        throw ApiNotSupportedException(error.responseBody ?: error.message ?: "")
                    }

                    else -> throw error
                }
            }
        )
    }
}
