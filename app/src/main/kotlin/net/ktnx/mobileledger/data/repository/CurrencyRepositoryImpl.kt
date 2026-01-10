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

import androidx.lifecycle.asFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import net.ktnx.mobileledger.dao.CurrencyDAO
import net.ktnx.mobileledger.db.Currency

/**
 * Implementation of [CurrencyRepository] that wraps the existing [CurrencyDAO].
 *
 * This implementation:
 * - Converts LiveData to Flow for reactive data access
 * - Uses Dispatchers.IO for database operations
 * - Delegates all operations to the underlying DAO
 *
 * Thread-safety: All operations are safe to call from any coroutine context.
 */
@Singleton
class CurrencyRepositoryImpl @Inject constructor(private val currencyDAO: CurrencyDAO) : CurrencyRepository {

    // ========================================
    // Query Operations
    // ========================================

    override fun getAllCurrencies(): Flow<List<Currency>> = currencyDAO.getAll().asFlow()

    override suspend fun getAllCurrenciesSync(): List<Currency> = withContext(Dispatchers.IO) {
        currencyDAO.getAllSync()
    }

    override fun getCurrencyById(id: Long): Flow<Currency?> = currencyDAO.getById(id).asFlow()

    override suspend fun getCurrencyByIdSync(id: Long): Currency? = withContext(Dispatchers.IO) {
        currencyDAO.getByIdSync(id)
    }

    override fun getCurrencyByName(name: String): Flow<Currency?> = currencyDAO.getByName(name).asFlow()

    override suspend fun getCurrencyByNameSync(name: String): Currency? = withContext(Dispatchers.IO) {
        currencyDAO.getByNameSync(name)
    }

    // ========================================
    // Mutation Operations
    // ========================================

    override suspend fun insertCurrency(currency: Currency): Long = withContext(Dispatchers.IO) {
        currencyDAO.insertSync(currency)
    }

    override suspend fun updateCurrency(currency: Currency) {
        withContext(Dispatchers.IO) {
            currencyDAO.updateSync(currency)
        }
    }

    override suspend fun deleteCurrency(currency: Currency) {
        withContext(Dispatchers.IO) {
            currencyDAO.deleteSync(currency)
        }
    }

    override suspend fun deleteAllCurrencies() {
        withContext(Dispatchers.IO) {
            currencyDAO.deleteAllSync()
        }
    }
}
