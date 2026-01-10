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

import kotlinx.coroutines.flow.Flow
import net.ktnx.mobileledger.db.Currency

/**
 * Repository interface for Currency data access.
 *
 * This repository provides:
 * - Reactive access to currencies via Flow
 * - CRUD operations for currencies
 *
 * ## Usage
 *
 * ```kotlin
 * @HiltViewModel
 * class CurrencyViewModel @Inject constructor(
 *     private val currencyRepository: CurrencyRepository
 * ) : ViewModel() {
 *     val currencies = currencyRepository.getAllCurrencies()
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
 * }
 * ```
 */
interface CurrencyRepository {

    // ========================================
    // Query Operations
    // ========================================

    /**
     * Get all currencies.
     *
     * @return Flow that emits the currency list whenever it changes
     */
    fun getAllCurrencies(): Flow<List<Currency>>

    /**
     * Get all currencies synchronously.
     *
     * @return List of all currencies
     */
    suspend fun getAllCurrenciesSync(): List<Currency>

    /**
     * Get a currency by its ID.
     *
     * @param id The currency ID
     * @return Flow that emits the currency when it changes
     */
    fun getCurrencyById(id: Long): Flow<Currency?>

    /**
     * Get a currency by its ID synchronously.
     *
     * @param id The currency ID
     * @return The currency or null if not found
     */
    suspend fun getCurrencyByIdSync(id: Long): Currency?

    /**
     * Get a currency by its name.
     *
     * @param name The currency name
     * @return Flow that emits the currency when it changes
     */
    fun getCurrencyByName(name: String): Flow<Currency?>

    /**
     * Get a currency by its name synchronously.
     *
     * @param name The currency name
     * @return The currency or null if not found
     */
    suspend fun getCurrencyByNameSync(name: String): Currency?

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert a new currency.
     *
     * @param currency The currency to insert
     * @return The ID of the inserted currency
     */
    suspend fun insertCurrency(currency: Currency): Long

    /**
     * Update an existing currency.
     *
     * @param currency The currency to update
     */
    suspend fun updateCurrency(currency: Currency)

    /**
     * Delete a currency.
     *
     * @param currency The currency to delete
     */
    suspend fun deleteCurrency(currency: Currency)

    /**
     * Delete all currencies.
     */
    suspend fun deleteAllCurrencies()
}
