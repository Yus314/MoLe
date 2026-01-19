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

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.setCookie
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import net.ktnx.mobileledger.TemporaryAuthData
import net.ktnx.mobileledger.domain.model.Profile

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
                throw ApiNotSupportedException(
                    "API not supported: HTTP ${response.status.value}",
                    responseBody
                )
            }

            401 -> throw AuthenticationException("Authentication required")

            else -> throw HttpException(
                response.status.value,
                "HTTP ${response.status.value}: ${response.status.description}"
            )
        }
    }.onFailure { e ->
        logcat(LogPriority.ERROR) { "PUT request failed: ${e.asLog()}" }
    }

    override suspend fun postForm(
        profile: Profile,
        path: String,
        formData: List<Pair<String, String>>,
        cookies: Map<String, String>,
        temporaryAuth: TemporaryAuthData?
    ): Result<FormPostResponse> = runCatching {
        val url = buildUrl(profile, path, temporaryAuth)
        logcat { "POST form to $url" }

        val response: HttpResponse = httpClient.submitForm(
            url = url,
            formParameters = Parameters.build {
                formData.forEach { (key, value) ->
                    append(key, value)
                }
            }
        ) {
            configureAuth(profile, temporaryAuth)
            if (cookies.isNotEmpty()) {
                val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
                header(HttpHeaders.Cookie, cookieHeader)
            }
        }

        val responseCookies = response.setCookie().associate { it.name to it.value }

        FormPostResponse(
            statusCode = response.status.value,
            body = response.bodyAsText(),
            cookies = responseCookies
        )
    }.onFailure { e ->
        logcat(LogPriority.ERROR) { "POST form request failed: ${e.asLog()}" }
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

            401 -> throw AuthenticationException("Authentication required")

            404 -> throw NotFoundException("Resource not found")

            else -> throw HttpException(
                response.status.value,
                "HTTP ${response.status.value}: ${response.status.description}"
            )
        }
    }
}

/**
 * Exception thrown when the API version is not supported.
 */
class ApiNotSupportedException(
    message: String,
    val responseBody: String? = null
) : Exception(message)

/**
 * Exception thrown when authentication fails.
 */
class AuthenticationException(message: String) : Exception(message)

/**
 * Exception thrown when a resource is not found.
 */
class NotFoundException(message: String) : Exception(message)

/**
 * General HTTP exception with status code.
 */
class HttpException(
    val statusCode: Int,
    message: String
) : Exception(message)
