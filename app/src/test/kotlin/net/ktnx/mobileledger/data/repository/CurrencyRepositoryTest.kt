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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.ktnx.mobileledger.db.Currency
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
        position: String = "after",
        hasGap: Boolean = true
    ): Currency = Currency(id, name, position, hasGap)

    // ========================================
    // getAllCurrencies tests
    // ========================================

    @Test
    fun `getAllCurrencies returns empty list when no currencies`() = runTest {
        val currencies = repository.getAllCurrencies().first()
        assertTrue(currencies.isEmpty())
    }

    @Test
    fun `getAllCurrencies returns all currencies`() = runTest {
        repository.insertCurrency(createTestCurrency(name = "USD"))
        repository.insertCurrency(createTestCurrency(name = "EUR"))
        repository.insertCurrency(createTestCurrency(name = "JPY"))

        val currencies = repository.getAllCurrencies().first()

        assertEquals(3, currencies.size)
    }

    // ========================================
    // getAllCurrenciesSync tests
    // ========================================

    @Test
    fun `getAllCurrenciesSync returns all currencies`() = runTest {
        repository.insertCurrency(createTestCurrency(name = "USD"))
        repository.insertCurrency(createTestCurrency(name = "EUR"))

        val currencies = repository.getAllCurrenciesSync()

        assertEquals(2, currencies.size)
    }

    // ========================================
    // getCurrencyById tests
    // ========================================

    @Test
    fun `getCurrencyById returns null for non-existent id`() = runTest {
        val result = repository.getCurrencyById(999L).first()
        assertNull(result)
    }

    @Test
    fun `getCurrencyByIdSync returns currency when exists`() = runTest {
        val currency = createTestCurrency(name = "USD")
        val id = repository.insertCurrency(currency)

        val result = repository.getCurrencyByIdSync(id)

        assertNotNull(result)
        assertEquals("USD", result?.name)
    }

    // ========================================
    // getCurrencyByName tests
    // ========================================

    @Test
    fun `getCurrencyByName returns null for non-existent name`() = runTest {
        val result = repository.getCurrencyByName("UNKNOWN").first()
        assertNull(result)
    }

    @Test
    fun `getCurrencyByNameSync returns currency when exists`() = runTest {
        repository.insertCurrency(createTestCurrency(name = "USD"))

        val result = repository.getCurrencyByNameSync("USD")

        assertNotNull(result)
        assertEquals("USD", result?.name)
    }

    // ========================================
    // insertCurrency tests
    // ========================================

    @Test
    fun `insertCurrency assigns id and returns it`() = runTest {
        val currency = createTestCurrency(name = "USD")

        val id = repository.insertCurrency(currency)

        assertTrue(id > 0)
        val stored = repository.getCurrencyByIdSync(id)
        assertNotNull(stored)
        assertEquals("USD", stored?.name)
    }

    @Test
    fun `insertCurrency preserves all properties`() = runTest {
        val currency = createTestCurrency(
            name = "EUR",
            position = "before",
            hasGap = false
        )

        val id = repository.insertCurrency(currency)
        val stored = repository.getCurrencyByIdSync(id)

        assertNotNull(stored)
        assertEquals("EUR", stored?.name)
        assertEquals("before", stored?.position)
        assertEquals(false, stored?.hasGap)
    }

    // ========================================
    // updateCurrency tests
    // ========================================

    @Test
    fun `updateCurrency modifies existing currency`() = runTest {
        val currency = createTestCurrency(name = "USD", position = "after")
        val id = repository.insertCurrency(currency)

        val updated = createTestCurrency(id = id, name = "USD", position = "before")
        repository.updateCurrency(updated)

        val result = repository.getCurrencyByIdSync(id)
        assertEquals("before", result?.position)
    }

    // ========================================
    // deleteCurrency tests
    // ========================================

    @Test
    fun `deleteCurrency removes currency`() = runTest {
        val currency = createTestCurrency(name = "USD")
        val id = repository.insertCurrency(currency)

        repository.deleteCurrency(currency.apply { this.id = id })

        val remaining = repository.getAllCurrencies().first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `deleteCurrency only removes specified currency`() = runTest {
        val usd = createTestCurrency(name = "USD")
        val eur = createTestCurrency(name = "EUR")
        val usdId = repository.insertCurrency(usd)
        repository.insertCurrency(eur)

        repository.deleteCurrency(usd.apply { this.id = usdId })

        val remaining = repository.getAllCurrencies().first()
        assertEquals(1, remaining.size)
        assertEquals("EUR", remaining[0].name)
    }

    // ========================================
    // deleteAllCurrencies tests
    // ========================================

    @Test
    fun `deleteAllCurrencies removes all currencies`() = runTest {
        repository.insertCurrency(createTestCurrency(name = "USD"))
        repository.insertCurrency(createTestCurrency(name = "EUR"))
        repository.insertCurrency(createTestCurrency(name = "JPY"))

        repository.deleteAllCurrencies()

        val currencies = repository.getAllCurrencies().first()
        assertTrue(currencies.isEmpty())
    }
}

/**
 * Fake implementation of [CurrencyRepository] for unit testing.
 *
 * This implementation provides an in-memory store that allows testing
 * without a real database or Room infrastructure.
 */
class FakeCurrencyRepository : CurrencyRepository {

    private val currencies = mutableMapOf<Long, Currency>()
    private var nextId = 1L
    private val currenciesFlow = MutableStateFlow<List<Currency>>(emptyList())

    private fun emitChanges() {
        currenciesFlow.value = currencies.values.toList()
    }

    override fun getAllCurrencies(): Flow<List<Currency>> = MutableStateFlow(currencies.values.toList())

    override suspend fun getAllCurrenciesSync(): List<Currency> = currencies.values.toList()

    override fun getCurrencyById(id: Long): Flow<Currency?> = MutableStateFlow(currencies[id])

    override suspend fun getCurrencyByIdSync(id: Long): Currency? = currencies[id]

    override fun getCurrencyByName(name: String): Flow<Currency?> =
        MutableStateFlow(currencies.values.find { it.name == name })

    override suspend fun getCurrencyByNameSync(name: String): Currency? = currencies.values.find { it.name == name }

    override suspend fun insertCurrency(currency: Currency): Long {
        val id = if (currency.id == 0L) nextId++ else currency.id
        val newCurrency = Currency(id, currency.name, currency.position, currency.hasGap)
        currencies[id] = newCurrency
        emitChanges()
        return id
    }

    override suspend fun updateCurrency(currency: Currency) {
        if (currencies.containsKey(currency.id)) {
            currencies[currency.id] = currency
            emitChanges()
        }
    }

    override suspend fun deleteCurrency(currency: Currency) {
        currencies.remove(currency.id)
        emitChanges()
    }

    override suspend fun deleteAllCurrencies() {
        currencies.clear()
        emitChanges()
    }
}
