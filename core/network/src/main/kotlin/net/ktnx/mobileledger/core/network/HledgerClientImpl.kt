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

package net.ktnx.mobileledger.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.TemporaryAuthData

/**
 * Ktor-based implementation of [HledgerClient].
 *
 * This implementation uses OkHttp as the underlying HTTP engine for Android optimization.
 */
@Singleton
class HledgerClientImpl @Inject constructor(
    private val httpClient: HttpClient
) : HledgerClient {

    override suspend fun get(profile: Profile, path: String, temporaryAuth: TemporaryAuthData?): Result<InputStream> =
        runCatching {
            val url = buildUrl(profile, path, temporaryAuth)
            logcat { "GET $url" }

            val response: HttpResponse = httpClient.get(url) {
                configureAuth(profile, temporaryAuth)
                header(HttpHeaders.AcceptCharset, "UTF-8")
            }

            handleResponse(response)
        }.onFailure { e ->
            logcat(LogPriority.ERROR) { "GET request failed: ${e.asLog()}" }
        }

    override suspend fun putJson(
        profile: Profile,
        path: String,
        body: String,
        temporaryAuth: TemporaryAuthData?
    ): Result<Unit> = runCatching {
        val url = buildUrl(profile, path, temporaryAuth)
        logcat { "PUT $url" }

        val response: HttpResponse = httpClient.put(url) {
            configureAuth(profile, temporaryAuth)
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(body)
        }

        when (response.status.value) {
            in 200..299 -> Unit

            400, 405 -> {
                val responseBody = response.bodyAsText()
                throw NetworkApiNotSupportedException(
                    "API not supported: HTTP ${response.status.value}",
                    responseBody
                )
            }

            401 -> throw NetworkAuthenticationException("Authentication required")

            else -> throw NetworkHttpException(
                response.status.value,
                "HTTP ${response.status.value}: ${response.status.description}"
            )
        }
    }.onFailure { e ->
        logcat(LogPriority.ERROR) { "PUT request failed: ${e.asLog()}" }
    }

    override fun close() {
        httpClient.close()
    }

    private fun buildUrl(profile: Profile, path: String, temporaryAuth: TemporaryAuthData?): String {
        val baseUrl = temporaryAuth?.url ?: profile.url
        val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return normalizedBase + path
    }

    private fun io.ktor.client.request.HttpRequestBuilder.configureAuth(
        profile: Profile,
        temporaryAuth: TemporaryAuthData?
    ) {
        val useAuth: Boolean
        val user: String
        val password: String

        if (temporaryAuth != null) {
            useAuth = temporaryAuth.useAuthentication
            user = temporaryAuth.authUser
            password = temporaryAuth.authPassword
        } else {
            useAuth = profile.isAuthEnabled
            user = profile.authentication?.user ?: ""
            password = profile.authentication?.password ?: ""
        }

        if (useAuth && user.isNotEmpty()) {
            val credentials = "$user:$password"
            val encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.toByteArray(StandardCharsets.UTF_8))
            header(HttpHeaders.Authorization, "Basic $encodedCredentials")
        }
    }

    private suspend fun handleResponse(response: HttpResponse): InputStream {
        when (response.status.value) {
            in 200..299 -> {
                val bodyText = response.bodyAsText()
                return ByteArrayInputStream(bodyText.toByteArray(StandardCharsets.UTF_8))
            }

            401 -> throw NetworkAuthenticationException("Authentication required")

            404 -> throw NetworkNotFoundException("Resource not found")

            else -> throw NetworkHttpException(
                response.status.value,
                "HTTP ${response.status.value}: ${response.status.description}"
            )
        }
    }
}
