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

/**
 * Network layer exceptions for HledgerClient operations.
 *
 * These exceptions are thrown by [HledgerClientImpl] when HTTP requests fail.
 * They are prefixed with "Network" to distinguish from [net.ktnx.mobileledger.json.ApiNotSupportedException]
 * which is used by JSON parsers.
 */

/**
 * Thrown when the server returns HTTP 400 (Bad Request) or 405 (Method Not Allowed),
 * indicating that the API version is not supported by the server.
 *
 * @property responseBody The response body from the server, if available
 */
class NetworkApiNotSupportedException(
    message: String,
    val responseBody: String? = null
) : Exception(message)

/**
 * Thrown when authentication fails (HTTP 401 Unauthorized).
 */
class NetworkAuthenticationException(message: String) : Exception(message)

/**
 * Thrown when a resource is not found (HTTP 404 Not Found).
 */
class NetworkNotFoundException(message: String) : Exception(message)

/**
 * General HTTP exception with status code for other error responses.
 *
 * @property statusCode The HTTP status code
 */
class NetworkHttpException(
    val statusCode: Int,
    message: String
) : Exception(message)
