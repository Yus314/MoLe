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

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import net.ktnx.mobileledger.service.CurrencyFormatConfig
import net.ktnx.mobileledger.service.CurrencyFormatter

/**
 * Fake implementation of [CurrencyFormatter] for testing.
 *
 * This fake provides simple formatting behavior without locale complexity.
 */
class FakeCurrencyFormatter : CurrencyFormatter {

    private val _locale = MutableStateFlow(Locale.US)
    override val locale: StateFlow<Locale> = _locale.asStateFlow()

    private val _config = MutableStateFlow(CurrencyFormatConfig.fromLocale(Locale.US))
    override val config: StateFlow<CurrencyFormatConfig> = _config.asStateFlow()

    private val _currencySymbolPosition = MutableStateFlow(CurrencyPosition.BEFORE)
    override val currencySymbolPosition: StateFlow<CurrencyPosition> = _currencySymbolPosition.asStateFlow()

    private val _currencyGap = MutableStateFlow(false)
    override val currencyGap: StateFlow<Boolean> = _currencyGap.asStateFlow()

    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    override fun formatCurrency(amount: Float, currencySymbol: String?): String {
        val formatted = decimalFormat.format(amount)
        return if (currencySymbol != null) {
            if (_currencySymbolPosition.value == CurrencyPosition.BEFORE) {
                if (_currencyGap.value) "$currencySymbol $formatted" else "$currencySymbol$formatted"
            } else {
                if (_currencyGap.value) "$formatted $currencySymbol" else "$formatted$currencySymbol"
            }
        } else {
            formatted
        }
    }

    override fun formatNumber(number: Float): String = decimalFormat.format(number)

    override fun parseNumber(str: String): Float {
        val normalized = str.replace(",", "")
        return normalized.toFloat()
    }

    override fun getDecimalSeparator(): String = "."

    override fun getGroupingSeparator(): String = ","

    override fun refresh(locale: Locale) {
        _locale.value = locale
        _config.value = CurrencyFormatConfig.fromLocale(locale)
    }

    override fun updateFromAmountStyle(symbolPosition: CurrencyPosition, hasGap: Boolean) {
        _currencySymbolPosition.value = symbolPosition
        _currencyGap.value = hasGap
    }

    fun setLocale(newLocale: Locale) {
        _locale.value = newLocale
    }

    fun setCurrencySymbolPosition(position: CurrencyPosition) {
        _currencySymbolPosition.value = position
    }

    fun setCurrencyGap(gap: Boolean) {
        _currencyGap.value = gap
    }
}
