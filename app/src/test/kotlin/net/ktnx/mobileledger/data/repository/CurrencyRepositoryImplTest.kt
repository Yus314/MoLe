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

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.core.data.exception.CoreExceptionMapper
import net.ktnx.mobileledger.core.data.repository.impl.CurrencyRepositoryImpl
import net.ktnx.mobileledger.core.database.dao.CurrencyDAO
import net.ktnx.mobileledger.core.database.entity.Currency as DbCurrency
import net.ktnx.mobileledger.core.domain.model.Currency as DomainCurrency
import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CurrencyRepositoryImpl].
 *
 * Tests verify:
 * - Query operations (observe and get)
 * - Mutation operations (save, delete)
 * - Error handling with Result<T>
 * - Domain model mapping
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockCurrencyDAO: CurrencyDAO
    private lateinit var exceptionMapper: CoreExceptionMapper
    private lateinit var repository: CurrencyRepositoryImpl

    @Before
    fun setup() {
        mockCurrencyDAO = mockk(relaxed = true)
        exceptionMapper = CoreExceptionMapper()

        repository = CurrencyRepositoryImpl(
            currencyDAO = mockCurrencyDAO,
            exceptionMapper = exceptionMapper,
            ioDispatcher = testDispatcher
        )
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createDbCurrency(
        id: Long = 1L,
        name: String = "USD",
        position: String = "before",
        hasGap: Boolean = true
    ): DbCurrency = DbCurrency(id, name, position, hasGap)

    private fun createDomainCurrency(
        id: Long? = 1L,
        name: String = "USD",
        position: CurrencyPosition = CurrencyPosition.BEFORE,
        hasGap: Boolean = true
    ): DomainCurrency = DomainCurrency(
        id = id,
        name = name,
        position = position,
        hasGap = hasGap
    )

    // ========================================
    // observeAllCurrenciesAsDomain tests
    // ========================================

    @Test
    fun `observeAllCurrenciesAsDomain returns mapped currencies`() = runTest(testDispatcher) {
        // Given
        val dbCurrencies = listOf(
            createDbCurrency(1L, "USD", "before", true),
            createDbCurrency(2L, "EUR", "after", false)
        )
        every { mockCurrencyDAO.getAll() } returns flowOf(dbCurrencies)

        // When
        val result = repository.observeAllCurrenciesAsDomain().first()

        // Then
        assertEquals(2, result.size)
        assertEquals("USD", result[0].name)
        assertEquals(CurrencyPosition.BEFORE, result[0].position)
        assertEquals("EUR", result[1].name)
        assertEquals(CurrencyPosition.AFTER, result[1].position)
    }

    @Test
    fun `observeAllCurrenciesAsDomain returns empty list when no currencies`() = runTest(testDispatcher) {
        // Given
        every { mockCurrencyDAO.getAll() } returns flowOf(emptyList())

        // When
        val result = repository.observeAllCurrenciesAsDomain().first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ========================================
    // getAllCurrenciesAsDomain tests
    // ========================================

    @Test
    fun `getAllCurrenciesAsDomain returns success with mapped currencies`() = runTest(testDispatcher) {
        // Given
        val dbCurrencies = listOf(
            createDbCurrency(1L, "USD"),
            createDbCurrency(2L, "JPY")
        )
        coEvery { mockCurrencyDAO.getAllSync() } returns dbCurrencies

        // When
        val result = repository.getAllCurrenciesAsDomain()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }

    @Test
    fun `getAllCurrenciesAsDomain returns failure on exception`() = runTest(testDispatcher) {
        // Given
        coEvery { mockCurrencyDAO.getAllSync() } throws RuntimeException("Database error")

        // When
        val result = repository.getAllCurrenciesAsDomain()

        // Then
        assertTrue(result.isFailure)
    }

    // ========================================
    // observeCurrencyAsDomain tests
    // ========================================

    @Test
    fun `observeCurrencyAsDomain returns mapped currency`() = runTest(testDispatcher) {
        // Given
        val dbCurrency = createDbCurrency(1L, "USD")
        every { mockCurrencyDAO.getById(1L) } returns flowOf(dbCurrency)

        // When
        val result = repository.observeCurrencyAsDomain(1L).first()

        // Then
        assertNotNull(result)
        assertEquals("USD", result?.name)
        assertEquals(1L, result?.id)
    }

    // ========================================
    // getCurrencyAsDomain tests
    // ========================================

    @Test
    fun `getCurrencyAsDomain returns success with mapped currency`() = runTest(testDispatcher) {
        // Given
        val dbCurrency = createDbCurrency(1L, "EUR")
        coEvery { mockCurrencyDAO.getByIdSync(1L) } returns dbCurrency

        // When
        val result = repository.getCurrencyAsDomain(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("EUR", result.getOrThrow()?.name)
    }

    @Test
    fun `getCurrencyAsDomain returns success with null when not found`() = runTest(testDispatcher) {
        // Given
        coEvery { mockCurrencyDAO.getByIdSync(999L) } returns null

        // When
        val result = repository.getCurrencyAsDomain(999L)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    // ========================================
    // getCurrencyAsDomainByName tests
    // ========================================

    @Test
    fun `getCurrencyAsDomainByName returns success with mapped currency`() = runTest(testDispatcher) {
        // Given
        val dbCurrency = createDbCurrency(1L, "JPY", "after", false)
        coEvery { mockCurrencyDAO.getByNameSync("JPY") } returns dbCurrency

        // When
        val result = repository.getCurrencyAsDomainByName("JPY")

        // Then
        assertTrue(result.isSuccess)
        val currency = result.getOrThrow()
        assertEquals("JPY", currency?.name)
        assertEquals(CurrencyPosition.AFTER, currency?.position)
        assertFalse(currency?.hasGap ?: true)
    }

    @Test
    fun `getCurrencyAsDomainByName returns null for unknown name`() = runTest(testDispatcher) {
        // Given
        coEvery { mockCurrencyDAO.getByNameSync("UNKNOWN") } returns null

        // When
        val result = repository.getCurrencyAsDomainByName("UNKNOWN")

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    // ========================================
    // observeCurrencyAsDomainByName tests
    // ========================================

    @Test
    fun `observeCurrencyAsDomainByName returns mapped currency`() = runTest(testDispatcher) {
        // Given
        val dbCurrency = createDbCurrency(1L, "GBP")
        every { mockCurrencyDAO.getByName("GBP") } returns flowOf(dbCurrency)

        // When
        val result = repository.observeCurrencyAsDomainByName("GBP").first()

        // Then
        assertNotNull(result)
        assertEquals("GBP", result?.name)
    }

    // ========================================
    // deleteAllCurrencies tests
    // ========================================

    @Test
    fun `deleteAllCurrencies calls DAO deleteAllSync`() = runTest(testDispatcher) {
        // Given
        coEvery { mockCurrencyDAO.deleteAllSync() } just Runs

        // When
        val result = repository.deleteAllCurrencies()

        // Then
        assertTrue(result.isSuccess)
        coVerify { mockCurrencyDAO.deleteAllSync() }
    }

    @Test
    fun `deleteAllCurrencies returns failure on exception`() = runTest(testDispatcher) {
        // Given
        coEvery { mockCurrencyDAO.deleteAllSync() } throws RuntimeException("Delete failed")

        // When
        val result = repository.deleteAllCurrencies()

        // Then
        assertTrue(result.isFailure)
    }

    // ========================================
    // saveCurrency tests
    // ========================================

    @Test
    fun `saveCurrency inserts new currency when id is null`() = runTest(testDispatcher) {
        // Given
        val newCurrency = createDomainCurrency(id = null, name = "CHF")
        coEvery { mockCurrencyDAO.insertSync(any()) } returns 5L

        // When
        val result = repository.saveCurrency(newCurrency)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5L, result.getOrThrow())
        coVerify { mockCurrencyDAO.insertSync(any()) }
    }

    @Test
    fun `saveCurrency inserts new currency when id is 0`() = runTest(testDispatcher) {
        // Given - Domain currency with id = null will be mapped to entity with id = 0
        val newCurrency = createDomainCurrency(id = null, name = "AUD")
        coEvery { mockCurrencyDAO.insertSync(any()) } returns 10L

        // When
        val result = repository.saveCurrency(newCurrency)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(10L, result.getOrThrow())
    }

    @Test
    fun `saveCurrency updates existing currency when id is set`() = runTest(testDispatcher) {
        // Given
        val existingCurrency = createDomainCurrency(id = 3L, name = "CAD")
        coEvery { mockCurrencyDAO.updateSync(any()) } just Runs

        // When
        val result = repository.saveCurrency(existingCurrency)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3L, result.getOrThrow())
        coVerify { mockCurrencyDAO.updateSync(any()) }
    }

    @Test
    fun `saveCurrency returns failure on insert exception`() = runTest(testDispatcher) {
        // Given
        val newCurrency = createDomainCurrency(id = null, name = "NZD")
        coEvery { mockCurrencyDAO.insertSync(any()) } throws RuntimeException("Insert failed")

        // When
        val result = repository.saveCurrency(newCurrency)

        // Then
        assertTrue(result.isFailure)
    }

    // ========================================
    // deleteCurrencyByName tests
    // ========================================

    @Test
    fun `deleteCurrencyByName returns true when currency exists`() = runTest(testDispatcher) {
        // Given
        val dbCurrency = createDbCurrency(1L, "SEK")
        coEvery { mockCurrencyDAO.getByNameSync("SEK") } returns dbCurrency
        coEvery { mockCurrencyDAO.deleteSync(dbCurrency) } just Runs

        // When
        val result = repository.deleteCurrencyByName("SEK")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
        coVerify { mockCurrencyDAO.deleteSync(dbCurrency) }
    }

    @Test
    fun `deleteCurrencyByName returns false when currency not found`() = runTest(testDispatcher) {
        // Given
        coEvery { mockCurrencyDAO.getByNameSync("NOTEXIST") } returns null

        // When
        val result = repository.deleteCurrencyByName("NOTEXIST")

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow())
    }

    @Test
    fun `deleteCurrencyByName returns failure on exception`() = runTest(testDispatcher) {
        // Given
        coEvery { mockCurrencyDAO.getByNameSync("DKK") } throws RuntimeException("DB error")

        // When
        val result = repository.deleteCurrencyByName("DKK")

        // Then
        assertTrue(result.isFailure)
    }

    // ========================================
    // Position mapping tests
    // ========================================

    @Test
    fun `currency position BEFORE is correctly mapped`() = runTest(testDispatcher) {
        // Given
        val dbCurrency = createDbCurrency(1L, "USD", "before", true)
        coEvery { mockCurrencyDAO.getByIdSync(1L) } returns dbCurrency

        // When
        val result = repository.getCurrencyAsDomain(1L)

        // Then
        assertEquals(CurrencyPosition.BEFORE, result.getOrThrow()?.position)
    }

    @Test
    fun `currency position AFTER is correctly mapped`() = runTest(testDispatcher) {
        // Given
        val dbCurrency = createDbCurrency(1L, "EUR", "after", false)
        coEvery { mockCurrencyDAO.getByIdSync(1L) } returns dbCurrency

        // When
        val result = repository.getCurrencyAsDomain(1L)

        // Then
        assertEquals(CurrencyPosition.AFTER, result.getOrThrow()?.position)
    }
}
