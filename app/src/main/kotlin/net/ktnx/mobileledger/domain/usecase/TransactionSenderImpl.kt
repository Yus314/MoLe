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

import java.io.IOException
import java.util.Locale
import java.util.regex.Pattern
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
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.domain.model.Transaction
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.json.ApiNotSupportedException
import net.ktnx.mobileledger.json.Gateway
import net.ktnx.mobileledger.network.ApiNotSupportedException as KtorApiNotSupportedException
import net.ktnx.mobileledger.network.HledgerClient
import net.ktnx.mobileledger.utils.Globals

/**
 * Pure Coroutines implementation of [TransactionSender].
 *
 * This implementation converts SendTransactionTask's Thread logic to pure suspend functions,
 * enabling proper integration with ViewModels and deterministic testing via TestDispatcher.
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionSender {

    // Session state for legacy HTML form submission
    private var token: String? = null
    private var session: String? = null

    override suspend fun send(profile: Profile, transaction: Transaction, simulate: Boolean): Result<Unit> {
        profile.id ?: return Result.failure(IllegalStateException("Cannot send from unsaved profile"))
        return sendInternalWithDomain(profile, transaction, simulate)
    }

    /**
     * Send transaction using domain models directly.
     * Uses JSON API with domain model serialization, falls back to HTML form.
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
                        logcat { "Trying HTML form emulation" }
                        legacySendOkWithRetry(profile, transaction, simulate)
                    }
                }

                API.html -> {
                    legacySendOkWithRetry(profile, transaction, simulate)
                }

                API.v1_14, API.v1_15, API.v1_19_1, API.v1_23, API.v1_32, API.v1_40, API.v1_50 -> {
                    sendOKViaAPIWithDomain(profile, transaction, profileApiVersion, simulate)
                }

                else -> error("Unexpected API version: $profileApiVersion")
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
        val body = gateway.transactionSaveRequest(transaction)

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
                    is KtorApiNotSupportedException -> {
                        logcat { "API not supported: ${error.message}" }
                        throw ApiNotSupportedException(error.responseBody ?: error.message ?: "")
                    }

                    else -> throw error
                }
            }
        )
    }

    /**
     * Send transaction via legacy HTML form
     */
    private suspend fun legacySendOK(profile: Profile, transaction: Transaction, simulate: Boolean): Boolean {
        coroutineContext.ensureActive()

        val formData = mutableListOf<Pair<String, String>>()
        formData.add("_formid" to "identify-add")
        token?.let { formData.add("_token" to it) }

        formData.add("date" to Globals.formatLedgerDate(transaction.date))
        formData.add("description" to transaction.description)
        for (line in transaction.lines) {
            formData.add("account" to line.accountName)
            val amountStr = if (line.amount != null) {
                String.format(Locale.US, "%1.2f", line.amount)
            } else {
                ""
            }
            formData.add("amount" to amountStr)
        }

        logcat { "Form data: $formData" }

        if (simulate) {
            logcat { "The request would be: $formData" }
            delay(1500)
            return true
        }

        val cookies = if (!session.isNullOrEmpty()) {
            mapOf("_SESSION" to session!!)
        } else {
            emptyMap()
        }

        val result = hledgerClient.postForm(profile, "add", formData, cookies)

        return result.fold(
            onSuccess = { response ->
                logcat { "Response status: ${response.statusCode}" }
                when (response.statusCode) {
                    303 -> true

                    200 -> {
                        // Update session from cookies
                        val newSession = response.cookies["_SESSION"]
                        if (newSession != null) {
                            session = newSession
                            logcat { "new session is $session" }
                        } else {
                            logcat(LogPriority.WARN) { "Response has no _SESSION cookie" }
                        }

                        // Extract token from response body
                        val re = Pattern.compile("<input type=\"hidden\" name=\"_token\" value=\"([^\"]+)\">")
                        val lines = response.body.lines()
                        for (line in lines) {
                            coroutineContext.ensureActive()
                            val m = re.matcher(line)
                            if (m.matches()) {
                                token = m.group(1)
                                logcat { line }
                                logcat { "Token=$token" }
                                return@fold false // retry
                            }
                        }
                        throw IOException("Can't find _token string")
                    }

                    else -> throw IOException(
                        String.format(Locale.ROOT, "Error response code %d", response.statusCode)
                    )
                }
            },
            onFailure = { error ->
                throw error
            }
        )
    }

    /**
     * Send transaction via legacy HTML form with retry
     */
    private suspend fun legacySendOkWithRetry(profile: Profile, transaction: Transaction, simulate: Boolean) {
        var tried = 0
        while (!legacySendOK(profile, transaction, simulate)) {
            coroutineContext.ensureActive()
            tried++
            if (tried >= 2) {
                throw IOException(String.format(Locale.ROOT, "aborting after %d tries", tried))
            }
            delay(100)
        }
    }
}
