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
import net.ktnx.mobileledger.domain.repository.CurrencyRepository
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

    override fun observeAllCurrenciesAsDomain(): Flow<List<DomainCurrency>> =
        currenciesFlow.map { list -> list.map { it.toDomain() } }

    override suspend fun getAllCurrenciesAsDomain(): Result<List<DomainCurrency>> =
        Result.success(currencies.values.map { it.toDomain() })

    override fun observeCurrencyAsDomain(id: Long): Flow<DomainCurrency?> =
        currenciesFlow.map { list -> list.find { it.id == id }?.toDomain() }

    override suspend fun getCurrencyAsDomain(id: Long): Result<DomainCurrency?> =
        Result.success(currencies[id]?.toDomain())

    override suspend fun getCurrencyAsDomainByName(name: String): Result<DomainCurrency?> =
        Result.success(currencies.values.find { it.name == name }?.toDomain())

    override fun observeCurrencyAsDomainByName(name: String): Flow<DomainCurrency?> =
        currenciesFlow.map { list -> list.find { it.name == name }?.toDomain() }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun deleteAllCurrencies(): Result<Unit> {
        currencies.clear()
        emitFlow()
        return Result.success(Unit)
    }

    override suspend fun saveCurrency(currency: DomainCurrency): Result<Long> {
        val entity = currency.toEntity()
        val id = if (entity.id == 0L) nextId++ else entity.id
        entity.id = id
        currencies[id] = entity
        emitFlow()
        return Result.success(id)
    }

    override suspend fun deleteCurrencyByName(name: String): Result<Boolean> {
        val currency = currencies.values.find { it.name == name }
        return if (currency != null) {
            currencies.remove(currency.id)
            emitFlow()
            Result.success(true)
        } else {
            Result.success(false)
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
