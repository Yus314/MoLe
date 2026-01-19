/*
 * Copyright © 2025 Damyan Ivanov.
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

package net.ktnx.mobileledger.fake

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import net.ktnx.mobileledger.TemporaryAuthData
import net.ktnx.mobileledger.domain.model.Profile
import net.ktnx.mobileledger.network.FormPostResponse
import net.ktnx.mobileledger.network.HledgerClient

/**
 * Fake implementation of [HledgerClient] for testing.
 *
 * This fake allows tests to:
 * - Configure responses for specific endpoints
 * - Verify requests were made with correct parameters
 * - Simulate errors and failures
 */
class FakeHledgerClient : HledgerClient {

    /**
     * Map of endpoint paths to response content for GET requests.
     */
    val getResponses: MutableMap<String, String> = mutableMapOf()

    /**
     * Map of endpoint paths to Result for GET requests (for more control).
     */
    val getResults: MutableMap<String, Result<InputStream>> = mutableMapOf()

    /**
     * Map of endpoint paths to Result for PUT JSON requests.
     */
    val putJsonResults: MutableMap<String, Result<Unit>> = mutableMapOf()

    /**
     * Map of endpoint paths to Result for POST form requests.
     */
    val postFormResults: MutableMap<String, Result<FormPostResponse>> = mutableMapOf()

    /**
     * Default response for PUT JSON requests if not in putJsonResults.
     */
    var defaultPutJsonResult: Result<Unit> = Result.success(Unit)

    /**
     * Default response for POST form requests if not in postFormResults.
     */
    var defaultPostFormResult: Result<FormPostResponse> = Result.success(
        FormPostResponse(200, "", emptyMap())
    )

    /**
     * If set, all requests will fail with this exception.
     */
    var shouldFailWith: Exception? = null

    /**
     * Delay in milliseconds before returning responses.
     */
    var networkDelay: Long = 0

    /**
     * History of all requests made.
     */
    val requestHistory: MutableList<RequestRecord> = mutableListOf()

    /**
     * Record of a request.
     */
    data class RequestRecord(
        val method: String,
        val path: String,
        val profile: Profile,
        val temporaryAuth: TemporaryAuthData?,
        val body: String? = null,
        val formData: List<Pair<String, String>>? = null,
        val cookies: Map<String, String>? = null
    )

    override suspend fun get(profile: Profile, path: String, temporaryAuth: TemporaryAuthData?): Result<InputStream> {
        if (networkDelay > 0) {
            kotlinx.coroutines.delay(networkDelay)
        }

        requestHistory.add(RequestRecord("GET", path, profile, temporaryAuth))

        shouldFailWith?.let { return Result.failure(it) }

        // Check for explicit Result first
        getResults[path]?.let { return it }

        // Fall back to string responses
        val responseContent = getResponses[path]
            ?: return Result.failure(Exception("No mock response for GET $path"))

        return Result.success(
            ByteArrayInputStream(responseContent.toByteArray(StandardCharsets.UTF_8))
        )
    }

    override suspend fun putJson(
        profile: Profile,
        path: String,
        body: String,
        temporaryAuth: TemporaryAuthData?
    ): Result<Unit> {
        if (networkDelay > 0) {
            kotlinx.coroutines.delay(networkDelay)
        }

        requestHistory.add(RequestRecord("PUT", path, profile, temporaryAuth, body = body))

        shouldFailWith?.let { return Result.failure(it) }

        return putJsonResults[path] ?: defaultPutJsonResult
    }

    override suspend fun postForm(
        profile: Profile,
        path: String,
        formData: List<Pair<String, String>>,
        cookies: Map<String, String>,
        temporaryAuth: TemporaryAuthData?
    ): Result<FormPostResponse> {
        if (networkDelay > 0) {
            kotlinx.coroutines.delay(networkDelay)
        }

        requestHistory.add(
            RequestRecord(
                "POST",
                path,
                profile,
                temporaryAuth,
                formData = formData,
                cookies = cookies
            )
        )

        shouldFailWith?.let { return Result.failure(it) }

        return postFormResults[path] ?: defaultPostFormResult
    }

    override fun close() {
        // No-op for fake
    }

    /**
     * Reset the fake to its initial state.
     */
    fun reset() {
        getResponses.clear()
        getResults.clear()
        putJsonResults.clear()
        postFormResults.clear()
        defaultPutJsonResult = Result.success(Unit)
        defaultPostFormResult = Result.success(FormPostResponse(200, "", emptyMap()))
        shouldFailWith = null
        networkDelay = 0
        requestHistory.clear()
    }

    /**
     * Get all requests for a specific path.
     */
    fun requestsForPath(path: String): List<RequestRecord> = requestHistory.filter { it.path == path }

    /**
     * Get all GET requests.
     */
    fun getRequests(): List<RequestRecord> = requestHistory.filter { it.method == "GET" }

    /**
     * Get all PUT requests.
     */
    fun putRequests(): List<RequestRecord> = requestHistory.filter { it.method == "PUT" }

    /**
     * Get all POST requests.
     */
    fun postRequests(): List<RequestRecord> = requestHistory.filter { it.method == "POST" }

    /**
     * Check if authentication was used for a specific request.
     */
    fun wasAuthUsed(requestIndex: Int): Boolean {
        val request = requestHistory.getOrNull(requestIndex) ?: return false
        return request.temporaryAuth?.useAuthentication == true ||
            request.profile.isAuthEnabled
    }
}
