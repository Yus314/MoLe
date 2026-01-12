/*
 * Copyright © 2023 Damyan Ivanov.
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

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.regex.Pattern
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.json.API
import net.ktnx.mobileledger.json.ApiNotSupportedException
import net.ktnx.mobileledger.json.Gateway
import net.ktnx.mobileledger.model.LedgerTransaction
import net.ktnx.mobileledger.utils.Globals
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import net.ktnx.mobileledger.utils.NetworkUtil
import net.ktnx.mobileledger.utils.SimpleDate
import net.ktnx.mobileledger.utils.UrlEncodedFormData

class SendTransactionTask(
    private val taskCallback: TaskCallback,
    private val mProfile: Profile,
    private val transaction: LedgerTransaction,
    private val simulate: Boolean
) : Thread() {

    private var error: String? = null
    private var token: String? = null
    private var session: String? = null

    @Throws(IOException::class, ApiNotSupportedException::class)
    private fun sendOK(apiVersion: API) {
        val http = NetworkUtil.prepareConnection(mProfile, "add")
        http.requestMethod = "PUT"
        http.setRequestProperty("Content-Type", "application/json")
        http.setRequestProperty("Accept", "*/*")

        val gateway = Gateway.forApiVersion(apiVersion)
        val body = gateway.transactionSaveRequest(transaction)

        Logger.debug("network", "Sending using API $apiVersion")
        sendRequest(http, body)
    }

    @Throws(IOException::class, ApiNotSupportedException::class)
    private fun sendRequest(http: HttpURLConnection, body: String) {
        if (simulate) {
            Logger.debug("network", "The request would be: $body")
            try {
                sleep(1500)
                if (Math.random() > 0.3) {
                    throw RuntimeException("Simulated test exception")
                }
            } catch (ex: InterruptedException) {
                Logger.debug("network", ex.toString())
            }
            return
        }

        val bodyBytes = body.toByteArray(StandardCharsets.UTF_8)
        http.doOutput = true
        http.doInput = true
        http.addRequestProperty("Content-Length", bodyBytes.size.toString())

        Logger.debug("network", "request header: ${http.requestProperties}")

        http.outputStream.use { req ->
            Logger.debug("network", "Request body: $body")
            req.write(bodyBytes)

            val responseCode = http.responseCode
            Logger.debug(
                "network",
                String.format(Locale.US, "Response: %d %s", responseCode, http.responseMessage)
            )

            http.errorStream?.use { resp ->
                when (responseCode) {
                    200, 201 -> { /* success */ }

                    400, 405 -> {
                        val reader = BufferedReader(InputStreamReader(resp))
                        val errorLines = StringBuilder()
                        var count = 0
                        while (count <= 5) {
                            val line = reader.readLine() ?: break
                            Logger.debug("network", line)
                            if (errorLines.isNotEmpty()) {
                                errorLines.append("\n")
                            }
                            errorLines.append(line)
                            count++
                        }
                        throw ApiNotSupportedException(errorLines.toString())
                    }

                    else -> {
                        val reader = BufferedReader(InputStreamReader(resp))
                        val line = reader.readLine()
                        Logger.debug("network", "Response content: $line")
                        throw IOException(String.format(Locale.ROOT, "Error response code %d", responseCode))
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun legacySendOK(): Boolean {
        val http = NetworkUtil.prepareConnection(mProfile, "add")
        http.requestMethod = "POST"
        http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        http.setRequestProperty("Accept", "*/*")
        if (!session.isNullOrEmpty()) {
            http.setRequestProperty("Cookie", String.format(Locale.ROOT, "_SESSION=%s", session))
        }
        http.doOutput = true
        http.doInput = true

        val params = UrlEncodedFormData()
        params.addPair("_formid", "identify-add")
        token?.let { params.addPair("_token", it) }

        val transactionDate = transaction.getDateIfAny() ?: SimpleDate.today()

        params.addPair("date", Globals.formatLedgerDate(transactionDate))
        params.addPair("description", transaction.description ?: "")
        for (acc in transaction.accounts) {
            params.addPair("account", acc.accountName)
            if (acc.isAmountSet) {
                params.addPair("amount", String.format(Locale.US, "%1.2f", acc.amount))
            } else {
                params.addPair("amount", "")
            }
        }

        val body = params.toString()
        http.addRequestProperty("Content-Length", body.length.toString())

        Logger.debug("network", "request header: ${http.requestProperties}")

        http.outputStream.use { req ->
            Logger.debug("network", "Request body: $body")
            req.write(body.toByteArray(StandardCharsets.US_ASCII))

            http.inputStream.use { resp ->
                Logger.debug("update_accounts", http.responseCode.toString())
                when (http.responseCode) {
                    303 -> return true

                    // everything is fine
                    200 -> {
                        // get the new cookie
                        val reSessionCookie = Pattern.compile("_SESSION=([^;]+);.*")
                        val header = http.headerFields
                        val cookieHeader = header["Set-Cookie"]
                        if (cookieHeader != null) {
                            val cookie = cookieHeader[0]
                            val m = reSessionCookie.matcher(cookie)
                            if (m.matches()) {
                                session = m.group(1)
                                Logger.debug("network", "new session is $session")
                            } else {
                                Logger.debug("network", "set-cookie: $cookie")
                                Log.w("network", "Response Set-Cookie headers is not a _SESSION one")
                            }
                        } else {
                            Log.w("network", "Response has no Set-Cookie header")
                        }

                        // the token needs to be updated
                        val reader = BufferedReader(InputStreamReader(resp))
                        val re = Pattern.compile("<input type=\"hidden\" name=\"_token\" value=\"([^\"]+)\">")
                        var line: String? = reader.readLine()
                        while (line != null) {
                            val m = re.matcher(line)
                            if (m.matches()) {
                                token = m.group(1)
                                Logger.debug("save-transaction", line)
                                Logger.debug("save-transaction", "Token=$token")
                                return false // retry
                            }
                            line = reader.readLine()
                        }
                        throw IOException("Can't find _token string")
                    }

                    else -> throw IOException(String.format(Locale.ROOT, "Error response code %d", http.responseCode))
                }
            }
        }
    }

    override fun run() {
        error = null
        try {
            val profileApiVersion = API.valueOf(mProfile.apiVersion)
            when (profileApiVersion) {
                API.auto -> {
                    var sendOK = false
                    for (ver in API.allVersions) {
                        Logger.debug("network", "Trying version $ver")
                        try {
                            sendOK(ver)
                            sendOK = true
                            Logger.debug("network", "Version $ver request succeeded")
                            break
                        } catch (e: ApiNotSupportedException) {
                            Logger.debug("network", "Version $ver not supported: ${e.message}", e)
                        }
                    }

                    if (!sendOK) {
                        Logger.debug("network", "Trying HTML form emulation")
                        legacySendOkWithRetry()
                    }
                }

                API.html -> legacySendOkWithRetry()

                API.v1_14, API.v1_15, API.v1_19_1, API.v1_23 -> sendOK(profileApiVersion)

                else -> error("Unexpected API version: $profileApiVersion")
            }
        } catch (e: Exception) {
            Logger.warn("SendTransaction", "Error sending transaction", e)
            error = e.message
        }

        Misc.onMainThread { taskCallback.onTransactionSaveDone(error, transaction) }
    }

    @Throws(IOException::class)
    private fun legacySendOkWithRetry() {
        var tried = 0
        while (!legacySendOK()) {
            tried++
            if (tried >= 2) {
                throw IOException(String.format(Locale.ROOT, "aborting after %d tries", tried))
            }
            try {
                sleep(100)
            } catch (e: InterruptedException) {
                Logger.debug("SendTransaction", "Retry interrupted", e)
                break
            }
        }
    }
}
