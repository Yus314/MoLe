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

import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.text.ParsePosition
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.logcat
import net.ktnx.mobileledger.domain.model.CurrencyPosition

/**
 * Implementation of [CurrencyFormatter].
 *
 * Provides locale-dependent currency and number formatting,
 * migrated from AppStateManager.
 */
@Singleton
class CurrencyFormatterImpl @Inject constructor() : CurrencyFormatter {

    private val _locale = MutableStateFlow(Locale.getDefault())
    override val locale: StateFlow<Locale> = _locale.asStateFlow()

    private val _config = MutableStateFlow(CurrencyFormatConfig.fromLocale(Locale.getDefault()))
    override val config: StateFlow<CurrencyFormatConfig> = _config.asStateFlow()

    private val _currencySymbolPosition = MutableStateFlow(CurrencyPosition.BEFORE)
    override val currencySymbolPosition: StateFlow<CurrencyPosition> =
        _currencySymbolPosition.asStateFlow()

    private val _currencyGap = MutableStateFlow(true)
    override val currencyGap: StateFlow<Boolean> = _currencyGap.asStateFlow()

    private var numberFormatter: NumberFormat? = null
    private var decimalSeparator: String = "."

    init {
        refresh(Locale.getDefault())
    }

    override fun formatCurrency(amount: Float, currencySymbol: String?): String {
        val formatted = formatNumber(amount)
        val symbol = currencySymbol ?: return formatted
        val gap = if (_currencyGap.value) " " else ""

        return when (_currencySymbolPosition.value) {
            CurrencyPosition.BEFORE -> "$symbol$gap$formatted"
            CurrencyPosition.AFTER -> "$formatted$gap$symbol"
            else -> formatted
        }
    }

    override fun formatNumber(number: Float): String = numberFormatter?.format(number) ?: number.toString()

    @Throws(ParseException::class)
    override fun parseNumber(str: String): Float {
        val pos = ParsePosition(0)
        val parsed = numberFormatter?.parse(str)
        if (parsed == null || pos.errorIndex > -1) {
            throw ParseException("Error parsing '$str'", pos.errorIndex)
        }
        return parsed.toFloat()
    }

    override fun getDecimalSeparator(): String = decimalSeparator

    override fun getGroupingSeparator(): String = _config.value.groupingSeparator.toString()

    override fun refresh(locale: Locale) {
        _locale.value = locale

        val formatter = NumberFormat.getCurrencyInstance(locale)
        val currency = formatter.currency
        val symbol = currency?.symbol ?: ""

        logcat { "Discovering currency symbol position for locale $locale (currency: $currency, symbol: $symbol)" }

        val formatted = formatter.format(1234.56f)
        logcat { "1234.56 formats as '$formatted'" }

        when {
            formatted.startsWith(symbol) -> {
                _currencySymbolPosition.value = CurrencyPosition.BEFORE
                val canary = formatted[symbol.length]
                _currencyGap.value = canary != '1'
            }

            formatted.endsWith(symbol) -> {
                _currencySymbolPosition.value = CurrencyPosition.AFTER
                val canary = formatted[formatted.length - symbol.length - 1]
                _currencyGap.value = canary != '6'
            }

            else -> _currencySymbolPosition.value = CurrencyPosition.NONE
        }

        numberFormatter = NumberFormat.getNumberInstance(locale).apply {
            isParseIntegerOnly = false
            isGroupingUsed = true
            minimumIntegerDigits = 1
            minimumFractionDigits = 2
        }

        val symbols = DecimalFormatSymbols.getInstance(locale)
        decimalSeparator = symbols.monetaryDecimalSeparator.toString()

        _config.value = CurrencyFormatConfig(
            locale = locale,
            symbolPosition = _currencySymbolPosition.value,
            hasGap = _currencyGap.value,
            decimalSeparator = symbols.decimalSeparator,
            groupingSeparator = symbols.groupingSeparator
        )
    }

    override fun updateFromAmountStyle(symbolPosition: CurrencyPosition, hasGap: Boolean) {
        _currencySymbolPosition.value = symbolPosition
        _currencyGap.value = hasGap
        _config.value = _config.value.copy(
            symbolPosition = symbolPosition,
            hasGap = hasGap
        )
    }
}
