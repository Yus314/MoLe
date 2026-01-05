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

package net.ktnx.mobileledger.json.v1_40

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.ktnx.mobileledger.json.ParsedPosting as BasePosting
import net.ktnx.mobileledger.model.AmountStyle
import net.ktnx.mobileledger.model.LedgerTransactionAccount

@JsonIgnoreProperties(ignoreUnknown = true)
class ParsedPosting : BasePosting() {
    var pbalanceassertion: Void? = null
    var pstatus: String = "Unmarked"
    var paccount: String? = null
    var pamount: MutableList<ParsedAmount>? = null
    var pdate: String? = null
    var pdate2: String? = null
    var ptype: String = "RegularPosting"
    var pcomment: String = ""
        set(value) {
            field = value.trim()
        }
    var ptags: MutableList<List<String>> = mutableListOf()
    var poriginal: String? = null
    var ptransaction_: String = "1"

    fun asLedgerAccount(): LedgerTransactionAccount {
        val amt = pamount?.get(0)
        val parsedStyle = amt?.astyle

        // Preserve style information from hledger JSON
        var amountStyle: AmountStyle? = null
        if (parsedStyle != null) {
            amountStyle = AmountStyle.fromParsedStyle(parsedStyle, amt.acommodity)
        }

        return LedgerTransactionAccount(
            paccount ?: "",
            amt?.aquantity?.asFloat() ?: 0f,
            amt?.acommodity,
            pcomment,
            amountStyle
        )
    }

    companion object {
        @JvmStatic
        fun fromLedgerAccount(acc: LedgerTransactionAccount): ParsedPosting {
            val result = ParsedPosting()
            result.paccount = acc.accountName
            result.pcomment = acc.comment ?: ""

            val amounts = mutableListOf<ParsedAmount>()
            val amt = ParsedAmount()
            amt.acommodity = acc.currency ?: ""
            amt.aismultiplier = false
            val qty = ParsedQuantity()
            qty.decimalPlaces = 2
            qty.decimalMantissa = Math.round(acc.amount * 100).toLong()
            amt.aquantity = qty
            val style = ParsedStyle()
            style.ascommodityside = getCommoditySide()
            style.isAscommodityspaced = getCommoditySpaced()
            style.asprecision = 2
            style.asdecimalmark = "."
            style.asrounding = "NoRounding"
            amt.astyle = style
            if (acc.currency != null) {
                amt.acommodity = acc.currency
            }
            amounts.add(amt)
            result.pamount = amounts
            return result
        }
    }
}
