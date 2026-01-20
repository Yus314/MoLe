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
import net.ktnx.mobileledger.domain.model.Currency

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
 *     val currencies = currencyRepository.observeAllCurrenciesAsDomain()
 *         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
 * }
 * ```
 */
interface CurrencyRepository {

    // ========================================
    // Query Operations
    // ========================================

    /**
     * Observe all currencies as domain models.
     *
     * @return Flow that emits the currency domain model list whenever it changes
     */
    fun observeAllCurrenciesAsDomain(): Flow<List<Currency>>

    /**
     * Get all currencies as domain models.
     *
     * @return List of all currency domain models
     */
    suspend fun getAllCurrenciesAsDomain(): List<Currency>

    /**
     * Observe a currency as domain model by its ID.
     *
     * @param id The currency ID
     * @return Flow that emits the currency domain model when it changes
     */
    fun observeCurrencyAsDomain(id: Long): Flow<Currency?>

    /**
     * Get a currency as domain model by its ID.
     *
     * @param id The currency ID
     * @return The currency domain model or null if not found
     */
    suspend fun getCurrencyAsDomain(id: Long): Currency?

    /**
     * Get a currency as domain model by its name.
     *
     * @param name The currency name
     * @return The currency domain model or null if not found
     */
    suspend fun getCurrencyAsDomainByName(name: String): Currency?

    /**
     * Observe a currency as domain model by its name.
     *
     * @param name The currency name
     * @return Flow that emits the currency domain model when it changes
     */
    fun observeCurrencyAsDomainByName(name: String): Flow<Currency?>

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Delete all currencies.
     */
    suspend fun deleteAllCurrencies()

    /**
     * Save a currency domain model.
     * Handles insert/update automatically based on whether the currency has an ID.
     *
     * @param currency The currency domain model to save
     * @return The saved currency ID
     */
    suspend fun saveCurrency(currency: Currency): Long

    /**
     * Delete a currency by its name.
     *
     * @param name The currency name to delete
     * @return true if deleted, false if not found
     */
    suspend fun deleteCurrencyByName(name: String): Boolean
}
