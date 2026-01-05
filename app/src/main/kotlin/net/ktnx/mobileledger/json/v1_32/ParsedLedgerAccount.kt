/*
 * Copyright © 2020, 2024 Damyan Ivanov.
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

package net.ktnx.mobileledger.json.v1_32

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.ktnx.mobileledger.json.ParsedLedgerAccount as BaseAccount
import net.ktnx.mobileledger.model.AmountStyle

@JsonIgnoreProperties(ignoreUnknown = true)
class ParsedLedgerAccount : BaseAccount() {
    var aebalance: List<ParsedBalance>? = null
    var aibalance: List<ParsedBalance>? = null
    var adeclarationinfo: ParsedDeclarationInfo? = null  // Added in hledger-web v1.32

    override fun getSimpleBalance(): List<SimpleBalance> {
        val result = mutableListOf<SimpleBalance>()
        aibalance?.forEach { b ->
            val style = AmountStyle.fromParsedStyle(b.astyle, b.acommodity)
            result.add(SimpleBalance(b.acommodity, b.aquantity?.asFloat() ?: 0f, style))
        }
        return result
    }
}
