/*
 * Copyright © 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.json.common

import net.ktnx.mobileledger.model.AmountStyle

/**
 * Utility object providing common functionality for ParsedLedgerAccount implementations.
 *
 * The primary logic for account processing is already in the base ParsedLedgerAccount class.
 * This object provides any additional helper methods that might be useful across versions.
 */
object AccountHelper {
    /**
     * Create a SimpleBalance from parsed balance data.
     *
     * @param commodity The commodity/currency code
     * @param amount The balance amount
     * @param amountStyle Optional amount style for formatting
     * @return A SimpleBalance object
     */
    @JvmStatic
    fun createSimpleBalance(
        commodity: String,
        amount: Float,
        amountStyle: AmountStyle?
    ): net.ktnx.mobileledger.json.ParsedLedgerAccount.SimpleBalance = net.ktnx.mobileledger.json.ParsedLedgerAccount.SimpleBalance(
            commodity,
            amount,
            amountStyle
        )
}
