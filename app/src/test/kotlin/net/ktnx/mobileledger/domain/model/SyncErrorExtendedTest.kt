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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Extended unit tests for SyncError sealed class.
 *
 * Tests verify all error types, their properties, and isRetryable behavior.
 */
class SyncErrorExtendedTest {

    // ========================================
    // NetworkError tests
    // ========================================

    @Test
    fun `NetworkError has default message`() {
        val error = SyncError.NetworkError()
        assertEquals("ネットワークに接続できません", error.message)
    }

    @Test
    fun `NetworkError with custom message`() {
        val error = SyncError.NetworkError("Custom network error")
        assertEquals("Custom network error", error.message)
    }

    @Test
    fun `NetworkError is retryable`() {
        val error = SyncError.NetworkError()
        assertTrue(error.isRetryable)
    }

    @Test
    fun `NetworkError with cause`() {
        val cause = RuntimeException("Socket closed")
        val error = SyncError.NetworkError(cause = cause)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `NetworkError cause is null by default`() {
        val error = SyncError.NetworkError()
        assertNull(error.cause)
    }

    // ========================================
    // TimeoutError tests
    // ========================================

    @Test
    fun `TimeoutError has default message`() {
        val error = SyncError.TimeoutError()
        assertEquals("サーバーが応答しません", error.message)
    }

    @Test
    fun `TimeoutError has default timeout`() {
        val error = SyncError.TimeoutError()
        assertEquals(30_000L, error.timeoutMs)
    }

    @Test
    fun `TimeoutError with custom timeout`() {
        val error = SyncError.TimeoutError(timeoutMs = 60_000L)
        assertEquals(60_000L, error.timeoutMs)
    }

    @Test
    fun `TimeoutError is retryable`() {
        val error = SyncError.TimeoutError()
        assertTrue(error.isRetryable)
    }

    // ========================================
    // AuthenticationError tests
    // ========================================

    @Test
    fun `AuthenticationError has default message`() {
        val error = SyncError.AuthenticationError()
        assertEquals("認証に失敗しました。ユーザー名とパスワードを確認してください", error.message)
    }

    @Test
    fun `AuthenticationError has default httpCode`() {
        val error = SyncError.AuthenticationError()
        assertEquals(401, error.httpCode)
    }

    @Test
    fun `AuthenticationError is not retryable`() {
        val error = SyncError.AuthenticationError()
        assertFalse(error.isRetryable)
    }

    // ========================================
    // ServerError tests
    // ========================================

    @Test
    fun `ServerError with 5xx is retryable`() {
        val error500 = SyncError.ServerError("Internal error", 500)
        val error502 = SyncError.ServerError("Bad gateway", 502)
        val error503 = SyncError.ServerError("Service unavailable", 503)

        assertTrue(error500.isRetryable)
        assertTrue(error502.isRetryable)
        assertTrue(error503.isRetryable)
    }

    @Test
    fun `ServerError with 4xx is not retryable`() {
        val error400 = SyncError.ServerError("Bad request", 400)
        val error403 = SyncError.ServerError("Forbidden", 403)
        val error404 = SyncError.ServerError("Not found", 404)
        val error422 = SyncError.ServerError("Unprocessable", 422)

        assertFalse(error400.isRetryable)
        assertFalse(error403.isRetryable)
        assertFalse(error404.isRetryable)
        assertFalse(error422.isRetryable)
    }

    @Test
    fun `ServerError has serverMessage`() {
        val error = SyncError.ServerError("Error", 500, "Detailed server message")
        assertEquals("Detailed server message", error.serverMessage)
    }

    @Test
    fun `ServerError serverMessage is null by default`() {
        val error = SyncError.ServerError("Error", 500)
        assertNull(error.serverMessage)
    }

    // ========================================
    // ValidationError tests
    // ========================================

    @Test
    fun `ValidationError is not retryable`() {
        val error = SyncError.ValidationError("Invalid amount")
        assertFalse(error.isRetryable)
    }

    @Test
    fun `ValidationError with field`() {
        val error = SyncError.ValidationError("Invalid", field = "amount")
        assertEquals("amount", error.field)
    }

    @Test
    fun `ValidationError with details`() {
        val error = SyncError.ValidationError(
            "Validation failed",
            details = listOf("Amount is negative", "Account not found")
        )
        assertEquals(2, error.details.size)
        assertEquals("Amount is negative", error.details[0])
        assertEquals("Account not found", error.details[1])
    }

    @Test
    fun `ValidationError details is empty by default`() {
        val error = SyncError.ValidationError("Error")
        assertTrue(error.details.isEmpty())
    }

    // ========================================
    // ParseError tests
    // ========================================

    @Test
    fun `ParseError has default message`() {
        val error = SyncError.ParseError()
        assertEquals("データの解析に失敗しました", error.message)
    }

    @Test
    fun `ParseError is not retryable`() {
        val error = SyncError.ParseError()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `ParseError with cause`() {
        val cause = RuntimeException("JSON parse error")
        val error = SyncError.ParseError(cause = cause)
        assertEquals(cause, error.cause)
    }

    // ========================================
    // ApiVersionError tests
    // ========================================

    @Test
    fun `ApiVersionError has default message`() {
        val error = SyncError.ApiVersionError()
        assertEquals("サポートされていないAPIバージョンです", error.message)
    }

    @Test
    fun `ApiVersionError is not retryable`() {
        val error = SyncError.ApiVersionError()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `ApiVersionError with detectedVersion`() {
        val error = SyncError.ApiVersionError(detectedVersion = "1.15.0")
        assertEquals("1.15.0", error.detectedVersion)
    }

    @Test
    fun `ApiVersionError with supportedVersions`() {
        val error = SyncError.ApiVersionError(
            supportedVersions = listOf("1.12", "1.13", "1.14")
        )
        assertEquals(3, error.supportedVersions.size)
    }

    // ========================================
    // Cancelled tests
    // ========================================

    @Test
    fun `Cancelled has message`() {
        val error = SyncError.Cancelled
        assertEquals("処理がキャンセルされました", error.message)
    }

    @Test
    fun `Cancelled is not retryable`() {
        val error = SyncError.Cancelled
        assertFalse(error.isRetryable)
    }

    @Test
    fun `Cancelled is singleton`() {
        val error1 = SyncError.Cancelled
        val error2 = SyncError.Cancelled
        assertTrue(error1 === error2)
    }

    // ========================================
    // UnknownError tests
    // ========================================

    @Test
    fun `UnknownError has default message`() {
        val error = SyncError.UnknownError()
        assertEquals("予期しないエラーが発生しました", error.message)
    }

    @Test
    fun `UnknownError is not retryable`() {
        val error = SyncError.UnknownError()
        assertFalse(error.isRetryable)
    }

    @Test
    fun `UnknownError with cause`() {
        val cause = RuntimeException("Unexpected exception")
        val error = SyncError.UnknownError(cause = cause)
        assertEquals(cause, error.cause)
    }

    // ========================================
    // When expression exhaustiveness
    // ========================================

    @Test
    fun `when expression handles all SyncError types`() {
        val errors: List<SyncError> = listOf(
            SyncError.NetworkError(),
            SyncError.TimeoutError(),
            SyncError.AuthenticationError(),
            SyncError.ServerError("Error", 500),
            SyncError.ValidationError("Error"),
            SyncError.ParseError(),
            SyncError.ApiVersionError(),
            SyncError.Cancelled,
            SyncError.UnknownError()
        )

        errors.forEach { error ->
            val description = when (error) {
                is SyncError.NetworkError -> "network"
                is SyncError.TimeoutError -> "timeout"
                is SyncError.AuthenticationError -> "auth"
                is SyncError.ServerError -> "server: ${error.httpCode}"
                is SyncError.ValidationError -> "validation"
                is SyncError.ParseError -> "parse"
                is SyncError.ApiVersionError -> "api version"
                is SyncError.Cancelled -> "cancelled"
                is SyncError.UnknownError -> "unknown"
            }
            assertTrue(description.isNotEmpty())
        }
    }

    // ========================================
    // Retryable summary
    // ========================================

    @Test
    fun `retryable errors list`() {
        val retryableErrors = listOf(
            SyncError.NetworkError(),
            SyncError.TimeoutError(),
            SyncError.ServerError("Error", 500)
        )

        retryableErrors.forEach { error ->
            assertTrue("${error::class.simpleName} should be retryable", error.isRetryable)
        }
    }

    @Test
    fun `non-retryable errors list`() {
        val nonRetryableErrors = listOf(
            SyncError.AuthenticationError(),
            SyncError.ServerError("Error", 400),
            SyncError.ValidationError("Error"),
            SyncError.ParseError(),
            SyncError.ApiVersionError(),
            SyncError.Cancelled,
            SyncError.UnknownError()
        )

        nonRetryableErrors.forEach { error ->
            assertFalse("${error::class.simpleName} should not be retryable", error.isRetryable)
        }
    }
}
