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
 * HTTP client interface for hledger-web communication.
 *
 * This interface abstracts all HTTP operations needed to interact with hledger-web servers
 * using the JSON API (v1_32+).
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
     * Close the HTTP client and release resources.
     *
     * Should be called when the client is no longer needed.
     */
    fun close()
}
