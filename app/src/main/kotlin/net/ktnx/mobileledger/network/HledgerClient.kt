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

package net.ktnx.mobileledger.network

import java.io.InputStream
import net.ktnx.mobileledger.TemporaryAuthData
import net.ktnx.mobileledger.domain.model.Profile

/**
 * Response from a form POST request.
 */
data class FormPostResponse(
    val statusCode: Int,
    val body: String,
    val cookies: Map<String, String>
)

/**
 * HTTP client interface for hledger-web communication.
 *
 * This interface abstracts all HTTP operations needed to interact with hledger-web servers,
 * supporting both JSON API and legacy HTML form modes.
 *
 * Authentication is handled explicitly per-request, allowing for:
 * - Profile-based authentication (normal operation)
 * - Temporary authentication (connection testing during profile editing)
 * - No authentication (public servers)
 */
interface HledgerClient {

    /**
     * Perform a GET request to a hledger-web endpoint.
     *
     * @param profile The profile containing server URL and authentication settings
     * @param path The endpoint path (e.g., "version", "accounts", "transactions")
     * @param temporaryAuth Optional temporary authentication for connection testing
     * @return Result containing the response body as an InputStream, or an error
     */
    suspend fun get(profile: Profile, path: String, temporaryAuth: TemporaryAuthData? = null): Result<InputStream>

    /**
     * Perform a PUT request with a JSON body.
     *
     * Used for saving transactions via the hledger-web JSON API.
     *
     * @param profile The profile containing server URL and authentication settings
     * @param path The endpoint path (typically "add")
     * @param body The JSON body to send
     * @param temporaryAuth Optional temporary authentication for connection testing
     * @return Result indicating success or containing an error
     */
    suspend fun putJson(
        profile: Profile,
        path: String,
        body: String,
        temporaryAuth: TemporaryAuthData? = null
    ): Result<Unit>

    /**
     * Perform a POST request with form-encoded data.
     *
     * Used for legacy HTML form submission to hledger-web servers
     * that don't support the JSON API.
     *
     * @param profile The profile containing server URL and authentication settings
     * @param path The endpoint path (typically "add")
     * @param formData List of key-value pairs for form fields. Uses List to support
     *                 multiple values for the same key (e.g., multiple "account" fields)
     * @param cookies Optional cookies to send (e.g., session cookie)
     * @param temporaryAuth Optional temporary authentication for connection testing
     * @return Result containing the response with status code, body, and cookies
     */
    suspend fun postForm(
        profile: Profile,
        path: String,
        formData: List<Pair<String, String>>,
        cookies: Map<String, String> = emptyMap(),
        temporaryAuth: TemporaryAuthData? = null
    ): Result<FormPostResponse>

    /**
     * Close the HTTP client and release resources.
     *
     * Should be called when the client is no longer needed.
     */
    fun close()
}
