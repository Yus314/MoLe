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
import net.ktnx.mobileledger.db.Currency as DbCurrency
import net.ktnx.mobileledger.domain.model.Currency as DomainCurrency

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
    // Domain Model Query Operations
    // ========================================

    /**
     * Observe all currencies as domain models.
     *
     * @return Flow that emits the currency domain model list whenever it changes
     */
    fun observeAllCurrenciesAsDomain(): Flow<List<DomainCurrency>>

    /**
     * Get all currencies as domain models.
     *
     * @return List of all currency domain models
     */
    suspend fun getAllCurrenciesAsDomain(): List<DomainCurrency>

    /**
     * Observe a currency as domain model by its ID.
     *
     * @param id The currency ID
     * @return Flow that emits the currency domain model when it changes
     */
    fun observeCurrencyAsDomain(id: Long): Flow<DomainCurrency?>

    /**
     * Get a currency as domain model by its ID.
     *
     * @param id The currency ID
     * @return The currency domain model or null if not found
     */
    suspend fun getCurrencyAsDomain(id: Long): DomainCurrency?

    /**
     * Get a currency as domain model by its name.
     *
     * @param name The currency name
     * @return The currency domain model or null if not found
     */
    suspend fun getCurrencyAsDomainByName(name: String): DomainCurrency?

    /**
     * Observe a currency as domain model by its name.
     *
     * @param name The currency name
     * @return Flow that emits the currency domain model when it changes
     */
    fun observeCurrencyAsDomainByName(name: String): Flow<DomainCurrency?>

    // ========================================
    // Database Entity Query Operations (for internal use)
    // Note: These methods are deprecated. Use domain model methods instead.
    // ========================================

    /**
     * Observe all currencies.
     *
     * @return Flow that emits the currency list whenever it changes
     */
    @Deprecated(
        message = "Use observeAllCurrenciesAsDomain() instead",
        replaceWith = ReplaceWith("observeAllCurrenciesAsDomain()")
    )
    fun observeAllCurrencies(): Flow<List<DbCurrency>>

    /**
     * Get all currencies.
     *
     * @return List of all currencies
     */
    @Deprecated(
        message = "Use getAllCurrenciesAsDomain() instead",
        replaceWith = ReplaceWith("getAllCurrenciesAsDomain()")
    )
    suspend fun getAllCurrencies(): List<DbCurrency>

    /**
     * Observe a currency by its ID.
     *
     * @param id The currency ID
     * @return Flow that emits the currency when it changes
     */
    @Deprecated(
        message = "Use observeCurrencyAsDomain(id) instead",
        replaceWith = ReplaceWith("observeCurrencyAsDomain(id)")
    )
    fun observeCurrencyById(id: Long): Flow<DbCurrency?>

    /**
     * Observe a currency by its name.
     *
     * @param name The currency name
     * @return Flow that emits the currency when it changes
     */
    @Deprecated(
        message = "Use observeCurrencyAsDomainByName(name) instead",
        replaceWith = ReplaceWith("observeCurrencyAsDomainByName(name)")
    )
    fun observeCurrencyByName(name: String): Flow<DbCurrency?>

    /**
     * Get a currency by its name.
     *
     * @param name The currency name
     * @return The currency or null if not found
     */
    @Deprecated(
        message = "Use getCurrencyAsDomainByName(name) instead",
        replaceWith = ReplaceWith("getCurrencyAsDomainByName(name)")
    )
    suspend fun getCurrencyByName(name: String): DbCurrency?

    // ========================================
    // Mutation Operations
    // ========================================

    /**
     * Insert a new currency.
     *
     * @param currency The currency to insert
     * @return The ID of the inserted currency
     */
    @Deprecated(
        message = "Use saveCurrency(DomainCurrency) instead",
        replaceWith = ReplaceWith("saveCurrency(currency.toDomain())")
    )
    suspend fun insertCurrency(currency: DbCurrency): Long

    /**
     * Update an existing currency.
     *
     * @param currency The currency to update
     */
    @Deprecated(
        message = "Use saveCurrency(DomainCurrency) instead",
        replaceWith = ReplaceWith("saveCurrency(currency.toDomain())")
    )
    suspend fun updateCurrency(currency: DbCurrency)

    /**
     * Delete a currency.
     *
     * @param currency The currency to delete
     */
    @Deprecated(
        message = "Use deleteCurrencyByName(name) instead",
        replaceWith = ReplaceWith("deleteCurrencyByName(currency.name)")
    )
    suspend fun deleteCurrency(currency: DbCurrency)

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
    suspend fun saveCurrency(currency: DomainCurrency): Long

    /**
     * Delete a currency by its name.
     *
     * @param name The currency name to delete
     * @return true if deleted, false if not found
     */
    suspend fun deleteCurrencyByName(name: String): Boolean
}
