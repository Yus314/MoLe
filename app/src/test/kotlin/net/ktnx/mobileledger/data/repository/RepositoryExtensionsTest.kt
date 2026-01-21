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

package net.ktnx.mobileledger.data.repository

import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.domain.model.AppError
import net.ktnx.mobileledger.domain.model.AppException
import net.ktnx.mobileledger.domain.model.DatabaseError
import net.ktnx.mobileledger.domain.usecase.AppExceptionMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for repository extension functions.
 *
 * Tests verify:
 * - safeCall wraps exceptions correctly
 * - asResultFlow converts Flow to Result Flow
 * - mapErrorToAppError transforms errors
 * - toAppError extracts AppError from Throwable
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryExtensionsTest {

    private lateinit var mockMapper: AppExceptionMapper

    @Before
    fun setup() {
        mockMapper = mockk()
        every { mockMapper.map(any()) } answers {
            val exception = firstArg<Exception>()
            DatabaseError.QueryFailed(exception.message ?: "Unknown error", exception)
        }
    }

    // ========================================
    // safeCall tests
    // ========================================

    @Test
    fun `safeCall returns success when block succeeds`() = runTest {
        val result = safeCall(mockMapper) { "success" }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
    }

    @Test
    fun `safeCall returns success with null value`() = runTest {
        val result = safeCall(mockMapper) { null as String? }

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun `safeCall returns failure when block throws exception`() = runTest {
        val result = safeCall(mockMapper) {
            throw IOException("Test error")
        }

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is AppException)
    }

    @Test
    fun `safeCall maps exception to AppError`() = runTest {
        val result = safeCall(mockMapper) {
            throw IllegalArgumentException("Invalid argument")
        }

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as AppException
        assertNotNull(exception.appError)
    }

    // ========================================
    // asResultFlow tests
    // ========================================

    @Test
    fun `asResultFlow emits success for successful items`() = runTest {
        val sourceFlow = flowOf("item1", "item2", "item3")

        val results = sourceFlow.asResultFlow(mockMapper).toList()

        assertEquals(3, results.size)
        assertTrue(results.all { it.isSuccess })
        assertEquals(listOf("item1", "item2", "item3"), results.map { it.getOrNull() })
    }

    @Test
    fun `asResultFlow emits failure when flow throws`() = runTest {
        val sourceFlow = flow<String> {
            emit("item1")
            throw IOException("Flow error")
        }

        val results = sourceFlow.asResultFlow(mockMapper).toList()

        assertEquals(2, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals("item1", results[0].getOrNull())
        assertTrue(results[1].isFailure)
        assertTrue(results[1].exceptionOrNull() is AppException)
    }

    @Test
    fun `asResultFlow with empty flow returns empty list`() = runTest {
        val sourceFlow = flowOf<String>()

        val results = sourceFlow.asResultFlow(mockMapper).toList()

        assertTrue(results.isEmpty())
    }

    // ========================================
    // mapErrorToAppError tests
    // ========================================

    @Test
    fun `mapErrorToAppError preserves success`() {
        val result = Result.success("value")

        val mapped = result.mapErrorToAppError(mockMapper)

        assertTrue(mapped.isSuccess)
        assertEquals("value", mapped.getOrNull())
    }

    @Test
    fun `mapErrorToAppError preserves AppException`() {
        val appError = DatabaseError.QueryFailed("test error")
        val appException = AppException(appError)
        val result = Result.failure<String>(appException)

        val mapped = result.mapErrorToAppError(mockMapper)

        assertTrue(mapped.isFailure)
        val exception = mapped.exceptionOrNull() as AppException
        assertEquals(appError, exception.appError)
    }

    @Test
    fun `mapErrorToAppError converts non-AppException`() {
        val result = Result.failure<String>(IOException("IO error"))

        val mapped = result.mapErrorToAppError(mockMapper)

        assertTrue(mapped.isFailure)
        assertTrue(mapped.exceptionOrNull() is AppException)
    }

    // ========================================
    // toAppError tests
    // ========================================

    @Test
    fun `toAppError extracts AppError from AppException`() {
        val appError = DatabaseError.QueryFailed("test error")
        val appException = AppException(appError)

        val result = appException.toAppError(mockMapper)

        assertEquals(appError, result)
    }

    @Test
    fun `toAppError maps non-AppException using mapper`() {
        val exception = IOException("IO error")

        val result = exception.toAppError(mockMapper)

        assertNotNull(result)
        assertTrue(result is DatabaseError.QueryFailed)
    }

    @Test
    fun `toAppError maps RuntimeException using mapper`() {
        val exception = RuntimeException("Runtime error")

        val result = exception.toAppError(mockMapper)

        assertNotNull(result)
        assertTrue(result is DatabaseError.QueryFailed)
        assertEquals("Runtime error", result.message)
    }

    // ========================================
    // Edge cases
    // ========================================

    @Test
    fun `safeCall with suspending block that returns after delay`() = runTest {
        val result = safeCall(mockMapper) {
            kotlinx.coroutines.delay(10)
            "delayed result"
        }

        assertTrue(result.isSuccess)
        assertEquals("delayed result", result.getOrNull())
    }

    @Test
    fun `asResultFlow processes multiple items before error`() = runTest {
        val sourceFlow = flow<Int> {
            emit(1)
            emit(2)
            emit(3)
            throw IllegalStateException("State error")
        }

        val results = sourceFlow.asResultFlow(mockMapper).toList()

        assertEquals(4, results.size)
        assertTrue(results[0].isSuccess)
        assertTrue(results[1].isSuccess)
        assertTrue(results[2].isSuccess)
        assertTrue(results[3].isFailure)
    }
}
