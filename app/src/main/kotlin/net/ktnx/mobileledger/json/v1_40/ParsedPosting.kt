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
import net.ktnx.mobileledger.domain.model.TransactionLine
import net.ktnx.mobileledger.json.ParsedPosting as BasePosting
import net.ktnx.mobileledger.json.common.StyleConfigurer
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
        val amountStyle = parsedStyle?.let {
            AmountStyle.fromParsedStyle(it, amt.acommodity)
        }

        return LedgerTransactionAccount(
            paccount ?: "",
            amt?.aquantity?.asFloat() ?: 0f,
            amt?.acommodity,
            pcomment,
            amountStyle
        )
    }

    fun toDomain(): TransactionLine {
        val amt = pamount?.firstOrNull()
        return TransactionLine(
            id = null,
            accountName = paccount ?: "",
            amount = amt?.aquantity?.asFloat(),
            currency = amt?.acommodity ?: "",
            comment = pcomment.takeIf { it.isNotEmpty() }
        )
    }

    companion object {
        @JvmStatic
        fun fromLedgerAccount(acc: LedgerTransactionAccount): ParsedPosting = ParsedPosting().apply {
            paccount = acc.accountName
            pcomment = acc.comment ?: ""
            pamount = mutableListOf(
                ParsedAmount().apply {
                    acommodity = acc.currency ?: ""
                    aismultiplier = false
                    aquantity = ParsedQuantity().apply {
                        decimalPlaces = 2
                        decimalMantissa = Math.round(acc.amount * 100).toLong()
                    }
                    astyle = ParsedStyle().apply {
                        ascommodityside = getCommoditySide()
                        isAscommodityspaced = getCommoditySpaced()
                        StyleConfigurer.DecimalMarkString.configureStyle(this, 2)
                    }
                }
            )
        }

        @JvmStatic
        fun fromDomain(line: TransactionLine): ParsedPosting = ParsedPosting().apply {
            paccount = line.accountName
            pcomment = line.comment ?: ""
            pamount = mutableListOf(
                ParsedAmount().apply {
                    acommodity = line.currency
                    aismultiplier = false
                    aquantity = ParsedQuantity().apply {
                        decimalPlaces = 2
                        decimalMantissa = Math.round((line.amount ?: 0f) * 100).toLong()
                    }
                    astyle = ParsedStyle().apply {
                        ascommodityside = getCommoditySide()
                        isAscommodityspaced = getCommoditySpaced()
                        StyleConfigurer.DecimalMarkString.configureStyle(this, 2)
                    }
                }
            )
        }
    }
}
