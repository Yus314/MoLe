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

package net.ktnx.mobileledger.domain.usecase.sync

import android.os.OperationCanceledException
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.text.ParseException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import net.ktnx.mobileledger.core.domain.model.SyncError
import net.ktnx.mobileledger.core.domain.model.SyncException
import net.ktnx.mobileledger.core.network.NetworkAuthenticationException
import net.ktnx.mobileledger.core.network.NetworkHttpException
import net.ktnx.mobileledger.json.ApiNotSupportedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [SyncExceptionMapper].
 *
 * Tests verify that various exception types are correctly mapped to
 * appropriate SyncError subtypes.
 */
@RunWith(RobolectricTestRunner::class)
class SyncExceptionMapperTest {

    private lateinit var mapper: SyncExceptionMapper

    @Before
    fun setup() {
        mapper = SyncExceptionMapper()
    }

    @Test
    fun `mapToSyncException returns original when already SyncException`() {
        // Given
        val original = SyncException(SyncError.NetworkError(message = "Test"))

        // When
        val result = mapper.mapToSyncException(original)

        // Then
        assertEquals(original, result)
    }

    @Test
    fun `mapToSyncException maps SocketTimeoutException to TimeoutError`() {
        // Given
        val exception = SocketTimeoutException("Read timed out")

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.TimeoutError)
    }

    @Test
    fun `mapToSyncException maps MalformedURLException to NetworkError`() {
        // Given
        val exception = MalformedURLException("Invalid URL")

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.NetworkError)
    }

    @Test
    fun `mapToSyncException maps NetworkAuthenticationException to AuthenticationError`() {
        // Given
        val exception = NetworkAuthenticationException("Auth failed")

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.AuthenticationError)
        assertEquals(401, (result.syncError as SyncError.AuthenticationError).httpCode)
    }

    @Test
    fun `mapToSyncException maps NetworkHttpException to ServerError`() {
        // Given
        val exception = NetworkHttpException(500, "Internal Server Error")

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.ServerError)
        assertEquals(500, (result.syncError as SyncError.ServerError).httpCode)
    }

    @Test
    fun `mapToSyncException maps IOException to NetworkError`() {
        // Given
        val exception = IOException("Connection reset")

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.NetworkError)
    }

    @Test
    fun `mapToSyncException maps SerializationException to ParseError`() {
        // Given
        val exception = SerializationException("Invalid JSON")

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.ParseError)
    }

    @Test
    fun `mapToSyncException maps ParseException to ParseError`() {
        // Given
        val exception = ParseException("Invalid date format", 0)

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.ParseError)
    }

    @Test
    fun `mapToSyncException maps OperationCanceledException to Cancelled`() {
        // Given
        val exception = OperationCanceledException("Cancelled by user")

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.Cancelled)
    }

    @Test
    fun `mapToSyncException maps ApiNotSupportedException to ApiVersionError`() {
        // Given
        val exception = ApiNotSupportedException()

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.ApiVersionError)
    }

    @Test
    fun `mapToSyncException maps CancellationException to Cancelled`() {
        // Given
        val exception = CancellationException("Coroutine cancelled")

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.Cancelled)
    }

    @Test
    fun `mapToSyncException maps unknown exception to UnknownError`() {
        // Given
        val exception = IllegalStateException("Unknown error")

        // When
        val result = mapper.mapToSyncException(exception)

        // Then
        assertTrue(result.syncError is SyncError.UnknownError)
    }

    @Test
    fun `mapToSyncException preserves original exception as cause`() {
        // Given
        val originalException = IOException("Connection failed")

        // When
        val result = mapper.mapToSyncException(originalException)

        // Then
        val syncError = result.syncError
        assertTrue(syncError is SyncError.NetworkError)
        assertEquals(originalException, (syncError as SyncError.NetworkError).cause)
    }
}
