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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.domain.model.Currency
import net.ktnx.mobileledger.domain.model.CurrencyPosition
import net.ktnx.mobileledger.fake.FakeCurrencyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CurrencyRepository] using a fake repository implementation.
 *
 * These tests verify:
 * - CRUD operations work correctly
 * - Lookups by ID and name function properly
 * - Flow emissions occur on data changes
 *
 * Note: For proper Flow testing with Room, use instrumentation tests.
 * These unit tests use a fake repository that implements the interface directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRepositoryTest {

    private lateinit var repository: FakeCurrencyRepository

    @Before
    fun setup() {
        repository = FakeCurrencyRepository()
    }

    // ========================================
    // Helper methods
    // ========================================

    private fun createTestCurrency(
        id: Long = 0L,
        name: String = "USD",
        position: CurrencyPosition = CurrencyPosition.AFTER,
        hasGap: Boolean = true
    ): Currency = Currency(id, name, position, hasGap)

    // ========================================
    // observeAllCurrenciesAsDomain tests
    // ========================================

    @Test
    fun `observeAllCurrenciesAsDomain returns empty list when no currencies`() = runTest {
        val currencies = repository.observeAllCurrenciesAsDomain().first()
        assertTrue(currencies.isEmpty())
    }

    @Test
    fun `observeAllCurrenciesAsDomain returns all currencies`() = runTest {
        repository.saveCurrency(createTestCurrency(name = "USD")).getOrThrow()
        repository.saveCurrency(createTestCurrency(name = "EUR")).getOrThrow()
        repository.saveCurrency(createTestCurrency(name = "JPY")).getOrThrow()

        val currencies = repository.observeAllCurrenciesAsDomain().first()

        assertEquals(3, currencies.size)
    }

    // ========================================
    // getAllCurrenciesAsDomain tests
    // ========================================

    @Test
    fun `getAllCurrenciesAsDomain returns all currencies`() = runTest {
        repository.saveCurrency(createTestCurrency(name = "USD")).getOrThrow()
        repository.saveCurrency(createTestCurrency(name = "EUR")).getOrThrow()

        val currencies = repository.getAllCurrenciesAsDomain().getOrThrow()

        assertEquals(2, currencies.size)
    }

    // ========================================
    // observeCurrencyAsDomain tests
    // ========================================

    @Test
    fun `observeCurrencyAsDomain returns null for non-existent id`() = runTest {
        val result = repository.observeCurrencyAsDomain(999L).first()
        assertNull(result)
    }

    @Test
    fun `observeCurrencyAsDomain returns currency when exists`() = runTest {
        val currency = createTestCurrency(name = "USD")
        val id = repository.saveCurrency(currency).getOrThrow()

        val result = repository.observeCurrencyAsDomain(id).first()

        assertNotNull(result)
        assertEquals("USD", result?.name)
    }

    // ========================================
    // observeCurrencyAsDomainByName tests
    // ========================================

    @Test
    fun `observeCurrencyAsDomainByName returns null for non-existent name`() = runTest {
        val result = repository.observeCurrencyAsDomainByName("UNKNOWN").first()
        assertNull(result)
    }

    @Test
    fun `getCurrencyAsDomainByName returns currency when exists`() = runTest {
        repository.saveCurrency(createTestCurrency(name = "USD")).getOrThrow()

        val result = repository.getCurrencyAsDomainByName("USD").getOrNull()

        assertNotNull(result)
        assertEquals("USD", result?.name)
    }

    // ========================================
    // saveCurrency tests
    // ========================================

    @Test
    fun `saveCurrency assigns id and returns it`() = runTest {
        val currency = createTestCurrency(name = "USD")

        val id = repository.saveCurrency(currency).getOrThrow()

        assertTrue(id > 0)
        val stored = repository.observeCurrencyAsDomain(id).first()
        assertNotNull(stored)
        assertEquals("USD", stored?.name)
    }

    @Test
    fun `saveCurrency preserves all properties`() = runTest {
        val currency = createTestCurrency(
            name = "EUR",
            position = CurrencyPosition.BEFORE,
            hasGap = false
        )

        val id = repository.saveCurrency(currency).getOrThrow()
        val stored = repository.observeCurrencyAsDomain(id).first()

        assertNotNull(stored)
        assertEquals("EUR", stored?.name)
        assertEquals(CurrencyPosition.BEFORE, stored?.position)
        assertEquals(false, stored?.hasGap)
    }

    // ========================================
    // saveCurrency update tests
    // ========================================

    @Test
    fun `saveCurrency with id modifies existing currency`() = runTest {
        val currency = createTestCurrency(name = "USD", position = CurrencyPosition.AFTER)
        val id = repository.saveCurrency(currency).getOrThrow()

        val updated = createTestCurrency(id = id, name = "USD", position = CurrencyPosition.BEFORE)
        repository.saveCurrency(updated).getOrThrow()

        val result = repository.observeCurrencyAsDomain(id).first()
        assertEquals(CurrencyPosition.BEFORE, result?.position)
    }

    // ========================================
    // deleteCurrencyByName tests
    // ========================================

    @Test
    fun `deleteCurrencyByName removes currency`() = runTest {
        val currency = createTestCurrency(name = "USD")
        repository.saveCurrency(currency).getOrThrow()

        repository.deleteCurrencyByName("USD").getOrThrow()

        val remaining = repository.observeAllCurrenciesAsDomain().first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `deleteCurrencyByName only removes specified currency`() = runTest {
        repository.saveCurrency(createTestCurrency(name = "USD")).getOrThrow()
        repository.saveCurrency(createTestCurrency(name = "EUR")).getOrThrow()

        repository.deleteCurrencyByName("USD").getOrThrow()

        val remaining = repository.observeAllCurrenciesAsDomain().first()
        assertEquals(1, remaining.size)
        assertEquals("EUR", remaining[0].name)
    }

    // ========================================
    // deleteAllCurrencies tests
    // ========================================

    @Test
    fun `deleteAllCurrencies removes all currencies`() = runTest {
        repository.saveCurrency(createTestCurrency(name = "USD")).getOrThrow()
        repository.saveCurrency(createTestCurrency(name = "EUR")).getOrThrow()
        repository.saveCurrency(createTestCurrency(name = "JPY")).getOrThrow()

        repository.deleteAllCurrencies().getOrThrow()

        val currencies = repository.observeAllCurrenciesAsDomain().first()
        assertTrue(currencies.isEmpty())
    }
}
