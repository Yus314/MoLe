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

package net.ktnx.mobileledger.service

import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import net.ktnx.mobileledger.core.domain.model.CurrencySettings

/**
 * Currency/number formatting service interface.
 *
 * Provides locale-dependent currency and number formatting.
 * @Singleton scoped to reflect locale changes across all screens.
 *
 * ## Usage in ViewModel
 *
 * ```kotlin
 * @HiltViewModel
 * class TransactionViewModel @Inject constructor(
 *     private val currencyFormatter: CurrencyFormatter
 * ) : ViewModel() {
 *     fun formatAmount(amount: Float): String =
 *         currencyFormatter.formatCurrency(amount)
 * }
 * ```
 *
 * ## Usage in Application
 *
 * ```kotlin
 * @HiltAndroidApp
 * class App : Application() {
 *     @Inject lateinit var currencyFormatter: CurrencyFormatter
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         currencyFormatter.refresh(Locale.getDefault())
 *     }
 *
 *     override fun onConfigurationChanged(newConfig: Configuration) {
 *         super.onConfigurationChanged(newConfig)
 *         currencyFormatter.refresh(Locale.getDefault())
 *     }
 * }
 * ```
 */
interface CurrencyFormatter : CurrencySettings {
    /**
     * Current locale.
     */
    val locale: StateFlow<Locale>

    /**
     * Current format configuration.
     */
    val config: StateFlow<CurrencyFormatConfig>

    /**
     * Currency symbol position (reactive).
     */
    val currencySymbolPosition: StateFlow<CurrencyPosition>

    /**
     * Whether there's a space between symbol and number (reactive).
     */
    val currencyGap: StateFlow<Boolean>

    // CurrencySettings implementation - provides current snapshot values
    override val symbolPosition: CurrencyPosition
        get() = currencySymbolPosition.value

    override val hasGap: Boolean
        get() = currencyGap.value

    /**
     * Format a number as currency.
     *
     * Example: 1234.56 -> "¥1,234.56" (ja-JP) or "1.234,56 €" (de-DE)
     *
     * @param amount Amount to format
     * @param currencySymbol Currency symbol (optional, omit for no symbol)
     * @return Formatted string
     */
    fun formatCurrency(amount: Float, currencySymbol: String? = null): String

    /**
     * Format a number according to locale.
     *
     * Example: 1234.56 -> "1,234.56" (en-US) or "1.234,56" (de-DE)
     *
     * @param number Number to format
     * @return Formatted string
     */
    fun formatNumber(number: Float): String

    /**
     * Parse a string to number according to locale.
     *
     * Example: "1,234.56" (en-US) or "1.234,56" (de-DE) -> 1234.56
     *
     * @param str String to parse
     * @return Parsed number
     * @throws NumberFormatException if parsing fails
     */
    fun parseNumber(str: String): Float

    /**
     * Get the decimal separator for current locale.
     *
     * @return Decimal separator character
     */
    fun getDecimalSeparator(): String

    /**
     * Get the grouping separator for current locale.
     *
     * @return Grouping separator character
     */
    fun getGroupingSeparator(): String

    /**
     * Update format configuration based on locale.
     *
     * @param locale New locale
     */
    fun refresh(locale: Locale)

    /**
     * Update configuration from hledger server's AmountStyle.
     *
     * @param symbolPosition Currency symbol position
     * @param hasGap Whether there's a space between symbol and number
     */
    fun updateFromAmountStyle(symbolPosition: CurrencyPosition, hasGap: Boolean)
}
