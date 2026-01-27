/*
 * Copyright © 2020 Damyan Ivanov.
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

package net.ktnx.mobileledger.json

import net.ktnx.mobileledger.core.domain.model.CurrencyPosition
import net.ktnx.mobileledger.core.domain.model.CurrencySettings

open class ParsedPosting {
    companion object {
        /**
         * Get whether currency symbol has a gap from the amount.
         *
         * @param settings Currency settings (defaults to [CurrencySettings.DEFAULT])
         * @return true if there should be a gap between currency and amount
         */
        @JvmStatic
        protected fun getCommoditySpaced(settings: CurrencySettings = CurrencySettings.DEFAULT): Boolean =
            settings.hasGap

        /**
         * Get the currency symbol side ('L' for left/before, 'R' for right/after).
         *
         * @param settings Currency settings (defaults to [CurrencySettings.DEFAULT])
         * @return 'L' for before, 'R' for after
         */
        @JvmStatic
        protected fun getCommoditySide(settings: CurrencySettings = CurrencySettings.DEFAULT): Char =
            if (settings.symbolPosition == CurrencyPosition.AFTER) 'R' else 'L'
    }
}
