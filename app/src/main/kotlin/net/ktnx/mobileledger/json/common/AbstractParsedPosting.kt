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

import net.ktnx.mobileledger.json.ParsedPosting
import net.ktnx.mobileledger.model.Currency
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.model.LedgerTransactionAccount

/**
 * Utility object providing common functionality for ParsedPosting implementations.
 *
 * This object extracts the common logic from the various version-specific
 * ParsedPosting classes to reduce code duplication while maintaining
 * compatibility with the existing class hierarchy.
 */
object PostingHelper {
    /**
     * Get the commodity side based on user settings.
     * @return 'R' for right (after), 'L' for left (before)
     */
    @JvmStatic
    fun getCommoditySide(): Char = if (Data.currencySymbolPosition.value == Currency.Position.after) 'R' else 'L'

    /**
     * Get whether commodity should have a gap from the amount.
     * @return true if there should be a space between amount and commodity
     */
    @JvmStatic
    fun getCommoditySpaced(): Boolean = Data.currencyGap.value ?: false

    /**
     * Common conversion from LedgerTransactionAccount to posting fields.
     * Sets paccount and pcomment on the provided posting fields.
     *
     * @param acc The ledger transaction account to convert from
     * @param setAccount Function to set the account name
     * @param setComment Function to set the comment
     */
    @JvmStatic
    fun populateFromLedgerAccount(
        acc: LedgerTransactionAccount,
        setAccount: (String) -> Unit,
        setComment: (String) -> Unit
    ) {
        setAccount(acc.accountName)
        setComment(acc.comment ?: "")
    }
}
