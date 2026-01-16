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

package net.ktnx.mobileledger.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import net.ktnx.mobileledger.data.repository.CurrencyRepository
import net.ktnx.mobileledger.data.repository.mapper.CurrencyMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.CurrencyMapper.toEntity
import net.ktnx.mobileledger.db.Currency
import net.ktnx.mobileledger.domain.model.Currency as DomainCurrency

/**
 * Fake implementation of [CurrencyRepository] for testing.
 */
class FakeCurrencyRepository : CurrencyRepository {

    private val currencies = mutableMapOf<Long, Currency>()
    private val currenciesFlow = MutableStateFlow<List<Currency>>(emptyList())
    private var nextId = 1L

    // ========================================
    // Domain Model Query Operations
    // ========================================

    override fun getAllCurrenciesAsDomain(): Flow<List<DomainCurrency>> =
        currenciesFlow.map { list -> list.map { it.toDomain() } }

    override suspend fun getAllCurrenciesAsDomainSync(): List<DomainCurrency> = currencies.values.map { it.toDomain() }

    override fun getCurrencyAsDomain(id: Long): Flow<DomainCurrency?> =
        currenciesFlow.map { list -> list.find { it.id == id }?.toDomain() }

    override suspend fun getCurrencyAsDomainSync(id: Long): DomainCurrency? = currencies[id]?.toDomain()

    override suspend fun getCurrencyAsDomainByNameSync(name: String): DomainCurrency? =
        currencies.values.find { it.name == name }?.toDomain()

    // ========================================
    // Database Entity Query Operations
    // ========================================

    override fun getAllCurrencies(): Flow<List<Currency>> = currenciesFlow

    override suspend fun getAllCurrenciesSync(): List<Currency> = currencies.values.toList()

    override fun getCurrencyById(id: Long): Flow<Currency?> = MutableStateFlow(currencies[id])

    override suspend fun getCurrencyByIdSync(id: Long): Currency? = currencies[id]

    override fun getCurrencyByName(name: String): Flow<Currency?> =
        MutableStateFlow(currencies.values.find { it.name == name })

    override suspend fun getCurrencyByNameSync(name: String): Currency? = currencies.values.find { it.name == name }

    override suspend fun insertCurrency(currency: Currency): Long {
        val id = if (currency.id == 0L) nextId++ else currency.id
        currency.id = id
        currencies[id] = currency
        emitFlow()
        return id
    }

    override suspend fun updateCurrency(currency: Currency) {
        currencies[currency.id] = currency
        emitFlow()
    }

    override suspend fun deleteCurrency(currency: Currency) {
        currencies.remove(currency.id)
        emitFlow()
    }

    override suspend fun deleteAllCurrencies() {
        currencies.clear()
        emitFlow()
    }

    override suspend fun saveCurrency(currency: DomainCurrency): Long {
        val entity = currency.toEntity()
        return if (entity.id == 0L) {
            insertCurrency(entity)
        } else {
            updateCurrency(entity)
            entity.id
        }
    }

    override suspend fun deleteCurrencyByName(name: String): Boolean {
        val currency = currencies.values.find { it.name == name }
        return if (currency != null) {
            currencies.remove(currency.id)
            emitFlow()
            true
        } else {
            false
        }
    }

    private fun emitFlow() {
        currenciesFlow.value = currencies.values.toList()
    }

    fun reset() {
        currencies.clear()
        nextId = 1L
        emitFlow()
    }
}
