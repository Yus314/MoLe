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

package net.ktnx.mobileledger.domain.model

import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.text.ParseException
import kotlinx.coroutines.CancellationException
import net.ktnx.mobileledger.core.common.utils.SimpleDate
import net.ktnx.mobileledger.core.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T043: SyncError mapping tests
 * T045: isRetryable property tests
 */
class SyncErrorTest {

    // ==========================================
    // T043: SyncError mapping tests
    // ==========================================

    @Test
    fun `NetworkError has correct default message`() {
        val error = SyncError.NetworkError()
        assertEquals("ネットワークに接続できません", error.message)
    }

    @Test
    fun `NetworkError preserves custom message`() {
        val error = SyncError.NetworkError("カスタムメッセージ")
        assertEquals("カスタムメッセージ", error.message)
    }

    @Test
    fun `NetworkError preserves cause`() {
        val cause = IOException("Connection reset")
        val error = SyncError.NetworkError(cause = cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `TimeoutError has correct default message`() {
        val error = SyncError.TimeoutError()
        assertEquals("サーバーが応答しません", error.message)
    }

    @Test
    fun `TimeoutError has default timeout value`() {
        val error = SyncError.TimeoutError()
        assertEquals(30_000L, error.timeoutMs)
    }

    @Test
    fun `AuthenticationError has correct default message`() {
        val error = SyncError.AuthenticationError()
        assertEquals("認証に失敗しました。ユーザー名とパスワードを確認してください", error.message)
    }

    @Test
    fun `AuthenticationError has default httpCode 401`() {
        val error = SyncError.AuthenticationError()
        assertEquals(401, error.httpCode)
    }

    @Test
    fun `ServerError preserves message and httpCode`() {
        val error = SyncError.ServerError("Internal Server Error", 500)
        assertEquals("Internal Server Error", error.message)
        assertEquals(500, error.httpCode)
    }

    @Test
    fun `ServerError preserves serverMessage`() {
        val error = SyncError.ServerError(
            message = "Server Error",
            httpCode = 500,
            serverMessage = "Database connection failed"
        )
        assertEquals("Database connection failed", error.serverMessage)
    }

    @Test
    fun `ValidationError preserves field and details`() {
        val error = SyncError.ValidationError(
            message = "Validation failed",
            field = "amount",
            details = listOf("Amount must be positive", "Amount exceeds limit")
        )
        assertEquals("amount", error.field)
        assertEquals(2, error.details.size)
    }

    @Test
    fun `ParseError has correct default message`() {
        val error = SyncError.ParseError()
        assertEquals("データの解析に失敗しました", error.message)
    }

    @Test
    fun `ApiVersionError preserves detected version`() {
        val error = SyncError.ApiVersionError(
            detectedVersion = "1.0",
            supportedVersions = listOf("1.19", "1.26", "1.32")
        )
        assertEquals("1.0", error.detectedVersion)
        assertEquals(3, error.supportedVersions.size)
    }

    @Test
    fun `Cancelled has correct message`() {
        val error = SyncError.Cancelled
        assertEquals("処理がキャンセルされました", error.message)
    }

    @Test
    fun `UnknownError preserves cause`() {
        val cause = RuntimeException("Unexpected error")
        val error = SyncError.UnknownError(cause = cause)
        assertEquals(cause, error.cause)
    }

    // ==========================================
    // T045: isRetryable property tests
    // ==========================================

    @Test
    fun `NetworkError isRetryable is true`() {
        val error = SyncError.NetworkError()
        assertTrue("NetworkError should be retryable", error.isRetryable)
    }

    @Test
    fun `TimeoutError isRetryable is true`() {
        val error = SyncError.TimeoutError()
        assertTrue("TimeoutError should be retryable", error.isRetryable)
    }

    @Test
    fun `AuthenticationError isRetryable is false`() {
        val error = SyncError.AuthenticationError()
        assertFalse("AuthenticationError should not be retryable", error.isRetryable)
    }

    @Test
    fun `ServerError 5xx isRetryable is true`() {
        val error500 = SyncError.ServerError("Internal Server Error", 500)
        val error502 = SyncError.ServerError("Bad Gateway", 502)
        val error503 = SyncError.ServerError("Service Unavailable", 503)

        assertTrue("500 error should be retryable", error500.isRetryable)
        assertTrue("502 error should be retryable", error502.isRetryable)
        assertTrue("503 error should be retryable", error503.isRetryable)
    }

    @Test
    fun `ServerError 4xx isRetryable is false`() {
        val error400 = SyncError.ServerError("Bad Request", 400)
        val error403 = SyncError.ServerError("Forbidden", 403)
        val error404 = SyncError.ServerError("Not Found", 404)

        assertFalse("400 error should not be retryable", error400.isRetryable)
        assertFalse("403 error should not be retryable", error403.isRetryable)
        assertFalse("404 error should not be retryable", error404.isRetryable)
    }

    @Test
    fun `ValidationError isRetryable is false`() {
        val error = SyncError.ValidationError("Validation failed")
        assertFalse("ValidationError should not be retryable", error.isRetryable)
    }

    @Test
    fun `ParseError isRetryable is false`() {
        val error = SyncError.ParseError()
        assertFalse("ParseError should not be retryable", error.isRetryable)
    }

    @Test
    fun `ApiVersionError isRetryable is false`() {
        val error = SyncError.ApiVersionError()
        assertFalse("ApiVersionError should not be retryable", error.isRetryable)
    }

    @Test
    fun `Cancelled isRetryable is false`() {
        val error = SyncError.Cancelled
        assertFalse("Cancelled should not be retryable", error.isRetryable)
    }

    @Test
    fun `UnknownError isRetryable is false`() {
        val error = SyncError.UnknownError()
        assertFalse("UnknownError should not be retryable", error.isRetryable)
    }

    // ==========================================
    // Edge cases
    // ==========================================

    @Test
    fun `all SyncError subclasses have non-empty message`() {
        val errors = listOf(
            SyncError.NetworkError(),
            SyncError.TimeoutError(),
            SyncError.AuthenticationError(),
            SyncError.ServerError("Server error", 500),
            SyncError.ValidationError("Validation error"),
            SyncError.ParseError(),
            SyncError.ApiVersionError(),
            SyncError.Cancelled,
            SyncError.UnknownError()
        )

        errors.forEach { error ->
            assertTrue(
                "${error::class.simpleName} message should not be blank",
                error.message.isNotBlank()
            )
        }
    }
}
