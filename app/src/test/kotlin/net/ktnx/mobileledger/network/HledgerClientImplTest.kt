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

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.domain.model.FutureDates
import net.ktnx.mobileledger.core.domain.model.Profile
import net.ktnx.mobileledger.core.domain.model.ProfileAuthentication
import net.ktnx.mobileledger.core.domain.model.TemporaryAuthData
import net.ktnx.mobileledger.core.network.HledgerClientImpl
import net.ktnx.mobileledger.core.network.KtorClientFactory
import net.ktnx.mobileledger.core.network.NetworkApiNotSupportedException
import net.ktnx.mobileledger.core.network.NetworkAuthenticationException
import net.ktnx.mobileledger.core.network.NetworkNotFoundException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HledgerClientImplTest {

    private var mockEngine: MockEngine? = null
    private var client: HledgerClientImpl? = null

    @After
    fun tearDown() {
        client?.close()
    }

    @Test
    fun `get request returns success with content`() = runTest {
        val responseContent = """{"version": "1.32"}"""
        setupMockEngine { request ->
            when {
                request.url.encodedPath.endsWith("version") -> respond(
                    content = responseContent,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )

                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val profile = createTestProfile()
        val result = client!!.get(profile, "version")

        assertTrue(result.isSuccess)
        val inputStream = result.getOrNull()
        assertNotNull(inputStream)
        val content = inputStream!!.bufferedReader(StandardCharsets.UTF_8).readText()
        assertEquals(responseContent, content)
    }

    @Test
    fun `get request with trailing slash in url`() = runTest {
        setupMockEngine { request ->
            respond(
                content = "OK",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        val profile = createTestProfile(url = "https://example.com/")
        val result = client!!.get(profile, "version")

        assertTrue(result.isSuccess)
        val lastRequest = mockEngine!!.requestHistory.last()
        assertEquals("https://example.com/version", lastRequest.url.toString())
    }

    @Test
    fun `get request without trailing slash in url`() = runTest {
        setupMockEngine { request ->
            respond(
                content = "OK",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        val profile = createTestProfile(url = "https://example.com")
        val result = client!!.get(profile, "version")

        assertTrue(result.isSuccess)
        val lastRequest = mockEngine!!.requestHistory.last()
        assertEquals("https://example.com/version", lastRequest.url.toString())
    }

    @Test
    fun `get request includes basic auth header when profile has authentication`() = runTest {
        setupMockEngine { request ->
            respond(
                content = "OK",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        val profile = createTestProfile(
            authentication = ProfileAuthentication(user = "testuser", password = "testpass")
        )
        val result = client!!.get(profile, "version")

        assertTrue(result.isSuccess)
        val lastRequest = mockEngine!!.requestHistory.last()
        val authHeader = lastRequest.headers[HttpHeaders.Authorization]
        assertNotNull(authHeader)
        assertTrue(authHeader!!.startsWith("Basic "))
    }

    @Test
    fun `get request uses temporary auth when provided`() = runTest {
        setupMockEngine { request ->
            respond(
                content = "OK",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        val profile = createTestProfile(authentication = null)
        val tempAuth = TemporaryAuthData(
            url = "https://temp.example.com",
            useAuthentication = true,
            authUser = "tempuser",
            authPassword = "temppass"
        )
        val result = client!!.get(profile, "version", tempAuth)

        assertTrue(result.isSuccess)
        val lastRequest = mockEngine!!.requestHistory.last()
        // Should use temp auth URL
        assertEquals("https://temp.example.com/version", lastRequest.url.toString())
        // Should have auth header
        val authHeader = lastRequest.headers[HttpHeaders.Authorization]
        assertNotNull(authHeader)
    }

    @Test
    fun `get request fails with authentication exception on 401`() = runTest {
        setupMockEngine { request ->
            respondError(HttpStatusCode.Unauthorized)
        }

        val profile = createTestProfile()
        val result = client!!.get(profile, "version")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkAuthenticationException)
    }

    @Test
    fun `get request fails with not found exception on 404`() = runTest {
        setupMockEngine { request ->
            respondError(HttpStatusCode.NotFound)
        }

        val profile = createTestProfile()
        val result = client!!.get(profile, "version")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkNotFoundException)
    }

    @Test
    fun `put json request succeeds with 201 response`() = runTest {
        setupMockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.Created,
                headers = headersOf()
            )
        }

        val profile = createTestProfile()
        val jsonBody = """{"date": "2025-01-20", "description": "Test"}"""
        val result = client!!.putJson(profile, "add", jsonBody)

        assertTrue(result.isSuccess)
        val lastRequest = mockEngine!!.requestHistory.last()
        assertEquals("https://example.com/add", lastRequest.url.toString())
    }

    @Test
    fun `put json request fails with api not supported on 400`() = runTest {
        setupMockEngine { request ->
            respond(
                content = "Bad Request",
                status = HttpStatusCode.BadRequest,
                headers = headersOf()
            )
        }

        val profile = createTestProfile()
        val result = client!!.putJson(profile, "add", "{}")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkApiNotSupportedException)
    }

    @Test
    fun `put json request fails with api not supported on 405`() = runTest {
        setupMockEngine { request ->
            respond(
                content = "Method Not Allowed",
                status = HttpStatusCode.MethodNotAllowed,
                headers = headersOf()
            )
        }

        val profile = createTestProfile()
        val result = client!!.putJson(profile, "add", "{}")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is NetworkApiNotSupportedException)
    }

    private fun setupMockEngine(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
        mockEngine = MockEngine(handler)
        val httpClient = KtorClientFactory.createWithEngine(mockEngine!!, enableLogging = false)
        client = HledgerClientImpl(httpClient)
    }

    private fun createTestProfile(
        id: Long? = 1L,
        name: String = "Test Profile",
        uuid: String = "test-uuid",
        url: String = "https://example.com",
        authentication: ProfileAuthentication? = null,
        orderNo: Int = 0,
        permitPosting: Boolean = true,
        theme: Int = -1,
        preferredAccountsFilter: String? = null,
        futureDates: FutureDates = FutureDates.None,
        apiVersion: Int = 0,
        showCommodityByDefault: Boolean = false,
        defaultCommodity: String? = null,
        showCommentsByDefault: Boolean = true
    ): Profile = Profile(
        id = id,
        name = name,
        uuid = uuid,
        url = url,
        authentication = authentication,
        orderNo = orderNo,
        permitPosting = permitPosting,
        theme = theme,
        preferredAccountsFilter = preferredAccountsFilter,
        futureDates = futureDates,
        apiVersion = apiVersion,
        showCommodityByDefault = showCommodityByDefault,
        defaultCommodity = defaultCommodity,
        showCommentsByDefault = showCommentsByDefault
    )
}
