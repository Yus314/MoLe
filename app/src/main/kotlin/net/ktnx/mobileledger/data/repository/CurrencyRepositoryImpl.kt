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

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.CurrencyDAO
import net.ktnx.mobileledger.data.repository.mapper.CurrencyMapper.toDomain
import net.ktnx.mobileledger.data.repository.mapper.CurrencyMapper.toEntity
import net.ktnx.mobileledger.di.IoDispatcher
import net.ktnx.mobileledger.domain.model.Currency
import net.ktnx.mobileledger.domain.usecase.AppExceptionMapper

/**
 * Implementation of [CurrencyRepository] that wraps the existing [CurrencyDAO].
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Uses ioDispatcher for database operations
 * - Delegates all operations to the underlying DAO
 * - Returns Result<T> for all suspend operations with error handling
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 */
@Singleton
class CurrencyRepositoryImpl @Inject constructor(
    private val currencyDAO: CurrencyDAO,
    private val appExceptionMapper: AppExceptionMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CurrencyRepository {

    // ========================================
    // Query Operations
    // ========================================

    override fun observeAllCurrenciesAsDomain(): Flow<List<Currency>> =
        currencyDAO.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllCurrenciesAsDomain(): Result<List<Currency>> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            currencyDAO.getAllSync().map { it.toDomain() }
        }
    }

    override fun observeCurrencyAsDomain(id: Long): Flow<Currency?> = currencyDAO.getById(id).map { it?.toDomain() }

    override suspend fun getCurrencyAsDomain(id: Long): Result<Currency?> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            currencyDAO.getByIdSync(id)?.toDomain()
        }
    }

    override suspend fun getCurrencyAsDomainByName(name: String): Result<Currency?> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            currencyDAO.getByNameSync(name)?.toDomain()
        }
    }

    override fun observeCurrencyAsDomainByName(name: String): Flow<Currency?> =
        currencyDAO.getByName(name).map { it?.toDomain() }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun deleteAllCurrencies(): Result<Unit> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            currencyDAO.deleteAllSync()
        }
    }

    override suspend fun saveCurrency(currency: Currency): Result<Long> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            val entity = currency.toEntity()
            if (entity.id == 0L) {
                currencyDAO.insertSync(entity)
            } else {
                currencyDAO.updateSync(entity)
                entity.id
            }
        }
    }

    override suspend fun deleteCurrencyByName(name: String): Result<Boolean> = safeCall(appExceptionMapper) {
        withContext(ioDispatcher) {
            val currency = currencyDAO.getByNameSync(name)
            if (currency != null) {
                currencyDAO.deleteSync(currency)
                true
            } else {
                false
            }
        }
    }
}
