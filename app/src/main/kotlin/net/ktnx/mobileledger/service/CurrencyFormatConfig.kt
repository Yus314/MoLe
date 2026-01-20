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
import java.util.Locale
import net.ktnx.mobileledger.domain.model.CurrencyPosition

/**
 * Currency format configuration.
 *
 * @property locale Current locale
 * @property symbolPosition Currency symbol position
 * @property hasGap Whether there's a space between symbol and number
 * @property decimalSeparator Decimal separator character
 * @property groupingSeparator Grouping separator character
 */
data class CurrencyFormatConfig(
    val locale: Locale,
    val symbolPosition: CurrencyPosition,
    val hasGap: Boolean,
    val decimalSeparator: Char,
    val groupingSeparator: Char
) {
    companion object {
        /**
         * Create default configuration from locale.
         */
        fun fromLocale(locale: Locale): CurrencyFormatConfig {
            val symbols = DecimalFormatSymbols.getInstance(locale)
            return CurrencyFormatConfig(
                locale = locale,
                symbolPosition = CurrencyPosition.BEFORE,
                hasGap = true,
                decimalSeparator = symbols.decimalSeparator,
                groupingSeparator = symbols.groupingSeparator
            )
        }
    }
}
